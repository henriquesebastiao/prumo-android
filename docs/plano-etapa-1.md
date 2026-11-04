# Plano da Etapa 1

Plano apresentado ao fim da Etapa 1 (2026-11-03), complementar a `docs/design-v1.md` (inventário e cruzamento com a API) e ao `ROADMAP.md` (fases). Depois da aprovação, o conteúdo daqui vai para o `CLAUDE.md`, `docs/arquitetura.md` e `docs/decisoes.md`, e este arquivo pode ser apagado.

## Achados

1. **O repositório estava vazio**: sem `CLAUDE.md` e sem git. O `CLAUDE.md` do servidor (`~/git/prumo`) não tem a seção "Servidor e app juntos" e pede "commits locais pequenos", o que conflita com a regra da tarefa ("não faça commit, stage nem push; descreva em `commit-message.md`"). O `CLAUDE.md` do Downtify Android (`docs/referencias/downtify-CLAUDE.md`, repositório público `henriquesebastiao/downtify-android`) tem exatamente essas regras e serve de base.
2. **Design**: importado do export local do projeto do Claude Design (o DesignSync pedia `/design-login`). Os 33 arquivos foram conferidos por SHA-256.
3. **Material 3 Expressive só no material3 1.5.0-beta02**: conferido nos AARs. A 1.4.0 estável não tem `FloatingToolbar`, `ButtonGroup`, `LoadingIndicator`, `ToggleButton` nem `SegmentedListItem`. O Downtify fixou a mesma versão.
4. Os valores estáticos de `design/v1/_ds/…/tokens/colors.css` são do seed do Netrik (`#136B79`); os do Prumo saem dos seeds da entrega em tempo de execução (ver `docs/design-v1.md`, seção 3).
5. `docs/referencias/compose-mapping.md` é o mapeamento token/componente → Compose do design system (vem da skill local do design system, que não está no export).

## Módulos Gradle

```
:app                  Activity, Application (Hilt), shell, telas de sistema
                      (primeiro acesso, Início, Configurações), montagem dos PrumoFeature
:core:model           JVM puro: centavos, km, UUID v7, códigos de erro, formatação pt-BR
:core:network         OkHttp + Retrofit + kotlinx.serialization, endereço do servidor dinâmico,
                      bearer, erros pelo code, DTOs do sistema (health, auth, me, preferences,
                      modules, sessions, envelope do dashboard)
:core:data            repositórios do sistema, sessão (Tink + DataStore), preferências do aparelho
:core:designsystem    tema, ModuleTheme, tokens, ícones, componentes, @PhonePreviews
:core:ui              estados de carregando/vazio/erro/offline, textos de erro por code, formatadores Compose
:core:feature         contrato PrumoFeature (id, seed, ícone, card do Início, ações rápidas,
                      entradas de navegação)
:core:testing         fakes e regras de teste
:core:database        (Fase 4) Room
:feature:finance      cada módulo com sua API (Retrofit), DTOs, repositório e telas;
:feature:garage       na fundação, só o PrumoFeature e o card do Início
:feature:reading
```

Regras de dependência:

- `:app` → tudo.
- `:feature:X` → `:core:feature`, `:core:designsystem`, `:core:ui`, `:core:data`, `:core:network`, `:core:model`. Nunca outro `:feature:*` (verificado no build, como o import-linter no servidor).
- `:core:data` → `:core:network`, `:core:model` (e `:core:database` na Fase 4).
- `:core:network` → `:core:model`. `:core:ui` → `:core:designsystem`, `:core:model`.
- O dashboard é uma união discriminada por `module`: `:core:network` entrega cada resumo como `JsonObject` e o `PrumoFeature` do módulo o decodifica. Módulo que o servidor tem e o app não conhece é ignorado; módulo que o app conhece e o servidor não tem fica oculto.
- Repositórios expõem `Flow` desde já, para o Room entrar na Fase 4 sem mudar as telas.

## Dependências

Versões estáveis conferidas em 2026-11-03 no Google Maven, Maven Central e Gradle Plugin Portal. Reconfira antes de criar o `libs.versions.toml`.

