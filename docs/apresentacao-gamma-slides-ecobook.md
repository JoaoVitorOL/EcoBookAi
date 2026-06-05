# Texto Dos Slides - Apresentacao Gamma - EcoBook AI

Baseado em:

- `README.md`
- `docs/architecture.md`
- `docs/testing.md`
- `docs/levantamento-tecnologias-ecobook.md`
- `specs/001-ecobook-core/PLAN-SUMMARY.md`
- PDF do professor `ABP_Projeto_Final_-_Solucoes_Mobile_-_UNISATC`

## Como usar este arquivo

- O Gamma deve gerar apenas o visual.
- O texto abaixo ja e o conteudo dos slides.
- Use fundo claro, texto escuro, poucos bullets e GIFs curtos como elemento principal nas demonstracoes.
- Duracao-alvo: ate `15 minutos`.
- Nao afirmar que o requisito de `icone personalizado` esta concluido no estado atual do repositorio.
- Os GIFs reais disponiveis hoje cobrem: login, area do doador, publicar novo, descoberta, detalhe do material, minhas solicitacoes, pedidos recebidos, notificacoes e perfil.
- Os pontos que nao aparecem diretamente nesses GIFs, como onboarding completo e consentimento de IA, devem entrar como texto de apoio ou fala da equipe.

---

## Slides Principais

### Slide 1 - EcoBook AI organiza a doacao de materiais de estudo

**Texto do slide**

- `EcoBook AI`
- Plataforma mobile para doacao e solicitacao de materiais de estudo
- Disciplina: `Solucoes Mobile`
- Equipe: `[nomes dos integrantes]`
- Semestre: `[preencher]`

**Observacao visual**

- Capa limpa, academica e tecnologica
- Um mockup de celular ou composicao com capas de livros

### Slide 2 - O problema nao e so falta de material, mas falta de organizacao

**Texto do slide**

- Muitas familias possuem livros e apostilas parados em casa
- Outras familias precisam desses materiais ao longo do ano letivo
- Hoje a doacao costuma acontecer de forma informal e desorganizada
- Faltam busca, filtros, acompanhamento e um fluxo minimo de seguranca

**Fecho do slide**

- O EcoBook nasce para organizar esse processo do inicio ao fim

### Slide 3 - Nossa solucao vai alem de um cadastro simples de livros

**Texto do slide**

- Aplicativo Android nativo para doadores e responsaveis legais
- Backend proprio para autenticar, persistir dados e controlar regras de negocio
- IA como apoio no cadastro dos materiais
- Notificacoes para acompanhar o andamento das solicitacoes

**Fecho do slide**

- O app organiza um fluxo completo de doacao, solicitacao e acompanhamento

### Slide 4 - O projeto atende os requisitos obrigatorios da disciplina

**Texto do slide**

| Requisito do professor | Situacao no EcoBook AI |
|---|---|
| Minimo de 4 telas | Atendido com folga |
| Persistencia de dados | Atendido com API propria e banco relacional |
| Minimo de 2 operacoes CRUD | Atendido com materiais, perfil e solicitacoes |
| 100% Android nativo com Kotlin e Jetpack Compose | Atendido |
| Nome do app personalizado | Atendido com `EcoBook` |
| Icone personalizado | Pendente no estado atual do repositorio |

**Fecho do slide**

- Alem do minimo, o projeto inclui backend proprio, push notifications, IA assistiva, consentimentos e exclusao de conta

### Slide 5 - Os extras existem de verdade e sustentam funcionalidades reais

**Texto do slide**

- `API propria para persistencia`
  - usada em autenticacao, perfil, materiais, solicitacoes, notificacoes e consentimentos
- `Firebase / FCM`
  - usado para push notifications sobre eventos do fluxo
- `Integracao com API externa - Gemini`
  - usada para sugerir metadados no cadastro do material

**Fecho do slide**

- Os extras nao foram adicionados como enfeite; eles apoiam funcionalidades centrais do sistema

### Slide 6 - O fluxo do sistema cabe bem em GIFs porque o produto tem caminho claro

**Texto do slide**

1. Login ou cadastro
2. Onboarding do adulto responsavel
3. Escolher entre doar ou buscar
4. Publicar material ou abrir uma solicitacao
5. Acompanhar aprovacao, reserva e entrega
6. Gerir perfil, sessao e notificacoes

**Observacao pequena**

- O contato externo por WhatsApp so aparece depois da aprovacao

