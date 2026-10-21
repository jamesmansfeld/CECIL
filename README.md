# Chicken Egg Collection Internet-Free Log (CECIL)
A native Android egg collection tracker with a dark interface, multiple egg types per collection, per-type colours, an all-time total, a week/month/year summary, and week/month/year calendar views. Records are stored in a local SQLite database.

## Offline and privacy

- The app requests **no Android permissions**, including no network permission.
- All egg records are stored in the app's private local database (`egg_counter.db`).
- The device's current local date is used automatically for the initial selection and today's counter.
- No account, server, analytics, or sync is included.
- Android backup is disabled in the manifest.

## Build with Android Studio

1. Install Android Studio (current stable) and its Android SDK Platform 35.
2. Extract this ZIP.
3. In Android Studio, choose **Open** and select the extracted `EggCounterAndroid` folder (the folder containing `settings.gradle`).
4. Allow Gradle to sync. The first build needs internet access to download the Android Gradle Plugin and Android SDK components; **the installed app itself works fully offline**.
5. Connect an Android phone with USB debugging enabled, or create an emulator.
6. Click **Run**. To create an installable APK, choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**. Android Studio shows the APK location when complete.

## How to use

- **+ Log Collection** records eggs against the selected date. Type an egg type name (for example `Chicken` or `Duck`) and a quantity for each type; use **+ Add another type** to log several types in one collection.
- Type names are remembered and suggested as you type. Tap the colour swatch next to a type to choose its colour; choosing a new colour for an existing type updates it everywhere.
- Tap a day on the calendar to select it. Day tiles show coloured eggs for the types collected that day.
- Tap a collection in the **Collections** list to change its per-type quantities. Set a quantity to `0` to remove that type; removing every type deletes the collection.
- Long-press a day tile to log a collection directly for that date.
- Use **Week / Month / Year** to switch calendar layouts and the arrows to move through time. The summary card at the top shows the total and per-type breakdown for the displayed week, month, or year.
- **Edit date** lets you jump directly to a date using `YYYY-MM-DD`.
- The all-time and today totals are calculated from every saved collection, so they update automatically when a collection is logged.

## Requirements and notes

- Minimum Android version: Android 8.0 (API 26).
- Java source, no third-party runtime libraries.
- The current project supports logging and editing collections, egg types with colours, and viewing totals. It does not yet include renaming types outside the log dialog or database export; avoid uninstalling the app if you need to preserve its local records.
- Existing databases from the first version are upgraded automatically; earlier collections appear as the `Eggs` type.
- This is a source project, not a prebuilt APK. Android Studio builds the APK locally.

## License

CECIL is licensed under the GNU General Public License
version 3 or, at your option, any later version (GPL-3.0-or-later).

See the [LICENSE](LICENSE) file for the full license text.
