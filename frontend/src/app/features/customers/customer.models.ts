export interface CustomerRequest {
  name: string;
  email: string | null;
  phone: string | null;
  company: string | null;
  address: string | null;
  contactType?: 'LEAD' | 'CUSTOMER' | 'SUPPLIER' | null;
  category?: string | null;
  segment?: string | null;
  assignedEmployeeId?: number | null;
}

export interface CustomerResponse extends CustomerRequest {
  id: number;
  createdAt: string | null;
  updatedAt: string | null;
}

export type CustomerSort = 'id' | 'name' | 'email' | 'phone' | 'company' | 'contactType' | 'category' | 'segment';

export interface CustomerPageParams {
  page: number;
  size: number;
  sortBy: CustomerSort;
  direction: 'asc' | 'desc';
  keyword?: string;
  contactType?: string;
  category?: string;
  segment?: string;
}

export interface CustomerPage {
  content: CustomerResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CustomerCsvImportReport {
  totalRows: number;
  acceptedRows: number;
  rejectedRows: number;
  issues: { row: number; message: string }[];
}

export type InteractionType = 'CALL' | 'EMAIL' | 'MEETING' | 'NOTE';

export interface CustomerInteraction {
  id: number;
  customerId: number;
  employeeId: number | null;
  interactionType: InteractionType;
  occurredAt: string;
  notes: string | null;
  createdAt: string | null;
}

export interface CustomerInteractionRequest {
  interactionType: InteractionType;
  occurredAt: string;
  employeeId?: number | null;
  notes?: string | null;
}
