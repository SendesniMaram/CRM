import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';
import { buildModel, migrationManifest } from './prepare-lot2b.mjs';

const core=['mysql','discovery-service','identity-service','gateway-service'];
export const stages={
 A:[...core],
 B:[...core,'department-service','employee-service','hr-service'],
 C:[...core,'customer-service','invoice-service'],
 D:[...core,'department-service','employee-service','hr-service'],
 'E-bootstrap':[...core,'department-service','employee-service','hr-service'],
 'E-payroll':[...core,'hr-service','payroll-service'],
 'E-fees':[...core,'fees-service']
};
export function progressiveModel(root,runId,stage){
 if(!/^\d{8}-\d{6}$/.test(runId)||!Object.hasOwn(stages,stage))throw new Error('Invalid campaign or stage');
 const model=buildModel(root,'crm-lot2b-'+runId);
 const family=stage==='E-bootstrap'||stage==='E-payroll'?'E':stage;
 model.name='crm-lot2br-'+runId+'-'+family.toLowerCase();
 model.services=Object.fromEntries(stages[stage].map(name=>[name,model.services[name]]));
 for(const [name,service] of Object.entries(model.services)){
  if(service.image.startsWith('crm-lot2b-'))service.image='crm-lot2br-'+runId+'/'+name+':candidate';
  if(service.depends_on)service.depends_on=Object.fromEntries(Object.entries(service.depends_on).filter(([dep])=>Object.hasOwn(model.services,dep)));
 }
 model.services.mysql.volumes.find(v=>v.type==='bind').source='./mysql/'+family;
 // Keep JVM/TLS/JWT settings intact. Reduce simultaneous services, not their protections.
 const used=new Set(Object.values(model.services).flatMap(s=>(s.volumes??[]).filter(v=>v.type==='volume').map(v=>v.source)));
 model.volumes=Object.fromEntries(Object.entries(model.volumes).filter(([name])=>used.has(name)));
 return model;
}
export function stageBudget(model){
 const limitMiB=Object.values(model.services).reduce((n,s)=>n+parseInt(s.mem_limit),0);
 return {limitMiB,hostFreeRequiredMiB:limitMiB+1536,dockerTotalRequiredMiB:limitMiB+512};
}
export function prepareProgressive(runId){
 const root=fs.realpathSync(fileURLToPath(new URL('../../',import.meta.url)));
 progressiveModel(root,runId,'A');
 const target=path.join(root,'target');
 if(fs.existsSync(target)&&fs.realpathSync(target)!==target)throw new Error('Redirected target refused');
 const output=path.join(target,'crm-lot2br-'+runId);
 if(fs.existsSync(output))throw new Error('Campaign already exists; no overwrite');
 const models=Object.fromEntries(Object.keys(stages).map(s=>[s,progressiveModel(root,runId,s)]));
 const migrations=migrationManifest(root),plan={runId,stages:{}};
 fs.mkdirSync(path.join(output,'gateway'),{recursive:true});
 fs.mkdirSync(path.join(output,'evidence'));
 fs.writeFileSync(path.join(output,'gateway/application-validation.yml'),fs.readFileSync(path.join(root,'microservices/gateway-service/src/main/resources/application.yml'),'utf8').replaceAll('http://localhost:4200','http://localhost:14200'));
 for(const [stage,model] of Object.entries(models)){
  fs.writeFileSync(path.join(output,stage+'.compose.json'),JSON.stringify(model,null,2)+'\n');
  const family=stage.startsWith('E-')&&stage!=='E-fees'?'E':stage;
  const domainSet=new Set(Object.keys(model.services).filter(s=>s.endsWith('-service')&&!['discovery-service','gateway-service'].includes(s)).map(s=>s.slice(0,-8)));
  if(family==='E')for(const d of ['identity','department','employee','hr','payroll'])domainSet.add(d);
  const folder=path.join(output,'mysql',family);fs.mkdirSync(folder,{recursive:true});
  fs.writeFileSync(path.join(folder,'001-schemas.sql'),[...domainSet].sort().map(d=>`CREATE DATABASE ${d}_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`).join('\n')+'\n');
  plan.stages[stage]={project:model.name,services:Object.keys(model.services),...stageBudget(model),
   migrations:Object.fromEntries([...domainSet].sort().map(d=>[d,migrations[d]]))};
 }
 fs.writeFileSync(path.join(output,'plan.json'),JSON.stringify(plan,null,2)+'\n');
 console.log(JSON.stringify({directory:output,status:'PREPARED ONLY; no Docker, SQL, HTTP, keys or secrets accessed',budgets:Object.fromEntries(Object.entries(plan.stages).map(([s,p])=>[s,stageBudget(models[s])]))},null,2));
 return output;
}
export function checkProgressive(directory){
 const root=fs.realpathSync(fileURLToPath(new URL('../../',import.meta.url))),output=fs.realpathSync(directory);
 const match=/^crm-lot2br-(\d{8}-\d{6})$/.exec(path.basename(output));
 if(path.dirname(output)!==path.join(root,'target')||!match)throw new Error('Unexpected campaign location');
 const runId=match[1],plan=JSON.parse(fs.readFileSync(path.join(output,'plan.json'),'utf8')),migrations=migrationManifest(root);
 assert.equal(plan.runId,runId);assert.deepEqual(Object.keys(plan.stages),Object.keys(stages));
 for(const stage of Object.keys(stages)){
  const expected=progressiveModel(root,runId,stage);
  assert.deepEqual(JSON.parse(fs.readFileSync(path.join(output,stage+'.compose.json'),'utf8')),expected);
  const family=stage.startsWith('E-')&&stage!=='E-fees'?'E':stage;
  const domains=new Set(Object.keys(expected.services).filter(s=>s.endsWith('-service')&&!['discovery-service','gateway-service'].includes(s)).map(s=>s.slice(0,-8)));
  if(family==='E')for(const d of ['identity','department','employee','hr','payroll'])domains.add(d);
  assert.deepEqual(plan.stages[stage],{project:expected.name,services:Object.keys(expected.services),...stageBudget(expected),migrations:Object.fromEntries([...domains].sort().map(d=>[d,migrations[d]]))});
  assert.equal(fs.readFileSync(path.join(output,'mysql',family,'001-schemas.sql'),'utf8'),[...domains].sort().map(d=>`CREATE DATABASE ${d}_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`).join('\n')+'\n');
 }
 assert.equal(fs.readFileSync(path.join(output,'gateway/application-validation.yml'),'utf8'),fs.readFileSync(path.join(root,'microservices/gateway-service/src/main/resources/application.yml'),'utf8').replaceAll('http://localhost:4200','http://localhost:14200'));
 console.log('STATIC CHECK PASSED: seven stages, budgets, isolated models, schemas and migration fingerprints');
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
 if(process.argv.length===4&&process.argv[2]==='--check')checkProgressive(process.argv[3]);
 else if(process.argv.length===3)prepareProgressive(process.argv[2]);
 else throw new Error('Usage: prepare-progressive.mjs YYYYMMDD-HHMMSS | --check CAMPAIGN_DIRECTORY');
}
