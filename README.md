# Streamworx Microservices Assignment

Arsitektur microservices berbasis **Java 17 (LTS)** dan **Spring Boot 3.x**, diorkestrasi menggunakan **Docker & Docker Compose**.

---

## 🏗 Arsitektur Sistem

Sistem ini terdiri dari 6 komponen (5 services + 1 API Gateway) yang saling berinteraksi:

```mermaid
flowchart TD
    Client(["Client / Browser / Postman"]) -->|HTTP Request| Gateway["API Gateway (Port 8080)<br/>Spring Cloud Gateway"]
    
    subgraph Routing ["Routing Rules"]
        Gateway -->|/api/v1/users/**| API1["API 1: User Service (Port 8081)"]
        Gateway -->|/api/v2/posts/**| API2["API 2: Post Service (Port 8082)"]
        Gateway -->|/api/v3/todos/**| API3["API 3: Todo Service (Port 8083)"]
    end

    subgraph Internal_Interactions ["Data & Service Integration"]
        API1 -->|1. Baca Cache User| Redis[("Redis Cache (Port 6379)<br/>Users Cache")]
        API1 -->|2. HTTP GET /todos?userId={id}| API3
        API1 -->|3. HTTP GET /posts?userId={id} (XML)| API2
        
        API2 -->|Simpan & Baca Posts| Postgres[("PostgreSQL (Port 5432)<br/>DB: posts_db")]
    end

    subgraph External_Sources ["External Data Sources (JSONPlaceholder)"]
        API1 -.->|Auto-seed Users saat startup| JP_Users["jsonplaceholder.typicode.com/users"]
        API2 -.->|Auto-seed Posts saat startup| JP_Posts["jsonplaceholder.typicode.com/posts"]
        API3 -.->|Proxy fetch on-demand| JP_Todos["jsonplaceholder.typicode.com/todos"]
    end
```

---

## 📋 Ringkasan Services & Port Mapping

| Service | Port | Teknologi / Stack | Deskripsi & Fungsi |
| :--- | :---: | :--- | :--- |
| **`api-gateway`** | `8080` | Spring Cloud Gateway (WebFlux) | Entry point tunggal untuk routing `/api/v1`, `/api/v2`, dan `/api/v3`. |
| **`api-1-user-service`** | `8081` | Spring Boot 3, Redis, Jackson XML | Mengambil User dari Redis, memanggil API 3 (Todos) dan API 2 (Posts XML), lalu menggabungkan menjadi JSON response. |
| **`api-2-post-service`** | `8082` | Spring Boot 3, Spring Data JPA, PostgreSQL, Jackson XML | Mengelola data Posts di PostgreSQL, seeding otomatis saat startup, dan menghasilkan output berformat **XML** (`<root><row>...</row></root>`). |
| **`api-3-todo-service`** | `8083` | Spring Boot 3, RestClient | Proxy service untuk mengambil Todos dari JSONPlaceholder berdasarkan `userId`. |
| **`postgres-db`** | `5432` | PostgreSQL 15 Alpine | Database untuk menyimpan data posts (`posts_db`, user: `user`, password: `password`). |
| **`redis-cache`** | `6379` | Redis 7 Alpine | Cache penyimpanan data master Users. |

---

## 📁 Struktur Direktori Project

