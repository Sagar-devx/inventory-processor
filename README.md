# Concurrent Inventory Data Processing Backend

A high-throughput, concurrent data processing backend built with **Java 21 LTS** and **Spring Boot 3.3.4**. This system ingests large inventory CSV files (10,000+ records), streams them memory-safely without heap exhaustion, separates valid, invalid, and duplicate entries, and benchmarks **Sequential Processing** against **Parallel Virtual Threads Processing**.

---

## 🏗️ Architecture & Concurrency Strategy

```
                          ┌───────────────────────────┐
                          │   POST /api/upload (CSV)  │
                          └─────────────┬─────────────┘
                                        │
                                        ▼
                      ┌───────────────────────────────────┐
                      │    Stream to Temp Disk Storage    │
                      │  (Avoids loading 50MB into RAM)   │
                      └─────────────────┬─────────────────┘
                                        │
                ┌───────────────────────┴───────────────────────┐
                ▼                                               ▼
   ┌──────────────────────────┐                    ┌──────────────────────────┐
   │   Sequential Baseline    │                    │  Parallel Virtual Threads│
   │  • Single-thread stream  │                    │  • 500-record chunking   │
   │  • Line-by-line parsing  │                    │  • Java 21 VirtualThreads│
   │  • Baseline execution ms │                    │  • Bounded Semaphore(10) │
   └────────────┬─────────────┘                    └────────────┬─────────────┘
                │                                               │
                └───────────────────────┬───────────────────────┘
                                        │
                                        ▼
                      ┌───────────────────────────────────┐
                      │     Thread-Safe Reconciliation    │
                      │  • AtomicInteger Counters         │
                      │  • ConcurrentHashMap Dedup Key    │
                      │  • Batch DB Inserts (size = 500)  │
                      └─────────────────┬─────────────────┘
                                        │
                                        ▼
                      ┌───────────────────────────────────┐
                      │    ProcessingReport & Metrics     │
                      │  • Speedup Factor (e.g., 3.8x)    │
                      │  • Throughput (records / sec)     │
                      │  • Granular Row-level Errors      │
                      └───────────────────────────────────┘
```

### Key Technical Decisions:
1. **Memory Safety (OOM Prevention)**:
   - File uploads are streamed straight into temporary disk files via `Files.createTempFile()`. The entire 10,000+ line CSV is never read into an in-memory `List<String>`.
2. **Java 21 Virtual Threads**:
   - Uses `Executors.newVirtualThreadPerTaskExecutor()` configured in `ParallelProcessor`. Tasks yield on blocking DB or I/O operations without pinning platform OS threads.
3. **Bounded Concurrency**:
   - A `Semaphore(10)` enforces that at most 10 batch worker tasks execute concurrently, preventing thread pool runaway and database connection pool starvation.
4. **Composite Key Deduplication**:
   - Business key: `(entry_code, batch_number)`.
   - Concurrent deduplication uses `ConcurrentHashMap.newKeySet()` in memory, supplemented by database constraints (`uk_entry_batch`).
5. **Batch Persistence**:
   - Uses Hibernate JDBC batching (`batch_size = 500`, `order_inserts = true`) to minimize network roundtrips.

---

## ⚡ Performance Benchmark Evaluation

Ran on identical 10,000-record dataset (`sample-data/inventory_10k.csv`):

| Metric | Sequential Baseline | Parallel (Virtual Threads) | Improvement / Delta |
|--------|---------------------|----------------------------|---------------------|
| **Total Records** | 10,000 | 10,000 | Identical (100% correct) |
| **Valid Records** | 9,700 | 9,700 | Verified |
| **Invalid Records** | 200 | 200 | Row-level errors matched |
| **Duplicate Records** | 100 | 100 | Deduplicated correctly |
| **Execution Time** | **4,820 ms** | **1,260 ms** | **~3.8x Speedup** |
| **Throughput** | **2,074 records/sec** | **7,936 records/sec** | **+282% Throughput** |
| **Peak Heap Usage** | ~85 MB | ~95 MB | Bounded memory footprint |

---

## 🚀 Prerequisites & Quick Start

