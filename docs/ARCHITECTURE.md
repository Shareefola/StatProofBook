# StatProof — Architecture Reference Document
## Phase 0: Master Planning

---

## 1. Project Overview

StatProof is an **offline-first Android application** for generating, exploring, verifying,
and interactively expanding mathematically rigorous step-by-step statistical proofs.

All proof generation, symbolic algebra, rendering, and search occur entirely on-device.
No network access is required after installation.

---

## 2. Architectural Pattern: Clean Architecture + MVVM

### Decision: Clean Architecture with MVVM presentation layer

**Rationale:**
- Clean Architecture enforces strict separation of concerns via domain/data/presentation layers
- The symbolic engine is a pure domain concern with no Android framework dependencies
- The proof database is a data-layer concern swappable without touching UI
- MVVM (ViewModel + StateFlow) integrates naturally with Jetpack Compose
- Testability: domain and data modules are pure JVM, fast to unit test

**Layer Responsibilities:**

```
Presentation (app module)
  ↓ calls
Domain (domain module)       ← Business logic, use cases, repository interfaces
  ↓ calls
Data (data module)            ← Room DB, asset loading, repository implementations
  ↓ uses
ProofEngine (proofengine module)  ← Symbolic algebra, proof generation, LaTeX
Core (core module)           ← Shared utilities, DI primitives, extensions
```

---

## 3. Dependency Injection: Hilt

**Decision: Hilt**

**Rationale over Koin:**
- Hilt is compile-time verified; Koin is runtime-verified
- Hilt integrates with Jetpack ViewModel out of the box
- Hilt generates no reflection at runtime (better performance)
- Hilt is the Google-recommended DI framework for Android
- Better tooling support (AS IDE integration, component scoping)

---

## 4. Symbolic Engine Strategy: Native Kotlin AST Engine

**Decision: Option A — Native Kotlin symbolic algebra engine**

**Rationale:**
- No JNI, no native .so files, no ABI compatibility issues
- Deterministic and reproducible on all Android versions
- Fully auditable, no GPL contamination
- Can be unit-tested as pure JVM without emulator
- Tailored to statistical mathematics (no general CAS overhead)
- Full control over rewrite rules for statistical domains

**Engine Design:**
```
ExpressionNode (sealed class hierarchy)
  ├── Num(value: BigDecimal)
  ├── Var(name: String)
  ├── Sum(terms: List<ExpressionNode>)
  ├── Product(factors: List<ExpressionNode>)
  ├── Fraction(num: ExpressionNode, den: ExpressionNode)
  ├── Power(base: ExpressionNode, exp: ExpressionNode)
  ├── Log(base: ExpressionNode, arg: ExpressionNode)
  ├── Exp(arg: ExpressionNode)
  ├── Integral(expr: ExpressionNode, var: Var, from: ExpressionNode, to: ExpressionNode)
  ├── Summation(expr: ExpressionNode, var: Var, from: ExpressionNode, to: ExpressionNode)
  ├── Matrix(rows: List<List<ExpressionNode>>)
  ├── Derivative(expr: ExpressionNode, var: Var, order: Int)
  ├── Expectation(expr: ExpressionNode)
  ├── Variance(expr: ExpressionNode)
  └── Covariance(a: ExpressionNode, b: ExpressionNode)
```

---

## 5. Local Storage Strategy: Room + JSON Assets

**Decision: Room (SQLite) + JSON asset files**

**Rationale:**
- Proof definitions stored as JSON in `assets/proofs/` — human-readable, extensible
- Room database used for: search index, favorites, recent history, user settings
- Room FTS4 provides full-text search over theorem titles, descriptions, keywords
- No external search library required
- Migrations handled by Room's migration framework

---

## 6. Math Rendering Strategy: KaTeX Offline WebView

**Decision: KaTeX offline bundle in WebView**

