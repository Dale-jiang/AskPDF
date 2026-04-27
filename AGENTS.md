# Repository Guidelines

## Project Structure & Module Organization

AskPDF is a single-module Android app. The Gradle root contains `settings.gradle`, `build.gradle`, `gradle.properties`, and the wrapper scripts. Application code lives in `app/src/main/java/com/ctf/askpdf`: use `app` for startup, `core` for shared utilities, `data` for storage/repositories, `document` for PDF/file behavior, `feature` for user flows, and `presentation` for Activity/Fragment/Adapter/Dialog code. Android resources are under `app/src/main/res`, including layout XML, launcher assets, strings, colors, themes, and XML config. Local JVM tests belong in `app/src/test/java`, and device or emulator tests belong in `app/src/androidTest/java`.

## Build, Test, and Development Commands

Use the Gradle wrapper from the repository root:

```sh
./gradlew assembleDebug
```

Builds the debug APK.

```sh
./gradlew testDebugUnitTest
```

Runs local JUnit tests on the host JVM.

```sh
./gradlew connectedDebugAndroidTest
```

Runs instrumentation tests on a connected device or emulator.

```sh
./gradlew lint
```

Runs Android lint checks when available for the module.

## Coding Style & Naming Conventions

Kotlin uses the official style configured in `gradle.properties`; keep 4-space indentation and run Android Studio formatting before committing. This project does not use Jetpack Compose; build screens with standard Android Activity/Fragment, XML layouts, adapters, and dialogs. Use `PascalCase` for classes, Activities, Fragments, and adapters; use `camelCase` for methods and properties. 新增方法需添加简短中文注释，说明用途、关键参数或副作用；简单 getter/setter 可省略。 Keep package names under `com.ctf.askpdf`. Declare Gradle plugins and dependencies directly in `build.gradle` files; do not use `libs.versions.toml`.

## Testing Guidelines

Use JUnit4 for local tests and AndroidX JUnit/Espresso for instrumentation tests. Name test methods after observable behavior, such as `useAppContext` or `addition_isCorrect`. Add local tests for pure Kotlin logic and instrumentation tests for Android framework, UI, or resource-dependent behavior. Run `./gradlew testDebugUnitTest` before opening a PR; run `./gradlew connectedDebugAndroidTest` for UI or platform changes.

## Commit & Pull Request Guidelines

No Git history is present in this workspace, so no existing commit convention can be inferred. Use short, imperative commit subjects such as `Add PDF picker screen` or Conventional Commit prefixes such as `feat:` and `fix:` if the project adopts them. Pull requests should include a brief summary, test results, linked issue if applicable, and screenshots or screen recordings for visible UI changes.

## Security & Configuration Tips

Do not commit `local.properties`, SDK paths, generated `build/` outputs, or IDE workspace files. Keep secrets out of resources and Gradle files; load environment-specific values through local configuration or CI secrets.
