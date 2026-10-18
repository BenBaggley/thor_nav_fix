# Thor Nav Fix

**Thor Nav Fix** is a lightweight Android utility designed specifically for the **AYN Thor** handheld gaming console.

---

## The Problem

On the AYN Thor, the navigation gesture bar on the secondary (lower) screen serves no practical purpose. Due to an OEM firmware quirk, this gesture bar automatically becomes visible again whenever the device:
- Wakes up from sleep (screen on / display wake),
- Is unlocked by the user, or
- Reboots,

even if navigation bar settings were configured to keep it hidden.

---

## The Solution

OEM firmware registers a `ContentObserver` on the global setting `second_disable_back_gesture` (representing the system toggle **"Disable the navigation bar on the secondary screen"**). Whenever this value changes, Android's `SystemUI` triggers `updateNavbarForSecondaryDisplay()`, immediately hiding the secondary navigation bar.

**Thor Nav Fix** fixes the issue by temporarily toggling `second_disable_back_gesture` and restoring its desired state. This forces SystemUI to re-evaluate the secondary screen layout and suppress the unwanted gesture bar seamlessly.

---

## Features

- **Automated Wake & Unlock Watcher**: An unobtrusive background Accessibility Service (`ThorFixService`) observes screen wake and user unlock events to re-hide the bar instantly.
- **Boot Completion Trigger**: Automatically cleans up the secondary navigation bar when the device finishes booting.
- **Configurable Triggers**: Enable or disable automated triggers individually (Screen Wake, Device Unlock, Device Boot) to suit your workflow.
- **Quick Settings Tile**: Includes a Quick Settings tile so you can manually re-apply the fix at any time from the notification shade with one tap.
- **Manual "Apply Fix Now"**: Test or trigger the fix immediately within the app.
- **1-Tap Permission Setup**:
  - **Shizuku**: 1-tap grant without root using privileged ADB APIs.
  - **Root (One-time)**: 1-tap grant using `su` (root is only used once to grant the permission; never required again).
  - **ADB Command**: Copy the standard `pm grant` shell command directly from the app.
- **Clean & Lightweight**: Built with Jetpack Compose, Material 3, Kotlin Coroutines, and DataStore Preferences. No polling loops, no analytics, no battery drain.

---

## Getting Started

### 1. Install the APK
Download or build the latest APK and install it on your AYN Thor.

### 2. Grant `WRITE_SECURE_SETTINGS`
To modify `Settings.Global`, Android requires the `WRITE_SECURE_SETTINGS` permission. You only need to grant this **once** using one of the following methods:

#### Option A: Shizuku (Recommended, No Root Needed)
1. Install and start [Shizuku](https://shizuku.rikka.app/) (via Wireless Debugging).
2. Open **Thor Nav Fix** and tap **Grant via Shizuku**.
3. Authorize the Shizuku prompt when requested.

#### Option B: Root (One-time)
If your device is rooted with Magisk or KernelSU:
1. Tap **Grant via Root**.
2. Grant superuser access in the prompt (One time grant will suffice).
3. Root permission is never accessed again after this grant.

#### Option C: ADB Command
If you do not use Shizuku or root, connect your AYN Thor to a PC with USB debugging enabled and run:
```bash
adb shell pm grant dev.benkidd.thornav android.permission.WRITE_SECURE_SETTINGS
```

---

### 3. Enable the Background Watcher Service
To automate hiding the navigation bar when waking from sleep:
1. In the app, locate the **Wake Watcher Service** status card.
2. Tap **Accessibility Settings** (or **Enable in Accessibility**).
3. Find **Thor Nav Fix** in the list of installed services and turn it **ON**.

*(Note: The Accessibility Service is used solely as a lightweight lifecycle container to listen for screen on and unlock intents without battery impact.)*

---

## How It Works Under the Hood

```
+-----------------------------------------------------------+
| Event: Screen Wake / Unlock / Boot / Quick Tile / Manual  |
+-----------------------------------------------------------+
                             |
                             v
+-----------------------------------------------------------+
| PermissionsHelper.applyFix()                             |
|                                                           |
| 1. Read current value of 'second_disable_back_gesture'    |
| 2. Write inverse value to Settings.Global                 |
| 3. Wait 200ms                                             |
| 4. Restore original value to Settings.Global              |
+-----------------------------------------------------------+
                             |
                             v
+-----------------------------------------------------------+
| AYN Thor SystemUI ContentObserver fires:                  |
| -> updateNavbarForSecondaryDisplay()                      |
| -> Secondary gesture bar is hidden!                       |
+-----------------------------------------------------------+
```

---

## AI Disclosure
AI assistance was used in the development of this app, specifically Gemini 3.8 Flash.

Gemini did the heavy lifting in regard to investigating the root cause of the issue and possible solutions.

The initial version of the app was generated as proof of concept, which was used as a base and went through significant refactoring and polish.