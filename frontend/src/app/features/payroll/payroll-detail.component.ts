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
import { InvoiceAmountPipe } from '../invoices/invoice-amount.pipe';
import { PayrollResponse } from './payroll.models';
import { PayrollService } from './payroll.service';
@Component({
  selector: 'app-payroll-detail', standalone: true,
  imports: [PdfDownloadComponent, DatePipe, InvoiceAmountPipe, RouterLink, MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './payroll-detail.component.html', styleUrl: './payroll.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PayrollDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(PayrollService);
  private readonly destroy = inject(DestroyRef);
  protected readonly payroll = signal<PayrollResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  ngOnInit() {
    this.route.paramMap.pipe(switchMap(params => {
      this.payroll.set(null); this.error.set(''); this.loading.set(false);
      const id = Number(params.get('id'));
      if (!Number.isSafeInteger(id) || id <= 0) { this.error.set('Fiche de paie introuvable.'); return EMPTY; }
      this.loading.set(true);
      return this.service.get(id).pipe(tap(row => this.payroll.set(row)), catchError((e: { status?: number }) => {
        this.error.set(e.status === 404 ? 'Fiche de paie introuvable.' : 'Impossible de charger la fiche de paie. Réessayez.'); return EMPTY;
      }), finalize(() => this.loading.set(false)));
    }), takeUntilDestroyed(this.destroy)).subscribe();
  }
}
