# Best By Manager

**Best By Manager** is an Android inventory-tracking app built to help small grocery stores, gas stations, and small businesses stay on top of product expiration dates. With barcode lookup, image support, and smart reporting, it ensures food is safe to consume and helps reduce waste by keeping your stock fresh.

## Table of Contents

[Features](#features)

[Preview](#preview)

[Download](#download)

[Quick Start Guide](#quick-start-guide)

[Build From Source](#build-from-source)

[Requirements & Permissions](https://github.com/Jwonka/BestByManager#requirementspermissions)

[Tech Stack & Architecture](https://github.com/Jwonka/BestByManager#techstackarchitecture)

[Attribution](#attribution)

[Testing Status](#testing-status)

[Known Issues](#known-issues)

[Privacy](#privacy)

[License](#license)

## Features

- 📦 **Inventory tracking** for all food products with brand, barcode, category, and expiration
- 📷 **Photo capture support** for each product ![Camera: Local Only](https://img.shields.io/badge/Camera-Local%20Only-blue?style=flat-square&logo=camera&logoColor=white)
- 📅 **Expiration reports** filtered by date range, barcode, or employee
- 🔎 **Barcode scanning** with 1D barcode support ![Powered by Open Food Facts](https://img.shields.io/badge/Data%20Source-Open%20Food%20Facts-brightgreen?style=flat-square&logo=android&logoColor=white)
- 🧾 **User-level reporting** with grouped results and total counts
- 📤 **Export & share** inventory summaries
- 🔐 **Admin mode**: First user account becomes administrator
- 🗃️ **Offline storage** using Room database ![Privacy First](https://img.shields.io/badge/Privacy-100%25%20Local-orange?style=flat-square&logo=lock&logoColor=white)
- 🛒 Built for small grocery stores, gas stations, or stockrooms ![Built in Wisconsin](https://img.shields.io/badge/Built%20with%20❤️-in%20Wisconsin-red?style=flat-square)

## Preview

<img/>

## Download

**SHA256 checksum:** `app-release.apk`  
**Size:** ~5.2 MB  

Android will prompt once to “Allow installs from unknown sources.” Accept to complete the installation.

## Quick Start Guide
1. On your Android device,  download [`BestByManager‑v1.0.0.apk`](https://github.com/Jwonka/BestByManager/releases/download/v1.0.0/app-release.apk).
2. When prompted, allow your browser to install unknown apps **(one‑time toggle).**
3. Tap the downloaded file to install.
4. Launch **Best By Manager** from your app drawer.
5. Create the first account → You are now the administrator.

## Build From Source
Clone the repo
- git clone [https://github.com/Jwonka/BestByManager/tree/main](https://github.com/Jwonka/BestByManager/tree/main)
- cd BestByManager

Build with Android Studio Meerkat 2024.3.2
OR from the command line
./gradlew assembleRelease

---

## Requirements & Permissions

- ![API](https://img.shields.io/badge/API-27%20to%2035-blue) API 27 (Android 8.1 Oreo) ~ API 35 (Android 15, Vanilla Ice Cream)


- Architecture ~ arm64‑v8a, armeabi‑v7a, x86_64

App‑level runtime permissions:

- POST_NOTIFICATIONS  – send excursion reminders

- WRITE_EXTERNAL_STORAGE (optional, export share) – export schedule to a text file

## Tech Stack & Architecture

- Language: ![Java](https://img.shields.io/badge/Java-17-green) (Android Desugaring) 

- UI: Jetpack ConstraintLayout, Material 3

- Architecture pattern: MVVM (ViewModel + LiveData)

- Persistence: Room

- Build: Gradle 8, Android Plugin = 8.x

## Attribution

This app uses data and product images from [Open Food Facts](https://openfoodfacts.org), a free and open database of food products created by a non-profit community of volunteers.

- Product data is © Open Food Facts contributors and available under the [Open Database License (ODbL)](https://opendatacommons.org/licenses/odbl/1-0/).
- Product images are licensed under [Creative Commons Attribution–ShareAlike (CC BY–SA 3.0)](https://creativecommons.org/licenses/by-sa/3.0/).

## Testing Status
🧪 All screens have been manually tested for scrollability, responsiveness, and crash-free usage on common Android phones and tablets.

## Known Issues
- Some screens may not scroll well on very small devices (under 5").
- No automated testing suite (yet).

## Privacy

Best By Manager stores all data locally on your device. No personal or product data is ever uploaded to external servers.

- 📷 The camera is only used to take product photos for local inventory tracking.
- 🌐 The app fetches public product information from [Open Food Facts](https://openfoodfacts.org) using barcode lookup. No data is sent back to Open Food Facts or any third party.
- 🚫 There is no analytics, tracking, or cloud sync.

This ensures your inventory stays private and under your control at all times.

## License
This project is licensed under the MIT ![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg) – see the [LICENSE](https://github.com/Jwonka/BestByManager/blob/main/LICENSE) file for details.

> ⚡ Built in Wisconsin with 💚 for small businesses.

