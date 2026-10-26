const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

function supportsAngular20(version) {
  const [major, minor] = version.replace(/^v/, '').split('.').map(Number);
  return (major === 20 && minor >= 19) || (major === 22 && minor >= 12) || major === 24;
}

function validateVersions({ java, javac, maven, node, npm }) {
  if (!/(?:openjdk|java) version "21(?:\.|\")/.test(java)) throw new Error('Java 21 is required on the Jenkins agent PATH.');
  if (!/^javac 21(?:\.|\s|$)/m.test(javac)) throw new Error('A Java 21 JDK (javac) is required.');
  if (!/Apache Maven 3\./.test(maven)) throw new Error('Maven 3 is required.');
  if (!/Java version: 21(?:\.|,|\s)/.test(maven)) throw new Error('Maven must run with Java 21; check JAVA_HOME.');
  if (!supportsAngular20(node)) throw new Error('Angular 20 requires Node 20.19+, 22.12+, or 24.x; prefer Node 22/24.');
  if (!/^\d+\.\d+\.\d+\s*$/.test(npm)) throw new Error('npm is required.');
}

function findChrome(env = process.env, platform = process.platform, exists = fs.existsSync) {
  if (env.CHROME_BIN) {
    if (!exists(env.CHROME_BIN)) throw new Error('CHROME_BIN does not point to an accessible browser.');
    return env.CHROME_BIN;
  }
  const candidates = platform === 'win32' ? [
    path.join(env.PROGRAMFILES || 'C:\\Program Files', 'Google/Chrome/Application/chrome.exe'),
    path.join(env['PROGRAMFILES(X86)'] || 'C:\\Program Files (x86)', 'Google/Chrome/Application/chrome.exe'),
    ...(env.LOCALAPPDATA ? [path.join(env.LOCALAPPDATA, 'Google/Chrome/Application/chrome.exe')] : []),
  ] : platform === 'darwin' ? ['/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'] : [
    '/usr/bin/google-chrome', '/usr/bin/google-chrome-stable', '/usr/bin/chromium', '/usr/bin/chromium-browser',
  ];
  const browser = candidates.find(exists);
  if (!browser) throw new Error('Chrome/Chromium is required; install it on the agent or set CHROME_BIN.');
  return browser;
}

function inspectTool(tool, args) {
  // Commands and arguments are fixed here, never taken from job parameters.
  const isBatch = process.platform === 'win32' && ['mvn', 'npm'].includes(tool);
  const command = isBatch ? 'cmd.exe' : tool;
  const commandArgs = isBatch ? ['/d', '/c', `${tool} ${args.join(' ')}`] : args;
  const result = spawnSync(command, commandArgs, { encoding: 'utf8', timeout: 30000, windowsHide: true });
  if (result.error || result.status !== 0) throw new Error(`${tool} is unavailable on this agent PATH.`);
  return `${result.stdout || ''}${result.stderr || ''}`.replace(/\u001b\[[0-9;]*m/g, '').trim();
}

module.exports = { supportsAngular20, validateVersions, findChrome };

if (require.main === module) {
  try {
    const versions = {
      java: inspectTool('java', ['-version']), javac: inspectTool('javac', ['-version']),
      maven: inspectTool('mvn', ['-v']), node: process.versions.node, npm: inspectTool('npm', ['--version']),
    };
    validateVersions(versions);
    findChrome();
    console.log(`Tools verified: Java/JDK 21, Maven 3, Node ${versions.node}, npm ${versions.npm}; browser available.`);
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
