import postgres from "postgres";
import { Pool } from "pg";
import { readFileSync } from "fs";
import { join } from "path";
import { getSecretJson } from "./secrets.js";

const RDS_CA = readFileSync(join(__dirname, "rds-ca-bundle.pem"), "utf-8");

export interface DbSecret {
  username: string;
  password: string;
}

const sslConfig = { rejectUnauthorized: true, ca: RDS_CA };

let sql: ReturnType<typeof postgres> | null = null;
let pgPool: Pool | null = null;

async function getDbCreds(): Promise<DbSecret> {
  return getSecretJson<DbSecret>(process.env.DB_SECRET_ARN!);
}

export async function getDb(maxConnections = 3): Promise<ReturnType<typeof postgres>> {
  if (sql) return sql;

  const creds = await getDbCreds();
  sql = postgres({
    host: process.env.DB_HOST!,
    port: 5432,
    database: process.env.DB_NAME!,
    username: creds.username,
    password: creds.password,
    ssl: sslConfig,
    max: maxConnections,
  });

  return sql;
}

export async function getPgPool(maxConnections = 3): Promise<Pool> {
  if (pgPool) return pgPool;

  const creds = await getDbCreds();
  pgPool = new Pool({
    host: process.env.DB_HOST!,
    port: 5432,
    database: process.env.DB_NAME!,
    user: creds.username,
    password: creds.password,
    ssl: sslConfig,
    max: maxConnections,
  });

  return pgPool;
}

export { RDS_CA, sslConfig };
