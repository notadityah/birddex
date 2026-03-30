# BirdDex Backend

Serverless AWS infrastructure for BirdDex, defined with AWS CDK (TypeScript). Deploys a fully managed stack: API Gateway v2, Lambda functions, RDS PostgreSQL, S3, CloudFront, and monitoring.

## Architecture

```
CloudFront
  |-- /           --> S3 (frontend static files)
  |-- /api/*      --> HTTP API Gateway v2
                        |-- /api/auth/{proxy+}  --> Auth Lambda (better-auth + Resend)
                        |-- /api/detect         --> Detect Lambda (ONNX inference)
                        |-- /api/{proxy+}       --> API Lambda (CRUD operations)
                                                        |
                                                        v
                                                   RDS PostgreSQL
```

## Directory Structure

```
lib/
  backend-stack.ts      # CDK stack definition (all infrastructure)
lambda/
  auth/index.ts         # Auth Lambda — better-auth with Google OAuth + email via Resend
  api/index.ts          # API Lambda — Hono REST (birds, sightings, gallery, admin, feedback)
  detect/index.ts       # Detect Lambda — ONNX bird classification model
  detect/Dockerfile     # Docker image for detect (onnxruntime-node exceeds zip limit)
  lib/db.ts             # Shared DB connection utilities
  lib/secrets.ts        # Cached Secrets Manager access
  migrate/index.ts      # Migration Lambda (Custom Resource, runs schema.sql)
  migrate/schema.sql    # Database schema + seed data
  tsconfig.json         # Lambda-specific TS config (ESNext/bundler for esbuild)
test/
  backend.test.ts       # CDK assertion tests (22 tests)
```

## Prerequisites

- **Node.js** 22+
- **AWS CLI** configured with credentials
- **AWS CDK** (`npm install -g aws-cdk`)
- **Docker** (required for detect Lambda — builds onnxruntime-node container image)

## Environment Setup

### 1. CDK environment variables

```bash
export CDK_DEFAULT_ACCOUNT=123456789012   # Your AWS account ID
export CDK_DEFAULT_REGION=ap-southeast-2  # Your target region
```

Or create a `backend/.env` file (see `.env.example`).

### 2. CDK context (optional)

In `cdk.context.json`, set:
- `env` — `"prod"` or `"dev"` (defaults to `"prod"`)
- `certArn` — ACM certificate ARN for custom domain (us-east-1, for CloudFront)
- `webAclArn` — WAF Web ACL ARN for CloudFront protection

### 3. AWS Secrets Manager

Create these secrets before first deploy:

**`/birddex/app/secrets`** (JSON):
```json
{
  "BETTER_AUTH_SECRET": "<random-secret>",
  "GOOGLE_CLIENT_ID": "<google-oauth-client-id>",
  "GOOGLE_CLIENT_SECRET": "<google-oauth-client-secret>",
  "RESEND_API_KEY": "<resend-api-key>",
  "FROM_EMAIL": "noreply@birddex.fun",
  "DATABASE_URL": "postgresql://user:pass@host:5432/birddex?sslmode=require"
}
```

> `DATABASE_URL` is populated after the first deploy creates the RDS instance. The DB credentials are auto-generated and stored at `/birddex/db/credentials`.

## Commands

```bash
npm run build     # TypeScript compile check
npm test          # Run CDK assertion tests (22 tests)
npx cdk synth     # Synthesize CloudFormation template
npx cdk diff      # Compare deployed stack with current state
npx cdk deploy    # Deploy stack to AWS
```

## Lambda Details

| Lambda | Runtime | Architecture | Memory | Timeout | Notes |
|--------|---------|-------------|--------|---------|-------|
| Auth | Node 22 | ARM64 | 512 MB | 15s | Higher memory for better-auth + Resend email sending |
| API | Node 22 | ARM64 | 256 MB | 10s | Standard CRUD, lightweight |
| Detect | Node 22 | **x86_64** | 1536 MB | 30s | onnxruntime-node only has x86 binaries; right-sized down from 3008 MB |

The detect Lambda uses a Docker container image because `onnxruntime-node` plus the ONNX model exceeds the Lambda zip size limit (250 MB). It runs on x86_64 because `onnxruntime-node` does not publish ARM64 binaries.

## Monitoring

All Lambda functions have CloudWatch error alarms (threshold: 1 error in 5 minutes) that publish to the `birddex-alarms` SNS topic. Subscribe your email after deploy:

```bash
aws sns subscribe --topic-arn <alarm-topic-arn> --protocol email --notification-endpoint you@example.com
```
