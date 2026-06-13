# Java/Spring Boot 4 微服務應用 — Testcontainers 學習專案

> 用 Java 21 + Spring Boot + Testcontainers，從零打造三個微服務（商品 / 支付 / 庫存），
> 走通 Hexagonal Architecture（六角架構）+ DDD + CQRS，並以「真實容器」驗證完整下單流程。

這份 README 是一份**完整的初學者教程**。即使你沒寫過 Testcontainers、沒做過 DDD，
跟著從第 0 章讀到第 7 章，最後就能跑通一個含 5 個基礎設施容器、3 個 Spring Boot 服務的 E2E 測試。

---

## 目錄

- [第 0 章：這份教程在教什麼](#第-0-章這份教程在教什麼)
- [第 1 章：開發環境準備](#第-1-章開發環境準備)
- [第 2 章：跑起來看看（5 分鐘上手）](#第-2-章跑起來看看5-分鐘上手)
- [第 3 章：專案結構與分層](#第-3-章專案結構與分層)
- [第 4 章：核心概念入門](#第-4-章核心概念入門)
  - [4.1 為什麼要用 Hexagonal Architecture](#41-為什麼要用-hexagonal-architecture六角架構)
  - [4.2 DDD 的 Aggregate / Value Object 入門](#42-ddd-的-aggregate--value-object-入門)
  - [4.3 CQRS：把讀和寫分開](#43-cqrs把讀和寫分開)
  - [4.4 Testcontainers 為什麼好用](#44-testcontainers-為什麼好用)
- [第 5 章：章節走讀](#第-5-章章節走讀)
  - [5.1 Shared Kernel（共用內核）](#51-shared-kernel共用內核)
  - [5.2 Product Service（商品 / 訂單）](#52-product-service商品--訂單)
  - [5.3 Payment Service（支付）](#53-payment-service支付)
  - [5.4 Inventory Service（庫存）](#54-inventory-service庫存)
  - [5.5 E2E：跨服務全鏈路測試](#55-e2e跨服務全鏈路測試)
- [第 6 章：Contract Testing — 把「介面合約」變成自動測試](#第-6-章contract-testing--把介面合約變成自動測試)
  - [6.1 兩種層級對照](#61-兩種層級對照)
  - [6.2 Port 行為合約（intra-service）](#62-port-行為合約intra-service)
  - [6.3 Consumer-Driven Contract（inter-service）](#63-consumer-driven-contractinter-service)
  - [6.4 Contract Testing 的「投入產出」決策](#64-contract-testing-的投入產出決策)
- [第 7 章：怎麼跑測試](#第-7-章怎麼跑測試)
- [第 8 章：故障排除 FAQ](#第-8-章故障排除-faq)
- [延伸閱讀](#延伸閱讀)

---

## 第 0 章：這份教程在教什麼

### 學完之後你會擁有

1. **三個 Spring Boot 微服務**，分別處理「商品下單」、「支付」、「庫存扣減」。
2. **75 個自動化測試**，分布在三個層級：
   - 純單元測試（Domain / Application Service，毫秒級）
   - Adapter 整合測試（單一容器，秒級）
   - E2E 全鏈路測試（5 容器 + 3 個 child JVM，46 秒）
3. **Hexagonal Architecture + DDD + CQRS** 三個模式的真實應用範例。
4. **Testcontainers 跨技術棧用法**：PostgreSQL、Redis、Kafka、Elasticsearch、MinIO、Vault、Toxiproxy 都有。

### 主要技術棧

| 類別             | 技術                                                                  |
|------------------|-----------------------------------------------------------------------|
| 語言             | Java 21（Record、Sealed Interface、Pattern Matching、Virtual Threads）|
| 框架             | Spring Boot 3.4 / Spring Framework 6.2                                |
| Build            | Gradle 8.14 + Kotlin DSL                                              |
| 測試             | JUnit 5、Testcontainers 1.20、Awaitility、ArchUnit、AssertJ            |
| Migration        | Flyway 10                                                              |
| 訊息             | Spring Kafka                                                           |
| 快取             | Spring Data Redis、Redisson（分散鎖）                                  |
| 搜尋             | Spring Data Elasticsearch                                              |
| 物件儲存         | AWS SDK v2（指向 MinIO，S3 相容）                                       |
| 機密管理         | Spring Vault                                                           |
| 認證             | Spring Security OAuth2 Resource Server                                 |

> **為什麼是 Spring Boot 3.4 而不是教學計畫裡的 4.0？**
> Spring Boot 4 GA 還未在 Maven Central 提供 stable artifact，3.4 已經是 `jakarta.*` namespace，
> 兩者語法 95% 共通；本教程的所有寫法在 4.0 上都能用，只需要升 BOM 版本。

---

## 第 1 章：開發環境準備

### 1.1 必裝工具

| 工具          | 最低版本 | 怎麼確認                              |
|---------------|----------|---------------------------------------|
| JDK 21        | 21.0+    | `java -version`                       |
| Gradle        | 8.x      | 用內建的 `./gradlew`，不需要全域安裝   |
| Docker 或 Podman | 任一新版 | `docker version` 或 `podman version` |

### 1.2 Mac + Podman 設定（推薦給沒裝 Docker Desktop 的）

如果你像作者一樣用 Podman：

```bash
# 安裝（如未安裝）
brew install podman

# 初始化 + 啟動，並給足夠 RAM 跑完整 E2E
podman machine init   # 第一次才需要
podman machine set --memory 6144
podman machine start

# 讓 Testcontainers 找得到 Docker socket（多數 podman 安裝會自動建這個 symlink）
ls -la /var/run/docker.sock
# 應該指向 ~/.local/share/containers/podman/machine/podman.sock
```

**為什麼要 6 GB？** 完整 E2E 同時起 5 個基礎設施容器 + 3 個 Spring Boot child JVM，
每個容器設了合理的記憶體上限（Elasticsearch 限 512 MB heap），總和約 3–4 GB。

### 1.3 Linux / Docker 設定

裝完 Docker Engine + Docker Compose（或 Docker Desktop），確認 daemon 在跑：

```bash
docker info | head -5
```

不需要其他額外設定。

### 1.4 IDE 設定（選用）

IntelliJ IDEA / VS Code 都可以，重點是：

- JDK 設成 21
- 啟用 Annotation Processing（Lombok 沒有，但 Spring Boot 的 configuration processor 可選）
- Gradle 設成「Wrapper」而不是本機 Gradle

---

## 第 2 章：跑起來看看（5 分鐘上手）

如果你只是想先看到效果：

```bash
# 1. clone 後進到 repo 根目錄
cd microservices-with-testcontainers-for-java

# 2. 跑所有「不需要容器」的單元測試（最快，秒回）
./gradlew :shared-kernel:test \
          :product-service:test --tests '*OrderTest' --tests '*PricingServiceTest' \
                                --tests '*PlaceOrderCommandHandlerTest' \
                                --tests '*SearchProductQueryHandlerTest' \
                                --tests '*HexagonalArchitectureTest' \
          :payment-service:test --tests '*PaymentTest' \
                                --tests '*ProcessPaymentCommandHandlerTest' \
                                --tests '*HexagonalArchitectureTest' \
          :inventory-service:test --tests '*StockTest' \
                                  --tests '*DeductInventoryCommandHandlerTest' \
                                  --tests '*HexagonalArchitectureTest'
```

預期看到約 **31 個測試通過**。

接著跑一個會啟動真實容器的整合測試試試水溫：

```bash
DOCKER_HOST=unix:///var/run/docker.sock \
TESTCONTAINERS_RYUK_DISABLED=true \
TESTCONTAINERS_CHECKS_DISABLE=true \
./gradlew :product-service:test --tests '*JpaOrderWriteRepositoryIT'
```

第一次會花 30–60 秒拉 `postgres:16-alpine` image，之後快很多。

最後 boss 關 — E2E：

```bash
DOCKER_HOST=unix:///var/run/docker.sock \
TESTCONTAINERS_RYUK_DISABLED=true \
TESTCONTAINERS_CHECKS_DISABLE=true \
./gradlew :e2e-tests:test
```

這會：
1. 起 5 個容器（PostgreSQL、Redis、Kafka、Vault、MinIO）
2. 編譯出 3 個 bootJar
3. 開 3 個 child JVM 跑 product / payment / inventory
4. 等每個服務的 `/actuator/health` 200 OK
5. POST 一筆訂單，跑完整鏈路，驗證庫存從 100 變 98

預期 **約 46 秒**通過。

> **環境變數說明**
> - `DOCKER_HOST`：告訴 Testcontainers Docker socket 在哪。Podman 在 macOS 上需要這個。
> - `TESTCONTAINERS_RYUK_DISABLED=true`：Ryuk 是 Testcontainers 的「清屍員」容器，會幫你
>   在 JVM 異常結束時清掉殘留容器。但它需要 privileged mode，Podman rootless 不支援，所以關掉。
>   代價是：如果你強制 kill JVM，殘留容器要 `podman ps -a` 自己清。
> - `TESTCONTAINERS_CHECKS_DISABLE=true`：跳過 Testcontainers 對 Docker daemon 的相容性檢查，
>   給 Podman 用比較順。

---

## 第 3 章：專案結構與分層

```
microservices-with-testcontainers-for-java/    # repo root
├── README.md                     # 本檔
├── settings.gradle.kts           # 列出所有子模組
├── build.gradle.kts              # 根層：BOM 統一版本
├── gradlew, gradlew.bat          # Gradle wrapper 腳本
├── gradle/wrapper/               # Gradle wrapper jar / properties
├── .github/workflows/ci.yml      # CI：unit → integration → e2e 三階段
├── docs/                         # 教學計畫、設計筆記
│   └── testcontainers-tutorial-plan-v2.md
│
├── shared-kernel/                # 跨服務共用：純 Java，無 Spring
│   └── src/main/java/com/example/ecommerce/shared/
│       ├── domain/               # Money、Quantity（Value Object）
│       ├── event/                # Integration Event 介面 + 5 個具體事件
│       └── port/                 # EventPublisher、ObjectStoragePort、SecretProvider
│
├── product-service/              # 商品 + 訂單服務
│   └── src/main/java/com/example/ecommerce/product/
│       ├── domain/               # 純 Java：Order、Product、PricingService、Port 介面
│       ├── application/          # Use Case 實作（Command/Query Handler）
│       ├── adapter/
│       │   ├── inbound/          # REST Controller、Kafka Consumer
│       │   └── outbound/         # JPA、Redis、ES、MinIO、Kafka 實作
│       └── config/               # Spring @Configuration
│
├── payment-service/              # 支付服務（結構同上）
├── inventory-service/            # 庫存服務（結構同上）
│
├── test-infrastructure/          # 測試共用：容器 helper、Podman 相容
│   └── src/main/java/com/example/ecommerce/test/
│       ├── containers/           # SharedContainers：singleton 容器定義
│       └── podman/               # PodmanCompatibility：自動關 Ryuk
│
└── e2e-tests/                    # 全鏈路測試
    └── src/test/java/com/example/ecommerce/e2e/
        ├── E2EInfrastructure.java   # 5 個共用容器
        ├── ServiceProcess.java      # bootJar + ProcessBuilder helper
        └── FullChainE2ETest.java    # 真正的 E2E 測試
```

### 每個服務內部的四層（Hexagonal）

```
        ┌─────────────────────────┐
        │  adapter/inbound/       │  ← REST、Kafka Consumer（驅動端）
        │  - 把外部請求翻譯成 Use Case 呼叫
        └──────────┬──────────────┘
                   ▼
        ┌─────────────────────────┐
        │  application/            │  ← Use Case = Application Service
        │  - 編排 Domain、發事件、儲存
        └──────────┬──────────────┘
                   ▼
        ┌─────────────────────────┐
        │  domain/                 │  ← 業務核心：Aggregate、Value Object、Domain Service
        │  - 純 Java，無 Spring/JPA/Kafka
        │  - port/inbound 定義 Use Case 介面
        │  - port/outbound 定義「我需要外面做什麼」
        └──────────┬──────────────┘
                   ▼
        ┌─────────────────────────┐
        │  adapter/outbound/       │  ← JPA、Redis、Kafka Producer…（被驅動端）
        │  - 實作 port/outbound 介面
        └─────────────────────────┘
```

**ArchUnit 守門員**會自動驗證這些依賴方向。Domain 偷偷 import `org.springframework.*`？CI 就紅給你看。

---

## 第 4 章：核心概念入門

### 4.1 為什麼要用 Hexagonal Architecture（六角架構）

#### 痛點：傳統三層架構的麻煩

```
Controller → Service → Repository
                  ↓
                  JPA Entity（同時是 Domain Model）
```

問題：
- 想把 JPA 換成 MongoDB？Service 全要改。
- 想單元測試 Service？得 mock 一堆 Spring。
- Domain 物件被 `@Entity` 綁死，業務邏輯散落各處。

#### 解法：Port + Adapter

把「需要外面做什麼」用**介面**（Port）表達，實作（Adapter）放在另一層。

```java
// 在 domain 層只看到「我要存訂單」這件事
public interface OrderWriteRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
}

// adapter/outbound/persistence 實作真正的 JPA 版本
@Repository
public class JpaOrderWriteRepository implements OrderWriteRepository { ... }

// 測試時可以用一個假的 InMemory 版本
public class InMemoryOrderWriteRepository implements OrderWriteRepository { ... }
```

#### 帶來的好處

| 想做的事                       | 怎麼做                                                  |
|--------------------------------|---------------------------------------------------------|
| 單元測試 Application Service   | new 一個 InMemory adapter 注入即可，不需要 Spring         |
| 把 Kafka 換成 GCP Pub/Sub      | 新增 `PubSubEventPublisher`，刪掉 KafkaEventPublisher    |
| 驗證 InMemory 和真實 adapter 行為一致 | 寫一份 Contract Test，兩個 adapter 各跑一次       |

本專案的 `OrderWriteRepositoryContract` 就是這樣，三條測試同時驗證 In-Memory 和 JPA 實作。

### 4.2 DDD 的 Aggregate / Value Object 入門

**Value Object**（值物件）：用值定義身份，不可變，沒有 ID。
```java
public record Money(BigDecimal amount, Currency currency) {
    public Money add(Money other) { ... }
}
```
兩個 `Money(10, USD)` 必相等；改變只能「複製出一個新的」。

**Aggregate Root**（聚合根）：有生命週期、有業務不變式（invariant），對外是入口。
```java
public final class Order {
    public static Order place(...) { ... }     // 工廠
    public void markPaid(UUID paymentId, Clock clock) {
        if (!(status instanceof OrderStatus.Created)) {
            throw new IllegalStateException("only Created orders can be paid");
        }
        // 狀態轉換 + emit Domain Event
    }
}
```

關鍵：**業務規則寫在 Aggregate 裡，外面只能透過方法操作它**，所以規則不會被繞過。

#### Java 21 的 Sealed Interface 在 DDD 怎麼用

訂單狀態有 4 種：Created、Paid、Completed、Cancelled。用普通 enum 不能帶不同資料；
sealed interface 可以！

```java
public sealed interface OrderStatus
    permits Created, Paid, Completed, Cancelled {

    record Created(Instant at)                          implements OrderStatus {}
    record Paid(Instant at, UUID paymentId)             implements OrderStatus {}
    record Completed(Instant at)                        implements OrderStatus {}
    record Cancelled(Instant at, String reason)         implements OrderStatus {}
}
```

搭配 **pattern matching**：

```java
switch (status) {
    case OrderStatus.Created __    -> entity.setStatus("CREATED");
    case OrderStatus.Paid paid     -> entity.setPaymentId(paid.paymentId());
    case OrderStatus.Completed __  -> entity.setStatus("COMPLETED");
    case OrderStatus.Cancelled c   -> entity.setReason(c.reason());
}
```

編譯器會強迫你 cover 全部 case，新增狀態忘記處理就紅。

### 4.3 CQRS：把讀和寫分開

**C**ommand：改變狀態（下單、扣庫存），走完整業務驗證。
**Q**uery：只讀，可以走快取、Elasticsearch 等優化路徑，不經過 Aggregate。

```
                    POST /api/orders           GET /api/products/search
                          │                              │
                          ▼                              ▼
                ┌────────────────────┐        ┌────────────────────┐
                │ PlaceOrderCommand  │        │ SearchProductQuery │
                │      Handler       │        │      Handler       │
                └─────────┬──────────┘        └─────────┬──────────┘
                          │                              │
                          ▼                              ▼
                ┌────────────────────┐        ┌────────────────────┐
                │  Order Aggregate   │        │   Read-only path   │
                │  (PostgreSQL)      │        │  Cache → Search    │
                └─────────┬──────────┘        │  (Redis → ES)      │
                          │                   └────────────────────┘
                          ▼ (Kafka Domain Event)
                    其他服務 + Projector 更新 Read Model
```

本專案的商品搜尋 `SearchProductQueryHandler` 走 cache-aside pattern：

```java
return cachePort.get(cacheKey, ProductViewList.class)
        .map(ProductViewList::items)
        .orElseGet(() -> {
            List<ProductView> result = searchPort.search(query);
            cachePort.put(cacheKey, new ProductViewList(result), CACHE_TTL);
            return result;
        });
```

### 4.4 Testcontainers 為什麼好用

#### Mock 的問題

```java
when(orderRepository.save(any())).thenReturn(savedOrder);
```

測試是綠的，但你不知道：
- JPA 的 OptimisticLockException 真的會丟嗎？
- Flyway migration 寫對了嗎？
- 你的 JSON serializer 真的能讓 Kafka consumer 反序列化嗎？

#### Testcontainers 的解法

啟動一個**真實的容器**，跑真正的 PostgreSQL / Kafka / Redis…

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:16-alpine"))
        .withDatabaseName("product");

@DynamicPropertySource
static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    // …
}
```

Spring Boot 啟動時用真實 Postgres 跑 Flyway → 你寫的 SQL 真的會被驗證；
寫進去再讀出來 → JPA mapping 真的會被驗證。

代價：每次測試多花 1–3 秒啟動容器。但**抓到的 bug 是 mock 永遠抓不到的**。

---

## 第 5 章：章節走讀

### 5.1 Shared Kernel（共用內核）

`shared-kernel/` 是純 Java module，**沒有 Spring**、**沒有 JPA**。所有服務都依賴它。

包含三類東西：

#### Value Object（domain/）
```java
public record Money(BigDecimal amount, Currency currency) {
    public Money add(Money other) { ... }
    public Money multiply(int n) { ... }
}

public record Quantity(int value) {
    public Quantity add(Quantity other) { ... }
}
```

#### Integration Event（event/）
跨服務通訊的**契約**。用 sealed interface 限制可能的事件種類：

```java
public sealed interface IntegrationEvent
    permits OrderCreatedIntegrationEvent,
            PaymentCompletedIntegrationEvent,
            PaymentFailedIntegrationEvent,
            InventoryDeductedIntegrationEvent,
            InventoryDeductionFailedIntegrationEvent {
    UUID eventId();
    Instant occurredAt();
    String topic();    // Kafka topic 名稱
}
```

#### 共用 Port（port/）
所有服務都會需要的「對外」介面：

```java
public interface EventPublisher {
    void publish(IntegrationEvent event);
}
public interface ObjectStoragePort { ... }
public interface SecretProvider { ... }
```

> **為什麼把 Port 放這裡？**
> 因為三個服務都要發 IntegrationEvent、都可能要存物件、都可能要拿密鑰。
> 介面共用，每個服務各自實作（或共用 KafkaEventPublisher）。

### 5.2 Product Service（商品 / 訂單）

#### Domain：Order Aggregate

```java
public final class Order {

    private final OrderId id;
    private final String buyerId;
    private final List<OrderLine> lines;
    private final Money totalAmount;
    private OrderStatus status;
    private long version;                  // 給 JPA optimistic lock 用
    private final List<OrderEvent> uncommittedEvents = new ArrayList<>();

    public static Order place(String buyerId, List<OrderLine> lines, Clock clock) {
        // 1. 驗證 lines 不為空
        // 2. 計算 totalAmount
        // 3. 建立 Order 並 emit OrderEvent.Created
    }

    public void markPaid(UUID paymentId, Clock clock) { ... }
    public void complete(Clock clock) { ... }
    public void cancel(String reason, Clock clock) { ... }

    public List<OrderEvent> drainEvents() { ... }
}
```

注意：
- `Clock` 透過參數傳，**不要在 Aggregate 內呼叫 `Instant.now()`**。這樣測試才能用 `Clock.fixed(...)` 凍結時間。
- 業務規則（如「只有 Created 狀態能 markPaid」）寫在方法裡，外面不需要記得。
- Domain Event 暫存在 `uncommittedEvents`，由 Application Service `drainEvents()` 取出來發布。

#### Application：PlaceOrderCommandHandler

```java
public class PlaceOrderCommandHandler implements PlaceOrderUseCase {

    private final OrderWriteRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public OrderId place(PlaceOrderCommand command) {
        // 1. 把 command 翻譯成 Domain 物件
        // 2. 呼叫 Order.place(...)
        // 3. 存進 repository
        // 4. 把 Domain Event 翻成 Integration Event 發到 Kafka
    }
}
```

對應的 unit test 用 `InMemoryOrderWriteRepository` + `InMemoryEventPublisher`，**完全不需要 Spring**：

```java
@Test
void persists_order_and_publishes_integration_event() {
    OrderId id = handler.place(new PlaceOrderCommand("buyer-1", "USD", lines));

    assertThat(orderRepo.findById(id)).isPresent();
    assertThat(events.published()).hasSize(1)
        .first().isInstanceOfSatisfying(OrderCreatedIntegrationEvent.class,
            e -> assertThat(e.totalAmount()).isEqualByComparingTo("25.50"));
}
```

#### Adapter：六個方向各一個範例

| Adapter                          | Port                  | 容器              | 用途                  |
|----------------------------------|-----------------------|-------------------|-----------------------|
| `JpaOrderWriteRepository`        | `OrderWriteRepository`| PostgreSQL        | 寫入訂單              |
| `RedisCacheAdapter`              | `CachePort`           | Redis             | 商品搜尋快取          |
| `KafkaEventPublisher`            | `EventPublisher`      | Kafka             | 發 Integration Event  |
| `ElasticsearchSearchAdapter`     | `SearchPort`          | Elasticsearch     | 全文搜尋              |
| `MinioObjectStorageAdapter`      | `ObjectStoragePort`   | MinIO             | 商品圖儲存（S3 相容） |
| `NoopSearchAdapter`              | `SearchPort`          | 無                | E2E 無 ES 時用 fallback |

每個 adapter 都有對應的 `*IT.java` 整合測試，跑真實容器驗證。

#### ArchUnit 三條鐵律

```java
@ArchTest
static final ArchRule LAYERS = Architectures.layeredArchitecture()
        .consideringAllDependencies()
        .layer("Domain").definedBy("..product.domain..")
        .layer("Application").definedBy("..product.application..")
        .layer("AdapterInbound").definedBy("..product.adapter.inbound..")
        .layer("AdapterOutbound").definedBy("..product.adapter.outbound..")
        .whereLayer("Domain").mayOnlyBeAccessedByLayers(
                "Application", "AdapterInbound", "AdapterOutbound", "Config");

@ArchTest
static final ArchRule DOMAIN_NO_SPRING = noClasses()
        .that().resideInAPackage("..product.domain..")
        .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..", "jakarta.persistence..",
                "org.hibernate..", "org.apache.kafka..");
```

CI 跑這些測試 → Domain 偷加 `@Entity` 立刻失敗。

### 5.3 Payment Service（支付）

#### 重點 1：Idempotency Key（防重複支付）

```java
public record IdempotencyKey(String value) {
    public IdempotencyKey {
        if (value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException(...);
        }
    }
}
```

`ProcessPaymentCommandHandler` 進來第一件事：

```java
var existing = paymentRepository.findByIdempotencyKey(key);
if (existing.isPresent()) {
    return existing.get().id();    // 直接回原本的，不重新付款
}
```

資料庫層 `payments.idempotency_key` 加 UNIQUE 約束，雙重保險。

#### 重點 2：Vault 取密鑰（可抽換）

正式環境：`VaultSecretProvider` 透過 `VaultTemplate` 連 Vault server。
開發 / E2E 環境：`PropertyFileSecretProvider` 從 Spring `Environment` 讀。

兩者都實作 `SecretProvider`，**呼叫端不知道差別**。

```java
@Component
@Primary                                    // Vault 在的時候優先用
@ConditionalOnBean(VaultTemplate.class)     // 沒 Vault 不註冊
public class VaultSecretProvider implements SecretProvider { ... }

@Component                                  // 永遠在，當作 fallback
public class PropertyFileSecretProvider implements SecretProvider { ... }
```

`StubPaymentGateway` 注入 `SecretProvider`，無論是哪種實作都能取到 API key。

#### 重點 3：訊息流

```
order.created                                    inventory.deducted
    │                                                    ▲
    ▼                                                    │
OrderCreatedEventConsumer ──→ ProcessPaymentCommandHandler ──→ KafkaEventPublisher
    │                              │                            │
    │                              ├─ Vault → Stripe Auth        ▼
    │                              ├─ MinIO ← 收據 PDF       payment.completed
    │                              └─ Log notification
    ▼
payment.failed (Saga 補償用)
```

整合測試 `VaultSecretProviderIT` 跑真實 Vault container，驗證寫入後讀回的 round-trip。

### 5.4 Inventory Service（庫存）

#### 重點 1：Stock Aggregate 的 Reservation

```java
public final class Stock {
    private Quantity available;
    private final Map<UUID, Quantity> reservations;   // orderId → 預留量
    private long version;                              // optimistic lock

    public void reserve(UUID orderId, Quantity amount) {
        if (reservations.containsKey(orderId)) return;        // idempotent
        if (available.lessThan(amount)) {
            throw new InsufficientStockException(...);
        }
        available = available.subtract(amount);
        reservations.put(orderId, amount);
    }

    public void release(UUID orderId) { ... }   // 補償用
}
```

注意「同一個 orderId 呼叫第二次 reserve 不會扣兩次」— 這就是 idempotency。

#### 重點 2：Redis 分散鎖

```java
@Override
public <T> T withLock(String key, Duration waitFor, Duration holdFor, Supplier<T> action) {
    RLock lock = redisson.getLock(key);
    boolean acquired = false;
    try {
        acquired = lock.tryLock(waitFor.toMillis(), holdFor.toMillis(), MILLISECONDS);
        if (!acquired) throw new LockAcquisitionFailedException(key);
        return action.get();
    } catch (RedisException e) {
        // Redis 斷線時，把基礎設施例外翻成「鎖拿不到」這個業務語意
        throw new LockAcquisitionFailedException(key);
    } finally {
        if (acquired && lock.isHeldByCurrentThread()) lock.unlock();
    }
}
```

`RedisDistributedLockAdapterIT` 跑 8 個 thread × 25 次 increment，驗證最終結果一定是 200，
無論 OS scheduler 怎麼排都不會掉。

### 5.5 E2E：跨服務全鏈路測試

#### 為什麼 E2E 走 ProcessBuilder 而不是單一 Spring Context

教學一開始嘗試在**同一個 JVM** 用 `SpringApplicationBuilder` 起三個 Spring Context，
但遇到一連串麻煩：

1. 三個服務的 `application.yml` 都在 classpath root，只有一個會被 Spring 讀到。
2. Spring Cloud Vault 的 bootstrap context 跨 context 共用，token 設定難以隔離。
3. Spring Security 的 default chain 一旦被 SecurityAutoConfiguration 啟用，會影響所有 context。
4. 不同服務依賴版本（Spring Cloud 2023 對應 Spring Boot 3.3）造成相容性檢查失敗。

**解法**：把每個服務打成 bootJar，用 `ProcessBuilder` 開獨立 JVM 跑。各跑各的 classpath，
完全隔離 — 跟 production 部署一模一樣。

#### `ServiceProcess` helper

```java
public static Builder named(String name) { return new Builder(name); }

// 用法：
ServiceProcess product = ServiceProcess.named("product")
    .jar(Path.of(System.getProperty("e2e.product.jar")))
    .javaHome(System.getProperty("e2e.java.home"))
    .port(freePort())
    .property("spring.datasource.url", pgBase + "product")
    .property("spring.kafka.bootstrap-servers", kafka)
    .start();   // 等到 /actuator/health 回 200 才返回
```

關鍵設計：
- bootJar 路徑經 Gradle systemProperty 傳進 test，避免硬編
- `--server.port=...` 用 0 + `freePort()` 防止 port 衝突
- 等 actuator health 確認 ready 才返回
- 自帶 stdout tail thread，子 JVM 的 log 即時印到測試輸出（方便除錯）

#### `e2e-tests/build.gradle.kts` 巧思

```kotlin
evaluationDependsOn(":product-service")
evaluationDependsOn(":payment-service")
evaluationDependsOn(":inventory-service")

val productBootJar = project(":product-service").tasks.named("bootJar")

tasks.withType<Test>().configureEach {
    dependsOn(productBootJar, paymentBootJar, inventoryBootJar)
    systemProperty("e2e.product.jar", productBootJar.get().outputs.files.singleFile.absolutePath)
    // …
}
```

- `evaluationDependsOn`：確保 Spring Boot plugin 評估完，`bootJar` task 才存在。
- `dependsOn(bootJar)`：跑測試前自動 build bootJar，不用手動 `./gradlew bootJar`。
- `systemProperty`：把 jar 絕對路徑餵給測試類。

#### 完整鏈路

```
                            ┌─────────────────────┐
                            │  FullChainE2ETest    │
                            └──────────┬──────────┘
                                       │ POST /api/orders
                                       ▼
                            ┌─────────────────────┐
                  ┌────────►│  product-service    │── publish order.created ──┐
                  │         │  (child JVM)         │                            │
                  │         └─────────────────────┘                            │
                  │                                                            ▼
                  │                                                  ┌─────────────────┐
                  │                                                  │  Kafka container │
                  │                                                  └─────────┬───────┘
                  │                                                            │
                  │                                                            ▼
                  │                                                  ┌─────────────────┐
                  │                                                  │ payment-service │
                  │                                                  │  (child JVM)     │
                  │                                                  └─────────┬───────┘
                  │                                                            │
                  │                            ┌── Vault 拿 API key ─────────┤
                  │                            ├── MinIO 存收據 ─────────────┤
                  │                            └── publish payment.completed │
                  │                                                            │
                  │  POST /api/inventory/deductions                            │
                  │         (test 模擬 orchestrator)                            │
                  │         ┌─────────────────────┐                            │
                  └────────►│ inventory-service   │◄───────────────────────────┘
                            │  (child JVM)         │── Redis lock + Stock 扣減
                            └─────────────────────┘
```

測試 assertion：

```java
await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
    Map<?, ?> stock = inventoryClient.get()
            .uri("/api/inventory/stock/SKU-COFFEE")
            .retrieve()
            .bodyToMono(Map.class)
            .block(Duration.ofSeconds(5));
    assertThat(((Number) stock.get("available")).intValue()).isEqualTo(98);
});
```

從 100 變 98 → 全鏈路通暢。

---

## 第 6 章：Contract Testing — 把「介面合約」變成自動測試

> 合約測試（Contract Testing）回答的問題是：
> **「我們約定好的介面，雙方真的都遵守嗎？」**

微服務裡有**兩個不同層級**的合約測試，常被混為一談，但解決的痛點完全不同。
本教程兩個都涵蓋。

### 6.1 兩種層級對照

| 層級                    | 合約位置          | 雙方                                | 解決的問題                                  | 本專案做法                          |
|-------------------------|------------------|-------------------------------------|---------------------------------------------|-------------------------------------|
| **Port 行為合約**       | service 內部     | Domain Port ↔ 多個 Adapter         | InMemory fake 和真實 adapter 行為會分歧     | 抽象 Contract class + 多個 subclass |
| **Consumer-Driven 合約**| service 之間     | 上游 (provider) ↔ 下游 (consumer)  | 上游改 JSON schema 沒通知下游就上 prod      | Pact / Spring Cloud Contract（建議）|

### 6.2 Port 行為合約（intra-service）

#### 為什麼需要

回顧第 4.1 章的設計：

```java
public interface OrderWriteRepository { ... }

public class InMemoryOrderWriteRepository implements OrderWriteRepository { ... }   // unit test 用
public class JpaOrderWriteRepository      implements OrderWriteRepository { ... }   // 正式環境
```

問題：如果**這兩個實作行為不一致**會怎樣？

實例：
- InMemory 的 `findById` 永遠回最新狀態；
- JPA 因為 `@Transactional` 邊界寫對了沒，可能拿到 stale data。

於是 unit test（用 InMemory）綠了，prod 卻爛。**Liskov 替換原則被悄悄破壞**。

#### 解法：抽象 Contract class

寫一份**抽象測試類別**，定義所有實作都必須通過的行為：

```java
// payment-service/src/test/.../contract/PaymentRepositoryContract.java
public abstract class PaymentRepositoryContract {

    protected abstract PaymentRepository repository();      // 子類別提供實作

    @Test
    void find_by_idempotency_key_returns_existing_payment() {
        Payment p = Payment.initiate(...);
        repository().save(p);

        assertThat(repository().findByIdempotencyKey(p.idempotencyKey()))
                .isPresent()
                .get().extracting(Payment::id).isEqualTo(p.id());
    }

    @Test
    void status_transition_authorised_then_completed_is_persisted() { ... }
    // … 5 條測試
}
```

每個實作各寫一個極簡 subclass：

```java
// 用 InMemory fake，毫秒級
class InMemoryPaymentRepositoryContractTest extends PaymentRepositoryContract {
    private final InMemoryPaymentRepository repo = new InMemoryPaymentRepository();
    @Override protected PaymentRepository repository() { return repo; }
}

// 用真實 JPA + Testcontainers PostgreSQL，秒級
@DataJpaTest @Testcontainers @Tag("integration")
class JpaPaymentRepositoryContractTest extends PaymentRepositoryContract {

    @Container static PostgreSQLContainer<?> postgres = ...;

    @Autowired JpaPaymentRepository jpa;
    @Override protected PaymentRepository repository() { return jpa; }
}
```

JUnit 5 會在 subclass 自動跑父類的所有 `@Test`，**同一份測試跑兩遍**。

#### 本專案的 Port 合約

| Contract                       | InMemory subclass                                | Real subclass                                  |
|--------------------------------|--------------------------------------------------|------------------------------------------------|
| `OrderWriteRepositoryContract` | `InMemoryOrderWriteRepositoryContractTest`       | `JpaOrderWriteRepositoryContractTest`          |
| `PaymentRepositoryContract`    | `InMemoryPaymentRepositoryContractTest`          | `JpaPaymentRepositoryContractTest`             |
| `StockRepositoryContract`      | `InMemoryStockRepositoryContractTest`            | `JpaStockRepositoryContractTest`               |

共 **6 個 subclass**，每個跑 3~5 條測試，**InMemory 跑 ~3 秒，JPA 整套 ~30 秒**。

#### 取捨小提醒

- **Contract 不該驗證「實作細節」**，只驗證對呼叫者可觀察的行為。例：「JPA 用了哪一個 SQL」**不該**寫進 contract。
- **每個實作可以多寫自己專屬的測試**。例：JPA 額外驗 optimistic lock；InMemory 額外驗 thread-safety。Contract 只是底線。
- **新增 adapter 的標準流程**：
  1. 寫一個新的 subclass 繼承既有 Contract
  2. 通過 → 直接上線
  3. 不通過 → 修 adapter（或如果是 Contract 不合理，調整 Contract，**所有人重跑**）

### 6.3 Consumer-Driven Contract（inter-service）

#### 為什麼 Port 合約不夠

Port 合約解決「**我自己的 InMemory fake 騙我**」。
但跨服務的問題是：「**上游改了 JSON schema，下游不知道**」。

例：product-service 把 `OrderCreatedIntegrationEvent` 的 `totalAmount` 從 number 改成字串，
payment-service 反序列化時直接炸 — 但兩邊的 unit test 都還是綠的。

#### 兩種主流工具

| 工具                          | 路線                                  | 適合場景                              |
|-------------------------------|---------------------------------------|---------------------------------------|
| **Pact**                      | Consumer 端寫期望 → 生 pact JSON → Provider 端 verify | 跨團隊、跨語言、廣為使用            |
| **Spring Cloud Contract**     | Provider 端寫 contract → 生 stub jar → Consumer 端用 | 純 Spring 團隊、與 Spring 整合最緊密|

Pact 還支援 **Pact Broker** — 集中存放 contract，CI 自動 publish + verify。

#### 本專案目前的近似做法（shared-kernel 當「結構合約」）

我們把所有 Integration Event 定義在 `shared-kernel/event/`：

```java
public record OrderCreatedIntegrationEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String buyerId,
        BigDecimal totalAmount,
        String currency,
        List<Line> lines
) implements IntegrationEvent { ... }
```

product-service 發布、payment-service 消費的**都是同一個 record class**。
編譯期就保證**結構一致**。

這只解決了結構問題，**沒解決語義問題**（例：欄位值的合理範圍、null 與否、列舉值）。
如果要嚴格，下面這條路推薦：

#### 升級方向（本教程未實作，留作練習）

```
                     ┌─────────────────────────────┐
                     │ payment-service (consumer)   │
                     │  pact test：                  │
                     │   "我期望 order.created       │
                     │    totalAmount > 0、          │
                     │    currency 是 3-char ISO"    │
                     └────────────┬────────────────┘
                                  │ generate
                                  ▼
                         ┌─────────────────┐
                         │ order.created    │
                         │ -consumer.json   │
                         │ (pact 檔)         │
                         └────────┬────────┘
                                  │ upload
                                  ▼
                         ┌─────────────────┐
                         │ Pact Broker      │
                         └────────┬────────┘
                                  │ verify
                                  ▼
                     ┌─────────────────────────────┐
                     │ product-service (provider)   │
                     │  pact verify：                │
                     │   啟動 service、發 event、    │
                     │   跑 consumer 的期望          │
                     └─────────────────────────────┘
```

實作參考：[pact-jvm-consumer-junit5](https://docs.pact.io/implementation_guides/jvm/consumer/junit5)
+ Pact 的 Message 模式（不是 HTTP，是訊息匯流排）。

#### Schema Registry 的替代方案

如果不想引入 Pact，業界另一個常見做法：**Schema Registry**（Confluent Schema Registry / Apicurio）。
事件用 Avro / Protobuf 序列化，schema 集中存。上游升級 schema 強制向後相容（broker reject），
**生產時就擋下**而不是等 runtime 才炸。本教程用 JSON，跳過了這個。

### 6.4 Contract Testing 的「投入產出」決策

不是每個 port 都值得 contract test。判斷標準：

| 情境                                                       | 該做嗎  |
|-----------------------------------------------------------|---------|
| 有 InMemory fake **且**有真實 adapter，application 都依賴 port | ✅ 該   |
| 只有一個 adapter（永遠不會換），且 application 直接用真實的 | ⛔ 不必 |
| Port 只在 single-test 使用（沒有共用價值）                 | ⛔ 不必 |
| 跨服務 schema，雙方對欄位含義有 prior agreement              | ✅ Pact |
| 跨服務 schema，可直接用 shared-kernel 的 class              | △ 可選 |

---

## 第 7 章：怎麼跑測試

### 7.1 測試金字塔

```
                       ╱╲
                      ╱  ╲          E2E (1 個)
                     ╱ 1  ╲         5 容器 + 3 child JVM，~46 秒
                    ╱──────╲
                   ╱        ╲
                  ╱  ~16    ╲       Integration（per adapter）
                 ╱  IT       ╲      每個 ~5 秒（容器開機）
                ╱──────────────╲
               ╱                ╲
              ╱      ~58        ╲   Unit + ArchUnit
             ╱  Unit / ArchUnit ╲   <1 秒
            ╱────────────────────╲
```

### 7.2 常用指令

```bash
# 只跑單元測試（不需 Docker）
./gradlew :shared-kernel:test
./gradlew :product-service:test --tests '*Test'   # 排除 *IT

# 只跑某個服務的整合測試
./gradlew :product-service:test --tests '*IT'

# 跑全部（含 E2E）
./gradlew test

# 看測試報告
open product-service/build/reports/tests/test/index.html
```

### 7.3 Tag 過濾

每個 IT/E2E 測試都標了 JUnit Tag：

```java
@Tag("integration")  // 單一 adapter 整合測試
@Tag("e2e")          // 跨服務全鏈路
```

CI 三階段（見 `.github/workflows/ci.yml`）就是用 tag 分群：

```
unit → integration (matrix per service) → e2e
```

### 7.4 第一次跑會慢

Testcontainers 第一次會 pull image：

| Image                                       | 大小   |
|---------------------------------------------|--------|
| `postgres:16-alpine`                        | ~80 MB |
| `redis:7-alpine`                            | ~40 MB |
| `confluentinc/cp-kafka:7.6.1`               | ~600 MB|
| `docker.elastic.co/.../elasticsearch:8.13.4`| ~700 MB|
| `minio/minio:RELEASE.2024-08-17...`         | ~150 MB|
| `hashicorp/vault:1.16`                      | ~250 MB|

之後跑會走快取，秒級啟動。

---

## 第 8 章：故障排除 FAQ

### Q1：跑 IT 卡在 "Could not find a valid Docker environment"

Testcontainers 找不到 Docker socket。檢查：

```bash
ls -la /var/run/docker.sock        # 應該存在或指向 podman.sock
export DOCKER_HOST=unix:///var/run/docker.sock
```

Podman 用戶補上 `TESTCONTAINERS_RYUK_DISABLED=true`。

### Q2：Elasticsearch 容器 boot 失敗，log 寫 "Native memory allocation (mmap) failed"

VM 記憶體不夠，ES 預設想要 2 GB heap。
本專案的 `ElasticsearchContainer` 設定已限縮：

```java
.withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx512m")
```

但你的 Podman VM 也要夠：

```bash
podman machine stop
podman machine set --memory 6144
podman machine start
```

### Q3：E2E 跑到 happy_path 拿到 401 Unauthorized

代表 product-service 的 Spring Security 攔了你的 request。檢查：
- E2E 是否設了 `product.security.jwt.enabled=false`？
- 是否走的是 ProcessBuilder 版本（已修好）而不是舊的 in-JVM 版本？

### Q4：JPA Schema validation 失敗 "found bpchar but expecting varchar"

PostgreSQL 的 `CHAR(N)` 在 JPA 預設 `length = N` 對應 VARCHAR。
Flyway migration 改成 `VARCHAR(N)` 即可。

### Q5：Kafka 容器啟動很慢

Confluent Kafka image 真的很大（~600 MB）。第一次拉需要時間，之後就快。
要加速可以考慮：
- 換 `confluentinc/cp-kafka:7.6.1` 為更輕量的 `apache/kafka:3.7.0`
- 或用 Redpanda（Kafka API 相容但啟動快很多）

### Q6：所有 IT 都過，E2E 卻 timeout

可能 child JVM 啟動慢（資源吃緊）。檢查：

```bash
podman stats   # 看容器 CPU/MEM
top            # 看主機端
```

E2E 的 `ServiceProcess` 等 health 最多 2 分鐘，超過就放棄。可調整 `waitForHealth`。

### Q7：Podman 跑 Ryuk 報錯

Ryuk 需要 privileged 才能管理其他容器。Podman rootless 不支援。
本專案 `PodmanCompatibility.apply()` 已自動關 Ryuk：

```java
System.setProperty("testcontainers.ryuk.disabled", "true");
```

代價：JVM 異常 kill 時容器不會被清。手動清：

```bash
podman ps -a | grep testcontainers
podman rm -f <id>
```

### Q8：在 IDE 跑單一 test 過、`./gradlew` 不過

通常是 IDE 把 `application.yml` 多載到 classpath 兩次。檢查：
- IDE Run Configuration 沒勾「Make project」這類額外動作
- `build/resources/test/` 沒有過時的 application.yml

### Q9：想驗證 ArchUnit 規則確實在守

故意去 Domain 加一個 `@Service`：

```java
// product-service/.../domain/model/Order.java
import org.springframework.stereotype.Service;
@Service              // ← 這行
public final class Order { ... }
```

跑 `./gradlew :product-service:test --tests '*HexagonalArchitectureTest'`，
應該看到 `DOMAIN_NO_SPRING` 紅炸。

---

## 延伸閱讀

### 書
- **Implementing Domain-Driven Design** — Vaughn Vernon。DDD 入門首選。
- **Patterns of Enterprise Application Architecture** — Martin Fowler。CQRS / Repository 原始定義。
- **Building Microservices** — Sam Newman。微服務拆分原則。

### 線上資源
- Hexagonal Architecture 原始論文：<https://alistair.cockburn.us/hexagonal-architecture/>
- Testcontainers 官方教學：<https://testcontainers.com/>
- ArchUnit User Guide：<https://www.archunit.org/userguide/html/000_Index.html>

### 進階主題（本教程未涵蓋，建議自學）
- **Outbox Pattern**：避免「DB commit 了但 Kafka 沒送出」的不一致。
- **Saga + Compensation**：本教程的 Inventory 已預留 release 介面，可以補完整個 saga。
- **Pact Contract Testing**：跨服務的 consumer-driven contract，跟 ArchUnit 互補。
- **OpenTelemetry**：trace 跨服務的 Kafka 訊息，看請求路徑。

---

## 後記

這份教程的所有程式碼都附帶可執行測試（75 個，跑得起來），不是貼上來給你看而已。

寫的時候有意識讓每個概念都有「對應的測試」：
- 不要相信「Money 不可變」？看 `MoneyTest`。
- 不要相信「Hexagonal 真的隔開了基礎設施」？看 `HexagonalArchitectureTest`。
- 不要相信「分散鎖真的有效」？看 `RedisDistributedLockAdapterIT` 的 200 次併發 increment。
- 不要相信「整個鏈路真的通」？看 `FullChainE2ETest`。

**測試就是會說真話的文件**。
