import {
  S3Client,
  PutObjectCommand,
  DeleteObjectCommand,
} from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";
import { betterAuth } from "better-auth";
import { Hono } from "hono";
import { handle } from "hono/aws-lambda";
import { randomUUID } from "crypto";
import { getDb, getPgPool } from "../lib/db.js";
import { getSecretJson } from "../lib/secrets.js";

const s3 = new S3Client({});

const ALLOWED_EXTS = new Set(["jpg", "jpeg", "png", "webp"]);

interface AppSecret {
  BETTER_AUTH_SECRET: string;
}

let auth: ReturnType<typeof betterAuth> | null = null;

async function getAuth(): Promise<ReturnType<typeof betterAuth>> {
  if (auth) return auth;
  const [pool, appSecret] = await Promise.all([
    getPgPool(3),
    getSecretJson<AppSecret>(process.env.APP_SECRET_ARN!),
  ]);
  auth = betterAuth({ secret: appSecret.BETTER_AUTH_SECRET, database: pool });
  return auth;
}

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

  let body: { birdId?: number; imageExt?: string; detectedAt?: string; notes?: string };
  try {
    body = await c.req.json();
  } catch {
    return c.json({ error: "Invalid JSON body" }, 400);
  }

  if (!body.birdId || typeof body.birdId !== "number" || body.birdId < 1) {
    return c.json({ error: "birdId must be a positive number" }, 400);
  }

  let imageKey: string | null = null;
  let uploadUrl: string | undefined;
  if (body.imageExt !== undefined) {
    const rawExt = body.imageExt.trim().toLowerCase();
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

  const detectedAt = body.detectedAt ? new Date(body.detectedAt) : new Date();
  if (isNaN(detectedAt.getTime())) {
    return c.json({ error: "Invalid detectedAt date" }, 400);
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
      ${detectedAt},
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

  const imageKey = result[0].image_key as string | null;
  if (imageKey) {
    try {
      await s3.send(
        new DeleteObjectCommand({
          Bucket: process.env.BUCKET_NAME!,
          Key: imageKey,
        }),
      );
    } catch (err) {
      console.error("Failed to delete S3 object:", imageKey, err);
    }
  }

  return c.json({ ok: true });
});

app.get("/api/upload-url", async (c) => {
  const session = await getSession(c.req.raw);
  if (!session) return c.json({ error: "Unauthorized" }, 401);

  const rawExt = (c.req.query("ext") ?? "jpg").trim().toLowerCase();
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
