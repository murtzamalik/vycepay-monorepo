#!/bin/bash
set -e

export JWT_SECRET="${JWT_SECRET:-vycepay-default-secret-key-min-256-bits-for-hs256}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
export ADMIN_JWT_SECRET="${ADMIN_JWT_SECRET:-dev-only-admin-secret-change-before-prod-123456}"
export ADMIN_CORS_ORIGINS="${ADMIN_CORS_ORIGINS:-http://localhost:3000}"
export ADMIN_BOOTSTRAP_USERNAME="${ADMIN_BOOTSTRAP_USERNAME:-}"
export ADMIN_BOOTSTRAP_EMAIL="${ADMIN_BOOTSTRAP_EMAIL:-}"
export ADMIN_BOOTSTRAP_PASSWORD="${ADMIN_BOOTSTRAP_PASSWORD:-}"
export ADMIN_HEALTH_BFF_URL="http://127.0.0.1:9090/actuator/health"
export ADMIN_HEALTH_CALLBACK_URL="http://127.0.0.1:8081/actuator/health"
export ADMIN_HEALTH_AUTH_URL="http://127.0.0.1:9091/actuator/health"
export ADMIN_HEALTH_KYC_URL="http://127.0.0.1:9092/actuator/health"
export ADMIN_HEALTH_WALLET_URL="http://127.0.0.1:9093/actuator/health"
export ADMIN_HEALTH_TRANSACTION_URL="http://127.0.0.1:9094/actuator/health"
export ADMIN_HEALTH_ACTIVITY_URL="http://127.0.0.1:9095/actuator/health"

export DB_USERNAME="${DB_USERNAME:-${MYSQL_USER:-vycepay}}"
export DB_PASSWORD="${DB_PASSWORD:-${MYSQL_PASSWORD:-vycepay}}"
export DB_NAME="${DB_NAME:-${MYSQL_DATABASE:-vycepay}}"
export DB_PORT="${DB_PORT:-3306}"

if [ -n "${DB_HOST:-}" ]; then
  echo "Using external database at ${DB_HOST}:${DB_PORT}/${DB_NAME}"
  export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}"

  echo "Waiting for external database..."
  for i in $(seq 1 90); do
    if (echo > "/dev/tcp/${DB_HOST}/${DB_PORT}") 2>/dev/null; then
      echo "External database is reachable"
      break
    fi
    if [ "$i" -eq 90 ]; then
      echo "External database ${DB_HOST}:${DB_PORT} not reachable"
      exit 1
    fi
    sleep 1
  done
else
  chown -R mysql:mysql /var/lib/mysql 2>/dev/null || true

  if [ ! -f /var/lib/mysql/ibdata1 ]; then
    echo "Initializing MariaDB..."
    mariadb-install-db --user=mysql --datadir=/var/lib/mysql --skip-test-db
  fi

  echo "Starting MySQL/MariaDB..."
  mkdir -p /var/run/mysqld
  chown mysql:mysql /var/run/mysqld 2>/dev/null || true
  mariadbd --user=mysql --datadir=/var/lib/mysql &

  echo "Waiting for MySQL to be ready..."
  for i in $(seq 1 90); do
    if mysql -u root -e "SELECT 1" &>/dev/null; then
      echo "MySQL is ready"
      break
    fi
    if [ $i -eq 90 ]; then
      echo "MySQL failed to start"
      exit 1
    fi
    sleep 1
  done

  mysql -u root -e "
    CREATE DATABASE IF NOT EXISTS ${DB_NAME};
    CREATE USER IF NOT EXISTS '${DB_USERNAME}'@'%' IDENTIFIED BY '${DB_PASSWORD}';
    CREATE USER IF NOT EXISTS '${DB_USERNAME}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
    GRANT ALL ON ${DB_NAME}.* TO '${DB_USERNAME}'@'%';
    GRANT ALL ON ${DB_NAME}.* TO '${DB_USERNAME}'@'localhost';
    FLUSH PRIVILEGES;
  " 2>/dev/null || true

  export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:mysql://127.0.0.1:3306/${DB_NAME}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}"
  sleep 5
fi

echo "Starting VycePay services (server port layout)..."

(PORT=8081 java -jar /app/callback.jar) &
(PORT=9091 java -jar /app/auth.jar) &
(PORT=9092 java -jar /app/kyc.jar) &
(PORT=9093 java -jar /app/wallet.jar) &
(PORT=9094 java -jar /app/transaction.jar) &
(PORT=9095 java -jar /app/activity.jar) &

sleep 5

export BFF_AUTH_URL="http://127.0.0.1:9091"
export BFF_KYC_URL="http://127.0.0.1:9092"
export BFF_WALLETS_URL="http://127.0.0.1:9093"
export BFF_TRANSACTIONS_URL="http://127.0.0.1:9094"
export BFF_ACTIVITY_URL="http://127.0.0.1:9095"

(PORT=9090 java -jar /app/bff.jar) &

wait
