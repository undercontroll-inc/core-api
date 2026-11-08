# Undercontroll Core API

CRM da oficina (eletrodoméstico pequeno). Java 21, Spring Boot 3.5, Maven Wrapper.

## Architecture

HTTP entra em `application/controller/impl`. Regras ficam em `domain/usecase` (portas `*Port` com records `Input`/`Output`). Persistência, JWT, Redis e LLM ficam em `infrastructure`. O Java **nunca** chama a API do Mercado Livre: lê só as views Postgres que o ETL grava (`vw_market_*`, contrato em `../etl/docs/core-api-contract.md`). Ana, insights, transcrição, importação de nota e leitura de etiqueta têm configuração própria (`undercontroll.ai.{ana|insights|transcription|stock-import|label-reading}.*`: provider, model, timeout, thinking); cada uma monta seu modelo via `ChatModelFactory` (autoconfig de chat do Spring AI desligada).

Insights seguem a publicação do ETL: `MarketSnapshotPoller` lê `vw_market_published_snapshot` e cria um `insight_job` por (snapshot, prompt-version, model_config_hash); `InsightJobWorker` pega o lease (UPDATE condicional) e roda `RunInsightJobImpl`: `InsightCandidateGenerator` (candidatos determinísticos) → draft do modelo com schema nativo → `InsightDraftValidator` (1 reparo com os erros) → `InsightNarrativeRenderer` → publicação em transação (`market_insight_publication`). O draft fica salvo na `insight_attempt`; falha de banco nunca chama o modelo de novo.

Histórico do chat: Postgres é a fonte da verdade (`chat_conversation` + `chat_message`, append-only, `seq` por conversa com `UNIQUE(conversation_id, seq)`); o Redis (`ana:memory:*`) é só a janela quente do modelo. `AnaTurnPreparer` grava a mensagem do usuário antes do modelo (`appendUserMessage`: cria em transação própria + `SELECT … FOR UPDATE` na conversa) e a resposta em `finish` (COMPLETED ou PARTIAL com `reason`; cancelamento/erro depois de texto enviado vira PARTIAL). Redis vazio/expirado → reidrata só pares completos do Postgres e faz `seed`. `conversationId` continua escolhido pelo cliente, único por usuário (`UNIQUE(user_id, public_id)`); excluída = soft delete, o id não volta (404) e `ChatHistoryPurgeJob` apaga após 30 dias. Leitura em `/v1/api/chats/conversations` com cursor keyset opaco (`ChatCursor`).

Ciclo da OS: `OrderStatus` = `RECEIVED → AWAITING_APPROVAL → IN_REPAIR → READY_FOR_PICKUP → DELIVERED` (+ `outcome` REPAIRED/NOT_REPAIRED). Toda mudança de status passa por `OrderLifecycle` (máquina de estados pura, tabela `ALLOWED`) via `OrderLifecycleService` (transação, `order_status_event`, métricas, eventos `order.status.changed`/`order.budget.decided` depois do commit em `AfterCommitOrderEventPublisher`). `/transitions`, `budget/send`, `budget/decision` e o link público (`PublicOrderService`, ator `CUSTOMER_LINK`) usam o mesmo serviço; PATCH não muda status (400). `@Version` em `orders`: perdedor → `OrderConcurrentUpdateException` (409 `ORDER_CONCURRENT_UPDATE`). Página do cliente `/p/{token}` (Thymeleaf `templates/public/`, sem JS, CSP sem script) e `/v1/public/orders/{token}`: token base62 de 22 caracteres (`TrackingTokens`), expira 60 dias após a entrega, só primeiro nome/aparelho/valores/status. Dados da loja em `undercontroll.shop.*` (`ShopProfile`).

