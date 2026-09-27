# Kharcha — UI design review

Reviewed at commit `090ceea`. Scope: all 9 screens, 1 modal sheet and 3 dialogs under
`app/src/main/java/com/expensetracker/app/ui/`, plus `ui/theme/Theme.kt`,
`core/.../model/Models.kt` (category palette) and `export/PdfExporter.kt` (second palette).

Findings are ordered so you can work top-down and stop when you run out of time. Each one has a
severity, a location, and the change I would actually make. Where something is a matter of taste
rather than a defect, it says so.

> Line numbers in `transactions/EditTransactionSheet.kt` and `ui/AppRoot.kt` were read while
> another change was in flight against those two files; the anchors may have shifted by a few
> lines. Everything else is stable as of `090ceea`.

---

## What's working

Worth saying plainly before the list, because these are the parts a rewrite would lose:

- **The palette is coherent and hand-authored.** `Theme.kt:17-75` is a real design decision, not a
  generated scheme: deep teal for commitment, amber reserved for "needs attention", and a warm
  off-white (`#F5F4EF`) instead of pure grey, which is what gives the app its calm. Both schemes are
  fully specified — 20-odd roles each, including `inverse*` and `outlineVariant` — which is more
  care than most Compose apps take.
- **The privacy thesis is surfaced consistently, not just claimed.** `OfflineBadge` on every top bar
  (`Components.kt:84`), the Privacy Center card (`SettingsScreen.kt:201`), onboarding step 1
  (`OnboardingFlow.kt:150`), the "How to check this yourself" dialog (`:188`), and the honest
  caveat on the export screen — *"Sharing it sends the file to whichever app you pick"*
  (`ExportScreen.kt:182`). That last one is the tell that this is designed rather than marketed: the
  one place the promise has a hole, the UI says so.
- **Three genuinely thoughtful `semantics` blocks.** The donut chart reads out its categories and
  percentages (`HomeScreen.kt:258-260`), the trend delta reads as *"Up 12 percent vs last month"*
  rather than a bare glyph (`:156`), and the offline pill has a real sentence (`Components.kt:92`).
  These are the hard ones to get right, and they're right.
- **Copy is in plain language throughout.** "transaction needs a quick check", "Not an expense",
  "Couldn't tell who was paid", "All caught up". No jargon, no "sync", no error codes.
- **Restraint in the right places.** `ReviewScreen` shows one card at a time instead of a list;
  `HomeScreen` hides every card that has no data (`:79-99`); `ReviewScreen.kt:82-83` deliberately
  shows nothing while loading rather than flashing a misleading "all caught up". Those are
  considered choices.

The findings below are mostly about the gap between this care and the places where it wasn't
carried through — not about the design's intent, which is sound.

---

## Critical

### C1 · Category-tinted initials fail contrast in light mode — 10 of 12 categories

**`ui/components/Components.kt:80`** (with `:207`, `:209`)

```kotlin
fun Category.textColor(): Color = if (isDark()) lerp(color, Color.White, 0.5f) else color
```

The dark branch works. The light branch doesn't: it draws the **raw** category colour as text on a
background of that **same colour at 14% alpha** (`:207`), which is by construction a near-white
tint. The two can't separate.

Measured (WCAG relative luminance, glyph over the composited avatar fill on the `#F5F4EF` app
background):

| Category | Colour | Ratio | |
|---|---|---|---|
| Other | `#B9B6AC` | **1.7 : 1** | ← the fallback category |
| Shopping | `#D08A2E` | 2.5 : 1 | |
| Health | `#2E9AA0` | 2.9 : 1 | |
| Groceries | `#3E8E5E` | 3.4 : 1 | |
| Travel | `#6C8A2E` | 3.4 : 1 | |
| Transfers | `#7A7F80` | 3.5 : 1 | |
| Entertainment | `#C0567A` | 3.6 : 1 | |
| Transport | `#4A7BC0` | 3.7 : 1 | |
| Bills | `#8B6BB8` | 3.7 : 1 | |
| Income | `#0E7A52` | 4.4 : 1 | marginal |
| Fuel | `#2F5D99` | 5.5 : 1 | passes |
| Food | `#0E5A52` | 6.5 : 1 | passes |

