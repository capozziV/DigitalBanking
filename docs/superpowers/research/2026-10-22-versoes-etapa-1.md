# Versões novas da etapa 1

Pesquisa de 2026-10-22. Versões lidas dos `maven-metadata.xml` e POMs no Maven Central (via espelho `maven-central.storage-download.googleapis.com`, que replica o Central) e das notas de release do springdoc.

| Dependência | Versão | Fonte |
|---|---|---|
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | 3.1.1 | [maven-metadata](https://repo1.maven.org/maven2/org/springdoc/springdoc-openapi-starter-webmvc-ui/maven-metadata.xml) (release 3.1.1, 2026-09-06); [notas de release](https://github.com/springdoc/springdoc-openapi/releases) |
| `com.fasterxml.uuid:java-uuid-generator` | 5.2.0 | [maven-metadata](https://repo1.maven.org/maven2/com/fasterxml/uuid/java-uuid-generator/maven-metadata.xml) (release 5.2.0, 2025-12-05) |
| `software.amazon.awssdk:bom` | 2.57.1 | [maven-metadata](https://repo1.maven.org/maven2/software/amazon/awssdk/bom/maven-metadata.xml) (release 2.57.1, 2026-10-20) |
| `org.testcontainers:testcontainers-localstack` | 2.0.5 (sem versão no catálogo; gerida pelo BOM do Boot) | [spring-boot-dependencies 4.1.1](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom): `testcontainers.version` = 2.0.5; [maven-metadata](https://repo1.maven.org/maven2/org/testcontainers/testcontainers-localstack/maven-metadata.xml) tem 2.0.5 |

## springdoc e Spring Boot 4.1.x

Há springdoc compatível. As notas da 3.1.0 dizem "Upgrade Spring Boot to version 4.1.0"; a 3.1.1 é a release seguinte da mesma linha (3.x é a linha do Spring Boot 4). O POM do `springdoc-openapi-starter-common` 3.1.1 usa os módulos modulares do Boot 4 (`spring-boot-jackson`, `spring-boot-web-server`, `spring-boot-validation`). Compatibilidade em tempo de execução ainda não foi exercitada (nenhum código usa springdoc nesta task); a Task 10 confirma, e o plano B em YAML continua valendo se falhar.

## Observações
- O BOM do Boot 4.1.1 não gerencia o AWS SDK; por isso o `aws-sdk-bom` fica no catálogo com versão própria. `sns` e `sqs` entram sem versão, sob esse BOM.
- Maven Central direto respondeu 429 neste ambiente (IP compartilhado); os mesmos arquivos foram lidos no espelho.
