# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Battle of the Universe is a browser-based, OGame/WarOfGalaxy-style strategy game: a Spring Boot backend (Java 25, Spring Modulith) and an Angular 22 frontend, talking over a cookie-session REST API.

- Backend root: `src/main/java/de/kugi/dev/battleoftheuniverse/`
- Frontend root: `frontend/src/app/`

## Commands

### Backend (run from repo root)

- Run the app (dev profile is default): `./mvnw spring-boot:run`
- Build / compile: `./mvnw compile`
- Run all tests: `./mvnw test`
- Run a single test class: `./mvnw test -Dtest=FleetServiceTest`
- Run a single test method: `./mvnw test -Dtest=FleetServiceTest#methodName`
- Package: `./mvnw package`

On Windows use `mvnw.cmd` instead of `./mvnw` from `cmd.exe`; from Git Bash/PowerShell here `./mvnw` works.

The H2 database file lives at `data/battleoftheuniverse.mv.db` (gitignored). Delete it to reset all game state; it is recreated (and reseeded, in dev) on next startup.

### Frontend (run from `frontend/`)

- Dev server: `npm start` (or `ng serve`) — serves on `http://localhost:4200`, proxying `/api` to the backend on `:8080` via `proxy.conf.json`
- Build: `npm run build`
- Unit tests (Vitest): `npm test`

Backend must be running on port 8080 for the frontend dev server's API calls (and its cookie-based auth) to work.

### Docker (production-like full stack)

