import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { configureDemo, configureMinimal } from './demo-model.mjs';

// Preparation only: no Docker, npm, HTTP, SQL or credential generation is executed here.
const runId = process.argv[2];
const demo = process.argv[3] === '--demo';
const minimal = process.argv[3] === '--minimal';
if (process.argv[3] && !demo && !minimal || process.argv.length > (demo || minimal ? 4 : 3)) throw new Error('Unsupported preparation options');
if (!/^\d{8}-\d{6}$/.test(runId ?? '')) {
  throw new Error('Usage: node scripts/e2e/prepare-sprint1b.mjs YYYYMMDD-HHMMSS');
}
const root = fs.realpathSync(fileURLToPath(new URL('../../', import.meta.url)));
const target = path.join(root, 'target');
if (fs.existsSync(target) && !fs.realpathSync(target).startsWith(root + path.sep)) {
  throw new Error('Artifact directory must remain inside the workspace');
}
const project = 'crm-sprint1b-e2e-' + runId;
const output = path.join(target, project);
if (fs.existsSync(output)) throw new Error('Run directory already exists; refusing to overwrite');
fs.mkdirSync(output, { recursive: true });
const domains = ['employee', 'department', 'customer', 'hr', 'payroll', 'fees', 'invoice'];
const schemas = minimal ? ['identity','department'] : ['identity', ...domains];
fs.mkdirSync(path.join(output, 'mysql'));
fs.writeFileSync(path.join(output, 'mysql', '001-schemas.sql'), schemas.map(name =>
  `CREATE DATABASE IF NOT EXISTS ${name}_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`).join('\n') + '\n');

const frontend = path.join(output, 'frontend');
fs.cpSync(path.join(root, 'frontend'), frontend, { recursive: true, filter: source => {
  const relative = path.relative(path.join(root, 'frontend'), source);
  return !fs.lstatSync(source).isSymbolicLink() && !relative.split(path.sep).some(part =>
    ['node_modules', '.angular', 'dist', 'tmp', '.git'].includes(part) || part.endsWith('.log') || part === '.env' || part.startsWith('.env.'));
} });
function changeCopiedApiOrigin(directory) {
  for (const item of fs.readdirSync(directory, { withFileTypes: true })) {
    const filename = path.join(directory, item.name);
    if (item.isDirectory()) changeCopiedApiOrigin(filename);
    else if (item.name.endsWith('.ts')) {
      const source = fs.readFileSync(filename, 'utf8');
      const updated = source.replaceAll('http://localhost:8080', 'http://localhost:18080');
      if (updated !== source) fs.writeFileSync(filename, updated);
    }
  }
}
changeCopiedApiOrigin(path.join(frontend, 'src'));
fs.mkdirSync(path.join(output, 'gateway'));
fs.writeFileSync(path.join(output, 'gateway', 'application-e2e.yml'),
  fs.readFileSync(path.join(root, 'microservices/gateway-service/src/main/resources/application.yml'), 'utf8')
    .replaceAll('http://localhost:4200', 'http://localhost:14200'));

function application(service) {
  return { build: { context: path.join(root, 'microservices', service), dockerfile: 'Dockerfile' },
    image: project + '/' + service + ':candidate', networks: ['isolated'],
    environment: { EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: 'http://discovery-service:8761/eureka' } };
}
function databaseEnvironment(schema) {
  return {
    SPRING_DATASOURCE_URL: `jdbc:mysql://mysql:3306/${schema}_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`,
    SPRING_DATASOURCE_USERNAME: 'root', SPRING_DATASOURCE_PASSWORD: '${E2E_MYSQL_PASSWORD:?Test password required}',
    APP_JWT_SECRET: '${E2E_JWT_SECRET:?Test signing key required}',
    // This generated, standalone test model must never be merged with production Compose.
    SPRING_JPA_HIBERNATE_DDL_AUTO: 'update', SPRING_SQL_INIT_MODE: 'never'
  };
}
const services = {
  mysql: { image: 'mysql:8.0', networks: ['isolated'], tmpfs: ['/var/lib/mysql'],
    environment: { MYSQL_ROOT_PASSWORD: '${E2E_MYSQL_PASSWORD:?Test password required}', MYSQL_ROOT_HOST: '%' },
    volumes: ['./mysql/001-schemas.sql:/docker-entrypoint-initdb.d/001-schemas.sql:ro'],
    healthcheck: { test: ['CMD-SHELL', 'MYSQL_PWD="$$MYSQL_ROOT_PASSWORD" mysqladmin ping -h 127.0.0.1 -uroot --silent'],
      interval: '5s', timeout: '5s', retries: 30, start_period: '30s' } },
  'discovery-service': application('discovery-service'),
  'identity-service': application('identity-service'),
  'gateway-service': application('gateway-service')
};
Object.assign(services['identity-service'].environment, databaseEnvironment('identity'), {
  SPRING_PROFILES_ACTIVE: 'local', APP_BOOTSTRAP_ADMIN_ENABLED: 'true', APP_BOOTSTRAP_ADMIN_USERNAME: 'sprint1b-admin',
  APP_BOOTSTRAP_ADMIN_EMAIL: 'sprint1b-admin@example.test', APP_BOOTSTRAP_ADMIN_PASSWORD: '${E2E_ADMIN_PASSWORD:?Test admin password required}'
});
services['identity-service'].ports = ['127.0.0.1:18082:8082'];
services['identity-service'].depends_on = { mysql: { condition: 'service_healthy' }, 'discovery-service': { condition: 'service_started' } };
Object.assign(services['gateway-service'].environment, { SPRING_PROFILES_ACTIVE: 'e2e', SPRING_CONFIG_ADDITIONAL_LOCATION: 'file:/config/' });
services['gateway-service'].volumes = ['./gateway/application-e2e.yml:/config/application-e2e.yml:ro'];
services['gateway-service'].ports = ['127.0.0.1:18080:8080'];
services['gateway-service'].depends_on = { 'discovery-service': { condition: 'service_started' } };
for (const [index, domain] of domains.entries()) {
  if (minimal && domain !== 'department') continue;
  const service = application(domain + '-service');
  Object.assign(service.environment, databaseEnvironment(domain), { IDENTITY_VALIDATION_BASE_URL: 'http://identity-service:8082' });
  service.ports = [`127.0.0.1:${18083 + index}:${8083 + index}`];
  service.depends_on = { mysql: { condition: 'service_healthy' }, 'identity-service': { condition: 'service_started' },
    'discovery-service': { condition: 'service_started' } };
  services[domain + '-service'] = service;
}
let model = { name: project, services, networks: { isolated: { driver: 'bridge' } } };
if (demo) {
  fs.copyFileSync(path.join(root, 'scripts/e2e/customer-baseline.sql'), path.join(output, 'mysql/002-customer-baseline.sql'));
  const migration = fs.readFileSync(path.join(root,
    'microservices/customer-service/src/main/resources/db/migration/V20261023_01__customer_user_associations.sql'), 'utf8');
  fs.writeFileSync(path.join(output, 'mysql/003-customer-association.sql'), 'USE customer_db;\n' + migration);
  model = configureDemo(model);
}
if (minimal) model = configureMinimal(model);
fs.writeFileSync(path.join(output, 'compose.json'), JSON.stringify(model, null, 2) + '\n');
console.log(JSON.stringify({ project, directory: output, compose: path.join(output, 'compose.json'),
  status: 'PREPARED ONLY: obtain explicit approval before build/start/SQL/E2E' }, null, 2));
