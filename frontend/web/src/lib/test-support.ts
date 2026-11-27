import { vi } from 'vitest';

/** One request made through `fetch`, answered by the test at the moment it chooses. */
export interface FakeCall {
  readonly path: string;
  readonly method: string;
  readonly headers: Headers;
  readonly credentials: RequestCredentials | undefined;
  readonly body: unknown;
  answered: boolean;
  respond(status: number, body?: unknown): void;
  fail(): void;
}

/** A controllable `fetch`: every call waits until the test answers it. */
export class FakeBff {
  readonly calls: FakeCall[] = [];

  install(): this {
    vi.stubGlobal(
      'fetch',
      vi.fn(
        (input: string, init: RequestInit = {}) =>
          new Promise<Response>((resolve, reject) => {
            const call: FakeCall = {
              path: input,
              method: init.method ?? 'GET',
              headers: new Headers(init.headers),
              credentials: init.credentials,
              body: typeof init.body === 'string' ? JSON.parse(init.body) : undefined,
              answered: false,
              respond: (status, body) => {
                call.answered = true;
                const noBody = body === undefined || status === 204;
                resolve(
                  new Response(noBody ? null : JSON.stringify(body), {
                    status,
                    headers: { 'Content-Type': 'application/json' },
                  }),
                );
              },
              fail: () => {
                call.answered = true;
                reject(new TypeError('Failed to fetch'));
              },
            };
            this.calls.push(call);
          }),
      ),
    );
    return this;
  }

  /** The calls to `path` that nobody answered yet, oldest first. */
  pending(path: string): FakeCall[] {
    return this.calls.filter((call) => call.path === path && !call.answered);
  }

  /** The one unanswered call to `path`; fails the test when there is not exactly one. */
  only(path: string): FakeCall {
    const found = this.pending(path);
    if (found.length !== 1) {
      throw new Error(`expected one pending call to ${path}, found ${found.length}`);
    }
    return found[0];
  }
}

/** Lets the promise callbacks queued so far run. */
export const tick = (): Promise<void> => new Promise((resolve) => setTimeout(resolve));
