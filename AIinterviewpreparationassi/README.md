# AI Interview Prep Assistant — Java Full Stack

This is a starter MVP rebuilt with Java 17, Spring Boot, Spring Data JPA/Hibernate, MySQL, HTML, CSS, JavaScript and Groq API.

## Features
- Registration and login with BCrypt password hashing and server-side HTTP sessions
- Generate five role-based interview questions with Groq
- Evaluate answers and persist feedback/scores
- Explain technical concepts
- Interview history
- Dashboard statistics

## Requirements
- JDK 17+
- Maven 3.8+
- MySQL 8+
- Groq API key from Groq Console

## 1. Create the database
Run in MySQL:
```sql
CREATE DATABASE ai_interview_prep;
```
The application can also create it if the MySQL user has permission.

## 2. Configure environment variables
Set these in the terminal before starting the app (do not commit secrets):
```bash
export DB_URL="jdbc:mysql://localhost:3306/ai_interview_prep?createDatabaseIfNotExist=true"
export DB_USERNAME="root"
export DB_PASSWORD="YOUR_MYSQL_PASSWORD"
export GROQ_API_KEY="YOUR_GROQ_API_KEY"
export GROQ_MODEL="openai/gpt-oss-20b"
```
In Eclipse, add these variables under Run > Run Configurations > Environment. A local `.env` file is not automatically loaded by Spring Boot.

## 3. Run
From the folder containing `pom.xml`:
```bash
mvn spring-boot:run
```
Open: http://localhost:8080

## API endpoints
- `POST /api/auth/register` — JSON `{ "email":"you@example.com", "password":"atleast8chars" }`
- `POST /api/auth/login` — same JSON
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `POST /api/questions/generate` — `{ "role":"Java Developer" }`
- `POST /api/answer/evaluate` — `{ "sessionId":1, "questionIndex":0, "answer":"..." }`
- `POST /api/assistant/explain` — `{ "topic":"Explain JVM" }`
- `GET /api/history`
- `GET /api/dashboard/stats`

All interview and assistant endpoints require a logged-in session. The static frontend is served by Spring Boot from `src/main/resources/static`.

## Important notes
- `spring.jpa.hibernate.ddl-auto=update` is convenient for local development, not a production migration strategy.
- Use HTTPS and set `SESSION_COOKIE_SECURE=true` in production.
- Add CSRF protection before deploying cookie-based session authentication publicly. This learning MVP does not yet include CSRF tokens or rate limiting.
- Groq model availability and usage limits can change; check the Groq model catalog and your account's limits if a request is rejected.
- Do not place real API keys or database passwords in source code or Git.


## Groq API setup

This version uses Groq instead of Gemini for question generation, answer evaluation,
and concept explanations.

1. Create an API key at https://console.groq.com/keys.
2. In Eclipse, open Run > Run Configurations.
3. Select the `InterviewPrepApplication` launch configuration.
4. Open the Environment tab and add `GROQ_API_KEY` with your secret key as its value.
5. Optionally add `GROQ_MODEL` with value `openai/gpt-oss-20b`.
6. Apply, run the application again, and open http://localhost:8080.

The API key is read on the backend. Do not put it in frontend JavaScript or commit it to Git.
The selected model is configurable in `application.properties`.
