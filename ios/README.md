# Deen for iOS

The iPhone version of Deen, written in SwiftUI for **iOS 26 and newer** so it uses Liquid Glass throughout
(tab bar, toolbars, buttons and cards). It shares everything it can with the Android app:

- **Content**: the Quran, hadith, adhkar, tafsir and city files are read straight from the Android modules
  (`core/content`, `core/location`); nothing is copied.
- **Prayer times**: `DeenKit` uses [adhan-swift](https://github.com/batoulapps/adhan-swift), the twin of the
  library Android uses, with the same settings, and is tested against the same official timetables.
- **Translations**: `Deen/Resources/Localizable.xcstrings` is generated from the Android strings by
  `tools/i18n/build_ios_strings.py` (Arabic, English, Urdu, Indonesian, Turkish, Bengali, French).

## Status

| Stage | What | State |
|---|---|---|
| 1 | Project, Liquid Glass design, themes (incl. your own colours), languages, welcome, location, method, Today | ✅ |
| 2 | Adhan with AlarmKit (and a "Full adhan" button), reminders before and after the adhan, Quran reader | ✅ |
| 3 | Adhkar with rewards, tasbih, hadith (daily hadith and fact, an-Nawawi's Forty, the full collections) | ✅ |
| 4 | Tafsir, recitation player with the recited word in green, Qibla, monthly timetable, home-screen widget | ✅ |

## Requirements

| Need | For | Notes |
|---|---|---|
| A Mac with **Xcode 26 or newer** | Building | Apple only builds iOS apps on macOS. Without a Mac, GitHub builds it (below). |
| **Apple Developer Program** ($99/year) | TestFlight and the App Store | A free Apple ID can install on your own iPhone from Xcode, for 7 days at a time. |
| An iPhone on **iOS 26+** | Trying it on a phone | The Simulator works too. |
| From April 2027 | App Store uploads | Must be built with the iOS 27 SDK (Xcode 27). |

**The adhan on iPhone.** iOS does not let apps run an alarm service the way Android does. Deen uses
AlarmKit (iOS 26), which rings through silent mode and Focus with a full-screen Stop button, but alarm
sounds are limited to about 30 seconds: the alarm plays the adhan's first 30 seconds, and its "Full adhan"
button opens Deen and plays the whole recording. Six days of alarms are scheduled at a time and refreshed
whenever the app runs and in the background.

**Donations.** The App Store allows tips to the developer only through in-app purchase, so the iOS app has
no donate button (sharing, starring and reporting problems stay).

## Trying it without a Mac

- **Use it live, from Windows or Linux:** `python ios/live/start.py` starts the iOS Simulator with the latest
  `ios` branch on GitHub's Mac (`.github/workflows/ios-live.yml`) and opens it in your browser: click to tap,
  drag or scroll to swipe, hold for a long press, type into text fields ([AXe](https://github.com/cameroncooke/AXe)
  streams the screen and carries the input; text in other alphabets goes to the phone's clipboard to paste). It takes about ten minutes to start and
  lasts an hour (`--minutes`, up to 120). The link is sent back encrypted for your computer only (a key pair
  kept in `~/.deen-live`). Sound plays on the Mac, so it isn't heard. Needs git, a signed-in GitHub CLI and
  openssl (Git for Windows includes it).
- **Watch it:** every build records a walkthrough video of the real app in the Simulator (a UI test taps
  through every screen): the `ios-walkthrough-video` artifact of the iOS workflow run.
- **Use it in a browser elsewhere:** upload the `ios-simulator-app` artifact (a zipped Simulator build) to
  [Appetize.io](https://appetize.io) with your own account.
- **On an iPhone:** needs a Mac with Xcode (free Apple ID, 7 days at a time) or the Apple Developer Program.
  [xtool](https://github.com/xtool-org/xtool) can build and install from Windows (WSL) or Linux instead of a
  Mac, but still needs the iPhone plugged in; there is no emulator that runs current iOS apps on Windows.

## Building without a Mac

Every push to the `ios` branch runs `.github/workflows/ios.yml` on GitHub's Mac runners (free for public
repositories): it runs the DeenKit tests, builds the app for the Simulator and saves screenshots of the main
screens as a downloadable artifact (`ios-screenshots`).

## Building on a Mac

```bash
brew install xcodegen
cd ios
xcodegen generate
open Deen.xcodeproj
```

Choose your team under Signing & Capabilities, then run on a Simulator or your iPhone. `swift test
--package-path ios/DeenKit` runs the core tests.

The app icon is `Deen/Resources/AppIcon.svg` (written by `tools/icon/build_icon.py`), exported to
`Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png` as a 1024×1024 PNG without transparency.
