# Security / dependency vulnerability handling

This document records how this project handles known vulnerabilities (CVEs) in its
dependencies, and the result of the most recent full audit.

## Process

1. **Continuous scanning.** [`.github/dependabot.yml`](.github/dependabot.yml) watches
   the Maven (`/`) and npm (`/frontend`) manifests weekly and opens a PR per outdated
   or vulnerable dependency. This is the first line of defense — most CVEs should be
   closed by simply merging the resulting PR after CI passes.
2. **Manual audit (this document).** For a point-in-time deep check (e.g. before a
   release, or when Dependabot can't auto-resolve something because a fix requires a
   `dependencyManagement` override rather than a plain version bump — see below):
   - Backend: resolve the full, flat dependency list with
     `./mvnw dependency:list -DincludeScope=runtime -Dsort=true` (this is more
     reliable than `dependency:tree`, which collapses repeated transitive nodes).
     Cross-reference each `groupId:artifactId@version` against the
     [GitHub Advisory Database](https://github.com/advisories) (GraphQL:
     `securityVulnerabilities(ecosystem: MAVEN, package: "...")`, batchable via
     query aliases — `gh api graphql` works since it reuses the local `gh` auth).
     [OSS Index](https://ossindex.sonatype.org) is a free alternative but now requires
     an account (anonymous requests return `401`).
   - Frontend: `npm audit --json` (uses the GitHub Advisory Database directly, no
     extra tooling needed).
3. **Triage every hit — don't just read the range.** A dependency appearing in an
   advisory's vulnerable range does not automatically mean the *application* is
   exploitable. For each hit, check in order:
   - **Is our resolved version actually inside the vulnerable range?** (inclusive vs.
     exclusive bounds matter — several hits below were an exact off-by-one away from
     being "already patched".)
   - **Is the vulnerable feature/code path even reachable?** e.g. a Log4j2 API CVE
     doesn't apply if `log4j-core` isn't on the classpath and the app never touches
     Log4j2's `Message` types directly; a Cloud Foundry actuator CVE doesn't apply if
     the app isn't deployed to Cloud Foundry.
   - **Is there a fix version compatible with our stack?** Prefer the smallest bump
     that closes the CVE and stays inside what the current Spring Boot BOM otherwise
     expects, to avoid unrelated breakage.
4. **No update path → workaround, not silence.** If a library is genuinely affected
   and has no compatible fixed release, the mitigation must be to disable/gate the
   vulnerable feature, add input validation at the boundary, or pin an excluded
   transitive dependency — not to leave it unaddressed. (Not needed this round; see
   "Not affected" below for cases that looked scary but weren't reachable, and
   "Accepted risk" for the one case with no clean fix, which is low-severity and
   dev-only.)
5. **Verify, don't just patch.** Every version bump below was confirmed to exist on
   Maven Central, applied via the smallest possible change (a `dependencyManagement`
   override rather than replacing the whole Spring Boot BOM), and the full test suite
   (`./mvnw test`, 236 tests) was re-run afterwards.

## Audit: 2026-08-28

Scope: full resolved `compile`+`runtime` Maven dependency tree (122 artifacts) and the
frontend's `npm audit` report (678 packages, 46 of them production).

### Fixed this round (backend `pom.xml` → `dependencyManagement` overrides)

| Dependency | Was | CVE / GHSA | Severity | Why affected | Fix |
|---|---|---|---|---|---|
| `org.postgresql:postgresql` | 42.7.11 | [CVE-2026-54291](https://github.com/advisories/GHSA-j92g-9f8w-j867) | High | Our exact resolved version (42.7.11) sits inside the vulnerable range `>= 42.7.4, < 42.7.12`. Allows a network attacker to silently downgrade SCRAM channel-binding when the server offers a certificate algorithm the client doesn't recognize — relevant because the driver is the app's only path to Postgres in the `prod` profile (`docker-compose.yml`). | → **42.7.13** |
| `tools.jackson.core:jackson-databind` (+ `jackson-core`, `jackson-dataformat-yaml` for release-train consistency) | 3.1.4 | [GHSA-5gvw-p9qm-jgwh](https://github.com/advisories/GHSA-5gvw-p9qm-jgwh) | Moderate | Our version (3.1.4) is inside the inclusive range `>= 3.0.0, <= 3.1.4`. `@JsonView` can be bypassed for `@JsonUnwrapped` container properties on deserialization. This is Jackson 3.x (`tools.jackson`), the JSON engine Spring Boot 4 / `SecurityConfig` actually use for every `/api/**` request, not a stray transitive — but grepped the codebase and **no DTO uses `@JsonView`**, so the bypass isn't currently reachable. Patched anyway since it's a zero-risk bump and closes the gap before the annotation is ever introduced. | → **3.1.5** |
| `org.apache.logging.log4j:log4j-api` | 2.25.4 | [CVE-2026-49844](https://github.com/advisories/GHSA-qv9r-c865-cp47) | Moderate | Our version (2.25.4) is inside `>= 2.13.1, < 2.25.5`. Improper encoding of non-finite floats in `MapMessage` JSON serialization. Not reachable here: the app pulls in `log4j-to-slf4j` (a bridge to Logback), never `log4j-core`, and doesn't construct `log4j2.Message` objects directly — the vulnerable JSON layout code isn't on the classpath at all. Patched anyway (trivial, zero-risk). | → **2.25.5** |

All three were verified to resolve correctly (`dependency:list`) and the full backend
suite (236 tests) passes unchanged.

### Reviewed, not affected

Everything below appeared in at least one advisory's *vulnerable-range list* but our
resolved version is outside every range that applies to us:

- **`com.h2database:h2` 2.4.240** — patched long before 2.4.240 for both the H2
  console RCE (CVE-2021-42392) and the JNDI/XXE issues (CVE-2022-23221,
  CVE-2021-23463). Independent of the CVE fix, the app already treats the H2 console
  as dev-only in depth: `SecurityConfig.h2ConsoleEnabled` defaults to `false` and only
  `application-dev.yml` sets it `true`; `application-prod.yml` never does, and a
  deployment that accidentally falls back to the `dev` profile still can't expose an
  unauthenticated SQL console because the `permitAll`/CSRF-exempt rules are gated on
  the same property (see the comment on `SecurityConfig.h2ConsoleEnabled`).
- **Spring Framework 7.0.8 / Spring Security 7.1.0 / Spring Boot 4.1.0** — the
  2026 advisories batch (SpEL DoS, `AntPathMatcher` DoS, `UriComponentsBuilder` SSRF,
  JSP form-tag XSS, multipart smuggling, actuator auth-bypass variants, etc.) all list
  fixed versions at or below what's already in the tree — several (SpEL DoS/RCE,
  `AntPathMatcher` DoS, `UriComponentsBuilder` SSRF) are fixed in *exactly* 7.0.8,
  meaning we're already on the patched release, not one CVE-range below it. The
  Cloud Foundry actuator auth-bypass CVEs are additionally moot because the app isn't
  deployed to Cloud Foundry (no `VCAP_APPLICATION`), and the "default security filter
  chain has no authorization rule" CVE (CVE-2026-40976) doesn't apply because
  `SecurityConfig` defines an explicit `SecurityFilterChain` bean rather than relying
  on Boot's autoconfigured default.
- **`spring-boot-devtools` 4.1.0** — the remote-secret timing-attack CVE
  (CVE-2026-40972) needs `spring.devtools.remote.secret` configured, which this repo
  never sets; devtools is also `<optional>true</optional>` and excluded from the
  packaged/repackaged jar by the Boot Maven plugin, so it isn't even present in a
  built artifact.
- **`io.micrometer:micrometer-core` 1.17.0** — the HTTP/gRPC server instrumentation
  DoS (CVE-2026-40984/40983) advisory's newest listed vulnerable+patched pair is
  `1.16.5`→`1.16.6`; 1.17.0 postdates that entirely.
- **`org.yaml:snakeyaml` 2.6**, **`org.apache.commons:commons-{text,io,lang3,collections4}`**,
  **`org.apache.tomcat.embed:*` 11.0.22**, **`org.hibernate.validator:hibernate-validator` 9.1.0.Final**,
  **`org.liquibase:liquibase-core` 5.0.3**, **`ch.qos.logback:*` 1.5.34**,
  **`tools.jackson.core:jackson-core` (pre-override)** — all several major/minor
  versions past the last vulnerable range in every matched advisory.

### Accepted risk

- **`qs` (moderate, [GHSA-q8mj-m7cp-5q26](https://github.com/advisories/GHSA-q8mj-m7cp-5q26)) via `typed-rest-client` via `@stryker-mutator/core`** —
  `npm audit` flags this in the frontend, but it's nested three levels inside
  Stryker's own dependency tree (an Azure DevOps reporter integration, unrelated to
  how this repo runs mutation testing), it's a `devDependency` only (`npm test:mutation`,
  never bundled into `ng build` output), and `npm audit fix` has no non-breaking
  resolution available (confirmed via `--dry-run`) since Stryker itself hasn't
  bumped its pin on `typed-rest-client`. Zero production exposure; revisit next time
  Dependabot/`npm outdated` shows a new `@stryker-mutator/core` release.
  `npm audit` on the 46 production dependencies alone reports **0** vulnerabilities.

## Reproducing this audit

```bash
# Backend: flat resolved list, then cross-reference each GAV against GH's advisory DB
./mvnw dependency:list -DincludeScope=runtime -Dsort=true

# Frontend
cd frontend && npm audit --json
```
