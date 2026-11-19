import { Pipe, PipeTransform } from '@angular/core';

/** Formats an integer amount in cents as reais, for display only (`123456` -> `R$ 1.234,56`). */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  transform(cents: number | null | undefined): string {
    if (cents === null || cents === undefined || !Number.isSafeInteger(cents)) {
      return '—';
    }
    const sign = cents < 0 ? '-' : '';
    const absolute = Math.abs(cents);
    const reais = Math.floor(absolute / 100)
      .toString()
      .replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    const rest = (absolute % 100).toString().padStart(2, '0');
    return `${sign}R$ ${reais},${rest}`;
  }
}
