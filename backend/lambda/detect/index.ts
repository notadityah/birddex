import { S3Client, GetObjectCommand } from "@aws-sdk/client-s3";
import { InferenceSession, Tensor } from "onnxruntime-node";
import sharp from "sharp";
import postgres from "postgres";
import { createWriteStream, existsSync, readFileSync } from "fs";
import { pipeline } from "stream/promises";
import { Readable } from "stream";
import { join } from "path";
import { getSecretJson } from "../lib/secrets.js";
import type { DbSecret } from "../lib/db.js";
import type {
  APIGatewayProxyEventV2,
  APIGatewayProxyResultV2,
} from "aws-lambda";

const RDS_CA = readFileSync(join(__dirname, "rds-ca-bundle.pem"), "utf-8");

const s3 = new S3Client({});

const MODEL_TMP = "/tmp/model.onnx";
const CLASSES_TMP = "/tmp/classes.txt";
const MODEL_S3_KEY = "models/model.onnx";
const CLASSES_S3_KEY = "models/classes.txt";

const MAX_TOP_N = 36;

// Module-scope singletons
let session: InferenceSession | null = null;
let classes: string[] | null = null;
let sql: ReturnType<typeof postgres> | null = null;

async function downloadFromS3(key: string, dest: string): Promise<void> {
  const res = await s3.send(
    new GetObjectCommand({ Bucket: process.env.BUCKET_NAME!, Key: key }),
  );
  const body = res.Body as Readable;
  await pipeline(body, createWriteStream(dest));
}

async function getInferenceSession(): Promise<{
  session: InferenceSession;
  classes: string[];
}> {
  if (session && classes) return { session, classes };

  if (!existsSync(MODEL_TMP)) {
    console.log("Downloading model from S3...");
    await downloadFromS3(MODEL_S3_KEY, MODEL_TMP);
  }
  if (!existsSync(CLASSES_TMP)) {
    await downloadFromS3(CLASSES_S3_KEY, CLASSES_TMP);
  }

  session = await InferenceSession.create(MODEL_TMP);
  classes = readFileSync(CLASSES_TMP, "utf-8")
    .split("\n")
    .map((l) => l.trim())
    .filter(Boolean);

  return { session, classes };
}

async function getDb(): Promise<ReturnType<typeof postgres>> {
  if (sql) return sql;

  const creds = await getSecretJson<DbSecret>(process.env.DB_SECRET_ARN!);

  sql = postgres({
    host: process.env.DB_HOST!,
    port: 5432,
    database: process.env.DB_NAME!,
    username: creds.username,
    password: creds.password,
    ssl: { rejectUnauthorized: true, ca: RDS_CA },
    max: 2,
  });

  return sql;
}

async function requireSession(
  event: APIGatewayProxyEventV2,
): Promise<boolean> {
  const cookie = event.headers["cookie"] ?? "";
  const authHeader = event.headers["authorization"] ?? "";
  const tokenMatch =
    cookie.match(/better-auth\.session_token=([^;]+)/) ??
    authHeader.match(/^Bearer (.+)$/);
  if (!tokenMatch) return false;
  const db = await getDb();
  const rows = await db`
    SELECT id FROM session WHERE token = ${tokenMatch[1]} AND "expiresAt" > NOW() LIMIT 1
  `;
  return rows.length > 0;
}

// Letterbox-resize to 640x640, convert HWC uint8 -> CHW float32 [0,1]
async function preprocessImage(imageData: Buffer): Promise<Float32Array> {
  const size = 640;
  const { data } = await sharp(imageData)
    .resize(size, size, { fit: "contain", background: { r: 114, g: 114, b: 114 } })
    .removeAlpha()
    .raw()
    .toBuffer({ resolveWithObject: true });

  const tensor = new Float32Array(3 * size * size);
  const stride = size * size;
  for (let i = 0; i < stride; i++) {
    tensor[i]              = data[i * 3]     / 255;
    tensor[stride + i]     = data[i * 3 + 1] / 255;
    tensor[stride * 2 + i] = data[i * 3 + 2] / 255;
  }
  return tensor;
}

