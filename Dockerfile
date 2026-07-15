# ---- Build stage ----
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# mvnw's "only-script" wrapper downloads its own Maven distribution; alpine's
# base image ships neither a downloader nor an unzip tool.
RUN apk add --no-cache curl unzip

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/config && chown -R app:app /app
COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080
# ./config/catalog is seeded from classpath defaults on first boot and edited
# at runtime via the admin catalog editor - persist it across container recreation.
VOLUME ["/app/config"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget --spider -q http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
