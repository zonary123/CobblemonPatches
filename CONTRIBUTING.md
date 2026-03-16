¡Perfecto! Aquí tienes la versión **en inglés y lista para GitHub en Markdown**, con un estilo profesional, aviso legal
y CLA integrado:

````markdown
# 🛠 Contributing to the Cobblemon Optimization Mod

> **Legal Notice:** This project is **All Rights Reserved**.
> By submitting a Pull Request (PR), you agree to grant a license for your contribution under
the [Contributor License Agreement (CLA)](#cla).

Thank you for your interest in contributing! This project focuses on **performance optimization**, fixes, and quality
improvements for **Cobblemon + Minecraft**. All contributions are welcome as long as they follow the guidelines below.

---

## 📜 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Structure](#project-structure)
3. [How to Contribute](#how-to-contribute)
4. [Testing & Benchmarks](#testing--benchmarks)
5. [Contributions We Will NOT Accept](#contributions-we-will-not-accept)
6. [Communication & Conduct](#communication--conduct)
7. [Contributor License Agreement (CLA)](#cla)
8. [Thank You](#thank-you)

---

## 📌 Prerequisites

Before contributing, make sure you have:

- **JDK 21** (required by Minecraft 1.21.1)
- Basic knowledge of Java and Minecraft mod development
- Recommended IDE: **IntelliJ IDEA**
- All project dependencies installed (Cobblemon, Fabric/Forge/Architectury, etc.)

---

## 🧱 Project Structure

The project is organized into modules:

- **Main Mod Module**: Core code and optimization patches
- **Cobblemon Integrations**: Cobblemon-specific compatibility and performance improvements
- **Internal Utilities**: Helper functions and shared logic

> **Note:** Review internal documentation before modifying critical areas.

---

## 🛠 How to Contribute

### 1. Fork & Clone

Create a fork of the repository and clone it locally:

```bash
git clone https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git
````

### 2. Create a Feature Branch

Use descriptive branch names:

```text
feature/optimize-spawn-system
fix/memory-leak-in-events
issue/123-startup-crash
```

### 3. Follow Code Style

* Use standard Java conventions
* Keep code readable and documented
* Avoid premature micro-optimizations without evidence

### 4. Commit Guidelines

* Commit in small, meaningful steps
* Recommended commit message format:

```text
[type] Short description

Optional detailed explanation
```

**Allowed types:** `fix`, `feat`, `perf`, `refactor`, `docs`, `test`

### 5. Testing

Before submitting a PR:

* Ensure the mod compiles successfully
* Test inside a local Minecraft environment
* Verify no regressions or new issues are introduced

### 6. Pull Requests

Your PR should include:

* Clear description of changes
* Reason and context for the change
* Logs, benchmarks, or profiling results if performance is affected

---

## 🧪 Testing & Benchmarks

For optimization-related contributions:

* Include before/after performance comparisons
* Explain impact on CPU, memory, server ticks, etc.
* Provide profiling results when applicable (Spark, WarmRoast, YourKit, JProfiler)

---

## 🚫 Contributions We Will NOT Accept

* Obfuscated or intentionally unclear code
* "Placebo" optimizations without evidence
* Changes outside the project scope
* Risky changes without proper testing

---

## 💬 Communication & Conduct

* Use official channels (issues, discussions, Discord)
* Maintain respectful and clear communication
* Check existing issues before opening new ones

> This project follows a Code of Conduct based on the **Contributor Covenant**.

---

## 🤝 Contributor License Agreement (CLA)

By submitting a PR, you agree to:

1. Grant the project owner a **perpetual, worldwide, royalty-free license** to use, modify, and distribute your
   contribution.
2. Confirm that your contribution is your original work or that you have the right to submit it.
3. Understand that the project remains **All Rights Reserved** and you do not acquire ownership of the project itself.

> See [`CLA.md`](CLA.md) for the full agreement if available.

---

## 🙌 Thank You

Thank you for your time and effort! Your contributions help improve the project and the community.
