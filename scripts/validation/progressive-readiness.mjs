import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { progressiveModel } from './prepare-progressive.mjs';
export async function waitReady(service,{fetchFn=globalThis.fetch,now=Date.now,sleep=ms=>new Promise(r=>setTimeout(r,ms)),timeoutMs=180000}={}){
 const deadline=now()+timeoutMs;
 while(now()<deadline){
  try{
   const registry=await fetchFn('http://127.0.0.1:18761/eureka/apps',{headers:{Accept:'application/json'},redirect:'error',signal:AbortSignal.timeout(Math.max(1,Math.min(2000,deadline-now())))});
   if(registry.status===200){
    const body=await registry.json();let ready=service==='discovery-service';
    const apps=body?.applications?.application??[];
    for(const app of Array.isArray(apps)?apps:[apps])if(app.name?.toLowerCase()===service){
     const instances=Array.isArray(app.instance)?app.instance:[app.instance];ready=instances.some(i=>i?.status==='UP');
    }
    if(ready&&service==='gateway-service'&&now()<deadline){
     const probe=await fetchFn('http://127.0.0.1:18080/identity/api/test',{redirect:'error',signal:AbortSignal.timeout(Math.max(1,Math.min(2000,deadline-now())))});
     ready=probe.status===200;await probe.body?.cancel();
    }
    if(ready&&now()<deadline)return;
   }else await registry.body?.cancel();
  }catch{/* No response bodies, tokens or diagnostics in output. */}
  await sleep(Math.min(1000,Math.max(0,deadline-now())));
 }
 throw new Error('Readiness deadline exceeded; stop this stage and diagnose without starting the next service');
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
 if(process.argv.length!==6||process.argv[2]!=='--authorized-probes')throw new Error('Future use only: --authorized-probes CAMPAIGN_DIRECTORY STAGE SERVICE');
 const directory=path.resolve(process.argv[3]),stage=process.argv[4],service=process.argv[5];
 const plan=JSON.parse(fs.readFileSync(path.join(directory,'plan.json'),'utf8'));
 const root=fileURLToPath(new URL('../../',import.meta.url));
 const expected=progressiveModel(root,plan.runId,stage);
 if(!Object.hasOwn(expected.services,service)||service==='mysql')throw new Error('Service outside stage; MySQL uses Compose healthcheck');
 const actual=JSON.parse(fs.readFileSync(path.join(directory,stage+'.compose.json'),'utf8'));
 if(JSON.stringify(actual)!==JSON.stringify(expected))throw new Error('Model drift; prepare a new campaign');
 await waitReady(service);console.log('READY: '+service+' (registration/routing only; business scenarios still required)');
}
