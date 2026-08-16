import Foundation
import Testing
@testable import BirdDexNetwork

struct APIClientTests {
    private struct BirdResponse: Decodable, Equatable {
        let name: String
    }

    private struct UploadResponse: Decodable, Equatable {
        let id: String
    }

    @Test
    func requestDecodesSuccessfulJSONResponse() async throws {
        let session = MockHTTPSession(
            behavior: .success(
                data: Data(#"{"name":"Kookaburra"}"#.utf8),
                response: makeHTTPResponse(statusCode: 200)
            )
        )
        let client = APIClient(session: session)
        let request = APIRequest(
            endpoint: .api(path: "birds"),
            queryItems: [URLQueryItem(name: "limit", value: "10")]
        )

        let result = try await client.request(request, as: BirdResponse.self)

        #expect(result == BirdResponse(name: "Kookaburra"))
        let captured = await session.lastCapturedRequest()
        #expect(captured?.method == "POST")
        #expect(captured?.headers["Accept"] == "application/json")
        #expect(captured?.url?.query == "limit=10")
    }

    @Test
    func requestSetsJSONContentTypeWhenBodyExists() async throws {
        let session = MockHTTPSession(
            behavior: .success(
                data: Data(#"{"name":"Magpie"}"#.utf8),
                response: makeHTTPResponse(statusCode: 200)
            )
        )
        let client = APIClient(session: session)
        let request = APIRequest(
            endpoint: .api(path: "birds"),
            body: Data(#"{"foo":"bar"}"#.utf8)
        )

        _ = try await client.request(request, as: BirdResponse.self)

        let captured = await session.lastCapturedRequest()
        #expect(captured?.headers["Content-Type"] == "application/json")
    }

    @Test
    func requestMaps404ToNotFoundError() async {
        let session = MockHTTPSession(
            behavior: .success(
                data: Data(),
                response: makeHTTPResponse(statusCode: 404)
            )
        )
        let client = APIClient(session: session)
        let request = APIRequest(endpoint: .api(path: "missing"))

        do {
            _ = try await client.request(request, as: BirdResponse.self)
            Issue.record("Expected APIError.notFound")
        } catch APIError.notFound {
        } catch {
            Issue.record("Unexpected error: \(error)")
        }
    }

    @Test
    func requestMapsNetworkingFailureToNetworkUnavailable() async {
        let session = MockHTTPSession(behavior: .failure(URLError(.notConnectedToInternet)))
        let client = APIClient(session: session)
        let request = APIRequest(endpoint: .api(path: "birds"))

        do {
            _ = try await client.request(request, as: BirdResponse.self)
            Issue.record("Expected APIError.networkUnavailable")
        } catch APIError.networkUnavailable {
        } catch {
            Issue.record("Unexpected error: \(error)")
        }
    }

    @Test
    func requestOn401InvokesUnauthorizedCallbackAndThrowsUnauthorized() async {
        let session = MockHTTPSession(
            behavior: .success(
                data: Data(),
                response: makeHTTPResponse(statusCode: 401)
            )
        )
        let tracker = UnauthorizedTracker()
        let client = APIClient(
            session: session,
            onUnauthorized: {
                await tracker.markUnauthorized()
            }
        )
        let request = APIRequest(endpoint: .api(path: "profile"))

        do {
            _ = try await client.request(request, as: BirdResponse.self)
            Issue.record("Expected APIError.unauthorized")
        } catch APIError.unauthorized {
        } catch {
            Issue.record("Unexpected error: \(error)")
        }

        #expect(await tracker.count() == 1)
    }

    @Test
    func uploadCreatesMultipartRequestAndDecodesResponse() async throws {
        let session = MockHTTPSession(
            behavior: .success(
                data: Data(#"{"id":"upload-1"}"#.utf8),
                response: makeHTTPResponse(statusCode: 200)
            )
        )
        let client = APIClient(session: session)

        let result = try await client.upload(
            imageData: Data([0x01, 0x02, 0x03]),
            to: .detect,
            as: UploadResponse.self
        )

        #expect(result == UploadResponse(id: "upload-1"))
        let captured = await session.lastCapturedRequest()
        #expect(captured?.headers["Content-Type"]?.hasPrefix("multipart/form-data; boundary=") == true)

        let bodyString = String(data: captured?.body ?? Data(), encoding: .utf8)
        #expect(bodyString?.contains("Content-Disposition: form-data; name=\"image\"") == true)
    }
}

private actor MockHTTPSession: HTTPSession {
    enum Behavior {
        case success(data: Data, response: HTTPURLResponse)
        case failure(Error)
    }

    private var behavior: Behavior
    private var capturedRequests: [CapturedRequest] = []

    init(behavior: Behavior) {
        self.behavior = behavior
    }

    func data(for request: URLRequest) async throws -> (Data, URLResponse) {
        capturedRequests.append(
            CapturedRequest(
                url: request.url,
                method: request.httpMethod,
                headers: request.allHTTPHeaderFields ?? [:],
                body: request.httpBody
            )
        )

        switch behavior {
        case .success(let data, let response):
            return (data, response)
        case .failure(let error):
            throw error
        }
    }

    func lastCapturedRequest() -> CapturedRequest? {
        capturedRequests.last
    }
}

private struct CapturedRequest: Sendable {
    let url: URL?
    let method: String?
    let headers: [String: String]
    let body: Data?
}

private actor UnauthorizedTracker {
    private var unauthorizedCount = 0

    func markUnauthorized() {
        unauthorizedCount += 1
    }

    func count() -> Int {
        unauthorizedCount
    }
}

private func makeHTTPResponse(statusCode: Int) -> HTTPURLResponse {
    HTTPURLResponse(
        url: URL(string: "https://example.com")!,
        statusCode: statusCode,
        httpVersion: nil,
        headerFields: nil
    )!
}
