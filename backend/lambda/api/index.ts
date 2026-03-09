import {
  S3Client,
  PutObjectCommand,
  DeleteObjectCommand,
  GetObjectCommand,
} from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";
import { betterAuth } from "better-auth";
import { admin } from "better-auth/plugins/admin";
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

// eslint-disable-next-line @typescript-eslint/no-explicit-any
let auth: any = null;

async function getAuth() {
  if (auth) return auth as ReturnType<typeof betterAuth>;
  const [pool, appSecret] = await Promise.all([
    getPgPool(3),
    getSecretJson<AppSecret>(process.env.APP_SECRET_ARN!),
  ]);
  auth = betterAuth({
    secret: appSecret.BETTER_AUTH_SECRET,
    baseURL: process.env.APP_BASE_URL,
    trustedOrigins: (process.env.FRONTEND_ORIGIN ?? "")
      .split(",")
      .filter(Boolean),
    database: pool,
    plugins: [admin()],
    advanced: {
      defaultCookieAttributes: {
        sameSite: "none",
        secure: true,
      },
    },
  });
  return auth as ReturnType<typeof betterAuth>;
}

async function getSession(req: Request) {
  const a = await getAuth();
  return a.api.getSession({ headers: req.headers });
}

async function requireAdmin(req: Request) {
  const session = await getSession(req);
  if (!session || (session.user as Record<string, unknown>).role !== "admin")
    return null;
  return session;
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

  const enriched = await Promise.all(
    rows.map(async (row) => {
      let image_url: string | null = null;
      if (row.image_key) {
        image_url = await getSignedUrl(
          s3,
          new GetObjectCommand({
            Bucket: process.env.BUCKET_NAME!,
            Key: row.image_key as string,
          }),
          { expiresIn: 3600 },
        );
      }
      return { ...row, image_url };
    }),
  );

  return c.json(enriched);
});

app.post("/api/sightings", async (c) => {
  const session = await getSession(c.req.raw);
  if (!session) return c.json({ error: "Unauthorized" }, 401);

  let body: {
    birdId?: number;
    imageExt?: string;
    detectedAt?: string;
    notes?: string;
  };
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
      return c.json(
        { error: "Invalid image extension. Allowed: jpg, jpeg, png, webp" },
        400,
      );
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
    return c.json(
      { error: "Invalid image extension. Allowed: jpg, jpeg, png, webp" },
      400,
    );
  }
  const key = `images/${session.user.id}/${randomUUID()}.${rawExt}`;
  const url = await getSignedUrl(
    s3,
    new PutObjectCommand({ Bucket: process.env.BUCKET_NAME!, Key: key }),
    { expiresIn: 300 },
  );

  return c.json({ key, url });
});

// =========================================================
// === ADMIN ENDPOINTS ===
// =========================================================

// GET /api/admin/stats
app.get("/api/admin/stats", async (c) => {
  const session = await requireAdmin(c.req.raw);
  if (!session) return c.json({ error: "Forbidden" }, 403);

  const db = await getDb();
  const [users, birds, sightings] = await Promise.all([
    db`SELECT COUNT(*)::int AS count FROM "user"`,
    db`SELECT COUNT(*)::int AS count FROM bird`,
    db`SELECT COUNT(*)::int AS count FROM sighting`,
  ]);

  return c.json({
    users: users[0].count,
    birds: birds[0].count,
    sightings: sightings[0].count,
  });
});

// GET /api/admin/birds
app.get("/api/admin/birds", async (c) => {
  const session = await requireAdmin(c.req.raw);
  if (!session) return c.json({ error: "Forbidden" }, 403);

  const db = await getDb();
  const q = c.req.query("q")?.trim() ?? "";
  const limit = Math.min(parseInt(c.req.query("limit") ?? "50", 10), 200);
  const offset = parseInt(c.req.query("offset") ?? "0", 10);

  let rows;
  if (q) {
    const pattern = `%${q}%`;
    rows = await db`
      SELECT id, name, scientific_name, slug FROM bird
      WHERE name ILIKE ${pattern} OR scientific_name ILIKE ${pattern} OR slug ILIKE ${pattern}
      ORDER BY name
      LIMIT ${limit} OFFSET ${offset}
    `;
  } else {
    rows = await db`
      SELECT id, name, scientific_name, slug FROM bird
      ORDER BY name
      LIMIT ${limit} OFFSET ${offset}
    `;
  }

  return c.json(rows);
});

