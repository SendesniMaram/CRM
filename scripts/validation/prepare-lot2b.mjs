import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

export const domains = ['identity','employee','department','customer','hr','payroll','fees','invoice'];
export function buildModel(root, project) {
  if (!/^crm-lot2b-\d{8}-\d{6}$/.test(project)) throw new Error('Invalid isolated project name');
  const common = name => ({
    image: `${project}/${name}:candidate`, build: {context:path.join(root,'microservices',name),dockerfile:'Dockerfile'},
    restart:'no', networks:['isolated'], mem_limit:'768m', pids_limit:256,
    environment:{EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:'http://discovery-service:8761/eureka/',
      JAVA_TOOL_OPTIONS:'-Xms64m -Xmx384m -XX:MaxMetaspaceSize=192m',
      SPRING_JPA_SHOW_SQL:'false', LOGGING_LEVEL_ORG_HIBERNATE_SQL:'WARN',
      LOGGING_LEVEL_ORG_HIBERNATE_ORM_JDBC_BIND:'OFF'},
    depends_on:{'discovery-service':{condition:'service_started'}}
  });
  const services={mysql:{image:'mysql:8.0',restart:'no',networks:['isolated'],mem_limit:'1024m',pids_limit:256,
    command:['--innodb-buffer-pool-size=256M','--max-connections=100'],
    environment:{MYSQL_ROOT_PASSWORD:'${CRM_2B_MYSQL_PASSWORD:?Dedicated validation password required}',MYSQL_ROOT_HOST:'%'},
    volumes:[{type:'volume',source:'mysql-data',target:'/var/lib/mysql'},
      {type:'bind',source:'./mysql',target:'/docker-entrypoint-initdb.d',read_only:true,bind:{create_host_path:false}}],
    healthcheck:{test:['CMD-SHELL','MYSQL_PWD="$$MYSQL_ROOT_PASSWORD" mysql -h 127.0.0.1 -uroot --batch --skip-column-names -e "SELECT 1" >/dev/null 2>&1'],interval:'5s',timeout:'5s',retries:30,start_period:'30s'}}};
  services['discovery-service']=common('discovery-service');delete services['discovery-service'].depends_on;
  services['discovery-service'].ports=['127.0.0.1:18761:8761'];
  services['gateway-service']=common('gateway-service');
  for(const name of ['discovery-service','gateway-service']){
    services[name].mem_limit='512m';services[name].environment.JAVA_TOOL_OPTIONS='-Xms64m -Xmx256m -XX:MaxMetaspaceSize=128m';
  }
  services['gateway-service'].ports=['127.0.0.1:18080:8080'];
  services['gateway-service'].environment.SPRING_CONFIG_ADDITIONAL_LOCATION='file:/config/application-validation.yml';
  services['gateway-service'].volumes=[{type:'bind',source:'./gateway/application-validation.yml',target:'/config/application-validation.yml',read_only:true,bind:{create_host_path:false}}];
  for(const domain of domains){
    const service=common(domain+'-service');
    Object.assign(service.environment,{
      SPRING_DATASOURCE_URL:`jdbc:mysql://mysql:3306/${domain}_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`,
      SPRING_DATASOURCE_USERNAME:'root',SPRING_DATASOURCE_PASSWORD:'${CRM_2B_MYSQL_PASSWORD:?Dedicated validation password required}',
      APP_JWT_SECRET:'${CRM_2B_JWT_SECRET:?Dedicated validation JWT secret required}',
      SPRING_JPA_HIBERNATE_DDL_AUTO:'validate',SPRING_SQL_INIT_MODE:'never',
      SPRING_FLYWAY_ENABLED:'true',SPRING_FLYWAY_BASELINE_ON_MIGRATE:'false',
      SPRING_FLYWAY_CLEAN_DISABLED:'true',SPRING_FLYWAY_VALIDATE_ON_MIGRATE:'true',
      SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:'5',SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE:'1'
    });
    service.depends_on.mysql={condition:'service_healthy'};
    if(domain!=='identity'){
      service.environment.IDENTITY_VALIDATION_BASE_URL='http://identity-service:8082';
      service.depends_on['identity-service']={condition:'service_started'};
    }
    services[domain+'-service']=service;
  }
  Object.assign(services['identity-service'].environment,{
    SPRING_PROFILES_ACTIVE:'local',APP_BOOTSTRAP_ADMIN_ENABLED:'true',
    APP_BOOTSTRAP_ADMIN_USERNAME:'lot2b-admin',APP_BOOTSTRAP_ADMIN_EMAIL:'lot2b-admin@example.test',
    APP_BOOTSTRAP_ADMIN_PASSWORD:'${CRM_2B_ADMIN_PASSWORD:?Dedicated validation admin password required}'
  });
  // Exact security properties already used by the Lot 1C overlay. No TLS bypass.
  services['employee-service'].environment.SPRING_APPLICATION_JSON=JSON.stringify({app:{security:{
    'internal-tls':{enabled:true,port:8443,'key-store':'/run/crm-profile/employee.p12','password-file':'/run/crm-profile/password','key-alias':'employee'},
    'hr-service-auth':{enabled:true,'trusted-keys':{profile:'/run/crm-profile/hr-public.pem'}}
  }}});
  services['hr-service'].environment.SPRING_APPLICATION_JSON=JSON.stringify({app:{security:{
    'employee-service-token':{enabled:true,'private-key':'/run/crm-profile/hr-private.pem','key-id':'profile',lifetime:'30s'},
    'employee-profile-client':{enabled:true,'base-url':'https://employee-service:8443','trusted-certificate':'/run/crm-profile/employee.pem'}
  }}});
  const key=(source,target)=>({type:'bind',source:'${CRM_PROFILE_MATERIAL_DIR:?External validation key directory required}/'+source,
    target:'/run/crm-profile/'+target,read_only:true,bind:{create_host_path:false}});
  services['employee-service'].volumes=[key('employee-tls.p12','employee.p12'),key('employee-tls-password','password'),key('hr-signing-public.pem','hr-public.pem')];
  services['hr-service'].volumes=[key('hr-signing-private.pem','hr-private.pem'),key('employee-tls.pem','employee.pem'),
    {type:'volume',source:'hr-documents',target:'/var/lib/crm/hr-documents'}];
  services['hr-service'].environment.APP_HR_DOCUMENTS_STORAGE_ROOT='/var/lib/crm/hr-documents';
  return {name:project,services,volumes:{'mysql-data':{},'hr-documents':{}},networks:{isolated:{driver:'bridge'}}};
}

