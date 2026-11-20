import { HttpErrorResponse } from '@angular/common/http';
export interface SettlementEntry { reference:string;kind:string;amount:string;date:string;method:string|null;transactionReference:string|null;reversesReference:string|null;reason:string|null;author:string|null;createdAt:string|null }
export interface SettlementSummary { paidAmount:string;remainingAmount:string|null;state:string;entries:SettlementEntry[];correctionReason:string|null;correctedBy:string|null;correctedAt:string|null }
export interface SettlementRequest {requestId:string;amount:number;paidAt:string;method:string|null;transactionReference:string|null}
const decimal=(s:unknown)=>typeof s==='string'&&/^\d+(\.\d{1,2})?$/.test(s);
export function validateSettlements(value:SettlementSummary){
 if(!value||!decimal(value.paidAmount)||(value.remainingAmount!==null&&!decimal(value.remainingAmount))
  ||!['LEGACY_UNRECONCILED','CANCELLED','NO_AMOUNT_DUE','SETTLED','PARTIAL','UNPAID'].includes(value.state)||!Array.isArray(value.entries)
  ||value.entries.some(e=>!e||typeof e.reference!=='string'||!['PAYMENT','REVERSAL'].includes(e.kind)||!decimal(e.amount)||typeof e.date!=='string'))throw new HttpErrorResponse({status:502});
 return value;
}
export function settlementError(error:{status?:number}){return ({400:'Données de règlement invalides.',401:'Session expirée. Reconnectez-vous.',403:'Accès non autorisé.',404:'Bulletin ou règlement inaccessible.',409:'Conflit : bulletin figé, règlement déjà enregistré ou solde insuffisant. Actualisez avant de réessayer.',502:'Réponse du service invalide.',503:'Service temporairement indisponible.'} as Record<number,string>)[error.status??0]??'Opération indisponible. Actualisez et vérifiez l’historique avant de réessayer.';}
