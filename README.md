# 🎧 Audio Listener — Speech-to-Text Transcription

<p align="center">
  <img src="./public/banner.png" alt="Audio Listener Banner" width="800px">
</p>

A full-stack application for audio transcription powered by **OpenAI Whisper**. Record audio in the browser or upload files, and get instant transcriptions with history tracking.

## 🏗 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 21 + Spring Boot 3.3 |
| **Frontend** | Next.js 15 (App Router) + TypeScript |
| **Database** | PostgreSQL 16 |
| **ORM** | Spring Data JPA + Hibernate |
| **Migrations** | Flyway |
| **Build** | Maven (backend) + npm (frontend) |
| **Containers** | Docker + Docker Compose |

## 📁 Project Structure

```
test-audio-listener/
├── backend/                     # Spring Boot API
│   ├── src/main/java/com/audiolistener/
│   │   ├── client/              # OpenAI integration
│   │   ├── config/              # App configuration
│   │   ├── controller/          # REST endpoints
│   │   ├── dto/                 # Data transfer objects
│   │   ├── entity/              # JPA entities
│   │   ├── exception/           # Global error handling
│   │   ├── repository/          # Data access layer
│   │   └── service/             # Business logic
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/        # Flyway SQL scripts
│   ├── src/test/                # Unit & integration tests
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                    # Next.js UI
│   ├── src/
│   │   ├── app/                 # Pages (App Router)
│   │   ├── components/          # React components
│   │   └── lib/                 # API client
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml
└── .env.example
```

## 🚀 Quick Start (Docker)

The fastest way to run the entire stack:

```bash
# 1. Clone and configure
cp .env.example .env
# Edit .env and set your OPENAI_API_KEY

# 2. Start everything
docker compose up --build

# 3. Open http://localhost:3000
```

## 🛠 Local Development Setup

### Prerequisites

- Java 21 (JDK)
- Node.js 20+
- PostgreSQL 16 (or use Docker for DB only)
- An OpenAI API key

### 1. Start PostgreSQL

```bash
# Using Docker (recommended)
docker compose up postgres -d

# Or use a local PostgreSQL instance
# Create database: audiolistener
```

### 2. Run Backend

```bash
cd backend

# Set environment variable
export OPENAI_API_KEY=sk-your-key-here

# Run with Maven wrapper
./mvnw spring-boot:run

# Backend starts at http://localhost:8080
```

### 3. Run Frontend

```bash
cd frontend

# Create .env.local
echo "NEXT_PUBLIC_API_URL=http://localhost:8080/api" > .env.local

# Install dependencies
npm install

# Start dev server
npm run dev

# Frontend starts at http://localhost:3000
```

## 🔑 Environment Variables

### Backend

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `OPENAI_API_KEY` | ✅ | — | OpenAI API key |
| `OPENAI_MODEL` | | `whisper-1` | Transcription model |
| `OPENAI_TIMEOUT` | | `120` | API timeout (seconds) |
| `DB_HOST` | | `localhost` | PostgreSQL host |
| `DB_PORT` | | `5432` | PostgreSQL port |
| `DB_NAME` | | `audiolistener` | Database name |
| `DB_USERNAME` | | `audiolistener` | Database user |
| `DB_PASSWORD` | | `audiolistener` | Database password |
| `AUDIO_UPLOAD_DIR` | | `./uploads` | Audio file storage path |

### Frontend

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `NEXT_PUBLIC_API_URL` | | `http://localhost:8080/api` | Backend API URL |

## 📡 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/transcriptions` | Upload audio & transcribe |
| `GET` | `/api/transcriptions` | List all transcriptions |
| `GET` | `/api/transcriptions?q=term` | Search transcriptions |
| `GET` | `/api/transcriptions/{id}` | Get transcription details |
| `DELETE` | `/api/transcriptions/{id}` | Delete a transcription |

### Sample curl Commands

```bash
# Upload and transcribe an audio file
curl -X POST http://localhost:8080/api/transcriptions \
  -F "file=@speech.mp3" \
  | python3 -m json.tool

# List all transcriptions
curl http://localhost:8080/api/transcriptions | python3 -m json.tool

# Search transcriptions
curl "http://localhost:8080/api/transcriptions?q=hello" | python3 -m json.tool

# Get a specific transcription
curl http://localhost:8080/api/transcriptions/1 | python3 -m json.tool

# Delete a transcription
curl -X DELETE http://localhost:8080/api/transcriptions/1
```

## 🧪 Testing

### Backend Tests

```bash
cd backend
./mvnw test
```

Tests include:
- **TranscriptionServiceTest** — 8 unit tests (success/failure/validation/search/delete)
- **TranscriptionControllerTest** — 5 integration tests (all endpoints + error handling)

### Frontend

```bash
cd frontend
npm run lint
```

## 🏛 Architecture Decisions

| Decision | Rationale |
|----------|-----------|
| **Synchronous transcription** | Simpler initial implementation; async with Redis can be added later |
| **File storage on disk** | `TranscriptionService.saveFileToDisk()` is designed for easy S3 swap |
| **No authentication** | Out of scope for MVP; CORS is configured for local dev |
| **Flyway for migrations** | Production-ready schema management vs `ddl-auto` |
| **Global exception handler** | Consistent JSON error responses across all endpoints |

## 🔒 Security Notes

- OpenAI API key is **only on the backend** via environment variables
- Frontend calls Spring Boot, **never** OpenAI directly
- File type and size validation on upload
- CORS restricted to `localhost:3000` and `localhost:3001`
- **TODO**: Add authentication (Spring Security + JWT)
- **TODO**: Add rate limiting (Bucket4j or Spring Cloud Gateway)
- **TODO**: Add CSRF protection when auth is enabled

## 📄 License

Private project — all rights reserved.
