# Midnight Assistant — Android Voice AI App

A native Android voice assistant built with Kotlin + Jetpack Compose. It listens with
on-device speech recognition, sends the transcript to an LLM through **Kilo Gateway**
(https://kilo.ai/gateway, an OpenAI-compatible router in front of 500+ models), and speaks
the reply back with text-to-speech. UI is implemented from `design.md`'s "Midnight
Intelligence" design system (dark glassmorphism, animated AI orb, frosted-glass cards).

## Requirements
- Android Studio (Koala/2024.1 or newer)
- JDK 17
- Android SDK 34, minSdk 26 (Android 8.0+)
- A Kilo Gateway API key — get one free at https://app.kilo.ai (Profile → Integrations)

## Getting started
1. Unzip the project and open the root folder in Android Studio ("Open an existing project").
2. Let Gradle sync (it will download the Gradle 8.7 wrapper and dependencies — first sync
   needs an internet connection).
3. Run the `app` configuration on a device or emulator with Google APIs (for on-device
   speech recognition / Google TTS voices) — API 26+.
4. In the app, open **Settings** (gear icon), paste your Kilo Gateway API key, optionally
   pick a model from "Fetch available models", tap **Test connection**, then **Save**.
5. Back on the main screen, tap the mic orb and talk. The assistant transcribes your
   speech, sends it to your selected model via Kilo Gateway, and reads the reply aloud.

## How it's wired to Kilo Gateway
`data/KiloGatewayClient.kt` is a small OkHttp client that talks to the OpenAI-compatible
gateway:
- `GET  {baseUrl}/models` — populates the model picker in Settings
- `POST {baseUrl}/chat/completions` — standard `{ model, messages, stream:false }` body,
  auth via `Authorization: Bearer <key>`

Default base URL: `https://api.kilo.ai/api/gateway`. You can point this at any other
OpenAI-compatible gateway/base URL from the Settings screen if you'd rather use a
different provider — nothing else in the app is Kilo-specific.

Nothing is hardcoded: the API key, base URL, model ID, and system prompt all live in
`DataStore` (`data/SettingsStore.kt`) so they persist across app restarts and never touch
source control.

## Conversation history & persistence
- **Typed preview**: while listening, the live transcript streams directly into the "Type
  a message" field (small, inline) instead of a separate large preview above the orb.
  Tapping the mic again **stops listening and immediately sends** whatever's in the field
  — no need to wait for silence-based auto-finalization. A 30s safety timeout also
  auto-stops (and sends) if a session somehow never finalizes on its own.
- **Every message is saved**: each exchange is written to disk (`data/ChatHistoryStore.kt`,
  plain JSON files under the app's private storage — nothing leaves the device except the
  text sent to Kilo Gateway) as soon as it happens, so nothing is lost if the app is closed
  or killed.
- **One conversation at a time**: all voice or typed messages keep appending to the same
  open conversation. Tapping the history icon (top bar) takes you to a list of past
  conversations — tap one to switch to it. Tapping "New conversation" (the refresh icon) is
  the *only* action that starts a fresh, empty thread; everything else stays in the
  conversation you're currently in, even across app restarts.
- **Model list is cached**: the model dropdown in Settings shows whatever was fetched last
  time immediately (persisted via DataStore) — opening Settings never re-hits the network
  on its own. Only an explicit tap on "Fetch available models" replaces the cached list.
  The dropdown also has a built-in search field to filter a long model catalog by name or id.

## Voice pipeline
- **Speech-to-text**: `speech/SpeechRecognizerManager.kt` wraps Android's built-in
  `SpeechRecognizer` (on-device where supported). `RECORD_AUDIO` is requested at runtime
  from `ChatScreen`.
- **Text-to-speech**: `speech/TextToSpeechManager.kt` wraps `android.speech.tts.TextToSpeech`
  and speaks each assistant reply when "Speak replies aloud" is enabled in Settings.
- **Orchestration**: `viewmodel/ChatViewModel.kt` owns the conversation state machine
  (idle → listening → thinking → speaking) and both managers.

## Design system mapping (design.md → code)
| design.md | Implementation |
|---|---|
| Color tokens (`surface`, `primary`, `secondary` = Electric Violet, `tertiary` = Azure, etc.) | `ui/theme/Color.kt` — copied 1:1, wired into a Material 3 `darkColorScheme` in `ui/theme/Theme.kt` |
| Typography (Hanken Grotesk / Inter / Geist, sizes, line-heights, tracking) | `ui/theme/Type.kt` — exact sizes/weights/line-heights/letter-spacing from design.md. Ships with system sans-serif/monospace fallbacks so the project builds with zero extra assets; see "Adding the real fonts" below. |
| `rounded.*` radii | `ui/theme/Shape.kt` (`MidnightRadius`, `MidnightShapes`) |
| `spacing.*` | `ui/theme/Shape.kt` (`MidnightSpacing`) |
| Glassmorphism cards, Ghost Borders | `ui/components/GlassCard.kt` |
| AI Orb / visualizer | `ui/components/AiOrb.kt` — animated gradient sphere with ambient glow, reacts to mic input level and assistant state (idle/listening/thinking/speaking) |
| Luminous-fill primary button, pill-shaped mic button | `ui/screens/ChatScreen.kt` mic button, `ui/screens/SettingsScreen.kt` primary Save button |
| Borderless lists / message bubbles | `ui/components/MessageBubble.kt` |

### Adding the real fonts (optional but recommended)
The type scale, weights and spacing already match design.md exactly — only the literal
typeface files are swapped for system fonts so the project has zero external asset
dependencies out of the box. To use the real families:
1. Download the `.ttf`/`.otf` files for Hanken Grotesk, Inter, and Geist you're licensed
   to use.
2. Put them in `app/src/main/res/font/` (Android requires lowercase-with-underscore
   filenames, e.g. `hanken_grotesk_bold.ttf`).
3. In `ui/theme/Type.kt`, replace the `HankenGrotesk`/`Inter`/`Geist` `FontFamily.*`
   placeholders with `FontFamily(Font(R.font.hanken_grotesk_bold, FontWeight.Bold), ...)`.

## Project layout
```
app/src/main/java/com/midnight/assistant/
├── MainActivity.kt              Compose entry point
├── AssistantApp.kt              Application class
├── data/
│   ├── SettingsStore.kt         DataStore-backed settings (API key, base URL, model…)
│   ├── ChatModels.kt            ChatMessage / GatewayModel / ChatSessionMeta / GatewayResult
│   ├── ChatHistoryStore.kt      File-based persistence for conversations (sessions + messages)
│   └── KiloGatewayClient.kt     OkHttp client for Kilo Gateway (models + chat completions)
├── speech/
│   ├── SpeechRecognizerManager.kt   Android SpeechRecognizer wrapper
│   └── TextToSpeechManager.kt       Android TextToSpeech wrapper
├── viewmodel/
│   └── ChatViewModel.kt         Conversation + settings + history state, orchestrates STT/API/TTS
├── navigation/
│   └── AppNav.kt                Chat ⇄ Settings ⇄ History navigation graph
└── ui/
    ├── theme/                   Color.kt, Type.kt, Shape.kt, Theme.kt (design.md tokens)
    ├── components/               AiOrb.kt, GlassCard.kt, MessageBubble.kt
    └── screens/                  ChatScreen.kt, SettingsScreen.kt, HistoryScreen.kt
```

## Permissions
- `RECORD_AUDIO` — requested at runtime, required for speech recognition
- `INTERNET` — required to call Kilo Gateway
- A `<queries>` block declares visibility into `RecognitionService` / `RECOGNIZE_SPEECH`
  intents, required on Android 11+ for `SpeechRecognizer` to resolve the system
  recognition service under package-visibility rules.

## Notes / next steps
- No API key ships with the app — everything routes through your own Kilo Gateway key,
  entered in Settings and stored locally via DataStore.
- The chat call is non-streaming (`stream:false`) for simplicity; swapping in
  server-sent-event streaming would mean parsing `text/event-stream` chunks in
  `KiloGatewayClient.sendChat` and emitting partial text as it arrives.
- `minSdk 26` was chosen because `TextToSpeech` and modern `SpeechRecognizer` behavior are
  most reliable from Android 8.0 onward; lower if you need to support older devices (some
  Compose APIs used here still work back to 21, but untested).
