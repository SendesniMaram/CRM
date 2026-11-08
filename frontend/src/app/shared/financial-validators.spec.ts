import { FormControl, FormGroup } from '@angular/forms';
import { money, invoiceDates, invoiceTotal, payrollNet } from './financial-validators';

describe('Financial validators shared with invoice and payroll forms', () => {
  it('accepts optional zero and DECIMAL(12,2) limits without floating point rejection', () => {
    for (const value of [null, 0, 0.01, 0.29, 123456789.99, 9999999999.99])
      expect(money(new FormControl(value))).toBeNull();
  });
  it('rejects negative, missing precision, exponent-scale and oversized values', () => {
    for (const value of [-0.01, 0.001, 0.000001, 10000000000, Infinity, NaN, '10.00'])
      expect(money(new FormControl(value))).not.toBeNull();
  });
  it('compares dates and bounds the calculated invoice total', () => {
    const form = new FormGroup({ issueDate: new FormControl('2026-11-02'), dueDate: new FormControl('2026-11-01'),
      subtotal: new FormControl(9999999999.99), tax: new FormControl(0.01) });
    expect(invoiceDates(form)).toEqual({ dateRange: true }); expect(invoiceTotal(form)).toEqual({ total: true });
    form.patchValue({ issueDate: '2026-11-01', tax: 0 });
    expect(invoiceDates(form)).toBeNull(); expect(invoiceTotal(form)).toBeNull();
  });
  it('calculates a zero net using exact minor units and rejects negative net', () => {
    const form = new FormGroup({ baseSalary: new FormControl(0.3), bonuses: new FormControl(0),
      deductions: new FormControl(0.1), taxAmount: new FormControl(0.2) });
    expect(payrollNet(form)).toBeNull(); form.patchValue({ taxAmount: 0.21 });
    expect(payrollNet(form)).toEqual({ net: true });
  });
});
