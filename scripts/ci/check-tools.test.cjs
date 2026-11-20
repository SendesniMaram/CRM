const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { supportsAngular20, validateVersions, findChrome } = require('./check-tools.cjs');
const { xmlSafeText } = require('./junit-text.cjs');

const versions = {
  java: 'openjdk version "21.0.13"', javac: 'javac 21.0.13',
  maven: 'Apache Maven 3.9.16\nJava version: 21.0.13, vendor: Azul',
  node: '24.20.0', npm: '11.6.2',
};

test('accepts the Java 21 and Angular 20 toolchain', () => {
  assert.doesNotThrow(() => validateVersions(versions));
});
for (const version of ['20.19.0', '22.12.0', '24.0.0']) {
  test(`accepts supported Node ${version}`, () => assert.equal(supportsAngular20(version), true));
}
for (const version of ['18.20.0', '20.18.0', '22.11.0', '23.1.0', '26.0.0']) {
  test(`rejects unsupported Node ${version}`, () => assert.equal(supportsAngular20(version), false));
}
test('rejects Java 17 even when Maven is available', () => {
  assert.throws(() => validateVersions({ ...versions, java: 'openjdk version "17.0.12"' }), /Java 21/);
});
test('rejects a mismatched Maven JVM', () => {
  assert.throws(() => validateVersions({ ...versions, maven: 'Apache Maven 3.9.16\nJava version: 17.0.12,' }), /JAVA_HOME/);
});
test('rejects a JRE without a Java 21 compiler', () => {
  assert.throws(() => validateVersions({ ...versions, javac: 'javac 17.0.12' }), /javac/);
});
test('rejects a missing npm executable', () => {
  assert.throws(() => validateVersions({ ...versions, npm: '' }), /npm/);
});
test('respects an explicit protected agent browser path', () => {
  assert.equal(findChrome({ CHROME_BIN: '/opt/chrome' }, 'linux', p => p === '/opt/chrome'), '/opt/chrome');
});
test('rejects invalid CHROME_BIN instead of silently choosing a different browser', () => {
  assert.throws(() => findChrome({ CHROME_BIN: '/missing' }, 'linux', () => false), /CHROME_BIN/);
});
test('fails clearly when no browser is installed', () => {
  assert.throws(() => findChrome({}, 'linux', () => false), /Chrome\/Chromium/);
});
test('discovers the system Chromium binary', () => {
  assert.equal(findChrome({}, 'linux', p => p === '/usr/bin/chromium'), '/usr/bin/chromium');
});

test('keeps a security test with a NUL in its title representable in JUnit XML', () => {
  assert.equal(xmlSafeText('returnUrl /employees\u0000'), 'returnUrl /employees\\u0000');
});
test('escapes illegal XML controls without deleting diagnostic information', () => {
  assert.equal(xmlSafeText('\u0001\u000b\ufffe'), '\\u0001\\u000b\\ufffe');
});
test('preserves normal XML text for the reporter to escape', () => {
  assert.equal(xmlSafeText('A&B <test>\n\t'), 'A&B <test>\n\t');
});
test('Jenkins covers every service POM and installs common-security first', () => {
  const root = path.resolve(__dirname, '../..');
  const pipeline = fs.readFileSync(path.join(root, 'Jenkinsfile'), 'utf8');
  const serviceList = pipeline.slice(pipeline.indexOf('def microservices'), pipeline.indexOf('def runCommand'));
  const declared = [...serviceList.matchAll(/'([a-z-]+-service)'/g)].map(m => m[1]).sort();
  const actual = fs.readdirSync(path.join(root, 'microservices')).filter(n =>
    n.endsWith('-service') && fs.existsSync(path.join(root, 'microservices', n, 'pom.xml'))).sort();
  assert.equal(declared.length, 10);
  assert.deepEqual(declared, actual);
  assert.ok(pipeline.indexOf("stage('Build and test common-security')") < pipeline.indexOf("stage('Build and test ten microservices')"));
});
test('Jenkins stays CI-only with blocking test failures and restricted artifact paths', () => {
  const pipeline = fs.readFileSync(path.resolve(__dirname, '../../Jenkinsfile'), 'utf8');
  assert.doesNotMatch(pipeline, /docker\s|withSonarQubeEnv|waitForQualityGate|SONAR_TOKEN|sonar-maven-plugin|mvn\s+deploy|returnStatus|catchError|skipTests|test\.failure\.ignore/);
  assert.match(pipeline, /-DfailIfNoTests=true/);
  assert.match(pipeline, /tests\.failCount > 0/);
  assert.doesNotMatch(pipeline, /deleteDir|cleanWs|archiveArtifacts\s+artifacts:\s*['"]\*\*/);
});
