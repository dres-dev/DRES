# Major Dependency Upgrade Plan

Status: backend Phase 1 applied on branch `chore/javalin-7` (Javalin 7, jline 4, clikt 5, junit 6). Frontend Angular 21 and yarn 4 applied earlier (see git log). Angular 22 deferred.
Baseline: branch `chore/minor-dependency-updates` (commits `51d0a014`, `1f675d68`, `ad51962a`) on top of `dev`.
Companion: `doc/dependency-audit.md` (version table).

## Baseline before any major change

| Check | Result |
|---|---|
| `./gradlew distZip` | pass (2m46s baseline; also passes after minor updates) |
| `./gradlew backend:test` | 174 tests, 0 failures, 0 errors (before and after minor updates) |
| Frontend `lint:ts`, `lint:prettier` | pass |
| Frontend `lint:style` (stylelint) | **fails**: 2842 errors, pre-existing |
| Frontend `pbuild` (production build) | pass |
| Frontend Karma tests (`ng test`) | **not run**: no Chrome/Chromium installed |
| Server smoke test (packaged dist, Javalin 6.7) | pass: UI served, OpenAPI served, `/api/v2/...` returns JSON 401 unauthenticated |

Note: the smoke test needs `lib/ffmpeg/{ffmpeg,ffprobe}` executable in the dist, and an interactive console needs a non-EOF stdin.

## Repo facts that drive the plan

Backend
- Javalin is used across the API. Server setup is concentrated in `backend/src/main/kotlin/dev/dres/api/rest/RestApi.kt`, which calls `Javalin.create { ... }` at line 222 and `.start(config.httpPort)` at line 383.
- Registration already uses `it.registerPlugin(...)` for OpenAPI, Swagger, client-OpenAPI and SSL plugins.
- Exception handlers are registered after create (e.g. `.error(401)`, `.exception(Exception::class.java)` at `RestApi.kt:378-380`). This is the Javalin 7 "configured upfront" change.
- No usages of `matchedPath`, `unsafeConfig`, `createAndStart`, `javax.servlet`.
- Clikt is used in 8 files, JLine in 1, JUnit 5 in 16 test files.

Frontend
- 85 Angular components, only 3 set `changeDetection`. 19 NgModules, 0 standalone components.
- Legacy `frontend/.eslintrc.json` (46 lines). No flat config. Legacy config is not supported by ESLint 9+ defaults, and ESLint 10 removes it.
- stylelint already fails at baseline (2842 errors).
- zone.js referenced in 4 places.
- Charts/picker/QR used in ~7 files (`ng-apexcharts`, `ngx-color-picker`, `angularx-qrcode`).

## Verified constraints from upstream docs

- Javalin 7 (migration guide, javalin.io):
  - Routes, exception/error handlers, SSE and WS exception handlers must be configured in the `Javalin.create(config -> ...)` block (`config.routes.*`).
  - `createAndStart()` removed; use `Javalin.create(...).start()` or `Javalin.start(config)`.
  - Jetty 12 (`javax.servlet` → `jakarta.servlet`); Java 17 minimum.
  - `ctx.matchedPath()` → `ctx.endpoint().path()`.
  - `JavalinVue` moved to a plugin. `app.unsafeConfig().pvt` → `app.unsafe`.
  - Session handler changes for Jetty 12.
- Angular 22 (angular.dev update guide, changelog):
  - Requires **TypeScript 6.0** (5.9 and below dropped). It does not require TypeScript 7.
  - Requires Node `^22.22.3`, `^24.15.0` or `^26.0.0`. Node 20 is dropped. (Repo pins 22.23.3 after minor pass; OK.)
  - `OnPush` becomes the default change detection strategy. Components without an explicit strategy change behaviour.
  - Stricter compiler diagnostics (`nullishCoalescingNotNullable`, `optionalChainNotNullable`, `in` in templates, `data-` prefixed attributes no longer bind).
- Not verified (web search for ESLint 10 / angular-eslint 22 failed: no search provider). Confirm the compatibility matrix (ESLint × @angular-eslint × @typescript-eslint × eslint-plugin-prettier) against upstream release notes at the time of execution.

## Strategy

Principles
1. One upgrade track per branch. Merge only when its gates pass.
2. Each step changes one major (or one tightly coupled group). Angular is upgraded one major at a time (`ng update`).
3. Establish gates before changing code: restore the ability to run frontend tests locally, and capture a baseline of the generated OpenAPI spec.
4. Behaviour-preserving first, then modernise. For example, keep Angular components on `ChangeDetectionStrategy.Default` (explicit) before opting into OnPush.

