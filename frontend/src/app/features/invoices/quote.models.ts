export interface QuoteLine { id?: number; description: string; quantity: number; unitPrice: number; lineTotal?: number; }
export type QuoteStatus = 'DRAFT' | 'SENT' | 'ACCEPTED' | 'REJECTED';
export interface QuoteRequest { customerId: string; issueDate: string | null; expiryDate: string | null; lines: QuoteLine[]; tax: number | null; currency: string | null; status: QuoteStatus; notes: string | null; }
export interface QuoteResponse extends QuoteRequest { id: number; quoteNumber: string; subtotal: number; totalAmount: number; convertedInvoiceId: number | null; createdAt: string | null; updatedAt: string | null; }
export interface QuotePage { content: QuoteResponse[]; totalElements: number; totalPages: number; number: number; size: number; }
