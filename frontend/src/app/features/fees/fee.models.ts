export interface FeeRequest {
  employeeId: string;
  customerId: string | null;
  serviceName: string;
  description: string | null;
  hourlyRate: number;
  workedHours: number;
  invoiceStatus: string | null;
  paymentStatus: string | null;
  workDate: string | null;
}

export interface FeeResponse extends FeeRequest {
  id: number;
  amount: number;
  createdAt: string | null;
  updatedAt: string | null;
}

export type FeeSort = 'id' | 'serviceName' | 'employeeId' | 'customerId' | 'workDate'
  | 'amount' | 'invoiceStatus' | 'paymentStatus';

export interface FeePageParams {
  page: number;
  size: number;
  sortBy: FeeSort;
  direction: 'asc' | 'desc';
  keyword?: string;
}

export interface FeePage {
  content: FeeResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
