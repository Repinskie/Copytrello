# Copytrello

**Copytrello** — это многопользовательский планировщик задач, который предоставляет функциональность для регистрации и аутентификации пользователей, управления задачами и отправки уведомлений по email. Этот проект использует такие технологии, как Kafka, Docker и Spring Boot. Проект построен на микросервисной архитектуре и использует Docker и Kafka для взаимодействия между сервисами.

### 📌 Сервисы Проекта

Copytrello состоит из нескольких сервисов:
- **Backend**: обрабатывает регистрацию и аутентификацию пользователей с использованием JWT, а также управление задачами.
- **Email Sender**: отвечает за отправку email-уведомлений о регистрации и обновлениях задач.
- **Scheduler**: в конце дня формирует список завершенных и незавершенных задач пользователя и составляет письмо для отправки на почту.

### ⚙️ Используемые технологии

- **Язык программирования**: Java
- **Сборка проекта**: Gradle
- **Фреймворк**: Spring Boot (включая модули Spring Security, Spring Kafka, Spring Scheduler, Spring Mail)
- **Аутентификация**: JWT
- **База данных**: PostgreSQL с использованием Spring Data JPA
- **Миграции**: Flyway
- **Фронтенд**: HTML/CSS, Bootstrap, JavaScript, Ajax
- **Инфраструктура**: Docker и микросервисы
- **Брокер сообщений**: Kafka 