export function migrationManifest(root){
  return Object.fromEntries(domains.map(domain=>{
    const folder=path.join(root,'microservices',domain+'-service/src/main/resources/db/migration');
    const seen=new Set();
    const migrations=fs.readdirSync(folder).filter(f=>f.endsWith('.sql')).map(file=>{
      const match=/^V(\d+(?:_\d+)*)__.+\.sql$/.exec(file);
      if(!match||seen.has(match[1]))throw new Error('Invalid or duplicate migration version: '+domain);
      seen.add(match[1]);const bytes=fs.readFileSync(path.join(folder,file));
      return {file,version:match[1],sha256:crypto.createHash('sha256').update(bytes).digest('hex')};
    }).sort((a,b)=>{const x=a.version.split('_').map(BigInt),y=b.version.split('_').map(BigInt);for(let i=0;i<Math.max(x.length,y.length);i++){if((x[i]??0n)!==(y[i]??0n))return (x[i]??0n)<(y[i]??0n)?-1:1;}return 0;});
    if(migrations[0]?.version!=='1')throw new Error('Missing V1 migration: '+domain);
    return [domain,migrations];
  }));
}

export function prepare(runId){
  if(!/^\d{8}-\d{6}$/.test(runId??''))throw new Error('Usage: node scripts/validation/prepare-lot2b.mjs YYYYMMDD-HHMMSS');
  const root=fs.realpathSync(fileURLToPath(new URL('../../',import.meta.url))),project='crm-lot2b-'+runId;
  const target=path.join(root,'target');
  if(fs.existsSync(target)&&fs.realpathSync(target)!==target)throw new Error('Artifact root must not be a symlink');
  const output=path.join(target,project);
  if(fs.existsSync(output))throw new Error('Campaign already exists; refusing overwrite');
  const model=buildModel(root,project),migrations=migrationManifest(root);
  fs.mkdirSync(path.join(output,'mysql'),{recursive:true});fs.mkdirSync(path.join(output,'gateway'));
  fs.writeFileSync(path.join(output,'mysql/001-schemas.sql'),domains.map(d=>`CREATE DATABASE ${d}_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`).join('\n')+'\n');
  fs.writeFileSync(path.join(output,'gateway/application-validation.yml'),fs.readFileSync(path.join(root,'microservices/gateway-service/src/main/resources/application.yml'),'utf8').replaceAll('http://localhost:4200','http://localhost:14200'));
  fs.writeFileSync(path.join(output,'compose.json'),JSON.stringify(model,null,2)+'\n');
  fs.writeFileSync(path.join(output,'migrations.json'),JSON.stringify(migrations,null,2)+'\n');
  // Copy sources only. Never inspect .env, private key material, dependencies or runtime output.
  const frontend=path.join(output,'frontend'),source=path.join(root,'frontend');
  fs.cpSync(source,frontend,{recursive:true,filter:p=>!fs.lstatSync(p).isSymbolicLink()&&!path.relative(source,p).split(path.sep).some(x=>
    ['node_modules','.angular','dist','target','tmp','.git','test-results','coverage','private'].includes(x)||x.startsWith('.env')||/\.(log|pem|p12|key)$/i.test(x))});
  function rewrite(directory){for(const item of fs.readdirSync(directory,{withFileTypes:true})){const p=path.join(directory,item.name);if(item.isDirectory())rewrite(p);else if(item.name.endsWith('.ts')){const text=fs.readFileSync(p,'utf8');fs.writeFileSync(p,text.replaceAll('http://localhost:8080','http://localhost:18080'));}}}
  rewrite(path.join(frontend,'src'));
  console.log(JSON.stringify({project,directory:output,status:'PREPARED ONLY; no Docker, database, Flyway, HTTP or E2E executed'},null,2));
  return output;
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  if(process.argv.length!==3)throw new Error('Exactly one campaign identifier required');
  prepare(process.argv[2]);
}
