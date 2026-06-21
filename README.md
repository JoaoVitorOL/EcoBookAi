# EcoBook AI

Plataforma mobile para doacao e solicitacao de materiais de estudo, com app Android nativo e backend Spring Boot.

![Android](https://img.shields.io/badge/Android-Native-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white)

> Fluxo de bootstrap revalidado em `2026-06-18`: `backend local -> health -> local.properties -> assembleDebug -> lintDebug -> testDebugUnitTest -> Android Studio`.

## Sumario

- [Sobre o Projeto](#sobre-o-projeto)
- [Tecnologias Utilizadas](#tecnologias-utilizadas)
- [Pre-requisitos](#pre-requisitos)
- [Como Instalar e Rodar](#como-instalar-e-rodar)
- [Como Usar](#como-usar)
- [Testes e Validacao](#testes-e-validacao)
- [Troubleshooting Rapido](#troubleshooting-rapido)
- [Documentacao Complementar](#documentacao-complementar)

## Sobre o Projeto

O EcoBook AI organiza o ciclo de doacao de materiais escolares entre responsaveis. O aplicativo permite cadastrar materiais, buscar itens por filtros, solicitar doacoes, aprovar pedidos e liberar o contato via WhatsApp somente no momento certo do fluxo.

Arquitetura resumida:

- `EcoBookAiAndroid`: aplicativo Android em `Kotlin + Jetpack Compose`.
- `EcoBookAiBackend`: API REST em `Spring Boot`, com autenticacao `JWT`, regras de negocio e persistencia.
- `docker-compose.yml`: sobe um `PostgreSQL` local para o perfil default do backend.
- `docs/`: arquitetura, testes, deploy, troubleshooting, seguranca e materiais de apoio.

Diferenciais do MVP atual:

- onboarding com validacao de perfil e consentimentos;
- publicacao de materiais com preview assistido por IA;
- busca com filtros academicos e geograficos;
- fluxo de solicitacoes com aprovar, recusar, revogar e concluir;
- notificacoes locais e suporte opcional a `Firebase Cloud Messaging`.

## Tecnologias Utilizadas

| Camada | Stack principal |
| --- | --- |
| Android | Kotlin, Jetpack Compose, Material 3, Navigation Compose, Hilt, Retrofit, OkHttp, Coil |
| Backend | Java 21+, Spring Boot, Spring Security, JWT, Spring Data JPA, Hibernate, Flyway |
| Banco de dados | PostgreSQL no perfil default, H2 no perfil local |
| Integracoes | Google Gemini no backend e Firebase Cloud Messaging opcional |
| Qualidade | JUnit, MockK, Espresso, Testcontainers, JaCoCo, Checkstyle |

## Pre-requisitos

- `Windows PowerShell` para seguir exatamente o fluxo validado deste README.
- `Java 21+` para o backend.
  Exemplo validado: `C:\Program Files\Java\jdk-26`
- `Maven 3.9+`
- `Android Studio`
- `Android SDK Platform 34`
- `Emulador Android` ou dispositivo fisico
- `Docker Desktop` apenas se voce quiser usar o backend com PostgreSQL no perfil default

Itens opcionais:

- `GEMINI_API_KEY` para usar o Gemini real.
- `EcoBookAiAndroid/app/google-services.json` para validar push real com Firebase.
- `FIREBASE_SERVICE_ACCOUNT_PATH` e `FIREBASE_DATABASE_URL` para o backend enviar FCM real.

## Como Instalar e Rodar

### 1. Entrar na raiz do projeto

```powershell
cd EcoBookAi
```

### 2. Conferir Java e Maven

```powershell
java -version
mvn -version
```

Se o `java -version` do terminal mostrar `17`, nao tem problema para o Android, mas o backend continua exigindo `21+`. Nesse caso, ajuste `JAVA_HOME` antes de subir o backend.

### 3. Subir o backend local

Esse e o caminho recomendado para primeiro boot. Ele usa `H2`, nao depende de Docker e sobe mesmo sem segredos opcionais.

```powershell
powershell -ExecutionPolicy Bypass -File .\EcoBookAiBackend\scripts\Run-Local.ps1 -JavaHome "C:\Program Files\Java\jdk-26"
```

Se a porta `8080` estiver ocupada:

```powershell
powershell -ExecutionPolicy Bypass -File .\EcoBookAiBackend\scripts\Run-Local.ps1 -JavaHome "C:\Program Files\Java\jdk-26" -Port 8081
```

Observacoes:

- sem `GEMINI_API_KEY`, o perfil `local` usa mock do Gemini;
- `google-services.json` nao e necessario para o backend subir;
- o endpoint base do backend e `http://127.0.0.1:8080/api`;
- deixe o backend rodando em um terminal separado antes de seguir para o Android.

### 4. Validar o health do backend

```powershell
Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8080/api/v1/health
```

Resposta esperada:

```json
{
  "status": 200,
  "message": "Backend online"
}
```

### 5. Configurar o Android

Edite `EcoBookAiAndroid/local.properties` com base no arquivo de exemplo:

```properties
sdk.dir=C\:\\Users\\SEU_USUARIO\\AppData\\Local\\Android\\Sdk
backend.url=http://10.0.2.2:8080/api
```

Se voce iniciou o backend em outra porta, ajuste a mesma porta em `backend.url`.

Regra importante para o emulador:

- use `http://10.0.2.2:8080/api` quando o backend foi iniciado pelo PowerShell do Windows, como neste README;
- use o IP do WSL apenas se o backend realmente estiver rodando dentro do WSL;
- em dispositivo fisico, use o IP da maquina que esta hospedando o backend.

Se o backend estiver no WSL, descubra o IP atual com:

```powershell
wsl.exe -e bash -lc "hostname -I"
```

Exemplo:

```properties
backend.url=http://172.22.160.34:8080/api
```

### 6. Validar o build Android

O projeto possui espacos e acentos no caminho do Windows. Por isso, use o wrapper ASCII do repositorio.

```powershell
cd .\EcoBookAiAndroid
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:assembleDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:lintDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest
```

### 7. Rodar o app no Android Studio

1. Abra a pasta `EcoBookAiAndroid`.
2. Aguarde o sync do Gradle.
3. Inicie um emulador API 34 ou conecte um dispositivo fisico.
4. Confirme que o backend continua respondendo em `/api/v1/health`.
5. Clique em `Run`.

### 8. Opcional: subir o backend com PostgreSQL

Se voce quiser rodar o perfil default em vez do perfil local:

```powershell
cd ..
docker compose up -d postgres
cd .\EcoBookAiBackend
$env:JAVA_HOME = "C:\Program Files\Java\jdk-26"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run
```

Esse perfil espera:

- banco em `jdbc:postgresql://localhost:5432/ecobook`;
- usuario `ecobook`;
- senha `dev_password_123`, salvo override por `DB_PASSWORD`.

## Como Usar

Fluxo principal do produto:

1. cadastre uma conta ou faca login;
2. conclua o onboarding com CPF, WhatsApp, cidade, bairro e aceite da plataforma;
3. publique um material com preenchimento manual ou assistido por IA;
4. busque materiais por filtros de disciplina, nivel, ano, cidade, bairro e necessidade academica;
5. solicite um item e acompanhe os status;
6. aprove ou recuse pedidos no lado do doador;
7. use o WhatsApp apenas apos a aprovacao.

Exemplo de registro via API:

```bash
curl -X POST http://127.0.0.1:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "teste@example.com",
    "password": "SenhaSegura123",
    "nome": "Usuario Teste"
  }'
```

Documentacao da API com o backend no ar:

- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

## Testes e Validacao

Comandos principais do backend:

```powershell
cd .\EcoBookAiBackend
$env:JAVA_HOME = "C:\Program Files\Java\jdk-26"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn test
```

Comandos principais do Android:

```powershell
cd ..\EcoBookAiAndroid
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:assembleDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:lintDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest
```

Validacao executada nesta revisao, em `2026-06-18`:

- backend local iniciado com `EcoBookAiBackend/scripts/Run-Local.ps1`;
- `GET /api/v1/health` retornando `200`;
- `app:assembleDebug` concluido com sucesso;
- `app:lintDebug` concluido com sucesso;
- `app:testDebugUnitTest` concluido com sucesso.

## Troubleshooting Rapido

- Backend nao sobe: confirme que o JDK usado no backend e `21+`. O Java padrao do terminal pode estar em `17`.
- Emulador nao encontra o backend: se o backend foi iniciado no Windows, use `10.0.2.2`, nao o IP do WSL.
- App nao abre no Android Studio: abra `EcoBookAiAndroid`, nao a raiz inteira do repositorio.
- Push nao chega: `google-services.json` e credenciais Firebase sao opcionais para o MVP, mas obrigatorios para validar FCM real.

## Documentacao Complementar

- [Arquitetura](./docs/architecture.md)
- [Testes](./docs/testing.md)
- [Deployment](./docs/deployment.md)
- [Troubleshooting detalhado](./docs/troubleshooting.md)
- [Analise estatica](./docs/static-analysis.md)
- [Security scan](./docs/security-scan.md)
- [Migracoes e rollbacks](./docs/migration-rollbacks.md)
- [Levantamento tecnologico](./docs/levantamento-tecnologias-ecobook.md)
- [README do backend](./EcoBookAiBackend/README.md)
- [README do Android](./EcoBookAiAndroid/README.md)
