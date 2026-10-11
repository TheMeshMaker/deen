"""Collects every translatable English string of the app into tools/i18n/source.json.

Run from the repository root after adding or changing English strings:
    python -I tools/i18n/export_strings.py
Then add the new keys to each tools/i18n/<lang>.json and run build_strings.py.
"""
import glob
import json
import os
import sys
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding="utf-8")
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def text_of(element):
    # Android escapes: \' \" \n stay as written; unescape for translators.
    t = element.text or ""
    return t.replace("\\'", "'").replace('\\"', '"').replace("\\n", "\n").replace("\\@", "@").replace("\\?", "?")


def modules():
    for path in sorted(glob.glob(os.path.join(ROOT, "**", "src", "main", "res", "values", "strings.xml"), recursive=True)):
        rel = os.path.relpath(path, ROOT).replace("\\", "/")
        if "/build/" in rel:
            continue
        yield rel.split("/src/main/")[0], path


def export():
    out = {}
    for module, path in modules():
        entries = {}
        for e in ET.parse(path).getroot():
            if e.get("translatable") == "false":
                continue
            name = e.get("name")
            if e.tag == "string":
                entries[name] = text_of(e)
            elif e.tag == "plurals":
                entries[name] = {i.get("quantity"): text_of(i) for i in e}
            elif e.tag == "string-array":
                entries[name] = [text_of(i) for i in e]
        out[module] = entries
    return out


if __name__ == "__main__":
    data = export()
    with open(os.path.join(ROOT, "tools", "i18n", "source.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print({m: len(v) for m, v in data.items()}, "total", sum(len(v) for v in data.values()))
