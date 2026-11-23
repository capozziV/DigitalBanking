---
name: stubborn-contract-pitfalls
description: Stubborn Contract 0.2.2 (successor of Spring Cloud Contract, user decision 2026-11-18, ADR 0022) traps in this repo - wiremock-standalone clash, WebMvcTest security, base class regex, ids
metadata:
  type: project
---

Stubborn Contract 0.2.2 (`sh.stubborn`, plugin id `sh.stubborn.contract`) is SCC renamed: Groovy DSL `sh.stubborn.contract.spec.Contract`, same tasks (`generateContractTests`, `verifierStubsJar`, `contractTest`), Stub Runner JUnit `sh.stubborn.contract.stubrunner.junit.StubRunnerExtension` with `StubsMode.CLASSPATH` (scans `META-INF/<group>/<artifact>/`; version ignored).

- `org.wiremock:wiremock-standalone` on the same classpath as the Stub Runner fails with ClassCastException `HandlebarsJsonPathHelper cannot be cast to wiremock.com.github.jknack.handlebars.Helper`. Drop standalone; the stub runner brings `wiremock` + `wiremock-jetty12`, and hand-written WireMockServer tests keep working.
- Boot 4 `@WebMvcTest` does not load security auto-config without `spring-boot-starter-security-test` (error: no `HttpSecurity` bean); `@WebMvcTest` itself needs `spring-boot-starter-webmvc-test`. Libs auto-configs come in via `@ImportAutoConfiguration`.
- `baseClassMappings` regex is matched against the contract folder relative to the contracts dir (`http`), not the full package: use `(.*\.)?http(\..*)?`.
- All bootstrap modules are named `bootstrap`; stub coordinates come from `project.group`, so the producer convention sets the group per service.
- ktlint-gradle lints every dir of the contractTest source set, the generated Java dir included: `build` fails with "implicit dependency" unless `runKtlint*OverContractTestSourceSet` dependsOn `generateContractTests` (done in the producer convention). A targeted `contractTest` run does not show it; only the full `build` does.
- Machine token in a contract: `producer(execute('serviceAuthorization()'))` (JwtTestSupport key is per JVM, so YAML literals cannot work).

**Why:** cost debug rounds in etapa 8 Task 7 (2026-11-18).
**How to apply:** check here first when touching contracts (Task 8 messaging contracts reuse the same conventions).
