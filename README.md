# BirdDex

A Pokedex-style bird-tracking web app that lets you identify Australian birds using AI-powered detection, log your sightings, and share them with the community. A native Android app is in progress in [`android/`](android/).

## Features

- **AI Bird Detection** — Upload a photo and an ONNX machine learning model identifies the bird species from 36 Australian birds
- **Pokédex Stats Card** — Detection results show a stats card for each species (wingspan, habitat, diet, rarity, fun facts) with a side-by-side photo comparison and your dex completion %
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
| Database       | Neon (serverless PostgreSQL 17)                            |
| Infrastructure | AWS CDK (TypeScript), HTTP API Gateway v2, CloudFront, S3  |
| Android app    | Kotlin, Jetpack Compose, Material 3, Hilt, Retrofit/OkHttp |

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
│       ├── composables/       # useFocusTrap, useImageResize, useAuthAction, useAdminPagination
│       ├── data/              # Bird reference photos attribution map
│       ├── stores/            # Pinia stores (auth, birds, gallery, admin)
│       ├── views/             # Page-level route components
│       └── lib/               # Auth client setup
│
├── backend/                   # AWS CDK infrastructure + Lambda handlers
│   ├── lib/
│   │   └── backend-stack.ts   # Single CDK stack (S3, Lambdas, API GW, CloudFront)
│   ├── lambda/
│   │   ├── api/               # REST CRUD (birds, sightings, gallery, account, admin)
│   │   ├── auth/              # Authentication handler
│   │   ├── detect/            # ONNX bird detection inference
│   │   ├── migrate/           # schema.sql — DB schema + seed data (applied manually)
│   │   └── lib/               # Shared utilities (DB connections, secrets caching)
│   └── test/                  # CDK assertion tests
│
├── android/                   # Native Android app (Kotlin, Jetpack Compose)
│   └── app/src/
│       ├── main/java/com/birddex/app/
│       │   ├── data/network/  # Retrofit API, persistent cookie jar
│       │   ├── di/            # Hilt modules
│       │   └── ui/            # Theme, navigation, one package per tab
│       └── test/              # JVM unit tests
│___
```

## Getting Started

### Prerequisites

- Node.js 22+
- AWS CLI configured (for backend deployment)
- AWS CDK CLI (`npm install -g aws-cdk`)
- Android Studio 2026.2+ (only for the Android app)

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

### Android app

The native app in `android/` uses the same API and accounts as the website; it has no backend of its own.

**Install it on a phone:** open the newest `android-v*` release on the [Releases page](https://github.com/notadityah/birddex/releases) on your phone, download the APK, open it and allow the install. It isn't on Google Play.

**Build it** in Android Studio (open the `android/` folder), or from a terminal with `JAVA_HOME` set to Android Studio's JDK (`<Android Studio>/jbr`; in PowerShell use `.\gradlew`):

```bash
cd android
./gradlew installDebug         # Debug app ("BirdDex Debug") on an emulator or phone
./gradlew testDebugUnitTest    # Unit tests
./gradlew assembleRelease      # Signed release APK in app/build/outputs/apk/release/
```

**Release signing:** release builds are signed with the keystore set in `android/local.properties`, which is never committed. Back up the keystore and its password: updates only install over the app when they're signed with the same key.

```properties
birddex.release.storeFile=C\:/Users/<you>/.android/birddex-upload.jks
birddex.release.storePassword=<password>
birddex.release.keyAlias=upload
birddex.release.keyPassword=<password>
```

A new signing key also needs an Android OAuth client in Google Cloud (package `fun.birddex.app`, the key's SHA-1) and an entry in `frontend/public/.well-known/assetlinks.json` (its SHA-256, published as in [`DEPLOY.md`](DEPLOY.md)), or Google sign-in and email links won't work.

**Publish a release:** raise `versionCode` and `versionName` in `android/app/build.gradle.kts`, build, then tag `android-v<version>` and attach the APK with `gh release create android-v<version> <apk> --prerelease`.

### Database

The app connects to a Neon PostgreSQL database via the `DATABASE_URL` stored in the `/birddex/app/secrets` Secrets Manager secret. Apply the schema by running `backend/lambda/migrate/schema.sql` against the database (it's idempotent — safe to re-run):

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 --single-transaction -f backend/lambda/migrate/schema.sql
```

### Updating the detection model

Model binaries are not stored in git — the detect Lambda downloads them from the app S3 bucket at cold start, using the `MODEL_S3_KEY`/`CLASSES_S3_KEY` env vars set by CDK. To roll out a new model:

1. Export the model (Ultralytics YOLO detect, 640×640, 36 classes matching `backend/classes.txt` — labels must equal `bird.slug` values in `schema.sql`).
2. Upload both artifacts to the next version prefix (bucket name is in the `BirddexBucketName` stack output):
   ```bash
   aws s3 cp best.onnx            s3://<bucket>/models/v<N>/model.onnx
   aws s3 cp backend/classes.txt  s3://<bucket>/models/v<N>/classes.txt
   ```
3. Bump the `MODEL_VERSION` constant in `backend/lib/backend-stack.ts` and `npx cdk deploy`. The env var change recycles all Lambda containers, so the new model takes effect atomically.
4. Rollback: revert `MODEL_VERSION` and redeploy — previous versions stay in S3 untouched.

## API Routes

| Route                         | Auth  | Description                                    |
| ----------------------------- | ----- | ---------------------------------------------- |
| `GET /api/birds`              | No    | List all birds with Pokédex stats              |
| `GET /api/sightings`          | Yes   | User's sightings with presigned image URLs     |
| `POST /api/sightings`         | Yes   | Create a sighting                              |
| `PATCH /api/sightings/:id`    | Yes   | Toggle sighting public visibility              |
| `DELETE /api/sightings/:id`   | Yes   | Delete a sighting                              |
| `DELETE /api/sightings`       | Yes   | Delete all of the user's sightings             |
| `GET /api/gallery`            | Yes   | Paginated public sightings feed                |
| `GET /api/account/settings`   | Yes   | Get account settings                           |
| `PATCH /api/account/settings` | Yes   | Update account settings (e.g. gallery privacy) |
| `POST /api/detect`            | Yes   | Run bird detection on an uploaded image        |
| `POST /api/feedback`          | Yes   | Submit user feedback                           |
| `ANY /api/auth/{proxy+}`      | —     | Auth endpoints (better-auth)                   |
| `ANY /api/admin/*`            | Admin | Admin management endpoints                     |
