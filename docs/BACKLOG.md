# BACKLOG — agent-task-router (formato Jira)

> Atualizado: 2026-08-01. Este arquivo é o board de execução — tickets, prioridade, critério de pronto.
> `DEVELOPMENT_PLAN.md` continua sendo o guia de conceito/recurso de estudo por fase; este arquivo é
> o board de tarefas real, granular, no formato usado em `task-now.md`.
>
> **Regra nova em todo ticket:** além do Acceptance Criteria técnico, todo ticket tem um campo
> **Prova de entendimento** — pergunta que só pode ser respondida de memória, sem abrir código/doc,
> antes de marcar o ticket como Done. Motivo registrado em
> `wiki/career/avaliacao-mentores-ago2026.md`: o gap real não é capacidade, é aprendizado raso — ticket
> "funciona" não é o mesmo que ticket "entendido". Sem essa prova, o ticket volta pra Doing.
>
> Estado real verificado em 01/08 (não o que o DEVELOPMENT_PLAN.md registra por último, que é de 09/06):
> zero arquivo de `RoutingEngine`, `ExecutionEngine`, `Execution` existe no código. Zero teste real além
> do smoke test default do Spring Boot. `.github/workflows/` vazio — nenhum CI configurado ainda, apesar
> do deploy manual já estar live no Railway.

---

## ÉPICO: Fase 2 — Routing (em andamento)

### 🎫 ATR-101 — RoutingEngine string-match
Já ticketado em `wiki/task-now.md` como JAVA-RT-01. Mesmo escopo, ID canônico aqui é ATR-101.
**Prioridade:** 🔴 Highest · **Pontos:** 3 · **Status:** ✅ Done (03/08, commit `e6212ef`)

### 🎫 ATR-102 — TaskRepository.findByStatus
**Tipo:** Task · **Prioridade:** 🟠 High · **Pontos:** 1 · **Status:** ✅ Done (05/08, commit `fc7f5d2`)

**Descrição:** Adicionar método derivado `List<Task> findByStatus(TaskStatus status)` em `TaskRepository`. Spring Data JPA gera a query pelo nome do método — sem SQL manual.

**Acceptance Criteria:**
- [x] Método adicionado, compila
- [x] Testado manualmente com pelo menos 2 tasks em status diferentes

**Prova de entendimento:** Por que Spring Data JPA consegue gerar essa query só pelo nome do método, sem você escrever nada? O que aconteceria se o nome fosse `findByTaskStatus` ao invés de `findByStatus`?

---

### 🎫 ATR-103 — GET /tasks?status= (filtro)
**Tipo:** Story · **Prioridade:** 🟠 High · **Pontos:** 2 · **Status:** ✅ Done (05/08, commit `fc7f5d2`)
**Bloqueador:** ATR-102

**Descrição:** Endpoint `GET /tasks` aceita `@RequestParam(required = false) TaskStatus status`. Se presente, usa `findByStatus`; se ausente, `findAll()`.

**Acceptance Criteria:**
- [x] `GET /tasks` sem param retorna tudo — testado via API real
- [x] `GET /tasks?status=PENDING` retorna só as pendentes — testado via API real
- [ ] Status inválido (string que não bate com o enum) retorna 400, não 500 — **não testado, gap real**. Comportamento atual não verificado.

**Prova de entendimento:** Como o Spring converte a string da URL (`?status=PENDING`) pro enum `TaskStatus` sozinho? O que você precisaria fazer se quisesse aceitar `pending` minúsculo também?

---

### 🎫 ATR-104 — Wire RoutingEngine em TaskService.submit()
**Tipo:** Task · **Prioridade:** 🔴 Highest · **Pontos:** 1 · **Status:** ✅ Done (03/08, commit `1750526`)
**Bloqueador:** ATR-101

**Descrição:** Já coberto pelo Acceptance Criteria de ATR-101/JAVA-RT-01 — ticket separado só se a integração não sair junto na mesma sessão.

**Prova de entendimento:** Se o `RoutingEngine` lançasse exceção ao invés de retornar `Optional.empty()` quando não acha agente, o que quebraria no fluxo de `submit()`?

