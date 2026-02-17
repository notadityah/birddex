import { type ClientSchema, a, defineData } from '@aws-amplify/backend'
import {
  listAllUsers,
  addUserToGroup,
  removeUserFromGroup,
} from '../functions/user-management/resource'

const schema = a.schema({
  Todo: a
    .model({
      content: a.string(),
    })
    .authorization((allow) => [allow.owner()]),

  // --- Admin user-management operations ---

  UserInfo: a.customType({
    username: a.string().required(),
    email: a.string().required(),
    name: a.string().required(),
    groups: a.string().array().required(),
    status: a.string().required(),
    createdAt: a.string().required(),
  }),

  MutationResult: a.customType({
    success: a.boolean().required(),
    message: a.string().required(),
  }),

  listAllUsers: a
    .query()
    .returns(a.ref('UserInfo').array())
    .authorization((allow) => [allow.group('ADMINS')])
    .handler(a.handler.function(listAllUsers)),

  addUserToGroup: a
    .mutation()
    .arguments({
      username: a.string().required(),
      groupName: a.string().required(),
    })
    .returns(a.ref('MutationResult'))
    .authorization((allow) => [allow.group('ADMINS')])
    .handler(a.handler.function(addUserToGroup)),

  removeUserFromGroup: a
    .mutation()
    .arguments({
      username: a.string().required(),
      groupName: a.string().required(),
    })
    .returns(a.ref('MutationResult'))
    .authorization((allow) => [allow.group('ADMINS')])
    .handler(a.handler.function(removeUserFromGroup)),
})

export type Schema = ClientSchema<typeof schema>

export const data = defineData({
  schema,
  authorizationModes: {
    defaultAuthorizationMode: 'userPool',
  },
})
