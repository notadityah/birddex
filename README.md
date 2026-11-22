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
│   ├── ROADMAP.md             # Task-by-task plan and progress
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

The Android app in `android/` uses the same production API as the website (`https://birddex.fun/api/*`); there is no separate mobile backend. Sign-in reuses better-auth's session cookie, which the app keeps in a persistent cookie jar. Progress is tracked task by task in [`android/ROADMAP.md`](android/ROADMAP.md).

Android Studio bundles the JDK and installs the Android SDK on first launch (the app compiles against API 37). Open the `android/` folder in Android Studio, or build from a terminal with `JAVA_HOME` pointing at Android Studio's bundled JDK (`<Android Studio>/jbr`):

```bash
cd android
./gradlew assembleDebug        # Build app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # Install on a running emulator or connected phone
./gradlew testDebugUnitTest    # Run JVM unit tests
```

On Windows PowerShell, use `.\gradlew` instead of `./gradlew`.

- **API URL:** `BuildConfig.BASE_URL`, set by `PRODUCTION_BASE_URL` in `android/app/build.gradle.kts`. Debug builds can target another API with `-Pbirddex.debugBaseUrl=https://...` (or the same key in `~/.gradle/gradle.properties`); release builds always use production.
- **App IDs:** release is `fun.birddex.app`; debug installs next to it as `fun.birddex.app.debug` ("BirdDex Debug"). Kotlin code uses the package `com.birddex.app` because `fun` is a Kotlin keyword.
- **Debug logging:** debug builds log HTTP traffic under the `BirdDexHttp` logcat tag, with `Cookie`/`Set-Cookie` headers redacted.

#### Release builds

`./gradlew assembleRelease` builds the R8-shrunk release APK at `android/app/build/outputs/apk/release/app-release.apk`. It is signed with the **release key** when one is configured, and unsigned otherwise. The app isn't on Google Play; the signed APK is installed directly (see "Installing on a phone" below). The keystore and its passwords are never committed (`*.jks`, `*.keystore` and `local.properties` are gitignored). Keep the keystore outside the repo and back it up together with its password: updates only install over the app if they're signed with the same key.

Create the release key once (keytool asks for the password; PKCS12 keystores use the same password for the key). The alias `upload` means the same key could be the Play upload key if the app ever goes to Google Play:

```bash
keytool -genkeypair -v -keystore ~/.android/birddex-upload.jks -alias upload \
  -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=BirdDex"
```

Then point the build at it in `android/local.properties`:

```properties
birddex.release.storeFile=C:/Users/<you>/.android/birddex-upload.jks
birddex.release.storePassword=<password>
birddex.release.keyAlias=upload
birddex.release.keyPassword=<password>
```

CI can set `BIRDDEX_RELEASE_STORE_FILE`, `BIRDDEX_RELEASE_STORE_PASSWORD`, `BIRDDEX_RELEASE_KEY_ALIAS` and `BIRDDEX_RELEASE_KEY_PASSWORD` instead.

Each signing certificate (the release key, or a new one if it's ever replaced) also needs:

- an **Android OAuth client** in the Google Cloud project, with package `fun.birddex.app` and the certificate's SHA-1, or Google sign-in fails;
- an entry in `frontend/public/.well-known/assetlinks.json` with its SHA-256, published as in [`DEPLOY.md`](DEPLOY.md), or email links open the website instead of the app.

Print both fingerprints with `keytool -list -v -keystore <keystore> -alias upload`.

#### Installing on a phone

The release app (`fun.birddex.app`, "BirdDex") installs next to the debug app ("BirdDex Debug"), so both can be on one device. Any of:

- **Download:** open the newest `android-v*` release on the repo's GitHub Releases page on the phone, tap the APK, and allow installs from the browser when Android asks;
- **USB:** turn on Developer options → USB debugging on the phone, plug it in, then run `adb install -r app/build/outputs/apk/release/app-release.apk` from `android/` (`adb` is in the Android SDK's `platform-tools`);
- **Copy the file:** copy the APK to the phone (USB, Drive, email to yourself), open it, and allow installs from that app when Android asks.

Installing a newer build the same way keeps the app's data, as long as it's signed with the same key.

#### Publishing a release

Each Android release is a GitHub release with the signed APK attached, tagged `android-v<versionName>` because the repo also holds the website and backend. From the repo root:

1. Raise `versionCode` (phones only install an update with a higher one) and set `versionName` in `android/app/build.gradle.kts`, then commit and push.
2. Build and publish:

   ```bash
   (cd android && ./gradlew assembleRelease)
   cp android/app/build/outputs/apk/release/app-release.apk android/app/build/outputs/apk/release/birddex-0.1.0.apk
   git tag -a android-v0.1.0 -m "BirdDex Android 0.1.0"
   git push origin android-v0.1.0
   gh release create android-v0.1.0 android/app/build/outputs/apk/release/birddex-0.1.0.apk \
     --verify-tag --prerelease --title "BirdDex Android 0.1.0" --notes "What changed, how to install"
   ```

Drop `--prerelease` for a build you'd call stable.

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
