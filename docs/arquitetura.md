# Arquitetura

O app é um cliente da API do Prumo (`~/git/prumo`), equivalente à web: mesmas funcionalidades, mesmos números, nenhuma regra de negócio. Tudo o que é derivado (saldos, faturas, status do óleo, consumo, progresso de leitura) vem pronto da API.

## Visão geral

```
 App Android ──HTTP(S)──► servidor do usuário (nginx) ──/api──► API FastAPI ──► PostgreSQL
               bearer                                    (mesma API da web)
```

- O endereço do servidor é escolhido pelo usuário no primeiro acesso (`http://192.168.0.10:8000`, `https://prumo.exemplo.com`) e pode ser trocado em Configurações › Servidor.
- O app só fala com esse servidor e só manda o token para ele.

## Módulos Gradle

```
:app                  Activity, Application (Hilt), shell, telas de sistema
                      (primeiro acesso, Início, Configurações), montagem dos PrumoFeature
:core:model           JVM puro: Outcome/AppError, códigos de erro, dinheiro, km, litros,
                      datas e endereço do servidor; modelos do sistema
:core:network         JVM puro: OkHttp + Retrofit + kotlinx.serialization, DTOs do sistema,
                      AuthInterceptor, ApiFactory (endereço dinâmico), ServerProbe (health),
                      apiCall (erros pelo code), o resumo do Início como JsonObject
:core:data            sessão (Tink + DataStore), preferências do aparelho, repositórios do
                      sistema (servidor, conta, preferências, módulos, sessões, Início), a
                      cópia das respostas do servidor (ResponseStore) e o estado do servidor
:core:designsystem    PrumoTheme, ModuleTheme, tokens, ícones, componentes, @PhonePreviews
:core:ui              estados (carregando, vazio, erro, sem conexão, servidor inacessível,
                      sessão expirada), textos de erro por code, formatadores Compose
:core:feature         contrato PrumoFeature
:core:testing         fakes dos repositórios e regras de teste
:core:database        Room (prumo.db): as respostas do servidor guardadas no aparelho (D21)
:feature:garage       cada módulo com sua API (Retrofit), DTOs, modelos, repositório e
:feature:finance      telas (data/, model/, ui/). Veículos (Fase 1), Finanças (Fase 2),
:feature:reading      Livros (Fase 3), Estudos (Fase 6) e Hábitos (Fase 7, D24) estão
:feature:studies      prontos
:feature:habits
```

Plugins de convenção (`build-logic/convention`):

| Plugin | Para |
|---|---|
| `prumo.jvm.library` | `:core:model`, `:core:network` (Kotlin JVM, bytecode 17, JUnit) |
| `prumo.android.library` | módulos Android (compileSdk/targetSdk 37, minSdk 26, JUnit, Turbine) |
| `prumo.android.compose` | Compose (BOM, compilador, tooling dos previews) |
| `prumo.android.feature` | `:feature:*`: biblioteca Android + Compose + Hilt + as dependências do contrato, e a regra que impede depender de outro `:feature:*` |
| `prumo.android.application` | `:app` |
| `prumo.hilt` | Hilt com KSP (em módulo JVM, `hilt-core`; em Android, `hilt-android` e o plugin do Hilt) |

### Regras de dependência

- `:app` → tudo.
- `:feature:X` → `:core:feature`, `:core:designsystem`, `:core:ui`, `:core:data`, `:core:network`, `:core:model`. Nunca outro `:feature:*` (o `prumo.android.feature` falha o build, como o import-linter no servidor).
- `:core:data` → `:core:network`, `:core:model`, `:core:database`.
- `:core:network` → `:core:model`. `:core:ui` → `:core:designsystem`, `:core:model`. `:core:feature` → `:core:designsystem`, `:core:model`.
- `:core:model` e `:core:network` não importam nada do Android.
- Repositórios expõem `Flow`/`StateFlow` desde já, para o Room entrar na Fase 4 sem mudar as telas.

## Dependências

