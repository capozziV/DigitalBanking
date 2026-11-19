import { Test } from '@nestjs/testing';
import type { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { AppModule } from './app.module.js';
import { configureApp } from './configure-app.js';

describe('HealthController', () => {
  let app: INestApplication;

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({ imports: [AppModule] }).compile();
    app = moduleRef.createNestApplication();
    configureApp(app);
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  it('reports UP under the /bff prefix', async () => {
    const response = await request(app.getHttpServer()).get('/bff/health').expect(200);
    expect(response.body).toEqual({ status: 'UP' });
  });

  it('does not expose health outside the /bff prefix', async () => {
    await request(app.getHttpServer()).get('/health').expect(404);
  });
});
