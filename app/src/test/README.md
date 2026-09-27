# Component screenshot + placement tests

Compose is rendered to a real bitmap **on the JVM** by Robolectric's native graphics backend and
captured by Roborazzi. There is no emulator and no `androidTest` source set: these are ordinary
unit tests, so they run on `ubuntu-latest` as a plain Gradle task.

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # macOS, if 17 is not already the default

./gradlew :app:verifyRoborazziDebug   # compare against the committed goldens
./gradlew :app:recordRoborazziDebug   # rewrite the goldens (but see "Goldens" below)
./gradlew :app:testDebugUnitTest      # placement assertions only, no image work
```

Failures write a three-panel `<name>_compare.png` (reference | diff | new) plus a `_actual.png`
to `app/build/outputs/roborazzi/`. CI uploads that directory as the `screenshot-diffs` artifact
whenever the job fails.

## The two halves, and why both are here

A pixel diff tells you *something changed*; it cannot tell a regression apart from an intended
redesign, and it says nothing about why. So every component gets both:

1. **Goldens** — one PNG per component per theme. `ExpenseTheme(mode)` takes an explicit
   `ThemeMode`, so light and dark are just two test methods; no Robolectric night-mode qualifier
   is needed. This is the only thing that can catch `Canvas` drawing changes in `DonutChart` and
   `SpendingBars`, which have no semantics tree at all.
2. **Placement assertions** — `assertWidthIsEqualTo`, `assertHeightIsEqualTo`,
   `getUnclippedBoundsInRoot()` arithmetic. These name the constant they are protecting:
   `TransactionRow`'s merchant text starts at 72dp because that is a 20dp margin plus a 40dp
   avatar plus a 12dp `Arrangement.spacedBy`. When one breaks, the message says which distance
   moved and by how much, which a diff image never does.

## Goldens are recorded on Linux CI, never on a developer machine

`DisplayFamily = FontFamily.Serif` (`ui/theme/Theme.kt`) is a **system** font with no bundled
font file. macOS and the Linux CI runner resolve it to different typefaces, so text rasterises
differently and every golden recorded on a Mac fails on the first CI run.

**To update goldens:** Actions → *Android CI* → *Run workflow*. The `record-screenshots` job
re-records on Linux and commits the PNGs back to the branch. Review them in the diff like any
other file.

Running `recordRoborazziDebug` locally is still useful — it is how you see what a new test
captures — but **do not commit what it produces**. If `app/src/test/screenshots/` is empty, CI
records instead of comparing and emits a warning rather than failing, so a branch that adds a
component is never red for the sole reason that nobody has recorded it yet.

## Scope

Phase 1 covers `ui/components/Components.kt` and `ui/theme/Theme.kt` only.

Screens are deliberately excluded. Every screen takes the DI graph and builds its own ViewModel
internally (`HomeScreen(container: AppContainer, …)`), and `AppContainer` eagerly opens a Room
database and an SMS reader, so a screen cannot be rendered without first being refactored into a
stateless overload that takes its state as parameters. That refactor is phase 2.

## Conventions

- **Fixtures** live in `Fixtures.kt` and are entirely literal — no clocks, no random ids, no
  device locale. Anything that varies between runs turns a golden into a flake.
- **The device profile** is pinned in `src/test/resources/robolectric.properties`
  (`w411dp-h891dp-xhdpi`, SDK 35) so goldens do not move when a host default changes.
- **Golden names** are `<Component>[_<variant>]_<theme>.png`, written by `ScreenshotTest.screenshot`.
- `ScreenshotTest.screenshot` renders inside a 12dp frame so borders clear the image edge;
  `setBareContent` renders with no frame, so placement assertions read the component's own offsets.
- `TransactionRow` is `clickable`, which **merges its descendants' semantics**. Text lookups
  inside it need `useUnmergedTree = true` or they silently return the whole row's bounds.

## Versions, and why these ones

| | version | why |
|---|---|---|
| Roborazzi | 1.60.0 | Newest release whose Kotlin metadata is 2.0.0. 1.61.0+ ship metadata 2.3.0, which this project's Kotlin 2.1.0 compiler refuses to read (`Module was compiled with an incompatible version of Kotlin`). Revisit when the project moves to Kotlin 2.3. |
| Robolectric | 4.17 | Current release; supports the SDK 35 the app compiles against. |
| Compose UI test | from the BOM | `ui-test-junit4` is version-managed by `composeBom`, and `ui-test-manifest` is a `debugImplementation` because it supplies the `ComponentActivity` that `createComposeRule()` launches. |
