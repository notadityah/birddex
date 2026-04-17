//Core/Network/APIClient.swift

//Created by Riley Wade on 13/04/2026

// In APIClient.swift — replace the session property and performRequest (17/04/2026)

// Use SessionManager's URLSession so cookies are shared
private let session = SessionManager.shared.urlSession

private func performRequest(
    _ apiRequest: APIRequest,
    retryCount: Int
) async throws -> Data {

    var urlRequest = try buildURLRequest(from: apiRequest)

    // No Bearer token — cookies are attached automatically by URLSession

    let (data, response): (Data, URLResponse)
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
        // Session expired — user needs to log in again
        // (better-auth doesn't have a refresh token flow)
        await SessionManager.shared.clearSession()
        throw APIError.unauthorized
    case 403: throw APIError.forbidden
    case 404: throw APIError.notFound
    case 413: throw APIError.payloadTooLarge
    case 500...599: throw APIError.serverError(http.statusCode)
    default: throw APIError.serverError(http.statusCode)
    }
}
