import { BadRequestException, Body, Controller, Get, Post, Put, UseGuards } from '@nestjs/common';
import { randomUUID } from 'node:crypto';
import { BackendClient } from '../backend/backend-client.js';
import { CurrentSession, SessionGuard } from '../session/session.guard.js';
import type { Session } from '../session/session.service.js';
import { normalizePhone } from './phone.js';
import { orNull, toAccountView, type AccountDTO, type CustomerDTO } from './views.js';

const MAX_NAME_LENGTH = 120;

function readName(body: Record<string, unknown> | undefined): string {
  const name = body?.['name'];
  if (typeof name !== 'string' || name.trim() === '' || name.trim().length > MAX_NAME_LENGTH) {
    throw new BadRequestException({
      code: 'invalid_name',
      message: `Informe seu nome completo (até ${MAX_NAME_LENGTH} caracteres).`,
    });
  }
  return name.trim();
}

const customerView = (customer: CustomerDTO) => ({
  name: customer.name,
  email: customer.email,
  phone: customer.phone,
});

/** Customer profile and account lifecycle: what "Completar cadastro", "Minha conta" and the first access need. */
@Controller()
@UseGuards(SessionGuard)
export class ProfileController {
  constructor(private readonly backend: BackendClient) {}

  @Get('me')
  async me(@CurrentSession() session: Session) {
    const options = { token: session.accessToken };
    const [customer, account] = await Promise.all([
      orNull(this.backend.get<CustomerDTO>('customer', '/customers/me', options)),
      orNull(this.backend.get<AccountDTO>('accounts', '/accounts/me', options)),
    ]);
    return {
      customer: customer ? customerView(customer) : null,
      account: account ? toAccountView(account) : null,
    };
  }

  @Post('customers')
  async register(
    @CurrentSession() session: Session,
    @Body() body: Record<string, unknown> | undefined,
  ) {
    const name = readName(body);
    const phone = normalizePhone(body?.['phone']);
    const customer = await this.backend.post<CustomerDTO>(
      'customer',
      '/customers',
      { name, email: session.email, phone },
      { token: session.accessToken },
    );
    return customerView(customer);
  }

  @Put('me/phone')
  async changePhone(
    @CurrentSession() session: Session,
    @Body() body: Record<string, unknown> | undefined,
  ) {
    const phone = normalizePhone(body?.['phone']);
    const customer = await this.backend.put<CustomerDTO>(
      'customer',
      '/customers/me/phone',
      { phone },
      { token: session.accessToken },
    );
    return customerView(customer);
  }

  @Post('accounts')
  async openAccount(@CurrentSession() session: Session) {
    // A key per request: the backend allows a single open account, so a second click is answered with a conflict.
    const account = await this.backend.post<AccountDTO>('accounts', '/accounts', undefined, {
      token: session.accessToken,
      idempotencyKey: randomUUID(),
    });
    return toAccountView(account);
  }

  @Post('accounts/close')
  async closeAccount(@CurrentSession() session: Session) {
    const options = { token: session.accessToken };
    const own = await this.backend.get<AccountDTO>('accounts', '/accounts/me', options);
    const closed = await this.backend.post<AccountDTO>(
      'accounts',
      `/accounts/${own.id}/close`,
      undefined,
      options,
    );
    return toAccountView(closed);
  }
}
