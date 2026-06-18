# Roteiro De Conducao Da Apresentacao - EcoBook AI

Baseado em:

- PDF final exportado `EcoBook-AI.pdf`
- revisao do codigo Android e backend
- `README.md`
- `EcoBookAiAndroid/README.md`
- `EcoBookAiBackend/README.md`
- `docs/architecture.md`
- `docs/testing.md`
- `docs/levantamento-tecnologias-ecobook.md`
- orientacoes do professor sobre apresentacao final

## Objetivo Deste Arquivo

Este arquivo nao e mais texto de slide.

Ele agora serve como:

- roteiro da fala
- guia de tempo
- guia de transicao entre slides
- checklist do que precisa ser corrigido no deck atual

## Estrategia Geral Da Apresentacao

- Duracao ideal: `12 a 15 minutos`
- Nao leiam o slide.
- Usem o slide como apoio visual e a fala como explicacao.
- Quando aparecer print de tela, expliquem o fluxo do usuario, nao o layout.
- Quando aparecer slide tecnico, resumam em blocos, nao em biblioteca por biblioteca.
- Reservem o final para: resultado do projeto, limites honestos e papel de cada integrante.

## Divisao Sugerida Da Fala

- Integrante 1: slides `1` a `4`
- Integrante 2: slides `5` a `10`
- Integrante 3: slides `11` a `18`
- Encerramento: todos ou o lider do grupo

Se forem 2 pessoas:

- Pessoa 1: contexto, proposta, requisitos e stack
- Pessoa 2: fluxo do app, telas, conclusao e perguntas

---

## Roteiro Slide A Slide

### Slide 1 - Capa (`20s a 30s`)

**Objetivo**

- abrir com seguranca
- dizer rapidamente o que e o projeto

**Fala sugerida**

"Nosso projeto e o EcoBook AI, uma plataforma mobile para doacao e solicitacao de materiais de estudo. A ideia central e organizar um fluxo que hoje costuma acontecer de forma informal, sem controle e sem acompanhamento."

**Nao gastar tempo com**

- ler nome da disciplina
- ler professor
- ler semestre

**Transicao**

"Antes de mostrar a solucao, vale explicar o problema que motivou o app."

### Slide 2 - Problema (`40s a 50s`)

**Objetivo**

- provar que existe um problema real

**Fala sugerida**

"A gente percebeu dois lados do mesmo problema: de um lado, familias com livros e apostilas parados em casa; do outro, familias que precisam desses materiais e nao sabem onde encontrar. O que existe hoje normalmente e doacao informal, pulverizada e sem registro. Falta busca, filtro, controle de pedidos e um minimo de seguranca."

**Frase de fechamento**

"Entao o nosso foco nao foi so listar livros, mas organizar esse ciclo inteiro."

### Slide 3 - Proposta (`40s a 50s`)

**Objetivo**

- posicionar o projeto como fluxo completo

**Fala sugerida**

"O EcoBook foi pensado como um app Android nativo com backend proprio. O backend autentica, aplica regras de negocio e persiste os dados. A IA entra como apoio no cadastro do material, sem substituir a revisao humana. E as notificacoes ajudam a manter doador e solicitante acompanhando o fluxo."

**Ponto importante**

- enfatizar que o produto vai de `publicar` ate `concluir a doacao`

### Slide 4 - Requisitos da disciplina (`45s`)

**Objetivo**

- mostrar aderencia ao que o professor pediu

**Fala sugerida**

"Aqui a gente mostra de forma objetiva que o projeto atende os requisitos da disciplina. Temos mais de 4 telas, persistencia com API propria e banco relacional, pelo menos 2 CRUDs, Android 100% nativo com Kotlin e Compose e nome personalizado."

**Fala obrigatoria**

"O app ja usa o icone personalizado do EcoBook no launcher, entao esse ponto agora tambem pode ser apresentado como concluido."

**Transicao**

"Alem do minimo da disciplina, o projeto tambem incorporou alguns extras que sustentam funcionalidade real."

### Slide 5 - Stack tecnologico (`55s a 1min10s`)

**Objetivo**

- mostrar maturidade tecnica sem cansar a banca

**Como conduzir**

Nao leiam item por item.

Falem em 3 blocos:

- Android nativo
- backend
- integracoes

**Fala sugerida**

"No Android, usamos Kotlin com Jetpack Compose, ViewModel, StateFlow, Coroutines, Hilt e Retrofit. No backend, usamos Spring Boot, Spring Security com JWT, JPA, Hibernate, Flyway e PostgreSQL. Nas integracoes, usamos Gemini no backend para o preview assistido dos materiais e Firebase Cloud Messaging para notificacoes."

**Fecho**

"Em resumo, a arquitetura ficou Android nativo consumindo uma API JWT, com persistencia relacional e integracoes externas controladas no backend."

**Cuidado**

- nao travar em detalhe de biblioteca
- nao transformar esse slide em aula de framework

### Slide 6 - Extras (`35s a 45s`)

**Objetivo**

- justificar os extras como parte do produto

**Fala sugerida**

