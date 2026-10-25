import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { Subscription, finalize } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { adminAccountError } from './admin-account-error';
import { AssociationOptions, EmployeeAssociation, EmployeeAssociationService } from './employee-association.service';

@Component({
  selector: 'app-employee-association', standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatSelectModule],
  templateUrl: './employee-association.component.html',
  styles: ':host{display:block}form{display:flex;flex-wrap:wrap;gap:1rem;align-items:baseline}p{overflow-wrap:anywhere}[role=alert]{color:var(--mat-sys-error)}',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmployeeAssociationComponent {
  private readonly service = inject(EmployeeAssociationService);
  private readonly auth = inject(AuthService);
  private readonly destroy = inject(DestroyRef);
  private readonly requests = new Subscription();
  private generation = this.auth.sessionGeneration;
  private selection = 0;
  protected readonly options = signal<AssociationOptions | null>(null);
  protected readonly link = signal<EmployeeAssociation | null>(null);
  protected readonly state = signal<'unknown' | 'absent' | 'linked'>('unknown');
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly notice = signal('');
  protected readonly user = new FormControl('', { nonNullable: true });
  protected readonly employee = new FormControl('', { nonNullable: true });
  constructor() {
    this.destroy.onDestroy(() => this.requests.unsubscribe());
    effect(() => {
      // Read the reactive session as well as the generation, including logout/login with the same role.
      this.auth.getCurrentUser();
      if (this.generation !== this.auth.sessionGeneration || !this.isAdmin()) this.clear();
    });
  }
  protected isAdmin() { return this.auth.hasAnyRole([CrmRole.ADMIN, CrmRole.SUPER_ADMIN]); }
  private setBusy(value: boolean) {
    this.busy.set(value);
    for (const control of [this.user, this.employee]) {
      if (value) control.disable({ emitEvent: false }); else control.enable({ emitEvent: false });
    }
  }
  private clear() {
    this.selection++; this.generation = this.auth.sessionGeneration;
    this.options.set(null); this.link.set(null); this.state.set('unknown');
    this.user.reset(''); this.employee.reset(''); this.setBusy(false); this.error.set(''); this.notice.set('');
  }
  private allowed() {
    if (this.generation !== this.auth.sessionGeneration) { this.clear(); return false; }
    return this.isAdmin();
  }
  private current(generation: number, selection: number) {
    if (generation !== this.auth.sessionGeneration) {
      if (this.generation !== this.auth.sessionGeneration) this.clear();
      return false;
    }
    return this.isAdmin() && selection === this.selection && !this.destroy.destroyed;
  }
  protected load() {
    if (!this.allowed() || this.busy()) return;
    const generation = this.generation, selection = ++this.selection;
    this.options.set(null); this.user.reset(''); this.employee.reset(''); this.link.set(null); this.state.set('unknown');
    this.error.set(''); this.notice.set(''); this.setBusy(true);
    this.requests.add(this.service.options().pipe(takeUntilDestroyed(this.destroy), finalize(() => {
      if (this.current(generation, selection)) this.setBusy(false);
    })).subscribe({
      next: options => { if (this.current(generation, selection)) this.options.set(options); },
      error: error => { if (this.current(generation, selection)) this.error.set(adminAccountError(error, 'association')); }
    }));
  }
  protected select() {
    if (!this.allowed() || this.busy()) return;
    this.notice.set(''); this.employee.reset(''); this.read();
  }
  protected read() {
    if (!this.allowed() || this.busy() || !this.options()?.users.some(row => row.id === this.user.value)) return;
    const userId = this.user.value, generation = this.generation, selection = ++this.selection;
    this.link.set(null); this.state.set('unknown'); this.error.set(''); this.setBusy(true);
    this.requests.add(this.service.read(userId).pipe(takeUntilDestroyed(this.destroy), finalize(() => {
      if (this.current(generation, selection)) this.setBusy(false);
    })).subscribe({
      next: link => { if (this.current(generation, selection)) { this.link.set(link); this.state.set('linked'); } },
      error: error => {
        if (!this.current(generation, selection)) return;
        if (error.status === 404) this.state.set('absent');
        else this.error.set(adminAccountError(error, 'association'));
      }
    }));
  }
  protected employeeLabel(id: string) { return this.options()?.employees.find(row => row.id === id)?.label ?? 'Fiche indisponible dans l’annuaire chargé'; }
  protected create() {
    if (!this.allowed() || this.busy() || this.state() !== 'absent') return;
    const userId = this.user.value, employeeId = this.employee.value;
    if (!this.options()?.users.some(row => row.id === userId) || !this.options()?.employees.some(row => row.id === employeeId)) return;
    const generation = this.generation, selection = ++this.selection;
    this.setBusy(true); this.error.set(''); this.notice.set('');
    this.requests.add(this.service.create(userId, employeeId).pipe(takeUntilDestroyed(this.destroy), finalize(() => {
      if (this.current(generation, selection)) this.setBusy(false);
    })).subscribe({
      next: result => {
        if (!this.current(generation, selection)) return;
        this.link.set(result.association); this.state.set('linked');
        this.notice.set(result.created ? 'Association créée.' : 'Cette association est déjà enregistrée.');
        queueMicrotask(() => { if (this.current(generation, selection)) this.read(); });
      },
      error: error => {
        if (!this.current(generation, selection)) return;
        // A conflict or lost response requires reading the authoritative state before another write.
        this.state.set('unknown');
        this.error.set(error.status === 409 ? 'Conflit : ce compte ou cette fiche est déjà associé. Rechargez l’état ; aucune réaffectation n’est proposée.'
          : adminAccountError(error, 'association'));
      }
    }));
  }
}
