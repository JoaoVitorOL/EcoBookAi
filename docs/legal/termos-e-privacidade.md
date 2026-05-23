# EcoBook AI - Termos de Uso e Privacidade

**Status**: rascunho operacional do MVP  
**Data**: 2026-05-23  
**Importante**: este texto descreve o comportamento atual do app e do backend, mas ainda precisa de revisao juridica e de privacidade antes de qualquer publicacao real.

---

## 1. O que e o EcoBook

O EcoBook e uma plataforma para conectar doadores e estudantes interessados em doar e solicitar materiais de estudo. O app nao e um marketplace de venda dentro do fluxo principal: o objetivo do MVP e facilitar doacoes, matching geografico e comunicacao segura entre as partes.

Ao criar uma conta e usar o app, o usuario concorda em:

- informar dados verdadeiros e atualizados
- publicar apenas materiais que realmente pode disponibilizar
- usar a plataforma de boa-fe, sem fraude, spam, assedio ou tentativa de burlar as regras de matching e moderacao
- respeitar a privacidade e a seguranca de outros usuarios

---

## 2. Dados que podem ser coletados

O funcionamento atual do MVP usa ou registra:

- nome
- email
- telefone/WhatsApp
- cidade
- bairro
- instituicao
- consentimentos da plataforma e da IA
- materiais publicados
- solicitacoes, aprovacoes, recusas e conclusoes
- notificacoes e inbox interno
- trilhas de auditoria e eventos tecnicos de seguranca

Alguns desses dados sao obrigatorios para liberar funcionalidades protegidas, como upload, matching e solicitacoes.

---

## 3. Como os dados sao usados

Os dados sao usados para:

- autenticar o usuario
- manter a sessao e a seguranca da conta
- completar o perfil obrigatorio
- normalizar cidade e bairro para matching geografico
- mostrar e filtrar materiais
- permitir solicitacoes, aprovacoes e conclusoes
- exibir formas de contato apenas quando o fluxo autoriza
- enviar notificacoes
- gerar exportacao de dados e suportar exclusao/anonimizacao da conta
- prevenir abuso, fraude e uso indevido da plataforma

Quando o usuario altera o email, o backend passa a tratar o novo email como identidade principal da conta. Por isso, a sessao antiga deixa de valer e um novo login e exigido.

---

## 4. Consentimento da plataforma e consentimento de IA

O app diferencia dois pontos:

- **Consentimento da plataforma**: necessario para concluir o onboarding. O usuario deve conseguir ler um resumo dos termos e da privacidade antes de aceitar.
- **Consentimento de IA**: opcional. Quando ativo, o backend pode usar a etapa de classificacao assistida por IA para sugerir metadados do material. Quando desativado, o preenchimento manual continua disponivel.

O consentimento de IA pode ser alterado depois no perfil.

---

## 5. Compartilhamento e exibicao de dados

O EcoBook nao deve expor todos os dados do usuario para qualquer pessoa. O comportamento esperado do MVP e:

- dados publicos e de busca mostram apenas o necessario para descoberta dos materiais
- dados de contato mais sensiveis so aparecem quando a solicitacao e aprovada ou quando o fluxo exigir
- informacoes administrativas e trilhas de auditoria ficam restritas a controles internos e moderacao

---

## 6. Retencao, exclusao e exportacao

O usuario pode:

- revisar e corrigir os dados do proprio perfil
- exportar os proprios dados
- solicitar a exclusao da conta com confirmacao explicita

Quando a conta e excluida, o runtime atual do backend busca:

- anonimizar os dados pessoais que precisem permanecer por integridade operacional
- remover ou desativar materiais e imagens ligadas ao fluxo ativo
- revogar a sessao e impedir o uso do token anterior
- registrar eventos necessarios para auditoria e seguranca

---

## 7. Responsabilidades do usuario

O usuario e responsavel por:

- manter email e telefone atualizados
- proteger a propria senha
- nao compartilhar a conta com terceiros
- nao usar a plataforma para publicar conteudo ilicito, enganoso ou ofensivo

O backend pode restringir ou remover conteudos e contas quando houver abuso, violacao de regras ou necessidade de moderacao.

---

## 8. Limitacoes deste texto

Este documento ainda nao substitui uma versao juridicamente revisada de Termos de Uso e Politica de Privacidade. Ele existe para que o aceite dentro do app nao seja cego e para registrar, de forma legivel, o que o MVP realmente faz hoje.

Antes de producao/publicacao externa, os proximos passos recomendados sao:

- revisao juridica/LGPD
- definicao formal de base legal, retencao e contato do controlador
- revisao de linguagem final para a interface e para a loja do app
