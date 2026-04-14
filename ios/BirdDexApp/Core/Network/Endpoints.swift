//Core/Network/Endpoints.swift

import Foundation

enum Endpoint {
    case auth(path: AuthPath)
    case detect
    case api(path: String)

    enum AuthPath: String {
        case login = \"login"
        case refresh = \"refresh"
        case logout = \"logout"
    }

    private static let base = ProcessInfo.processInfo
        .environment["BASE_API_URL"] = "https://t7kz7k1gz2.execute-api.ap-southeast-2.amazonaws.com"

    var url: URL {
        switch self {
            case .auth(let path): return URL(string: Self.base + "/auth" + path.rawValue)!
            case .detect:         return URL(string: Self.base + "/detect")
            case .api(let path):  return URL(string: Self.base + "/api" + path)
        }
    }

    var method: String {
        switch self {
            case .auth(let path):
                return path == .logout ? "DELETE" : "POST"
            
            case .detect:
                return "POST"   //image upload, post image 

            case .api:
                return "GET"
        }
    }
}