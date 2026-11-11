# Prumo Android

App Android nativo do Prumo, o sistema open source e self-hosted de organização pessoal em módulos (Finanças, Veículos, Livros, Estudos, Hábitos e outros no futuro). O servidor (API FastAPI e web Vue) fica em `~/git/prumo`; este app é um cliente da mesma API, com as mesmas funcionalidades da web e nenhum cálculo de regra de negócio.

O estado atual e a próxima tarefa estão sempre no `ROADMAP.md`.

## Mapa do repositório

- `app/`: Activity, Application (Hilt), shell de navegação, telas de sistema (primeiro acesso, Início, Configurações) e montagem dos módulos
- `core/`: `model`, `network`, `data`, `database`, `designsystem`, `ui`, `feature`, `testing` (ver "Módulos e regras de dependência")
- `feature/`: um módulo Gradle por módulo do Prumo (`finance`, `garage`, `reading`, `studies`, `habits`)
- `build-logic/`: plugins de convenção do Gradle; versões só em `gradle/libs.versions.toml`
- `design/`: exportações do Claude Design, versionadas em `design/v1/`… É **somente referência visual**: nunca importe nada daqui no código. A versão ativa está em `design/README.md`
- `config/detekt/`: regras do detekt
- `scripts/`: ícones, cópia do contrato do servidor, gravação de respostas reais e render das pranchas
- `docs/`: documentação técnica

## Leia antes de trabalhar em…

- Módulos Gradle, camadas, rede, sessão, navegação ou o contrato `PrumoFeature`: `docs/arquitetura.md`
- O porquê de uma escolha: `docs/decisoes.md`
- Uma tela: `docs/design-v1.md` (inventário do design, telas × API, lacunas A1–A12 e conflitos C1–C16) e as pranchas em `design/v1/`
- Endpoints, autenticação, formatos e erros: `~/git/prumo/docs/api.md` e `~/git/prumo/openapi/openapi.json`
- Regras de negócio: `~/git/prumo/docs/dominio.md`
- Tokens e componentes do design system: `docs/referencias/compose-mapping.md`

## Comandos

- Gradle roda com JDK 17 ou 21 (`JAVA_HOME`; o padrão da máquina do mantenedor é o 27) e o SDK em `local.properties` (`sdk.dir`). Detalhes em `docs/arquitetura.md`, seção "Ambiente"
- `./gradlew build detekt spotlessCheck`: compila, roda os testes de unidade e o Android lint, o detekt e o ktlint. É o que o CI roda. Rode antes de considerar qualquer tarefa pronta
- `./gradlew spotlessApply`: formata
- `./gradlew :core:model:test :core:network:test`: testes JVM; `./gradlew testDebugUnitTest`: testes de unidade e Robolectric dos módulos Android
- `./gradlew installDebug`: instala o app de debug (`com.henriquesebastiao.prumo.debug`)
- `scripts/update-openapi.sh [caminho do prumo]`: copia `openapi/openapi.json` do servidor para o teste de contrato
- `scripts/record-fixtures.py [url] [usuário] [senha]`: grava respostas reais do servidor em `core/network/src/testFixtures/resources/fixtures/`
- `scripts/material_symbol.py <nome>…`: gera os vetores de Material Symbols Rounded em `:core:designsystem`
- `scripts/render-design.mjs [caminho do prumo]`: renderiza as pranchas de `design/v1/` em PNG (`design/renders/v1/`) para comparar com capturas do emulador

## O contrato do servidor

