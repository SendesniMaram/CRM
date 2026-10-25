import { Pipe, PipeTransform } from '@angular/core';
@Pipe({ name: 'invoiceAmount', standalone: true })
export class InvoiceAmountPipe implements PipeTransform {
  private readonly formatter = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  transform(value: number | null | undefined): string {
    return typeof value === 'number' && Number.isFinite(value) ? this.formatter.format(value) : '—';
  }
}
