import { BadRequestException, Controller, Get, Query, UseGuards } from '@nestjs/common';
import { BackendClient } from '../backend/backend-client.js';
import { CurrentSession, SessionGuard } from '../session/session.guard.js';
import type { Session } from '../session/session.service.js';
import { toAccountView, toEntryView, type AccountDTO, type StatementDTO } from './views.js';

const LATEST_ENTRIES = 5;
const MAX_CURSOR_LENGTH = 1024;

interface DailyLimitDTO {
  limitCents: number;
  usedCents: number;
  availableCents: number;
}

/** "Início" and "Extrato": the screens that read the account, the daily limit and the statement. */
@Controller()
@UseGuards(SessionGuard)
export class HomeController {
  constructor(private readonly backend: BackendClient) {}

  @Get('home')
  async home(@CurrentSession() session: Session) {
    const options = { token: session.accessToken };
    const account = await this.backend.get<AccountDTO>('accounts', '/accounts/me', options);
    // Statement default page (the cached one); the screen shows its first entries.
    const [limit, statement] = await Promise.all([
      this.backend.get<DailyLimitDTO>('transfers', `/accounts/${account.id}/daily-limit`, options),
      this.backend.get<StatementDTO>('statement', `/accounts/${account.id}/statement`, options),
    ]);
    return {
      account: toAccountView(account),
      dailyLimit: {
        limit: limit.limitCents,
        used: limit.usedCents,
        available: limit.availableCents,
      },
      latestEntries: statement.entries.slice(0, LATEST_ENTRIES).map(toEntryView),
    };
  }

  @Get('statement')
  async statement(@CurrentSession() session: Session, @Query('cursor') cursor?: unknown) {
    const options = { token: session.accessToken };
    const account = await this.backend.get<AccountDTO>('accounts', '/accounts/me', options);
    let query = '';
    if (cursor !== undefined && cursor !== '') {
      if (typeof cursor !== 'string' || cursor.length > MAX_CURSOR_LENGTH) {
        throw new BadRequestException({ code: 'invalid_cursor', message: 'Cursor inválido.' });
      }
      query = `?cursor=${encodeURIComponent(cursor)}`;
    }
    const page = await this.backend.get<StatementDTO>(
      'statement',
      `/accounts/${account.id}/statement${query}`,
      options,
    );
    return { entries: page.entries.map(toEntryView), nextCursor: page.nextCursor ?? null };
  }
}
