---
name: sqs-listener-tests
description: notification service — once @SqsListener beans exist, every @SpringBootTest starts them; how the tests keep them off or give them queues, and where the shared test properties live
metadata:
  type: project
---

Spring Cloud AWS 4.2.1 listener containers resolve queue URLs at startup, so any `@SpringBootTest` of `notification` fails without the queues. Base `NotificationIntegrationTest` sets `spring.cloud.aws.sqs.listener.auto-startup=false` (stops ALL listeners); tests that need real listeners use their own `@SpringBootTest` + LocalStack and create the queues (including the app's default-named ones) in `@DynamicPropertySource` before the context starts.

**Why:** found in stage 3 Task A5; the A1 spike test broke as soon as the real listener was added.

Trap: a subclass `@SpringBootTest(properties = [...])` REPLACES the base's properties. Since card #113 the base keeps its properties in `@TestPropertySource` (inherited and merged), so subclasses declare only `@SpringBootTest(webEnvironment = ...)`. LocalStack classes call `NotificationTestPropertiesSupport.localStack(registry, container)`.

Since #113 the SQS endpoint default is blank (real AWS, like the relays) and an endpoint selects static test credentials (`AwsCredentialsConfiguration`), so every test must set `spring.cloud.aws.sqs.endpoint` explicitly (base uses `http://127.0.0.1:1`). The DLQ gauge reads a value refreshed by `@Scheduled` (`notification.dlq-metrics.refresh-interval`); a test awaiting the gauge needs a short interval.

SQS keeps `ApproximateReceiveCount` when redriving to the DLQ, and an inspection receive adds one (DLQ of maxReceiveCount=5 reads 6). Shared queues across tests in a class: compare DLQ depth with a `@BeforeEach` snapshot.

**How to apply:** a new SQS listener or a new queue property means checking every integration test class that boots the app. Message attributes reach the listener as `@Header(name = "<attribute>")`. The `statement` service (stage 4) should copy this layout.
