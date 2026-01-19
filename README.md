# DIGIT - A Digital Wellbeing Assistant

![DIGIT App Banner](https://raw.githubusercontent.com/your-username/your-repository/main/banner.png)  <!-- Optional: Create and upload a cool banner image -->

A sleek, offline-first Android digital wellbeing assistant built with **Jetpack Compose**. DIGIT tracks app usage with a gorgeous neon/glassmorphism UI, provides detailed reports, helps set usage goals, and supports both light and dark modes. It runs entirely on-device, ensuring complete user privacy.

## ✨ Features

- **📱 App Usage Tracking**: Monitors time spent on apps and categorizes them automatically (Productive, Study, Unproductive, Game, Neutral).
- **📊 Modern Dashboard**: Visualizes daily usage with a beautiful concentric ring chart and progress bars for your goals.
- **🎨 Dual Theme**: Seamlessly switch between a neon-infused **Dark Mode** and a clean **Light Mode**.
- **🎯 Goal Setting**: Set daily limits for media consumption and targets for study/productive apps.
- **✍️ Manual Logging**: Log offline study time to get a complete picture of your productive hours.
- **📄 PDF Export**: Generate and download a professional PDF summary of your app usage.
- **🔒 Privacy-Focused**: 100% offline. No data ever leaves your device. No servers, no accounts.
- **💎 Glassmorphism UI**: Features a modern, frosted-glass bottom navigation bar and card design.

## 📸 Screenshots

| Dark Mode | Light Mode |
| :---: | :---: |
| ![Dark Mode Screenshot](https://raw.githubusercontent.com/your-username/your-repository/main/dark_mode.png) | ![Light Mode Screenshot](https://raw.githubusercontent.com/your-username/your-repository/main/light_mode.png) |
_Note: You will need to take these screenshots and upload them to your repository._

## 🛠️ Built With

This project showcases a modern Android development stack.

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3) for a fully declarative and dynamic UI.
- **Primary Language**: [Kotlin](https://kotlinlang.org/) - including Coroutines for asynchronous operations.
- **Architecture**: A simple, single-activity architecture with stateful composables.
- **Core Dependencies**:
  - `androidx.activity:activity-compose`
  - `androidx.compose.material3` & `androidx.compose.material`
  - `androidx.lifecycle:lifecycle-runtime-ktx`
- **System APIs**:
  - `UsageStatsManager` for core app usage tracking.
  - `SharedPreferences` for on-device goal and settings persistence.
  - `PdfDocument` for generating reports.

## 🚀 Getting Started

To build and run the project, you need Android Studio Giraffe or newer.

1.  **Clone the repository:**
    
