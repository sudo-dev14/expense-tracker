# Expense Tracker (Android)

An offline expense tracker that builds your spending history from payment messages on your phone.

> **Your data never leaves your phone.** The app declares no internet permission, so Android itself
> blocks it from going online. There's no account, cloud backup, analytics or ads. CI fails the build if
> a network permission ever shows up in the APK.

## Features (MVP)

- **Auto-tracking:** reads payment messages on the device, both the existing inbox on first run and new
  messages as they arrive. OTPs, promotions, payment requests, due-date reminders, failed transactions
  and anything from a personal phone number are skipped.
- **Dashboard:** date ranges (This month, 3M, 6M, 1Y, 3Y, All, Custom) with total spent, income, net,
  change vs the previous period, a category donut, spending over time, top merchants and recent
  transactions.
- **Transactions:** search, filters (category, type, date range), grouped by day. Swipe left to mark
  something "Not an expense" (with undo).
- **Edit sheet:** change amount, payee, category, note and date, and see the original message. "Always
  put X in Y" creates a merchant rule and re-categorises past transactions.
- **Needs review:** transactions the parser was unsure about, one card at a time (Looks right / Edit /
  Ignore).
- **Manual entry:** for anything that didn't come in by message, or when auto-tracking is off.
- **PDF export:** pick a range and choose sections (summary, category chart, transaction list, income),
  then save to the phone or share. The PDF is generated on the device.
- **Privacy center:** shows what the app does and doesn't do, explains how to check it yourself, and
  lets you delete all data.
- Light and dark themes.

## Project layout

```
core/   Pure Kotlin/JVM library: SMS parsing, analytics, money formatting. No Android deps.
app/    Android app: Jetpack Compose UI, Room database, SMS reader/receiver, PDF export.
```

`core` is a separate Gradle build (`includeBuild`), so it builds and tests on any JVM without the
Android SDK.

### SMS parsing pipeline (pluggable)

```
SMS ─► SmsFilter ─► [BankTemplates…] ─► GenericBankSmsParser ─► (future: on-device LLM)
         │ drops OTP/promo/requests/     first parser that returns a result wins
         │ reminders/failed/personal
         ▼
      TransactionRepository: merchant rules → dedupe → Room
```

- `SmsParser` is the extension point. To add an on-device model later (Gemini Nano, or a bundled small
  model), implement `SmsParser` and append it to `ParserPipeline`. Callers don't change.
- `TemplateSmsParser` covers exact bank formats (named regex groups). `GenericBankSmsParser` handles
  everything else with shared `Extractors`.
- `DuplicateDetector` merges the same payment reported twice, for example by the bank and a UPI app.
- Low-confidence results go to **Needs review** instead of being dropped.

**The bank templates are based on common message shapes and still need checking against real
messages.** When a format is missed, add a masked sample to `ParserPipelineTest` and a template if
needed.

## Build

Requirements: JDK 17+ and the Android SDK (API 35).

```bash
./gradlew -p core test          # parser/analytics unit tests (no Android SDK needed)
./gradlew :app:assembleDebug    # debug APK → app/build/outputs/apk/debug/
```

GitHub Actions (`.github/workflows/android.yml`) runs the core tests, builds the APK, checks that the
APK requests no network permission, and uploads the APK as an artifact.

## Play Store notes

`READ_SMS`/`RECEIVE_SMS` are restricted permissions. Publishing needs the Permissions Declaration Form
(the "SMS-based money management" use case), plus a clear in-app explanation, which the onboarding
screen provides.

## Roadmap

- Learn new message formats from user corrections (saved templates)
- Optional on-device model parser for formats no rule understands
- Custom categories, budgets, recurring payment detection
- App lock (fingerprint/PIN), encrypted database
- Bundled display fonts (Fraunces/Manrope from the wireframes); the app currently uses system serif/sans