// POST /api/admin/birds
app.post("/api/admin/birds", async (c) => {
  const session = await requireAdmin(c.req.raw);
  if (!session) return c.json({ error: "Forbidden" }, 403);

  let body: { name?: string; scientificName?: string; slug?: string };
  try {
    body = await c.req.json();
  } catch {
    return c.json({ error: "Invalid JSON body" }, 400);
  }

  const name = body.name?.trim();
  const scientificName = body.scientificName?.trim();
  if (!name || !scientificName) {
    return c.json({ error: "name and scientificName are required" }, 400);
  }

  const slug =
    body.slug?.trim() ||
    name
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "_")
      .replace(/^_|_$/g, "");

  const db = await getDb();
  try {
    const result = await db`
      INSERT INTO bird (name, scientific_name, slug)
      VALUES (${name}, ${scientificName}, ${slug})
      RETURNING id, name, scientific_name, slug
    `;
    console.log(
      `[ADMIN] Bird created by ${session.user.id}: id=${result[0].id}, name=${name}`,
    );
    return c.json(result[0], 201);
  } catch (err: unknown) {
    if (
      err instanceof Error &&
      err.message.includes("duplicate key value violates unique constraint")
    ) {
      return c.json({ error: "A bird with this slug already exists" }, 409);
    }
    throw err;
  }
});

// PUT /api/admin/birds/:id
app.put("/api/admin/birds/:id", async (c) => {
  const session = await requireAdmin(c.req.raw);
  if (!session) return c.json({ error: "Forbidden" }, 403);

  const birdId = parseInt(c.req.param("id"), 10);
  if (isNaN(birdId)) return c.json({ error: "Invalid bird ID" }, 400);

  let body: { name?: string; scientificName?: string; slug?: string };
  try {
    body = await c.req.json();
  } catch {
    return c.json({ error: "Invalid JSON body" }, 400);
  }

  const name = body.name?.trim();
  const scientificName = body.scientificName?.trim();
  const slug = body.slug?.trim();

  if (!name && !scientificName && !slug) {
    return c.json({ error: "At least one field is required" }, 400);
  }

  const db = await getDb();
  const existing = await db`SELECT id FROM bird WHERE id = ${birdId}`;
  if (existing.length === 0) return c.json({ error: "Bird not found" }, 404);

  try {
    const result = await db`
      UPDATE bird SET
        name = COALESCE(${name ?? null}, name),
        scientific_name = COALESCE(${scientificName ?? null}, scientific_name),
        slug = COALESCE(${slug ?? null}, slug)
      WHERE id = ${birdId}
      RETURNING id, name, scientific_name, slug
    `;
    console.log(
      `[ADMIN] Bird updated by ${session.user.id}: id=${birdId}`,
    );
    return c.json(result[0]);
  } catch (err: unknown) {
    if (
      err instanceof Error &&
      err.message.includes("duplicate key value violates unique constraint")
    ) {
      return c.json({ error: "A bird with this slug already exists" }, 409);
    }
    throw err;
  }
});

