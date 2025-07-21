# Best By Manager
![Java](https://img.shields.io/badge/Java-17-green)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)
![API](https://img.shields.io/badge/API-27--35-blue)
![Privacy: Local Only](https://img.shields.io/badge/Privacy-100%25%20Local-orange)

**Best By Manager** is an Android inventory-tracking app built to help small grocery stores, gas stations, and small businesses stay on top of product expiration dates. With barcode lookup, image support, and smart reporting, it ensures food is safe to consume and helps reduce waste by keeping your stock fresh.

## Table of Contents

[Features](#features)

[Preview](#preview)

[Admin Setup](#admin-setup)

[Usage Notes](#usage-notes)

[Download](#download)

[Quick Start Guide](#quick-start-guide)

[Build From Source](#build-from-source)

[Requirements](#requirements)

[Permissions](#permissions)

[Tech Stack & Architecture](https://github.com/Jwonka/BestByManager#techstackarchitecture)

[Attribution](#attribution)

[Testing Status](#testing-status)

[Known Issues](#known-issues)

[Privacy](#privacy)

[License](#license)

[Contributing](#contributing)

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

## Admin Setup
🧑‍💼 The first account created after installation is automatically granted **administrator** privileges.

👥 Admins can add or edit users through the **Employee Details** screen.

🔐 Admins have additional privileges to:

- Add additional administrators
  
- Reset passwords for users

- View **User-Level Reports** (grouped product activity by each user)

## Usage Notes
🛒 To add products, tap the **Add Product** button or navigate to the **Product Details** screen.
- Users must fill in the following fields:

  - Brand
    
  - Product name
  
  - Weight
  
  - Quantity
  
  - Expiration date
    
  - Barcode
  
  - Category
  
  - (Optional) Isle and product image

🔄 In the **Product Details** screen, users can either:

- Update an existing product
  
- Or add a **new expiration date** for the same product by toggling the switch next to the **Save Product** button

📋 All users can access **Product Reports** from the **Product Search** screen to view product data filtered by:

- Barcode

- Date range

- Expiration status (e.g., expired, expiring soon)

## Download
**SHA256 checksum:** `bestbymanager-v1.0.apk`  
**Size:** ~9.4MB

Android will prompt once to “Allow installs from unknown sources.” Accept to complete the installation.

## Quick Start Guide
1. On your Android device,  download [`BestByManager‑v1.0apk`](https://github.com/Jwonka/BestByManager/releases/download/v1.0/bestbymanagerv1.0.apk).
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

## Requirements
- ![API](https://img.shields.io/badge/API-27%20to%2035-blue) API 27 (Android 8.1 Oreo) ~ API 35 (Android 15, Vanilla Ice Cream)
- Architecture ~ arm64‑v8a, armeabi‑v7a, x86_64

## Permissions
- 📷 `CAMERA` – required to take product photos using the device camera
- 🌐 `INTERNET` – required to fetch product info from Open Food Facts 
- 📂 `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` – used to export inventory summaries (optional on Android 10+)
- 🔔 `POST_NOTIFICATIONS` – used to display expiration reminders (required on Android 13+)

Note: Best By Manager does not request location, contacts, or any sensitive personal permissions. All data stays local to the device.

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

- Accessibility features (e.g. TalkBack) not yet verified.

## Known Issues
- Some screens may not scroll optimally on very small devices (< 5").
- No automated testing suite (yet).
- Cloud sync is not implemented, but may be added in a future version.
- Product deletion is permanent (no undo).

## Privacy
Best By Manager stores all data locally on your device. No personal or product data is ever uploaded to external servers.

- 📷 The camera is only used to take product photos for local inventory tracking.
- 🌐 The app fetches public product information from [Open Food Facts](https://openfoodfacts.org) using barcode lookup.
- 🔒 No data is sent back to Open Food Facts or any third party.
- 🚫 There is no analytics, tracking, or cloud sync.

This ensures your inventory stays private and under your control at all times.

## License
This project is licensed under the MIT ![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg) – see the [LICENSE](https://github.com/Jwonka/BestByManager/blob/main/LICENSE) file for details.</br>
You are free to use, modify, and distribute this application commercially or privately under the terms of the MIT License.

## Contributing
This project is currently not accepting outside contributions.</br> However, feel free to fork the repository, open issues, or suggest improvements. Bug reports are always welcome!

> ⚡ Built in Wisconsin with 💚 for small businesses.

