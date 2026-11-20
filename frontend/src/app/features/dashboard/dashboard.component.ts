import { DatePipe, DecimalPipe } from '@angular/common';
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
  imports: [ClientWorkspaceComponent, DatePipe, DecimalPipe, RouterLink, MatButtonModule, MatCardModule, MatIconModule, MatProgressBarModule],
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
  protected opportunityStages() {
    const stages = this.visible()?.contacts.value?.opportunitiesByStage;
    return stages ? Object.values(stages) : [];
  }
  protected readonly cards = computed(() => {
    const data = this.visible();
    const leaves = data?.leaves.value;
    if (this.scope() === 'client') {
      const stats = data?.clientInvoices.value;
      return [
        { title: 'Mes factures', value: stats?.invoiceCount ?? null, icon: 'description', route: '/client', error: data?.clientInvoices.error },
        { title: 'Factures payées', value: stats?.paidInvoiceCount ?? null, icon: 'task_alt', route: '/client', error: data?.clientInvoices.error },
        { title: 'Factures avec un reste dû', value: stats?.unpaidInvoiceCount ?? null, icon: 'pending_actions', route: '/client', error: data?.clientInvoices.error },
        { title: 'Montants restant dus', value: stats ? stats.byCurrency.length
          ? stats.byCurrency.map(row => `${row.outstanding.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${row.currency}`).join(' · ')
          : '0,00' : null, icon: 'payments', route: '/client', error: data?.clientInvoices.error }
      ];
    }
    const personal = this.scope() === 'employee';
    const common = [
      { title: personal ? 'Mes demandes de congé' : 'Demandes de congé', value: leaves?.length ?? null, icon: 'event_note', route: '/leaves', error: data?.leaves.error },
      { title: personal ? 'Mes congés en attente' : 'Congés en attente', value: leaves ? leaves.filter(row => row.status === 'PENDING').length : null, icon: 'pending_actions', route: '/leaves', error: data?.leaves.error }
    ];
    if (personal) return [
      { title: 'Mon profil', value: data?.personalProfile.value ? [data.personalProfile.value.firstName, data.personalProfile.value.lastName].filter(Boolean).join(' ') || 'Disponible' : null, icon: 'badge', route: '/profile', error: data?.personalProfile.error },
      ...common,
      { title: 'Mes bulletins publiés', value: data?.personalPayroll.value ?? null, icon: 'payments', route: '/my-payroll', error: data?.personalPayroll.error },
      { title: 'Mes évaluations finalisées', value: data?.personalEvaluations.value ?? null, icon: 'fact_check', route: '/my-evaluations', error: data?.personalEvaluations.error }
    ];
    if (this.scope() !== 'admin') return [];
    const opportunityCount = data?.contacts.value ? Object.values(data.contacts.value.opportunitiesByStage).reduce((total, row) => total + row.count, 0) : null;
    return [
      { title: 'Employés', value: data?.employees.value ?? null, icon: 'badge', route: '/employees', error: data?.employees.error },
      { title: 'Départements', value: data?.departments.value ?? null, icon: 'account_tree', route: '/departments', error: data?.departments.error },
      { title: 'Clients', value: data?.customers.value ?? null, icon: 'groups', route: '/customers', error: data?.customers.error },
      { title: 'Prospects', value: data?.contacts.value?.prospects ?? null, icon: 'person_search', route: '/customers', error: data?.contacts.error },
      { title: 'Fournisseurs', value: data?.contacts.value?.suppliers ?? null, icon: 'local_shipping', route: '/customers', error: data?.contacts.error },
      { title: 'Opportunités', value: opportunityCount, icon: 'trending_up', route: '/customers/opportunities', error: data?.contacts.error },
      ...common,
      { title: 'Factures', value: data?.invoices.value?.count ?? null, icon: 'description', route: '/invoices', error: data?.invoices.error },
      { title: 'Honoraires', value: data?.fees.value ?? null, icon: 'receipt_long', route: '/fees', error: data?.fees.error },
      { title: 'Fiches de paie', value: data?.payroll.value ?? null, icon: 'payments', route: '/payroll', error: data?.payroll.error },
      { title: 'Formations', value: data?.trainings.value ?? null, icon: 'school', route: '/trainings', error: data?.trainings.error },
      { title: 'Présences enregistrées', value: data?.attendances.value ?? null, icon: 'schedule', route: '/attendances', error: data?.attendances.error },
      { title: 'Dossiers RH', value: data?.hrRecords.value ?? null, icon: 'folder_shared', route: '/hr-records', error: data?.hrRecords.error },
      { title: 'Contrats', value: data?.contracts.value ?? null, icon: 'description', route: '/contracts', error: data?.contracts.error },
      { title: 'Evaluations RH', value: data?.evaluations.value ?? null, icon: 'fact_check', route: '/evaluations', error: data?.evaluations.error },
      { title: 'Offres de recrutement', value: data?.jobOffers.value ?? null, icon: 'work', route: '/job-offers', error: data?.jobOffers.error },
      { title: 'Candidatures', value: data?.applications.value ?? null, icon: 'group_add', route: '/applications', error: data?.applications.error }
    ];
  });
  protected readonly links = computed(() => this.scope() === 'admin' ? [
    { label: 'Employés', route: '/employees', icon: 'badge' },
    { label: 'Départements', route: '/departments', icon: 'corporate_fare' },
    { label: 'Clients', route: '/customers', icon: 'groups' },
    { label: 'Congés', route: '/leaves', icon: 'event_note' },
    { label: 'Calendrier des congés', route: '/leave-calendar', icon: 'calendar_month' },
    { label: 'Formations', route: '/trainings', icon: 'school' },
    { label: 'Présences', route: '/attendances', icon: 'schedule' },
    { label: 'Dossiers RH', route: '/hr-records', icon: 'folder_shared' },
    { label: 'Contrats', route: '/contracts', icon: 'description' },
    { label: 'Évaluations', route: '/evaluations', icon: 'fact_check' },
    { label: 'Offres', route: '/job-offers', icon: 'work' },
    { label: 'Candidatures', route: '/applications', icon: 'group_add' },
    { label: 'Factures', route: '/invoices', icon: 'description' },
    { label: 'Devis', route: '/invoices/quotes', icon: 'request_quote' },
    { label: 'Opportunités', route: '/customers/opportunities', icon: 'trending_up' },
    { label: 'Honoraires', route: '/fees', icon: 'receipt_long' },
    { label: 'Paie', route: '/payroll', icon: 'payments' }
  ] : this.scope() === 'employee' ? [{ label: 'Mon profil', route: '/profile', icon: 'badge' }, { label: 'Mes congés', route: '/leaves', icon: 'event_note' }, { label: 'Mes bulletins', route: '/my-payroll', icon: 'payments' }, { label: 'Mes évaluations', route: '/my-evaluations', icon: 'fact_check' }] : []);

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
          data.contracts, data.evaluations, data.jobOffers, data.applications,
          data.customers, data.fees, data.payroll, data.leaves, data.invoices, data.contacts, data.feeOverview,
          data.personalProfile, data.personalPayroll, data.personalEvaluations, data.clientInvoices].some(item => !!item.error)
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
