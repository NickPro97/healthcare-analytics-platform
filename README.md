# Healthcare Analytics Platform

A microservices-based healthcare analytics platform combining real-time clinical data processing, ML-driven risk prediction, and an AI agent for clinician decision support.

## Architecture

- **Frontend:** React 18 + TypeScript (planned)
- **Backend:** Spring Boot 4 microservices (Java 21)
- **ML Service:** Python FastAPI + scikit-learn (planned)
- **AI Layer:** Spring AI + OpenAI + ChromaDB for RAG (planned)
- **Messaging:** Apache Kafka (planned)
- **Data:** PostgreSQL 16, Redis (planned)
- **Infra:** Docker, Kubernetes (planned), AWS EKS (planned)
- **CI/CD:** GitHub Actions (planned)

## Status

🚧 In active development — May 2026

## Modules

- `backend/patient-service` — Patient records management (Spring Boot + PostgreSQL)
- `ml-service/` — Python ML and AI agent service (planned)
- `frontend/` — React TypeScript app (planned)
- `infra/` — Docker, Kubernetes, deployment configs (planned)

## Currently Running

- Spring Boot Patient Service on `http://localhost:8080`
- PostgreSQL 16 in Docker on port 5432
- Health check endpoint: `GET /api/health`