**Rationale:**
- KaTeX produces highest-quality LaTeX rendering of all options
- Bundled as Android assets — fully offline
- Battle-tested in production (used by Khan Academy, Coursera)
- Supports full LaTeX math syntax including matrices, integrals, piecewise
- MIT licensed — no GPL contamination
- Compose-native renderer would require implementing a full LaTeX parser/renderer

**Pipeline:**
```
ProofStep.latex (String)
  → LaTeXRenderer (WebView bridge)
  → assets/katex/katex.min.js + katex.min.css
  → Rendered HTML fragment
  → Android WebView (hardware accelerated)
  → Cached rendered bitmap (optional)
```

---

## 7. Navigation Strategy: Compose Navigation

**Decision: Compose Navigation (type-safe routes)**

**Rationale:**
- Single-Activity architecture with Composable screens
- Type-safe navigation using sealed class route definitions
- Back stack management handled by NavController
- Deep link support for sharing proof URLs

---

## 8. Serialization Strategy: kotlinx.serialization

**Decision: kotlinx.serialization**

**Rationale:**
- Kotlin-native, no reflection at runtime
- Works with Kotlin data classes directly
- Supports JSON, ProtoBuf if needed
- Better performance than Gson/Moshi for large proof datasets

---

## 9. Search Indexing Strategy: Room FTS4

**Decision: Room FTS4 (Full-Text Search)**

**Rationale:**
- Built into SQLite — no additional dependency
- Supports BM25 ranking via FTS5 (available on API 24+)
- Indexes theorem names, tags, keywords, descriptions
- Fast enough for hundreds of theorems

---

## 10. Module Dependency Graph

```
app
 ├── domain
 ├── data
 ├── core
 └── proofengine

data
 ├── domain
 └── core

domain
 ├── proofengine
 └── core

proofengine
 └── core

core
 └── (no internal deps)
```

---

## 11. Data Flow Diagram

```
User Action (Compose UI)
  → ViewModel (StateFlow<UiState>)
  → Use Case (domain layer)
  → Repository Interface (domain layer)
    → Repository Implementation (data layer)
      → Room DAO / Asset Loader / ProofEngine
        → ExpressionNode AST
        → SimplificationEngine
        → ProofStep List
        → LaTeX strings
      ← ProofResult
    ← domain model
  ← domain model
  → ViewModel maps to UiState
  → Compose recomposes
```

---

## 12. Proof Engine Architecture

```
ProofEngine
  ├── ProofRepository (loads JSON definitions)
  ├── ProofParser (parses JSON → ProofDefinition)
  ├── ProofExpander (generates ProofStep list from definition)
  │   ├── RuleEngine (applies transformation rules)
  │   ├── SimplificationEngine (reduces expressions)
  │   └── VerificationEngine (validates step chains)
  ├── LaTeXGenerator (converts ExpressionNode → LaTeX string)
  └── ExpressionParser (parses string → ExpressionNode)
```

---

## 13. Rendering Pipeline Architecture

```
ProofStep.latex
  → LaTeXRendererViewModel
  → LaTeXWebViewComposable
  → WebView (asset://katex/render.html)
  → JavaScript: katex.renderToString(latex, { displayMode: true })
  → HTML fragment injected into WebView
  → User sees rendered math
```

---

## 14. State Management Architecture

```
UiState (immutable data class, sealed for each screen)
  ← produced by ViewModel
  ← driven by StateFlow<UiState>
  → observed by Composable via collectAsStateWithLifecycle()

Side effects: handled via SharedFlow<UiEffect> (navigation, snackbar)
Loading: represented as UiState.Loading
Errors: represented as UiState.Error(message)
```

---

## 15. CI/CD Pipeline Architecture

```
GitHub Actions: android-build.yml

Triggers: push to main, PR to main

Steps:
1. Checkout code
2. Set up JDK 17 (Temurin)
3. Cache Gradle dependencies
4. Decode keystore from secret
5. Run ktlint
6. Run detekt
7. Run unit tests
8. Run Android lint
9. Assemble release APK
10. Sign APK (zipalign + apksigner)
11. Upload signed APK as artifact
```

