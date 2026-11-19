import type { INestApplication } from '@nestjs/common';

export const GLOBAL_PREFIX = 'bff';

export function configureApp(app: INestApplication): void {
  app.setGlobalPrefix(GLOBAL_PREFIX);
}
