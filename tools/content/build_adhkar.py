"""Builds core/content/src/main/assets/adhkar.json.

Inputs: Seen-Arabic Morning-And-Evening-Adhkar-DB ar.json (MIT) for morning/evening Arabic, counts
and Arabic virtues; curated categories below (Arabic from the cited hadith); English written for
Deen. Quranic passages are references into the Tanzil text, never retyped.

Usage: python -I build_adhkar.py <seen-arabic ar.json> <tanzil txt> <hadith dir> <out json>
"""
import json
import sys as _s
_s.stdout.reconfigure(encoding="utf-8")
import re
import sys

SEEN, TANZIL, HADITH_DIR, OUT = sys.argv[1:5]

ISTIADHA = "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ"


def L(ar, en):
    return {"ar": ar, "en": en}


# ---------------------------------------------------------------- morning & evening (Seen-Arabic order numbers)
# order -> (English, Arabic reference, English reference, English virtue or None, quran override)
ME = {
    2: ("Allah: there is no god but He, the Ever-Living, the Sustainer of all. Neither drowsiness nor sleep overtakes Him. "
        "To Him belongs whatever is in the heavens and whatever is on the earth. Who could intercede with Him except by His "
        "permission? He knows what is before them and what is behind them, and they grasp nothing of His knowledge except what "
        "He wills. His Kursi extends over the heavens and the earth, and guarding them does not tire Him. He is the Most High, "
        "the Most Great.",
        "الحاكم (1/562)، وصححه الألباني في صحيح الترغيب (1/273)", "Al-Hakim 1/562; graded sahih by al-Albani (Sahih at-Targhib 1/273)",
        "Whoever says it in the morning is protected from the jinn until evening, and whoever says it in the evening is protected until morning.",
        [[2, 255, 255]]),
    3: ("The Messenger believes in what was sent down to him from his Lord, and so do the believers. Each believes in Allah, "
        "His angels, His Books and His messengers: “We make no distinction between any of His messengers.” And they say, "
        "“We hear and we obey. Grant us Your forgiveness, our Lord; to You is the final return.” Allah does not burden a soul "
        "beyond what it can bear. It has the good it has earned and bears the evil it has earned. “Our Lord, do not take us to "
        "task if we forget or make a mistake. Our Lord, do not lay on us a burden like the one You laid on those before us. Our "
        "Lord, do not load us with what we have no strength to bear. Pardon us, forgive us and have mercy on us. You are our "
        "Protector, so help us against the people who disbelieve.”",
        "البخاري (5009)، ومسلم (807)", "Al-Bukhari 5009, Muslim 807",
        "Whoever recites the last two verses of al-Baqarah at night, they will suffice him.",
        [[2, 285, 286]]),
    4: ("In the name of Allah, the Most Compassionate, the Most Merciful. Say: He is Allah, the One. Allah, the Self-Sufficient, "
        "on whom all depend. He does not beget, nor was He begotten, and there is none comparable to Him.",
        "أبو داود (5082)، والترمذي (3575)", "Abu Dawud 5082, at-Tirmidhi 3575",
        "Said three times in the morning and in the evening, these three surahs will suffice you against everything.",
        [[112, 1, 4]]),
    5: ("In the name of Allah, the Most Compassionate, the Most Merciful. Say: I seek refuge in the Lord of the daybreak, from "
        "the evil of what He has created, from the evil of darkness when it settles, from the evil of those who blow on knots, "
        "and from the evil of an envier when he envies.",
        "أبو داود (5082)، والترمذي (3575)", "Abu Dawud 5082, at-Tirmidhi 3575",
        "Said three times in the morning and in the evening, these three surahs will suffice you against everything.",
        [[113, 1, 5]]),
    6: ("In the name of Allah, the Most Compassionate, the Most Merciful. Say: I seek refuge in the Lord of mankind, the King "
        "of mankind, the God of mankind, from the evil of the whisperer who withdraws, who whispers in the hearts of mankind, "
        "from among the jinn and mankind.",
        "أبو داود (5082)، والترمذي (3575)", "Abu Dawud 5082, at-Tirmidhi 3575",
        "Said three times in the morning and in the evening, these three surahs will suffice you against everything.",
        [[114, 1, 6]]),
    7: ("We have entered the morning, and the whole kingdom belongs to Allah. Praise be to Allah. There is no god but Allah "
        "alone, without partner; His is the dominion and His is the praise, and He has power over all things. My Lord, I ask "
        "You for the good of this day and the good of what follows it, and I seek refuge in You from the evil of this day and "
        "the evil of what follows it. My Lord, I seek refuge in You from laziness and the misery of old age. My Lord, I seek "
        "refuge in You from punishment in the Fire and punishment in the grave.",
        "مسلم (2723)", "Muslim 2723", None, None),
    8: ("We have entered the evening, and the whole kingdom belongs to Allah. Praise be to Allah. There is no god but Allah "
        "alone, without partner; His is the dominion and His is the praise, and He has power over all things. My Lord, I ask "
        "You for the good of this night and the good of what follows it, and I seek refuge in You from the evil of this night "
        "and the evil of what follows it. My Lord, I seek refuge in You from laziness and the misery of old age. My Lord, I "
        "seek refuge in You from punishment in the Fire and punishment in the grave.",
        "مسلم (2723)", "Muslim 2723", None, None),
    9: ("O Allah, by You we enter the morning and by You we enter the evening, by You we live and by You we die, and to You "
        "is the resurrection.", "الترمذي (3391)", "At-Tirmidhi 3391", None, None),
    10: ("O Allah, by You we enter the evening and by You we enter the morning, by You we live and by You we die, and to You "
         "is the final return.", "الترمذي (3391)", "At-Tirmidhi 3391", None, None),
    11: ("O Allah, You are my Lord; there is no god but You. You created me and I am Your servant, and I hold to Your covenant "
         "and promise as best I can. I seek refuge in You from the evil of what I have done. I acknowledge Your favour upon me "
         "and I acknowledge my sin, so forgive me, for none forgives sins but You.",
         "البخاري (6306)", "Al-Bukhari 6306",
         "The best way of asking forgiveness. Whoever says it by day with firm faith and dies before evening is among the people "
         "of Paradise, and likewise whoever says it at night and dies before morning.", None),
    12: ("O Allah, I have entered the morning calling You, the bearers of Your Throne, Your angels and all Your creation to "
         "witness that You are Allah: there is no god but You alone, without partner, and that Muhammad is Your servant and "
         "Messenger.", "أبو داود (5069)، والبخاري في الأدب المفرد (1201)", "Abu Dawud 5069; al-Bukhari, al-Adab al-Mufrad 1201",
         "Whoever says it four times in the morning or the evening, Allah frees him from the Fire.", None),
    13: ("O Allah, I have entered the evening calling You, the bearers of Your Throne, Your angels and all Your creation to "
         "witness that You are Allah: there is no god but You alone, without partner, and that Muhammad is Your servant and "
         "Messenger.", "أبو داود (5069)، والبخاري في الأدب المفرد (1201)", "Abu Dawud 5069; al-Bukhari, al-Adab al-Mufrad 1201",
         "Whoever says it four times in the morning or the evening, Allah frees him from the Fire.", None),
    14: ("O Allah, whatever blessing I or any of Your creation have this morning is from You alone, without partner. To You "
         "belong all praise and all thanks.", "أبو داود (5073)، والنسائي في عمل اليوم والليلة (7)",
         "Abu Dawud 5073; an-Nasa'i, 'Amal al-Yawm wal-Laylah 7",
         "Whoever says it in the morning has given thanks for that day, and whoever says it in the evening has given thanks for that night.", None),
    15: ("O Allah, whatever blessing I or any of Your creation have this evening is from You alone, without partner. To You "
         "belong all praise and all thanks.", "أبو داود (5073)، والنسائي في عمل اليوم والليلة (7)",
         "Abu Dawud 5073; an-Nasa'i, 'Amal al-Yawm wal-Laylah 7",
         "Whoever says it in the morning has given thanks for that day, and whoever says it in the evening has given thanks for that night.", None),
    16: ("O Allah, grant me well-being in my body. O Allah, grant me well-being in my hearing. O Allah, grant me well-being "
         "in my sight. There is no god but You. O Allah, I seek refuge in You from disbelief and poverty, and I seek refuge in "
         "You from the punishment of the grave. There is no god but You.", "أبو داود (5090)، وأحمد (20430)",
         "Abu Dawud 5090, Ahmad 20430", None, None),
    17: ("Allah is enough for me; there is no god but He. In Him I put my trust, and He is the Lord of the Mighty Throne.",
         "أبو داود (5081) موقوفًا، وابن السني (71) مرفوعًا", "Abu Dawud 5081 (as a Companion's saying); Ibn as-Sunni 71",
         "Whoever says it seven times in the morning and the evening, Allah will suffice him in what concerns him of this world and the next.", None),
    18: ("O Allah, I ask You for pardon and well-being in this world and the Hereafter. O Allah, I ask You for pardon and "
         "well-being in my religion, my worldly life, my family and my wealth. O Allah, cover my faults and calm my fears. O "
         "Allah, guard me from in front of me and behind me, from my right and my left, and from above me, and I seek refuge "
         "in Your greatness from being taken unawares from beneath me.", "أبو داود (5074)، وابن ماجه (3871)",
         "Abu Dawud 5074, Ibn Majah 3871", None, None),
    19: ("O Allah, Knower of the unseen and the seen, Originator of the heavens and the earth, Lord and Sovereign of "
         "everything: I bear witness that there is no god but You. I seek refuge in You from the evil of my own soul, from the "
         "evil of Satan and his snares, and from bringing harm upon myself or causing it to reach any Muslim.",
         "الترمذي (3392)، وأبو داود (5067)", "At-Tirmidhi 3392, Abu Dawud 5067", None, None),
    20: ("In the name of Allah, with whose name nothing on earth or in the heaven can cause harm, and He is the All-Hearing, "
         "the All-Knowing.", "أبو داود (5088)، والترمذي (3388)، وابن ماجه (3869)", "Abu Dawud 5088, at-Tirmidhi 3388, Ibn Majah 3869",
         "Whoever says it three times in the morning and three times in the evening, nothing will harm him.", None),
    21: ("I am pleased with Allah as my Lord, with Islam as my religion and with Muhammad ﷺ as my Prophet.",
         "أحمد (18967)، والترمذي (3389)", "Ahmad 18967, at-Tirmidhi 3389",
         "Whoever says it three times in the morning and the evening, Allah has promised to please him on the Day of Resurrection.", None),
    22: ("O Ever-Living, O Sustainer of all, by Your mercy I seek help: set right all my affairs, and do not leave me to "
         "myself even for the blink of an eye.", "الحاكم (1/545)، وانظر صحيح الترغيب (1/273)", "Al-Hakim 1/545; see Sahih at-Targhib 1/273", None, None),
    23: ("We have entered the morning, and the whole kingdom belongs to Allah, Lord of the worlds. O Allah, I ask You for the "
         "good of this day: its openings, its victory, its light, its blessing and its guidance. And I seek refuge in You from "
         "the evil of what is in it and the evil of what comes after it.", "أبو داود (5084)", "Abu Dawud 5084", None, None),
    24: ("We have entered the evening, and the whole kingdom belongs to Allah, Lord of the worlds. O Allah, I ask You for the "
         "good of this night: its openings, its victory, its light, its blessing and its guidance. And I seek refuge in You "
         "from the evil of what is in it and the evil of what comes after it.", "أبو داود (5084)", "Abu Dawud 5084", None, None),
    25: ("We have entered the morning upon the natural way of Islam, upon the word of sincere devotion, upon the religion of "
         "our Prophet Muhammad ﷺ, and upon the way of our father Ibrahim, who turned to Allah alone in submission and was not "
         "one of those who associate partners with Him.", "أحمد (15360)، وابن السني (34)", "Ahmad 15360, Ibn as-Sunni 34", None, None),
    26: ("We have entered the evening upon the natural way of Islam, upon the word of sincere devotion, upon the religion of "
         "our Prophet Muhammad ﷺ, and upon the way of our father Ibrahim, who turned to Allah alone in submission and was not "
         "one of those who associate partners with Him.", "أحمد (15360)، وابن السني (34)", "Ahmad 15360, Ibn as-Sunni 34", None, None),
    27: ("O Allah, I ask You for beneficial knowledge, good provision and deeds that are accepted.",
         "ابن ماجه (925)، وابن السني (54)", "Ibn Majah 925, Ibn as-Sunni 54", None, None),
    28: ("There is no god but Allah alone, without partner. His is the dominion and His is the praise, and He has power over "
         "all things.", "أبو داود (5077)، وابن ماجه (3798)، وأحمد (8719)", "Abu Dawud 5077, Ibn Majah 3798, Ahmad 8719",
         "Saying it ten times is like freeing four souls from the descendants of Isma'il.", None),
    29: ("O Allah, send blessings and peace upon our Prophet Muhammad.",
         "الطبراني، وحسّنه الألباني في صحيح الترغيب (656)", "At-Tabarani; graded hasan by al-Albani (Sahih at-Targhib 656)",
         "Whoever sends blessings on the Prophet ten times in the morning and ten in the evening will reach his intercession on the Day of Resurrection.", None),
    30: ("I seek refuge in the perfect words of Allah from the evil of what He has created.",
         "أحمد (7898)، والنسائي في عمل اليوم والليلة (590)", "Ahmad 7898; an-Nasa'i, 'Amal al-Yawm wal-Laylah 590",
         "Whoever says it three times in the evening, no sting will harm him that night.", None),
    31: ("Glory be to Allah and praise be to Him, as many times as the number of His creation, as much as pleases Him, as "
         "much as the weight of His Throne and as much as the ink of His words.", "مسلم (2726)", "Muslim 2726", None, None),
    32: ("There is no god but Allah alone, without partner. His is the dominion and His is the praise, and He has power over "
         "all things.", "البخاري (3293)، ومسلم (2691)", "Al-Bukhari 3293, Muslim 2691",
         "Said a hundred times in a day: the reward of freeing ten slaves, a hundred good deeds written, a hundred bad deeds "
         "erased, and protection from Satan for that day until evening.", None),
    33: ("I seek Allah's forgiveness and turn to Him in repentance.", "البخاري (6307)، ومسلم (2702)", "Al-Bukhari 6307, Muslim 2702", None, None),
    34: ("Glory be to Allah and praise be to Him.", "مسلم (2692)", "Muslim 2692",
         "Whoever says it a hundred times in the morning and the evening, no one will bring anything better on the Day of "
         "Resurrection, except someone who said the same or more.", None),
}