Versões só em `gradle/libs.versions.toml`, conferidas em 2026-11-04 (Google Maven pelas notas de versão do AndroidX, Maven Central e Gradle Plugin Portal). Confira as estáveis atuais antes de mudar uma.

| Grupo | Bibliotecas |
|---|---|
| Build | Gradle 9.8.2 · AGP 9.5.0 (Kotlin embutido) · Kotlin 2.4.21 · KSP 2.3.13 · compileSdk e targetSdk 37 · minSdk 26 · bytecode 17 |
| Interface | Compose BOM 2026.09.00 · material3 e navigation-suite 1.5.0-beta02 (D03) · material3-adaptive 1.3.0 · Navigation 3 1.2.0 · lifecycle 2.11.0 (com `lifecycle-viewmodel-navigation3`) · activity-compose 1.13.0 · core-ktx 1.19.1 · core-splashscreen 1.2.0 · material-color-utilities (`com.materialkolor`) 5.0.1 |
| Injeção e dados | Hilt 2.60.1 · hilt-lifecycle-viewmodel-compose 1.4.0 · OkHttp 5.5.1 · Retrofit 3.0.0 + converter kotlinx-serialization · kotlinx-serialization-json 1.12.0 · coroutines 1.11.0 · DataStore 1.3.0 · Tink 1.24.0 · Coil 3.6.3 (`coil-compose` e `coil-network-okhttp`, as capas de Livros) · Paging 3.5.3 (Finanças) · Room 2.8.5 (a cópia das respostas, conferido em 2026-11-09) |
| Previstas por fase | WorkManager 2.12.0 (fila de pendentes) · Glance 1.2.0 (widget) · material3-adaptive-navigation3 1.3.0 (telas grandes) |
| Qualidade | detekt 1.23.8 (a 2.0 ainda é alpha) + compose-rules 0.6.8 · Spotless 8.11.0 + ktlint 1.8.0 · JUnit 4.13.2 · Turbine 1.2.1 · Robolectric 4.17 · MockK 1.14.11 (só onde um fake for difícil) · mockwebserver3 5.5.1 |

Sem biblioteca para UUID v7 (`kotlin.uuid.Uuid.generateV7()`, da stdlib) nem para datas (`java.time`). Ícones: Material Symbols Rounded convertidos em vetores (`scripts/material_symbol.py`), sem a fonte de ícones.

## Ambiente

- Gradle roda com JDK 17 ou 21. Na máquina do mantenedor, `JAVA_HOME=/usr/lib/jvm/java-17-openjdk` (o padrão do sistema é o 27, novo demais para o Gradle 9.8).
- Android SDK em `~/Android/Sdk` (`sdk.dir` no `local.properties`, fora do git), com `platforms;android-37`, `build-tools;37.0.0` e `platform-tools`. Emulador `Pixel_9` com API 37 (imagem com Play Store). Sem janela, ele precisa de `-skip-adb-auth`, ou o adb fica `unauthorized` sem diálogo para aceitar: `~/Android/Sdk/emulator/emulator -avd Pixel_9 -no-window -no-audio -no-snapshot-save -gpu swiftshader_indirect -skip-adb-auth`.
- `ksp.incremental=false` (D17): o KSP incremental chegou a deixar o Hilt sem um `PrumoFeature`.
- O CI (GitHub Actions) roda `./gradlew spotlessCheck detekt` e `./gradlew build` em cada pull request para a `main`.

## Camadas

```
XRoute ──► XViewModel ──► Repository (:core:data) ──► PrumoApi (:core:network)
  │            │ StateFlow<XUiState>        │ Outcome<T>          │ DTO
XScreen ◄──────┘                            └── modelo (:core:model) ◄── map
```

