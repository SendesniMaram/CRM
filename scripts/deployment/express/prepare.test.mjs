import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {modelFor} from './prepare.mjs';
const root=fileURLToPath(new URL('../../../',import.meta.url));
for(const group of ['A','demo','full'])test('isolated topology and protections: '+group,()=>{
 const m=modelFor(root,'20261111-120000',group);
 for(const[n,s]of Object.entries(m.services)){
  for(const p of s.ports??[])assert.ok(p.startsWith('127.0.0.1:'));
  for(const dep of Object.keys(s.depends_on??{}))assert.ok(m.services[dep]);
  assert.equal(s.restart,'no');assert.ok(s.healthcheck);
  if(n==='mysql')assert.equal(s.ports,undefined);
  if(s.environment?.SPRING_DATASOURCE_URL){assert.equal(s.environment.SPRING_FLYWAY_BASELINE_ON_MIGRATE,'false');assert.equal(s.environment.SPRING_FLYWAY_CLEAN_DISABLED,'true');assert.equal(s.environment.SPRING_JPA_HIBERNATE_DDL_AUTO,'validate');}
 }
 for(const v of Object.values(m.volumes))assert.ok(!v.external&&!v.name);
 if(group!=='A'){const security=JSON.parse(m.services['hr-service'].environment.SPRING_APPLICATION_JSON).app.security;assert.equal(security['employee-profile-client']['base-url'],'https://employee-service:8443');}
 assert.ok(m.services.frontend.volumes.every(v=>v.read_only&&v.bind.create_host_path===false));
});
test('refuse invalid group or unsafe campaign identifiers',()=>{assert.throws(()=>modelFor(root,'../../data','A'));assert.throws(()=>modelFor(root,'20261111-120000','unknown'));});
