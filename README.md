# Order Saga with Camunda 8 (Java 25)

Projeto independente para demonstrar uma Saga orquestrada por BPMN 2.0 com workers Spring Boot. Todas as operações de negócio são simuladas: não há cobrança real.

## Fluxo

1. Reservar estoque.
2. Simular cobrança.
3. Reservar entrega.
4. Se a entrega falhar, o evento de erro BPMN inicia a compensação: estornar pagamento e liberar estoque.

## Requisitos

- JDK 25
- Maven 3.9+
- Docker Desktop com Docker Compose

## Executar localmente

Terminal 1 — iniciar o Camunda 8 local:

```bash
docker compose up -d
```

Terminal 2 — iniciar a API e os workers:

```bash
mvn spring-boot:run
```

Na inicialização, a aplicação publica automaticamente `processes/order-saga.bpmn`. A API fica em http://localhost:8081 se executar com a configuração abaixo, ou na porta configurada em `server.port`.

## Criar pedido de teste

Sucesso:

```bash
curl -X POST http://localhost:8081/api/orders -H "Content-Type: application/json" -d '{"orderId":"pedido-200","customerId":"cliente-1","sku":"CAMISETA-1","quantity":1,"amount":49.90,"currency":"BRL"}'
```

Falha simulada na entrega — deve executar estorno e liberação de estoque:

```bash
curl -X POST http://localhost:8081/api/orders -H "Content-Type: application/json" -d '{"orderId":"pedido-201","customerId":"cliente-1","sku":"FAIL_DELIVERY","quantity":1,"amount":49.90,"currency":"BRL"}'
```

## Testes

```bash
mvn --batch-mode verify
```

O teste automatizado verifica que o BPMN está no classpath, é XML válido e contém os tipos de job e o evento de erro usados pela Saga. O teste não substitui um teste de integração contra um cluster Camunda real.

## Configuração e segurança

O `application.yaml` usa endpoints locais sem autenticação, somente para desenvolvimento isolado. Para clusters remotos, configure autenticação e TLS e nunca armazene segredos no Git. A configuração Docker Compose é para avaliação local, não produção.

Cada worker é um adapter de demonstração. Em integrações reais, use chaves de idempotência, retries/timeouts apropriados, logs estruturados e compensações seguras.

Referências:
- [Camunda Spring Boot Starter](https://docs.camunda.io/docs/apis-tools/camunda-spring-boot-starter/getting-started/)
- [Camunda Java Client](https://docs.camunda.io/docs/apis-tools/java-client/getting-started/)
- [Camunda Docker Compose quickstart](https://docs.camunda.io/docs/self-managed/quickstart/developer-quickstart/docker-compose/)
