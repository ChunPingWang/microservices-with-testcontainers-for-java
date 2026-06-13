# Testcontainers 電商微服務整合測試教程（架構強化版）

## 教程目標

以 Java 21 + Spring Boot 4 為基礎，採用 DDD + CQRS + Hexagonal Architecture，
建構三個電商微服務（商品、支付、庫存），透過 Testcontainers 驗證完整訂購流程。
重點展示「可抽換外部介接」的架構設計，讓基礎設施元件可以在測試與正式環境間無縫切換。

---

## 一、技術棧

| 類別             | 技術選型                                                     |
|------------------|--------------------------------------------------------------|
| Language         | Java 21（Record、Sealed Interface、Pattern Matching、Virtual Threads）|
| Framework        | Spring Boot 4.x, Spring Framework 7                         |
| Build            | Gradle 8.x (Kotlin DSL)                                     |
| Test             | JUnit 5, Testcontainers 1.20+, Awaitility, ArchUnit         |
| DB Migration     | Flyway 10                                                    |
| Messaging        | Spring Kafka, Spring Cloud GCP Pub/Sub                       |
| Cache            | Spring Data Redis, Redisson                                  |
| Search           | Spring Data Elasticsearch (或 REST client)                   |
| Auth             | Spring Security OAuth2 Resource Server                       |
| Storage          | AWS SDK v2 (S3 相容 → MinIO)                                |
| Secrets          | Spring Cloud Vault                                           |
| BDD (選配)       | Cucumber 7.x                                                 |
| Architecture Test| ArchUnit (強制驗證分層依賴方向)                                |

### Spring Boot 4 關注事項

Spring Boot 4 基於 Spring Framework 7，以 Jakarta EE 11 為底，最低要求 Java 17，
建議直接使用 Java 21 以啟用 Virtual Threads 與完整語言特性。
教程中需留意以下變動：
- `jakarta.*` namespace 已全面取代 `javax.*`
- 部分 deprecated API 移除（SecurityFilterChain 寫法調整等）
- Virtual Threads 可透過 `spring.threads.virtual.enabled=true` 全域啟用
- Observation API (Micrometer) 成為一等公民

---

## 二、架構設計原則

### 2.1 Hexagonal Architecture（六角架構 / Ports & Adapters）

這是實現「快速切換外部介接」的核心。每個微服務內部分為三層：

```
                    ┌─────────────────────────────────┐
                    │         Driving Adapters         │
                    │   (REST Controller, Kafka        │
                    │    Consumer, Scheduler)          │
                    └────────────┬────────────────────┘
                                 │ calls
                    ┌────────────▼────────────────────┐
                    │      Inbound Ports (Interface)   │
                    │   e.g. PlaceOrderUseCase         │
                    │        QueryProductUseCase       │
                    ├─────────────────────────────────┤
                    │       Domain Model (Core)        │
                    │   Entity, Value Object, Domain   │
                    │   Event, Domain Service,         │
                    │   Aggregate Root                 │
                    ├─────────────────────────────────┤
                    │      Outbound Ports (Interface)  │
                    │   e.g. OrderRepository           │
                    │        PaymentGateway            │
                    │        EventPublisher            │
                    └────────────┬────────────────────┘
                                 │ implemented by
                    ┌────────────▼────────────────────┐
                    │        Driven Adapters           │
                    │   (JPA Repo, Kafka Producer,     │
                    │    Redis Client, MinIO Client,   │
                    │    Vault Client, ES Client)      │
                    └─────────────────────────────────┘
```

**切換外部介接的機制：**