---

**Fase 2 completa** — todos os 4 tickets fechados. Próximo épico real: Fase 3 (Execution Engine), abaixo.

---

## ÉPICO: Fase 3 — Execution Engine (não iniciada)

### 🎫 ATR-201 — Execution entity + migration V4
**Tipo:** Story · **Prioridade:** 🟠 High · **Pontos:** 3 · **Status:** ✅ Done (05/08, commit `ef2a2d1`)

**Descrição real (revisada):** tabela `executions` já existia desde o `V3` — não foi criada do zero. `V4` só adicionou a coluna `error_message` que faltava. Entidade `Execution` criada em pacote próprio (`execution/`), `ExecutionStatus` (RUNNING/SUCCESS/FAILED) como enum novo, separado de `TaskStatus`.

**Acceptance Criteria:**
- [x] Migration roda limpo (`V4`, confirmado no log de boot)
- [~] Relação `Execution → Task` — **escopo reduzido conscientemente**: campos `taskId`/`agentId` são `UUID` puro, sem `@ManyToOne`, mesmo padrão já usado em `Task.agentId`. Sem relação JPA navegável por enquanto.
- [~] `ExecutionRepository` — básico (extends JpaRepository), sem `findByTaskId` ainda — não foi necessário pro escopo atual, fica pra quando o `ExecutionEngine` (ATR-203) precisar consultar por task

**Prova de entendimento:** Por que `Execution` é uma entidade separada de `Task`, e não só um campo `status` a mais na própria `Task`? O que essa separação permite fazer que um campo único não permitiria?

---

### 🎫 ATR-202 — Spring State Machine (PENDING→RUNNING→COMPLETED/FAILED)
**Tipo:** Story · **Prioridade:** 🟠 High · **Pontos:** 5 · **Status:** ✅ Done (06/08, commit `0fa2513`) — escopo reduzido, ver nota
**Bloqueador:** ATR-201

**Descrição:** Configurar Spring State Machine com as transições válidas do `TaskStatus` real (4 estados: `PENDING, RUNNING, COMPLETED, FAILED` — não os 5 originalmente planejados, `ROUTING`/`EXECUTING` nunca existiram no enum real). Task só pode ir de PENDING→RUNNING, e de RUNNING só pra COMPLETED ou FAILED.

**Acceptance Criteria:**
- [x] Config de state machine define as transições válidas (`TaskStateMachineConfig`, `TaskEvent`)
- [~] Transição inválida rejeitada — confirmado pelo comportamento documentado da lib (evento sem transição válida é ignorado silenciosamente, estado não muda), **não coberto por teste automatizado** — pulado por decisão do Matheus (06/08)
- [ ] Estado persistido junto da `Execution` — **não feito**. A state machine existe mas não está plugada em nenhum lugar do código ainda: nenhum endpoint ou service chama `sendEvent`. Isso é trabalho do ATR-203 (`ExecutionEngine`), que vai efetivamente disparar as transições.

**Nota de escopo (06/08):** ticket fechado como "config validada e app sobe limpo com as 3 transições registradas", não como "state machine em uso real". Verificado via `./mvnw compile` + `spring-boot:run` (boot limpo, sem erro `Must have at least one transition`), não via teste unitário ou chamada de API real — ainda não existe caminho de API que dispare um evento.

**Prova de entendimento:** Desenhe de memória (papel ou fala) o diagrama de estados completo com as transições permitidas. O que faz um evento (`TaskEvent`) ser diferente de um estado (`TaskStatus`) na Spring State Machine?

---

### 🎫 ATR-203 — ExecutionEngine com @Async
**Tipo:** Story · **Prioridade:** 🟠 High · **Pontos:** 5 · **Status:** ✅ Done (10/08, commits `ca70b3f`, `c86d21b`, `f9497b3`)
**Bloqueador:** ATR-201, ATR-202

**Descrição:** `ExecutionEngine.execute(Task task)` roda em thread separada (`@Async`), chamado automaticamente por `TaskService.create()` só quando um agente foi atribuído. Persiste `RUNNING` → `COMPLETED` na Task — não simula trabalho real do agente ainda (fora de escopo).

