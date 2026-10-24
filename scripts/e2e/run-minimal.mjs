import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { waitForServices } from './readiness.mjs';

// This runner mutates ONLY a demonstrably isolated Department service.
// Preparation and --help never read credentials, call HTTP or start Docker.
if (process.argv[2] === '--help') {
  console.log('After explicit approval: node scripts/e2e/run-minimal.mjs --authorized PROJECT EXTERNAL_ENV_FILE');
  process.exit(0);
}
if (process.argv[2] !== '--authorized' || process.argv.length !== 5) {
  throw new Error('Explicit execution approval, project and external credential file required');
}
const project = process.argv[3];
if (!/^crm-sprint1b-e2e-\d{8}-\d{6}$/.test(project)) throw new Error('Invalid isolated project');
const root = fs.realpathSync(fileURLToPath(new URL('../../', import.meta.url)));
const credentialPath = fs.realpathSync(process.argv[4]);
if (credentialPath.toLowerCase() === root.toLowerCase()
    || credentialPath.toLowerCase().startsWith((root + path.sep).toLowerCase())) {
  throw new Error('Credentials must stay outside the repository');
}
const model = JSON.parse(fs.readFileSync(path.join(root, 'target', project, 'compose.json'), 'utf8'));
const expected = ['mysql','discovery-service','identity-service','gateway-service','department-service'].sort();
if (model.name !== project || JSON.stringify(Object.keys(model.services).sort()) !== JSON.stringify(expected)
    || !model.services['gateway-service'].ports.includes('127.0.0.1:18080:8080')
    || model.services.mysql.ports || model.volumes
    || !model.services.mysql.tmpfs?.some(v => v.startsWith('/var/lib/mysql:'))) {
  throw new Error('Unexpected demo model; refusing HTTP mutations');
}
const containers = spawnSync('docker', ['ps', '--filter', 'label=com.docker.compose.project=' + project,
  '--format', '{{.Label "com.docker.compose.service"}}|{{.Ports}}'], {encoding:'utf8',windowsHide:true,timeout:10000});
if (containers.status !== 0) throw new Error('Cannot verify isolated running project');
const lines = containers.stdout.trim().split(/\r?\n/).filter(Boolean);
if (lines.length !== 5 || expected.some(service => !lines.some(line => line.startsWith(service+'|')))
    || !lines.some(line => line.startsWith('gateway-service|') && line.includes('127.0.0.1:18080->8080/tcp'))) {
  throw new Error('Expected isolated services or Gateway binding not found');
}
const secrets = Object.fromEntries(fs.readFileSync(credentialPath,'utf8').split(/\r?\n/)
  .filter(line => /^[A-Z_]+=/.test(line)).map(line => {const i=line.indexOf('=');return [line.slice(0,i),line.slice(i+1)];}));
if (!secrets.E2E_ADMIN_PASSWORD) throw new Error('Demo credentials incomplete');
const base = 'http://127.0.0.1:18080';
const results = [];
async function request(method, route, expectedStatus, body, token) {
  const response = await fetch(base+route,{method,redirect:'error',signal:AbortSignal.timeout(15000),
    headers:{...(body?{'Content-Type':'application/json'}:{}),...(token?{Authorization:'Bearer '+token}:{})},
    ...(body?{body:JSON.stringify(body)}:{})});
  if (response.status !== expectedStatus) throw new Error(method+' '+route.split('?')[0]+' unexpected status '+response.status);
  results.push({method,route:route.split('?')[0],status:response.status});
  return expectedStatus === 204 ? null : response.json();
}
try {
  // Bounded, read-only readiness checks also prove Gateway discovery routing.
  await waitForServices(base);
  await request('GET','/department/api/departments',401);
  const login = await request('POST','/identity/api/auth/login',200,
    {username:'sprint1b-admin',password:secrets.E2E_ADMIN_PASSWORD});
  if (typeof login.token !== 'string' || !login.roles?.includes('SUPER_ADMIN')) throw new Error('Unexpected bootstrap login contract');
  const unique = randomUUID();
  const fixture = {name:'E2E-'+unique,code:'E2E-'+unique.slice(0,12),description:'Isolated API demonstration'};
  const department = await request('POST','/department/api/departments',201,fixture,login.token);
  if (!Number.isSafeInteger(department.id) || department.id <= 0) throw new Error('Invalid Department identifier');
  const route = '/department/api/departments/'+department.id;
  const detail = await request('GET',route,200,null,login.token);
  if (detail.name !== fixture.name) throw new Error('Department read mismatch');
  const page = await request('GET','/department/api/departments?keyword='+encodeURIComponent(fixture.name)+'&sortBy=id',200,null,login.token);
  if (!Array.isArray(page.content) || !page.content.some(row => row.id === department.id)) throw new Error('Department search mismatch');
  const updated = await request('PUT',route,200,{...fixture,description:'Updated isolated fixture'},login.token);
  if (updated.description !== 'Updated isolated fixture') throw new Error('Department update mismatch');
  await request('DELETE',route,204,null,login.token);
  await request('GET',route,404,null,login.token);
  console.log(JSON.stringify({project,status:'PASS_API_ONLY',checks:results,scope:'Real HTTP API; Angular browser not validated'},null,2));
} catch (error) {
  // Do not print remote bodies, tokens, credentials or environment diagnostics.
  console.error('E2E did not pass; '+(error instanceof TypeError || error?.name === 'TimeoutError'
    ? 'communication failed or timed out' : error.message));
  console.error('Partial synthetic data may remain in this isolated project. No automatic teardown performed.');
  process.exitCode = 1;
}
