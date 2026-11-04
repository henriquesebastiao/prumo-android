# Design v1: inventário e cruzamento com a API

Análise da Etapa 1 sobre `design/v1/` (Fase 2, variante B) contra o contrato do servidor (`~/git/prumo/openapi/openapi.json`, `docs/api.md`, `docs/dominio.md`). Regras de negócio seguem os docs do servidor; aparência segue o design. Conflitos ficam listados aqui até serem decididos (a decisão vai para `docs/decisoes.md`).

## 1. Telas e fluxos

### Navegação

- **Primeiro acesso** (rodapé de etapas 1/3 → 3/3): Boas-vindas → Endereço do servidor (testa ao sair do campo; "Continuar" só com servidor encontrado) → Login (servidor numa `Tag` com "Trocar").
- **Celular**: o Início é a raiz e não há barra de navegação. Os módulos abrem pelos cards do Início e voltam pelo gesto. Configurações abre pela engrenagem no topo do Início. O ícone de nuvem ao lado indica sincronizado (`cloud_done`), sem conexão com pendentes (`cloud_off`) ou servidor inacessível (`cloud_alert`).
- **Toolbar flutuante por destino**: no Início, 4 ações fixadas e um FAB que abre "Todas as ações" (folha agrupada por módulo, na ordem dos módulos; "Editar" escolhe as 4 fixadas). Em cada módulo e detalhe, as ações do contexto, com a principal no FAB. Some sem nada a registrar (estado vazio).
- **Registros rápidos são folhas** sobre a tela atual (abastecimento, atualizar km, pagar fatura, atualizar progresso, nova anotação, categoria), não rotas. **Formulários grandes são telas cheias** que sobem no eixo Y (nova transação, nova conta, veículo, manutenção, troca de óleo, livro).
- **≥ 600 dp**: `NavigationSuiteScaffold` com rail de 96 dp (menu, FAB de todas as ações ou da ação do módulo, Início + módulos, Configurações no rodapé; com mais de 7 módulos o menu expande o rail). Transações em lista e detalhe (`ListDetailPaneScaffold`).
- **Widget** (Glance) 4×2 e 2×2: troca de óleo que vence primeiro + até 4 atalhos que abrem a folha de registro direto.

### Como os módulos aparecem

| Lugar | Comportamento |
|---|---|
| Início (celular) | Um card por módulo ativo, cada um no tema do seu seed. O primeiro (Finanças no exemplo) é o hero de largura total; os outros ficam em meia largura, em grade de 2. "Organizar módulos" leva a Configurações › Módulos |
| Início (tablet) | Os cards lado a lado, o primeiro mais largo |
| Todas as ações | Seções por módulo, na ordem dos módulos |
| Rail (tablet) | Início, módulos na ordem, Configurações no rodapé |
| Configurações › Módulos | Linha por módulo com alça de arraste e switch; desativar esconde no app e mantém os dados |

### Telas por área

| Área | Telas |
|---|---|
| Início e acesso | Início claro/escuro · Todas as ações · Boas-vindas · Servidor ok · Servidor com erro · Login |
| Configurações | Configurações · Conta · Aparência · Módulos · Sessões ativas · Servidor · Sair (diálogo) |
| Estados | Carregando · Erro · Sem conexão com pendentes · Servidor inacessível · Sessão expirada (diálogo) · Confirmar exclusão · Confirmar arquivamento · Snackbar com desfazer |
| Finanças | Resumo (claro, rolado, escuro) · Vazio · Contas · Nova conta · Transações · Nova transação (despesa, transferência) · Categorias · Criar/editar categoria (folha) · Cartões · Detalhe do cartão/fatura · Pagar fatura (folha). Abas: Resumo, Transações, Contas, Cartões |
| Veículos | Lista (claro, escuro) · Vazio · Cadastro/edição · Detalhe · Atualizar km (folha) · Detalhe de manutenção · Abastecimento (folha, claro e escuro) · Manutenção (oficina, peças) · Troca de óleo. Abas do histórico: Manutenções, Abastecimentos, Óleo, Km |
| Livros | Estante (claro, escuro com filtro) · Vazio · Cadastro · Detalhe · Atualizar progresso (folha) · Nova anotação (folha) |
| Telas grandes | Início no tablet · Transações lista e detalhe · Widget |

## 2. Componentes

### Do design system usados

