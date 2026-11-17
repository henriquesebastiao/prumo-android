# Prumo para Android

App Android nativo do [Prumo](https://github.com/henriquesebastiao/prumo), o sistema open source e self-hosted de organização pessoal em módulos: finanças, veículos, livros, estudos e hábitos.

O app é um cliente do seu servidor Prumo: conecta na mesma API da interface web, tem as mesmas funcionalidades e não calcula nenhuma regra de negócio. Saldos, faturas, consumo, progresso de leitura e sequências chegam prontos do servidor.

**Situação:** versão 0.2.0, com todos os módulos do servidor. O projeto está completo para o uso pretendido; novas funcionalidades podem chegar quando surgirem novas ideias (veja o [ROADMAP](ROADMAP.md)). A interface está em português do Brasil.

## Funcionalidades

**Início.** Um card por módulo, cada um nas cores do seu módulo, com o resumo do momento; "Vem aí" e "Atividade recente"; ações rápidas na barra flutuante e na folha "Todas as ações". Folhas de registro rápido (abastecimento, despesa, progresso de leitura, sessão de estudo, marcar hábitos) abrem sobre a tela atual.

**Finanças.** Resumo do mês com seletor de período e gráfico por categoria; contas, ajuste de saldo e arquivamento; transações por dia com busca e filtros; receita, despesa, transferência e compra parcelada; categorias; cartões com faturas e pagamento.

**Veículos.** Lista e detalhe com km atual, status da troca de óleo, consumo e gastos; histórico em abas (manutenções, abastecimentos, óleo, km); folhas de atualizar km e de abastecimento; manutenção com oficina e peças; troca de óleo com a próxima sugerida; oficinas e veículos arquivados.

**Livros.** Estante com filtros, busca e "lendo agora"; capa pela câmera ou pela galeria; progresso rápido (+1, +10, +25) e a confirmação de "Terminei"; ritmo e previsão de término; anotações com busca.

**Estudos.** Hoje, Semana, Matérias (com tópicos, minutos por semana e "Abrir no Obsidian") e Plano semanal versionado; folha "Registrar estudo".

**Hábitos.** Hoje agrupado (com horário e a marca de "agora", sem horário, neste período, feitos, dias anteriores) com "Desfazer"; lista com busca, filtros e reordenação por arraste; detalhe com calendário do mês; frequências, pausas e recaídas. Marque hábitos também pelo card do Início, por um atalho no ícone do app e por um **widget na tela inicial** com os pendentes de hoje.

**Sistema.** Primeiro acesso com o endereço do servidor e o teste de conexão; Configurações com conta (nome e senha), aparência (tema claro, escuro ou preto puro e cor dinâmica), módulos (ligar, desligar e reordenar), sessões ativas, servidor e exportação dos dados em `.zip`.

**Sem conexão.** O app guarda a última resposta de cada tela no aparelho: com o servidor fora do ar, as telas já abertas antes continuam mostrando os dados, com o aviso de servidor inacessível. Registrar algo ainda exige o servidor.

## Instalação

Você precisa de um servidor Prumo rodando (veja [a instalação do servidor](https://github.com/henriquesebastiao/prumo#instala%C3%A7%C3%A3o-self-host)) e de um usuário criado nele.

1. Baixe o APK da [última release](https://github.com/henriquesebastiao/prumo-android/releases/latest). Cada release traz o APK assinado e o arquivo `.sha256` para conferir: `sha256sum -c prumo-*.apk.sha256`.
2. Instale o APK no celular (o Android pede permissão para instalar apps de fontes desconhecidas).
3. Abra o app, informe o endereço do servidor (por exemplo, `https://prumo.exemplo.com` ou `http://192.168.0.10:8080`) e toque em testar: o app confere se ali está um servidor Prumo e mostra a versão.
4. Entre com o seu usuário e senha.

Requer Android 8.0 (API 26) ou mais recente. No Android 17 ou mais recente, um servidor na rede local exige a permissão de rede local, que o app pede antes de testar o endereço. Endereços `http://` sem TLS são aceitos para a sua instância, mas prefira HTTPS fora de casa.

O app guarda o token da sessão criptografado no aparelho e o envia só ao seu servidor.

## Desenvolvimento

Requisitos: JDK 17 ou 21 e o Android SDK com a plataforma 37 (o Android Studio instala tudo). Para testar contra um servidor, um checkout do [servidor](https://github.com/henriquesebastiao/prumo).

```bash
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
./gradlew build detekt spotlessCheck   # compila, testes de unidade, Android lint, detekt e ktlint (o mesmo do CI)
./gradlew spotlessApply                # formata
./gradlew installDebug                 # instala o app de debug (com.henriquesebastiao.prumo.debug)
```

Para um servidor de desenvolvimento, rode `make dev` e `make user` no repositório do servidor; no emulador, o endereço é `http://10.0.2.2:8000`. Para dados de demonstração, `uv run python scripts/e2e_server.py` em `prumo/backend` sobe uma instância na porta 8001 com o usuário `henrique` e a senha `prumo-demo-01`.

Os scripts de `scripts/` copiam o contrato do servidor (`update-openapi.sh`), gravam respostas reais para os testes (`record-fixtures.py`), geram ícones do Material Symbols (`material_symbol.py`) e renderizam as pranchas do design para comparação (`render-design.mjs`). Detalhes do ambiente, do emulador e da release assinada em [docs/arquitetura.md](docs/arquitetura.md).

## Tecnologias

Kotlin, Jetpack Compose, Material 3 Expressive, Navigation 3, Hilt, Retrofit, OkHttp, kotlinx.serialization, Room, DataStore com Tink, Paging 3, Coil e Glance. Testes com JUnit 4, Turbine, MockWebServer e Robolectric; qualidade com detekt, ktlint (Spotless) e Android lint.

## Estrutura do repositório

```
prumo-android/
├── app/           Activity, navegação, telas de sistema (primeiro acesso, Início, Configurações)
├── core/          model, network, data, database, designsystem, ui, feature, testing
├── feature/       um módulo por módulo do Prumo: finance, garage, reading, studies, habits
├── build-logic/   plugins de convenção do Gradle
├── design/        exportações do design (só referência visual)
├── config/        regras do detekt
├── scripts/       contrato, fixtures, ícones e render das pranchas
└── docs/          documentação técnica
```

## Documentação

- [Arquitetura](docs/arquitetura.md): módulos Gradle, camadas, rede, sessão, navegação e o contrato `PrumoFeature`
- [Decisões técnicas](docs/decisoes.md)
- [Design v1](docs/design-v1.md): inventário das telas, telas × API, lacunas e conflitos
- [Tokens e componentes](docs/referencias/compose-mapping.md)
- [API do servidor](https://github.com/henriquesebastiao/prumo/blob/main/docs/api.md) e [regras de negócio](https://github.com/henriquesebastiao/prumo/blob/main/docs/dominio.md)

## Contribuindo

Issues e sugestões são bem-vindas. Antes de abrir um pull request, leia a [arquitetura](docs/arquitetura.md). Uma funcionalidade nova que precise de dados que a API não oferece começa no [servidor](https://github.com/henriquesebastiao/prumo).

---

Feito por [Henrique Sebastião](https://henriquesebastiao.com).
