# Decisões técnicas

Registro curto das decisões que moldam o app. Os números D01… são deste repositório; decisões do servidor são citadas como "D40 do servidor". Para mudar uma decisão aceita, adicione uma nova entrada que a substitua (e marque a antiga como substituída) em vez de editar o histórico.

As decisões D01 a D08 foram pedidas ao mantenedor no fim da Etapa 1 (análise do design e plano, 2026-11-03) e aprovadas em 2026-11-04. A análise do design, com as lacunas A1–A12 e os conflitos C1–C16, está em `docs/design-v1.md`.

---

## D01 — `CLAUDE.md` com base no Downtify e um fluxo de trabalho com o servidor
**Status:** aceita · 2026-11
**Contexto:** o repositório começou vazio, sem `CLAUDE.md` nem git. O app e o servidor avançam em paralelo, e o mantenedor revisa e commita à mão. O `CLAUDE.md` do Downtify Android (`docs/referencias/downtify-CLAUDE.md`) já tinha as regras de um app irmão (Kotlin, Compose, Material 3 Expressive, o mesmo design system) e de "servidor e cliente juntos".
**Decisão:** o `CLAUDE.md` do app segue o do Downtify, adaptado ao Prumo e em pt-BR. A seção "Servidor e app juntos" é a mesma nos dois repositórios (D43 do servidor): a mudança que um lado precisa é feita no outro na mesma tarefa; ninguém faz commit, stage nem push, exceto num branch de trabalho quando o mantenedor pede numa sessão na nuvem; toda mudança é descrita em `commit-message.md`, ignorado pelo git. O repositório git foi criado pelo mantenedor.
**Consequências:** o servidor trocou "commits locais pequenos" por essa regra. Cada lado de uma mudança conjunta cita o outro na mensagem.

## D02 — Navigation 3
**Status:** aceita · 2026-11
**Contexto:** o design abre registros rápidos como folhas sobre a tela atual, cada módulo traz os seus destinos e as telas grandes mostram lista e detalhe.
**Decisão:** Navigation 3 (`androidx.navigation3` 1.2) em vez de Navigation Compose 2: a pilha é uma lista de chaves `@Serializable` do próprio app; cada `PrumoFeature` contribui as suas entradas; folhas ficam fora da pilha; lista e detalhe vêm do `ListDetailPaneScaffold` com `adaptive-navigation3`.
**Consequências:** sem grafo declarado nem rotas em texto. O `ViewModel` de cada entrada vive enquanto ela está na pilha (`rememberViewModelStoreNavEntryDecorator`).

## D03 — material3 1.5.0-beta02 fixo
**Status:** aceita · 2026-11
**Contexto:** a 1.4.0 estável (a do BOM 2026.09.00) não tem `FloatingToolbar`, `ButtonGroup`, `LoadingIndicator`, `ToggleButton` nem `SegmentedListItem`, que o design usa. O Downtify fixou a mesma versão.
**Decisão:** `androidx.compose.material3:material3` e `material3-adaptive-navigation-suite` 1.5.0-beta02, fora do BOM, até a 1.5 estável.
**Consequências:** APIs com `@ExperimentalMaterial3ExpressiveApi`; uma beta nova pode mudar assinaturas. Trocar pela 1.5 estável assim que sair.

## D04 — DTOs escritos à mão, com teste de contrato
**Status:** aceita · 2026-11
**Contexto:** o OpenAPI do servidor é a fonte de verdade (D01 do servidor), mas geradores Kotlin produzem muito código, lidam mal com a união discriminada do Início e acoplam o app a campos que ele não usa.
**Decisão:** DTOs `@Serializable` escritos à mão em `:core:network`, só com os campos usados. Uma cópia de `openapi/openapi.json` fica nos test fixtures de `:core:network` (`src/testFixtures/resources/`, atualizada por `scripts/update-openapi.sh`), e `OpenApiContract` confere, para cada DTO: que todo campo dele existe no schema; que todo campo sem valor padrão é obrigatório no schema (uma resposta válida sempre decodifica); que um DTO enviado tem todos os campos obrigatórios; e que os enums do app cobrem os do servidor. Os `:feature:*` usam os mesmos fixtures para os seus resumos do Início.
**Consequências:** mudança no contrato aparece num teste vermelho, não em produção. Campos novos do servidor não quebram o app (`ignoreUnknownKeys`).

