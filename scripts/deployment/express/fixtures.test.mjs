import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {materialize} from './materialize-fixtures.mjs';
const fixtures=JSON.parse(fs.readFileSync(new URL('./demo-fixtures.json',import.meta.url)));
test('fixtures have unique references, reserved emails and all four roles',()=>{assert.equal(new Set([...fixtures.accounts,...fixtures.controlAccounts].map(a=>a.username)).size,6);assert.deepEqual(new Set(fixtures.accounts.map(a=>a.role)),new Set(['SUPER_ADMIN','ADMIN','EMPLOYEE','CLIENT']));for(const a of [...fixtures.accounts,...fixtures.controlAccounts]){assert.match(a.email,/@example\.test$/);assert.ok(!Object.hasOwn(a,'password'));}const refs=new Set(fixtures.departments.map(d=>d.ref));for(const e of fixtures.employees)assert.ok(refs.has(e.departmentRef));for(const c of fixtures.customers)assert.ok(fixtures.employees.some(e=>e.ref===c.assignedEmployeeRef));});
test('future leave and invoice dates derive from rehearsal date without mutating template',()=>{const out=materialize(fixtures,'2026-12-29');assert.equal(out.leaves[0].payload.startDate,'2027-01-05');assert.equal(out.invoices[0].payload.dueDate,'2027-01-28');assert.equal(out.imported,false);assert.equal(out.invoices[0].payload.subtotal,61.7);assert.equal(fixtures.leaves[0].payload,undefined);});
