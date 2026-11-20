import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';
import { buildModel, migrationManifest } from './prepare-lot2b.mjs';

export function check(directory){
 const root=fs.realpathSync(fileURLToPath(new URL('../../',import.meta.url)));
 const output=fs.realpathSync(directory),project=path.basename(output);
 assert.equal(path.dirname(output),path.join(root,'target'),'Campaign must be directly inside workspace target');
 const actual=JSON.parse(fs.readFileSync(path.join(output,'compose.json'),'utf8'));
 assert.deepEqual(actual,buildModel(root,project),'Model drift: prepare a new campaign instead of editing artifacts');
 assert.deepEqual(JSON.parse(fs.readFileSync(path.join(output,'migrations.json'),'utf8')),migrationManifest(root),'Migration drift: prepare a new campaign');
 assert.equal(fs.readFileSync(path.join(output,'mysql/001-schemas.sql'),'utf8'),
   Object.keys(migrationManifest(root)).map(d=>`CREATE DATABASE ${d}_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`).join('\n')+'\n');
 const gateway=fs.readFileSync(path.join(root,'microservices/gateway-service/src/main/resources/application.yml'),'utf8').replaceAll('http://localhost:4200','http://localhost:14200');
 assert.equal(fs.readFileSync(path.join(output,'gateway/application-validation.yml'),'utf8'),gateway,'Gateway source drift');
 console.log('STATIC CHECK PASSED: isolated model, schema initialization, migration fingerprints and Gateway source. No runtime or certificate validation.');
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
 if(process.argv.length!==3)throw new Error('Usage: node scripts/validation/check-lot2b.mjs target/crm-lot2b-YYYYMMDD-HHMMSS');
 check(process.argv[2]);
}
