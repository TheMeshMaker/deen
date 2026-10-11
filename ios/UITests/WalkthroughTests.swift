import XCTest

/// Drives the real app through every screen, slowly enough to watch. The workflow records the
/// Simulator while this runs (ios/walkthrough.mp4), so the app can be seen working without a Mac.
final class WalkthroughTests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = true
        app.launchArguments = ["-DeenDemo", "today", "-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
    }

    func testWalkthrough() {
        pause(4)
        // Today: the countdown, the times, then the quick actions.
        app.swipeUp()
        pause(2)
        open("timetableButton")
        back()
        open("qiblaButton")
        back()
        app.swipeDown()
        pause(1)

        // Quran: continue reading, player mode with the green word, a verse's tafsir.
        tab("Quran")
        open("continueReading", wait: 4)
        if element("readerMenu").waitForExistence(timeout: 5) {
            element("readerMenu").tap()
            pause(1)
            app.switches.firstMatch.tapIfHittable()
            app.buttons["Player mode"].tapIfHittable()
            pause(8)
        }
        app.swipeUp()
        pause(2)
        let verse = element("verse-12")
        if verse.waitForExistence(timeout: 4) {
            verse.press(forDuration: 1.2)
            pause(1)
            app.buttons["Tafsir"].tapIfHittable()
            pause(4)
            app.swipeDown(velocity: .fast)
            pause(1)
        }
        back()

        // Hadith: an-Nawawi's Forty.
        tab("Hadith")
        pause(2)
        open("nawawiLink")
        app.cells.element(boundBy: 0).tapIfHittable()
        pause(3)
        back()
        back()

        // Adhkar: count a dhikr, then the tasbih.
        tab("Adhkar")
        pause(2)
        let category = app.descendants(matching: .any).matching(NSPredicate(format: "identifier BEGINSWITH 'category-'")).firstMatch
        category.tapIfHittable()
        pause(2)
        for _ in 0..<3 {
            element("dhikr-1").tapIfHittable()
            pause(0.6)
        }
        pause(2)
        back()
        open("tasbihCard")
        for _ in 0..<7 {
            element("tasbihButton").tapIfHittable()
            pause(0.35)
        }
        pause(2)
        back()

        // More → Settings: switch to Taif Rose, look through the adhan options.
        tab("More")
        open("settingsLink")
        element("theme-rose").tapIfHittable()
        pause(2)
        app.swipeUp()
        pause(2)
        app.swipeUp()
        pause(2)
        back()
        tab("Today")
        pause(5)
    }

    // MARK: Helpers

    private func element(_ id: String) -> XCUIElement {
        app.descendants(matching: .any).matching(identifier: id).firstMatch
    }

    private func open(_ id: String, wait: TimeInterval = 3) {
        let e = element(id)
        if e.waitForExistence(timeout: 6) {
            e.tapIfHittable()
            pause(wait)
        }
    }

    private func tab(_ title: String) {
        let button = app.tabBars.buttons[title]
        if button.waitForExistence(timeout: 5) { button.tap() } else { app.buttons[title].tapIfHittable() }
        pause(2)
    }

    private func back() {
        let bar = app.navigationBars.firstMatch
        if bar.waitForExistence(timeout: 3), bar.buttons.firstMatch.exists { bar.buttons.firstMatch.tap() }
        pause(1.5)
    }

    private func pause(_ seconds: TimeInterval) {
        Thread.sleep(forTimeInterval: seconds)
    }
}

private extension XCUIElement {
    func tapIfHittable() {
        if waitForExistence(timeout: 3), isHittable { tap() }
    }
}
