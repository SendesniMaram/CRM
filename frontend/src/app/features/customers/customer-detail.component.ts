import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EMPTY, catchError, finalize, switchMap, tap } from 'rxjs';
import { CustomerResponse } from './customer.models';
import { CustomerService } from './customer.service';

@Component({
  selector: 'app-customer-detail', standalone: true,
  imports: [DatePipe, RouterLink, MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './customer-detail.component.html', styleUrl: './customers.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CustomerDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(CustomerService);
  private readonly destroy = inject(DestroyRef);
  protected readonly customer = signal<CustomerResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  ngOnInit() {
    this.route.paramMap.pipe(switchMap(params => {
      this.customer.set(null); this.error.set(''); this.loading.set(false);
      const id = Number(params.get('id'));
      if (!Number.isSafeInteger(id) || id <= 0) { this.error.set('Client introuvable.'); return EMPTY; }
      this.loading.set(true);
      return this.service.get(id).pipe(tap(row => this.customer.set(row)), catchError((e: { status?: number }) => {
        this.error.set(e.status === 404 ? 'Client introuvable.' : 'Impossible de charger ce client. Réessayez.'); return EMPTY;
      }), finalize(() => this.loading.set(false)));
    }), takeUntilDestroyed(this.destroy)).subscribe();
  }
}
