---
name: sqs-listener-tests
description: notification service — once @SqsListener beans exist, every @SpringBootTest starts them; how the tests keep them off or give them queues
metadata:
  type: project
---

Spring Cloud AWS 4.2.1 listener containers resolve queue URLs at startup, so any `@SpringBootTest` of `notification` fails without the queues. Base `NotificationIntegrationTest` sets `spring.cloud.aws.sqs.listener.auto-startup=false` (stops ALL listeners); tests that need real listeners use their own `@SpringBootTest` + LocalStack and create the queues (including the app's default-named ones) in `@DynamicPropertySource` before the context starts.

**Why:** found in stage 3 Task A5; the A1 spike test broke as soon as the real listener was added.

**How to apply:** a new SQS listener or a new queue property means checking every integration test class that boots the app. Message attributes reach the listener as `@Header(name = "<attribute>")`.
