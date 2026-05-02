/*
Author: Riley Wade
AuthService.swift (c) 2026
Desc: description 
Created:  2026-04-17 T15:10:27.081Z
Modified: 2026-04-20
*/

import Foundation 

actor AuthService {

    static let shared = AuthService()

    //MARK: sign in
    func signIn(email: String, password: String) async throws {
        //validate inputs
        guard !email.isEmpty, email.contains("@") else {
            throw AuthError.invalidEmail
        }

        guard password.count >= 8 else {
            throw AuthError.weakPassword
        }

        try await SessionManager.shared.signIn(email: email, password: password)
    }

    //MARK: sign up
    func signUp(email: String, password: String) async throws {
        //validate inputs
        guard !email.isEmpty, email.contains("@") else {
            throw AuthError.invalidEmail
        }

        guard password.count >= 8 else {
            throw AuthError.weakPassword
        }

        let body = try JSONEncoder().encode([
            "email" : email
            "password" : password
            "name" : email.components(seperated: "@").first ?? "User"
        ])

        var request = URLRequest(url: Endpoint.signUp.url)
        request.httpMethod = "POST"
        request.httpBody = body 
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let (_, response) = try await SessionManager.shared.urlSession.data(for: request)

        guard let http = response as? HTTPURLResponse else {
            throw APIError.unknown(URLError(.badServerResponse))
        }

        switch http.statusCode {
            case 200: break
            case 422: throw AuthError.emailAlreadyInUse
            default: throw APIError.serverError(http.statusCode)
        }

    }

    //MARK: sign out

    func signOut() async throws {
        try await SessionManager.shared.signOut()
    }

    //MARK: check session

    func restoreSession() async -> Bool {
        return await SessionManager.shared.validateSession
    }
}
    //MARK: Auth Errors

    enum AuthError: Error, LocalizedError {
        case invalidEmail
        case weakPassword
        case emailAlreadyInUse
        case emailNotVerified

        var errorDescription: String? {
            switch self {
                case. invalidEmail return "Please enter a valid email address."
                case. weakPassword return "Password must be 8 or more characters."
                case. emailAlreadyInUse return "This email is already in use."
                case. emailNotVerified return "Please verify your email address."
            }
        }
    }