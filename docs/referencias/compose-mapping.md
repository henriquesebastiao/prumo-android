# compose-mapping.md

Cada token CSS e cada componente deste design system → o nome correspondente em Kotlin/Compose no Netrik (`com.netrik.core.designsystem`). As tabelas abaixo descrevem o app **hoje**; a seção [M3 Expressive (v2)](#m3-expressive-v2--alvo) no fim dá o alvo de migração (Compose Material3 com `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`). Fonte: `theme/Theme.kt`, `Color.kt`, `Type.kt`, `ExtendedColors.kt` e `component/*.kt` em https://github.com/henriquesebastiao/netrik.

Convenção: `--md-<role-kebab>` ↔ `MaterialTheme.colorScheme.<role>`; `--md-success*`/`--md-warning*` ↔ `NetrikTheme.extendedColors.<role>`; `--type-<role>` ↔ `MaterialTheme.typography.<role>` ou `NetrikTheme.dataTypography.<role>`.

## Tema

| CSS / Web | Compose |
|---|---|
| `<AppTheme seed mode amoled dynamicColor>` | `NetrikTheme(darkTheme, dynamicColor, amoled) { … }` |
| `--seed: #136B79` | seed usado para gerar `Color.kt` (material-color-utilities, `SchemeTonalSpot`) |
| `data-theme="light"` / `:root` | `LightColorScheme` (`lightColorScheme(primary = primaryLight, …)`) |
| `data-theme="dark"` | `DarkColorScheme` |
| `data-theme="black"` | `DarkColorScheme.toPureBlack()` (privada em `Theme.kt`) |
| dynamic color | `dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)` (API 31+) |
| success/warning | `LightExtendedColors` / `DarkExtendedColors` via `LocalExtendedColors` |

## Cor

| Token CSS | Compose | Valor claro (Color.kt) |
|---|---|---|
| `--md-primary` | `colorScheme.primary` (`primaryLight`/`primaryDark`) | #006877 |
| `--md-on-primary` | `colorScheme.onPrimary` | #FFFFFF |
| `--md-primary-container` | `colorScheme.primaryContainer` | #A2EEFF |
| `--md-on-primary-container` | `colorScheme.onPrimaryContainer` | #004E5A |
| `--md-inverse-primary` | `colorScheme.inversePrimary` | #83D2E3 |
| `--md-secondary` / `--md-on-secondary` | `colorScheme.secondary` / `onSecondary` | #4A6268 / #FFFFFF |
| `--md-secondary-container` / `--md-on-secondary-container` | `colorScheme.secondaryContainer` / `onSecondaryContainer` | #CDE7ED / #334A50 |
| `--md-tertiary*` | `colorScheme.tertiary`, `onTertiary`, `tertiaryContainer`, `onTertiaryContainer` | #545D7E … |
| `--md-error*` | `colorScheme.error`, `onError`, `errorContainer`, `onErrorContainer` | #BA1A1A … |
| `--md-background` / `--md-on-background` | `colorScheme.background` / `onBackground` | #F5FAFC / #171D1E |
| `--md-surface` / `--md-on-surface` | `colorScheme.surface` / `onSurface` | #F5FAFC / #171D1E |
| `--md-surface-variant` / `--md-on-surface-variant` | `colorScheme.surfaceVariant` / `onSurfaceVariant` | #DBE4E7 / #3F484A |
| `--md-surface-tint` | `colorScheme.surfaceTint` | #006877 |
| `--md-inverse-surface` / `--md-inverse-on-surface` | `colorScheme.inverseSurface` / `inverseOnSurface` | #2B3133 / #ECF2F3 |
| `--md-outline` / `--md-outline-variant` | `colorScheme.outline` / `outlineVariant` | #6F797B / #BFC8CB |
| `--md-scrim` | `colorScheme.scrim` | #000000 |
| `--md-surface-bright` / `--md-surface-dim` | `colorScheme.surfaceBright` / `surfaceDim` | #F5FAFC / #D5DBDC |
| `--md-surface-container-lowest` | `colorScheme.surfaceContainerLowest` | #FFFFFF |
| `--md-surface-container-low` | `colorScheme.surfaceContainerLow` | #EFF5F6 |
| `--md-surface-container` | `colorScheme.surfaceContainer` | #E9EFF0 |
| `--md-surface-container-high` | `colorScheme.surfaceContainerHigh` | #E3E9EB |
| `--md-surface-container-highest` | `colorScheme.surfaceContainerHighest` | #DEE3E5 |
| `--md-success` / `--md-on-success` | `NetrikTheme.extendedColors.success` / `onSuccess` | #006D43 / #FFFFFF |
| `--md-success-container` / `--md-on-success-container` | `extendedColors.successContainer` / `onSuccessContainer` | #8AF8BA / #005231 |
| `--md-warning` / `--md-on-warning` | `extendedColors.warning` / `onWarning` | #855400 / #FFFFFF |
| `--md-warning-container` / `--md-on-warning-container` | `extendedColors.warningContainer` / `onWarningContainer` | #FFDDB7 / #653E00 |

Aliases semânticos (web) → role:

| Alias | Role Compose |
|---|---|
| `--surface-page` | `surface` (Scaffold) |
| `--surface-card`, `--surface-nav` | `surfaceContainer` |
| `--surface-card-low` | `surfaceContainerLow` |
| `--surface-card-high`, `--surface-hover` | `surfaceContainerHigh` |
| `--surface-card-highest`, `--surface-pressed` | `surfaceContainerHighest` |
| `--surface-value` | `surface` (blocos de valor dentro de card) |
| `--text-primary` / `--text-secondary` / `--text-accent` | `onSurface` / `onSurfaceVariant` / `primary` |
| `--text-copy-icon` | `outline` |
| `--border-subtle` / `--border-strong` | `outlineVariant` / `outline` |
| `--elevation-0…4` | `surface`, `surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest` |

Mocks do projeto de design usam `--sc1…--sc4`: `sc1` = `surfaceContainerLow`, `sc2` = `surfaceContainer`, `sc3` = `surfaceContainerHigh`, `sc4` = `surfaceContainerHighest`.

## Tipografia

| Token CSS | Compose |
|---|---|
| `--font-ui` (Roboto Flex) | `RobotoFlex` (`R.font.roboto_flex`, pesos 300–700 por `FontVariation.weight`) |
| `--font-data` (JetBrains Mono) | `JetBrainsMono` (`R.font.jetbrains_mono`, 400/500) |
| `--font-data-features: 'tnum'` | `fontFeatureSettings = "tnum"` (`MonoBase`) |
| `--type-display-large…small` | `MaterialTheme.typography.displayLarge…displaySmall` |
| `--type-headline-large…small` | `typography.headlineLarge…headlineSmall` |
| `--type-title-large` | `typography.titleLarge` (top app bar, título do sheet) |
| `--type-title-medium` | `typography.titleMedium` (título de card, SSID) |
| `--type-title-small` | `typography.titleSmall` (`SectionHeader`) |
| `--type-body-large/medium/small` | `typography.bodyLarge/bodyMedium/bodySmall` |
| `--type-label-large/medium/small` | `typography.labelLarge/labelMedium/labelSmall` |
| `--type-data-large` | `NetrikTheme.dataTypography.dataLarge` (22/28, Medium) |
| `--type-data-medium` | `dataTypography.dataMedium` (14/20, Medium) |
| `--type-data-small` | `dataTypography.dataSmall` (12/16, Normal) |
| `--type-terminal` | `dataTypography.terminal` (13/18) |
| `--type-*-tracking` | `letterSpacing` padrão do M3 em cada estilo |

## Forma, espaço, elevação

| Token CSS | Compose |
|---|---|
| `--shape-xs` 4 | `RoundedCornerShape(4.dp)` (snackbar M3, OutlinedTextField) |
| `--shape-chip-sm` 6 | `RoundedCornerShape(6.dp)` (`StatusChipSize.Small`) |
| `--shape-sm` 8 | `RoundedCornerShape(8.dp)` (chips 32dp, `Tile`) |
| `--shape-md` 12 | `RoundedCornerShape(12.dp)` (`ErrorCard`, `NoConnectionCard`, `AdvancedOptionsPanel`) |
| `--shape-lg` 16 / `--shape-lg-inner` 4 | `groupedItemShape(index, count)` (16 nas pontas, 4 entre itens); `NetworkCard` 16 |
| `--shape-xxl` 24 | `RoundedCornerShape(24.dp)` (ícone do `PermissionRationale`, `SearchTopBar`) |
| `--shape-sheet` 28 | forma padrão do `ModalBottomSheet` |
| `--shape-full` | `CircleShape` / forma padrão de `Button` |
| `--gutter` 16, `--section-gap` 24, `--space-2` | `padding(horizontal = 16.dp)`, `Arrangement.spacedBy(24.dp)`, `spacedBy(2.dp)` |
| `--touch-min` 48 | `Modifier.height(48.dp)` / `minimumInteractiveComponentSize()` |
| `--top-app-bar-height` 64 | `TopAppBar` (M3 small, 64dp) |
| `--nav-bar-height` 80 | `NavigationBar` |
| `--shadow-snackbar` | elevação padrão do `Snackbar` |
| `--state-disabled-opacity` .38 | alpha padrão de conteúdo desabilitado no M3 |
| `--icon-fvs` / `--icon-fvs-filled` | `R.drawable.ic_<nome>` / `R.drawable.ic_<nome>_filled` |

## Componentes

| Web (`components/…`) | Compose no Netrik | Notas |
|---|---|---|
| `AppTheme` | `NetrikTheme` | |
| `Icon name filled` | `Icon(painterResource(R.drawable.ic_<name>[_filled]), …)` | drawables gerados por `scripts/material_symbol.py` |
| `TopAppBar title onBack actions` | `NetrikTopAppBar(title, onBack, actions)` | |
| `TopAppBar search={…}` | `SearchTopBar(query, hint, closeLabel, onQueryChange, onClose, autoFocus)` | |
| `NavigationBar` | `NavigationBar` + `NavigationBarItem` (M3) | indicador `secondaryContainer`, ícone `_filled` quando selecionado |
| `SectionCard` | `Surface(shape = RoundedCornerShape(16.dp), color = surfaceContainer)` + `CardHeader` (privado em `NetworkCard.kt`) | |
| `ValueTile` | `Tile` / `FieldTile` / `PublicIpTile` (privados em `NetworkCard.kt`) | |
| `IconAvatar` | `IconAvatar(icon, containerColor, contentColor)` | |
| `KpiTile variant="tile"` | KPIs do Port Scanner (`feature/portscan/PortScanScreen.kt`) | grade 2×2 |
| `KpiTile variant="stat"` | estatísticas do Ping (`feature/ping/PingScreen.kt`) | |
| `SectionHeader` | `SectionHeader(text)` | |
| `GroupedList` / `groupedRadius()` | `Column(verticalArrangement = spacedBy(2.dp))` + `groupedItemShape(index, count)` | |
| `GroupedListItem` | `SettingsRow` (privado em `SettingsScreen.kt`) / itens do hub (`HubScreen.kt`) | |
| `SwitchRow` | `SwitchRow` (privado em `SettingsScreen.kt`) | variante inline = linha do Port Scanner |
| `Switch` | `Switch(checked, onCheckedChange, enabled)` (M3) | |
| `TextField` | `TargetField(...)` (alvo) / `NumberOptionField(...)` (compacto) / `OutlinedTextField` | |
| `StatusChip tone size` | `StatusChip(label, tone: StatusTone, size: StatusChipSize)` | `running` com spinner = `RunningChip` (privado em `ToolComponents.kt`) |
| `FilterChip` | `FilterChip` (M3) | |
| `FilterChip leadingIcon="history" mono` | `RecentTargets(targets, enabled, onSelect)` (`OutlinedButton` 32dp) | |
| `CopyChip` | chips IP/MAC em `feature/devices/DevicesScreen.kt` | |
| `Button variant="filled" size="large"` | `Button(modifier = fillMaxWidth().height(48.dp))` / `RunButton(running = false)` | |
| `Button variant="stop"` | `RunButton(running = true)` (`errorContainer`) | |
| `Button variant="tonal"` | `FilledTonalButton` | |
| `Button variant="text"` | `TextButton` | |
| `Button variant="outlined"` | `OutlinedButton` | |
| `Button variant="error"` | botão de `ErrorCard` (`buttonColors(error, onError)`) | |
| `SegmentedButton` | `SingleChoiceSegmentedButtonRow` + `SegmentedButton` | |
| `BottomSheet` | `ModalBottomSheet` (ex.: `NetworkDetailsSheet`) | |
| `Snackbar` | `SnackbarHost(LocalSnackbarHostState.current)` / `rememberCopyAction()` | |
| `Spinner` | `CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)` | |
| `ProgressBar` | `LinearProgressIndicator` | |
| `EmptyState` | `ToolEmptyState(icon, text)` | |
| `ErrorCard` | `ErrorCard(title, body, onRetry, retryLabel, retryIcon)` | |
| `NoConnectionCard` | `NoConnectionCard()` | |
| `PlaceholderContent` | `PlaceholderContent(icon, title, text, actionLabel, onAction)` | |
| `PermissionRationale` | `PermissionRationale(icon, title, body, items, note, primaryLabel, onPrimary, laterLabel, onLater)` | |

Peças do UI kit sem componente próprio aqui: `AdvancedOptionsPanel(summary, expanded, onToggle)`, `RunHeader(chipLabel, chipTone, running, runLine, onCopy)` e `Modifier.bleedHorizontal(16.dp)` (lista horizontal até a borda), todos em `ToolComponents.kt`.

## M3 Expressive (v2) · alvo

### Tema

| CSS / Web | Compose alvo |
|---|---|
| `<AppTheme motion="expressive">` | `MaterialExpressiveTheme(colorScheme, motionScheme = MotionScheme.expressive(), shapes, typography)` dentro de `NetrikTheme` |
| `data-motion="standard"` | `MotionScheme.standard()` |
| `--motion-spatial-fast` / `--motion-spatial` / `--motion-spatial-slow` | `MaterialTheme.motionScheme.fastSpatialSpec()` / `defaultSpatialSpec()` / `slowSpatialSpec()` |
| `--motion-effects-fast` / `--motion-effects` / `--motion-effects-slow` | `motionScheme.fastEffectsSpec()` / `defaultEffectsSpec()` / `slowEffectsSpec()` |
| `--md-sys-motion-spring-*-damping/stiffness` | valores de `ExpressiveMotionTokens` / `StandardMotionTokens` |
| `--type-<role>-emphasized` | `MaterialTheme.typography.<role>Emphasized` (ex.: `titleLargeEmphasized`) |
| `--md-sys-shape-corner-extra-small … extra-large` | `MaterialTheme.shapes.extraSmall … extraLarge` / `ShapeDefaults.*` |
| `--md-sys-shape-corner-large-increased` (20) | `ShapeDefaults.LargeIncreased` |
| `--md-sys-shape-corner-extra-large-increased` (32) | `ShapeDefaults.ExtraLargeIncreased` |
| `--md-sys-shape-corner-extra-extra-large` (48) | `ShapeDefaults.ExtraExtraLarge` |
| `--md-sys-elevation-level1…5` | `ElevationTokens.Level1…5` (`Surface(shadowElevation = …)`) |
| `--md-sys-state-*-opacity` | state layers da `ripple()` / `LocalIndication` |

### Componentes

| Web | Compose hoje (Netrik) | Compose alvo (M3E) |
|---|---|---|
| `Button size="xs|s|m|l|xl"` | `Button` 40/48dp | `Button(modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight), contentPadding = ButtonDefaults.contentPaddingFor(height), shapes = ButtonDefaults.shapesFor(height))` |
| `Button shape="square"` | — | `shapes = ButtonShapes(shape = ButtonDefaults.squareShape, pressedShape = ButtonDefaults.pressedShape)` |
| `Button toggle selected` | — | `ToggleButton(checked, onCheckedChange)` / `ElevatedToggleButton` / `TonalToggleButton` / `OutlinedToggleButton` |
| `Button variant="elevated"` | — | `ElevatedButton` |
| `Button size="m" fullWidth` (ação principal) | `RunButton` 48dp | `RunButton` com altura `ButtonDefaults.MediumContainerHeight` (56dp) e `shapesFor` |
| `IconButton` | `IconButton` | `IconButton(shapes = IconButtonDefaults.shapes())`, `FilledIconButton`, `FilledTonalIconButton`, `OutlinedIconButton`; tamanho `Modifier.size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))`; toggles `IconToggleButton` / `FilledIconToggleButton` |
| `ButtonGroup variant="connected"` | `SingleChoiceSegmentedButtonRow` | `Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) { ToggleButton(shapes = ButtonGroupDefaults.connectedLeadingButtonShapes() / connectedMiddleButtonShapes() / connectedTrailingButtonShapes()) }` |
| `ButtonGroup variant="standard"` | — | `ButtonGroup(overflowIndicator = …) { clickableItem(…) / toggleableItem(…) }` (`Modifier.animateWidth(interactionSource)`) |
| `SegmentedButton` (v1) | `SegmentedButton` | substituir por ButtonGroup conectado |
| `Fab size="fab|medium|large"` | — | `FloatingActionButton` / `MediumFloatingActionButton` / `LargeFloatingActionButton` |
| `Fab label` | — | `ExtendedFloatingActionButton` / `MediumExtendedFloatingActionButton` / `LargeExtendedFloatingActionButton` (`expanded`) |
| `FloatingToolbar` | — | `HorizontalFloatingToolbar(expanded, floatingActionButton = { FloatingToolbarDefaults.VibrantFloatingActionButton { … } }, colors = FloatingToolbarDefaults.standardFloatingToolbarColors() / vibrantFloatingToolbarColors())` |
| `NavigationBar` (64dp) | `NavigationBar` + `NavigationBarItem` (80dp) | `ShortNavigationBar` + `ShortNavigationBarItem` |
| `TopAppBar size="medium"` | `NetrikTopAppBar` | `MediumFlexibleTopAppBar(title, subtitle, titleHorizontalAlignment)` |
| `TopAppBar subtitle` | — | `TopAppBar(title, subtitle = { … })` |
| `LoadingIndicator` | `CircularProgressIndicator` | `LoadingIndicator()` / `ContainedLoadingIndicator()` |
| `ProgressBar wavy` | `LinearProgressIndicator` | `LinearWavyProgressIndicator(progress = { … })` (indeterminado sem `progress`) |
| `ProgressBar` (plana) | `LinearProgressIndicator` | `LinearProgressIndicator(gapSize = 4.dp, drawStopIndicator = …)` (padrão atual) |
| `MaterialShape shape="cookie9"` | — | `Modifier.clip(MaterialShapes.Cookie9Sided.toShape())` |
| nomes de `MaterialShape` | — | `circle`→`Circle`, `square`→`Square`, `cookie4/6/9/12`→`Cookie4Sided/Cookie6Sided/Cookie9Sided/Cookie12Sided`, `sunny`→`Sunny`, `soft-burst`→`SoftBurst`, `flower`→`Flower`, `clover4`→`Clover4Leaf`, `pentagon`→`Pentagon`, `triangle`→`Triangle`, `pill`→`Pill`, `oval`→`Oval` |
| `IconAvatar shape` | `IconAvatar` (círculo) | `IconAvatar` + parâmetro `shape: Shape = CircleShape` recebendo `MaterialShapes.X.toShape()` |
| `SectionCard emphasis="hero"` | `Surface(RoundedCornerShape(16.dp))` | `Surface(shape = MaterialTheme.shapes.extraLarge)` + título `titleLargeEmphasized` |
| `Switch` (polegar 28 ao pressionar) | `Switch` | `Switch` (comportamento padrão) |
| `BottomSheet` | `ModalBottomSheet` | `ModalBottomSheet` (animações seguem o `motionScheme`) |
| `StateLayer` / `useInteraction` | ripple | `ripple()` + `MutableInteractionSource` |

## v3 · Expressive+ (inspirada no PixelPlayerOSS)

| Web | Compose |
|---|---|
| `--font-ui` Google Sans Flex + `--font-ui-fvs: 'ROND' 100` | `FontFamily(Font(R.font.google_sans_flex, weight, variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.Setting("ROND", 100f))))` (PixelPlayer `RoundedSans`) |
| `--type-display-hero` (wdth 151, ROND 0, 60sp 600, −0.02em, lh .95em) | `TextStyle(fontFamily = GoogleSansFlex, fontWeight = SemiBold, fontSize = 60.sp, letterSpacing = (-0.02).em, lineHeight = 0.95.em)` + `FontVariation.width(151f)` (PixelPlayer usa `TextGeometricTransform(scaleX = 1.5f)`) |
| `--type-page-title` / displayLarge 48/56 bold | `MaterialTheme.typography.displayLarge` com a escala de `Type.kt` do PixelPlayer |
| `--shape-list-outer` 24 / `--shape-list-inner` 8 / `--list-gap` 4 | `getSegmentedItemShape(index, count, outerRadius = 24.dp, innerRadius = 8.dp)` + `Arrangement.spacedBy(4.dp)` |
| `GroupedList variant="separated"` | `RoundedCornerShape(24.dp)` em cada item + `spacedBy(8.dp)` |
| `--shape-artwork-thumb` (squircle 22%) | `AbsoluteSmoothCornerShape(12.dp, 60)` (smooth-corner-rect) em 56–64dp |
| `AppTheme variant` | `dynamicColorScheme(seedColor, isDark, style = PaletteStyle.TonalSpot/Vibrant/Expressive/Content/Fidelity)` (material-kolor) ou MCU `DynamicScheme` |
| `seedFromImage(img)` | Palette/MCU `QuantizerCelebi` + `Score` sobre o bitmap da capa (PixelPlayer `ColorSchemeProcessor`, cache em `AlbumArtThemeEntity`) |
| `TopAppBar size="large"` | `LargeFlexibleTopAppBar` ou `Column { Row { FilledTonalIconButton(CircleShape) } ; Text(displayLarge) }` |
| `PillTabs` | `LazyRow` de `Surface(shape = CircleShape, color = if (sel) primary else surfaceContainerLowest)` + indicador 4×28dp |
| `NavigationBar variant="floating"` | `NavBarStyle.DEFAULT`: `Surface(shape = RoundedCornerShape(28.dp), modifier = Modifier.padding(horizontal = 16.dp))` com `NavigationBarItem`; `FULL_WIDTH` = barra padrão |
| `Tag` | `AssistChip` / `Surface(CircleShape)` + `Text(labelLarge)` |
| `KpiTile variant="hero"` | `Surface(color = onPrimaryContainer.copy(alpha = .1f), shape = RoundedCornerShape(24.dp))` + `Text(40.sp, Bold)` |
| `SectionCard tone="primary"` | `Surface(color = primaryContainer, contentColor = onPrimaryContainer, shape = RoundedCornerShape(32.dp))` |
| `EmptyState variant="hero"` | `Icon(88.dp, tint = primary)` + `Text(headlineMedium)` + `Text(bodyLarge)` |
| `Artwork` | Coil `AsyncImage` + `Modifier.clip(shape)`; placeholder `Box(background = container) { Icon(music_note) }` |
| `ShapeCollage` | `Box` de `Artwork` com `MaterialShapes.Pill/Cookie9Sided/SoftBurst.toShape()` e `rotate()` (PixelPlayer `CollagePatterns`) |
| `MediaListItem` | PixelPlayer `ExpressiveSongListItem` (arte 56–64dp, `titleMedium`, `GraphicEq` quando tocando) |
| `MiniPlayer` | `Surface(primaryContainer, RoundedCornerShape(24.dp))` observando `MediaController` (`Player.Listener`) |
| `PlaybackControls` | `Row` de `Button`/`FilledTonalButton` com `Modifier.weight(animateFloatAsState(...))` e `shapes` animadas; ações `player.seekToPrevious()/play()/pause()/seekToNext()` |
| `Slider variant="wavy"` | `LinearWavyProgressIndicator` + gesto, ou o `WavySliderExpressive`/`WavyMusicSlider` do PixelPlayer |
| `Slider` (padrão) | `Slider` M3 (track 16dp, handle em barra, `steps`) |
| `HeroHeader` | `Text(ExpTitle.displayLarge)` + `LargeFloatingActionButton(shape = CircleShape)` |
| `OnboardingFooter` | `Surface(surfaceContainer, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))` + `FilledTonalIconButton(96.dp)` com cantos animados por etapa |
| `IconButton variant="surface" size="r"` | `FilledTonalIconButton(colors = surfaceContainerHigh, Modifier.size(48.dp))` |
