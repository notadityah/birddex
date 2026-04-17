// Core/Network/SessionManager.swift

//Created by Riley Wade on 16/04/2026

import Foundation

actor SessionManager {

    static let shared = SessionManager()

    // URLSession with persistent cookie storage
    // This automatically handles sending/receiving cookies
    let urlSession: URLSession = {
        let config = URLSessionConfiguration.default
        config.httpCookieAcceptPolicy  = .always
        config.httpShouldSetCookies    = true
        config.httpCookieStorage       = .shared
        return URLSession(configuration: config)
    }()

    // MARK: - Session state

    var isLoggedIn: Bool {
        sessionCookie != nil
    }

    private var sessionCookie: HTTPCookie? {
        HTTPCookieStorage.shared.cookies?.first {
            $0.name.contains("better-auth") || $0.name.contains("session")
        }
    }

    // MARK: - Sign in

    func signIn(email: String, password: String) async throws {
        let body = try JSONEncoder().encode([
            "email":    email,
            "password": password
        ])

        var request = URLRequest(url: Endpoint.signIn.url)
        request.httpMethod = "POST"
        request.httpBody   = body
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let (_, response) = try await urlSession.data(for: request)

        guard let http = response as? HTTPURLResponse else {
            throw APIError.unknown(URLError(.badServerResponse))
        }

        switch http.statusCode {
        case 200:
            break   // cookie is automatically stored by URLSession
        case 401, 403:
            throw APIError.unauthorized
        default:
            throw APIError.serverError(http.statusCode)
        }
    }

    // MARK: - Sign out

    func signOut() async throws {
        var request = URLRequest(url: Endpoint.signOut.url)
        request.httpMethod = "POST"

        try await urlSession.data(for: request)
        clearSession()
    }

    // MARK: - Verify session is still valid

    func validateSession() async -> Bool {
        guard isLoggedIn else { return false }

        var request = URLRequest(url: Endpoint.session.url)
        request.httpMethod = "GET"

        guard let (_, response) = try? await urlSession.data(for: request),
              let http = response as? HTTPURLResponse else {
            return false
        }

        return http.statusCode == 200
    }

    // MARK: - Clear

    func clearSession() {
        guard let cookies = HTTPCookieStorage.shared.cookies else { return }
        cookies.forEach { HTTPCookieStorage.shared.deleteCookie($0) }
    }
}