```text
streamworx-assignment/
├── pom.xml                               # Root Maven POM (Aggregator multi-module)
├── docker-compose.yml                    # Konfigurasi orkestrasi seluruh container
├── README.md                             # Dokumentasi project
├── .gitignore
├── .dockerignore
│
├── api-gateway/                          # API Gateway (Spring Cloud Gateway)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/streamworx/gateway/
│       │   └── ApiGatewayApplication.java
│       └── resources/
│           └── application.yml
│
├── api-1-user-service/                   # API 1 - User Service & Data Aggregator
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/streamworx/userservice/
│       │   ├── Api1UserServiceApplication.java
│       │   ├── config/
│       │   │   └── AppConfig.java
│       │   ├── controller/
│       │   │   └── UserController.java
│       │   ├── dto/
│       │   │   ├── AddressDto.java
│       │   │   ├── CompanyDto.java
│       │   │   ├── GeoDto.java
│       │   │   ├── PostDto.java
│       │   │   ├── PostXmlResponse.java
│       │   │   ├── TodoDto.java
│       │   │   ├── UserDetailResponse.java
│       │   │   └── UserDto.java
│       │   ├── exception/
│       │   │   ├── GlobalExceptionHandler.java
│       │   │   └── UserNotFoundException.java
│       │   ├── loader/
│       │   │   └── UserDataLoader.java      # Auto-seed Users ke Redis
│       │   └── service/
│       │       └── UserService.java         # Fetch Redis + API 3 + API 2 (parse XML)
│       └── resources/
│           └── application.yml
│
├── api-2-post-service/                   # API 2 - Post Service (PostgreSQL + XML Output)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/streamworx/postservice/
│       │   ├── Api2PostServiceApplication.java
│       │   ├── controller/
│       │   │   └── PostController.java
│       │   ├── dto/
│       │   │   ├── PostDto.java             # XML item <row>
│       │   │   └── PostXmlResponse.java     # XML root <root>
│       │   ├── entity/
│       │   │   └── Post.java
│       │   ├── loader/
│       │   │   └── PostDataLoader.java      # Auto-seed Posts ke PostgreSQL
│       │   ├── repository/
│       │   │   └── PostRepository.java
│       │   └── service/
│       │       └── PostService.java
│       └── resources/
│           └── application.yml
│
└── api-3-todo-service/                   # API 3 - Todo Proxy Service
    ├── pom.xml
    ├── Dockerfile
    └── src/main/
        ├── java/com/streamworx/todoservice/
        │   ├── Api3TodoServiceApplication.java
        │   ├── controller/
        │   │   └── TodoController.java
        │   ├── dto/
        │   │   └── TodoDto.java
        │   └── service/
        │       └── TodoService.java
        └── resources/
            └── application.yml
```

---

## 🚀 Cara Menjalankan Project

### Prasyarat
- **Java 17+** & **Maven 3.9+** (untuk mem-package jar).
- **Docker & Docker Compose** terinstall pada sistem.

### 1. Menjalankan Menggunakan Docker Compose (Direkomendasikan)

Tersedia script otomatis sekali klik:
- **Windows:** Jalankan `run.bat` (atau `.\run.bat` di terminal).
- **Linux / macOS:** Jalankan `./run.sh` (pastikan izin execute: `chmod +x run.sh`).

Atau jalankan perintah manual berikut di root folder project:

```bash
# Step 1: Package seluruh module microservices
mvn clean package -DskipTests

# Step 2: Build & jalankan seluruh container
docker compose up --build
```
*(Atau gunakan flag `-d` untuk mode background: `docker compose up -d --build`)*

Docker Compose akan otomatis:
1. Menjalankan container database `postgres-db` dan melakukan pengecekan healthcheck.
2. Menjalankan container `redis-cache` dan melakukan pengecekan healthcheck.
3. Membangun (*build*) image masing-masing Spring Boot microservice secara multi-stage.
4. Menjalankan `api-2-post-service` dan otomatis melakukan seeding 100 posts dari JSONPlaceholder ke PostgreSQL jika tabel masih kosong.
5. Menjalankan `api-1-user-service` dan otomatis melakukan seeding 10 users dari JSONPlaceholder ke Redis cache jika belum ada.
6. Menjalankan `api-3-todo-service` dan `api-gateway`.

Untuk menghentikan semua service:
```bash
docker compose down
```

---

## 🔍 Daftar Endpoint & Pengujian (API Testing)

> [!TIP]
> **Postman Collection Siap Pakai:** File collection [Streamworx_Microservices.postman_collection.json](Streamworx_Microservices.postman_collection.json) sudah tersedia di root project. Anda cukup mengklik **Import** di Postman dan memilih file tersebut untuk langsung mengetes semua skenario request.

Semua request dapat diarahkan melalui **API Gateway (`http://localhost:8080`)**:

### 1. API 1: Detail User beserta Todos dan Posts (Combined JSON)
- **Method:** `GET`
- **URL via Gateway:** `http://localhost:8080/api/v1/users/1`
- **Direct URL:** `http://localhost:8081/users/1`
- **Format Output:** JSON

#### Contoh Request:
```bash
curl -X GET http://localhost:8080/api/v1/users/1
```

#### Contoh Response:
```json
[
  {
    "id": 1,
    "name": "Leanne Graham",
    "username": "Bret",
    "email": "Sincere@april.biz",
    "address": {
      "street": "Kulas Light",
      "suite": "Apt. 556",
      "city": "Gwenborough",
      "zipcode": "92998-3874",
      "geo": {
        "lat": "-37.3159",
        "lng": "81.1496"
      }
    },
    "phone": "1-770-736-8031 x56442",
    "website": "hildegard.org",
    "company": {
      "name": "Romaguera-Crona",
      "catchPhrase": "Multi-layered client-server neural-net",
      "bs": "harness real-time e-markets"
    },
    "todos": [
      {
        "id": 1,
        "title": "delectus aut autem",
        "completed": false
      },
      {
        "id": 2,
        "title": "quis ut nam facilis et officia qui",
        "completed": false
      }
    ],
    "posts": [
      {
        "id": 1,
        "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
        "body": "quia et suscipit\nsuscipit recusandae consequuntur expedita et cum..."
      },
      {
        "id": 2,
        "title": "qui est esse",
        "body": "est rerum tempore vitae\nsequi sint nihil non debitis possimus..."
      }
    ]
  }
]
```

