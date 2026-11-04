# Henrique Sebastião — Android · Design System

Design system para apps Android nativos (Jetpack Compose + Material 3) do Henrique Sebastião, extraído do app **Netrik**, um kit de ferramentas de rede open source (GPL-3.0) para analistas de rede, pentesters e curiosos. Roda no celular, sem root e sem rastreamento.

O sistema é genérico: cada app troca **só o seed** de cor. Todo o resto (tipografia, forma, espaço, movimento, componentes, voz) é compartilhado.

**Versão 2 · Material 3 Expressive.** A base Netrik foi levada para o M3 Expressive (escala de forma completa, morph de forma, estilos tipográficos emphasized, esquema de movimento com molas, novos componentes e tamanhos). O que mudou em relação ao app atual está em [M3 Expressive](#m3-expressive) e em `compose-mapping.md` (coluna “alvo M3E”).

**Versão 3 · Expressive+ (inspirada no PixelPlayer).** Para apps bonitos e marcantes (Netrik, Downtfy e os próximos), a v3 adota Google Sans Flex arredondada, títulos de página grandes e em negrito, um título hero largo, listas com cantos de 24dp, abas em pílula, barra de navegação flutuante, cores tiradas da capa (content color) e componentes de mídia para Media3. Veja [v3 · Expressive+](#v3--expressive).

## Fontes

Em ordem de prioridade. Nada aqui foi inventado fora delas; o que faltou está em [Lacunas](#lacunas).

1. **Projeto Claude Design "Netrik"** (`a79c09a3-0c10-41a4-97e4-8fde20361378`), arquivo enviado `uploads/Netrik Etapa 1.dc.html` (ícone, seed, esquema, tipografia, forma) e as telas `NetrikHub`, `NetrikTool` (Ping/Traceroute), `NetrikDevices`, `NetrikWifi`, `NetrikPorts`, `NetrikSSH`, `NetrikOUI`, `NetrikIcon`, `Netrik Protótipo navegável`. Link: https://claude.ai/design/p/a79c09a3-0c10-41a4-97e4-8fde20361378
2. **Código**: https://github.com/henriquesebastiao/netrik (branch `main`)
   - `app/src/main/java/com/netrik/core/designsystem/theme/` → `Theme.kt`, `Color.kt`, `Type.kt`, `ExtendedColors.kt`
   - `app/src/main/java/com/netrik/core/designsystem/component/` → `NetrikTopAppBar`, `SearchTopBar`, `GroupedList` (`groupedItemShape`, `SectionHeader`), `IconAvatar`, `StatusChip`, `PermissionRationale`, `PlaceholderContent`, `ToolComponents` (`TargetField`, `RecentTargets`, `AdvancedOptionsPanel`, `NumberOptionField`, `RunButton`, `ToolEmptyState`, `RunHeader`, `ErrorCard`, `NoConnectionCard`)
   - `feature/hub/NetworkCard.kt` (card de rede, `Tile`, bottom sheet de detalhes), `feature/settings/SettingsScreen.kt` (`SwitchRow`, `ThemeRow`)
   - `res/font/` (Roboto Flex e JetBrains Mono, copiadas), `res/drawable/` (ícones Material Symbols como vector drawables), `res/values*/strings.xml` (textos en e pt-BR)
   - `CLAUDE.md` do repositório: decisões de design aprovadas (ícone 2b, seed, hub em lista agrupada, regras de layout 360–412dp, "MAC indisponível").
3. **Material 3 Expressive**: https://m3.material.io/ (shape, buttons, button groups, icon buttons, split button, navigation bar, toolbars, loading/progress indicators, motion) e a pesquisa https://design.google/library/expressive-material-design-google-research. O site m3.material.io não renderiza sem JavaScript; medidas de componentes vieram do site via busca e foram conferidas com os tokens do Jetpack Compose Material3 (`ShortNavigationBar`, `FloatingToolbarTokens`, `ExpressiveMotionTokens`, `TypeScaleTokens`).
4. **PixelPlayerOSS** (inspiração da v3, GPL-3.0): https://github.com/PixelPlayerHQ/PixelPlayerOSS (fork FOSS de https://github.com/PixelPlayerHQ/PixelPlayer). Lidos: `ui/theme/Type.kt`, `Shape.kt`, `presentation/utils/SegmentedItemShape.kt`, `subcomps/ExpressiveSongListItem.kt`, `data/preferences/NavBarStyle.kt`, `CollagePatterns.kt`; copiado `res/font/gflex_variable.ttf` (Google Sans Flex). Mais 25 capturas de tela do app enviadas pelo usuário em `uploads/`. Nenhum código, ícone ou marca do PixelPlayer foi copiado; o UI kit de mídia é uma composição original com os mesmos padrões.

Vale explorar o repositório e o projeto de design para detalhes que não couberam aqui (estados de cada tela, textos completos, gráficos de espectro e latência, terminal SSH).

## Índice

| Caminho | O que é |
|---|---|
| `styles.css` | Entrada única: só `@import` dos tokens |
| `tokens/colors.css` | Esquema M3 claro, escuro e preto puro + success/warning + aliases semânticos |
| `tokens/typography.css` | Escala M3 + 15 estilos emphasized (Roboto Flex) + escala de dados (JetBrains Mono) |
| `tokens/shape.css` | Escala de cantos M3E (none → extra-extra-large, full), cantos de botões por tamanho, formas semânticas |
| `tokens/motion.css` | Molas M3E (expressive/standard) como `linear()` + durações, `data-motion`, movimento reduzido, keyframes |
| `tokens/elevation.css` | Elevação tonal, sombras level 0–5 (só elementos flutuantes), opacidades de state layer |
| `tokens/spacing.css`, `icons.css`, `fonts.css`, `base.css` | Espaço e medidas, ícones, `@font-face`, base |
| `components/` | Componentes React (cada um com `.jsx`, `.d.ts`, `.prompt.md`) e um card por pasta |
| `guidelines/` | Cards de fundamentos (cores, tipo, emphasized, forma, morph, movimento, elevação, state layers, layout, ícones, marca, voz, táticas expressivas) |
| `ui_kits/netrik/` | UI kit interativo do Netrik em celulares 412×915, claro e escuro |
| `ui_kits/media/` | UI kit de referência v3 (app de música): início, biblioteca, tocando agora, letra, configurações, onboarding |
| `assets/fonts/` | `google_sans_flex.ttf` (v3, eixos wght/wdth/ROND/opsz, do PixelPlayerOSS), `roboto_flex.ttf`, `jetbrains_mono.ttf` (do Netrik) |
| `assets/netrik/` | Ícone do app (SVG convertido dos vector drawables) e os XML originais |
| `compose-mapping.md` | Cada token e componente → nome em Kotlin/Compose no Netrik |
| `SKILL.md` | Skill para uso no Claude Code |
| `github.md` | Associação com o repositório e mapa de telas |

## Componentes

Todos em `window.HenriqueSebastiOAndroidDesignSystem_5ac522` nos cards; o mapeamento para Compose está em `compose-mapping.md`.

- **theme/**: `AppTheme` (seed, claro/escuro/preto, esquema de movimento), `Icon`, `MaterialShape`, `StateLayer` (+ `useInteraction`, `FocusRing`)
- **navigation/**: `TopAppBar` (small 64dp, subtítulo, medium flexível, busca), `NavigationBar` (64dp flexível ou pílula flutuante), `FloatingToolbar`, `PillTabs` (abas em pílula)
- **containers/**: `SectionCard`, `ValueTile`, `IconAvatar`, `KpiTile`
- **lists/**: `GroupedList`, `GroupedListItem`, `SectionHeader`, `SwitchRow`
- **chips/**: `StatusChip` (success/warning/error/neutral/running, 32 e 24dp), `FilterChip`, `CopyChip`, `Tag` (rótulo em pílula)
- **actions/**: `Button` (XS/S/M/L/XL, redondo/quadrado, morph ao pressionar, toggle; filled, tonal, elevated, outlined, text, stop, error), `IconButton` (5 tamanhos, 3 larguras, toggle), `ButtonGroup` (connected e standard), `Fab` (56/80/96, estendido), `SegmentedButton` (v1, agora desenha um ButtonGroup conectado)
- **inputs/**: `TextField` (padrão 56dp e compacto 48dp), `Switch`, `Slider` (M3E com barra e paradas, ou onda para seek)
- **overlays/**: `BottomSheet`, `Snackbar`
- **media/**: `Artwork` (capa ou placeholder em squircle/círculo/forma Material), `MediaListItem` (linha de conteúdo), `MiniPlayer` (player recolhido, Media3), `PlaybackControls` (transporte expressivo), `ShapeCollage` (colagem de capas em formas)
- **layout/**: `HeroHeader` (título display largo + ação circular), `OnboardingFooter` (rodapé de etapas com botão que muda de forma)
- **feedback/**: `LoadingIndicator` (formas que se transformam), `ProgressBar` (plana com gap e ponto final, ou ondulada), `Spinner`, `EmptyState`, `ErrorCard`, `NoConnectionCard`, `PlaceholderContent`, `PermissionRationale`

### Adições intencionais

- `AppTheme`: equivalente web do `NetrikTheme`, necessário para aplicar o esquema e gerar outro seed em tempo de execução.
- `Icon`: wrapper da fonte Material Symbols Rounded (no app cada ícone é um vector drawable).
- `ValueTile`, `KpiTile`, `CopyChip`: existem no código/design como peças privadas de telas (`Tile` do `NetworkCard`, KPIs do Port Scanner e estatísticas do Ping, chips IP/MAC de Dispositivos); viraram componentes porque se repetem.
- `Spinner`, `ProgressBar`: `CircularProgressIndicator` e `LinearProgressIndicator` do M3 com as medidas usadas no design.
- **M3 Expressive (v2)**: `IconButton`, `ButtonGroup`, `Fab`, `FloatingToolbar`, `LoadingIndicator`, `MaterialShape`, `StateLayer`. Não existem no app Netrik hoje; vêm do catálogo M3 Expressive para que telas novas sigam o padrão. Split button, FAB menu, carrossel e rail de navegação não foram adicionados (sem uso nas telas atuais).

## Trocando o seed

O seed é o único parâmetro por app. Netrik: `#136B79` (azul-petróleo).

**No protótipo/mock**: `<AppTheme seed="#8F4C38" mode="dark">…</AppTheme>`. O seed padrão usa os tokens estáticos; qualquer outro é gerado em tempo de execução com `material-color-utilities` (SchemeTonalSpot, contraste 0), incluindo success/warning harmonizados e o preto puro. No UI kit, o painel **Tweaks** troca o seed.

**Nos tokens estáticos** (`tokens/colors.css`): regenere o arquivo com o mesmo algoritmo e troque `--seed`. Nenhum outro arquivo muda: componentes só leem `var(--md-*)`.

**No app Android**: gere o esquema com material-color-utilities (ou o Material Theme Builder, mesmo algoritmo) a partir do novo seed e substitua os valores em `core/designsystem/theme/Color.kt`; success (`#3B8A3E`) e warning (`#B5651D`) passam por `Blend.harmonize(base, seed)` com tons 40/100/90/30 (claro) e 80/20/30/90 (escuro). `Theme.kt`, `Type.kt`, `ExtendedColors.kt` e os componentes não mudam. Também troque `ic_launcher_background` e o ícone do app.

Verificado: o JS `material-color-utilities@0.3.0` reproduz exatamente os valores de `Color.kt` para `#136B79`.

## M3 Expressive

O que a v2 aplica, com os valores usados:

- **Forma**: escala completa `none 0 · extra-small 4 · small 8 · medium 12 · large 16 · large-increased 20 · extra-large 28 · extra-large-increased 32 · extra-extra-large 48 · full`. Botões e botões de ícone repousam redondos (raio finito = altura/2) ou quadrados (XS/S 12 · M 16 · L/XL 28) e ficam mais quadrados enquanto pressionados (XS/S 8 · M 12 · L/XL 16). Toggles selecionados trocam redondo↔quadrado. Formas Material (cookie, sunny, soft burst, pill…) em avatares de destaque via `MaterialShape` / `IconAvatar shape`.
- **Tamanho**: botões XS 32 · S 40 · M 56 · L 96 · XL 136 (padding 12/16/24/48/64; ícone 20/20/24/32/40; rótulo labelLarge, titleMedium, headlineSmall, headlineLarge). A ação principal de cada tela passa de 48 para **M 56dp**. FAB 56 / 80 / 96 (cantos 16 / 20 / 28).
- **Tipografia**: 15 estilos emphasized com o mesmo tamanho e peso maior (400→500, 500→700). Usados em títulos de abas raiz (app bar média), no card hero, no título da permissão e em resultados-chave.
- **Movimento**: `MotionScheme.expressive()`. Molas espaciais (posição, tamanho, raio, rotação) fast 0.6/800 · default 0.8/380 · slow 0.8/200, com leve overshoot; molas de efeito (cor, opacidade) 1.0/3800 · 1600 · 800, sem overshoot. Na web cada mola é a resposta analítica amostrada em `linear()` (fast 359ms, default 435ms, slow 599ms; efeitos 150/231/326ms). `data-motion="standard"` troca para 0.9/1400 · 700 · 300. Movimento reduzido zera só o espacial.
- **Navegação**: navigation bar flexível de **64dp** (era 80), indicador 56×32 que abre com mola, rótulo ativo em `secondary` sem negrito. Abas raiz com app bar média (headlineMedium emphasized); telas empilhadas com app bar small de 64dp e subtítulo opcional.
- **Seleção**: segmented buttons deixam de ser recomendados; viram `ButtonGroup` conectado (2dp entre itens, cantos externos redondos, internos XS 4 · S/M 8 · L 16 · XL 20; item selecionado fica totalmente redondo).
- **Espera**: `LoadingIndicator` (48dp, forma de 38dp que passa por soft burst → cookie 9 → pentágono → pill → sunny → cookie 4 → oval) para esperas curtas; `ProgressBar wavy` (amplitude 3, comprimento de onda 40/20) para scans longos; trilha com gap de 4dp e ponto final.
- **Contenção e foco**: um card hero por tela (`SectionCard emphasis="hero"`: cantos 28, padding 20, avatar cookie 48, título emphasized). Listas segmentadas continuam (2dp, 16/4).
- **Flutuantes**: FAB e floating toolbar são os únicos com sombra (level 3 e level 1); a barra inferior de app é substituída pela floating toolbar.
- **State layers**: hover 8%, foco 10% + anel de 3dp em `secondary`, pressionado 10% + morph, arrastado 16%; desabilitado 38% conteúdo / 12% container.
- **Contexto**: a pesquisa do Google mostra ganhos de localização de ações com tamanho, cor e contenção, mas também perda de usabilidade quando padrões básicos são quebrados (listas soltas, ações sem rótulo). Aqui a expressividade fica nos pontos de foco; listas, dados e rótulos continuam previsíveis.

## v3 · Expressive+

Valores e regras novos, todos em tokens:

- **Tipografia**: `--font-ui` = Google Sans Flex com `'ROND' 100` (cantos arredondados; aplicado no `body`). Escala do PixelPlayer: displayLarge 48/56 bold (título de página), headlines 600, titleMedium 18, labelLarge 16, labelMedium 14. Hero largo: `--type-display-hero` 60/57 600 com `'wdth' 151, 'ROND' 0` e −0,02em (início, boas-vindas). JetBrains Mono continua em todo dado técnico. Roboto Flex fica como `--font-ui-legacy`.
- **Formas**: card de seção 24, card hero e painel de conteúdo 32, arte em lista 64dp squircle (22%), arte grande 28, mini player 24, barra flutuante 28.
- **Listas**: três estilos (`GroupedList variant`): segmented 24/8 gap 4 (configurações), separated 24 gap 8 (conteúdo: músicas, arquivos, downloads), compact 16/4 gap 2 (listas técnicas densas, Netrik v1).
- **Cabeçalhos**: telas raiz e de configuração usam `TopAppBar size="large"` (botão voltar circular 48dp, ações circulares tonais, título 48sp bold). Biblioteca: cabeçalho com degradê suave de primary 18% e título em primary. Início: `HeroHeader` + `ShapeCollage`.
- **Navegação**: abas de seção em `PillTabs` (52dp, caixa alta, indicador 4×28). Barra inferior flutuante (`NavigationBar variant="floating"`, inset 16, cantos 28) ou de largura total; o app pode deixar o usuário escolher.
- **Cor de conteúdo**: o chrome usa o seed do app; Tocando agora, mini player e letra usam um `AppTheme` aninhado com o seed da capa (`seedFromImage`). Fundo do player = `primaryContainer` da capa.
- **Mídia (Media3)**: `MiniPlayer` acima da navegação, `PlaybackControls` (pílulas largas, play que vira squircle ao tocar, grupo aleatório/repetir/curtir), `Slider variant="wavy"` para posição, tempos e formato em mono.
- **Destaques**: `SectionCard tone="primary"` com `KpiTile variant="hero"` para números grandes; `EmptyState variant="hero"` para telas vazias; `Tag` para rótulos.
- **Onboarding**: título grande centralizado, ilustração de formas Material + ícones, botão principal 56dp, `OnboardingFooter` com botão de 96dp que muda de forma a cada etapa.
- **Para apps técnicos (Netrik)**: use os mesmos componentes, mas mantenha listas `compact` onde há muitos dados, `SectionCard` comum (não hero) para cada assunto e um único hero por tela.

## CONTENT FUNDAMENTALS

- **Idiomas**: inglês (padrão, `values/`) e português do Brasil (`values-pt-rBR/`), mesmas chaves. Os mocks do design estão em pt-BR. Números e datas seguem o locale do app.
- **Tom**: direto, técnico e honesto. Frases curtas que dizem o que acontece ou o que fazer: "Informe um IP ou domínio e toque em Iniciar. As respostas aparecem aqui em tempo real."
- **Nunca inventar dados**. Se o Android não deixa obter algo, a interface diz isso: "MAC indisponível" / "MAC unavailable", "Hostname não resolvido", "Indisponível" / "Unavailable". Marcas de risco do Port Scanner avisam que não são vulnerabilidades confirmadas.
- **Privacidade explícita**: quando algo sai do aparelho, o texto avisa: "Consulta o serviço externo api.ipify.org, que fica sabendo do seu IP."
- **Pessoa**: fala com "você" de forma implícita, no imperativo ("Confira o endereço digitado…", "Conecte-se a uma rede Wi-Fi…"). O app não fala em primeira pessoa.
- **Caixa**: sentence case em títulos, botões e rótulos ("Detalhes da rede", "Copiar tudo", "Mostrar só portas abertas"). Rótulos de coluna em CAIXA ALTA pequena com tracking (`SEQ · TTL`). Nomes próprios de ferramentas mantêm a forma conhecida (Ping, Traceroute, Port Scanner, SSH, Wi-Fi).
- **Confirmações**: "<campo> copiado" ("IP local copiado", "Dados da rede copiados"), sem ponto final.
- **Erros**: título diz o que aconteceu, corpo diz o que checar, ação "Tentar novamente": "Host não encontrado" / "Confira o endereço digitado ou o DNS da rede e tente novamente."
- **Valores técnicos**: sempre em mono, com o sinal de menos tipográfico (−54 dBm), separadores " · " entre partes ("5 GHz · Canal 36 · 80 MHz").
- **Emoji**: não usa. Ícones são sempre Material Symbols.

## VISUAL FOUNDATIONS

- **Cor**: Material 3 a partir de um seed com SchemeTonalSpot (v3: variantes vibrant/expressive/content disponíveis para superfícies de conteúdo, e cor da capa no player). Superfícies quase neutras com leve tom do seed; `primary` só em ações, títulos de seção, indicadores e a rede conectada. Claro, escuro, preto puro (AMOLED: fundo `#000` e containers escurecidos em Oklab) e dynamic color opcional (Android 12+, ligado por padrão no app; o esquema do seed é o fallback). Status semântico com success/warning harmonizados ao seed; erro usa o `error` do M3. Status é sempre cor + ícone + rótulo.
- **Tipo**: Roboto Flex na interface (escala M3 padrão + estilos emphasized) e JetBrains Mono em todo dado técnico (`dataLarge 22/28 500`, `dataMedium 14/20 500`, `dataSmall 12/16 400`, `terminal 13/18`), com algarismos tabulares. App bar small em `titleLarge 22/28`, média em `headlineMedium` emphasized; cabeçalhos de seção em `titleSmall` na cor primary.
- **Espaço**: base 4; gutter de 16dp nas telas; 24dp entre seções; 2dp entre itens de lista agrupada; 8dp entre cabeçalho e lista; alvos de toque ≥ 48dp (chips menores estendem a área de toque).
- **Forma**: escala M3E (ver acima). 4 (snackbar, campo outlined), 6 (chip pequeno, fora da escala, mantido do app), 8 (chip, tile de valor, KPI), 12 (cards de erro/aviso, painel avançado), 16 (card de seção, pontas da lista agrupada, FAB), 4 entre itens agrupados, 20 (FAB médio), 28 (card hero, bottom sheet, diálogos, FAB grande), pílula com morph em botões e grupos; formas Material em avatares de destaque.
- **Cards**: sem sombra e sem borda. Profundidade é o tom do container (`surfaceContainerLow/…/Highest`); dentro de um card, os valores ficam em blocos `surface` (mais claros no tema claro, mais escuros no escuro). Exceções: a rede Wi-Fi conectada tem borda 2dp primary; painéis de opções têm borda 1dp outlineVariant.
- **Elevação**: tonal para tudo no fluxo. Sombras M3 só no que flutua: FAB (level 3, level 4 no hover), floating toolbar (level 1), snackbar (level 3). Bottom sheet e diálogo usam scrim 32%.
- **Fundos**: cor sólida da superfície. Sem imagens, gradientes, texturas ou ilustrações. O terminal SSH usa uma paleta escura fixa própria.
- **Estados**: state layers M3 (hover 8%, foco 10% + anel, pressionado 10%) em botões, botões de ícone, grupos e nav; botões também mudam de forma ao pressionar. Linhas de lista e tiles trocam o container para `surfaceContainerHighest` ao pressionar (como no app). Desabilitado = 38% conteúdo / 12% container. Selecionado = `secondaryContainer`/`secondary` (chips, grupos), indicador pílula + ícone preenchido (nav), troca de forma (toggles).
- **Movimento**: molas M3E (`tokens/motion.css`). Espacial com leve overshoot (indicador da nav, morph de botões, thumb do switch, entrada do sheet e do FAB); efeitos sem overshoot (cores, opacidade, state layers). Loading indicator gira 360° a cada 4,67s e troca de forma a cada 650ms; onda do progresso se desloca continuamente. Movimento reduzido: sem deslocamento espacial.
- **Transparência e blur**: não usa blur. Transparência só em scrim (32%), state layers e na área do gráfico de latência (primary 12%).
- **Layout fixo**: status bar 40dp, top app bar 64dp (média: 64 + bloco de título), navigation bar 64dp + 24dp de gesto, edge-to-edge. Snackbar e FAB flutuam 16dp acima da nav bar.
- **Layout em 360–412dp**: nada quebra no meio de um valor. Valores têm `nowrap` e reticências (copiam completos); linhas de atributos usam FlowRow de colunas inteiras (Sinal/Banda/Canal/Largura); mais de 3 KPIs vão em grade 2×2; colunas com largura do conteúdo; grades com `minmax(0,1fr)`. Testar em 360 e 412dp nos dois idiomas.
- **Gráficos**: linha primary 2dp com área 12%, grade tracejada outlineVariant, limite em warning tracejado, timeouts como traços error.

## ICONOGRAPHY

- **Sistema**: Material Symbols **Rounded**, peso 400, opsz 24. Contorno (FILL 0) por padrão; preenchido (FILL 1) só no item selecionado da navigation bar e em ícones de status (`check_circle`, `warning`, `error`, `do_not_disturb_on`) e alguns avatares de estado (Wi-Fi conectado).
- **No app**: cada símbolo vira um vector drawable em `res/drawable` (`ic_<nome>` e `ic_<nome>_filled`), gerado por `scripts/material_symbol.py`. São ~130 arquivos XML de Android, não copiados para cá (não rodam na web).
- **Aqui**: a mesma família vem da fonte variável do Google Fonts (`tokens/icons.css`), via o componente `Icon`. É o mesmo desenho, não uma substituição.
- **Tamanhos**: 24 padrão; 20 em botões XS/S, 24 em M, 32 em L, 40 em XL; 18 em chips de 32dp; 14 em chips de 24dp; 16 em ícones de copiar dentro de tiles; 28/32/36 em tiles de estado e FABs maiores.
- **Ícones mais usados**: `home_repair_service`, `lan`, `wifi`, `terminal`, `network_ping`, `route`, `radar`, `manage_search`, `content_copy`, `copy_all`, `history`, `tune`, `play_arrow`, `stop`, `refresh`, `sort`, `search`, `more_vert`, `arrow_outward` (atalho para aba), `chevron_right` (abre tela), `open_in_new` (abre navegador).
- **Emoji / unicode**: não usa emoji. O único caractere especial recorrente é o sinal de menos tipográfico (−) e o separador " · ".
- **Logo**: o ícone aprovado é o 2b "Ping" (um nó emitindo duas ondas), em `assets/netrik/` (composição, foreground, monocromático; canvas 108dp, zona segura 66dp, fundo `#0A3A44`). Não há arquivo de wordmark: no design, o nome "Netrik" aparece em Roboto Flex 600, 28px, tracking −0,02em, ao lado do ícone. Não existe marca do "Henrique Sebastião"; onde precisar, use o nome em texto.

## Lacunas

- **Dynamic color** não pode ser simulado na web (depende do papel de parede); `AppTheme dynamicColor` só registra a intenção.
- **M3 Expressive x app atual**: o app Netrik ainda usa `MaterialTheme` (não `MaterialExpressiveTheme`), nav bar de 80dp, segmented buttons e botões de 48dp. A v2 é o alvo; o mapeamento de migração está em `compose-mapping.md`.
- **Formas Material**: `MaterialShape` e `LoadingIndicator` usam aproximações paramétricas das formas do Material Shape Library (mesmos nomes, geometria próxima, não idêntica). No app use `MaterialShapes.*` e `LoadingIndicator()`.
- **Molas na web**: transições CSS reiniciam com velocidade zero quando interrompidas; uma mola real mantém a velocidade.
- **Medidas a conferir no site M3**: larguras narrow/wide dos botões de ícone, alturas da app bar média com subtítulo e paddings do FAB estendido médio/grande vieram de fontes secundárias (tokens Compose citados em implementações abertas), pois m3.material.io não abre sem JavaScript.
- **Cores M3E**: mantido `SchemeTonalSpot` (pedido do projeto). O Compose oferece `expressiveLightColorScheme()`, que não foi adotado.
- **Fora do UI kit** (existem no design ou no código, mas não foram recriados): gráfico de espectro Wi-Fi, medidor de sinal e canais, formulário e terminal SSH, formulário de configuração do Port Scanner e scan de rede, ordenação da lista de Dispositivos (removida no código), diálogos de permissão do sistema.
- **Só no código, sem design**: Calculadora de sub-redes, Port Knocking, Descoberta de vizinhos, Bloqueio do app (PIN/digital), grupo "Utilitários" no hub. Não entraram no UI kit.
- **Textos em inglês**: o UI kit está em pt-BR; nem todas as strings em inglês foram trazidas (estão em `values/strings.xml`).
- **Commit**: a importação foi feita da `main` sem SHA de commit conhecido.
- **Licença da Google Sans Flex**: o arquivo veio do PixelPlayerOSS; confirme a licença (OFL) antes de distribuir em apps.
- **v3 não recriado**: equalizador (sliders verticais com thumb cookie, knobs em arco), fila, telas de álbum/artista, widget e barra de rolagem expressiva do PixelPlayer.
- **Downtfy**: não tive acesso ao app; o UI kit de mídia é a referência mais próxima até receber o repositório ou capturas.

## Substituições

- Nenhuma de fonte: Google Sans Flex vem do PixelPlayerOSS; Roboto Flex e JetBrains Mono do Netrik.
- Capas no UI kit de mídia: placeholders tonais com ícone (o mesmo fallback que o PixelPlayer usa sem arte). Troque por imagens reais via `Artwork src`.
- Ícones: fonte Material Symbols Rounded do Google Fonts no lugar dos vector drawables do app (mesma família).
- Formas Material e loading indicator: aproximações paramétricas (ver Lacunas).
