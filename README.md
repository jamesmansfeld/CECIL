# Chicken Egg Collection Internet-Free Log (CECIL)
A native Android egg collection and incubation tracker with a dark interface, chicken breeds with custom egg colours, full HSV colour selection, unique per-egg incubation codes, an all-time total, week/month/year summaries, and week/month/year calendar views. Records are stored in a local SQLite database.

## Pages

Use the hamburger menu in the top bar to switch between:

- **Collections** — egg collection log.
- **Hatching** — incubation log with started, failed, successful, and aborted outcomes.
- **Settings** — chicken breeds and HSV egg colours, default calendar view, data summary, clear-all, and app information.

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

- Open **Settings** and use **+ Add breed** to create chicken breeds. Enter the breed name and tap the egg to choose its colour with the full HSV picker (saturation/brightness square and hue bar). Tap an existing breed to rename it, change its colour, or delete it. Deleting a breed also removes its entries from existing logs, with a confirmation showing how many logs are affected.
- **+ Log Collection** records eggs against the selected date. Choose a saved breed and a quantity; use **+ Add another breed** to log several breeds in one collection.
- Only breeds saved in **Settings** appear when logging a collection or incubation. If no breeds exist yet, the app offers to open Settings.
- Tap a day on the calendar to select it. Day tiles show coloured eggs for the breeds collected that day.
- Tap a collection in the **Collections** list to change its per-breed quantities. Set a quantity to `0` to remove that breed, or use **Delete** to remove the whole collection.
- Long-press a day tile to log a collection directly for that date.
- Open **Hatching** from the hamburger menu and use **+ Log Incubation** to record an incubation. Choose **Incubation started**, **Incubation failed**, **Incubation successful**, or **Incubation aborted**.
- When logging **Incubation started**, choose the breeds and quantities. Every egg receives a unique code made from the breed's first letter plus four digits, for example `S0001` for Silkie and `I0001` for Isa Brown. Numbers are unique within each breed, and full code strings never repeat (breeds that share an initial receive different numbers). Codes are shown in the log row.
- When logging **Incubation failed**, **Incubation successful**, or **Incubation aborted**, search for an egg code or its four-digit number and select the started egg (or eggs) the outcome applies to. The outcome log stores and displays the same code, linking it back to the egg that was started.
- New incubation logs default to the device's current date and time. Edit the date field to log a past date; future dates are rejected. The time is recorded from the device clock when you save.
- Tap an incubation log to edit it. Started logs can change date, breeds, and quantities; changing quantities adds or removes generated codes. Outcome logs can change status, date, and selected egg codes. **Delete** removes the log; deleting an outcome returns its codes to incubation, while deleting a started log also removes outcome logs linked to its eggs.
- Only eggs still in incubation appear in outcome code searches. Once an egg has a failed, successful, or aborted outcome, it cannot be selected for another outcome.
- Use **Week / Month / Year** to switch calendar layouts and the arrows to move through time. The summary card at the top shows totals and per-breed breakdowns for the displayed week, month, or year.
- **Edit date** lets you jump directly to a date using `YYYY-MM-DD`.
- **Settings** also sets the default Week/Month/Year calendar view, shows collection and incubation counts, and offers **Clear all logs** with confirmation. Breeds and their colours are kept when logs are cleared.
- Collection and hatching totals update automatically when logs are added, edited, or deleted.

## Requirements and notes

- Minimum Android version: Android 8.0 (API 26).
- Java source, no third-party runtime libraries.
- The current project supports chicken breeds with full HSV egg colours, including creating, editing, and deleting breeds, unique incubation egg codes, outcome logs linked by code, and logging/editing/deleting collections and incubation logs. Database export is not included; avoid uninstalling the app if you need to preserve its local records.
- Existing databases are upgraded automatically. Types created by earlier versions appear as breeds in **Settings** and can be renamed or recoloured. Earlier collections without a breed appear as `Eggs`; earlier started incubation logs are assigned egg codes during the upgrade.
- This is a source project, not a prebuilt APK. Android Studio builds the APK locally.

## License

CECIL is licensed under the GNU General Public License
version 3 or, at your option, any later version (GPL-3.0-or-later).

See the [LICENSE](LICENSE) file for the full license text.
