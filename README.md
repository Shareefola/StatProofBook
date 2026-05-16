# StatProof

**Offline Android application for generating, exploring, and verifying step-by-step mathematical statistical proofs.**

[![Android CI](https://github.com/your-org/StatProof/actions/workflows/android-build.yml/badge.svg)](https://github.com/your-org/StatProof/actions/workflows/android-build.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-purple.svg)](https://kotlinlang.org/)

---

## Overview

StatProof is a fully **offline-first** Android application that provides rigorous, step-by-step mathematical derivations for statistical theorems. No internet connection is required after installation — all proof generation, rendering, and search run entirely on-device.

### Features

- **20+ canonical proof derivations** covering Probability Theory, Distribution Theory, Statistical Inference, Bayesian Statistics, Regression Models, and more
- **Native Kotlin symbolic algebra engine** — custom AST with simplification, LaTeX generation, and rule-based transformations
- **Offline KaTeX rendering** — high-quality mathematical typesetting via bundled KaTeX assets
- **Five proof modes** — Standard, Beginner (verbose), Compact, Exam, and Formal
- **Expandable substeps** — drill down into any algebraic manipulation on demand
- **Full-text search** with topic and difficulty filters using Room FTS4
- **Dark mode** support with Material You dynamic colour (Android 12+)
- **No telemetry**, no analytics, no accounts, no internet permission

---

## Architecture

StatProof follows **Clean Architecture** with an **MVVM** presentation layer:

```
app/                  — Compose UI, ViewModels, Hilt DI, Navigation
domain/               — Use cases, repository interfaces (pure JVM)
data/                 — Room database, DataStore, repository implementations
proofengine/          — Symbolic algebra AST, simplification, LaTeX, rule engine
core/                 — Shared utilities and coroutine helpers
```

### Key Technology Decisions

| Concern | Choice | Reason |
|---|---|---|
| UI | Jetpack Compose + Material 3 | Modern declarative Android UI |
| DI | Hilt | Compile-time verified, Jetpack integration |
| Symbolic Engine | Native Kotlin AST | No JNI, fully auditable, tailored to statistics |
| Math Rendering | KaTeX (offline WebView) | Best quality, MIT licence, fully offline |
| Database | Room + FTS4 | Built-in full-text search, no extra deps |
| Preferences | DataStore | Coroutine-native replacement for SharedPreferences |
| Async | Coroutines + Flow | Kotlin-native, Compose-compatible |
| Serialization | kotlinx.serialization | Kotlin-native, no reflection |

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full architecture reference.

---

## Building

### Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Hedgehog or newer |
| JDK | 17 (Temurin recommended) |
| Android SDK | API 35 (compile), API 26+ (device) |
| Gradle | 8.9 (via wrapper — no installation needed) |

### Step 1: Clone the repository

```bash
git clone https://github.com/your-org/StatProof.git
cd StatProof
```

### Step 2: Download KaTeX assets (required)

KaTeX fonts and JS are not committed to the repository. Run the setup script:

```bash
chmod +x scripts/download_katex.sh
./scripts/download_katex.sh
```

This downloads KaTeX 0.16.x and places it under `app/src/main/assets/katex/`.

### Step 3: Build debug APK

```bash
./gradlew assembleDebug
```

The APK is output to: `app/build/outputs/apk/debug/app-debug.apk`

### Step 4: Run unit tests

```bash
./gradlew test
```

### Step 5: Run static analysis

```bash
./gradlew ktlintCheck detekt lint
```

---

## Release Build

### Generating a Signing Keystore

Use the provided script to generate a release keystore:

```bash
chmod +x scripts/generate_keystore.sh
./scripts/generate_keystore.sh
```

This generates `statproof-release.jks` and prints the base64-encoded value for GitHub Secrets.

> ⚠️ **Never commit the `.jks` file to version control.**

### Building a Signed Release APK

```bash
export ANDROID_KEYSTORE_PATH=/path/to/statproof-release.jks
export ANDROID_KEYSTORE_PASSWORD=your_keystore_password
export ANDROID_KEY_ALIAS=statproof-key
export ANDROID_KEY_PASSWORD=your_key_password

./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

---

## GitHub Actions Setup

The CI/CD pipeline runs automatically on every push and pull request to `main`.

### Required GitHub Secrets

Navigate to: **Repository → Settings → Secrets and variables → Actions**

Add the following secrets:

| Secret Name | Description |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded `.jks` keystore file (output by `generate_keystore.sh`) |
| `ANDROID_KEYSTORE_PASSWORD` | Password for the keystore |
| `ANDROID_KEY_ALIAS` | Key alias (default: `statproof-key`) |
| `ANDROID_KEY_PASSWORD` | Password for the specific key |

### Pipeline Jobs

1. **Static Analysis** — ktlint + detekt
2. **Unit Tests** — all `:proofengine`, `:domain`, `:data` tests
3. **Android Lint** — resource and API compatibility checks
4. **Release Build** — assembleRelease + keystore signing
5. **Upload Artifact** — signed APK available in GitHub Actions artifacts

The signed APK is uploaded as `statproof-release-apk` and retained for 30 days.

---

## Project Structure

```
StatProof/
├── .github/workflows/android-build.yml   # CI/CD pipeline
├── app/                                   # Android application module
│   ├── src/main/
│   │   ├── assets/
│   │   │   ├── katex/                    # KaTeX offline rendering assets
│   │   │   └── proofs/                   # JSON theorem database
│   │   │       ├── probability_theory.json
│   │   │       ├── distribution_theory.json
│   │   │       ├── statistical_inference.json
│   │   │       ├── bayesian_statistics.json
│   │   │       ├── regression_models.json
│   │   │       └── ... (10 topic files)
│   │   └── java/com/statproof/app/
│   │       ├── di/                       # Hilt modules
│   │       ├── navigation/               # NavGraph + routes
│   │       └── ui/                       # Screens and components
├── proofengine/                          # Pure Kotlin symbolic engine
│   └── src/main/java/com/statproof/proofengine/
│       ├── ast/ExpressionNode.kt         # Sealed AST hierarchy
│       ├── simplifier/                   # Algebraic simplification
│       ├── latex/                        # LaTeX code generation
│       ├── rules/                        # Transformation rule registry
│       ├── engine/                       # Proof generation
│       ├── verification/                 # Proof validation
│       └── models/                       # Domain data models
├── domain/                               # Use cases + repository interfaces
├── data/                                 # Room DB + repository implementations
├── core/                                 # Shared utilities
├── scripts/
│   ├── download_katex.sh                 # KaTeX asset downloader
│   └── generate_keystore.sh             # Release keystore generator
├── docs/ARCHITECTURE.md                  # Full architecture reference
├── detekt.yml                            # Detekt static analysis config
├── .editorconfig                         # Code style config
├── gradle/libs.versions.toml            # Version catalog
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
└── LICENSE                               # Apache 2.0
```

---

## Adding New Theorems

Theorems are defined as JSON in `app/src/main/assets/proofs/`. Each file is a JSON array of `TheoremDefinition` objects.

### Theorem JSON Schema

```json
{
  "id": "unique_snake_case_id",
  "title": "Human-readable theorem title",
  "topic": "probability_theory",
  "subtopic": "Conditional Probability",
  "difficulty": "undergraduate",
  "statement": "\\text{LaTeX statement of the theorem}",
  "conclusion": "\\text{LaTeX conclusion}",
  "tags": ["tag1", "tag2"],
  "assumptions": ["Assumption 1", "Assumption 2"],
  "searchKeywords": ["keyword1", "keyword2"],
  "intuition": "Plain-language explanation.",
  "pitfalls": ["Common mistake 1"],
  "derivationSteps": [
    {
      "stepNumber": 1,
      "expression": "\\text{LaTeX expression at this step}",
      "justification": "Rule Name",
      "explanation": "Why this step is valid.",
      "ruleId": "rule_registry_id",
      "assumptions": [],
      "hints": ["Helpful hint for beginners"],
      "substeps": []
    }
  ],
  "examples": [],
  "references": [],
  "relatedIds": []
}
```

### Valid `topic` values

`probability_theory` | `distribution_theory` | `statistical_inference` | `hypothesis_testing` | `regression` | `bayesian` | `sampling_theory` | `stochastic_processes` | `information_theory` | `asymptotics`

### Valid `difficulty` values

`elementary` | `undergraduate` | `advanced_undergraduate` | `graduate` | `research`

### Valid `ruleId` values (Rule Registry)

| ID | Rule |
|---|---|
| `add_zero` | a + 0 = a |
| `mul_one` | a × 1 = a |
| `mul_zero` | a × 0 = 0 |
| `log_product` | ln(ab) = ln(a) + ln(b) |
| `log_power` | ln(xⁿ) = n ln(x) |
| `log_fraction` | ln(a/b) = ln(a) - ln(b) |
| `log_exp` | ln(eˣ) = x |
| `exp_sum` | e^(a+b) = eᵃ eᵇ |
| `expectation_linearity_sum` | E[X+Y] = E[X]+E[Y] |
| `expectation_linearity_constant` | E[cX] = c E[X] |
| `expectation_constant` | E[c] = c |
| `variance_definition` | Var(X) = E[X²] - E[X]² |
| `variance_constant` | Var(c) = 0 |
| `covariance_definition` | Cov(X,Y) = E[XY] - E[X]E[Y] |
| `covariance_symmetry` | Cov(X,Y) = Cov(Y,X) |
| `transpose_product` | (AB)ᵀ = BᵀAᵀ |
| `transpose_transpose` | (Aᵀ)ᵀ = A |
| `power_product` | (ab)ⁿ = aⁿbⁿ |
| `complete_the_square` | Completing the square |
| `definition` | By definition |
| `axiom` | Axiom / given |

After adding new JSON, the database auto-seeds on next launch.

---

## Testing

### Unit Tests (JVM — fast, no emulator)

```bash
# All modules
./gradlew test

# Proof engine only
./gradlew :proofengine:test

# Domain only
./gradlew :domain:test
```

Key test classes:
- `SymbolicEngineTest` — 50+ tests covering AST, simplification, LaTeX, rules
- `ProofEngineTest` — proof generation, modes, expansion, determinism
- `VerificationEngineTest` — proof chain validation
- `CanonicalDerivationTest` — 20 canonical theorems × 4 property tests = 80 assertions

### Instrumented Tests (requires Android emulator/device)

```bash
./gradlew connectedAndroidTest
```

Key test classes:
- `NavigationTest` — end-to-end navigation smoke tests

### Static Analysis

```bash
./gradlew ktlintCheck   # Formatting
./gradlew detekt        # Code quality
./gradlew lint          # Android resource/API lint
```

---

## Proof Modes

| Mode | Description |
|---|---|
| **Standard** | Default academic presentation |
| **Beginner** | All substeps expanded, hints shown |
| **Compact** | Top-level steps only, minimal explanations |
| **Exam** | Clean format without extended commentary |
| **Formal** | All assumptions stated, maximum rigor |

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Add theorems to the appropriate JSON file in `app/src/main/assets/proofs/`
4. Add unit tests for any new engine functionality
5. Run `./gradlew ktlintFormat detekt test` and fix any issues
6. Open a pull request with a clear description

### Code Style

- Kotlin: follow `ktlint` rules (enforced in CI)
- Maximum line length: 140 characters
- All public functions require KDoc
- No `TODO` or `FIXME` in merged code

---

## Security

- **No INTERNET permission** declared in `AndroidManifest.xml`
- **No telemetry**, analytics, or crash reporting
- **No external APIs** or remote services of any kind
- **ProGuard/R8** enabled for release builds (strips debug info, obfuscates)
- **Keystore** managed via GitHub Secrets — never committed to the repository
- All data stays on-device: Room database + asset files

---

## Acknowledgements

StatProof is built on the shoulders of:

- [KaTeX](https://katex.org/) (MIT) — offline LaTeX rendering
- [Jetpack Compose](https://developer.android.com/compose) (Apache 2.0) — UI framework
- [Hilt](https://dagger.dev/hilt/) (Apache 2.0) — dependency injection
- [Room](https://developer.android.com/training/data-storage/room) (Apache 2.0) — SQLite persistence
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) (Apache 2.0) — JSON
- [Turbine](https://github.com/cashapp/turbine) (Apache 2.0) — Flow testing
- [Mockk](https://mockk.io/) (Apache 2.0) — Kotlin mocking

---

## License

```
Copyright 2024 StatProof Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

See [LICENSE](LICENSE) for the full text.
