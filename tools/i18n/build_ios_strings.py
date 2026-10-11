"""Builds ios/Deen/Resources/Localizable.xcstrings from the Android strings, so both apps share one set of
translations. Keys stay the Android names (e.g. "core_ui_prayer_fajr").

Run from the repository root after build_strings.py:
    python -I tools/i18n/build_ios_strings.py

Placeholders become iOS ones (%1$s → %1$@, %d → %lld). Plurals with exactly one number become plural
variations; plurals with more than one placeholder are written as their "other" form under the key
"<name>.other" (the iOS app does not use them yet).
"""
import glob
import json
import os
import re
import sys
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding="utf-8")
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "ios", "Deen", "Resources", "Localizable.xcstrings")

# Android resource folder → iOS language code
LANGUAGES = {"values": "en", "values-ar": "ar", "values-ur": "ur", "values-in": "id", "values-tr": "tr",
             "values-bn": "bn", "values-fr": "fr"}
PLACEHOLDER = re.compile(r"%(\d+\$)?([sd])")


def unescape(text):
    text = text or ""
    return (text.replace("\\'", "'").replace('\\"', '"').replace("\\n", "\n")
            .replace("\\@", "@").replace("\\?", "?").replace("\\\\", "\\"))


def ios(text):
    return PLACEHOLDER.sub(lambda m: "%" + (m.group(1) or "") + ("@" if m.group(2) == "s" else "lld"), text)


def unit(value):
    return {"stringUnit": {"state": "translated", "value": value}}


def main():
    strings = {}
    for folder, lang in LANGUAGES.items():
        for path in sorted(glob.glob(os.path.join(ROOT, "**", "src", "main", "res", folder, "strings.xml"), recursive=True)):
            if "build" + os.sep in path:
                continue
            for e in ET.parse(path).getroot():
                if e.get("translatable") == "false":
                    continue
                name = e.get("name")
                if e.tag == "string":
                    strings.setdefault(name, {})[lang] = unit(ios(unescape(e.text)))
                elif e.tag == "plurals":
                    raw = {i.get("quantity"): unescape(i.text) for i in e}
                    numbers = set(PLACEHOLDER.findall(raw.get("other", "")))
                    if len(numbers) <= 1:
                        # One number: iOS picks the form, so that argument is always an integer (%lld),
                        # even where Android passes it as a formatted string.
                        as_int = {q: PLACEHOLDER.sub(lambda m: "%" + (m.group(1) or "") + "lld", t) for q, t in raw.items()}
                        variations = {q: unit(t) for q, t in as_int.items()}
                        strings.setdefault(name, {})[lang] = {"variations": {"plural": variations}}
                    else:
                        strings.setdefault(name + ".other", {})[lang] = unit(ios(raw.get("other", "")))
                elif e.tag == "string-array":
                    for i, item in enumerate(e):
                        strings.setdefault(f"{name}.{i}", {})[lang] = unit(ios(unescape(item.text)))
    catalog = {
        "sourceLanguage": "en",
        "strings": {key: {"extractionState": "manual", "localizations": strings[key]} for key in sorted(strings)},
        "version": "1.0",
    }
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=2, sort_keys=False)
        f.write("\n")
    missing = {lang: sum(1 for v in strings.values() if lang not in v) for lang in LANGUAGES.values()}
    print(f"ok: {len(strings)} keys; missing per language: {missing}")


if __name__ == "__main__":
    main()
