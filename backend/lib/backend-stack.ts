import * as cdk from "aws-cdk-lib/core";
import * as s3 from "aws-cdk-lib/aws-s3";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as rds from "aws-cdk-lib/aws-rds";
import * as iam from "aws-cdk-lib/aws-iam";
import * as secretsmanager from "aws-cdk-lib/aws-secretsmanager";
import * as lambda from "aws-cdk-lib/aws-lambda";
import {
  NodejsFunction,
  NodejsFunctionProps,
} from "aws-cdk-lib/aws-lambda-nodejs";
import * as apigwv2 from "aws-cdk-lib/aws-apigatewayv2";
import { HttpLambdaIntegration } from "aws-cdk-lib/aws-apigatewayv2-integrations";
import * as cr from "aws-cdk-lib/custom-resources";
import * as cloudfront from "aws-cdk-lib/aws-cloudfront";
import * as origins from "aws-cdk-lib/aws-cloudfront-origins";
import * as acm from "aws-cdk-lib/aws-certificatemanager";
import { Construct } from "constructs";
import * as path from "path";

const DB_NAME = "birddex";

export class BackendStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // =========================================================
    // === S3 ===
    // =========================================================

    const bucket = new s3.Bucket(this, "BirddexBucket", {
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      encryption: s3.BucketEncryption.S3_MANAGED,
      versioned: true,
      removalPolicy: cdk.RemovalPolicy.RETAIN,
      cors: [
        {
          allowedMethods: [s3.HttpMethods.GET, s3.HttpMethods.PUT],
          allowedOrigins: ["https://birddex.fun", "http://localhost:5173"],
          allowedHeaders: ["*"],
          maxAge: 3000,
        },
      ],
    });

    const frontendBucket = new s3.Bucket(this, "FrontendBucket", {
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      autoDeleteObjects: true,
    });

    // =========================================================
    // === VPC ===
    // =========================================================

    const vpc = new ec2.Vpc(this, "BirddexVpc", {
      maxAzs: 2,
      natGateways: 1,
      subnetConfiguration: [
        {
          cidrMask: 24,
          name: "public",
          subnetType: ec2.SubnetType.PUBLIC,
        },
        {
          cidrMask: 24,
          name: "private",
          subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
        },
        {
          cidrMask: 28,
          name: "isolated",
          subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
        },
      ],
    });

    // =========================================================
    // === SECURITY GROUPS ===
    // =========================================================

    const lambdaSg = new ec2.SecurityGroup(this, "LambdaSg", {
      vpc,
      description: "Security group for Lambda functions",
      allowAllOutbound: true,
    });

    const dbSg = new ec2.SecurityGroup(this, "DbSg", {
      vpc,
      description: "Security group for RDS instance",
      allowAllOutbound: false,
    });
    dbSg.addIngressRule(lambdaSg, ec2.Port.tcp(5432), "Allow Lambda to RDS");

    // =========================================================
    // === RDS POSTGRESQL ===
    // =========================================================

    const dbSecret = new rds.DatabaseSecret(this, "DbSecret", {
      username: "birddex_admin",
      secretName: "/birddex/db/credentials",
    });

    const db = new rds.DatabaseInstance(this, "BirddexDb", {
      engine: rds.DatabaseInstanceEngine.postgres({
        version: rds.PostgresEngineVersion.VER_16,
      }),
      instanceType: ec2.InstanceType.of(
        ec2.InstanceClass.T4G,
        ec2.InstanceSize.MICRO,
      ),
      credentials: rds.Credentials.fromSecret(dbSecret),
      databaseName: DB_NAME,
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED },
      securityGroups: [dbSg],
      multiAz: false,
      storageEncrypted: true,
      backupRetention: cdk.Duration.days(7),
      deletionProtection: true,
      removalPolicy: cdk.RemovalPolicy.RETAIN,
    });

    // =========================================================
    // === APP SECRET (Google OAuth + better-auth + SES) ===
    // =========================================================

    const appSecret = new secretsmanager.Secret(this, "AppSecret", {
      secretName: "/birddex/app/secrets",
      description: "BirdDex app secrets: better-auth, Google OAuth, Resend",
      generateSecretString: {
        secretStringTemplate: JSON.stringify({
          BETTER_AUTH_SECRET: "",
          GOOGLE_CLIENT_ID: "",
          GOOGLE_CLIENT_SECRET: "",
          RESEND_API_KEY: "",
          FROM_EMAIL: "noreply@birddex.fun",
        }),
        generateStringKey: "_placeholder",
      },
    });

    // =========================================================
    // === API GATEWAY ===
    // =========================================================

    // Created here (before lambdas) so APP_BASE_URL can be passed in lambda env vars.
    const httpApi = new apigwv2.HttpApi(this, "BirddexApi", {
      apiName: "birddex-api",
      corsPreflight: {
        allowCredentials: true,
        allowHeaders: [
          "Content-Type",
          "Authorization",
          "Cookie",
          "X-Requested-With",
        ],
        allowMethods: [
          apigwv2.CorsHttpMethod.GET,
          apigwv2.CorsHttpMethod.POST,
          apigwv2.CorsHttpMethod.PUT,
          apigwv2.CorsHttpMethod.DELETE,
          apigwv2.CorsHttpMethod.OPTIONS,
        ],
        allowOrigins: [
          // Replace with your frontend domain in production
          "http://localhost:5173",
          "https://birddex.fun",
        ],
        maxAge: cdk.Duration.hours(1),
      },
    });

    // =========================================================
    // === SHARED LAMBDA CONFIG ===
    // =========================================================

    const sharedBundling: NodejsFunctionProps["bundling"] = {
      externalModules: ["@aws-sdk/*"],
      minify: true,
      tsconfig: path.join(__dirname, "../lambda/tsconfig.json"),
    };

    const sharedLambdaProps: Partial<NodejsFunctionProps> = {
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      securityGroups: [lambdaSg],
      runtime: lambda.Runtime.NODEJS_22_X,
      architecture: lambda.Architecture.ARM_64,
      bundling: sharedBundling,
    };

    const dbEnv = {
      DB_SECRET_ARN: db.secret!.secretArn,
      DB_HOST: db.dbInstanceEndpointAddress,
      DB_NAME,
    };

    // =========================================================
    // === MIGRATION (Custom Resource) ===
    // =========================================================

    const migrateLambda = new NodejsFunction(this, "MigrateLambda", {
      ...sharedLambdaProps,
      entry: path.join(__dirname, "../lambda/migrate/index.ts"),
      handler: "handler",
      functionName: "birddex-migrate",
      memorySize: 256,
      timeout: cdk.Duration.minutes(5),
      environment: dbEnv,
      bundling: {
        ...sharedBundling,
        commandHooks: {
          beforeBundling: () => [],
          beforeInstall: () => [],
          afterBundling: (inputDir: string, outputDir: string) => [
            `cp ${inputDir}/lambda/migrate/schema.sql ${outputDir}/schema.sql`,
          ],
        },
      },
    });

    const migrateProvider = new cr.Provider(this, "MigrateProvider", {
      onEventHandler: migrateLambda,
    });

    new cdk.CustomResource(this, "MigrateResource", {
      serviceToken: migrateProvider.serviceToken,
      properties: {
        schemaVersion: "2",
      },
    });

    // =========================================================
    // === LAMBDAS ===
    // =========================================================

    const authLambda = new NodejsFunction(this, "AuthLambda", {
      ...sharedLambdaProps,
      entry: path.join(__dirname, "../lambda/auth/index.ts"),
      handler: "handler",
      functionName: "birddex-auth",
      memorySize: 512,
      timeout: cdk.Duration.seconds(15),
      environment: {
        ...dbEnv,
        APP_SECRET_ARN: appSecret.secretArn,
        APP_BASE_URL: "https://birddex.fun",
        FRONTEND_ORIGIN: "https://birddex.fun,http://localhost:5173",
      },
      bundling: {
        ...sharedBundling,
        commandHooks: {
          beforeBundling: () => [],
          beforeInstall: () => [],
          afterBundling: (inputDir: string, outputDir: string) => [
            `cp ${inputDir}/lambda/rds-ca-bundle.pem ${outputDir}/rds-ca-bundle.pem`,
          ],
        },
      },
    });

    const apiLambda = new NodejsFunction(this, "ApiLambda", {
      ...sharedLambdaProps,
      entry: path.join(__dirname, "../lambda/api/index.ts"),
      handler: "handler",
      functionName: "birddex-api",
      memorySize: 256,
      timeout: cdk.Duration.seconds(10),
      environment: {
        ...dbEnv,
        BUCKET_NAME: bucket.bucketName,
        APP_SECRET_ARN: appSecret.secretArn,
      },
      bundling: {
        ...sharedBundling,
        commandHooks: {
          beforeBundling: () => [],
          beforeInstall: () => [],
          afterBundling: (inputDir: string, outputDir: string) => [
            `cp ${inputDir}/lambda/rds-ca-bundle.pem ${outputDir}/rds-ca-bundle.pem`,
          ],
        },
      },
    });

    // Detect Lambda: container image (onnxruntime-node exceeds Lambda zip size limit)
    const detectLambda = new lambda.DockerImageFunction(this, "DetectLambda", {
      code: lambda.DockerImageCode.fromImageAsset(path.join(__dirname, ".."), {
        file: "lambda/detect/Dockerfile",
      }),
      functionName: "birddex-detect",
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      securityGroups: [lambdaSg],
      architecture: lambda.Architecture.X86_64,
      memorySize: 3008,
      timeout: cdk.Duration.seconds(30),
      ephemeralStorageSize: cdk.Size.mebibytes(1024),
      environment: {
        ...dbEnv,
        BUCKET_NAME: bucket.bucketName,
      },
    });

    // =========================================================
    // === IAM GRANTS ===
    // =========================================================

    db.secret!.grantRead(authLambda);
    db.secret!.grantRead(apiLambda);
    db.secret!.grantRead(detectLambda);
    db.secret!.grantRead(migrateLambda);

    appSecret.grantRead(authLambda);
    appSecret.grantRead(apiLambda);

    bucket.grantPut(apiLambda, "images/*");
    bucket.grantRead(apiLambda, "images/*");
    bucket.grantDelete(apiLambda, "images/*");

    bucket.grantRead(detectLambda, "models/*");
    bucket.grantRead(detectLambda, "images/*");

    // =========================================================
    // === API GATEWAY ROUTES ===
    // =========================================================

    httpApi.addRoutes({
      path: "/api/auth/{proxy+}",
      methods: [apigwv2.HttpMethod.ANY],
      integration: new HttpLambdaIntegration("AuthIntegration", authLambda),
    });

    httpApi.addRoutes({
      path: "/api/detect",
      methods: [apigwv2.HttpMethod.POST],
      integration: new HttpLambdaIntegration("DetectIntegration", detectLambda),
    });

    httpApi.addRoutes({
      path: "/api/{proxy+}",
      methods: [apigwv2.HttpMethod.ANY],
      integration: new HttpLambdaIntegration("ApiIntegration", apiLambda),
    });

    // =========================================================
    // === CLOUDFRONT ===
    // =========================================================

    const certArn = this.node.tryGetContext("certArn") as string | undefined;

    const oac = new cloudfront.S3OriginAccessControl(this, "FrontendOAC", {
      signing: cloudfront.Signing.SIGV4_NO_OVERRIDE,
    });

    const distribution = new cloudfront.Distribution(this, "Distribution", {
      webAclId: "arn:aws:wafv2:us-east-1:159987617860:global/webacl/CreatedByCloudFront-d6eca98a/e2e78ffa-77a6-456d-bb06-fda1aaddac97",
      defaultBehavior: {
        origin: origins.S3BucketOrigin.withOriginAccessControl(frontendBucket, {
          originAccessControl: oac,
        }),
        viewerProtocolPolicy:
          cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
        cachePolicy: cloudfront.CachePolicy.CACHING_OPTIMIZED,
      },
      additionalBehaviors: {
        "/api/*": {
          origin: new origins.HttpOrigin(
            `${httpApi.apiId}.execute-api.${this.region}.amazonaws.com`,
          ),
          viewerProtocolPolicy:
            cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
          cachePolicy: cloudfront.CachePolicy.CACHING_DISABLED,
          originRequestPolicy:
            cloudfront.OriginRequestPolicy.ALL_VIEWER_EXCEPT_HOST_HEADER,
          allowedMethods: cloudfront.AllowedMethods.ALLOW_ALL,
        },
      },
      ...(certArn
        ? {
            domainNames: ["birddex.fun", "www.birddex.fun"],
            certificate: acm.Certificate.fromCertificateArn(
              this,
              "Cert",
              certArn,
            ),
          }
        : {}),
      errorResponses: [
        {
          httpStatus: 403,
          responseHttpStatus: 200,
          responsePagePath: "/index.html",
        },
        {
          httpStatus: 404,
          responseHttpStatus: 200,
          responsePagePath: "/index.html",
        },
      ],
    });

    // =========================================================
    // === OUTPUTS ===
    // =========================================================

    new cdk.CfnOutput(this, "ApiUrl", {
      description: "HTTP API Gateway endpoint URL",
      value: httpApi.apiEndpoint,
      exportName: "BirddexApiUrl",
    });

    new cdk.CfnOutput(this, "BucketName", {
      description: "S3 bucket name",
      value: bucket.bucketName,
      exportName: "BirddexBucketName",
    });

    new cdk.CfnOutput(this, "DbEndpoint", {
      description: "RDS instance endpoint",
      value: db.dbInstanceEndpointAddress,
      exportName: "BirddexDbEndpoint",
    });

    new cdk.CfnOutput(this, "CloudFrontUrl", {
      description: "CloudFront distribution URL",
      value: `https://${distribution.distributionDomainName}`,
      exportName: "BirddexCloudFrontUrl",
    });

    new cdk.CfnOutput(this, "FrontendBucketName", {
      description: "Frontend S3 bucket name",
      value: frontendBucket.bucketName,
      exportName: "BirddexFrontendBucketName",
    });
  }
}