# Arabic virtue for item 32 (Seen-Arabic shares item 28's source text; the virtue line itself is right).
VIRTUE_AR_FIX = {2: lambda s: re.sub(r"^\[[^\]]*\]\s*", "", s)}

# ---------------------------------------------------------------- curated categories
# (arabic or None, quran refs, count, English, ar ref, en ref, virtue (ar, en) or None, instruction (ar, en) or None, collection check)
CURATED = [
    ("after_prayer", L("أذكار بعد الصلاة", "After prayer"), [
        ("أَسْتَغْفِرُ اللَّهَ، أَسْتَغْفِرُ اللَّهَ، أَسْتَغْفِرُ اللَّهَ، اللَّهُمَّ أَنْتَ السَّلَامُ، وَمِنْكَ السَّلَامُ، تَبَارَكْتَ ذَا الْجَلَالِ وَالْإِكْرَامِ",
         None, 1, "I seek Allah's forgiveness (three times). O Allah, You are Peace and from You comes peace. Blessed are You, Owner of majesty and honour.",
         "مسلم (591)", "Muslim 591", None, L("بعد السلام مباشرة", "Right after the salam"), "muslim"),
        ("لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، اللَّهُمَّ لَا مَانِعَ لِمَا أَعْطَيْتَ، وَلَا مُعْطِيَ لِمَا مَنَعْتَ، وَلَا يَنْفَعُ ذَا الْجَدِّ مِنْكَ الْجَدُّ",
         None, 1, "There is no god but Allah alone, without partner. His is the dominion and His is the praise, and He has power over all things. O Allah, none can withhold what You give, none can give what You withhold, and no one's wealth or standing avails him against You.",
         "البخاري (844)، ومسلم (593)", "Al-Bukhari 844, Muslim 593", None, None, "bukhari"),
        ("لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ، لَا إِلَهَ إِلَّا اللَّهُ، وَلَا نَعْبُدُ إِلَّا إِيَّاهُ، لَهُ النِّعْمَةُ وَلَهُ الْفَضْلُ وَلَهُ الثَّنَاءُ الْحَسَنُ، لَا إِلَهَ إِلَّا اللَّهُ مُخْلِصِينَ لَهُ الدِّينَ وَلَوْ كَرِهَ الْكَافِرُونَ",
         None, 1, "There is no god but Allah alone, without partner. His is the dominion and His is the praise, and He has power over all things. There is no power and no strength except through Allah. There is no god but Allah, and we worship none but Him. His are all blessings, all grace and all fine praise. There is no god but Allah; we devote our religion purely to Him, even if the disbelievers dislike it.",
         "مسلم (594)", "Muslim 594", None, None, "muslim"),
        ("سُبْحَانَ اللَّهِ", None, 33, "Glory be to Allah.", "مسلم (597)", "Muslim 597", None, None, "muslim"),
        ("الْحَمْدُ لِلَّهِ", None, 33, "Praise be to Allah.", "مسلم (597)", "Muslim 597", None, None, "muslim"),
        ("اللَّهُ أَكْبَرُ", None, 33, "Allah is the Greatest.", "مسلم (597)", "Muslim 597", None, None, "muslim"),
        ("لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
         None, 1, "There is no god but Allah alone, without partner. His is the dominion and His is the praise, and He has power over all things.",
         "مسلم (597)", "Muslim 597",
         L("من سبّح وحمد وكبّر ثلاثًا وثلاثين ثم ختم المئة بها غُفرت خطاياه وإن كانت مثل زبد البحر",
           "Whoever glorifies, praises and magnifies Allah thirty-three times each and completes the hundred with this, his sins are forgiven even if they are like the foam of the sea."),
         None, "muslim"),
        (None, [[2, 255, 255]], 1, ME[2][0], "النسائي في السنن الكبرى (9928)، وصححه الألباني في صحيح الجامع (6464)",
         "An-Nasa'i, as-Sunan al-Kubra 9928; graded sahih by al-Albani (Sahih al-Jami' 6464)",
         L("من قرأها دبر كل صلاة مكتوبة لم يمنعه من دخول الجنة إلا أن يموت",
           "Whoever recites it after every obligatory prayer, nothing stands between him and Paradise except death."), None, None),
        (None, [[112, 1, 4], [113, 1, 5], [114, 1, 6]], 1,
         "Surahs al-Ikhlas, al-Falaq and an-Nas.",
         "أبو داود (1523)، والترمذي (2903)", "Abu Dawud 1523, at-Tirmidhi 2903", None,
         L("بعد كل صلاة، وثلاثًا بعد الفجر والمغرب", "After every prayer; three times after Fajr and Maghrib"), None),
        ("اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ", None, 1,
         "O Allah, help me to remember You, to thank You and to worship You well.",
         "أبو داود (1522)، والنسائي (1303)", "Abu Dawud 1522, an-Nasa'i 1303", None, None, "abudawud"),
    ]),
    ("sleep", L("أذكار النوم", "Before sleep"), [
        (None, [[112, 1, 4], [113, 1, 5], [114, 1, 6]], 3, "Surahs al-Ikhlas, al-Falaq and an-Nas.",
         "البخاري (5017)", "Al-Bukhari 5017", None,
         L("اجمع كفّيك وانفث فيهما واقرأ، ثم امسح بهما ما استطعت من جسدك، تبدأ برأسك ووجهك. ثلاث مرات",
           "Cup your hands, blow lightly into them and recite, then wipe over as much of your body as you can, starting with your head and face. Three times."), None),
        (None, [[2, 255, 255]], 1, ME[2][0], "البخاري (2311)", "Al-Bukhari 2311",
         L("لا يزال عليك من الله حافظ، ولا يقربك شيطان حتى تصبح",
           "A guardian from Allah stays with you, and no devil comes near you until morning."), None, None),
        (None, [[2, 285, 286]], 1, ME[3][0], "البخاري (5009)، ومسلم (807)", "Al-Bukhari 5009, Muslim 807",
         L("من قرأ الآيتين من آخر سورة البقرة في ليلة كفتاه", ME[3][3]), None, None),
        ("بِاسْمِكَ رَبِّ وَضَعْتُ جَنْبِي، وَبِكَ أَرْفَعُهُ، إِنْ أَمْسَكْتَ نَفْسِي فَارْحَمْهَا، وَإِنْ أَرْسَلْتَهَا فَاحْفَظْهَا بِمَا تَحْفَظُ بِهِ عِبَادَكَ الصَّالِحِينَ",
         None, 1, "In Your name, my Lord, I lay down my side, and by You I raise it. If You take my soul, have mercy on it, and if You send it back, protect it as You protect Your righteous servants.",
         "البخاري (6320)، ومسلم (2714)", "Al-Bukhari 6320, Muslim 2714", None, None, "bukhari"),
        ("اللَّهُمَّ قِنِي عَذَابَكَ يَوْمَ تَبْعَثُ عِبَادَكَ", None, 3,
         "O Allah, protect me from Your punishment on the day You resurrect Your servants.",
         "أبو داود (5045)، والترمذي (3398)", "Abu Dawud 5045, at-Tirmidhi 3398", None,
         L("وتضع يدك اليمنى تحت خدّك", "With your right hand under your cheek"), "abudawud"),
        ("بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا", None, 1, "In Your name, O Allah, I die and I live.",
         "البخاري (6324)", "Al-Bukhari 6324", None, None, "bukhari"),
        ("سُبْحَانَ اللَّهِ", None, 33, "Glory be to Allah.", "البخاري (3705)، ومسلم (2727)", "Al-Bukhari 3705, Muslim 2727", None, None, "bukhari"),
        ("الْحَمْدُ لِلَّهِ", None, 33, "Praise be to Allah.", "البخاري (3705)، ومسلم (2727)", "Al-Bukhari 3705, Muslim 2727", None, None, "bukhari"),
        ("اللَّهُ أَكْبَرُ", None, 34, "Allah is the Greatest.", "البخاري (3705)، ومسلم (2727)", "Al-Bukhari 3705, Muslim 2727",
         L("قال ﷺ لعلي وفاطمة: هو خير لكما من خادم", "The Prophet ﷺ told Ali and Fatimah this is better for them than a servant."), None, "bukhari"),
        ("اللَّهُمَّ أَسْلَمْتُ نَفْسِي إِلَيْكَ، وَفَوَّضْتُ أَمْرِي إِلَيْكَ، وَوَجَّهْتُ وَجْهِي إِلَيْكَ، وَأَلْجَأْتُ ظَهْرِي إِلَيْكَ، رَغْبَةً وَرَهْبَةً إِلَيْكَ، لَا مَلْجَأَ وَلَا مَنْجَا مِنْكَ إِلَّا إِلَيْكَ، آمَنْتُ بِكِتَابِكَ الَّذِي أَنْزَلْتَ، وَبِنَبِيِّكَ الَّذِي أَرْسَلْتَ",
         None, 1, "O Allah, I submit myself to You, entrust my affairs to You, turn my face to You and rely on You completely, out of hope in You and fear of You. There is no refuge and no escape from You except to You. I believe in Your Book which You sent down and in Your Prophet whom You sent.",
         "البخاري (6313)، ومسلم (2710)", "Al-Bukhari 6313, Muslim 2710",
         L("فإن مُتَّ من ليلتك مُتَّ على الفطرة، واجعلهن آخر ما تقول",
           "If you die that night, you die upon the natural way. Make these your last words."), None, "bukhari"),
        ("اللَّهُمَّ رَبَّ السَّمَوَاتِ وَرَبَّ الْأَرْضِ وَرَبَّ الْعَرْشِ الْعَظِيمِ، رَبَّنَا وَرَبَّ كُلِّ شَيْءٍ، فَالِقَ الْحَبِّ وَالنَّوَى، وَمُنْزِلَ التَّوْرَاةِ وَالْإِنْجِيلِ وَالْفُرْقَانِ، أَعُوذُ بِكَ مِنْ شَرِّ كُلِّ شَيْءٍ أَنْتَ آخِذٌ بِنَاصِيَتِهِ، اللَّهُمَّ أَنْتَ الْأَوَّلُ فَلَيْسَ قَبْلَكَ شَيْءٌ، وَأَنْتَ الْآخِرُ فَلَيْسَ بَعْدَكَ شَيْءٌ، وَأَنْتَ الظَّاهِرُ فَلَيْسَ فَوْقَكَ شَيْءٌ، وَأَنْتَ الْبَاطِنُ فَلَيْسَ دُونَكَ شَيْءٌ، اقْضِ عَنَّا الدَّيْنَ، وَأَغْنِنَا مِنَ الْفَقْرِ",
         None, 1, "O Allah, Lord of the heavens, Lord of the earth and Lord of the Mighty Throne, our Lord and Lord of all things, who splits the grain and the date-stone, who sent down the Torah, the Injil and the Criterion: I seek refuge in You from the evil of everything whose forelock You hold. O Allah, You are the First, there is nothing before You; You are the Last, there is nothing after You; You are the Manifest, there is nothing above You; You are the Hidden, there is nothing beyond You. Settle our debts for us and free us from poverty.",
         "مسلم (2713)", "Muslim 2713", None, None, "muslim"),
        ("الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنَا وَسَقَانَا، وَكَفَانَا، وَآوَانَا، فَكَمْ مِمَّنْ لَا كَافِيَ لَهُ وَلَا مُئْوِيَ",
         None, 1, "Praise be to Allah who fed us, gave us drink, sufficed us and gave us shelter. How many there are with no one to suffice them and no one to shelter them.",
         "مسلم (2715)", "Muslim 2715", None, None, "muslim"),
    ]),
    ("waking", L("أذكار الاستيقاظ", "Waking up"), [
        ("الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ", None, 1,
         "Praise be to Allah who gave us life after He had caused us to die, and to Him is the resurrection.",
         "البخاري (6312)", "Al-Bukhari 6312", None, None, "bukhari"),
        ("لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، الْحَمْدُ لِلَّهِ، وَسُبْحَانَ اللَّهِ، وَلَا إِلَهَ إِلَّا اللَّهُ، وَاللَّهُ أَكْبَرُ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ، اللَّهُمَّ اغْفِرْ لِي",
         None, 1, "There is no god but Allah alone, without partner. His is the dominion and His is the praise, and He has power over all things. Praise be to Allah, glory be to Allah, there is no god but Allah, Allah is the Greatest, and there is no power and no strength except through Allah. O Allah, forgive me.",
         "البخاري (1154)", "Al-Bukhari 1154",
         L("من قالها إذا استيقظ من الليل ثم دعا استُجيب له، فإن توضأ وصلى قُبلت صلاته",
           "Whoever wakes at night and says this, then supplicates, is answered; and if he makes wudu and prays, his prayer is accepted."),
         None, "bukhari"),
        ("الْحَمْدُ لِلَّهِ الَّذِي عَافَانِي فِي جَسَدِي، وَرَدَّ عَلَيَّ رُوحِي، وَأَذِنَ لِي بِذِكْرِهِ", None, 1,
         "Praise be to Allah who gave health to my body, returned my soul to me and allowed me to remember Him.",
         "الترمذي (3401)", "At-Tirmidhi 3401", None, None, "tirmidhi"),
    ]),
    ("adhan", L("عند الأذان", "With the adhan"), [
        ("لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", None, 1, "There is no power and no strength except through Allah.",
         "البخاري (611)، ومسلم (385)", "Al-Bukhari 611, Muslim 385", None,
         L("ردّد مع المؤذن ما يقول، إلا عند «حيّ على الصلاة» و«حيّ على الفلاح» فتقول:",
           "Repeat what the muezzin says, except at “Hayya ʿala-s-salah” and “Hayya ʿala-l-falah”, where you say:"), "muslim"),
        ("وَأَنَا أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، وَأَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ، رَضِيتُ بِاللَّهِ رَبًّا، وَبِمُحَمَّدٍ رَسُولًا، وَبِالْإِسْلَامِ دِينًا",
         None, 1, "And I bear witness that there is no god but Allah alone, without partner, and that Muhammad is His servant and Messenger. I am pleased with Allah as my Lord, with Muhammad as Messenger and with Islam as my religion.",
         "مسلم (386)", "Muslim 386", L("غُفر له ذنبه", "His sins are forgiven."),
         L("بعد شهادة المؤذن", "After the muezzin's testimony of faith"), "muslim"),
        ("اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ", None, 1, "O Allah, send blessings and peace upon our Prophet Muhammad.",
         "مسلم (384)", "Muslim 384", None, L("بعد انتهاء الأذان", "When the adhan ends"), None),
        ("اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلَاةِ الْقَائِمَةِ، آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ، وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ",
         None, 1, "O Allah, Lord of this perfect call and of the prayer about to be established, grant Muhammad the place of intercession and distinction, and raise him to the praised station You have promised him.",
         "البخاري (614)", "Al-Bukhari 614", L("حلّت له شفاعة النبي ﷺ يوم القيامة", "The Prophet's intercession becomes due for him on the Day of Resurrection."), None, "bukhari"),
    ]),
    ("mosque", L("المسجد", "The mosque"), [
        ("اللَّهُمَّ اجْعَلْ فِي قَلْبِي نُورًا، وَفِي بَصَرِي نُورًا، وَفِي سَمْعِي نُورًا، وَعَنْ يَمِينِي نُورًا، وَعَنْ يَسَارِي نُورًا، وَفَوْقِي نُورًا، وَتَحْتِي نُورًا، وَأَمَامِي نُورًا، وَخَلْفِي نُورًا، وَاجْعَلْ لِي نُورًا",
         None, 1, "O Allah, place light in my heart, light in my sight and light in my hearing; light on my right and light on my left; light above me and light below me; light in front of me and light behind me; and grant me light.",
         "البخاري (6316)، ومسلم (763)", "Al-Bukhari 6316, Muslim 763", None, L("في الطريق إلى المسجد", "On the way to the mosque"), "bukhari"),
        ("أَعُوذُ بِاللَّهِ الْعَظِيمِ، وَبِوَجْهِهِ الْكَرِيمِ، وَسُلْطَانِهِ الْقَدِيمِ، مِنَ الشَّيْطَانِ الرَّجِيمِ", None, 1,
         "I seek refuge in Allah the Mighty, in His noble Face and His eternal authority, from Satan the accursed.",
         "أبو داود (466)", "Abu Dawud 466", L("قال الشيطان: حُفظ مني سائر اليوم", "Satan says: he is protected from me for the rest of the day."),
         L("عند دخول المسجد", "When entering the mosque"), "abudawud"),
        ("اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ", None, 1, "O Allah, open for me the gates of Your mercy.",
         "مسلم (713)", "Muslim 713", None, L("عند الدخول، بالقدم اليمنى", "When entering, right foot first"), "muslim"),
        ("اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ", None, 1, "O Allah, I ask You of Your bounty.",
         "مسلم (713)", "Muslim 713", None, L("عند الخروج، بالقدم اليسرى", "When leaving, left foot first"), "muslim"),
    ]),
    ("wudu", L("الوضوء", "Wudu"), [
        ("بِسْمِ اللَّهِ", None, 1, "In the name of Allah.", "أبو داود (101)، والترمذي (25)", "Abu Dawud 101, at-Tirmidhi 25", None,
         L("قبل الوضوء", "Before wudu"), None),
        ("أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ", None, 1,
         "I bear witness that there is no god but Allah alone, without partner, and I bear witness that Muhammad is His servant and Messenger.",
         "مسلم (234)", "Muslim 234", L("فُتحت له أبواب الجنة الثمانية يدخل من أيها شاء", "The eight gates of Paradise are opened for him, to enter by whichever he wishes."),
         L("بعد الوضوء", "After wudu"), "muslim"),
        ("اللَّهُمَّ اجْعَلْنِي مِنَ التَّوَّابِينَ، وَاجْعَلْنِي مِنَ الْمُتَطَهِّرِينَ", None, 1,
         "O Allah, make me among those who repent often and among those who purify themselves.",
         "الترمذي (55)", "At-Tirmidhi 55", None, L("بعد الوضوء", "After wudu"), "tirmidhi"),
        ("سُبْحَانَكَ اللَّهُمَّ وَبِحَمْدِكَ، أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا أَنْتَ، أَسْتَغْفِرُكَ وَأَتُوبُ إِلَيْكَ", None, 1,
         "Glory be to You, O Allah, and praise be to You. I bear witness that there is no god but You. I seek Your forgiveness and turn to You in repentance.",
         "النسائي في عمل اليوم والليلة (81)", "An-Nasa'i, 'Amal al-Yawm wal-Laylah 81", None, L("بعد الوضوء", "After wudu"), None),
    ]),
    ("home", L("الخروج من المنزل", "Leaving home"), [
        ("بِسْمِ اللَّهِ، تَوَكَّلْتُ عَلَى اللَّهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", None, 1,
         "In the name of Allah; I put my trust in Allah, and there is no power and no strength except through Allah.",
         "أبو داود (5095)، والترمذي (3426)", "Abu Dawud 5095, at-Tirmidhi 3426",
         L("يُقال له: هُديت وكُفيت ووُقيت، وتنحّى عنه الشيطان", "It is said to him: you are guided, sufficed and protected, and Satan turns away from him."),
         None, "abudawud"),
        ("اللَّهُمَّ إِنِّي أَعُوذُ بِكَ أَنْ أَضِلَّ أَوْ أُضَلَّ، أَوْ أَزِلَّ أَوْ أُزَلَّ، أَوْ أَظْلِمَ أَوْ أُظْلَمَ، أَوْ أَجْهَلَ أَوْ يُجْهَلَ عَلَيَّ",
         None, 1, "O Allah, I seek refuge in You from going astray or being led astray, from slipping or being made to slip, from wronging or being wronged, and from acting foolishly or being treated foolishly.",
         "أبو داود (5094)، والترمذي (3427)", "Abu Dawud 5094, at-Tirmidhi 3427", None, None, "abudawud"),
        ("بِسْمِ اللَّهِ", None, 1, "In the name of Allah.", "مسلم (2018)", "Muslim 2018",
         L("إذا ذكر الله عند دخوله وعند طعامه قال الشيطان: لا مبيت لكم ولا عشاء", "If you mention Allah when entering and when eating, Satan says: no place to stay and no supper for you."),
         L("عند دخول المنزل", "When coming home"), None),
    ]),
]