- **Tela**: `XRoute` pega o ViewModel (`hiltViewModel()`) e coleta o estado; `XScreen(state, callbacks, modifier)` é sem estado, e é o que previews e testes usam.
- **ViewModel**: um `StateFlow<XUiState>`; eventos são funções públicas. Texto digitado em estado de snapshot.
- **Repositório**: chama a API por `apiCall { }`, converte DTO em modelo e devolve `Outcome<T>`; dados que a tela observa ficam num `StateFlow`.
- **Rede**: DTOs `@Serializable` em snake_case (`@SerialName`), Retrofit com `suspend`.
- **Módulo** (`:feature:<id>`): `data/` tem a interface Retrofit do módulo (`GarageApi`, caminhos relativos `api/v1/garage/…`), os DTOs e o repositório, que chama a API por `ServerCalls.module(GarageApi::class.java) { … }` (sem sessão, falha como `SessionExpired` sem sair do aparelho); `model/` tem os modelos que as telas usam, e `ui/` as telas. DTOs não saem de `data/`.
- **Destino com argumento**: a chave do Navigation 3 leva os ids (`VehicleDetail(vehicleId)`), e o ViewModel os recebe por injeção assistida (`@HiltViewModel(assistedFactory = …)` e `hiltViewModel<VM, Factory> { it.create(id) }`), testável pelo construtor.
- **Listas longas** (as transações de Finanças): Paging 3 sobre a paginação por `offset` da API (`TransactionPagingSource`); a primeira página também traz os totais do filtro inteiro, que a tela mostra no topo. Cabeçalhos de dia entram com `insertSeparators`. Listas pequenas (contas, cartões, categorias) vêm inteiras.
- **Compartilhado entre módulos**: enums do fio (`Wire`, `wireOf`) ficam em `:core:model`; o JSON dos corpos com nulos explícitos (`RequestJson`, `requestBody`), em `:core:network`; o menu ⋮ (`OverflowMenu`), no design system. Um módulo nunca importa outro.
- **Imagens autenticadas** (as capas de Livros, R6 do servidor): o Coil busca pelo `OkHttpClient` da sessão (`@ApiClient`, que põe o bearer só no servidor da sessão), num `ImageLoader` do módulo (`Covers`, singleton) entregue às telas por `LocalCovers`; o endereço leva `?v=<cover_updated_at>`, então a cache troca quando a capa muda. Previews e testes ficam sem `Covers` e desenham a capa de título e autor. Fotos da câmera passam por um `FileProvider` do próprio módulo (`CoverFileProvider`, autoridade `<applicationId>.covers`, caminhos no manifest do módulo).
- **Depois de gravar**: o repositório do módulo avisa `DataChanges` (`:core:data`) a cada gravação que deu certo. As telas abertas do módulo recarregam (o repositório expõe `changes`) e o Início recarrega os resumos, então um abastecimento salvo na folha sobre o Início já muda o card.
- **Cópia no aparelho** (D21): as leituras passam pelo `CachingInterceptor` (`:core:network`), que guarda a resposta de cada `GET` no Room (`ResponseStore` em `:core:data`, tabela `responses` de `:core:database`) e responde dela. Telas e repositórios não sabem de onde a resposta veio. Pull-to-refresh chama `expireCache()` do repositório do módulo (`ServerCalls.expireCache`) antes de recarregar.

## Rede

- `NetworkJson`: ignora campos desconhecidos, aceita nulos implícitos e enums desconhecidos (`coerceInputValues`). A v1 só cresce, e o app não quebra quando o servidor ganha campos.
- Corpos de requisição dos módulos vão como `JsonObject` escrito com nulos explícitos (`RequestJson`, `:core:network`): no `PATCH`, campo ausente não muda e `null` limpa (`docs/api.md` do servidor), e o `NetworkJson` omitiria o `null`. Enums viajam como texto; valor desconhecido vira um padrão do modelo (`wireOf`).
- O cliente da API (`@ApiClient`) tem dois interceptores, nesta ordem: `CachingInterceptor` (a cópia no aparelho) e `AuthInterceptor` (o bearer). Uma resposta do aparelho não leva token; a conferência em segundo plano passa pelos dois.
- `ApiFactory.create(baseUrl)` monta (e guarda) um `PrumoApi` para o endereço da sessão. O endereço é dinâmico: trocar de servidor é encerrar a sessão e entrar no outro.
- `AuthInterceptor`: acrescenta `Authorization: Bearer <token>` só a requisições para o servidor da sessão (mesmo esquema, host, porta e caminho base). Um `401` a uma requisição que levou o token (fora de `/auth/login`) avisa a sessão (`SessionGateway.onUnauthorized`), que é encerrada com o motivo "expirada".
- `apiCall { }` converte o resultado:

| Falha | `AppError` |
|---|---|
| Resposta com `{"error": {…}}` | `Api(status, code, message, details, fields, retryAfterSeconds)` |
| `401` com `auth.required` ou `auth.session_expired` | `SessionExpired` |
| Sem conexão, DNS, recusa, tempo esgotado, TLS | `Unreachable(cause)`: `UnknownHost`, `Refused`, `Timeout`, `Certificate`, `Other` (cada causa tem o seu texto do que conferir) |
| Corpo que não é JSON do Prumo, `5xx` sem `error` | `Unexpected` |

- Os textos são do app: `errorText(error)` em `:core:ui` traduz cada `code` conhecido (lista em `ErrorCodes`) e cai num texto genérico pelo status quando não conhece o `code`. `auth.rate_limited` usa o `Retry-After`.
- `ServerProbe.check(input)`: transforma o que o usuário digitou em endereços candidatos (`ServerAddress`: sem esquema, tenta `https://` e depois `http://`; IP ou `.local` tenta `http://` primeiro), chama `GET /api/health` em cada um com um cliente sem token e mede o tempo de resposta. Resultado (`ServerCheck`):

| Resposta | Resultado |
|---|---|
| `200` com `service: "prumo"` (ou sem `service`, com `status`, `database`, `version`: servidores anteriores à D44 do servidor) | `Found(baseUrl, version, latência)` |
| `503` com o formato do health | `DatabaseDown(baseUrl, version)` |
| Outra resposta (HTML, outro JSON, `404`) | `NotPrumo` |
| Sem resposta | `Unreachable(baseUrl, cause)` |
| Texto que não forma um endereço | `InvalidAddress` |

## Cópia no aparelho (Fase 4, D21)

```
tela ─► repositório ─► Retrofit ─► CachingInterceptor ─► AuthInterceptor ─► servidor
                                        │  ▲
                              ResponseStore (Room, por conta: usuário@endereço)
```

| Situação do `GET` | O que acontece |
|---|---|
| Sem cópia | Pergunta ao servidor e guarda a resposta `200` em JSON |
| Cópia de menos de 30 s | Responde da cópia, sem rede |
| Cópia mais velha | Responde da cópia e confere em segundo plano; resposta diferente avisa `DataChanges` (módulo do caminho) e as telas recarregam |
| Cópia velha (houve gravação ou pull-to-refresh) | Pergunta ao servidor; sem resposta (sem conexão, recusa, tempo esgotado, `502`–`504`), usa a cópia |
| `404` | Apaga a cópia daquele caminho |
| `/auth/`, `/me/export`, `/sync/`, imagens | Nunca guardados |

- Toda requisição ao servidor da sessão diz ao `ServerStatusRepository` se ele respondeu; o Início mostra "Servidor inacessível" com a hora da cópia do resumo (`X-Prumo-Cached-At` marca as respostas do aparelho; o Início lê a hora no `ResponseStore`).
- Entrar com outra conta apaga as respostas da anterior; Sair e Trocar servidor apagam tudo; sessão expirada mantém.
- O Room não roda na thread principal: o interceptor lê e grava nas threads do OkHttp; `expire()` (chamado pelo pull-to-refresh, na thread principal) marca a hora em memória, e as respostas mais antigas contam como velhas na hora, enquanto a tabela é atualizada em segundo plano; o Início lê a hora da cópia no dispatcher de I/O.
- O esquema do banco é exportado em `core/database/schemas/` para as migrações dos próximos itens (fila de pendentes).

