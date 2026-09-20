import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
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

  ngOnInit(): void {
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

    this.loading.set(true);
    this.errorMessage.set('');

    this.employeeService.getEmployeesPage(params).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (response) => {
        this.employees.set(response.content);
        this.totalElements.set(response.totalElements);
      },
      error: () => {
        this.employees.set([]);
        this.totalElements.set(0);
        this.errorMessage.set('Impossible de charger les employés.');
      }
    });
  }
}
