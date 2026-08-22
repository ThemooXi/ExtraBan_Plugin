# Changelog

All notable changes to this project are documented in this file.

---

## [2.1.0] - 2026-08-22

### Added
- `/eb kick <player> [reason]` with dedicated settings, messages, and 
action-bar countdown

### Fixed
- Config upgrades now write new keys into the existing `config.yml` (no need to delete the plugin folder)
- Restored simple auto-merge: any key in the JAR missing from the live file is added automatically
- Config rewrites keep the same section/key order as the shipped `config.yml`
- Added `config-revision` so same plugin version still merges new settings (e.g. kick)
- `/eb reload` also merges missing keys from the JAR defaults
- Removed `saveConfig()` on disable which could overwrite/skip migrated settings

### Changed
- Redesigned `/eb` help menu with clear sections (Ban, Freeze, Warn, Utility)
- Unified and polished all plugin messages (kick screens, broadcasts, banlist, updates)
- Locked help, errors, usage, banlist, system, update messages, and Discord (not editable via config)
- Reorganized config by feature: settings and messages grouped under ban / freeze / warn / kick

---

## [2.0.0] - 2026-08-20

### Added
- Renamed plugin to **ExtraBan**
- Minecraft 26.2 and modern Paper API support
- Profile-based ban system (Profile Ban List)
- Adventure API messages instead of legacy chat APIs

### Changed
- Upgraded to Java 25
- Set `api-version: 26.2` in plugin.yml
- Replaced deprecated ChatColor and `BanList.Type.NAME` APIs
- Full English localization for plugin messages and documentation
- Free MIT license — no purchase required
- Free downloads via GitHub Releases

---

## [1.17.0] - 2024-12-07

### Added
- `MessageUtils` class for unified messaging
- Comprehensive documentation (README, IMPROVEMENTS, PROJECT_OVERVIEW)
- Full error handling and exception management
- Improved logging with dynamic symbols

### Changed
- NIO API for file copying (2–3x performance improvement)
- Semantic Versioning for version comparison
- Reduced memory usage with Streams and Collections
- Safe resource handling with try-with-resources

### Fixed
- Exception handling in `onEnable()`
- Improved kick messages in ban commands
- Java 21+ compatibility issues
- Unified code standards

---

## Community & Communication

- **Discord Account:** `m_1z.4`
- **Discord Server (Extra P & S):** [discord.gg/esrAMcSBSR](https://discord.gg/esrAMcSBSR)
- **Discord Server (ExtraBan Plugin):** [discord.gg/fJTSA6vnVQ](https://discord.gg/fJTSA6vnVQ)
- **GitHub:** [github.com/ThemooXi](https://github.com/ThemooXi)
- **SpigotMC Profile:** [themo_124x](https://www.spigotmc.org/members/themo_124x.1835993/)
- **Lead Developer:** m_1z.4
