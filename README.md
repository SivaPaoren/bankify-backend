# Bankify Core Backend

Spring Boot Core Banking System (Accounts, Transactions, ATM, Partner APIs)

---

## Requirements
Install:
- Docker Desktop
- Java 21 (or newer)
- Git

---

## Step 1 — Start infrastructure (DB, Redis, RabbitMQ)

Run:

docker compose -f docker-compose.dev.yml up -d

This will start:
- PostgreSQL (port 5433)
- Redis (6379)
- RabbitMQ (5672, UI 15672)

RabbitMQ UI:
http://localhost:15672
user: bankify
pass: bankify

---

## Step 2 — Run the backend

Run:

SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

Server:
http://localhost:8080

Health:
http://localhost:8080/health

---

## First Admin (ONLY FIRST TIME)

In a new terminal:

export BANKIFY_BOOTSTRAP_TOKEN=testtoken

curl -X POST "http://localhost:8080/api/v1/bootstrap/admin" \
-H "Content-Type: application/json" \
-H "X-BOOTSTRAP-TOKEN: testtoken" \
-d '{"email":"admin@bankify.com","password":"StrongPassword123!!"}'

Then login using:

POST /api/v1/admin/auth/login

---

## API Base
http://localhost:8080/api/v1

---

## Troubleshooting

If DB errors:
docker compose -f docker-compose.dev.yml down -v
docker compose -f docker-compose.dev.yml up -d