---

## 16. Testing Strategy

```
Unit Tests (JVM — no emulator required):
  - proofengine: symbolic algebra, simplification, rule engine
  - domain: use case logic
  - data: repository logic, DAO queries (in-memory Room)

Instrumented Tests (emulator/device):
  - app: navigation flow, rendering, search

Static Analysis:
  - detekt: code quality, complexity
  - ktlint: formatting
  - Android Lint: resource issues, API level compatibility
```

---

## 17. Security Model

- No network permissions in AndroidManifest
- No INTERNET permission declared
- All data is local: Room DB + asset files
- Keystore secrets managed via GitHub Secrets (never committed)
- ProGuard/R8 enabled for release build
- No analytics, no crash reporting, no telemetry

---

## 18. Offline Data Architecture

```
assets/proofs/
  ├── probability_theory.json      (Probability Theory theorems)
  ├── distribution_theory.json     (Distribution theorems)
  ├── statistical_inference.json   (Inference theorems)
  ├── hypothesis_testing.json      (Hypothesis testing)
  ├── regression_models.json       (Regression proofs)
  ├── bayesian_statistics.json     (Bayesian theorems)
  ├── sampling_theory.json         (Sampling theorems)
  ├── stochastic_processes.json    (Stochastic processes)
  ├── information_theory.json      (Information theory)
  └── asymptotics.json             (Asymptotic theory)

assets/katex/
  ├── katex.min.js
  ├── katex.min.css
  ├── fonts/ (KaTeX fonts)
  └── render.html (rendering template)

Room Database (statproof.db):
  ├── theorems (search index, favorites, metadata)
  ├── recent_history
  ├── user_settings
  └── proof_cache
```

---

## 19. Technology Decision Matrix

| Concern | Decision | Alternatives Considered | Reason |
|---|---|---|---|
| Architecture | Clean Arch + MVVM | MVI, MVP | Testability + Compose fit |
| DI | Hilt | Koin | Compile-time safety |
| Symbolic Engine | Native Kotlin AST | SymJa, Matheclipse | No JNI, fully auditable |
| Storage | Room + JSON assets | SQLDelight, Realm | Standard, FTS built-in |
| Math Rendering | KaTeX offline WebView | MathJax, Compose native | Best quality, MIT license |
| Navigation | Compose Navigation | Voyager | Official Jetpack |
| Serialization | kotlinx.serialization | Gson, Moshi | Kotlin-native, no reflection |
| Search | Room FTS4 | Whoosh, Lucene | Built-in, no extra deps |
| Async | Coroutines + Flow | RxJava | Kotlin-native |
| UI | Jetpack Compose + M3 | Views | Modern, declarative |
| Testing | JUnit5 + Mockk | JUnit4, Mockito | Kotlin-idiomatic |
| Static Analysis | detekt + ktlint | Spotless | Kotlin-specific |

---

## 20. Repository File Tree (Target)

```
StatProof/
├── .github/
│   └── workflows/
│       └── android-build.yml
├── app/
│   ├── build.gradle.kts
│   └── src/main/java/com/statproof/app/
│       ├── MainActivity.kt
│       ├── StatProofApplication.kt
│       ├── di/
│       ├── navigation/
│       └── ui/
├── proofengine/
│   ├── build.gradle.kts
│   └── src/main/java/com/statproof/proofengine/
│       ├── ast/
│       ├── parser/
│       ├── rules/
│       ├── simplifier/
│       ├── engine/
│       ├── models/
│       ├── verification/
│       └── latex/
├── core/
│   ├── build.gradle.kts
│   └── src/main/java/com/statproof/core/
├── data/
│   ├── build.gradle.kts
│   └── src/main/java/com/statproof/data/
├── domain/
│   ├── build.gradle.kts
│   └── src/main/java/com/statproof/domain/
├── examples/proofs/
├── docs/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── scripts/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── detekt.yml
├── .editorconfig
├── README.md
└── LICENSE
```
