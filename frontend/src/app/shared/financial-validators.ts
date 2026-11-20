import { AbstractControl } from '@angular/forms';

/** Same non-negative DECIMAL(12,2) limits as Invoice/Payroll API. Null is optional. */
export function money(control: AbstractControl) {
  const n = control.value;
  return n === null || (typeof n === "number" && Number.isFinite(n) && n >= 0 && n <= 9999999999.99
    && /^\d+(\.\d{1,2})?$/.test(String(n))) ? null : { money: true };
}
export function invoiceDates(form: AbstractControl) {
  const start = form.get("issueDate")?.value, end = form.get("dueDate")?.value;
  return !start || !end || end >= start ? null : { dateRange: true };
}
export function invoiceTotal(form: AbstractControl) {
  return cents(form.get("subtotal")?.value) + cents(form.get("tax")?.value) <= 999999999999 ? null : { total: true };
}
export function payrollNet(form: AbstractControl) {
  const net = cents(form.get("baseSalary")?.value) + cents(form.get("bonuses")?.value)
    - cents(form.get("deductions")?.value) - cents(form.get("taxAmount")?.value);
  return net >= 0 && net <= 999999999999 ? null : { net: true };
}

function cents(value: number | null | undefined): number { return Math.round((value ?? 0) * 100); }
