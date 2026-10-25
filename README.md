# Peewee Block 🛡️

**Peewee Block** is a dedicated, minimal, privacy-focused website and adult-content blocking Android app built with Kotlin and Jetpack Compose.

---

## 🌟 Key Features

1. **100% On-Device Local DNS Filter VPN**
   - Intercepts DNS queries locally on device loopback (`127.0.0.1:53`).
   - Blocks over 150,000+ explicit domains without routing traffic through external third-party proxy servers.
   - Preserves complete network privacy and device battery life.

2. **Accessibility URL & Incognito Guard**
   - Continuously monitors active browser address bars across Google Chrome, Brave, Firefox, Microsoft Edge, and Opera.
   - Instantly detects and terminates Incognito / Private browsing tabs.
   - Matches against sensitive keyword lists to intercept explicit content before it renders.

3. **Strict Lock Mode**
   - Configurable emergency lockdown mode (15m, 30m, 1h, etc.) triggered automatically upon explicit access attempts or manually by the user.
   - Enforces tamper-resistant countdowns using `SystemClock.elapsedRealtime()` (immune to system date/time manipulation).
   - Prevents impulsive disabling while ensuring emergency dialer and phone functions remain 100% accessible.

4. **Private On-Device Statistics**
   - **Today's Stats**: Detected Block Attempts, Strict Lock Activations, and Successfully Blocked Events.
   - **Weekly Summary**: 7-day daily breakdown and weekly totals.
   - **Monthly Overview**: Month totals and weekly trend distribution.
   - Safe reset mechanism and zero remote tracking.

5. **Accountability PIN Security**
   - Optional 4-digit accountability PIN that can be set by a trusted partner, parent, or friend.
   - Gated access for disabling protection, modifying blocklists, or pausing shields.

---

## 🔒 Privacy & Android Limitations

- **Zero Data Collection**: Peewee Block does not record, log, or upload your browsing history, visited URLs, or search queries. All data remains exclusively on your device.
- **DNS-over-HTTPS (DoH)**: If a browser has "Use Secure DNS" (DoH) enabled in its internal privacy settings, queries are routed over HTTPS (port 443), which can bypass local UDP DNS interception. To prevent this, disable DoH in your browser settings (guided inside the app) or rely on the Accessibility URL Extractor layer.
- **Android Safe Mode**: Booting an Android device into Safe Mode disables all third-party services at the OS hardware level. Upon returning to normal mode, Peewee Block's boot watchdog immediately restores all protections.

---

## 📱 Permissions Required

| Permission | Purpose |
| :--- | :--- |
| `BIND_VPN_SERVICE` | Enables local loopback DNS filtering (`127.0.0.1:53`) |
| `BIND_ACCESSIBILITY_SERVICE` | Inspects browser URL bars and closes incognito tabs |
| `BIND_DEVICE_ADMIN` | Prevents unauthorized uninstallation during Strict Lock |
| `POST_NOTIFICATIONS` | Displays foreground service status and Strict Lock alerts |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevents aggressive OEM task killers from terminating background shields |

---

## 🛠️ Building the Project

### Prerequisites
- JDK 17
- Android SDK 36 (Min SDK 26)

### Build Debug APK
```bash
# Using Gradle
gradle assembleDebug

# Using Gradle wrapper (if available)
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### GitHub Actions CI/CD
This repository includes `.github/workflows/android-build.yml` which automatically:
- Builds the debug APK on every commit and pull request.
- Uploads the debug APK as a downloadable GitHub Actions artifact (`peewee-block-debug-apk`).
- Supports building signed release APKs when configured with repository secrets (`SIGNING_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).

---

## 📄 License
Open source and private under MIT License.