**Acceptance Criteria:**
- [x] `@EnableAsync` configurado (`ProcurementApiApplication`)
- [x] `create()` não bloqueia a resposta HTTP — provado com delay artificial de 3s em `execute()` (removido depois), `TaskServiceTest` confirma retorno em <500ms
- [x] Estado da Task muda de forma observável via `GET /tasks/{id}` — `RUNNING` → `COMPLETED` persistido via `TaskRepository`

**Gap real, não escondido:** transições de status são atribuição direta de campo (`task.setStatus(...)`), não passam pela `TaskStateMachineConfig` do ATR-202 — nenhuma validação de transição acontece nesse ponto do código ainda.

**Prova de entendimento:** Sem `@Async`, o que aconteceria com a requisição HTTP se a "execução" demorasse 5 segundos? Por que isso é um problema real num roteador de tasks com múltiplos agentes?

---

### 🎫 ATR-204 — POST /tasks/{id}/retry
**Tipo:** Task · **Prioridade:** 🟡 Medium · **Pontos:** 2 · **Status:** 🔲 To Do
**Bloqueador:** ATR-203

**Descrição:** Só permite retry se a última `Execution` da task está em FAILED. Cria nova `Execution`, reseta o estado da Task pra ROUTING.

**Acceptance Criteria:**
- [ ] Retry em task SUCCESS ou EXECUTING retorna 409 (conflito), não executa
- [ ] Retry em task FAILED cria nova Execution e reinicia o fluxo

**Prova de entendimento:** Por que reiniciar do estado ROUTING e não direto do EXECUTING? O agente que falhou da primeira vez pode ter ficado indisponível nesse meio tempo — como isso se conecta com o próprio RoutingEngine?

---

### 🎫 ATR-205 — Virtual Threads no executor
**Tipo:** Task · **Prioridade:** 🟢 Low · **Pontos:** 2 · **Status:** 🔲 To Do
**Bloqueador:** ATR-203

**Descrição:** Configurar o executor do `@Async` para usar Virtual Threads (Java 21) ao invés do thread pool tradicional.

**Acceptance Criteria:**
- [ ] `TaskExecutor` bean configurado com `Executors.newVirtualThreadPerTaskExecutor()`
- [ ] Testado com múltiplas execuções concorrentes (ex: 20 tasks disparadas ao mesmo tempo)

**Prova de entendimento:** Qual é a diferença real entre uma virtual thread e uma thread de plataforma tradicional? Por que isso importa especificamente pro caso de "várias tasks esperando resposta de agente ao mesmo tempo"?

---

## ÉPICO: Fase 4 — Fechar gaps de segurança (quase pronta)

### 🎫 ATR-301 — UserDetailsService customizado
**Tipo:** Task · **Prioridade:** 🟠 High · **Pontos:** 2 · **Status:** 🔲 To Do

**Contexto:** `AuthController` hoje acessa `UserRepository` diretamente no login/registro, contornando a abstração do Spring Security — confirmado ausente em 2026-07-29.

**Descrição:** Criar `CustomUserDetailsService implements UserDetailsService`, plugar no `AuthenticationManager` do `SecurityConfig`. Login passa a usar o fluxo padrão do Spring Security, não acesso direto ao repositório.

**Acceptance Criteria:**
- [ ] `UserDetailsService` implementado e registrado
- [ ] Login continua funcionando via `/auth/login` sem regressão
- [ ] `AuthController` não chama mais `UserRepository` diretamente pra autenticar

**Prova de entendimento:** Qual problema concreto o acesso direto ao repositório cria que o `UserDetailsService` resolve? (Dica: pense em o que mais no Spring Security espera essa abstração pra funcionar — ex: `@PreAuthorize`.)

---

### 🎫 ATR-302 — Role-based @PreAuthorize
**Tipo:** Task · **Prioridade:** 🟡 Medium · **Pontos:** 2 · **Status:** 🔲 To Do
**Bloqueador:** ATR-301

**Descrição:** O campo `role` já existe em `User` mas nada o aplica. Adicionar `@PreAuthorize("hasRole('ADMIN')")` (ou equivalente) em endpoints administrativos (ex: criar/deletar agentes).

