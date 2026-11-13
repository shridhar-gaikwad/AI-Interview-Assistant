# Docker Setup

This document covers running **AI Interview Assistant** entirely through
Docker — Java, Maven, PostgreSQL/pgvector, and Ollama all run inside
containers, so none of them need to be installed on your machine.

> Looking for the manual, run-it-on-your-machine workflow instead? See
> `setup.md` (installs Java, Maven, PostgreSQL + pgvector, and Ollama
> directly, Docker Desktop only needed if you want it).

This guide currently covers:
1. [Docker Desktop](#1-docker-desktop)
2. [Environment configuration](#2-environment-configuration)
3. [Build and start the containers](#3-build-and-start-the-containers)
4. [Verify the containers](#4-verify-the-containers)
5. [Run the app](#5-run-the-app)

---

## 1. Docker Desktop

| Requirement | Value |
|---|---|
| Tool | **Docker Desktop** (Docker Engine **29.6.1** tested) |
| Backend (Windows) | WSL 2 |

Docker Desktop is the only thing you need installed locally. The
`Dockerfile` (multi-stage Maven build + JRE runtime) and `docker-compose.yml`
build and run everything else — the app, PostgreSQL/pgvector, and Ollama —
inside containers.

### 1.1 Install (Windows)

1. Download **Docker Desktop** from
   [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/).
2. Run the installer and keep "Use WSL 2 instead of Hyper-V" enabled
   (the default on modern Windows 11).
3. Restart when prompted.
4. Launch Docker Desktop and wait for it to report the engine is running.

### 1.2 Verify

```powershell
docker --version
docker compose version
```

Expected output, e.g.:

```
Docker version 29.6.1, build ...
Docker Compose version v2.x.x
```

---

## 2. Environment configuration

`docker-compose.yml` reads shared settings from a `.env` file at the project
root and injects them into the `app` and `ollama`/`postgres` services'
environment, so the containers stay in sync without touching any YAML.

| Variable | Purpose | Default (`.env.example`) |
|---|---|---|
| `OLLAMA_MODEL` | Chat model name, used by both the app and the `ollama` container | `qwen2.5:7b` |
| `OLLAMA_EMBEDDING_MODEL` | Embedding model name used for RAG vectors | `nomic-embed-text` |
| `POSTGRES_DB` | Database name | `interview` |
| `POSTGRES_USER` | Database user | `postgres` |
| `POSTGRES_PASSWORD` | Database password | `postgres` |

### 2.1 Create your `.env` file

```powershell
copy .env.example .env
```

Adjust values if needed — e.g. swap `OLLAMA_MODEL` to `llama3.2:3b` for
faster responses on CPU-only hardware.

---

## 3. Build and start the containers

From the project root:

```powershell
docker-compose up --build
```

What this starts, per `docker-compose.yml`:

- **`app`** (`spring_app`) — built from the project's multi-stage
  `Dockerfile` (`maven:3.9.6-eclipse-temurin-17` build stage, running
  `mvn clean package` inside the container, then an
  `eclipse-temurin:17-jre` runtime stage). Waits for `postgres` to be
  healthy and for the `ollama` container to have started before it starts.
- **`postgres`** (`pgvector_db`) — `pgvector/pgvector:pg16`, with the
  `vector` extension already built in. Creates the database/user from the
  `POSTGRES_*` variables; a healthcheck (`pg_isready`) gates the app's
  startup.
- **`ollama`** (`ollama_ai`) — `ollama/ollama:latest`. Its entrypoint starts
  a temporary background `ollama serve`, pulls `OLLAMA_MODEL` only if it
  isn't already present in the `ollama_data` volume, stops that temporary
  server, then runs `ollama serve` as the real foreground process.

> **Important — the embedding model is *not* auto-pulled.** The `ollama`
> service's entrypoint only pulls `OLLAMA_MODEL` (the chat model).
> `OLLAMA_EMBEDDING_MODEL` (`nomic-embed-text`) must be pulled manually —
> see [4.3](#43-pull-the-embedding-model). Skipping this lets plain chat
> work while ingestion/RAG fails.

> **First run is slow.** The Maven build downloads dependencies, and the
> `ollama` container pulls `qwen2.5:7b` (~4.7 GB) — expect several minutes
> depending on your connection. Later `docker-compose up` runs reuse the
> build cache and the `ollama_data`/`pgvector_data` volumes, so they're much
> faster.

To run in the background instead of holding the terminal:

```powershell
docker-compose up --build -d
```

---

## 4. Verify the containers

### 4.1 Check container status

```powershell
docker-compose ps
```

Expect `spring_app`, `ollama_ai`, and `pgvector_db` all `Up`/`running`
(`pgvector_db` should show `healthy`).

### 4.2 Watch the chat model finish pulling

```powershell
docker logs -f ollama_ai
```

Look for `model ready, starting ollama serve...` — the chat model isn't
usable until that line appears.

### 4.3 Pull the embedding model

```powershell
docker exec -it ollama_ai ollama pull nomic-embed-text
```

This is a one-time step per `ollama_data` volume — once pulled, it
persists across `docker-compose down`/`up` cycles (as long as the volume
isn't removed).

### 4.4 Confirm both models are present

```powershell
docker exec -it ollama_ai ollama list
```

Expected output includes both your configured `OLLAMA_MODEL`
(`qwen2.5:7b` by default) and `nomic-embed-text`.

### 4.5 Confirm the vector extension

```powershell
docker exec -it pgvector_db psql -U postgres -d interview -c "\dx"
```

Confirm `vector` appears in the extensions list (created automatically by
Spring AI's `initialize-schema: true` on the app's first startup).

---

## 5. Run the app

The app is reachable at `http://localhost:8080` (mapped by
`docker-compose.yml`) — same endpoints as the manual setup.

### 5.1 Ingest the knowledge base (first run / RAG)

The vector store starts **empty**; ingestion only runs when triggered:

```powershell
curl -X POST http://localhost:8080/api/rag/ingest
```

This reads every file under `src/main/resources/docs/` (currently
`java-interview-notes.md`), splits it into chunks, embeds each chunk via
`nomic-embed-text`, and stores the vectors in PostgreSQL. The response
reports how many chunks were stored.

### 5.2 Verify end-to-end

- **Plain chat (no RAG)**:
  ```powershell
  curl "http://localhost:8080/api/ask?question=What+is+polymorphism?"
  ```
- **RAG-grounded chat** (after ingestion):
  ```powershell
  curl "http://localhost:8080/api/rag/ask?question=What+is+polymorphism?"
  ```

If both return streamed text answers, the full containerized stack
(app → Ollama, and Postgres/pgvector for the RAG path) is working
end-to-end.

### 5.3 Stop / clean up

```powershell
docker-compose down
```

Stops and removes the containers but keeps the `ollama_data` and
`pgvector_data` volumes, so pulled models and ingested vectors survive for
next time.

```powershell
docker-compose down -v
```

Also removes those volumes — the next `docker-compose up` re-pulls the
chat model and re-initializes Postgres from scratch.
