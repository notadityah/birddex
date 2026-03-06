import {
  SecretsManagerClient,
  GetSecretValueCommand,
} from "@aws-sdk/client-secrets-manager";
import {
  S3Client,
  PutObjectCommand,
  DeleteObjectCommand,
} from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";
import { betterAuth } from "better-auth";
import { Hono } from "hono";
import { handle } from "hono/aws-lambda";
import { Pool } from "pg";
import postgres from "postgres";
import { randomUUID } from "crypto";
import { readFileSync } from "fs";
import { join } from "path";

const RDS_CA = readFileSync(join(__dirname, "rds-ca-bundle.pem"), "utf-8");

const sm = new SecretsManagerClient({});
const s3 = new S3Client({});

const ALLOWED_EXTS = new Set(["jpg", "jpeg", "png", "webp"]);

interface DbSecret {
  username: string;
  password: string;
}

interface AppSecret {
  BETTER_AUTH_SECRET: string;
}

let sql: ReturnType<typeof postgres> | null = null;
let pgPool: Pool | null = null;
let auth: ReturnType<typeof betterAuth> | null = null;
let betterAuthSecret: string | null = null;

async function getDb(): Promise<ReturnType<typeof postgres>> {
  if (sql) return sql;

  const res = await sm.send(
    new GetSecretValueCommand({ SecretId: process.env.DB_SECRET_ARN! }),
  );
  const creds: DbSecret = JSON.parse(res.SecretString!);

  sql = postgres({
    host: process.env.DB_HOST!,
    port: 5432,
    database: process.env.DB_NAME!,
    username: creds.username,
    password: creds.password,
    ssl: { rejectUnauthorized: true, ca: RDS_CA },
    max: 5,
  });

  return sql;
}

async function getAppSecret(): Promise<string> {
  if (betterAuthSecret) return betterAuthSecret;
  const res = await sm.send(
    new GetSecretValueCommand({ SecretId: process.env.APP_SECRET_ARN! }),
  );
  betterAuthSecret = (JSON.parse(res.SecretString!) as AppSecret)
    .BETTER_AUTH_SECRET;
  return betterAuthSecret;
}

async function getAuth(): Promise<ReturnType<typeof betterAuth>> {
  if (auth) return auth;
  const [creds, secret] = await Promise.all([
    sm.send(new GetSecretValueCommand({ SecretId: process.env.DB_SECRET_ARN! }))
      .then((r) => JSON.parse(r.SecretString!) as DbSecret),
    getAppSecret(),
  ]);
  pgPool ??= new Pool({
    host: process.env.DB_HOST!,
    port: 5432,
    database: process.env.DB_NAME!,
    user: creds.username,
    password: creds.password,
    ssl: { rejectUnauthorized: true, ca: RDS_CA },
    max: 3,
  });
  auth = betterAuth({ secret, database: pgPool });
  return auth;
}

// Middleware: verify session
async function getSession(req: Request) {
  const a = await getAuth();
  return a.api.getSession({ headers: req.headers });
}

const app = new Hono();

// GET /api/birds — public endpoint
app.get("/api/birds", async (c) => {
  const db = await getDb();
  const rows =
    await db`SELECT id, name, scientific_name, slug FROM bird ORDER BY name`;
  return c.json(rows);
});

// All sighting routes require authentication
app.get("/api/sightings", async (c) => {
  const session = await getSession(c.req.raw);
  if (!session) return c.json({ error: "Unauthorized" }, 401);

  const db = await getDb();
  const rows = await db`
    SELECT s.id, s.bird_id, b.slug, b.name AS bird_name, s.image_key, s.detected_at, s.notes, s.created_at
    FROM sighting s
    JOIN bird b ON b.id = s.bird_id
    WHERE s.user_id = ${session.user.id}
    ORDER BY s.created_at DESC
  `;
  return c.json(rows);
});

app.post("/api/sightings", async (c) => {
  const session = await getSession(c.req.raw);
  if (!session) return c.json({ error: "Unauthorized" }, 401);

  const body = await c.req.json<{
    birdId: number;
    imageExt?: string;
    detectedAt?: string;
    notes?: string;
  }>();

  // Server-generates the image key so client cannot supply arbitrary S3 paths
  let imageKey: string | null = null;
  let uploadUrl: string | undefined;
  if (body.imageExt !== undefined) {
    const rawExt = body.imageExt.toLowerCase();
    if (!ALLOWED_EXTS.has(rawExt)) {
      return c.json({ error: "Invalid image extension. Allowed: jpg, jpeg, png, webp" }, 400);
    }
    imageKey = `images/${session.user.id}/${randomUUID()}.${rawExt}`;
    uploadUrl = await getSignedUrl(
      s3,
      new PutObjectCommand({
        Bucket: process.env.BUCKET_NAME!,
        Key: imageKey,
      }),
      { expiresIn: 300 },
    );
  }

  const db = await getDb();
  const id = randomUUID();
  await db`
    INSERT INTO sighting (id, user_id, bird_id, image_key, detected_at, notes)
    VALUES (
      ${id},
      ${session.user.id},
      ${body.birdId},
      ${imageKey},
      ${body.detectedAt ? new Date(body.detectedAt) : new Date()},
      ${body.notes ?? null}
    )
  `;

  return c.json({ id, imageKey, uploadUrl }, 201);
});

app.delete("/api/sightings/:id", async (c) => {
  const session = await getSession(c.req.raw);
  if (!session) return c.json({ error: "Unauthorized" }, 401);

  const db = await getDb();
  const result = await db`
    DELETE FROM sighting
    WHERE id = ${c.req.param("id")} AND user_id = ${session.user.id}
    RETURNING image_key
  `;

  if (result.length === 0) return c.json({ error: "Not found" }, 404);

  // Clean up S3 image if present
  const imageKey = result[0].image_key as string | null;
  if (imageKey) {
    await s3.send(
      new DeleteObjectCommand({
        Bucket: process.env.BUCKET_NAME!,
        Key: imageKey,
      }),
    );
  }

  return c.json({ ok: true });
});

app.get("/api/upload-url", async (c) => {
  const session = await getSession(c.req.raw);
  if (!session) return c.json({ error: "Unauthorized" }, 401);

  const rawExt = (c.req.query("ext") ?? "jpg").toLowerCase();
  if (!ALLOWED_EXTS.has(rawExt)) {
    return c.json({ error: "Invalid image extension. Allowed: jpg, jpeg, png, webp" }, 400);
  }
  const key = `images/${session.user.id}/${randomUUID()}.${rawExt}`;
  const url = await getSignedUrl(
    s3,
    new PutObjectCommand({ Bucket: process.env.BUCKET_NAME!, Key: key }),
    { expiresIn: 300 },
  );

  return c.json({ key, url });
});

export const handler = handle(app);
