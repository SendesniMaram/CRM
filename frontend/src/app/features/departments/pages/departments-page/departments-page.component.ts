import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { EMPTY, finalize, switchMap } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { CrmRole } from '../../../../core/models/role.model';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog/confirm-dialog.component';
import { DepartmentResponse } from '../../../employees/models/employee.models';
import { DepartmentService } from '../../services/department.service';

@Component({
  selector: 'app-departments-page', standalone: true,
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatPaginatorModule, RouterLink, MatButtonModule, MatCardModule, MatProgressSpinnerModule, MatTableModule],
  templateUrl: './departments-page.component.html',
  styleUrl: './departments-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DepartmentsPageComponent implements OnInit {
  private readonly service = inject(DepartmentService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly departments = signal<DepartmentResponse[]>([]);
  protected readonly query = signal('');
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly selected = signal<DepartmentResponse | null>(null);
  protected readonly filtered = computed(() => {
    const query = this.query().trim().toLocaleLowerCase('fr');
    return this.departments().filter(row => !query || [row.name, row.code, row.description]
      .some(value => value?.toLocaleLowerCase('fr').includes(query)));
  });
  protected readonly visible = computed(() => this.filtered().slice(this.pageIndex() * this.pageSize(), (this.pageIndex() + 1) * this.pageSize()));
  protected search(value: string) { this.query.set(value); this.pageIndex.set(0); }
  protected paginate(event: PageEvent) { this.pageIndex.set(event.pageIndex); this.pageSize.set(event.pageSize); }
  private generation = this.auth.sessionGeneration;
  constructor() {
    effect(() => {
      this.auth.getCurrentUser();
      const generation = this.auth.sessionGeneration;
      const allowed = this.canManage();
      untracked(() => {
        if (generation !== this.generation || !allowed) {
          this.generation = generation; this.departments.set([]); this.selected.set(null); this.search('');
          this.loading.set(false); this.confirming.set(false); this.deletingId.set(null); this.errorMessage.set('');
          if (allowed) this.loadDepartments();
        }
      });
    });
  }
  private current(generation: number) { return generation === this.auth.sessionGeneration && this.canManage() && !this.destroyRef.destroyed; }
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly confirming = signal(false);
  protected readonly deletingId = signal<number | null>(null);
  protected readonly columns = ['name', 'code', 'description', 'actions'];

  ngOnInit(): void { this.loadDepartments(); }

  protected canManage(): boolean {
    return this.auth.hasAnyRole([CrmRole.SUPER_ADMIN, CrmRole.ADMIN]);
  }

  protected loadDepartments(): void {
    if (this.loading() || !this.canManage()) return;
    const generation = this.auth.sessionGeneration;
    this.loading.set(true);
    this.errorMessage.set('');
    this.service.getDepartments().pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => { if (this.current(generation)) this.loading.set(false); })
    ).subscribe({
      next: (departments) => { if (this.current(generation)) { this.departments.set(departments); this.pageIndex.set(0); this.selected.set(null); } },
      error: () => {
        if (!this.current(generation)) return;
        this.departments.set([]);
        this.errorMessage.set('Impossible de charger les départements. Réessayez.');
      }
    });
  }

  protected deleteDepartment(department: DepartmentResponse): void {
    if (!this.canManage() || this.loading() || this.confirming() || this.deletingId() !== null) return;
    const generation = this.auth.sessionGeneration;
    this.confirming.set(true);
    this.dialog.open(ConfirmDialogComponent, {
      width: '440px', maxWidth: '90vw', autoFocus: 'first-tabbable',
      data: {
        title: 'Supprimer un département',
        message: `Supprimer le département « ${department.name} » ? Cette action est irréversible.`,
        confirmLabel: 'Supprimer'
      }
    }).afterClosed().pipe(
      switchMap((confirmed: boolean) => {
        if (confirmed !== true || !this.current(generation)) return EMPTY;
        this.deletingId.set(department.id);
        return this.service.deleteDepartment(department.id);
      }),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => { if (this.current(generation)) { this.confirming.set(false); this.deletingId.set(null); } })
    ).subscribe({
      next: () => {
        if (!this.current(generation)) return;
        this.snackBar.open('Département supprimé.', 'Fermer', { duration: 5000 });
        this.loadDepartments();
      },
      error: (error: { status?: number }) => {
        if (!this.current(generation)) return;
        if (error.status === 401 || error.status === 403) return;
        const message = error.status === 404 ? 'Ce département est introuvable ou a déjà été supprimé.'
          : error.status === 400 || error.status === 409
            ? 'Suppression refusée. Le département peut être associé à d’autres données.'
            : 'Impossible de supprimer ce département. Réessayez plus tard.';
        this.snackBar.open(message, 'Fermer', { duration: 8000 });
      }
    });
  }
}
