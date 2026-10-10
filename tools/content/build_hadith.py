"""Builds core/content/src/main/assets/hadith/nawawi40.json: an-Nawawi's Forty (42 hadith).

Arabic: classical text (public domain) via fawazahmed0/hadith-api (Unlicense), lightly cleaned.
English, titles and short source lines: written for Deen.

Usage: python -I build_hadith.py <ara-nawawi.json> <out json>
"""
import json
import re
import sys

sys.stdout.reconfigure(encoding="utf-8")
SRC, OUT = sys.argv[1:3]

# number -> (title ar, title en, source ar, source en, English)
H = {
    1: ("الأعمال بالنيات", "Actions are by intentions", "رواه البخاري ومسلم", "Al-Bukhari and Muslim",
        "ʿUmar ibn al-Khattab reported: I heard the Messenger of Allah ﷺ say: “Actions are only by intentions, and each person will have only what he intended. So whoever emigrated for Allah and His Messenger, his emigration is for Allah and His Messenger; and whoever emigrated for some worldly gain or to marry a woman, his emigration is for what he emigrated for.”"),
    2: ("الإسلام والإيمان والإحسان", "Islam, faith and excellence", "رواه مسلم", "Muslim",
        "ʿUmar also reported: One day, while we were sitting with the Messenger of Allah ﷺ, a man appeared before us with extremely white clothes and extremely black hair. No sign of travel could be seen on him, and none of us knew him. He sat down facing the Prophet ﷺ, rested his knees against his knees, placed his palms on his thighs and said: “O Muhammad, tell me about Islam.” The Messenger of Allah ﷺ said: “Islam is to testify that there is no god but Allah and that Muhammad is the Messenger of Allah, to establish the prayer, to give zakah, to fast Ramadan, and to make pilgrimage to the House if you are able to.” He said: “You have spoken truly.” We were amazed that he asked him and then confirmed his answer. He said: “Tell me about faith.” He said: “It is to believe in Allah, His angels, His Books, His messengers and the Last Day, and to believe in divine decree, its good and its bad.” He said: “You have spoken truly. Tell me about excellence (ihsan).” He said: “It is to worship Allah as though you see Him, for though you do not see Him, He sees you.” He said: “Tell me about the Hour.” He said: “The one asked knows no more about it than the one asking.” He said: “Then tell me about its signs.” He said: “That the slave-woman gives birth to her mistress, and that you see barefoot, unclothed, destitute shepherds competing in constructing tall buildings.” Then he left, and I stayed a while. Then he said: “O ʿUmar, do you know who the questioner was?” I said: “Allah and His Messenger know best.” He said: “That was Jibril, who came to teach you your religion.”"),
    3: ("أركان الإسلام", "The pillars of Islam", "رواه البخاري ومسلم", "Al-Bukhari and Muslim",
        "ʿAbdullah ibn ʿUmar reported: I heard the Messenger of Allah ﷺ say: “Islam is built on five: testifying that there is no god but Allah and that Muhammad is the Messenger of Allah, establishing the prayer, giving zakah, pilgrimage to the House, and fasting Ramadan.”"),
    4: ("خلق الإنسان والقدر", "Creation and destiny", "رواه البخاري ومسلم", "Al-Bukhari and Muslim",
        "ʿAbdullah ibn Masʿud reported: The Messenger of Allah ﷺ, the truthful and the believed, told us: “The creation of each of you is gathered in his mother's womb for forty days as a drop, then it becomes a clinging clot for a similar period, then a lump of flesh for a similar period. Then the angel is sent to him and breathes the soul into him, and he is commanded to write four things: his provision, his lifespan, his deeds, and whether he will be wretched or happy. By Allah, besides whom there is no god, one of you may do the deeds of the people of Paradise until there is only an arm's length between him and it, then what is written overtakes him, and he does the deeds of the people of the Fire and enters it. And one of you may do the deeds of the people of the Fire until there is only an arm's length between him and it, then what is written overtakes him, and he does the deeds of the people of Paradise and enters it.”"),
    5: ("رد المحدثات", "Rejecting what is invented", "رواه البخاري ومسلم", "Al-Bukhari and Muslim",
        "The Mother of the Believers ʿAʾishah reported: The Messenger of Allah ﷺ said: “Whoever introduces into this matter of ours something that is not part of it, it is rejected.” In a narration of Muslim: “Whoever does a deed that is not in accordance with our matter, it is rejected.”"),
    6: ("الحلال بيّن والحرام بيّن", "The lawful and the unlawful", "رواه البخاري ومسلم", "Al-Bukhari and Muslim",
        "An-Nuʿman ibn Bashir reported: I heard the Messenger of Allah ﷺ say: “The lawful is clear and the unlawful is clear, and between them are doubtful matters that many people do not know. Whoever guards against doubtful matters keeps his religion and his honour clear, and whoever falls into doubtful matters falls into the unlawful, like a shepherd grazing around a sanctuary, about to graze in it. Truly every king has a sanctuary, and truly Allah's sanctuary is what He has forbidden. Truly in the body there is a piece of flesh: if it is sound, the whole body is sound, and if it is corrupt, the whole body is corrupt. Truly, it is the heart.”"),
    7: ("الدين النصيحة", "The religion is sincerity", "رواه مسلم", "Muslim",
        "Tamim ad-Dari reported that the Prophet ﷺ said: “The religion is sincerity.” We asked: “To whom?” He said: “To Allah, His Book, His Messenger, the leaders of the Muslims and their common folk.”"),
    8: ("حرمة المسلم", "The sanctity of a Muslim", "رواه البخاري ومسلم", "Al-Bukhari and Muslim",
        "Ibn ʿUmar reported that the Messenger of Allah ﷺ said: “I have been commanded to fight people until they testify that there is no god but Allah and that Muhammad is the Messenger of Allah, establish the prayer and give zakah. If they do that, their blood and their wealth are protected from me, except by the right of Islam, and their reckoning is with Allah the Exalted.”"),
    9: ("ما استطعتم", "Do what you can", "رواه البخاري ومسلم", "Al-Bukhari and Muslim",
        "Abu Hurayrah reported: I heard the Messenger of Allah ﷺ say: “What I have forbidden you, avoid it; and what I have commanded you, do as much of it as you can. Those before you were destroyed only by their excessive questioning and their disagreeing with their prophets.”"),
    10: ("إن الله طيب", "Allah accepts only what is pure", "رواه مسلم", "Muslim",
         "Abu Hurayrah reported: The Messenger of Allah ﷺ said: “Allah is pure and accepts only what is pure. Allah has commanded the believers with what He commanded the messengers. He said: ‘O messengers, eat of the good things and do righteous deeds’, and He said: ‘O you who believe, eat of the good things We have provided you.’” Then he mentioned a man who travels far, dishevelled and dusty, stretching his hands to the sky: “O Lord! O Lord!” while his food is unlawful, his drink is unlawful, his clothing is unlawful and he has been nourished with the unlawful. “So how can he be answered?”"),
    11: ("دع ما يريبك", "Leave what makes you doubt", "رواه الترمذي والنسائي، وقال الترمذي: حسن صحيح", "At-Tirmidhi and an-Nasa'i; at-Tirmidhi: hasan sahih",
         "Al-Hasan ibn ʿAli, the grandson of the Messenger of Allah ﷺ and his beloved, said: I memorised from the Messenger of Allah ﷺ: “Leave what makes you doubt for what does not make you doubt.”"),
    12: ("ترك ما لا يعني", "Leaving what does not concern you", "حديث حسن، رواه الترمذي وغيره", "At-Tirmidhi and others; hasan",
         "Abu Hurayrah reported: The Messenger of Allah ﷺ said: “Part of the excellence of a person's Islam is his leaving what does not concern him.”"),
    13: ("حب الخير للأخ", "Love for your brother", "رواه البخاري ومسلم", "Al-Bukhari and Muslim",
         "Anas ibn Malik, the servant of the Messenger of Allah ﷺ, reported that the Prophet ﷺ said: “None of you truly believes until he loves for his brother what he loves for himself.”"),
    14: ("حرمة دم المسلم", "The sanctity of life", "رواه البخاري ومسلم", "Al-Bukhari and Muslim",
         "Ibn Masʿud reported: The Messenger of Allah ﷺ said: “The blood of a Muslim who testifies that there is no god but Allah and that I am the Messenger of Allah is not lawful except in one of three cases: the married person who commits adultery, a life for a life, and one who abandons his religion and separates from the community.”"),
    15: ("قل خيرًا أو اصمت", "Speak good or stay silent", "رواه البخاري ومسلم", "Al-Bukhari and Muslim",
         "Abu Hurayrah reported that the Messenger of Allah ﷺ said: “Whoever believes in Allah and the Last Day, let him speak good or remain silent. Whoever believes in Allah and the Last Day, let him honour his neighbour. Whoever believes in Allah and the Last Day, let him honour his guest.”"),
    16: ("لا تغضب", "Do not become angry", "رواه البخاري", "Al-Bukhari",
         "Abu Hurayrah reported that a man said to the Prophet ﷺ: “Advise me.” He said: “Do not become angry.” The man asked again several times, and each time he said: “Do not become angry.”"),
    17: ("الإحسان في كل شيء", "Excellence in everything", "رواه مسلم", "Muslim",
         "Shaddad ibn Aws reported that the Messenger of Allah ﷺ said: “Allah has prescribed excellence in everything. So when you kill, kill well, and when you slaughter, slaughter well. Let each of you sharpen his blade and spare the animal suffering.”"),
    18: ("تقوى الله وحسن الخلق", "Mindfulness and good character", "رواه الترمذي، وقال: حسن", "At-Tirmidhi; hasan",
         "Abu Dharr and Muʿadh ibn Jabal reported that the Messenger of Allah ﷺ said: “Be mindful of Allah wherever you are. Follow a bad deed with a good one and it will erase it, and treat people with good character.”"),
    19: ("احفظ الله يحفظك", "Be mindful of Allah", "رواه الترمذي، وقال: حسن صحيح", "At-Tirmidhi; hasan sahih",
         "Ibn ʿAbbas said: I was behind the Messenger of Allah ﷺ one day when he said: “Young man, I will teach you some words: Be mindful of Allah and He will protect you. Be mindful of Allah and you will find Him before you. When you ask, ask Allah; and when you seek help, seek help from Allah. Know that if the whole nation gathered to benefit you with something, they would benefit you only with what Allah had already written for you; and if they gathered to harm you with something, they would harm you only with what Allah had already written against you. The pens have been lifted and the pages have dried.” In a narration other than at-Tirmidhi's: “Be mindful of Allah and you will find Him in front of you. Know Allah in times of ease and He will know you in times of hardship. Know that what passed you by was never going to reach you, and what reached you was never going to pass you by. Know that victory comes with patience, relief comes with distress, and with hardship comes ease.”"),
    20: ("الحياء", "Modesty", "رواه البخاري", "Al-Bukhari",
         "Abu Masʿud ʿUqbah ibn ʿAmr reported: The Messenger of Allah ﷺ said: “Among the words people have received from the earliest prophecy is: if you feel no shame, then do as you wish.”"),
    21: ("آمنت بالله ثم استقم", "Believe, then stand firm", "رواه مسلم", "Muslim",
         "Sufyan ibn ʿAbdullah said: I said: “O Messenger of Allah, tell me something about Islam that I will not need to ask anyone else about.” He said: “Say: I believe in Allah, then stand firm.”"),
    22: ("الطريق إلى الجنة", "The way to Paradise", "رواه مسلم", "Muslim",
         "Jabir ibn ʿAbdullah reported that a man asked the Messenger of Allah ﷺ: “Tell me: if I pray the obligatory prayers, fast Ramadan, treat the lawful as lawful and the unlawful as unlawful, and do nothing more than that, will I enter Paradise?” He said: “Yes.”"),
    23: ("الطهور شطر الإيمان", "Purity is half of faith", "رواه مسلم", "Muslim",
         "Abu Malik al-Ashʿari reported: The Messenger of Allah ﷺ said: “Purity is half of faith. Alhamdulillah fills the scale, and Subhanallah and Alhamdulillah fill what is between the heavens and the earth. Prayer is light, charity is proof, patience is brightness, and the Quran is an argument for you or against you. Every person goes out in the morning and sells himself, either freeing himself or ruining himself.”"),
    24: ("يا عبادي", "O My servants", "رواه مسلم", "Muslim",
         "Abu Dharr reported that the Prophet ﷺ said, in what he narrates from his Lord, Blessed and Exalted: “O My servants, I have forbidden injustice for Myself and made it forbidden among you, so do not wrong one another. O My servants, all of you are astray except those I guide, so seek guidance from Me and I will guide you. O My servants, all of you are hungry except those I feed, so ask Me for food and I will feed you. O My servants, all of you are unclothed except those I clothe, so ask Me for clothing and I will clothe you. O My servants, you sin by night and by day, and I forgive all sins, so ask Me for forgiveness and I will forgive you. O My servants, you will never be able to harm Me, nor will you ever be able to benefit Me. O My servants, if the first and the last of you, your humans and your jinn, were as pious as the most pious heart of any one of you, that would add nothing to My dominion. O My servants, if the first and the last of you, your humans and your jinn, were as wicked as the most wicked heart of any one of you, that would take nothing away from My dominion. O My servants, if the first and the last of you, your humans and your jinn, stood together on one plain and asked of Me, and I gave each one what he asked, that would not decrease what I have, any more than a needle decreases the sea when it is dipped into it. O My servants, it is only your deeds that I record for you and then repay you for. So whoever finds good, let him praise Allah, and whoever finds otherwise, let him blame no one but himself.”"),
    25: ("كل معروف صدقة", "Every good deed is charity", "رواه مسلم", "Muslim",
         "Abu Dharr also reported that some of the Companions of the Messenger of Allah ﷺ said to him: “O Messenger of Allah, the wealthy have taken all the rewards. They pray as we pray and fast as we fast, and they give charity from their surplus wealth.” He said: “Has Allah not given you things you can give in charity? Every Subhanallah is charity, every Allahu akbar is charity, every Alhamdulillah is charity, every La ilaha illallah is charity, enjoining good is charity, forbidding evil is charity, and in the intimacy of one of you with his wife there is charity.” They said: “O Messenger of Allah, does one of us fulfil his desire and get a reward for it?” He said: “Tell me, if he fulfilled it unlawfully, would he not bear a sin? Likewise, if he fulfils it lawfully, he has a reward.”"),
    26: ("صدقة كل سُلامى", "Charity for every joint", "رواه البخاري ومسلم", "Al-Bukhari and Muslim",
         "Abu Hurayrah reported: The Messenger of Allah ﷺ said: “Every joint of a person owes charity on every day the sun rises. Judging justly between two people is charity. Helping a man with his mount, lifting him onto it or lifting his belongings onto it, is charity. A good word is charity. Every step you take to the prayer is charity. Removing harm from the road is charity.”"),
    27: ("البر والإثم", "Righteousness and sin", "رواه مسلم؛ وحديث وابصة رواه أحمد والدارمي بإسناد حسن", "Muslim; Wabisah's narration: Ahmad and ad-Darimi, hasan",
         "An-Nawwas ibn Samʿan reported that the Prophet ﷺ said: “Righteousness is good character, and sin is what wavers in your heart and you dislike people finding out about it.” And Wabisah ibn Maʿbad said: I came to the Messenger of Allah ﷺ and he said: “You have come to ask about righteousness?” I said: “Yes.” He said: “Consult your heart. Righteousness is what the soul and the heart feel at peace with, and sin is what wavers in the soul and stirs uneasily in the chest, even if people give you their verdict again and again.”"),
    28: ("الوصية بالسنة", "Hold to the Sunnah", "رواه أبو داود والترمذي، وقال: حسن صحيح", "Abu Dawud and at-Tirmidhi; hasan sahih",
         "Al-ʿIrbad ibn Sariyah said: The Messenger of Allah ﷺ gave us a sermon that made our hearts tremble and our eyes shed tears. We said: “O Messenger of Allah, it is as if this were a farewell sermon, so advise us.” He said: “I advise you to be mindful of Allah, and to listen and obey even if a slave is made your leader. Whoever of you lives on will see much disagreement, so hold to my Sunnah and the Sunnah of the rightly guided caliphs; bite onto it with your molar teeth. Beware of newly invented matters, for every innovation is misguidance.”"),
    29: ("أبواب الخير", "The gates of goodness", "رواه الترمذي، وقال: حسن صحيح", "At-Tirmidhi; hasan sahih",
         "Muʿadh ibn Jabal said: I said: “O Messenger of Allah, tell me of a deed that will take me into Paradise and keep me away from the Fire.” He said: “You have asked about something great, yet it is easy for whoever Allah makes it easy for: worship Allah without associating anything with Him, establish the prayer, give zakah, fast Ramadan and make pilgrimage to the House.” Then he said: “Shall I not guide you to the gates of goodness? Fasting is a shield; charity extinguishes sin as water extinguishes fire; and a man's prayer in the depth of the night.” Then he recited: “Their sides forsake their beds…” until he reached “…what they used to do.” Then he said: “Shall I not tell you of the head of the matter, its pillar and its peak?” I said: “Yes, O Messenger of Allah.” He said: “The head of the matter is Islam, its pillar is the prayer, and its peak is jihad.” Then he said: “Shall I not tell you what holds all of that together?” I said: “Yes, O Messenger of Allah.” He took hold of his tongue and said: “Restrain this.” I said: “O Prophet of Allah, will we be held to account for what we say?” He said: “May your mother lose you! Is there anything that topples people on their faces (or he said: on their noses) into the Fire other than the harvests of their tongues?”"),
    30: ("حدود الله", "The limits set by Allah", "حديث حسن، رواه الدارقطني وغيره", "Ad-Daraqutni and others; hasan",
         "Abu Thaʿlabah al-Khushani reported that the Messenger of Allah ﷺ said: “Allah the Exalted has laid down obligations, so do not neglect them; He has set limits, so do not overstep them; He has forbidden things, so do not violate them; and He has remained silent about things, out of mercy to you and not out of forgetfulness, so do not go searching into them.”"),
    31: ("الزهد", "True detachment", "حديث حسن، رواه ابن ماجه وغيره", "Ibn Majah and others; hasan",
         "Sahl ibn Saʿd as-Saʿidi said: A man came to the Prophet ﷺ and said: “O Messenger of Allah, guide me to a deed which, if I do it, Allah will love me and people will love me.” He said: “Be detached from the world and Allah will love you; be detached from what people have and people will love you.”"),
    32: ("لا ضرر ولا ضرار", "No harm", "حديث حسن، رواه ابن ماجه والدارقطني وغيرهما", "Ibn Majah, ad-Daraqutni and others; hasan",
         "Abu Saʿid al-Khudri reported that the Messenger of Allah ﷺ said: “There should be no harm and no returning harm.”"),
    33: ("البينة على المدعي", "Proof and oath", "حديث حسن، رواه البيهقي وغيره، وبعضه في الصحيحين", "Al-Bayhaqi and others; hasan, partly in al-Bukhari and Muslim",
         "Ibn ʿAbbas reported that the Messenger of Allah ﷺ said: “If people were given whatever they claimed, men would claim the wealth and lives of others. But the burden of proof is on the claimant, and the oath is on the one who denies.”"),
    34: ("تغيير المنكر", "Changing a wrong", "رواه مسلم", "Muslim",
         "Abu Saʿid al-Khudri said: I heard the Messenger of Allah ﷺ say: “Whoever of you sees a wrong, let him change it with his hand; if he cannot, then with his tongue; and if he cannot, then with his heart, and that is the weakest of faith.”"),
    35: ("أخوّة الإسلام", "Brotherhood", "رواه مسلم", "Muslim",
         "Abu Hurayrah reported: The Messenger of Allah ﷺ said: “Do not envy one another, do not inflate prices against one another, do not hate one another, do not turn your backs on one another, and do not undercut one another's sales. Be, O servants of Allah, brothers. A Muslim is the brother of a Muslim: he does not wrong him, abandon him, lie to him or look down on him. Piety is here,” and he pointed to his chest three times. “It is enough evil for a person to look down on his Muslim brother. All of a Muslim is sacred to another Muslim: his blood, his wealth and his honour.”"),
    36: ("قضاء حوائج الناس", "Helping others", "رواه مسلم", "Muslim",
         "Abu Hurayrah reported that the Prophet ﷺ said: “Whoever relieves a believer of a hardship of this world, Allah will relieve him of a hardship of the Day of Resurrection. Whoever makes things easy for someone in difficulty, Allah will make things easy for him in this world and the Hereafter. Whoever covers a Muslim's faults, Allah will cover his faults in this world and the Hereafter. Allah helps His servant as long as the servant helps his brother. Whoever takes a path seeking knowledge, Allah makes easy for him a path to Paradise. No people gather in one of the houses of Allah, reciting the Book of Allah and studying it together, except that tranquillity descends upon them, mercy covers them, the angels surround them, and Allah mentions them among those with Him. Whoever is held back by his deeds will not be hurried forward by his lineage.”"),
    37: ("كتابة الحسنات والسيئات", "Good and bad deeds", "رواه البخاري ومسلم", "Al-Bukhari and Muslim",
         "Ibn ʿAbbas reported that the Messenger of Allah ﷺ said, in what he narrates from his Lord, Blessed and Exalted: “Allah has written down good deeds and bad deeds, and then made that clear. Whoever intends a good deed but does not do it, Allah writes it with Him as one complete good deed. If he intends it and does it, Allah writes it with Him as ten good deeds, up to seven hundred times, up to many times more. Whoever intends a bad deed but does not do it, Allah writes it with Him as one complete good deed. If he intends it and does it, Allah writes it as one bad deed.”"),
    38: ("التقرّب إلى الله", "Drawing near to Allah", "رواه البخاري", "Al-Bukhari",
         "Abu Hurayrah reported: The Messenger of Allah ﷺ said: “Allah the Exalted said: Whoever shows hostility to a friend of Mine, I declare war on him. My servant does not draw near to Me with anything more beloved to Me than what I have made obligatory upon him. My servant keeps drawing near to Me with voluntary deeds until I love him. When I love him, I become his hearing with which he hears, his sight with which he sees, his hand with which he strikes and his foot with which he walks. If he asks Me, I will surely give him, and if he seeks refuge in Me, I will surely grant him refuge.”"),
    39: ("رفع الحرج", "Mistakes are pardoned", "حديث حسن، رواه ابن ماجه والبيهقي", "Ibn Majah and al-Bayhaqi; hasan",
         "Ibn ʿAbbas reported that the Messenger of Allah ﷺ said: “Allah has pardoned my nation for mistakes, forgetfulness and what they are forced to do.”"),
    40: ("كن في الدنيا كأنك غريب", "A traveller in this world", "رواه البخاري", "Al-Bukhari",
         "Ibn ʿUmar said: The Messenger of Allah ﷺ took me by the shoulder and said: “Be in this world as if you were a stranger or a traveller passing through.” And Ibn ʿUmar used to say: “When evening comes, do not wait for the morning, and when morning comes, do not wait for the evening. Take from your health for your illness, and from your life for your death.”"),
    41: ("اتباع الهدي النبوي", "Following the Prophet", "قال النووي: حديث حسن صحيح، رويناه في كتاب الحجة", "An-Nawawi: hasan sahih, from Kitab al-Hujjah",
         "ʿAbdullah ibn ʿAmr ibn al-ʿAs reported: The Messenger of Allah ﷺ said: “None of you truly believes until his desires follow what I have brought.”"),
    42: ("سعة مغفرة الله", "The breadth of forgiveness", "رواه الترمذي، وقال: حسن صحيح", "At-Tirmidhi; hasan sahih",
         "Anas ibn Malik said: I heard the Messenger of Allah ﷺ say: “Allah the Exalted said: O son of Adam, as long as you call on Me and hope in Me, I will forgive you for whatever you have done, and I do not mind. O son of Adam, if your sins reached the clouds of the sky and then you asked My forgiveness, I would forgive you. O son of Adam, if you came to Me with sins nearly filling the earth and then met Me without associating anything with Me, I would come to you with forgiveness nearly filling it.”"),
}

