// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "BirdDexIOSNetworkTests",
    platforms: [
        .macOS(.v13),
        .iOS(.v17)
    ],
    products: [
        .library(name: "BirdDexNetwork", targets: ["BirdDexNetwork"])
    ],
    dependencies: [
        .package(url: "https://github.com/apple/swift-testing.git", from: "0.6.0")
    ],
    targets: [
        .target(
            name: "BirdDexNetwork",
            path: "BirdDexApp/Core/Network",
            sources: [
                "APIClient.swift",
                "Endpoints.swift",
                "SessionManager.swift"
            ]
        ),
        .testTarget(
            name: "BirdDexNetworkTests",
            dependencies: [
                "BirdDexNetwork",
                .product(name: "Testing", package: "swift-testing")
            ],
            path: "Tests/BirdDexNetworkTests"
        )
    ]
)
