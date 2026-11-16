# Imagem do transfers a partir do bootJar ja construido pelo Gradle (a imagem nao compila).
# Contexto: services/transfers. Construa antes o jar: ./gradlew :services:transfers:bootstrap:bootJar
# A 1a etapa extrai as camadas do jar; a 2a copia uma camada por vez, da que menos muda (dependencias) para a
# que mais muda (classes do servico), para o cache do Docker reaproveitar as de baixo.
# Sem HEALTHCHECK: no Kubernetes valem as probes; no compose, o healthcheck do servico.
FROM eclipse-temurin:21-jre AS layers
WORKDIR /build
COPY bootstrap/build/libs/transfers.jar transfers.jar
RUN java -Djarmode=tools -jar transfers.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21-jre
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app --no-create-home app
WORKDIR /app
COPY --from=layers /build/extracted/dependencies/ ./
COPY --from=layers /build/extracted/spring-boot-loader/ ./
COPY --from=layers /build/extracted/snapshot-dependencies/ ./
COPY --from=layers /build/extracted/application/ ./
USER 10001:10001
EXPOSE 8082
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
