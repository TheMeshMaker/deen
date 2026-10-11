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
| 2 | Adhan with AlarmKit, reminders (before, after, masjid), Quran reader | next |
| 3 | Adhkar with rewards, tasbih, hadith | |
| 4 | Tafsir and the recitation player, Qibla, home-screen widget, monthly timetable | |

## Requirements

| Need | For | Notes |
|---|---|---|
| A Mac with **Xcode 26 or newer** | Building | Apple only builds iOS apps on macOS. Without a Mac, GitHub builds it (below). |
| **Apple Developer Program** ($99/year) | TestFlight and the App Store | A free Apple ID can install on your own iPhone from Xcode, for 7 days at a time. |
| An iPhone on **iOS 26+** | Trying it on a phone | The Simulator works too. |
| From April 2027 | App Store uploads | Must be built with the iOS 27 SDK (Xcode 27). |

**The adhan on iPhone.** iOS does not let apps run an alarm service the way Android does. Deen will use
AlarmKit (iOS 26), which rings through silent mode with a full-screen Stop button, but alarm sounds are
limited to about 30 seconds, so the full adhan plays when the alarm is opened.

**Donations.** The App Store allows tips to the developer only through in-app purchase, so the iOS app has
no donate button (sharing, starring and reporting problems stay).

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
