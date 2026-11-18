# Snackshot: Visual Assets Design Brief

**Status:** DRAFT for designer review
**Document Version:** 0.4
**Date:** November 14, 2026 (v0.4: November 18, 2026, MVP scope revision: Quick add in the MVP, manual food search post-MVP)
**Owner:** Snackshot product team
**Related documents:** [Project charter](../../01_project_charter.md), [Architecture UX spec](../../Architecture/10_architecture_ux_spec.md) (§3 visual system, §4 screens, §5 accessibility), [prototype theme tokens](../Prototype/prototype-theme.css), [Compose typography](../theme.kt), [HTML prototype](../Prototype/)

---

## 1. Summary

> **Prototype scope:** the HTML prototype ([docs/UX_UI/Prototype](../Prototype/)) and the draw.io schemas show the **final UX/UI solution**, including features that come after the MVP (for example "Quick search" in the Home carousel and S-08 Manual food search). In the MVP build these post-MVP features are shown **disabled** (not available); they are not removed from the design.


Snackshot needs a first complete visual identity. This brief asks for four groups of deliverables:

| # | Deliverable | Replaces in the prototype today |
|---|---|---|
| A | **App icon** for Android (adaptive and themed) and iOS (light, dark, tinted, layered) plus store listing icons | Nothing yet (no launcher icon exists) |
| B | **Brand logo**: the app icon artwork is also the logo on the Welcome screen, Privacy screen and splash/launch screen, plus a wordmark and lockups | Placeholder fruit glyph in an 88dp rounded square (S-01, S-01A) |
| C | **In-app icon set**: custom and restyled icons for every icon used in the app (inventory in §7) | Google Material Symbols Rounded, used as-is |
| D | **Welcome screen background image**, light and dark versions | Plain theme surface (`#F8F9FA` light / `#121314` dark) |

All deliverables must work in **both light and dark themes**, follow **Material Design 3** on Android and **Apple Human Interface Guidelines** on iOS, and meet **WCAG 2.2 AA** contrast.

---

## 2. Product context

### 2.1 What Snackshot is

Snackshot is a mobile nutrition and calorie tracker. The user **takes a photo of a meal**, AI recognizes the food and estimates the portion, and the app logs calories and macros (carbs, fat, protein). Meals can also be logged by quick manual entry (Quick add), which is always free. Text food search (S-08) comes after the MVP.

Key product promises that the visuals should express:

1. **Snap and done:** logging a meal takes a few seconds, and the camera is the main entry point.
2. **Privacy-first and local-first:** no registration, and health data stays on the phone. Sync with Google Health Connect is optional.
3. **Friendly, not judgmental:** the app helps people build a habit. It does not shame or police them.

### 2.2 The name

**Snackshot** = *snack* + *snapshot*. The name is the main creative hook: food meets camera. A playful, light English pun is welcome in the identity, but the result must still look trustworthy enough for a health app.

### 2.3 Platforms and audience

| Item | Details |
|---|---|
| MVP platform | Android (Kotlin, Jetpack Compose, Material 3), reference device Pixel 9 (412 x 892dp) |
| Post-MVP platform | iOS (native UI, HealthKit). The iOS assets are requested now so the identity is ready |
| MVP audience | Closed pilot of 5 friends and family: adults of all genders, around 40 years old (as in the [project charter](../../01_project_charter.md)), everyday smartphone users, not athletes or dieticians |
| Language | English UI; the identity must not depend on text other than the wordmark |

### 2.4 Brand personality

| We are | We are not |
|---|---|
| Friendly, light, a little playful | Childish, cartoonish, or meme-like |
| Calm, clean, modern | Clinical, medical, or "hospital" |
| Trustworthy and private | Corporate or cold |
| Quick and effortless | Busy, loud, or gamified |
| Food-positive | Diet-shaming (no scales, measuring tapes, slim bodies, "before/after") |

### 2.5 Visual direction already agreed

The UI follows a **Google Health-inspired** direction: a low-chroma neutral foundation, one bright **sky-blue** action color, and distinct chart colors (UX spec §3.1). Typography is **Google Sans** (headlines, big numbers) and **Google Sans Text** (UI and body) at Material 3 type scale. New assets must sit naturally inside this system.

---

## 3. Color scheme

The tokens below are the current source of truth for the **UI**, taken from [prototype-theme.css](../Prototype/prototype-theme.css) and UX spec §3.1. They are **Material 3 color roles**.

**The brand palette is open.** The designer is free to propose their own colors, gradients and accents for the app icon, logo and welcome background; they don't have to come from the table below. The only conditions are that the brand assets look good next to the UI colors in both themes and meet the contrast rules in §3.4. If the designer proposes changes to UI colors, they are welcome as a separate proposal.

The app supports three theme modes in Settings: **Light**, **Dark**, and **System** (follows the phone setting). Dark is the default look for statistics screens.

### 3.1 Core Material 3 roles

| Role | Light | Dark | Usage |
|---|---|---|---|
| Primary | `#075985` | `#79C8F5` | Primary buttons, selected states, active controls, current brand-mark container |
| OnPrimary | `#FFFFFF` | `#073B58` | Text and icons on Primary; current brand-mark glyph |
| PrimaryContainer | `#C9E9FA` | `#075985` | Emphasized surfaces, icon containers (paywall sheet icon) |
| OnPrimaryContainer | `#202124` | `#FFFFFF` | Content on PrimaryContainer |
| Secondary | `#5D6165` | `#C8C9CB` | Neutral controls and supporting actions |
| Tertiary | `#356B45` | `#79D889` | In-range status, positive progress |
| Surface | `#F8F9FA` | `#121314` | App background, **Welcome screen background today** |
| SurfaceContainer | `#FFFFFF` | `#1E2022` | Cards, grouped rows, bottom sheets |
| SurfaceContainerHigh | `#F1F3F4` | `#292B2E` | Secondary surfaces, chart tracks |
| OnSurface | `#202124` | `#F1F3F4` | Main text and large values; default icon color |
| OnSurfaceVariant | `#5F6368` | `#C5C7C9` | Supporting text, metadata, secondary icons |
| Outline | `#74777A` | `#8E918F` | Field borders, dividers |
| OutlineVariant | `#C4C7C5` | `#444746` | Subtle dividers |
| Error | `#B3261E` | `#FFB4AB` | Failed recognition, permission or sync |
| ErrorContainer | `#F9DEDC` | `#8C1D18` | Error banners |
| OnErrorContainer | `#410E0B` | `#F9DEDC` | Content on ErrorContainer |

