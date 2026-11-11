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

## D16 — Finanças como na web, sem cálculo no app
**Status:** aceita · 2026-11
**Contexto:** as pranchas de Finanças pedem dados que a API não tem (A6–A10) e prévias no formulário (C7), usam 8 cores hex e ícones Material onde o servidor guarda 11 chaves de cor e nomes Lucide (C10, C11), e mostram "Excluir" categoria sem olhar o uso (C15). A web já resolveu a maioria na Fase 2 dela (D31–D34 do servidor).
**Decisão:** o app segue a web e o que a API já entrega.
- A6: o cabeçalho de cada dia em Transações mostra só a data ("Hoje · 02/11"); os totais de receitas e despesas são os do filtro inteiro, que a API já devolve (D34 do servidor). Somar a página no app ficaria errado com paginação. O total por dia no servidor fica no ROADMAP, à espera de aprovação.
- A7: o percentual por categoria é o inteiro da API ("78 %").
- A8: sem "Criada por" no detalhe da transação.
- A9: a conta pré-selecionada é a última usada nesta execução do app; sem uso, a primeira da lista (a web usa a primeira). Igual ao veículo de Veículos (D15).
- A10: o detalhe de uma compra no cartão mostra a fatura dela ("Fatura de dezembro") e o status, lido de `GET …/invoices/{closing_on}`; sem campo novo.
- C7: "Pagar fatura" tem Total (o `remaining_cents` da fatura) e Parcial. No parcial, "Restam R$ 564,22 nesta fatura" é prévia (`remaining_cents` menos o digitado), como as prévias de D15; o que vale é a fatura que a API devolve depois.
- C10: cada uma das 11 chaves de cor (`green`…`gray`) vira o seed do avatar (`ModuleTheme`), com o mesmo hex da paleta da web. Os seletores oferecem as 7 cores da web (azul, violeta, laranja, verde, rosa, ciano, cinza) e a cor atual, se for outra.
- C11: os seletores de ícone gravam os nomes Lucide da web (`ACCOUNT_ICONS` e `CATEGORY_ICONS` de `helpers.ts`), desenhados pelo `ServerIcons`; nome desconhecido usa o ícone genérico.
- C15: categoria, conta ou cartão sem transações podem ser excluídos; com transações, a ação vira "Arquivar" (D32 do servidor).
- Navegação: Finanças abre no Resumo, com abas em pílula (Resumo, Transações, Contas, Cartões); Categorias e a busca ficam no topo. A toolbar tem nova receita, nova transferência e pagar fatura, e o FAB lança despesa. O formulário de transação é uma tela (prancha 08), não uma folha; categoria e pagamento de fatura são folhas (pranchas 11 e 14).
- Exclusões pedem confirmação e excluem na hora, como em Veículos (D15); uma parcela pergunta se é só ela ou a compra inteira (`?installments=all`).
**Consequências:** nenhuma mudança no servidor nesta fase. Se o total por dia (A6) for aprovado, o app passa a mostrá-lo no cabeçalho do dia.

## D17 — KSP sem modo incremental
**Status:** aceita · 2026-11
**Contexto:** na primeira verificação local (2026-11-09), um build incremental instalou um app sem Finanças: a saída do KSP de `:feature:finance` (e a entrada dela no build cache) tinha só o arquivo da última mudança, e o Hilt perdeu o `@IntoSet` do `FinanceFeature`. Nada falhou; o módulo só sumiu do Início e de Configurações. O CI, que compila do zero, não pega isso.
**Decisão:** `ksp.incremental=false` em `gradle.properties`. Cada módulo roda o KSP inteiro quando muda.
**Consequências:** alguns segundos a mais por módulo alterado. Voltar ao incremental só com uma versão do KSP que corrija a saída parcial, conferindo que `app/build/generated/hilt/component_trees/…/PrumoApplication_ComponentTreeDeps.java` cita os três `*Module` depois de um build incremental.

