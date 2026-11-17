# Imagem do accounts a partir do bootJar ja construido pelo Gradle (a imagem nao compila).
# Contexto: services/accounts. Construa antes o jar: ./gradlew :services:accounts:bootstrap:bootJar
# A 1a etapa extrai as camadas do jar; a 2a copia uma camada por vez, da que menos muda (dependencias) para a
# que mais muda (classes do servico), para o cache do Docker reaproveitar as de baixo.
# Sem HEALTHCHECK: no Kubernetes valem as probes; no compose, o healthcheck do servico.
FROM eclipse-temurin:21-jre@sha256:ca73b4d3961703d6d7830a4361f49ef0478f8f28b39defc07bb72f4cc7f834f6 AS layers
WORKDIR /build
COPY bootstrap/build/libs/accounts.jar accounts.jar
RUN java -Djarmode=tools -jar accounts.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21-jre@sha256:ca73b4d3961703d6d7830a4361f49ef0478f8f28b39defc07bb72f4cc7f834f6
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app --no-create-home app
WORKDIR /app
COPY --from=layers /build/extracted/dependencies/ ./
COPY --from=layers /build/extracted/spring-boot-loader/ ./
COPY --from=layers /build/extracted/snapshot-dependencies/ ./
COPY --from=layers /build/extracted/application/ ./
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
