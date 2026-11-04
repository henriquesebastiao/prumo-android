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

> **Fundação implementada em 2026-11-04 (sessão na nuvem, sem Android SDK local).** Os itens de tela abaixo estão implementados e passam no CI (build, lint, testes de unidade e Robolectric), mas só são marcados depois da conferência no emulador contra `make dev` e da comparação com `design/v1/`, que ficam com o mantenedor. "Vem aí", "Atividade recente" e a exportação dos dados ainda não foram feitos.

### Design system (`:core:designsystem`)
- [ ] Tema: `MaterialExpressiveTheme` + `MotionScheme.expressive()`, esquemas claro, escuro e preto puro do seed do app, cor dinâmica, success/warning harmonizados
- [ ] `ModuleTheme(seed)`: esquema aninhado por módulo (e por conta e categoria), harmonizado com a cor dinâmica
- [ ] Tipografia (Google Sans Flex arredondada, hero largo, dados em JetBrains Mono), formas, espaço, elevação
- [ ] Ícones Material Symbols Rounded como vetores, e a tabela de ícones Lucide do servidor → Material Symbols
- [ ] Componentes base usados pela fundação, cada um com `@PhonePreviews` (claro e escuro, 360 e 412 dp)
- [ ] Novos: diálogo de confirmação, grade de ações rotuladas, linha reordenável

### Rede e sessão
- [ ] Endereço do servidor configurável e teste de conexão (`GET /api/health`, versão e tempo de resposta, mensagem por causa)
- [ ] Login com `client: "app"` e `device_name`; token criptografado (Tink + DataStore); bearer em todas as requisições
- [ ] 401 encerra a sessão; erros mapeados pelo `code` para textos em pt-BR
- [ ] Permissão de rede local do Android 17 (`ACCESS_LOCAL_NETWORK`) para servidores na LAN; HTTP sem TLS permitido para a instância do usuário

### Módulos e telas de sistema
- [ ] Contrato `PrumoFeature` e montagem dos módulos ativos e da ordem a partir de `GET /api/v1/modules`
- [ ] Primeiro acesso: boas-vindas, servidor, login
- [ ] Shell: Início como raiz, toolbar flutuante com ações fixadas, folha "Todas as ações", engrenagem para Configurações
- [ ] Início com os resumos de `GET /api/v1/dashboard` (um card por módulo, no tema do módulo)
- [ ] Configurações: conta (nome e senha), aparência (tema, preto puro, cor dinâmica), módulos (ativar e reordenar), sessões ativas, servidor, sair
- [ ] Paridade com a web, sem prancha: "Vem aí" e "Atividade recente" no Início, exportação dos dados
- [ ] Estados de carregando, vazio, erro, servidor inacessível e sessão expirada
- [ ] Verificação no emulador contra `make dev` e comparação com `design/v1/`

## Fase 1 — Veículos (`garage`)
- [ ] Lista, estado vazio, cadastro, edição e arquivamento
- [ ] Detalhe: km atual, hero da troca de óleo, consumo e gastos, histórico em abas (manutenções, abastecimentos, óleo, km)
- [ ] Folhas: atualizar km e abastecimento
- [ ] Formulários: manutenção (autocompletar de oficina, lista de peças) e troca de óleo (próxima troca sugerida e editável)
- [ ] Detalhe de manutenção, edição e exclusão com confirmação e desfazer
- [ ] Paridade com a web, sem prancha (compor com os componentes do design system): combustível habitual e observações no cadastro, óleo utilizado com sugestões, ritmo de uso, detalhe e edição de abastecimento, exclusão de leitura manual, oficinas, listas entre veículos, arquivados
- [ ] Card do Início e ações rápidas do módulo
- [ ] Novos componentes: campo de valor, autocompletar, lista de peças dinâmica
- [ ] Revisão visual contra `design/v1/`

## Fase 2 — Finanças (`finance`)
- [ ] Resumo do mês com seletor de período e gráfico por categoria
- [ ] Contas: lista, cadastro, edição, arquivamento
- [ ] Transações: lista por dia com busca e filtros (Paging 3), nova despesa, receita e transferência, detalhe, edição e exclusão
- [ ] Categorias: lista, criar, editar, arquivar
- [ ] Cartões: lista, detalhe com faturas, pagar fatura
- [ ] Paridade com a web, sem prancha: cartão (cadastro e edição), compra parcelada, ajuste de saldo, detalhe de conta, arquivados, excluir sem uso, série de 6 meses e receitas por categoria
- [ ] **(servidor)** Totais por dia na lista de transações, se aprovado (A6)
- [ ] **(servidor)** Decidir percentual com uma casa (A7), origem do registro (A8) e status da fatura na transação (A10)
- [ ] Card do Início e ações rápidas
- [ ] Novos componentes: seletor de período, seletor de cor, gráfico por categoria
- [ ] Revisão visual contra `design/v1/`

## Fase 3 — Livros (`reading`)
- [ ] Estante com filtros, hero "lendo agora", estado vazio
- [ ] Cadastro com capa (câmera ou galeria), edição e exclusão
- [ ] Detalhe com progresso rápido (+1, +10, +25), anotações
- [ ] Folhas: atualizar progresso ("Terminei" com a confirmação expressiva) e nova anotação
- [ ] Card do Início e ações rápidas
- [ ] Paridade com a web, sem prancha: busca, todas as anotações, editar e excluir anotação, ritmo, previsão e páginas por semana, remover capa
- [ ] Novo componente: campo multilinha
- [ ] Revisão visual contra `design/v1/`

## Fase 4 — Offline e sincronização
- [ ] Room como cache: telas leem do banco local e atualizam por pull-to-refresh
- [ ] Sincronização incremental com `updated_since` e `/sync/deletions` (refaz a carga completa em `410`)
- [ ] Fila de registros pendentes com ids UUID v7 do app (criação idempotente) e envio pelo WorkManager quando há rede
- [ ] Estados: sem conexão com pendentes, servidor inacessível com dados em cache, ícone de nuvem no Início, pendentes em Sair e Trocar servidor
- [ ] **(servidor)** Controle de concorrência em edições, se aprovado (A2)

## Fase 5 — Telas grandes e widget
- [ ] `NavigationSuiteScaffold` com rail a partir de 600 dp
- [ ] Início em grade larga; transações em lista e detalhe (`ListDetailPaneScaffold`)
- [ ] Widget Glance 4×2 e 2×2 com a próxima troca de óleo e atalhos para as folhas de registro
- [ ] Revisão em tablet e dobrável

## Ideias (sem ordem definida)

- Pranchas para as telas de paridade com a web que o design v1 não cobre (`docs/design-v1.md`, seção 7)
- Publicação (assinatura, CI de release, F-Droid)