// DELETE /api/admin/birds/:id
app.delete("/api/admin/birds/:id", async (c) => {
  const session = await requireAdmin(c.req.raw);
  if (!session) return c.json({ error: "Forbidden" }, 403);

  const birdId = parseInt(c.req.param("id"), 10);
  if (isNaN(birdId)) return c.json({ error: "Invalid bird ID" }, 400);

  const db = await getDb();

  // Check for sightings referencing this bird
  const sightings =
    await db`SELECT COUNT(*)::int AS count FROM sighting WHERE bird_id = ${birdId}`;
  if (sightings[0].count > 0) {
    return c.json(
      {
        error: `Cannot delete: ${sightings[0].count} sighting(s) reference this bird`,
      },
      409,
    );
  }

  const result =
    await db`DELETE FROM bird WHERE id = ${birdId} RETURNING id`;
  if (result.length === 0) return c.json({ error: "Bird not found" }, 404);

  console.log(
    `[ADMIN] Bird deleted by ${session.user.id}: id=${birdId}`,
  );
  return c.json({ ok: true });
});

// GET /api/admin/sightings
app.get("/api/admin/sightings", async (c) => {
  const session = await requireAdmin(c.req.raw);
  if (!session) return c.json({ error: "Forbidden" }, 403);

  const db = await getDb();
  const userId = c.req.query("userId")?.trim();
  const birdId = c.req.query("birdId")?.trim();
  const limit = Math.min(parseInt(c.req.query("limit") ?? "50", 10), 200);
  const offset = parseInt(c.req.query("offset") ?? "0", 10);

  if (userId && birdId) {
    const rows = await db`
      SELECT s.id, s.user_id, u.name AS user_name, u.email AS user_email,
             s.bird_id, b.name AS bird_name, s.image_key, s.detected_at, s.notes, s.created_at
      FROM sighting s
      JOIN "user" u ON u.id = s.user_id
      JOIN bird b ON b.id = s.bird_id
      WHERE s.user_id = ${userId} AND s.bird_id = ${parseInt(birdId, 10)}
      ORDER BY s.created_at DESC
      LIMIT ${limit} OFFSET ${offset}
    `;
    return c.json(rows);
  } else if (userId) {
    const rows = await db`
      SELECT s.id, s.user_id, u.name AS user_name, u.email AS user_email,
             s.bird_id, b.name AS bird_name, s.image_key, s.detected_at, s.notes, s.created_at
      FROM sighting s
      JOIN "user" u ON u.id = s.user_id
      JOIN bird b ON b.id = s.bird_id
      WHERE s.user_id = ${userId}
      ORDER BY s.created_at DESC
      LIMIT ${limit} OFFSET ${offset}
    `;
    return c.json(rows);
  } else if (birdId) {
    const rows = await db`
      SELECT s.id, s.user_id, u.name AS user_name, u.email AS user_email,
             s.bird_id, b.name AS bird_name, s.image_key, s.detected_at, s.notes, s.created_at
      FROM sighting s
      JOIN "user" u ON u.id = s.user_id
      JOIN bird b ON b.id = s.bird_id
      WHERE s.bird_id = ${parseInt(birdId, 10)}
      ORDER BY s.created_at DESC
      LIMIT ${limit} OFFSET ${offset}
    `;
    return c.json(rows);
  } else {
    const rows = await db`
      SELECT s.id, s.user_id, u.name AS user_name, u.email AS user_email,
             s.bird_id, b.name AS bird_name, s.image_key, s.detected_at, s.notes, s.created_at
      FROM sighting s
      JOIN "user" u ON u.id = s.user_id
      JOIN bird b ON b.id = s.bird_id
      ORDER BY s.created_at DESC
      LIMIT ${limit} OFFSET ${offset}
    `;
    return c.json(rows);
  }
});

// DELETE /api/admin/sightings/:id
app.delete("/api/admin/sightings/:id", async (c) => {
  const session = await requireAdmin(c.req.raw);
  if (!session) return c.json({ error: "Forbidden" }, 403);

  const db = await getDb();
  const result = await db`
    DELETE FROM sighting WHERE id = ${c.req.param("id")} RETURNING image_key
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

  console.log(
    `[ADMIN] Sighting deleted by ${session.user.id}: id=${c.req.param("id")}`,
  );
  return c.json({ ok: true });
});

export const handler = handle(app);
