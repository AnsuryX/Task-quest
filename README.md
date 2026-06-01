<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# TaskQuest - RPG Productivity

TaskQuest (formerly Ansury Quest) is a premium gamified productivity application.

## Key Features
- **Eisenhower Matrix Quest Log**: Prioritize tasks like a pro.
- **Focus Chamber**: Pomodoro timer with "Shield" XP.
- **Companion Chronos**: AI-powered insights (Gemini 1.5 Flash).

## Local Setup & Build

**Prerequisites:** [Android Studio Ladybug](https://developer.android.com/studio) or newer.

1. **API Key**: Ensure you have a `.env` file in the root with your `GEMINI_API_KEY`.
2. **Keystore**: The `debug.keystore` has been decoded for you.
3. **Build**:
   - Open the project in Android Studio.
   - Wait for Gradle sync to complete.
   - Click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. **Deploy**: The fresh APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

## Improvements in v1.1
- **Consistent Branding**: Renamed app to TaskQuest throughout.
- **Decoded Keystore**: Ready for immediate local building.
- **Updated API Integration**: Configured to inject your `.env` secrets automatically.
