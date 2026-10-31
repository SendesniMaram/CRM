import { PdfDownloadComponent } from '../../shared/pdf-download/pdf-download.component';
import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { EMPTY, catchError, finalize, forkJoin, switchMap, tap } from 'rxjs';
import { InvoiceAmountPipe } from './invoice-amount.pipe';
import { InvoiceResponse } from './invoice.models';
import { InvoiceService } from './invoice.service';
@Component({
  selector: 'app-invoice-detail', standalone: true,
  imports: [PdfDownloadComponent, DatePipe, InvoiceAmountPipe, RouterLink, ReactiveFormsModule, MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule, MatFormFieldModule, MatInputModule],
  templateUrl: './invoice-detail.component.html', styleUrl: './invoices.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InvoiceDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(InvoiceService);
  private readonly destroy = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly snack = inject(MatSnackBar);
  protected readonly invoice = signal<InvoiceResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly paymentSummary = signal<import('./invoice.models').InvoicePaymentSummary | null>(null);
  protected readonly reminders = signal<import('./invoice.models').InvoiceReminder[]>([]);
  protected readonly paymentError = signal('');
  protected readonly paymentBusy = signal(false);
  protected readonly reminderBusy = signal(false);
  protected readonly historyLoading = signal(false);
  protected readonly reminderNotes = this.fb.nonNullable.control('');
  protected readonly paymentForm = this.fb.nonNullable.group({ amount: [0, [Validators.required, Validators.min(0.01)]], paidAt: [new Date().toISOString().slice(0, 10), Validators.required], method: [''], reference: [''], notes: [''] });
  ngOnInit() {
    this.route.paramMap.pipe(switchMap(params => {
      this.invoice.set(null); this.error.set(''); this.loading.set(false);
      const id = Number(params.get('id'));
      if (!Number.isSafeInteger(id) || id <= 0) { this.error.set('Facture introuvable.'); return EMPTY; }
      this.loading.set(true);
      return this.service.get(id).pipe(tap(row => { this.invoice.set(row); this.loadFinancialHistory(id); }), catchError((e: { status?: number }) => {
        this.error.set(e.status === 404 ? 'Facture introuvable.' : 'Impossible de charger la facture. Réessayez.'); return EMPTY;
      }), finalize(() => this.loading.set(false)));
    }), takeUntilDestroyed(this.destroy)).subscribe();
  }
  protected loadFinancialHistory(id: number) {
    this.historyLoading.set(true);
    forkJoin({ payments: this.service.payments(id), reminders: this.service.reminders(id) }).pipe(
      takeUntilDestroyed(this.destroy), finalize(() => this.historyLoading.set(false))).subscribe({
        next: result => { this.paymentError.set(''); this.paymentSummary.set(result.payments); this.reminders.set(result.reminders); },
        error: () => this.paymentError.set('Historique des paiements et relances indisponible.')
      });
  }
  protected recordPayment() {
    const invoice = this.invoice(); if (!invoice || this.paymentForm.invalid || this.paymentBusy()) { this.paymentForm.markAllAsTouched(); return; }
    const value = this.paymentForm.getRawValue();
    this.paymentBusy.set(true);
    this.service.recordPayment(invoice.id, { amount: value.amount, paidAt: value.paidAt, method: value.method.trim() || null, reference: value.reference.trim() || null, notes: value.notes.trim() || null }).pipe(
      takeUntilDestroyed(this.destroy), finalize(() => this.paymentBusy.set(false))).subscribe({
        next: () => { this.paymentError.set(''); this.paymentForm.controls.amount.setValue(0); this.loadFinancialHistory(invoice.id); this.snack.open('Paiement enregistré.', 'Fermer', { duration: 4000 }); },
        error: (e: { status?: number }) => this.paymentError.set(e.status === 409 ? 'Paiement refusé : il dépasse le solde restant ou la facture est déjà réglée.' : e.status === 400 ? 'Paiement refusé : vérifiez le montant et les champs.' : 'Impossible d’enregistrer le paiement.')
      });
  }
  protected recordReminder() {
    const invoice = this.invoice(); if (!invoice || this.reminderBusy()) return;
    this.reminderBusy.set(true);
    this.service.recordReminder(invoice.id, { channel: 'MANUAL', notes: this.reminderNotes.value.trim() || null }).pipe(
      takeUntilDestroyed(this.destroy), finalize(() => this.reminderBusy.set(false))).subscribe({
        next: () => { this.reminderNotes.setValue(''); this.loadFinancialHistory(invoice.id); this.snack.open('Relance consignée dans l’historique.', 'Fermer', { duration: 4000 }); },
        error: () => this.paymentError.set('Impossible de consigner la relance.')
      });
  }
}
