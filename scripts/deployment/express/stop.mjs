import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {execFileSync} from 'node:child_process';
import {modelFor} from './prepare.mjs';
export function stopPlan(root,campaign,manifest,model,envFile){
 assert.equal(path.dirname(campaign),path.join(root,'target'));
 assert.deepEqual(model,modelFor(root,manifest.run,manifest.group));
 assert.equal(manifest.project,model.name);
 assert.equal(path.basename(campaign),model.name);
 assert.ok(path.isAbsolute(envFile));
 const relativeEnv=path.relative(path.resolve(root),path.resolve(envFile));
 assert.ok(relativeEnv==='..'||relativeEnv.startsWith('..'+path.sep)||path.isAbsolute(relativeEnv),'Secrets must be outside repository');
 return ['compose','-p',model.name,'--env-file',envFile,'-f',path.join(campaign,'compose.json'),'stop','--timeout','30'];
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
 const root=fileURLToPath(new URL('../../../',import.meta.url));
 const [campaignArg,envArg,authorization]=process.argv.slice(2);
 const campaign=fs.realpathSync(campaignArg),envFile=fs.realpathSync(envArg);
 const manifest=JSON.parse(fs.readFileSync(path.join(campaign,'manifest.json'))),model=JSON.parse(fs.readFileSync(path.join(campaign,'compose.json')));
 const args=stopPlan(root,campaign,manifest,model,envFile);
 if(authorization&&authorization!=='--authorized-stop')throw Error('Unknown authorization option');
 if(!authorization){console.log('PLAN ONLY: stop containers of '+manifest.project+'; volumes/network/data preserved. No Docker command executed.');}
 else {
  const endpoint=JSON.parse(execFileSync('docker',['context','inspect','--format','{{json .Endpoints.docker.Host}}'],{encoding:'utf8'}));
  assert.match(endpoint,/^(npipe:|unix:)/,'Local Docker context required');
  const base=args.slice(0,-3);
  const ids=execFileSync('docker',[...base,'ps','-aq'],{encoding:'utf8'}).trim().split(/\r?\n/).filter(Boolean);
  for(const id of ids){const labels=JSON.parse(execFileSync('docker',['inspect','--format','{{json .Config.Labels}}',id],{encoding:'utf8'}));assert.equal(labels['com.docker.compose.project'],manifest.project);}
  execFileSync('docker',args,{stdio:['ignore','pipe','pipe']});
  const running=execFileSync('docker',['ps','--filter','label=com.docker.compose.project='+manifest.project,'-q'],{encoding:'utf8'}).trim();
  if(running)throw Error('Campaign still has running containers; no cleanup attempted');
  console.log('Campaign stopped; all volumes and files retained.');
 }
}
