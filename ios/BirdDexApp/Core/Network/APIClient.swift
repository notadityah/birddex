// Core/Network/APIClient.swift
// Created by Riley Wade on 13/04/2026

import Foundation

enum APIError: Error, LocalizedError {
    case invalidURL
    case unauthorized
    case forbidden
    case notFound
    case payloadTooLarge
    case serverError(Int)
    case decodingFailed(Error)
    case networkUnavailable
    case unknown(Error)

    var errorDescription: String? {
        switch self {
        case .unauthorized:
            return "Session expired. Please log in again."
        case .payloadTooLarge:
            return "Image is too large. Please try using a smaller image."
        case .networkUnavailable:
            return "No internet connection."
        case .serverError(let code):
            return "Server error (\(code)). Please try again."
        default:
            return "Something went wrong."
        }
    }
}

struct APIRequest {
    let endpoint: Endpoint
    var body: Data? = nil
    var headers: [String: String] = [:]
    var queryItems: [URLQueryItem] = []
}

protocol HTTPSession: Sendable {
    func data(for request: URLRequest) async throws -> (Data, URLResponse)
}

extension URLSession: HTTPSession {}

actor APIClient {
    static let shared = APIClient(
        session: APIClient.makeSession(),
        onUnauthorized: {
            await SessionManager.shared.clearSession()
        }
    )

    private let session: HTTPSession
    private let onUnauthorized: @Sendable () async -> Void

    init(
        session: HTTPSession,
        onUnauthorized: @escaping @Sendable () async -> Void = {}
    ) {
        self.session = session
        self.onUnauthorized = onUnauthorized
    }

    func request<T: Decodable>(
        _ apiRequest: APIRequest,
        as type: T.Type
    ) async throws -> T {
        let data = try await performRequest(apiRequest)
        do {
            return try JSONDecoder().decode(T.self, from: data)
        } catch {
            throw APIError.decodingFailed(error)
        }
    }

    func upload<T: Decodable>(
        imageData: Data,
        to endpoint: Endpoint,
        as type: T.Type
    ) async throws -> T {
        let boundary = "Boundary-\(UUID().uuidString)"
        var body = Data()

        body.append(Data("--\(boundary)\r\n".utf8))
        body.append(Data("Content-Disposition: form-data; name=\"image\"; filename=\"photo.jpg\"\r\n".utf8))
        body.append(Data("Content-Type: image/jpeg\r\n\r\n".utf8))
        body.append(imageData)
        body.append(Data("\r\n--\(boundary)--\r\n".utf8))

        var request = APIRequest(endpoint: endpoint, body: body)
        request.headers["Content-Type"] = "multipart/form-data; boundary=\(boundary)"

        return try await self.request(request, as: type)
    }

    private func performRequest(_ apiRequest: APIRequest) async throws -> Data {
        let urlRequest = try buildURLRequest(from: apiRequest)

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: urlRequest)
        } catch {
            throw APIError.networkUnavailable
        }

        guard let http = response as? HTTPURLResponse else {
            throw APIError.unknown(URLError(.badServerResponse))
        }

        switch http.statusCode {
        case 200...299:
            return data
        case 401:
            await onUnauthorized()
            throw APIError.unauthorized
        case 403:
            throw APIError.forbidden
        case 404:
            throw APIError.notFound
        case 413:
            throw APIError.payloadTooLarge
        case 500...599:
            throw APIError.serverError(http.statusCode)
        default:
            throw APIError.serverError(http.statusCode)
        }
    }

    private func buildURLRequest(from apiRequest: APIRequest) throws -> URLRequest {
        guard var components = URLComponents(
            url: apiRequest.endpoint.url,
            resolvingAgainstBaseURL: true
        ) else {
            throw APIError.invalidURL
        }

        if !apiRequest.queryItems.isEmpty {
            components.queryItems = apiRequest.queryItems
        }

        guard let url = components.url else {
            throw APIError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = apiRequest.endpoint.method
        request.httpBody = apiRequest.body
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        if apiRequest.body != nil, apiRequest.headers["Content-Type"] == nil {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        apiRequest.headers.forEach { key, value in
            request.setValue(value, forHTTPHeaderField: key)
        }

        return request
    }

    private static func makeSession() -> URLSession {
        let config = URLSessionConfiguration.default
        config.httpCookieAcceptPolicy = .always
        config.httpShouldSetCookies = true
        config.httpCookieStorage = .shared
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        return URLSession(configuration: config)
    }
}
