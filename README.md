# Tally

Keeps a persistent unread count for every app, so counts survive clearing the notification panel.
Counts show on a home screen widget and inside the app. Nothing leaves the phone (no INTERNET permission).

## Toolchain

| Piece | Version |
|---|---|
| Android Gradle Plugin | 9.3.0 (built-in Kotlin) |
| Gradle | 9.5.0 (wrapper included) |
| Kotlin / Compose compiler plugin | 2.4.0 |
| KSP | 2.3.11 |
| Compose BOM | 2026.09.00 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 29 |

Needs Android Studio Quail 2 (2026.1.2) or newer. Any current Android Studio download works.

## Setup checklist

1. **Open**: unzip, then in Android Studio choose *File → Open* and select the `Tally` folder (the one containing `settings.gradle.kts`).
2. **Sync**: Android Studio syncs Gradle automatically. If it asks to install Android SDK Platform 37 or Build-Tools, accept. Wait for "Gradle sync finished".
3. **Phone**: on the S26 Ultra, enable *Developer options* (Settings → About phone → Software information → tap *Build number* 7 times), then turn on *USB debugging*. Plug in and tap *Allow* on the phone.
4. **Run**: pick your phone in the device dropdown and press **Run ▶**.
5. **Notification access (required)**: in Tally's onboarding tap *Allow access*, find **Tally** in the list and turn it on.
   If the switch is greyed out ("Restricted setting"): Settings → Apps → Tally → ⋮ → *Allow restricted settings*, then try again.
6. **Usage access (optional)**: tap *Allow access* on the Usage access card and turn on Tally. This lets counts reset when you open an app.
7. **Battery → Unrestricted**: tap *Set Unrestricted* and confirm. To check: Settings → Apps → Tally → Battery → **Unrestricted**.
8. **Widget**: long-press an empty spot on the home screen → *Widgets* → **Tally** → drag *Tally unread counts* onto the screen. Resize it as you like.

## How counting works

- Messaging apps (MessagingStyle: WhatsApp, Telegram, Messages…) count each new message; your own replies aren't counted.
- Other apps count 1 per new notification. Updates to a notification that's already there aren't counted again.
- Ongoing notifications, group summaries, media/progress/navigation notifications, and Tally itself are ignored.
- Swiping notifications away or "Clear all" **keeps** counts.
- A count resets when you tap the notification, when the app removes its own notification (per-app toggle), when you open the app (needs Usage access), when you tap it in the widget, or when you swipe it in Tally.
- "System apps" that are excluded by default are system components **without a launcher icon** (Android System, Play services…). Preinstalled apps you actually use, like Messages, Phone and Gmail, are counted. Change any of this in Settings → Choose apps.
