# Roadmap

As fases são executadas em ordem e cada uma termina com uma revisão antes da próxima começar. Um item só é marcado quando `./gradlew build detekt spotlessCheck` está verde, a tela foi conferida no emulador contra uma instância de desenvolvimento do servidor e comparada com `design/v1/`, e a documentação afetada foi atualizada. Itens marcados com **(servidor)** são feitos em `~/git/prumo`, seguindo o `CLAUDE.md` de lá.

## Fase 0 — Fundação

### Repositório e ferramentas
- [x] `design/v1/` com a exportação do Claude Design e `design/README.md` indicando a versão ativa
- [x] `CLAUDE.md` do app (stack, módulos, arquitetura, regras, "Servidor e app juntos", resposta final)
- [x] `build-logic` com plugins de convenção e `gradle/libs.versions.toml`
- [x] Módulos `:core:*`, `:feature:*` (vazios) e `:app`; regra de build que impede `:feature:*` de depender de outro `:feature:*`
- [x] detekt (com compose-rules), Spotless (ktlint), Android lint, `.editorconfig`, `.gitignore` com `commit-message.md`
- [x] CI no GitHub Actions: Spotless, detekt, build, testes de unidade
- [x] `docs/arquitetura.md` e `docs/decisoes.md` do app
- [x] **(servidor)** Seção "Servidor e app juntos" no `CLAUDE.md` e `commit-message.md` no `.gitignore`
- [x] **(servidor)** `service: "prumo"` no `GET /api/health`, para reconhecer um servidor Prumo (A5)

> **Fundação implementada em 2026-11-04 (sessão na nuvem) e verificada em 2026-11-09 (sessão local):** emulador `Pixel_9` (API 37) contra o `e2e_server.py` do servidor, telas comparadas com as pranchas renderizadas por `scripts/render-design.mjs`. Fluxos exercitados: servidor errado e certo (com a permissão de rede local), senha errada, login, token cifrado no DataStore, Início claro e escuro, Todas as ações, Configurações e as cinco subtelas, reordenar módulos por arraste, sessão expirada (sessão revogada pela API) e volta à mesma tela depois do login, servidor inacessível com dados em cache, sair. Não exercitados no emulador: troca de nome e de senha, estado vazio (só nos previews e testes). A verificação corrigiu: o círculo do rodapé do primeiro acesso, o `StatusChip` sem ícone, a app bar grande sem o título de 48 sp, os avatares das linhas sem o tom primary, os cards do Início, as abas em pílula, a toolbar dos módulos, o Campo de valor (D18), a barra de status no tema escuro da conta, o arraste dos módulos que não terminava e um build incremental que deixava o app sem Finanças (D17).

> **Paridade sem prancha feita em 2026-11-09 (sessão local, D20),** verificada no emulador `Pixel_9` (API 37) contra o `e2e_server.py`: "Vem aí" e "Atividade recente" no Início (claro e escuro), com o toque abrindo o veículo, a transação e o cartão; "Exportar dados" gravou um `.zip` válido em Downloads (`data.json` e os CSVs) e, com o servidor parado, mostrou "recusou a conexão" e apagou o arquivo vazio. O servidor passou a mandar `remaining_cents` em `finance.invoice_due`, para o app não subtrair. Não exercitados no emulador: o toque num item de Livros e de manutenção (cobertos pelos testes das descrições sobre o `dashboard.json` gravado).

### Design system (`:core:designsystem`)
- [x] Tema: `MaterialExpressiveTheme` + `MotionScheme.expressive()`, esquemas claro, escuro e preto puro do seed do app, cor dinâmica, success/warning harmonizados
- [x] `ModuleTheme(seed)`: esquema aninhado por módulo (e por conta e categoria), harmonizado com a cor dinâmica
- [x] Tipografia (Google Sans Flex arredondada, hero largo, dados em JetBrains Mono), formas, espaço, elevação
- [x] Ícones Material Symbols Rounded como vetores, e a tabela de ícones Lucide do servidor → Material Symbols
- [x] Componentes base usados pela fundação, cada um com `@PhonePreviews` (claro e escuro, 360 e 412 dp)
- [x] Novos: diálogo de confirmação, grade de ações rotuladas, linha reordenável

