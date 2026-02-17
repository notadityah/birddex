import { defineAuth, secret } from '@aws-amplify/backend'

/**
 * Define and configure your auth resource
 * @see https://docs.amplify.aws/gen2/build-a-backend/auth
 */
export const auth = defineAuth({
  loginWith: {
    email: true,
    externalProviders: {
      google: {
        clientId: secret('GOOGLE_CLIENT_ID'),
        clientSecret: secret('GOOGLE_CLIENT_SECRET'),
        scopes: ['email', 'profile', 'openid'],
        attributeMapping: {
          email: 'email',
          fullname: 'name',
        },
      },
      callbackUrls: ['http://localhost:5173/', 'https://birddex.app/'],
      logoutUrls: ['http://localhost:5173/', 'https://birddex.app/'],
    },
  },
  userAttributes: {
    fullname: {
      required: true,
      mutable: true,
    },
  },
  groups: ['ADMINS'],
})
