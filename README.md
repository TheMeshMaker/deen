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
  countdown and a monthly timetable by Gregorian or Hijri month, the middle and last third of the night, and
  suhoor and iftar in Ramadan
- **Prayer tracker**: tick off each prayer from the Today screen, see the week at a glance, your streak and a
  month calendar; it stays on the phone
- **Islamic calendar**: the coming days of the year (Ramadan, the last ten nights, the two Eids, ʿArafah,
  ʿĀshūrāʾ, the new year) and the white days, with a countdown
- **Adhan alarms** built to survive battery savers, a checklist that works on every Android phone, three adhan
  voices or your own recording (also a separate one for Fajr), reminders before the prayer and an optional
  "have you prayed?" reminder 5–20 minutes after each adhan (set per prayer)
- **Quran** (Tanzil Uthmani text): every surah, juz index, bookmarks, and it opens where you stopped; tafsir for
  every verse; a recitation player (seven reciters) that lights up the word being recited; a **khatmah plan**
  that splits the 604 pages into daily portions and follows your progress; an optional Friday reminder to read
  Al-Kahf
- **Hadith**: a hadith of the day from an-Nawawi's Forty, a daily fact about the Prophet ﷺ, and the six books,
  the Muwatta and the Forty Qudsi in Arabic with gradings and search
- **Adhkar** for morning, evening, after prayer, sleep, waking up, the adhan, the mosque, wudu and leaving
  home, with tap-to-count and the reward of each from the hadith; a **tasbih** with vibration
- **Qibla** compass, Hijri date, home-screen widget
- **Your look**: six colour themes plus your own two-colour gradient, five styles (Classic, Sleek, Glass,
  Liquid Glass like iOS 26, and Heritage), light / dark / black
- **Languages**: Arabic and English, plus Urdu, Indonesian, Turkish, Bengali and French (translations awaiting
  review by native speakers; corrections welcome in [`tools/i18n`](tools/i18n/README.md))

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

Copyright © 2026 TheMeshMaker. Deen is developed by one person.

Code: [GPL-3.0-or-later](LICENSE), **with additional terms** (allowed by section 7 of the licence) in
[NOTICE](NOTICE). Fonts, icons and religious texts keep their own licenses, listed in
[CONTENT_SOURCES.md](CONTENT_SOURCES.md).

### Please don't republish Deen

I don't want copies of this app published, under any name. If you'd like to improve Deen, please open
an issue or a pull request here instead, so everyone gets the one app whose religious texts have been
checked. (This is a request: the licence lets people copy the code. The rules below are binding.)

### Rules for any copy or modified version

Anyone who copies or changes Deen must follow the GPL and the terms in [NOTICE](NOTICE). In short:

- **No name or icon.** A copy may not be called Deen or دِين or use Deen's icon, and may not suggest
  it is the original or made by me. See [TRADEMARKS.md](TRADEMARKS.md).
- **Mark it as changed.** A modified version must say clearly, in its store listing and in the app,
  that it is a modified version of Deen.
- **Keep the credit.** The copyright notice, this NOTICE and the app's licence screen (Settings →
  Sources and licences) must stay.
- **Keep it open.** Anyone who publishes a copy must share its full source code under the same licence.
- **The Quran stays as it is.** The Quran text may only be copied exactly as published by the Tanzil
  Project; changing it is not allowed. Altered religious texts may never be presented as Deen's.

A copy that breaks these rules loses its licence (GPL section 8), and I will have it taken down. The
official Deen is published only from this repository. To report a copy, open an issue.
