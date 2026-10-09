// ESLint flat config (ESLint 9+ / 10). Replaces the legacy .eslintrc.json.
// Behaviour is kept equal to the legacy config: *.ts gets the Angular rules,
// *.html gets template rules and Prettier (angular parser).
import { includeIgnoreFile } from '@eslint/compat';
import angular from 'angular-eslint';
import prettierRecommended from 'eslint-plugin-prettier/recommended';
import tseslint from 'typescript-eslint';
import { fileURLToPath } from 'node:url';

const gitignorePath = fileURLToPath(new URL('.gitignore', import.meta.url));

export default tseslint.config(
  includeIgnoreFile(gitignorePath),
  // openapi/ is generated from doc/oas.json and gitignored (root .gitignore)
  { ignores: ['projects/**/*', 'openapi/**'] },
  {
    files: ['**/*.ts'],
    extends: [tseslint.configs.base, ...angular.configs.tsRecommended],
    processor: angular.processInlineTemplates,
    languageOptions: {
      parserOptions: {
        project: ['tsconfig.json'],
        tsconfigRootDir: import.meta.dirname,
      },
    },
    rules: {
      // Relaxed from error to warn while the codebase still uses constructor
      // injection, opt-outs of standalone and empty lifecycle hooks (~430 hits).
      // Revisit in a separate change.
      '@angular-eslint/prefer-inject': 'warn',
      '@angular-eslint/prefer-standalone': 'warn',
      '@angular-eslint/no-empty-lifecycle-method': 'warn',
      '@angular-eslint/component-selector': ['error', { prefix: 'app', style: 'kebab-case', type: 'element' }],
      '@angular-eslint/directive-selector': ['error', { prefix: 'app', style: 'camelCase', type: 'attribute' }],
    },
  },
  {
    files: ['**/*.html'],
    extends: [...angular.configs.templateRecommended],
    rules: {
      // Relaxed like the prefer-inject group above; ngIf -> @if migration is separate work.
      '@angular-eslint/template/prefer-control-flow': 'warn',
    },
  },
  {
    // based on https://dev.to/eneajaho/configure-prettier-and-eslint-with-angular-526c
    files: ['**/*.html'],
    ignores: ['**/*inline-template-*.component.html'],
    ...prettierRecommended,
    rules: {
      'prettier/prettier': ['error', { parser: 'angular' }],
    },
  }
);
