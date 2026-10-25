import { DOCUMENT } from '@angular/common';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, tap } from 'rxjs';
import { PDF_DOWNLOAD_REQUEST } from '../../core/http/pdf-download-context';

export type PdfKind = 'invoice' | 'leave' | 'payroll';
const paths = { invoice: 'invoice/api/invoices', leave: 'hr/api/leaves', payroll: 'payroll/api/payrolls' };
const names = { invoice: 'facture', leave: 'conge', payroll: 'paie' };

@Injectable({ providedIn: 'root' })
export class PdfDownloadService {
  private readonly http = inject(HttpClient);
  private readonly document = inject(DOCUMENT);

  download(kind: PdfKind, id: number) {
    if (!Number.isSafeInteger(id) || id <= 0) throw new Error('Invalid document identifier');
    return this.http.get(`http://localhost:8080/${paths[kind]}/${id}/pdf`, {
      observe: 'response', responseType: 'blob', context: new HttpContext().set(PDF_DOWNLOAD_REQUEST, true),
      headers: { Accept: 'application/pdf, application/json' }
    }).pipe(map(response => {
      const type = response.headers.get('Content-Type')?.split(';')[0].trim().toLowerCase();
      if (type !== 'application/pdf' || !response.body?.size) throw new Error('Invalid PDF response');
      return response.body;
    }), tap(blob => {
      const url = URL.createObjectURL(blob);
      const link = this.document.createElement('a');
      link.href = url; link.download = `${names[kind]}-${id}.pdf`;
      this.document.body.appendChild(link);
      try { link.click(); } finally {
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      }
    }));
  }
}
