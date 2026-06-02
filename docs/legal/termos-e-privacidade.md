# EcoBook AI - Termos de Uso, Privacidade e LGPD

**Status**: rascunho operacional ampliado do MVP  
**Data**: 2026-06-02  
**Importante**: este texto descreve o fluxo atual do aplicativo e do backend, mas ainda precisa de revisão jurídica formal antes de qualquer publicação pública, comercial ou institucional.

---

## 1. Finalidade do EcoBook

O EcoBook é uma plataforma para conectar doadores e responsáveis interessados em doar, localizar, solicitar e receber materiais de estudo. O objetivo principal do app é facilitar a circulação de materiais educacionais entre famílias, com foco em descoberta, organização do fluxo de pedidos e comunicação responsável entre adultos.

O EcoBook não é um marketplace de venda dentro do fluxo principal do MVP. O aplicativo também não intermedeia pagamento, transporte, retirada física nem agenda de entrega. Depois que uma solicitação é aprovada, o contato entre as partes ocorre fora do app, por WhatsApp, sob responsabilidade dos adultos envolvidos.

---

## 2. Público autorizado

O EcoBook deve ser usado por pessoas adultas, especialmente pais, mães e responsáveis legais.

Crianças e adolescentes não devem:

- criar conta por conta própria
- negociar entrega ou retirada sozinhos
- conduzir conversas externas sem supervisão do responsável
- fornecer CPF, telefone ou dados pessoais próprios fora do fluxo do responsável

Se houver tratamento de dados relacionados a estudantes menores de idade no contexto do uso do app, esse tratamento deve sempre ocorrer de forma indireta, limitada e sob atuação do adulto responsável.

---

## 3. Visão geral do fluxo do aplicativo

O funcionamento atual do EcoBook pode envolver as seguintes etapas:

1. Cadastro e autenticação
   O usuário cria uma conta com email e senha e passa a acessar o app com sessão autenticada.

2. Completar perfil
   Para liberar fluxos protegidos, o responsável informa nome, WhatsApp, CPF, cidade, bairro e, opcionalmente, instituição e foto de perfil.

3. Aceite dos termos e consentimentos
   O onboarding exige aceite dos termos da plataforma. O consentimento para IA é separado e opcional.

4. Cadastro de material
   O doador envia a imagem frontal obrigatória e, se quiser, a imagem traseira. Depois revisa manualmente os campos do material, incluindo título, descrição, disciplina, nível de ensino, ano, sistema de ensino, estado de conservação, ano de publicação e necessidade acadêmica.

5. Classificação assistida por IA
   Se o consentimento de IA estiver ativo, o backend pode sugerir parte dos metadados do material a partir das imagens. O usuário continua responsável por revisar e corrigir tudo antes da publicação.

6. Descoberta e busca
   Quem procura materiais pode usar filtros de texto, disciplina, nível de ensino, ano escolar, sistema de ensino, necessidade acadêmica, cidade, bairro e faixa de publicação.

7. Solicitação
   O responsável interessado envia uma solicitação pelo app. Enquanto a solicitação não é aprovada, os dados de contato do doador não devem ser expostos além do necessário ao funcionamento do fluxo.

8. Aprovação ou recusa
   O doador analisa os pedidos recebidos e pode aprovar, recusar, revogar ou concluir a doação conforme o estado do fluxo.

9. Contato externo
   Depois da aprovação, a conversa, o alinhamento do local e o agendamento da entrega passam a ocorrer via WhatsApp, entre os adultos responsáveis.

10. Conclusão, exclusão e auditoria
    O sistema mantém histórico técnico, notificações, eventos de segurança, registros de consentimento e rotinas de exportação/exclusão dentro do escopo do MVP.

---

## 4. Dados que podem ser tratados

Dependendo do uso do app, o EcoBook pode coletar, armazenar, exibir internamente, processar ou registrar:

- nome do responsável
- email
- senha em formato protegido pelo backend
- telefone/WhatsApp
- CPF do adulto responsável
- cidade
- bairro
- instituição
- foto de perfil
- consentimento da plataforma
- consentimento para IA
- histórico de materiais cadastrados
- metadados dos materiais, inclusive necessidade acadêmica
- imagens dos materiais enviadas no preview e na publicação final
- solicitações, aprovações, recusas, cancelamentos e conclusões
- notificações e caixa de entrada interna
- tokens técnicos de notificação
- dados de auditoria, segurança, exportação e exclusão de conta

Alguns desses dados são obrigatórios para liberar recursos protegidos do aplicativo. Outros são opcionais, mas podem melhorar a identificação do responsável, a usabilidade ou a contextualização do material.

---

## 5. Como os dados são usados

Os dados são tratados para:

- autenticar o usuário e proteger a sessão
- verificar se o perfil obrigatório foi concluído
- identificar o adulto responsável pela conta
- normalizar cidade e bairro para descoberta geográfica
- permitir o cadastro, a edição e a publicação de materiais
- executar a busca por filtros e a ordenação da discovery
- receber, aprovar, recusar, cancelar e concluir solicitações
- liberar a comunicação entre as partes no momento certo do fluxo
- enviar notificações dentro do app e por infraestrutura compatível
- prevenir abuso, fraude, spam, uso indevido e inconsistências operacionais
- gerar exportação de dados
- executar rotinas de exclusão, anonimização e auditoria

Quando o usuário altera o email da conta, o backend passa a tratar o novo email como identidade principal. Por isso, a sessão anterior pode ser encerrada e um novo login pode ser exigido.

---

## 6. Necessidade acadêmica no material

A necessidade acadêmica deixou de ser apenas uma preferência abstrata de perfil e passou a fazer parte do cadastro de cada material publicado.

Isso significa que:

- a necessidade acadêmica é escolhida manualmente pelo doador em cada material
- esse campo compõe os metadados do item publicado
- o receptor pode usar esse campo como filtro de busca
- a qualidade dessa informação depende da revisão humana antes da publicação

O usuário continua responsável por selecionar a opção mais adequada ao contexto real do material.

---

## 7. Como a IA funciona no EcoBook

O uso de IA no EcoBook é opcional e separado do aceite geral da plataforma.

Quando o consentimento de IA estiver ativo:

- o backend pode enviar as imagens do material para a etapa de classificação assistida
- o sistema pode sugerir campos como título, autor, editora, disciplina, nível de ensino, ano e sistema
- a necessidade acadêmica continua sendo um campo manual, escolhido pelo usuário
- o resultado da IA pode vir com alta confiança, baixa confiança ou falha

Quando o consentimento de IA estiver desativado:

- o usuário continua podendo cadastrar o material normalmente
- o preenchimento passa a ser manual
- o backend não deve depender da IA para permitir a publicação

Limitações importantes:

- a IA pode errar, omitir ou sugerir classificações imprecisas
- a IA não substitui revisão humana
- o EcoBook não garante exatidão pedagógica, editorial ou comercial do retorno automatizado
- a decisão final sobre o que será publicado é sempre do usuário

---

## 8. Comunicação entre usuários

O EcoBook não oferece chat interno completo no fluxo atual do MVP.

O desenho operacional do app é:

- descoberta e solicitação acontecem dentro da plataforma
- aprovação do pedido acontece dentro da plataforma
- conversa, retirada, entrega e ponto de encontro são combinados fora da plataforma
- o canal externo previsto para esse contato é o WhatsApp

Isso significa que o usuário é responsável por:

- conferir com quem está falando
- validar local, horário e condições da entrega
- agir com cautela antes de compartilhar informações adicionais
- não expor dados de terceiros além do necessário

O EcoBook não assume responsabilidade por encontros presenciais, atrasos, ausência de comparecimento, conflitos externos ou qualquer negociação feita fora da plataforma.

---

## 9. Compartilhamento de dados

O EcoBook deve limitar o compartilhamento de dados ao mínimo necessário para operar o fluxo.

Em linhas gerais:

- dados públicos de busca devem mostrar apenas o necessário para descoberta do material
- dados de contato mais sensíveis devem ser exibidos apenas quando o fluxo autorizar
- dados administrativos, trilhas de auditoria e informações internas ficam restritos a controles internos, moderação, segurança e obrigações legais
- provedores técnicos de infraestrutura podem processar dados na medida necessária para autenticação, armazenamento, notificações, observabilidade ou classificação assistida por IA

Se houver uso de fornecedor externo para IA, armazenamento, mensageria ou analytics operacional, esse tratamento deve respeitar a finalidade informada ao titular e as obrigações contratuais e legais aplicáveis.

---

## 10. Segurança e retenção

O EcoBook adota medidas técnicas e operacionais compatíveis com o estágio atual do MVP, como:

- autenticação por credenciais e token
- controle de sessão
- trilhas de auditoria
- separação entre dados de perfil, materiais, solicitações e notificações
- limpeza de uploads temporários e rotinas de exclusão

Mesmo assim, nenhum serviço digital é absolutamente imune a falhas, incidentes, uso indevido ou acessos indevidos.

Sobre retenção:

- dados podem permanecer armazenados enquanto forem necessários para operar o serviço, cumprir obrigações legais, preservar integridade do fluxo, manter segurança ou atender auditoria
- imagens temporárias podem ser removidas quando expiram ou deixam de ser necessárias
- materiais e imagens ligadas a uma conta excluída podem sair do fluxo ativo
- registros que precisem permanecer por integridade operacional podem ser anonimizados em vez de apagados integralmente

---

## 11. Exportação, correção e exclusão

O usuário pode, dentro do escopo suportado pelo MVP:

- revisar dados do próprio perfil
- corrigir dados cadastrais
- exportar os próprios dados
- solicitar a exclusão da conta

Quando a conta é excluída, o comportamento esperado do backend inclui, conforme aplicável:

- revogação da sessão
- interrupção do uso do token anterior
- cancelamento ou retirada de materiais do fluxo ativo
- remoção de arquivos ou referências operacionais associadas
- anonimização de dados que precisem permanecer para integridade do sistema, segurança ou auditoria