# ---------------------------------------------------------------- rewards
# Every dhikr shows what is promised for it. Where the hadith states a reward it is a "virtue"; where it only
# reports that the Prophet ﷺ said or taught it, that is shown as "sunnah" instead, never an invented reward.
# Keyed by the English reference; checked against the cited hadith (find.py) before adding.
def V(ar, en):
    return ("virtue", L(ar, en))


def S(ar, en):
    return ("sunnah", L(ar, en))


NOTES = {
    "Muslim 2723": S("كان النبي ﷺ يقوله إذا أمسى، ويقول مثله إذا أصبح.",
                     "The Prophet ﷺ said this every evening, and the same every morning."),
    "At-Tirmidhi 3391": S("كان النبي ﷺ يعلّمه أصحابه ليقولوه في الصباح والمساء.",
                          "The Prophet ﷺ taught it to his Companions to say morning and evening."),
    "Abu Dawud 5090, Ahmad 20430": S("كان أبو بكرة رضي الله عنه يكرره ثلاثًا صباحًا ومساءً ويقول: سمعت رسول الله ﷺ يدعو بهن، فأحب أن أستنّ بسنته.",
                                     "Abu Bakrah repeated it three times morning and evening, saying: I heard the Messenger of Allah ﷺ supplicate with these words, and I love to follow his Sunnah."),
    "Abu Dawud 5074, Ibn Majah 3871": S("لم يكن النبي ﷺ يدع هذه الدعوات حين يمسي وحين يصبح.",
                                        "The Prophet ﷺ never left these supplications, evening or morning."),
    "At-Tirmidhi 3392, Abu Dawud 5067": S("علّمه النبي ﷺ أبا بكر الصديق لما سأله ما يقول إذا أصبح وإذا أمسى، وقال: قله أيضًا إذا أخذت مضجعك.",
                                          "The Prophet ﷺ taught it to Abu Bakr when he asked what to say morning and evening, and told him to say it at bedtime too."),
    "Al-Hakim 1/545; see Sahih at-Targhib 1/273": S("أوصى به النبي ﷺ ابنته فاطمة أن تقوله إذا أصبحت وإذا أمست.",
                                                    "The Prophet ﷺ advised his daughter Fatimah to say it every morning and evening."),
    "Abu Dawud 5084": S("أمر النبي ﷺ أن يقوله المسلم إذا أصبح، ومثله إذا أمسى.",
                        "The Prophet ﷺ told us to say it in the morning, and the same in the evening."),
    "Ahmad 15360, Ibn as-Sunni 34": S("كان النبي ﷺ يقوله إذا أصبح وإذا أمسى.",
                                      "The Prophet ﷺ said it every morning and evening."),
    "Ibn Majah 925, Ibn as-Sunni 54": S("كان النبي ﷺ يقوله بعد صلاة الصبح حين يسلّم.",
                                        "The Prophet ﷺ said it after the Fajr prayer, as soon as he gave the salam."),
    "Muslim 2726": V("قال النبي ﷺ لجويرية: لقد قلتُ بعدكِ أربع كلمات ثلاث مرات، لو وُزنت بما قلتِ منذ اليوم لوزنتهن.",
                     "Said three times, these four phrases outweigh all the dhikr made since dawn."),
    "Al-Bukhari 6307, Muslim 2702": S("كان النبي ﷺ يستغفر الله ويتوب إليه في اليوم مائة مرة.",
                                      "The Prophet ﷺ asked Allah's forgiveness and turned to Him in repentance a hundred times a day."),
    "Muslim 591": S("كان النبي ﷺ يقوله إذا انصرف من صلاته.",
                    "The Prophet ﷺ said this as soon as he finished his prayer."),
    "Al-Bukhari 844, Muslim 593": S("كان النبي ﷺ يقوله في دبر كل صلاة مكتوبة.",
                                    "The Prophet ﷺ said it after every obligatory prayer."),
    "Muslim 594": S("كان النبي ﷺ يهلّل به في دبر كل صلاة حين يسلّم.",
                    "The Prophet ﷺ said it after every prayer, once he gave the salam."),
    "Muslim 597": V("مع التسبيح والتحميد والتكبير وتمام المئة: غُفرت خطاياه وإن كانت مثل زبد البحر.",
                    "With the other two and the words that complete the hundred, his sins are forgiven even if they are like the foam of the sea."),
    "Abu Dawud 1523, at-Tirmidhi 2903": S("أمر النبي ﷺ عقبة بن عامر أن يقرأ بالمعوذات دبر كل صلاة.",
                                          "The Prophet ﷺ told 'Uqbah ibn 'Amir to recite them after every prayer."),
    "Abu Dawud 1522, an-Nasa'i 1303": S("أخذ النبي ﷺ بيد معاذ وقال: والله إني لأحبك، ثم أوصاه ألا يدعه في دبر كل صلاة.",
                                        "The Prophet ﷺ took Mu'adh's hand, told him he loved him, and advised him never to leave it after any prayer."),
    "Al-Bukhari 5017": S("كان النبي ﷺ يفعله كل ليلة إذا أوى إلى فراشه.",
                         "The Prophet ﷺ did this every night when he went to bed."),
    "Al-Bukhari 6320, Muslim 2714": S("علّمه النبي ﷺ لمن أوى إلى فراشه.",
                                      "The Prophet ﷺ taught it for when you lie down to sleep."),
    "Abu Dawud 5045, at-Tirmidhi 3398": S("كان النبي ﷺ يقوله ثلاثًا إذا أراد أن ينام، ويده اليمنى تحت خده.",
                                          "The Prophet ﷺ said it three times when he lay down to sleep, his right hand under his cheek."),
    "Al-Bukhari 6324": S("كان النبي ﷺ يقوله إذا أخذ مضجعه من الليل.",
                         "The Prophet ﷺ said it when he lay down at night."),
    "Muslim 2713": S("كان النبي ﷺ يأمر أصحابه أن يقولوه إذا أخذوا مضاجعهم.",
                     "The Prophet ﷺ told his Companions to say it when they lay down to sleep."),
    "Muslim 2715": S("كان النبي ﷺ يقوله إذا أوى إلى فراشه.",
                     "The Prophet ﷺ said it when he went to bed."),
    "Al-Bukhari 6312": S("كان النبي ﷺ يقوله إذا استيقظ من نومه.",
                         "The Prophet ﷺ said it when he woke up."),
    "At-Tirmidhi 3401": S("أمر به النبي ﷺ من استيقظ من نومه.",
                          "The Prophet ﷺ told us to say it on waking."),
    "Al-Bukhari 611, Muslim 385": V("من أجاب المؤذن هكذا من قلبه دخل الجنة.",
                                    "Whoever answers the adhan like this, sincerely from the heart, enters Paradise."),
    "Muslim 384": V("من صلّى على النبي ﷺ صلاة صلّى الله عليه بها عشرًا.",
                    "Whoever sends one blessing on the Prophet ﷺ, Allah sends ten blessings on him."),
    "Al-Bukhari 6316, Muslim 763": S("كان النبي ﷺ يقوله وهو خارج إلى الصلاة.",
                                     "The Prophet ﷺ said it as he went out to the prayer."),
    "Muslim 713": S("علّمه النبي ﷺ لدخول المسجد والخروج منه.",
                    "The Prophet ﷺ taught it for entering and leaving the mosque."),
    "Abu Dawud 101, at-Tirmidhi 25": S("قال النبي ﷺ: لا وضوء لمن لم يذكر اسم الله عليه.",
                                       "The Prophet ﷺ said: there is no wudu for one who does not mention Allah's name over it."),
    "At-Tirmidhi 55": V("من قالها مع الشهادتين بعد الوضوء فُتحت له أبواب الجنة الثمانية، يدخل من أيها شاء.",
                        "Said with the testimony after wudu, the eight gates of Paradise are opened for him, to enter by whichever he wishes."),
    "An-Nasa'i, 'Amal al-Yawm wal-Laylah 81": V("كُتبت في رَقّ ثم طُبع عليها بطابع فلم يُكسر إلى يوم القيامة.",
                                                "It is written on a parchment and sealed, and the seal is not broken until the Day of Resurrection."),
    "Abu Dawud 5094, at-Tirmidhi 3427": S("ما خرج النبي ﷺ من بيت أم سلمة قط إلا رفع طرفه إلى السماء وقاله.",
                                          "The Prophet ﷺ never left Umm Salamah's house without raising his eyes to the sky and saying it."),
}


