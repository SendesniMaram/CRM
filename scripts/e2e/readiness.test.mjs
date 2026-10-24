import test from 'node:test';
import assert from 'node:assert/strict';
import { waitForServices } from './readiness.mjs';

test('readiness uses only anonymous GETs on existing routes and cancels bodies', async () => {
  const calls = []; let cancelled = 0;
  await waitForServices('http://isolated.invalid', {fetchFn: async (url, options) => {
    calls.push({url, options});
    return {status: url.endsWith('/identity/api/test') ? 200 : 401, body:{cancel:async()=>cancelled++}};
  }});
  assert.deepEqual(calls.map(c=>c.url), ['http://isolated.invalid/identity/api/test','http://isolated.invalid/department/api/departments']);
  assert.equal(cancelled, 2);
  assert.ok(calls.every(c=>c.options.method==='GET' && c.options.redirect==='error' && !c.options.headers));
});

test('a 503 retries within the budget and never invokes login or mutations', async () => {
  let time=0; const calls=[]; let attempt=0;
  await waitForServices('http://isolated.invalid', {now:()=>time,sleep:async ms=>{time+=ms;},timeoutMs:3000,
    fetchFn:async url=>{calls.push(url);return {status:++attempt===1?503:url.endsWith('/identity/api/test')?200:401};}});
  assert.equal(calls.length,3); assert.equal(time,1000);
  assert.ok(calls.every(url=>!url.includes('/auth/')&&!url.endsWith('/department/api/test')));
});

test('unexpected public access to Department never counts as healthy authentication', async () => {
  let time=0;
  await assert.rejects(waitForServices('http://isolated.invalid', {now:()=>time,sleep:async ms=>{time+=ms;},timeoutMs:1500,
    fetchFn:async()=>({status:200})}), /readiness deadline exceeded/);
  assert.equal(time,1500);
});

test('connection errors exhaust a global bounded budget without exposing diagnostics', async () => {
  let time=0;
  await assert.rejects(waitForServices('http://isolated.invalid', {now:()=>time,sleep:async ms=>{time+=ms;},timeoutMs:500,
    fetchFn:async()=>{throw new Error('private remote diagnostics');}}), error=>
      error.message==='Gateway service readiness deadline exceeded');
  assert.equal(time,500);
});
