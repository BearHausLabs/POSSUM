# POSSUM and URSA Integration

This document describes how POSSUM integrates with URSA (or any backoffice/retail suite) **without depending on Target-specific code**. The contract is standard HTTP, JSON, and configuration.

---

## 1. What POSSUM Is in the URSA World

POSSUM runs as **"URSA JavaPOS Middleware"** (see `scripts/ursa-pos-middleware.service` and the Windows service scripts): it is the process that talks to POS hardware via JavaPOS and exposes a REST API. URSA (the retail suite / backoffice) is the **client** of that API. There is no URSA-specific Java inside POSSUM — only the service display name references URSA.

---

## 2. Integration Touchpoints (All Standard HTTP / Config)

| Touchpoint | Direction | Protocol / Contract |
|------------|-----------|---------------------|
| **Device control & status** | URSA → POSSUM | REST over HTTP. URSA (or any client) calls POSSUM's `/v1/*` endpoints. |
| **CORS** | Browser (e.g. backoffice) → POSSUM | POSSUM allows origins from the `CORS_ORIGINS` environment variable. Set it so the URSA backoffice origin can call POSSUM. |
| **Device assignment** | POSSUM → your backend | At startup, POSSUM does one **outbound** HTTP GET to PostgREST (or any API you configure) to load which devices this register has. |
| **Register identity** | Config file | `possum.store.id` and `possum.workstation.id` in POSSUM config; must match how you identify the register in URSA/DB. |

---

## 3. URSA → POSSUM: The REST API

POSSUM listens on a port (e.g. 8080 by default). URSA (or any client) calls it with:

- **Base URL:** `http://<possum-host>:<port>`  
  Example: `http://localhost:8080` when URSA and POSSUM are on the same machine.

**Key paths:**

- **Aggregate health:** `GET /v1/health` — returns all devices' health (used by backoffice / health checks).
- **Per-device:**  
  - Cash drawer: `POST /v1/cashdrawer/{drawerId}/open`, `GET /v1/cashdrawer/health`, `GET /v1/cashdrawer/{drawerId}/health`, etc. (drawerId 1–4).  
  - Tone: `POST /v1/toneindicator/sound`, `GET /v1/toneindicator/health`.  
  - Printer: `POST /v1/print`, `GET /v1/printer/health`.  
  - Others: `/v1/msr`, `/v1/keylock`, `/v1/poskeyboard`, `/v1/scale`, `/v1/linedisplay`, `/v1/check`, `/v1/scanner`, etc.

Swagger UI is available at `/swagger-ui.html` when the app is running for full API documentation.

**Integration on URSA side:** Configure a "POSSUM base URL" (and optional API key if you add auth). That is done in URSA's code (e.g. a `possum.service.ts` or equivalent); POSSUM does not reference URSA.

---

## 4. POSSUM → URSA (or Your Backend): Device Config at Startup

This is the **only outbound integration** from POSSUM:

- **When:** Before any device beans are created (during Spring Boot startup).
- **What:** One GET request to the URL you configure:
  - `possum.postgrest.url` — base URL (e.g. `http://ursa-api-host:3000`).
  - `possum.postgrest.table` — table/resource name (default `device_config`).
- **Request:**  
  `GET {possum.postgrest.url}/{possum.postgrest.table}?store_id=eq.{possum.store.id}&lane_id=eq.{possum.workstation.id}`  
  So POSSUM sends a normal HTTP GET with query parameters; it does not care whether the server is "URSA" or PostgREST or another API, as long as the response shape matches.
- **Expected response:** JSON array of objects with `device_type` and `logical_name`, e.g.  
  `[{"device_type":"cashDrawer1","logical_name":"CashDrawer_NCR"},{"device_type":"toneIndicator","logical_name":"ToneIndicator"}]`

So **beyond Target's code**, integration is: **any backend that exposes that URL and returns that JSON**. That backend can be PostgREST in front of PostgreSQL (where URSA stores data), or any REST API (URSA app, BFF, etc.) that reads from your DB and returns the same structure. POSSUM does not call "URSA" by name; it calls the URL in `possum.postgrest.url`.

If the GET fails or the URL is not set, POSSUM falls back to a local cache file (`POSSUM_HOME/config/device-config-cache.json`) if present, then to no devices (nothing claimed).

---

## 5. Config That Ties POSSUM to This Register and to URSA

All of this is file or environment configuration; no Target-specific code.

- **Register identity (used in the GET above):**
  - `possum.store.id`
  - `possum.workstation.id`  
  These must match the store/lane IDs used in URSA and in the `device_config` table (or whatever your API returns).

- **Outbound device-config URL:**
  - `possum.postgrest.url`
  - `possum.postgrest.table` (optional; default `device_config`)

- **CORS (so URSA backoffice in a browser can call POSSUM):**
  - `CORS_ORIGINS` — comma-separated allowed origins, e.g. `https://backoffice.ursa.example`.

Set in `possum-config.yml` or via environment variables / `application.properties` overrides.

---

## 6. Summary

- **URSA → POSSUM:** HTTP client calls POSSUM's `/v1/*` REST API for device actions and health.
- **POSSUM → URSA (or your backend):** One HTTP GET at startup to a configurable URL for device assignment by store/lane; response is a JSON array of `device_type` and `logical_name`.
- **Config:** Register identity (`possum.store.id`, `possum.workstation.id`), device-config URL (`possum.postgrest.url`, `possum.postgrest.table`), and CORS (`CORS_ORIGINS`) complete the integration.

No proprietary or Target-only protocol is required; everything is standard HTTP, JSON, and configuration.
