# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

BirdDex — a Pokedex-style bird-tracking web app. Two independent workspaces: `frontend/` (Vue SPA) and `backend/` (AWS CDK infrastructure + Lambda handlers).

## Commands

### Frontend (`cd frontend`)
```bash
npm run dev          # Vite dev server on :5173
npm run build        # Production build → dist/
npm run lint         # oxlint + eslint (auto-fix)
npm run format       # Prettier
```

### Backend (`cd backend`)
```bash
npm run build        # TypeScript compile check (tsc)
npm test             # Jest CDK assertion tests (22 tests)
npm test -- -t "Auth Lambda"  # Run a single test by name
npx cdk synth        # Synthesize CloudFormation template
npx cdk deploy       # Deploy (requires CDK_DEFAULT_ACCOUNT and CDK_DEFAULT_REGION)
```

## Architecture

### Frontend
- **Vue 3** + Vite + Tailwind CSS v4 + Pinia stores + Vue Router
- `@` alias maps to `src/`
- Auth via `better-auth` client (`src/lib/auth-client.js`), session state in `src/stores/auth.js`
- Bird collection state in `src/stores/birds.js` — talks to `/api/sightings`, has loading/error state with optimistic update + rollback
- Gallery state in `src/stores/gallery.js` — paginated public sightings feed with bird filtering
- Admin state in `src/stores/admin.js` — user management (via better-auth admin client), bird CRUD, sighting management
- Composables: `useFocusTrap` (modal tab trapping), `useImageResize` (client-side resize to max 1280px JPEG), `useAuthAction` (loading state wrapper)
- Vite proxies `/api` to `VITE_API_PROXY_TARGET` in dev mode
- Linting: oxlint first, then eslint (flat config), prettier for formatting

### Backend — CDK Stack (`lib/backend-stack.ts`)
Single stack deploying all infra to `ap-southeast-2`. Environment parameterized via CDK context (`env`, `certArn`, `webAclArn`).

- **S3** — two buckets: one for app data (bird images, ONNX models) with 30-day noncurrent version expiry, one for frontend static hosting
- **VPC** — 2 AZs, 1 NAT, public / private-with-egress / isolated subnets; S3 Gateway Endpoint + Secrets Manager Interface Endpoint to reduce NAT costs
- **RDS PostgreSQL 16** — `t4g.micro`, isolated subnets, credentials in Secrets Manager at `/birddex/db/credentials`
- **CloudFront** — serves frontend bucket (OAC) + proxies `/api/*` to HTTP API Gateway
- **HTTP API Gateway v2** — routes: `ANY /api/auth/{proxy+}` → auth, `POST /api/detect` → detect, `ANY /api/{proxy+}` → api
- **Monitoring** — SNS alarm topic (`birddex-alarms`), CloudWatch alarms for Lambda errors + RDS CPU, 14-day log retention on all Lambdas

### Lambda Functions (`lambda/`)
All use Hono as HTTP framework. Shared utilities in `lambda/lib/` (db connections, secrets caching). All connect to RDS via SSL with CA validation (`rds-ca-bundle.pem`).

| Lambda | Runtime | Arch | Purpose |
|--------|---------|------|---------|
| `migrate/` | Node 22 | ARM64 | Custom Resource — runs `schema.sql` on deploy |
| `auth/` | Node 22 | ARM64 | better-auth (email/password + Google OAuth + Resend email) |
| `api/` | Node 22 | ARM64 | REST CRUD for birds/sightings/gallery/account, admin endpoints, S3 presigned URLs |
| `detect/` | Docker | x86_64 | ONNX bird detection inference (onnxruntime-node requires x86) |

### Shared Lambda Utilities (`lambda/lib/`)
- `db.ts` — `getDb()` (postgres driver) and `getPgPool()` (pg driver) with consistent SSL config
- `secrets.ts` — `getSecretJson<T>()` with in-memory caching

### Database Schema (`lambda/migrate/schema.sql`)
better-auth tables (`user`, `session`, `account`, `verification`) + app tables (`bird`, `sighting`). 36 Australian birds seeded. Gallery columns: `sighting.public` (boolean), `user.gallery_anonymous` (boolean).

### API Routes (`lambda/api/index.ts`)
| Route | Auth | Purpose |
|-------|------|---------|
| `GET /api/birds` | No | List all birds |
| `GET /api/sightings` | Yes | User's sightings with presigned image URLs |
| `POST /api/sightings` | Yes | Create sighting (optional `public` flag) |
| `PATCH /api/sightings/:id` | Yes | Toggle sighting `public` flag |
| `DELETE /api/sightings/:id` | Yes | Delete single sighting + S3 cleanup |
| `DELETE /api/sightings` | Yes | Bulk delete all user's sightings + S3 cleanup |
| `GET /api/gallery` | Yes | Paginated public sightings feed (limit/offset/birdId) |
| `GET /api/account/settings` | Yes | Fetch `galleryAnonymous` |
| `PATCH /api/account/settings` | Yes | Update `galleryAnonymous` |
| `GET /api/upload-url` | Yes | Presigned S3 upload URL |
| `GET/POST/PUT/DELETE /api/admin/*` | Admin | Stats, bird CRUD, sighting management |

## Key Gotchas

- **Detect Lambda must be x86_64** — `onnxruntime-node` ships only x86 native binaries. It's deployed as a Docker image because the bundle exceeds the Lambda zip size limit.
- **RDS SSL** — Node.js Lambda runtime doesn't include RDS intermediate CAs. All Lambdas that talk to Postgres must bundle `lambda/rds-ca-bundle.pem` via `afterBundling` hooks or Dockerfile `COPY`.
- **CDK deploy requires concrete env** — `CDK_DEFAULT_ACCOUNT` and `CDK_DEFAULT_REGION` must be set (VPC/RDS need them).
- **CloudFront cert** — `certArn` in `backend/cdk.context.json` controls the `birddex.fun` domain alias. Without it, CDK strips the domain.
- **CloudFront WAF** — the WAF ACL ARN is passed via `webAclArn` context key in `cdk.context.json`.
- **CDK test assertions** — `Match.arrayWith` is order-sensitive; match elements in their declaration order.
- Lambda `tsconfig.json` is separate from the CDK one (`lambda/tsconfig.json` uses `ESNext`/`bundler` for esbuild; root `tsconfig.json` uses `NodeNext` for CDK code).
- **`logRetention` deprecation warning** — CDK warns about `logRetention` in favor of `logGroup`, but it still works. Can migrate later.
- **DB migration trigger** — The `MigrateResource` Custom Resource in `backend-stack.ts` only re-runs when its `schemaVersion` property changes. After modifying `schema.sql`, bump this value (currently `"4"`) or the migration won't execute on deploy.
- **ESLint v10 broken** — `eslint-plugin-vue` incompatible with eslint v10 (`scopeManager.addGlobals` error). Pre-existing issue.

## CI/CD

GitHub Actions workflow at `.github/workflows/ci.yml` runs on push/PR to main:
- Backend: `npm ci && npm run build && npm test`
- Frontend: `npm ci && npm run build`