## D18 — Campo de valor como calculadora
**Status:** aceita · 2026-11
**Contexto:** o design pede o "Campo de valor" (borda 2 dp primary, valor grande com algarismos tabulares, foco ao abrir) nos registros rápidos. Os valores são digitados só em dígitos e desenhados por uma `VisualTransformation` ("4636" → "R$ 46,36"). Na verificação, um campo que abria preenchido e recebia o foco por código ficava com o cursor no início: o pagamento parcial de "500,00" virou R$ 50.001.284,60, e o servidor aceitou.
**Decisão:** `ValueField` e `DecimalField` com `large` desenham o Campo de valor (`ValueBox` no design system); o cursor fica sempre no fim, como numa calculadora, e a caixa inteira dá o foco ao campo. As folhas e formulários focam o valor ao abrir (`rememberOpeningFocus`). "Parcial" em Pagar fatura começa vazio (a prancha 14 mostra o valor digitado e "Restam …"). No abastecimento, data e km são tags que mudam com um toque (prancha 08); o km abre o campo e fica aberto com um erro do servidor.
**Consequências:** não dá para editar o meio de um valor; apaga-se do fim. Pagar mais que a fatura continua aceito pelo servidor (F5 não proíbe); fica como pergunta ao mantenedor.

## D19 — Livros como na web, com a capa pelo app
**Status:** aceita · 2026-11
**Contexto:** as pranchas de Livros (01–07) e a web do servidor (D35 e D36 de lá) divergem em alguns pontos, e a capa precisa de câmera, galeria e uma rota autenticada. C6 de `docs/design-v1.md` pede "56 % · +24 páginas hoje" enquanto se digita a página, mas o percentual é do servidor (R5).
**Decisão:**
- C6: a folha "Atualizar progresso" mostra a onda acompanhando a página digitada e "+N páginas" como prévia; o percentual, o status e as datas só aparecem depois de salvar, vindos da API (R2–R5). "Terminei" e "Marcar como lido" mandam a última página e o servidor marca o livro como lido (R4); se isso acontece com o livro aberto, o avatar do herói faz a morfose de celebração (cookie → sol, `CelebrationAvatar`).
- "Lendo agora": entre os livros em leitura que o filtro mostra, o que avançou por último (`last_progress_at`), como a web. É escolha de apresentação, não regra.
- Sem capa, o app desenha uma com título e autor (R6), na cor do status (lendo em primary, quero ler em secondary, lido em tertiary), em vez do só-ícone das pranchas; a capa pequena do herói só tem espaço para o título e o autor abreviado.
- A capa vem pela rota autenticada com o Coil 3 sobre o `OkHttpClient` da sessão (`@ApiClient`, com o bearer) e com `?v=<cover_updated_at>` no endereço, para a cache trocar junto com a capa. A foto da câmera (`TakePicture` num `FileProvider` próprio, `CoverFileProvider`) ou da galeria (`PickVisualMedia`) vai como está quando é JPEG, PNG ou WebP de até 5 MB; senão o app a reduz a 1600 px, endireita pelo EXIF e manda em JPEG. Reduzir a 800 px e regravar em WebP continua no servidor (D36 de lá). Se o livro salva e a capa não, o formulário fica aberto com o motivo e o próximo "Salvar" edita o livro já criado.
- A data da anotação (`noted_on`, D35 de lá) é uma tag "Hoje" na folha, que abre o seletor de data; uma anotação nova sem data trocada vai sem `noted_on` e o servidor a data no fuso do usuário. A edição manda a data só quando há uma (o `PATCH` recusa `null`).
- Estante: os filtros de status são exclusivos e "Tenho" combina com qualquer um (prancha 01). A ordem segue o menu da web (mais recentes, título, autor), pelo `sort` do servidor. O "Não tenho" e o ano dos lidos vieram depois (D20).
- Excluir livro e anotação pedem confirmação; "Remover capa" remove na hora (a capa pode ser escolhida de novo).
**Consequências:** a única dependência nova é o Coil, já prevista em `docs/arquitetura.md`. Nenhuma mudança no servidor nesta fase.

