import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
const [campaignArg,envArg,authorization]=process.argv.slice(2);
if(authorization!=='--authorized-recette')throw Error('Explicit authorization required for fictitious API writes');
const campaign=fs.realpathSync(campaignArg),envFile=fs.realpathSync(envArg);
const manifest=JSON.parse(fs.readFileSync(path.join(campaign,'manifest.json')));
if(!/^crm-express-\d{8}-\d{6}-(a|demo|full)$/.test(manifest.project))throw Error('Unexpected campaign');
const compose=['compose','-p',manifest.project,'--env-file',envFile,'-f',path.join(campaign,'compose.json')];
const gateway=execFileSync('docker',[...compose,'ps','-q','gateway-service'],{encoding:'utf8'}).trim();
const info=JSON.parse(execFileSync('docker',['inspect',gateway],{encoding:'utf8'}))[0];
assert.equal(info.Config.Labels['com.docker.compose.project'],manifest.project);
assert.ok(info.NetworkSettings.Ports['8080/tcp'].some(p=>p.HostIp==='127.0.0.1'&&p.HostPort==='18080'));
const env=Object.fromEntries(fs.readFileSync(envFile,'utf8').split(/\r?\n/).filter(l=>l.includes('=')).map(l=>[l.slice(0,l.indexOf('=')),l.slice(l.indexOf('=')+1)]));
const credentialFile=path.join(path.dirname(envFile),'demo-accounts.json');
if(fs.existsSync(credentialFile))throw Error('Prior acceptance exists; inspect its private state before any retry');
const password=()=>crypto.randomBytes(24).toString('hex');
const accounts=[{username:'express-admin',role:'SUPER_ADMIN',initialPassword:env.CRM_2B_ADMIN_PASSWORD,password:password()},...['ADMIN','EMPLOYEE','CLIENT'].map(role=>({username:'demo-'+role.toLowerCase(),role,initialPassword:password(),password:password()}))];
fs.writeFileSync(credentialFile,JSON.stringify({state:'PREPARED_NOT_COMPLETE',accounts},null,2),{flag:'wx',mode:0o600});
const results=[];
async function call(name,method,route,body,token,expected){
 const response=await fetch('http://127.0.0.1:18080'+route,{method,headers:{...(body?{'Content-Type':'application/json'}:{}),...(token?{Authorization:'Bearer '+token}:{})},body:body?JSON.stringify(body):undefined,redirect:'error',signal:AbortSignal.timeout(15000)});
 const text=await response.text();
 results.push({name,expected,observed:response.status,pass:response.status===expected});
 assert.equal(response.status,expected,'API status mismatch: '+name);
 return {body:text?JSON.parse(text):null,headers:response.headers};
}
try {
 await call('anonymous directory denied','GET','/identity/api/users/directory',null,null,401);
 const first=await call('initial login requires change','POST','/identity/api/auth/login',{username:accounts[0].username,password:accounts[0].initialPassword},null,200);
 assert.equal(first.body.passwordChangeRequired,true);assert.ok(!first.body.token&&!first.body.refreshToken);
 for(const account of accounts){
  if(account.role!=='SUPER_ADMIN'){
   await call('create fictitious '+account.role,'POST','/identity/api/users/admin',{firstName:'Demo',lastName:account.role,username:account.username,email:account.username+'@example.test',password:account.initialPassword,roleType:account.role},accounts[0].token,201);
   const required=await call('initial '+account.role,'POST','/identity/api/auth/login',{username:account.username,password:account.initialPassword},null,200);
   assert.equal(required.body.passwordChangeRequired,true);assert.ok(!required.body.token);
  }
  await call('change initial '+account.role,'POST','/identity/api/auth/password',{login:account.username,currentPassword:account.initialPassword,newPassword:account.password},null,204);
  const login=await call('login '+account.role,'POST','/identity/api/auth/login',{username:account.username,password:account.password},null,200);
  assert.ok(login.body.token&&login.body.refreshToken);account.token=login.body.token;account.refreshToken=login.body.refreshToken;
  const display=await call('authenticated display '+account.role,'GET','/identity/api/users/display-settings',null,account.token,200);
  assert.equal(display.headers.get('cache-control'),'no-store');
  await call('directory RBAC '+account.role,'GET','/identity/api/users/directory',null,account.token,['ADMIN','SUPER_ADMIN'].includes(account.role)?200:403);
  if(account.role!=='SUPER_ADMIN')await call('settings privilege denied '+account.role,'GET','/identity/api/users/administration/settings',null,account.token,403);
 }
 const day=new Date().toISOString().slice(0,10);
 await call('SUPER audit','GET','/identity/api/users/administration/audit?from='+day+'&to='+day,null,accounts[0].token,200);
 const employee=accounts.find(a=>a.role==='EMPLOYEE');
 await call('employee logout','POST','/identity/api/auth/logout',{refreshToken:employee.refreshToken},null,204);
 await call('old access revoked','GET','/identity/api/users/display-settings',null,employee.token,401);
 await call('old refresh revoked','POST','/identity/api/auth/refresh',{refreshToken:employee.refreshToken},null,401);
 fs.writeFileSync(credentialFile,JSON.stringify({state:'CORE_API_ACCEPTANCE_PASSED',accounts:accounts.map(({token,refreshToken,...a})=>a)},null,2));
 console.log('Core API acceptance passed; credentials retained privately. Browser/business acceptance remains pending.');
} catch {
 console.log('Core API acceptance failed; partial fictitious state retained. No automatic retry or cleanup.');process.exitCode=1;
} finally {
 fs.writeFileSync(path.join(campaign,'evidence/core-api-results.json'),JSON.stringify({status:process.exitCode?'FAILED':'PASSED',project:manifest.project,commit:manifest.commit,manifestSha256:crypto.createHash('sha256').update(fs.readFileSync(path.join(campaign,'manifest.json'))).digest('hex'),cases:results,scope:'REAL_GATEWAY_IDENTITY_WHEN_EXECUTED'},null,2));
}