**Acceptance Criteria:**
- [ ] Ao menos 2 endpoints protegidos por role
- [ ] Usuário sem a role correta recebe 403, não 500 nem 200

**Prova de entendimento:** Por que `@PreAuthorize` não funciona sem o `UserDetailsService` do ATR-301 estar correto? O que exatamente ele lê pra saber a role do usuário autenticado?

---

## ÉPICO: Fase 5 — Routing semântico (não iniciada)

### 🎫 ATR-401 — pgvector + coluna embedding
**Tipo:** Story · **Prioridade:** 🟢 Low · **Pontos:** 3 · **Status:** 🔲 To Do

**Descrição:** Adicionar extensão pgvector no docker-compose e no banco de produção. Migration `V5` adiciona coluna `embedding vector(1536)` em `agents`.

**Acceptance Criteria:**
- [ ] pgvector habilitado localmente e testado
- [ ] Migration aplicada sem quebrar dados existentes

**Prova de entendimento:** O que um vetor de embedding representa, em termos práticos, pra permitir "buscar o agente mais parecido com essa task"? Por que uma string simples (o que ATR-101 usa) não escala pra esse caso?

---

### 🎫 ATR-402 — Spring AI + geração de embedding no registro do agente
**Tipo:** Story · **Prioridade:** 🟢 Low · **Pontos:** 5 · **Status:** 🔲 To Do
**Bloqueador:** ATR-401

**Descrição:** Ao registrar um agente (`POST /agents`), gerar embedding da descrição de capabilities via API da Claude/OpenAI (Spring AI abstrai o provider), salvar na coluna `embedding`.

**Acceptance Criteria:**
- [ ] Embedding gerado e salvo automaticamente no registro
- [ ] Chave de API isolada em variável de ambiente, nunca hardcoded

**Prova de entendimento:** Se dois agentes têm descrições de capability com palavras completamente diferentes mas significado parecido (ex: "processa PDFs" vs "extrai texto de documentos"), o que faz o embedding capturar essa semelhança que o string-match do ATR-101 não capta?

---

### 🎫 ATR-403 — EmbeddingMatcher (routing semântico substitui string-match)
**Tipo:** Story · **Prioridade:** 🟢 Low · **Pontos:** 5 · **Status:** 🔲 To Do
**Bloqueador:** ATR-402

**Descrição:** Novo `EmbeddingMatcher` calcula similaridade de cosseno entre embedding da task e dos agentes ativos, retorna o mais próximo acima de um threshold. `RoutingEngine` passa a usar esse matcher como estratégia principal, com fallback pro string-match do ATR-101 se não houver embedding.

**Acceptance Criteria:**
- [ ] Similaridade de cosseno implementada (ou via função nativa do pgvector, `<=>`)
- [ ] Fallback pro string-match funcional se algum agente não tiver embedding
- [ ] Comparado lado a lado: mesmo cenário de teste do ATR-101 roteia igual ou melhor

**Prova de entendimento:** Por que manter o fallback pro string-match ao invés de simplesmente substituir? O que quebraria em produção se um agente for cadastrado sem API de embedding disponível no momento?

---

## ÉPICO: Fase 6 — Observabilidade (não iniciada)

### 🎫 ATR-501 — Métricas Micrometer
**Tipo:** Task · **Prioridade:** 🟡 Medium · **Pontos:** 2 · **Status:** 🔲 To Do

**Descrição:** Counters (tasks criadas, tasks roteadas, tasks falhadas), timers (tempo de roteamento, tempo de execução), gauges (agentes ativos).

**Acceptance Criteria:**
- [ ] Ao menos 3 counters e 1 timer implementados
- [ ] Visíveis em `/actuator/metrics`

**Prova de entendimento:** Qual a diferença prática entre um counter e um gauge? Dê um exemplo de métrica deste projeto que só faz sentido como gauge, nunca como counter.

---

### 🎫 ATR-502 — Endpoint Prometheus
**Tipo:** Task · **Prioridade:** 🟡 Medium · **Pontos:** 1 · **Status:** 🔲 To Do
**Bloqueador:** ATR-501

