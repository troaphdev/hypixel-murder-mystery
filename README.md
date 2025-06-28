# Murder Mystery Helper

A comprehensive Minecraft 1.8.9 Forge mod that enhances Hypixel Murder Mystery games with visual player highlighting and role detection features.

**Developed by Troaph Innovations**

## ✨ Features

### 🎯 Smart Detection System
- **Automatic Player Role Detection**: Identifies murderers, detectives, and innocents
- **Weapon Tracking**: Comprehensive detection of all murder weapons and detective items
- **Game State Management**: Only activates during active Murder Mystery games
- **Real-time Updates**: Instant detection when players pick up or drop items

### 🌈 Visual Enhancements
- **ESP Highlighting**: Color-coded player outlines (Red: Murderers, Blue: Detectives, Gold: First Bow Holder, Lime: Victims)
- **Tracer Lines**: Visual indicators pointing toward detected murderers
- **Custom Nametags**: Always-visible player names with role-based coloring
- **Gold Item Glow**: Enhanced visibility for gold items with distance-based color gradients

### 📋 UI Improvements
- **Murder List Display**: Top-center overlay showing detected murderers
- **Enhanced Tab List**: Custom player list with role indicators and rank prefixes
- **Clean Integration**: Silent operation with no chat spam

### 🔧 Advanced Features
- **Bow Detection**: Tracks dropped detective bows and pickup events
- **Multi-trigger Reset**: Automatic cleanup when switching games/lobbies
- **Performance Optimized**: Smart caching and efficient scanning algorithms

## 📥 Installation

1. **Download** the latest release from the [Releases](../../releases) page
2. **Install** Minecraft Forge 1.8.9
3. **Place** the mod file in your `mods` folder:
   - Standard Minecraft: `%APPDATA%\.minecraft\mods\`
   - Feather Client: `%APPDATA%\.feather\user-mods\1.8.9\`
4. **Launch** Minecraft and join Hypixel Murder Mystery!

## 🎮 Usage

The mod automatically activates when you join a Hypixel Murder Mystery game and deactivates when you leave. No configuration required!

### Game Detection
- **Game Start**: Activates when seeing the "Teaming with the Detective/Innocents is not allowed!" message
- **Game End**: Deactivates when returning to lobby or switching games
- **Location Aware**: Uses Hypixel's `/locraw` system for precise game detection

## 📋 Requirements

- **Minecraft**: 1.8.9
- **Forge**: 11.15.1.2318+
- **Server**: Hypixel Network

## 🛠️ Development

This mod is built using Minecraft Forge 1.8.9 with ForgeGradle 2.x.

### Building from Source
```bash
git clone https://github.com/troaphdev/hypixel-murder-mystery.git
cd hypixel-murder-mystery
./gradlew build
```

## 📄 License

This project includes code from Minecraft Forge and follows their licensing terms. See the included license files for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

## ⚠️ Disclaimer

This mod is designed for educational and enhancement purposes. Use responsibly and in accordance with Hypixel's rules and policies.

---

**© 2025 Troaph Innovations** 