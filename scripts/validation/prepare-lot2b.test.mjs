import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { buildModel, domains, migrationManifest } from './prepare-lot2b.mjs';
const root=fileURLToPath(new URL('../../',import.meta.url));
const model=buildModel(root,'crm-lot2b-20261101-000001');
test('project identifiers cannot select a development project',()=>{
 for(const name of ['crm','crm-validation','../crm','crm-lot2b-other'])assert.throws(()=>buildModel(root,name));
});
test('storage and network are project scoped, never external or globally named',()=>{
 for(const declaration of [...Object.values(model.volumes),...Object.values(model.networks)]){
  assert.equal(declaration.external,undefined);assert.equal(declaration.name,undefined);
 }
 for(const service of Object.values(model.services)){
  assert.deepEqual(service.networks,['isolated']);assert.equal(service.container_name,undefined);
  for(const volume of service.volumes??[])if(volume.type==='volume')assert.ok(Object.hasOwn(model.volumes,volume.source));
 }
});
test('only Gateway and Eureka inspection are published, on loopback',()=>{
 assert.deepEqual(Object.entries(model.services).flatMap(([name,s])=>(s.ports??[]).map(p=>[name,p])),[['discovery-service','127.0.0.1:18761:8761'],['gateway-service','127.0.0.1:18080:8080']]);
});
test('all ten JVMs and MySQL have bounded memory and SQL logging is disabled',()=>{
 assert.equal(Object.keys(model.services).length,11);
 for(const [name,service] of Object.entries(model.services)){
  assert.match(service.mem_limit,/^\d+m$/);assert.equal(service.restart,'no');
  if(name!=='mysql'){assert.match(service.environment.JAVA_TOOL_OPTIONS,/-Xmx(256|384)m/);assert.equal(service.environment.SPRING_JPA_SHOW_SQL,'false');}
 }
 const total=Object.values(model.services).reduce((n,s)=>n+parseInt(s.mem_limit),0);assert.equal(total,8192);
});
test('all eight schemas use Flyway strictly instead of Hibernate update or automatic baseline',()=>{
 for(const d of domains){const e=model.services[d+'-service'].environment;
  assert.ok(e.SPRING_DATASOURCE_URL.startsWith('jdbc:mysql://mysql:3306/'+d+'_db?'));
  assert.equal(e.SPRING_JPA_HIBERNATE_DDL_AUTO,'validate');assert.equal(e.SPRING_SQL_INIT_MODE,'never');
  assert.equal(e.SPRING_FLYWAY_BASELINE_ON_MIGRATE,'false');assert.equal(e.SPRING_FLYWAY_ENABLED,'true');assert.equal(e.SPRING_FLYWAY_CLEAN_DISABLED,'true');
 }
});
test('no actual credentials or development variable names are embedded',()=>{
 const text=JSON.stringify(model);assert.ok(!text.includes('${MYSQL_ROOT_PASSWORD'));assert.ok(!text.includes('${APP_JWT_SECRET'));
 for(const name of ['CRM_2B_MYSQL_PASSWORD','CRM_2B_JWT_SECRET','CRM_2B_ADMIN_PASSWORD','CRM_PROFILE_MATERIAL_DIR'])assert.ok(text.includes('${'+name+':?'));
});
test('HTTPS security settings match the preserved Lot 1C overlay exactly',()=>{
 const overlay=fs.readFileSync(path.join(root,'docker-compose.personal-spaces.yml'),'utf8');
 const settings=overlay.split(/\r?\n/).filter(l=>l.trim().startsWith('{"app"')).map(l=>JSON.parse(l.trim()));
 assert.equal(settings.length,2);
 for(const [i,name] of ['employee-service','hr-service'].entries())assert.deepEqual(JSON.parse(model.services[name].environment.SPRING_APPLICATION_JSON),settings[i]);
 for(const name of ['employee-service','hr-service'])for(const mount of model.services[name].volumes.filter(v=>v.type==='bind')){
  assert.equal(mount.read_only,true);assert.equal(mount.bind.create_host_path,false);
 }
});
test('dependency graph has no cycle and validation points only to isolated Identity',()=>{
 const visited=new Set(),active=new Set();
 function visit(name){assert.ok(model.services[name]);assert.ok(!active.has(name));if(visited.has(name))return;active.add(name);for(const dep of Object.keys(model.services[name].depends_on??{}))visit(dep);active.delete(name);visited.add(name);}
 Object.keys(model.services).forEach(visit);
 for(const d of domains.filter(x=>x!=='identity'))assert.equal(model.services[d+'-service'].environment.IDENTITY_VALIDATION_BASE_URL,'http://identity-service:8082');
});
test('migration manifest covers all domains, V1, session identity and document sharing',()=>{
 const manifest=migrationManifest(root);assert.equal(Object.keys(manifest).length,8);
 for(const rows of Object.values(manifest)){assert.equal(rows[0].version,'1');for(const row of rows)assert.match(row.sha256,/^[a-f0-9]{64}$/);}
 assert.ok(manifest.identity.some(r=>r.file.includes('refresh_session_identity')));
 assert.ok(manifest.hr.some(r=>r.file.includes('employee_document_visibility')));
});