**Acceptance Criteria:**
- [ ] `/actuator/prometheus` expõe as métricas do ATR-501 em formato scrape-able
- [ ] Testado com `curl` local, formato validado

**Prova de entendimento:** O que "scrape" significa nesse contexto — quem chama esse endpoint e com que frequência?

---

### 🎫 ATR-503 — Dashboard Grafana
**Tipo:** Task · **Prioridade:** 🟢 Low · **Pontos:** 3 · **Status:** 🔲 To Do
**Bloqueador:** ATR-502

**Acceptance Criteria:**
- [ ] Grafana no docker-compose, conectado ao Prometheus local
- [ ] Ao menos 1 painel mostrando taxa de sucesso de roteamento ao longo do tempo

**Prova de entendimento:** Qual pergunta de negócio real esse painel responde que um log isolado não responde?

---

### 🎫 ATR-504 — Logging JSON estruturado
**Tipo:** Task · **Prioridade:** 🟡 Medium · **Pontos:** 2 · **Status:** 🔲 To Do

**Descrição:** Configurar Logback pra saída JSON, incluindo `traceId`, `taskId`, `agentId` em cada linha relevante.

**Acceptance Criteria:**
- [ ] Logs saem em JSON, não texto plano
- [ ] `taskId` presente em toda linha de log do ciclo de vida de uma task

**Prova de entendimento:** Por que log estruturado importa mais aqui do que num projeto pequeno sem múltiplos agentes concorrentes? O que você faria hoje, sem isso, se precisasse rastrear por que a task X falhou?

---

### 🎫 ATR-505 — OpenTelemetry + Jaeger
**Tipo:** Story · **Prioridade:** 🟢 Low · **Pontos:** 5 · **Status:** 🔲 To Do
**Bloqueador:** ATR-504

**Acceptance Criteria:**
- [ ] Spans criados para: recebimento da task, roteamento, execução
- [ ] Trace completo visível no Jaeger local (docker-compose) pra uma task de ponta a ponta

**Prova de entendimento:** Qual a diferença entre um log e um span de trace? Por que "tempo total de vida de uma task, através de 3 componentes diferentes" é mais fácil de ver com trace do que juntando logs na mão?

---

## ÉPICO: Fase 7 — Resiliência (não iniciada)

> Nota: Redis já está provisionado no docker-compose desde `ADR-011` (commit `a4fece7`), antes da implementação — decisão já tomada, falta só o uso.

### 🎫 ATR-601 — Cache `agents:active` no Redis
**Tipo:** Task · **Prioridade:** 🟡 Medium · **Pontos:** 3 · **Status:** 🔲 To Do

**Descrição:** `RoutingEngine` hoje bate direto no Postgres a cada task pra listar agentes ativos. Cachear essa lista no Redis, TTL de 30s.

**Acceptance Criteria:**
- [ ] Cache-aside implementado (lê Redis, se vazio busca Postgres e popula)
- [ ] TTL de 30s configurado e testado (esperar expirar, confirmar nova query ao Postgres)

**Prova de entendimento:** Por que 30s e não, por exemplo, 5 minutos? O que esse número está balanceando (dica: agente pode ficar inativo no meio da janela)?

---

### 🎫 ATR-602 — Lock distribuído (Redisson)
**Tipo:** Task · **Prioridade:** 🟢 Low · **Pontos:** 3 · **Status:** 🔲 To Do
**Bloqueador:** ATR-601

**Descrição:** Evitar que duas tasks concorrentes roteiem pro mesmo agente que só suporta 1 execução simultânea — lock por `agentId` durante a atribuição.

**Acceptance Criteria:**
- [ ] Lock adquirido antes de atribuir agente, liberado após persistir a atribuição
- [ ] Testado com 2 requisições simultâneas pro mesmo agente — só uma deve conseguir

**Prova de entendimento:** Por que um lock local (`synchronized` do Java) não resolveria esse problema se o projeto rodar em 2 instâncias? O que "distribuído" está resolvendo especificamente?

---