### Slide 7 - O acesso inicial e simples, e o cadastro completo acontece em seguida

**Texto do slide**

- GIF principal: `login`
- Autenticacao com email e senha
- Entrada limpa, objetiva e facil de entender
- Quem ainda nao tem conta pode seguir para o cadastro
- No primeiro acesso, o app exige onboarding do adulto responsavel
- O perfil completo inclui CPF, cidade, bairro e consentimentos

**Fecho do slide**

- O sistema libera os fluxos protegidos somente depois da identificacao e do preenchimento essencial do perfil

### Slide 8 - A area do doador separa bem publicar, acompanhar e gerenciar

**Texto do slide**

- GIF principal: `area-do-doador`
- Aba `Meus materiais` para acompanhar o que ja foi publicado
- Aba `Pedidos recebidos` para responder ou finalizar reservas
- Aba `Publicar novo` para abrir um novo cadastro
- O menu do doador deixa o fluxo bem dividido por tarefa

**Fecho do slide**

- A usabilidade melhora porque o doador nao precisa misturar cadastro, acompanhamento e entrega na mesma tela

### Slide 9 - A publicacao comeca por imagens validas e um fluxo guiado

**Texto do slide**

- GIF principal: `publicar-novo`
- Capa da frente obrigatoria e capa de tras opcional
- Validacao de JPEG/PNG com limite de `5MB`
- Compressao quando necessario antes do envio
- Depois do upload, o backend pode sugerir metadados com IA e o usuario revisa tudo antes de publicar

**Fecho do slide**

- O fluxo de doacao combina validacao tecnica, apoio de IA e revisao humana

### Slide 10 - A descoberta funciona com lista clara, detalhe rico e CTA direto

**Texto do slide**

- GIF principal: `descoberta` + `detalhe-material`
- Cards mostram titulo, tags, local e estado do material
- A busca usa filtros academicos e contexto geografico
- O detalhe abre imagens, descricao, ano, sistema de ensino e necessidade academica
- A acao `Solicitar material` fica no fim do detalhe, sem poluir a listagem

**Fecho do slide**

- O usuario entende o material antes de solicitar, o que melhora a chance de uma solicitacao mais qualificada

### Slide 11 - O estudante acompanha o pedido desde o vazio ate a aprovacao

**Texto do slide**

- GIF principal: `minhas-solicitacoes-vazio` + `minhas-solicitacoes-aprovada`
- A tela vazia orienta o usuario antes da primeira solicitacao
- Depois da aprovacao, o card mostra status, local, reserva ativa e liberacao de contato
- O aluno consegue cancelar quando necessario
- O app deixa visivel a diferenca entre antes e depois da aprovacao

**Fecho do slide**

- Isso reforca usabilidade, porque o usuario acompanha o estado do pedido sem depender de memoria ou conversa externa

### Slide 12 - O doador recebe os pedidos e as notificacoes mantem o fluxo vivo

**Texto do slide**

- GIF principal: `pedidos-recebidos` + `notificacoes`
- O doador enxerga pedidos pendentes e pedidos aprovados em areas separadas
- Depois da aprovacao, ele pode concluir a entrega ou revogar a aprovacao
- A central de notificacoes registra eventos importantes do fluxo
- O Firebase FCM envia push, e o app tambem guarda uma inbox local para acompanhamento

**Fecho do slide**

- O sistema nao depende de um unico momento de uso; ele continua orientando as duas partes ate o fim da doacao

### Slide 13 - O perfil centraliza confianca, sessao e governanca da conta

**Texto do slide**

- GIF principal: `perfil`
- A visao geral deixa claro quem e o responsavel pela conta
- O usuario pode ver termos, trocar foto e revisar dados essenciais
- `Sair da conta` e `Excluir conta` ficam em destaque na propria area de perfil
- A tela reforca que entrega e retirada sao combinadas fora do app, pelo WhatsApp

**Fecho do slide**

- O perfil nao e decorativo; ele participa da privacidade, da transparencia e do controle da conta

### Slide 14 - No Android, cada tecnologia tem uma funcao clara

**Texto do slide**