- O contrato é `~/git/prumo/openapi/openapi.json`, com `docs/api.md` (autenticação, formatos, erros, rotas) e `docs/dominio.md` (regras). Uma cópia do OpenAPI fica nos test fixtures de `:core:network` (`src/testFixtures/resources/openapi.json`, com as respostas gravadas em `fixtures/`), e o `ContractTest` e os testes de resumo de cada `:feature:*` (`OpenApiContract`) conferem os campos de cada DTO escrito à mão e os enums. Mudou o contrato no servidor? Rode `scripts/update-openapi.sh` e ajuste os DTOs.
- **Não invente endpoints.** O app só chama o que o servidor tem. Se o app precisa de algo que o servidor não oferece, faça a mudança no servidor na mesma tarefa (ver "Servidor e app juntos").
- Docs e código do servidor discordam? O código vence; corrija os docs de lá.
- **Nenhuma regra de negócio no app.** Saldos, faturas, status do óleo, consumo, progresso de leitura, "hoje" e "este mês" vêm prontos da API. Prévias no formulário (C1, C3, C5–C7 de `docs/design-v1.md`) só entram com decisão registrada em `docs/decisoes.md`, na fase do módulo.
- Clientes ignoram campos desconhecidos e toleram valores de enum desconhecidos (`NetworkJson` em `:core:network`); a v1 só cresce.
- Toda requisição ao servidor da sessão leva `Authorization: Bearer <token>` (`AuthInterceptor`), e só a ele. Um `401` fora do login encerra a sessão (`SessionRepository`), e o app mostra o diálogo de sessão expirada.
- Erros chegam como `{"error": {"code", "message", "details", "fields"}}` e viram `AppError.Api` (`:core:model`). A tela mostra o texto do `code` (`errorText` em `:core:ui`); `message` é só para depuração. Código desconhecido cai no texto genérico do status.
- Servidor para testar: `make dev` e `make user` em `~/git/prumo` (API na porta 8000; no emulador, `http://10.0.2.2:8000`). Para dados iguais aos das pranchas, `uv run python scripts/e2e_server.py` em `~/git/prumo/backend` (porta 8001, usuário `henrique` / `prumo-demo-01`, relógio congelado em 17/11/2026 14:30 de São Paulo). Grave fixtures a partir dele, nunca à mão.

## Módulos e regras de dependência

```
:app ──► :feature:* ──► :core:feature ──► :core:designsystem
                    ──► :core:ui ───────► :core:designsystem, :core:model
                    ──► :core:data ─────► :core:network ──► :core:model
                                      ──► :core:database (Room)
:core:testing (fakes, só em testes)
```

- `:core:model` e `:core:network` são JVM puro: sem imports do Android. Testes rápidos e baratos; ponha lógica aqui sempre que ela viver sem Android. Eles também rodam no Android, então nada de APIs do JVM mais novas que o API 26 (o Android lint não olha estes módulos).
- `:core:network` conhece o formato do fio (DTOs, Retrofit, OkHttp). DTOs não saem dele, exceto os resumos do Início, que chegam como `JsonObject` ao `PrumoFeature` de cada módulo.
- `:core:data` guarda a sessão (Tink + DataStore), as preferências do aparelho, os repositórios do sistema e a cópia das respostas do servidor no Room (`:core:database`, D21: o `CachingInterceptor` responde dela, e pull-to-refresh chama `expireCache()` antes de recarregar). Repositórios expõem `Flow`/`StateFlow`; a interface nunca chama a API direto.
- `:core:designsystem` tem tema, tokens, ícones e componentes; `:core:ui` tem os estados de carregando, vazio, erro e sem conexão, os textos de erro por `code` e os formatadores Compose.
- `:core:feature` define o contrato `PrumoFeature`. Cada `:feature:<id>` implementa o seu e o registra no Hilt (`@IntoSet`).
- `:feature:X` nunca depende de outro `:feature:*`: o plugin `prumo.android.feature` falha o build. Integração entre módulos é do servidor (eventos).
- Telas de sistema ficam no `:app` (`app/src/main/kotlin/…/system/`), como o `system/` do servidor.
- Configuração do Gradle vai nos plugins de convenção de `build-logic/`; versões só em `gradle/libs.versions.toml`. Confira as versões estáveis atuais antes de mudar uma: não chute.

## Arquitetura

- Uma Activity, Navigation 3 com chaves `@Serializable` (`NavKey`). As chaves de sistema ficam em `app/…/navigation/`; cada `PrumoFeature` contribui as suas entradas. Folhas de registro rápido são folhas sobre a tela atual, não destinos.
- MVVM com fluxo unidirecional: o ViewModel expõe um `StateFlow<XUiState>` e a tela chama funções do ViewModel para os eventos.
- Toda tela é dividida em `XRoute` (pega o ViewModel e coleta o estado) e `XScreen(state, callbacks…, modifier)` sem estado, que previews e testes chamam direto.
- **Campos de texto**: o texto fica em estado de snapshot síncrono (`var x by mutableStateOf("")` no ViewModel, ou um `MutableStateFlow` simples), nunca num fluxo que passou por `combine`/`stateIn`: o salto assíncrono perde teclas. Não reescreva o texto no `onValueChange`; use `VisualTransformation`.
- Corrotinas: `viewModelScope` nos ViewModels, `@ApplicationScope` para o que vive com o app. Todo `catch` amplo relança `CancellationException`.
- Chamadas à API passam por `apiCall { }` (`:core:network`) e devolvem `Outcome<T>` (`Ok` ou `Failed(AppError)`). Exceção de rede não sobe para a interface.
- Datas: `java.time`. "Hoje", saudação e data do Início usam o fuso da preferência do usuário (`timezone`), não o do aparelho (S3 do servidor). Dinheiro em centavos (`Long`), formatado com a moeda da preferência (`currency`).
- Android 17+ (target 37): falar com a rede local exige a permissão `ACCESS_LOCAL_NETWORK` em tempo de execução. A tela do servidor pede antes de testar um endereço local.

