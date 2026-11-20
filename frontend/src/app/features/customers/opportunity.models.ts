export type OpportunityStage = 'NEW' | 'QUALIFICATION' | 'PROPOSAL' | 'NEGOTIATION' | 'WON' | 'LOST';

export interface SalesOpportunity {
  id: number;
  customerId: number;
  employeeId: number | null;
  title: string;
  description: string | null;
  estimatedAmount: number | null;
  stage: OpportunityStage;
  expectedCloseDate: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface OpportunityRequest {
  customerId: number;
  employeeId: number | null;
  title: string;
  description: string | null;
  estimatedAmount: number | null;
  stage: OpportunityStage;
  expectedCloseDate: string | null;
}

export interface OpportunityStageHistory {
  id: number;
  opportunityId: number;
  previousStage: OpportunityStage | null;
  newStage: OpportunityStage;
  changedAt: string;
}

export interface OpportunityPage {
  content: SalesOpportunity[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
