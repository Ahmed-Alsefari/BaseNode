<div align="center">

<img src="BaseNode/src/main/resources/static/images/BaseNode.png" alt="BaseNode Logo" width="180"/>

# BaseNode

**A lightweight personal file server — access your files from any browser, anywhere.**

![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?style=flat-square&logo=springboot)
![H2](https://img.shields.io/badge/Database-H2-blue?style=flat-square)
![Thymeleaf](https://img.shields.io/badge/Template-Thymeleaf-005F0F?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square)

</div>

---

## What is BaseNode?

Most people have no simple, free way to access their home PC files remotely. Cloud storage costs money. Remote desktop is overkill. Network drives only work at home.

BaseNode solves this — install it on your PC, and access your files from any phone, laptop, or tablet through a browser. Your files stay on your machine. Private. Free. Simple.

---

## Features

- 📁 Browse, upload, download, and delete files and folders
- 🔄 Real-time sync — changes on disk appear instantly in the browser (no refresh needed)
- 🗂 Folder navigation with breadcrumb trail
- 📦 Download entire folders as `.zip`
- 🔐 Login system to protect access
- ⚠️ Upload size limit with friendly error notifications
- 🖥 Works from any browser on any device

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 3.5 |
| Template Engine | Thymeleaf |
| Database | H2 (embedded, file-based) |
| ORM | Hibernate / Spring Data JPA |
| Security | Spring Security + BCrypt |
| Real-time | Java WatchService + SSE (Server-Sent Events) |
| Build Tool | Maven |

---

## Getting Started

### Requirements

- Java 17 or higher
- Maven 3.6 or higher

### Run the project

```bash
# 1. Clone the repository
git clone https://github.com/Ahmed-Alsefari/BaseNode.git
cd BaseNode/BaseNode

# 2. Build the project
mvn clean install

# 3. Run it
mvn spring-boot:run
```

Then open your browser and go to:

```
http://localhost:8080
```

---

## Configuration

All settings are in `BaseNode/src/main/resources/application.properties`.


# Upload size limit (change to whatever suits your machine)
spring.servlet.multipart.max-file-size=**500MB**  
spring.servlet.multipart.max-request-size=**500MB**  
basenode.max-upload-size=**500MB**

# Prevents connection reset on oversized uploads
server.tomcat.max-swallow-size=-1


> The `Uploads` folder is created automatically one level above the project directory on first run.

---

## Project Structure

```
BaseNode/
├── src/main/java/com/BaseNode/BaseNode/
│   ├── controller/       # Web + API controllers
│   ├── service/          # Business logic + file watcher
│   ├── model/            # JPA entities
│   ├── repository/       # Spring Data repositories
│   ├── factory/          # EntityFactory (Factory pattern)
│   └── config/           # Security + storage config
├── src/main/resources/
│   ├── templates/        # Thymeleaf HTML pages
│   ├── static/           # Images
│   └── application.properties
└── pom.xml
```

---

## Design Patterns Used

- **Factory Pattern** — `EntityFactory` centralizes object creation for files, folders, users, and requests
- **Proxy Pattern** — `SecureFileServiceProxy` wraps file service calls with session-based access control

---

## Screenshots

> Coming soon.

---

## License

MIT — free to use, modify, and share.

---

<div align="center">
  Made by <a href="https://github.com/Ahmed-Alsefari/BaseNode">BaseNode team</a>
</div>
