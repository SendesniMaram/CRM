import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {buildModel, migrationManifest} from '../../validation/prepare-lot2b.mjs';

export const groups = {
  A: [],
  demo: ['department-service', 'employee-service', 'hr-service', 'customer-service', 'invoice-service'],
  full: ['department-service', 'employee-service', 'hr-service', 'customer-service', 'invoice-service', 'payroll-service', 'fees-service']
};
export function modelFor(root, run, group) {
  if (!/^\d{8}-\d{6}$/.test(run) || !Object.hasOwn(groups, group)) throw Error('Invalid campaign/group');
  const model = buildModel(root, 'crm-lot2b-' + run);
  model.name = 'crm-express-' + run + '-' + group.toLowerCase();
  const names = ['mysql', 'discovery-service', 'identity-service', 'gateway-service', ...groups[group]];
  model.services = Object.fromEntries(names.map(name => [name, model.services[name]]));
  for (const [name, service] of Object.entries(model.services)) {
    if (name === 'mysql') continue;
    service.image = model.name + '/' + name + ':candidate';
    service.labels = {'crm.campaign': model.name};
    service.healthcheck = {test: ['CMD-SHELL', 'bash -c "exec 3<>/dev/tcp/127.0.0.1/' + ({'discovery-service':8761,'gateway-service':8080,'identity-service':8082,'department-service':8084,'employee-service':8083,'customer-service':8085,'hr-service':8086,'payroll-service':8087,'fees-service':8088,'invoice-service':8089}[name]) + '"'], interval:'10s',timeout:'3s',retries:18,start_period:'40s'};
    if (service.depends_on) service.depends_on = Object.fromEntries(Object.entries(service.depends_on).filter(([dep]) => names.includes(dep)).map(([dep]) => [dep,{condition:'service_healthy'}]));
  }
  model.services['identity-service'].environment.APP_BOOTSTRAP_ADMIN_USERNAME = 'express-admin';
  model.services['identity-service'].environment.APP_BOOTSTRAP_ADMIN_EMAIL = 'express-admin@example.test';
  // A TCP healthcheck is only startup ordering; API/security tests remain mandatory.
  model.services.frontend = {image:'nginx:alpine',restart:'no',mem_limit:'128m',pids_limit:64,networks:['isolated'],ports:['127.0.0.1:14200:80'],volumes:[{type:'bind',source:'./frontend-dist/browser',target:'/usr/share/nginx/html',read_only:true,bind:{create_host_path:false}},{type:'bind',source:'./nginx.conf',target:'/etc/nginx/conf.d/default.conf',read_only:true,bind:{create_host_path:false}}],healthcheck:{test:['CMD-SHELL','wget -q -O /dev/null http://127.0.0.1/health'],interval:'10s',timeout:'3s',retries:6}};
  const used = new Set(Object.values(model.services).flatMap(s => (s.volumes ?? []).filter(v => v.type === 'volume').map(v => v.source)));
  model.volumes = Object.fromEntries(Object.entries(model.volumes).filter(([name]) => used.has(name)));
  return model;
}
export function prepare(root, run, group) {
  const model = modelFor(root, run, group);
  const output = path.join(root, 'target', model.name);
  if (fs.existsSync(output)) throw Error('Campaign exists; refusing overwrite');
  fs.mkdirSync(path.join(output,'mysql'),{recursive:true});
  fs.mkdirSync(path.join(output,'gateway'));
  fs.mkdirSync(path.join(output,'evidence'));
  const domains = Object.keys(model.services).filter(s => s.endsWith('-service') && !['gateway-service','discovery-service'].includes(s)).map(s => s.slice(0,-8));
  fs.writeFileSync(path.join(output,'mysql/001-schemas.sql'),domains.map(d => 'CREATE DATABASE ' + d + '_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;').join('\n')+'\n');
  fs.writeFileSync(path.join(output,'gateway/application-validation.yml'),fs.readFileSync(path.join(root,'microservices/gateway-service/src/main/resources/application.yml'),'utf8').replaceAll('http://localhost:4200','http://localhost:14200'));
  fs.writeFileSync(path.join(output,'nginx.conf'),'server { listen 80; server_name _; root /usr/share/nginx/html; location = /health { access_log off; return 200 "ok"; } location / { try_files $uri $uri/ /index.html; } add_header X-Content-Type-Options nosniff always; add_header Referrer-Policy no-referrer always; }\n');
  fs.writeFileSync(path.join(output,'compose.json'),JSON.stringify(model,null,2));
  const limit = Object.values(model.services).reduce((n,s) => n + parseInt(s.mem_limit),0);
  const migrations = migrationManifest(root);
  const jarFiles = Object.keys(model.services).filter(n => n.endsWith('-service')).map(n => {const p = path.join(root,'microservices',n,'target',n+'-0.0.1-SNAPSHOT.jar');return {service:n,path:p,sha256:fs.existsSync(p)?crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex'):null};});
  const sourceFiles = {};
  function snapshot(dir) { for (const e of fs.readdirSync(dir,{withFileTypes:true})) {const p=path.join(dir,e.name);if(e.isSymbolicLink())throw Error('Source link refused');if(e.isDirectory())snapshot(p);else if(!/\.(pem|key|p12)$/i.test(e.name))sourceFiles[path.relative(root,p)]=crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');} }
  for(const name of ['common-security',...Object.keys(model.services).filter(n=>n.endsWith('-service'))]) snapshot(path.join(root,'microservices',name,'src/main'));
  const configFiles=Object.fromEntries(['compose.json','nginx.conf','mysql/001-schemas.sql','gateway/application-validation.yml'].map(p=>[p,crypto.createHash('sha256').update(fs.readFileSync(path.join(output,p))).digest('hex')]));
  fs.writeFileSync(path.join(output,'manifest.json'),JSON.stringify({run,group,project:model.name,commit:execFileSync('git',['rev-parse','HEAD'],{cwd:root,encoding:'utf8'}).trim(),limitMiB:limit,hostFreeRequiredMiB:limit+1536,dockerRequiredMiB:limit+512,migrations,jars:jarFiles,sourceFiles,configFiles,status:'PREPARED_NOT_DEPLOYED'},null,2));
  return output;
}
if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const root = fileURLToPath(new URL('../../../',import.meta.url));
  console.log(prepare(root,process.argv[2],process.argv[3]));
}
