---
name: dynamic-property-source-override
description: A subclass @DynamicPropertySource cannot override a key the base IT class registers (superclass wins); use placeholder indirection keys
metadata:
  type: project
---

In Spring Boot 4 / Framework 7 integration tests, when the abstract base (e.g. `AccountsIntegrationTest`) and a subclass both register the same key with `@DynamicPropertySource`, the **superclass value wins** (methods are collected subclass-first, the later put overwrites). `@TestPropertySource` in the subclass loses too (dynamic beats it).

**Why:** stage 7 Task 5 fixes: "customer down" and "token endpoint down" ITs got 201 instead of 503, because the base still pointed at the stub.

**How to apply:** the base registers the value as a placeholder with the default, `"\${accounts-test.customer-base-url:<stub url>}"`, and the subclass registers only the indirection key (`AccountsIntegrationTest.CUSTOMER_BASE_URL_OVERRIDE` / `TOKEN_URI_OVERRIDE`). Reuse the pattern in other services' IT bases.
