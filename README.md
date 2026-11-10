# MemoryCare (SIH26003) 🛰️🧠

**Cognitive Games & Memory Assistance Platform for Elderly Dementia Patients**  
*Thematic Focus: Space Technology & Telemetry (ISRO / NavIC) with North-Eastern Region (NER) Safety Monitoring.*

---

## 📱 Tech Stack
- **Language**: Kotlin 2.0
- **UI Toolkit**: Jetpack Compose + Material 3
- **Architecture**: Single Activity (`MainActivity`) with Compose Navigation (`NavHost`)
- **State Management**: Centralized `MemoryCareViewModel` (SharedViewModel) ensuring reactive live state updates across all screens
- **Persistence**: Local Storage Repository (`SharedPreferences` with JSON serialization)
- **Minimum SDK**: Android API 26+ (Android 8.0 Oreo) | **Target SDK**: API 34

---

## 🌟 5 Core Screens

| Screen | File | Highlights |
| :--- | :--- | :--- |
| **A. Role Selection** | `RoleSelectionScreen.kt` | Large icon cards to switch between **Patient** and **Caretaker** modes. |
| **B. Patient Home** | `PatientHomeScreen.kt` | Big icon menu ("Play Game", "My Reminders", "SOS Alert"), NavIC satellite sync chip. |
| **C. Memory Match Game** | `MemoryMatchGameScreen.kt` | 3x4 card grid (12 cards, 6 pairs), smooth flip animations, records accuracy % and reaction time (ms) to local storage. |
| **D. Caretaker Dashboard** | `CaretakerDashboardScreen.kt` | Circular Effectiveness Score ring, NER Patient Location card with Geofence toggle & breach simulation, Satellite Telemetry card. |
| **E. Reminders Screen** | `RemindersScreen.kt` | 3 mock reminders (medication, nutrition, walk) with large 56dp+ checkboxes and voice read-aloud prompts. |

---

## ♿ Elderly-Accessible Design Standards
- **Large Touch Targets**: Minimum 56dp–64dp height for buttons and interactive cards.
- **High-Contrast Typography**: Minimum 20sp+ font size for body text and bold headers.
- **Visual Feedback**: High-contrast color indicators (Green for completed/optimal, Amber for warning, Red for breach/alert).
- **Audio Cue Readiness**: Text-to-Speech simulation dialogs for elderly cognitive reassurance.

---

## 🔄 Live Reactivity Loop
1. **Game $\rightarrow$ Dashboard**: When the patient finishes a round of the 3x4 Memory Match game, accuracy % and reaction time are stored and immediately update the **Effectiveness Score** on the Caretaker Dashboard.
2. **Reminders $\rightarrow$ Dashboard**: Checking off medication or meal items immediately updates adherence scores on the Caretaker console.
3. **Geofence Simulation $\rightarrow$ Alert**: Enabling geofence monitoring and triggering a breach displays a critical alert banner across screens.

---

## 🚀 How to Run in Android Studio
1. Open Android Studio.
2. Select **Open** and choose the `MemoryCare` directory.
3. Allow Gradle to sync dependencies.
4. Select an Android Emulator (API 26+) or physical Android device.
5. Click **Run ('app')** (Shift + F10).
