# Translations

English (`values/strings.xml`) and Arabic (`values-ar/strings.xml`) are written by hand in each module. The other
interface languages live here, one file per language, so a translator only edits one file:

| File | Language | Resource folder |
|---|---|---|
| `ur.json` | Urdu · اردو | `values-ur` |
| `id.json` | Indonesian · Bahasa Indonesia | `values-in` |
| `tr.json` | Turkish · Türkçe | `values-tr` |
| `bn.json` | Bengali · বাংলা | `values-bn` |
| `fr.json` | French · Français | `values-fr` |

These translations were made for Deen and **still need review by native speakers**. Corrections are very
welcome: edit the JSON file and open a pull request. Religious content (Quran, hadith, adhkar) is not part of
this; it stays in Arabic with English.

## Workflow

Run from the repository root:

```bash
python -I tools/i18n/export_strings.py   # after changing English strings: refresh source.json
python -I tools/i18n/build_strings.py    # check every language and write values-<lang>/strings.xml
```

`build_strings.py` stops if a key is missing or a placeholder (`%1$s`, `%d`…) differs from the English, and fills
in the plural forms each language needs (French "many" reads like "other"; Indonesian has one form).
The generated XML files say so at the top: edit the JSON, not the XML.

To add a language: add its file here, its entry in `LANGUAGES` (and `PLURALS` if needed) in `build_strings.py`,
and its entry in `AppLanguage` (core/model). Android 13's per-app language list is generated from the resource
folders by the build.
