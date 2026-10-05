# Mera Hisaab — Android

Offline Android expense and business cash manager. The interface follows the chosen white / purple / coral theme, gradient balance card, smooth line graph and centered purple + menu. Language is in Settings. App package: `com.imdadh.merahisaab`.

## Included

- Independent profiles, opening cash and pre-existing business investments.
- Daily income/expenses with optional notes, dates, search, sort, edit and guarded deletion.
- Available cash excludes invested capital and unpaid receivables.
- Side-work / business-profit / opening / borrowed source balances separate from payment accounts.
- Personal payments default to side income, investment defaults to business profit. A second source can explicitly cover a shortage.
- Optional Cash/Bank/Digital Bank tracking. Enabling allocates existing money; disabling consolidates without duplicating income. Transfers are excluded from income and expense totals.
- Investment lots, partial sales, full loss, partial receipts, recorded sale profit/loss, linked receipt entries.
- Loans given/received, partial repayment and current receivable/payable totals.
- Daily, weekly, monthly, yearly, all-time and custom-date reports. Smooth line chart and daily/weekly/monthly normalized averages.
- English / Roman Urdu, device-credential app lock, local JSON backup/restore through Android Storage Access Framework. Google Drive can be selected when its document provider is installed, enabled and signed in. No app login, backend, ads, analytics or bank integration.
- Private atomic on-device storage. No Android INTERNET permission. Restore validates ledger and asks before replacing all profiles.

## Financial conventions

Amounts use integer paisa. Cash-source and wallet totals must reconcile. A transaction that would overdraw a wallet/source is rejected. Transactions replay chronologically; edits and backdated entries must keep subsequent transactions valid.

Opening balances, transfers, borrowing and principal repayment are not income. Sale profit/loss is recorded at sale time, separately from cash income. Sale receipts first recover original cost (returned to its original funding sources); any receipt above cost becomes received business profit. On a loss, only the recoverable principal returns. Unpaid sale value remains receivable and is never included in available cash.

Net capital is available + invested cost + receivables − payables. This is an internal tracking measure, not a valuation or tax report. All business spending recorded as investment follows the user's requested convention.

Monthly averages divide by the sum of covered fractions of each calendar month. Daily = selected total / included days. Weekly = daily × 7. Zero-transaction days are included. These are normalized rates, not forecasts.

## Build

Java 17, Gradle 8.9, Android SDK platform 35 and build tools 35.0.0. Minimum Android 8.0 (API 26). No third-party runtime packages.

Create `local.properties` with your Android SDK path. Restore the ORIGINAL release key from the separate private signing backup. Create `signing.properties` locally:

```
storeFile=/absolute/path/mera-hisaab-release.jks
storePassword=FROM_PRIVATE_BACKUP
keyAlias=mera-hisaab
keyPassword=FROM_PRIVATE_BACKUP
```

Run `gradle assembleRelease -x test -x lint`. Alternatively, the dependency-free SDK builder used for the delivered APK is `ANDROID_SDK_ROOT=/path/to/sdk ./scripts/build-sdk.sh /path/to/signing-backup`; it uses the official SDK tools directly and does not resolve Gradle plugins. Release output: `app/build/outputs/apk/release/app-release.apk`.

**No tests or emulator/device runs were performed, at the user's request. A successful APK build is not a claim of runtime validation.**

## Updates without uninstalling

Keep applicationId unchanged and use the SAME signing key. Increase versionCode for each release; optional environment variables APP_VERSION_CODE and APP_VERSION_NAME override the defaults. Install the newer APK over the existing app; do not uninstall or clear storage. Future schema changes must include migrations. Retain a data backup before updating.

The signing key and password are intentionally excluded from source and must never be committed, including to a private repository. Preserve the private signing backup securely; losing it prevents compatible future updates.

## GitHub

This app lives in the `mera-hisaab/` folder of the existing `imdadh01/Highway-Rush` repository. Open this folder as the Android project; the root project is the separate Highway Rush game.

[Download Mera Hisaab 1.0.0 APK](downloads/Mera-Hisaab-1.0.0.apk?raw=true)

The nested `.github/workflows/android-release.yml` is a future build template, not an active workflow. Before activating it at the repository root, remove its push trigger, set run working-directory to `mera-hisaab`, prefix artifact paths with `mera-hisaab/`, use unique release tags, and rename its signing secrets to `MERA_HISAAB_KEYSTORE_BASE64` and `MERA_HISAAB_KEYSTORE_PASSWORD`. Configure those secrets from the ORIGINAL private backup. Never use the game's signing key. No signing secrets have been configured by this upload.

The APK was built locally. No automated tests were run. Keep the private signing backup outside this repository.

## Backup

Settings → Save backup → choose Google Drive or local storage in Android's file picker. Restore replaces all profiles only after confirmation. Backup files are plaintext financial data; save privately. App lock uses the phone's existing device credential. No automatic scheduled backup is claimed.
