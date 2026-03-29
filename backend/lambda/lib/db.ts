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

export async function getDb(maxConnections = 3): Promise<ReturnType<typeof postgres>> {
  if (sql) return sql;

  const url = await getDatabaseUrl();
  sql = postgres(url, { max: maxConnections });

  return sql;
}

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