```
Outbound Port (Interface)          Driven Adapter (實作)
─────────────────────────          ─────────────────────────
OrderRepository                 →  JpaOrderRepository (PostgreSQL)
                                →  InMemoryOrderRepository (單元測試)

EventPublisher                  →  KafkaEventPublisher (正式)
                                →  InMemoryEventPublisher (單元測試)
                                →  PubSubEventPublisher (外部通知)

ObjectStoragePort               →  MinioObjectStorageAdapter (正式)
                                →  LocalFileObjectStorageAdapter (開發)
                                →  InMemoryObjectStorageAdapter (單元測試)

SecretProvider                  →  VaultSecretProvider (正式)
                                →  PropertyFileSecretProvider (開發/測試)

SearchPort                      →  ElasticsearchSearchAdapter (正式)
                                →  JdbcSearchAdapter (降級方案)

CachePort                       →  RedisCacheAdapter (正式)
                                →  CaffeineLocalCacheAdapter (單元測試)

AuthPort                        →  KeycloakAuthAdapter (正式)
                                →  StubAuthAdapter (單元測試)
```

透過 Spring `@Profile` 或 `@ConditionalOnProperty` 切換 Bean 注入，
Testcontainers 測試使用真實 Adapter + 真實容器，單元測試使用 InMemory Adapter。

---

### 2.2 SOLID 原則在各層的體現

| 原則 | 體現位置 | 具體做法 |
|------|----------|----------|
| **S** - Single Responsibility | Use Case 類別 | 每個 Use Case 只做一件事：`PlaceOrderUseCase`、`ProcessPaymentUseCase`、`DeductInventoryUseCase` |
| **O** - Open/Closed | Outbound Port | 新增 Adapter（如從 MinIO 換到 Azure Blob）不修改 Domain，只新增 Adapter 實作 |
| **L** - Liskov Substitution | Port / Adapter | `InMemoryOrderRepository` 與 `JpaOrderRepository` 行為完全一致，可互換 |
| **I** - Interface Segregation | Port 拆分 | `OrderRepository` 拆為 `OrderReadRepository`（CQRS Query 端）與 `OrderWriteRepository`（Command 端） |
| **D** - Dependency Inversion | 分層方向 | Domain 層不依賴任何外部框架；Adapter 依賴 Domain，反之不成立 |

---

### 2.3 DDD Domain Model

#### Bounded Context 劃分

```
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  Product Context │  │  Payment Context │  │ Inventory Context│
│                  │  │                  │  │                  │
│  Aggregate:      │  │  Aggregate:      │  │  Aggregate:      │
│    Order         │  │    Payment       │  │    Stock         │
│    Product       │  │                  │  │                  │
│                  │  │  Value Object:   │  │  Value Object:   │
│  Value Object:   │  │    Money         │  │    StockLevel    │
│    ProductId     │  │    PaymentMethod │  │    Reservation   │
│    OrderId       │  │    PaymentId     │  │    SkuId         │
│    OrderLine     │  │    IdempotencyKey│  │                  │
│    Price (Money) │  │                  │  │  Domain Event:   │
│                  │  │  Domain Event:   │  │    InventoryDe-  │
│  Domain Event:   │  │    PaymentCom-   │  │    ductedEvent   │
│    OrderCreated- │  │    pletedEvent   │  │    StockDepleted-│
│    Event         │  │    PaymentFailed-│  │    Event         │
│                  │  │    Event         │  │                  │
│  Domain Service: │  │                  │  │  Domain Service: │
│    PricingService│  │  Domain Service: │  │    StockAlloca-  │
│                  │  │    PaymentVali-  │  │    tionService   │
│                  │  │    dationService │  │                  │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

#### Domain Model 設計要點

**Aggregate Root 封裝業務規則：**

```
Order (Aggregate Root)
├── OrderId        : Value Object (UUID-based record)
├── OrderStatus    : Sealed Interface → CREATED | PAID | COMPLETED | REFUNDED
├── OrderLines     : List<OrderLine>  (Value Object)
├── TotalAmount    : Money            (Value Object)
├── placeOrder()   → 驗證 → 狀態變更 → 產生 OrderCreatedEvent
├── markPaid()     → 狀態變更 → 產生 OrderPaidEvent
└── cancel()       → 補償邏輯 → 產生 OrderCancelledEvent
```

**Java 21 語言特性應用：**

```
// Value Object 用 Record
public record OrderId(UUID value) {}
public record Money(BigDecimal amount, Currency currency) {}

