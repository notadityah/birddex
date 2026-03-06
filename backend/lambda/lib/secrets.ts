import {
  SecretsManagerClient,
  GetSecretValueCommand,
} from "@aws-sdk/client-secrets-manager";

const sm = new SecretsManagerClient({});

const cache = new Map<string, unknown>();

export async function getSecretJson<T>(arn: string): Promise<T> {
  const cached = cache.get(arn);
  if (cached) return cached as T;

  const res = await sm.send(new GetSecretValueCommand({ SecretId: arn }));
  if (!res.SecretString) {
    throw new Error(`Secret ${arn} has no SecretString`);
  }
  const parsed = JSON.parse(res.SecretString) as T;
  cache.set(arn, parsed);
  return parsed;
}
