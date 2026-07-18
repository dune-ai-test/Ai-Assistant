# Solace — Android Voice AI App

A native Android voice assistant built with Kotlin + Jetpack Compose. It listens
continuously (ChatGPT-style Voice Mode), sends the transcript to an LLM through **Kilo
Gateway** (https://kilo.ai/gateway, an OpenAI-compatible router in front of 500+ models),
and speaks
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
- **Voice input**: see "Voice Mode" below for the full continuous conversation flow.
  Typing in the message field works identically at any time as a fallback/alternative.
- **Review before sending**: every recognized turn sits on screen for a few seconds
  ("Did I get that right?") before it's actually sent, with the exact heard text visible.
  Tap **Cancel** to stop it (the text is copied into the typed field so you can fix and
  resend it manually), **Send now** to skip the wait, or just let it send automatically
  when the countdown hits zero.
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
  The model field opens a full searchable picker dialog (`ModelPickerDialog` in
  `SettingsScreen.kt`) — tap it to browse/search the fetched catalog by name or id and pick one.

## Voice Mode (continuous, ChatGPT-style conversation)
Tap the orb once to enter **Voice Mode** — a continuous, hands-mostly-free conversation
loop, not a "hold to talk" or "tap per sentence" interaction:

1. The app starts listening immediately. Speak naturally — there's no button to press per
   sentence.
2. Android's own pause/silence detection decides when your turn ends (`ContinuousSpeechRecognizer`
   drives one `SpeechRecognizer` turn at a time; the loop that keeps calling it again is
   what makes it feel continuous).
3. Once you pause, the transcript is sent to Kilo Gateway automatically.
4. The reply is spoken aloud via TTS, then the app **automatically starts listening again**
   — no tap needed — continuing the back-and-forth until you end Voice Mode.
5. **Interrupting**: while the assistant is talking, tap the orb to jump back in
   immediately (stops the reply, starts listening). There's also a **best-effort automatic
   barge-in** — `speech/MicActivityMonitor.kt` uses a lightweight `AudioRecord` amplitude
   check (with acoustic echo cancellation where the device supports it) to notice you've
   started talking over the assistant and interrupt on its own. This is inherently
   approximate — Android doesn't guarantee echo cancellation between arbitrary speaker
   output and mic input — so it can occasionally misfire on speakerphone. Turn it off in
   Settings ("Allow voice interruption") if that happens; manually tapping the orb to
   interrupt always works regardless.
6. Tap the orb again while it's your turn to talk (or the **"End Voice Mode"** text button)
   to leave the loop entirely. There's no separate mic button — the orb itself is the
   start/stop/interrupt control.

Why this isn't the "tap mic → get one system dialog" approach from an earlier iteration:
that one-shot system dialog is reliable but is fundamentally modal — there's no way to
loop it invisibly, show partial transcripts inline, or support interruption. True
continuous Voice Mode requires driving `SpeechRecognizer` directly, restarting it after
each turn. The earlier reliability problems with that API came from custom silence-length
intent extras that some OEM recognizers mishandle; those have been removed here, and only
the always-supported extras are used.

## Settings
The Settings screen (redesigned, icon-labeled sections) covers:
- **Connection** — API key, base URL, model picker (searchable dialog), fetch/test.
- **Voice Mode** — speak-replies toggle, allow-interruption toggle, **review-before-sending**
  (on/off + adjustable delay: 2/3/4/5/6/8s — the delay before a heard transcript is actually
  sent, with a Cancel option), and **show typing bar** (off by default — Voice Mode via the
  orb is the primary interaction; turn this on to get a text-entry fallback on the Chat screen).