// 狀態用 Sealed Interface + Pattern Matching
public sealed interface OrderStatus
    permits Created, Paid, Completed, Refunded {

    record Created(Instant at) implements OrderStatus {}
    record Paid(Instant at, PaymentId paymentId) implements OrderStatus {}
    record Completed(Instant at) implements OrderStatus {}
    record Refunded(Instant at, String reason) implements OrderStatus {}
}

// Domain Event 用 Sealed Interface
public sealed interface OrderEvent
    permits OrderCreatedEvent, OrderPaidEvent, OrderCancelledEvent {}
```

---

### 2.4 CQRS 架構

```
                    ┌─────────────────────────────────────────┐
                    │              API Gateway                 │
                    │           (Keycloak JWT 驗證)             │
                    └─────┬─────────────────────┬─────────────┘
                          │                     │
                   Command (寫)             Query (讀)
                          │                     │
               ┌──────────▼──────────┐  ┌──────▼───────────┐
               │   Command Handler   │  │   Query Handler   │
               │                     │  │                    │
               │ PlaceOrderCommand   │  │ GetOrderQuery      │
               │ ProcessPaymentCmd   │  │ SearchProductQuery │
               │ DeductInventoryCmd  │  │ GetInventoryQuery  │
               └──────────┬──────────┘  └──────┬───────────┘
                          │                     │
               ┌──────────▼──────────┐  ┌──────▼───────────┐
               │   Write Model       │  │   Read Model      │
               │   (PostgreSQL)      │  │   (ES / Redis)    │
               │                     │  │                    │
               │  Domain Aggregate   │  │  Denormalized View │
               │  完整業務規則驗證    │  │  查詢最佳化         │
               └──────────┬──────────┘  └──────▲───────────┘
                          │                     │
                          │   Domain Event      │
                          └─────────────────────┘
                              (Kafka 投遞 →
                               Projector 更新 Read Model)
