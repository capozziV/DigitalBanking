import {
  BadRequestException,
  Body,
  Controller,
  Get,
  HttpCode,
  Post,
  Query,
  UseGuards,
} from '@nestjs/common';
import { BackendClient } from '../backend/backend-client.js';
import { CurrentSession, SessionGuard } from '../session/session.guard.js';
import type { Session } from '../session/session.service.js';
import { UUID } from './views.js';

const MAX_CURSOR_LENGTH = 1024;

interface NotificationDTO {
  id: string;
  kind: string;
  title: string;
  body: string;
  createdAt: string;
  readAt: string | null;
}

interface NotificationPageDTO {
  items: NotificationDTO[];
  unreadCount: number;
  nextCursor: string | null;
}

/** "Avisos": the caller's notifications, always taken from the token (never a customer id from the browser). */
@Controller('notifications')
@UseGuards(SessionGuard)
export class NotificationsController {
  constructor(private readonly backend: BackendClient) {}

  @Get()
  async list(@CurrentSession() session: Session, @Query('cursor') cursor?: unknown) {
    let query = '';
    if (cursor !== undefined && cursor !== '') {
      if (typeof cursor !== 'string' || cursor.length > MAX_CURSOR_LENGTH) {
        throw new BadRequestException({ code: 'invalid_cursor', message: 'Cursor inválido.' });
      }
      query = `?cursor=${encodeURIComponent(cursor)}`;
    }
    const page = await this.backend.get<NotificationPageDTO>(
      'notification',
      `/notifications${query}`,
      { token: session.accessToken },
    );
    // Whitelist: the event `data` carries account ids, which the browser must never see.
    return {
      items: page.items.map(({ id, kind, title, body, createdAt, readAt }) => ({
        id,
        kind,
        title,
        body,
        createdAt,
        readAt,
      })),
      unreadCount: page.unreadCount,
      nextCursor: page.nextCursor,
    };
  }

  @Get('unread-count')
  async unreadCount(@CurrentSession() session: Session) {
    const page = await this.backend.get<NotificationPageDTO>('notification', '/notifications', {
      token: session.accessToken,
    });
    return { count: page.unreadCount };
  }

  /** Marks as read up to `upTo`, or up to the newest notification when the body has none. */
  @Post('read')
  @HttpCode(200)
  async read(
    @CurrentSession() session: Session,
    @Body() body: Record<string, unknown> | undefined,
  ) {
    const options = { token: session.accessToken };
    let upTo = body?.['upTo'];
    if (upTo === undefined || upTo === null) {
      const page = await this.backend.get<NotificationPageDTO>(
        'notification',
        '/notifications',
        options,
      );
      upTo = page.items[0]?.id;
      if (upTo === undefined) return { marked: 0 };
    }
    if (typeof upTo !== 'string' || !UUID.test(upTo)) {
      throw new BadRequestException({ code: 'invalid_id', message: 'Aviso inválido.' });
    }
    return this.backend.post<{ marked: number }>(
      'notification',
      '/notifications/read',
      { upTo },
      options,
    );
  }
}
