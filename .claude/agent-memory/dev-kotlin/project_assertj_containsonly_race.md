---
name: assertj-containsonly-race
description: AssertJ containsOnly requires every listed value to appear; in concurrency tests asserting allowed HTTP statuses use isSubsetOf, since CI runners serialize requests
metadata:
  type: project
---

`assertThat(list).containsOnly(200, 409)` fails when no 409 appears: it means "exactly these values, all present". To say "only these values are allowed", use `isSubsetOf(200, 409)`.

**Why:** PR #193 CI went red on `TransferAdminApiIntegrationTest` (concurrent manual compensations). The GitHub runner (2 vCPU) ran the 8 requests one after another, so every one answered 200 (repeated resolution). On the 8-core Mac there is always contention, so it passed 40/40 locally. Reproduce such flakes by forcing a 1-thread pool, not by repeating locally.

**How to apply:** in any race test that asserts a set of allowed outcomes, check that the assertion accepts the fully serialized interleaving. Prove it with a pool of 1 thread.
