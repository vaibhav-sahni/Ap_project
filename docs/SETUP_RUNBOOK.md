# Setup & Runbook

## Prerequisites

- JDK 17+
- Maven 3.8+
- MySQL 8.x
- Windows PowerShell (pwsh) or terminal

## Database

1. Create database and user

```sql
CREATE DATABASE erp_db CHARACTER SET utf8mb4;
CREATE USER 'erp_user'@'localhost' IDENTIFIED BY 'erp_pass';
GRANT ALL PRIVILEGES ON erp_db.* TO 'erp_user'@'localhost';
FLUSH PRIVILEGES;
```

2. Load seed data

```powershell
mysql -u erp_user -p erp_db < "data/seeds/dummy_seed.sql"
```

## Build

```powershell
cd "c:\ROAD TO SDE\iiitd\third sem\AP project\Ap_project\erp-server"
mvn -DskipTests package
cd "..\erp-client"
mvn -DskipTests package
```

## Run

Server (IDE): run `ServerMain` in `erp-server`.

Server (JAR):

```powershell
java -jar erp-server\target\erp-server-1.0-SNAPSHOT.jar
```

Client (IDE): run `application.Application` in `erp-client`.

Client (JAR):

```powershell
java -jar erp-client\target\erp-client-1.0-SNAPSHOT.jar
```

## Environment Config

- Server DB settings in `erp-server` resources.
- Client server host/port in client config.
