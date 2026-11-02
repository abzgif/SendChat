<p align="center">
  <img src="master_logo_1024.png" width="128" height="128" alt="SendChat Logo" />
</p>

<h1 align="center">SendChat</h1>

<p align="center">
  <strong>Lightning-fast, 100% free and open-source peer-to-peer local network sharing for Android, Windows, macOS, Linux, and any Web browser.</strong>
</p>

<p align="center">
  <a href="https://bytegif.com/sendchat"><img src="https://img.shields.io/badge/Website-bytegif.com%2Fsendchat-FF5C5C.svg?style=flat-square&logo=googlechrome&logoColor=white" alt="Official Website"></a>
  <a href="https://github.com/abzgif/SendChat/releases/latest"><img src="https://img.shields.io/github/v/release/abzgif/SendChat?color=3DDC84&label=Latest%20Release&style=flat-square" alt="Release"></a>
  <a href="https://github.com/abzgif/SendChat/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square" alt="License"></a>
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF.svg?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Compose-Multiplatform-4285F4.svg?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose Multiplatform">
  <img src="https://img.shields.io/badge/Platforms-Android%20%7C%20macOS%20%7C%20Windows%20%7C%20Linux%20%7C%20Web-brightgreen?style=flat-square" alt="Platforms">
</p>

---

**SendChat** is a completely free, open-source, cross-platform local sharing application that connects your phones, tablets, laptops, and desktop computers over your local Wi-Fi or hotspot. No cloud servers, no user accounts, no tracking or telemetry, and no file size limits — just pure, high-speed local network connectivity with end-to-end encryption.

