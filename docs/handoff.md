# Tarefa original (2026-11-03)

Texto da tarefa que iniciou este repositório, guardado para quem continuar o trabalho. O design já foi importado para `design/v1/`: não reimporte.

Algumas premissas do texto não se confirmaram na Etapa 1: o `CLAUDE.md` deste repositório não existia e o do servidor não tinha a seção "Servidor e app juntos". Os dois foram escritos no início da Etapa 2 (D01 em `docs/decisoes.md`). O plano aprovado da Etapa 1 foi distribuído entre o `CLAUDE.md`, `docs/arquitetura.md`, `docs/decisoes.md` e o `ROADMAP.md`.

---

# Prumo Android — início do desenvolvimento

## Contexto
Este handoff contém o design do app Android do Prumo, feito com o design system "Henrique Sebastião — Android Design System" sobre Material 3 Expressive.

- App: `~/git/prumo-android`. Trabalhe a partir daqui. O `CLAUDE.md` deste repositório já define stack, módulos Gradle, arquitetura e regras.
- Servidor: `~/git/prumo`, com API FastAPI e web Vue já funcionando. O contrato está em `openapi/openapi.json`, `docs/api.md` (autenticação, formatos, erros, rotas) e `docs/dominio.md` (regras de negócio).
- O app é um cliente da mesma API da web: tem as mesmas funcionalidades e nenhum cálculo de regra de negócio. Valores derivados vêm prontos da API.
- Servidor e app avançam em paralelo. As regras estão na seção "Servidor e app juntos" do `CLAUDE.md` dos dois repositórios. Lacunas do servidor que o app revelar são implementadas em `~/git/prumo`, seguindo o `CLAUDE.md` de lá.
- Não faça commit, stage nem push em nenhum dos dois repositórios. Descreva tudo em `commit-message.md`.

## Etapa 1 — Entender e planejar (não escreva código ainda)
1. Leia `~/git/prumo-android/CLAUDE.md`, `~/git/prumo/CLAUDE.md`, `~/git/prumo/ROADMAP.md` e, em `~/git/prumo/docs/`, os arquivos `api.md`, `dominio.md`, `arquitetura.md` e `decisoes.md`. Confira também `openapi/openapi.json`.
2. Salve o conteúdo deste handoff em `design/v1/` sem alterar nada e crie `design/README.md` indicando a versão ativa.
3. Faça o inventário do design:
   - telas e fluxos de navegação (incluindo como os módulos aparecem na navegação e no Início);
   - componentes do design system usados e componentes novos;
   - tokens: cores claro e escuro, cores por módulo, tipografia, formas, espaçamento e movimento;
   - ícones.
4. Cruze o design com a API. Para cada tela, liste os endpoints e campos que ela usa. Depois liste:
   - o que o design pede e a API não oferece (por exemplo: estados offline e de sincronização, criação idempotente, `updated_since`, ou algum campo específico);
   - o que a API oferece e o design não mostra;
   - conflitos entre o design e as regras de `docs/dominio.md`. Para regras de negócio valem os docs; para aparência vale o design. Liste os conflitos em vez de resolvê-los.
5. Apresente:
   - a árvore de módulos Gradle e as dependências, conferindo as versões estáveis atuais;
   - um `ROADMAP.md` do app em fases: fundação → Veículos → Finanças → Livros → offline e sincronização → telas grandes e widget;
   - as mudanças necessárias no servidor e em qual fase do app cada uma entra;
   - como você vai verificar cada etapa, inclusive contra uma instância de desenvolvimento do servidor.

Pare aqui e espere minha aprovação.

## Etapa 2 — Fundação (depois da aprovação)
- Projeto conforme o `CLAUDE.md`:
  - `build-logic` com plugins de convenção e `libs.versions.toml`;
  - módulos `:core:*`, `:feature:*` vazios e `:app`;
  - detekt, spotless, CI e `.gitignore` com `commit-message.md`.
- `ROADMAP.md` aprovado e `docs/arquitetura.md` do app.
- `:core:designsystem`:
  - `MaterialExpressiveTheme` com os tokens do design system nos temas claro e escuro e as cores por módulo;
  - componentes base do design com previews `@PhonePreviews`.
- Rede e sessão:
  - endereço do servidor configurável, com teste de conexão (`GET /api/health`);
  - login com `client: "app"` e `device_name`;
  - token criptografado e bearer em todas as requisições;
  - 401 encerra a sessão;
  - erros mapeados pelo `code`.
- Contrato `PrumoFeature` e montagem dos módulos ativos e da ordem vindos de `GET /api/v1/modules`.
- Telas de sistema:
  - primeiro acesso (boas-vindas, servidor, login);
  - shell de navegação conforme o design;
  - Início com os resumos de `GET /api/v1/dashboard`;
  - Configurações: conta, tema, módulos ativos e ordem, sessões ativas, servidor, sair.
- Estados de carregando, vazio, erro, servidor inacessível e sessão expirada.
- Verificação real: emulador contra `make dev` em `~/git/prumo`, com usuário criado por `make user` e acesso por `http://10.0.2.2:8000`. Compare com as imagens de `design/v1/`.

## Ao terminar a fundação
Entregue a resposta final no formato do `CLAUDE.md`: como testar, onde aparece na interface, e os repositórios alterados com seus `commit-message.md`. Inclua também desvios do plano, pendências e perguntas. Não comece o módulo de Veículos.