`AppTheme` (aninhado por módulo), `TopAppBar` (large, small, tint, subtítulo, ações), `HeroHeader`, `SectionCard` (comum, hero, `tone="primary"`), `KpiTile` (com `tone`), `IconAvatar` (formas cookie9, sunny, flower), `SectionHeader`, `GroupedList` (segmented, separated), `GroupedListItem`, `SwitchRow` (e inline), `Switch`, `Button` (filled, tonal, outlined, text; XS, S, M; full width), `IconButton` (surface, tonal, filled, outlined; quadrado; r, s, m; wide), `ButtonGroup` connected, `FilterChip`, `StatusChip` (success, warning, error, neutral; small), `Tag`, `TextField` (padrão, compacto, erro, sufixo, desabilitado), `BottomSheet`, `Snackbar`, `FloatingToolbar` (standard e vibrant, com FAB), `PillTabs`, `LoadingIndicator` contained, `ProgressBar` (plana e ondulada), `Artwork`, `EmptyState` hero, `ErrorCard`, `NoConnectionCard`, `OnboardingFooter`.

Do design system e **não** usados: `NavigationBar`, `Fab` avulso, `ValueTile`, `CopyChip`, `Slider`, `Spinner`, `PlaceholderContent`, `PermissionRationale` e os componentes de mídia.

### Novos (fora do design system, só com tokens dele)

| Componente | Onde | Compose |
|---|---|---|
| Campo de valor | Abastecimento, despesa, transferência, saldo inicial, km, pagar fatura, progresso | `BasicTextField` + `VisualTransformation` + `KeyboardType.Decimal` |
| Diálogo de confirmação | Sair, sessão expirada, excluir, arquivar, revogar | `AlertDialog` (ação destrutiva em `error`) |
| Tema por módulo | Cards do Início, telas do módulo, avatares de conta, categoria e pendência | `ModuleTheme(seed)` aninhado |
| Navigation rail | ≥ 600 dp | `WideNavigationRail`, `ModalWideNavigationRail` |
| Seletor de período | Resumo do mês, faturas | `Row` + `AnimatedContent` |
| Grade de ações rotuladas | Todas as ações, categoria da transação | `LazyVerticalGrid` + `FilledTonalIconButton` |
| Seletor de cor | Conta, categoria | `Row(selectableGroup)` |
| Autocompletar | Oficina | `ExposedDropdownMenuBox` |
| Campo multilinha | Anotação, observações | `OutlinedTextField(minLines = 4)` |
| Linha reordenável | Configurações › Módulos | `animateItem()` + `semantics { customActions }` |
| Gráfico por categoria | Resumo do mês | `Canvas` ou `Row` com `weight` |
| Lista de peças dinâmica | Manutenção, troca de óleo | `SnapshotStateList` + `animateItem()` |
| Widget | Tela inicial | `GlanceAppWidget` |

## 3. Tokens

### Cor

- Esquema `SchemeTonalSpot` (contraste 0) gerado de um seed, nos modos claro, escuro e preto puro.
- Seeds: app (chrome) `#6A4F86`, Finanças `#2E6B4F`, Veículos `#1F5F8B`, Livros `#8A4B2E`.
- Cor dinâmica ligada por padrão no Android 12+; o seed do app é o fallback. Com ela ativa, cada seed de módulo é harmonizado com o `primary` do sistema.
- success `#3B8A3E` e warning `#B5651D` harmonizados com o seed (tons 40/100/90/30 no claro, 80/20/30/90 no escuro).
- Preto puro: background, surface, surfaceDim e containerLowest `#000`; containers escurecidos em Oklab (Low 0,6 · Container 0,5 · High 0,4 · Highest 0,3).
- Seletor de cor de conta e categoria: `#6A4F86 · #1F5F8B · #136B79 · #2E6B4F · #3B8A3E · #B5651D · #8F4C38 · #8A4B2E`.
- Os valores estáticos de `_ds/…/tokens/colors.css` são do seed do Netrik e não valem para o Prumo.

### Tipografia

- UI: Google Sans Flex arredondada (`ROND 100`). Escala v3: displayLarge 48/56 700, displayMedium 36/44 700, displaySmall 30/38 400, headline 32/40 · 28/36 · 24/32 600, titleLarge 22/28 400, titleMedium 18/24 500, titleSmall 14/20 500, body 16/24 · 14/20 · 12/16 400, labelLarge 16/20 500, labelMedium 14/16 500, labelSmall 11/16 500. Emphasized = mesmo tamanho, um passo de peso acima.
- Hero largo: 60/57 600, `wdth 151`, `ROND 0`, −0,02 em (boas-vindas); 44/42 na versão pequena.
- Dados: JetBrains Mono com algarismos tabulares: dataLarge 22/28 500, dataMedium 14/20 500, dataSmall 12/16 400.

