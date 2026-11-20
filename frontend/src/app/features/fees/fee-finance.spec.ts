import { FormControl,FormGroup } from '@angular/forms';import { feeProduct,feeEditable } from './fee-finance';import { FeeResponse } from './fee.models';
describe('Exact fee limits',()=>{const form=(rate:number,hours:number)=>new FormGroup({hourlyRate:new FormControl(rate),workedHours:new FormControl(hours)});
it('rounds half cents and rejects a zero rounded amount',()=>{expect(feeProduct(form(1.5,.01))).toBeNull();expect(feeProduct(form(.01,.01))).toEqual({amount:true});});
it('accepts the DECIMAL boundary and rejects an overflowing product without binary rounding',()=>{expect(feeProduct(form(9999999999.99,1))).toBeNull();expect(feeProduct(form(9999999999.99,1.01))).toEqual({amount:true});});
it('freezes generated fees and conservatively protects engaged or unknown statuses',()=>{const row={sourceTimeId:null,invoiceStatus:null,paymentStatus:null} as FeeResponse;expect(feeEditable(row)).toBeTrue();expect(feeEditable({...row,sourceTimeId:9})).toBeFalse();for(const value of ['PAID','INVOICED','Custom'])expect(feeEditable({...row,paymentStatus:value})).toBeFalse();expect(feeEditable({...row,invoiceStatus:'À facturer'})).toBeTrue();});
});
