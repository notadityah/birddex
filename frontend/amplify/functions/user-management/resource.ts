import { defineFunction } from '@aws-amplify/backend'

export const listAllUsers = defineFunction({
  name: 'listAllUsers',
  entry: './handler.ts',
  environment: {
    USER_POOL_ID: '', // injected by backend.ts
  },
})

export const addUserToGroup = defineFunction({
  name: 'addUserToGroup',
  entry: './handler.ts',
  environment: {
    USER_POOL_ID: '', // injected by backend.ts
  },
})

export const removeUserFromGroup = defineFunction({
  name: 'removeUserFromGroup',
  entry: './handler.ts',
  environment: {
    USER_POOL_ID: '', // injected by backend.ts
  },
})
