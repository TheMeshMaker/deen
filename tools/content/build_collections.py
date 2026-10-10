"""Builds core/content/src/main/assets/hadith/: the full Arabic text of the major collections, one TSV per book.

Text, numbering, book ranges and gradings: fawazahmed0/hadith-api (Unlicense). Arabic book titles: the compilers'
own (public-domain) titles, matched by English name from AhmedBaset/hadith-json. Only titles are taken from there.

Output:
  hadith/collections.json   collections, their books, and the grader / grade label tables
  hadith/<id>/<book>.tsv    number <TAB> number-in-book <TAB> grade ("grader:label" indexes or empty) <TAB> text

Usage: python -I build_collections.py <hadith-api dir> <hadith-json dir> <out dir>
"""
import json
import os
import re
import sys

sys.stdout.reconfigure(encoding="utf-8")
HAPI, HJ, OUT = sys.argv[1:4]

COLLECTIONS = [
    ("bukhari", "صحيح البخاري", "Sahih al-Bukhari", "الإمام البخاري", "Imam al-Bukhari", True),
    ("muslim", "صحيح مسلم", "Sahih Muslim", "الإمام مسلم", "Imam Muslim", True),
    ("abudawud", "سنن أبي داود", "Sunan Abi Dawud", "الإمام أبو داود", "Imam Abu Dawud", False),
    ("tirmidhi", "جامع الترمذي", "Jamiʿ at-Tirmidhi", "الإمام الترمذي", "Imam at-Tirmidhi", False),
    ("nasai", "سنن النسائي", "Sunan an-Nasa'i", "الإمام النسائي", "Imam an-Nasa'i", False),
    ("ibnmajah", "سنن ابن ماجه", "Sunan Ibn Majah", "الإمام ابن ماجه", "Imam Ibn Majah", False),
    ("malik", "موطأ مالك", "Muwatta Malik", "الإمام مالك", "Imam Malik", False),
    ("qudsi", "الأربعون القدسية", "Forty Hadith Qudsi", "", "", False),
]

GRADERS_AR = {
    "Al-Albani": "الألباني",
    "Zubair Ali Zai": "زبير علي زئي",
    "Shuaib Al Arnaut": "شعيب الأرناؤوط",
    "Abu Ghuddah": "عبد الفتاح أبو غدة",
    "Muhammad Muhyi Al-Din Abdul Hamid": "محمد محيي الدين عبد الحميد",
    "Muhammad Fouad Abd al-Baqi": "محمد فؤاد عبد الباقي",
    "Ahmad Muhammad Shakir": "أحمد شاكر",
    "Bashar Awad Maarouf": "بشار عواد معروف",
    "Salim al-Hilali": "سليم الهلالي",
}
# Preferred grader per collection (the one shown); falls back to the first grading listed.
PREFERRED = ["Al-Albani", "Salim al-Hilali", "Shuaib Al Arnaut", "Zubair Ali Zai"]


def grade_class(label):
    l = label.lower()
    if "mawdu" in l or "batil" in l or "maudu" in l:
        return "FABRICATED"
    if "daif" in l or "da'if" in l or "munkar" in l or "shadh" in l:
        return "WEAK"
    if "hasan" in l and "sahih" not in l:
        return "GOOD"
    if "sahih" in l or "agreed" in l:
        return "AUTHENTIC"
    return "OTHER"


def grade_ar(label):
    l = label.lower()
    if "mawdu" in l or "maudu" in l:
        base = "موضوع"
    elif "batil" in l:
        base = "باطل"
    elif "very daif" in l:
        base = "ضعيف جدًا"
    elif "munkar" in l:
        base = "منكر"
    elif "shadh" in l:
        base = "شاذ"
    elif "daif" in l:
        base = "ضعيف"
    elif "hasan" in l and "sahih" in l:
        base = "حسن صحيح"
    elif "sahih" in l and "lighairihi" in l:
        base = "صحيح لغيره"
    elif "hasan" in l and "lighairihi" in l:
        base = "حسن لغيره"
    elif "agreed" in l:
        base = "صحيح، متفق عليه"
    elif "sahih" in l:
        base = "صحيح"
    elif "hasan" in l:
        base = "حسن"
    elif "maqtu" in l:
        base = "مقطوع"
    elif "mursal" in l:
        base = "مرسل"
    elif "malool" in l or "maalool" in l:
        base = "معلول"
    else:
        return label
    if ("isnaad" in l or "sanad" in l) and "agreed" not in l:
        base = "إسناده " + base
    if "mauquf" in l or "muquf" in l:
        base += " (موقوف)"
    elif "maqtu" in l and base != "مقطوع":
        base += " (مقطوع)"
    return base