| Tecnologia | Onde aparece | Para que foi usada | Por que fez sentido |
|---|---|---|---|
| `Kotlin` | Base do app Android | Implementacao nativa | Linguagem oficial, moderna e segura |
| `Jetpack Compose` | Login, onboarding, busca, doacao, perfil | Construir a UI | Menos boilerplate e melhor trabalho com estado |
| `Navigation Compose` | Fluxo entre telas | Navegacao | Integracao natural com Compose |
| `ViewModel + StateFlow` | Estado das telas | Separar UI e logica | Reatividade e testabilidade |
| `Coroutines` | Chamadas de rede e upload | Assincronismo | Solucao idiomatica do ecossistema Kotlin |
| `Hilt` | Injecao de dependencias | Organizar servicos e repositorios | Melhor manutencao e testes |
| `Retrofit + OkHttp + Gson` | Consumo da API | HTTP + JSON | Stack madura para Android |
| `EncryptedSharedPreferences` | Sessao local | Guardar JWT e dados sensiveis | Persistencia segura no dispositivo |

**Fecho do slide**

- O Android foi construido com uma stack nativa moderna e coerente com o que a disciplina pede

### Slide 15 - O backend sustenta a complexidade, a persistencia e a validacao

**Texto do slide**

| Tecnologia | Onde aparece | Para que foi usada | Por que fez sentido |
|---|---|---|---|
| `Spring Boot` | Base da API | Estruturar o backend | Produtividade e ecossistema robusto |
| `Spring Web` | Controllers REST | Expor endpoints HTTP | Padrao consolidado para APIs Java |
| `Spring Security + JWT` | Login e rotas protegidas | Autenticacao e autorizacao | Adequado para app mobile com API stateless |
| `JPA + Hibernate` | Persistencia | Mapear entidades e consultas | Menos codigo repetitivo |
| `PostgreSQL` | Banco principal | Persistencia relacional oficial | Banco robusto para dados transacionais |
| `H2` | Perfil local e testes | Apoio ao desenvolvimento | Ambiente leve e rapido |
| `Flyway` | Migracoes | Versionar schema | Evolucao controlada do banco |
| `Gemini + OkHttp` | Preview IA | Sugerir metadados do material | Integracao externa simples e funcional |
| `Firebase Admin SDK` | Envio de push | Disparar notificacoes | Integracao oficial com FCM |

**Fecho do slide**

- Evidencias tecnicas: `229` testes backend verdes, `84.72%` JaCoCo, Android `app:testDebugUnitTest` verde, lint Android com `0` erros e fluxo FCM validado
- Conclusao: o EcoBook resolve um problema real e vai alem de um CRUD simples
- Equipe: `[Nome] - Android/UI`, `[Nome] - Backend/API`, `[Nome] - Integracoes/Testes`, `[Nome] - Documentacao/Apresentacao`

---

## Slides De Apoio Para Perguntas Tecnicas

### Apoio 1 - Qual e o banco de dados do projeto

**Texto do slide**

- O banco principal do sistema e `PostgreSQL`
- Ele armazena usuarios, materiais, solicitacoes, notificacoes, consentimentos, auditoria e tokens revogados
- O backend tambem usa `H2` como apoio para desenvolvimento local e parte dos testes
- O schema e versionado com `Flyway`

**Resposta pronta**

- `PostgreSQL = banco principal`
- `H2 = banco auxiliar para local/teste`

### Apoio 2 - O app Android usa RoomDatabase

**Texto do slide**

- `Nao, o projeto nao usa RoomDatabase`
- Tambem nao usa `SQLiteOpenHelper`, `Realm`, `ObjectBox` ou `DataStore`
- A persistencia local no Android e feita com `EncryptedSharedPreferences`
- O app salva localmente JWT, sessao, dados basicos do usuario, token FCM e inbox local de notificacoes

**Resposta pronta**

- O banco oficial esta no backend
- No Android, a persistencia local atual e chave-valor criptografada

### Apoio 3 - Tecnologias Android usadas no projeto

**Texto do slide**

