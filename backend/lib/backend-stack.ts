import * as cdk from "aws-cdk-lib/core";
import * as s3 from "aws-cdk-lib/aws-s3";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as rds from "aws-cdk-lib/aws-rds";
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
import * as logs from "aws-cdk-lib/aws-logs";
import * as cloudwatch from "aws-cdk-lib/aws-cloudwatch";
import * as sns from "aws-cdk-lib/aws-sns";
import * as cw_actions from "aws-cdk-lib/aws-cloudwatch-actions";
import { Construct } from "constructs";
import * as path from "path";

const DB_NAME = "birddex";

interface EnvConfig {
  domain?: string;
  allowedOrigins: string[];
  certArn?: string;
  webAclArn?: string;
}

const ENV_CONFIGS: Record<string, EnvConfig> = {
  prod: {
    domain: "birddex.fun",
    allowedOrigins: ["https://birddex.fun"],
  },
  dev: {
    allowedOrigins: ["http://localhost:5173"],
  },
};

export class BackendStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const envName = (this.node.tryGetContext("env") as string) ?? "prod";
    const envConfig = ENV_CONFIGS[envName] ?? ENV_CONFIGS.prod;

    // Override with context values if provided
    const certArn = this.node.tryGetContext("certArn") as string | undefined;
    const webAclArn = this.node.tryGetContext("webAclArn") as
      | string
      | undefined;
    if (certArn) envConfig.certArn = certArn;
    if (webAclArn) envConfig.webAclArn = webAclArn;

    // In dev mode, add localhost to allowed origins
    if (envName === "prod") {
      envConfig.allowedOrigins.push("http://localhost:5173");
    }

    // =========================================================
    // === S3 ===
    // =========================================================

    const bucket = new s3.Bucket(this, "BirddexBucket", {
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      encryption: s3.BucketEncryption.S3_MANAGED,
      versioned: true,
      removalPolicy: cdk.RemovalPolicy.RETAIN,
      lifecycleRules: [
        {
          noncurrentVersionExpiration: cdk.Duration.days(30),
        },
      ],
      cors: [
        {
          allowedMethods: [s3.HttpMethods.GET, s3.HttpMethods.PUT],
          allowedOrigins: envConfig.allowedOrigins,
          allowedHeaders: ["Content-Type", "Authorization", "x-amz-*"],
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

    // VPC Endpoints to reduce NAT Gateway data transfer costs
    vpc.addGatewayEndpoint("S3Endpoint", {
      service: ec2.GatewayVpcEndpointAwsService.S3,
    });

    vpc.addInterfaceEndpoint("SecretsManagerEndpoint", {
      service: ec2.InterfaceVpcEndpointAwsService.SECRETS_MANAGER,
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
    // === APP SECRET (Google OAuth + better-auth + Resend) ===
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
        allowOrigins: envConfig.allowedOrigins,
        maxAge: cdk.Duration.hours(1),
      },
    });

    // =========================================================
    // === SHARED LAMBDA CONFIG ===
    // =========================================================

    const LOG_RETENTION = logs.RetentionDays.TWO_WEEKS;

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
      logRetention: LOG_RETENTION,
    };

    const dbEnv = {
      DB_SECRET_ARN: db.secret!.secretArn,
      DB_HOST: db.dbInstanceEndpointAddress,
      DB_NAME,
    };

    const appBaseUrl = envConfig.domain
      ? `https://${envConfig.domain}`
      : "http://localhost:5173";

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
            `cp ${inputDir}/lambda/rds-ca-bundle.pem ${outputDir}/rds-ca-bundle.pem`,
          ],
        },
      },
    });

    const migrateProvider = new cr.Provider(this, "MigrateProvider", {
      onEventHandler: migrateLambda,
      logRetention: LOG_RETENTION,
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
        APP_BASE_URL: appBaseUrl,
        FRONTEND_ORIGIN: envConfig.allowedOrigins.join(","),
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
        APP_BASE_URL: appBaseUrl,
        FRONTEND_ORIGIN: envConfig.allowedOrigins.join(","),
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
      memorySize: 1536,
      timeout: cdk.Duration.seconds(30),
      ephemeralStorageSize: cdk.Size.mebibytes(1024),
      logRetention: LOG_RETENTION,
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

    const oac = new cloudfront.S3OriginAccessControl(this, "FrontendOAC", {
      signing: cloudfront.Signing.SIGV4_NO_OVERRIDE,
    });

    const distributionProps: cloudfront.DistributionProps = {
      defaultBehavior: {
        origin: origins.S3BucketOrigin.withOriginAccessControl(frontendBucket, {
          originAccessControl: oac,
        }),
        viewerProtocolPolicy: cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
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
    };

    // Conditionally add domain + cert + WAF
    if (envConfig.certArn && envConfig.domain) {
      Object.assign(distributionProps, {
        domainNames: [envConfig.domain, `www.${envConfig.domain}`],
        certificate: acm.Certificate.fromCertificateArn(
          this,
          "Cert",
          envConfig.certArn,
        ),
      });
    }

    if (envConfig.webAclArn) {
      Object.assign(distributionProps, {
        webAclId: envConfig.webAclArn,
      });
    }

    const distribution = new cloudfront.Distribution(
      this,
      "Distribution",
      distributionProps,
    );

    // =========================================================
    // === MONITORING ===
    // =========================================================

    const alarmTopic = new sns.Topic(this, "AlarmTopic", {
      topicName: "birddex-alarms",
    });

    const alarmAction = new cw_actions.SnsAction(alarmTopic);

    // Lambda error alarms
    for (const [name, fn] of Object.entries({
      Auth: authLambda,
      Api: apiLambda,
      Detect: detectLambda,
      Migrate: migrateLambda,
    })) {
      const alarm = fn
        .metricErrors({ period: cdk.Duration.minutes(5) })
        .createAlarm(this, `${name}ErrorAlarm`, {
          alarmName: `birddex-${name.toLowerCase()}-errors`,
          threshold: 1,
          evaluationPeriods: 1,
          treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });
      alarm.addAlarmAction(alarmAction);
    }

    // RDS CPU alarm
    const cpuAlarm = db
      .metricCPUUtilization({ period: cdk.Duration.minutes(5) })
      .createAlarm(this, "RdsCpuAlarm", {
        alarmName: "birddex-rds-cpu",
        threshold: 80,
        evaluationPeriods: 3,
        treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
      });
    cpuAlarm.addAlarmAction(alarmAction);

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

    new cdk.CfnOutput(this, "AlarmTopicArn", {
      description: "SNS topic for alarms — subscribe your email",
      value: alarmTopic.topicArn,
      exportName: "BirddexAlarmTopicArn",
    });
  }
}
