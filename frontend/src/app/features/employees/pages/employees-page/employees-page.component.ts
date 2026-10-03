import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EMPTY, Subject, catchError, switchMap, tap } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { CrmRole } from '../../../../core/models/role.model';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog/confirm-dialog.component';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import {
  EmployeePageParams,
  EmployeeResponse
} from '../../models/employee.models';
import { EmployeeService } from '../../services/employee.service';

export function getFrenchPaginatorIntl(): MatPaginatorIntl {
  const intl = new MatPaginatorIntl();
  intl.itemsPerPageLabel = 'Éléments par page :';
  intl.nextPageLabel = 'Suivant';
  intl.previousPageLabel = 'Précédent';
  intl.firstPageLabel = 'Première page';
  intl.lastPageLabel = 'Dernière page';
  intl.getRangeLabel = (page: number, pageSize: number, length: number): string => {
    if (length === 0 || pageSize === 0) {
      return `0 sur ${length}`;
    }
    const safeLength = Math.max(length, 0);
    const startIndex = page * pageSize;
    const endIndex = startIndex < safeLength ? Math.min(startIndex + pageSize, safeLength) : startIndex + pageSize;
    return `${startIndex + 1} – ${endIndex} sur ${safeLength}`;
  };
  return intl;
}

@Component({
  selector: 'app-employees-page',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSortModule,
    MatTableModule,
    MatTooltipModule,
    ReactiveFormsModule,
    RouterLink
  ],
  providers: [
    { provide: MatPaginatorIntl, useFactory: getFrenchPaginatorIntl }
  ],
  templateUrl: './employees-page.component.html',
  styleUrl: './employees-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmployeesPageComponent implements OnInit {
  private readonly employeeService = inject(EmployeeService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly deletingId = signal<number | null>(null);
  protected readonly confirming = signal(false);

  protected canDelete(): boolean {
    return this.auth.hasAnyRole([CrmRole.ADMIN, CrmRole.SUPER_ADMIN]);
  }

  protected deleteEmployee(employee: EmployeeResponse): void {
    if (!this.canDelete() || this.confirming() || this.deletingId() !== null || this.loading()) return;
    const deletionListRevision = this.listRevision;
    const deletionKeyword = this.searchControl.value.trim();
    const deletingLastRow = this.employees().length === 1 && this.employees()[0].id === employee.id;
    this.confirming.set(true);
    const name = [employee.firstName, employee.lastName].filter(Boolean).join(' ');
    this.dialog.open(ConfirmDialogComponent, {
      width: '440px', maxWidth: '90vw', autoFocus: 'first-tabbable',
      data: {
        title: 'Supprimer un employé',
        message: `Supprimer l’employé ${employee.employeeCode}${name ? ' — ' + name : ''} ? Cette action est irréversible.`,
        confirmLabel: 'Supprimer'
      }
    }).afterClosed().pipe(
      switchMap((confirmed: boolean) => {
        if (confirmed !== true || !this.canDelete()) return EMPTY;
        this.deletingId.set(employee.id);
        return this.employeeService.deleteEmployee(employee.id);
      }),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => { this.confirming.set(false); this.deletingId.set(null); })
    ).subscribe({
      next: () => {
        this.snackBar.open(`L’employé ${employee.employeeCode} a été supprimé.`, 'Fermer', { duration: 5000 });
        // Never apply the old page's last-row decision to a newer list context.
        if (deletingLastRow && deletionListRevision === this.listRevision
            && deletionKeyword === this.searchControl.value.trim() && this.pageIndex() > 0) {
          this.pageIndex.update((page) => page - 1);
        }
        this.loadEmployees();
      },
      error: (error: { status?: number; error?: { message?: unknown } }) => {
        // Authentication errors remain the responsibility of the interceptor.
        if (error.status === 401 || error.status === 403) return;
        // No stable deletion-specific 400/409 code exists: never reflect backend text.
        const message = error.status === 404
          ? 'Cet employé est introuvable ou a déjà été supprimé.'
          : 'Impossible de supprimer cet employé. Réessayez plus tard.';
        this.snackBar.open(message, 'Fermer', { duration: 8000 });
      }
    });
  }

  protected readonly employees = signal<EmployeeResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly totalElements = signal(0);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly sortBy = signal('id');
  protected readonly direction = signal<'asc' | 'desc'>('asc');
  protected readonly searchControl = new FormControl('', { nonNullable: true });
  protected readonly displayedColumns = [
    'employeeCode',
    'firstName',
    'lastName',
    'email',
    'jobTitle',
    'hireDate',
    'salary',
    'status',
    'actions'
  ];
  protected readonly pageSizeOptions = [10, 25, 50];
  private readonly listLoads = new Subject<EmployeePageParams>();
  private listRevision = 0;

  ngOnInit(): void {
    this.listLoads.pipe(
      switchMap((params) => {
        // The old request is finalized before the new loading state is set.
        this.loading.set(true);
        this.errorMessage.set('');
        return this.employeeService.getEmployeesPage(params).pipe(
          tap((response) => {
            this.employees.set(response.content);
            this.totalElements.set(response.totalElements);
          }),
          catchError(() => {
            this.employees.set([]);
            this.totalElements.set(0);
            this.errorMessage.set('Impossible de charger les employés.');
            return EMPTY;
          }),
          finalize(() => this.loading.set(false))
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
    this.loadEmployees();
  }

  protected searchEmployees(): void {
    this.pageIndex.set(0);
    this.loadEmployees();
  }

  protected onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadEmployees();
  }

  protected onSortChange(sort: Sort): void {
    this.pageIndex.set(0);
    this.sortBy.set(sort.active || 'id');
    this.direction.set(sort.direction === 'desc' ? 'desc' : 'asc');
    this.loadEmployees();
  }

  private loadEmployees(): void {
    const params: EmployeePageParams = {
      page: this.pageIndex(),
      size: this.pageSize(),
      sortBy: this.sortBy(),
      direction: this.direction()
    };
    const keyword = this.searchControl.value.trim();

    if (keyword) {
      params.keyword = keyword;
    }

    this.listRevision++;
    this.listLoads.next(params);
  }
}
