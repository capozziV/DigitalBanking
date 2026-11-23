---
name: docker-desktop-down
description: Integration tests failing en masse with NoClassDefFoundError/DockerClientProviderStrategy mean Docker Desktop is stopped, not a code bug
metadata:
  type: project
---

When most `integrationTest` cases fail with `NoClassDefFoundError ... Caused by: ExceptionInInitializerError at DockerClientProviderStrategy.java`, Docker Desktop is not running (`docker version` shows no server, `~/.docker/run/docker.sock` missing). `open -a Docker` starts it; ready in under a minute.

**Why:** cost a full build run in stage 7 Task 6 (136 of 168 integration tests red, all from this).

**How to apply:** before the first `./gradlew ...build` with integration tests, run `docker version`; if the server is missing, start Docker Desktop first. Related: [[kotlin-test-pitfalls]].
