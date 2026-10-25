import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { adminAccountError } from './admin-account-error';
import { DirectoryUser, UserDirectoryService } from './user-directory.service';
import { EmployeeAssociationComponent } from './employee-association.component';

@Component({
  selector: 'app-users-page', standalone: true,
  imports: [EmployeeAssociationComponent, ReactiveFormsModule, RouterLink, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatPaginatorModule],
  templateUrl: './users-page.component.html',
  styles: `:host{display:block}mat-card{margin-bottom:1rem}form{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:1rem}table{width:100%;border-collapse:collapse}th,td{text-align:left;padding:.75rem;border-bottom:1px solid #ddd;overflow-wrap:anywhere}.table-scroll{overflow:auto}[role=alert]{color:var(--mat-sys-error)}.actions{display:flex;flex-wrap:wrap;gap:.5rem}`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersPageComponent implements OnInit {
  private readonly service = inject(UserDirectoryService);
  private readonly auth = inject(AuthService);
  private readonly destroy = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private generation = this.auth.sessionGeneration;
  protected readonly users = signal<DirectoryUser[]>([]);
  protected readonly query = signal('');
  protected readonly roleFilter = signal('');
  protected readonly stateFilter = signal('');
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly roles = Object.values(CrmRole);
  protected readonly selectedUser = signal<DirectoryUser | null>(null);
  protected readonly filteredUsers = computed(() => {
    const query = this.query().trim().toLocaleLowerCase('fr');
    return this.users().filter(user => (!query || [user.id, user.username, user.firstName, user.lastName, user.email]
      .some(value => value?.toLocaleLowerCase('fr').includes(query)))
      && (!this.roleFilter() || user.roles.some(role => role === this.roleFilter()))
      && (!this.stateFilter() || (this.stateFilter() === 'active' ? user.enabled && user.accountNonLocked
        : this.stateFilter() === 'disabled' ? !user.enabled : !user.accountNonLocked)));
  });
  protected readonly visibleUsers = computed(() => this.filteredUsers().slice(
    this.pageIndex() * this.pageSize(), (this.pageIndex() + 1) * this.pageSize()));
  protected filter(query: string, role: string, state: string) {
    this.query.set(query); this.roleFilter.set(role); this.stateFilter.set(state); this.pageIndex.set(0);
  }
  protected paginate(event: PageEvent) { this.pageIndex.set(event.pageIndex); this.pageSize.set(event.pageSize); }
  protected readonly loading = signal(false);
  protected readonly busy = signal(false);
  protected readonly listError = signal('');
  protected readonly error = signal('');
  protected readonly createdUsername = signal('');
  protected readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)]],
    username: ['', [Validators.required, Validators.pattern(/^\S{3,50}$/)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    password: ['', [Validators.required, Validators.pattern(/\S/), Validators.minLength(6), Validators.maxLength(100)]],
    phone: ['', Validators.maxLength(20)]
  });
  protected isAdmin() { return this.auth.hasAnyRole([CrmRole.ADMIN, CrmRole.SUPER_ADMIN]); }
  constructor() {
    effect(() => {
      this.auth.getCurrentUser();
      if (this.generation !== this.auth.sessionGeneration || !this.isAdmin()) {
        untracked(() => {
        this.generation = this.auth.sessionGeneration;
        this.users.set([]); this.createdUsername.set(''); this.form.reset();
        this.filter('', '', ''); this.selectedUser.set(null);
        this.error.set(''); this.listError.set(''); this.loading.set(false); this.busy.set(false);
        if (this.isAdmin()) this.load();
        });
      }
    });
  }
  private current(generation: number) { return generation === this.auth.sessionGeneration && this.isAdmin(); }
  ngOnInit() { this.load(); }
  protected load() {
    if (!this.isAdmin() || this.loading() || this.busy()) return;
    const generation = this.auth.sessionGeneration;
    this.loading.set(true); this.listError.set('');
    this.service.list().pipe(takeUntilDestroyed(this.destroy), finalize(() => { if (this.current(generation)) this.loading.set(false); })).subscribe({
      next: rows => { if (this.current(generation)) { this.users.set(rows); this.pageIndex.set(0); this.selectedUser.set(null); } },
      error: error => { if (this.current(generation)) { this.users.set([]); this.selectedUser.set(null); this.listError.set(adminAccountError(error, 'account')); } }
    });
  }
  protected create() {
    if (!this.isAdmin() || this.busy() || this.createdUsername()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const request = this.form.getRawValue();
    const generation = this.auth.sessionGeneration;
    this.busy.set(true); this.error.set('');
    this.service.createEmployee(request).pipe(takeUntilDestroyed(this.destroy), finalize(() => { if (this.current(generation)) this.busy.set(false); })).subscribe({
      next: () => {
        if (!this.current(generation)) return;
        this.createdUsername.set(request.username);
        this.form.controls.password.reset('');
        // Start the independent list refresh after the creation subscription has finalized.
        queueMicrotask(() => { if (!this.destroy.destroyed && this.current(generation)) this.load(); });
      },
      error: error => {
        if (!this.current(generation)) return;
        this.form.controls.password.reset('');
        this.error.set(adminAccountError(error, 'account'));
      }
    });
  }
  protected another() {
    if (this.busy()) return;
    this.form.reset(); this.createdUsername.set(''); this.error.set('');
  }
}
