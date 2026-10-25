export interface CustomerRequest {
  name: string;
  email: string | null;
  phone: string | null;
  company: string | null;
  address: string | null;
}

export interface CustomerResponse extends CustomerRequest {
  id: number;
  createdAt: string | null;
  updatedAt: string | null;
}

export type CustomerSort = 'id' | 'name' | 'email' | 'phone' | 'company';

export interface CustomerPageParams {
  page: number;
  size: number;
  sortBy: CustomerSort;
  direction: 'asc' | 'desc';
  keyword?: string;
}

export interface CustomerPage {
  content: CustomerResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