## D05 — Telas de sistema no `:app`
**Status:** aceita · 2026-11
**Contexto:** primeiro acesso, Início e Configurações dependem da sessão, dos módulos e uns dos outros; separá-las em `:feature:*` exigiria um contrato só para elas.
**Decisão:** ficam em `app/src/main/kotlin/…/system/` (como o `system/` do servidor). `:feature:*` é só para os módulos do Prumo.
**Consequências:** o `:app` cresce um pouco; nenhum `:feature:*` depende de telas de sistema.

## D06 — Fontes empacotadas no APK
**Status:** aceita · 2026-11
**Contexto:** o design usa Google Sans Flex (arredondada e larga) e JetBrains Mono. Fontes baixáveis dependem do Google Play Services, e o Prumo não deve depender de terceiros em tempo de execução (o mesmo espírito da D18 do servidor).
**Decisão:** os arquivos variáveis oficiais do repositório `google/fonts` (`ofl/googlesansflex`, `ofl/jetbrainsmono`), ambos sob a SIL Open Font License 1.1, ficam em `core/designsystem/src/main/res/font/`, com as licenças em `core/designsystem/fonts/`. O arquivo que veio no export do design (cópia do PixelPlayerOSS) não é usado.
**Consequências:** cerca de 4 MB a mais no APK. Funciona em aparelhos sem Play Services.

## D07 — Interface só em pt-BR
**Status:** aceita · 2026-11
**Contexto:** o servidor só tem `pt-BR` (`locale` aceita só esse valor) e a web também.
**Decisão:** textos em `values/strings.xml` de cada módulo, em pt-BR, sem pastas de tradução. Formatos com o locale `pt-BR` (`Locale.forLanguageTag`).
**Consequências:** traduzir depois é acrescentar `values-xx/` e o `localeFilters`; nenhum texto fica em Kotlin.

## D08 — Card hero do Início é o primeiro módulo da ordem
**Status:** aceita · 2026-11
**Contexto:** a prancha do Início põe Finanças no card hero (C16), mas a ordem dos módulos é do usuário (D20 do servidor).
**Decisão:** o hero é o primeiro módulo ativo na ordem do usuário; os demais ficam em meia largura, em grade de 2. Com a ordem padrão (`finance`, `garage`, `reading`), o resultado é o da prancha.
**Consequências:** cada `PrumoFeature` sabe desenhar o seu card nas duas formas. Os demais conflitos (C1–C15) são decididos na fase de cada módulo e registrados aqui.

## D09 — `:core:model` e `:core:network` em JVM puro
**Status:** aceita · 2026-11
**Contexto:** o plano previa `:core:model` em JVM puro e não dizia o tipo de `:core:network`. A rede (OkHttp, Retrofit, kotlinx.serialization) não precisa do Android, e os testes dela são os mais importantes da fundação (bearer, 401, erros, health, Início).
**Decisão:** `:core:network` também é um módulo Kotlin JVM, com Hilt pelo `hilt-core`. O que precisa do Android (o token cifrado, o nome do aparelho) fica em `:core:data`; a rede só conhece a interface `SessionGateway` (o endereço e o token da sessão, e o aviso de `401`). Os resumos do Início e as respostas gravadas ficam em test fixtures (`java-test-fixtures`) que os módulos Android também usam.
**Consequências:** `./gradlew :core:network:test` roda no JVM, sem Robolectric (e roda mesmo sem o Android SDK, o que permitiu verificar a rede numa sessão na nuvem sem acesso ao Google Maven). O Android lint não olha o módulo: nada de API do JVM mais nova que o API 26.

## D10 — Como o app reconhece um servidor Prumo
**Status:** aceita · 2026-11
**Contexto:** a tela do servidor diz "Este endereço não é um servidor Prumo" (A5), e o health não identificava o produto.
**Decisão:** o servidor ganhou `service: "prumo"` no `GET /api/health` (D44 do servidor). O app aceita `service == "prumo"` e, enquanto houver servidores de desenvolvimento anteriores, uma resposta sem `service` com `status`, `database` e `version` válidos. `503` com o formato do health é "banco indisponível"; qualquer outra resposta é "não é um servidor Prumo"; sem resposta, "servidor inacessível". Sem esquema, o app tenta `https://` e depois `http://`, exceto IP e `.local`, que tentam `http://` primeiro.
**Consequências:** o fallback sem `service` pode sair quando a primeira versão publicada do servidor já tiver o campo.