```

**CQRS 在各服務的具體分工：**

| 服務   | Write Model (PostgreSQL)       | Read Model                      | 同步機制              |
|--------|--------------------------------|---------------------------------|-----------------------|
| 商品   | Order Aggregate, Product       | Elasticsearch (商品搜尋)         | Kafka → ES Projector  |
|        |                                | Redis (商品快取)                 | Kafka → Cache Updater |
| 支付   | Payment Aggregate              | PostgreSQL View (支付查詢)       | 同 DB，用 View 隔離    |
| 庫存   | Stock Aggregate                | Redis (即時庫存查詢)             | Kafka → Redis Updater |

---

## 三、Module 結構

```
ecommerce-testcontainers/
├── build.gradle.kts                     # 根專案：BOM 版本管理
│
├── shared-kernel/                       # 跨 Context 共用
│   ├── domain/                          # 共用 Value Object：Money, Quantity
│   ├── event/                           # Integration Event 定義 (跨服務通訊用)
│   └── port/                            # 共用 Port 介面：EventPublisher, ObjectStoragePort
│
├── product-service/
│   ├── domain/                          # Aggregate: Order, Product
│   │   ├── model/                       # Entity, VO, Sealed Interface
│   │   ├── event/                       # Domain Event (內部)
│   │   ├── port/
│   │   │   ├── inbound/                 # PlaceOrderUseCase, QueryProductUseCase
│   │   │   └── outbound/               # OrderRepository, SearchPort, CachePort
│   │   └── service/                     # Domain Service: PricingService
│   │
│   ├── application/                     # Use Case 實作 (Application Service)
│   │   ├── command/                     # PlaceOrderCommandHandler
│   │   └── query/                       # SearchProductQueryHandler
│   │
│   ├── adapter/
│   │   ├── inbound/
│   │   │   ├── rest/                    # ProductController, OrderController
│   │   │   └── kafka/                   # InventoryDeductedEventConsumer
│   │   └── outbound/
│   │       ├── persistence/             # JpaOrderRepository, JPA Entity 映射
│   │       ├── search/                  # ElasticsearchSearchAdapter
│   │       ├── cache/                   # RedisCacheAdapter
│   │       ├── storage/                 # MinioObjectStorageAdapter
│   │       ├── messaging/               # KafkaEventPublisher
│   │       └── auth/                    # KeycloakAuthAdapter
│   │
│   └── config/                          # Spring Configuration, Profile 切換
│
├── payment-service/
│   ├── domain/
│   │   ├── model/                       # Payment Aggregate, Money, IdempotencyKey
│   │   ├── event/
│   │   ├── port/
│   │   │   ├── inbound/                 # ProcessPaymentUseCase
│   │   │   └── outbound/               # PaymentRepository, SecretProvider
│   │   └── service/                     # PaymentValidationService
│   ├── application/
│   │   ├── command/                     # ProcessPaymentCommandHandler
│   │   └── query/                       # GetPaymentStatusQueryHandler
│   ├── adapter/
│   │   ├── inbound/
│   │   │   ├── rest/                    # PaymentController
│   │   │   └── kafka/                   # OrderCreatedEventConsumer
│   │   └── outbound/
│   │       ├── persistence/             # JpaPaymentRepository
│   │       ├── messaging/               # KafkaEventPublisher
│   │       ├── notification/            # PubSubNotificationAdapter
│   │       ├── storage/                 # MinioReceiptStorageAdapter
│   │       └── secret/                  # VaultSecretProvider
│   └── config/
│
├── inventory-service/
│   ├── domain/
│   │   ├── model/                       # Stock Aggregate, StockLevel, Reservation
│   │   ├── event/
│   │   ├── port/
│   │   │   ├── inbound/                 # DeductInventoryUseCase
│   │   │   └── outbound/               # StockRepository, DistributedLockPort
│   │   └── service/                     # StockAllocationService
│   ├── application/
│   │   ├── command/                     # DeductInventoryCommandHandler
│   │   └── query/                       # GetStockLevelQueryHandler
│   ├── adapter/
│   │   ├── inbound/
│   │   │   ├── rest/                    # InventoryController
│   │   │   └── kafka/                   # PaymentCompletedEventConsumer
│   │   └── outbound/
│   │       ├── persistence/             # JpaStockRepository
│   │       ├── messaging/               # KafkaEventPublisher
│   │       ├── lock/                    # RedisDistributedLockAdapter
│   │       └── secret/                  # VaultSecretProvider
│   └── config/
│
└── test-infrastructure/                 # 測試共用基礎設施
    ├── containers/                      # Singleton Container 定義
    ├── fixtures/                        # 測試資料工廠 (Domain-aware)
    ├── fakes/                           # InMemory Adapter 實作 (用於單元測試)
    └── bdd/                             # Cucumber Step Definitions
```

---

## 四、測試分層策略

### 4.1 測試金字塔 × Hexagonal Architecture

```
                        ╱╲
                       ╱  ╲          E2E Test
                      ╱ 少 ╲         全容器 + 跨服務 Kafka 事件流
                     ╱──────╲        Testcontainers × 8
                    ╱        ╲
                   ╱   中等    ╲      Integration Test (per Adapter)
                  ╱            ╲     每個 Adapter 對應一個容器
                 ╱──────────────╲    Testcontainers × 1~2
                ╱                ╲
               ╱       大量       ╲   Unit Test (Domain + Application)
              ╱                    ╲  InMemory Adapter / 無容器
             ╱──────────────────────╲ 純 JUnit 5