# ---------------------------------------------------------------- helpers
TASHKEEL = re.compile(r"[ؐ-ًؚ-ٰٟۖ-ۭـ]")


def norm(text):
    text = TASHKEEL.sub("", text)
    text = re.sub("[أإآٱ]", "ا", text).replace("ى", "ي").replace("ة", "ه").replace("ؤ", "و").replace("ئ", "ي")
    text = re.sub(r"[^ء-ي ]", " ", text)
    return re.sub(r"\s+", " ", text).strip()


def load_tanzil(path):
    verses = {}
    for line in open(path, encoding="utf-8"):
        if not line.strip() or line.startswith("#"):
            continue
        s, a, t = line.rstrip("\n").split("|", 2)
        verses[(int(s), int(a))] = t
    return verses


verses = load_tanzil(TANZIL)
collections = {}


def collection(name):
    if name not in collections:
        data = json.load(open(f"{HADITH_DIR}/ara-{name}.json", encoding="utf-8"))
        collections[name] = " | ".join(norm(h["text"]) for h in data["hadiths"])
    return collections[name]


def check_quran(refs):
    for s, a, b in refs:
        for n in range(a, b + 1):
            assert (s, n) in verses, f"missing verse {s}:{n}"


def verify(text, coll):
    """Every clause of our Arabic must appear verbatim (letters only) in the cited collection."""
    corpus = collection(coll)
    missing = [c for c in (norm(p) for p in re.split("[،,.]", text)) if c and c not in corpus]
    return missing


