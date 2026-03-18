# Multi-stage build for VycePay services
# Build: docker build --build-arg MODULE=auth-service -t vycepay-auth-service .
# MODULE: auth-service | callback-service | kyc-service | wallet-service | transaction-service | activity-service

ARG MODULE=auth-service
FROM eclipse-temurin:17-jdk AS builder
ARG MODULE

WORKDIR /build

# Copy parent and module poms
COPY pom.xml .
COPY vycepay-common/pom.xml vycepay-common/
COPY vycepay-database/pom.xml vycepay-database/
COPY vycepay-auth-service/pom.xml vycepay-auth-service/
COPY vycepay-callback-service/pom.xml vycepay-callback-service/
COPY vycepay-kyc-service/pom.xml vycepay-kyc-service/
COPY vycepay-wallet-service/pom.xml vycepay-wallet-service/
COPY vycepay-transaction-service/pom.xml vycepay-transaction-service/
COPY vycepay-activity-service/pom.xml vycepay-activity-service/

# Download dependencies (cached layer)
RUN apt-get update && apt-get install -y --no-install-recommends maven && rm -rf /var/lib/apt/lists/* && \
    mvn dependency:go-offline -B -q || true

# Copy source
COPY vycepay-common vycepay-common
COPY vycepay-database vycepay-database
COPY vycepay-auth-service vycepay-auth-service
COPY vycepay-callback-service vycepay-callback-service
COPY vycepay-kyc-service vycepay-kyc-service
COPY vycepay-wallet-service vycepay-wallet-service
COPY vycepay-transaction-service vycepay-transaction-service
COPY vycepay-activity-service vycepay-activity-service

# Build specific module and its dependencies
RUN mvn package -DskipTests -B -q -pl vycepay-${MODULE} -am

# Runtime stage
FROM eclipse-temurin:17-jre
ARG MODULE

RUN groupadd -g 2000 vycepay && useradd -u 2000 -g vycepay -m -s /bin/bash vycepay
WORKDIR /app

COPY --from=builder /build/vycepay-${MODULE}/target/vycepay-${MODULE}-*.jar app.jar
RUN chown -R 2000:2000 /app

USER vycepay
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