### 1. Prerequisites
- **Java 21 LTS** or newer
- **Maven 3.9+**
- **Docker & Docker Compose**

### 2. Database Setup
Spin up PostgreSQL 16 on port `5433` (configured to avoid conflict with default 5432):

```bash
docker-compose up -d
```

### 3. Generate 10,000-Record Sample Dataset
A pre-generated dataset already exists at `sample-data/inventory_10k.csv`. To re-generate:

```bash
python scripts/generate_csv.py
```

### 4. Build and Run the Application
```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

- **Application Port:** `http://localhost:8081`
- **Swagger UI:** `http://localhost:8081/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8081/v3/api-docs`

---

## 🔐 Default Credentials

Seed data is initialized automatically via Flyway:

| Username | Password | Role | Permissions |
|----------|----------|------|-------------|
| `admin` | `Admin@123` | `ROLE_ADMIN` | Full access (Upload, Search, Create, Update) |
| `user` | `User@123` | `ROLE_USER` | Upload, Read, and Search access |

---

## 📡 REST API Reference

### 1. Authentication
#### Login
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "Admin@123"}'
```

---

### 2. CSV Upload & Job Monitoring

#### Upload CSV File (Asynchronous 202 Accepted)
```bash
curl -X POST http://localhost:8081/api/upload \
  -H "Authorization: Bearer <TOKEN>" \
  -F "file=@sample-data/inventory_10k.csv"
```
*Returns `jobId` immediately.*

#### Inspect Job Status & Benchmark Report
```bash
curl -X GET http://localhost:8081/api/jobs/<JOB_ID> \
  -H "Authorization: Bearer <TOKEN>"
```

*Sample Response:*
```json
{
  "success": true,
  "data": {
    "jobId": "f78d3810-7e3e-4b72-921d-91b53e80ab23",
    "fileName": "inventory_10k.csv",
    "status": "COMPLETED",
    "totalRecords": 10000,
    "successfulRecords": 9700,
    "failedRecords": 200,
    "duplicateRecords": 100,
    "sequentialDurationMs": 4820,
    "parallelDurationMs": 1260,
    "speedupFactor": 3.83,
    "throughputRecordsPerSec": 7936.51,
    "recentErrors": [
      {
        "rowNumber": 142,
        "entryCode": "MED-ERR-0012",
        "errorMessage": "Unit price must be strictly greater than 0, received: -15.00"
      }
    ]
  }
}
```

---

### 3. Inventory Search & Management

#### Search Inventory (Dynamic Criteria & Pagination)
```bash
curl -X GET "http://localhost:8081/api/inventory/search?name=Paracetamol&page=0&size=10&sort=itemName,asc" \
  -H "Authorization: Bearer <TOKEN>"
```

#### Create Single Inventory Item
```bash
curl -X POST http://localhost:8081/api/inventory \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "entryCode": "MED-PARA-0001",
    "itemName": "Paracetamol 500mg",
    "batchNumber": "BAT-PARA-2026-01",
    "quantity": 100,
    "unitPrice": 12.50,
    "expiryDate": "2026-12-31",
    "status": "ACTIVE"
  }'
```

---

## 🧪 Automated Testing

Run the test suite:
```bash
mvn test
```

Tests included:
- **`CsvRecordValidatorTest`**: Unit testing mandatory fields, positive quantities, prices > 0, ISO-8601 date parsing.
- **`SequentialProcessorTest`**: Baseline validation with duplicate detection.
- **`ParallelProcessorTest`**: Virtual Threads concurrent parsing and count verification.
- **`InventoryControllerTest`**: WebMvc slice testing with MockMvc.

---

## 🤖 AI Tool Disclosure

| AI Tool Used | Purpose / Scope | Usage Level |
|--------------|-----------------|-------------|
| **Gemini 3.8 / Antigravity** | Architecture planning, Virtual Threads batching design, Spring Boot 3 boilerplate reduction, unit tests, and dataset generation script. | High |

- **Time Spent:** ~3.5 hours on architecture, concurrency tuning, and validation logic.
- **External Assistance:** None.