## D11 — Fuso e moeda vêm da preferência do usuário
**Status:** aceita · 2026-11
**Contexto:** o design formata tudo com "R$" e usa a data do aparelho na saudação do Início (C12, C13), mas a moeda e o fuso são preferências do usuário (F9 e S3 do servidor).
**Decisão:** formatos com o locale pt-BR e a moeda da preferência (`currency`); "hoje", a saudação e a data do Início no fuso da preferência (`timezone`), não no do aparelho.
**Consequências:** com a moeda padrão (BRL) e o fuso do aparelho igual ao da conta, o resultado é o das pranchas.

## D12 — Preferências do servidor e do aparelho
**Status:** aceita · 2026-11
**Contexto:** a tela Aparência tem tema, preto puro e cor dinâmica; a toolbar do Início tem 4 ações fixadas (A12). Só o tema é preferência da conta no servidor (D22 do servidor).
**Decisão:** o tema (`system`, `light`, `dark`), os módulos ativos e a ordem são da conta (`PATCH /me/preferences`) e valem na web e em outros aparelhos. Preto puro, cor dinâmica e as ações fixadas são do aparelho (DataStore).
**Consequências:** nenhuma mudança na API. Trocar de aparelho mantém tema e módulos, não as ações fixadas.

## D13 — Sessões ativas sem IP e "Encerrar as outras" uma a uma
**Status:** aceita · 2026-11
**Contexto:** a prancha de sessões mostra o IP de cada sessão e um botão para encerrar as outras (A3, A4). `SessionRead` não tem IP e não há rota em lote.
**Decisão:** a linha mostra aparelho, cliente e último uso, sem IP. "Encerrar as outras sessões" chama `DELETE /auth/sessions/{id}` para cada sessão que não é a atual e recarrega a lista.
**Consequências:** nenhuma mudança no servidor por enquanto. Se o IP fizer falta, entra como campo anulável em `SessionRead` (migration aditiva).

## D14 — HTTP sem TLS permitido no app inteiro
**Status:** aceita · 2026-11
**Contexto:** o Prumo é self-hosted: o servidor costuma estar na rede de casa, em `http://192.168.x.x:8000`. A configuração de segurança de rede do Android não permite liberar só endereços privados (ela compara nomes, não faixas de IP).
**Decisão:** `cleartextTrafficPermitted="true"` no app inteiro, como no Downtify, e certificados instalados pelo usuário são aceitos (proxy com CA própria). A tela do servidor avisa quando o endereço é público e sem HTTPS.
**Consequências:** o token viaja sem cifra em HTTP; na rede de casa é a mesma exposição da web. Para acesso pela internet, o guia de instalação do servidor recomenda um proxy com TLS.

## D15 — Conflitos de Veículos resolvidos como na web
**Status:** aceita · 2026-11
**Contexto:** as pranchas de Veículos pedem cálculos e campos que o domínio não tem do mesmo jeito (C1–C5 e C9 de `docs/design-v1.md`). A web do servidor já passou por eles na Fase 1 dela (D27 do servidor e `docs/frontend.md` de lá, "Veículos (Fase 1, D27): resolvidas"), e o app deve ter as mesmas funções da web.
**Decisão:** o app segue o que a web faz.
- C1: no abastecimento, o usuário digita valor total e litros; o preço por litro aparece como prévia marcada "calculado", só para conferir. O valor gravado vem da API (G7). O app não calcula o terceiro valor a partir de dois.
- C2: cada peça tem quantidade (padrão 1), como no domínio (D27 do servidor).
- C3: o total da manutenção (peças × quantidade + mão de obra) aparece como prévia "calculado"; o total gravado e exibido depois vem da API (G4).
- C4: o óleo da troca é um campo próprio (`oil_type`), com as sugestões de `previous_oil_types`; peças continuam disponíveis na troca de óleo.
- C5: o app não valida a ordem do km; o servidor responde `garage.odometer_inconsistent`, mostrado no campo com o km do registro em conflito. A diferença "+92 km desde a última atualização" é só prévia.
- C9: o combustível padrão é o habitual do veículo (`fuel_type`); sem ele, gasolina comum. O veículo padrão é o último usado nesta execução do app; sem uso, o primeiro da lista.
- Excluir uma manutenção, um abastecimento ou uma leitura pede confirmação e exclui na hora, como a web ("Não dá para desfazer"); o snackbar com desfazer das pranchas fica para depois.
**Consequências:** as prévias são conveniência de digitação, nunca o valor salvo; quando o servidor ganhar uma prévia própria (como já há para a próxima troca de óleo, `oil-change-suggestion`), o app passa a usá-la.

---

## Modelo

```
## Dxx — Título
**Status:** proposta | aceita | substituída por Dyy · AAAA-MM
**Contexto:** …
**Decisão:** …
**Consequências:** …
```