### Forma, espaço, elevação

- Cantos M3E: 0 · 4 · 8 · 12 · 16 · 20 · 28 · 32 · 48 · full. Card de seção 24, card hero 32, listas segmented 24/8 (gap 4) e separated 24 (gap 8), folha e diálogo 28, campo 4, chip 8 (6 no pequeno), FAB 16/20/28. Botões quadrados e pressionados por tamanho (XS/S 12→8, M 16→12, L/XL 28→16); grupo conectado com cantos internos 4/8/8/16/20.
- Espaço: base 4, gutter 16, 24 entre seções, toque mínimo 48, app bar 64, toolbar flutuante 64, botões 32/40/56/96/136, item de lista ≥ 72, avatar 40.
- Elevação tonal; sombra só no que flutua: FAB nível 3, toolbar nível 1, snackbar nível 3, menus nível 2. Scrim 32 %. State layers 8/10/10/16 %; desabilitado 38 %/12 %.

### Movimento

`MotionScheme.expressive()`. Espaciais (com overshoot): rápida 0,6/800, padrão 0,8/380, lenta 0,8/200. Efeitos (sem overshoot): 1,0/3800, 1,0/1600, 1,0/800. Com animações desligadas, as espaciais viram `snap()`. Transições descritas na entrega: card do Início → módulo (container transform, cantos 32 → 0), volta preditiva, lista → detalhe (eixo X 1/12 + fade), ação → folha, ação → formulário (eixo Y 10 %), salvar (morph do botão, item novo destacado), excluir/desfazer, abas, mês ou fatura, progresso, confirmação expressiva (cookie9 → sunny, só ao terminar livro ou registrar troca de óleo), rolagem, carregamento, primeiro acesso, tablet, tema.

### Formatos

Locale pt-BR: `R$ 1.234,56 · 02/11/2026 · 14:30 · 12.345 km · 40,5 L`. Valores e km com teclado numérico e `VisualTransformation`. Números em mono ou com algarismos tabulares.

## 4. Ícones

Material Symbols Rounded, peso 400, opsz 24, contorno por padrão; preenchido só no item selecionado do rail e em ícones de status. No app, vetores gerados em `:core:designsystem` (como no Netrik e no Downtify). Usados nas pranchas (~80):

`account_balance account_balance_wallet add add_photo_alternate archive arrow_drop_down arrow_forward auto_stories brightness_auto build calculate category celebration check chevron_left chevron_right close cloud_alert cloud_done cloud_off compare_arrows computer contrast credit_card currency_exchange dark_mode delete devices directions_car dns done_all donut_small drag_indicator edit edit_note event flight format_color_fill history home image info inventory_2 library_add light_mode link local_gas_station lock lock_clock login logout menu menu_book more_vert movie music_note network_check notes oil_barrel palette park payments person photo_camera refresh remove_circle_outline restaurant savings schedule search settings smartphone speed sports_esports sticky_note_2 storefront subscriptions swap_horiz swap_vert trending_up tune two_wheeler visibility wallet widgets work`

Ícones de conta e categoria vêm do servidor como nomes Lucide (`landmark`, `utensils`, `house`…; `ACCOUNT_ICONS` e `DEFAULT_CATEGORIES` do backend, `frontend/src/ui/icons.ts` na web). O app precisa de uma tabela nome Lucide → Material Symbol (conflito C11).

## 5. Telas × API

Rotas abaixo de `/api/v1`, exceto `/api/health`.

### Sistema

