# BirdDex Android — Roadmap

Native Kotlin app (Jetpack Compose) that reuses the existing production API at
`https://birddex.fun/api/*` unchanged. Auth uses better-auth's session **cookie**,
stored by a persistent OkHttp cookie jar (no backend changes).

**How to track:** each task below is one commit. Put the task ID at the end of the
commit subject, e.g. `Add login and register screens (A2.3)`, and tick the box in the
same commit. A task is done when its "Done when" line is true.

## Decisions

- **Application ID** (permanent once on Play Store): `fun.birddex.app` ✅ confirmed.
- **Code package / namespace**: `com.birddex.app`. It differs from the app ID because
  `fun` is a Kotlin keyword and would need backticks in every `package`/`import`.
- **Min SDK** 26, **target/compile SDK** 37 (Android 17).
- **Toolchain**: AGP 9.5.1 (built-in Kotlin, no `kotlin-android` plugin), Gradle 9.9.1,
  Kotlin 2.4.21, Android Studio 2026.2.2 with its bundled JDK 25.
- Admin dashboard stays **web-only**.

---

## Phase 1 — Scaffold

- [x] **A1.1 Create the Gradle project** in `android/`: Kotlin DSL, version catalog
      (`gradle/libs.versions.toml`), single `:app` module, `.gitignore` for build outputs
      and `local.properties`.
      *Done when:* `./gradlew assembleDebug` succeeds and the empty app launches.
- [x] **A1.2 Compose + Material 3 theme** with colors/typography taken from
      `frontend/src/assets/main.css`, light and dark.
      *Done when:* a preview screen renders in both themes.
      Font: Inter variable font bundled in `res/font/inter.ttf` (OFL license shipped in
      `assets/licenses/inter-OFL.txt`).
- [x] **A1.3 Build config**: `BASE_URL` per build type (`https://birddex.fun` for both
      for now), debug app ID suffix `.debug` so it can be installed next to release.
      *Done when:* `BuildConfig.BASE_URL` is readable from code.
      Debug builds can target another API with `-Pbirddex.debugBaseUrl=https://...`.
- [x] **A1.4 Hilt + navigation skeleton**: Navigation Compose with a bottom bar
      (Dex · Detect · Gallery · Account) and placeholder screens.
      *Done when:* you can tap between all four tabs.
      Type-safe routes (`ui/navigation/TopLevelDestination.kt`); tab icons are Material
      icons copied from Android Studio's bundled set into `res/drawable`.