| Tecnologia | Finalidade | Como foi usada | Justificativa |
|---|---|---|---|
| `Kotlin` | Linguagem principal | Todo o app Android foi escrito em Kotlin | Linguagem oficial do Android |
| `Jetpack Compose` | Interface nativa declarativa | Telas, componentes e navegacao | Moderniza a UI e reduz boilerplate |
| `Material 3` | Padrao visual | Scaffold, barras, dialogs e componentes | Consistencia visual e acessibilidade |
| `Navigation Compose` | Navegacao | Fluxo auth, onboarding, abas e detalhes | Integracao natural com Compose |
| `ViewModel + StateFlow` | Estado de tela | Cada tela observa estado reativo | UI mais previsivel e testavel |
| `Coroutines` | Assincronismo | Rede, upload e sincronizacao | Evita travar a UI |
| `Hilt` | Injecao de dependencia | Repositorios, managers e clientes | Melhor organizacao da arquitetura |
| `Retrofit + OkHttp + Gson` | Consumo da API | Endpoints de auth, materiais e pedidos | Stack madura para REST |
| `EncryptedSharedPreferences` | Persistencia local segura | Sessao, JWT e notificacoes locais | Protege dados sensiveis |
| `Coil` | Imagens | Exibicao de capas e fotos | Biblioteca leve para Compose |
| `Firebase Cloud Messaging` | Push notification | Recebimento de mensagens no device | Suporta os eventos do fluxo |
| `Timber` | Logging | Logs de app e FCM | Logging mais limpo no Android |

### Apoio 4 - Tecnologias backend e infraestrutura usadas no projeto

**Texto do slide**

| Tecnologia | Finalidade | Como foi usada | Justificativa |
|---|---|---|---|
| `Java 21` | Runtime do backend | Execucao do Spring Boot | LTS moderno para Spring 3 |
| `Spring Boot` | Base da API | Configuracao, REST, seguranca e testes | Framework robusto e produtivo |
| `Spring Web` | API REST | Controllers HTTP | Padrao consolidado |
| `Spring Data JPA + Hibernate` | Persistencia | Entidades e repositorios | Agilidade no acesso relacional |
| `Spring Security + JWT` | Seguranca | Login, filtro JWT e rotas protegidas | Sessao stateless para mobile |
| `BCrypt` | Hash de senha | Cadastro e login | Seguranca de credenciais |
| `PostgreSQL` | Banco principal | Dados oficiais do sistema | Confiavel para dados transacionais |
| `H2` | Local e testes | Perfil `local` e fallback de testes | Simplicidade no desenvolvimento |
| `Flyway` | Migracoes | Versionamento do schema | Reproducao consistente do banco |
| `HikariCP` | Pool de conexoes | Datasource do backend | Performance e estabilidade |
| `Gemini + OkHttp` | IA assistiva | Preview de materiais | Integracao externa direta e simples |
| `Firebase Admin SDK` | Envio de push | Backend dispara notificacoes | Integracao oficial com FCM |
| `Spring Cache + Caffeine` | Cache | Leituras quentes e dados de referencia | Melhora desempenho |
| `Micrometer + Prometheus` | Observabilidade | Health, metrics e contadores | Maturidade tecnica |
| `OpenAPI / Swagger` | Documentacao | `/api/v3/api-docs` e `/api/swagger-ui.html` | Facilita teste e demonstracao |
| `Lombok` | Produtividade | Getters, builders e construtores | Reduz boilerplate |

### Apoio 5 - O que nao esta sendo usado no projeto atual

**Texto do slide**

- `Nao usa RoomDatabase`
- `Nao usa SQLite manual`
- `Nao usa DataStore`
- `Nao usa WorkManager`
- `Nao usa Paging 3`
- `Nao usa Firestore`
- `Nao usa Firebase Auth`
- `Nao usa Google Sign-In`
- `Nao usa Realm`
- `Nao usa ObjectBox`
- `Nao usa Supabase`
- `Nao usa Redis, RabbitMQ ou Kafka`
- `Nao usa GraphQL ou WebSocket`
- `Nao usa o SDK oficial google-generativeai`

**Observacao pequena**

- A integracao atual do Gemini foi feita no backend com `OkHttp`

### Apoio 6 - O que vale dizer com honestidade na apresentacao

**Texto do slide**

- O projeto esta implementado e validado, mas ainda nao e produto pronto para loja
- O texto legal do MVP existe, mas ainda depende de revisao juridica formal
- O app nao tem chat interno completo
- O app nao faz pagamento
- O app nao define ponto de encontro
- O app nao usa mapa como funcionalidade central pronta
- O contato acontece via WhatsApp apenas depois da aprovacao
- O repositorio ainda nao deve ser apresentado como `icone personalizado concluido`

---

## Encerramento Sugerido

Se quiser uma frase final curta para a ultima fala:

> O EcoBook AI resolve um problema real com uma arquitetura completa: Android nativo, backend proprio, banco relacional, IA assistiva, notificacoes e regras de negocio consistentes. Ele vai alem do basico sem exagerar o que realmente foi entregue.
