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

## The font-scale matrix

`MerchantAvatar`, `TransactionRow` and `RangeChips` are additionally covered at **`fontScale = 2.0f`**,
because the design review found layout defects that only appear at large font scales.

The mechanism is Robolectric's `@Config(fontScale = 2.0f)`, which this project's Robolectric
(4.17) supports as a **per-test-method** annotation — no separate test class and no
`RuntimeEnvironment.setFontScale` call needed. It sets `Configuration.fontScale`, which Compose
reads into `LocalDensity`.

`FontScaleHarnessTest` guards that plumbing: it asserts the scale Compose actually observes is
1.0 by default and 2.0 under the annotation. Without it, a Robolectric upgrade that stopped
propagating font scale would leave every large-font test below passing while silently rendering
at 1x — testing nothing.

What the 2x coverage pins down, measured rather than assumed:

| | at 1x | at 2x |
|---|---|---|
| `TransactionRow` height | 60dp | 94dp — grows, does not clip |
| `TransactionRow` amount right edge | 391dp | 391dp — still flush to the 20dp margin |
| `RangeChips` chip height | 32dp | 37.5dp — grows with its label |
| `RangeChips` inset / gaps | 20dp / 8dp | 20dp / 8dp — unchanged |
| `MerchantAvatar` box | 40dp | 40dp — fixed |
| `MerchantAvatar` "WW" text width | fits | **40dp — saturated** |

The last row is a real defect and is deliberately pinned by
`widest initials saturate the avatar at 2x - known defect`. `MerchantAvatar` sizes its text as
`(size.value * 0.33f).sp` inside a fixed-size box, so the glyphs scale with the user's font
setting but their container does not. In `MerchantAvatar_fontScale2_light.png` the pair no
longer fits and **only the first letter survives** — "Wonder World" renders as "W", "Mega Mart"
as "M" — so two different merchants become indistinguishable. That test is a *characterisation*
test: it records today's behaviour, not the desired behaviour. When the component is fixed to
clamp its font size, the test fails; that failure is the signal to delete it, because
`initials never escape the avatar at 2x` then carries the invariant alone.

`TransactionRow`'s truncation at 2x (both labels are `maxLines = 1` + ellipsis, so the subtitle
degrades to "Food & dini…" and loses the account and channel) is visible in its 2x goldens. It
is left as-is rather than asserted against, since single-line truncation is the component's
stated intent; the assertions pin the part that must not break — the amount staying flush right
and un-truncated, and the merchant column yielding to it rather than overlapping.

## Goldens are recorded on Linux CI, never on a developer machine

`DisplayFamily = FontFamily.Serif` (`ui/theme/Theme.kt`) is a **system** font with no bundled
font file. macOS and the Linux CI runner resolve it to different typefaces, so text rasterises
differently and every golden recorded on a Mac fails on the first CI run.

**To update goldens:** Actions → *Android CI* → *Run workflow*. The `record-screenshots` job
re-records on Linux and commits the PNGs back to the branch. Review them in the diff like any
other file.

Running `recordRoborazziDebug` locally is still useful — it is how you see what a new test
captures — but **do not commit what it produces**. `app/src/test/screenshots/` is gitignored so
that this cannot happen by accident; the recording job force-adds the directory, which is the
only path by which goldens enter the repository. If that directory is empty, CI
records instead of comparing and emits a warning rather than failing, so a branch that adds a
component is never red for the sole reason that nobody has recorded it yet.

## Scope

Phase 1 covers `ui/components/Components.kt` and `ui/theme/Theme.kt` only.

There are deliberately **no `@Preview` functions**. Previews would have to live in
`app/src/main` — i.e. inside `Components.kt` — so this phase uses test-local fixtures instead.
A dual-use preview set belongs in phase 2, alongside the stateless-screen refactor.

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