13sp bold at a 40dp avatar is *normal* text under WCAG (the large-text exemption starts at 14pt
bold ≈ 18.7sp), so the bar is 4.5:1. Ten categories miss it and the default one misses it by 2.6×.

Two things make this worse than a single-component bug:

- `HomeScreen.kt:324` passes `Category.OTHER` for **every** avatar on the Top merchants card, so
  that entire card renders initials at 1.7:1 regardless of the transaction's real category.
- The same avatar appears in `TransactionRow` (`Components.kt:224`), the edit sheet header
  (`EditTransactionSheet.kt:185`, at 52dp), the import list (`OnboardingFlow.kt:331`) and the rules
  list (`RulesScreen.kt:64`) — so it's on essentially every screen.

For the record, the dark path is fine (Food 5.8:1, Other 6.8:1). **The luminance sniff in
`isDark()` is not the defect** — it returns the right answer. The light branch just doesn't do
anything.

**Fix (cheap, provably correct):** stop tinting the glyph. The category signal is already carried by
the fill; the initials only need to be legible.

```kotlin
// Components.kt:209
Text(initials, color = MaterialTheme.colorScheme.onSurface, ...)
```

`#17191A` on the 14% tint is ~14:1 in light and `#ECEDEA` is ~11:1 in dark. One line, both schemes.

**Fix (durable, if you want to keep coloured initials):** add a second value to the enum so the
app and the PDF read one source, and hand-pick 12 values measured against the tint:

```kotlin
// core/.../model/Models.kt:32
enum class Category(val key: String, val label: String, val colorArgb: Long, val onTintArgb: Long) {
    FOOD("food", "Food & dining", 0xFF0E5A52, 0xFF0A3F3A),
    ...
```

`textColor()` then becomes a lookup instead of a luminance sniff. Don't do this with a blind
`lerp(color, Color.Black, 0.5f)` — I checked, and `OTHER` still only reaches ~4.4:1 at 0.5; each
value needs measuring.

---

### C2 · Choosing Light or Dark in Settings doesn't reach the window — cold start flashes the wrong colour

**`MainActivity.kt:21`**, **`res/values/themes.xml`**, with **`Theme.kt:94-100`** and
**`SettingsScreen.kt:158-166`**

`ExpenseTheme` resolves `ThemeMode.SYSTEM / LIGHT / DARK` correctly for everything Compose paints.
But two things outside Compose still follow the **system** night setting only:

- `android:windowBackground` → `@color/window_background`, overridden in `res/values-night/colors.xml`
  (`#F5F4EF` / `#121414`). There is no `values-night/themes.xml`.
- `enableEdgeToEdge()` is called with no arguments (`MainActivity.kt:21`), so the status- and
  navigation-bar icon style is derived from the system configuration.

For a user whose phone is in light mode but who picked **Dark** in Settings:

1. The splash and window background paint cream `#F5F4EF`;
2. Compose then paints `#121414` — a full-screen white flash on every cold start;
3. The status bar keeps dark icons on the now-dark background, so the clock and battery are
   unreadable for the whole session.

The inverse case (system dark, user picks Light) gives a black flash and washed-out white icons.

**Fix.** Hoist the resolved boolean out of `ExpenseTheme` and drive both from it:

```kotlin
// Theme.kt — expose the resolution
@Composable fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true
}

// MainActivity.onCreate, inside setContent
val themeMode by container.prefs.themeMode.collectAsStateWithLifecycle()
val dark = themeMode.isDark()
SideEffect {
    enableEdgeToEdge(
        statusBarStyle = if (dark) SystemBarStyle.dark(TRANSPARENT)
                         else SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
        navigationBarStyle = if (dark) SystemBarStyle.dark(TRANSPARENT)
                             else SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
    )
    window.setBackgroundDrawable(ColorDrawable(if (dark) 0xFF121414.toInt() else 0xFFF5F4EF.toInt()))
}
```