## Tema e interface

- Componentes reais do Material 3 (Expressive); nada de imitação. Cores vêm de `MaterialTheme.colorScheme`: nunca hex numa tela. Tokens e componentes vivem em `:core:designsystem`; um redesign muda o design system, não as telas.
- O tema é `PrumoTheme` (`MaterialExpressiveTheme` + `MotionScheme.expressive()`): esquema `SchemeTonalSpot` gerado do seed do app (`#6A4F86`), claro, escuro e preto puro; cor dinâmica ligada por padrão no Android 12+. Anime pelo `MaterialTheme.motionScheme` (molas espaciais para tamanho, posição e cantos; de efeito para cor e opacidade).
- Cada módulo tem o seu seed (Finanças `#2E6B4F`, Veículos `#1F5F8B`, Livros `#8A4B2E`, Estudos `#1C6B74`, Hábitos `#7A5A14`) e é desenhado dentro de `ModuleTheme(seed)`, um tema aninhado harmonizado com a cor dinâmica. success e warning são `PrumoTheme.extendedColors`.
- Tipografia: Google Sans Flex arredondada (`ROND 100`) empacotada em `res/font` (sem fontes baixáveis), escala v3; `PrumoType.hero` (wdth 151) para o título largo; `PrumoType.data*` em JetBrains Mono com algarismos tabulares para valores. Não defina `fontFamily` numa tela.
- Formas, espaço e listas: `PrumoShapes`, `PrumoShape` (seção 24, hero 32, folha 28), `ListStyle` (segmented 24/8 gap 4, separated 24 gap 8) e `Spacing` (grade de 4 dp, margem 16).
- Ícones: Material Symbols Rounded como vetores em `:core:designsystem` (`PrumoIcons`), contorno em repouso e preenchido quando selecionado. Gere com `scripts/material_symbol.py`. Ícones de conta e categoria vêm do servidor como nomes Lucide e passam por `ServerIcons`.
- Todo texto visível vai em `values/strings.xml` do módulo (só pt-BR, D07). Plurais com `one` e `other`. Nada de texto fixo em Kotlin.
- Edge-to-edge: use `WindowInsets`. Toque mínimo de 48 dp. Telas funcionam de 360 dp para cima.
- Valores (dinheiro, km, percentuais) e títulos nunca quebram linha nem perdem caractere: use `FitText`, que diminui a fonte até caber (D23). Teste com fonte grande (130 %) e 360 dp.
- Cada tela e componente tem previews `@PhonePreviews` (claro e escuro, 360 e 412 dp), as telas montadas com dados de `PreviewData`.

## Testes e qualidade

- `./gradlew build detekt spotlessCheck` verde é o mínimo.
- Testes de unidade: JUnit 4 + Turbine + fakes de `:core:testing` (MockK só onde um fake for difícil). Lógica pura em `:core:model`; rede com MockWebServer em `:core:network` (bearer, 401, erros pelo `code`, health, união do Início com respostas reais gravadas).
- Telas: Robolectric no JVM (`src/test`), sempre sobre o `XScreen` sem estado.
- O que muda o comportamento contra o servidor é verificado de verdade (emulador + servidor de desenvolvimento). Diga exatamente o que rodou e o que não rodou.
- Pronto significa: build, detekt e ktlint verdes, documentação afetada atualizada, item marcado no `ROADMAP.md` e `commit-message.md` descrevendo a mudança.
- Ambiguidade nos docs durante a execução: escolha a opção mais simples que não feche portas, registre em `docs/decisoes.md` e siga.

## Convenções

