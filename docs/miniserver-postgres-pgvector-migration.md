# FairPlay mini server migration notes

## Current target

- Runtime host: `192.168.123.103`
- Database: PostgreSQL with `pgvector`
- Cache/session broker: Redis
- Upload storage: Docker volume mounted at `/app/uploads`
- Public static files: nginx `/uploads/` alias
- Deployment: GitHub Actions on `main` -> Docker Hub image -> mini server compose pull/up

## Backup artifacts checked

- `rds_fairplay_20260224_224446.sql.gz`
  - MySQL dump, schema plus data, 107 `CREATE TABLE` statements.
  - Restore target is PostgreSQL, so it should be migrated through a converter such as `pgloader` or a staged MySQL restore plus export. Do not import this dump directly with `psql`.
- `ec2_fairplay_backend_image_20260224_224816.tar.gz`
  - OCI/Docker image archive for `songchih/fairplay-backend:latest`.
- `ec2_fairplay_static_files_volume_20260224_224630.tar.gz`
  - Upload/static volume backup. It contains top-level upload directories such as `banner`, `booth`, `event`, `uploads`, and `tmpYYYY-MM-DD`.

## PostgreSQL/pgvector changes in this branch

- Spring JDBC driver changed from MySQL to PostgreSQL.
- `application.yml` now defaults `DB_URL` to a PostgreSQL URL and uses `PostgreSQLDialect`.
- RAG chunks are stored in `rag_chunks` with `embedding vector(768)`.
- The old JVM-wide chunk cache has been removed from `VectorSearchService`; vector similarity now runs in PostgreSQL via pgvector.
- Redis remains for session/chat/presence/token use, but RAG no longer relies on Redis hash/set storage.
- `RAG_PGVECTOR_AUTO_INIT=true` creates the `vector` extension, table, and indexes on startup. Disable it if the DB user cannot run `CREATE EXTENSION`.

## Mini server deploy files

- `deploy/docker-compose.miniserver.yml`
- `deploy/nginx.miniserver.conf`
- `deploy/miniserver.env.example`

Server-side `.env` should be created from `deploy/miniserver.env.example`, with real secrets filled in on the server only.

## Restore outline

1. Install/enable pgvector on the PostgreSQL server.
2. Create the target database and user.
3. Convert the MySQL dump to PostgreSQL-compatible DDL/DML. Prefer a dry-run database first.
4. Apply the converted schema/data.
5. Run `src/main/resources/db/postgres/001_pgvector_rag.sql` or start the app with `RAG_PGVECTOR_AUTO_INIT=true`.
6. Restore static files into the Docker volume used by `fairplay-static-files`.
7. Run app smoke checks: `/actuator/health`, login, upload/download, chat, SSE notification, WebSocket flows, RAG ingestion/search.

## Known risks

- The Notion SQL page is older than the dump and contains historical secrets. Treat it as stale reference only and rotate anything that was copied into runtime environments.
- Existing native SQL was partially converted, but the full JPA-vs-dump schema diff still needs a database-backed migration test.
- PostgreSQL restore into the live mini server is intentionally not automated here because it is destructive unless a fresh target database is confirmed.
