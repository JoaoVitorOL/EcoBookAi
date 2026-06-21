# EcoBook AI - Alinhamento Com A Avaliacao Do Professor

Base desta leitura:

- documento da disciplina extraido de `ABP_Projeto_Final_-_Solucoes_Mobile_-_UNISATC`
- codigo atual do Android, backend e documentacao do repositorio
- revisao executada em `2026-06-21`

## 1. Requisitos obrigatorios

Status geral: `atendidos no codigo atual`

- `No minimo 4 telas`: atendido com folga. O app possui, entre outras, `Auth`, `Onboarding`, `Discovery`, `Donate`, `Profile`, `MyRequests`, `DonorRequests`, `Notifications`, `DeleteAccount` e `MaterialUpload`.
- `Nome e icone do App personalizado`: atendido. O projeto usa a identidade `EcoBook AI` e possui recursos de launcher em `EcoBookAiAndroid/app/src/main/res/mipmap-*`.
- `Persistencia dos dados (local ou com API)`: atendido. O app persiste sessao/notificacoes localmente e o fluxo principal usa API propria com backend Spring Boot e banco relacional.
- `No minimo 2 operacoes no estilo CRUD`: atendido. Ha CRUD de materiais e operacoes claras de perfil/solicitacoes.
- `100% Android nativo com Kotlin e Jetpack Compose`: atendido. Nao encontrei React Native nem stack mobile nao autorizada.

## 2. Requisitos opcionais que o projeto complementa

Os opcionais abaixo sao os que o projeto complementa de forma real hoje:

- `Criar uma API online para persistencia dos dados ou utilizar o Firebase`
  O projeto tem API propria para autenticacao, perfil, materiais, solicitacoes e notificacoes. Tambem possui integracao com Firebase Cloud Messaging.
  Observacao importante: para reivindicar a palavra `online` sem margem, o backend precisa estar publicado em um host acessivel pela internet ate a apresentacao final.

- `Integracao com alguma API externa (Clima, chat gpt, etc.)`
  Atendido via `Google Gemini` no backend para preview/classificacao assistida de materiais.

Os opcionais abaixo `nao` estao claramente atendidos no estado atual:

- `Mapas e geolocalizacao`
- `Integracao com um App WEB ja existente`
- `Publicar o App na loja`
- `Salvar informacoes em arquivo (gerar pdf ou arquivo de texto)`
- `Utilizar o acelerometro ou giroscopio`

## 3. Qualidade e legibilidade do codigo

Leitura geral: o projeto `tende a pontuar bem` nesse criterio.

Pontos fortes:

- arquitetura separada em camadas no backend: `controller -> service -> repository`
- Android organizado em `screen/viewmodel/repository/api/dto`
- README raiz, README do Android, README do backend e docs complementares relativamente completos
- evidencias de testes, lint e analise estatica ja documentadas
- nomes de classes e responsabilidades geralmente claros
- boas validacoes de dominio, fluxo de sessao e tratamento de erro

Pontos de atencao:

- alguns arquivos cresceram bastante e aumentam custo de leitura, principalmente:
  - `EcoBookAiAndroid/app/src/main/kotlin/com/ecobook/ui/screens/ProfileScreen.kt`
  - `EcoBookAiAndroid/app/src/main/kotlin/com/ecobook/ui/screens/DonateScreen.kt`
  - `EcoBookAiAndroid/app/src/main/kotlin/com/ecobook/onboarding/OnboardingScreen.kt`
  - `EcoBookAiBackend/src/main/java/com/ecobook/service/GeminiService.java`
- ainda existe muita string de interface direto no codigo Compose, o que nao quebra o app, mas dificulta manutencao e localizacao futura
- a documentacao principal ja esta mais enxuta, mas ainda vale apontar `README.md`, os READMEs dos modulos e `docs/` como fonte atual de verdade na apresentacao

Melhorias aplicadas nesta rodada:

- a documentacao principal e o texto legal foram alinhados ao comportamento real do sistema

## 4. Riscos de desconto e impedimentos atuais

Riscos que `nao dependem do codigo`, mas precisam de acao do grupo:

- `Quantidade de integrantes do grupo`: nao e verificavel pelo repositorio. Validem isso manualmente.
- `Publicar codigo + apresentacao no GitHub e enviar no portal`: tambem e acao externa ao codigo.
- `Apresentacao final`: o documento do professor pede organizacao, clareza e, de preferencia, slides com fundo claro. Confirmem isso no deck final.

Riscos tecnicos/operacionais que ainda merecem atencao:

- `Java 21+ no backend`: o backend falha se o ambiente estiver em Java 17. Isso precisa estar preparado antes da demo.
- `Backend online`: se a banca/professor esperar a interpretacao literal de `API online`, falta publicar o backend em host externo.
- `Firebase real`: o projeto suporta FCM, mas a validacao real depende de `google-services.json` e credenciais Firebase configuradas.
- `Gemini real`: o fluxo com IA depende de `GEMINI_API_KEY`. Sem isso, o projeto cai em mock/fallback local.

## 5. Como defender o projeto na avaliacao

Narrativa recomendada:

- o problema real e reduzir desperdicio de material escolar e facilitar o encontro entre doadores e responsaveis
- o app e `Android nativo com Kotlin + Jetpack Compose`
- a persistencia principal ocorre por `API propria + banco relacional`
- os extras mais fortes sao `API propria`, `Gemini` e `FCM`
- o cuidado com qualidade aparece em `arquitetura`, `testes`, `lint`, `checkstyle`, `documentacao` e `tratamento de erro`

Resumo final:

- obrigatorios do trabalho: `atendidos`
- opcionais complementados com seguranca: `API propria/Firebase`, `API externa`
- impedimentos reais restantes: `publicacao online do backend`, `segredos para demo real`, `passos externos de apresentacao/entrega`
