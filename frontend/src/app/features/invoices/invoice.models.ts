export interface InvoiceRequest {
  invoiceNumber: string;
  customerId: string;
  issueDate: string | null;
  dueDate: string | null;
  subtotal: number;
  tax: number | null;
  currency: string | null;
  status: string;
  notes: string | null;
}
export interface InvoiceResponse extends InvoiceRequest {
  id: number;
  totalAmount: number;
  createdAt: string | null;
  updatedAt: string | null;
}
export type InvoiceSort = 'id' | 'invoiceNumber' | 'customerId' | 'issueDate' | 'totalAmount' | 'status';
export interface InvoicePageParams {
  page: number; size: number; sortBy: InvoiceSort; direction: 'asc' | 'desc'; keyword?: string;
}
export interface InvoicePage {
  content: InvoiceResponse[]; totalElements: number; totalPages: number; number: number; size: number;
}
// No coercion of arbitrary legacy references, fractions or unsafe numeric IDs.
export function parseCustomerId(value: string): number | null {
  if (!/^[1-9]\d*$/.test(value)) return null;
  const id = Number(value);
  return Number.isSafeInteger(id) ? id : null;
}
