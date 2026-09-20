import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs';
import { DepartmentResponse, EmployeeResponse } from '../../models/employee.models';
import { EmployeeService } from '../../services/employee.service';

@Component({
  selector: 'app-employee-detail',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './employee-detail.component.html',
  styleUrl: './employee-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmployeeDetailComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly employeeService = inject(EmployeeService);
  private readonly router = inject(Router);

  protected readonly employee = signal<EmployeeResponse | null>(null);
  protected readonly department = signal<DepartmentResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly notFound = signal(false);

  ngOnInit(): void {
    const id = Number(this.activatedRoute.snapshot.paramMap.get('id'));

    if (!Number.isInteger(id) || id <= 0) {
      this.notFound.set(true);
      return;
    }

    this.loadEmployee(id);
  }

  protected goBackToEmployees(): void {
    void this.router.navigate(['/employees']);
  }

  private loadEmployee(id: number): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.notFound.set(false);

    this.employeeService.getEmployeeDetails(id).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (response) => {
        this.employee.set(response.employee);
        this.department.set(response.department);
      },
      error: (error: { status?: number }) => {
        if (error.status === 404) {
          this.notFound.set(true);
          return;
        }

        this.errorMessage.set("Une erreur est survenue lors du chargement de l'employé.");
      }
    });
  }
}