### Rede e sessão
- [x] Endereço do servidor configurável e teste de conexão (`GET /api/health`, versão e tempo de resposta, mensagem por causa)
- [x] Login com `client: "app"` e `device_name`; token criptografado (Tink + DataStore); bearer em todas as requisições
- [x] 401 encerra a sessão; erros mapeados pelo `code` para textos em pt-BR
- [x] Permissão de rede local do Android 17 (`ACCESS_LOCAL_NETWORK`) para servidores na LAN; HTTP sem TLS permitido para a instância do usuário

### Módulos e telas de sistema
- [x] Contrato `PrumoFeature` e montagem dos módulos ativos e da ordem a partir de `GET /api/v1/modules`
- [x] Primeiro acesso: boas-vindas, servidor, login
- [x] Shell: Início como raiz, toolbar flutuante com ações fixadas, folha "Todas as ações", engrenagem para Configurações
- [x] Início com os resumos de `GET /api/v1/dashboard` (um card por módulo, no tema do módulo)
- [x] Configurações: conta (nome e senha), aparência (tema, preto puro, cor dinâmica), módulos (ativar e reordenar), sessões ativas, servidor, sair
- [x] Paridade com a web, sem prancha: "Vem aí" e "Atividade recente" no Início, exportação dos dados
- [x] Estados de carregando, vazio, erro, servidor inacessível e sessão expirada
- [x] Verificação no emulador contra `make dev` e comparação com `design/v1/`

## Fase 1 — Veículos (`garage`)

> **Implementada em 2026-11-04 (sessão na nuvem) e verificada em 2026-11-09 (sessão local),** no emulador contra o `e2e_server.py`. Gravados pelo app e conferidos na API: veículo (cadastro, edição, arquivar pelo menu, desarquivar nos arquivados), abastecimento (cadastro e edição, com o erro `garage.odometer_inconsistent` mostrado no campo), leitura de km (cadastro e exclusão), manutenção (oficina pelo autocompletar, peças, mão de obra; edição e exclusão com confirmação), troca de óleo com a próxima sugerida. Vistos: estado vazio, oficinas, listas entre veículos. Pranchas comparadas: 01, 03 a 08, 10 e 12 (as escuras e a 11, rolada, usam os mesmos componentes). Não exercitados: cadastro e edição de oficina. Os conflitos C1–C5 e C9 seguem a web (D15): exclusões pedem confirmação e acontecem na hora; o snackbar com desfazer ficou para depois.
- [x] Lista, estado vazio, cadastro, edição e arquivamento
- [x] Detalhe: km atual, hero da troca de óleo, consumo e gastos, histórico em abas (manutenções, abastecimentos, óleo, km)
- [x] Folhas: atualizar km e abastecimento
- [x] Formulários: manutenção (autocompletar de oficina, lista de peças) e troca de óleo (próxima troca sugerida e editável)
- [x] Detalhe de manutenção, edição e exclusão com confirmação (o desfazer das pranchas ficou para depois, D15)
- [x] Paridade com a web, sem prancha (compor com os componentes do design system): combustível habitual e observações no cadastro, óleo utilizado com sugestões, ritmo de uso, detalhe e edição de abastecimento, exclusão de leitura manual, oficinas, listas entre veículos, arquivados
- [x] Card do Início e ações rápidas do módulo
- [x] Novos componentes: campo de valor, autocompletar, lista de peças dinâmica
- [x] Revisão visual contra `design/v1/`

## Fase 2 — Finanças (`finance`)

