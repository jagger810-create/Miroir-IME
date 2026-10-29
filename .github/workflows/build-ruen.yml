#!/usr/bin/env python3
"""Adapt a checkout of nctahiti/Miroir-IME for experimental Russian/English ML Kit handwriting.

Usage: python3 apply_ru_en.py /path/to/Miroir-IME

This is a source patch only. It requires building with Android Studio/Gradle.
Do not use this experimental IME for passwords or other sensitive text.
"""
from pathlib import Path
import sys


def substitute(text: str, before: str, after: str, label: str) -> str:
    count = text.count(before)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one anchor, found {count}. Upstream changed; no file written.")
    return text.replace(before, after, 1)


def main(repo: Path) -> None:
    src = repo / "app/src/main/java/com/parnasse/miroir"
    wrapper = src / "DigitalInkWrapper.kt"
    settings = src / "CalibrationActivity.kt"
    ime = src / "MiroirIME.kt"
    manifest = repo / "app/src/main/AndroidManifest.xml"
    gradle = repo / "app/build.gradle.kts"
    files = [wrapper, settings, ime, manifest, gradle]
    for f in files:
        if not f.is_file():
            raise FileNotFoundError(f"Missing {f}. Run this on the Miroir-IME repository root.")
    original = {f: f.read_text(encoding="utf-8") for f in files}
    updated = dict(original)

    w = updated[wrapper]
    w = substitute(w,
        '        private const val LANGUAGE_TAG = "fr"\n',
        '        // Selected in CalibrationActivity. Do not hard-code French here.\n',
        'DigitalInkWrapper language constant')
    w = substitute(w,
        '    fun load() {\n        try {\n            val modelId = DigitalInkRecognitionModelIdentifier.fromLanguageTag(LANGUAGE_TAG)\n                ?: run { Log.e(TAG, "Langue non supportée: $LANGUAGE_TAG"); return }',
        '    fun load() {\n        try {\n            val languageTag = CalibrationActivity.getRecognitionLanguage(context)\n            val modelId = DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)\n                ?: run { Log.e(TAG, "Langue non supportée: $languageTag"); return }',
        'DigitalInkWrapper model identifier')
    updated[wrapper] = w

    s = updated[settings]
    s = substitute(s,
        '        private const val PREFS_NAME = "miroir_calibration"\n',
        '        private const val PREFS_NAME = "miroir_calibration"\n'
        '        private const val KEY_RECOGNITION_LANGUAGE = "recognition_language"\n',
        'CalibrationActivity preference key')
    s = substitute(s,
        '        fun getSpatialDistanceX(ctx: Context): Float =\n',
        '        fun getRecognitionLanguage(ctx: Context): String =\n'
        '            prefs(ctx).getString(KEY_RECOGNITION_LANGUAGE, "ru")?.takeIf { it == "ru" || it == "en" } ?: "ru"\n'
        '        fun getSpatialDistanceX(ctx: Context): Float =\n',
        'CalibrationActivity getter')
    s = substitute(s,
        '        // ═══ 🎯 Blob ═══\n',
        '        // RU/EN handwriting recognition — change takes effect after reopening the IME.\n'
        '        root.addView(sectionHeader("🌐 Язык распознавания / Recognition"))\n'
        '        val languageGroup = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }\n'
        '        val ruButton = RadioButton(this).apply {\n'
        '            text = "Русский"\n'
        '            id = android.view.View.generateViewId()\n'
        '            setTextColor(Color.BLACK)\n'
        '        }\n'
        '        val enButton = RadioButton(this).apply {\n'
        '            text = "English"\n'
        '            id = android.view.View.generateViewId()\n'
        '            setTextColor(Color.BLACK)\n'
        '        }\n'
        '        languageGroup.addView(ruButton)\n'
        '        languageGroup.addView(enButton)\n'
        '        languageGroup.check(if (getRecognitionLanguage(this) == "ru") ruButton.id else enButton.id)\n'
        '        languageGroup.setOnCheckedChangeListener { _, id ->\n'
        '            prefs(this).edit().putString(KEY_RECOGNITION_LANGUAGE, if (id == ruButton.id) "ru" else "en").apply()\n'
        '        }\n'
        '        root.addView(languageGroup)\n'
        '        root.addView(TextView(this).apply {\n'
        '            text = "Close and reopen the keyboard to apply the language. Wi-Fi needed for the first model download."\n'
        '            setTextColor(Color.DKGRAY)\n'
        '        })\n'
        '        // ═══ 🎯 Blob ═══\n',
        'CalibrationActivity UI')
    updated[settings] = s

    m = updated[ime]
    m = substitute(m,
        '    private var recognizer: DigitalInkWrapper? = null\n',
        '    private var recognizer: DigitalInkWrapper? = null\n'
        '    private var recognizerLanguage: String? = null\n',
        'IME language state')
    m = substitute(m,
        '        recognizer = DigitalInkWrapper(this)\n        recognizer?.load()\n',
        '        recognizer = DigitalInkWrapper(this)\n'
        '        recognizerLanguage = CalibrationActivity.getRecognitionLanguage(this)\n'
        '        recognizer?.load()\n',
        'IME initial load')
    m = substitute(m,
        '        super.onStartInputView(info, restarting)\n',
        '        super.onStartInputView(info, restarting)\n'
        '        val selectedLanguage = CalibrationActivity.getRecognitionLanguage(this)\n'
        '        if (recognizerLanguage != selectedLanguage) {\n'
        '            // Settings changed while the keyboard was open. Reload the new language model.\n'
        '            recognizer?.close()\n'
        '            recognizer = DigitalInkWrapper(this)\n'
        '            recognizerLanguage = selectedLanguage\n'
        '            recognizer?.load()\n'
        '            Log.i(TAG, "Recognition language: $selectedLanguage")\n'
        '        }\n',
        'IME input-view reload')
    updated[ime] = m

    g = updated[gradle]
    g = substitute(g,
        '        applicationId = "com.parnasse.miroir.v4"\n',
        '        applicationId = "com.booxinklab.miroirruen"\n',
        'unique experimental application id')
    updated[gradle] = g

    x = updated[manifest]
    x = substitute(x, 'android:label="Miroir"', 'android:label="Miroir RU EN (test)"', 'app label')
    x = substitute(x, 'android:label="Miroir manuscrit"', 'android:label="Miroir RU EN"', 'keyboard label')
    updated[manifest] = x

    # Validate all modifications before writing any files.
    for path, value in updated.items():
        if value == original[path]:
            raise RuntimeError(f"No change in {path}")
    for path, value in updated.items():
        path.write_text(value, encoding="utf-8")
        print(f"Updated {path.relative_to(repo)}")
    print("\nReady for Gradle sync. This is unbuilt experimental source; see README_RU.md")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: python3 apply_ru_en.py /path/to/Miroir-IME")
    main(Path(sys.argv[1]).expanduser().resolve())