## Sessão

- Login: `POST /api/v1/auth/login` com `client: "app"` e `device_name` (o nome do aparelho em Configurações do Android, `Settings.Global.DEVICE_NAME`, ou o modelo). A resposta traz `token` e `expires_at`.
- O token é cifrado com AES-256-GCM (Tink) sob uma chave do Android Keystore e guardado no DataStore com o endereço do servidor e o usuário (`SessionStore`). Nada da sessão vai para o backup (`data_extraction_rules.xml`): a chave do Keystore não sai do aparelho.
- `SessionRepository.state: StateFlow<SessionState>`: `Loading` (lendo o DataStore, a splash fica na tela), `SignedOut(reason)` ou `SignedIn(session)`. O `AuthInterceptor` lê o token desse estado em memória.
- Sair: `POST /auth/logout` (o erro de rede não impede sair) e limpeza local, inclusive das respostas guardadas. Sessão expirada (401): limpeza do token e diálogo; o login seguinte volta ao mesmo servidor, e as respostas guardadas continuam se o usuário for o mesmo (D21).
- Exportar dados (`DataExportRepository`): `GET /me/export` (`@Streaming`) baixa o `.zip` pela sessão para o cache e o copia para o documento que o usuário criou no seletor do Android (`CreateDocument`); numa falha, o documento é apagado (D20).
- Chave do Keystore perdida ou trocada (backup restaurado em outro aparelho): o token não decifra, a sessão é descartada e o app volta ao login.

## Módulos do Prumo no app

O servidor tem a lista de módulos instalados e a preferência de cada usuário (`GET /api/v1/modules`: `id`, `enabled`, `position`). O app tem os `PrumoFeature` que conhece (um por `:feature:*`, registrados no Hilt com `@IntoSet`).

```kotlin
interface PrumoFeature {
    val id: String                       // "garage": o mesmo id do servidor
    val nameRes: Int                     // "Veículos"
    val icon: Int                        // Material Symbol
    val seed: Long                       // seed do ModuleTheme (ARGB)
    val entryKey: NavKey                 // destino inicial do módulo
    val quickActions: List<QuickAction>  // "Todas as ações" e a toolbar do Início; com sheet, abrem sobre ele
    fun EntryProviderScope<NavKey>.entries(navigator: Navigator)
    @Composable fun HomeCard(summary: JsonObject?, hero: Boolean, context: HomeContext, onOpen: () -> Unit, modifier: Modifier)
    @Composable fun upcoming(item: UpcomingItem, context: HomeContext): HomeEvent?  // "Vem aí" (padrão: null)
    @Composable fun activity(item: ActivityItem, context: HomeContext): HomeEvent?  // "Atividade recente"
}
```

- **Montagem** (`ModuleRegistry` no `:app`): a ordem e os ativos vêm do servidor; o app mostra a interseção. Módulo que o servidor tem e o app não conhece é ignorado; módulo que o app conhece e o servidor não tem (ou que o usuário desativou) fica oculto.
- **Início**: `GET /api/v1/dashboard` devolve um resumo por módulo ativo, na ordem do usuário, como união discriminada por `module`. `:core:network` entrega cada resumo como `JsonObject`; o `PrumoFeature` do módulo decodifica o seu. O primeiro módulo da ordem é o card hero (D08). Os itens `upcoming` e `activity` chegam como `UpcomingItem`/`ActivityItem` (`kind` estável e `data` com valores simples, lidos por `text`, `long` e `double`); o módulo de cada um escreve a linha (`HomeEvent`) e o Início mostra até 4 de cada lista (D20).
- **Navegação**: cada módulo registra as suas entradas no `NavDisplay` do shell, cada uma dentro do `ModuleTheme` do módulo.
- **Ações rápidas**: uma `QuickAction` com `sheet` abre a folha do módulo sobre o Início (abastecimento, km); sem `sheet`, abre `target` (manutenção e troca de óleo abrem o formulário). A folha cria o seu ViewModel no destino que a mostra e recomeça vazia a cada abertura (um id de abertura guardado com `rememberSaveable`, que sobrevive à rotação).