That fixes the icons and everything after the splash. The very first splash frame is drawn before
any app code runs, so it can only follow the system setting unless you persist the resolved mode
into a resource-visible form; given the pref is already in `Prefs`, the pragmatic answer is to
accept one splash frame and kill the flash at the handoff, which `setBackgroundDrawable` does.

---

## High

### H1 · The app cannot be localised: one string resource, ~200 hardcoded literals

**`res/values/strings.xml`** (one entry, `app_name`); **zero** `stringResource` calls anywhere in
`app/` or `core/`.

Every user-facing string is a Kotlin literal, and plurals are hand-rolled with `if (n == 1)` at five
sites:

- `HomeScreen.kt:210` — "transaction needs a quick check"
- `HomeScreen.kt:328` — "1 payment" / "N payments"
- `SettingsScreen.kt:112` — "N merchant rule(s)"
- `EditTransactionSheet.kt:263` — "updates N past transaction(s)"
- `ExportScreen.kt:154` — "about N page(s)"

For an India-targeted app with a Hindi name, English-only is the single biggest limit on reach. The
manual pluralisation is also simply wrong outside English — Hindi, Marathi and Tamil all have
different rules, and `<plurals>` exists for exactly this.

**Fix, in order:**

1. Move the five plural sites to `<plurals>` first — those are broken today, not just untranslated.
2. Bulk-extract the rest to `strings.xml`.
3. The structural part: `RangePreset.label` (`core/.../DateRanges.kt:7`), `Channel.label` and
   `Category.label` (`Models.kt:15-23`, `:32-44`) and `TypeFilter.label`
   (`TransactionsViewModel.kt:29`) are `String` constants in a module with no Android dependency.
   Change them to `@StringRes Int`, or keep `core` pure and add a `Category.labelRes()` mapping in
   the UI layer. The second keeps `core`'s unit tests android-free, which is worth preserving.

### H2 · Switch rows aren't associated with their labels, and the label isn't tappable

**`ExportScreen.kt:219-222`**, **`SettingsScreen.kt:116-137`**

```kotlin
Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(52.dp)) {
    Text(label, modifier = Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = onChange)
}
```

A plain `Row` doesn't merge semantics, so TalkBack reads two unrelated nodes: *"Summary and totals"*
… *"Switch, on"* — with no indication which switch belongs to which label in a list of four. Sighted
users can only hit the switch itself, not the row, which is the standard Android affordance.

**Fix** (both sites):

```kotlin
Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth().height(52.dp)
        .toggleable(value = checked, onValueChange = onChange, role = Role.Switch),
) {
    Text(label, modifier = Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = null)   // null → the Row owns the click
}
```

### H3 · Three interactive targets below the 48dp minimum

| Where | Current | What it does |
|---|---|---|
| `Components.kt:84-103` `OfflineBadge` | `height(32.dp)` + `clickable` | Entry point to the Privacy Center — on **every** top bar |
| `SettingsScreen.kt:232-238` | `clickable` then `.padding(top = 8.dp)` → ~24dp tall | Opens the "how to verify" dialog |
| `HomeScreen.kt:264-276` legend rows | `padding(vertical = 5.dp)` on a 14sp line → ~28dp | Navigates to a category-filtered list |

The offline badge is the worst of the three, because it's the product's headline feature and it's
repeated on every screen.

**Fix** — keep the visual size, grow the hit target. For the badge:

```kotlin
Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier.heightIn(min = 48.dp)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
) {
    Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.height(32.dp), ...) { ... }
}
```

For the other two, `Modifier.heightIn(min = 48.dp)` on the clickable node and let the content centre
inside it. `minimumInteractiveComponentSize()` also works and is one token, but it only pads to
48dp, it won't help where the row is already `fillMaxWidth`.

