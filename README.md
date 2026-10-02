# PocketDev (Pocket Codex) — Mobile Code Editor with Multi-Provider AI

> **Code anywhere, learn everything.** A full-featured Android coding workspace
> with an on-device editor, triple execution engines, and AI assistance powered
> by **Groq, OpenRouter, and Google Gemini** — including **thinking (reasoning)
> models** whose live thought process streams right into the UI.

The Android app source lives in [`PocketDev/`](PocketDev/). See
[`PocketDev/README.md`](PocketDev/README.md) for the full feature tour.

---

## What's new in v1.1.0

### 🧠 Thinking-model support
- Reasoning tokens (`reasoning`, `reasoning_content`, inline `<thinking>` blocks)
  are normalized across all providers and streamed live into a dedicated
  **thought-process panel** (fix bug / explain / improve / chat / auto-fix).
- Per-provider reasoning parameters: Groq `reasoning_format`/`reasoning_effort`,
  OpenRouter `reasoning` object, Gemini `reasoning_effort`.
- Automatic fallback: if a provider rejects thinking parameters, the request is
  transparently retried without them.

### 🔌 Three AI providers
- **Groq**, **OpenRouter**, and **Google Gemini** (OpenAI-compatible endpoint).
- Per-provider API keys, each stored encrypted (AES-256, `EncryptedSharedPreferences`).
- Curated model catalog with "thinking" badges, plus custom model-id input.

### ⚡ Performance on large files
- The editor no longer copies the whole file buffer on every recomposition;
  text is materialized once per debounced burst (stale jobs are cancelled on
  tab switch).
- Word-wrap auto-disables for files > 120k chars; AI ghost suggestions are
  skipped for files > 80k chars; streaming UI updates throttle to ~12 fps.
- TextMate grammars are memoized per language.

### 🎬 Professional animations
- Live AI streaming dialog with phase transitions, auto-scrolling
  reasoning/answer panels, collapsible reasoning in results.
- Animated provider/model pickers, micro-interactions, animated terminal output.

---

## Build

Requirements: JDK 17+, Android SDK 34, Python 3 (Chaquopy plugin).

```bash
cd PocketDev
./gradlew assemblePyRelease
# Output: app/build/outputs/apk/py/release/app-py-release.apk
```

The release build is signed with the committed keystore
(`PocketDev/keystore/pocketdev-release.keystore`, wired up via
`PocketDev/keystore.properties`). If those files are removed, the build
automatically falls back to debug signing.

## Release signing

The signing key, passwords, and usage instructions are committed at the
owner's request so the signed release is reproducible:

| File | Purpose |
|---|---|
| [`PocketDev/keystore/pocketdev-release.keystore`](PocketDev/keystore/pocketdev-release.keystore) | Release keystore (JKS, RSA-4096, alias `pocketdev`, valid until ~2056) |
| [`PocketDev/keystore.properties`](PocketDev/keystore.properties) | Signing config consumed by Gradle |
| [`SIGNING-CREDENTIALS.txt`](SIGNING-CREDENTIALS.txt) | Passwords + how to sign/verify |

> ⚠️ **Note**: since the key is in a public repo, anyone can sign APKs that
> install as updates of this app. Fine for a hobby/learning project; if you
> ever distribute seriously, generate a new private key and keep it out of VCS.

## Download

Grab the signed APK from the
[**Releases**](https://github.com/lsgzt/pocket-codex/releases) page
(`PocketDev-v1.1.0-release.apk`, ~63 MB, minSdk 26 / targetSdk 34).
