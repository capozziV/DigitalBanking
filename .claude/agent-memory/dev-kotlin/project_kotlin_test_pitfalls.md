---
name: kotlin-test-pitfalls
description: Compile and integration-test traps hit in this repo - nested KDoc comments, backtick test names, Apache HttpClient retrying 503 in test clients
metadata:
  type: project
---

- KDoc that contains `/admin/**` or any `/**` opens a nested comment in Kotlin. The result is "Unclosed comment" at end of file. Write `/admin/` instead.
- Backtick test names cannot contain `:` ("Name contains illegal characters").
- Apache HttpClient 5 sits on the integration-test classpath (accounts, from transitive deps). A `RestClient.builder()` without a request factory picks it, and it auto-retries 503 after 1 s (`HttpRequestRetryExec`). That sends duplicate requests and breaks timing and "nothing written" assertions. Test clients pin `JdkClientHttpRequestFactory`.

**Why:** each one cost a debug round in etapa 7 task 5 (2026-11-13).
**How to apply:** check these first when a test client sees doubled calls or the compiler fails with odd syntax errors.
