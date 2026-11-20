import { AbstractControl } from '@angular/forms';
export function minor(value:number|null|undefined):bigint {
 const [whole,fraction='']=String(value??0).split('.');return BigInt(whole)*100n+BigInt(fraction.padEnd(2,'0'));
}
export function overtimeMinor(hours:number|null|undefined,rate:number|null|undefined){return (minor(hours)*minor(rate)+50n)/100n;}
export function payrollCalculation(form:AbstractControl){
 const keys=['baseSalary','bonuses','deductions','taxAmount','overtimeHours','overtimeHourlyRate'];
 const values=keys.map(k=>form.get(k)?.value as number|null|undefined);
 if(values.some(v=>v!=null&&(typeof v!=='number'||!Number.isFinite(v)||v<0||!/^\d+(\.\d{1,2})?$/.test(String(v)))))return null;
 const hours=form.get('overtimeHours')?.value,rate=form.get('overtimeHourlyRate')?.value;
 if((hours==null)!==(rate==null)||(hours??0)>0&&(rate??0)<=0)return {overtime:true};
 const extra=overtimeMinor(hours,rate);
 const net=minor(values[0])+minor(values[1])+extra-minor(values[2])-minor(values[3]);
 return extra<=999999999999n&&net>=0n&&net<=999999999999n?null:{net:true};
}
export function previewNet(form:AbstractControl):number|null{
 if(form.invalid)return null;
 return Number(minor(form.get('baseSalary')?.value)+minor(form.get('bonuses')?.value)+overtimeMinor(form.get('overtimeHours')?.value,form.get('overtimeHourlyRate')?.value)-minor(form.get('deductions')?.value)-minor(form.get('taxAmount')?.value))/100;
}