# ---------------------------------------------------------------- build
seen = json.load(open(SEEN, encoding="utf-8"))
morning, evening = [], []
for x in seen:
    order = x["order"]
    if order not in ME:
        continue  # #1 is an opening line, not a dhikr
    en, ref_ar, ref_en, virtue_en, quran = ME[order]
    virtue_ar = (x.get("fadl") or "").strip()
    if order in VIRTUE_AR_FIX:
        virtue_ar = VIRTUE_AR_FIX[order](virtue_ar)
    item = {"count": int(x["count"]), "translation": en, "reference": L(ref_ar, ref_en)}
    if quran:
        check_quran(quran)
        item["quran"] = quran
        if order in (2, 3):
            item["preface"] = ISTIADHA
    else:
        item["text"] = x["content"].strip()
    if virtue_ar or virtue_en:
        item["virtue"] = L(virtue_ar or None, virtue_en)
    if x["type"] in (0, 1):
        morning.append(dict(item))
    if x["type"] in (0, 2):
        evening.append(dict(item))

categories = [
    {"id": "morning", "title": L("أذكار الصباح", "Morning"), "items": morning},
    {"id": "evening", "title": L("أذكار المساء", "Evening"), "items": evening},
]

problems = 0
for cid, title, items in CURATED:
    out = []
    for text, quran, count, en, ref_ar, ref_en, virtue, instruction, coll in items:
        item = {"count": count, "translation": en, "reference": L(ref_ar, ref_en)}
        if text:
            item["text"] = text
            if coll:
                missing = verify(text, coll)
                if missing:
                    problems += 1
                    print(f"[{cid}] NOT FOUND in {coll}: {missing}")
        if quran:
            check_quran(quran)
            item["quran"] = quran
        if virtue:
            item["virtue"] = virtue
        if instruction:
            item["instruction"] = instruction
        out.append(item)
    categories.append({"id": cid, "title": title, "items": out})