| Tela | Endpoints | Campos |
|---|---|---|
| Servidor (primeiro acesso e Configurações) | `GET /api/health` | `status`, `database`, `version`; tempo de resposta medido no app |
| Login | `POST /auth/login` | `username`, `password`, `client: "app"`, `device_name` → `token`, `expires_at`, `user`; erros `auth.invalid_credentials`, `auth.rate_limited` (`Retry-After`) |
| Configurações | `GET /me`, `GET /modules`, `GET /auth/sessions` | `display_name`, `username`; quantidade e nomes dos ativos; quantidade de sessões |
| Conta | `GET, PATCH /me`, `PUT /me/password` | `display_name`; senha atual e nova (`auth.wrong_password`, `users.password_too_short`) |
| Aparência | `GET, PATCH /me/preferences` | `theme`. Preto puro e cor dinâmica são do aparelho (DataStore) |
| Módulos | `GET /modules`, `PATCH /me/preferences` | `id`, `enabled`, `position`; `enabled_modules` em ordem |
| Sessões ativas | `GET /auth/sessions`, `DELETE /auth/sessions/{id}` | `device_name`, `client`, `last_used_at`, `current` |
| Sair | `POST /auth/logout` | |
| Início | `GET /dashboard`, `GET /me/preferences` | `modules[]` (união por `module`): Finanças `total_balance_cents`, `expense_cents`, `next_invoice` (`card.name`, `invoice.amount_cents`, `closing_on`); Veículos `next_due` (`vehicle.name`, `status`, `primary`, `remaining_km`, `remaining_days`, `used_percent`, `due_on`), `vehicles[]`; Livros `current` (`book.title`, `current_page`, `total_pages`, `progress_percent`, `has_cover`); `timezone` para saudação e data |
| Todas as ações | local (`PrumoFeature`) | ações fixadas guardadas no aparelho |
| Sem conexão, servidor inacessível, sessão expirada | local + `401 auth.session_expired` | fila local, hora do cache |

### Finanças

| Tela | Endpoints | Campos |
|---|---|---|
| Resumo | `GET /finance/overview?month=` | `total_balance_cents`, `accounts[]` (`account`, `balance_cents`), `income_cents`, `expense_cents`, `expense_percent`, `result_cents`, `expenses_by_category[]` (`category`, `amount_cents`, `percent`), `open_invoices[]` |
| Contas | `GET /finance/accounts` | `items[]` (`name`, `kind`, `color`, `icon`, `balance_cents`, `include_in_total`), `total_balance_cents` |
| Nova conta | `POST /finance/accounts` | `name`, `kind`, `initial_balance_cents`, `color`, `icon`, `include_in_total` |
| Transações | `GET /finance/transactions` (`from`, `to`, `kind`, `account_id`, `credit_card_id`, `category_id`, `q`) | `items[]` (`description`, `kind`, `amount_cents`, `occurred_on`, `occurred_time`, `category`, `account`, `credit_card`), `income_cents`, `expense_cents` |
| Nova transação | `POST /finance/transactions`, `GET /finance/accounts`, `GET /finance/credit-cards`, `GET /finance/categories?kind=` | `kind`, `amount_cents`, `occurred_on`, `occurred_time`, `description`, `account_id` / `credit_card_id` / `destination_account_id`, `category_id` |
| Categorias e folha | `GET /finance/categories?kind=&month=`, `POST`, `PATCH`, `/archive` | `name`, `kind`, `color`, `icon`, `month_total_cents`, `archived_at` |
| Cartões | `GET /finance/credit-cards` | `name`, `limit_cents`, `used_cents`, `available_limit_cents`, `current_invoice` (`amount_cents`, `status`, `closing_on`, `due_on`) |
| Detalhe do cartão | `GET /finance/credit-cards/{id}`, `GET …/invoices`, `GET …/invoices/{closing_on}` | `closing_day`, `due_day`, fatura (`amount_cents`, `status`, `closing_on`, `due_on`, `transactions_count`, `transactions[]`) |
| Pagar fatura | `POST …/invoices/{closing_on}/payments` | `amount_cents`, `account_id`, `occurred_on`; `remaining_cents` |

### Veículos