Storage: S3 em prod e MinIO no `docker compose` (dev), ambos pelo mesmo `S3ObjectStorageAdapter` (`ObjectStorageGateway`). Cliente nunca envia arquivo pela API: escrita só por PUT pré-assinado em duas fases (`StorageUploadService.request` grava `storage_upload` PENDING e assina `Content-Type`/`Content-Length`; o `complete` faz `stat` + magic bytes, apaga e rejeita se não bater). Leitura só por GET pré-assinado (`presignRead`, cache Caffeine reaproveita a URL até metade do TTL, com `response-cache-control`). Servidor só faz `put` do que ele mesmo gera (PDF). Política por área (prefixo, tipos, tamanho, TTLs) em `StorageArea`; chaves sempre `{prefixo}/{dono}/{uuid}.{ext}` geradas no servidor. `StorageUploadSweeper` apaga uploads abandonados.

Importação de nota (`/v1/api/stock-imports`, ADMINISTRATOR): o arquivo sobe direto para o storage (`/uploads` → PUT pré-assinado), o POST com `uploadId` valida o objeto e devolve 202 PROCESSING (`stock_import.storage_key`; `stock_import_file` só para importações antigas); `StockImportWorker` (dispatch imediato no `taskExecutor` + varredura) pega o lease e roda `RunStockImportImpl`: NF-e XML via `DomNfeXmlParser` (sem IA) ou foto/PDF via modelo multimodal com schema nativo (`InvoiceExtractionSchema`) → `ComponentMatcher` (alias aprendido `component_supplier_alias` > similaridade de tokens, sugestão só com score ≥ 0.75; o modelo nunca escolhe componente) → READY. Confirmar é uma transação: trava a importação e cada `component` (`ComponentGateway.addStock`), cria os novos e grava os aliases.

## Commands

Rodar na raiz deste repositório.