# A reward for every dhikr: a stated virtue, else one shared by the same narration in this category (the three
# tasbih of a hundred), else the Sunnah note above.
missing = []
for cat in categories:
    for item in cat["items"]:
        if "virtue" in item:
            continue
        note = NOTES.get(item["reference"]["en"])
        if note and note[0] == "virtue":
            item["virtue"] = note[1]
            continue
        shared = next((o["virtue"] for o in cat["items"] if "virtue" in o and o["reference"] == item["reference"]), None)
        if shared:
            item["virtue"] = shared
        elif note:
            item["sunnah"] = note[1]
        else:
            missing.append((cat["id"], item["reference"]["en"]))
assert not missing, f"no reward or Sunnah note for: {missing}"

for cat in categories:
    for i, item in enumerate(cat["items"], 1):
        item["id"] = i
    keys = ("id", "preface", "text", "quran", "count", "instruction", "translation", "virtue", "sunnah", "reference")
    cat["items"] = [{k: item[k] for k in keys if k in item} for item in cat["items"]]

doc = {
    "version": 1,
    "license": "Arabic morning/evening text: Seen-Arabic Morning-And-Evening-Adhkar-DB (MIT). Other Arabic: classical hadith "
               "texts (public domain). Quran passages are references into the Tanzil text. English: written for Deen (GPL-3.0-or-later). "
               "See CONTENT_SOURCES.md.",
    "categories": categories,
}
json.dump(doc, open(OUT, "w", encoding="utf-8", newline="\n"), ensure_ascii=False, indent=1)
print("categories:", [(c["id"], len(c["items"])) for c in categories], "verification problems:", problems)
