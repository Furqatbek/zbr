# Multi-stage build for Food Delivery Platform

# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy pom.xml first for better caching
COPY pom.xml .

# Download dependencies (cached if pom.xml unchanged)
RUN for i in 1 2 3 4; do \
      mvn dependency:go-offline -B && break || \
      { echo "Attempt $i failed, retrying in $((i*2))s..."; sleep $((i*2)); }; \
    done

# Copy source code
COPY src ./src

# Build: skip tests + JaCoCo, parallel compile (1 thread per CPU core)
RUN mvn clean package -Dmaven.test.skip=true -Djacoco.skip=true -T 1C -B

# Extract layered jar for faster Docker rebuilds
RUN mkdir -p target/extracted && java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy layers separately — bottom layers change less often, Docker caches them
COPY --from=build /app/target/extracted/dependencies/ ./
COPY --from=build /app/target/extracted/spring-boot-loader/ ./
COPY --from=build /app/target/extracted/snapshot-dependencies/ ./
COPY --from=build /app/target/extracted/application/ ./

# Set ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose ports
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM options for containers.
# MaxRAMPercentage is read from the CONTAINER's memory limit — so the container
# MUST have one (docker-compose sets mem_limit). Without a limit the JVM sizes
# its heap against the whole host and starves Postgres/Redis/RabbitMQ on a small
# VPS, which ends in the kernel OOM-killer. Override with JAVA_OPTS if needed.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+UseG1GC -XX:+UseStringDeduplication -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError"

# Run with Spring Boot layered jar launcher
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
