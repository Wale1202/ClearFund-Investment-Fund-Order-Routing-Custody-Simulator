# syntax=docker/dockerfile:1

# ---- Build stage -----------------------------------------------------------
# Tests are run in CI, not during the image build, so the build stays fast
# and reproducible. Dependencies are cached in their own layer.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests clean package

# ---- Runtime stage ---------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as an unprivileged user.
RUN groupadd --system clearfund && useradd --system --gid clearfund clearfund

COPY --from=build /build/target/clearfund-*.jar app.jar
RUN chown clearfund:clearfund app.jar
USER clearfund

EXPOSE 8080

# JAVA_OPTS lets compose / the runtime tune the JVM without rebuilding.
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
