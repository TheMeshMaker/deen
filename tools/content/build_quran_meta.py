"""Builds core/content/src/main/assets/quran-meta.json from Tanzil's quran-data.xml (CC BY 3.0), and checks the text's
Bismillah convention the reader relies on.

Usage: python -I build_quran_meta.py <quran-data.xml> <quran-uthmani.txt> <out json>
"""
import json
import sys
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding="utf-8")
XML, TXT, OUT = sys.argv[1:4]

root = ET.parse(XML).getroot()
surahs = [
    {
        "number": int(s.get("index")),
        "ayahs": int(s.get("ayas")),
        "name": s.get("name"),
        "transliteration": s.get("tname"),
        "translation": s.get("ename"),
        "revelation": "MECCAN" if s.get("type") == "Meccan" else "MEDINAN",
        "order": int(s.get("order")),
    }
    for s in root.find("suras")
]
juz = [{"juz": int(j.get("index")), "surah": int(j.get("sura")), "ayah": int(j.get("aya"))} for j in root.find("juzs")]
sajdas = [{"surah": int(s.get("sura")), "ayah": int(s.get("aya")), "type": s.get("type")} for s in root.find("sajdas")]
pages = [[int(p.get("sura")), int(p.get("aya"))] for p in root.find("pages")]

assert len(surahs) == 114 and sum(s["ayahs"] for s in surahs) == 6236 and len(juz) == 30 and len(pages) == 604

# The reader shows the Bismillah as a header (the first four words): Tanzil prefixes it to verse 1 of every surah except 1 and 9.
verses = {}
for line in open(TXT, encoding="utf-8"):
    if line.strip() and not line.startswith("#"):
        s, a, t = line.rstrip("\n").split("|", 2)
        verses[(int(s), int(a))] = t
bismillah = verses[(1, 1)]
for n in range(2, 115):
    first = verses[(n, 1)]
    if n == 9:
        assert not first.startswith(bismillah), "9:1"
    else:
        # The first four words; 95:1 and 97:1 write the first word with an extra shadda (بِّسْمِ).
        words = first.split(" ")
        assert len(words) > 4 and [w.replace("ّ", "") for w in words[:4]] == [w.replace("ّ", "") for w in bismillah.split(" ")], f"{n}:1"
print("bismillah convention ok:", bismillah)

doc = {
    "source": "Tanzil Quran metadata (tanzil.net), CC BY 3.0",
    "surahs": surahs,
    "juz": juz,
    "sajdas": sajdas,
    "pages": pages,
}
json.dump(doc, open(OUT, "w", encoding="utf-8", newline="\n"), ensure_ascii=False, separators=(",", ":"))
print("surahs", len(surahs), "juz", len(juz), "sajdas", len(sajdas), "pages", len(pages))
