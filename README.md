# Deen · دِين

A free, open-source Islamic app for Android: accurate offline prayer times with an adhan
that actually plays, daily adhkar with a tasbih counter, and a daily hadith with its source.

**No ads. No tracking. No paywalls. Ever.**

تطبيق إسلامي مجاني ومفتوح المصدر لأندرويد: مواقيت صلاة دقيقة بدون إنترنت مع أذان يعمل فعلًا،
وأذكار يومية مع عدّاد تسبيح، وحديث يومي مع مصدره. بلا إعلانات، بلا تتبّع، بلا اشتراكات.

> **Status:** early development (Milestone 0: project foundation). Not usable yet.
> The full plan is in [docs/BLUEPRINT.md](docs/BLUEPRINT.md).

## Planned for v1

- Prayer times calculated on the device (Umm al-Qura, MWL, ISNA, Egypt, Karachi and more)
- Adhan alarms built to survive battery savers on Xiaomi, OPPO, vivo, Samsung, Huawei and others
- Morning, evening, before-sleep and after-prayer adhkar, with a tasbih counter
- Daily hadith with Arabic text, translation, reference and grading
- Qibla compass, Hijri date, home-screen widget
- Arabic and English, Light / Dark / OLED Black themes, Material You

## Builds

| Flavor | For | Contains |
|---|---|---|
| `foss` | F-Droid, GitHub Releases | Only free software. No internet permission. |
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
