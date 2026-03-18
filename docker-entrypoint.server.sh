#!/bin/bash
set -e

# Ensure mysql owns the datadir (fixes volume permission issues from previous MySQL containers)
chown -R mysql:mysql /var/lib/mysql 2>/dev/null || true

# Initialize MariaDB if datadir is empty (first run with volume)
if [ ! -f /var/lib/mysql/ibdata1 ]; then
  echo "Initializing MariaDB..."
  mariadb-install-db --user=mysql --datadir=/var/lib/mysql --skip-test-db
fi

# Start MariaDB
echo "Starting MySQL/MariaDB..."
mkdir -p /var/run/mysqld
chown mysql:mysql /var/run/mysqld 2>/dev/null || true
mariadbd --user=mysql --datadir=/var/lib/mysql &

# Wait for MySQL to accept connections (use socket-based root login)
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

# Create database and user if not exists
mysql -u root -e "
  CREATE DATABASE IF NOT EXISTS vycepay;
  CREATE USER IF NOT EXISTS 'vycepay'@'%' IDENTIFIED BY 'vycepay';
  CREATE USER IF NOT EXISTS 'vycepay'@'localhost' IDENTIFIED BY 'vycepay';
  GRANT ALL ON vycepay.* TO 'vycepay'@'%';
  GRANT ALL ON vycepay.* TO 'vycepay'@'localhost';
  FLUSH PRIVILEGES;
" 2>/dev/null || true

# Start Spring Boot services in background (single container, fixed ports)
echo "Starting VycePay services (server port layout)..."

export JWT_SECRET="${JWT_SECRET:-vycepay-default-secret-key-min-256-bits-for-hs256}"

# Use env vars so Spring Boot picks them up (overrides fixed application.yml datasource if needed)
export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/vycepay?useSSL=false&serverTimezone=UTC"
export DB_USERNAME=vycepay
export DB_PASSWORD=vycepay

# Callback must run on 8081 (registered port in your requirement)
(PORT=8081 java -jar /app/callback.jar) &

# Other services run on 9090 series
# BFF: 9090
# Auth: 9091
# KYC: 9092
# Wallet: 9093
# Transaction: 9094
# Activity: 9095
(PORT=9091 java -jar /app/auth.jar) &
(PORT=9092 java -jar /app/kyc.jar) &
(PORT=9093 java -jar /app/wallet.jar) &
(PORT=9094 java -jar /app/transaction.jar) &
(PORT=9095 java -jar /app/activity.jar) &

# BFF (single entry point) - start after backends
sleep 5

# Configure BFF backend URLs for the new internal ports
export BFF_AUTH_URL="http://127.0.0.1:9091"
export BFF_KYC_URL="http://127.0.0.1:9092"
export BFF_WALLETS_URL="http://127.0.0.1:9093"
export BFF_TRANSACTIONS_URL="http://127.0.0.1:9094"
export BFF_ACTIVITY_URL="http://127.0.0.1:9095"

(PORT=9090 java -jar /app/bff.jar) &

# Keep container running - wait for all background processes
wait

