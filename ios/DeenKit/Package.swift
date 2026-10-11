// swift-tools-version: 6.2

import PackageDescription

// Deen's platform-independent core for iOS: prayer times, Hijri dates, Qibla and the city search.
// It mirrors the Android modules core:prayer, core:location and core:model, and is unit-tested on
// macOS (`swift test`) against the same official timetables as Android.
let package = Package(
    name: "DeenKit",
    platforms: [.iOS(.v26), .macOS(.v15)],
    products: [
        .library(name: "DeenKit", targets: ["DeenKit"])
    ],
    dependencies: [
        // The Swift twin of adhan-kotlin, which the Android app uses (MIT).
        .package(url: "https://github.com/batoulapps/adhan-swift", exact: "1.5.0")
    ],
    targets: [
        .target(
            name: "DeenKit",
            dependencies: [.product(name: "Adhan", package: "adhan-swift")]
        ),
        .testTarget(
            name: "DeenKitTests",
            dependencies: ["DeenKit"]
        )
    ],
    swiftLanguageModes: [.v5]
)
