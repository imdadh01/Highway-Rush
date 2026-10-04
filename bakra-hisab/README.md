# Bakra Hisab 1.3.4

- Feeding selection displays goat photo or the existing goat icon.
- Stock uses compact spacing and a header search with live photo results; sorting opens beside its button.
- Direct-cost cards open category totals and dated expense details.
- Employee salary responsibility uses either percentages or fixed amounts adding to monthly salary. Payments have Main, Partner or Dono with actual payer amounts; no payment percentage input.
- Employee and PDF summaries show each payer's net payments, earned salary responsibility, advance/payable and contribution difference. Partner Hisab includes salary contribution differences.
- On the first upgrade, legacy payment responsibilities are corrected using each employee's configured salary split. Original payment amounts/payers and earned salary entries are preserved. Previous payment share is retained internally, with a native pre-update snapshot. Future salary setting changes do not rewrite saved payment responsibility snapshots.
- Android versionCode 10, same package and signing identity. Tests deliberately not run for this release at the user's request; only release compilation and signing.

## Earlier accounting rules

## Accounting rules

- Owner Cash Book contains goat purchase cost, the owner's share of expenses and salary accruals, sale receipts, and general entries. Temporary partner settlements never enter this book.
- Employee salary accrual is green credit in the employee book and owner-share expense in Cash Book. Actual payments/advances and returned money adjust the employee balance; they do not charge salary twice.
- Employee Total view is earned salary minus net payments. Mine and Partner views show actual net payments by payer. A positive Total balance is payable to employee; a negative balance is advance held by employee.
- Each employee payment records its owner's allocation percentage at entry time. Partner adjustments use actual payer minus allocated owner share; a returned payment reverses that adjustment. Later employee percentage changes do not rewrite old entries.
- Recurring salary is optional, configured per employee from the three-dot menu. Missing due months are posted on launch/resume and when settings are saved. There is no guaranteed closed-app background execution. Days beyond month end use the last calendar day. Closed seasons are skipped. Deleted salary months are suppressed from automatic re-creation.
- Each season has an optional partner. Empty partner name hides all partner controls. Existing seasons migrate with their old partner enabled to preserve accounting. Once transactions exist, partner mode cannot be changed retroactively; the name may be edited.

## Upgrade and backups

Schema 1 migrates to schema 2, preserving all Cash Book and partner totals. Previously paid employee salary expenses become an accrual plus corresponding payments. Auto salary starts OFF. A local pre-upgrade snapshot is written before data replacement. Existing phone database filename and package ID are retained. Export an external backup before installing an update; install the APK over the existing app without uninstalling.

Confirmed transactions and drafts save locally offline. Android Storage Access Framework can save a backup file to a provider such as Google Drive. File save success is not a guarantee that the provider has uploaded it. The app has no private backend and no Google OAuth API integration. Manual backups and schema 1/2 restore are supported.

## Corrections and reports

Expenses have category totals/history and an Add button. Cash Book supports newest/oldest and amount sorting. PDF print/export supports season totals including Feeding, Medicine and Salary, Cash Book, all customers, and a selected customer's payments.

Employee long-press or Select enables bulk deletion; confirmation removes linked salary/payment entries. Goat deletion removes related sales, receipts, direct expenses, and transfer copies. Customer contact removal can preserve the sale, or an explicitly confirmed sale deletion removes receipts and returns the goat to stock. Season deletion removes its records; outgoing stock transfers must be resolved first to avoid deleting another season's history accidentally.

## Validation

`node bakra-hisab/tests/accounting.test.cjs` checks migration, owner share, payments/returns, partner balance, recurrence, sorting, cascades and restore. `node bakra-hisab/tests/ui.cjs` runs browser integration flows against the assets served on port 8765. GitHub Actions builds the unsigned release. The signed APK is checked with Android apksigner and installed over 1.0.0 in an Android 35 emulator to verify data retention.

