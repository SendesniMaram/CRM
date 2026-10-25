import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { DashboardData, DashboardDataService, distribution } from './dashboard-data.service';
import { ClientWorkspaceComponent } from '../client/client-workspace.component';

@Component({
  selector: 'app-dashboard', standalone: true,
  imports: [ClientWorkspaceComponent, DatePipe, RouterLink, MatButtonModule, MatCardModule, MatIconModule, MatProgressBarModule],
  templateUrl: './dashboard.component.html', styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent {
  private readonly service = inject(DashboardDataService);
  protected readonly auth = inject(AuthService);
  private readonly snack = inject(MatSnackBar);
  private request?: Subscription;
  protected readonly data = signal<DashboardData | null>(null);
  protected readonly loading = signal(false);
  protected readonly failed = signal(false);
  protected readonly refreshedAt = signal<Date | null>(null);
  protected readonly scope = computed(() => this.service.scope());
  private readonly identity = computed(() => {
    const user = this.auth.getCurrentUser();
    return user ? `${this.auth.sessionGeneration}:${user.username}` : null;
  });
  private readonly dataOwner = signal<string | null>(null);
  protected readonly visible = computed(() => this.data()?.scope === this.scope()
    && this.dataOwner() === this.identity() ? this.data() : null);
  protected readonly leaveStatuses = computed(() => distribution((this.visible()?.leaves.value ?? []).map(row =>
    ({ PENDING: 'En attente', APPROVED: 'Approuvés', REJECTED: 'Refusés' })[row.status])));
  protected readonly recent = computed(() => [...(this.visible()?.leaves.value ?? [])]
    .filter(row => Number.isFinite(Date.parse(row.createdAt)))
    .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt)).slice(0, 5));
  protected readonly cards = computed(() => {
    const data = this.visible();
    const leaves = data?.leaves.value;
    const personal = this.scope() === 'employee';
    const common = [
      { title: personal ? 'Mes demandes de congé' : 'Demandes de congé', value: leaves?.length ?? null, icon: 'event_note', route: '/leaves', error: data?.leaves.error },
      { title: personal ? 'Mes congés en attente' : 'Congés en attente', value: leaves ? leaves.filter(row => row.status === 'PENDING').length : null, icon: 'pending_actions', route: '/leaves', error: data?.leaves.error }
    ];
    if (this.scope() !== 'admin') return personal ? common : [];
    return [
      { title: 'Employés', value: data?.employees.value ?? null, icon: 'badge', route: '/employees', error: data?.employees.error },
      { title: 'Départements', value: data?.departments.value ?? null, icon: 'account_tree', route: '/departments', error: data?.departments.error },
      { title: 'Clients', value: data?.customers.value ?? null, icon: 'groups', route: '/customers', error: data?.customers.error },
      ...common,
      { title: 'Factures', value: data?.invoices.value?.count ?? null, icon: 'description', route: '/invoices', error: data?.invoices.error },
      { title: 'Honoraires', value: data?.fees.value ?? null, icon: 'receipt_long', route: '/fees', error: data?.fees.error },
      { title: 'Fiches de paie', value: data?.payroll.value ?? null, icon: 'payments', route: '/payroll', error: data?.payroll.error },
      { title: 'Formations', value: data?.trainings.value ?? null, icon: 'school', route: '/trainings', error: data?.trainings.error },
      { title: 'Présences enregistrées', value: data?.attendances.value ?? null, icon: 'schedule', route: '/attendances', error: data?.attendances.error },
      { title: 'Dossiers RH', value: data?.hrRecords.value ?? null, icon: 'folder_shared', route: '/hr-records', error: data?.hrRecords.error }
    ];
  });
  protected readonly links = computed(() => this.scope() === 'admin' ? [
    { label: 'Employés', route: '/employees', icon: 'badge' },
    { label: 'Départements', route: '/departments', icon: 'corporate_fare' },
    { label: 'Clients', route: '/customers', icon: 'groups' },
    { label: 'Congés', route: '/leaves', icon: 'event_note' },
    { label: 'Formations', route: '/trainings', icon: 'school' },
    { label: 'Présences', route: '/attendances', icon: 'schedule' },
    { label: 'Dossiers RH', route: '/hr-records', icon: 'folder_shared' },
    { label: 'Factures', route: '/invoices', icon: 'description' },
    { label: 'Honoraires', route: '/fees', icon: 'receipt_long' },
    { label: 'Paie', route: '/payroll', icon: 'payments' }
  ] : this.scope() === 'employee' ? [{ label: 'Mon profil', route: '/profile', icon: 'badge' }, { label: 'Mes congés', route: '/leaves', icon: 'event_note' }] : []);

  constructor() {
    effect(() => {
      this.scope();
      this.identity();
      untracked(() => this.refresh(false));
    });
    inject(DestroyRef).onDestroy(() => this.request?.unsubscribe());
  }
  protected refresh(notify = true) {
    this.request?.unsubscribe();
    this.data.set(null); this.failed.set(false); this.loading.set(true); this.refreshedAt.set(null);
    const owner = this.identity();
    this.request = this.service.load().subscribe({
      next: data => {
        this.dataOwner.set(owner);
        this.data.set(data); this.loading.set(false); this.refreshedAt.set(new Date());
        const errors = [data.employees, data.departments, data.trainings, data.attendances, data.hrRecords,
          data.customers, data.fees, data.payroll, data.leaves, data.invoices].some(item => !!item.error)
          || !!data.invoices.value?.reason;
        if (notify) this.snack.open(errors ? 'Actualisation partielle : certaines données sont indisponibles.'
          : 'Tableau de bord actualisé.', 'Fermer', { duration: 5000 });
      },
      error: () => {
        this.loading.set(false); this.failed.set(true);
        if (notify) this.snack.open('Impossible d’actualiser le tableau de bord.', 'Fermer', { duration: 5000 });
      }
    });
  }
}
