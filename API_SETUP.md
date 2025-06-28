# Hypixel API Key Setup

## Getting Your API Key

The mod requires a valid Hypixel API key to fetch player levels. Follow these steps:

### 1. Go to Hypixel Developer Dashboard
Visit: https://developer.hypixel.net/

### 2. Login
Login using your Hypixel forums account (same as your Minecraft account)

### 3. Create Development Key
Click "Create Development Key" to enable the "Create App" button

### 4. Create Personal API Key
- Click "Create App" in the top right
- Select "Personal API Key" 
- Fill out the form (you can put any website URL)
- Submit the application

### 5. Get Your Key
Once approved (usually instant), copy your API key from the dashboard

### 6. Update the Mod
1. Open `src/main/java/me/Troaph/TroaphMod.java`
2. Find this line:
   ```java
   private static final String HYPIXEL_API_KEY = "YOUR_API_KEY_HERE";
   ```
3. Replace `YOUR_API_KEY_HERE` with your actual API key:
   ```java
   private static final String HYPIXEL_API_KEY = "12345678-1234-1234-1234-123456789abc";
   ```
4. Rebuild the mod: `./gradlew build`
5. Copy to mods folder: `copy "build\libs\modid-1.0.jar" "$env:APPDATA\.minecraft\mods\"`

## Rate Limits
- Personal API keys have a limit of 300 requests per 5 minutes
- The mod automatically handles rate limiting
- Keys last forever (unlike old temporary keys)

## Troubleshooting
- If you get "Rate limited" messages immediately, your key is invalid
- Make sure you're using the new developer dashboard, not old keys
- Check the console for error messages during mod startup 