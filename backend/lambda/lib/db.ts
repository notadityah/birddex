/**
 * Two DB clients are needed because different consumers have different requirements:
 * - postgres.js (getDb): Used by API/detect/migrate lambdas for direct SQL queries.
 *   Lightweight, tagged template syntax, great DX.
 * - pg Pool (getPgPool): Used by better-auth's Kysely adapter, which requires node-pg.
 *
 * Both are cached as module-level singletons so Lambda warm starts reuse the same
 * connection pool instead of opening new connections on every invocation.
 */
import postgres from "postgres";
import { Pool } from "pg";
import { getSecretJson } from "./secrets.js";

interface AppSecret {
  DATABASE_URL: string;
}

let sql: ReturnType<typeof postgres> | null = null;
let pgPool: Pool | null = null;

async function getDatabaseUrl(): Promise<string> {
  const { DATABASE_URL } = await getSecretJson<AppSecret>(process.env.APP_SECRET_ARN!);
  return DATABASE_URL;
}

// Default max 3 connections — Lambda has limited concurrent DB connections.
// Detect Lambda passes max=2 since it only makes a single query per invocation.
export async function getDb(maxConnections = 3): Promise<ReturnType<typeof postgres>> {
  if (sql) return sql;

  const url = await getDatabaseUrl();
  sql = postgres(url, { max: maxConnections });

  return sql;
}

// Default max 5 for better-auth (more concurrent queries during auth flows).
export async function getPgPool(maxConnections = 5): Promise<Pool> {
  if (pgPool) return pgPool;

  const url = await getDatabaseUrl();
  pgPool = new Pool({
    connectionString: url,
    max: maxConnections,
    ssl: { rejectUnauthorized: true },
  });

  return pgPool;
}
