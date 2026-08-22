# ExtraBan - Developer & Contribution Guide

## For Developers

This guide helps you understand the project structure and contribute effectively.

---

## Development Setup

### Requirements
- **Java JDK 25+**
- **Maven 3.6+**
- **Git** (optional)
- **IDE**: IntelliJ IDEA / VS Code / Eclipse

### Installation

```bash
# Clone the repository
git clone https://github.com/ThemooXi/ExtraBan_Plugin.git
cd ExtraBan_Plugin

# Install dependencies
mvn clean install

# Build the plugin
mvn clean package
```

---

## Folder Structure

```
ExtraBan/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/extraban/
│   │   │       ├── Main.java                     # Main plugin class
│   │   │       ├── commands/                     # Command handlers
│   │   │       │   ├── ExtraBanCommand.java
│   │   │       │   ├── BanCommand.java
│   │   │       │   ├── TempBanCommand.java
│   │   │       │   ├── UnbanCommand.java
│   │   │       │   ├── ActionBarManager.java
│   │   │       │   ├── FreezeManager.java
│   │   │       │   ├── WarnManager.java
│   │   │       │   └── ExtraBanTabCompleter.java
│   │   │       └── utils/                        # Utility classes
│   │   │           ├── BanUtils.java
│   │   │           ├── MessageUtils.java
│   │   │           └── SimpleUpdateChecker.java
│   │   └── resources/
│   │       ├── plugin.yml
│   │       └── config.yml
│   └── test/                                     # Future tests
│
├── pom.xml                                       # Maven build file
├── README.md                                     # Main documentation
├── CHANGELOG.md                                  # Change log
├── IMPROVEMENTS.md                               # Improvements summary
├── PROJECT_OVERVIEW.md                           # Project overview
├── DEVELOPER_GUIDE.md                            # This file
├── LICENSE                                       # License
└── .gitignore                                    # Git ignore rules
```

---

## Code Standards

### Naming Conventions
```java
// Good
private final Main plugin;
private List<String> bannedPlayers;
public void executeCommand() { }

// Bad
private final m p;
private list L;
public void exCmd() { }
```

### Comments
```java
/**
 * Brief description of the method.
 *
 * @param parameter Parameter description
 * @return Return value description
 * @throws Exception Possible exceptions
 */
public String doSomething(String parameter) throws Exception {
    // Comment explaining non-obvious logic
    return result;
}
```

### Error Handling
```java
// Good
try {
    performOperation();
} catch (IOException e) {
    getLogger().log(Level.WARNING, "Operation failed: ", e);
} catch (Exception e) {
    getLogger().log(Level.SEVERE, "Unexpected error: ", e);
}

// Bad
try {
    performOperation();
} catch (Exception e) {
    e.printStackTrace();
}
```

---

## Adding a New Feature

### 1. Planning
- Define requirements clearly
- Sketch the design
- Ensure no conflict with existing features

### 2. Development
```bash
# Create a new branch
git checkout -b feature/new-feature

# Write code following project standards
# Add comments and documentation
```

### 3. Testing
```bash
# Test locally
mvn clean compile

# Verify no build errors
mvn clean package
```

### 4. Commit
```bash
git add .
git commit -m "feat: add new feature description"
git push origin feature/new-feature
```

---

## Testing

### Manual Tests
```java
// Test the command on the server
/eb ban <player> test reason

// Verify results:
// - Player was banned
// - Message displayed correctly
// - Broadcast was sent
```

### Performance Testing
- Use `/gc` to measure memory
- Check response time
- Test with 100+ players

---

## Bug Reports

When you find a bug:

1. **Reproduce the issue**
   - Write clear steps
   - Record version and settings

2. **Collect information**
   ```
   Version: 2.1.0
   Java Version: 25
   Paper Version: 26.2
   Error: [detailed description]
   Steps: [how to reproduce]
   Log: [server.log excerpt]
   ```

3. **Open a new Issue**
   - Clear title
   - Detailed description
   - System information
   - Steps to reproduce

---

## Building Releases

### Development Build
```bash
mvn clean compile
```

### Production Build
```bash
mvn clean package
# Creates ExtraBan-2.1.0.jar in target/
```

### With Shade (bundle dependencies)
```bash
mvn clean package shade:shade
```

---

## Useful Tools

### Maven Commands
```bash
mvn clean          # Clean the project
mvn compile        # Compile only
mvn test           # Run tests
mvn package        # Package JAR
mvn install        # Install to local repo
mvn clean package install
```

---

## Educational Resources

### Beginners
- [Java Programming](https://www.oracle.com/java/technologies/downloads/)
- [Bukkit Plugin Development](https://bukkit.org/threads/bukkit-plugin-development.3152/)

### Advanced
- [Design Patterns in Java](https://refactoring.guru/design-patterns/java)
- [Clean Code Principles](https://www.oreilly.com/library/view/clean-code-a/9780136083238/)

---

## Contribution Rules

1. **Follow code standards**
2. **Write tests for new features**
3. **Document major changes**
4. **Add helpful comments**
5. **Do not break existing features**

---

## Community & Communication

| Platform | Link |
|---|---|
| **Discord Account** | `m_1z.4` |
| **Discord Server (Extra P & S)** | [discord.gg/esrAMcSBSR](https://discord.gg/esrAMcSBSR) |
| **Discord Server (ExtraBan Plugin)** | [discord.gg/fJTSA6vnVQ](https://discord.gg/fJTSA6vnVQ) |
| **GitHub** | [github.com/ThemooXi](https://github.com/ThemooXi) |
| **SpigotMC Profile** | [themo_124x](https://www.spigotmc.org/members/themo_124x.1835993/) |
| **GitHub Issues** | Bug reports and feature requests |

---

## License

This project is licensed under MIT License.  
By contributing, you agree to the license terms.

---

**Thank you for contributing!**