```bash
./mvnw test
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
./mvnw clean package -DskipTests
./mvnw -Plive-ai verify   # smoke real (LIVE_AI_PROVIDER + chave); fora do build padrão, cobrado
docker compose up -d    # postgres, rabbitmq, redis, minio (console :9001, minioadmin/minioadmin)
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
- Insights: o modelo só escolhe `candidate_id` e escreve `text`/`action` com `{{placeholders}}` do próprio candidato; números, meses e nomes vêm do servidor. Schema nativo (`StructuredOutputChatOptions.setOutputSchema`) com os limites de `InsightContract` (fonte única). Mudou prompt ou contrato → subir `prompt-version`. Retry só para TRANSIENT/RATE_LIMITED/TIMEOUT (`LlmErrorClassifier` por tipo/status, nunca por texto), no máximo 2, com jitter e `Retry-After` dentro do `job-deadline`. `provider: none` desliga só aquela funcionalidade (Ana: 503). Provider inválido, chave ausente ou `thinking` não suportado pelo modelo derrubam a inicialização com a chave no erro. Gemini precisa de `GEMINI_API_KEY` (ou `GOOGLE_API_KEY`); OpenAI de `OPENAI_API_KEY`. `ANA_PROVIDER`/`TRANSCRIPTION_PROVIDER`/`STOCK_IMPORT_PROVIDER`/`LABEL_READING_PROVIDER` caem em `INSIGHTS_PROVIDER` se ausentes.
- Stock import: upload até 10MB por URL pré-assinada (multipart global 8MB/9MB, só transcrição usa), JPEG/PNG/WEBP/PDF (≤ 5 páginas)/XML detectados pelos bytes; HEIC rejeitado. Chave de acesso/QR não busca XML na SEFAZ (precisa de certificado A1). Estoque é inteiro: confirmar exige quantidade inteira. Todo 4xx do controller sai com `code`. Mudou prompt ou `InvoiceExtractionSchema` → subir `InvoiceExtractionSchema.VERSION`.
- Cache do histórico do chat nunca usa `@CacheEvict`: páginas de mensagem antigas são imutáveis (chave por `beforeSeq`), a página mais recente usa `lastSeq` na chave, e a lista de conversas usa a versão `ana:chats:ver:{userId}` (INCR em todo turno, renomear e excluir; `-1` = Redis fora, sem cache). Valores cacheados usam `ArrayList` (o serializer com default typing não desserializa `List.of`/`toList()`). Falha de cache cai no banco (`LoggingCacheErrorHandler` em `RedisConfig`).
- Storage: nunca devolver URL do bucket sem assinatura nem gravar bytes do cliente pelo backend. URL assinada não vai para cache Redis (sempre calculada ao montar a resposta). `STORAGE_PUBLIC_ENDPOINT` é o host que o cliente enxerga (entra na assinatura): no celular use o IP da rede local, não `localhost`.
- OS: ownership em GET por id e export (`OrderAccess`; outro cliente → 404). `photos[]`/`statusHistory[]` só no detalhe (`OrderDetailsAssembler`), nunca em listas. Dashboard: receita, lucro, preço médio e top peças excluem `outcome = NOT_REPAIRED`; tempo médio = `completed_at − received_at` só REPAIRED. Fotos em `order-photos/{orderId}/` no bucket privado, URL pré-assinada de 15 min, máx. 6 por OS. Rotas `/p/**` e `/v1/public/**` são públicas com bucket próprio de 30 req/min/IP (`RateLimitFilter`). Texto de WhatsApp em `OrderMessages` (pt-BR).
- Texto visível de insight: mês por extenso ("mês de agosto de 2026"), nunca `2026-08` nem a palavra `bucket`.
- GET `/v1/api/insights` sempre responde com `state` (READY, GENERATING, FAILED, STALE, AWAITING_DATA); só READY vai para o cache. Sem snapshot publicado = AWAITING_DATA. POST `/v1/api/market/insights/jobs` (ADMINISTRATOR) devolve 202.
- Mediana de preço só com `valid_price_count >= 3`; variação de preço só com `consecutive` e `paired_count` mínimo (`MarketEvidence`). `available_offer_count` pode ser nulo.
- `spring.jpa.open-in-view: false`. Associação LAZY só dentro de `@Transactional` (ou EntityGraph / fetch join). Não mapear proxy depois da sessão fechada.
- Sugestões da Ana saem de fatos do banco (`GroundedQuestionGenerator.groundedQuestions`), sem LLM. O briefing do chat usa o mesmo snapshot (abertos, pickup, estoque, demandas, aviso). Listagens da oficina usam query limitada, não `findAll` da tabela toda.
- Não commitar `.env`, credenciais, nem `jwt.secret` real.

## Gotchas

- README ainda cita H2/MySQL; o default real é Postgres (`application-dev.yml`).
- App real precisa de Redis (cache, memória da Ana, sugestões) e RabbitMQ (notificações). Memória da Ana: lista `ana:memory:{userId}:{conversationId}` com append atômico (Lua) e lock `ana:lock:...` (turno simultâneo na mesma conversa → 409). Redis fora do ar → Ana responde sem histórico e sem gravar.
- `TokenGateway.generateToken(userId, ...)` recebe o **id** do usuário (vira o `sub` do JWT).
- `domain` não importa `application`, `infrastructure`, Jackson nem `Resource`. Porta/gateway no domain, adapter no infrastructure; controllers mapeiam DTO ↔ `Input`/`Output` com MapStruct; sem comentários.
- Chaves do cache Redis têm prefixo de versão (`v5:`); ao mudar o tipo de um valor cacheado, subir a versão em `RedisConfig`.
- Schema: prod usa Flyway (`db/migration`, próximo é V9); `LegacyOrderStatusBackfill` aplica no dev (sem Flyway) os mesmos UPDATEs de status do V7; dev usa `ddl-auto: update`; testes usam H2 create-drop sem Flyway e `src/test/resources/schema.sql` recria tabelas e views do ETL. `ApiApplicationTests` não roda no build (surefire só pega `*Test`).
- Métricas de IA: `undercontroll.ai.call` (feature, provider, model, outcome, finish_reason), `undercontroll.ai.tool.calls`, `undercontroll.insights.job`, idade do snapshot e da publicação. Nunca usar usuário, conversa ou texto como tag.
