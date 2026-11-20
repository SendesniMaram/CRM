import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
const root=fileURLToPath(new URL('../../../',import.meta.url));
export function materialize(fixtures,day){
 assert.equal(fixtures.classification,'FICTIONAL_ONLY_NOT_IMPORTED');
 const offset=n=>{const date=new Date(day+'T00:00:00Z');assert.ok(Number.isFinite(date.getTime()));date.setUTCDate(date.getUTCDate()+n);return date.toISOString().slice(0,10);};
 const result=structuredClone(fixtures);
 for(const row of result.leaves){row.payload={startDate:offset(row.startOffsetDays),endDate:offset(row.endOffsetDays),reason:row.reason};}
 for(const row of result.invoices){row.payload.issueDate=offset(row.issueOffsetDays);row.payload.dueDate=offset(row.dueOffsetDays);}
 for(const account of [...result.accounts,...result.controlAccounts]){assert.ok(account.email.endsWith('@example.test'));assert.ok(!Object.hasOwn(account,'password'));}
 return {...result,preparedForDate:day,imported:false};
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
 const campaign=fs.realpathSync(process.argv[2]);
 assert.equal(path.dirname(campaign),path.join(root,'target'));
 assert.match(path.basename(campaign),/^crm-express-\d{8}-\d{6}-(a|demo|full)$/);
 const fixtures=JSON.parse(fs.readFileSync(new URL('./demo-fixtures.json',import.meta.url)));
 const result=materialize(fixtures,new Date().toISOString().slice(0,10));
 fs.writeFileSync(path.join(campaign,'demo-data.json'),JSON.stringify(result,null,2),{flag:'wx'});
 fs.writeFileSync(path.join(campaign,result.document.filename),result.document.content+'\n',{flag:'wx'});
 console.log('Fictitious dataset prepared only; zero HTTP/SQL calls, zero accounts created.');
}
