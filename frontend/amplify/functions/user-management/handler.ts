import type { Schema } from '../../data/resource'
import {
  CognitoIdentityProviderClient,
  AdminAddUserToGroupCommand,
  AdminRemoveUserFromGroupCommand,
  ListUsersCommand,
  AdminListGroupsForUserCommand,
} from '@aws-sdk/client-cognito-identity-provider'

const client = new CognitoIdentityProviderClient()
const USER_POOL_ID = process.env.USER_POOL_ID!

type ListUsersHandler = Schema['listAllUsers']['functionHandler']
type AddToGroupHandler = Schema['addUserToGroup']['functionHandler']
type RemoveFromGroupHandler = Schema['removeUserFromGroup']['functionHandler']

/** List all users in the Cognito user pool with their group memberships */
export const listUsers: ListUsersHandler = async () => {
  const { Users = [] } = await client.send(
    new ListUsersCommand({ UserPoolId: USER_POOL_ID, Limit: 60 }),
  )

  const users = await Promise.all(
    Users.map(async (u) => {
      const email = u.Attributes?.find((a) => a.Name === 'email')?.Value ?? ''
      const name = u.Attributes?.find((a) => a.Name === 'name')?.Value ?? ''

      const { Groups = [] } = await client.send(
        new AdminListGroupsForUserCommand({
          UserPoolId: USER_POOL_ID,
          Username: u.Username!,
        }),
      )

      return {
        username: u.Username!,
        email,
        name,
        groups: Groups.map((g) => g.GroupName!),
        status: u.UserStatus ?? 'UNKNOWN',
        createdAt: u.UserCreateDate?.toISOString() ?? '',
      }
    }),
  )

  return users
}

/** Add a user to a Cognito group */
export const addToGroup: AddToGroupHandler = async (event) => {
  const { username, groupName } = event.arguments

  await client.send(
    new AdminAddUserToGroupCommand({
      UserPoolId: USER_POOL_ID,
      Username: username,
      GroupName: groupName,
    }),
  )

  return { success: true, message: `User ${username} added to ${groupName}` }
}

/** Remove a user from a Cognito group */
export const removeFromGroup: RemoveFromGroupHandler = async (event) => {
  const { username, groupName } = event.arguments

  await client.send(
    new AdminRemoveUserFromGroupCommand({
      UserPoolId: USER_POOL_ID,
      Username: username,
      GroupName: groupName,
    }),
  )

  return { success: true, message: `User ${username} removed from ${groupName}` }
}
