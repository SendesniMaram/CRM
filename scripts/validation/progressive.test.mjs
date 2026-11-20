import test from 'node:test';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';
import { progressiveModel,stageBudget,stages } from './prepare-progressive.mjs';
import { buildModel } from './prepare-lot2b.mjs';
import { waitReady } from './progressive-readiness.mjs';
const root=fileURLToPath(new URL('../../',import.meta.url)),id='20261102-100000';
test('all stages retain existing JWT, Identity, Flyway and heap settings',()=>{
 const full=buildModel(root,'crm-lot2b-'+id);
 for(const s of Object.keys(stages))for(const [name,service] of Object.entries(progressiveModel(root,id,s).services)){
  assert.deepEqual(service.environment,full.services[name].environment);assert.equal(service.mem_limit,full.services[name].mem_limit);
 }
});
test('budgets reflect the exact limited simultaneous service sets',()=>{
 const expected={A:2816,B:5120,C:4352,D:5120,'E-bootstrap':5120,'E-payroll':4352,'E-fees':3584};
 for(const [s,total] of Object.entries(expected)){const b=stageBudget(progressiveModel(root,id,s));assert.equal(b.limitMiB,total);assert.equal(b.hostFreeRequiredMiB,total+1536);}
});
test('all stage dependency edges stay inside the stage',()=>{
 for(const s of Object.keys(stages)){const model=progressiveModel(root,id,s);for(const service of Object.values(model.services))for(const dep of Object.keys(service.depends_on??{}))assert.ok(model.services[dep]);}
});
test('groups are isolated, except deliberate E bootstrap/payroll continuity',()=>{
 const projects=Object.fromEntries(Object.keys(stages).map(s=>[s,progressiveModel(root,id,s).name]));
 assert.equal(projects['E-bootstrap'],projects['E-payroll']);assert.equal(new Set(Object.values(projects)).size,6);
 for(const s of Object.keys(stages))for(const declaration of Object.values(progressiveModel(root,id,s).volumes)){assert.equal(declaration.external,undefined);assert.equal(declaration.name,undefined);}
});
test('profile HTTPS remains enabled and Fees is independent of Employee data services',()=>{
 for(const s of ['B','D','E-bootstrap','E-payroll']){const e=progressiveModel(root,id,s).services['hr-service'].environment;assert.equal(JSON.parse(e.SPRING_APPLICATION_JSON).app.security['employee-profile-client'].enabled,true);}
 assert.deepEqual(Object.keys(progressiveModel(root,id,'E-fees').services),['mysql','discovery-service','identity-service','gateway-service','fees-service']);
});
test('invalid stage and project identifiers are refused',()=>{assert.throws(()=>progressiveModel(root,id,'all'));assert.throws(()=>progressiveModel(root,'../dev','A'));});
test('registration probe uses only injected fake HTTP during this unit test',async()=>{
 let calls=0;await waitReady('identity-service',{fetchFn:async()=>{calls++;return {status:200,json:async()=>({applications:{application:[{name:'IDENTITY-SERVICE',instance:[{status:'UP'}]}]}})};}});assert.equal(calls,1);
});
test('gateway registration alone is not enough if its route is unavailable',async()=>{
 let time=0;await assert.rejects(waitReady('gateway-service',{now:()=>time,sleep:async n=>{time+=n;},timeoutMs:3,fetchFn:async url=>url.includes('18761')?{status:200,json:async()=>({applications:{application:[{name:'GATEWAY-SERVICE',instance:{status:'UP'}}]}})}:{status:503}}),/deadline/);
});
test('missing registration terminates after bounded fake time without network',async()=>{
 let time=0;await assert.rejects(waitReady('hr-service',{now:()=>time,sleep:async n=>{time+=n;},timeoutMs:3,fetchFn:async()=>({status:200,json:async()=>({applications:{application:[]}})})}),/deadline/);
});
