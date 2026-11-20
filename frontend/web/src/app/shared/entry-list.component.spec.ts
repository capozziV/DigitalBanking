import { TestBed } from '@angular/core/testing';
import { EntryListComponent } from './entry-list.component';

describe('EntryListComponent', () => {
  const entries = [
    {
      id: '1',
      kind: 'CREDIT' as const,
      amount: 10000,
      description: 'Bônus de boas-vindas',
      balanceAfter: 10000,
      createdAt: '2026-11-19T12:00:00Z',
    },
    {
      id: '2',
      kind: 'DEBIT' as const,
      amount: 2500,
      description: 'Transferência enviada',
      balanceAfter: 7500,
      createdAt: '2026-11-19T13:00:00Z',
    },
  ];

  function render() {
    const fixture = TestBed.createComponent(EntryListComponent);
    fixture.componentRef.setInput('entries', entries);
    return fixture;
  }

  it('shows the welcome bonus as an entry (down arrow, bold amount)', async () => {
    const fixture = render();
    await fixture.whenStable();
    const row = (fixture.nativeElement as HTMLElement).querySelectorAll('li')[0];
    expect(row.textContent).toContain('Bônus de boas-vindas');
    expect(row.querySelector('app-entry-icon')?.textContent).toContain('↓');
    expect(row.querySelector('.amount')?.classList).toContain('in');
    expect(row.querySelector('.amount')?.textContent).toContain('R$ 100,00');
  });

  it('shows a debit as an exit (up arrow, grey amount)', async () => {
    const fixture = render();
    await fixture.whenStable();
    const row = (fixture.nativeElement as HTMLElement).querySelectorAll('li')[1];
    expect(row.querySelector('app-entry-icon')?.textContent).toContain('↑');
    expect(row.querySelector('.amount')?.classList).toContain('out');
    expect(row.querySelector('.amount')?.textContent).toContain('R$ 25,00');
  });
});
