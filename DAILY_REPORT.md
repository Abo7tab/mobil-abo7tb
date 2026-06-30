# Daily Report

## Date: 2026-06-30

### Completed Phases:
- **Phase 1 (Project Setup & Core Infrastructure):** 
  - Created Android native project using Jetpack Compose, Hilt, and Kotlin DSL.
  - Setup `build.gradle.kts` files with dependencies (Compose, Hilt, Retrofit, WorkManager, Accompanist, Location, etc.).
  - Configured `AndroidManifest.xml` with all necessary permissions and services.
  - Created `ChildApplication.kt` and `MainActivity.kt`.
  - Defined `Constants.kt`.

- **Phase 2 (API Client & Registration):**
  - Setup Retrofit client with `ChildApiService.kt` matching the Backend endpoints.
  - Created `AuthInterceptor.kt` to inject the Bearer token automatically.
  - Implemented `SecurePrefsManager.kt` using `EncryptedSharedPreferences` for UUID and Token storage.
  - Implemented UI flow skeleton in `RegistrationScreen.kt` (Onboarding, Child Consent, Permissions, and Parent Auth).
  
- **Phase 3 (Core Security Services):**
  - Implemented `BootReceiver.kt` to restart the app after a reboot.
  - Implemented `ChildForegroundService.kt` to keep the app alive and immune to "Recent Apps" swipe-to-kill.
  - Implemented the Heartbeat Coroutine Loop inside the Foreground Service (1-minute interval as requested, bypassing WorkManager's 15m minimum limit).
  - Parent Verification API endpoint integrated in `ChildApiService.kt` for sensitive actions.

- **Phase 4 (Background Jobs):**
  - Setup WorkManager workers for Data Sync (`DataSyncWorker.kt`) and Location (`LocationWorker.kt`).
  - Created `WorkerHelper.kt` to enqueue periodic workers.

### Technical Decisions Made:
- **Heartbeat & Command Polling:** Instead of using WorkManager for 1-minute tasks (which is impossible since WorkManager enforces a 15-minute minimum interval), I implemented a reliable 1-minute `delay` loop inside the `ChildForegroundService`.
- **Jetpack Compose:** Used Compose exclusively for UI to ensure modern and maintainable code.
- **Hilt:** Used for Dependency Injection to cleanly provide Retrofit, SharedPrefs, and WorkManager factories.

### Next Steps:
- Refine the actual Data Sync implementation (SMS, Calls, Contacts).
- Integrate Accompanist completely for the permission request flow.
- Ensure Battery Optimization Exemption is requested properly during onboarding.

### FCM Setup Checklist:
- FCM Setup Complete
- Token: [يتم ملء هذا الحقل بعد اختبار التطبيق على هاتف حقيقي]
- Test message: [يتم التحديد بعد الإرسال من Firebase Console]
### GitHub Actions & Stealth Mode Update:
- **GitHub Actions Setup:** Added CI workflow `build-apk.yml` to automatically build Debug & Release APKs on push/PR, along with `README.md` and upload guide.
- **Stealth Mode Implementation:** 
  - Added 3 Stealth Levels (Visible, Hidden Name, Fully Hidden).
  - Created `SecretCodeReceiver` to launch the app via dialer code `*#*#7269#*#*`.
  - Added transparent launcher alias and updated `AndroidManifest.xml`.
  - Configured `MainActivity` to be hidden from Recent Apps (`FLAG_SECURE`).
- Updated `INSTALLATION_GUIDE.md` and `TEST_SCENARIOS.md` with the new Stealth features.
### Verify Parent Security Enhancements:
- **Rate Limiting Logic:** Created `PasswordAttemptManager` to progressively lock the app after multiple failed verify parent attempts (up to 60m lock).
- **Intruder Capture:** Created `IntruderCaptureManager` to silently capture front-camera photo and device screenshot on >= 3 failed attempts and upload via `MediaRepository`.
- **Dynamic Dialer Code:** Created `DialerCodeManager` and `SettingsSyncWorker` to allow changing the hidden app dialer code from the dashboard and syncing it locally.
- **UI & VM:** Developed `VerifyParentScreen` and `VerifyParentViewModel` integrating the countdown lock UI.