# Text that continues after the first "رواه" in the source and should stay (other narrations).
KEEP_TAIL = {
    5: "وَفِي رِوَايَةٍ لِمُسْلِمٍ: مَنْ عَمِلَ عَمَلًا لَيْسَ عَلَيْهِ أَمْرُنَا فَهُوَ رَدٌّ",
}

data = json.load(open(SRC, encoding="utf-8"))
out = []
for h in data["hadiths"]:
    n = h["hadithnumber"]
    text = h["text"].replace("<br>", " ")
    text = text.replace("‫", "").replace("‬", "")
    if n == 27:
        text = text.replace("رَوَاهُ مُسْلِمٌ [رَوَاهُ مُسْلِمٌ].", "")
    if n == 14:
        text = text.replace("[ يشهد أن لا إله إلا الله، وأني رسول الله]", "يَشْهَدُ أَنْ لَا إلَهَ إلَّا اللَّهُ وَأَنِّي رَسُولُ اللَّهِ")
    if n == 19:
        # keep the second narration, drop only the attribution sentence between them
        text = re.sub(r"\s*رَوَاهُ التِّرْمِذِيُّ \[رقم:2516\] وَقَالَ: حَدِيثٌ حَسَنٌ صَحِيحٌ\.\s*", " ", text)
        cut = None
    else:
        m = re.search(r"\s*(\[?رَوَاهُ|حَدِيثٌ حَسَنٌ|حديث حسن|رَوَيْنَاهُ)", text)
        cut = m.start() if m else None
    if cut is not None:
        text = text[:cut]
    if n in KEEP_TAIL:
        text = text.rstrip(" .") + ". " + KEEP_TAIL[n]
    text = re.sub(r"صلى الله عليه و ?سلم", "ﷺ", text)
    text = re.sub(r"\s*\[[^\]]*سورة[^\]]*\]\s*", " ", text)
    text = re.sub(r"\s+([.،؛؟!])", r"", text)
    text = re.sub(r"\s+", " ", text).strip().rstrip("،,")
    if not text.endswith((".", "!", "؟", "”", "\"")):
        text += "."
    title_ar, title_en, src_ar, src_en, english = H[n]
    out.append({
        "number": n,
        "title": {"ar": title_ar, "en": title_en},
        "arabic": text,
        "english": english,
        "source": {"ar": src_ar, "en": src_en},
    })

assert len(out) == 42 and [h["number"] for h in out] == list(range(1, 43))
doc = {
    "version": 1,
    "collection": {"ar": "الأربعون النووية", "en": "An-Nawawi's Forty Hadith"},
    "license": "Arabic: classical text (public domain), via fawazahmed0/hadith-api (Unlicense). English, titles and source lines: written for Deen (GPL-3.0-or-later).",
    "hadiths": out,
}
json.dump(doc, open(OUT, "w", encoding="utf-8", newline="\n"), ensure_ascii=False, indent=1)
for h in out[:3] + [out[4], out[18], out[26]]:
    print(h["number"], h["arabic"][-120:])
