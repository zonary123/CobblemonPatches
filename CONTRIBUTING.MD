# Contributing to the Project

Thank you for your interest in contributing! This project is a **Cobblemon + Minecraft mod** focused on improving
overall performance through **optimization patches**, fixes, and quality improvements. All contributions are welcome as
long as they follow the guidelines below.

---

## 📌 Prerequisites

Before contributing, ensure you have the following:

* **JDK 21** (Minecraft 1.21.1 requirement)
* Basic knowledge of Java and Minecraft mod development
* Recommended IDE: **IntelliJ IDEA**
* All project dependencies installed (Cobblemon, Fabric/Forge/Architectury, etc.)

---

## 🧱 Project Structure

The project is organized into:

* **Main Mod Module**: Core code and optimization patches
* **Cobblemon Integrations**: Compatibility and performance improvements exclusive to Cobblemon
* **Internal Utilities**: Helpers and shared logic used across systems

Please review the internal documentation before modifying any of these areas.

---

## 🛠️ How to Contribute

### 1. Fork the Repository

Create a fork of the project and clone it locally.

### 2. Create a Feature Branch

Use descriptive branch names:

```
feature/optimize-spawn-system
fix/memory-leak-in-events
issue/123-startup-crash
```

### 3. Follow the Code Style

* Use standard Java conventions
* Keep the code readable and documented
* Avoid premature micro-optimizations without measurable benefits

### 4. Write Clear Commits

* Commit in small, meaningful steps
* Recommended commit message format:

```
[type] Short description

Optional detailed explanation.
```

**Allowed types:** `fix`, `feat`, `perf`, `refactor`, `docs`, `test`

### 5. Test Your Changes

Before submitting a PR:

* Ensure the mod compiles successfully
* Test inside a local Minecraft environment
* Verify that no regressions or new issues were introduced

### 6. Open a Pull Request

Your PR should include:

* A clear description of what was changed
* Why the change was necessary / what issue it fixes
* Logs, benchmarks, or profiling results if it improves performance

---

## 🧪 Testing & Benchmarks

Optimization-related contributions **must include**:

* Before/after performance comparisons
* Explanation of impact (CPU, memory, server tick usage, etc.)
* When applicable: profiling results from tools such as Spark, WarmRoast, YourKit, or JProfiler

---

## 🚫 Contributions That Will NOT Be Accepted

* Obfuscated or intentionally unclear code
* "Placebo" optimizations without evidence
* Unrelated feature changes outside the scope of the project
* Risky changes without proper testing, especially in critical systems

---

## 💬 Communication

Use the official communication channels (issues, discussions, or Discord if available). Please:

* Maintain a respectful tone
* Be clear when reporting issues or suggesting improvements
* Check existing issues before opening new ones

---

## 🤝 Code of Conduct

This project follows a Code of Conduct based on the Contributor Covenant. Any abusive, discriminatory, or toxic behavior
will not be tolerated.

---

## 🙌 Thank You

Your time and effort help make this project better. **Thank you for contributing!**
