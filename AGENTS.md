# Repository Guidelines

## Project Structure & Module Organization

BirdDex is split into a Vue 3 frontend, an AWS CDK/Lambda backend, and a native Android app. `frontend/src/views/` contains route pages, while `frontend/src/components/` is grouped by feature (`admin/`, `auth/`, `dashboard/`, `detect/`, `gallery/`). Shared client code lives in `stores/`, `composables/`, `utils/`, `data/`, and `lib/`; static assets live in `frontend/public/` and `frontend/src/assets/`. Backend infrastructure is in `backend/lib/backend-stack.ts`; Lambda handlers and shared runtime utilities are under `backend/lambda/`. CDK tests are in `backend/test/`. Detection labels are `backend/classes.txt` (must match `bird.slug` values in `backend/lambda/migrate/schema.sql`); model binaries (`*.onnx`) are gitignored — the source of truth is the versioned app S3 bucket under `models/<version>/`, selected by the `MODEL_VERSION` constant in `backend/lib/backend-stack.ts` (see "Updating the detection model" in the README).

The Android app lives in `android/` (single `:app` Gradle module; versions in `android/gradle/libs.versions.toml`). It calls the same production API as the website, so it has no backend of its own. Kotlin sources are under `android/app/src/main/java/com/birddex/app/`: `data/` for API and storage code, `di/` for Hilt modules, and `ui/` for Compose code with one package per tab (`dex/`, `detect/`, `gallery/`, `account/`) plus `theme/`, `navigation/`, and shared `components/`. Strings live in `res/values/strings.xml`. The plan and progress are in `android/ROADMAP.md`.

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
- `cd android && ./gradlew assembleDebug`: build the debug APK (needs `JAVA_HOME` set to Android Studio's bundled JDK, `<Android Studio>/jbr`; use `.\gradlew` in PowerShell).
- `cd android && ./gradlew installDebug`: install the debug app on a running emulator or connected phone.
- `cd android && ./gradlew testDebugUnitTest`: run Android JVM unit tests.
- `cd android && ./gradlew assembleRelease`: build the R8-shrunk release APK, signed with the release key from `android/local.properties` (unsigned without one; see "Release builds" in the README).
- Android on-device tests (`android/app/src/androidTest/`): `./gradlew connectedDebugAndroidTest` works but uninstalls the app afterwards, wiping its sign-in. To keep app data, run `./gradlew installDebug assembleDebugAndroidTest`, `adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`, then `adb shell am instrument -w fun.birddex.app.debug.test/androidx.test.runner.AndroidJUnitRunner`.

## Coding Style & Naming Conventions

Frontend code uses ES modules, Vue single-file components, Pinia, and Tailwind CSS. Follow `frontend/.prettierrc.json`: no semicolons, single quotes, and 100-character line width. Use PascalCase for Vue components, `useXyz.js` for composables, and camelCase elsewhere.

Backend code is TypeScript CDK plus Lambda handlers. Keep infrastructure in `backend/lib/`, runtime code in `backend/lambda/`, and shared backend helpers in `backend/lambda/lib/`.

Android code is Kotlin with Jetpack Compose and Material 3, using the official Kotlin code style. Use PascalCase for composables and classes, `XyzScreen` for a tab's screen and `XyzViewModel` for its `@HiltViewModel`. Take colors, type, and shapes from `MaterialTheme` (defined in `ui/theme/`) rather than hard-coding them, and put user-visible text in string resources. The app ID is `fun.birddex.app`, but the code package is `com.birddex.app` because `fun` is a Kotlin keyword.

## Testing Guidelines

Backend tests use Jest with `ts-jest`; files must match `backend/test/**/*.test.ts`. Add CDK assertions when changing resources, routes, permissions, alarms, or environment configuration. There is no frontend test runner configured, so validate frontend changes with `npm run lint`, `npm run build`, and local browser checks.

Android unit tests use JUnit 4 under `android/app/src/test/` and run on the JVM with `./gradlew testDebugUnitTest`. Code that sign-in depends on (cookies, auth state) needs unit tests. Check UI changes on an emulator, and check changes to networking or serialization with a release (R8) build as well, since shrinking can break them.

## Commit & Pull Request Guidelines

Recent commits use concise, imperative summaries such as `Add API Gateway throttle settings` or `Remove CI/CD section from README`. Keep subjects specific and one line. Pull requests should include a change summary, tests run, linked issues when relevant, screenshots for UI changes, and `cdk synth` or `cdk diff` notes for infrastructure changes.

Android work follows `android/ROADMAP.md`: one roadmap task per commit, with the task ID at the end of the subject (e.g. `Add login and register screens (A2.3)`), and the task's checkbox ticked in the same commit.

## Security & Configuration Tips

Copy `frontend/.env.example` to `frontend/.env` for local development and do not commit secrets. Backend deploys depend on AWS/CDK credentials and Secrets Manager values; document any new required secret or context key in the relevant README.

For Android, never commit `android/local.properties`, signing keystores (`*.jks`, `*.keystore`), or their passwords; all are gitignored. The session cookie is the user's credential: never log it (the debug HTTP logger redacts `Cookie`/`Set-Cookie`), and keep its file excluded from backups in `res/xml/`.
