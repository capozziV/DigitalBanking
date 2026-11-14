---
name: local-scripts-pitfalls
description: Pitfalls of smoke/seed shell scripts and Bruno collections (bash 3.2 errexit, bru sandbox, one account per customer)
metadata:
  type: project
---

Pitfalls found while putting tokens into `infra/local/smoke.sh`, `seed.sh` and `api/bruno/` (etapa 7, 2026-11-14):

- macOS runs `#!/usr/bin/env bash` as bash 3.2: no `inherit_errexit`, so `set -e` does not apply inside a function called through `$(...)`, and a failure inside `< <(...)` is silent. Use `|| exit 1` inside such functions and call them as `x=$(f)`.
- `bru` 4.3.0 script sandbox has no `URLSearchParams`; build form bodies by hand with `encodeURIComponent`. `collection.bru` scripts and `auth: inherit` work in the CLI.
- `accounts` allows one account per customer (`customer_id UNIQUE`, `409 customer-already-has-account`, stage 1). The stage 7 spec asked for two owner accounts in the seed; this conflict was reported as a pending decision.
- `uuidgen` on macOS prints uppercase; lowercase it before using it as a `sub`.

**Why:** each one cost a failed run that looked green or a confusing error.
**How to apply:** check these when editing local scripts or Bruno collections; see also [[worktree-bash-guard]].