- Código, identificadores, commits e comentários em inglês. Documentação e textos de interface em pt-BR.
- Conventional Commits com o escopo do módulo (`feat(garage): add the fueling sheet`, `build: …`, `docs: …`), escritos em `commit-message.md` e nunca commitados por você (ver "Servidor e app juntos").
- Kotlin com detekt e ktlint (`.editorconfig`), sem curinga em import. Gerenciador: Gradle wrapper.
- A versão do app só em `app/build.gradle.kts` (`versionName` e `versionCode`, que sobe a cada versão).

## Pergunte antes de

- Adicionar uma dependência que não esteja em `gradle/libs.versions.toml` ou prevista em `docs/arquitetura.md`
- Contrariar uma decisão registrada em `docs/decisoes.md`
- Pedir ao servidor uma mudança incompatível na API v1
- Resolver um conflito entre o design e o domínio de um jeito que não esteja em `docs/decisoes.md`

## Resposta final

Toda tarefa termina com uma resposta que deixa o mantenedor conferir a mudança à mão, nesta ordem:

- **Como testar.** Passos concretos, não "rode os testes": os comandos (`./gradlew build detekt spotlessCheck`, um teste só como `./gradlew :core:network:test --tests '*HealthTest'`, `./gradlew installDebug`), o servidor que precisa (`make dev` e `make user` em `~/git/prumo`, ou o `e2e_server.py`), o que fazer no emulador (`Pixel_9`, API 37, servidor em `http://10.0.2.2:8000`) e o que deve aparecer quando funciona. Diga o que você já rodou e o que fica para o mantenedor.
- **Onde aparece na interface.** Quando uma tela mudou, o caminho até ela e o controle: "Início → engrenagem → Configurações → *Sessões ativas*, o botão *Encerrar* de cada linha" ou "Início → card de Veículos → ⋮ → *Arquivar*". Diga quem vê (módulo ativo, celular × rail ≥ 600 dp, uma configuração ligada). Se a web do servidor mudou também, onde (página e rota). Se nada visível mudou, diga em uma linha.
- **Repositórios alterados**, cada um com o seu `commit-message.md` (e o branch, numa sessão na nuvem).
- **Desvios do plano, pendências e perguntas**, quando houver.

## Servidor e app juntos

O app fica aqui, em `~/git/prumo-android`; o servidor (API e web), em `~/git/prumo`. Os dois evoluem como um produto só: quando uma tarefa num repositório precisa de uma mudança no outro (um endpoint ou campo novo da v1, um bug do servidor que o app encontrou, documentação de contrato errada), faça a mudança no outro repositório na mesma tarefa, sem parar para perguntar. Uma sessão aberta no servidor pode mudar este repositório do mesmo jeito.

- Antes de editar o outro repositório, leia o `CLAUDE.md` dele e siga-o lá (ferramentas, testes, documentação, critério de pronto). Uma mudança do servidor de que o app depende leva testes, documentação (`docs/api.md`, `docs/dominio.md`) e o snapshot do OpenAPI na mesma mudança; depois, rode `scripts/update-openapi.sh` aqui.
- Apps instalados continuam chamando o formato antigo: na v1, só mudanças aditivas. A lista "Pergunte antes de" vale nos dois lados.
- **Não faça commit, stage nem push em nenhum dos dois repositórios.** O mantenedor faz os commits. Exceção: numa sessão na nuvem, em que o trabalho só sobrevive no GitHub, o mantenedor pode pedir commit e push num branch de trabalho, nunca na `main`; o `commit-message.md` continua descrevendo tudo o que está pendente de revisão.
- **Nenhuma mudança sem descrição.** Toda edição em arquivo versionado (código, testes, docs, configuração) é descrita em `commit-message.md` na raiz do repositório que ela tocou (ignorado pelo git nos dois). Uma tarefa que tocou os dois repositórios deixa uma mensagem em cada um, e cada mensagem cita o outro lado ("Needs the server's …" / "Used by the Android app's …").
- Escreva a mensagem antes da resposta final, em inglês e em Conventional Commits com o escopo do módulo: assunto no imperativo com até 72 caracteres, corpo dizendo o porquê e, depois, o que mudou por arquivo ou área.
- O arquivo pode ter uma mensagem antiga. Se o `git log` já tem o assunto dela, ela foi commitada: substitua. Se ela descreve mudanças ainda não commitadas, mantenha: amplie a mensagem para cobrir tudo o que está pendente ou acrescente outra para um trabalho não relacionado, separada por uma linha `---` e citando os arquivos que cobre.
- Termine a resposta final citando cada repositório alterado e o seu `commit-message.md`.
