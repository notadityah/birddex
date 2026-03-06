#!/usr/bin/env node
import * as cdk from 'aws-cdk-lib/core';
import { BackendStack } from '../lib/backend-stack';

const app = new cdk.App();
new BackendStack(app, 'BackendStack', {
  // Aurora Serverless v2 and RDS Proxy require a concrete account/region.
  // Set CDK_DEFAULT_ACCOUNT and CDK_DEFAULT_REGION before deploying.
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION,
  },
});