- [x] **A1.5 Networking core**: OkHttp + Retrofit + kotlinx.serialization, persistent
      cookie jar backed by DataStore, debug-only logging interceptor, an interceptor that
      sends `Origin: https://birddex.fun` (only if A2.1 shows it's needed).
      *Done when:* `GET /api/birds` returns data in a debug log.
      Cookie jar has unit tests (`PersistentCookieJarTest`); the cookie file is excluded
      from cloud backup. The `Origin` interceptor moved to A2.2 (A2.1 showed it's needed). Release (R8) build
      verified against the live API too.
- [x] **A1.6 Docs**: add an Android section to `AGENTS.md` and `README.md`
      (how to build/run, where the API URL lives).

## Phase 2 — Auth

- [x] **A2.1 Cookie-auth spike** (go/no-go for the cookie approach): from the app or
      curl, `POST /api/auth/sign-in/email` → call `GET /api/sightings` with the cookie →
      `POST /api/auth/sign-out`. Note whether sign-out needs an `Origin` header.
      *Done when:* all three work; findings written in this file under "Notes".
      **GO.** Sign-out needs `Origin`; see Notes. The debug-only check panel
      (`ui/account/AuthCheck*`) is removed in A2.3.
- [x] **A2.2 AuthRepository**: sign in, sign up, get session (`/api/auth/get-session`),
      sign out; maps better-auth errors (e.g. unverified email) to app errors.
      From A2.1: send `Origin: https://birddex.fun` on every `/api/auth/*` request, and
      treat a `200` HTML response from `/api/*` as an error (guard against the CloudFront
      masking that A2.1 found, now fixed in production; see Notes).
      `data/auth/` (`AuthRepository`, `SessionState`, `AuthError`) plus
      `AuthOriginInterceptor` and `HtmlErrorPageInterceptor`; also resends verification
      emails. Unit-tested against MockWebServer; live check covered signed-out session,
      sign-out and a wrong password (the signed-in path was proven live in A2.1).
- [x] **A2.3 Login and register screens**: validation, loading and error states,
      "check your inbox to verify" state after sign-up.
      `ui/auth/` (Login, Register, VerifyEmail) with the website's copy and rules; the
      Account tab shows signed-in/out state with Sign out; debug check panel removed.
      Live: real sign-in → Account tab, cold restart stays signed in, sign-out works.
      Real sign-up not run live (would create a production account); unit-tested instead.

_A2.6 is done next, ahead of A2.4–A2.5: the owner's account and probably most users'
are Google-only, so they can't use email sign-in at all (see A2.1 notes). IDs keep their
original numbers._

- [ ] **A2.6 Google sign-in**: create an **Android** OAuth client in Google Cloud
      (package name + debug and release SHA-1), use Credential Manager with the existing
      **web** client ID as `serverClientId`, then
      `POST /api/auth/sign-in/social` with `{ provider: "google", idToken: { token } }`.
      *Done when:* Google sign-in lands on the Dex tab with a working session.
- [ ] **A2.4 Forgot password screen**: requests the reset email; the reset link opens
      the website for now (App Links come in A6.2).
- [ ] **A2.5 Session gate**: on launch, check the session and route to login or main;
      any `401` clears cookies and returns to login.
      *Done when:* killing and reopening the app keeps you signed in.

## Phase 3 — Birds + My Dex

- [ ] **A3.1 Birds API + models** for `GET /api/birds` (including Pokédex stats).
- [ ] **A3.2 Offline cache**: Room table for birds; repository serves the cache first,
      then refreshes from the network.
      *Done when:* the Dex shows birds in airplane mode after one online launch.
- [ ] **A3.3 Sightings API** for `GET /api/sightings`. Cache images by `image_key`,
      not the presigned URL (URLs expire and change).
- [ ] **A3.4 Dex grid screen**: seen/unseen birds, completion %.
- [ ] **A3.5 Bird detail sheet**: stats, reference photo, your sighting photos.
- [ ] **A3.6 Manage sightings**: toggle public (`PATCH /api/sightings/:id`), delete
      (`DELETE /api/sightings/:id`) with confirmation.

## Phase 4 — Detect (core feature)

- [ ] **A4.1 Image input**: Android Photo Picker plus the system camera
      (`ActivityResultContracts.TakePicture`); runtime permissions where needed.
- [ ] **A4.2 Image prep**: fix EXIF rotation, resize to max 1280 px, JPEG quality 85
      (matches `frontend/src/composables/useImageResize.js`).
- [ ] **A4.3 Detect call**: `POST /api/detect` with `{ imageBase64, topN: 3 }`; loading,
      error, cancel-on-new-photo states.
- [ ] **A4.4 Results UI**: top matches with confidence + stats card, like
      `BirdStatsCard.vue` / `DetectionResults.vue`.
- [ ] **A4.5 Save sighting**: `POST /api/sightings`
      `{ birdId, imageExt: "jpg", detectedAt, notes, public }` → `PUT` the JPEG to the
      returned `uploadUrl`; refresh the Dex.
      *Done when:* a saved sighting appears in the Dex and on the website.
- [ ] **A4.6 Report wrong match**: `POST /api/feedback` with `category: "detection"`
      and `imageExt` → `PUT` the photo to `uploadUrl`.

## Phase 5 — Gallery, account, feedback

- [ ] **A5.1 Gallery feed**: Paging 3 over `GET /api/gallery?limit&offset`.
- [ ] **A5.2 Gallery filter + detail**: species filter (`birdId`), full-screen sighting view.
- [ ] **A5.3 Account screen**: gallery anonymity toggle (`/api/account/settings`),
      delete all sightings (`DELETE /api/sightings`), sign out.
- [ ] **A5.4 Feedback form**: bug / suggestion / other via `POST /api/feedback`.
- [ ] **A5.5 Welcome / onboarding** (optional, matches the web `WelcomeModal`).

## Phase 6 — Ship

- [ ] **A6.1 App icon, adaptive icon, splash screen.**
- [ ] **A6.2 App Links**: verify-email and reset-password links on `birddex.fun` open the
      app. Needs `frontend/public/.well-known/assetlinks.json` and a frontend deploy.
- [ ] **A6.3 Release build**: signing config read from `local.properties` / env (keystore
      never committed), R8 rules for kotlinx.serialization and Retrofit.
- [ ] **A6.4 CI**: GitHub Actions workflow that runs `assembleDebug` and lint on PRs
      touching `android/`.
- [ ] **A6.5 Play Console**: listing, data safety form, privacy policy URL, internal
      testing track.

## Later (not scheduled)

- On-device detection with ONNX Runtime Android (offline use in the field).
- Switch to better-auth bearer tokens if cookies cause trouble.

## Notes

### A2.1 cookie-auth spike (2026-11-19): GO

Run against production from the debug app's check panel, with a verified
email/password test account:

- `POST /api/auth/sign-in/email` (no cookies, no `Origin`) → `200`. The jar stored
  `__Secure-better-auth.session_token` as a persistent cookie with a 7-day expiry
  (`rememberMe: true`).
- After a force-stop and cold start the cookie reloaded from DataStore:
  `GET /api/auth/get-session` → session present, `GET /api/sightings` → `200`.
- `POST /api/auth/sign-out` **without** `Origin` → rejected by better-auth's origin check,
  which applies to any auth request that carries a cookie: `403 MISSING_OR_NULL_ORIGIN`
  (confirmed once CloudFront stopped hiding it, see below).
  **With** `Origin: https://birddex.fun` → `200 {"success":true}`,
  better-auth expired the cookie, the jar dropped it, and `GET /api/sightings` → `401`.
- Accounts created with Google have no password: email sign-in answers
  `401 INVALID_EMAIL_OR_PASSWORD`. The owner's own account is Google-only, so
  Google sign-in (A2.6) matters for real users.

**CloudFront masks API errors.** `errorResponses` in `backend/lib/backend-stack.ts`
turns every 403 and 404 into `200` + `index.html` for SPA routing, and it applies to
`/api/*` too. A masked response has `content-type: text/html` and
`x-cache: Error from cloudfront`. (Errors CloudFront doesn't remap, like 401, also carry
`x-cache: Error from cloudfront` but keep their real status and JSON body.)

**Fixed in production the same day** by commit `9479de4` (SPA routing moved to a
CloudFront Function on the website behavior only), deployed from branch
`claude/quirky-mccarthy-d2f47c`, which is not on `main` yet. Checked live afterwards:
sign-out without `Origin` → `403` JSON, unknown `/api/*` route → `404`, `/mydex` still
loads the website. The app still treats `200` + HTML from `/api/*` as an error, as a
cheap guard in case the old rule ever comes back.
