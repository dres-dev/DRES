#!/usr/bin/env node
/*
 * Makes sure the generated API client exists before the frontend is built, tested or served.
 *
 * The client in `frontend/openapi/` is generated from `doc/oas.json` and deliberately not committed
 * (`.gitignore`, "Do not include the generated client bindings"): it is 126 files of pure output. The cost of
 * that decision is that a fresh clone cannot build the frontend at all - the Angular build fails with
 * `Could not resolve "../../openapi"`, which says nothing about how to fix it. Gradle-driven builds are fine
 * (`buildFrontend` depends on `openApiGenerate`), so this only matters when the frontend is driven directly
 * with `yarn`.
 *
 * This script closes that gap by generating the client on demand. It never regenerates an existing client:
 * doing that on every build would undo local edits while debugging the spec, and clearing the directory is
 * the caller's way of asking for a fresh one.
 */
import { existsSync, readdirSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const frontendDir = resolve(here, '..');
const repoRoot = resolve(frontendDir, '..');
const clientDir = join(frontendDir, 'openapi');

/** The generator always writes these; their absence means an empty or partial directory. */
function isUsable(dir) {
  if (!existsSync(dir)) return false;
  try {
    return readdirSync(dir).includes('index.ts') && existsSync(join(dir, 'model'));
  } catch {
    return false;
  }
}

if (isUsable(clientDir)) {
  process.exit(0);
}

console.log('The generated API client is missing; generating it from doc/oas.json...');

const gradle = spawnSync('./gradlew', ['openApiGenerate', '--console=plain'], {
  cwd: repoRoot,
  stdio: 'inherit',
  shell: process.platform === 'win32',
});

if (gradle.error) {
  console.error(`Could not run Gradle: ${gradle.error.message}`);
  process.exit(1);
}

if (gradle.status !== 0 || !isUsable(clientDir)) {
  console.error(
    [
      '',
      'Could not generate the API client in frontend/openapi.',
      'Run it by hand to see the error:',
      '',
      '    ./gradlew openApiGenerate',
      '',
      'The generator reads doc/oas.json, so that file must be present and valid.',
    ].join('\n')
  );
  process.exit(1);
}

console.log('API client generated.');
