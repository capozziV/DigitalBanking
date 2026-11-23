import { TestBed } from '@angular/core/testing';
import { TransferAttemptStore } from './transfer-attempt.store';

describe('TransferAttemptStore', () => {
  const intent = { owner: 'ana@example.com', phone: '11987654321', amount: 2500 };
  let store: TransferAttemptStore;

  beforeEach(() => {
    store = TestBed.inject(TransferAttemptStore);
  });
  afterEach(() => sessionStorage.clear());

  it('says whether the key already existed', async () => {
    const first = await store.open(intent);
    expect(first.resumed).toBe(false);
    const again = await store.open(intent);
    expect(again.resumed).toBe(true);
    expect(again.idempotencyKey).toBe(first.idempotencyKey);
  });

  it('a late settle of an older attempt does not drop the key of a newer one', async () => {
    const older = await store.open(intent);
    store.settle(older);
    const newer = await store.open(intent);
    expect(newer.resumed).toBe(false);
    store.settle(older);
    const again = await store.open(intent);
    expect(again.idempotencyKey).toBe(newer.idempotencyKey);
    expect(again.resumed).toBe(true);
  });
});
