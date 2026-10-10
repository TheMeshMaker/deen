# Content build scripts

These scripts produce the files in `core/content/src/main/assets/`. Run them with Python 3.11+ (`python -I`, no
third-party packages). Keep the downloaded inputs in their own folder outside the repo.

| Output | Script | Inputs |
|---|---|---|
| `quran/quran-uthmani.txt` | none (copied verbatim) | Tanzil Uthmani text 1.1, from tanzil.net/download with "pause marks", "sajdah signs" and "tatweel" on, text format `sura\|aya\|text`. **Never edit this file**: the Tanzil licence forbids changes. |
| `quran/quran-meta.json` | `build_quran_meta.py` | Tanzil `quran-data.xml` (tanzil.net/res/text/metadata), plus the text above (to check the Bismillah convention the reader relies on) |
| `adhkar.json` | `build_adhkar.py` | `ar.json` from [Seen-Arabic/Morning-And-Evening-Adhkar-DB](https://github.com/Seen-Arabic/Morning-And-Evening-Adhkar-DB) (MIT); the Tanzil text; the hadith-api Arabic collections (for checking our Arabic against the sources) |
| `hadith/nawawi40.json` | `build_hadith.py` | `ara-nawawi.json` from [fawazahmed0/hadith-api](https://github.com/fawazahmed0/hadith-api) (Unlicense) |
| `hadith/collections.json`, `hadith/<id>/<book>.tsv` | `build_collections.py` | hadith-api `ara-{bukhari,muslim,abudawud,tirmidhi,nasai,ibnmajah,malik,qudsi}.min.json`; Arabic book titles only from [AhmedBaset/hadith-json](https://github.com/AhmedBaset/hadith-json) |
| `prophet-facts.json` | `build_facts.py` | hadith-api Arabic collections (each fact's hadith is checked) |

The English in `adhkar.json`, `hadith/nawawi40.json` and `prophet-facts.json` is written for Deen and lives in the
scripts. Edit the script, re-run it, and commit both. Content changes need a reviewer before a release
(docs/BLUEPRINT.md §15). Licences and attributions are listed in `CONTENT_SOURCES.md`.

```bash
python -I build_adhkar.py <dl>/ar.json <dl>/quran-uthmani.txt <dl>/hadith ../../core/content/src/main/assets/adhkar.json
python -I build_hadith.py <dl>/hadith/ara-nawawi.json ../../core/content/src/main/assets/hadith/nawawi40.json
python -I build_facts.py <dl>/hadith ../../core/content/src/main/assets/prophet-facts.json
python -I build_quran_meta.py <dl>/quran-data.xml <dl>/quran-uthmani.txt ../../core/content/src/main/assets/quran/quran-meta.json
python -I build_collections.py <dl>/hadith <dl>/hadith-json ../../core/content/src/main/assets/hadith
```
