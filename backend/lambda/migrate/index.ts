import postgres from 'postgres';
import { readFileSync } from 'fs';
import { join } from 'path';
import { getSecretJson } from '../lib/secrets.js';
import type { DbSecret } from '../lib/db.js';

const RDS_CA = readFileSync(join(__dirname, 'rds-ca-bundle.pem'), 'utf-8');

interface CloudFormationEvent {
  RequestType: 'Create' | 'Update' | 'Delete';
  ResourceProperties: {
    schemaVersion?: string;
  };
}

interface CloudFormationResponse {
  PhysicalResourceId: string;
  Data?: Record<string, string>;
}

export async function handler(
  event: CloudFormationEvent,
): Promise<CloudFormationResponse> {
  if (event.RequestType === 'Delete') {
    return { PhysicalResourceId: 'migrate' };
  }

  const creds = await getSecretJson<DbSecret>(process.env.DB_SECRET_ARN!);

  const sql = postgres({
    host: process.env.DB_HOST!,
    port: 5432,
    database: process.env.DB_NAME!,
    username: creds.username,
    password: creds.password,
    ssl: { rejectUnauthorized: true, ca: RDS_CA },
    max: 1,
  });

  try {
    const schema = readFileSync(join(__dirname, 'schema.sql'), 'utf-8');
    await sql.unsafe(schema);
    console.log('Migration completed successfully');
  } finally {
    await sql.end();
  }

  return { PhysicalResourceId: 'migrate' };
}
