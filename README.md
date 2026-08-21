# ExtraBan

<p align="center">
  <img src="ExtraBan-Icon.png" alt="ExtraBan Icon" width="256">
</p>

**Advanced moderation plugin for Paper 26.2+**

ExtraBan is a modern Minecraft moderation plugin built for server owners who want powerful staff tools without unnecessary complexity. Manage bans, freezes, and warnings from one clean command hub — with fully customizable messages, action-bar countdowns, and profile-based bans for modern Paper servers.

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2+-green.svg)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-25+-orange.svg)](https://adoptium.net/)
[![Version](https://img.shields.io/badge/Version-2.0.0-blue.svg)](https://github.com/ThemooXi/ExtraBan_Plugin/releases)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Free](https://img.shields.io/badge/Download-Free-brightgreen.svg)](https://github.com/ThemooXi/ExtraBan_Plugin/releases)

> **Free Plugin** — ExtraBan is free to download and use. No purchase required.

---

## Why ExtraBan?

Most moderation plugins feel either outdated or overloaded. **ExtraBan** focuses on what staff teams actually need:

- Fast moderation workflow
- Clear player feedback
- Reliable enforcement
- Full message customization
- Safe config migration on updates

---

## Features

### Ban System
- Permanent bans with custom reasons
- Temporary bans with flexible durations (`1s`, `30m`, `2h`, `7d`, `1w`)
- Profile-based bans for Paper 26.2+
- Ban list viewer with expiry details
- Automatic cleanup of expired temporary bans
- Optional action-bar countdown before execution

### Freeze System
- Temporary or permanent player freeze
- Blocks movement, interaction, inventory access, and damage
- Action bar countdown before freeze is applied
- Auto-unfreeze when the timer expires

### Warning System
- Track player warnings with persistent YAML storage
- View warning history per player
- Remove the latest warning with one command
- Optional visual warning barrier item

### Staff Tools
- Unified command: `/eb`
- Smart tab completion
- Permission-based help menu
- Broadcast messages for bans, freezes, and warnings
- Manual and automatic update checker

### Configuration
- Fully customizable messages
- Automatic config backup before updates
- Version-aware config migration
- Granular permission nodes

---

## Requirements

| Requirement | Version |
|---|---|
| Minecraft | 26.2+ |
| Server Software | Paper (recommended) |
| Java | 25+ |
| Plugin Version | 2.0.0 |

---

## Installation

1. Download the latest free release from [GitHub Releases](https://github.com/ThemooXi/ExtraBan_Plugin/releases)
2. Place `ExtraBan-2.0.0.jar` in your server's `plugins/` folder
3. Restart the server — `config.yml` will be generated automatically
4. Edit `plugins/ExtraBan/config.yml` to customize settings and messages
5. Assign permissions to your staff team (LuckPerms, PermissionsEx, etc.)

---

## Commands

**Main command:** `/eb`  
**Aliases:** `/extraban`, `/eban`, `/extrab`

| Command | Permission | Description |
|---|---|---|
| `/eb ban <player> [reason]` | `extraban.ban` | Permanently ban a player |
| `/eb tban <player> <time> [reason]` | `extraban.tempban` | Temporarily ban a player |
| `/eb unban <player>` | `extraban.unban` | Remove a ban |
| `/eb freeze <player> [time] [reason]` | `extraban.freeze` | Freeze a player |
| `/eb unfreeze <player>` | `extraban.unfreeze` | Unfreeze a player |
| `/eb warn <player> [reason]` | `extraban.warn` | Warn a player |
| `/eb unwarn <player>` | `extraban.warn` | Remove last warning |
| `/eb banlist` | `extraban.banlist` | View banned players |
| `/eb reload` | `extraban.reload` | Reload configuration |
| `/eb update` | `extraban.update` | Check for updates |
| `/eb version` | — | Show plugin version |
| `/eb help` | — | Show command help |

### Time Format Examples
- `1s` — 1 second
- `30m` — 30 minutes
- `2h` — 2 hours
- `7d` — 7 days
- `1w` — 1 week

---

## Permissions

Use `extraban.*` for full access, or assign individual nodes:

| Permission | Description |
|---|---|
| `extraban.ban` | Ban players permanently |
| `extraban.tempban` | Temporarily ban players |
| `extraban.unban` | Unban players |
| `extraban.freeze` | Freeze players |
| `extraban.unfreeze` | Unfreeze players |
| `extraban.warn` | Warn players |
| `extraban.banlist` | View ban list |
| `extraban.reload` | Reload configuration |
| `extraban.update` | Check for updates |

Legacy `extstraban.*` permissions are supported for backward compatibility.

---

## Configuration

Key settings in `config.yml`:

```yaml
config-version: "2.0.0"

update-checker:
  enabled: true
  check-on-startup: true
  notify-console: true
  notify-staff: true

settings:
  general:
    Discord: "https://discord.gg/fJTSA6vnVQ"
    default-reason: "Unfair advantage"
    tempban-max-duration: "30d"

  action-bar:
    enabled: true
    countdown-time: 10
```

All in-game messages, kick screens, help menus, and broadcasts are fully editable under the `messages:` section.

---

## Data Storage

| Data | Location |
|---|---|
| Bans | Paper profile ban list (`banned-players.json`) |
| Warnings | `plugins/ExtraBan/warnings.yml` |
| Config backups | `plugins/ExtraBan/config-backups/` |

---

## Building from Source

```bash
mvn clean package
```

The compiled JAR will be in the `target/` directory.

**Dependencies:**
- Paper API `26.2.build.112-stable`
- Java 25+

---

## Project Structure

```
ExtraBan/
├── src/main/
│   ├── java/com/extraban/
│   │   ├── Main.java
│   │   ├── commands/
│   │   │   ├── ExtraBanCommand.java
│   │   │   ├── ExtraBanTabCompleter.java
│   │   │   ├── ActionBarManager.java
│   │   │   ├── FreezeManager.java
│   │   │   └── WarnManager.java
│   │   └── utils/
│   │       ├── BanUtils.java
│   │       ├── MessageUtils.java
│   │       └── SimpleUpdateChecker.java
│   └── resources/
│       ├── plugin.yml
│       └── config.yml
└── pom.xml
```

---

## Community & Communication

Join the community, report issues, or request features:

| Platform | Link |
|---|---|
| **Discord Account** | `m_1z.4` |
| **Discord Server (Extra P & S)** | [discord.gg/esrAMcSBSR](https://discord.gg/esrAMcSBSR) |
| **Discord Server (ExtraBan Plugin)** | [discord.gg/fJTSA6vnVQ](https://discord.gg/fJTSA6vnVQ) |
| **GitHub Profile** | [github.com/ThemooXi](https://github.com/ThemooXi) |
| **GitHub Repository** | [ExtraBan_Plugin](https://github.com/ThemooXi/ExtraBan_Plugin) |
| **SpigotMC Profile** | [themo_124x](https://www.spigotmc.org/members/themo_124x.1835993/) |
| **SpigotMC Plugin Page** | [ExtraBan Plugin](https://www.spigotmc.org/resources/extraban-plugin.138140/) |

---

## License

MIT License © 2024–2026 **m_1z.4**

Free to use, modify, and distribute. See [LICENSE](LICENSE) for full terms.

---

**ExtraBan** — Making server moderation easier.