"Esses extras nao foram colocados so para cumprir checklist. A API propria sustenta autenticacao, perfil, materiais e solicitacoes. O Gemini ajuda no preview do cadastro. E o FCM mantem o fluxo vivo fora do momento em que o usuario esta com o app aberto."

### Slide 7 - Fluxo principal (`35s a 45s`)

**Objetivo**

- dar visao macro antes das telas

**Fala sugerida**

"Esse e o fluxo principal do produto. O usuario entra ou se cadastra, completa o onboarding, escolhe entre buscar ou doar, publica ou solicita material e depois acompanha aprovacao, reserva e entrega. O ponto importante aqui e que o contato via WhatsApp so aparece depois da aprovacao."

**Cuidado**

- apontem visualmente o caminho
- nao corram demais, porque esse slide organiza o resto da apresentacao

### Slide 8 - Login e onboarding (`45s a 55s`)

**Objetivo**

- explicar autenticacao e desbloqueio dos fluxos protegidos

**Fala sugerida**

"A autenticacao e feita com email e senha no nosso proprio backend, sem Firebase Auth. Depois do primeiro acesso, o usuario precisa concluir o onboarding com CPF, WhatsApp, cidade, bairro e aceite dos termos da plataforma. So depois disso os fluxos protegidos ficam liberados."

**Observacao importante para a fala**

- diga que o consentimento da plataforma e obrigatorio
- diga que o consentimento de IA e separado e opcional

**Cuidado**

- o print deste slide mostra mais o login do que o onboarding
- entao a fala precisa cobrir os dois para compensar essa limitacao visual

### Slide 9 - Perfil (`35s a 45s`)

**Objetivo**

- mostrar confianca, privacidade e dados do responsavel

**Fala sugerida**

"A tela de perfil concentra os dados principais da conta, os termos, a foto, os consentimentos e tambem as acoes sensiveis, como sair da conta e excluir conta. Isso reforca que o app foi pensado para adultos responsaveis e que a conta precisa ter identificacao minima."

**Muito importante**

- nao falem que `autorizar IA` e obrigatorio, porque isso nao esta correto
- falem que IA e opcional

### Slide 10 - Publicacao com IA (`40s a 50s`)

**Objetivo**

- introduzir o fluxo do doador

**Como conduzir do jeito que o slide esta hoje**

"Antes da publicacao em si, o doador entra por essa area central onde acompanha seus materiais, pedidos recebidos e abre um novo cadastro."

**Observacao**

- o titulo fala `Publicacao com IA`, mas a imagem esta mais para `Area do doador`
- na fala, tentem usar este slide para explicar a entrada do fluxo do doador

### Slide 11 - Publicacao com IA / uso da camera (`45s a 55s`)

**Objetivo**

- explicar o cadastro do material

**Fala sugerida**

"Aqui sim a publicacao aparece de forma mais clara. O usuario pode escolher imagem da galeria ou usar a camera. A capa frontal e obrigatoria e a traseira e opcional. O Android valida JPEG e PNG, faz compressao quando necessario e envia o upload. No backend, o Gemini so entra se houver consentimento de IA; se nao houver, o cadastro segue manualmente."

**Frase forte**

"A IA ajuda o preenchimento, mas nao decide pelo usuario."

### Slide 12 - Descoberta (`35s a 45s`)

**Objetivo**

- explicar busca e filtros

**Fala sugerida**

"Na descoberta, o usuario encontra materiais com base em titulo, sistema de ensino, disciplina, nivel, localizacao e necessidade academica. O objetivo nao foi criar uma lista solta, mas uma busca com contexto suficiente para o usuario decidir antes de pedir."

### Slide 13 - Solicitacao (`35s a 45s`)

**Objetivo**

- explicar o momento em que o solicitante pede o material

**Fala sugerida**

"Quando o usuario encontra um material interessante, ele entra no detalhe e faz a solicitacao. Depois disso, o app passa a mostrar os status com clareza: pendente, aprovada, cancelada e concluida."

**Cuidado**

- visualmente este slide parece mais um detalhe de material com botao de solicitar do que a lista de solicitacoes
- entao a fala precisa deixar claro em que parte do fluxo ele esta

### Slide 14 - Notificacoes e acompanhamento (`35s a 45s`)

**Objetivo**

- mostrar continuidade do fluxo

**Fala sugerida**

"Depois que existem pedidos, o acompanhamento nao depende so de memoria ou conversa externa. O app guarda uma inbox local de notificacoes, e quando a infraestrutura real esta configurada o FCM envia push para avisar eventos importantes."

### Slide 15 - Fluxo do solicitante: pedir material (`25s a 35s`)

**Objetivo**

- reforcar o lado do solicitante

**Fala sugerida**

"Do lado do solicitante, o primeiro passo e buscar um material compativel com sua necessidade e abrir o pedido pelo detalhe do item."

**Dica**

- falem rapido aqui, porque o slide 12 ja fez boa parte desse trabalho

### Slide 16 - Fluxo do doador: aceitar pedido (`25s a 35s`)

**Objetivo**

- explicar aprovacao ou recusa

**Fala sugerida**

"Do lado do doador, o pedido recebido pode ser avaliado e aprovado ou recusado dentro do app. A partir da aprovacao, o fluxo avanca para a conversa externa entre os adultos responsaveis."

