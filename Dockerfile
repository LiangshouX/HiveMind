# ============================================================
# HiveMind - Multi-stage Dockerfile
# Stage 1: Maven build  |  Stage 2: JRE runtime
# ============================================================

# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy POM files first for dependency caching
COPY pom.xml .
COPY hivemind-agent-engine/pom.xml hivemind-agent-engine/pom.xml
COPY hivemind-backend/pom.xml hivemind-backend/pom.xml
COPY hivemind-launcher/pom.xml hivemind-launcher/pom.xml

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B -q 2>/dev/null || true

# Copy source code
COPY hivemind-agent-engine/src hivemind-agent-engine/src
COPY hivemind-backend/src hivemind-backend/src
COPY hivemind-launcher/src hivemind-launcher/src

# Build the application (skip tests — tests run in CI)
RUN mvn clean package -DskipTests -B -q

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="LiangshouX"
LABEL description="HiveMind - Enterprise-Grade AI Agent Platform"

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

# Copy the built jar from builder stage
COPY --from=builder /build/hivemind-launcher/target/hivemind-launcher-1.0-SNAPSHOT.jar app.jar

# Copy SQL init scripts (for reference / manual init)
COPY hivemind-backend/src/main/resources/db/init/ /app/db/init/

# JVM tuning
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom"
ENV TZ=Asia/Shanghai

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD curl -sf http://localhost:8080/actuator/health || curl -sf http://localhost:8080/swagger-ui.html || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar $@"]
