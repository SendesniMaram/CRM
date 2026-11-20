export interface PayrollRequest {
  employeeId: string;
  baseSalary: number;
  bonuses: number | null;
  deductions: number | null;
  taxAmount: number | null;
  overtimeHours?: number|null;
  overtimeHourlyRate?: number|null;
  payPeriod: string;
  paymentDate: string | null;
  currency: string | null;
  status: string;
}

export interface PayrollResponse extends PayrollRequest {
  id: number;
  netSalary: number;
  overtimeAmount?: number|null;
  cancelled?: boolean;
  replacementPayrollId?: number|null;
  correctionOfId?: number|null;
  published: boolean;
  createdAt: string | null;
  updatedAt: string | null;
}

export type PayrollSort = 'id' | 'employeeId' | 'payPeriod' | 'paymentDate' | 'baseSalary'
  | 'netSalary' | 'currency' | 'status';

export interface PayrollPageParams {
  page: number;
  size: number;
  sortBy: PayrollSort;
  direction: 'asc' | 'desc';
  keyword?: string;
}

export interface PayrollPage {
  content: PayrollResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
