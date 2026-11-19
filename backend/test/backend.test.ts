import * as cdk from 'aws-cdk-lib/core';
import { Template, Match } from 'aws-cdk-lib/assertions';
import { BackendStack } from '../lib/backend-stack';
import { SPA_REWRITE_FUNCTION_CODE } from '../lib/spa-routing';

function buildTemplate() {
  const app = new cdk.App();
  const stack = new BackendStack(app, 'TestStack', {
    env: { account: '123456789012', region: 'ap-southeast-2' },
  });
  return Template.fromStack(stack);
}

describe('BackendStack', () => {
  let template: Template;

  beforeAll(() => {
    template = buildTemplate();
  });

  // =========================================================
  // S3
  // =========================================================
  test('S3 bucket with versioning and block-all public access', () => {
    template.hasResourceProperties('AWS::S3::Bucket', {
      VersioningConfiguration: { Status: 'Enabled' },
      PublicAccessBlockConfiguration: {
        BlockPublicAcls: true,
        BlockPublicPolicy: true,
        IgnorePublicAcls: true,
        RestrictPublicBuckets: true,
      },
    });
  });

  test('S3 bucket has CORS configuration for PUT and GET', () => {
    template.hasResourceProperties('AWS::S3::Bucket', {
      CorsConfiguration: {
        CorsRules: Match.arrayWith([
          Match.objectLike({
            // CDK serializes methods in declaration order: GET then PUT
            AllowedMethods: Match.arrayWith(['GET', 'PUT']),
          }),
        ]),
      },
    });
  });

  test('S3 bucket has lifecycle rule for noncurrent version expiration', () => {
    template.hasResourceProperties('AWS::S3::Bucket', {
      LifecycleConfiguration: {
        Rules: Match.arrayWith([
          Match.objectLike({
            NoncurrentVersionExpiration: { NoncurrentDays: 30 },
            Status: 'Enabled',
          }),
        ]),
      },
    });
  });

  // =========================================================
  // No VPC/RDS (Neon serverless Postgres)
  // =========================================================
  test('No VPC is created (using Neon)', () => {
    template.resourceCountIs('AWS::EC2::VPC', 0);
  });

  test('No RDS instance is created (using Neon)', () => {
    template.resourceCountIs('AWS::RDS::DBInstance', 0);
  });

  test('No NAT Gateway is created (using Neon)', () => {
    template.resourceCountIs('AWS::EC2::NatGateway', 0);
  });

  // =========================================================
  // Lambda functions
  // =========================================================
  test('Auth Lambda is created with ARM_64 and correct memory', () => {
    template.hasResourceProperties('AWS::Lambda::Function', {
      FunctionName: 'birddex-auth',
      Architectures: ['arm64'],
      MemorySize: 512,
    });
  });

  test('API Lambda is created with ARM_64 and correct memory', () => {
    template.hasResourceProperties('AWS::Lambda::Function', {
      FunctionName: 'birddex-api',
      Architectures: ['arm64'],
      MemorySize: 256,
    });
  });

  test('Detect Lambda is created as container image with 1.5 GiB memory', () => {
    template.hasResourceProperties('AWS::Lambda::Function', {
      FunctionName: 'birddex-detect',
      PackageType: 'Image',
      MemorySize: 1536,
    });
  });

  test('Detect Lambda points at a versioned model in S3', () => {
    template.hasResourceProperties('AWS::Lambda::Function', {
      FunctionName: 'birddex-detect',
      Environment: {
        Variables: Match.objectLike({
          MODEL_S3_KEY: Match.stringLikeRegexp('^models/v\\d+/model\\.onnx$'),
          CLASSES_S3_KEY: Match.stringLikeRegexp('^models/v\\d+/classes\\.txt$'),
        }),
      },
    });
  });

  // =========================================================
  // API Gateway
  // =========================================================
  test('HTTP API is created', () => {
    template.resourceCountIs('AWS::ApiGatewayV2::Api', 1);
  });

  test('HTTP API has CORS preflight config', () => {
    template.hasResourceProperties('AWS::ApiGatewayV2::Api', {
      CorsConfiguration: Match.objectLike({
        AllowCredentials: true,
      }),
    });
  });

  test('Auth route exists', () => {
    template.hasResourceProperties('AWS::ApiGatewayV2::Route', {
      RouteKey: 'ANY /api/auth/{proxy+}',
    });
  });

  test('Detect route exists', () => {
    template.hasResourceProperties('AWS::ApiGatewayV2::Route', {
      RouteKey: 'POST /api/detect',
    });
  });

  test('API catch-all route exists', () => {
    template.hasResourceProperties('AWS::ApiGatewayV2::Route', {
      RouteKey: 'ANY /api/{proxy+}',
    });
  });

  test('API Gateway default stage has throttle settings', () => {
    template.hasResourceProperties('AWS::ApiGatewayV2::Stage', {
      StageName: '$default',
      DefaultRouteSettings: {
        ThrottlingBurstLimit: 50,
        ThrottlingRateLimit: 100,
      },
    });
  });

  // =========================================================
  // CloudFront
  // =========================================================
  function distributionConfig() {
    const distributions = Object.values(template.findResources('AWS::CloudFront::Distribution'));
    expect(distributions).toHaveLength(1);
    return distributions[0].Properties.DistributionConfig;
  }

  test('CloudFront has no distribution-level error responses (they would mask /api/* 403/404 as 200 index.html)', () => {
    expect(distributionConfig().CustomErrorResponses).toBeUndefined();
  });

  test('SPA rewrite function runs on viewer-request for the default (S3) behavior only', () => {
    const functionIds = Object.keys(template.findResources('AWS::CloudFront::Function'));
    expect(functionIds).toHaveLength(1);

    const config = distributionConfig();
    expect(config.DefaultCacheBehavior.FunctionAssociations).toEqual([
      {
        EventType: 'viewer-request',
        FunctionARN: { 'Fn::GetAtt': [functionIds[0], 'FunctionARN'] },
      },
    ]);

    expect(config.CacheBehaviors.map((b: { PathPattern: string }) => b.PathPattern)).toEqual([
      '/api/*',
    ]);
    for (const behavior of config.CacheBehaviors) {
      expect(behavior.FunctionAssociations).toBeUndefined();
      expect(behavior.LambdaFunctionAssociations).toBeUndefined();
    }
  });

  test('SPA rewrite function deploys the tested source on the cloudfront-js-2.0 runtime', () => {
    template.hasResourceProperties('AWS::CloudFront::Function', {
      AutoPublish: true,
      FunctionCode: SPA_REWRITE_FUNCTION_CODE,
      FunctionConfig: Match.objectLike({ Runtime: 'cloudfront-js-2.0' }),
    });
  });

  test('Frontend bucket lets CloudFront list keys, so missing assets are 404 (not 403)', () => {
    const statements = Object.values(template.findResources('AWS::S3::BucketPolicy')).flatMap(
      (policy) => policy.Properties.PolicyDocument.Statement,
    );
    const cloudFrontList = statements.filter(
      (s) =>
        s.Principal?.Service === 'cloudfront.amazonaws.com' &&
        [s.Action].flat().includes('s3:ListBucket'),
    );
    expect(cloudFrontList).toHaveLength(1);
    // With LIST granted, "/" must never reach S3 as a bucket listing.
    expect(distributionConfig().DefaultRootObject).toBe('index.html');
  });

  // =========================================================
  // Monitoring
  // =========================================================
  test('SNS alarm topic is created', () => {
    template.hasResourceProperties('AWS::SNS::Topic', {
      TopicName: 'birddex-alarms',
    });
  });

  test('Lambda error alarms are created', () => {
    template.resourceCountIs('AWS::CloudWatch::Alarm', 3); // Auth + Api + Detect
  });

  // =========================================================
  // Outputs
  // =========================================================
  test('ApiUrl output is exported', () => {
    template.hasOutput('ApiUrl', {
      Export: { Name: 'BirddexApiUrl' },
    });
  });

  test('BucketName output is exported', () => {
    template.hasOutput('BucketName', {
      Export: { Name: 'BirddexBucketName' },
    });
  });
});

// Runs the exact CloudFront Function source that the stack deploys.
describe('SPA rewrite CloudFront Function', () => {
  type ViewerRequestHandler = (event: { request: { uri: string } }) => { uri: string };
  const handler = new Function(
    `${SPA_REWRITE_FUNCTION_CODE}\nreturn handler;`,
  )() as ViewerRequestHandler;
  const rewrite = (uri: string) => handler({ request: { uri } }).uri;

  test.each([
    '/',
    '/mydex',
    '/gallery',
    '/gallery/',
    '/login',
    '/admin',
    '/reset-password',
    '/some/unknown/route',
  ])('rewrites client-side route %s to /index.html', (uri) => {
    expect(rewrite(uri)).toBe('/index.html');
  });

  test.each([
    '/index.html',
    '/favicon.ico',
    '/assets/index-BxY12abc.js',
    '/assets/index-BxY12abc.css',
    '/birds/american-robin.webp',
    '/.well-known/assetlinks.json',
  ])('leaves static file %s unchanged', (uri) => {
    expect(rewrite(uri)).toBe(uri);
  });
});
