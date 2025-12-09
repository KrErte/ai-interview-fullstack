# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AI Interview Fullstack is an AI-powered interview preparation and career coaching platform. It helps users prepare for job interviews through CV analysis, job matching, skill assessments, and personalized training roadmaps.

## Tech Stack

- **Backend**: Spring Boot 3.3.4, Java 17, Gradle
- **Frontend**: Angular 19, TypeScript, Tailwind CSS
- **Database**: H2 (PostgreSQL mode) with Liquibase migrations
- **Auth**: JWT-based authentication
- **AI Integration**: OpenAI API (GPT-4.1-mini)
- **E2E Testing**: Playwright (frontend), Selenium (backend)

## Build & Run Commands

### Backend (from root or `backend/` directory)
```bash
# Build
./gradlew :backend:build

# Run (starts on port 8080)
./gradlew :backend:bootRun

# Run tests
./gradlew :backend:test

# Run a single test class
./gradlew :backend:test --tests "ee.kerrete.ainterview.auth.AuthServiceLoginTest"
```

### Frontend (from `frontend/` directory)
```bash
# Install dependencies
npm install

# Dev server (port 4200, proxies /api to localhost:8080)
npm start

# Build
npm run build

# Unit tests
npm test

# E2E tests (requires frontend running on 4200)
npm run e2e

# E2E with UI
npm run e2e:ui
```

## Architecture

### Backend Structure
- `ee.kerrete.ainterview.api/` - REST controllers (30+ endpoints)
- `ee.kerrete.ainterview.auth/` - Authentication (JWT, login/register)
- `ee.kerrete.ainterview.config/` - Spring config (Security, OpenAI, CORS)
- `ee.kerrete.ainterview.dto/` - Request/response DTOs
- `ee.kerrete.ainterview.model/` - JPA entities
- `ee.kerrete.ainterview.repository/` - Spring Data repositories
- `ee.kerrete.ainterview.service/` - Business logic and OpenAI integration

### Frontend Structure
- `src/app/pages/` - Main route components (dashboard, job-match, training, profile)
- `src/app/services/` - HTTP services communicating with backend
- `src/app/models/` - TypeScript interfaces
- `src/app/guards/` - Route guards (auth)
- `src/app/interceptors/` - HTTP interceptors (JWT token injection)
- `src/environments/` - Environment configs

### Key Architectural Patterns
- Frontend uses Angular standalone components with lazy loading
- Backend uses stateless JWT auth (token in Authorization header)
- All protected endpoints require JWT; public endpoints: `/api/auth/**`, `/actuator/**`, Swagger
- Proxy config (`proxy.conf.json`) routes `/api` from Angular dev server to backend
- CORS configured in `SecurityConfig` for `localhost:4200`

## Database

- Local dev: file-based H2 at `backend/data/aiinterview-db` (Hibernate auto-update)
- Tests: in-memory H2 with Liquibase (drops and recreates schema)
- H2 Console available at `http://localhost:8080/h2-console` (local profile only)
- Migrations in `backend/src/main/resources/db/changelog/`

## Environment Variables

- `OPENAI_API_KEY` - Required for AI features (defaults to dummy key if not set)

## Spring Profiles

- `local` (default): H2 file DB, H2 console enabled, Liquibase disabled
- `test`: In-memory H2, Liquibase enabled with drop-first
- `prod`: Production configuration