| Tela | Endpoints | Campos |
|---|---|---|
| Lista | `GET /garage/vehicles` | `name`, `kind`, `brand`, `model`, `year`, `current_km`, `oil_change.status` |
| Cadastro e edição | `POST`, `PATCH /garage/vehicles/{id}`, `/archive` | `kind`, `name`, `brand`, `model`, `year`, `current_km`, `oil_interval_km`, `oil_interval_months` |
| Detalhe | `GET /garage/vehicles/{id}`, listas do histórico (`/maintenances`, `/fuelings`, `/odometer`) | `current_km`, `current_km_at`, `oil_change` (`status`, `primary`, `remaining_km`, `remaining_days`, `next_km`, `next_on`, `used_km_percent`, `used_days_percent`), `stats` (`avg_consumption_km_l`, `last_consumption_km_l`, `last_fueling`, `month_spent_cents`) |
| Atualizar km | `POST /garage/vehicles/{id}/odometer` | `km`; erro `garage.odometer_inconsistent` com `details` |
| Detalhe de manutenção | `GET …/maintenances/{id}`, `DELETE` | `performed_at`, `km`, `workshop`, `items[]`, `labor_cost_cents`, `total_cents`, `notes` |
| Abastecimento | `POST …/fuelings` | `fueled_at`, `km`, `fuel_type`, `volume_ml`, `total_cents`, `full_tank`; resposta com `price_per_liter` |
| Manutenção | `POST …/maintenances`, `GET /garage/workshops?q=` | `performed_at`, `km`, `workshop_id` / `workshop_name`, `items[]` (`description`, `quantity`, `unit_price_cents`), `labor_cost_cents`, `notes`; oficina `maintenances_count`, `last_maintenance_at` |
| Troca de óleo | `GET …/oil-change-suggestion?km=&on=`, `POST …/maintenances` (`kind: oil_change`) | `next_oil_change_km`, `next_oil_change_on`, `interval_km`, `interval_months`, `previous_oil_types` |

### Livros

| Tela | Endpoints | Campos |
|---|---|---|
| Estante | `GET /reading/books?status=&owned=` | `title`, `author`, `status`, `owned`, `current_page`, `total_pages`, `progress_percent`, `has_cover` |
| Capa | `GET, PUT /reading/books/{id}/cover` | `image/webp`; `multipart` campo `file` |
| Cadastro | `POST /reading/books` | `title`, `author`, `total_pages`, `status`, `owned`, `current_page`, `started_on`, `finished_on` |
| Detalhe | `GET /reading/books/{id}`, `GET …/notes` | `progress_percent`, `remaining_pages`, `started_on`, `finished_on`, notas (`content`, `page`, `noted_on`) |
| Atualizar progresso | `POST …/progress` | `page` (a resposta é o livro) |
| Nova anotação | `POST …/notes` | `content`, `page` |

### Telas grandes e widget

Mesmos endpoints das telas de celular. Detalhe da transação no tablet: `GET /finance/transactions/{id}`. Widget: `GET /dashboard` (`garage.next_due`).

## 6. O que o design pede e a API não oferece

| # | Item | Situação |
|---|---|---|
| A1 | Estados offline e de sincronização (pendentes, "Tudo sincronizado", hora do cache) | Base pronta no servidor: criação idempotente (D40), `updated_since` e `/sync/deletions` (D41). O resto é do app (Room, WorkManager) |
| A2 | Edição offline de registro existente sem sobrescrever mudança feita em outro cliente | Não há controle de concorrência (`If-Match`/versão); hoje vale a última escrita. Decidir na fase offline |
| A3 | IP na lista de sessões ("192.168.0.12") | `SessionRead` não tem IP |
| A4 | "Encerrar as outras sessões" | Não há rota em lote; dá para revogar uma a uma |
| A5 | "Este endereço não é um servidor Prumo" | `/api/health` não identifica o produto; dá para inferir pelo formato (`status`, `database`, `version`) |
| A6 | Total do dia no cabeçalho de cada dia em Transações | A lista só traz `income_cents`/`expense_cents` do filtro inteiro; somar a página no app é cálculo no cliente e fica errado com paginação |
| A7 | Percentual por categoria com uma casa ("77,5 %") | `CategoryShare.percent` é inteiro |
| A8 | "Criada por: App Android" no detalhe da transação | Não há campo de origem do registro |
| A9 | "Conta mais usada pré-selecionada" | Não há dado de uso; dá para lembrar a última usada no aparelho |
| A10 | "Fatura: Novembro · aberta" no detalhe da transação | A transação traz `invoice_closing_on`, não o status da fatura |
| A11 | Preço por litro antes de salvar e "digite dois e o terceiro é calculado" | Só existe depois de salvar (`price_per_liter`); ver C1 |
| A12 | Ações fixadas na toolbar, preto puro, cor dinâmica | Preferências do aparelho (DataStore); não precisam de API |

## 7. O que a API oferece e o design não mostra

