# BirdDex Deployment Runbook

Concrete, copy-paste steps to deploy the whole app. All values below are the real
production values for this project.

## Environment / IDs

| Thing | Value |
|-------|-------|
| AWS account | `159987617860` |
| Region | `ap-southeast-2` |
| CloudFormation stack | `BackendStack` |
| Frontend S3 bucket | `backendstack-frontendbucketefe2e19c-vdgdgv7nhjjb` |
| Images S3 bucket | `backendstack-birddexbucketf7c906bd-0uedspheratt` |
| CloudFront distribution ID | `E2EZ5RFQJAH3C0` |
| CloudFront URL | https://dmelw6vnulrpb.cloudfront.net (custom domain: https://birddex.fun) |
| API Gateway | https://tjcctm87rf.execute-api.ap-southeast-2.amazonaws.com |
| Neon project / db | `long-leaf-02679026` / `neondb` |

> Bucket names and IDs come from the stack outputs. If the stack is ever recreated,
> refresh them with:
> ```bash
> aws cloudformation describe-stacks --stack-name BackendStack --region ap-southeast-2 \
>   --query "Stacks[0].Outputs" --output table
> ```

---

## 1. Database schema (only when `schema.sql` changed)

The schema is applied manually (there is no migration lambda). It is idempotent, so
re-running is safe. Apply via the Neon MCP `run_sql` or psql. Example (the "wrong match"
feature added this column):

```sql
ALTER TABLE feedback ADD COLUMN IF NOT EXISTS image_key TEXT;
```

Apply the relevant statements from `backend/lambda/migrate/schema.sql`.

---

## 2. Backend deploy (lambdas / API / infra)

**Preconditions:** Docker Desktop must be running (the DetectLambda is an x86_64
container image that gets built and pushed to ECR on every deploy).

```bash
cd backend
CDK_DEFAULT_ACCOUNT=159987617860 CDK_DEFAULT_REGION=ap-southeast-2 \
  npx cdk deploy --require-approval never
```

Optional sanity checks before deploying:
```bash
cd backend && npm run build && npm test   # tsc + 20 CDK assertion tests
CDK_DEFAULT_ACCOUNT=159987617860 CDK_DEFAULT_REGION=ap-southeast-2 npx cdk diff
```

### Known failure: ECR `broken pipe` on the Detect image push
The Docker image push to ECR sometimes dies mid-upload with
`write: broken pipe` / `Failed to publish asset DetectLambda/AssetImage`. This is a
transient network error, and it aborts the **entire** deploy (so the API lambda change
won't land either). **Just re-run the same `cdk deploy` command** — the image is already
built locally, so the retry resumes the push. Repeat until it completes.

> Note: `cdk deploy` rebuilds the Detect container image even when only `api/index.ts`
> changed, because the image hash is not perfectly reproducible. The rebuilt image is
> functionally identical (the ONNX model lives in S3, not in the image).

---

## 3. Frontend deploy (static site → S3 → CloudFront)

```bash
cd frontend
npm run build
aws s3 sync dist/ s3://backendstack-frontendbucketefe2e19c-vdgdgv7nhjjb --delete
aws cloudfront create-invalidation --distribution-id E2EZ5RFQJAH3C0 --paths "/*"
```

The invalidation takes a minute or two to propagate. After it completes, hard-refresh
https://birddex.fun (or the CloudFront URL) to see the new build.

---

## 4. Order of operations

For a change that touches DB + backend + frontend (like the "wrong match" report feature):

1. Apply schema changes (step 1).
2. Deploy backend (step 2) — retry on `broken pipe`.
3. Deploy frontend (step 3).

Frontend degrades gracefully if deployed before the backend (e.g. an image just won't
upload), but deploying backend first avoids any window of mismatch.

---

## 5. Local testing (no local backend)

There is no local backend — the Vue dev server talks to the **deployed** API.

```bash
cd frontend && npm run dev   # http://localhost:5173
```

- `.env` sets `VITE_API_URL=https://birddex.fun`, so the dev app calls the production API
  directly. To point dev at the raw API Gateway instead, blank `VITE_API_URL` (requests
  become relative `/api/*` and are proxied to `VITE_API_PROXY_TARGET`).
- Auth is real and server-side: you must be logged in for `POST /api/feedback` (401
  otherwise), and your user needs `role='admin'` to load the admin Feedback tab (403
  otherwise). Grant admin with:
  ```sql
  UPDATE "user" SET role = 'admin' WHERE email = '<your-email>';
  ```