## D20 — Paridade sem prancha: "Vem aí", "Atividade recente", exportação e a estante da web
**Status:** aceita · 2026-11
**Contexto:** a web já mostra os itens `upcoming` e `activity` do `GET /dashboard` (D28 do servidor), exporta os dados (`GET /me/export`, D38 de lá) e filtra a estante por "Não tenho" e pelo ano dos lidos. O design v1 não tem prancha para nada disso, e a fatura de "Vem aí" mostrava "faltam R$ …" subtraindo `paid_cents` de `amount_cents` na web.
**Decisão:**
- Cada `PrumoFeature` descreve os seus `kind` (`upcoming` e `activity`, funções `@Composable` que devolvem um `HomeEvent` com título, linha de apoio, valor, ícone e destino), com os textos da web; um `kind` que o módulo não conhece é pulado. O Início mostra, depois dos cards, até 4 itens de cada lista (como a web) num grupo segmentado: "Vem aí" com o dia num `DateBadge` e "Atividade recente" com o ícone, os dois nas cores do módulo; tocar abre o veículo, a manutenção, o cartão, a transação ou o livro. Datas relativas ("Hoje, 08:12", "Ontem", "15/11") no fuso e no "hoje" da preferência (S3), por `relativeDateTime` de `:core:ui`.
- Nenhum valor é calculado no app: o servidor passou a mandar `remaining_cents` em `finance.invoice_due` (mudança aditiva; a web também o usa). Contra um servidor anterior, a linha mostra o valor da fatura.
- Exportação: Configurações → "Exportar dados" abre o seletor de arquivos do Android (`ACTION_CREATE_DOCUMENT`) com o nome do servidor (`prumo-<usuário>-<AAAAMMDD-HHMM>.zip`, no fuso do usuário). O app baixa o `.zip` pela sessão para o cache e copia para o arquivo escolhido; se o servidor falha, o arquivo vazio é apagado e o snackbar diz o motivo pelo `code`. Sem permissão de armazenamento.
- Estante: "Tenho" e "Não tenho" são exclusivos entre si (o mesmo de novo mostra os dois) e combinam com qualquer status. Com "Lidos", um chip com menu escolhe o ano (`finished_on`): começa no mais recente, como a web, e tem "Todos os anos". O filtro de ano é sobre a estante inteira que já veio (até 200 livros, como a web), sem nova requisição.
**Consequências:** um módulo novo pode publicar itens no Início sem mudar o `:app`. Os itens do Início só existem com rede até a Fase 4 (cache).

## D21 — As respostas do servidor guardadas no Room
**Status:** aceita · 2026-11
**Contexto:** a Fase 4 pede telas que abrem com dados e funcionam sem o servidor (pranchas 08 e 11: "com dados em cache, eles aparecem na hora e o indicador vira pull-to-refresh"; "mostra os últimos dados em cache, com a hora"). Os valores derivados (saldos, faturas, status do óleo, ritmo de leitura, totais de um filtro) são do servidor e não mudam o `updated_at` dos registros (D41 do servidor): guardar registros soltos e montar as telas com eles faria o app recalcular ou mostrar derivados velhos. As telas leem por repositórios que devolvem `Outcome`.
**Decisão:**
- O Room (`:core:database`, `prumo.db`) guarda a última resposta de cada `GET` ao servidor da sessão, inteira e como chegou (o JSON), por conta (`usuário@endereço`) e por caminho com a query. Nada é recalculado nem mesclado: a tela sempre recebe uma resposta inteira do servidor.
- A regra fica num interceptor do OkHttp (`CachingInterceptor`, `:core:network`), e repositórios e telas não mudam de assinatura. Uma resposta guardada há menos de 30 s responde na hora. Uma mais velha também, e é conferida em segundo plano; se o servidor responde outra coisa, `DataChanges` avisa o módulo do caminho e as telas abertas dele e o Início recarregam. Depois de uma gravação que deu certo (ou de um pull-to-refresh), toda resposta guardada fica velha: a próxima leitura pergunta ao servidor primeiro e só usa a cópia se ele não responde (sem conexão, recusa, tempo esgotado, ou `502`–`504` de um proxy na frente da API parada). Um `404` apaga a cópia daquele caminho.
- Não são guardados: sessões (`/auth/`), a exportação e `/sync/`. Imagens também não (as capas têm a cache do Coil).
- Pull-to-refresh marca a cópia como velha e recarrega: Início, Veículos (lista, detalhe, abastecimentos e manutenções), Finanças (as quatro abas) e Livros (estante, detalhe, anotações). As demais telas recarregam pela conferência em segundo plano.
- O ícone de nuvem do Início segue a última requisição ao servidor (`ServerStatusRepository`): "Servidor inacessível" e "Dados de hoje, 13:05", a hora em que o servidor mandou o resumo guardado.
- Conta: entrar como outro usuário ou em outro servidor apaga as respostas da conta anterior (S2 do servidor); Sair e Trocar servidor apagam todas; uma sessão expirada as mantém, para o mesmo usuário que entra de novo. O banco já fica fora do backup.
**Consequências:** sem tabela por entidade nem migração quando o servidor ganha campos (o JSON é lido pelos mesmos DTOs). Só há cópia do que já foi aberto com rede, e uma tela aberta sem rede e sem cópia mostra o erro de sempre. A sincronização incremental e a fila de pendentes, próximos itens da Fase 4, usam o mesmo banco.

