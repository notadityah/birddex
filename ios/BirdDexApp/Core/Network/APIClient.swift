//Core/Network/APIClient.swift

//Created by Riley Wade on 13/04/2026

import Foundation

//MARK: errors

enum APIError: Error, LocalizedError {
    case invalidURL
    case unauthorized       //401
    case forbidden          //403
    case notFound           //404
    case payloadTooLarge    //413 - 6MB limit
    case serverError(Int)
    case decodingFailed(Error)
    case networkUnavailable
    case unknown(Error)

    var errorDescription: String? {
        switch self{
            case .unauthorized:         return "Session expired. Please log in again."
            case .payloadTooLarge:      return "Image is too large. Please try using a smaller image"
            case .networkUnavailable:   return "No Internet connection."
            case .serverError(let c):   return "Server Error(\(c)). Please try again"
            default:                    return "Something went wrong."
        }
    }
}

//MARK: request config

struct APIrequest {
    let endpoint: Endpoint
    var body: Data? = nil
    var headers: [String:String] = [:]
    var queryItems: [URLQueryItem] = []
}

actor APIClient {

    static let shared = APIClient()

    private let session: URLSession
    private let tokenManager: TokenManager
    private let maxRetries = 1          //retry once for token refresh

    private init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        self.session = URLSession(configuration: config)
        self.tokenManager = TokenManager.shared
    }

    //MARK: generic request
    func request<T: Decodable>(
        _ apirequest: APIrequest,
        as type: T.Type
    )   async throws -> T {
        let data = try await performRequest(apirequest, retryCount: 0)
        do {
            return try JSONDecoder().decode(T.self, from: data)
        } catch {
            throw APIError.decodingFailed(error)
        }
    }

    }

    //MARK: Multipart (image upload & detect)

    func upload<T: Decodable>(
        imageData: Data,
        to endpoint: Endpoint,
        as type: T.Type
    )   async throws -> T {
        let boundary = "Boundary-\(UUID().uuidString)"
        var body = Data()

        // Multipart field
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"image\"; filename=\"photo.jpg\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: image/jpeg\r\n\r\n".data(using: .utf8)!)
        body.append(imageData)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)

        var req = APIRequest(endpoint: endpoint, body: body)
        req.headers["Content-Type"] = "multipart/form-data; boundary=\(boundary)"

        return try await request(req, as: T.self)
    }

    //MARK: Core Execution

    private func performRequest(
        _ apirequest: APIrequest,
        retryCount: Int
    )   async throws -> Data {

            var urlRequest = try buildURLRequest(from: apirequest)

            //attach auth token
            if let token = await tokenManager.accessToken {
                urlRequest.setValue("Bearer\(token)", forHTTPHeaderField: "Authorization")
            }

            let (data, response): (Data, URLResponse)
            do {
                (data, response) = try await session.data(urlRequest)
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
                    //attempt to refresh token 
                    guard retryCount < maxRetries else {
                        throw APIError.unauthorized
                    }
                    try await tokenManager.refresh()
                    return try await performRequest(apirequest, retryCount: retryCount + 1)

                case 403: throw APIError.forbidden
                case 404: throw APIError.notFound
                case 413: throw APIError.payloadTooLarge
                case 500...599: throw APIError.serverError(http.statusCode)

                default: throw APIError.serverError(http.statusCode)
            }
    }

    //MARK: URL Builder
    private func buildURLRequest(from: apiRequest) throws -> URLRequest {
        guard var components = URLComponents (
            url: apiRequest.endpoint.url,
            resolvingAgainstBaseURL: true 
        ) else {
            throw APIError.invalidURL
        }

        if !apiRequest.queryItems.isEmpty {
            components.queryItems = apiRequest.queryItems
        }

        guard let url = components.url else {throw APIError.invalidURL}

        var request = URLRequest(url: url)
        request.httpMethod = apiRequest.endpoint.method
        request.httpbody = apiRequest.body

        //default headers
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if apiRequest.body != nil && apiRequest.headers["Content-Type"] == nil {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        //custom headers
        apiRequest.headers.forEach { request.setValue($1, forHTTPHeaderField: $0) }

        return request
    }
}
