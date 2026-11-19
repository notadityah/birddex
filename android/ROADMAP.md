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

- [x] **A2.6 Google sign-in**: create an **Android** OAuth client in Google Cloud
      (package name + debug and release SHA-1), use Credential Manager with the existing
      **web** client ID as `serverClientId`, then
      `POST /api/auth/sign-in/social` with `{ provider: "google", idToken: { token } }`.
      *Done when:* Google sign-in lands back in the app with a working session.
      "or continue with / Google" on sign-in and register (`ui/auth/GoogleSignIn*`).
      `BuildConfig.GOOGLE_WEB_CLIENT_ID` is the backend's web client; Google Cloud project
      `bird-dex-487702` has an Android client for `fun.birddex.app.debug` with this PC's
      debug SHA-1 (other machines need their own). No backend changes. Live: Google
      sign-in → 200, survives a cold restart, sign-out works.
      **Still to do:** an Android client for the release ID `fun.birddex.app` with the
      release key's SHA-1, once release signing exists (A6.3).
- [x] **A2.4 Forgot password screen**: requests the reset email; the reset link opens
      the website for now (App Links come in A6.2).
      "Forgot password?" on sign-in (carries the typed email) → `ForgotPasswordScreen` →
      `POST /api/auth/request-password-reset` with `redirectTo: <site>/reset-password`,
      same as the website. Live check used a made-up address (better-auth answers 200 and
      sends nothing); a real reset email was not sent.
- [x] **A2.5 Session gate**: on launch, check the session and route to login or main;
      any `401` clears cookies and returns to login.
      *Done when:* killing and reopening the app keeps you signed in.
      `SessionGateViewModel` picks the sign-in flow (no tabs) or the tabs. No saved cookie →
      sign-in instantly, no network; saved cookie → tabs instantly, confirmed in the
      background (offline stays in the app). `UnauthorizedInterceptor` + `SessionExpiry`:
      a 401 from `/api/` (not `/api/auth/`) signs out. Live: signed-out launch → sign-in
      with zero network calls; kill + reopen while signed in → tabs, get-session 200.
      The 401 path is unit-tested only until Phase 3 calls signed-in endpoints.

## Phase 3 — Birds + My Dex

- [x] **A3.1 Birds API + models** for `GET /api/birds` (including Pokédex stats).
      `data/birds/` (`Bird`, `Rarity`, `BirdRepository`) and a shared `ApiResult`/`ApiError`
      + `apiCall` for all non-auth endpoints. Tested against a saved copy of the live
      response (`src/test/resources/fixtures/birds.json`, 36 birds).
      For A3.5: reference photos are on the website at `/birds/<slug>.jpg`; their
      CC BY / CC BY-SA licences require the author credit listed in
      `frontend/src/data/birdImages.js`.
- [x] **A3.2 Offline cache**: Room table for birds; repository serves the cache first,
      then refreshes from the network.
      *Done when:* the Dex shows birds in airplane mode after one online launch.
      Room 2.8.5 (`BirdDexDatabase` v1, schemas in `app/schemas/`), `OfflineFirstBirdRepository`
      (failed or empty refreshes keep the saved list). The Dex tab shows a simple list with
      pull-to-refresh until A3.4. Device tests in `androidTest/` (Room). Live: airplane-mode
      cold start shows all 36; the R8 release build loads the list too. Coroutines pinned to
      1.11.0 so the app matches its test libraries.
- [x] **A3.3 Sightings API** for `GET /api/sightings`. Cache images by `image_key`,
      not the presigned URL (URLs expire and change).
      `data/sightings/` (`Sighting`, offline-first `SightingRepository`), saved in Room
      (database v2, automatic migration from v1, tested on device). Sightings are wiped on
      sign-out or session expiry (`UserDataCleaner`), and `birddex.db` is excluded from
      backups. The Dex shows "You've seen N of 36". Live: `/api/sightings` 200 with the
      owner's account (2 species), offline count kept, R8 release OK. The 401 path still
      hasn't happened live (needs a server-side expired session). Image loading/caching by
      `imageKey` lands with the first photos (A3.5).
- [x] **A3.4 Dex grid screen**: seen/unseen birds, completion %. Replaces A3.2's simple list.
      Matches the website's MyDex: "You've found N of M birds", progress bar + % (rounded
      down), All / Found / Not Found chips (kept across rotation), 2-column cards with the
      newest sighting photo and a Found badge, origami placeholder otherwise. Photos load
      with Coil 3 via `ui/components/SightingPhoto`, cached by `imageKey`. Live: photos
      online, from cache in airplane mode, in dark mode, and in an R8 release build.
- [x] **A3.5 Bird detail sheet**: stats, reference photo, your sighting photos.
      Bottom sheet from any dex card, like the website's BirdDetailModal: "N sightings",
      the newest photo uncropped, `BirdStatsCard` (rarity, conservation, Wingspan/Length/
      Weight/Diet, Appearance/Habitat/Call, fun fact) and the sightings list (date, shared,
      notes). **Stats unlock only once found**, as on the website; unfound birds show "Not
      yet found" and "Go to Detect". The website's modal has no reference photo, so that
      moved to A4.4. Checked on the emulator, light and dark. One unreproduced glitch: on
      the very first open, an unfound bird's sheet seemed to appear late; 5 reruns were fine.
