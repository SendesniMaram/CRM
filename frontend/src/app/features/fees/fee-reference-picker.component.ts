import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { EMPTY, Subject, catchError, finalize, map, switchMap, tap } from 'rxjs';
import { CustomerService } from '../customers/customer.service';
import { EmployeeService } from '../employees/services/employee.service';

interface ReferenceChoice { id: number; label: string }

// Optional directory assistance: Fees stores string references and does not enforce foreign keys.
@Component({
  selector: 'app-fee-reference-picker', standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <section class="reference-assistance" [attr.aria-label]="label()">
      <h2>{{ label() }}</h2>
      <div class="toolbar">
        <mat-form-field appearance="outline" class="search-field"><mat-label>{{ kind() === 'employee' ? 'Rechercher un employé' : 'Rechercher un client' }}</mat-label>
          <input matInput [formControl]="search" (keydown.enter)="find(); $event.preventDefault()" /></mat-form-field>
        <button mat-stroked-button type="button" (click)="find()" [disabled]="disabled()">Rechercher</button>
      </div>
      @if (loading()) { <p role="status">Chargement de l’annuaire…</p> }
      @if (error()) { <p class="error" role="alert">{{ error() }}</p><button mat-button type="button" (click)="load()" [disabled]="disabled()">Réessayer</button> }
      @if (loaded() && !loading() && !error()) {
        @if (choices().length === 0) { <p role="status">Aucun résultat dans cet annuaire.</p> }
        @else {
          <mat-form-field appearance="outline" class="full-width"><mat-label>Utiliser une référence</mat-label>
            <mat-select [value]="null" (selectionChange)="select($event.value)" [disabled]="disabled()">
              @for (choice of choices(); track choice.id) { <mat-option [value]="choice.id">{{ choice.label }} — {{ choice.id }}</mat-option> }
            </mat-select></mat-form-field>
        }
        <div class="toolbar"><button mat-button type="button" (click)="paginate(-1)" [disabled]="disabled() || page() === 0">Précédents</button>
          <span>Page {{ pages() ? page() + 1 : 0 }} sur {{ pages() }}</span>
          <button mat-button type="button" (click)="paginate(1)" [disabled]="disabled() || page() + 1 >= pages()">Suivants</button></div>
      }
    </section>`,
  styleUrl: './fees.scss', changeDetection: ChangeDetectionStrategy.OnPush
})
export class FeeReferencePickerComponent implements OnInit {
  readonly kind = input.required<'employee' | 'customer'>();
  readonly disabled = input(false);
  readonly referenceSelected = output<string>();
  private readonly employees = inject(EmployeeService);
  private readonly customers = inject(CustomerService);
  private readonly destroy = inject(DestroyRef);
  private readonly requests = new Subject<void>();
  protected readonly search = new FormControl('', { nonNullable: true });
  protected readonly choices = signal<ReferenceChoice[]>([]);
  protected readonly page = signal(0);
  protected readonly pages = signal(0);
  protected readonly loading = signal(false);
  protected readonly loaded = signal(false);
  protected readonly error = signal('');
  protected label() { return this.kind() === 'employee' ? 'Choisir depuis les employés' : 'Choisir depuis les clients'; }

  ngOnInit() {
    this.requests.pipe(switchMap(() => {
      this.loading.set(true); this.error.set(''); this.choices.set([]);
      const query = { page: this.page(), size: 25, direction: 'asc' as const, keyword: this.search.value.trim() };
      const request = this.kind() === 'employee'
        ? this.employees.getEmployeesPage({ ...query, sortBy: 'employeeCode' }).pipe(map(result => ({
            totalPages: result.totalPages, choices: result.content.map(row => ({ id: row.id,
              label: [row.firstName, row.lastName].filter(Boolean).join(' ') || row.employeeCode })) })))
        : this.customers.list({ ...query, sortBy: 'name' }).pipe(map(result => ({
            totalPages: result.totalPages, choices: result.content.map(row => ({ id: row.id, label: row.name })) })));
      return request.pipe(tap(result => {
        this.choices.set(result.choices.filter(row => Number.isSafeInteger(row.id) && row.id > 0));
        this.pages.set(result.totalPages); this.loaded.set(true);
      }), catchError(() => {
        this.pages.set(0); this.error.set('Annuaire indisponible. Réessayez ou conservez la référence saisie.'); return EMPTY;
      }), finalize(() => this.loading.set(false)));
    }), takeUntilDestroyed(this.destroy)).subscribe();
  }
  protected load() { if (!this.disabled()) this.requests.next(); }
  protected find() { this.page.set(0); this.load(); }
  protected paginate(offset: number) {
    if (this.disabled() || this.loading()) return;
    const page = this.page() + offset;
    if (page >= 0 && page < this.pages()) { this.page.set(page); this.load(); }
  }
  protected select(value: unknown) {
    if (!this.disabled() && this.choices().some(row => row.id === value)) this.referenceSelected.emit(String(value));
  }
}