---

### 2. API 2: Daftar Posts dari PostgreSQL (XML Output)
- **Method:** `GET`
- **URL via Gateway (Semua Posts):** `http://localhost:8080/api/v2/posts`
- **URL via Gateway (Filter userId):** `http://localhost:8080/api/v2/posts?userId=1`
- **Direct URL:** `http://localhost:8082/posts?userId=1`
- **Format Output:** XML (`Content-Type: application/xml`)

#### Contoh Request:
```bash
curl -X GET "http://localhost:8080/api/v2/posts?userId=1"
```

#### Contoh Response:
```xml
<root>
  <row>
    <userId>1</userId>
    <id>1</id>
    <title>sunt aut facere repellat provident occaecati excepturi optio reprehenderit</title>
    <body>quia et suscipit
suscipit recusandae consequuntur expedita et cum
reprehenderit molestiae ut ut quas totam
nostrum rerum est autem sunt rem eveniet architecto</body>
  </row>
  <row>
    <userId>1</userId>
    <id>2</id>
    <title>qui est esse</title>
    <body>est rerum tempore vitae
sequi sint nihil reprehenderit dolor beatae ea dolores neque
fugiat blanditiis voluptate porro vel nihil molestiae ut reiciendis
qui aperiam non debitis possimus qui neque nisi nulla</body>
  </row>
</root>
```

---

### 3. API 3: Daftar Todos (Proxy Service)
- **Method:** `GET`
- **URL via Gateway (Semua Todos):** `http://localhost:8080/api/v3/todos`
- **URL via Gateway (Filter userId):** `http://localhost:8080/api/v3/todos?userId=1`
- **Direct URL:** `http://localhost:8083/todos?userId=1`
- **Format Output:** JSON

#### Contoh Request:
```bash
curl -X GET "http://localhost:8080/api/v3/todos?userId=1"
```

#### Contoh Response:
```json
[
  {
    "userId": 1,
    "id": 1,
    "title": "delectus aut autem",
    "completed": false
  },
  {
    "userId": 1,
    "id": 2,
    "title": "quis ut nam facilis et officia qui",
    "completed": false
  }
]
```

---

## ⚙️ Detail Mekanisme Seeding Data Otomatis

1. **PostgreSQL Seeding (`api-2-post-service`)**:
   - Komponen `PostDataLoader` mengimplementasikan `CommandLineRunner`.
   - Mengecek apakah tabel `posts` kosong (`postRepository.count() == 0`).
   - Jika kosong, melakukan HTTP request ke `https://jsonplaceholder.typicode.com/posts`, kemudian menyimpan 100 record posts ke PostgreSQL database `posts_db`.

2. **Redis Seeding (`api-1-user-service`)**:
   - Komponen `UserDataLoader` mengimplementasikan `CommandLineRunner`.
   - Mengecek apakah key flag `users:seeded` ada di Redis.
   - Jika belum ada, mengambil data users dari `https://jsonplaceholder.typicode.com/users`, kemudian menyimpan tiap user dengan key `user:{id}` (berformat JSON) serta mendaftarkan ID ke dalam set `users:ids`.

---

## 🛠 Build & Development Lokal (Tanpa Docker)

Jika Anda ingin menjalankan database dan cache melalui docker, namun menjalankan aplikasi Spring Boot secara lokal di IDE (IntelliJ / VS Code / Eclipse):

1. Jalankan PostgreSQL dan Redis saja:
   ```bash
   docker compose up -d postgres-db redis-cache
   ```

2. Compile seluruh module:
   ```bash
   mvn clean package -DskipTests
   ```

3. Jalankan masing-masing service sesuai urutan:
   - `api-3-todo-service`: `mvn -pl api-3-todo-service spring-boot:run`
   - `api-2-post-service`: `mvn -pl api-2-post-service spring-boot:run`
   - `api-1-user-service`: `mvn -pl api-1-user-service spring-boot:run`
   - `api-gateway`: `mvn -pl api-gateway spring-boot:run`
