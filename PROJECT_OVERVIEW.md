# ExtraBan - Project Overview

## Project Information

**Name:** ExtraBan - Advanced Ban & Moderation System  
**Version:** 2.0.0  
**Type:** Paper/Bukkit Plugin  
**Language:** Java 25  
**Minecraft Version:** 26.2+  

---

## Description

**ExtraBan** is a professional Minecraft moderation plugin offering a comprehensive staff toolkit:

### Main Features:

1. **Advanced Ban System**
   - Permanent bans with customizable reasons
   - Temporary bans with flexible durations
   - Professional dynamic kick messages
   - Public broadcast notifications

2. **Flexible Freeze System**
   - Permanent or temporary player freeze
   - Blocks movement and interaction
   - Action bar countdown before execution
   - Automatic unfreeze when timer expires

3. **Warning System**
   - Track player warnings
   - Persistent YAML storage
   - Full warning history display
   - Individual warning removal

4. **Automatic Update Checker**
   - Checks for updates on server startup
   - Notifications for staff and console
   - Smart version comparison

5. **Smart Configuration System**
   - Automatic config backups
   - Config migration while preserving data
   - Fully customizable messages
   - Granular permissions

---

## Architecture

```
ExtraBan/
├── src/main/
│   ├── java/com/extraban/
│   │   ├── Main.java
│   │   │   └─ Main plugin entry point
│   │   │   └─ System and manager initialization
│   │   │   └─ Plugin lifecycle management
│   │   │
│   │   ├── commands/
│   │   │   ├── ExtraBanCommand.java
│   │   │   │   └─ Main command handler
│   │   │   │   └─ Subcommand routing
│   │   │   │
│   │   │   ├── BanCommand.java
│   │   │   │   └─ Permanent ban logic
│   │   │   │   └─ Ban list management
│   │   │   │
│   │   │   ├── TempBanCommand.java
│   │   │   │   └─ Temporary ban handling
│   │   │   │   └─ Duration calculation
│   │   │   │
│   │   │   ├── ActionBarManager.java
│   │   │   │   └─ Action bar countdown
│   │   │   │   └─ Pending operation management
│   │   │   │
│   │   │   ├── FreezeManager.java
│   │   │   │   └─ Full freeze system
│   │   │   │   └─ Event handling
│   │   │   │
│   │   │   ├── WarnManager.java
│   │   │   │   └─ Warning management
│   │   │   │   └─ YAML save/load
│   │   │   │
│   │   │   ├── ExtraBanTabCompleter.java
│   │   │   │   └─ Command tab completion
│   │   │   │
│   │   │   └── UnbanCommand.java
│   │   │       └─ Ban removal
│   │   │
│   │   └── utils/
│   │       ├── BanUtils.java
│   │       │   └─ Profile ban list helpers
│   │       │
│   │       ├── MessageUtils.java
│   │       │   └─ Unified message handling
│   │       │   └─ Duration processing
│   │       │   └─ General helper functions
│   │       │
│   │       └── SimpleUpdateChecker.java
│   │           └─ Update checking
│   │           └─ Update notifications
│   │
│   └── resources/
│       ├── plugin.yml (main plugin config)
│       └── config.yml (default settings)
│
├── pom.xml (Maven build file)
├── README.md (usage guide)
├── IMPROVEMENTS.md (improvements summary)
├── LICENSE (MIT License)
└── .gitignore (version control)
```

---

## Implemented Improvements

### 1. Performance
- NIO API for file operations (2–3x faster)
- Reduced memory usage with Streams
- Efficient event handling

### 2. Security
- Comprehensive exception handling
- Null checks using Objects.requireNonNull()
- Safe resource handling (try-with-resources)
- Detailed error logging

### 3. Maintainability
- New MessageUtils class to reduce duplication
- Comprehensive JavaDoc
- Unified code standards
- Dead code removal

### 4. User Experience
- Clearer messages with symbols (✓, ❌, ⚠, ℹ)
- Professional message designs
- Fully customizable settings

### 5. Documentation
- Comprehensive README.md
- IMPROVEMENTS.md file
- MIT LICENSE
- JavaDoc comments in code

---

## Available Commands

| Command | Permission | Description |
|---|---|---|
| `/eb ban <player> [reason]` | extraban.ban | Permanently ban a player |
| `/eb tban <player> <time> [reason]` | extraban.tempban | Temporarily ban a player |
| `/eb unban <player>` | extraban.unban | Remove a ban |
| `/eb freeze <player> [time] [reason]` | extraban.freeze | Freeze a player |
| `/eb unfreeze <player>` | extraban.unfreeze | Unfreeze a player |
| `/eb warn <player> [reason]` | extraban.warn | Warn a player |
| `/eb unwarn <player>` | extraban.warn | Remove last warning |
| `/eb reload` | extraban.reload | Reload configuration |
| `/eb update` | extraban.update | Check for updates |
| `/eb banlist` | extraban.banlist | View ban list |

---

## Supported Time Formats

- `1s` = 1 second
- `30m` = 30 minutes
- `2h` = 2 hours
- `7d` = 7 days
- `1w` = 1 week

---

## Technical Requirements

### To Run the Plugin:
- **Java**: 25 or newer
- **Minecraft Server**: 26.2+ (Paper recommended)
- **Memory**: 128 MB minimum

### For Development:
- **Maven**: 3.6+
- **Java JDK**: 25+

---

## Getting Started

1. **Download the Plugin**
   ```bash
   mvn clean package
   ```

2. **Install**
   - Copy `ExtraBan-2.0.0.jar` to `plugins/`
   - Restart the server

3. **Configure**
   - Edit `plugins/ExtraBan/config.yml`
   - Assign permissions via your permission manager

4. **Use**
   - Use available commands
   - Customize messages and settings

---

## Project Statistics

- **Java Files**: 12+
- **Lines of Code**: ~4000+
- **Main Classes**: 8+
- **Subcommands**: 11
- **Permissions**: 8+

---

## Security & Privacy

- No player data sent to external servers (except optional update check)
- All data stored locally
- Safe input handling
- Comprehensive operation logging

---

## Community & Communication

| Platform | Link |
|---|---|
| **Discord Account** | `m_1z.4` |
| **Discord Server (Extra P & S)** | [discord.gg/esrAMcSBSR](https://discord.gg/esrAMcSBSR) |
| **Discord Server (ExtraBan Plugin)** | [discord.gg/fJTSA6vnVQ](https://discord.gg/fJTSA6vnVQ) |
| **GitHub** | [github.com/ThemooXi](https://github.com/ThemooXi) |
| **SpigotMC Profile** | [themo_124x](https://www.spigotmc.org/members/themo_124x.1835993/) |
| **SpigotMC Plugin Page** | [Plugin Page](https://www.spigotmc.org/resources/exstraban-plugin.119078/) |

---

## License

This project is licensed under **MIT License**  
All rights reserved © 2024–2026 m_1z.4

---

## Current Status

| Feature | Status | Notes |
|---|---|---|
| Bans | Complete | Permanent and temporary |
| Freeze | Complete | With action bar countdown |
| Warnings | Complete | With persistent storage |
| Update Checker | Complete | Automatic check |
| Configuration | Complete | Smart backup |
| Messages | Complete | Fully customizable |

---

## Special Thanks

Thank you for using **ExtraBan**!  
If you encounter any issues or have suggestions, feel free to reach out.

---

**Made with care for the Minecraft Community**
