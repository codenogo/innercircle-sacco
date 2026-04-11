# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY pom.xml ./
COPY sacco-common/pom.xml sacco-common/
COPY sacco-config/pom.xml sacco-config/
COPY sacco-member/pom.xml sacco-member/
COPY sacco-security/pom.xml sacco-security/
COPY sacco-contribution/pom.xml sacco-contribution/
COPY sacco-loan/pom.xml sacco-loan/
COPY sacco-payout/pom.xml sacco-payout/
COPY sacco-investment/pom.xml sacco-investment/
COPY sacco-ledger/pom.xml sacco-ledger/
COPY sacco-reporting/pom.xml sacco-reporting/
COPY sacco-audit/pom.xml sacco-audit/
COPY sacco-app/pom.xml sacco-app/

RUN apt-get update && apt-get install -y --no-install-recommends maven && rm -rf /var/lib/apt/lists/* && \
    mvn dependency:go-offline -B 2>/dev/null || true

COPY . .
RUN mvn package -DskipTests -B && \
    mv sacco-app/target/sacco-app-0.0.1-SNAPSHOT.jar target/app.jar

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && \
    groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=build /app/target/app.jar app.jar

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -sf http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
