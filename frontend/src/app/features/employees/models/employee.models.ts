export interface EmployeeRequest {
  employeeCode: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  jobTitle?: string;
  hireDate?: string;
  salary: number;
  address?: string;
  dateOfBirth?: string;
  gender?: string;
  status?: string;
}

export interface EmployeeResponse {
  id: number;
  employeeCode: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  jobTitle?: string;
  hireDate?: string;
  salary: number;
  address?: string;
  dateOfBirth?: string;
  gender?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface DepartmentResponse {
  id: number;
  name: string;
  description: string;
  code: string;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeWithDepartmentResponse {
  employee: EmployeeResponse;
  department: DepartmentResponse;
}

export interface EmployeePageParams {
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: 'asc' | 'desc';
  keyword?: string;
}

export interface EmployeePageSort {
  empty: boolean;
  sorted: boolean;
  unsorted: boolean;
}

export interface EmployeePageable {
  offset: number;
  pageNumber: number;
  pageSize: number;
  paged: boolean;
  unpaged: boolean;
  sort: EmployeePageSort;
}

export interface EmployeePage {
  content: EmployeeResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  sort: EmployeePageSort;
  pageable: EmployeePageable;
}
