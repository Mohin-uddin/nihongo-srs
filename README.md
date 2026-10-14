# Nihongo SRS • জাপানি শিখি

A free, offline Android app to learn Japanese with the 1-4-7 spaced repetition method,
with meanings in **English and Bangla (বাংলা)** and **speaking practice**.

No ads, no account, no internet permission, no paid APIs.

## Open and run in Android Studio

1. Unzip `japanese-srs-app.zip`.
2. Android Studio → **File → Open** → choose the `japanese-srs-app` folder.
3. Wait for **Gradle sync** to finish (first time downloads libraries, needs internet once).
4. Plug in your phone (USB debugging on) or start an emulator.
5. Press **Run ▶**.

Requirements: Android Studio Ladybug (2024.2) or newer, JDK 17+ (bundled with Android Studio).
Phone: Android 8.0 (API 26) or newer.

### One-time phone setup (free)
- **Japanese voice (TTS):** Settings → System → Languages → Text-to-speech → Google engine
  → Install voice data → 日本語. The app shows an "Install voice" button if it is missing.
- **Offline mic check (optional):** Google app → Settings → Voice → Offline speech recognition → 日本語.
  Without it the mic check may need internet; listen-and-repeat always works offline.

## Features

| Screen | What it does |
|---|---|
| Home | Due today, Learning / Mastered counts, learn N5 / N4 / your own words, practice, daily reminder |
| Flashcards | Review: some cards show the Japanese, some show the Bangla and ask for the Japanese (active recall). Learn: whole card shown, no flipping. Every word has an example sentence |
| My words | All learned words (today or all) with answers and sentences visible, no flipping. Sentences-only mode for reading aloud |
| Exam | Today's or full exam, at least 20 multiple-choice questions (meaning, Japanese, listening). Wrong answers are asked again and go back to tomorrow's review |
| Daily conversation | 72 phrases in 8 topics (greetings, self-intro, shopping, restaurant incl. halal, work, directions, daily life, health). Listen → repeat → mic check |
| Manage | Add your own words (with Bangla meaning and sentence), reload N5 + N4, search, reset, delete |
| Progress | N5 / N4 completion %, words at each SRS stage, streak, last 7 days chart, exam history |
| Today's goals | On Home: finish reviews, learn 10 words, take an exam, speak 5 phrases |
| Kana | Hiragana and katakana chart with sound, plus a 20-question kana quiz |
| Reminder | Local notification once a day at a time you choose (default 8 PM) |

Bundled content (in `app/src/main/assets`): 719 N5 words (the full N5 level) and 106 N4 words, each with an example
sentence, plus 72 phrases, all in English and Bangla. Edit those text files to add more; bump
`StarterContent.VERSION` so existing installs pick up the changes.

## The 1-4-7 schedule (as specified)

| Review | Answer | New stage | Next review |
|---|---|---|---|
| Day 0 (learn) | – | 0 | Day 1 (+1) |
| Day 1 | correct | 1 | Day 4 (+3) |
| Day 4 | correct | 2 | Day 7 (+3) |
| Day 7 | correct | 3 | Day 14 (+7) |
| Day 14 | correct | 4 = **Mastered** | none |
| any | forgot | 0 | tomorrow (+1) |

Days are calendar days in your phone's time zone. A late review still counts; the next gap
starts from the day you actually reviewed.

## Code map (`app/src/main/java/com/nihongo/srs`)

| Step | File(s) |
|---|---|
| 1. Room DB | `data/VocabEntity.kt`, `data/UserProgress.kt`, `data/VocabDao.kt` (`getDueReviews`), `data/AppDatabase.kt` |
| 2. SRS engine | `srs/SpacedRepetitionEngine.kt` (+ unit test in `app/src/test`) |
| 3. ViewModel | `ui/review/ReviewViewModel.kt` (also Home, Manage, Speak ViewModels) |
| 4. Flashcard UI | `ui/review/FlashcardScreen.kt` |
| 5. Manifest & Gradle | `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, `build.gradle.kts` |
| Speech | `speech/JapaneseSpeaker.kt` (TTS), `speech/PronunciationChecker.kt` (mic), `speech/PronunciationScorer.kt` |
| Word lists | `app/src/main/assets/n5.txt`, `n4.txt`, `phrases.txt` |
| Exam | `ui/exam/ExamGenerator.kt`, `ExamViewModel.kt`, `ExamScreen.kt` |
| Reminder | `reminder/ReminderWorker.kt`, `ReminderScheduler.kt` |

## How to actually learn to speak • কিভাবে কথা বলা শিখবেন

1. **Every day, 20 minutes.** Reviews first, then 10 new words, then speaking. একদিনও বাদ দেবেন না।
2. **Say everything out loud.** মনে মনে নয়, জোরে বলুন।
3. **Shadowing:** play the phrase, speak *with* it, copy the rhythm. Use Slow first.
4. **Learn hiragana and katakana in the first 2 weeks** so you stop depending on romaji.
5. **Talk to yourself in Japanese:** "今日は暑いです。水を飲みます。" Use the words you just learned.
6. After N5 words feel easy, add your own sentences in Manage (with Bangla meaning).