- `cp .env.example .env` (fill in a real `DB_PASSWORD`), then `docker compose up --build` runs the full stack: `postgres` (16-alpine), `backend` (multi-stage build, `pom.xml`'s `spring.profiles.active=prod` via `SPRING_PROFILES_ACTIVE`, port 8080), `frontend` (multi-stage build, static Angular build served by nginx, port 8081).
- The frontend's nginx (`frontend/nginx.conf`) reverse-proxies `/api/**` to the `backend` service so the browser only ever talks to one origin — session/CSRF cookies stay first-party and no CORS setup is needed for real browser traffic. `CORS_ALLOWED_ORIGINS` is still required by `application-prod.yml` for any direct (non-browser-proxied) API access.
- `backend`'s `./config` (mutable catalog JSON, see below) is a named volume (`backend-config`), so admin catalog edits survive container recreation; `postgres-data` likewise persists the database.
- Both Dockerfiles' `HEALTHCHECK`s use `wget` against `127.0.0.1` explicitly, not `localhost` — Alpine resolves `localhost` to `::1` first, and nginx/the JVM here only bind IPv4, so a `localhost` healthcheck reports false-unhealthy despite the service working fine.

## Architecture

### Spring Modulith module boundaries

The backend is organized as explicit Spring Modulith `@ApplicationModule`s (one per top-level package, declared in each package's `package-info.java`), with enforced `allowedDependencies`. Whenever you add a new cross-package call, check the target module's `allowedDependencies` list first — `ModularityTests` (`./mvnw test -Dtest=ModularityTests`) fails the build if you violate the dependency graph.

Dependency graph (who each module is allowed to call into):
- `catalog` — depends on nothing. Static game-balance data (buildings/ships/technologies).
- `user` — depends on nothing. Accounts, auth, roles.
- `planet` — depends on `user`.
- `resource` — depends on `planet`, `catalog`, `user`.
- `building` — depends on `planet`, `resource`, `catalog`, `user`.
- `research` — depends on `planet`, `resource`, `catalog`, `user`, `building`.
- `fleet` — depends on `planet`, `resource`, `catalog`, `user`, `research` (and `research::dto`).
- `combat` — depends on nothing (currently minimal: just `BattleReport`).
- `config` — an `OPEN` module (exempt from the boundary check), depends on `user`. Cross-cutting security/CORS/error-handling wiring.

Modules communicate either through direct calls allowed by the graph above, or through **Spring Modulith application events** for one-way, decoupled notification across module boundaries not covered by `allowedDependencies` — e.g. `user.UserRegistered` is published by the user module and consumed via `@ApplicationModuleListener` in `planet.UserRegistrationListener` to create a starter planet, and `planet.PlanetCreated` is consumed in `building.PlanetCreatedBuildingListener` and `resource.PlanetCreatedResourceListener` to bootstrap a new planet's buildings/resources. Prefer this event pattern over widening `allowedDependencies` when a module just needs to react to something happening elsewhere.

`DevWorldSeeder` and `DevAccountSeeder` (dev-profile-only `ApplicationRunner`s) deliberately live in the root package, outside any `@ApplicationModule`, because they wire across modules that aren't allowed to depend on each other directly (bootstrap/dev-tooling glue, not game logic).

### Per-module package shape

Most modules follow the same internal layout: an entity (JPA), a `*Repository`, a `*Service` holding business logic, a `*Controller` exposing REST endpoints under `/api/...`, and a `dto/` subpackage of request/response records exposed across the module boundary (only `dto` subpackages are ever added to `allowedDependencies`, e.g. `research::dto`).

### Versioning

The application ships as one unit (frontend and backend always deploy together), so there is a single SemVer (`MAJOR.MINOR.PATCH`) version for the whole product rather than independent frontend/backend versions. The backend's `pom.xml` `<version>` is the single source of truth (currently pre-1.0, `-SNAPSHOT` for in-progress builds); bump `frontend/package.json`'s `version` to match in the same commit whenever it changes. Pre-1.0, MINOR bumps may still contain breaking changes since the API isn't yet stable — once at 1.0.0, standard SemVer rules apply.

The version is exposed at runtime rather than duplicated into frontend code: `spring-boot-maven-plugin`'s `build-info` execution (bound in `pom.xml`) generates `META-INF/build-info.properties` at build time, which Spring Boot auto-configures into a `BuildProperties` bean; `config.VersionController` serves it at the public (`permitAll`) `GET /api/version` endpoint. The frontend's `core/version.service.ts` fetches it and `core/app-footer/app-footer.component.ts` renders it fixed bottom-left in `app.html`, visible regardless of auth state; if the call fails, it silently renders nothing rather than showing a stale/guessed version.

### Async game mechanics: schedulers + due-job pattern

Time-based mechanics (construction, research, production, shipyard queues, fleet missions) are modeled as DB rows with a completion timestamp (`ConstructionJob`, `ResearchJob`, `ShipyardJob`, `FleetMovement`), and a `@Scheduled` poller in the same module sweeps for and completes due rows: `ConstructionScheduler`/`ProductionScheduler` (building), `ResearchScheduler`, `ShipyardScheduler` and `FleetMissionScheduler` (fleet). When adding a new time-delayed mechanic, follow this same "persisted job row + scheduler poll" shape rather than in-memory timers.

### Catalog: JSON-driven game balance data

Buildings, ships, and technologies are *not* hardcoded — they're defined in JSON, validated against a JSON Schema generated at runtime from the corresponding Java record (`BuildingDefinition`, `ShipDefinition`, `TechnologyDefinition`) via `jsonschema-generator`, and served/edited through an admin UI (`AdminCatalogController` + frontend `catalog-editor.component.ts`, which renders a form from the generated schema using JSONForms).

`CatalogService` seeds `config/catalog/*.json` (gitignored, mutable at runtime) from the read-only `classpath:catalog/*.json` defaults (`src/main/resources/catalog/`) on first access, validates on every load and save, and keeps parsed definitions in memory. To change default game balance, edit `src/main/resources/catalog/*.json`; to test live edits, use the admin catalog editor or edit `config/catalog/*.json` directly (existing live files are never overwritten by the classpath defaults).

### Game icons (planets/buildings/ships)

Icon assets are static `.webp` files under `frontend/public/images/<category>/`, resolved via the `gameAsset` pipe (`core/game-asset.pipe.ts`) as `/images/<category>/<idOrKey>.webp`, with `appImgFallback` (`core/img-fallback.directive.ts`) hiding the `<img>` instead of showing a broken-image icon if the file is missing. Recommended size: 256×256 px, square, transparent background (enough headroom for ~64-96px CSS display size at up to 4x pixel density).

- **Planets** get per-instance variance: `PlanetView.imageVariant` (an int in `[0, PlanetMapper.PLANET_IMAGE_VARIANT_COUNT)`) is computed deterministically from the planet's id (`planet/dto/PlanetMapper.java`), so a given planet always shows the same image but different planets are visually varied. Assets present: `frontend/public/images/planets/0.webp` .. `5.webp` (all 6 variants, matching `PLANET_IMAGE_VARIANT_COUNT = 6`).
- **Buildings and ships** use one fixed icon per catalog `key` (no variance — the type itself is the visual identity, so `key`-based lookup is enough, no backend field needed). Assets present: `frontend/public/images/buildings/<key>.webp` for all 9 building keys. `frontend/public/images/ships/` is still empty — ship icons haven't been generated yet, so ship cards fall back to no icon via `appImgFallback` until `<key>.webp` files are added there for `light_fighter`, `cruiser`, `small_cargo`, `colony_ship`, `espionage_probe`.

### Auth model

Session-cookie auth via Spring Security form login (not JWT/OAuth): `POST /api/auth/login` and `/api/auth/register` are the only unauthenticated endpoints besides `GET /api/version`, `/actuator/health` and `/h2-console/**`; everything else under `/api/**` requires an authenticated session. CSRF is enabled with a cookie-based token repository (`CsrfCookieFilter` ensures the XSRF cookie is present); the frontend's `credentialsInterceptor` attaches cookies to every `/api` request. Roles are `PLAYER`, `MODERATOR`, `ADMIN` (`Role` enum); admin-only endpoints/routes are gated by `@PreAuthorize`-style method security on the backend and `adminGuard` on the frontend router.

### Frontend structure

Angular 22, standalone components (no NgModules), lazy-loaded routes (`app.routes.ts`) guarded by `authGuard`/`adminGuard` (`core/auth.guard.ts`). `core/` holds cross-cutting concerns (auth service/guard, HTTP interceptor, sidebar shell); `features/` holds one directory per game area (`universe`, `fleet`, `research`, `auth`, `admin`), each typically pairing a `*-api.service.ts` (HTTP calls to the matching backend controller) with one or more components. The admin catalog editor's `renderers/` implement custom JSONForms renderers for editing catalog JSON Schema-driven forms.

### Dev environment seeding

On the `dev` profile (default), `DevAccountSeeder` creates `admin`/`player` accounts and `DevWorldSeeder` creates a few NPC "enemy" accounts with pre-built colonies plus maxes out the `player` account's buildings/research/fleet, so combat and fleet features are exercisable without manual grinding. Configure via `game.dev.*` in `application-dev.yml`; disable with `game.dev.seed-accounts=false`.
