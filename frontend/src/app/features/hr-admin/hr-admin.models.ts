export type HrModule = 'trainings' | 'attendances' | 'hr-records' | 'contracts' | 'evaluations' | 'job-offers' | 'applications';
export interface HrField {
  key: string; label: string; type: 'text'|'date'|'time'|'datetime-local'|'number'|'select'|'checkbox';
  required?: boolean; maxLength?: number; options?: readonly string[];
}
export interface HrModuleDefinition {
  title: string; singular: string; fields: readonly HrField[]; columns: readonly string[];
}
const employee: HrField = {key:'employeeId',label:'Référence Employee',type:'text',required:true,maxLength:50};
export const HR_MODULES: Record<HrModule,HrModuleDefinition> = {
  trainings: {
    title:'Formations',singular:'formation',columns:['employeeId','title','provider','startDate','endDate','cost','status','certificateObtained'],
    fields:[employee,
      {key:'title',label:'Intitulé',type:'text',required:true,maxLength:200},
      {key:'provider',label:'Organisme',type:'text',required:true,maxLength:200},
      {key:'startDate',label:'Début',type:'date',required:true},
      {key:'endDate',label:'Fin',type:'date',required:true},
      {key:'cost',label:'Coût (devise non fournie par le contrat)',type:'number',required:true},
      {key:'certificateObtained',label:'Certificat obtenu',type:'checkbox'},
      {key:'status',label:'Statut',type:'select',required:true,options:['PLANNED','IN_PROGRESS','COMPLETED','CANCELLED']}]
  },
  attendances: {
    title:'Présences',singular:'présence',columns:['employeeId','date','checkIn','checkOut','status','notes'],
    fields:[employee,{key:'date',label:'Date',type:'date',required:true},
      {key:'checkIn',label:"Heure d'arrivée",type:'time'},{key:'checkOut',label:'Heure de départ',type:'time'},
      {key:'status',label:'Statut',type:'select',options:['PRESENT','ABSENT','LATE','HALF_DAY','LEAVE']},
      {key:'notes',label:'Notes',type:'text',maxLength:500}]
  },
  'hr-records': {
    title:'Dossiers RH',singular:'dossier RH',columns:['employeeId','contractType','jobTitle','employmentStatus','hireDate'],
    fields:[employee,{key:'contractType',label:'Type de contrat',type:'text',maxLength:100},
      {key:'hireDate',label:"Date d'embauche",type:'datetime-local'},
      {key:'jobTitle',label:'Poste',type:'text',maxLength:255},
      {key:'salaryGrade',label:'Grade salarial',type:'text',maxLength:50},
      {key:'manager',label:'Responsable',type:'text',maxLength:255},
      {key:'workLocation',label:'Lieu de travail',type:'text',maxLength:255},
      {key:'employmentStatus',label:"Statut d'emploi",type:'text',maxLength:50}]
  },
  contracts: {
    title:'Contrats et avenants',singular:'contrat',columns:['employeeId','contractType','startDate','endDate','status','parentContractId'],
    fields:[employee,{key:'contractType',label:'Type de contrat',type:'text',required:true,maxLength:100},
      {key:'startDate',label:'Date de début',type:'date',required:true},{key:'endDate',label:'Date de fin',type:'date'},
      {key:'status',label:'Statut',type:'select',required:true,options:['DRAFT','ACTIVE','ENDED','CANCELLED']},
      {key:'parentContractId',label:'Contrat parent (avenant)',type:'number'},
      {key:'notes',label:'Notes',type:'text',maxLength:2000}]
  },
  evaluations: {
    title:'Évaluations',singular:'évaluation',columns:['employeeId','periodStart','periodEnd','status','criteria','comments'],
    fields:[employee,{key:'periodStart',label:'Début de période',type:'date',required:true},{key:'periodEnd',label:'Fin de période',type:'date',required:true},
      {key:'criteria',label:'Critères',type:'text',maxLength:10000},{key:'comments',label:'Appréciations et commentaires',type:'text',maxLength:10000},
      {key:'status',label:'Statut',type:'select',required:true,options:['DRAFT','FINALIZED']}]
  },
  'job-offers': {
    title:'Offres de recrutement',singular:'offre',columns:['title','status','description'],
    fields:[{key:'title',label:'Intitulé',type:'text',required:true,maxLength:200},{key:'description',label:'Description',type:'text',maxLength:10000},
      {key:'status',label:'Statut',type:'select',required:true,options:['OPEN','CLOSED']}]
  },
  applications: {
    title:'Candidatures',singular:'candidature',columns:['offerId','candidateName','candidateEmail','status','notes'],
    fields:[{key:'offerId',label:'Identifiant de l’offre',type:'number',required:true},{key:'candidateName',label:'Nom du candidat',type:'text',required:true,maxLength:200},
      {key:'candidateEmail',label:'Courriel',type:'text',required:true,maxLength:254},{key:'notes',label:'Notes',type:'text',maxLength:10000},
      {key:'status',label:'Étape',type:'select',required:true,options:['RECEIVED','IN_REVIEW','ACCEPTED','REJECTED']}]
  }
};
export interface HrRow { id:string; employeeId?:string; [key:string]:unknown }
export interface HrPage { content:HrRow[]; totalElements:number; totalPages:number; number:number }
export function hrError(error:{status?:number}):string {
  return ({
    400:'Données refusées : vérifiez les champs, dates, statuts et doublons.',
    401:'Votre session a expiré. Reconnectez-vous.',
    403:"Vous n'avez pas les permissions nécessaires.",
    404:"Cet enregistrement n'existe plus. Actualisez la liste.",
    409:'Un conflit empêche cette opération. Actualisez la liste.',
    502:'Réponse du service invalide. Réessayez plus tard.',
    503:'Service temporairement indisponible. Réessayez plus tard.'
  } as Record<number,string>)[error.status??0] ?? 'Impossible de communiquer avec le service. Réessayez.';
}
