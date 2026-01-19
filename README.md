# DIGIT – A Digital Wellbeing Assistant

A sleek, **offline-first Android digital wellbeing assistant** built with **Jetpack Compose**. DIGIT tracks app usage with a gorgeous neon + glassmorphism UI, provides detailed reports, helps set usage goals, and supports both light and dark modes — all while keeping your data **100% private and on-device**.

---

## ✨ Features

- 📱 **App Usage Tracking** – Monitors time spent on apps and auto-categorizes them (Productive, Study, Unproductive, Game, Neutral)
- 📊 **Modern Dashboard** – Concentric ring charts and progress visuals for daily goals
- 🎨 **Dual Theme** – Neon-infused Dark Mode + Clean Light Mode
- 🎯 **Goal Setting** – Set daily limits and productivity targets
- ✍️ **Manual Logging** – Add offline study or productive time
- 📄 **PDF Export** – Generate professional usage reports
- 🔒 **Privacy-Focused** – Fully offline. No servers. No accounts.
- 💎 **Glassmorphism UI** – Frosted-glass cards & bottom navigation

---

## 📸 Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/5e102858-b573-493b-a378-a6385222098a" width="220"/>
  <img src="https://github.com/user-attachments/assets/e40d4389-45f3-4749-8649-b778024733f1" width="220"/>
  <img src="https://github.com/user-attachments/assets/183ae6bc-a2ff-44bd-a12f-c31f5c624d06" width="220"/>
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/5db604c7-83c1-40df-95fc-f3d9b2def656" width="220"/>
  <img src="https://github.com/user-attachments/assets/0fa3f4be-b61c-4d37-8a64-0d9c90efd248" width="220"/>
  <img src="https://github.com/user-attachments/assets/47e02b44-2c3d-4f68-9f05-701a475fbdcd" width="220"/>
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/adf4c1f2-f55e-4a3c-9828-d6ac9a81fa67" width="220"/>
</p>

---

## 🛠️ Built With

This project uses a modern Android development stack:

### 🧩 UI
- **Jetpack Compose (Material 3)** – Fully declarative UI
- Custom charts & glassmorphism components

### 🧠 Language
- **Kotlin**
- Coroutines for async operations

### 🏗 Architecture
- Single-activity
- Stateful composables
- Local persistence

### 📦 Core Dependencies
```gradle
androidx.activity:activity-compose
androidx.compose.material3
androidx.compose.material
androidx.lifecycle:lifecycle-runtime-ktx
