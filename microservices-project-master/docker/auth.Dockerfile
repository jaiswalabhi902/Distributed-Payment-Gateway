# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy all module descriptors first so dependency resolution layers can cache.
COPY pom.xml ./
COPY common/pom.xml common/
COPY auth-service/pom.xml auth-service/
COPY payment-service/pom.xml payment-service/
COPY api-gateway/pom.xml api-gateway/
RUN mvn -B -pl auth-service -am -DskipTests dependency:go-offline || true

# Copy only the sources needed to build this service and its deps.
COPY common common
COPY auth-service auth-service
RUN mvn -B -DskipTests -pl auth-service -am package

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app app \
    && mkdir -p /app/keys && chown -R app:app /app/keys
COPY --from=build /workspace/auth-service/target/auth-service-*.jar app.jar
# Docker seeds an empty named volume from this dir's ownership, so app can write keys here.
VOLUME /app/keys
USER app
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