- **Sistema**: "Vem aí" e "Atividade recente" do `GET /dashboard` (D28); exportação dos dados (`GET /me/export`); fuso e moeda nas preferências; login bloqueado (`429` com `Retry-After`).
- **Finanças**: formulário de cartão (novo e editar) e de receita; compra parcelada (`installments`, exclusão `?installments=all`); detalhe, edição e exclusão de transação no celular; detalhe e edição de conta; ajuste de saldo (F11); excluir categoria, conta ou cartão sem uso; listas de arquivados (contas, cartões; categorias só mostra a entrada "Arquivadas"); série de 6 meses e receitas por categoria na visão geral (F12); conta fora do total (`include_in_total`) só citada em texto.
- **Veículos**: combustível habitual (`fuel_type`) e observações no cadastro; óleo utilizado (`oil_type`) e sugestões (`previous_oil_types`); ritmo de uso e data estimada (`pace_km_per_day`, `estimated_km_on`); km do mês e histórico de consumo (`recent_consumption`); detalhe e edição de abastecimento; exclusão de leitura manual; listas entre veículos (`/garage/maintenances`, `/garage/fuelings`); gestão de oficinas; arquivados; resumo do mês (`/garage/overview`); veículo sem intervalo de óleo (`oil_change: null`).
- **Livros**: ritmo e previsão de término (R7, R8); páginas por semana (R9, `BookDetail`); todas as anotações com busca (`GET /reading/notes`); editar e excluir anotação e livro; remover capa; busca por título e autor (o ícone existe, a tela não); contagens da estante (`/reading/overview`).

## 8. Conflitos entre o design e `docs/dominio.md`

Listados aqui; a decisão de cada um fica em `docs/decisoes.md`, na fase do módulo. Resolvidos até agora: C1–C5 e C9 (D15, Veículos).

| # | Design | Regra | Conflito |
|---|---|---|---|
| C1 | Abastecimento: "digite dois e o terceiro é calculado" e "R$ 6,29 por litro · calculado" antes de salvar | G7 e D02: preço por litro é calculado pelo backend | O app calcularia preço, litros ou valor |
| C2 | Manutenção: peça com nome e valor, sem quantidade | `MaintenanceItem.quantity` (≥ 1, padrão 1; D27) | A quantidade some da interface |
| C3 | Manutenção e troca de óleo: "Total calculado" ao vivo no formulário | G4: total calculado pelo backend | Soma no cliente antes de salvar |
| C4 | Troca de óleo: o óleo entra como peça ("Óleo 10W-30 semissintético 1 L") | `oil_type` próprio com sugestões (G14, D27) | Falta o campo `oil_type`; as sugestões ficam sem uso |
| C5 | Atualizar km: erro "Menor que o último registro" no campo e "+92 km desde a última atualização" | G3: consistência cronológica é do backend | Validação e diferença no cliente (equivalente a G3 só porque a leitura é "agora") |
| C6 | Atualizar progresso: "56 % · +24 páginas hoje" enquanto digita | R5: `progress_percent` é do backend | Percentual e avanço calculados no cliente como prévia |
| C7 | Pagar fatura: "Restam R$ 564,22" com o valor digitado | F5: fatura calculada no backend (`remaining_cents`) | Subtração no cliente como prévia |
| C8 | Transações: total do dia no cabeçalho | D02, D34 | Ver A6 |
| C9 | Abastecimento: "veículo e combustível vêm do último uso" | `fuel_type` habitual do veículo é o padrão (o backend usa quando o campo falta) | Último uso × combustível habitual |
| C10 | Seletor de cor com 8 seeds hex | `color` é uma de 11 chaves (`green`…`gray`; D33) | 8 cores × 11 chaves; o app precisa de uma tabela chave → seed e aceitar as 11 |
| C11 | Ícones de conta e categoria como Material Symbols (`account_balance`, `celebration`…) | `icon` é um nome livre que a web lê como Lucide (`landmark`, `ticket`…; D33) | Gravar nomes Material faria a web mostrar ícone genérico e vice-versa |
| C12 | Formatadores com locale pt-BR fixo e "R$" | F9: moeda é preferência do usuário (`currency`, padrão BRL) | Símbolo fixo × moeda da preferência |
| C13 | "Hoje", saudação e data do Início | S3: "hoje" no fuso da preferência | Usar `timezone` da preferência, não o do aparelho |
| C14 | Sessão expirada: pendentes "são enviados depois do login" | S2: usuários não compartilham dados | Se outra pessoa entrar, os pendentes não podem ir para a conta dela |
| C15 | Categorias: menu só com Editar e Arquivar | F8 e D32: categoria sem uso pode ser excluída | Exclusão ausente (não contradiz, só omite) |
| C16 | Início: "Finanças no card hero" | D20: ordem é do usuário | Hero fixo em Finanças ou o primeiro da ordem? |