## D22 — Ícone do app "Cristal"
**Status:** aceita · 2026-11
**Contexto:** o ícone da fundação (um prumo maciço, branco no roxo do seed) não agradou ao mantenedor, e no splash o desenho branco ficava quase invisível sobre o fundo claro do app. Quatro variantes minimalistas, todas no seed `#6A4F86`, foram desenhadas com o design system e comparadas numa prancha (grandes, nas máscaras dos launchers, temáticas e no tamanho real): Cristal, Equilíbrio (o prumo dentro do cookie do M3), Pêndulo e Módulos.
**Decisão:** o mantenedor escolheu o **Cristal**: o peso do prumo como um losango de duas faces (branca e lilás `#CFB6F2`) preso ao fio, sobre o roxo `#6A4F86`. Ícone adaptativo de 108 dp com o desenho na zona segura de 66 dp; o monocromático (ícones temáticos do Android 13+) tem a segunda face a 55 %. O splash mostra o mesmo ícone: o desenho sobre um círculo roxo (`windowSplashScreenIconBackgroundColor`).
**Consequências:** as cores do ícone ficam em `app/src/main/res/values/colors.xml` (`launcher_*`); trocar o seed do app é trocar essas três também.

## D23 — Valores e títulos numa linha só, e 80 ícones de categoria
**Status:** aceita · 2026-11
**Contexto:** num aparelho com tela mais estreita ou fonte maior, o saldo total do Início perdia o último dígito (uma linha cortada), o saldo das contas e o título de Configurações quebravam em duas linhas. O mantenedor pediu que nada disso aconteça: o que não cabe diminui. Pediu também uma variedade bem maior de ícones de categoria.
**Decisão:**
- `FitText` (`:core:designsystem`): uma linha, sem quebra e sem corte; a fonte desce de 1 em 1 sp até um mínimo (11 sp nos valores, 16 sp nos títulos da barra, 24 sp na saudação) e só abaixo dele vira reticências. Usado em todo valor (dinheiro, km, percentuais, contagens), nos títulos da barra superior (grande e pequena), na saudação do Início, nos títulos e rótulos de cartão e no `KpiTile` (rótulos até 8 sp). O valor à direita de uma linha de lista (`RowValue`) tem no máximo 96 dp: um valor grande diminui em vez de espremer o nome e a linha de apoio em várias linhas. Nomes e frases (título de livro, descrição de transação, textos de apoio) continuam podendo ocupar duas linhas.
- Ícones de categoria: 80 nomes do Lucide em oito grupos (casa, alimentação, transporte, saúde e cuidado, lazer, pessoal e família, trabalho e estudo, dinheiro e contas), os mesmos da web (D45 do servidor; o ícone continua um nome livre na API, sem mudança no contrato). A folha da categoria mostra sete escolhas rápidas (mais o ícone atual) e "Mais ícones", que troca o conteúdo da folha pelo seletor completo (`IconChooser`): os grupos e uma busca pelo nome em pt-BR que ignora acentos ("farmacia" acha Farmácia). Cada nome é desenhado pelo Material Symbol mais próximo (`ServerIcons`). `ticket` (Lazer) passou a ser o ingresso (`confirmation_number`), e `party-popper` (Festas) ficou com o confete. Uma categoria nova começa com `tag` (Outros), o padrão do servidor.
**Consequências:** um valor muito longo numa tela estreita fica menor, nunca cortado nem em duas linhas. Um ícone novo precisa entrar no app (`ServerIcons`, `CategoryIconGroups`, o rótulo `finance_icon_*`) e na web; um cliente que não o conhece mostra o ícone genérico.

