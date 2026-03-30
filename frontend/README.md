# BirdDex Frontend

Vue 3 single-page application for BirdDex — a Pokedex-style bird-tracking app for Australian birds. Built with Vite, Tailwind CSS v4, Pinia for state management, and better-auth for authentication.

## Directory Structure

```
src/
  views/          # Page-level components (one per route)
  components/     # Reusable UI components, organized by feature
  stores/         # Pinia stores (auth, birds)
  composables/    # Shared reactive logic (useAnimation, useImageResize)
  router/         # Vue Router config with auth guards
  lib/            # Auth client setup (better-auth)
  assets/         # Static assets, CSS
  utils/          # Utility functions
  data/           # Static data files
```

## Dev Setup

```bash
npm install
cp .env.example .env    # Edit with your API URL
npm run dev             # Starts on http://localhost:5173
```

### Environment Variables

See `.env.example` for all variables. Key ones:

- `VITE_API_URL` — Backend API base URL (CloudFront or API Gateway)
- `VITE_DEV_BYPASS_AUTH` — Set `true` to skip auth in local dev (auto-logs in as admin)
- `VITE_ENABLE_GOOGLE_AUTH` — Show/hide Google OAuth button

## Key Patterns

### Auth Guards (`router/index.js`)
Routes use `meta` fields: `requiresAuth`, `requiresAdmin`, `guestOnly`. The global `beforeEach` guard waits for the session to resolve, then redirects based on auth state.

### Store Structure (`stores/`)
- **auth.js** — Wraps better-auth's reactive `useSession()`. Provides login/register/logout actions with error mapping. `DEV_BYPASS` flag creates a fake admin session for local dev.
- **birds.js** — Loads bird catalog + user sightings. Uses `AbortController` to cancel in-flight requests on re-fetch. Optimistic updates with rollback on failure for toggling public sightings.

### Composables
- **useAnimation.js** — GSAP + ScrollTrigger with automatic cleanup on unmount.
- **useImageResize.js** — Client-side image resize before upload (max 1280px, JPEG at 0.85 quality).

### Detection Flow (`views/DetectPage.vue`)
State machine: `idle -> selected -> detecting -> results -> saving -> saved`. Uses `AbortController` to cancel in-flight detection requests on unmount.

## Build & Deploy

```bash
npm run build    # Output in dist/
```

Deploy to S3 + CloudFront:
```bash
aws s3 sync dist/ s3://<frontend-bucket-name> --delete
aws cloudfront create-invalidation --distribution-id <dist-id> --paths "/*"
```
