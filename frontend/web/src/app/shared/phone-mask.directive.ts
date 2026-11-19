import { Directive, ElementRef, HostListener, inject } from '@angular/core';
import { NgControl } from '@angular/forms';
import { formatPhone } from './phone';

/** Masks a phone input as `(11) 98765-4321` while typing. */
@Directive({ selector: 'input[appPhoneMask]' })
export class PhoneMaskDirective {
  private readonly element = inject<ElementRef<HTMLInputElement>>(ElementRef);
  private readonly control = inject(NgControl, { self: true });

  @HostListener('input')
  protected onInput(): void {
    const masked = formatPhone(this.element.nativeElement.value);
    this.element.nativeElement.value = masked;
    this.control.control?.setValue(masked, { emitEvent: true });
  }
}
