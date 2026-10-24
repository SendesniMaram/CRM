// Pure Compose transformation. No processes, network, SQL execution or credentials.
export function configureMinimal(model) {
  const names = ['mysql','discovery-service','identity-service','gateway-service','department-service'];
  if (Object.keys(model.services).some(name => !names.includes(name)) || names.some(name => !model.services[name])) {
    throw new Error('Minimal model must contain only the five approved service definitions');
  }
  const mysql = model.services.mysql;
  mysql.mem_limit = '1g';
  mysql.tmpfs = ['/var/lib/mysql:size=512m'];
  mysql.healthcheck.test = ['CMD-SHELL',
    'MYSQL_PWD="$$MYSQL_ROOT_PASSWORD" mysql -h 127.0.0.1 -uroot --batch --skip-column-names -e "SELECT 1" >/dev/null 2>&1'];
  for (const name of names.slice(1)) {
    model.services[name].mem_limit = '512m';
    model.services[name].environment.JAVA_TOOL_OPTIONS = '-Xms64m -Xmx256m';
  }
  return model;
}

export function configureDemo(model) {
  const mysql = model.services.mysql;
  mysql.mem_limit = '1g';
  mysql.tmpfs = ['/var/lib/mysql:size=512m'];
  mysql.healthcheck.test = ['CMD-SHELL',
    'MYSQL_PWD="$$MYSQL_ROOT_PASSWORD" mysql -h 127.0.0.1 -uroot --batch --skip-column-names -e "SELECT 1" >/dev/null 2>&1'];
  mysql.volumes.push('./mysql/002-customer-baseline.sql:/docker-entrypoint-initdb.d/002-customer-baseline.sql:ro',
    './mysql/003-customer-association.sql:/docker-entrypoint-initdb.d/003-customer-association.sql:ro');
  for (const [name, service] of Object.entries(model.services)) {
    if (name === 'mysql') continue;
    service.mem_limit = name === 'discovery-service' || name === 'gateway-service' ? '512m' : '768m';
    service.environment.JAVA_TOOL_OPTIONS = '-Xms64m -Xmx384m';
  }
  const external = name => '${E2E_KEY_MATERIAL_DIR:?External test key directory required}/' + name;
  const employee = model.services['employee-service'];
  // JSON preserves hyphenated property names and the exact approved kid map key.
  employee.environment.SPRING_APPLICATION_JSON = JSON.stringify({app:{security:{
    'internal-tls':{enabled:true,port:8443,'key-store':'/run/crm-tls/employee.p12',
      'password-file':'/run/crm-tls/password','key-alias':'employee'},
    'hr-service-auth':{enabled:true,'trusted-keys':{demo:'/run/crm-service/hr-public.pem'}}
  }}});
  employee.volumes = [
    external('employee-tls.p12') + ':/run/crm-tls/employee.p12:ro',
    external('employee-tls-password') + ':/run/crm-tls/password:ro',
    external('hr-signing-public.pem') + ':/run/crm-service/hr-public.pem:ro'
  ];
  const hr = model.services['hr-service'];
  hr.environment.SPRING_APPLICATION_JSON = JSON.stringify({app:{security:{
    'employee-service-token':{enabled:true,'private-key':'/run/crm-service/hr-private.pem','key-id':'demo',lifetime:'30s'},
    'employee-profile-client':{enabled:true,'base-url':'https://employee-service:8443',
      'trusted-certificate':'/run/crm-tls/employee.pem'}
  }}});
  hr.volumes = [
    external('hr-signing-private.pem') + ':/run/crm-service/hr-private.pem:ro',
    external('employee-tls.pem') + ':/run/crm-tls/employee.pem:ro'
  ];
  model.services['customer-service'].environment.SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate';
  return model;
}
