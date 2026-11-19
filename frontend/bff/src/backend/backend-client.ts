import { Inject, Injectable } from '@nestjs/common';
import { APP_CONFIG, type AppConfig, type BackendService } from '../config/config.js';
import { CORRELATION_HEADER, currentCorrelationId } from '../observability/correlation.js';

export const FETCH = Symbol('FETCH');
const TIMEOUT_MS = 10_000;

export interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  [key: string]: unknown;
}

export class BackendProblemError extends Error {
  constructor(
    readonly status: number,
    readonly problem: ProblemDetails,
  ) {
    super(`backend answered ${status}${problem.type ? ` (${problem.type})` : ''}`);
    this.name = 'BackendProblemError';
  }
}

export class BackendUnavailableError extends Error {
  constructor(service: string) {
    super(`${service} is unavailable`);
    this.name = 'BackendUnavailableError';
  }
}

export interface CallOptions {
  token: string;
  idempotencyKey?: string;
}

@Injectable()
export class BackendClient {
  constructor(
    @Inject(APP_CONFIG) private readonly config: AppConfig,
    @Inject(FETCH) private readonly fetchFn: typeof fetch,
  ) {}

  get<T = unknown>(service: BackendService, path: string, options: CallOptions): Promise<T> {
    return this.request<T>(service, 'GET', path, undefined, options);
  }

  post<T = unknown>(
    service: BackendService,
    path: string,
    body: unknown,
    options: CallOptions,
  ): Promise<T> {
    return this.request<T>(service, 'POST', path, body, options);
  }

  put<T = unknown>(
    service: BackendService,
    path: string,
    body: unknown,
    options: CallOptions,
  ): Promise<T> {
    return this.request<T>(service, 'PUT', path, body, options);
  }

  private async request<T>(
    service: BackendService,
    method: string,
    path: string,
    body: unknown,
    options: CallOptions,
  ): Promise<T> {
    const headers: Record<string, string> = {
      Authorization: `Bearer ${options.token}`,
      Accept: 'application/json',
    };
    const correlationId = currentCorrelationId();
    if (correlationId) headers[CORRELATION_HEADER] = correlationId;
    if (options.idempotencyKey) headers['Idempotency-Key'] = options.idempotencyKey;
    if (body !== undefined) headers['Content-Type'] = 'application/json';

    let response: Response;
    try {
      response = await this.fetchFn(`${this.config.services[service]}${path}`, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
        signal: AbortSignal.timeout(TIMEOUT_MS),
      });
    } catch {
      throw new BackendUnavailableError(service);
    }

    if (!response.ok)
      throw new BackendProblemError(response.status, await this.problemOf(response));
    if (response.status === 204) return undefined as T;
    const text = await response.text();
    return (text ? JSON.parse(text) : undefined) as T;
  }

  private async problemOf(response: Response): Promise<ProblemDetails> {
    try {
      const parsed = (await response.json()) as ProblemDetails;
      return { ...parsed, status: parsed.status ?? response.status };
    } catch {
      return { title: response.statusText || 'Backend error', status: response.status };
    }
  }
}