---

## 12. LGPD: como este app interpreta suas obrigações

Esta seção resume como o EcoBook deve ser lido à luz da Lei nº 13.709/2018, sem substituir avaliação jurídica formal do ambiente de produção.

### 12.1. Princípios aplicáveis

O tratamento de dados no EcoBook deve respeitar os princípios da LGPD, especialmente:

- finalidade
- adequação
- necessidade
- livre acesso
- qualidade dos dados
- transparência
- segurança
- prevenção
- não discriminação
- responsabilização e prestação de contas

Na prática, isso significa que o app deve coletar apenas o que for compatível com o fluxo informado ao usuário, evitar excesso de dados e explicar com clareza o que acontece em cada etapa.

### 12.2. Agentes de tratamento

Para a LGPD, o operador real do EcoBook em produção deve definir formalmente:

- quem é o **controlador** dos dados
- quais fornecedores atuam como **operadores**
- quem será o **encarregado/canal de privacidade**

Enquanto este projeto permanecer como MVP técnico, essa identificação institucional ainda precisa ser preenchida antes de qualquer lançamento público.

### 12.3. Bases legais

Com base no fluxo atual do aplicativo, as bases legais mais prováveis para o tratamento são:

- execução de procedimentos relacionados ao uso da plataforma e ao fluxo solicitado pelo próprio usuário
- cumprimento de obrigações legais e regulatórias, quando aplicável
- exercício regular de direitos, prevenção à fraude e segurança do serviço, quando cabível
- consentimento, nos pontos em que o produto exige escolha específica do titular, especialmente para a classificação assistida por IA

O consentimento para IA:

- deve ser livre, informado e destacado
- não pode ser presumido por autorização genérica
- pode ser revogado pelo titular
- não impede o uso manual do app quando estiver desligado

### 12.4. Direitos do titular

Nos termos da LGPD, o titular pode requerer, conforme aplicável:

- confirmação da existência de tratamento
- acesso aos dados
- correção de dados incompletos, inexatos ou desatualizados
- anonimização, bloqueio ou eliminação de dados desnecessários ou tratados em desconformidade
- portabilidade, quando cabível
- informação sobre compartilhamento
- informação sobre a possibilidade de não fornecer consentimento e sobre as consequências
- revogação do consentimento
- eliminação dos dados tratados com base em consentimento, observadas as exceções legais

O EcoBook deve oferecer meio claro, gratuito e facilitado para essas solicitações dentro do que estiver implementado no produto e nas obrigações legais do operador real.

### 12.5. Crianças e adolescentes

Como o aplicativo foi desenhado para ser operado por adultos responsáveis, o EcoBook reduz o risco de tratamento direto de dados de crianças e adolescentes. Ainda assim, qualquer operação que venha a envolver dados desse público deve observar as proteções reforçadas da LGPD.

### 12.6. Canal de contato LGPD

Antes de produção, este documento deve ser complementado com:

- nome empresarial ou institucional do controlador
- email oficial de privacidade
- canal para solicitações LGPD
- canal para comunicação de incidentes ou dúvidas sobre tratamento

Modelo de preenchimento obrigatório antes do lançamento:

- Controlador: `[preencher antes da publicação]`
- Encarregado/canal LGPD: `[preencher antes da publicação]`
- Email de privacidade: `[preencher antes da publicação]`

---

## 13. Responsabilidades do usuário

Ao usar o EcoBook, o usuário se compromete a:

- informar dados verdadeiros e atualizados
- usar apenas conta própria
- proteger a própria senha
- publicar somente materiais que realmente pode disponibilizar
- revisar manualmente os campos antes de confirmar um material
- escolher corretamente a necessidade acadêmica do item
- agir de boa-fé na comunicação com outras pessoas
- não praticar fraude, spam, assédio, discriminação, falsidade ou exposição indevida de dados de terceiros

O EcoBook poderá restringir uso, remover conteúdo ou encerrar acesso quando houver abuso, violação de regras, risco de segurança ou necessidade de moderação.

---

## 14. Alterações deste documento

Este texto pode ser atualizado para refletir:

- mudanças no fluxo do app
- alterações de infraestrutura
- novos fornecedores
- revisão jurídica
- exigências regulatórias

Se houver mudanças relevantes em pontos baseados em consentimento, o titular deverá ser informado de forma destacada e, quando aplicável, poderá revisar ou revogar esse consentimento.

---

## 15. Limitação e próximo passo obrigatório

Este documento existe para que o aceite do usuário não seja cego e para registrar, com linguagem clara, o que o MVP realmente faz hoje.

Ainda assim, ele não substitui:

- política de privacidade revisada por profissional jurídico
- definição institucional do controlador e do encarregado
- revisão contratual com fornecedores
- validação formal de retenção, segurança e compartilhamento

Antes de qualquer lançamento real, a revisão jurídica e de privacidade é obrigatória.
