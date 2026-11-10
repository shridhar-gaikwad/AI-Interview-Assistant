# Local Environment Setup

This document covers installing and configuring the tools required to build and
run **AI Interview Assistant** directly on your machine (no Docker).

> Looking for the containerized workflow instead? See `docker_setup.md`
> (Docker Desktop only, everything else runs inside containers).

This guide currently covers:
1. [Java](#1-java)
2. [Maven](#2-maven)
3. [PostgreSQL](#3-postgresql)
4. [Ollama](#4-ollama)
5. [Spring AI](#5-spring-ai)

---

## 1. Java

| Requirement | Value |
|---|---|
| Version | **17** (LTS) |
| Distribution | Eclipse Temurin (matches the `eclipse-temurin:17-jre` / `maven:3.9.6-eclipse-temurin-17` images used in the project's `Dockerfile`) |

Using the same distribution as the Docker image avoids subtle JVM behavior
differences between your local runs and the containerized build.

### 1.1 Install (Windows)

1. Download the **Eclipse Temurin JDK 17 (LTS)** installer from
   [adoptium.net](https://adoptium.net/temurin/releases/?version=17).
2. Run the installer. During setup, enable:
   - "Set JAVA_HOME variable"
   - "Add to PATH"
3. Restart any open terminal windows so the updated environment variables load.

### 1.2 Verify

```powershell
java -version
```

Expected output should report version `17.x.x`, e.g.:

```
openjdk version "17.0.x" ...
```

Confirm `JAVA_HOME` points at the JDK 17 install directory:

```powershell
$env:JAVA_HOME
```

---

## 2. Maven

| Requirement | Value |
|---|---|
| Version | **3.9.6** |

This version matches the `maven:3.9.6-eclipse-temurin-17` image used in the
build stage of the project's `Dockerfile`, so a local `mvn` build and the
Docker build behave the same way.

> Note: `README.md`'s tech-stack table currently lists Maven 3.9.16. That value
> is out of sync with the Dockerfile and should be treated as incorrect until
> reconciled; this guide follows the Dockerfile's pinned 3.9.6.

### 2.1 Install (Windows)

1. Download the binary zip archive for Maven **3.9.6** from the
   [Apache Maven release archive](https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/)
   (`apache-maven-3.9.6-bin.zip`).
2. Extract it to a permanent location, e.g. `C:\tools\apache-maven-3.9.6`.
3. Add a `MAVEN_HOME` (or `M2_HOME`) environment variable pointing to that folder.
4. Add `%MAVEN_HOME%\bin` to your `PATH`.
5. Restart any open terminal windows.

### 2.2 Verify

```powershell
mvn -version
```

Expected output should show Maven `3.9.6` and the Java `17.x.x` runtime it's
using, e.g.:

```
Apache Maven 3.9.6
Java version: 17.0.x, vendor: Eclipse Adoptium
```

### 2.3 Build the project

From the project root:

```powershell
mvn clean package -DskipTests
```

This produces the runnable jar under `target/`.

---

## 3. PostgreSQL

| Requirement | Value |
|---|---|
| Version | **16.x** |
| Extension | **pgvector** (required — stores document embedding vectors for RAG) |

This matches the `pgvector/pgvector:pg16` image used by the `postgres` service
in `docker-compose.yml`. Running the same major version locally keeps schema
and extension behavior identical between local and containerized runs.

### 3.1 Install (Windows)

1. Download the **PostgreSQL 16** installer from
   [postgresql.org downloads](https://www.postgresql.org/download/windows/)
   (via EnterpriseDB).
2. Run the installer and note the password you set for the `postgres`
   superuser — you'll need it below.
3. Keep the default port **5432** (the project's `application.yml` and
   `docker-compose.yml` both assume this port).

### 3.2 Install the pgvector extension

The Windows installer does not bundle `pgvector`, and there are no official
prebuilt Windows binaries. If you skip this step, the app fails on startup
with:

```
org.postgresql.util.PSQLException: ERROR: extension "vector" is not available
  Detail: Could not open extension control file ".../share/extension/vector.control": No such file or directory.
```

This happens because `application.yml` sets
`spring.ai.vectorstore.pgvector.initialize-schema: true`, which runs
`CREATE EXTENSION IF NOT EXISTS vector;` on startup. That statement only
*registers* an extension whose files already exist on disk — it does not
install them. On Windows, EDB's PostgreSQL binaries are built with MSVC, so
`pgvector` must be built with the same toolchain (a plain `gcc`/MinGW build
will not be ABI-compatible).

**Validated steps (PostgreSQL 16.15, pgvector v0.8.0):**

1. **Install Visual Studio Build Tools.** Download "Build Tools for Visual
   Studio 2022" from
   [visualstudio.microsoft.com/downloads](https://visualstudio.microsoft.com/downloads/)
   and install the **"Desktop development with C++"** workload (provides
   `nmake` and the MSVC compiler).

2. **Open "x64 Native Tools Command Prompt for VS 2022" as Administrator**
   (Start Menu). A plain PowerShell/cmd window won't have the MSVC build
   environment loaded.

3. **Build and install pgvector** from that prompt:
   ```cmd
   set "PGROOT=C:\Program Files\PostgreSQL\16"
   cd %TEMP%
   git clone --branch v0.8.0 https://github.com/pgvector/pgvector.git
   cd pgvector
   nmake /F Makefile.win
   nmake /F Makefile.win install
   ```
   This compiles `vector.dll` and copies it, plus `vector.control` and
   `vector--*.sql`, into
   `C:\Program Files\PostgreSQL\16\lib` and
   `C:\Program Files\PostgreSQL\16\share\extension`.
   The cloned source under `%TEMP%\pgvector` is just build scaffolding and
   can be deleted afterward.

4. **Restart the PostgreSQL service** so it picks up the new extension files:
   ```powershell
   Restart-Service postgresql-x64-16
   ```

5. **Enable the extension** in the app's database:
   ```powershell
   psql -U postgres -h localhost -p 5432 -d interview -c "CREATE EXTENSION IF NOT EXISTS vector;"
   ```
   (Spring AI also runs this automatically on startup via
   `initialize-schema: true`, so this step mainly matters for verifying the
   build before running the app.)

> **Alternative**: if you'd rather avoid installing Visual Studio Build
> Tools, run PostgreSQL via the project's `docker-compose.yml` instead (the
> `postgres` service uses `pgvector/pgvector:pg16`, which has the extension
> pre-built), or build pgvector inside WSL2/Linux where it's a plain
> `make && make install` with no MSVC dependency.

### 3.3 Create the application database

The app expects a database and credentials matching `.env.example`
(`POSTGRES_DB=interview`, `POSTGRES_USER=postgres`). Using `psql`:

```powershell
psql -U postgres -h localhost -p 5432 -c "CREATE DATABASE interview;"
```

Enable the `vector` extension inside that database (Spring AI's pgvector
store also auto-creates this on startup via `initialize-schema: true`, but
it can be created manually too):

```powershell
psql -U postgres -h localhost -p 5432 -d interview -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

### 3.4 Verify

```powershell
psql -U postgres -h localhost -p 5432 -d interview -c "\dx"
```

Confirm `vector` appears in the extensions list.

### 3.5 Local connection settings

Copy `.env.example` to `.env` and adjust if your local credentials differ
from the defaults:

```
POSTGRES_DB=interview
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
```

When running the app **without Docker**, `application.yml` already falls
back to `jdbc:postgresql://localhost:5432/interview` with these same
defaults, so no extra configuration is needed if you keep them unchanged.

---

## 4. Ollama

| Requirement | Value |
|---|---|
| Version | **0.33.3** or later |
| Chat model | **qwen2.5:7b** (default; overridable via `OLLAMA_MODEL`) |
| Embedding model | **nomic-embed-text** (outputs 768-dim vectors; overridable via `OLLAMA_EMBEDDING_MODEL`) |
| Server endpoint | `http://localhost:11434` (matches `spring.ai.ollama.base-url` in `application.yml`) |

Ollama runs the local LLM(s) the app talks to: one model for chat/RAG answers,
and a separate, smaller model purely for turning text into embedding vectors
during ingestion and similarity search.

### 4.1 Install (Windows)

1. Download the installer from
   [ollama.com/download/windows](https://ollama.com/download/windows).
2. Run it. Ollama installs as a background app and starts automatically,
   serving its API on `http://localhost:11434`.

### 4.2 Verify

```powershell
ollama --version
curl http://localhost:11434
```

The `curl` call should return `Ollama is running`. If it doesn't, start the
Ollama app from the Start Menu (or run `ollama serve` manually).

### 4.3 Pull the required models

The app does **not** auto-pull models (`spring.ai.init.pull-model-strategy`
is set to `never` in `application.yml` — see [Section 5](#5-spring-ai)), so
both models must be pulled manually before first use:

```powershell
ollama pull qwen2.5:7b
ollama pull nomic-embed-text
```

> Tip: `qwen2.5:7b` is a ~4.7 GB download and, on CPU-only hardware,
> noticeably slow to generate full answers (this is why
> `HttpClientConfig` configures a 5-minute read timeout). For faster
> responses on limited hardware, swap in a smaller chat model such as
> `llama3.2:3b` — update `OLLAMA_MODEL` in `.env` (Docker) or
> `spring.ai.ollama.chat.options.model` in `application.yml` (local) to match.

### 4.4 Verify models are available

```powershell
ollama list
```

Expected output includes both `qwen2.5:7b` and `nomic-embed-text`.

> **Important — embedding dimension coupling:** `nomic-embed-text` produces
> 768-dimensional vectors, which must exactly match
> `spring.ai.vectorstore.pgvector.dimensions: 768` in `application.yml`. If
> you ever switch the embedding model, update `dimensions` to match its
> output size, or ingestion/similarity search will fail.

---

## 5. Spring AI

Spring AI itself needs **no separate installation** — it's a set of Maven
dependencies already declared in `pom.xml` (pinned via the
`spring-ai-bom` version `1.0.9`):

- `spring-ai-starter-model-ollama` — auto-configures the `ChatClient`/
  `ChatModel` beans from `spring.ai.ollama.*` properties.
- `spring-ai-starter-vector-store-pgvector` — auto-configures a `VectorStore`
  backed by PostgreSQL + pgvector.
- `spring-ai-advisors-vector-store` — provides `QuestionAnswerAdvisor`, the
  RAG plumbing used by `RagService`.

Running `mvn clean package` (see [Section 2.3](#23-build-the-project))
downloads all of these from Maven Central automatically — no extra
repositories or manual setup needed.

### 5.1 Configuration (already wired in `application.yml`)

| Property | Purpose |
|---|---|
| `spring.ai.ollama.base-url` | Where the Ollama server listens (`http://localhost:11434`) |
| `spring.ai.ollama.chat.options.model` | Chat model name (`${OLLAMA_MODEL:qwen2.5:7b}`) |
| `spring.ai.ollama.embedding.options.model` | Embedding model name (`${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}`) |
| `spring.ai.init.pull-model-strategy: never` | Prevents Spring AI from auto-pulling models — they must already exist (see [4.3](#43-pull-the-required-models)) |
| `spring.ai.vectorstore.pgvector.*` | Schema auto-init, HNSW index, cosine distance, 768 dimensions |
| `app.rag.*` | App-specific RAG tuning: docs location, top-k, similarity threshold |

### 5.2 Prerequisites before running the app

All of the following must be up and reachable:
1. Java 17 ([Section 1](#1-java))
2. Maven-built jar, or an IDE run configuration ([Section 2](#2-maven))
3. PostgreSQL 16 with the `vector` extension enabled ([Section 3](#3-postgresql))
4. Ollama serving, with both models pulled ([Section 4](#4-ollama))

### 5.3 Run the app

```powershell
mvn spring-boot:run
```

or, after `mvn clean package -DskipTests`:

```powershell
java -jar target/AI-Interview-Assistant-1.0-SNAPSHOT.jar
```

### 5.4 Ingest the knowledge base (first run / RAG)

The vector store starts **empty**. Ingestion only happens when explicitly
triggered — it is not run automatically on startup. Before asking any
RAG-grounded question, call:

```powershell
curl -X POST http://localhost:8080/api/
```

This reads every file under `src/main/resources/docs/` (currently
`java-interview-notes.md`), splits it into chunks, embeds each chunk via
`nomic-embed-text`, and stores the vectors in PostgreSQL. The response
reports how many chunks were stored.

### 5.5 Verify end-to-end

- **Plain chat (no RAG)**:
  ```powershell
  curl "http://localhost:8080/api/ask?question=What+is+polymorphism?"
  ```
- **RAG-grounded chat** (after ingestion):
  ```powershell
  curl "http://localhost:8080/api/rag/ask?question=What+is+polymorphism?"
  ```

If both return streamed text answers, the full stack (Java → Spring AI →
Ollama, and Postgres/pgvector for the RAG path) is working end-to-end.
