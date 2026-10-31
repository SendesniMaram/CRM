import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { EMPTY, catchError, finalize, of, switchMap, tap } from 'rxjs';
import { CustomerInteraction, CustomerInteractionRequest, CustomerResponse, InteractionType } from './customer.models';
import { CustomerService } from './customer.service';

@Component({
  selector: 'app-customer-detail', standalone: true,
  imports: [DatePipe, ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatIconModule, MatInputModule, MatProgressSpinnerModule, MatSelectModule],
  templateUrl: './customer-detail.component.html', styleUrl: './customers.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CustomerDetailComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(CustomerService);
  private readonly destroy = inject(DestroyRef);
  protected readonly customer = signal<CustomerResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly interactions = signal<CustomerInteraction[]>([]);
  protected readonly interactionsLoading = signal(false);
  protected readonly interactionsError = signal('');
  protected readonly savingInteraction = signal(false);
  protected readonly interactionError = signal('');
  protected readonly interactionForm = this.fb.nonNullable.group({
    interactionType: ['CALL' as InteractionType, Validators.required],
    occurredAt: [this.localDateTime(), Validators.required],
    notes: ['', Validators.maxLength(5000)]
  });
  ngOnInit() {
    this.route.paramMap.pipe(switchMap(params => {
      this.customer.set(null); this.error.set(''); this.loading.set(false);
      this.interactions.set([]); this.interactionsError.set(''); this.interactionError.set('');
      const id = Number(params.get('id'));
      if (!Number.isSafeInteger(id) || id <= 0) { this.error.set('Client introuvable.'); return EMPTY; }
      this.loading.set(true);
      return this.service.get(id).pipe(
        tap(row => this.customer.set(row)),
        switchMap(() => {
          this.interactionsLoading.set(true);
          return this.service.listInteractions(id).pipe(
            tap(rows => this.interactions.set(rows)),
            catchError(() => {
              this.interactionsError.set('Impossible de charger l’historique des interactions.');
              return of([]);
            }),
            finalize(() => this.interactionsLoading.set(false))
          );
        }),
        catchError((e: { status?: number }) => {
          this.error.set(e.status === 404 ? 'Client introuvable.' : 'Impossible de charger ce client. Réessayez.');
          return EMPTY;
        }),
        finalize(() => this.loading.set(false))
      );
    }), takeUntilDestroyed(this.destroy)).subscribe();
  }

  protected interactionLabel(type: InteractionType) {
    return { CALL: 'Appel', EMAIL: 'E-mail', MEETING: 'Réunion', NOTE: 'Note' }[type];
  }

  protected submitInteraction() {
    const customer = this.customer();
    if (this.savingInteraction() || !customer) return;
    if (this.interactionForm.invalid) {
      this.interactionForm.markAllAsTouched();
      return;
    }

    const value = this.interactionForm.getRawValue();
    const request: CustomerInteractionRequest = {
      interactionType: value.interactionType,
      occurredAt: value.occurredAt,
      notes: value.notes.trim() || null
    };
    this.savingInteraction.set(true);
    this.interactionError.set('');
    this.service.addInteraction(customer.id, request).pipe(
      takeUntilDestroyed(this.destroy),
      finalize(() => this.savingInteraction.set(false))
    ).subscribe({
      next: row => {
        this.interactions.update(rows => [row, ...rows].sort(
          (left, right) => right.occurredAt.localeCompare(left.occurredAt)
        ));
        this.interactionForm.reset({ interactionType: 'CALL', occurredAt: this.localDateTime(), notes: '' });
      },
      error: (e: { status?: number }) => {
        if (e.status !== 401 && e.status !== 403) {
          this.interactionError.set('Impossible d’enregistrer l’interaction. Vérifiez les informations et réessayez.');
        }
      }
    });
  }

  private localDateTime() {
    const now = new Date();
    return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  }
}
