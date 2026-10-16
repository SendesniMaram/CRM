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
import { FeeResponse } from './fee.models';
import { FeeService } from './fee.service';
@Component({
  selector: 'app-fee-detail', standalone: true,
  imports: [DatePipe, InvoiceAmountPipe, RouterLink, MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './fee-detail.component.html', styleUrl: './fees.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FeeDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(FeeService);
  private readonly destroy = inject(DestroyRef);
  protected readonly fee = signal<FeeResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected formatHours(value: number) {
    return Number.isFinite(value) ? value.toLocaleString('fr-FR', { maximumFractionDigits: 20 }) : '—';
  }
  ngOnInit() {
    this.route.paramMap.pipe(switchMap(params => {
      this.fee.set(null); this.error.set(''); this.loading.set(false);
      const id = Number(params.get('id'));
      if (!Number.isSafeInteger(id) || id <= 0) { this.error.set('Honoraire introuvable.'); return EMPTY; }
      this.loading.set(true);
      return this.service.get(id).pipe(tap(row => this.fee.set(row)), catchError((e: { status?: number }) => {
        this.error.set(e.status === 404 ? 'Honoraire introuvable.' : 'Impossible de charger l’honoraire. Réessayez.'); return EMPTY;
      }), finalize(() => this.loading.set(false)));
    }), takeUntilDestroyed(this.destroy)).subscribe();
  }
}
