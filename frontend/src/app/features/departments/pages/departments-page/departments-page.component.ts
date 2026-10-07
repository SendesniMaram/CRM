import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
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
  imports: [RouterLink, MatButtonModule, MatCardModule, MatProgressSpinnerModule, MatTableModule],
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
    if (this.loading()) return;
    this.loading.set(true);
    this.errorMessage.set('');
    this.service.getDepartments().pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false))
    ).subscribe({
      next: (departments) => this.departments.set(departments),
      error: () => {
        this.departments.set([]);
        this.errorMessage.set('Impossible de charger les départements. Réessayez.');
      }
    });
  }

  protected deleteDepartment(department: DepartmentResponse): void {
    if (!this.canManage() || this.loading() || this.confirming() || this.deletingId() !== null) return;
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
        if (confirmed !== true || !this.canManage()) return EMPTY;
        this.deletingId.set(department.id);
        return this.service.deleteDepartment(department.id);
      }),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => { this.confirming.set(false); this.deletingId.set(null); })
    ).subscribe({
      next: () => {
        this.snackBar.open('Département supprimé.', 'Fermer', { duration: 5000 });
        this.loadDepartments();
      },
      error: (error: { status?: number }) => {
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
