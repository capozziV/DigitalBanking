// Loaded before the application: `node --import ./dist/observability/tracing.js dist/main.js`, so the
// instrumentations patch `http`, `express` and `ioredis` before anything imports them.
import { startTracing } from './otel.js';

const tracing = startTracing();

for (const signal of ['SIGTERM', 'SIGINT'] as const) {
  process.once(signal, () => {
    void tracing.shutdown().finally(() => process.exit(0));
  });
}
