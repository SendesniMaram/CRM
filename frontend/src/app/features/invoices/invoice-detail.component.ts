import { PdfDownloadComponent } from '../../shared/pdf-download/pdf-download.component';
import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EMPTY, catchError, finalize, switchMap, tap } from 'rxjs';
import { InvoiceAmountPipe } from './invoice-amount.pipe';
import { InvoiceResponse } from './invoice.models';
import { InvoiceService } from './invoice.service';
@Component({
  selector: 'app-invoice-detail', standalone: true,
  imports: [PdfDownloadComponent, DatePipe, InvoiceAmountPipe, RouterLink, MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './invoice-detail.component.html', styleUrl: './invoices.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InvoiceDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(InvoiceService);
  private readonly destroy = inject(DestroyRef);
  protected readonly invoice = signal<InvoiceResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  ngOnInit() {
    this.route.paramMap.pipe(switchMap(params => {
      this.invoice.set(null); this.error.set(''); this.loading.set(false);
      const id = Number(params.get('id'));
      if (!Number.isSafeInteger(id) || id <= 0) { this.error.set('Facture introuvable.'); return EMPTY; }
      this.loading.set(true);
      return this.service.get(id).pipe(tap(row => this.invoice.set(row)), catchError((e: { status?: number }) => {
        this.error.set(e.status === 404 ? 'Facture introuvable.' : 'Impossible de charger la facture. Réessayez.'); return EMPTY;
      }), finalize(() => this.loading.set(false)));
    }), takeUntilDestroyed(this.destroy)).subscribe();
  }
}
