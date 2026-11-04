# Design

Exportações do Claude Design usadas como **referência visual**. Nada daqui é importado pelo código: os tokens e componentes do app vivem em `:core:designsystem`.

**Versão ativa: `v1/`**

| Versão | Conteúdo | Status |
|---|---|---|
| `v1/` | Projeto "Prumo Android" do Claude Design (`f1e4db47-a3d0-4a92-aef2-4d18e166ec42`), exportado em 2026-11-03: Fase 2, variante B, com o design system "Henrique Sebastião — Android Design System" (`_ds/`) | ativa |

## Arquivos de `v1/`

Ponto de entrada: **`Prumo Fase 2 - Handoff.dc.html`** (entrega para implementação: tema, componentes por tela, componentes novos, movimento e transições). Ele leva às pranchas:

| Arquivo | Pranchas |
|---|---|
| `Prumo Fase 2 - Inicio e acesso.dc.html` | Início (claro, escuro, todas as ações) e primeiro acesso (boas-vindas, servidor, servidor com erro, login) |
| `Prumo Fase 2 - Configuracoes e estados.dc.html` | Configurações, conta, aparência, módulos, sessões, servidor, sair; estados (carregando, erro, sem conexão, servidor inacessível, sessão expirada, exclusão, arquivamento, snackbar) |
| `Prumo Fase 2 - Financas.dc.html` | Resumo, contas, transações, categorias, cartões, fatura |
| `Prumo Fase 2 - Veiculos.dc.html` | Lista, cadastro, detalhe, atualizar km, abastecimento, manutenção, troca de óleo |
| `Prumo Fase 2 - Livros.dc.html` | Estante, cadastro, detalhe, progresso, anotação |
| `Prumo Fase 2 - Telas grandes.dc.html` | Início no tablet, transações em lista e detalhe, widget |
| `_ds/…/` | Design system: tokens CSS, `_ds_bundle.js` (componentes React das pranchas), fontes |
| `support.js`, `PrumoPhone.jsx`, `android-frame.jsx` | Runtime das pranchas e moldura do celular |

Também vieram no export, como histórico do projeto (não são referência): `Prumo Android - Fase 1.dc.html` (três estruturas de navegação), `Prumo Android - Fase 1 v2.dc.html`, `Prumo Android Fase 1.dc.html` (vazio), `Prumo Handoff export.dc.html` (mesmo texto da entrega da Fase 2) e `Prumo - Entrega para implementação.html` (página única empacotada).

## Como usar

- Abra os `.dc.html` no navegador. Eles carregam o `support.js` e o `_ds/` relativos à pasta e buscam a fonte Material Symbols no Google Fonts, então precisam de internet para os ícones.
- Cada celular (`PrumoPhone.jsx`) gera o esquema do seed em tempo de execução: app `#6A4F86` por padrão, Finanças `#2E6B4F`, Veículos `#1F5F8B`, Livros `#8A4B2E`. Os valores estáticos de `_ds/…/tokens/colors.css` são do seed do Netrik (`#136B79`) e não valem para o Prumo.
- Os arquivos de uma versão nunca são alterados. Um redesign entra como `v2/`, e esta tabela passa a indicar a nova versão ativa.
