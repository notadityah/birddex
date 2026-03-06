import {
  SecretsManagerClient,
  GetSecretValueCommand,
} from '@aws-sdk/client-secrets-manager';
import postgres from 'postgres';
import { readFileSync } from 'fs';
import { join } from 'path';

const sm = new SecretsManagerClient({});

interface DbSecret {
  username: string;
  password: string;
}

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

  const secretArn = process.env.DB_SECRET_ARN!;
  const dbHost = process.env.DB_HOST!;
  const dbName = process.env.DB_NAME!;

  const secretValue = await sm.send(
    new GetSecretValueCommand({ SecretId: secretArn }),
  );
  const creds: DbSecret = JSON.parse(secretValue.SecretString!);

  const sql = postgres({
    host: dbHost,
    port: 5432,
    database: dbName,
    username: creds.username,
    password: creds.password,
    ssl: 'require',
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
