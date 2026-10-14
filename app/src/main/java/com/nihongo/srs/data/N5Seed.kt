package com.nihongo.srs.data

import com.nihongo.srs.data.VocabEntity.Companion.TYPE_PHRASE

/**
 * Starter content bundled inside the app (no internet needed).
 *
 * - `words`: common JLPT N5 vocabulary with English + Bengali meanings.
 * - `phrases`: everyday sentences for the Speaking Practice screen.
 *
 * Format of w(): japanese, kana reading, romaji, English, বাংলা
 */
object N5Seed {

    private fun w(jp: String, kana: String, romaji: String, en: String, bn: String) =
        VocabEntity(
            japanese = jp, reading = kana, romaji = romaji,
            meaning = en, meaningBn = bn, level = "N5"
        )

    private fun p(jp: String, kana: String, romaji: String, en: String, bn: String) =
        w(jp, kana, romaji, en, bn).copy(type = TYPE_PHRASE, level = "Speaking")

    val words: List<VocabEntity> = listOf(
        // --- People / মানুষ ---
        w("私", "わたし", "watashi", "I, me", "আমি"),
        w("あなた", "あなた", "anata", "you", "তুমি / আপনি"),
        w("人", "ひと", "hito", "person", "মানুষ / লোক"),
        w("友達", "ともだち", "tomodachi", "friend", "বন্ধু"),
        w("先生", "せんせい", "sensei", "teacher", "শিক্ষক"),
        w("学生", "がくせい", "gakusei", "student", "ছাত্র / ছাত্রী"),
        w("家族", "かぞく", "kazoku", "family", "পরিবার"),
        w("父", "ちち", "chichi", "(my) father", "(আমার) বাবা"),
        w("母", "はは", "haha", "(my) mother", "(আমার) মা"),
        w("子供", "こども", "kodomo", "child", "শিশু / বাচ্চা"),
        w("男", "おとこ", "otoko", "man, male", "পুরুষ / ছেলে"),
        w("女", "おんな", "onna", "woman, female", "মহিলা / মেয়ে"),

        // --- Numbers / সংখ্যা ---
        w("一", "いち", "ichi", "one", "এক"),
        w("二", "に", "ni", "two", "দুই"),
        w("三", "さん", "san", "three", "তিন"),
        w("四", "よん", "yon", "four", "চার"),
        w("五", "ご", "go", "five", "পাঁচ"),
        w("六", "ろく", "roku", "six", "ছয়"),
        w("七", "なな", "nana", "seven", "সাত"),
        w("八", "はち", "hachi", "eight", "আট"),
        w("九", "きゅう", "kyuu", "nine", "নয়"),
        w("十", "じゅう", "juu", "ten", "দশ"),
        w("百", "ひゃく", "hyaku", "hundred", "একশো"),
        w("千", "せん", "sen", "thousand", "হাজার"),
        w("円", "えん", "en", "yen (Japanese money)", "ইয়েন (জাপানি টাকা)"),

        // --- Time / সময় ---
        w("今日", "きょう", "kyou", "today", "আজ"),
        w("明日", "あした", "ashita", "tomorrow", "আগামীকাল"),
        w("昨日", "きのう", "kinou", "yesterday", "গতকাল"),
        w("今", "いま", "ima", "now", "এখন"),
        w("朝", "あさ", "asa", "morning", "সকাল"),
        w("昼", "ひる", "hiru", "noon, daytime", "দুপুর"),
        w("夜", "よる", "yoru", "night", "রাত"),
        w("時間", "じかん", "jikan", "time, hour", "সময় / ঘণ্টা"),
        w("毎日", "まいにち", "mainichi", "every day", "প্রতিদিন"),
        w("週", "しゅう", "shuu", "week", "সপ্তাহ"),
        w("月", "つき", "tsuki", "moon, month", "চাঁদ / মাস"),
        w("年", "とし", "toshi", "year", "বছর"),

        // --- Places / জায়গা ---
        w("家", "いえ", "ie", "house, home", "বাড়ি / ঘর"),
        w("学校", "がっこう", "gakkou", "school", "স্কুল / বিদ্যালয়"),
        w("会社", "かいしゃ", "kaisha", "company", "কোম্পানি / অফিস"),
        w("駅", "えき", "eki", "train station", "রেলস্টেশন"),
        w("店", "みせ", "mise", "shop, store", "দোকান"),
        w("病院", "びょういん", "byouin", "hospital", "হাসপাতাল"),
        w("国", "くに", "kuni", "country", "দেশ"),
        w("日本", "にほん", "nihon", "Japan", "জাপান"),
        w("トイレ", "トイレ", "toire", "toilet", "টয়লেট / বাথরুম"),
        w("部屋", "へや", "heya", "room", "রুম / কক্ষ"),
        w("道", "みち", "michi", "road, way", "রাস্তা / পথ"),

        // --- Food & things / খাবার ও জিনিস ---
        w("水", "みず", "mizu", "water", "পানি / জল"),
        w("ご飯", "ごはん", "gohan", "rice, meal", "ভাত / খাবার"),
        w("お茶", "おちゃ", "ocha", "tea (green tea)", "চা (সবুজ চা)"),
        w("肉", "にく", "niku", "meat", "মাংস"),
        w("魚", "さかな", "sakana", "fish", "মাছ"),
        w("野菜", "やさい", "yasai", "vegetable", "সবজি"),
        w("卵", "たまご", "tamago", "egg", "ডিম"),
        w("パン", "パン", "pan", "bread", "রুটি / পাউরুটি"),
        w("本", "ほん", "hon", "book", "বই"),
        w("電話", "でんわ", "denwa", "telephone", "ফোন"),
        w("車", "くるま", "kuruma", "car", "গাড়ি"),
        w("電車", "でんしゃ", "densha", "train", "ট্রেন"),
        w("お金", "おかね", "okane", "money", "টাকা / অর্থ"),
        w("傘", "かさ", "kasa", "umbrella", "ছাতা"),
        w("雨", "あめ", "ame", "rain", "বৃষ্টি"),
        w("天気", "てんき", "tenki", "weather", "আবহাওয়া"),
        w("名前", "なまえ", "namae", "name", "নাম"),
        w("日本語", "にほんご", "nihongo", "Japanese language", "জাপানি ভাষা"),

        // --- Verbs / ক্রিয়া ---
        w("食べる", "たべる", "taberu", "to eat", "খাওয়া"),
        w("飲む", "のむ", "nomu", "to drink", "পান করা"),
        w("行く", "いく", "iku", "to go", "যাওয়া"),
        w("来る", "くる", "kuru", "to come", "আসা"),
        w("帰る", "かえる", "kaeru", "to go home, return", "বাড়ি ফেরা"),
        w("見る", "みる", "miru", "to see, watch", "দেখা"),
        w("聞く", "きく", "kiku", "to listen, ask", "শোনা / জিজ্ঞেস করা"),
        w("話す", "はなす", "hanasu", "to speak", "কথা বলা"),
        w("読む", "よむ", "yomu", "to read", "পড়া"),
        w("書く", "かく", "kaku", "to write", "লেখা"),
        w("買う", "かう", "kau", "to buy", "কেনা"),
        w("待つ", "まつ", "matsu", "to wait", "অপেক্ষা করা"),
        w("寝る", "ねる", "neru", "to sleep", "ঘুমানো"),
        w("起きる", "おきる", "okiru", "to wake up", "ঘুম থেকে ওঠা"),
        w("働く", "はたらく", "hataraku", "to work", "কাজ করা"),
        w("勉強する", "べんきょうする", "benkyou suru", "to study", "পড়াশোনা করা"),
        w("分かる", "わかる", "wakaru", "to understand", "বোঝা"),
        w("ある", "ある", "aru", "to exist (things)", "আছে (জিনিস)"),
        w("いる", "いる", "iru", "to exist (people, animals)", "আছে (মানুষ/প্রাণী)"),
        w("する", "する", "suru", "to do", "করা"),

        // --- Adjectives / বিশেষণ ---
        w("大きい", "おおきい", "ookii", "big", "বড়"),
        w("小さい", "ちいさい", "chiisai", "small", "ছোট"),
        w("新しい", "あたらしい", "atarashii", "new", "নতুন"),
        w("古い", "ふるい", "furui", "old (things)", "পুরানো"),
        w("高い", "たかい", "takai", "expensive, tall", "দামি / উঁচু"),
        w("安い", "やすい", "yasui", "cheap", "সস্তা"),
        w("暑い", "あつい", "atsui", "hot (weather)", "গরম (আবহাওয়া)"),
        w("寒い", "さむい", "samui", "cold (weather)", "ঠান্ডা (আবহাওয়া)"),
        w("美味しい", "おいしい", "oishii", "delicious", "সুস্বাদু / মজা"),
        w("いい", "いい", "ii", "good", "ভালো"),
        w("悪い", "わるい", "warui", "bad", "খারাপ"),
        w("好き", "すき", "suki", "liked, to like", "পছন্দ"),
        w("元気", "げんき", "genki", "healthy, energetic", "ভালো আছি / সুস্থ"),
        w("忙しい", "いそがしい", "isogashii", "busy", "ব্যস্ত"),
    )

