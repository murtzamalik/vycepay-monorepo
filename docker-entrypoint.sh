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

# Wait for MySQL to accept connections (use socket: -u root with no -h)
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

# Start Spring Boot services in background (each on its port)
echo "Starting VycePay services..."
export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/vycepay?useSSL=false&serverTimezone=UTC"
export DB_USERNAME=vycepay
export DB_PASSWORD=vycepay
export JWT_SECRET="${JWT_SECRET:-vycepay-default-secret-key-min-256-bits-for-hs256}"

(PORT=8081 java -jar /app/callback.jar) &
(PORT=8082 java -jar /app/auth.jar) &
(PORT=8083 java -jar /app/kyc.jar) &
(PORT=8084 java -jar /app/wallet.jar) &
(PORT=8085 java -jar /app/transaction.jar) &
(PORT=8086 java -jar /app/activity.jar) &

# BFF (single entry for mobile) - start after backends
sleep 5
(PORT=8080 java -jar /app/bff.jar) &

# Keep container running - wait for all background processes
wait