## Navegação

- Navigation 3: a pilha (`NavBackStack<NavKey>`) é do `:app`; `NavDisplay` com os decoradores de estado salvo e de `ViewModelStore` por entrada.
- Primeiro acesso: Boas-vindas → Servidor → Login, numa pilha própria. Ao entrar, a pilha vira `[Início]`.
- Celular: o Início é a raiz, sem barra de navegação. Módulos abrem pelos cards; Configurações, pela engrenagem. A toolbar flutuante muda por destino.
- Folhas de registro rápido (`ModalBottomSheet`) não são destinos.
- Telas grandes (Fase 5): `NavigationSuiteScaffold` com rail a partir de 600 dp e lista e detalhe.

## Tema

- `PrumoTheme(themeMode, dynamicColor, pureBlack)`: `MaterialExpressiveTheme` com `MotionScheme.expressive()`. Esquema `SchemeTonalSpot` (contraste 0) do seed do app `#6A4F86`, calculado com material-color-utilities; cor dinâmica no Android 12+ quando ligada (padrão); preto puro escurece as superfícies em Oklab.
- O tema vem da preferência do servidor (`theme`: `system`, `light`, `dark`); preto puro e cor dinâmica são do aparelho (DataStore).
- `ModuleTheme(seed)`: esquema aninhado gerado do seed do módulo, harmonizado com o `primary` do tema quando a cor dinâmica está ligada.
- success `#3B8A3E` e warning `#B5651D`, harmonizados com o seed, em `PrumoTheme.extendedColors`.

## Testes e verificação

| O quê | Onde | Como |
|---|---|---|
| Dinheiro, datas, endereço do servidor, erros | `:core:model` | JUnit no JVM |
| Bearer, 401, erros pelo `code`, health, união do Início | `:core:network` | MockWebServer com respostas reais gravadas (test fixtures, `fixtures/`) |
| Cópia no aparelho (regra do interceptor) | `:core:network` (`CachingInterceptorTest`) | MockWebServer e uma cópia em memória |
| Cópia por conta no Room | `:core:data` (`ResponseStoreTest`) | Robolectric com Room em memória |
| DTOs × contrato | `:core:network` (`ContractTest`) | cópia de `openapi.json`: campos obrigatórios e enums de cada DTO |
| Sessão, repositórios | `:core:data` | fakes e Robolectric (Keystore fora do teste) |
| ViewModels | `:app`, `:feature:*` | Turbine + fakes de `:core:testing` |
| Telas | `:app` | Robolectric sobre os `XScreen` |
| Aparência | todos os componentes e telas | `@PhonePreviews` (claro, escuro, 360 e 412 dp) |
| Contra o servidor | emulador `Pixel_9` (API 37) | `make dev` e `make user` em `~/git/prumo`, `http://10.0.2.2:8000` |

Fluxos do teste real: servidor errado e certo, senha errada, login, Início, Configurações, sessão expirada (revogar a sessão do app pela web), servidor inacessível (parar a API), token cifrado (`adb shell run-as com.henriquesebastiao.prumo.debug cat files/datastore/session.preferences_pb`). Para dados iguais aos das pranchas, `backend/scripts/e2e_server.py` (D30 do servidor) na porta 8001.

Comparação visual: `design/v1/` não tem PNGs. `scripts/render-design.mjs` renderiza cada prancha numerada (2×, com a legenda) em `design/renders/v1/` (ignorado pelo git) com o Playwright da web do servidor; compare com `adb exec-out screencap -p` da mesma tela. Os dados das pranchas e do `e2e_server.py` são parecidos, não iguais (nomes, valores), e o relógio do servidor fica congelado em 17/11/2026 enquanto o do emulador é o real: registros com data de hoje no app podem dar `garage.odometer_inconsistent` contra leituras "futuras" do servidor, o que é a regra G3 funcionando.