- [x] **A3.6 Manage sightings**: toggle public (`PATCH /api/sightings/:id`), delete
      (`DELETE /api/sightings/:id`) with confirmation. In the detail sheet's sighting rows,
      like the website's globe toggle; also "Download image" (save the photo to the phone).
      Share toggle is optimistic with rollback (like the website); delete waits for the
      server and asks first (the website has no per-sighting delete for users); a 404 means
      "already gone" and removes it locally. Save photo copies from Coil's cache to
      Pictures/BirdDex (MediaStore; storage permission only on Android 8–9). Live: share
      (`PATCH public:true` → 200, confirmed after a fresh fetch) and save photo. A confirmed
      delete ran live in A4.5 (`DELETE` → 200, gone after a fresh fetch). Not yet run live:
      unshare.

## Phase 4 — Detect (core feature)

- [x] **A4.1 Image input**: Android Photo Picker plus the system camera
      (`ActivityResultContracts.TakePicture`); runtime permissions where needed.
      No permission is needed: the picker grants just the chosen photo, and the camera app
      writes into a FileProvider file (declaring CAMERA would make it required). The photo
      is copied into `cache/detect/` (`CacheDetectPhotoStore`, max 50 MB) and its path kept
      in `SavedStateHandle`, so it survives tab switches and the app being killed, including
      while the camera is open. Checked on the emulator: pick, take, back out of the camera,
      change photo, kill-and-restore for both, dark mode.
- [x] **A4.2 Image prep**: fix EXIF rotation, resize to max 1280 px, JPEG quality 85
      (matches `frontend/src/composables/useImageResize.js`). The website also makes a
      2048 px, quality 0.92 copy for the sighting upload (`DetectPage.vue`).
      `BitmapPhotoPreparer` makes both as soon as a photo is chosen (or restored), decoding
      once with `inSampleSize` so a 50 MP photo never sits in memory at full size. Browsers
      apply EXIF rotation themselves; Android's decoder doesn't, so it reads the tag
      (`androidx.exifinterface`) and turns or un-mirrors the pixels; outputs carry no EXIF.
      Sizes round like JavaScript's `Math.round`; transparency goes on white. A photo that
      can't be decoded is dropped with "Couldn't read that photo". Device tests
      (`PhotoPreparerTest`) cover the sizes, three orientations, PNG transparency and
      non-photos; with rotation switched off, exactly the three orientation tests fail.
      The JPEGs stay in memory only, so A4.3/A4.5 read them from `DetectUiState.prepared`.
- [x] **A4.3 Detect call**: `POST /api/detect` with `{ imageBase64, topN: 5 }` (5, as the website); loading,
      error, cancel-on-new-photo states.
      `NetworkDetectRepository` sends `PreparedPhoto.detectJpeg`; the answer
      (`predictions`, `bird`, `birds` keyed by slug) reuses the `Bird` model. "Detect bird"
      is enabled once the photo is prepared; "Detecting…" shows the website's cold-start
      hint; errors show with Retry (offline gets its own message); changing the photo
      cancels the call. Results are a plain list for now (name, "Best match", confidence),
      replaced in A4.4. The debug HTTP log keeps /api/detect to headers, since its body is
      the whole photo. Live: a cold start took 15 s (House Sparrow 78.23%); offline then
      Retry, a bird-less photo ("No birds detected"), and Change photo mid-call
      (`Canceled` after 172 ms) all checked, plus the same detection on a release (R8) build.
- [x] **A4.4 Results UI**: top matches with confidence + stats card, like
      `BirdStatsCard.vue` / `DetectionResults.vue`. Reuse `ui/components/BirdStatsCard` and
      add its reference photo (`<site>/birds/<slug>.jpg`) with the photographer credit and
      licence link from `frontend/src/data/birdImages.js` (CC BY / CC BY-SA require it).
      As on the website's Detect page, the reference photo sits beside the user's photo
      (not in the stats card), captioned "Reference — <name>" with "Photo: <author>
      (<licence>)", the author linking to Wikimedia Commons. Credits are copied into
      `data/birds/BirdImages.kt` (36, one CC0; a test checks every bird has one); photos
      load from the site through Coil. Matches show a thumbnail, name, "Best match",
      confidence and bar, and act as radio buttons; tapping one swaps the reference photo
      and stats without a request. Matches the server sent no bird row for can't be picked.
      Checked live with four of the website's reference photos copied to the emulator:
      Little Wattlebird 70.39% / Red Wattlebird 56.9%, picking the second swapped photo,
      credit and stats; the credit link opened commons.wikimedia.org; dark mode.
- [x] **A4.5 Save sighting**: `POST /api/sightings`
      `{ birdId, imageExt: "jpg", detectedAt, notes, public }` → `PUT` the JPEG to the
      returned `uploadUrl`; refresh the Dex.
      *Done when:* a saved sighting appears in the Dex and on the website.
      Then delete that test sighting from the app to run A3.6's delete live.
      `detectedAt` is left out, as on the website (the server uses now); blank notes are
      left out too. `SightingRepository.create` uploads the 2048 px JPEG; if the upload
      fails it deletes the new row again (the website leaves a photo-less sighting), then
      refreshes. The form copies SightingSaver.vue, including "You've already recorded
      this bird"; "Change photo" and match picking are locked while saving. After saving,
      the photo is deleted and "Sighting saved!" shows the Dex progress, with "View in
      MyDex" / "Detect another" (both start Detect over, like the website). The debug log
      redacts the presigned link's signature, credential and token. Live, with the
      owner's OK: a private House Sparrow test sighting (POST 201, S3 PUT 200, 342 KB)
      showed in the Dex with its note and photo (the website reads the same
      `/api/sightings`), then was deleted from the Dex sheet. Release (R8) build: an
      offline save fails cleanly; not saved for real there, to avoid a second test row.
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
      never committed), R8 rules for kotlinx.serialization and Retrofit. Then add a Google
      Cloud Android OAuth client for `fun.birddex.app` with the release (and Play app
      signing) SHA-1, or Google sign-in won't work in release builds.
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