### H4 · No headings anywhere — screen readers can't navigate

There are exactly three `semantics` blocks in the codebase (`HomeScreen.kt:156`, `:258`,
`Components.kt:92`) and none of them is `heading()`. TalkBack's heading navigation and Switch
Access's jump-to-section do nothing in this app; a user has to swipe through every row of a
200-transaction list to reach the next section.

**Fix.** `Modifier.semantics { heading() }` on:

- `Components.kt:113` — the `ScreenHeader` title (covers Home and Transactions)
- `SettingsScreen.kt:101` — "Settings"
- `ReviewScreen.kt:76`, `ExportScreen.kt:143`, `RulesScreen.kt:41` — the back-bar titles
- `Components.kt:119-126` — `SectionLabel`, which is the app's section divider everywhere
- `TransactionsScreen.kt:186-197` — `DayHeader`, so date navigation works in the long list

`SectionLabel` and `DayHeader` are single shared components, so most of this is two lines.

### H5 · `SpendingBars` is invisible to screen readers

**`HomeScreen.kt:300`** (component at `Components.kt:267-288`)

`SpendingBars` is a bare `Canvas` with no semantics, so the entire "Spending over time" card is
silent apart from the peak label at `:293`. Its sibling `DonutChart` gets a good description at
`:258-260` — the gap is an oversight, not a decision.

**Fix**, mirroring the donut exactly:

```kotlin
// HomeScreen.kt:300
SpendingBars(
    state.buckets,
    Modifier.fillMaxWidth().height(100.dp).semantics {
        contentDescription = state.buckets.joinToString { "${it.label} ${Money.compact(it.amountMinor)}" }
    },
)
```

---

## Medium

### M1 · The hero Summary card nearly disappears in dark mode

**`HomeScreen.kt:143`** — `background(MaterialTheme.colorScheme.inverseSurface)`

| | Card | Page | Contrast |
|---|---|---|---|
| Light | `#17191A` | `#F5F4EF` | ~15 : 1 — a striking near-black slab, clearly the page anchor |
| Dark | `#263030` | `#121414` | **1.34 : 1** — a faint rectangle |

The `inverseSurface` role is doing opposite jobs in the two schemes: in light it's the dramatic
high-contrast surface the design wants; in dark it's just "slightly lighter than the page". The
app's single most important number ends up with almost no visual weight in dark mode.

**Fix.** Give the card an edge so it reads as an object in both schemes:

```kotlin
// HomeScreen.kt:142-144
.clip(RoundedCornerShape(24.dp))
.background(MaterialTheme.colorScheme.inverseSurface)
.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
```

and lift `Dark.inverseSurface` (`Theme.kt:71`) from `#263030` to about `#2E3A39`, which takes the
page contrast to ~1.9:1 — still calm, but the card has a shape. The border is what actually does the
work; the token bump is polish.

### M2 · The flattened `surfaceContainer*` roles are intentional and mostly work — but popups pay for it

**`Theme.kt:36-38`** and **`:66-68`** set `surfaceContainerLow`, `surfaceContainer` and
`surfaceContainerHigh` to a single value per scheme (`#FFFFFF` light; `#1B1E1E` / `#1B1E1E` /
`#222626` dark).

**Verdict: deliberate, and right for this design.** The app's own cards get their separation from an
explicit 1dp `outlineVariant` border (`Components.kt:135`), not from tonal elevation, and that's a
consistent flat style that suits the calm palette. Don't undo it.

The concrete cost is in the Material components you *didn't* author, which rely on those roles for
their only edge:

- `DropdownMenu` — `TransactionsScreen.kt:159`, `:175`
- `DatePickerDialog` — `Components.kt:173`, `EditTransactionSheet.kt:345`
- `AlertDialog` — `SettingsScreen.kt:182`, `OnboardingFlow.kt:189`

In light mode all of these land on pure `#FFFFFF` over pure `#FFFFFF` cards with no border, so a
dropdown opened over the Transactions list has no visible boundary at all.