def clean(text):
    text = re.sub(r"[‎‏‪-‮﻿]", "", text)
    return re.sub(r"\s+", " ", text).strip()


def norm_en(s):
    return re.sub(r"[^a-z]", "", s.lower().replace("the book of", "").replace("chapters on", "").replace("book of", ""))


graders, grade_labels = [], []


def index_of(table, key, value):
    for i, row in enumerate(table):
        if row["en"] == key:
            return i
    table.append(value)
    return len(table) - 1


collections = []
total_bytes = 0
for cid, name_ar, name_en, author_ar, author_en, all_sahih in COLLECTIONS:
    api = json.load(open(f"{HAPI}/ara-{cid}.json", encoding="utf-8"))
    sections = api["metadata"]["sections"]
    ranges = api["metadata"].get("section_details", {})
    titles_ar = {}
    hj_path = f"{HJ}/{cid}.json" if cid != "qudsi" else f"{HJ}/qudsi40.json"
    if os.path.exists(hj_path):
        hj = json.load(open(hj_path, encoding="utf-8"))
        titles_ar = {norm_en(c["english"]): c["arabic"] for c in hj["chapters"]}

    books = {}
    for h in api["hadiths"]:
        text = clean(h["text"])
        if not text:
            continue
        book = int(h["reference"]["book"])
        if book == 0 and h["reference"]["hadith"] == 0:
            # No in-book reference: place it by the collection's own number ranges.
            n = float(h["hadithnumber"])
            book = next((int(k) for k, r in ranges.items() if int(k) > 0 and r["hadithnumber_first"] <= n <= r["hadithnumber_last"]), 0)
        grade = ""
        if h.get("grades"):
            chosen = next((g for p in PREFERRED for g in h["grades"] if g["name"] == p), h["grades"][0])
            label = chosen["grade"].strip()
            if label and label != "-":
                gi = index_of(graders, chosen["name"], {"en": chosen["name"], "ar": GRADERS_AR.get(chosen["name"], chosen["name"])})
                li = index_of(grade_labels, label, {"en": label, "ar": grade_ar(label), "class": grade_class(label)})
                grade = f"{gi}:{li}"
        number = h["hadithnumber"]
        number = int(number) if float(number).is_integer() else number
        books.setdefault(book, []).append(f"{number}\t{h['reference']['hadith']}\t{grade}\t{text}")

    os.makedirs(f"{OUT}/{cid}", exist_ok=True)
    book_rows = []
    for book in sorted(books):
        title_en = (sections.get(str(book)) or "").strip() or ("Introduction" if book == 0 else name_en)
        title_ar = titles_ar.get(norm_en(title_en)) or ("المقدمة" if book == 0 else name_ar)
        if cid == "qudsi":
            title_en, title_ar = name_en, name_ar
        lines = books[book]
        body = "\n".join(lines) + "\n"
        path = f"{OUT}/{cid}/{book}.tsv"
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(body)
        total_bytes += len(body.encode("utf-8"))
        book_rows.append({"book": book, "title": {"ar": title_ar, "en": title_en}, "count": len(lines),
                          "first": lines[0].split("\t", 1)[0], "last": lines[-1].split("\t", 1)[0]})
    collections.append({
        "id": cid,
        "name": {"ar": name_ar, "en": name_en},
        "author": {"ar": author_ar, "en": author_en} if author_en else None,
        "allAuthentic": all_sahih,
        "count": sum(b["count"] for b in book_rows),
        "books": book_rows,
    })
    print(cid, sum(b["count"] for b in book_rows), "hadith in", len(book_rows), "books")

doc = {
    "license": "Arabic text, numbering and gradings: fawazahmed0/hadith-api (Unlicense). Book titles: the compilers' own. See CONTENT_SOURCES.md.",
    "graders": graders,
    "grades": grade_labels,
    "collections": collections,
}
json.dump(doc, open(f"{OUT}/collections.json", "w", encoding="utf-8", newline="\n"), ensure_ascii=False, separators=(",", ":"))
print("graders", len(graders), "grade labels", len(grade_labels), "text MB", round(total_bytes / 1e6, 1))