### 🎫 ATR-603 — Circuit Breaker (Resilience4j)
**Tipo:** Story · **Prioridade:** 🟡 Medium · **Pontos:** 3 · **Status:** 🔲 To Do

**Descrição:** Se um agente falhar 5 vezes seguidas, marcar como `OVERLOADED` por 60s, não rotear novas tasks pra ele nesse período.

**Acceptance Criteria:**
- [ ] Circuit breaker configurado com threshold de 5 falhas
- [ ] Estado `OVERLOADED` visível em `GET /agents/{id}`
- [ ] Volta a rotear automaticamente após os 60s

**Prova de entendimento:** Qual problema real esse padrão evita que "simplesmente tentar de novo sempre" não evita? Pense num agente que está fora do ar de verdade — o que acontece sem circuit breaker vs. com ele.

---

### 🎫 ATR-604 — @Retry
**Tipo:** Task · **Prioridade:** 🟢 Low · **Pontos:** 1 · **Status:** 🔲 To Do
**Bloqueador:** ATR-603

**Acceptance Criteria:**
- [ ] Retry automático (ex: 3 tentativas, backoff exponencial) na chamada ao agente antes de considerar falha definitiva

**Prova de entendimento:** Por que retry e circuit breaker são coisas diferentes que se complementam, e não a mesma ideia com nome diferente?

---

## ÉPICO: Fase 8 — Testes e CI/CD (não iniciada — só smoke test default existe hoje)

### 🎫 ATR-701 — Testes unitários (Mockito)
**Tipo:** Story · **Prioridade:** 🔴 Highest · **Pontos:** 5 · **Status:** 🔲 To Do

**Descrição:** `AgentServiceTest`, `TaskServiceTest`, `RoutingEngineTest` — mockando repositórios, testando lógica de negócio isolada.

**Acceptance Criteria:**
- [ ] Ao menos 8 testes unitários cobrindo os caminhos principais (feliz + erro) de Agent/Task/Routing
- [ ] Rodam via `mvn test` sem depender de banco real

**Prova de entendimento:** Por que esses testes usam mock do repositório e não o banco real? O que exatamente isso torna mais rápido e mais confiável de rodar?

---

### 🎫 ATR-702 — Testes de integração (MockMvc)
**Tipo:** Story · **Prioridade:** 🟠 High · **Pontos:** 5 · **Status:** 🔲 To Do
**Bloqueador:** ATR-701

**Descrição:** Testes via `MockMvc` cobrindo os controllers HTTP reais (`POST /tasks`, `POST /agents`, `POST /auth/login`), validando status code e corpo da resposta.

**Acceptance Criteria:**
- [ ] Ao menos 5 testes de integração cobrindo os endpoints principais
- [ ] Inclui um teste de caso de erro (ex: payload inválido → 400)

**Prova de entendimento:** Qual a diferença real entre o que ATR-701 testa e o que este ticket testa? Se ambos passam, o que ainda pode estar quebrado?

---

### 🎫 ATR-703 — Testcontainers
**Tipo:** Story · **Prioridade:** 🟠 High · **Pontos:** 5 · **Status:** 🔲 To Do
**Bloqueador:** ATR-702

**Descrição:** Teste end-to-end usando Postgres real via Testcontainers (não H2, não mock) — sobe um container Postgres real pro teste, roda o fluxo completo: registrar agente → submeter task → verificar roteamento.

**Acceptance Criteria:**
- [ ] Testcontainers configurado, container Postgres real sobe/desce no ciclo de teste
- [ ] Fluxo completo (agent → task → routing) validado de ponta a ponta

**Prova de entendimento:** Por que usar Postgres real aqui e não H2 em memória, que seria mais rápido? O que H2 pode esconder que só aparece com o banco real (dica: pgvector do ATR-401, tipos específicos do Postgres)?

---

### 🎫 ATR-704 — Cobertura JaCoCo ≥70%
**Tipo:** Task · **Prioridade:** 🟡 Medium · **Pontos:** 2 · **Status:** 🔲 To Do
**Bloqueador:** ATR-701, ATR-702

**Acceptance Criteria:**
- [ ] Plugin JaCoCo configurado no `pom.xml`
- [ ] Relatório gerado, cobertura real ≥70% nas classes de negócio (não conta getter/setter)

