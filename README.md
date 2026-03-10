# BirdDex

A Pokedex-style bird-tracking web app that lets you identify Australian birds using AI-powered detection, log your sightings, and share them with the community.

## Features

- **AI Bird Detection** — Upload a photo and an ONNX machine learning model identifies the bird species from 36 Australian birds
- **Personal Dex** — Track your bird sightings with photos, dates, and notes in a Pokedex-inspired collection view
- **Public Gallery** — Browse a paginated feed of community sightings, filterable by bird species
- **Privacy Controls** — Choose which sightings to share publicly, and optionally appear anonymous in the gallery
- **Authentication** — Email/password and Google OAuth sign-in, with email verification and password reset
- **Admin Dashboard** — User management, bird CRUD, sighting moderation, and feedback review
- **Feedback System** — Users can submit feedback directly from the app

## Tech Stack

| Layer          | Technology                                                 |
| -------------- | ---------------------------------------------------------- |
| Frontend       | Vue 3, Vite, Tailwind CSS v4, Pinia, Vue Router            |
| Auth           | better-auth (email/password + Google OAuth + Resend email) |
| API            | AWS Lambda + Hono (HTTP framework)                         |
| Detection      | ONNX Runtime inference on Lambda (Docker, x86_64)          |
| Database       | PostgreSQL 16 on RDS                                       |
| Infrastructure | AWS CDK (TypeScript), HTTP API Gateway v2, CloudFront, S3  |
| CI/CD          | GitHub Actions                                             |

## Project Structure

```
bird-dex/
├── frontend/                  # Vue 3 SPA
│   └── src/
│       ├── components/
│       │   ├── admin/         # Admin dashboard tabs & modals
│       │   ├── auth/          # Auth form inputs, layout, social login
│       │   ├── dashboard/     # Bird cards, detail modal, sidebar
│       │   ├── detect/        # Image upload, detection results, sighting saver
│       │   ├── gallery/       # Gallery cards & modal
│       │   └── landingpage/   # Hero, how-it-works, CTA, footer
│       ├── composables/       # useFocusTrap, useImageResize, useAuthAction
│       ├── stores/            # Pinia stores (auth, birds, gallery, admin)
│       ├── views/             # Page-level route components
│       └── lib/               # Auth client setup
│
├── backend/                   # AWS CDK infrastructure + Lambda handlers
│   ├── lib/
│   │   └── backend-stack.ts   # Single CDK stack (S3, VPC, RDS, Lambdas, API GW, CloudFront)
│   ├── lambda/
│   │   ├── api/               # REST CRUD (birds, sightings, gallery, account, admin)
│   │   ├── auth/              # Authentication handler
│   │   ├── detect/            # ONNX bird detection inference
│   │   ├── migrate/           # DB migration (schema.sql, runs on deploy)
│   │   └── lib/               # Shared utilities (DB connections, secrets caching)
│   └── test/                  # CDK assertion tests
│___
```

## Getting Started

### Prerequisites

- Node.js 22+
- AWS CLI configured (for backend deployment)
- AWS CDK CLI (`npm install -g aws-cdk`)

### Frontend

```bash
cd frontend
npm install
npm run dev          # Dev server on http://localhost:5173
```

Set `VITE_API_PROXY_TARGET` in `.env` to proxy `/api` requests to your backend in dev mode.

### Backend

```bash
cd backend
npm install
npm run build        # TypeScript compile check
npm test             # Run CDK assertion tests
npx cdk synth        # Synthesize CloudFormation template
npx cdk deploy       # Deploy (requires CDK_DEFAULT_ACCOUNT and CDK_DEFAULT_REGION)
```

## API Routes

| Route                       | Auth  | Description                                |
| --------------------------- | ----- | ------------------------------------------ |
| `GET /api/birds`            | No    | List all birds                             |
| `GET /api/sightings`        | Yes   | User's sightings with presigned image URLs |
| `POST /api/sightings`       | Yes   | Create a sighting                          |
| `PATCH /api/sightings/:id`  | Yes   | Toggle sighting public visibility          |
| `DELETE /api/sightings/:id` | Yes   | Delete a sighting                          |
| `GET /api/gallery`          | Yes   | Paginated public sightings feed            |
| `GET /api/upload-url`       | Yes   | Presigned S3 upload URL                    |
| `POST /api/detect`          | Yes   | Run bird detection on an uploaded image    |
| `ANY /api/auth/{proxy+}`    | —     | Auth endpoints (better-auth)               |
| `ANY /api/admin/*`          | Admin | Admin management endpoints                 |
