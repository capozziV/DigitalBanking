export interface BackendCall {
  method: string;
  url: string;
  headers: Record<string, string>;
  body: unknown;
}

export interface FakeResponse {
  status?: number;
  body?: unknown;
}

type Handler = FakeResponse | ((call: BackendCall) => FakeResponse | Promise<FakeResponse>);

/** Stands in for `fetch` towards the business services: routes are matched on `METHOD full-url`. */
export class FakeBackend {
  readonly calls: BackendCall[] = [];
  private readonly routes = new Map<string, Handler>();

  on(method: string, url: string, handler: Handler): this {
    this.routes.set(`${method} ${url}`, handler);
    return this;
  }

  callsTo(method: string, url: string): BackendCall[] {
    return this.calls.filter((c) => c.method === method && c.url === url);
  }

  readonly fetch = (async (input: string | URL | Request, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? 'GET';
    const call: BackendCall = {
      method,
      url,
      headers: Object.fromEntries(
        Object.entries((init?.headers ?? {}) as Record<string, string>).map(([k, v]) => [
          k.toLowerCase(),
          v,
        ]),
      ),
      body: typeof init?.body === 'string' ? JSON.parse(init.body) : undefined,
    };
    this.calls.push(call);
    const handler = this.routes.get(`${method} ${url}`);
    if (!handler) return json(500, { title: `no fake route for ${method} ${url}` });
    const result = typeof handler === 'function' ? await handler(call) : handler;
    return json(result.status ?? 200, result.body);
  }) as typeof fetch;
}

function json(status: number, body: unknown): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  });
}
