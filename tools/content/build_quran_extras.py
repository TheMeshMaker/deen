"""Builds the Quran extras in core/content/src/main/assets:

  tafsir/jalalayn.tsv     Tafsir al-Jalalayn (Arabic, classical, public domain): surah, ayah, text
  quran/pickthall.tsv     Pickthall's English meaning (1930, public domain): surah, ayah, text
  audio/timings/<id>.tsv  Word timings per reciter (quran-align, CC BY 4.0): surah, ayah, segments

Inputs (download first, see tools/content/README.md):
  <tafsir dir>   spa5k/tafsir_api `tafsir/ar-tafsir-al-jalalayn/<surah>.json` for surahs 1..114
  <pickthall>    fawazahmed0/quran-api `editions/eng_mohammedmarmadu.min.json`
  <align dir>    quran-align release data (one JSON per reciter)

Usage: python -I build_quran_extras.py <tafsir dir> <pickthall json> <align dir> <assets dir>
"""
import json
import os
import re
import sys

sys.stdout.reconfigure(encoding="utf-8")
TAFSIR_DIR, PICKTHALL, ALIGN_DIR, ASSETS = sys.argv[1:5]

# Reciters with word timings; ids are EveryAyah folder names (the app streams from everyayah.com/data/<id>/).
RECITERS = [
    "Alafasy_128kbps",
    "Abdurrahmaan_As-Sudais_192kbps",
    "Saood_ash-Shuraym_128kbps",
    "Abu_Bakr_Ash-Shaatree_128kbps",
    "Husary_64kbps",
    "Minshawy_Murattal_128kbps",
    "Abdul_Basit_Murattal_64kbps",
]
AYAH_COUNTS = None


def clean(text):
    text = re.sub(r"<[^>]+>", "", text)
    text = text.replace("\r", "").replace("\t", " ")
    text = re.sub(r"\n{3,}", "\n\n", text).strip()
    return text.replace("\n", "\\n")


def write_tsv(path, rows):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        for row in rows:
            f.write("\t".join(str(c) for c in row) + "\n")
    print(path, len(rows), "rows", round(os.path.getsize(path) / 1e6, 2), "MB")


# ---- Tafsir al-Jalalayn: some passages explain several verses at once; those verses are simply absent and the
# app shows the passage that covers them.
rows = []
for surah in range(1, 115):
    data = json.load(open(f"{TAFSIR_DIR}/{surah}.json", encoding="utf-8"))
    for entry in sorted(data, key=lambda e: e["ayah"]):
        text = clean(entry.get("text") or "")
        if text:
            rows.append((surah, entry["ayah"], text))
write_tsv(f"{ASSETS}/tafsir/jalalayn.tsv", rows)

# ---- Pickthall
data = json.load(open(PICKTHALL, encoding="utf-8"))
verses = data["quran"] if "quran" in data else data
rows = [(v["chapter"], v["verse"], clean(v["text"])) for v in verses]
assert len(rows) == 6236, len(rows)
write_tsv(f"{ASSETS}/quran/pickthall.tsv", rows)

# ---- Word timings: one line per verse, segments "firstWord,endWord,startMs,endMs" joined by ";"
for reciter in RECITERS:
    raw = open(f"{ALIGN_DIR}/{reciter}.json", encoding="utf-8").read()
    # The Sudais file starts with the aligner's crash log for a few verses; the JSON after it is complete.
    data = json.loads(raw[raw.index("[{"):])
    rows = []
    for entry in data:
        segments = ";".join(f"{a},{b},{s},{e}" for a, b, s, e in entry.get("segments") or [])
        rows.append((entry["surah"], entry["ayah"], segments))
    assert len(rows) == 6236, (reciter, len(rows))
    write_tsv(f"{ASSETS}/audio/timings/{reciter}.tsv", rows)
