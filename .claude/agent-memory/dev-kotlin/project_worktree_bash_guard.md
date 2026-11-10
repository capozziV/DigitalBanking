---
name: worktree-bash-guard
description: In worktree-isolated runs, compound Bash commands (heredoc python + other commands, docker --format templates, loops with variables) are refused; split them
metadata:
  type: project
---

When running as an agent isolated in `.claude/worktrees/…`, the Bash guard refuses commands it cannot prove avoid git outside the worktree: `python3 - <<EOF` heredocs combined with other commands, `for` loops with runtime variables passed to unzip, `docker … {{.X}}` templates, and some `cd && python3 heredoc` chains. A refused compound command runs NOTHING (no partial effect), so check later whether an edit really happened.

**Why:** cost several silent no-op edits in Task A5 (stage 3), noticed only via `git status`.

**How to apply:** prefer the Edit/Write tools for file changes; keep Bash to one plain command per call; verify with `git status --short` after any scripted edit.