**Cuidado muito importante**

- a imagem deste slide nao parece doador recebendo pedido
- ela parece `Minhas solicitacoes`, que e o lado do solicitante
- se isso nao for corrigido, a fala pode ficar contraditoria com o que esta na tela

### Slide 17 - Fluxo do solicitante: falar com doador (`20s a 30s`)

**Objetivo**

- explicar liberacao de contato

**Fala sugerida**

"Depois da aprovacao, o solicitante passa a ver a opcao de falar com o doador via WhatsApp para combinar entrega e retirada."

**Frase importante**

"O app nao define ponto de encontro; ele so organiza o fluxo ate a liberacao do contato."

### Slide 18 - Fluxo do doador: confirmar doacao (`20s a 30s`)

**Objetivo**

- encerrar o fluxo funcional

**Fala sugerida**

"Depois da conversa externa, o doador pode marcar a doacao como concluida ou revogar a aprovacao se necessario. Com isso, o ciclo fecha dentro da plataforma."

**Fecho sugerido para a apresentacao**

"Entao o EcoBook AI nao entrega apenas cadastro e listagem. Ele entrega um ciclo completo, com autenticacao, regras de negocio, persistencia, IA assistiva e acompanhamento do processo ate a conclusao da doacao."

---

## O Que Esta Fraco, Faltando Ou Ruim No Deck Atual

### Alta Prioridade

- O badge `Made with Gamma` aparece nos slides. Isso passa cara de exportacao nao finalizada. Remover antes da entrega final.
- O slide do problema usa imagem com marca d'agua da `Alamy`. Isso precisa sair. Do jeito atual, derruba muito a percepcao de acabamento.
- O deck atual nao termina com um slide forte de `encerramento tecnico + resultado + papel de cada integrante`. Isso faz falta para a banca.
- O deck atual tambem nao mostra, de forma isolada, `banco de dados`, `integracao com Gemini`, `como o backend funciona` e `por que nao usamos Room`. Isso aparece diluido, mas nao fica forte para defesa.
- O slide de perfil afirma que o usuario deve `autorizar a classificacao assistida por IA`. Isso esta tecnicamente errado. O consentimento de IA e opcional.

### Media Prioridade

- O slide 8 fala de onboarding, mas o print esta muito mais para `login` do que para `onboarding`.
- O slide 10 esta com titulo `Publicacao com IA`, mas o print mostra a `Area do doador`.
- O slide 16 fala `Fluxo do usuario (Doador)`, mas a imagem parece do fluxo do `solicitante`.
- O slide 13 fala `Tela de solicitacao`, mas visualmente parece mais o detalhe do material com CTA de solicitar.
- O slide 5 `Stack Tecnologico` esta bom como referencia, mas esta denso para apresentacao oral. Se voces lerem tudo, vai ficar cansativo.
- O slide 6 do fluxo principal funciona, mas o desenho em zigue-zague exige que alguem aponte bem o caminho. Se a fala correr, o slide perde efeito.

### Baixa Prioridade, Mas Vale Corrigir

- No slide de perfil, evitar dizer que `senha` faz parte dos dados do perfil. Isso fica estranho e passa uma mensagem ruim.
- No texto da publicacao, trocar `load temporario` por `upload temporario`.
- Padronizar `usuario`, `WhatsApp`, `doador` e `solicitante`, para nao parecer texto montado em blocos separados.
- Alguns slides finais repetem a ideia de fluxo do usuario e podem ser condensados se o grupo estiver com tempo apertado.

---

## O Que Ainda Falta Ter, Mesmo Que Seja Como Slide De Apoio

- Um slide final com `229 testes backend`, `84.72% JaCoCo`, `app:testDebugUnitTest` verde e `lint` com `0` erros.
- Um slide ou fala pronta sobre `PostgreSQL como banco principal`, `H2 para local/teste` e `Flyway nas migracoes`.
- Uma resposta explicita para a pergunta: `Nao usamos Room. Usamos EncryptedSharedPreferences no Android e o dado oficial fica no backend.`
- Uma resposta explicita para a pergunta: `Por que Gemini fica no backend e nao no app?` Resposta: seguranca da chave, controle de retry, timeout e parsing.
- Um slide de encerramento com `papel de cada integrante`.
- Uma fala curta reforcando que o launcher agora usa o icone personalizado do EcoBook.

---

## Ordem De Correcao Sugerida Antes Da Banca

1. Remover marca d'agua da imagem do problema.
2. Remover o badge do Gamma.
3. Corrigir os slides com imagem e titulo desencontrados.
4. Corrigir a parte que faz parecer que consentimento de IA e obrigatorio.
5. Adicionar um slide final de evidencias tecnicas e equipe.
6. Preparar 2 ou 3 slides de apoio para banco, backend, Gemini e Room.

---

## Frase Final Sugerida

> O EcoBook AI resolve um problema real com Android nativo, backend proprio, banco relacional, IA assistiva e notificacoes, organizando o processo de doacao do inicio ao fim sem fingir que o projeto ja esta acima do que realmente entrega hoje.
