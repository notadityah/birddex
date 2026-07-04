# Repository Guidelines

## Project Structure & Module Organization

BirdDex is split into a Vue 3 frontend and an AWS CDK/Lambda backend. `frontend/src/views/` contains route pages, while `frontend/src/components/` is grouped by feature (`admin/`, `auth/`, `dashboard/`, `detect/`, `gallery/`). Shared client code lives in `stores/`, `composables/`, `utils/`, `data/`, and `lib/`; static assets live in `frontend/public/` and `frontend/src/assets/`. Backend infrastructure is in `backend/lib/backend-stack.ts`; Lambda handlers and shared runtime utilities are under `backend/lambda/`. CDK tests are in `backend/test/`. Detection labels are `backend/classes.txt` (must match `bird.slug` values in `backend/lambda/migrate/schema.sql`); model binaries (`*.onnx`) are gitignored — the source of truth is the versioned app S3 bucket under `models/<version>/`, selected by the `MODEL_VERSION` constant in `backend/lib/backend-stack.ts` (see "Updating the detection model" in the README).

## Build, Test, and Development Commands

Run commands from the relevant package directory.

- `cd frontend && npm install`: install frontend dependencies.
- `cd frontend && npm run dev`: start Vite at `http://localhost:5173`.
- `cd frontend && npm run build`: build the SPA into `frontend/dist/`.
- `cd frontend && npm run lint`: run Oxlint and ESLint with auto-fixes.
- `cd frontend && npm run format`: format `frontend/src/` with Prettier.
- `cd backend && npm install`: install CDK and Lambda dependencies.
- `cd backend && npm run build`: compile TypeScript.
- `cd backend && npm test`: run Jest CDK assertions.
- `cd backend && npx cdk synth`: synthesize CloudFormation before deploys.

## Coding Style & Naming Conventions

Frontend code uses ES modules, Vue single-file components, Pinia, and Tailwind CSS. Follow `frontend/.prettierrc.json`: no semicolons, single quotes, and 100-character line width. Use PascalCase for Vue components, `useXyz.js` for composables, and camelCase elsewhere.

Backend code is TypeScript CDK plus Lambda handlers. Keep infrastructure in `backend/lib/`, runtime code in `backend/lambda/`, and shared backend helpers in `backend/lambda/lib/`.

## Testing Guidelines

Backend tests use Jest with `ts-jest`; files must match `backend/test/**/*.test.ts`. Add CDK assertions when changing resources, routes, permissions, alarms, or environment configuration. There is no frontend test runner configured, so validate frontend changes with `npm run lint`, `npm run build`, and local browser checks.

## Commit & Pull Request Guidelines

Recent commits use concise, imperative summaries such as `Add API Gateway throttle settings` or `Remove CI/CD section from README`. Keep subjects specific and one line. Pull requests should include a change summary, tests run, linked issues when relevant, screenshots for UI changes, and `cdk synth` or `cdk diff` notes for infrastructure changes.

## Security & Configuration Tips

Copy `frontend/.env.example` to `frontend/.env` for local development and do not commit secrets. Backend deploys depend on AWS/CDK credentials and Secrets Manager values; document any new required secret or context key in the relevant README.