### Phase 0: Prerequisites (no version bumps)

- Install Chrome/Chromium and confirm `yarn test` (Karma, headless) runs at baseline. Without it, Angular majors are not verifiable.
- Decide on stylelint: either fix the 2842 pre-existing errors or scope the rule set for the migration. Do not let it block other tracks; record as a known failure.
- Fix the Docker base image (`zenika/kotlin:1.4.20-jdk11`, Java 17 target). This is not a version bump but a blocker for the Docker path. Track separately.
- Capture a golden copy of the generated API spec: `./gradlew generateOAS` (or `distZip` then `/openapi.json`) to `doc/oas.json` before backend changes, so Javalin/Jackson/plugin changes show up as a diff.

### Phase 1: Backend majors (independent of frontend)

Order within phase (each its own commit/branch):
1. **Javalin 6.7 → 7.x** (with `javalin-openapi-plugin`, `javalin-swagger-plugin`, `openapi-annotation-processor`, `io.javalin.community.ssl:javalin-ssl` on matching 7.x; the old `ssl-plugin` artifact stops at 6.7.0).
   - Status: migrated on branch `chore/javalin-7` (commits 72880a3b, 3514ad7a). 174/0/0 tests. Live check: openapi/clientapi served, 401 and login errors match the 6.7 baseline, HTTPS with HTTP/2 works. WebSocket run-viewer connect verified by `RunExecutorWebSocketLiveTest` (commit d3b87c91, 5 tests against a real Javalin server, verified to fail on a wrong route path). Known spec differences: see commit 3514ad7a message.
   - OpenAPI document is pinned to 3.0.3, and Javalin 7's nullable output is normalised in `backend/src/main/kotlin/dev/dres/api/rest/OpenApiNullability.kt` so the generated TypeScript client does not change.
   - Move `RestApi.kt` routes, error and exception handlers into `Javalin.create { config -> config.routes... }`.
   - Replace `.start(port)` pattern with `config.jetty.port` as appropriate (check current code).
   - Check WebSocket handlers (`WsContext`, `WsConfig` used in 3 places) against Javalin 7 `config.routes.ws`.
   - Check `JsonMapper` and Jackson coupling. Re-run `doc/oas.json` diff.
   - Gate: tests green (179 now incl. WebSocket live tests), OAS diff empty or documented, smoke test (UI, OpenAPI, 401 on protected API, WebSocket connect for run viewer). All met.
2. **Jline 3 → 4** (commit 11be2910). `jline-terminal-jna` dropped (no 4.x release; `jline-terminal` 4.x pulls `jline-native`); `jline-terminal-ffm` needs Java 22, not used. Gate met: pty session starts, `user list` round-trips, `quit` exits 0, same as jline 3 baseline.
3. **Clikt 4 → 5** (commit c849757d). `help`/`printHelpOnEmptyArgs` constructor args replaced by overrides. Gate met: pty transcripts of config, user/evaluation/template/collection/openapi/exec help and list commands identical to the clikt 4 build (only paths/ports differ).
4. **JUnit 5 → 6** (commit 983cbd52). No test source changes. Gate met: 179 tests, 0 failures, 0 errors (174 before the WebSocket tests were added).

Note: Jackson 2.22.3 (minor pass) already produces two new deprecation warnings (`JsonNode.fields()`, `ObjectMapper.readValue(URL, ...)`). Resolve before or during phase 1.

### Phase 2: Frontend lint toolchain

Required before Angular 22 because `@angular-eslint` 22 and current ESLint are on flat config.
1. Migrate `.eslintrc.json` → `eslint.config.js` (flat config) while still on ESLint 8 → 9 and `@angular-eslint` 20 (supported path). Keep rules identical; gate: `lint:ts` passes with same rule results.
2. Upgrade ESLint to the version required by `@angular-eslint` 22 (10.x per current deps), with `@typescript-eslint` and `eslint-plugin-prettier` matched. Verify matrix first.
3. `lint-staged` 12 → 17 (`.husky` hooks; check `pre-commit`).
4. stylelint 15 → 17 with `stylelint-config-standard` 25 → 40 and related configs. Treat as its own track because the baseline already fails.

### Phase 3: Angular 20 → 21 → 22 (with TypeScript 5.8 → 6.0)

