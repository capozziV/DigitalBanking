import type { ProblemDetails } from './problem';

const CSRF_COOKIE = 'bank_csrf';

type Listener = () => void;
const sessionLostListeners = new Set<Listener>();

/** Every failure of a BFF call: `status` 0 means the request never got an answer. */
export class BffError extends Error {
  readonly status: number;
  readonly problem: ProblemDetails | null;

  constructor(status: number, problem: ProblemDetails | null, message?: string) {
    super(message ?? problem?.detail ?? `BFF answered ${status}`);
    this.name = 'BffError';
    this.status = status;
    this.problem = problem;
  }
}

/** Called when a BFF call outside `/bff/auth/` answers 401: the session is gone. */
export function onSessionLost(listener: Listener): () => void {
  sessionLostListeners.add(listener);
  return () => sessionLostListeners.delete(listener);
}

const notifySessionLost = (): void => {
  for (const listener of [...sessionLostListeners]) listener();
};

function readCookie(name: string): string | null {
  for (const part of document.cookie.split(';')) {
    const [key, ...value] = part.trim().split('=');
    if (key === name) return decodeURIComponent(value.join('='));
  }
  return null;
}

function parseBody(text: string): unknown {
  if (!text) return undefined;
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return undefined;
  }
}

export interface BffInit {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  headers?: Record<string, string>;
}

/**
 * Calls the BFF with the session cookie. Non GET calls carry the CSRF header (double submit with
 * the `bank_csrf` cookie). Always throws `BffError`.
 */
export async function bff<T = unknown>(path: `/bff/${string}`, init: BffInit = {}): Promise<T> {
  const method = init.method ?? 'GET';
  const headers: Record<string, string> = { Accept: 'application/json', ...init.headers };
  if (init.body !== undefined) headers['Content-Type'] = 'application/json';
  if (method !== 'GET' && path.startsWith('/bff/')) {
    const token = readCookie(CSRF_COOKIE);
    if (token) headers['X-CSRF-Token'] = token;
  }

  let response: Response;
  try {
    response = await fetch(path, {
      method,
      credentials: 'same-origin',
      headers,
      body: init.body === undefined ? undefined : JSON.stringify(init.body),
    });
  } catch {
    throw new BffError(0, null, 'The BFF could not be reached');
  }

  let text: string;
  try {
    text = await response.text();
  } catch {
    throw new BffError(response.status, null, 'The BFF answer could not be read');
  }
  const body = parseBody(text);
  if (response.ok) return body as T;

  const problem = body !== null && typeof body === 'object' ? (body as ProblemDetails) : null;
  // Auth calls report their own 401 (wrong password, no session yet) on the form itself.
  if (response.status === 401 && path.startsWith('/bff/') && !path.startsWith('/bff/auth/')) {
    notifySessionLost();
  }
  throw new BffError(response.status, problem);
}
