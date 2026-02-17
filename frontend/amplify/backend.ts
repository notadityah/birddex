import { defineBackend } from '@aws-amplify/backend'
import { PolicyStatement, Effect } from 'aws-cdk-lib/aws-iam'
import { auth } from './auth/resource'
import { data } from './data/resource'
import {
  listAllUsers,
  addUserToGroup,
  removeUserFromGroup,
} from './functions/user-management/resource'

const backend = defineBackend({
  auth,
  data,
  listAllUsers,
  addUserToGroup,
  removeUserFromGroup,
})

// Get the Cognito User Pool ID and grant Lambda functions access
const userPoolId = backend.auth.resources.userPool.userPoolId

const cognitoPolicy = new PolicyStatement({
  effect: Effect.ALLOW,
  actions: [
    'cognito-idp:ListUsers',
    'cognito-idp:AdminListGroupsForUser',
    'cognito-idp:AdminAddUserToGroup',
    'cognito-idp:AdminRemoveUserFromGroup',
  ],
  resources: [backend.auth.resources.userPool.userPoolArn],
})

// Inject USER_POOL_ID env var and attach IAM policy to each function
for (const fn of [backend.listAllUsers, backend.addUserToGroup, backend.removeUserFromGroup]) {
  fn.addEnvironment('USER_POOL_ID', userPoolId)
  fn.resources.lambda.addToRolePolicy(cognitoPolicy)
}
