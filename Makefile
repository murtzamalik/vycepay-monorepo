# VycePay Local Development
# Prerequisites: Docker (MySQL), Java 17, Maven

.PHONY: mysql up down build run-auth run-kyc run-wallet run-tx run-callback run-activity docker-up docker-down docker-build docker-up-vycepay docker-down-vycepay test-apis

mysql:
	docker compose up -d mysql

up: mysql
	@echo "MySQL started. Run services with: make run-auth, make run-kyc, etc."

down:
	docker compose down

build:
	mvn clean compile -DskipTests -q

run-auth:
	mvn spring-boot:run -pl vycepay-auth-service -q

run-kyc:
	mvn spring-boot:run -pl vycepay-kyc-service -q

run-wallet:
	mvn spring-boot:run -pl vycepay-wallet-service -q

run-tx:
	mvn spring-boot:run -pl vycepay-transaction-service -q

run-callback:
	mvn spring-boot:run -pl vycepay-callback-service -q

run-activity:
	mvn spring-boot:run -pl vycepay-activity-service -q

# Docker full stack (multi-container)
docker-build:
	docker compose build

docker-up:
	docker compose up -d --build

docker-down:
	docker compose down

# Docker single container (all services + DB in one container named vycepay)
docker-up-vycepay:
	docker compose -f docker-compose.vycepay.yml up -d --build

docker-down-vycepay:
	docker compose -f docker-compose.vycepay.yml down

# API test script (requires vycepay container running)
test-apis:
	./scripts/test-apis.sh
