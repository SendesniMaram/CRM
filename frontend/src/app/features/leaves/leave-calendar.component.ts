import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LeaveResponse } from './leave.models';
import { LeaveCalendarService } from './leave-calendar.service';

@Component({
  selector: 'app-leave-calendar', standalone: true, imports: [DatePipe, FormsModule],
  template: `
    <main class="container py-4">
      <h1>Calendrier des congés</h1>
      <p>Les périodes et statuts proviennent des demandes enregistrées. Le calcul des soldes et des jours ouvrés n’est pas appliqué.</p>
      <div class="d-flex gap-2 align-items-center flex-wrap mb-3">
        <button type="button" class="btn btn-outline-primary" (click)="move(-1)">Mois précédent</button>
        <strong>{{ monthLabel() }}</strong>
        <button type="button" class="btn btn-outline-primary" (click)="move(1)">Mois suivant</button>
        <label>Employé <input class="form-control" [(ngModel)]="employeeId" (keyup.enter)="load()" maxlength="50"></label>
        <button type="button" class="btn btn-primary" (click)="load()" [disabled]="loading()">Filtrer</button>
      </div>
      @if (error()) { <p role="alert" class="text-danger">{{ error() }}</p> }
      @if (loading()) { <p role="status">Chargement…</p> }
      @if (!loading() && !error() && entries().length === 0) { <p>Aucune demande sur cette période.</p> }
      @if (entries().length) {
        <table class="table table-striped"><thead><tr><th>Employé</th><th>Début</th><th>Fin</th><th>Statut</th><th>Motif</th></tr></thead>
          <tbody>@for (entry of entries(); track entry.id) { <tr><td>{{ entry.employeeId }}</td><td>{{ entry.startDate | date:'mediumDate' }}</td><td>{{ entry.endDate | date:'mediumDate' }}</td><td>{{ entry.status }}</td><td>{{ entry.reason || '—' }}</td></tr> }</tbody>
        </table>
      }
    </main>`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LeaveCalendarComponent {
  private readonly service = inject(LeaveCalendarService);
  protected readonly entries = signal<LeaveResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected month = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
  protected employeeId = '';
  protected monthLabel() { return new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(this.month); }
  protected move(amount: number) { this.month = new Date(this.month.getFullYear(), this.month.getMonth() + amount, 1); this.load(); }
  protected load() {
    if (this.employeeId.trim() && !/^[A-Za-z0-9_-]{1,50}$/.test(this.employeeId.trim())) { this.error.set('Identifiant employé invalide.'); return; }
    const from = this.date(this.month), to = this.date(new Date(this.month.getFullYear(), this.month.getMonth() + 1, 0));
    this.loading.set(true); this.error.set('');
    this.service.list(from, to, this.employeeId).subscribe({
      next: rows => { this.entries.set(rows); this.loading.set(false); },
      error: (e: HttpErrorResponse) => { this.entries.set([]); this.loading.set(false); this.error.set(e.status === 403 ? 'Accès réservé à l’administration.' : e.status === 401 ? 'Session expirée.' : 'Impossible de charger le calendrier.'); }
    });
  }
  private date(value: Date) { return [value.getFullYear(), String(value.getMonth() + 1).padStart(2, '0'), String(value.getDate()).padStart(2, '0')].join('-'); }
}