> **Implementada em 2026-11-04 (sessão na nuvem) e verificada em 2026-11-09 (sessão local),** no emulador contra o `e2e_server.py`. Gravados pelo app e conferidos na API: despesa, receita, transferência, compra parcelada em 3x (e a exclusão de todas as parcelas), edição e exclusão de transação, conta (cadastro, edição, ajuste de saldo, arquivamento), categoria (cadastro com ícone Lucide, exclusão sem uso e arquivamento, ambos com confirmação, e reativação), cartão (cadastro), pagamento parcial de fatura, navegação entre faturas. Pranchas comparadas: 01, 05 a 14 (02, 03 e 04 usam os mesmos componentes do Resumo e do estado vazio de Veículos). Não exercitados: edição de categoria e de cartão. As lacunas e conflitos seguem a web e a API como estão (D16): sem total por dia (A6), percentual inteiro (A7), sem origem do registro (A8) e o status da fatura lido da própria fatura (A10); nenhuma mudança no servidor. O total por dia continua à espera de aprovação.
- [x] Resumo do mês com seletor de período e gráfico por categoria
- [x] Contas: lista, cadastro, edição, arquivamento
- [x] Transações: lista por dia com busca e filtros (Paging 3), nova despesa, receita e transferência, detalhe, edição e exclusão
- [x] Categorias: lista, criar, editar, arquivar
- [x] Cartões: lista, detalhe com faturas, pagar fatura
- [x] Paridade com a web, sem prancha: cartão (cadastro e edição), compra parcelada, ajuste de saldo, detalhe de conta, arquivados, excluir sem uso, série de 6 meses e receitas por categoria
- [ ] **(servidor)** Totais por dia na lista de transações, se aprovado (A6)
- [ ] **(servidor)** Decidir percentual com uma casa (A7), origem do registro (A8) e status da fatura na transação (A10)
- [x] Card do Início e ações rápidas
- [x] Novos componentes: seletor de período, seletor de cor, gráfico por categoria
- [x] Revisão visual contra `design/v1/`

## Fase 3 — Livros (`reading`)

> **Implementada e verificada em 2026-11-09 (sessão local),** no emulador `Pixel_9` (API 37) contra o `e2e_server.py`. Gravados pelo app e conferidos na API: livro novo "Lendo" com página atual, "Tenho" e capa da galeria (o servidor guardou WebP 533×800) e outro com capa da câmera (WebP 600×800), edição do título, remoção da capa, "Marcar como lido" (status, término e 100% vindos do servidor, com a morfose de celebração), exclusão do livro; +1 e página digitada na folha (também pela ação rápida do Início, escolhendo entre dois livros em leitura); "Começar a ler"; anotação nova, editada (texto e data) e excluída. Também: filtros de status e "Tenho", ordenação por título, busca por autor, todas as anotações com busca, estante vazia, tema escuro, card do Início atualizado depois de gravar. Pranchas comparadas: 01 a 07. Decisões em D19: prévia da folha de progresso (C6), capa desenhada com título e autor (R6), Coil sobre o cliente da sessão, data da anotação. A verificação corrigiu: a câmera que derrubava o app (o `FileProvider` sem os caminhos no manifest), a busca da estante que abria fora da tela, a folha de anotação e a de progresso fora do layout das pranchas, o sufixo da unidade do Campo de valor ("de 425", "km") no mesmo tamanho do número, o plural de "Avançar 1 página" e o ritmo "aparece depois…" num livro já lido. Não exercitados no emulador: "Terminei" na folha (manda a última página, como "Marcar como lido"; coberto pelo teste do ViewModel) e a capa recusada pelo servidor (só no teste de tela).
- [x] Estante com filtros, hero "lendo agora", estado vazio
- [x] Cadastro com capa (câmera ou galeria), edição e exclusão
- [x] Detalhe com progresso rápido (+1, +10, +25), anotações
- [x] Folhas: atualizar progresso ("Terminei" com a confirmação expressiva) e nova anotação
- [x] Card do Início e ações rápidas
- [x] Paridade com a web, sem prancha: busca, ordenação, todas as anotações, editar e excluir anotação (com a data), ritmo, previsão e páginas por semana, remover capa
- [x] Paridade com a web que faltava: filtro "Não tenho" e o ano dos lidos (D20; no emulador, com um lido de 2025 criado pela API)
- [x] Novo componente: campo multilinha (e `CelebrationAvatar`, a morfose do herói)
- [x] Revisão visual contra `design/v1/`

## Fase 4 — Offline e sincronização

