# Content sources and third-party assets

Everything bundled in Deen that isn't original code, with its license.

| Asset | Where | Source | License |
|---|---|---|---|
| Readex Pro font (variable) | `core/designsystem/src/main/res/font/readex_pro.ttf` | [ThomasJockin/readexpro](https://github.com/ThomasJockin/readexpro) via Google Fonts | SIL OFL 1.1, see [licenses/ReadexPro-OFL.txt](licenses/ReadexPro-OFL.txt) |
| Amiri font (Regular, Bold) | `core/designsystem/src/main/res/font/` | [aliftype/amiri](https://github.com/aliftype/amiri) via Google Fonts | SIL OFL 1.1, see [licenses/Amiri-OFL.txt](licenses/Amiri-OFL.txt) |
| UI icons | `core/designsystem/src/main/res/drawable/deen_ic_*.xml` | [Material Symbols Rounded](https://github.com/google/material-design-icons) | Apache-2.0 |
| App icon (Rub el Hizb star) | `app/src/main/res/drawable/ic_launcher_*.xml` | Original, geometric construction | GPL-3.0-or-later (with the code) |
| City database | `core/location/src/main/assets/cities.tsv` | [GeoNames](https://www.geonames.org/) `cities5000`, `admin1CodesASCII`, `alternateNamesV2` (Arabic names), built by `tools/geonames/build_cities.py` | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Contains data from GeoNames. |
| Prayer-time calculation | dependency `com.batoulapps.adhan:adhan2` | [batoulapps/adhan-kotlin](https://github.com/batoulapps/adhan-kotlin) | MIT |
| Official timetable fixtures (tests only) | `core/prayer/src/test/resources/times/` | Collected by the adhan project from Umm al-Qura, Moonsighting Committee, Diyanet, MUIS and others | MIT (adhan project) |

## Planned (not bundled yet)

| Content | Candidate source | Notes |
|---|---|---|
| Quran verses in adhkar | [Tanzil](https://tanzil.net/) Uthmani text | CC BY 3.0, text must not be modified |
| Adhkar and hadith | To be decided with the reviewer | Every item needs a reference and grading |
| Adhan recordings | To be decided | Only recordings we have the rights to |