### 3.2 Data and chart colors

| Token | Light | Dark | Usage |
|---|---|---|---|
| Chart intake | `#32C5F0` | `#32C5F0` | Calorie intake bars |
| Chart in range | `#6FD77C` | `#6FD77C` | Value within personal range |
| Chart above range | `#FF9A68` | `#FF9A68` | Value above range |
| Chart below range | `#8CB5FF` | `#8CB5FF` | Value below range |
| Chart warning | `#FFC15A` | `#FFC15A` | Warnings |
| Macro: carbs | `#2D9CDB` | `#8FD3FF` | Carbohydrates (dots, progress tracks) |
| Macro: fat | `#E5675C` | `#FF8A85` | Fat |
| Macro: protein | `#C77700` | `#FFBA66` | Protein |
| Macro track | `#E3E5E8` | `#2A2C2F` | Empty part of progress tracks |

### 3.3 Status badges (always paired with a text label)

| Status | Light bg / fg | Dark bg / fg |
|---|---|---|
| Below range | `#FFF0C7` / `#6B4E00` | `#5A4300` / `#FFD27A` |
| In range | `#D6F5DA` / `#1E5B2A` | `#1E4A27` / `#8EE29A` |
| Above range | `#FFE1D2` / `#8A3A10` | `#5A2A12` / `#FFB38A` |
| Under target | `#D3EEF8` / `#0B4F63` | `#0B4A5C` / `#8FDDF5` |

### 3.4 Color rules for all new assets

1. **Never rely on color alone.** Every status icon is paired with a label or a distinct shape (UX spec §5.2).
2. **UI icons are monochrome.** In-app icons are drawn in one color and tinted at runtime with `OnSurface`, `OnSurfaceVariant`, `Primary`, `OnPrimary`, or `Error`. Do not bake colors into UI icons.
3. **Only brand assets use full color:** the app icon, logo symbol and welcome background.
4. **Contrast targets (WCAG 2.2 AA):** 4.5:1 for body text, 3:1 for large text and essential graphics (icons, logo symbol at small size), in both themes.
5. **Color space:** sRGB for all deliverables. An optional Display P3 version of the iOS app icon is welcome.

> Note for the team: in UX spec §3.1 the dark `Outline` value is `#74777A`, while the prototype CSS uses `#8E918F`. This brief follows the CSS; align the spec before final hand-off.

---

## 4. Deliverable A: App icon (Android and iOS)

### 4.1 Creative requirements

- **One simple, bold symbol** that is recognizable at 48dp on a busy home screen and still readable at 16px (notification, settings list).
- The app icon **is also the logo**: the same artwork is shown on the Welcome screen (§5).
- **No text or letters**, except a single stylized "S" if it is integral to the concept.
- **No photos, no fine detail, no thin hairlines.**
- Must read well on light and dark wallpapers and inside every OEM mask shape (circle, squircle, rounded square, teardrop).

**Avoid:** resemblance to Instagram, Google Photos, Google Lens, Apple Camera, Google Health Connect, or any existing nutrition app (Cal AI, MyFitnessPal, Yazio, Lifesum, etc.); medical crosses; scales; measuring tapes; body shapes.

### 4.2 Android specifications