🌐 **Official Webpage**: [https://bytegif.com/sendchat](https://bytegif.com/sendchat)

---

## 📱 Screenshots

| Device Discovery | P2P Chat & Media Transfers | Conversation Options | Local Web Share Portal | Settings & Diagnostics |
| :---: | :---: | :---: | :---: | :---: |
| <img src="screenshots/device_discovery.webp" width="190" alt="Device Discovery" /> | <img src="screenshots/chat_transfers.webp" width="190" alt="P2P Chat and Media Transfers" /> | <img src="screenshots/conversation_settings.webp" width="190" alt="Conversation Controls" /> | <img src="screenshots/web_share_portal.webp" width="190" alt="Local Web Share Portal" /> | <img src="screenshots/app_settings.webp" width="190" alt="Settings & Diagnostics" /> |

---

## ✨ Key Features

- ⚡ **Zero Cloud & Maximum Speed**: Direct device-to-device streaming across your local network running at your full router / Wi-Fi bandwidth.
- 🌐 **Local Web Share (Zero-Install Sharing)**: Share files and text with devices that don't have the app installed (such as iPhones, iPads, or work computers). Open a QR code or local link (`http://<ip>:42831`) in any web browser to stream files bidirectionally with live chat history and instant copy options.
- 🔒 **End-to-End Encryption**: Every session between paired SendChat devices is protected with Elliptic Curve Diffie-Hellman (ECDH) key agreement and AES-256-GCM authenticated encryption. Zero trust, eavesdropping-proof.
- 📡 **Zero-Config Discovery**: Automatic peer discovery powered by mDNS (Multicast DNS) and UDP broadcast fallback. Devices appear instantly without typing IP addresses.
- 🤝 **Fast Mutual Pairing**: Connect securely using QR code scanning or a 6-digit verification PIN to confirm peer authenticity.
- 💬 **Bidirectional P2P & Web Chat**: Fast, offline messaging with delivery confirmations, copy/paste shortcuts, bulk message cleanup, and chat between apps and web browsers.
- 📁 **Unlimited File & Media Transfer**: Send photos, videos, archives, documents, or entire folders with real-time transfer progress, pause/cancel controls, and native drag-and-drop support.
- 🪶 **Optimized Lightweight Footprint**: Ultra-compact builds with R8 full mode on Android (~3.4 MB APK) and runtime ZIP compression across desktop platforms (~25 MB runtime reduction).
- 🔔 **Per-Device Notification Controls**: Granular mute / sound toggle for each connected device.
- 🎨 **Adaptive Material 3 UI**: Beautiful, responsive layout built with Jetpack Compose that seamlessly transitions between desktop and mobile form factors.
- 💻📱 **True Cross-Platform**: Native applications for **Android**, **macOS** (Apple Silicon & Intel), **Windows**, and **Linux**, plus universal browser access via **Local Web Share**.

---

## 📥 Downloads

Pre-built binaries for all platforms are available on the **[Releases Page](https://github.com/abzgif/SendChat/releases/latest)** or through our official site at **[bytegif.com/sendchat](https://bytegif.com/sendchat)**:

| Platform | Format | Description | Download |
| :--- | :--- | :--- | :--- |
| **Windows** | `.exe` / `.msi` | Windows 10 / 11 (64-bit) | [Download Installer](https://github.com/abzgif/SendChat/releases/latest) |
| **macOS** | `.dmg` / `.pkg` / `.app` | macOS 11+ (Apple Silicon & Intel) | [Download macOS](https://github.com/abzgif/SendChat/releases/latest) |
| **Linux (Debian/Ubuntu)** | `.deb` | Ubuntu, Debian, Mint (64-bit) | [Download DEB](https://github.com/abzgif/SendChat/releases/latest) |
| **Linux (Fedora/RHEL)** | `.rpm` | Fedora, RHEL, openSUSE (64-bit) | [Download RPM](https://github.com/abzgif/SendChat/releases/latest) |
| **Android** | `.apk` / Google Play | Android 7.0+ (API 24+) | [Releases](https://github.com/abzgif/SendChat/releases/latest) / [bytegif.com/sendchat](https://bytegif.com/sendchat) |
| **iOS / iPadOS / Others** | Web Browser | Connect via **Local Web Share** in app | No install needed |

SendChat can check GitHub Releases from **Settings → App Updates** on desktop, download the current platform’s installer, and open it. Android updates are delivered through Google Play and GitHub Releases.

---

## 🌐 How Local Web Share Works

Need to send files to an iPhone, iPad, or a PC without installing software?
1. Open SendChat and tap the **Web Share** icon (🌐) in the top bar.
2. A QR code and local web URL (`http://192.168.x.x:42831`) will appear.
3. The other device connects using their camera or browser on the same Wi-Fi.
4. **App to Web**: Queue files in the app to let the browser download them instantly, or send messages directly to the "Web Browser" chat room.
5. **Web to App**: Drag and drop files or type messages in the browser to receive them on your device immediately, view chat history, and copy texts with one tap.

---

## 🔐 Security Architecture

SendChat is built on a zero-trust local security model:

```mermaid
sequenceDiagram
    autonumber
    actor Alice as Device A (Sender)
    actor Bob as Device B (Receiver)

    Alice->>Bob: 1. mDNS / UDP Discovery Announcement
    Note over Alice,Bob: Devices discover each other on local Wi-Fi
    Alice->>Bob: 2. Pairing Request (ECDH Public Key)
    Bob-->>Alice: 3. Pairing Response (ECDH Public Key)
    Note over Alice,Bob: Derive Shared Secret (ECDH Curve25519)
    Alice->>Bob: 4. Verify 6-digit PIN / QR Code
    Note over Alice,Bob: Establish AES-256-GCM Session
    Alice->>Bob: 5. High-speed encrypted transfer (Messages & Files)
```

1. **Discovery**: Devices announce their presence on the local subnet via mDNS (`_sendchat._tcp.local`) and fallback UDP multicast.
2. **Key Agreement**: Devices exchange ephemeral keys using ECDH (Elliptic-Curve Diffie-Hellman).
3. **Verification**: Users verify connection authenticity through a 6-digit confirmation PIN or QR code scan.
4. **Data Transfer**: All chat messages, metadata, and binary file chunks are encrypted with AES-256-GCM before transmission over Ktor CIO sockets.

---

## 🛠️ Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) (Material 3)
- **Language**: [Kotlin](https://kotlinlang.org/) (Kotlin Multiplatform 2.1+)
- **Networking**: [Ktor](https://ktor.io/) 3.x (CIO client & embedded HTTP server)
- **Database**: [SQLDelight](https://cashapp.github.io/sqldelight/) 2.x (SQLite driver with coroutines extensions)
- **Discovery**: [JmDNS](https://github.com/jmdns/jmdns) (Desktop) & Android NSD (Network Service Discovery)
- **Cryptography**: [Bouncy Castle](https://www.bouncycastle.org/) (ECDH, AES-256-GCM, SHA-256)
- **QR Codes**: [ZXing](https://github.com/zxing/zxing)

---

## 🚀 Building from Source

### Prerequisites
- **JDK 17** or higher (Eclipse Temurin 17 recommended)
- **Android Studio** (for building Android targets) or IntelliJ IDEA (for Desktop)

### 1. Clone the repository
```bash
git clone https://github.com/abzgif/SendChat.git
cd SendChat
```

### 2. Run Desktop Application (Local Development)
```bash
./gradlew :desktopApp:run
```

### 3. Package Desktop Installers
```bash
# Package for macOS (DMG & PKG)
./gradlew :desktopApp:packageReleaseDmg :desktopApp:packageReleasePkg

# Package for Windows (EXE & MSI - run on Windows)
./gradlew.bat :desktopApp:packageReleaseExe :desktopApp:packageReleaseMsi

# Package for Linux (DEB & RPM - run on Linux)
./gradlew :desktopApp:packageReleaseDeb :desktopApp:packageReleaseRpm
```

### 4. Build Android Application
```bash
# Build Android APK
./gradlew :androidApp:assembleRelease

# Build Android App Bundle (for Google Play)
./gradlew :androidApp:bundleRelease
```

---

## 🤝 Contributing

Contributions, bug reports, and feature requests are welcome!
Feel free to open an issue or submit a pull request.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is open-source and licensed under the [MIT License](LICENSE).