## D24 — Estudos e Hábitos sem prancha
**Status:** aceita · 2026-11
**Contexto:** os módulos Estudos (`studies`, D46 do servidor) e Hábitos (`habits`, D47) não têm pranchas no design v1. O link do Obsidian precisa abrir outro app; os dois módulos precisam de um calendário e de uma seleção de dias da semana que o design system não tinha.
**Decisão:** as telas são compostas com o design system, como na paridade sem prancha (D20): Estudos abre em abas em pílula (Hoje, Semana, Matérias, Plano) com a toolbar "Registrar estudo"; Hábitos, em Hoje e Hábitos. Os componentes novos vão para o `:core:designsystem` com previews: `WeekdayChips` (dias da semana), `DayStrip` (os 7 dias com status) e, na Fase 7, `MonthCalendar`. O link do Obsidian abre por `Intent.ACTION_VIEW` com a URI; sem app que a abra, um snackbar diz isso. Todo derivado (meta, feito, status, semana, minutos por semana; taxa, sequência, dias sem) vem do servidor; "terminei agora" é só o valor padrão do campo de início (agora menos a duração). As fases entram no ROADMAP como 6 e 7, feitas antes do restante das Fases 4 e 5 por decisão do mantenedor.
**Consequências:** nenhum cálculo no app; com o cache (D21) as telas abrem com a última resposta e uma gravação faz a leitura seguinte perguntar ao servidor. A fila de pendentes continua na Fase 4.
**Na Fase 7:** Hábitos abre em Hoje (marcar com um toque, a quantidade com − e +, enviada depois de uma pausa curta para que uma sequência de toques seja uma gravação) e Hábitos; o detalhe tem o `MonthCalendar`, em que um dia até hoje é marcado com um toque (um dia futuro não é tocável) e a quantidade de um dia passado abre uma folha. Marcar usa o "hoje" que o servidor manda (`/habits/today`, o calendário), nunca o relógio do aparelho; os campos de data das folhas só começam no hoje do fuso do usuário. Os ícones de hábito são a lista da web (nomes livres, D33 do servidor) desenhada por `ServerIcons`, que ganhou `moon` e `sun`.

## D25 — APK assinado anexado a cada release do GitHub
**Status:** aceita · 2026-11
**Contexto:** o build de release era assinado com a chave de debug (a publicação estava em "Ideias" do ROADMAP). O mantenedor pediu um CI que gere o APK a cada release publicada e o anexe a ela, como no Downtify Android.
**Decisão:** `.github/workflows/release.yml` roda quando uma release é publicada (ou à mão, com a tag, em *Run workflow*): confere que a tag é `v<versionName>` de `app/build.gradle.kts`, monta `:app:assembleRelease` com a chave de release, confere com o `apksigner` que o APK não saiu com a chave de debug e anexa `prumo-<tag>.apk` e o `.sha256` à release; o `mapping.txt` do R8 fica 90 dias como artefato do run, sem publicar. A chave vem de quatro secrets do repositório (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`), que o workflow passa ao Gradle como `PRUMO_KEYSTORE_FILE`, `PRUMO_KEYSTORE_PASSWORD`, `PRUMO_KEY_ALIAS` e `PRUMO_KEY_PASSWORD`. Sem `PRUMO_KEYSTORE_FILE` (um `assembleRelease` local, o CI dos pull requests), o release continua assinado com a chave de debug.
**Consequências:** publicar uma versão é subir `versionName` e `versionCode`, e publicar a release com a tag `v<versionName>`. A chave de release não pode se perder nem mudar: um APK assinado com outra chave não atualiza o app instalado. Um APK de release montado localmente não atualiza o publicado (e vice-versa). F-Droid continua em "Ideias".

---

## Modelo

```
## Dxx — Título
**Status:** proposta | aceita | substituída por Dyy · AAAA-MM
**Contexto:** …
**Decisão:** …
**Consequências:** …
```
