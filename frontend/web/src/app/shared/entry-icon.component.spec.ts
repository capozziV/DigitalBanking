import { TestBed } from '@angular/core/testing';
import { EntryIconComponent } from './entry-icon.component';

describe('EntryIconComponent', () => {
  async function render(kind: 'CREDIT' | 'DEBIT') {
    const fixture = TestBed.createComponent(EntryIconComponent);
    fixture.componentRef.setInput('kind', kind);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows a filled circle with a down arrow for a credit', async () => {
    const el = await render('CREDIT');
    const icon = el.querySelector('.icon') as HTMLElement;
    expect(icon.textContent?.trim()).toBe('↓');
    expect(icon.classList).toContain('filled');
    expect(icon.getAttribute('aria-label')).toBe('Entrada');
  });

  it('shows a hollow circle with an up arrow for a debit', async () => {
    const el = await render('DEBIT');
    const icon = el.querySelector('.icon') as HTMLElement;
    expect(icon.textContent?.trim()).toBe('↑');
    expect(icon.classList).toContain('hollow');
    expect(icon.getAttribute('aria-label')).toBe('Saída');
  });
});
