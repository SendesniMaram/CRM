import { ChangeDetectionStrategy, Component, DestroyRef, Injector, Input, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { PdfDownloadService, PdfKind } from './pdf-download.service';

/** Rendered only inside existing role-guarded details or the server-filtered leave list. */
@Component({
  selector: 'app-pdf-download', standalone: true,
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<button mat-stroked-button type="button" [disabled]="busy()" [attr.aria-busy]="busy()"
    (click)="download()" aria-label="Télécharger le document PDF">
    @if (busy()) { <mat-spinner diameter="18" aria-hidden="true" /> } @else { <mat-icon>download</mat-icon> }
    {{ busy() ? 'Génération…' : 'Télécharger PDF' }}
  </button>
  @if (error()) { <p class="pdf-error" role="alert">{{ error() }}</p> }`,
  styles: [`:host { display: inline-block; min-width: 0; max-width: 100%; } button { min-height: 44px; max-width: 100%; }
    button:not(:disabled) { background: #fff; color: #082b49; border-color: #18ddd2; }
    button:disabled { background: #eef3f6; color: #526573; }
    button:focus-visible { outline: 3px solid #18ddd2; outline-offset: 3px; }
    mat-spinner { display: inline-block; margin-right: 8px; }
    .pdf-error { color: #a32020; background: #fff; border-radius: 8px; padding: .5rem;
      font-size: .875rem; max-width: 24rem; margin: .5rem 0; overflow-wrap: anywhere; }`]
})
export class PdfDownloadComponent {
  @Input({ required: true }) kind!: PdfKind;
  @Input({ required: true }) id!: number;
  private readonly injector = inject(Injector);
  private readonly destroy = inject(DestroyRef);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected download() {
    if (this.busy()) return;
    this.error.set(''); this.busy.set(true);
    try {
      this.injector.get(PdfDownloadService).download(this.kind, this.id).pipe(
        takeUntilDestroyed(this.destroy), finalize(() => this.busy.set(false))
      ).subscribe({ error: (error: unknown) => this.showError(error) });
    } catch {
      this.busy.set(false); this.error.set('Téléchargement impossible. Réessayez.');
    }
  }
  private showError(error: unknown) {
    if (!(error instanceof HttpErrorResponse)) {
      this.error.set('Le serveur n’a pas renvoyé un document PDF valide.'); return;
    }
    const messages: Record<number, string> = {
      0: 'Impossible de contacter le serveur.',
      401: 'Session expirée. Veuillez vous reconnecter.',
      403: "Vous n'êtes pas autorisé à télécharger ce document.",
      404: 'Document introuvable.',
      500: 'Une erreur serveur est survenue.',
      503: 'Le service est temporairement indisponible. Réessayez.'
    };
    this.error.set(messages[error.status] ?? 'Téléchargement impossible. Réessayez.');
  }
}
