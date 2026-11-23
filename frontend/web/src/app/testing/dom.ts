import { ComponentFixture } from '@angular/core/testing';

export function setInput(fixture: ComponentFixture<unknown>, name: string, value: string): void {
  const input = (fixture.nativeElement as HTMLElement).querySelector(
    `input[name="${name}"]`,
  ) as HTMLInputElement;
  input.value = value;
  input.dispatchEvent(new Event('input'));
}

export function submitButton(fixture: ComponentFixture<unknown>): HTMLButtonElement {
  return (fixture.nativeElement as HTMLElement).querySelector(
    'button[type="submit"]',
  ) as HTMLButtonElement;
}

export function submit(fixture: ComponentFixture<unknown>): void {
  const form = (fixture.nativeElement as HTMLElement).querySelector('form') as HTMLFormElement;
  form.dispatchEvent(new Event('submit'));
}

export const tick = () => new Promise((resolve) => setTimeout(resolve));
