#!/usr/bin/env python3
"""Builds core/location/src/main/assets/cities.tsv from GeoNames.

Inputs (CC BY 4.0, https://download.geonames.org/export/dump/):
  cities5000.txt         (unzip cities5000.zip)
  admin1CodesASCII.txt
  alternateNamesV2.zip   (~200 MB; language-tagged names, used for the Arabic name)

Usage:
  python3 tools/geonames/build_cities.py cities5000.txt admin1CodesASCII.txt alternateNamesV2.zip OUT

where OUT is core/location/src/main/assets/cities.tsv.

Output: plain UTF-8 TSV (the APK compresses it; plain text keeps city fixes reviewable in PRs),
one city per line, sorted by population (largest first):
  id  name  arabic_name  country  region  lat  lon  population  timezone

The output is deterministic (stable sort), so re-running on the same input
gives a byte-identical file.
"""
import io
import sys
import zipfile

# Neighbourhoods, historical, abandoned and destroyed places are left out.
EXCLUDED_FEATURE_CODES = {"PPLX", "PPLH", "PPLQ", "PPLW", "PPLCH"}

# Arabic script is shared with Persian, Urdu, Pashto…; names containing letters that Arabic
# itself doesn't use are skipped so we pick the Arabic spelling.
NON_ARABIC_LETTERS = set("پچژگکیےۀہھٹڈڑںۓڤ")


def tagged_arabic_names(alternate_names_zip: str, wanted_ids: set) -> dict:
    """geonameid -> best Arabic ("ar") name: preferred first, then short, never colloquial/historic."""
    best = {}
    with zipfile.ZipFile(alternate_names_zip) as z:
        with z.open("alternateNamesV2.txt") as raw:
            for line in io.TextIOWrapper(raw, encoding="utf-8"):
                c = line.rstrip("\n").split("\t")
                if len(c) < 8 or c[2] != "ar":
                    continue
                geoname_id = int(c[1])
                if geoname_id not in wanted_ids or c[6] == "1" or c[7] == "1":
                    continue
                rank = (0 if c[4] == "1" else 1, 0 if c[5] == "1" else 1)
                if geoname_id not in best or rank < best[geoname_id][0]:
                    best[geoname_id] = (rank, c[3].strip())
    return {k: v[1] for k, v in best.items()}


def guessed_arabic_name(alternate_names: str) -> str:
    """Fallback when GeoNames has no "ar"-tagged name: first Arabic-script name that isn't Persian/Urdu."""
    for candidate in alternate_names.split(","):
        candidate = candidate.strip()
        has_arabic = any("ء" <= ch <= "ي" for ch in candidate)
        has_latin = any(ch.isascii() and ch.isalpha() for ch in candidate)
        if has_arabic and not has_latin and not (set(candidate) & NON_ARABIC_LETTERS):
            return candidate
    return ""


def main(cities_path: str, admin1_path: str, alternate_names_zip: str, out_path: str) -> None:
    regions = {}
    with open(admin1_path, encoding="utf-8") as f:
        for line in f:
            code, _name, ascii_name, _id = line.rstrip("\n").split("\t")
            regions[code] = ascii_name

    rows = []
    with open(cities_path, encoding="utf-8") as f:
        for line in f:
            c = line.rstrip("\n").split("\t")
            geoname_id, name, alternates, lat, lon = c[0], c[1], c[3], c[4], c[5]
            feature_code, country, admin1 = c[7], c[8], c[10]
            population, timezone = int(c[14] or 0), c[17]
            if feature_code in EXCLUDED_FEATURE_CODES or not timezone:
                continue
            rows.append((
                -population, int(geoname_id), name, alternates, country,
                regions.get(f"{country}.{admin1}", ""), f"{float(lat):.4f}", f"{float(lon):.4f}",
                population, timezone,
            ))

    tagged = tagged_arabic_names(alternate_names_zip, {r[1] for r in rows})
    rows = [r[:3] + (tagged.get(r[1]) or guessed_arabic_name(r[3]),) + r[4:] for r in rows]
    rows.sort()

    buffer = io.StringIO()
    for r in rows:
        buffer.write("\t".join(str(v) for v in r[1:]) + "\n")
    with open(out_path, "w", encoding="utf-8", newline="\n") as out:
        out.write(buffer.getvalue())

    with_tagged = sum(1 for r in rows if r[1] in tagged)
    with_arabic = sum(1 for r in rows if r[3])
    print(f"{len(rows)} cities, {with_arabic} with an Arabic name ({with_tagged} tagged 'ar') -> {out_path}")


if __name__ == "__main__":
    if len(sys.argv) != 5:
        sys.exit(__doc__)
    main(*sys.argv[1:])
