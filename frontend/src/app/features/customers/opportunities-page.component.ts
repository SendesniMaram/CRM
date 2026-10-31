import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EMPTY, Subject, catchError, finalize, switchMap, tap } from 'rxjs';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { CustomerResponse } from './customer.models';
import { CustomerService } from './customer.service';
import { OpportunityRequest, OpportunityStage, OpportunityStageHistory, SalesOpportunity } from './opportunity.models';
import { OpportunityQuery, OpportunityService } from './opportunity.service';

const STAGES: readonly OpportunityStage[] = ['NEW', 'QUALIFICATION', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST'];

function opportunityPaginator(): MatPaginatorIntl {
  const labels = new MatPaginatorIntl();
  labels.itemsPerPageLabel = 'Opportunités par page :';
  labels.nextPageLabel = 'Page suivante';
  labels.previousPageLabel = 'Page précédente';
  labels.getRangeLabel = (page, size, total) => total && size
    ? `${page * size + 1} – ${Math.min((page + 1) * size, total)} sur ${total}` : `0 sur ${total}`;
  return labels;
}

@Component({
  selector: 'app-opportunities-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule,
    MatFormFieldModule, MatIconModule, MatInputModule, MatPaginatorModule, MatProgressSpinnerModule, MatSelectModule],
  providers: [{ provide: MatPaginatorIntl, useFactory: opportunityPaginator }],
  templateUrl: './opportunities-page.component.html',
  styleUrl: './customers.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OpportunitiesPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(OpportunityService);
  private readonly customerApi = inject(CustomerService);
  private readonly destroy = inject(DestroyRef);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly requests = new Subject<OpportunityQuery>();

  protected readonly stages = STAGES;
  protected readonly rows = signal<SalesOpportunity[]>([]);
  protected readonly customers = signal<CustomerResponse[]>([]);
  protected readonly history = signal<OpportunityStageHistory[]>([]);
  protected readonly selected = signal<SalesOpportunity | null>(null);
  protected readonly total = signal(0);
  protected readonly page = signal(0);
  protected readonly size = signal(10);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly busy = signal(false);
  protected readonly historyError = signal('');
  protected readonly error = signal('');
  protected readonly search = this.fb.nonNullable.control('');
  protected readonly stageFilter = this.fb.nonNullable.control<OpportunityStage | ''>('');
  protected readonly form = this.fb.nonNullable.group({
    customerId: [0, [Validators.required, Validators.min(1)]],
    employeeId: [''],
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', Validators.maxLength(1000)],
    estimatedAmount: ['', Validators.pattern(/^\d+(\.\d{1,2})?$/)],
    stage: ['NEW' as OpportunityStage, Validators.required],
    expectedCloseDate: ['']
  });

  ngOnInit() {
    this.requests.pipe(switchMap(query => {
      this.loading.set(true);
      this.error.set('');
      return this.api.list(query).pipe(
        tap(result => { this.rows.set(result.content); this.total.set(result.totalElements); }),
        catchError(() => {
          this.rows.set([]);
          this.total.set(0);
          this.error.set('Impossible de charger le pipeline commercial.');
          return EMPTY;
        }),
        finalize(() => this.loading.set(false))
      );
    }), takeUntilDestroyed(this.destroy)).subscribe();
    this.load();
    this.customerApi.list({ page: 0, size: 100, sortBy: 'name', direction: 'asc' }).pipe(
      takeUntilDestroyed(this.destroy)
    ).subscribe({
      next: result => this.customers.set(result.content.filter(customer => customer.contactType !== 'SUPPLIER')),
      error: () => this.error.set('Impossible de charger les prospects et clients.')
    });
  }

  protected stageLabel(stage: OpportunityStage) {
    return {
      NEW: 'Nouvelle', QUALIFICATION: 'Qualification', PROPOSAL: 'Proposition',
      NEGOTIATION: 'Négociation', WON: 'Gagnée', LOST: 'Perdue'
    }[stage];
  }

  protected load() {
    this.requests.next({
      page: this.page(),
      size: this.size(),
      ...(this.search.value.trim() ? { keyword: this.search.value.trim() } : {}),
      ...(this.stageFilter.value ? { stage: this.stageFilter.value } : {})
    });
  }

  protected find() { this.page.set(0); this.load(); }
  protected paginate(event: PageEvent) { this.page.set(event.pageIndex); this.size.set(event.pageSize); this.load(); }

  protected edit(row: SalesOpportunity) {
    this.selected.set(row);
    this.error.set('');
    this.form.reset({
      customerId: row.customerId,
      employeeId: row.employeeId === null ? '' : String(row.employeeId),
      title: row.title,
      description: row.description ?? '',
      estimatedAmount: row.estimatedAmount === null ? '' : String(row.estimatedAmount),
      stage: row.stage,
      expectedCloseDate: row.expectedCloseDate ?? ''
    });
    this.loadHistory(row.id);
  }

  protected startNew() {
    this.selected.set(null);
    this.history.set([]);
    this.historyError.set('');
    this.error.set('');
    this.form.reset({
      customerId: 0, employeeId: '', title: '', description: '',
      estimatedAmount: '', stage: 'NEW', expectedCloseDate: ''
    });
  }

  protected save() {
    if (this.saving()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const employeeId = value.employeeId.trim() ? Number(value.employeeId) : null;
    if (employeeId !== null && (!Number.isSafeInteger(employeeId) || employeeId <= 0)) {
      this.form.controls.employeeId.setErrors({ pattern: true });
      return;
    }
    const request: OpportunityRequest = {
      customerId: value.customerId,
      employeeId,
      title: value.title.trim(),
      description: value.description.trim() || null,
      estimatedAmount: value.estimatedAmount === '' ? null : Number(value.estimatedAmount),
      stage: value.stage,
      expectedCloseDate: value.expectedCloseDate || null
    };
    const existing = this.selected();
    this.saving.set(true);
    this.error.set('');
    (existing ? this.api.update(existing.id, request) : this.api.create(request)).pipe(
      takeUntilDestroyed(this.destroy),
      finalize(() => this.saving.set(false))
    ).subscribe({
      next: row => {
        this.snack.open(existing ? 'Opportunité mise à jour.' : 'Opportunité créée.', 'Fermer', { duration: 4000 });
        this.selected.set(row);
        this.load();
        this.loadHistory(row.id);
      },
      error: (e: { status?: number }) => {
        this.error.set(e.status === 400 ? 'Vérifiez les champs et les associations.' :
          e.status === 404 ? 'Le contact ou le collaborateur sélectionné est introuvable.' :
            'Enregistrement refusé. Réessayez.');
      }
    });
  }

  protected changeStage(row: SalesOpportunity, stage: OpportunityStage) {
    if (row.stage === stage || this.busy()) return;
    this.busy.set(true);
    this.api.changeStage(row.id, stage).pipe(
      takeUntilDestroyed(this.destroy),
      finalize(() => this.busy.set(false))
    ).subscribe({
      next: updated => {
        this.rows.update(rows => rows.map(item => item.id === updated.id ? updated : item));
        if (this.selected()?.id === updated.id) {
          this.selected.set(updated);
          this.form.controls.stage.setValue(updated.stage);
          this.loadHistory(updated.id);
        }
      },
      error: () => this.error.set('Impossible de modifier l’étape de cette opportunité.')
    });
  }

  protected remove(row: SalesOpportunity) {
    if (this.busy() || row.stage === 'WON') return;
    this.busy.set(true);
    this.dialog.open(ConfirmDialogComponent, {
      width: '440px',
      maxWidth: '90vw',
      data: { title: 'Supprimer l’opportunité', message: `Supprimer « ${row.title} » ?`, confirmLabel: 'Supprimer' }
    }).afterClosed().pipe(
      switchMap(confirmed => confirmed === true ? this.api.delete(row.id) : EMPTY),
      takeUntilDestroyed(this.destroy),
      finalize(() => this.busy.set(false))
    ).subscribe({
      next: () => {
        if (this.selected()?.id === row.id) this.startNew();
        this.snack.open('Opportunité supprimée.', 'Fermer', { duration: 4000 });
        this.load();
      },
      error: (e: { status?: number }) => this.error.set(e.status === 409
        ? 'Une opportunité gagnée ne peut pas être supprimée.'
        : 'Impossible de supprimer cette opportunité.')
    });
  }

  private loadHistory(id: number) {
    this.history.set([]);
    this.historyError.set('');
    this.api.history(id).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: rows => this.history.set(rows),
      error: () => this.historyError.set('Historique de statut indisponible.')
    });
  }
}
