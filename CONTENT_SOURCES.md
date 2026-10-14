# Content sources and third-party assets

Everything bundled in Deen that isn't original code, with its licence. The religious texts are
built by the scripts in [`tools/content`](tools/content/README.md); the app shows a summary of this
page under Settings → Sources and licences.

Deen's own code, artwork and texts written for it are © 2026 TheMeshMaker under GPL-3.0-or-later with
the additional terms in [NOTICE](NOTICE) (keep the credit, mark modified versions, no use of the name
or icon: see [TRADEMARKS.md](TRADEMARKS.md)). The Quran text is never changed: Tanzil's licence allows
verbatim copies only.

## Religious texts

| Content | Where | Source | Licence |
|---|---|---|---|
| Quran text (Uthmani, 6,236 verses) | `core/content/src/main/assets/quran/quran-uthmani.txt` | [Tanzil Project](https://tanzil.net), Uthmani text 1.1 with pause marks and sajdah signs | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/). **Bundled verbatim, with its copyright block; changing the text is not allowed.** The reader only shows the first four words of verse 1 (the Bismillah) as a header. |
| Quran metadata (surah names, revelation place, juz, pages) | `core/content/src/main/assets/quran/quran-meta.json` | Tanzil `quran-data.xml`, via `tools/content/build_quran_meta.py` | CC BY 3.0 |
| Tafsir al-Jalalayn (Arabic, every verse) | `core/content/src/main/assets/tafsir/jalalayn.tsv` | [spa5k/tafsir_api](https://github.com/spa5k/tafsir_api) `ar-tafsir-al-jalalayn`, HTML stripped, via `tools/content/build_quran_extras.py` | Classical text (public domain); dataset MIT, see [licenses/tafsir_api-MIT.txt](licenses/tafsir_api-MIT.txt) |
| Tafsir Ibn Kathir, al-Baghawi, al-Qurtubi (Arabic) | **Not bundled.** Downloaded per surah from `cdn.jsdelivr.net/gh/spa5k/tafsir_api` when the user opens it, and cached on the phone | spa5k/tafsir_api | Classical texts (public domain); dataset MIT |
| English meaning of the Quran (shown with the tafsir) | `core/content/src/main/assets/quran/pickthall.tsv` | M. M. Pickthall, *The Meaning of the Glorious Koran* (1930), from [fawazahmed0/quran-api](https://github.com/fawazahmed0/quran-api) `eng-mohammedmarmadu` (originally Tanzil) | Public domain (published 1930; the author died in 1936). Dataset Unlicense |
| Word timings for highlighting the recited word (7 reciters) | `core/content/src/main/assets/audio/timings/<reciter>.tsv` | [cpfair/quran-align](https://github.com/cpfair/quran-align) by Collin Fair, converted by `build_quran_extras.py` | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/), see [licenses/quran-align-CC-BY-4.0.txt](licenses/quran-align-CC-BY-4.0.txt). Changed: reduced to word segments per verse |
| Quran recitations (Alafasy, as-Sudais, ash-Shuraim, ash-Shatri, al-Husary, al-Minshawi, Abdul Basit) | **Not bundled.** Streamed verse by verse from [EveryAyah.com](https://everyayah.com) when the user presses play; kept in a 400 MB cache on the phone | EveryAyah.com | Served by EveryAyah for free listening; nothing is redistributed by Deen |
| Morning and evening adhkar (Arabic, counts, Arabic virtues) | `core/content/src/main/assets/adhkar.json` | [Seen-Arabic/Morning-And-Evening-Adhkar-DB](https://github.com/Seen-Arabic/Morning-And-Evening-Adhkar-DB) `ar.json` (Hisn al-Muslim wording) | MIT, see [licenses/Seen-Arabic-Adhkar-MIT.txt](licenses/Seen-Arabic-Adhkar-MIT.txt). Their English was **not** used. Quranic passages are taken from the Tanzil text instead of their typed copy; one wrong source line was corrected. |
| Other adhkar (after prayer, sleep, waking up, adhan, mosque, wudu, leaving home) | `core/content/src/main/assets/adhkar.json` | Compiled for Deen from the cited hadith; `build_adhkar.py` checks every clause against the Arabic of the cited collection | Classical texts (public domain); compilation GPL-3.0-or-later |
| Reward of each dhikr | `adhkar.json` (`virtue`, or `sunnah` where no reward is stated) | Taken from the cited hadith (Arabic and English written for Deen); where a hadith only reports the Prophet ﷺ saying or teaching it, that is what is shown, never an invented reward | GPL-3.0-or-later |
| An-Nawawi's Forty (Arabic) | `core/content/src/main/assets/hadith/nawawi40.json` | [fawazahmed0/hadith-api](https://github.com/fawazahmed0/hadith-api) `ara-nawawi`, lightly cleaned | Classical text (public domain); dataset Unlicense |
| Full hadith collections, Arabic: al-Bukhari, Muslim, Abu Dawud, at-Tirmidhi, an-Nasa'i, Ibn Majah, Malik's Muwatta, Forty Qudsi (about 36,000 hadith) | `core/content/src/main/assets/hadith/` | hadith-api `ara-*` editions: text, numbering, book ranges and gradings (al-Albani preferred, otherwise the listed scholar) | Classical texts (public domain); dataset [Unlicense](https://unlicense.org) |
| Arabic book (kitab) titles of those collections | `hadith/collections.json` | The compilers' own titles, matched by English name from [AhmedBaset/hadith-json](https://github.com/AhmedBaset/hadith-json) (only the titles are used) | Public domain (classical titles) |
| Facts about the Prophet ﷺ (61, Arabic and English) | `core/content/src/main/assets/prophet-facts.json` | Written for Deen from the cited hadith; `build_facts.py` checks each hadith's key phrase in the cited collection | GPL-3.0-or-later |
| English translations of the adhkar, an-Nawawi's Forty and the facts | the JSON files above | Written for Deen. The English translations that come with the datasets above have unclear copyright and are **not** used. | GPL-3.0-or-later |

**Review:** all religious content needs a qualified reviewer before the 1.0 release (docs/BLUEPRINT.md §15). The
full collections are Arabic only until an English translation with a clear licence is found.

## Adhan recordings

| Voice | Where | Source | Licence |
|---|---|---|---|
| "Deen" (default, 2:34) | `core/alarm/src/main/res/raw/adhan_default.ogg` | ["Beautiful adhan.ogg"](https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg) by Adam-synagda, Wikimedia Commons | CC0 1.0 |
| "Traditional" (3:03) | `core/alarm/src/main/res/raw/adhan_traditional.ogg` | ["Azan.ogg"](https://commons.wikimedia.org/wiki/File:Azan.ogg) by Andrewler, Wikimedia Commons, unmodified | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |
| "Aaqib Azeez" (1:27) | `core/alarm/src/main/res/raw/adhan_aaqib.ogg` | ["The Adhan - Muslim Call to Prayer - Aaqib Azeez.mp3"](https://commons.wikimedia.org/wiki/File:The_Adhan_-_Muslim_Call_to_Prayer_-_Aaqib_Azeez.mp3) by Atcovi, Wikimedia Commons. Changed: loudness normalised (−9 LUFS) and converted to Ogg Vorbis. | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/); the changed file is under the same licence |
| Your own recording | copied into the app's device-protected storage | Picked by the user | The user's own |

For the iPhone app, `tools/audio/build_ios_sounds.py` converts these recordings to AAC (`ios/Deen/Resources/Sounds/adhan_*.m4a`)
and cuts a 30-second alarm clip of each with a short fade-out (`adhan_*_alarm.caf`, the longest sound iOS allows for an
alarm): changed versions under each recording's own licence.

None of the bundled recordings includes the Fajr phrase "as-salātu khayrun mina-n-nawm"; Settings lets the user
pick a separate recording for Fajr.

## Fonts, icons and data

| Asset | Where | Source | Licence |
|---|---|---|---|
| Readex Pro font (variable) | `core/designsystem/src/main/res/font/readex_pro.ttf` | [ThomasJockin/readexpro](https://github.com/ThomasJockin/readexpro) via Google Fonts | SIL OFL 1.1, see [licenses/ReadexPro-OFL.txt](licenses/ReadexPro-OFL.txt) |
| Amiri font (Regular, Bold) | `core/designsystem/src/main/res/font/` | [aliftype/amiri](https://github.com/aliftype/amiri) via Google Fonts | SIL OFL 1.1, see [licenses/Amiri-OFL.txt](licenses/Amiri-OFL.txt) |
| Amiri Quran font | `core/designsystem/src/main/res/font/amiri_quran.ttf` | [aliftype/amiri](https://github.com/aliftype/amiri) release 1.003 | SIL OFL 1.1, see [licenses/Amiri-OFL.txt](licenses/Amiri-OFL.txt) |
| UI icons | `core/designsystem/src/main/res/drawable/deen_ic_*.xml` | [Material Symbols Rounded](https://github.com/google/material-design-icons), except the prayer-beads icon (`deen_ic_adhkar*.xml`), drawn for Deen | Apache-2.0 / GPL-3.0-or-later |
| App icon and mark (the letter nun as a crescent, in an eight-pointed star) | `app/src/main/res/drawable/ic_launcher_*.xml`, `core/alarm/.../ic_stat_deen.xml`, `core/designsystem/.../deen_mark.xml` | Original, geometric construction, generated by `tools/icon/build_icon.py` | GPL-3.0-or-later (with the code) |
| Interface translations (Urdu, Indonesian, Turkish, Bengali, French) | `tools/i18n/<lang>.json`, generated into `values-<lang>/strings.xml` | Written for Deen; awaiting review by native speakers | GPL-3.0-or-later (with the code) |
| City database | `core/location/src/main/assets/cities.tsv` | [GeoNames](https://www.geonames.org/) `cities5000`, `admin1CodesASCII`, `alternateNamesV2` (Arabic names), built by `tools/geonames/build_cities.py` | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Contains data from GeoNames. |
| Liquid Glass and blur (the Liquid style) | dependencies `dev.chrisbanes.haze:haze`, `haze-glass` | [chrisbanes/haze](https://github.com/chrisbanes/haze) | Apache-2.0 |
| Madani page index (khatmah plan) | `core/content/src/main/assets/quran/quran-meta.json` (`pages`) | Tanzil Quran metadata, built by `tools/content/build_quran_meta.py` | CC BY 3.0 |
| Islamic days and night times (calendar, Today) | `core/prayer/.../IslamicEvents.kt`, `NightTimes.kt`; one-line notes in `core/ui` strings | Umm al-Qura calendar (java.time). The notes follow: Quran 2:185 (Ramadan); Ṣaḥīḥ Muslim 1162 (ʿArafah, ʿĀshūrāʾ) and 1134 (the 9th); al-Bukhārī 2017 (odd nights); al-Tirmidhī 761 (white days) and 747 (Mondays, Thursdays); al-Bukhārī 1145 (last third of the night); Muslim 612 (ʿIshāʾ until midnight) | Written for Deen; GPL-3.0-or-later |
| Prayer times and Qibla | dependency `com.batoulapps.adhan:adhan2` | [batoulapps/adhan-kotlin](https://github.com/batoulapps/adhan-kotlin) | MIT |
| Official timetable fixtures (tests only) | `core/prayer/src/test/resources/times/` | Collected by the adhan project from Umm al-Qura, Moonsighting Committee, Diyanet, MUIS and others | MIT (adhan project) |