```

### 4.2 各層測試對照

| 測試層級 | 測試對象 | 外部依賴 | 使用容器 | 速度 |
|----------|----------|----------|----------|------|
| **Unit — Domain** | Aggregate 業務規則、Domain Service | 無 | 無 | <1ms/test |
| **Unit — Application** | Use Case / Command Handler | InMemory Adapter (Fake) | 無 | <5ms/test |
| **Integration — Adapter** | 單一 Adapter vs 真實基礎設施 | 真實容器 | 1~2 個 | ~1s/test |
| **Integration — Slice** | Controller → UseCase → Adapter | `@SpringBootTest` + 部分容器 | 2~3 個 | ~3s/test |
| **E2E — 全鏈路** | 下單 → 支付 → 扣庫存 | 全部容器 | 8 個 | ~10s/test |

### 4.3 快速切換的測試驗證

**ArchUnit 強制分層規則：**

```
domain 層不得 import adapter 層任何類別
domain 層不得 import spring framework 任何類別
adapter.outbound 層必須實作 domain.port.outbound 介面
application 層只能依賴 domain 層
```

**切換驗證測試：**

| 測試目的 | 做法 |
|----------|------|
| 驗證 InMemory 與 Real Adapter 行為一致 | Contract Test：對 Port interface 寫一套測試，兩個 Adapter 各跑一次 |
| 驗證新增 Adapter 不影響 Domain | 替換 Profile 後全部 Domain Unit Test 仍通過 |
| 驗證切換 Kafka → Pub/Sub | 將 `EventPublisher` 實作從 Kafka 換成 Pub/Sub，E2E 仍通過 |

---

## 五、章節規劃（修訂版）

### 第一章：專案骨架與架構基礎 (Day 1-2)

**1.1 Gradle Multi-Module 搭建**
- Java 21 toolchain 設定
- Spring Boot 4 BOM + Testcontainers BOM
- Module 間依賴方向設定（domain → 無外部依賴、adapter → domain）

**1.2 Shared Kernel**
- 共用 Value Object：`Money`、`Quantity`（Java Record）
- Integration Event 介面定義（跨服務通訊契約）
- 共用 Outbound Port：`EventPublisher`、`ObjectStoragePort`

**1.3 ArchUnit 守門員**
- 設定分層依賴規則
- 禁止 Domain 層引入 Spring / JPA annotation
- CI 中自動執行，違反即失敗

---

### 第二章：商品服務 — Domain & CQRS (Day 3-4)

**2.1 Domain Model 建構**
- `Product` Entity、`Order` Aggregate Root
- Sealed Interface：`OrderStatus`、`OrderEvent`
- Domain Service：`PricingService`（計算折扣、稅）
- 純 Unit Test：驗證下單規則、狀態流轉

**2.2 CQRS — Command Side**
- `PlaceOrderUseCase` (Inbound Port)
- `PlaceOrderCommandHandler` (Application Service)
- Outbound Port：`OrderWriteRepository`
- Unit Test with `InMemoryOrderRepository`

**2.3 CQRS — Query Side**
- `SearchProductUseCase` (Inbound Port)
- `SearchProductQueryHandler`
- Outbound Port：`SearchPort`、`CachePort`
- Unit Test with `InMemorySearchAdapter`、`InMemoryCacheAdapter`

**2.4 Adapter — PostgreSQL**
- `JpaOrderRepository` 實作 `OrderWriteRepository`
- JPA Entity ↔ Domain Model 雙向映射（Anti-Corruption Layer）
- Testcontainers：`PostgreSQLContainer` + Flyway
- Contract Test：`OrderWriteRepositoryContract` 跑 InMemory + JPA 兩份

**2.5 Adapter — Elasticsearch**
- `ElasticsearchSearchAdapter` 實作 `SearchPort`
- Testcontainers：`ElasticsearchContainer`
- 測試索引建立、全文檢索、facet

**2.6 Adapter — Redis**
- `RedisCacheAdapter` 實作 `CachePort`
- Testcontainers：`GenericContainer("redis:7")`
- 測試 Cache Aside Pattern、TTL

**2.7 Adapter — MinIO**
- `MinioObjectStorageAdapter` 實作 `ObjectStoragePort`
- Testcontainers：`GenericContainer("minio/minio")`
- 測試商品圖片上傳與 Presigned URL

**2.8 Adapter — Keycloak**
- `KeycloakAuthAdapter`
- Testcontainers：`KeycloakContainer`
- 測試 JWT 驗證、角色授權 (BUYER / ADMIN)

---

### 第三章：支付服務 — Event-Driven & Secrets (Day 5-6)

**3.1 Domain Model**
- `Payment` Aggregate Root
- `IdempotencyKey` Value Object（防重複支付）
- Sealed Interface：`PaymentStatus`、`PaymentEvent`
- Unit Test：冪等驗證、金額驗證

**3.2 CQRS Command — 處理支付**
- `ProcessPaymentUseCase`
- Outbound Port：`PaymentWriteRepository`、`SecretProvider`、`ReceiptStoragePort`

**3.3 Adapter — Kafka (Consumer + Producer)**
- Inbound：`OrderCreatedEventConsumer`（Driving Adapter）
- Outbound：`KafkaEventPublisher` 實作 `EventPublisher`
- Testcontainers：`KafkaContainer` (KRaft mode)
- 測試：收到 OrderCreatedEvent → 執行支付 → 發送 PaymentCompletedEvent
- 測試：支付失敗 → PaymentFailedEvent → 補償流程

**3.4 Adapter — Vault**
- `VaultSecretProvider` 實作 `SecretProvider`
- Testcontainers：`VaultContainer`
- 測試：啟動時自動注入第三方支付 API Key
- 切換展示：替換為 `PropertyFileSecretProvider` 不影響 Domain

**3.5 Adapter — GCP Pub/Sub**
- `PubSubNotificationAdapter` 實作 `NotificationPort`
- Testcontainers：`PubSubEmulatorContainer`
- 測試：支付完成 → 發送客戶通知
- 切換展示：替換為 `LogNotificationAdapter`（開發用）

**3.6 Adapter — MinIO (收據歸檔)**
- 複用 `ObjectStoragePort`，展示同一 Port 在不同 Context 的複用

---

### 第四章：庫存服務 — 併發控制 & 動態密鑰 (Day 7)

**4.1 Domain Model**
- `Stock` Aggregate Root（含 version 樂觀鎖）
- `StockLevel` Value Object、`Reservation` Value Object
- Domain Service：`StockAllocationService`
- Unit Test：庫存不足拋 Domain Exception

**4.2 Adapter — Redis 分散式鎖**
- `RedisDistributedLockAdapter` 實作 `DistributedLockPort`
- Testcontainers：Redis Container
- 測試：併發扣庫存 → 鎖保護 → 最終庫存正確
- 切換展示：替換為 `JdbcPessimisticLockAdapter`（用 DB row lock 替代）

**4.3 Adapter — Vault 動態密鑰**
- Vault Database Secret Engine → 每次連線取得臨時 DB 帳密
- 測試：lease renewal、credential rotation

**4.4 Adapter — Kafka**
- Consumer：`PaymentCompletedEventConsumer`
- Producer：`InventoryDeductedEvent`

---

### 第五章：E2E 全鏈路測試 (Day 8)

**5.1 Singleton Container 基礎設施**
- `SharedContainers` 類別統一管理 8 個容器
- 使用 `Network.newNetwork()` 讓容器互通
- 容器啟動順序與健康檢查

**5.2 全鏈路 Happy Path**
- Keycloak 取得 JWT → 呼叫商品服務下單
  → Kafka 傳遞 OrderCreatedEvent
  → 支付服務處理 (Vault 取密鑰 + MinIO 存收據 + Pub/Sub 通知)
  → Kafka 傳遞 PaymentCompletedEvent
  → 庫存服務扣庫存 (Redis Lock)
  → Kafka 傳遞 InventoryDeductedEvent
  → 商品服務更新訂單狀態 + ES 索引 + Redis 快取失效
- `Awaitility.await()` 等待非同步事件收斂

**5.3 Saga 補償測試**
- 支付成功但庫存不足 → InventoryDeductionFailedEvent → 退款
- Kafka Dead Letter Topic 驗證
- 最終一致性驗證：所有服務的狀態都回到一致

**5.4 Adapter 切換展示**
- 同一套 E2E 測試，切換 Profile：
  - `test-real`：全部使用 Testcontainers 真實容器
  - `test-lite`：EventPublisher 換成 InMemory（不啟動 Kafka）
  - 展示 Hexagonal 帶來的彈性

---

### 第六章：進階主題 (Day 9-10)

**6.1 Contract Test — Port 行為契約**
- 為每個 Outbound Port 撰寫 Contract Test
- 同一份測試跑 InMemory 與 Real Adapter，確保行為一致
- 新增 Adapter 時只需通過 Contract Test 即可上線

**6.2 Chaos Testing — Toxiproxy**
- Testcontainers + ToxiproxyContainer
- 模擬：Kafka broker 延遲、Redis 斷線、PostgreSQL timeout
- 驗證：Circuit Breaker、Retry、Fallback 行為

**6.3 BDD 整合**
- Cucumber Feature File 直接對應 Domain 語言
- Step Definition 透過 Inbound Port 驅動（不直接呼叫 Controller）

**6.4 CI/CD 配置**
- GitHub Actions：Docker-in-Docker 設定
- 測試分群：`@Tag("unit")` / `@Tag("integration")` / `@Tag("e2e")`
- 平行執行策略：Unit 全跑、Integration 按服務分組、E2E 串行

---

## 六、Port / Adapter 快速切換一覽表

| Outbound Port | Real Adapter (Testcontainers) | Lightweight Adapter (Unit Test) | 替代方案 Adapter |
|----------------|-------------------------------|----------------------------------|-------------------|
| `OrderWriteRepository` | `JpaOrderRepository` + PostgreSQL | `InMemoryOrderRepository` | — |
| `OrderReadRepository` | `JpaOrderReadRepository` + PostgreSQL | `InMemoryOrderReadRepository` | — |
| `SearchPort` | `ElasticsearchSearchAdapter` + ES | `InMemorySearchAdapter` | `JdbcSearchAdapter` (降級) |
| `CachePort` | `RedisCacheAdapter` + Redis | `InMemoryCacheAdapter` | `CaffeineLocalCacheAdapter` |
| `EventPublisher` | `KafkaEventPublisher` + Kafka | `InMemoryEventPublisher` | `PubSubEventPublisher` |
| `NotificationPort` | `PubSubNotificationAdapter` + GCP | `LogNotificationAdapter` | `SesNotificationAdapter` (AWS) |
| `ObjectStoragePort` | `MinioObjectStorageAdapter` + MinIO | `InMemoryObjectStorageAdapter` | `S3ObjectStorageAdapter` (AWS) |
| `SecretProvider` | `VaultSecretProvider` + Vault | `PropertyFileSecretProvider` | `AwsSmSecretProvider` (AWS SM) |
| `DistributedLockPort` | `RedisDistributedLockAdapter` + Redis | `ReentrantLockAdapter` (本地鎖) | `JdbcPessimisticLockAdapter` |
| `AuthPort` | `KeycloakAuthAdapter` + Keycloak | `StubAuthAdapter` (全放行) | `Auth0AuthAdapter` |

---

## 七、可行性與風險評估

| 面向         | 評估 |
|--------------|------|
| 技術可行性   | ✅ 全部元件均有成熟 Testcontainers 支援 |
| 架構合理性   | ✅ Hexagonal + CQRS + DDD 是企業微服務主流範式 |
| SOLID 體現   | ✅ Port/Adapter 天然滿足 OCP、DIP、ISP |
| 切換彈性     | ✅ 每個外部介接都有至少 2 種 Adapter 可替換 |
| Spring Boot 4| ⚠️ 如 Spring Boot 4 正式版 API 有異動需微調 |
| 學習曲線     | ⚠️ DDD + CQRS + Hexagonal 對初學者有門檻，建議先有 DDD 基礎 |
| 硬體需求     | ⚠️ 全量 8 容器建議 16GB RAM（含 IDE + Docker）|
| 開發時間     | 完整教程含程式碼約 10 個工作天 |