| Grupo | Versões |
|---|---|
| Build | Gradle 9.8.2 · AGP 9.5.0 · Kotlin 2.4.21 · KSP 2.3.13 · compileSdk/targetSdk 37 · minSdk 26 · bytecode 17 |
| UI | Compose BOM 2026.09.00 · **material3 1.5.0-beta02** (fixo até a 1.5 estável) · material3-adaptive 1.3.0 · adaptive-navigation3 1.3.0 · navigation-suite 1.5.0-beta02 · Navigation 3 1.2.0 · lifecycle 2.11.0 (com `lifecycle-viewmodel-navigation3`) · activity-compose 1.13.0 · core-ktx 1.19.1 · core-splashscreen 1.2.0 |
| Injeção e dados | Hilt 2.60.1 · hilt-lifecycle-viewmodel-compose 1.4.0 · OkHttp 5.5.1 · Retrofit 3.0.0 + converter kotlinx-serialization · kotlinx-serialization-json 1.12.0 · coroutines 1.11.0 · DataStore 1.3.0 · Tink 1.24.0 · material-color-utilities (`com.materialkolor`) 6.0.0 |
| Por fase | Coil 3.6.3 (Livros) · Paging 3.5.3 (Finanças) · Room 2.8.5 e WorkManager 2.12.0 (offline) · Glance 1.2.0 (widget) |
| Qualidade | detekt 1.23.8 (a 2.0 ainda é alpha) + compose-rules 0.6.8 · Spotless 8.11.0 + ktlint 1.8.0 · JUnit 4.13.2 · Turbine 1.2.1 · Robolectric 4.17 · MockK 1.14.11 (só onde um fake for difícil) · mockwebserver3 5.5.1 |

Sem biblioteca para UUID v7 (`kotlin.uuid.Uuid.generateV7()`, presente na stdlib 2.4.21) nem para datas (`java.time`).

Fontes: Google Sans Flex e JetBrains Mono empacotadas em `res/font` (os arquivos estão em `design/v1/_ds/…/assets/fonts/`), sem fontes baixáveis pelo Google Play (mesmo espírito da D18 do servidor). Confirmar a licença OFL da Google Sans Flex. Ícones: Material Symbols Rounded convertidos em vetores Android.

## Mudanças no servidor por fase

| Fase | Mudança | Situação |
|---|---|---|
| Fundação | Seção "Servidor e app juntos" no `CLAUDE.md` e `commit-message.md` no `.gitignore` | necessária |
| Fundação | `service: "prumo"` no `GET /api/health` (A5), aditivo, sem migration | recomendada |
| Fundação | IP da sessão em `SessionRead` (A3): coluna anulável, migration aditiva | opcional |
| Fundação | "Encerrar as outras sessões" (A4) | sem mudança: o app revoga uma a uma |
| Finanças | Totais por dia na lista de transações (A6) | decidir na fase (ou tirar do design) |
| Finanças | Percentual por categoria com uma casa (A7), origem do registro (A8), status da fatura na transação (A10) | decidir na fase |
| Offline | Controle de concorrência nas edições (A2) | decidir na fase |

## Verificação

- Em cada etapa: `./gradlew build detekt spotlessCheck`.
- Testes de unidade: JVM para `:core:model` (dinheiro, formatação, UUID); MockWebServer para `:core:network` (bearer, 401 encerra a sessão, erros pelo `code`, detecção do health, decodificação da união do dashboard com respostas reais gravadas do servidor); Turbine para ViewModels; Robolectric para as telas sem estado (`XScreen`).
- Teste de contrato: cópia de `~/git/prumo/openapi/openapi.json` no app (com script para atualizar) e um teste JVM que confere os campos obrigatórios e enums de cada DTO escrito à mão.
- Previews `@PhonePreviews`: claro e escuro, 360 e 412 dp.
- Teste real (local): `make dev` e `make user` em `~/git/prumo`; emulador `Pixel_9` (API 37) em `http://10.0.2.2:8000`. Fluxos: servidor errado e certo, senha errada, login, Início, Configurações, sessão expirada (revogar a sessão do app pela web), servidor inacessível (parar a API), token criptografado (`adb shell run-as`). Para dados iguais aos das pranchas, `backend/scripts/e2e_server.py` (D30) numa porta separada.
- Comparação visual: `design/v1/` não tem PNGs. Renderizar as pranchas a 412×915 com Playwright em `design/renders/v1/` (fora da v1) e comparar com capturas do `adb exec-out screencap`.
- Ambiente local do mantenedor: SDK em `~/Android/Sdk` (plataformas 35, 36.1, 37; AVD `Pixel_9`), JDK 17 em `/usr/lib/jvm/java-17-openjdk` (o padrão do sistema é o 27).

## Decisões pedidas ao mantenedor (recomendação primeiro)

1. `CLAUDE.md` do app com base no do Downtify, adaptado ao Prumo; no servidor, trocar "commits locais" por "não faça commit; descreva em `commit-message.md`".
2. Navigation 3 em vez de Navigation Compose 2: folhas fora da pilha, entradas vindas de cada `PrumoFeature`, lista e detalhe no tablet prontos.
3. material3 1.5.0-beta02 fixo até a 1.5 estável.
4. DTOs escritos à mão com teste de contrato, em vez de gerador OpenAPI.
5. Telas de sistema no `:app` (como o `system/` do servidor), em vez de `:feature:onboarding|home|settings`.
6. Fontes empacotadas no APK.
7. Interface só em pt-BR, com os textos em `values/`.
8. Repositório git criado pelo mantenedor.
9. Card hero do Início = primeiro módulo da ordem (C16). Os demais conflitos são decididos na fase de cada módulo e registrados em `docs/decisoes.md`.
