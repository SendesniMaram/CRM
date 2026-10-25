import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { configureDemo, configureMinimal } from './demo-model.mjs';

function baseline() {
  const services = {mysql:{volumes:['./mysql/001-schemas.sql:/docker-entrypoint-initdb.d/001-schemas.sql:ro'],healthcheck:{}}};
  for (const name of ['discovery','gateway','identity','employee','department','customer','hr','payroll','fees','invoice']) {
    services[name+'-service']={environment:{APP_JWT_SECRET:'${E2E_JWT_SECRET:?required}'},ports:[],networks:['isolated']};
  }
  return {services,networks:{isolated:{driver:'bridge'}}};
}
test('minimal model requires only login, discovery, gateway and Department',()=>{
  const m=baseline();
  for(const name of Object.keys(m.services))if(!['mysql','identity-service','discovery-service','gateway-service','department-service'].includes(name))delete m.services[name];
  configureMinimal(m);
  assert.equal(Object.keys(m.services).length,5);
  assert.deepEqual(m.services.mysql.volumes,['./mysql/001-schemas.sql:/docker-entrypoint-initdb.d/001-schemas.sql:ro']);
  assert.equal(m.services.mysql.mem_limit,'1g');
  assert.match(m.services.mysql.healthcheck.test[1],/SELECT 1/);
  assert.ok(!JSON.stringify(m).includes('E2E_KEY_MATERIAL_DIR'));
  assert.ok(!JSON.stringify(m).includes('customer'));
  assert.ok(!JSON.stringify(m).includes('hr-signing'));
  for(const name of ['identity','discovery','gateway','department']) {
    assert.equal(m.services[name+'-service'].mem_limit,'512m');
    assert.equal(m.services[name+'-service'].environment.JAVA_TOOL_OPTIONS,'-Xms64m -Xmx256m');
  }
});
test('minimal configuration refuses accidental additional services',()=>{
  assert.throws(()=>configureMinimal(baseline()),/only the five/);
});
test('database is transient, bounded, authenticated and migration ordered',()=>{
  const m=configureDemo(baseline());
  assert.equal(m.services.mysql.mem_limit,'1g');
  assert.deepEqual(m.services.mysql.tmpfs,['/var/lib/mysql:size=512m']);
  assert.match(m.services.mysql.healthcheck.test[1],/SELECT 1/);
  assert.ok(m.services.mysql.volumes[1].includes('002-customer-baseline'));
  assert.ok(m.services.mysql.volumes[2].includes('003-customer-association'));
  assert.equal(m.services['customer-service'].environment.SPRING_JPA_HIBERNATE_DDL_AUTO,'validate');
});
test('Employee TLS uses a second unpublished port and local trusted signing key',()=>{
  const m=configureDemo(baseline()),e=m.services['employee-service'];
  const p=JSON.parse(e.environment.SPRING_APPLICATION_JSON).app.security;
  assert.equal(p['internal-tls'].port,8443);
  assert.equal(p['internal-tls']['password-file'],'/run/crm-tls/password');
  assert.deepEqual(p['hr-service-auth']['trusted-keys'],{demo:'/run/crm-service/hr-public.pem'});
  assert.deepEqual(e.ports,[]);
  assert.ok(e.volumes.every(v=>v.endsWith(':ro')));
  assert.ok(e.volumes.every(v=>!v.includes('hr-signing-private')));
});
test('HR uses HTTPS, dedicated signing key and only the approved public TLS certificate',()=>{
  const m=configureDemo(baseline()),h=m.services['hr-service'];
  const p=JSON.parse(h.environment.SPRING_APPLICATION_JSON).app.security;
  assert.equal(p['employee-profile-client']['base-url'],'https://employee-service:8443');
  assert.equal(p['employee-service-token']['key-id'],'demo');
  assert.equal(p['employee-service-token'].lifetime,'30s');
  assert.ok(h.volumes.some(v=>v.includes('hr-signing-private.pem')));
  assert.ok(h.volumes.every(v=>!v.includes('employee-tls.p12')));
  assert.ok(!h.environment.JAVA_TOOL_OPTIONS.includes('Password'));
});
test('user JWT configuration is independent and unrelated services keep their configuration',()=>{
  const original=baseline();
  const m=configureDemo(original);
  for(const [name,s] of Object.entries(m.services)){
    if(name==='mysql')continue;
    assert.equal(s.environment.APP_JWT_SECRET,'${E2E_JWT_SECRET:?required}');
    assert.ok(s.mem_limit);
    if(!['hr-service','employee-service'].includes(name))assert.equal(s.environment.SPRING_APPLICATION_JSON,undefined);
  }
});
test('migration stays additive and preflight stays read-only',()=>{
  const migration=fs.readFileSync(new URL('../../microservices/customer-service/src/main/resources/db/migration/V20261023_01__customer_user_associations.sql',import.meta.url),'utf8');
  assert.match(migration,/PRIMARY KEY \(user_id\)/);
  assert.match(migration,/UNIQUE \(customer_id\)/);
  assert.match(migration,/REFERENCES customers\(id\)/);
  assert.doesNotMatch(migration,/\b(DROP|TRUNCATE|DELETE|UPDATE|INSERT)\b/i);
  const preflight=fs.readFileSync(new URL('./customer-preflight.sql',import.meta.url),'utf8');
  assert.doesNotMatch(preflight,/\b(CREATE|ALTER|DROP|TRUNCATE|INSERT|UPDATE|DELETE)\b/i);
});
