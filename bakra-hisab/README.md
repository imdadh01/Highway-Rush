# Bakra Hisab
Personal Android goat-business ledger. Package `com.bakrahisab.app`, Android 8+ with an updated Android System WebView. Java 17 / Gradle 8.9 / Android SDK 35.

## Features
- Home-only season selector; isolated seasonal data.
- Owner-share Cash Book with persistent Cash In and Cash Out buttons.
- Goat purchase, photos, batch entry, sale, optional customer credit and recovery.
- Feeding, medicine, employee salary and general expenses; custom expense share and actual payer.
- Separate partner ledger for advances and settlements. Positive means partner owes the owner.
- Sold-stock profit, remaining inventory, customer balances, PDF printing and CSV export.
- Automatic atomic local saves, draft recovery, JSON export/restore, optional PIN.
- Android document-provider backup file: select Google Drive if its provider is available. Best-effort writes while app is active/on resume; this is not direct Drive OAuth or guaranteed background cloud sync. Check the Drive app for upload completion.

## Accounting rules
Amounts are integer paisa. An 8000 salary bill with owner share 4000 always adds 4000 owner Out. Partner change equals amount actually paid by owner minus owner share. Repayments and temporary advances affect only Partner Hisab. This Cash Book is an owner-share ledger, not a bank reconciliation or actual physical cash balance. Capital and personal withdrawals affect season balance but not profit. A later sale collection affects receipts, not sales/profit twice. Unsold goats remain inventory. Shared overhead is charged to the season, not arbitrarily allocated per goat.

## Device storage and recovery
No fixed total goat-record count; practical capacity depends on phone storage. Photos are resized. Current JSON snapshot is bounded to approximately 55 MB; large herds should use modest photos and separate seasons. Camera provides the camera app thumbnail; Gallery supports higher-resolution images resized to 1000px. Business data and backup photos are private local data and are never committed to this repository. JSON backups are not encrypted. PIN is a local privacy lock, not encryption.

Before reset/uninstall keep a completed backup outside the phone. Restoring replaces the complete database after validation and preserves a local pre-restore safety snapshot. Newer unsupported backup schemas are rejected instead of silently corrupted. Closed seasons must be reopened before editing.

## Build and updates
Run `node tests/accounting.test.cjs`, then `gradle assembleRelease`. Release APK is deliberately unsigned in CI. The private release key is kept separately, never in this public repository. Sign with the same key and increase versionCode for every update. Keep applicationId and signing identity unchanged to update without uninstalling. Future schema changes need explicit migrations and restore tests. Do not use CI-generated debug keys for released updates.
