---
name: kdoc-glob-nested-comment
description: Kotlin KDoc/comments containing a route glob like `/internal/**` open a nested comment and fail compilation with "Unclosed comment"; also libs must be top-level dirs for the CI loop
metadata:
  type: project
---

Kotlin supports nested block comments, so writing a route glob such as `/internal/**` or `/admin/**` inside a `/** ... */` KDoc opens a new comment and compilation fails with `Syntax error: Unclosed comment` at the end of the file. Write "the internal routes" instead, or keep globs in `//` line comments.

Related: the CI job for shared libs runs `for dir in libs/*/` → `:libs:<name>:build`, so a nested subproject (e.g. `libs/security/caller`) never has its tests or ktlint run. New libs must be top-level dirs under `libs/` (e.g. `libs/security-caller`).

**Why:** both cost a review round in stage 7, Task 1 (2026-11-13).

**How to apply:** avoid `/**` sequences in KDoc; create every new Gradle lib as `libs/<name>` and check with `--dry-run` that its `test` task is in the CI task list.
