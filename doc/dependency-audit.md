# Dependency Audit

Snapshot of outdated dependencies in the backend (Gradle/Kotlin) and frontend (Angular/npm).
Versions "latest" were taken from npm and Maven Central at audit time. Majors are flagged **MAJOR**.

## Repository notes

- Backend: Kotlin on JVM 17, Javalin 6 web server, Xodus DB via DNQ, Jaffree (FFmpeg), Clikt/JLine CLI. Packages under `backend/src/main/kotlin/dev/dres/{api,run,mgmt,data,utilities}`.
- Frontend: Angular 20 + Angular Material, ApexCharts, Yarn 3 (`nodeLinker: node-modules`).
- API spec: `doc/oas.json` (83 paths). The root `build.gradle` generates the TypeScript Angular client into `frontend/openapi` via openapi-generator 7.10.0.
- Build: Gradle multi-project. Frontend is built through `com.github.node-gradle.node` (Node 22.12.0).

## Known issues (found during audit)

- `frontend/yarn.lock` is intentionally untracked (`.gitignore`: "Do not include the generated yarn.lock when using the build process"; it was tracked in earlier history). The stale lock seen during the audit was only the local, ignored file (16 ranges unresolved, e.g. `@angular-eslint/*` locked at 15.x). `yarn install` regenerated it; no repo change needed.
- Yarn version mismatch: `packageManager: yarn@3.2.0` vs `.yarnrc.yml` `yarnPath: yarn-3.8.3`. Fixed on the minor-update branch (`packageManager` set to `yarn@3.8.3`).
- `Dockerfile` uses `zenika/kotlin:1.4.20-jdk11`, but the backend targets Java 17 and the Gradle wrapper is 9.0. The Docker build very likely fails.

## Frontend (npm)

| Package | Repo | Latest | Notes |
|---|---|---|---|
| @angular/core, common, forms, router, platform-browser, compiler | 20.3.x | 22.2.1 | MAJOR |
| @angular/cdk, @angular/material | 20.2.14 | 22.2.2 | MAJOR |
| @angular/cli, compiler-cli, language-service | 20.3.x | 22.2.2 | MAJOR |
| @angular-devkit/build-angular | 20.3.25 | 21.2.26 | MAJOR |
| @angular/animations | 20.3.19 | npm `latest` = 20.1.8 | likely deprecated upstream; verify |
| @angular-eslint/* | declared 20.x, locked 15.x | 22.5.0 | MAJOR; lock stale |
| eslint | 8.57.1 | 10.12.0 | MAJOR (v8 EOL) |
| @typescript-eslint/* | 8.x (lock 5.17) | 8.71.1 | in range; lock stale |
| typescript | 5.8.3 | 7.0.2 | MAJOR |
| prettier | 3.x (lock 2.6.2) | 3.9.9 | in range; lock stale |
| @types/node | 22.x (installed 12.11.7) | 26.6.4 | match Node major |
| @types/jasmine | 3.6.11 | 7.0.0 | MAJOR |
| jasmine-core | 3.8.0 | 7.0.2 | MAJOR |
| jasmine-spec-reporter | 5.0.2 | 7.0.0 | MAJOR |
| karma | 6.3.20 | 6.4.4 | minor |
| karma-jasmine | 4.0.2 | 5.1.0 | MAJOR |
| karma-jasmine-html-reporter | 1.5.4 | 2.3.0 | MAJOR |
| karma-chrome-launcher | 3.1.1 | 3.2.0 | minor |
| stylelint | 15.11.0 | 17.16.0 | MAJOR |
| stylelint-config-standard | 25.0.0 | 40.0.0 | MAJOR |
| stylelint-config-standard-scss | 3.0.0 | 17.0.0 | MAJOR |
| stylelint-config-html | 1.1.0 | 2.0.0 | MAJOR |
| stylelint-config-prettier-scss | 0.0.1 | 1.0.0 | MAJOR |
| lint-staged | 12.5.0 | 17.6.0 | MAJOR |
| eslint-config-prettier | ^9 | 10.1.8 | MAJOR |
| prettier-eslint | ^16 | 17.1.2 | MAJOR |
| ts-node | 10.x (lock 8.3) | 10.9.2 | in range; lock stale |
| apexcharts | 3.x (3.54.1) | 7.8.0 | MAJOR |
| ng-apexcharts | 1.x | 3.1.0 | MAJOR |
| ngx-color-picker | ^14 | 20.1.1 | MAJOR |
| angularx-qrcode | 20.0.0 | 22.0.1 | MAJOR (tracks Angular) |
| zone.js | 0.15.1 | 0.16.3 | minor |

## Backend (Gradle, `gradle.properties` + build files)

| Dependency | Repo | Latest | Notes |
|---|---|---|---|
| Kotlin (`version_kotlin`) | 2.4.0 | 2.4.21 | patch |
| javalin + openapi plugins + openapi-annotation-processor | 6.1.6 | 7.2.3 | MAJOR; coupled |
| io.javalin.community.ssl:ssl-plugin | 6.1.6 | 6.7.0 | minor (stay on 6.x) |
| org.jline (terminal, reader, jna, builtins) | 3.26.2 | 4.4.7 | MAJOR |
| clikt | 4.4.0 | 5.1.0 | MAJOR |
| junit-jupiter | 5.11.3 | 6.1.3 | MAJOR |
| jackson-module-kotlin | 2.17.3 | 2.22.3 | minor; repo comment says coupled to javalin-openapi |
| kotlinx-serialization-json | 1.7.3 | 1.11.0 | minor |
| log4j (api/core/slf4j2/jul) | 2.24.1 | 2.26.1 | minor |
| jaffree | 2023.09.10 | 2024.08.29 | newer release line |
| kotlin-csv-jvm | 1.9.3 | 1.10.0 | minor |
| fastutil | 8.5.12 | 8.5.19 | patch |
| xodus (openAPI, environment, entity-store, vfs), dnq, fuel, picnic, jbcrypt | current | current | ok |

Gradle plugins:
- `de.undercouch.download` 5.3.1 → 5.7.0
- `org.openapi.generator` 7.10.0 → 7.26.0 (regenerate client, review diff)
- `com.github.node-gradle.node` 3.0.1 → 7.1.0 (MAJOR)

Toolchain:
- Gradle wrapper 9.0 → 9.8.1
- Node 22.12.0 pinned; Node 22 LTS latest 22.23.3; Node 24 LTS latest 24.21.0
- Yarn: `packageManager` 3.2.0 vs `yarnPath` 3.8.3; latest Yarn Berry 4.18.1
- Docker base `zenika/kotlin:1.4.20-jdk11`: badly outdated, incompatible with Java 17 target

## Caveats

- Frontend "current" versions come from the installed `node_modules`, which differs from the lockfile.
- Maven "latest" is the highest stable-looking version in `maven-metadata.xml` (pre-releases filtered). Check release notes before majors.
- Javalin 7, the openapi plugins and jackson are coupled: upgrade together and expect generated-client diffs.
