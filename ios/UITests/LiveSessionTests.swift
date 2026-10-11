import XCTest

/// A live, hands-on session. ios/live/server.py streams the Simulator's screen to a browser and queues every
/// tap, swipe and key press made there; this test fetches them one by one and carries them out. Started by
/// .github/workflows/ios-live.yml (which sets DEEN_LIVE_PORT); skipped in every other test run.
final class LiveSessionTests: XCTestCase {
    private let app = XCUIApplication()
    private let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")

    func testLiveSession() throws {
        guard let port = ProcessInfo.processInfo.environment["DEEN_LIVE_PORT"] else {
            throw XCTSkip("Only runs in a live session")
        }
        continueAfterFailure = true
        let host = LiveHost(port: port)
        app.launch()
        let screen = springboard.frame.size
        _ = host.send("hello", ["width": screen.width, "height": screen.height])

        var misses = 0
        while misses < 120 {
            switch host.next() {
            case .none:
                misses += 1
                Thread.sleep(forTimeInterval: 0.5)
            case .idle:
                misses = 0
            case .command(let command):
                misses = 0
                let kind = command["type"] as? String ?? ""
                if kind == "stop" {
                    _ = host.send("done", ["id": command["id"] ?? 0, "ok": true])
                    return
                }
                let error = perform(kind, command)
                _ = host.send("done", ["id": command["id"] ?? 0, "ok": error == nil, "error": error ?? ""])
            }
        }
    }

    /// Carries out one command; returns why it couldn't, if it couldn't.
    private func perform(_ kind: String, _ c: [String: Any]) -> String? {
        func number(_ key: String, _ fallback: Double = 0) -> Double {
            (c[key] as? NSNumber)?.doubleValue ?? fallback
        }
        // Coordinates are fractions of the screen, so they work whatever the device and whatever is in front.
        func point(_ x: String, _ y: String) -> XCUICoordinate {
            springboard.coordinate(withNormalizedOffset: CGVector(dx: number(x), dy: number(y)))
        }
        switch kind {
        case "tap":
            point("x", "y").tap()
        case "longpress":
            point("x", "y").press(forDuration: min(max(number("seconds", 1), 0.5), 5))
        case "swipe":
            let velocity = XCUIGestureVelocity(rawValue: CGFloat(min(max(number("velocity", 1500), 100), 8000)))
            point("x1", "y1").press(
                forDuration: 0.05, thenDragTo: point("x2", "y2"), withVelocity: velocity,
                thenHoldForDuration: min(max(number("hold"), 0), 2)
            )
        case "type":
            let text = c["text"] as? String ?? ""
            let target = app.state == .runningForeground ? app : springboard
            guard target.keyboards.firstMatch.exists else { return "No text field is selected" }
            target.typeText(text)
        case "home":
            XCUIDevice.shared.press(.home)
        case "open":
            app.activate()
        case "terminate":
            app.terminate()
        case "launch":
            app.terminate()
            let language = c["language"] as? String ?? ""
            app.launchArguments = language.isEmpty ? [] : ["-AppleLanguages", "(\(language))", "-AppleLocale", language]
            app.launch()
        default:
            return "Unknown command \(kind)"
        }
        return nil
    }
}

/// The session server, reached from the Simulator on the Mac's own loopback address.
private struct LiveHost {
    enum Poll {
        case none
        case idle
        case command([String: Any])
    }

    let base: URL

    init(port: String) {
        base = URL(string: "http://127.0.0.1:\(port)")!
    }

    /// Waits (up to a second, on the server) for the next command.
    func next() -> Poll {
        guard let (status, data) = request("next", body: nil, timeout: 10) else { return .none }
        guard status == 200 else { return status == 204 ? .idle : .none }
        guard let command = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else { return .idle }
        return .command(command)
    }

    func send(_ path: String, _ body: [String: Any]) -> Bool {
        request(path, body: body, timeout: 10)?.0 == 200
    }

    private func request(_ path: String, body: [String: Any]?, timeout: TimeInterval) -> (Int, Data)? {
        var request = URLRequest(url: base.appendingPathComponent(path), timeoutInterval: timeout)
        if let body {
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try? JSONSerialization.data(withJSONObject: body)
        }
        let result = Box()
        let done = DispatchSemaphore(value: 0)
        URLSession.shared.dataTask(with: request) { data, response, _ in
            if let http = response as? HTTPURLResponse { result.value = (http.statusCode, data ?? Data()) }
            done.signal()
        }.resume()
        done.wait()
        return result.value
    }

    private final class Box: @unchecked Sendable {
        var value: (Int, Data)?
    }
}
