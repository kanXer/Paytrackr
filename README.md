# PayTrackr 💰

**PayTrackr** is a modern, privacy-focused Android application designed for small business owners, shopkeepers, and individuals to effortlessly manage customer credit (*Udhar*), payments, and digital ledger (*Khata*).

Built with **Jetpack Compose** and **Material 3**.

---

## ✨ Features

- 🔐 **Flexible Authentication**:
  - **Sign In with Google**: Quick 1-tap sign-in with Google.
  - **Email & Password**: Traditional registration and login.
  - **Set Password for Google Accounts**: After Google Sign-In, users can set a password to also log in directly via email & password.
  - **Change Password**: Change or update passwords anytime from the Profile section.
  - **Guest Mode**: Use the app completely offline without creating an account.

- ☁️ **Real-Time Cloud Sync & Offline Support**:
  - Powered by **Firebase Firestore**.
  - Works 100% offline with local storage and automatically syncs when online.

- 📒 **Khata & Customer Management**:
  - Track customer dues, credit entries, and partial/full payments.
  - One-tap customer settlement.
  - Quick contact picker to import customer details from your phonebook.
  - Search and filter by pending dues or settled accounts.

- 💬 **WhatsApp Payment Reminders**:
  - Pre-formatted personalized payment reminder messages sent directly via WhatsApp.

- 💳 **UPI Integration**:
  - Configure your shop UPI ID (VPA) in profile for quick digital collections.

---

## 🛠️ Tech Stack

- **Language**: Kotlin 2.3+
- **UI Toolkit**: Jetpack Compose with Material 3
- **Navigation**: Jetpack Navigation 3
- **Architecture**: MVVM with Kotlin Coroutines & `StateFlow`
- **Backend / Services**:
  - Firebase Authentication (Google Auth + Email/Password)
  - Firebase Cloud Firestore (Real-time data sync)
- **Minimum SDK**: Android 7.0 (API Level 24)
- **Target SDK**: Android 15 (API Level 36)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 17+
- Android SDK API 36

### Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/kanXer/Paytrackr.git
   cd Paytrackr
   ```
2. Open the project in Android Studio.
3. Place your `google-services.json` inside the `app/` directory (already included for `paytrackr-nexus`).
4. Build and run the app:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📄 License
This project is licensed under the MIT License.
