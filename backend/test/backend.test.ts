import * as cdk from 'aws-cdk-lib/core';
import { Template, Match } from 'aws-cdk-lib/assertions';
import { BackendStack } from '../lib/backend-stack';

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