- **Assistant behavior** — the system prompt.
- **Usage** — cumulative token count across every request to Kilo Gateway (parsed from the
  API's own `usage.total_tokens`), with a Reset button.
- **Backup & restore** — **Export** writes every conversation (all sessions, all messages) to
  a JSON file you pick via Android's file picker; **Import** reads one back in. Imported
  conversations get fresh ids and are added alongside existing history (titled "… (imported)")
  rather than overwriting anything.

## Voice pipeline
- **Speech-to-text**: `speech/ContinuousSpeechRecognizer.kt` wraps `SpeechRecognizer` for
  one turn at a time; `ChatViewModel` re-invokes it after each turn to keep the
  conversation going. `RECORD_AUDIO` is requested at runtime from `ChatScreen`, and a
  connectivity check fails fast with a clear message instead of hanging if there's no
  network (most on-device recognizers need it).
- **Barge-in detection**: `speech/MicActivityMonitor.kt` — see "Voice Mode" above.
- **Text-to-speech**: `speech/TextToSpeechManager.kt` wraps `android.speech.tts.TextToSpeech`.
- **Orchestration**: `viewmodel/ChatViewModel.kt` owns the full conversation state machine
  (idle → listening → thinking → speaking → listening → …) and both speech components.

## The Solace design system
This app was originally built against a client-supplied `design.md` spec ("Midnight
Intelligence" — cool slate/violet/azure glassmorphism). It's since been fully redesigned
and rebranded as **Solace**, moving deliberately away from that cool-blue/violet "generic
AI app" look:

| Element | Choice | Where |
|---|---|---|
| Palette | Warm, near-black charcoal base (`#0D0B08`) with a champagne-gold signature accent, and jewel-tone states — jade for listening, dusty wine for thinking, glowing copper for speaking, warm ember for errors — instead of a single blue/violet hue reused everywhere | `ui/theme/Color.kt` |
| Typography | Serif display face (headlines) paired with clean sans body text — an editorial, boutique feel rather than all-sans-everything. Ships on system serif/sans so it builds with zero bundled fonts; swap in a licensed serif (e.g. Fraunces, Freight Display) via `res/font/` for the exact intended look | `ui/theme/Type.kt` |
| Shape | A tighter, more tailored corner-radius scale than a typical bubbly consumer app | `ui/theme/Shape.kt` (`MidnightRadius`) |
| Surfaces | "Candlelit glass" — warm-tinted translucent panels with a fine gold hairline border | `ui/components/GlassCard.kt` |
| Signature mark | A jewel-toned sphere with a fine independently-rotating bezel ring (a nod to precision objects — a watch face, glassware — rather than a generic glowing blob), distinct color family per conversational state | `ui/components/AiOrb.kt` |
| App icon | Gold bezel ring over a jade core on the warm charcoal background, echoing the orb mark | `res/drawable/ic_launcher_*.xml` |

Note: the Kotlin package (`com.midnight.assistant`) and internal identifiers (`MidnightColors`,
`MidnightTypography`, etc.) were intentionally left as-is — that's implementation detail
invisible to anyone using the app, and renaming it project-wide would be a large, purely
mechanical change with no visible benefit. Only the visible brand — name, icon, colors,
type, and the orb mark — changed.

### Adding real licensed fonts (optional)
The type scale, weights, and line-heights are already final — only the literal typeface
files are system fallbacks so the project builds with zero bundled assets. To use real
fonts:
1. Get `.ttf`/`.otf` files for your chosen serif display face and sans body face.
2. Put them in `app/src/main/res/font/` (lowercase-with-underscore filenames).
3. In `ui/theme/Type.kt`, replace the `DisplaySerif`/`BodySans`/`LabelSans` `FontFamily.*`
   placeholders with `FontFamily(Font(R.font.your_font, FontWeight.X), ...)`.

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
│   ├── ContinuousSpeechRecognizer.kt  Loop-friendly SpeechRecognizer wrapper (Voice Mode)
│   ├── MicActivityMonitor.kt          Amplitude-based barge-in detector while TTS plays
│   └── TextToSpeechManager.kt         Android TextToSpeech wrapper
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