Gates for each step: `ng build` (production) passes, Karma passes (after Phase 0), lint passes, manual check of run/judgement/viewer views with websockets.

1. Angular 20 → 21 (`ng update @angular/core@21 @angular/cli@21`, then `@angular/material` and `@angular/cdk` 21 in the same step). Check `@angular-devkit/build-angular` 21.
2. Angular 21 → 22 plus TypeScript 5.8.3 → 6.0 (required by Angular 22). Do **not** jump to TypeScript 7 in this track; Angular 22 does not require it.
3. Change detection: 82 of 85 components have no explicit strategy and will become OnPush. Preferred approach:
   - Add explicit `changeDetection: ChangeDetectionStrategy.Default` to existing components in this track, so behaviour is unchanged.
   - Move to OnPush component by component in a later track, with reviews.
4. Compiler diagnostics: fix `nullishCoalescingNotNullable`, `optionalChainNotNullable` and `in` in templates. Check `data-` prefixed attribute bindings.
5. zone.js: `zone.js` 0.15.1 → 0.16.3 only after confirming the Angular 22 peer range. Do not bump in isolation (Angular 20 peers exclude it).
6. Angular-coupled packages in the same track: `@angular/animations` (check it is not deprecated; 3 files use it), `angularx-qrcode` 20 → 22 (1 file), `@angular/cdk`, `@angular/material` (all 22).

### Phase 4: Feature libraries (independent after Phase 3)

- `apexcharts` 3.54 → 7.x and `ng-apexcharts` 1.x → 3.x (4 files). Breaking chart option changes likely. Gate: each chart type renders in the score history and scoreboard views (manual + screenshot).
- `ngx-color-picker` 14 → 20 (2 files). Requires Angular 22 compatibility; check peer deps. Gate: colour selection in template builder/competition builder.

### Phase 5: Build tooling majors

- `com.github.node-gradle.node` 3.6 → 7.1.0 (frontend build). **Completed on `chore/jaffree-gradle-compat`**; the frontend production build has passed with the upgraded plugin.
- Gradle wrapper 9.8.1 → 10.x only after a Gradle 10 release is available and the full build is verified against it. Current 9.8.1 warning is emitted by OpenAPI Generator 7.26.0, which calls `Configuration.setVisible(false)` while creating `openApiGeneratorExtra`. Gradle reports removal in Gradle 11, not 10; current upstream plugin source still makes this call, so this is tracked as an external warning rather than worked around locally.
- Jaffree `2023.09.10` → `2024.08.29` is **completed on `chore/jaffree-gradle-compat`**. Regression gates: backend tests and distribution tasks, including FFmpeg discovery/runtime smoke checks.

## Sequencing and ownership

```
Phase 0 (prereqs)
   ├─ Phase 1 backend (javalin 7 → jline 4 → clikt 5 → junit 6)
   └─ Phase 2 lint toolchain (flat config → eslint 10 → lint-staged → stylelint)
            └─ Phase 3 Angular 21 → 22 + TS 6.0 + Material/CDK/animations/qrcode
                     └─ Phase 4 apexcharts + ngx-color-picker
Phase 5 build tooling (independent, can run any time after Phase 1)
```

Phase 1 and Phase 2 are independent and can run in parallel branches. Phase 3 depends on Phase 2.

## Risks

- **Javalin 7 / Jetty 12**: session, SSL, WebSocket behaviour changes. Highest runtime risk; covered by smoke test plus manual run-viewer check.
- **OnPush default (Angular 22)**: silent UI staleness if not handled. Mitigated by explicit `Default` first.
- **Frontend test coverage is thin**: 8 spec files. Karma must run before Phase 3 gates mean anything.
- **Lockfile is untracked** (`.gitignore`). Upgrades resolve fresh on each machine; reproducibility depends on pinned `package.json` ranges. Decide whether to track `yarn.lock` (it was tracked before) before Phase 3.
- **Docker image** is broken independent of these upgrades; fix in Phase 0.

## Decisions needed from you

1. Target scope: full track through Angular 22 + ESLint 10, or stop at Angular 21 first?
2. Yarn: keep Yarn 3.8.3 (current `yarnPath`) or move to Yarn 4 (latest 4.18.1)?
3. Commit `yarn.lock` again (reverses the `.gitignore` decision), or keep it untracked?
4. stylelint: fix the 2842 pre-existing errors, or migrate with rules scoped down?
5. Install Chrome locally for Karma so frontend tests can run?