**Fix.** Keep the flat scheme and nudge one token so popups sit just off card white:

```kotlin
// Theme.kt:38
surfaceContainerHigh = Color(0xFFFBFAF6),   // warm, a half-step down from surface
```

That's enough of an edge without reintroducing a tonal ladder you don't want.

### M3 · The FAB is copy-pasted and the back bar exists in three drifting versions

**FAB** — `HomeScreen.kt:119-126` and `TransactionsScreen.kt:132-138` are byte-identical (same
colours, same `RoundedCornerShape(20.dp)`, same 20dp padding, same content description).

**Back bar** — three versions, and they don't line up:

| File | Height | Horizontal padding | Title style | Trailing |
|---|---|---|---|---|
| `ReviewScreen.kt:74-80` | 56dp | **20dp** (from parent `Column`) | `titleMedium` | "N left" |
| `ExportScreen.kt:141-144` | 56dp | **8dp** | `titleMedium` | — |
| `RulesScreen.kt:39-42` | 56dp | **8dp** | `titleMedium` | — |

Review's back arrow sits 12dp further right than the other two. Settings → Export → back → Settings
→ Rules is a common path, and the arrow visibly jumps.

**Fix.** Two components in `Components.kt`:

```kotlin
@Composable fun AddExpenseFab(onClick: () -> Unit, modifier: Modifier = Modifier) { ... }

@Composable fun BackBar(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp)) {
        IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        Text(title, style = MaterialTheme.typography.titleMedium,
             modifier = Modifier.weight(1f).semantics { heading() })
        trailing()
    }
}
```

`horizontal = 4.dp` puts the 48dp `IconButton`'s *optical* left edge on the 20dp content gutter the
rest of the app uses, so the title aligns with the body text below it. Note `ReviewScreen`'s root
`Column` applies `padding(horizontal = 20.dp)` at `:73` — the bar has to move outside that padding.

### M4 · `MaterialTheme.shapes` is never set; 12 distinct corner radii are hardcoded

Counted across `app/src/main/java`: **3, 5, 6, 12, 14×5, 16×5, 18×5, 20×4, 22, 24×2, 26×5, 28×3** dp.

Most are harmless neighbours, but one pair is a visible inconsistency: the *same* primary-action
affordance is a pill in one place and a rounded rect in another —

- `EditTransactionSheet.kt:320,325,332` — 52dp tall, `RoundedCornerShape(26.dp)` → full pill
- `ExportScreen.kt:193,209` — 52dp tall, `RoundedCornerShape(26.dp)` → full pill
- `ReviewScreen.kt:117,122,127` — 56dp tall, `RoundedCornerShape(18.dp)` → rounded rect

**Fix.** Define the scale once and use it:

```kotlin
// Theme.kt
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(20.dp),   // AppCard, FAB
    extraLarge = RoundedCornerShape(28.dp),   // buttons, onboarding icon tile
)
MaterialTheme(colorScheme = ..., typography = AppTypography, shapes = AppShapes, content = content)
```

Then replace call sites with `MaterialTheme.shapes.*`, and make `ReviewScreen`'s three buttons a
pill like the others. Chips (`Components.kt:160`, `EditTransactionSheet.kt:237`, both 18dp) are
legitimately their own thing — leave them, or add a named `ChipShape` constant.

### M5 · Button heights drift across 48 / 52 / 56dp

- 56dp — `OnboardingFlow.kt:144`, `:286`; `ReviewScreen.kt:117,122,127`
- 52dp — `EditTransactionSheet.kt:319,324,331`; `ExportScreen.kt:192,208`
- 48dp — `OnboardingFlow.kt:228`; `SettingsScreen.kt:171`

**Fix.** Two tokens in `Theme.kt`, applied consistently:

```kotlin
val PrimaryButtonHeight = 56.dp    // full-width page-bottom commit actions
val SecondaryButtonHeight = 52.dp  // paired in-content actions
```

