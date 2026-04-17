// Core/Network/Endpoints.swift

// Created by Riley Wade on 15/04/2026

import Foundation

enum Endpoint {

    // Auth (better-auth routes)
    case signIn
    case signUp
    case signOut
    case session

    // Core API
    case detect
    case api(path: String)

    private static let base = "https://YOUR-ID.execute-api.ap-southeast-2.amazonaws.com/prod"

    var url: URL {
        switch self {
        case .signIn:         return URL(string: Self.base + "/api/auth/sign-in/email")!
        case .signUp:         return URL(string: Self.base + "/api/auth/sign-up/email")!
        case .signOut:        return URL(string: Self.base + "/api/auth/sign-out")!
        case .session:        return URL(string: Self.base + "/api/auth/session")!
        case .detect:         return URL(string: Self.base + "/detect")!
        case .api(let path):  return URL(string: Self.base + "/api/" + path)!
        }
    }

    var method: String {
        switch self {
        case .session:        return "GET"
        default:              return "POST"
        }
    }
}