# Deen · دِين

A free, open-source Islamic app for Android: accurate offline prayer times with an adhan
that actually plays, the Quran, the major hadith collections, daily adhkar with a tasbih counter,
and a Qibla compass.

**No ads. No tracking. No paywalls. Ever.**

تطبيق إسلامي مجاني ومفتوح المصدر لأندرويد: مواقيت صلاة دقيقة بدون إنترنت مع أذان يعمل فعلًا،
والقرآن الكريم، وكتب الحديث، والأذكار مع السبحة، واتجاه القبلة. بلا إعلانات، بلا تتبّع، بلا اشتراكات.

> **Status:** in development, milestones 0–4 done. The full plan is in [docs/BLUEPRINT.md](docs/BLUEPRINT.md).
> Religious content is awaiting review by a qualified person before version 1.0.

## Features

- **Prayer times** calculated on the phone (Umm al-Qura, MWL, ISNA, Egypt, Karachi and more), with a live
  countdown and a monthly timetable by Gregorian or Hijri month
- **Adhan alarms** built to survive battery savers, a checklist that works on every Android phone, three adhan
  voices or your own recording (also a separate one for Fajr), reminders before the prayer
- **Quran** (Tanzil Uthmani text): every surah, juz index, bookmarks, and it opens where you stopped
- **Hadith**: a hadith of the day from an-Nawawi's Forty, a daily fact about the Prophet ﷺ, and the six books,
  the Muwatta and the Forty Qudsi in Arabic with gradings and search
- **Adhkar** for morning, evening, after prayer, sleep, waking up, the adhan, the mosque, wudu and leaving
  home, with tap-to-count; a **tasbih** with vibration
- **Qibla** compass, Hijri date, home-screen widget
- **Your look**: three free colour themes, two styles (Classic and Sleek), light / dark / black, Arabic or English

## Builds

| Flavor | For | Contains |
|---|---|---|
| `foss` | F-Droid, GitHub Releases | Only free software. Internet is used only for Quran recitations and the longer tafsirs. |
| `play` | Google Play | Adds Google location services and Play Billing (for optional tips) |

## Building

Requirements: Android Studio 2026.1.3 or newer (AGP 9.4), JDK 17+.

```bash
./gradlew :app:assembleFossDebug
```

Before sending a pull request:

```bash
./gradlew spotlessApply test :app:lintFossDebug
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Religious content (adhkar, hadith, translations) goes through review
by a qualified person before it ships.

## License

Code: [GPL-3.0-or-later](LICENSE). Fonts, icons and religious texts keep their own licenses, listed in
[CONTENT_SOURCES.md](CONTENT_SOURCES.md).

Copyright © 2026 TheMeshMaker
