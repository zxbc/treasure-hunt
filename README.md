# Treasure Hunt

An Android app that turns a list of named locations into a proximity "hotter or colder" clue machine. Activate a hunt and the phone buzzes when you come within your chosen alert radius of your closest target — and the buzzing gets faster and more urgent the closer you get.

## Features

- **Hunt lists** — each list is a saved set of spots (name, description/clue, latitude, longitude), stored in a Room database.
- **JSON import** — create or extend a list from a JSON file via the system file picker (see format below). Spots can also be entered by hand.
- **Built-in list** — the Kolodko mini statues of Budapest (48 spots, bundled as `app/src/main/assets/kolodko-mini-statues.json`) are imported automatically on first launch, so you can start hunting without importing anything. Deleting the list keeps it deleted; the app remembers it was already imported.
- **Proximity buzzing** — a foreground location service tracks you and buzzes (vibration only, no sound) whenever you're within the alert radius of your closest *unmuted* spot. If several spots are in range, the closest one wins.
- **Urgency scaling** — buzz pulses get longer, more frequent and more numerous as you close in (COLD → WARM → WARMER → HOT → BURNING HOT). The main screen shows a live gauge with distance.
- **Per-spot muting** — "Mute nearest" silences the currently closest spot (persisted per list); mute it again later from the hunt screen. You can also stop the whole hunt.
- **Configurable radius** — slider in Settings, 100 m to 2000 m, default 500 m, persisted.
- **Live notification** — while a hunt runs you always see the current target, distance and urgency, with *Mute nearest* / *Stop hunt* buttons, so it works with the screen off.
- **Compass** — a direction rose on the hunt screen points at the nearest active spot; the bearing is computed from your location and tracked with the phone's rotation-vector sensor, and the needle takes the current urgency color.

## JSON format

The import accepts either an object or a bare array of locations:

```json
{
  "name": "Harbor Hunt",
  "locations": [
    { "name": "Old Lighthouse", "description": "The first beacon, long dark", "lat": 48.8566, "lon": 2.2919 },
    { "name": "Copper Anchor", "description": "Where the sea keeps its coins", "coordinates": [48.8606, 2.3376] }
  ]
}
```

Accepted key aliases: `lat`/`latitude` and `lon`/`lng`/`long`/`longitude`, or `"coordinates"`/`"coords"`/`"geo"`/`"position"` as a `[lat, lon]` array; `"name"`/`"title"`/`"label"`; `"description"`/`"hint"`/`"clue"`/`"details"`. A bare array uses the file name as the list name. See `sample-data/example-hunt.json`.

## Build

Requires JDK 17 and the Android SDK (API 34, build-tools 34.0.0):

```sh
# from this directory
gradle assembleDebug        # or use the included wrapper once generated
# APK lands in app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in Android Studio (Hedgehog or newer) and hit Run.

## Permissions

- **Fine location** — requested when you start a hunt; the hunt cannot run without it.
- **Notifications** (Android 13+) — for the live hunt notification; the buzzing works without it, but you lose the on-screen-away status and notification buttons.
- The hunt runs as a **foreground service with a wake lock**, so alerts keep coming with the screen off. To make this bulletproof on your device, consider excluding the app from battery optimization in Android settings.

## Project layout

- `app/src/main/kotlin/com/treasurehunt/app/data/` — Room entities/DAO, repository, JSON parser, built-in list import
- `app/src/main/assets/` — hunt lists bundled with the APK (imported automatically on first launch)
- `app/src/main/kotlin/com/treasurehunt/app/hunt/` — `HuntEngine` (state, nearest-spot math, buzz patterns) and `HuntService` (foreground location service)
- `app/src/main/kotlin/com/treasurehunt/app/ui/` — Compose screens: hunt gauge, lists, editor, settings
