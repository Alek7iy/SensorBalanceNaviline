# SensorBalanceAPI

REST API для получения баланса и статуса ГЛОНАСС‑датчиков по IMEI. Коммерческое решение: автоматизация проверки баланса датчиков, интеграция с Fort Monitor, мониторинг телематики.

## Стек технологий

- **Язык и рантайм:** Java 17
- **Фреймворк:** Spring Boot 3.x
- **Безопасность:** Spring Security + JWT (с `UserDetailsService`)
- **ORM:** Hibernate / Spring Data JPA
- **База данных:** PostgreSQL
- **Миграции БД:** Flyway
- **Сборка:** Maven
- **Документация API:** Swagger UI
- **Тестирование:** JUnit 5 + MockMvc

## Работа приложения

### В браузере откройте SWAGGER и введите ссылку:
http://localhost:8080/swagger-ui/index.html#/Баланс%20SIM-карт/getBalance

## Быстрый старт

### 1. Запуск PostgreSQL (Docker)

```bash
docker run --name pg-local -e POSTGRES_PASSWORD=secret -p 5432:5432 -d postgres

