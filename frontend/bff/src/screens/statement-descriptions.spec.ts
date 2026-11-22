import { IDS, URLS } from '../testing/bodies.js';
import { createTestApp, type SignedIn, type TestApp } from '../testing/test-app.js';

const ANY_UUID = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i;

describe('descriptions of the statement entries', () => {
  let t: TestApp;
  let user: SignedIn;

  beforeEach(async () => {
    t = await createTestApp();
    user = await t.signIn();
  });
  afterEach(() => t.close());

  function given(kind: string, description: string | null) {
    t.backend
      .on('GET', `${URLS.accounts}/accounts/me`, { body: { id: IDS.account, type: 'FREE' } })
      .on('GET', `${URLS.transfers}/accounts/${IDS.account}/daily-limit`, {
        body: { day: '2026-11-22', limitCents: 500000, usedCents: 0, availableCents: 500000 },
      })
      .on('GET', `${URLS.statement}/accounts/${IDS.account}/statement`, {
        body: {
          accountId: IDS.account,
          entries: [
            {
              entryId: '77777777-7777-4777-8777-777777777777',
              sequence: 2,
              kind,
              amount: 2500,
              balanceAfter: 7500,
              description,
              createdAt: '2026-11-22T10:00:00Z',
            },
          ],
          nextCursor: null,
        },
      });
  }

  describe.each([
    ['/bff/home', 'latestEntries'],
    ['/bff/statement', 'entries'],
  ])('GET %s', (path, field) => {
    it.each([
      // The three strings of TransferStep (transfers service), and the bonus of OpenAccountUseCase.
      ['DEBIT', `Transfer to ${IDS.otherAccount}`, 'Transferência enviada'],
      ['CREDIT', `Transfer from ${IDS.otherAccount}`, 'Transferência recebida'],
      ['CREDIT', `Transfer ${IDS.transfer} compensation`, 'Transferência devolvida'],
      ['CREDIT', 'Bônus de boas-vindas', 'Bônus de boas-vindas'],
      ['CREDIT', 'seed saldo inicial', 'seed saldo inicial'],
      ['CREDIT', null, null],
      // Any other text with an id falls back to the kind of the entry.
      ['CREDIT', `manual credit of ${IDS.otherAccount}`, 'Crédito'],
      ['DEBIT', `adjustment ${IDS.otherAccount.toUpperCase()}`, 'Débito'],
      ['DEBIT', `Transfer to ${IDS.otherAccount} (retry)`, 'Débito'],
    ])('shows the %s entry "%s" as "%s"', async (kind, stored, shown) => {
      given(kind as string, stored);
      const response = await user.get(path).expect(200);
      const [entry] = response.body[field];
      expect(entry.description).toBe(shown);
      expect(entry.description ?? '').not.toMatch(ANY_UUID);
      expect(response.text).not.toContain(IDS.otherAccount);
    });
  });
});
