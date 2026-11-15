import fs from 'node:fs';
import path from 'node:path';
import {execFileSync} from 'node:child_process';
const [campaignArg,envFile,destination,authorization]=process.argv.slice(2);
if(authorization!=='--authorized-backup')throw Error('Explicit backup authorization required');
const campaign=fs.realpathSync(campaignArg),manifest=JSON.parse(fs.readFileSync(path.join(campaign,'manifest.json')));
if(!/^crm-express-\d{8}-\d{6}-(a|demo|full)$/.test(manifest.project))throw Error('Unexpected project');
if(fs.existsSync(destination))throw Error('Backup destination exists; no overwrite');
const args=['compose','-p',manifest.project,'--env-file',fs.realpathSync(envFile),'-f',path.join(campaign,'compose.json')];
const containers=execFileSync('docker',[...args,'ps','--format','json'],{encoding:'utf8'}).trim().split(/\r?\n/).filter(Boolean).map(s=>JSON.parse(s));
if(containers.some(c=>c.Service!=='mysql'&&c.State==='running'))throw Error('Stop all application containers before consistent backup; database remains running');
// Credentials expand only inside the isolated container; no password in CLI output.
const sql=execFileSync('docker',[...args,'exec','-T','mysql','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --all-databases --single-transaction --routines --events --hex-blob'],{maxBuffer:128*1024*1024,stdio:['ignore','pipe','ignore']});
fs.writeFileSync(destination,sql,{flag:'wx',mode:0o600});
console.log('SQL backup written; no restore performed. HR document volume requires its separate read-only archive.');
