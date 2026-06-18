# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY common/pom.xml common/
COPY auth-service/pom.xml auth-service/
COPY payment-service/pom.xml payment-service/
COPY api-gateway/pom.xml api-gateway/
RUN mvn -B -pl payment-service -am -DskipTests dependency:go-offline || true

COPY common common
COPY payment-service payment-service
RUN mvn -B -DskipTests -pl payment-service -am package

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app app
COPY --from=build /workspace/payment-service/target/payment-service-*.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
