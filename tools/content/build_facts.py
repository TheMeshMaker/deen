"""Builds core/content/src/main/assets/prophet-facts.json: short facts about the Prophet ﷺ, each from a hadith.

Text (Arabic and English) written for Deen. Every fact names its hadith; the build checks that a key phrase of
that hadith really is in the cited collection (and, where the numbering allows, at the cited number).

Usage: python -I build_facts.py <hadith dir> <out json>
"""
import json
import re
import sys

sys.stdout.reconfigure(encoding="utf-8")
HDIR, OUT = sys.argv[1:3]

# (Arabic, English, source ar, source en, collection, number or None (presence only), probe phrase)
FACTS = [
    ("وُلد النبي ﷺ في مكة عام الفيل.", "The Prophet ﷺ was born in Makkah in the Year of the Elephant.",
     "الترمذي (3619)", "At-Tirmidhi 3619", "tirmidhi", 3619, "عام الفيل"),
    ("كان ﷺ يصوم يوم الاثنين، وقال: «ذاك يوم وُلدت فيه، ويوم بُعثت أو أُنزل عليّ فيه».",
     "He ﷺ fasted on Mondays and said: “That is the day I was born, and the day I was sent (or revelation came to me).”",
     "مسلم (1162)", "Muslim 1162", "muslim", None, "ذاك يوم ولدت فيه"),
    ("رعى ﷺ الغنم لأهل مكة على قراريط، وقال: «ما بعث الله نبيًّا إلا رعى الغنم».",
     "He ﷺ tended sheep for the people of Makkah for a few qirats, and said: “Allah never sent a prophet who did not tend sheep.”",
     "البخاري (2262)", "Al-Bukhari 2262", "bukhari", 2262, "ما بعث الله نبيا الا رعى الغنم"),
    ("كان ﷺ قبل البعثة يخلو بغار حراء يتعبّد الليالي، وفيه نزل عليه أول الوحي: ﴿اقرأ باسم ربك الذي خلق﴾.",
     "Before prophethood he ﷺ would spend nights in worship in the cave of Hira, and there the first revelation came: “Read in the name of your Lord who created.”",
     "البخاري (3)", "Al-Bukhari 3", "bukhari", 3, "يخلو بغار حراء"),
    ("لما نزل الوحي على النبي ﷺ أول مرة طمأنته خديجة رضي الله عنها: «كلا والله ما يخزيك الله أبدًا، إنك لتصل الرحم، وتحمل الكَلّ، وتُكسب المعدوم، وتقري الضيف».",
     "When the first revelation shook him ﷺ, Khadijah comforted him: “By Allah, Allah will never disgrace you. You keep family ties, carry the burdens of the weak, help the destitute and honour your guests.”",
     "البخاري (3)", "Al-Bukhari 3", "bukhari", 3, "ما يخزيك الله ابدا"),
    ("بُعث ﷺ وهو ابن أربعين سنة، فمكث بمكة ثلاث عشرة سنة، ثم بالمدينة عشر سنين، وتوفي وهو ابن ثلاث وستين.",
     "He ﷺ was sent as a prophet at forty, stayed thirteen years in Makkah and ten in Madinah, and passed away at sixty-three.",
     "البخاري (3902)", "Al-Bukhari 3902", "bukhari", 3902, "فمكث بمكه ثلاث عشره سنه"),
    ("سُئلت عائشة رضي الله عنها عن خلقه ﷺ فقالت: «كان خلقه القرآن».",
     "Aishah was asked about his character ﷺ. She said: “His character was the Quran.”",
     "مسلم (746)", "Muslim 746", "muslim", None, "كان القران"),
    ("كان ﷺ في بيته في خدمة أهله، فإذا حضرت الصلاة خرج إلى الصلاة.",
     "At home he ﷺ helped his family with their work, and when the time for prayer came he went out to pray.",
     "البخاري (676)", "Al-Bukhari 676", "bukhari", 676, "في مهنه اهله"),
    ("ما ضرب ﷺ بيده شيئًا قط، لا امرأة ولا خادمًا، إلا أن يجاهد في سبيل الله.",
     "He ﷺ never struck anyone with his hand, neither a woman nor a servant, except when fighting in the cause of Allah.",
     "مسلم (2328)", "Muslim 2328", "muslim", None, "ما ضرب رسول الله"),
    ("ما خُيّر ﷺ بين أمرين إلا أخذ أيسرهما ما لم يكن إثمًا، وما انتقم لنفسه قط.",
     "Whenever he ﷺ had to choose between two things, he chose the easier, as long as it was not sinful, and he never took revenge for himself.",
     "البخاري (3560)", "Al-Bukhari 3560", "bukhari", 3560, "الا اخذ ايسرهما"),
    ("ما عاب ﷺ طعامًا قط؛ إن اشتهاه أكله، وإلا تركه.",
     "He ﷺ never criticised food: if he liked it he ate it, and if not, he left it.",
     "البخاري (3563)", "Al-Bukhari 3563", "bukhari", 3563, "ما عاب النبي"),
    ("كان ﷺ أجود الناس، وكان أجود ما يكون في رمضان حين يلقاه جبريل فيدارسه القرآن.",
     "He ﷺ was the most generous of people, and most generous of all in Ramadan, when Jibril met him to review the Quran.",
     "البخاري (6)", "Al-Bukhari 6", "bukhari", 6, "اجود الناس"),
    ("خدم أنسٌ رضي الله عنه النبيَّ ﷺ عشر سنين، فما قال له: أفٍّ قط، ولا: لِمَ صنعت؟",
     "Anas served him ﷺ for ten years, and he never once said “uff” to him, nor asked “Why did you do that?”",
     "البخاري (6038)", "Al-Bukhari 6038", "bukhari", 6038, "فما قال لي اف"),
    ("قال عبد الله بن الحارث رضي الله عنه: «ما رأيت أحدًا أكثر تبسّمًا من رسول الله ﷺ».",
     "ʿAbdullah ibn al-Harith said: “I never saw anyone smile more than the Messenger of Allah ﷺ.”",
     "الترمذي (3641)", "At-Tirmidhi 3641", "tirmidhi", 3641, "اكثر تبسما"),
    ("كان ﷺ يستغفر الله ويتوب إليه في اليوم أكثر من سبعين مرة.",
     "He ﷺ asked Allah's forgiveness and turned to Him more than seventy times a day.",
     "البخاري (6307)", "Al-Bukhari 6307", "bukhari", 6307, "اكثر من سبعين مره"),
    ("كان ﷺ يقوم الليل حتى تتفطّر قدماه، فلما سُئل قال: «أفلا أكون عبدًا شكورًا؟».",
     "He ﷺ prayed at night until his feet cracked. When asked why, he said: “Should I not be a grateful servant?”",
     "البخاري (4837)، ومسلم (2820)", "Al-Bukhari 4837, Muslim 2820", "bukhari", None, "افلا احب ان اكون عبدا شكورا"),
    ("كان ﷺ يحب التيمّن في تنعّله وترجّله وطهوره، وفي شأنه كله.",
     "He ﷺ liked to start with the right when putting on his sandals, combing his hair and purifying himself, and in all his affairs.",
     "البخاري (168)", "Al-Bukhari 168", "bukhari", 168, "يعجبه التيمن"),
    ("كان ﷺ أشدّ حياءً من العذراء في خِدرها، فإذا رأى شيئًا يكرهه عرفناه في وجهه.",
     "He ﷺ was more modest than a young girl behind her curtain; when he disliked something, it showed on his face.",
     "البخاري (3562)", "Al-Bukhari 3562", "bukhari", 3562, "اشد حياء من العذراء"),
    ("نام ﷺ على حصير أثّر في جنبه، فقال: «ما لي وللدنيا! ما أنا في الدنيا إلا كراكب استظلّ تحت شجرة ثم راح وتركها».",
     "He ﷺ slept on a mat that left marks on his side, and said: “What have I to do with this world? I am in this world like a rider who rests in the shade of a tree, then goes on and leaves it.”",
     "الترمذي (2377)", "At-Tirmidhi 2377", "tirmidhi", 2377, "كراكب استظل تحت شجره"),
    ("قالت عائشة رضي الله عنها: كان يمرّ الهلال ثم الهلال ثم الهلال، ثلاثة أهلّة في شهرين، وما أوقدت في أبيات رسول الله ﷺ نار، وإنما هما الأسودان: التمر والماء.",
     "Aishah said three new moons could pass in two months without a fire being lit in the Prophet's houses ﷺ; they lived on the two black things: dates and water.",
     "البخاري (2567)", "Al-Bukhari 2567", "bukhari", 2567, "التمر والماء"),
    ("كان ﷺ أحسن الناس وجهًا وأحسنهم خَلقًا، ليس بالطويل البائن ولا بالقصير.",
     "He ﷺ had the most handsome face and the finest build, neither very tall nor short.",
     "البخاري (3549)", "Al-Bukhari 3549", "bukhari", 3549, "ليس بالطويل الباين ولا بالقصير"),
    ("سُئل البراء رضي الله عنه: أكان وجه النبي ﷺ مثل السيف؟ قال: «لا، بل مثل القمر».",
     "Al-Bara was asked: “Was the Prophet's face ﷺ like a sword?” He said: “No, it was like the moon.”",
     "البخاري (3552)", "Al-Bukhari 3552", "bukhari", 3552, "بل مثل القمر"),
    ("كان ﷺ يمازح أصحابه، وقال: «إني لا أقول إلا حقًّا».",
     "He ﷺ joked with his companions, and said: “I only ever say what is true.”",
     "الترمذي (1990)", "At-Tirmidhi 1990", "tirmidhi", 1990, "اني لا اقول الا حقا"),
    ("سابق ﷺ زوجته عائشة رضي الله عنها فسبقته، ثم سابقها بعد أن حملت اللحم فسبقها، فقال: «هذه بتلك السبقة».",
     "He ﷺ raced his wife Aishah on foot and she won. Years later they raced again and he won, and said: “This one is for that one.”",
     "أبو داود (2578)", "Abu Dawud 2578", "abudawud", 2578, "هذه بتلك السبقه"),
    ("كان ﷺ إذا مرّ على الصبيان سلّم عليهم.",
     "When he ﷺ passed by children, he greeted them with salam.",
     "البخاري (6247)", "Al-Bukhari 6247", "bukhari", 6247, "مر على صبيان فسلم عليهم"),
    ("صلّى ﷺ وهو حامل حفيدته أُمامة، فإذا سجد وضعها وإذا قام حملها.",
     "He ﷺ prayed while carrying his granddaughter Umamah, putting her down when he prostrated and picking her up when he stood.",
     "البخاري (516)", "Al-Bukhari 516", "bukhari", 516, "حامل امامه"),
    ("كان ﷺ يدخل في الصلاة يريد إطالتها، فيسمع بكاء الصبي فيتجوّز رحمةً بأمه.",
     "He ﷺ would begin a prayer meaning to make it long, then shorten it on hearing a baby cry, out of mercy for its mother.",
     "البخاري (707)", "Al-Bukhari 707", "bukhari", 707, "فاسمع بكاء الصبي"),
    ("لما مات ابنه إبراهيم بكى ﷺ وقال: «إن العين تدمع، والقلب يحزن، ولا نقول إلا ما يُرضي ربنا».",
     "When his infant son Ibrahim died, he ﷺ wept and said: “The eyes shed tears and the heart grieves, but we say only what pleases our Lord.”",
     "البخاري (1303)", "Al-Bukhari 1303", "bukhari", 1303, "ان العين تدمع"),
    ("كان ﷺ يقبل الهدية ويُثيب عليها.",
     "He ﷺ accepted gifts and always gave something in return.",
     "البخاري (2585)", "Al-Bukhari 2585", "bukhari", 2585, "يقبل الهديه ويثيب عليها"),
    ("مرض غلام يهودي كان يخدمه ﷺ، فأتاه النبي ﷺ يعوده.",
     "When a Jewish boy who served him fell ill, he ﷺ went to visit him.",
     "البخاري (1356)", "Al-Bukhari 1356", "bukhari", 1356, "غلام يهودي يخدم النبي"),
    ("مرّت به ﷺ جنازة فقام لها، فقيل: إنها جنازة يهودي، فقال: «أليست نفسًا؟».",
     "A funeral passed and he ﷺ stood up for it. Told that it was a Jew's funeral, he said: “Was it not a soul?”",
     "البخاري (1312)", "Al-Bukhari 1312", "bukhari", 1312, "اليست نفسا"),
    ("كان ﷺ يأكل بثلاث أصابع، ويلعقها قبل أن يمسحها.",
     "He ﷺ ate with three fingers and licked them before wiping them.",
     "مسلم (2032)", "Muslim 2032", "muslim", None, "ياكل بثلاث اصابع"),
    ("كان ﷺ يتنفّس في الشراب ثلاثًا.",
     "He ﷺ drank in three breaths, taking the cup away from his mouth between them.",
     "مسلم (2028)", "Muslim 2028", "muslim", None, "يتنفس في الشراب ثلاثا"),
    ("كان ﷺ يحب الحلواء والعسل.",
     "He ﷺ loved sweet things and honey.",
     "البخاري (5431)", "Al-Bukhari 5431", "bukhari", 5431, "يحب الحلواء والعسل"),
    ("قال ﷺ: «لولا أن أشقّ على أمتي لأمرتهم بالسواك مع كل صلاة».",
     "He ﷺ said: “Were it not that it would be hard on my nation, I would have commanded them to use the siwak with every prayer.”",
     "البخاري (887)", "Al-Bukhari 887", "bukhari", 887, "بالسواك مع كل صلاه"),
    ("اتخذ ﷺ خاتمًا من فضة نقشه «محمد رسول الله»، يختم به كتبه إلى الملوك.",
     "He ﷺ had a silver ring engraved “Muhammad Rasul Allah”, which he used to seal his letters to kings.",
     "البخاري (5872)", "Al-Bukhari 5872", "bukhari", 5872, "محمد رسول الله"),
    ("حجّ ﷺ بعد الهجرة حجة واحدة، هي حجة الوداع.",
     "After the Hijrah he ﷺ performed Hajj only once: the Farewell Pilgrimage.",
     "البخاري (4404)", "Al-Bukhari 4404", "bukhari", 4404, "حجه واحده"),
    ("في الغار يوم الهجرة قال ﷺ لأبي بكر رضي الله عنه: «ما ظنك يا أبا بكر باثنين الله ثالثهما؟».",
     "In the cave during the Hijrah he ﷺ said to Abu Bakr: “What do you think, Abu Bakr, of two when Allah is their third?”",
     "البخاري (3653)", "Al-Bukhari 3653", "bukhari", 3653, "باثنين الله ثالثهما"),
    ("قال ﷺ: «خيركم خيركم لأهله، وأنا خيركم لأهلي».",
     "He ﷺ said: “The best of you are the best to their families, and I am the best of you to my family.”",
     "الترمذي (3895)", "At-Tirmidhi 3895", "tirmidhi", 3895, "وانا خيركم لاهلي"),
    ("ما سُئل ﷺ عن شيء قط فقال: لا.",
     "He ﷺ was never asked for anything and answered “No.”",
     "البخاري (6034)", "Al-Bukhari 6034", "bukhari", 6034, "عن شيء قط فقال لا"),
    ("ما كان ﷺ يزيد في رمضان ولا في غيره على إحدى عشرة ركعة، يصلّيهن بحُسن وطول.",
     "In Ramadan and outside it, his night prayer ﷺ did not go beyond eleven rak'ahs, prayed beautifully and at length.",
     "البخاري (1147)", "Al-Bukhari 1147", "bukhari", 1147, "على احدي عشره ركعه"),
    ("مثّل ﷺ الأنبياء ببيت جميل ينقصه موضع لبنة، وقال: «فأنا اللبنة، وأنا خاتم النبيين».",
     "He ﷺ likened the prophets to a beautiful house missing one brick, and said: “I am that brick, and I am the last of the prophets.”",
     "البخاري (3535)", "Al-Bukhari 3535", "bukhari", 3535, "وانا خاتم النبيين"),
    ("حمل ﷺ التراب بنفسه يوم الخندق حتى وارى التراب بطنه.",
     "At the Battle of the Trench he ﷺ carried earth himself until the dust covered his stomach.",
     "البخاري (2837)", "Al-Bukhari 2837", "bukhari", 2837, "ينقل التراب"),
    ("لما بُني مسجده ﷺ في المدينة كان ينقل الحجارة مع أصحابه.",
     "When his mosque in Madinah was built, he ﷺ carried stones alongside his companions.",
     "البخاري (428)", "Al-Bukhari 428", "bukhari", 428, "ينقلون الصخر"),
    ("لما آذاه أهل الطائف لم يدعُ عليهم ﷺ، وقال: «بل أرجو أن يُخرج الله من أصلابهم من يعبد الله وحده لا يشرك به شيئًا».",
     "After the people of Ta'if drove him away, he ﷺ did not ask for their destruction, but said: “Rather, I hope Allah will bring from their descendants people who worship Allah alone, associating nothing with Him.”",
     "البخاري (3231)", "Al-Bukhari 3231", "bukhari", 3231, "بل ارجو ان يخرج الله من اصلابهم"),
    ("كان ﷺ يوصي: «يسّروا ولا تعسّروا، وبشّروا ولا تنفّروا».",
     "He ﷺ taught: “Make things easy and do not make them difficult; give good news and do not drive people away.”",
     "البخاري (69)", "Al-Bukhari 69", "bukhari", 69, "يسروا ولا تعسروا"),
    ("فُرضت الصلوات الخمس على النبي ﷺ ليلة الإسراء والمعراج، فُرضت خمسين ثم خُفّفت إلى خمس.",
     "The daily prayers were given to him ﷺ on the night of the Ascension (al-Miʿraj): first fifty, then lightened to five.",
     "البخاري (349)", "Al-Bukhari 349", "bukhari", 349, "ففرض الله على امتي خمسين صلاه"),
    ("قال أنس رضي الله عنه: «كان النبي ﷺ أحسن الناس خُلُقًا».",
     "Anas said: “The Prophet ﷺ was the best of people in character.”",
     "البخاري (6203)", "Al-Bukhari 6203", "bukhari", 6203, "احسن الناس خلقا"),
    ("قبّل ﷺ حفيده الحسن، فقال رجل: إن لي عشرة من الولد ما قبّلت منهم أحدًا، فقال ﷺ: «من لا يَرحم لا يُرحم».",
     "He ﷺ kissed his grandson al-Hasan. A man said he had ten children and had never kissed one. The Prophet ﷺ said: “Whoever does not show mercy will not be shown mercy.”",
     "البخاري (5997)", "Al-Bukhari 5997", "bukhari", 5997, "من لا يرحم لا يرحم"),
    ("رأى ﷺ جملًا يبكي فمسح عليه، وقال لصاحبه: «أفلا تتقي الله في هذه البهيمة التي ملّكك الله إياها؟».",
     "He ﷺ saw a camel weeping and stroked it, then told its owner: “Do you not fear Allah regarding this animal that Allah has placed in your care?”",
     "أبو داود (2549)", "Abu Dawud 2549", "abudawud", 2549, "افلا تتقي الله في هذه البهيمه"),
    ("قال ﷺ عن الأنصار الذين آووه ونصروه: «لا يحبهم إلا مؤمن، ولا يبغضهم إلا منافق».",
     "He ﷺ said of the Ansar, who sheltered and supported him: “Only a believer loves them, and only a hypocrite hates them.”",
     "البخاري (3783)", "Al-Bukhari 3783", "bukhari", 3783, "لا يحبهم الا مومن"),
    ("صلّى معه حذيفة رضي الله عنه ليلة، فقرأ ﷺ في ركعة واحدة البقرة ثم النساء ثم آل عمران، مترسّلًا يسبّح ويسأل.",
     "Hudhayfah prayed with him ﷺ one night; in a single rak'ah he recited al-Baqarah, then an-Nisa', then Al ʿImran, slowly, glorifying Allah and asking of Him as he went.",
     "مسلم (772)", "Muslim 772", "muslim", None, "فافتتح البقره"),
    ("كان ﷺ يجلس بين أصحابه، فيجيء الغريب فلا يدري أيّهم هو حتى يسأل.",
     "He ﷺ sat among his companions so simply that a stranger arriving could not tell which of them he was until he asked.",
     "أبو داود (4698)", "Abu Dawud 4698", "abudawud", 4698, "فيجيء الغريب فلا يدري ايهم هو"),
    ("كان ﷺ إذا أخذ مضجعه من الليل وضع يده تحت خده، ثم قال: «باسمك اللهم أموت وأحيا».",
     "When he ﷺ lay down at night, he put his hand under his cheek and said: “In Your name, O Allah, I die and I live.”",
     "البخاري (6314)", "Al-Bukhari 6314", "bukhari", 6314, "وضع يده تحت خده"),
    ("كان ﷺ يصوم الاثنين والخميس، وقال: «تُعرض الأعمال يوم الاثنين والخميس، فأحب أن يُعرض عملي وأنا صائم».",
     "He ﷺ fasted on Mondays and Thursdays, saying: “Deeds are presented on Monday and Thursday, and I love for my deeds to be presented while I am fasting.”",
     "الترمذي (747)", "At-Tirmidhi 747", "tirmidhi", 747, "تعرض الاعمال يوم الاثنين والخميس"),
    ("كان ﷺ يأتي مسجد قباء كل سبت، ماشيًا وراكبًا.",
     "He ﷺ visited the Mosque of Quba every Saturday, walking or riding.",
     "البخاري (1193)", "Al-Bukhari 1193", "bukhari", 1193, "ياتي مسجد قباء كل سبت"),
    ("كان ﷺ إذا قدم من سفر ضحًى دخل المسجد فصلّى ركعتين قبل أن يجلس.",
     "Returning from a journey in the morning, he ﷺ went into the mosque and prayed two rak'ahs before sitting down.",
     "البخاري (3088)", "Al-Bukhari 3088", "bukhari", 3088, "دخل المسجد فصلى ركعتين قبل ان يجلس"),
    ("كان ﷺ إذا قدم من سفر فنظر إلى جُدُرات المدينة أسرع براحلته، من حبّها.",
     "Coming home from a journey, when he ﷺ saw the walls of Madinah he urged his mount on, out of love for the city.",
     "البخاري (1886)", "Al-Bukhari 1886", "bukhari", 1886, "حركها من حبها"),
    ("كان ﷺ يحب أن يخرج للسفر يوم الخميس.",
     "He ﷺ liked to set out on journeys on Thursdays.",
     "البخاري (2950)", "Al-Bukhari 2950", "bukhari", 2950, "يحب ان يخرج يوم الخميس"),
    ("كان ﷺ أحسن الناس وأجود الناس وأشجع الناس؛ فزع أهل المدينة ليلة فسبقهم إلى الصوت على فرس، ورجع يقول: «لن تُراعوا».",
     "He ﷺ was the best, most generous and bravest of people. One night Madinah was alarmed by a noise; he rode out ahead of everyone to check, and came back saying: “Do not be afraid.”",
     "البخاري (2908)", "Al-Bukhari 2908", "bukhari", 2908, "واشجع الناس"),
    ("توفي ﷺ يوم الاثنين.",
     "He ﷺ passed away on a Monday.",
     "البخاري (1387)", "Al-Bukhari 1387", "bukhari", 1387, "يوم الاثنين"),
]