Follow [Android adaptive icons](https://developer.android.com/develop/ui/views/launch/icon_design_adaptive) and the [Google Play icon specification](https://developer.android.com/distribute/google-play/resources/icon-design-specifications).

| Asset | Spec | Format |
|---|---|---|
| Adaptive icon: **foreground** layer | 108 x 108dp canvas; visible area 72 x 72dp after the mask; keep key content inside the **66dp diameter safe zone** | Android VectorDrawable XML (preferred) and SVG |
| Adaptive icon: **background** layer | 108 x 108dp, full bleed, solid color or simple gradient (no content that must stay visible) | VectorDrawable XML or color value |
| **Monochrome** (themed icon, Android 13+) | 108 x 108dp, single-color silhouette within the 66dp safe zone; the system tints it to match the wallpaper | VectorDrawable XML |
| Legacy launcher icon (Android 7.1 and lower) | 48dp at mdpi through xxxhdpi: 48, 72, 96, 144, 192 px | PNG 32-bit |
| **Google Play store icon** | 512 x 512 px, full-bleed square, 32-bit PNG, sRGB, max 1024 KB, **no rounded corners or shadows** (Play applies the mask) | PNG |
| Splash screen icon (Android 12+ SplashScreen API) | Icon **without** background: 288 x 288dp, content within a 192dp circle. Icon **with** background: 240 x 240dp, content within a 160dp circle. Splash background uses `Surface` light/dark | VectorDrawable XML |
| Notification small icon (optional, for sync notifications) | 24 x 24dp, white silhouette on transparent, content within 22dp | VectorDrawable XML |

### 4.3 iOS specifications

Follow [Apple HIG: App icons](https://developer.apple.com/design/human-interface-guidelines/app-icons).

| Asset | Spec | Format |
|---|---|---|
| App icon master | 1024 x 1024 px, square, **no transparency**, no pre-applied corner radius (the system masks it), sRGB or Display P3 | PNG |
| **Default (light)** appearance | Full-color icon | PNG |
| **Dark** appearance | Same symbol on a dark or transparent background, as described in the HIG dark appearance guidance | PNG |
| **Tinted** appearance | Grayscale version that the system tints | PNG |
| **Layered source** for current iOS releases (Liquid Glass) | Separate background and up to three foreground layers, built and previewed in Apple **Icon Composer** | `.icon` file plus layer SVGs |
| Launch screen | Apple discourages branded splash screens. Use a plain `Surface` background, optionally with the logo symbol centered at a small size. No text | Asset catalog color plus PDF/SVG symbol |
| App Store listing icon | Generated from the 1024 px master | n/a |

### 4.4 App icon acceptance checks

- [ ] Recognizable at 48dp and at 29pt (iOS Settings).
- [ ] Survives circle, squircle and rounded-square masks with no clipping of key content.
- [ ] Monochrome and tinted versions are legible on both a light and a dark wallpaper.
- [ ] Distinct from the Google Health Connect icon when shown next to it in Health Connect permission screens.
- [ ] Previewed on a real Pixel device and an iPhone, light and dark mode.

---

## 5. Deliverable B: Brand logo (Welcome screen, Privacy screen, splash)

The Welcome screen logo **is the application icon**. There is one brand mark: it is drawn once for the app icon canvas (§4) and reused as the logo inside the app.

### 5.1 Where the logo appears

| Screen | Current placeholder | Required asset |
|---|---|---|
| **S-01 Welcome** | 88 x 88dp rounded square (28dp radius), `Primary` fill, 48dp fruit glyph in `OnPrimary`, centered above the headline "Welcome to Snackshot" | The **app icon** artwork, 88 to 120dp. No wordmark: the headline already says "Welcome to Snackshot" |
| **S-01 Welcome, large-text mode** | Same mark reduced to 72 x 72dp (24dp radius) | Same symbol, must stay legible at 72dp |
| **S-01A Privacy details** | Same fruit glyph above the intro "Your data stays on your phone" | Logo symbol, small (about 56 to 72dp) |
| **Android splash / iOS launch screen** | Not designed yet | See §4.2 and §4.3 |
| **S-16 Settings → About** (future) | Not designed yet | Horizontal lockup |
| Store listings, website, presentations | Not designed yet | All lockups |

### 5.2 Logo deliverables

| Item | Description |
|---|---|
| **Symbol (brand mark)** | The app icon artwork as a standalone vector, for use inside the app (S-01, S-01A, splash) |
| **Wordmark** | "Snackshot", custom-lettered or based on a licensed typeface. The UI fonts are Google Sans and Google Sans Text; check their license before using them in a logo, otherwise draw custom lettering in a compatible geometric, rounded style |
| **Lockups** | Horizontal (symbol left, wordmark right) and stacked (symbol above wordmark) |
| **Color versions** | Full color on light `Surface` `#F8F9FA`; full color on dark `Surface` `#121314`; single-color dark (`#202124`); single-color light (`#FFFFFF`); a variant on `Primary` (as used on S-01 today) |
| **Construction sheet** | Grid, clear-space rule (at least the height of the symbol's main element), minimum sizes (for example 24dp symbol on screen, 16 mm in print) |
| **Misuse examples** | Stretching, recoloring outside the palette, adding effects, placing on busy images without a scrim |

**Formats:** SVG (master), Android VectorDrawable XML, PDF (vector, for iOS asset catalogs and print), and PNG at 1x, 2x, 3x for quick use.

### 5.3 Welcome screen composition rules

The S-01 layout is fixed by UX spec §4.2: `logo symbol → headline (32/40 Google Sans) → body copy (16/24) → Continue button → Privacy details button`. The logo must:

- sit centered in a 412dp-wide column with 24dp side padding,
- not push the Continue button below the fold on a 412 x 892dp screen with system font scale at 200%,
- remain decorative for accessibility (screen readers skip it; the headline carries the name).

---

## 6. Deliverable C: Welcome screen background image

### 6.1 Intent

Give the first screen a warm, inviting feeling that hints at the "snack + snapshot" idea, without competing with the text and buttons.

**Chosen direction: abstract shapes.** Soft, blurred color shapes (blobs, gradients) fading evenly to `Surface` behind the content. The colors are the designer's choice (see §3); they should feel fresh and food-positive and work in both themes.

**Avoid:** stock photography of people or bodies, realistic food photos (they compete with user photos and date quickly), text inside the image, and strong contrast behind the copy area.

### 6.2 Requirements

| Item | Requirement |
|---|---|
| Versions | **Light** (over `#F8F9FA`) and **Dark** (over `#121314`); both must look intentional, not inverted |
| Content-safe zone | The center and bottom area behind the headline, body copy and buttons (about y = 200 to 840dp on 412 x 892dp) must keep text contrast **≥ 4.5:1** with `OnSurface` / `OnSurfaceVariant` text |
| Aspect ratios | Design for 412 x 892dp (Pixel 9, about 9:19.5); verify crops at 9:16 (older phones), 9:21 (tall phones), foldable inner screen (about 4:3) and tablets. The image scales to fill (center-crop), so keep important elements away from the edges |
| Format, preferred | Vector (SVG and Android VectorDrawable) if the style allows; lightweight and sharp at any size |
| Format, raster | WebP (Android) and PNG/HEIC (iOS). Masters: 1440 x 3200 px (Android xxxhdpi class) and 1290 x 2796 px (iPhone @3x). Target **≤ 300 KB** per file |
| Motion | Static. Any optional animation must respect "Remove animations" / Reduce Motion |
| Accessibility | Decorative only, excluded from the accessibility tree |
| Large-text mode | At 200% font scale the copy covers more of the screen; the image must still not reduce text contrast |

> **Spec change required:** UX spec §4.2 currently says the Welcome screen uses a "solid light or dark theme surface". Adopting a background image means updating that line and the S-01 contrast edge case once the design is approved.

---

## 7. Deliverable D: In-app icon set

### 7.1 Icon style standard

The app currently uses **Google Material Symbols Rounded** (variable font: `FILL 0` outline by default, `FILL 1` for selected or status states, weight 400, grade 0, optical size 24). New icons must be **drop-in compatible** with this system so that untouched standard icons and new custom icons look like one family.

| Property | Requirement |
|---|---|
| Grid | 24 x 24dp canvas, 20 x 20dp live area (2dp padding), Material keyline shapes (circle 20dp, square 18dp, portrait 16 x 20dp, landscape 20 x 16dp) |
| Stroke | 2dp, rounded caps and joins, matching Material Symbols Rounded weight 400 |
| Corners | Outer corner radius 2dp (rounded family) |
| Styles | **Outlined** (default) and **Filled** (selected state and status). Both required for every custom icon |
| Color | Single color, black `#000000` on transparent; tinted at runtime |
| Sizes in use | 16dp (status bar, chips), 20dp (inline with text), 24dp (standard), 32dp (sheet icons), 48dp (empty-state and permission illustrations) |
| Touch target | Icon buttons keep a 48 x 48dp target regardless of icon size (UX spec §5.1) |
| Metaphor | Literal, universally understood; one idea per icon; no text inside icons |
| Optical balance | Check every icon next to Material Symbols `search`, `close` and `photo_camera` at 24dp |
| Formats | SVG (master), Android VectorDrawable XML, and iOS custom symbol (SF Symbols template SVG with weight variants, or PDF vector) |
| Naming | `ic_<name>_outlined` / `ic_<name>_filled`, snake_case, same `<name>` as the Material Symbol it replaces where possible (for example `ic_photo_camera_outlined`) |

**iOS note:** for post-MVP iOS, standard navigation icons (back, close, share, more) should use the **platform-native** SF Symbols, as Apple's HIG requires. Custom Snackshot icons (Redesign and Restyle groups below) ship on both platforms.

### 7.2 Action legend

| Action | Meaning |
|---|---|
| **Redesign** | Brand or domain icon. Create a custom Snackshot icon with its own character. Highest priority |
| **Restyle** | Keep the same, well-known metaphor but redraw it to match the Snackshot family so it sits naturally next to Redesign icons |
| **Keep** | Universal navigation or action icon. Keep Material Symbols Rounded (Android) or SF Symbols (iOS) for platform familiarity. No work needed |
| **New** | Icon that doesn't exist in the app yet but is recommended |

**Scope confirmed:** Redesign, Restyle and New icons are all drawn now, as one custom Snackshot set for the MVP.

### 7.3 Icon inventory

Each row shows how the icon looks in the app today (Material Symbols Rounded, outlined) and its Material Symbol name, so the developer can map each new file 1:1. Screens use the IDs from §8.

**Meal images:** the gallery picker always shows the user's real photos, and meal lists (Home, History, dashboards) show the real meal photo. A meal added without a photo shows **one** placeholder icon (`image`, Group 1). No food category icons are needed.

#### Group 1: Core meal-logging actions (Redesign, priority 1)

| Now | Current icon | Meaning | Used on screens | Action |
|---|---|---|---|---|
| <img src="icons_current/photo_camera.png" width="28" alt="photo_camera"> | `photo_camera` | Take a photo / AI recognition, the main brand action | S-01A, S-02 (carousel "Take a photo"), S-03 (shutter, permission states), S-12, S-14, S-15 (retake), S-16 (AI recognition), S-19 (sheet icon) | **Redesign** |
| <img src="icons_current/photo_library.png" width="28" alt="photo_library"> | `photo_library` | Pick from gallery | S-02 (carousel "Gallery"), S-03, S-05, S-16, S-19 | **Redesign** |
| <img src="icons_current/search.png" width="28" alt="search"> | `search` | Food database search | S-08, S-14 (post-MVP search); the "Quick search" carousel action and the Search buttons on S-05, S-15, S-19 are removed from the MVP | **Redesign**, lower priority (post-MVP) |
| <img src="icons_current/edit_note.png" width="28" alt="edit_note"> | `edit_note` | Manual add (kcal and macros); manual meal source | S-02 (carousel "Manual add", meal thumbnail), S-05, S-12, S-14, S-16 ("Write nutrition"), S-19 | **Redesign** |
| <img src="icons_current/image.png" width="28" alt="image"> | `image` | **Meal placeholder**: the single thumbnail for a meal logged without a photo (meals with a photo always show the real photo); also the generic "image" sign | S-02, S-04, S-09, S-12, S-14 | **Redesign** |
| <img src="icons_current/auto_awesome.png" width="28" alt="auto_awesome"> | `auto_awesome` | AI estimate / high recognition confidence | S-07, S-15 | **Redesign**: Snackshot's "AI" sign |
| <img src="icons_current/restaurant.png" width="28" alt="restaurant"> | `restaurant` | Meal (generic) | S-02, S-07, S-10, S-14 | **Redesign** |
| <img src="icons_current/nutrition.png" width="28" alt="nutrition"> | `nutrition` | Nutrition / nutrition preferences | S-16 | **Redesign** |
| <img src="icons_current/local_fire_department.png" width="28" alt="local_fire_department"> | `local_fire_department` | Calories (kcal) | S-16 (calorie goal) | **Redesign** |
| <img src="icons_current/bolt.png" width="28" alt="bolt"> | `bolt` | Quick add (shortcut to S-09) | S-05, S-15, S-19, S-20 | **Redesign**. Today the same action uses `edit_note` on S-02 and `bolt` on S-15; the new set should define **one** Quick add icon used everywhere |

#### Group 2: Macronutrients (New, priority 1)

Today, macros are shown as colored dots (carbs `#2D9CDB`, fat `#E5675C`, protein `#C77700`). Shape-coded icons improve accessibility because color is no longer the only signal. The style references in [icons_current](icons_current/) (`carbs.png` wheat, `fat.png`, `protein.png`, `meal.png`) are **direction only**; the Depositphotos sheet is a watermarked stock preview and must not be used or traced.

| Proposed icon | Meaning | Would be used on | Action |
|---|---|---|---|
| `ic_carbs` | Carbohydrates (for example a wheat ear or grain) | S-02, S-07, S-08, S-12, S-14, S-15 | **New** |
| `ic_fat` | Fat (for example a drop) | Same | **New** |
| `ic_protein` | Protein (for example an egg or drumstick) | Same | **New** |

#### Group 3: Main navigation (Restyle, priority 2)

| Now | Current icon | Meaning | Used on | Action |
|---|---|---|---|---|
| <img src="icons_current/home.png" width="28" alt="home"> | `home` | Home tab | S-02 | **Restyle** |
| <img src="icons_current/bar_chart.png" width="28" alt="bar_chart"> | `bar_chart` | Dashboard / statistics | S-02 | **Restyle** |
| <img src="icons_current/history.png" width="28" alt="history"> | `history` | History feed, recent items | S-01A, S-02, S-08, S-12, S-15, S-16 | **Restyle** |
| <img src="icons_current/settings.png" width="28" alt="settings"> | `settings` | Settings | S-02 | **Restyle** |
| <img src="icons_current/person.png" width="28" alt="person"> | `person` | Profile | S-02 | **Restyle** |

#### Group 4: Privacy, data and sync (Restyle, priority 2)

These carry the privacy-first promise and should feel calm and reassuring.

| Now | Current icon | Meaning | Used on | Action |
|---|---|---|---|---|
| <img src="icons_current/smartphone.png" width="28" alt="smartphone"> | `smartphone` | Data kept on this phone / local-only | S-01A, S-02 (sync state), S-10, S-11, S-16 | **Restyle** |
| <img src="icons_current/shield.png" width="28" alt="shield"> | `shield` | Privacy protection | S-10, S-16 | **Restyle** |
| <img src="icons_current/health_and_safety.png" width="28" alt="health_and_safety"> | `health_and_safety` | Health data permission | S-11, S-16 | **Restyle** |
| <img src="icons_current/cloud_off.png" width="28" alt="cloud_off"> | `cloud_off` | Nothing stored on servers / offline | S-01A, S-05, S-08 | **Restyle** |
| <img src="icons_current/lock.png" width="28" alt="lock"> | `lock` | Locked or protected | S-03, S-04, S-16 | **Restyle** |
| <img src="icons_current/key.png" width="28" alt="key"> | `key` | Access key / AI access | S-01A | **Restyle** |
| <img src="icons_current/account_circle.png" width="28" alt="account_circle"> | `account_circle` | Account (used to say "no account needed") | S-01A | **Restyle** |
| <img src="icons_current/favorite.png" width="28" alt="favorite"> | `favorite` | Health and wellbeing data | S-01A | **Restyle** |
| <img src="icons_current/tune.png" width="28" alt="tune"> | `tune` | Preferences, adjustments | S-01A, S-10, S-15 | **Restyle** |
| <img src="icons_current/sync.png" width="28" alt="sync"> | `sync` | Sync with Health Connect / syncing | S-01A, S-02, S-10, S-16 | **Restyle** |
| <img src="icons_current/cloud_done.png" width="28" alt="cloud_done"> | `cloud_done` | Synced | S-02 | **Restyle** |
| <img src="icons_current/sync_problem.png" width="28" alt="sync_problem"> | `sync_problem` | Sync failed | S-02 | **Restyle** |
| <img src="icons_current/sync_disabled.png" width="28" alt="sync_disabled"> | `sync_disabled` | Sync off | S-16 | **Restyle** |
| <img src="icons_current/link_off.png" width="28" alt="link_off"> | `link_off` | Disconnect Health Connect | S-16 | **Restyle** |
| <img src="icons_current/block.png" width="28" alt="block"> | `block` | Permission not allowed | S-01A, S-11, S-16 | **Restyle** |
| <img src="icons_current/extension_off.png" width="28" alt="extension_off"> | `extension_off` | Health Connect not available | S-11 | **Restyle** |
| <img src="icons_current/flag.png" width="28" alt="flag"> | `flag` | Nutrition goals | S-16 | **Restyle** |

#### Group 5: Photo guidance and image states (Restyle, priority 2)

| Now | Current icon | Meaning | Used on | Action |
|---|---|---|---|---|
| <img src="icons_current/lightbulb.png" width="28" alt="lightbulb"> | `lightbulb` | Photo tips | S-03 | **Restyle** |
| <img src="icons_current/wb_sunny.png" width="28" alt="wb_sunny"> | `wb_sunny` | Use good light | S-03 | **Restyle** |
| <img src="icons_current/light_mode.png" width="28" alt="light_mode"> | `light_mode` | Too dark, add light | S-15 | **Restyle** |
| <img src="icons_current/brightness_low.png" width="28" alt="brightness_low"> | `brightness_low` | Image too dark | S-05 | **Restyle** |
| <img src="icons_current/crop_free.png" width="28" alt="crop_free"> | `crop_free` | Frame the whole plate | S-03 | **Restyle** |
| <img src="icons_current/center_focus_strong.png" width="28" alt="center_focus_strong"> | `center_focus_strong` | Photo out of focus | S-15 | **Restyle** |
| <img src="icons_current/straighten.png" width="28" alt="straighten"> | `straighten` | Shoot from above / keep level | S-03 | **Restyle** |
| <img src="icons_current/flash_off.png" width="28" alt="flash_off"> | `flash_off` | Flash toggle | S-03 | **Restyle**; also needs `flash_on` / `flash_auto` |
| <img src="icons_current/no_photography.png" width="28" alt="no_photography"> | `no_photography` | Camera unavailable or permission denied; unusable photo | S-03, S-15 | **Restyle** |
| <img src="icons_current/hide_image.png" width="28" alt="hide_image"> | `hide_image` | No photo for this meal / image not supported | S-04, S-14 | **Restyle** |
| <img src="icons_current/screenshot_region.png" width="28" alt="screenshot_region"> | `screenshot_region` | Screenshot rejected as a meal photo | S-04 | **Restyle** |
| <img src="icons_current/landscape.png" width="28" alt="landscape"> | `landscape` | Not a food photo (landscape) | S-04 | **Restyle** |
| <img src="icons_current/download.png" width="28" alt="download"> | `download` | Downloaded image | S-04 | **Restyle** |

#### Group 6: Status and feedback (Restyle, priority 3)

| Now | Current icon | Meaning | Used on | Action |
|---|---|---|---|---|
| <img src="icons_current/check_circle.png" width="28" alt="check_circle"> | `check_circle` | Success, allowed, in range | S-01A, S-05, S-09, S-10, S-11, S-12, S-15, S-16, S-19 | **Restyle** |
| <img src="icons_current/error.png" width="28" alt="error"> | `error` | Error, low confidence | All screens with error states | **Restyle** |
| <img src="icons_current/info.png" width="28" alt="info"> | `info` | Information | S-09, S-10, S-16 | **Restyle** |
| <img src="icons_current/priority_high.png" width="28" alt="priority_high"> | `priority_high` | Attention required | S-04 | **Restyle** |
| <img src="icons_current/hourglass_top.png" width="28" alt="hourglass_top"> | `hourglass_top` | Processing; AI waitlist | S-05, S-09, S-15, S-19/S-20 | **Restyle** |
| <img src="icons_current/pending.png" width="28" alt="pending"> | `pending` | Checking permission | S-11 | **Restyle** |
| <img src="icons_current/schedule.png" width="28" alt="schedule"> | `schedule` | Waiting, time | S-05 | **Restyle** |
| <img src="icons_current/draft.png" width="28" alt="draft"> | `draft` | Unfinished meal draft saved | S-05 | **Restyle** |
| <img src="icons_current/restore.png" width="28" alt="restore"> | `restore` | Restoring settings and draft | S-01 | **Restyle** |
| <img src="icons_current/verified.png" width="28" alt="verified"> | `verified` | Goal reached / verified value | S-12 weekly | **Restyle** |
| <img src="icons_current/rule.png" width="28" alt="rule"> | `rule` | Partly connected | S-11 | **Restyle** |
| <img src="icons_current/fact_check.png" width="28" alt="fact_check"> | `fact_check` | Review values | S-15 | **Restyle** |
| <img src="icons_current/calculate.png" width="28" alt="calculate"> | `calculate` | Calculated value (kcal from macros) | S-09 | **Restyle** |
| <img src="icons_current/arrow_upward.png" width="28" alt="arrow_upward"> <img src="icons_current/arrow_downward.png" width="28" alt="arrow_downward"> | `arrow_upward` / `arrow_downward` | Above / below range | S-12 range | **Restyle** |

#### Group 7: Standard navigation and actions (Keep)

Universal controls. Keep Material Symbols Rounded on Android and use the matching SF Symbols on iOS. The designer only checks that they sit well next to the custom icons.

| Now | Current icon | Meaning | Action |
|---|---|---|---|
| <img src="icons_current/arrow_back.png" width="28" alt="arrow_back"> | `arrow_back` | Back | **Keep** |
| <img src="icons_current/arrow_forward.png" width="28" alt="arrow_forward"> | `arrow_forward` | Forward / next step | **Keep** |
| <img src="icons_current/close.png" width="28" alt="close"> | `close` | Close, cancel | **Keep** |
| <img src="icons_current/check.png" width="28" alt="check"> | `check` | Confirm, selected | **Keep** |
| <img src="icons_current/add.png" width="28" alt="add"> | `add` | Increase, add | **Keep** |
| <img src="icons_current/remove.png" width="28" alt="remove"> | `remove` | Decrease | **Keep** |
| <img src="icons_current/chevron_left.png" width="28" alt="chevron_left"> | `chevron_left` | Previous period | **Keep** |
| <img src="icons_current/chevron_right.png" width="28" alt="chevron_right"> | `chevron_right` | Next period, open row | **Keep** |
| <img src="icons_current/expand_more.png" width="28" alt="expand_more"> | `expand_more` | Expand | **Keep** |
| <img src="icons_current/arrow_drop_down.png" width="28" alt="arrow_drop_down"> | `arrow_drop_down` | Dropdown | **Keep** |
| <img src="icons_current/more_vert.png" width="28" alt="more_vert"> | `more_vert` | More actions | **Keep** |
| <img src="icons_current/edit.png" width="28" alt="edit"> | `edit` | Edit | **Keep** |
| <img src="icons_current/delete.png" width="28" alt="delete"> | `delete` | Delete | **Keep** |
| <img src="icons_current/undo.png" width="28" alt="undo"> | `undo` | Undo | **Keep** |
| <img src="icons_current/refresh.png" width="28" alt="refresh"> | `refresh` | Retry | **Keep** |
| <img src="icons_current/restart_alt.png" width="28" alt="restart_alt"> | `restart_alt` | Reset estimate | **Keep** |
| <img src="icons_current/open_in_new.png" width="28" alt="open_in_new"> | `open_in_new` | Open system settings | **Keep** |
| <img src="icons_current/share.png" width="28" alt="share"> | `share` | Share | **Keep** |
| <img src="icons_current/calendar_month.png" width="28" alt="calendar_month"> | `calendar_month` | Pick a date | **Keep** |
| <img src="icons_current/filter_alt_off.png" width="28" alt="filter_alt_off"> | `filter_alt_off` | Clear filter | **Keep** |
| <img src="icons_current/search_off.png" width="28" alt="search_off"> | `search_off` | No search results | **Keep** |
| <img src="icons_current/contrast.png" width="28" alt="contrast"> | `contrast` | Theme setting | **Keep** |

---

## 8. Screens and where the assets appear

Screenshots are taken from the current [HTML prototype](../Prototype/) (Pixel 9 frame, 412 x 892dp) in the light and dark themes. Icon groups (G1 to G7) refer to §7.3.

Two surfaces have no separate prototype page: **S-17 Confirmation dialogs** (confirm delete and disconnect; G4, G7 icons) and **S-18 Snackbars and banners** (save, sync, retry, undo; G6, G7 icons). The **app icon** also appears outside the app: launcher, Google Play, App Store and the splash / launch screen.

<div class="screen">

### S-01 Welcome

First screen: logo, "Welcome to Snackshot", the privacy promise, Continue and Privacy details.

**Brand assets:** App icon as logo, background image · **Icons:** Restore, error, refresh (G6, G7)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s01_welcome_light.png" width="260" alt="S-01 Welcome, light theme"></td><td><img src="screenshots/s01_welcome_dark.png" width="260" alt="S-01 Welcome, dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-01A Privacy details

Explains what stays on the phone, what is sent for AI recognition, and what changes with Health Connect.

**Brand assets:** App icon as logo (small) · **Icons:** Privacy and data (G4), camera (G1)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s01a_privacy_light.png" width="260" alt="S-01A Privacy details, light theme"></td><td><img src="screenshots/s01a_privacy_dark.png" width="260" alt="S-01A Privacy details, dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-02 Home

Start screen: daily calorie ring with macros, free-pictures counter, meal-entry carousel (Take a photo, Gallery, Manual add), recent meals, bottom navigation. The screenshots show the final design, including the "Quick search" carousel action, which is disabled in the MVP.

**Brand assets:** none · **Icons:** Core actions (G1), macros (G2), navigation (G3), sync state (G4)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s02_home_light.png" width="260" alt="S-02 Home, light theme"></td><td><img src="screenshots/s02_home_dark.png" width="260" alt="S-02 Home, dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-03 Camera capture

Live camera, shutter, flash, gallery shortcut, photo tips and free-pictures counter.

**Brand assets:** none · **Icons:** Camera and gallery (G1), photo guidance (G5)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s03_camera_light.png" width="260" alt="S-03 Camera capture, light theme"></td><td><img src="screenshots/s03_camera_dark.png" width="260" alt="S-03 Camera capture, dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-04 Gallery picker

Android photo picker with validation of the selected image.

**Brand assets:** none · **Icons:** Image states (G5)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s04_gallery_light.png" width="260" alt="S-04 Gallery picker, light theme"></td><td><img src="screenshots/s04_gallery_dark.png" width="260" alt="S-04 Gallery picker, dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-05 Recognition progress

Validating the photo and waiting for AI; cancelling keeps a draft.

**Brand assets:** none · **Icons:** Status (G6), offline (G4), fallbacks (G1)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s05_progress_light.png" width="260" alt="S-05 Recognition progress, light theme"></td><td><img src="screenshots/s05_progress_dark.png" width="260" alt="S-05 Recognition progress, dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-07 Edit nutrition (bottom sheet)

Correct the food name, grams, calories and macros.

**Brand assets:** none · **Icons:** AI estimate, meal (G1), stepper (G7)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s07_edit_nutrition_light.png" width="260" alt="S-07 Edit nutrition (bottom sheet), light theme"></td><td><img src="screenshots/s07_edit_nutrition_dark.png" width="260" alt="S-07 Edit nutrition (bottom sheet), dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-08 Manual food search (post-MVP)

Search the food database, pick a result, set the portion. Not part of the MVP; assets for this screen have the lowest priority.

**Brand assets:** none · **Icons:** Search, Quick add (G1)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s08_manual_food_search_light.png" width="260" alt="S-08 Manual food search, light theme"></td><td><img src="screenshots/s08_manual_food_search_dark.png" width="260" alt="S-08 Manual food search, dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-09 Quick add

Enter calories and macros by hand. Always free; also opened from S-05, S-15, S-19, and S-20 when AI is unavailable.

**Brand assets:** none · **Icons:** Calculate, info (G6), image (G1)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s09_quick_add_light.png" width="260" alt="S-09 Quick add, light theme"></td><td><img src="screenshots/s09_quick_add_dark.png" width="260" alt="S-09 Quick add, dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-10 Save / Google Health Connect prompt

On first save, explains optional Google Health Connect sync: keep local or connect.

**Brand assets:** none · **Icons:** Privacy and sync (G4)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s10_health_connect_prompt_light.png" width="260" alt="S-10 Save / Health Connect prompt, light theme"></td><td><img src="screenshots/s10_health_connect_prompt_dark.png" width="260" alt="S-10 Save / Health Connect prompt, dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-11 Health Connect authorization

Result of the permission request: connected, partial, denied or unavailable.

**Brand assets:** none · **Icons:** Permission states (G4, G6)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s11_health_connect_authorization_light.png" width="260" alt="S-11 Health Connect authorization, light theme"></td><td><img src="screenshots/s11_health_connect_authorization_dark.png" width="260" alt="S-11 Health Connect authorization, dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-12 Food and drink dashboard: day

Calories and macros for one day against the goal, with the meal list.

**Brand assets:** none · **Icons:** Macros (G2), meal source (G1), navigation (G7)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s12_daily_dashboard_light.png" width="260" alt="S-12 Food and drink dashboard: day, light theme"></td><td><img src="screenshots/s12_daily_dashboard_dark.png" width="260" alt="S-12 Food and drink dashboard: day, dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-12 Food and drink dashboard: week

Seven daily totals and daily averages.

**Brand assets:** none · **Icons:** Range status, verified (G6), macros (G2)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s12_weekly_dashboard_light.png" width="260" alt="S-12 Food and drink dashboard: week, light theme"></td><td><img src="screenshots/s12_weekly_dashboard_dark.png" width="260" alt="S-12 Food and drink dashboard: week, dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-12 Food and drink dashboard: month / 3 months / year

Long-range trends with personal range bands.

**Brand assets:** none · **Icons:** Range status arrows (G6)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s12_range_dashboard_light.png" width="260" alt="S-12 Food and drink dashboard: month / 3 months / year, light theme"></td><td><img src="screenshots/s12_range_dashboard_dark.png" width="260" alt="S-12 Food and drink dashboard: month / 3 months / year, dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-14 History feed

Chronological meal list with date filter, edit and delete.

**Brand assets:** none · **Icons:** Meal source (G1), image states (G5), actions (G7)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s14_history_feed_light.png" width="260" alt="S-14 History feed, light theme"></td><td><img src="screenshots/s14_history_feed_dark.png" width="260" alt="S-14 History feed, dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-15 Meal review

Review a recognized or saved meal: photo, confidence, grams, kcal, macros; save, edit or retake.

**Brand assets:** none · **Icons:** AI confidence (G1), photo quality (G5), status (G6)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s15_meal_review_light.png" width="260" alt="S-15 Meal review, light theme"></td><td><img src="screenshots/s15_meal_review_dark.png" width="260" alt="S-15 Meal review, dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-16 Settings

AI recognition status, Health Connect, image retention, nutrition goals, theme, privacy, delete all data.

**Brand assets:** Future: horizontal lockup in About · **Icons:** Privacy and sync (G4), nutrition (G1), theme (G7)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s16_settings_light.png" width="260" alt="S-16 Settings, light theme"></td><td><img src="screenshots/s16_settings_dark.png" width="260" alt="S-16 Settings, dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-19 AI paywall (bottom sheet)

Shown after the 90 free AI pictures are used; offers Quick add and Close.

**Brand assets:** none · **Icons:** Camera (G1), check (G6)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s19_paywall_light.png" width="260" alt="S-19 AI paywall (bottom sheet), light theme"></td><td><img src="screenshots/s19_paywall_dark.png" width="260" alt="S-19 AI paywall (bottom sheet), dark theme"></td></tr>
</table>

</div>

<div class="screen">

### S-20 AI waitlist (bottom sheet)

Shown when the 5-user pilot is full.

**Brand assets:** none · **Icons:** Hourglass (G6), quick add (G1)

<table class="shots">
<tr><th>Light</th><th>Dark</th></tr>
<tr><td><img src="screenshots/s20_waitlist_light.png" width="260" alt="S-20 AI waitlist (bottom sheet), light theme"></td><td><img src="screenshots/s20_waitlist_dark.png" width="260" alt="S-20 AI waitlist (bottom sheet), dark theme"></td></tr>
</table>

</div>

---

## 9. Hand-off package

### 9.1 Folder structure

```
docs/UX_UI/Design_brief/deliverables/
  app-icon/
    android/   ic_launcher_foreground.xml, ic_launcher_background.xml,
               ic_launcher_monochrome.xml, mipmap-*/ic_launcher.png,
               play_store_512.png, splash_icon.xml
    ios/       AppIcon-1024.png, AppIcon-1024-dark.png, AppIcon-1024-tinted.png,
               Snackshot.icon (Icon Composer), layers/*.svg
  logo/        snackshot_symbol.svg, snackshot_wordmark.svg,
               snackshot_lockup_horizontal.svg, snackshot_lockup_stacked.svg,
               color variants (light, dark, mono-dark, mono-light, on-primary),
               android/*.xml, ios/*.pdf, png/@1x @2x @3x
  welcome-background/
               welcome_bg_light.(svg|webp|png), welcome_bg_dark.(svg|webp|png)
  icons/
    svg/       ic_<name>_outlined.svg, ic_<name>_filled.svg
    android/   ic_<name>_outlined.xml, ic_<name>_filled.xml
    ios/       <name>.svg (SF Symbols custom-symbol template)
  source/      Figma link or .fig file, construction grids
```

### 9.2 Source files

- Figma (preferred), with **components and variants** for icons (outlined/filled) and logo (color versions).
- All vectors with outlined strokes in export files, editable strokes in the source.
- Color styles in the source file named after the Material 3 roles in §3.

---

## 10. References

- Material Design 3: [Icons](https://m3.material.io/styles/icons), [Color roles](https://m3.material.io/styles/color/roles), [Material Symbols](https://fonts.google.com/icons)
- Android: [Adaptive icons](https://developer.android.com/develop/ui/views/launch/icon_design_adaptive), [Splash screens](https://developer.android.com/develop/ui/views/launch/splash-screen), [Google Play icon specification](https://developer.android.com/distribute/google-play/resources/icon-design-specifications)
- Apple Human Interface Guidelines: [App icons](https://developer.apple.com/design/human-interface-guidelines/app-icons), [Launching](https://developer.apple.com/design/human-interface-guidelines/launching), [SF Symbols](https://developer.apple.com/design/human-interface-guidelines/sf-symbols)
- Accessibility: [WCAG 2.2](https://www.w3.org/TR/WCAG22/)
- Internal: [UX spec §3 Visual system, §4.2 Welcome, §5 Accessibility](../../Architecture/10_architecture_ux_spec.md), [HTML prototype](../Prototype/s01_welcome.html), [style references](icons_current/)
