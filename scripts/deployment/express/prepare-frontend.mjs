import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = fileURLToPath(new URL('../../../',import.meta.url));
const campaign = fs.realpathSync(process.argv[2]);
if (path.dirname(campaign) !== path.join(root,'target') || !/^crm-express-\d{8}-\d{6}-(a|demo|full)$/.test(path.basename(campaign))) throw Error('Unexpected campaign');
const output = path.join(campaign,'frontend-source');
if (fs.existsSync(output)) throw Error('Frontend source copy exists; no overwrite');
fs.cpSync(path.join(root,'frontend'), output, {recursive:true,filter:p => !fs.lstatSync(p).isSymbolicLink() && !path.relative(path.join(root,'frontend'),p).split(path.sep).some(n => ['node_modules','.angular','dist','coverage'].includes(n) || n.startsWith('.env') || /\.(log|pem|key|p12)$/i.test(n))});
fs.cpSync(path.join(root,'scripts/ci'),path.join(campaign,'scripts/ci'),{recursive:true});
let replacements = 0;
function rewrite(dir) { for (const item of fs.readdirSync(dir,{withFileTypes:true})) {const p=path.join(dir,item.name);if(item.isDirectory())rewrite(p);else if(item.name.endsWith('.ts')){const s=fs.readFileSync(p,'utf8');replacements += s.split('http://localhost:8080').length-1;fs.writeFileSync(p,s.replaceAll('http://localhost:8080','http://localhost:18080'));}} }
rewrite(path.join(output,'src'));
// Rewrite includes the interceptor origin allowlist, not just service URLs.
fs.symlinkSync(path.join(root,'frontend/node_modules'),path.join(output,'node_modules'),'junction');
console.log(JSON.stringify({source:output,replacements,status:'SOURCE_COPY_ONLY; production build still required'}));
