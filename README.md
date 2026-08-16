# SayIt English 0.2

Android English-learning MVP designed to work locally on the phone.

## What's new in 0.2

- Grammar is no longer a fixed list of 15 questions. Questions are generated from multiple templates and weak skills get extra review weight.
- Vocabulary is now an interactive 10-round session with three rotating modes: meaning, sentence context, and recall by typing.
- Vocabulary bank expanded to 104 A2-B1 words.
- Spelling and dictation pick random content instead of walking through the same order.
- Dictation bank expanded to 40 sentences.
- Writing Coach no longer sends the user's text through the tiny local LLM. It uses a fast deterministic English correction engine and shows only: score, corrected text, up to four concrete issues, and a more natural version.
- Improve My Sentence is instant: corrected -> natural -> stronger vocabulary, with word replacements explained in Arabic.
- Qwen/llama runtime removed from the core app, reducing memory use and avoiding slow/hallucinated writing feedback.
- Whisper Tiny English remains for offline speech-to-text. It is downloaded once from Hugging Face (~75 MB), then speaking works offline.

## Build without Android Studio

1. Create a GitHub repository.
2. Upload the CONTENTS of this folder to the repository root. Make sure `.github/workflows/build-apk.yml` is included.
3. Open GitHub -> Actions -> `Build SayIt APK`.
4. Press `Run workflow` (or simply push to main).
5. After the workflow succeeds, download the artifact `SayIt-Offline-English-APK`.
6. Extract it and install `app-debug.apk` on the Android phone.

The phone only needs internet once if you want Speaking, to download Whisper Tiny from inside the app. Grammar, vocabulary, spelling, dictation, writing and sentence improvement do not need the model download.

## Prototype update signing

This repository includes a **prototype-only** signing key so APKs produced by future GitHub Actions runs can update v0.2 without losing local progress. Do not use this public/demo key for a production Play Store release.

If v0.1 was built before this fixed prototype key was added, Android may reject the v0.2 APK as an update because the old APK has a different debug signature. In that case, uninstall v0.1 once, install v0.2, and future prototype updates from this project can install over v0.2 normally.
"# SayItOfflineEnglish2" 