    /** Everyday speaking phrases — say these out loud every day! */
    val phrases: List<VocabEntity> = listOf(
        p("おはようございます", "おはようございます", "ohayou gozaimasu", "Good morning", "শুভ সকাল"),
        p("こんにちは", "こんにちは", "konnichiwa", "Hello / Good afternoon", "হ্যালো / শুভ দুপুর"),
        p("こんばんは", "こんばんは", "konbanwa", "Good evening", "শুভ সন্ধ্যা"),
        p("ありがとうございます", "ありがとうございます", "arigatou gozaimasu", "Thank you very much", "অনেক ধন্যবাদ"),
        p("すみません", "すみません", "sumimasen", "Excuse me / Sorry", "মাফ করবেন / দুঃখিত"),
        p("はじめまして", "はじめまして", "hajimemashite", "Nice to meet you (first time)", "আপনার সাথে পরিচিত হয়ে ভালো লাগলো"),
        p("私はバングラデシュ人です", "わたしはバングラデシュじんです", "watashi wa banguradeshu-jin desu", "I am Bangladeshi", "আমি বাংলাদেশি"),
        p("よろしくお願いします", "よろしくおねがいします", "yoroshiku onegaishimasu", "Please treat me well / Nice working with you", "আমার প্রতি সদয় থাকবেন"),
        p("お元気ですか", "おげんきですか", "ogenki desu ka", "How are you?", "আপনি কেমন আছেন?"),
        p("元気です", "げんきです", "genki desu", "I'm fine", "আমি ভালো আছি"),
        p("お名前は何ですか", "おなまえはなんですか", "onamae wa nan desu ka", "What is your name?", "আপনার নাম কী?"),
        p("分かりません", "わかりません", "wakarimasen", "I don't understand", "আমি বুঝতে পারছি না"),
        p("もう一度お願いします", "もういちどおねがいします", "mou ichido onegaishimasu", "One more time, please", "আরেকবার বলুন, প্লিজ"),
        p("ゆっくり話してください", "ゆっくりはなしてください", "yukkuri hanashite kudasai", "Please speak slowly", "দয়া করে আস্তে বলুন"),
        p("これはいくらですか", "これはいくらですか", "kore wa ikura desu ka", "How much is this?", "এটার দাম কত?"),
        p("トイレはどこですか", "トイレはどこですか", "toire wa doko desu ka", "Where is the toilet?", "টয়লেট কোথায়?"),
        p("駅はどこですか", "えきはどこですか", "eki wa doko desu ka", "Where is the station?", "স্টেশন কোথায়?"),
        p("水をください", "みずをください", "mizu o kudasai", "Water, please", "পানি দিন, প্লিজ"),
        p("いただきます", "いただきます", "itadakimasu", "(said before eating)", "(খাওয়ার আগে বলা হয়)"),
        p("ごちそうさまでした", "ごちそうさまでした", "gochisousama deshita", "Thank you for the meal (after eating)", "খাবারের জন্য ধন্যবাদ (খাওয়ার পরে)"),
        p("日本語を勉強しています", "にほんごをべんきょうしています", "nihongo o benkyou shite imasu", "I am studying Japanese", "আমি জাপানি ভাষা শিখছি"),
        p("大丈夫です", "だいじょうぶです", "daijoubu desu", "I'm okay / It's fine", "ঠিক আছে / সমস্যা নেই"),
        p("お疲れ様です", "おつかれさまです", "otsukaresama desu", "Thanks for your hard work (greeting at work)", "পরিশ্রমের জন্য ধন্যবাদ (কাজে শুভেচ্ছা)"),
        p("さようなら", "さようなら", "sayounara", "Goodbye", "বিদায়"),
        p("また明日", "またあした", "mata ashita", "See you tomorrow", "কাল দেখা হবে"),
    )
}