The edit sheet's Save/Delete row is the same "commit" moment as onboarding's Continue, so it should
be 56dp too. The 48dp `TextButton` at `OnboardingFlow.kt:228` is fine — it's a tertiary skip.

### M6 · The export screen's snackbar is in the layout flow and resizes the page

**`ExportScreen.kt:213`** — `SnackbarHost(snackbar)` is the last child of the root `Column`, *after*
the button block, not overlaid on it. When "Report saved" appears it consumes vertical space and the
`weight(1f)` scroll region at `:145` shrinks, so the whole page jumps at the moment the user is
reading the confirmation.

Every other screen does this correctly — `TransactionsScreen.kt:139` and `SettingsScreen.kt:176`
both overlay it inside a `Box` with `Alignment.BottomCenter`.

**Fix.** Wrap the root in a `Box` and match the other two:

```kotlin
Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) { /* existing content */ }
    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
}
```

### M7 · `MerchantAvatar` initials break at large font scales

**`Components.kt:198`, `:206`, `:209`**

```kotlin
Box(modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.32f))...) {
    Text(initials, ..., fontSize = (size.value * 0.33f).sp)
}
```

The box is a fixed `40.dp`; the glyphs are `.sp`, so they scale with the user's font setting while
their container does not. At `fontScale = 2.0` (Android's maximum) the text is 26.4sp: a bold
two-letter pair of wide glyphs — "WW", "MM" — measures roughly 46dp against a 40dp box. With
`softWrap` on and no `maxLines`, it breaks to two lines (~62dp) and the `clip()` at `:206` cuts it
in half.

At default scale this is invisible, and at 1.3× it's fine — this is specifically a large-font
failure. (It does *not* clip at default settings; worth stating because it makes the fix low-risk.)

**Fix** — the size is derived from a `dp`, so it should be converted, not reinterpreted:

```kotlin
Text(
    initials,
    color = MaterialTheme.colorScheme.onSurface,          // see C1
    fontWeight = FontWeight.Bold,
    maxLines = 1,
    fontSize = with(LocalDensity.current) { (size * 0.33f).toSp() },
)
```

`Dp.toSp()` divides out `fontScale`, so the glyph keeps a constant ratio to its box.

### M8 · The PDF exporter keeps a second copy of the light palette

**`export/PdfExporter.kt:85-88`**

```kotlin
private val ink    = Color.rgb(0x17, 0x19, 0x1A)   // == Light.onSurface        (Theme.kt:33)
private val muted  = Color.rgb(0x5B, 0x60, 0x60)   // == Light.onSurfaceVariant (Theme.kt:35)
private val line   = Color.rgb(0xE3, 0xE1, 0xD9)   // == Light.outlineVariant   (Theme.kt:40)
private val accent = Color.rgb(0x0E, 0x5A, 0x52)   // == Light.primary          (Theme.kt:18)
```

Four values duplicated by hand. Change the theme and the PDF drifts silently — there's no test that
would catch it. Note the exporter already does the right thing for category swatches (`:170`,
`:179` read `Category.colorArgb`), so the pattern exists; these four just weren't included.

**Fix.** Put the brand constants where the category colours already live and read them from both
sides:

```kotlin
// core/src/main/kotlin/com/expensetracker/core/model/Brand.kt
object Brand {
    const val INK = 0xFF17191AL; const val MUTED = 0xFF5B6060L
    const val LINE = 0xFFE3E1D9L; const val ACCENT = 0xFF0E5A52L
}
```

`Theme.kt` builds `Color(Brand.INK)`, `PdfExporter` uses `Brand.INK.toInt()`.

The PDF being light-only is correct, by the way — it's paper. Don't make it theme-aware.

---

## Low / taste

### L1 · No dynamic color (Material You) — keep it that way

`minSdk = 26`, `compileSdk = 35`, so `dynamicLightColorScheme()` has been available since API 31 and
isn't used anywhere. **This is a legitimate choice, not a gap.** The teal/amber pairing carries
meaning — amber is "needs attention" across the review banner (`HomeScreen.kt:201`), the peak bar
(`Components.kt:281`), the swipe-to-dismiss background (`TransactionsScreen.kt:217`) and the review
chip (`ReviewScreen.kt:144`) — and a wallpaper-derived scheme would scramble all four.

**Recommendation:** keep it, and write it down at `Theme.kt:16` so the next contributor doesn't
"fix" it. One sentence: *"Deliberately not dynamic — amber is a semantic 'needs attention' colour,
not decoration."*

### L2 · `FontFamily.Serif` as the display family is a per-device lottery

**`Theme.kt:78`.** `FontFamily.Serif` resolves to Noto Serif on stock Android but to the OEM's
substitution on Samsung, Xiaomi and Oppo. The app's most distinctive typographic gesture — the 40sp
hero amount (`HomeScreen.kt:149`), the 34sp onboarding headline (`OnboardingFlow.kt:167`), the
wordmark (`OnboardingFlow.kt:130`) — therefore renders differently per device, and the serif is
exactly the thing carrying the brand.

The README roadmap already names Fraunces/Manrope. **Fix:** bundle one variable serif in `res/font/`
(Fraunces subset to Latin + Devanagari digits is ~120KB) and set
`DisplayFamily = FontFamily(Font(R.font.fraunces))`. Until then it's a known risk, not a bug.

### L3 · ~20 `fontSize` overrides bypass the type scale

20 occurrences in `ui/`. Most are near-duplicates of roles that already exist:

| Site | Value | Nearest role |
|---|---|---|
| `HomeScreen.kt:274`, `:275` | 14.sp | `bodyMedium` |
| `HomeScreen.kt:176`, `:180` | 12.sp | `labelMedium` |
| `HomeScreen.kt:166`, `:208` | 13.sp / 14.sp | `labelLarge` |
| `Components.kt:100` | 13.sp | `labelLarge` |
| `ReviewScreen.kt:151` | 12.sp | `labelMedium` |
| `SettingsScreen.kt:212` | 17.sp | `titleMedium` |

**Fix.** Replace those with `style = MaterialTheme.typography.*`. Keep the genuinely bespoke ones —
`HomeScreen.kt:149` (40sp hero), `OnboardingFlow.kt:167` (34sp), `:234` and `:296` (32sp) — but
promote them to named styles next to `AmountStyle` in `Theme.kt`:

```kotlin
val HeroAmount = AmountStyle.copy(fontSize = 40.sp)
val OnboardingTitle = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
                                fontSize = 32.sp, lineHeight = 37.sp)
```

Note `OnboardingFlow.kt:167` and `:234` use 34sp and 32sp for the same role on consecutive screens —
that's drift, not intent.

### L4 · Zero `@Preview` functions, zero `testTag`s

Nothing in this UI can be rendered without building and installing the app. That is almost certainly
why **C1** survived: a 12-row preview of `MerchantAvatar` makes it obvious in two seconds.

**Fix.** Start with the components that carry the most risk, each with three variants:

```kotlin
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, fontScale = 2.0f)
annotation class AppPreviews

@AppPreviews @Composable private fun AvatarPreview() = ExpenseTheme(ThemeMode.SYSTEM) {
    Column { Category.entries.forEach { MerchantAvatar("Big Bazaar", it) } }
}
```

Then `TransactionRow`, `SummaryCard`, `OfflineBadge` and the three onboarding pages. The `fontScale`
variant catches **M7** and **L8**; the night variant catches **M1**.

This is the natural input to the Roborazzi work in task #3 — previews are what a screenshot harness
should be capturing, so it's worth agreeing on the set before either job hardens.

### L5 · One stray `Locale("en","IN")`, inconsistent with the app's own money formatting

**`OnboardingFlow.kt:277`** — `NumberFormat.getIntegerInstance(Locale("en", "IN"))`, used for the
"messages checked / transactions found" counters.

Everything *else* formats numbers through `Money` (`core/.../format/Money.kt`), which implements
Indian digit grouping by hand precisely so it doesn't depend on device locale data — a good decision,
and well covered by `MoneyTest`. This one call reintroduces the dependency for two counters, and
uses the deprecated two-arg `Locale` constructor.

**Fix:** `Money.group(state.scanned.toLong())`, which already produces `12,34,567` and needs no
`NumberFormat` at all. Drop the `remember` and the `java.text`/`java.util.Locale` imports with it.

### L6 · `OfflineBadge` announces itself twice

**`Components.kt:92`** sets a `contentDescription` on the `Surface` without `mergeDescendants`, so
the child `Text("Offline")` at `:100` stays a separate node. TalkBack reads *"Offline. Your data
stays on this phone."* and then *"Offline."*

**Fix:** `Modifier.semantics(mergeDescendants = true) { contentDescription = "..." }`.

### L7 · The Privacy Center card is the brightest thing in the app in dark mode

**`SettingsScreen.kt:207`** — `background(MaterialTheme.colorScheme.primary)`. In light that's
`#0E5A52`, a deep teal slab that reads as authoritative. In dark, `primary` is `#7FCBBC` — bright
mint, a large high-luminance block on a `#121414` page.

Contrast is fine either way (`onPrimary` is `#00382F` in dark), so this is **taste, not a defect**.
But it works against the "calm palette" note at `Theme.kt:16`, and it's the one surface that changes
character between schemes rather than just tone.

**Fix if you agree:** `primaryContainer` / `onPrimaryContainer` for the card. That's `#E1EEEB` in
light — which loses the light-mode drama, so the honest alternative is to branch: keep `primary` in
light, use `primaryContainer` (`#1D4A43`, dark teal) in dark. Worth a decision either way rather
than leaving it to the token.

### L8 · `TransactionRow` truncates the line that disambiguates rows

**`Components.kt:226-233`** — `maxLines = 1` + `TextOverflow.Ellipsis` on both the merchant and the
metadata line (`"Food & dining · HDFC ••1234 · UPI"`).

At default scale this is a deliberate, good choice — uniform row heights make a long list scannable.
At `fontScale` 1.5+ the metadata line loses the account and channel, which is exactly the
information that tells two same-merchant rows apart.

**Fix:** keep the merchant at `maxLines = 1`; give the metadata line `maxLines = 2`. Row heights stay
uniform at normal scale (the string only wraps when it has to) and large-font users keep the data.

### L9 · Centred empty states aren't centred when the text wraps

**`HomeScreen.kt:234-247`** and **`TransactionsScreen.kt:114-121`** use
`horizontalAlignment = Alignment.CenterHorizontally`, which centres each child's *box*. Once the
body text wraps to two lines it fills the available width, so the lines themselves render
left-aligned under a centred title — on a 360dp screen, "Pick a longer range above, or add an
expense yourself." does wrap.

**Fix:** add `textAlign = TextAlign.Center` to the body `Text` at `HomeScreen.kt:241` and
`TransactionsScreen.kt:117`.

---

## Suggested order of work

1. **C1** — one line for the safe fix; it's the only finding that makes content unreadable today.
2. **C2** — contained to `MainActivity`; fixes a flash every user sees on every cold start.
3. **H2, H3, H5, H4** — the accessibility set, roughly an afternoon together. H4 is mostly two
   shared components.
4. **M3, M6, M7, M1** — the visible-quality set. M3 and M6 are pure refactors with no design
   decision attached.
5. **L4** — previews, before the remaining polish, so the rest can be checked visually. Worth
   syncing with task #3 first.
6. **M4, M5, L3, M8** — consistency tokens. Mechanical, large diff, zero user-visible risk.
7. **H1** — localisation. The largest job by far and the one that needs its own plan; the five
   `<plurals>` sites are a useful standalone first slice.
8. **L1, L2, L7, L8, L9, L5, L6** — taste calls and small polish.
