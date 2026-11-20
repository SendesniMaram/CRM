import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
const campaign=fs.realpathSync(process.argv[2]);
const manifestPath=path.join(campaign,'manifest.json');
const manifest=JSON.parse(fs.readFileSync(manifestPath));
if(manifest.frontendFiles)throw Error('Already sealed; do not overwrite validated artifacts');
const files={};
function scan(dir){for(const e of fs.readdirSync(dir,{withFileTypes:true})){const p=path.join(dir,e.name);if(e.isSymbolicLink())throw Error('Bundle symlink refused');if(e.isDirectory())scan(p);else {const bytes=fs.readFileSync(p);if(e.name.endsWith('.js')&&bytes.toString().includes('http://localhost:8080'))throw Error('Development URL in bundle');files[path.relative(campaign,p)]=crypto.createHash('sha256').update(bytes).digest('hex');}}}
scan(path.join(campaign,'frontend-dist/browser'));
manifest.frontendFiles=files;
fs.writeFileSync(manifestPath,JSON.stringify(manifest,null,2));
console.log('Production frontend fingerprints sealed; runtime not validated.');