**Prova de entendimento:** Por que 70% e não 100%? O que faz sentido não testar (dica: getters/setters, configuração)?

---

### 🎫 ATR-705 — GitHub Actions CI (`ci.yml`)
**Tipo:** Task · **Prioridade:** 🔴 Highest · **Pontos:** 3 · **Status:** 🔲 To Do
**Bloqueador:** ATR-701

**Descrição:** Workflow roda `mvn test` a cada push/PR. Hoje `.github/workflows/` está vazio — nenhum CI existe, apesar do deploy manual já estar live.

**Acceptance Criteria:**
- [ ] `.github/workflows/ci.yml` criado
- [ ] Roda testes automaticamente em push pra qualquer branch
- [ ] Badge de status adicionado ao README

**Prova de entendimento:** Qual é o risco real de ter deploy funcionando (Railway) mas zero CI? O que pode ir pra produção sem ninguém perceber?

---

### 🎫 ATR-706 — GitHub Actions CD (`deploy.yml`)
**Tipo:** Task · **Prioridade:** 🟡 Medium · **Pontos:** 3 · **Status:** 🔲 To Do
**Bloqueador:** ATR-705

**Descrição:** Nota: deploy já é feito manualmente no Railway (live). Este ticket é sobre automatizar esse deploy via GitHub Actions, disparado só após CI verde.

**Acceptance Criteria:**
- [ ] Deploy automático dispara em merge na branch principal, só se ATR-705 passou
- [ ] Rollback manual documentado (não precisa ser automático)

**Prova de entendimento:** Por que o deploy deve depender do CI verde e não rodar sempre? O que aconteceria se um push quebrado fosse direto pro Railway?

---

### 🎫 ATR-707 — docker-compose.prod.yml
**Tipo:** Task · **Prioridade:** 🟢 Low · **Pontos:** 2 · **Status:** 🔲 To Do

**Descrição:** Separar config de produção (sem volumes de dev, com variáveis de ambiente reais) do `docker-compose.yml` de desenvolvimento local.

**Acceptance Criteria:**
- [ ] Arquivo separado, sem segredo nenhum commitado
- [ ] Testado localmente simulando produção (`docker compose -f docker-compose.prod.yml up`)

**Prova de entendimento:** O que muda de verdade entre dev e prod nesse compose, além de "menos coisas ligadas"?

---

## 📊 STATUS GERAL

| Épico | Tickets | Pontos totais | Status |
|---|---|---|---|
| Fase 2 — Routing | ATR-101 a 104 | 7 | 🟠 Em andamento |
| Fase 3 — Execution Engine | ATR-201 a 205 | 17 | 🔲 Não iniciada |
| Fase 4 — Segurança | ATR-301 a 302 | 4 | 🔲 Não iniciada |
| Fase 5 — Routing semântico | ATR-401 a 403 | 13 | 🔲 Não iniciada |
| Fase 6 — Observabilidade | ATR-501 a 505 | 13 | 🔲 Não iniciada |
| Fase 7 — Resiliência | ATR-601 a 604 | 10 | 🔲 Não iniciada |
| Fase 8 — Testes e CI/CD | ATR-701 a 707 | 25 | 🔲 Não iniciada |

**Total:** 89 pontos, 27 tickets restantes. Ritmo do `DEVELOPMENT_PLAN.md` (45-60min/dia, 5-6x/sem) sugere ~1 ticket pequeno (1-2pts) por dia, ou 2-3 dias por ticket de 5pts. Não pular fase — Fase 3 depende de rotear funcionar (Fase 2), Fase 5 depende de rotear existir pra ter o que substituir.

---

*Ordem de execução recomendada: fechar Fase 2 (ATR-101 a 104, já em `task-now.md`) → ATR-701/705 (testes e CI, porque sem isso todo o resto que vier depois é built on sand) → Fase 3 → Fase 4 (rápida, 2 tickets) → Fase 6/7 em paralelo com Fase 3 se quiser variar → Fase 5 por último, é a mais especulativa e a que menos importa pra portfolio hoje.*
