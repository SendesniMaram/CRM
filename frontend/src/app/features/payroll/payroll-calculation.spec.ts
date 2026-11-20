import { FormControl,FormGroup } from '@angular/forms';
import { overtimeMinor,payrollCalculation,previewNet } from './payroll-calculation';
describe('Payroll variable calculation',()=>{
 function form(values:Record<string,number|null>={}){return new FormGroup(Object.fromEntries(Object.entries({baseSalary:100,bonuses:0,deductions:0,taxAmount:0,overtimeHours:null,overtimeHourlyRate:null,...values}).map(([k,v])=>[k,new FormControl(v)])),{validators:payrollCalculation});}
 it('preserves historical arithmetic without overtime',()=>expect(previewNet(form({bonuses:10,deductions:5,taxAmount:2}))).toBe(103));
 it('calculates an explicit hourly rate without a legal multiplier',()=>expect(previewNet(form({overtimeHours:2.5,overtimeHourlyRate:12.34}))).toBe(130.85));
 it('rounds half cents up using integer arithmetic',()=>{expect(overtimeMinor(.01,.5)).toBe(1n);expect(overtimeMinor(.01,.49)).toBe(0n);});
 it('requires hours and rate together and a positive rate for positive hours',()=>{expect(form({overtimeHours:1}).hasError('overtime')).toBeTrue();expect(form({overtimeHours:1,overtimeHourlyRate:0}).hasError('overtime')).toBeTrue();});
 it('accepts exact cancellation without a binary rounding error',()=>expect(previewNet(form({baseSalary:.1,bonuses:.2,deductions:.3}))).toBe(0));
 it('rejects negative or overflowing net and overflowing overtime',()=>{expect(form({deductions:101}).invalid).toBeTrue();expect(form({baseSalary:9999999999.99,bonuses:1}).invalid).toBeTrue();expect(form({overtimeHours:999999.99,overtimeHourlyRate:9999999999.99}).invalid).toBeTrue();});
});