# Undercontroll Core API

CRM da oficina (eletrodoméstico pequeno). Java 21, Spring Boot 3.5, Maven Wrapper.

## Architecture

HTTP entra em `application/controller/impl`. Regras ficam em `domain/usecase` (portas `*Port` com records `Input`/`Output`). Persistência, JWT, Redis e LLM ficam em `infrastructure`. O Java **nunca** chama a API do Mercado Livre: lê só as views Postgres que o ETL grava (`vw_market_*`, contrato em `../etl/docs/core-api-contract.md`). Ana, insights e transcrição têm configuração própria (`undercontroll.ai.{ana|insights|transcription}.*`: provider, model, timeout, thinking); cada uma monta seu modelo via `ChatModelFactory` (autoconfig de chat do Spring AI desligada).

Insights seguem a publicação do ETL: `MarketSnapshotPoller` lê `vw_market_published_snapshot` e cria um `insight_job` por (snapshot, prompt-version, model_config_hash); `InsightJobWorker` pega o lease (UPDATE condicional) e roda `RunInsightJobImpl`: `InsightCandidateGenerator` (candidatos determinísticos) → draft do modelo com schema nativo → `InsightDraftValidator` (1 reparo com os erros) → `InsightNarrativeRenderer` → publicação em transação (`market_insight_publication`). O draft fica salvo na `insight_attempt`; falha de banco nunca chama o modelo de novo.

## Commands

Rodar na raiz deste repositório.

```bash
./mvnw test
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
./mvnw clean package -DskipTests
./mvnw -Plive-ai verify   # smoke real (LIVE_AI_PROVIDER + chave); fora do build padrão, cobrado
docker compose up -d    # postgres, rabbitmq, redis
```

Perfil `dev` usa Postgres em `localhost:5432` (não H2). Testes unitários/WebMvc não precisam de Redis (`cache.type: simple` em `src/test/resources`).

Swagger: `http://localhost:8080/swagger-ui.html`.

Collections HTTP (manter as duas alinhadas ao contrato):

- Bruno: `Bruno Collection/`
- Yaak: `Yaak Collection/` (abrir com **File → Open Workspace** nessa pasta; escolher env **Local**)

## HTTP collections

Depois de **qualquer** mudança de contrato HTTP (path, método, status, query, body, headers, auth, CSRF), atualizar **Bruno Collection e Yaak Collection no mesmo trabalho**. Fonte de verdade: interfaces `*Api.java` + records em `application/dto`. Incluir request novo, apagar o obsoleto, e criar/ajustar env vars se nascer path ou id novo.

- Yaak automatiza Bearer (`Auth / Sign in password` → `$.accessToken`) via `response.body.path` com `behavior=smart`. A pasta Authenticated herda isso. Não commitar `cookie_jar` nem tokens no YAML.
- Bruno não encadeia requests: rodar `Auth / Sign in password` (grava `token` / `refreshToken`). Requests autenticados usam Bearer; CSRF não é exigido em `/v1/api/**`.

## Conventions

- OpenAPI/Swagger na interface `*Api`; mappings HTTP (`@GetMapping`, `@PostMapping`) na `*Controller` impl.
- DTOs e I/O das portas são records. Entidades JPA em `infrastructure/persistence/entity`.
- Lombok + MapStruct (`componentModel = SPRING`).
- Erros HTTP em `infrastructure/handler/*ExceptionHandler`.
- User id autenticado: JWT `sub` é `String.valueOf(user.getId())`, não e-mail. Ler via `CurrentUserIdPort` no use case, não na controller.
- Roles: `ADMINISTRATOR`, `CUSTOMER` (`ROLE_*` no Spring Security).
- Chat da Ana (`/v1/api/chats/**`) é só `ADMINISTRATOR`.

## Constraints

- Não chamar Mercado Livre, Highlights, Trends nem `/products` a partir deste serviço.
- Insights: o modelo só escolhe `candidate_id` e escreve `text`/`action` com `{{placeholders}}` do próprio candidato; números, meses e nomes vêm do servidor. Schema nativo (`StructuredOutputChatOptions.setOutputSchema`) com os limites de `InsightContract` (fonte única). Mudou prompt ou contrato → subir `prompt-version`. Retry só para TRANSIENT/RATE_LIMITED/TIMEOUT (`LlmErrorClassifier` por tipo/status, nunca por texto), no máximo 2, com jitter e `Retry-After` dentro do `job-deadline`. `provider: none` desliga só aquela funcionalidade (Ana: 503). Provider inválido, chave ausente ou `thinking` não suportado pelo modelo derrubam a inicialização com a chave no erro. Gemini precisa de `GEMINI_API_KEY` (ou `GOOGLE_API_KEY`); OpenAI de `OPENAI_API_KEY`. `ANA_PROVIDER`/`TRANSCRIPTION_PROVIDER` caem em `INSIGHTS_PROVIDER` se ausentes.
- Texto visível de insight: mês por extenso ("mês de agosto de 2026"), nunca `2026-08` nem a palavra `bucket`.
- GET `/v1/api/insights` sempre responde com `state` (READY, GENERATING, FAILED, STALE, AWAITING_DATA); só READY vai para o cache. Sem snapshot publicado = AWAITING_DATA. POST `/v1/api/market/insights/jobs` (ADMINISTRATOR) devolve 202.
- Mediana de preço só com `valid_price_count >= 3`; variação de preço só com `consecutive` e `paired_count` mínimo (`MarketEvidence`). `available_offer_count` pode ser nulo.
- `spring.jpa.open-in-view: false`. Associação LAZY só dentro de `@Transactional` (ou EntityGraph / fetch join). Não mapear proxy depois da sessão fechada.
- Sugestões da Ana saem de fatos do banco (`GroundedQuestionGenerator.groundedQuestions`), sem LLM. O briefing do chat usa o mesmo snapshot (abertos, pickup, estoque, demandas, aviso). Listagens da oficina usam query limitada, não `findAll` da tabela toda.
- Não commitar `.env`, credenciais, nem `jwt.secret` real.

## Gotchas

- README e `.env.example` ainda citam H2/MySQL; o default real é Postgres (`application-dev.yml`).
- App real precisa de Redis (cache, memória da Ana, sugestões) e RabbitMQ (notificações). Memória da Ana: lista `ana:memory:{userId}:{conversationId}` com append atômico (Lua) e lock `ana:lock:...` (turno simultâneo na mesma conversa → 409). Redis fora do ar → Ana responde sem histórico e sem gravar.
- `TokenGateway.generateToken(userId, ...)` recebe o **id** do usuário (vira o `sub` do JWT).
- `domain` não importa `application`, `infrastructure`, Jackson nem `Resource`. Porta/gateway no domain, adapter no infrastructure; controllers mapeiam DTO ↔ `Input`/`Output` com MapStruct; sem comentários.
- Chaves do cache Redis têm prefixo de versão (`v4:`); ao mudar o tipo de um valor cacheado, subir a versão em `RedisConfig`.
- Schema: prod usa Flyway (`db/migration`, próximo é V5); dev usa `ddl-auto: update`; testes usam H2 create-drop sem Flyway e `src/test/resources/schema.sql` recria tabelas e views do ETL. `ApiApplicationTests` não roda no build (surefire só pega `*Test`).
- Métricas de IA: `undercontroll.ai.call` (feature, provider, model, outcome, finish_reason), `undercontroll.ai.tool.calls`, `undercontroll.insights.job`, idade do snapshot e da publicação. Nunca usar usuário, conversa ou texto como tag.
