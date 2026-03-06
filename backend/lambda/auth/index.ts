import {
  SecretsManagerClient,
  GetSecretValueCommand,
} from "@aws-sdk/client-secrets-manager";
import { Resend } from "resend";
import { betterAuth } from "better-auth";
import { Hono } from "hono";
import { cors } from "hono/cors";
import { handle } from "hono/aws-lambda";
import { Pool } from "pg";

const sm = new SecretsManagerClient({});

interface DbSecret {
  username: string;
  password: string;
}

interface AppSecret {
  BETTER_AUTH_SECRET: string;
  GOOGLE_CLIENT_ID: string;
  GOOGLE_CLIENT_SECRET: string;
  RESEND_API_KEY: string;
  FROM_EMAIL: string;
}

// Module-scope singletons — initialized once per cold start
let auth: ReturnType<typeof betterAuth> | null = null;

async function getSecretJson<T>(arn: string): Promise<T> {
  const res = await sm.send(new GetSecretValueCommand({ SecretId: arn }));
  return JSON.parse(res.SecretString!) as T;
}

async function initAuth() {
  if (auth) return auth;

  const [dbCreds, appSecret] = await Promise.all([
    getSecretJson<DbSecret>(process.env.DB_SECRET_ARN!),
    getSecretJson<AppSecret>(process.env.APP_SECRET_ARN!),
  ]);

  const pool = new Pool({
    host: process.env.DB_HOST!,
    port: 5432,
    database: process.env.DB_NAME!,
    user: dbCreds.username,
    password: dbCreds.password,
    ssl: { rejectUnauthorized: true },
    max: 5,
  });

  const instance = betterAuth({
    secret: appSecret.BETTER_AUTH_SECRET,
    baseURL: process.env.APP_BASE_URL,
    trustedOrigins: (process.env.FRONTEND_ORIGIN ?? "http://localhost:5173").split(","),
    database: pool,
    advanced: {
      defaultCookieAttributes: {
        sameSite: "none",
        secure: true,
      },
    },
    emailAndPassword: {
      enabled: true,
      requireEmailVerification: true,
    },
    socialProviders: {
      google: {
        clientId: appSecret.GOOGLE_CLIENT_ID,
        clientSecret: appSecret.GOOGLE_CLIENT_SECRET,
      },
    },
    emailVerification: {
      callbackURL: (process.env.FRONTEND_ORIGIN ?? "http://localhost:5173").split(",")[0] + "/login",
      sendVerificationEmail: async ({ user, url }) => {
        const resend = new Resend(appSecret.RESEND_API_KEY);
        await resend.emails.send({
          from: appSecret.FROM_EMAIL,
          to: user.email,
          subject: "Verify your BirdDex email",
          html: `<p>Click <a href="${url}">here</a> to verify your email address.</p>`,
          text: `Verify your email: ${url}`,
        });
      },
    },
  });

  auth = instance;
  return instance;
}

const app = new Hono();

const allowedOrigins = (process.env.FRONTEND_ORIGIN ?? "http://localhost:5173").split(",");

app.use(
  "/api/auth/*",
  cors({
    origin: allowedOrigins,
    allowMethods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allowHeaders: ["Content-Type", "Authorization", "Cookie", "X-Requested-With"],
    credentials: true,
    maxAge: 3600,
  }),
);

app.on(["GET", "POST"], "/api/auth/*", async (c) => {
  const a = await initAuth();
  return a.handler(c.req.raw);
});

export const handler = handle(app);
