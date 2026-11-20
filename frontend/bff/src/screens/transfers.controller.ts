import {
  BadRequestException,
  Body,
  Controller,
  Get,
  Headers,
  NotFoundException,
  Param,
  Post,
  Query,
  UnprocessableEntityException,
  UseGuards,
} from '@nestjs/common';
import { BackendClient, BackendProblemError } from '../backend/backend-client.js';
import { CurrentSession, SessionGuard } from '../session/session.guard.js';
import type { Session } from '../session/session.service.js';
import { normalizePhone } from './phone.js';
import { RecipientTokenService } from './recipient-token.js';
import { UUID, isNotFound, type AccountDTO, type CustomerDTO, type TransferDTO } from './views.js';

const MAX_IDEMPOTENCY_KEY_LENGTH = 255;

/** Reasons the saga records (`StepOutcome` of the transfers service), in the words of the screen. */
const REASONS: Record<string, string> = {
  'daily-limit-exceeded': 'Você atingiu o limite diário de transferências.',
  'daily-limit-unavailable': 'Não foi possível conferir o limite diário agora. Tente de novo.',
  'insufficient-available-balance': 'Saldo insuficiente para esta transferência.',
  'entry-not-found': 'A transferência não pôde ser concluída.',
  'debit-not-confirmed':
    'A transferência não pôde ser concluída e o valor voltou para a sua conta.',
  'credit-not-confirmed':
    'A transferência não pôde ser concluída e o valor voltou para a sua conta.',
};
/** 422 problems of the transfers service that carry the id of the FAILED transfer. */
const REFUSAL_TYPES = new Set([
  '/problems/transfer-failed',
  '/problems/insufficient-available-balance',
]);
const GENERIC_REASON = 'A transferência não pôde ser concluída.';

interface RecipientLookupDTO {
  customerId: string;
  fullName: string;
}

/** "Transferir": find the recipient by phone, send, follow the status. */
@Controller('transfers')
@UseGuards(SessionGuard)
export class TransfersController {
  constructor(
    private readonly backend: BackendClient,
    private readonly tokens: RecipientTokenService,
  ) {}

  // Declared before `:id` so that "recipient" is not read as an id.
  @Get('recipient')
  async recipient(@CurrentSession() session: Session, @Query('phone') phone?: unknown) {
    const e164 = normalizePhone(phone);
    const options = { token: session.accessToken };
    const [found, me] = await Promise.all([
      this.backend
        .get<RecipientLookupDTO>(
          'customer',
          `/customers/lookup?phone=${encodeURIComponent(e164)}`,
          options,
        )
        .catch((error: unknown) => {
          if (isNotFound(error)) {
            throw new NotFoundException({
              code: 'recipient_not_found',
              message: 'Não encontramos ninguém com esse telefone.',
            });
          }
          throw error;
        }),
      this.backend.get<CustomerDTO>('customer', '/customers/me', options),
    ]);
    if (found.customerId === me.id) {
      throw new UnprocessableEntityException({
        code: 'self_transfer',
        message: 'Não é possível transferir para você mesmo.',
      });
    }
    const account = await this.backend
      .get<{ accountId: string }>(
        'accounts',
        `/accounts/recipient?customerId=${encodeURIComponent(found.customerId)}`,
        options,
      )
      .catch((error: unknown) => {
        if (isNotFound(error)) {
          throw new NotFoundException({
            code: 'recipient_without_account',
            message: 'Este telefone não tem conta para receber.',
          });
        }
        throw error;
      });
    return {
      recipientName: found.fullName,
      recipientToken: await this.tokens.issue(account.accountId, found.fullName, session.id),
    };
  }

  @Post()
  async create(
    @CurrentSession() session: Session,
    @Body() body: Record<string, unknown> | undefined,
    @Headers('idempotency-key') idempotencyKey?: string,
  ) {
    if (
      typeof idempotencyKey !== 'string' ||
      idempotencyKey.trim() === '' ||
      idempotencyKey.length > MAX_IDEMPOTENCY_KEY_LENGTH
    ) {
      throw new BadRequestException({
        code: 'idempotency_key_required',
        message: 'Chave de idempotência ausente ou inválida.',
      });
    }
    const amount = body?.['amount'];
    if (typeof amount !== 'number' || !Number.isSafeInteger(amount) || amount <= 0) {
      throw new BadRequestException({
        code: 'invalid_amount',
        message: 'Informe um valor maior que zero.',
      });
    }
    const destinationAccountId = await this.tokens.resolve(body?.['recipientToken'], session.id);
    if (!destinationAccountId) {
      throw new BadRequestException({
        code: 'invalid_recipient',
        message: 'Destinatário expirado. Busque o telefone de novo.',
      });
    }
    const options = { token: session.accessToken };
    const source = await this.backend.get<AccountDTO>('accounts', '/accounts/me', options);
    if (source.id === destinationAccountId) {
      throw new UnprocessableEntityException({
        code: 'self_transfer',
        message: 'Não é possível transferir para você mesmo.',
      });
    }
    try {
      const transfer = await this.backend.post<TransferDTO>(
        'transfers',
        '/transfers',
        { sourceAccountId: source.id, destinationAccountId, amount },
        { ...options, idempotencyKey },
      );
      return { id: transfer.id, status: transfer.status };
    } catch (error) {
      // The saga refused it before taking any money (e.g. daily limit): the service answers 422 with the id of the
      // FAILED transfer. The screen follows it like any other, and shows the reason of the status.
      if (
        error instanceof BackendProblemError &&
        error.status === 422 &&
        REFUSAL_TYPES.has(error.problem.type ?? '') &&
        typeof error.problem['transferId'] === 'string' &&
        UUID.test(error.problem['transferId'])
      ) {
        return { id: error.problem['transferId'], status: 'FAILED' };
      }
      throw error;
    }
  }

  @Get(':id')
  async status(@CurrentSession() session: Session, @Param('id') id: string) {
    if (!UUID.test(id)) {
      throw new BadRequestException({ code: 'invalid_id', message: 'Transferência inválida.' });
    }
    const transfer = await this.backend.get<TransferDTO>('transfers', `/transfers/${id}`, {
      token: session.accessToken,
    });
    const code = transfer.failureReason ?? null;
    return {
      id: transfer.id,
      status: transfer.status,
      reason: code === null ? null : (REASONS[code] ?? GENERIC_REASON),
      reasonCode: code,
    };
  }
}