// Parse YOLO output [1, 40, 8400]: 4 bbox coords + numClasses scores per anchor.
function parseDetections(
  raw: Float32Array,
  classLabels: string[],
  confThreshold: number,
  topN: number,
): { label: string; confidence: number }[] {
  const numDet = 8400;
  const numCls = classLabels.length;
  const best = new Map<string, number>();

  for (let d = 0; d < numDet; d++) {
    let maxScore = 0;
    let maxCls = 0;
    for (let c = 0; c < numCls; c++) {
      const score = raw[(4 + c) * numDet + d];
      if (score > maxScore) { maxScore = score; maxCls = c; }
    }
    if (maxScore < confThreshold) continue;
    const label = classLabels[maxCls] ?? `class_${maxCls}`;
    if ((best.get(label) ?? 0) < maxScore) best.set(label, maxScore);
  }

  return [...best.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, topN)
    .map(([label, confidence]) => ({
      label,
      confidence: Math.round(confidence * 10000) / 100,
    }));
}

export async function handler(
  event: APIGatewayProxyEventV2,
): Promise<APIGatewayProxyResultV2> {
  try {
    const authed = await requireSession(event);
    if (!authed) {
      return {
        statusCode: 401,
        body: JSON.stringify({ error: "Unauthorized" }),
      };
    }

    let body: { imageBase64?: string; imageKey?: string; topN?: number };
    try {
      body = event.body
        ? JSON.parse(
            event.isBase64Encoded
              ? Buffer.from(event.body, "base64").toString()
              : event.body,
          )
        : {};
    } catch {
      return {
        statusCode: 400,
        body: JSON.stringify({ error: "Invalid JSON body" }),
      };
    }

    const { imageBase64, imageKey } = body;
    const topN = Math.min(Math.max(body.topN ?? 3, 1), MAX_TOP_N);

    if (!imageBase64 && !imageKey) {
      return {
        statusCode: 400,
        body: JSON.stringify({ error: "imageBase64 or imageKey required" }),
      };
    }

    // Validate imageKey format to prevent path traversal
    if (imageKey && !/^images\/[a-zA-Z0-9_-]+\/[a-zA-Z0-9_-]+\.\w+$/.test(imageKey)) {
      return {
        statusCode: 400,
        body: JSON.stringify({ error: "Invalid imageKey format" }),
      };
    }

    let imageBuffer: Buffer;
    if (imageBase64) {
      imageBuffer = Buffer.from(imageBase64, "base64");
    } else {
      const res = await s3.send(
        new GetObjectCommand({
          Bucket: process.env.BUCKET_NAME!,
          Key: imageKey!,
        }),
      );
      const chunks: Uint8Array[] = [];
      for await (const chunk of res.Body as AsyncIterable<Uint8Array>) {
        chunks.push(chunk);
      }
      imageBuffer = Buffer.concat(chunks);
    }

    const { session: onnxSession, classes: classLabels } =
      await getInferenceSession();

    const inputData = await preprocessImage(imageBuffer);
    const inputTensor = new Tensor("float32", inputData, [1, 3, 640, 640]);
    const feeds: Record<string, Tensor> = { [onnxSession.inputNames[0]]: inputTensor };

    const results = await onnxSession.run(feeds);
    const raw = results[onnxSession.outputNames[0]].data as Float32Array;
    const topPredictions = parseDetections(raw, classLabels, 0.25, topN);

    const topSlug = topPredictions[0]?.label;
    type BirdRow = {
      id: number;
      name: string;
      scientific_name: string;
      slug: string;
    };
    let bird: BirdRow | null = null;
    if (topSlug) {
      const db = await getDb();
      const rows = await db<BirdRow[]>`
        SELECT id, name, scientific_name, slug FROM bird WHERE slug = ${topSlug} LIMIT 1
      `;
      bird = rows[0] ?? null;
    }

    return {
      statusCode: 200,
      body: JSON.stringify({ predictions: topPredictions, bird }),
    };
  } catch (err) {
    console.error("Detect error:", err);
    return {
      statusCode: 500,
      body: JSON.stringify({ error: "Internal server error" }),
    };
  }
}
