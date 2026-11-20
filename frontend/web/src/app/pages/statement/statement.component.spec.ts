import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { tick } from '../../testing/dom';
import { StatementComponent } from './statement.component';

const entry = (id: string, kind: 'CREDIT' | 'DEBIT', amount: number, description: string) => ({
  id,
  kind,
  amount,
  description,
  balanceAfter: 1000,
  createdAt: '2026-11-19T12:00:00Z',
});

describe('StatementComponent', () => {
  let fixture: ComponentFixture<StatementComponent>;
  let http: HttpTestingController;
  const el = () => fixture.nativeElement as HTMLElement;
  const loadMore = () =>
    Array.from(el().querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Carregar mais'),
    ) as HTMLButtonElement | undefined;

  async function create(): Promise<void> {
    TestBed.configureTestingModule({
      imports: [StatementComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(StatementComponent);
    await fixture.whenStable();
  }

  async function flush(match: (cursor: string | null) => boolean, body: object): Promise<void> {
    http.expectOne((r) => r.url === '/bff/statement' && match(r.params.get('cursor'))).flush(body);
    await tick();
    await fixture.whenStable();
  }

  afterEach(() => http.verify());

  it('shows "Bônus de boas-vindas" as an entry, with the down arrow', async () => {
    await create();
    await flush((cursor) => cursor === null, {
      entries: [entry('1', 'CREDIT', 10000, 'Bônus de boas-vindas')],
      nextCursor: null,
    });
    const row = el().querySelector('li') as HTMLElement;
    expect(row.textContent).toContain('Bônus de boas-vindas');
    expect(row.querySelector('app-entry-icon')?.textContent).toContain('↓');
    expect(row.querySelector('.amount')?.classList).toContain('in');
  });

  it('loads the next page by the cursor and hides the button on the last one', async () => {
    await create();
    await flush((cursor) => cursor === null, {
      entries: [entry('2', 'DEBIT', 500, 'Transferência enviada')],
      nextCursor: 'abc',
    });
    expect(el().querySelectorAll('li')).toHaveLength(1);
    loadMore()?.click();
    await flush((cursor) => cursor === 'abc', {
      entries: [entry('1', 'CREDIT', 10000, 'Bônus de boas-vindas')],
      nextCursor: null,
    });
    expect(el().querySelectorAll('li')).toHaveLength(2);
    expect(el().querySelectorAll('li')[1].textContent).toContain('Bônus de boas-vindas');
    expect(loadMore()).toBeUndefined();
  });

  it('says so when there are no entries', async () => {
    await create();
    await flush((cursor) => cursor === null, { entries: [], nextCursor: null });
    expect(el().textContent).toContain('Nenhum lançamento ainda');
  });

  it('shows the retry notice when the load fails and tries again', async () => {
    await create();
    http.expectOne('/bff/statement').flush({}, { status: 503, statusText: 'Unavailable' });
    await tick();
    await fixture.whenStable();
    expect(el().textContent).toContain('Não foi possível carregar. Tente de novo.');
    (
      Array.from(el().querySelectorAll('button')).find((b) =>
        b.textContent?.includes('Tentar de novo'),
      ) as HTMLButtonElement
    ).click();
    await flush((cursor) => cursor === null, {
      entries: [entry('1', 'CREDIT', 100, 'x')],
      nextCursor: null,
    });
    expect(el().querySelectorAll('li')).toHaveLength(1);
  });
});