TASHKEEL = re.compile(r"[ؐ-ًؚ-ٰٟۖ-ۭـ]")


def norm(text):
    text = TASHKEEL.sub("", text)
    text = re.sub("[أإآٱ]", "ا", text).replace("ى", "ي").replace("ة", "ه").replace("ؤ", "و").replace("ئ", "ي")
    text = re.sub(r"[^ء-ي ]", " ", text)
    return re.sub(r"\s+", " ", text).strip()


cache = {}


def hadiths(coll):
    if coll not in cache:
        cache[coll] = json.load(open(f"{HDIR}/ara-{coll}.json", encoding="utf-8"))["hadiths"]
    return cache[coll]


problems = 0
for i, (ar, en, src_ar, src_en, coll, number, probe) in enumerate(FACTS, 1):
    key = norm(probe)
    hits = [h for h in hadiths(coll) if key in norm(h["text"])]
    numbers = sorted({h.get("arabicnumber", h["hadithnumber"]) for h in hits})
    if not hits:
        problems += 1
        print(f"#{i} NOT FOUND in {coll}: {probe}")
    elif number is not None and number not in numbers:
        problems += 1
        print(f"#{i} {coll} {number}: phrase found at {numbers[:12]} instead")

out = [{"id": i, "text": {"ar": ar, "en": en}, "source": {"ar": src_ar, "en": src_en}}
       for i, (ar, en, src_ar, src_en, *_rest) in enumerate(FACTS, 1)]
doc = {
    "version": 1,
    "license": "Written for Deen (GPL-3.0-or-later) from the cited hadith.",
    "facts": out,
}
json.dump(doc, open(OUT, "w", encoding="utf-8", newline="\n"), ensure_ascii=False, indent=1)
print("facts:", len(out), "problems:", problems)