> **Cópia no aparelho feita em 2026-11-10 (sessão local, D21),** verificada no emulador `Pixel_9` (API 37) contra o `e2e_server.py`: com o servidor no ar, Início, Veículos (lista e detalhe), as abas de Finanças e Livros (estante e detalhe) encheram a tabela `responses` (13 respostas, uma conta); com o servidor parado e o app reaberto, todas essas telas abriram com os dados, o Início mostrou "Servidor inacessível" e "Dados de hoje, 04:58" depois do pull-to-refresh, e um veículo nunca aberto mostrou o erro de servidor inacessível. Com o servidor de volta: +1 página no livro apareceu na hora na estante e no Início; uma leitura de km gravada pela API apareceu na lista de veículos pela conferência em segundo plano; Sair apagou as respostas guardadas (15 → 0). A verificação corrigiu: a leitura do Room na thread principal (o Início e o pull-to-refresh derrubavam o app) e o cartão de servidor inacessível escondido acima do card hero.
- [x] Room como cache: telas leem do banco local e atualizam por pull-to-refresh (D21: a última resposta de cada `GET`, inteira, por conta)
- [ ] Sincronização incremental com `updated_since` e `/sync/deletions` (refaz a carga completa em `410`)
- [ ] Fila de registros pendentes com ids UUID v7 do app (criação idempotente) e envio pelo WorkManager quando há rede
- [ ] Estados: sem conexão com pendentes, servidor inacessível com dados em cache, ícone de nuvem no Início, pendentes em Sair e Trocar servidor
- [ ] **(servidor)** Controle de concorrência em edições, se aprovado (A2)

## Fase 5 — Telas grandes e widget
- [ ] `NavigationSuiteScaffold` com rail a partir de 600 dp
- [ ] Início em grade larga; transações em lista e detalhe (`ListDetailPaneScaffold`)
- [ ] Widget Glance 4×2 e 2×2 com a próxima troca de óleo e atalhos para as folhas de registro
- [ ] Revisão em tablet e dobrável

## Fase 6 — Estudos (`studies`)
Aprovado em 2026-11-10 (D24; D46 do servidor), feita antes do restante das Fases 4 e 5 por decisão do mantenedor. Sem prancha: telas compostas com o design system.
- [x] `:feature:studies`: DTOs e contrato, repositório, Hoje, Semana, Matérias (áreas, detalhe da matéria com tópicos, Obsidian e minutos por semana), Plano (versões e editor), folha "Registrar estudo"
- [x] Card do Início, "Atividade recente" e ação rápida
- [x] Novos componentes: `WeekdayChips`, `DayStrip`
- [x] Testes (contrato, ViewModels, telas) e verificação no emulador: `Pixel_9` (API 37) contra o `e2e_server.py` (Hoje, Semana e a anterior, Matérias, matéria, Plano, edição da versão futura, nova versão recusada na data ocupada e criada em outra, exclusão, sessão com tópico novo atualizando Hoje, Semana e Início) e contra `make dev` com um usuário novo (área, matéria com link recusado e corrigido, plano a partir de hoje, sessões pela toolbar e pela ação rápida do Início, módulo desligado e religado em Configurações); claro e escuro, 360 dp e fonte 130 %; "Abrir no Obsidian" sem o app mostra o aviso (com o Obsidian instalado não foi verificado: não há APK sem a Play Store) — 2026-11-11

## Fase 7 — Hábitos (`habits`)
Aprovado em 2026-11-10 (D24; D47 do servidor), só depois de alguns dias de uso de Estudos.
- [x] `:feature:habits`: Hoje, Hábitos, detalhe com calendário do mês, editor, folhas de marcação, frequência, pausa e recaída
- [x] Card do Início e ação rápida "Marcar hábitos"
- [x] Novo componente: `MonthCalendar`
- [x] Testes (contrato, repositório com fixtures gravadas, ViewModels, telas) e verificação no emulador: `Pixel_9` (API 37) contra o `e2e_server.py` (Hoje, marcar e desmarcar, quantidade, lista, detalhe com calendário e um dia passado desmarcado e marcado de novo, outubro com a pausa, nova versão da frequência recusada antes do hoje do servidor e criada em 23/11, pausa aberta encerrada e excluída, recaída registrada e excluída) e contra `make dev` com o usuário de Estudos (um hábito `check`, um de quantidade e um a abandonar criados do zero, marcar e desmarcar, início mudado para 01/11 e um dia passado marcado, dia futuro sem toque, frequência 3× por semana a partir de hoje, pausa, recaída, ação rápida no Início, módulo desligado e religado); claro e escuro, 360 dp e fonte 130 % — 2026-11-11

## Ideias (sem ordem definida)

- Pranchas para as telas de paridade com a web que o design v1 não cobre (`docs/design-v1.md`, seção 7)
- Publicação (assinatura, CI de release, F-Droid)
