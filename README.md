# J-LanDrive

轻量级局域网文件共享工具，支持免客户端跨设备（手机/电脑/平板）进行文件浏览、即时下载、多选打包 ZIP 与大文件快速上传。

## 是什么 / 解决什么问题

在局域网多设备协作场景下，传统传输方式常常面临需要安装特定客户端、依赖外部网络或存在传输体积限制的问题。J-LanDrive 是一款基于 Spring Boot 3 与 Vue 3 的轻量级单体 Web 应用，直接挂载本机文件系统提供以下能力：

- **磁盘与文件浏览**：跨平台查看主机磁盘根驱动器与目录树，内置桌面、文档、下载等快捷入口。
- **单文件与批量下载**：单文件直链高速下载；多选或目录自动流式压缩为 ZIP 打包下载，兼容 IDM 等并发下载工具。
- **大文件快速上传**：支持最大 10GB 单文件与批量分段上传，重名文件自动追加防冲突序号；根视图设置写保护。
- **纯内网零外链**：前端资源全本地打包托管，无外网依赖，随起随用。

## 快速开始

三条命令即可在本地完成构建并启动服务：

```bash
# 1. 克隆代码仓库
git clone https://github.com/soldier-cv/J-LanDrive.git && cd J-LanDrive

# 2. 编译打包应用（需 Java 21+ 与 Maven 3.x）
mvn clean package -DskipTests

# 3. 启动 Web 服务
java -jar target/j-landrive-0.0.1-SNAPSHOT.jar
```

启动完成后，在局域网内任意设备的浏览器访问：

```text
http://<本机局域网IP>:8000
```

## 部署与打包

### 1. 常驻服务部署

通过 Java 运行时直接后台常驻：

```bash
nohup java -Xms256m -Xmx1024m -jar target/j-landrive-0.0.1-SNAPSHOT.jar > landrive.log 2>&1 &
```

- **监听端口**：`8000`（HTTP）
- **依赖服务**：零外部数据库与中间件依赖，直接读写本地文件系统。

### 2. Windows 独立程序打包

仓库内附带针对 Windows 平台的离线打包脚本：

- **基于 jpackage 打包独立运行目录**：
  ```powershell
  .\package.ps1
  ```
  生成带内置 JRE 运行时的独立目录（输出于 `dist/J-LanDrive`），双击即可运行，用户电脑无需预装 Java。

- **基于 GraalVM Native Image 编译原生可执行文件**：
  ```powershell
  .\package-native.ps1
  ```
  通过 GraalVM AOT 编译为毫秒级秒开的独立 `.exe` 二进制程序。

### 3. 可选 Docker 部署

若作为家庭 NAS 或 Linux 服务器上的网盘服务，支持基于容器运行：

```bash
docker run -d \
  --name j-landrive \
  -p 8000:8000 \
  -v /data/shared:/shared \
  eclipse-temurin:21-jre-alpine \
  sh -c "exec java -jar /app/j-landrive.jar"
```

## 配置

本应用遵循开箱即用设计，无需初始数据库建表或 `.env` 凭据文件。核心参数预设于 `src/main/resources/application.properties`，并支持通过命令行参数或系统环境变量覆盖：

| 属性名 (properties) | 环境变量 (ENV) | 必填 | 默认值 | 说明 |
|---|---|:---:|---|---|
| `server.port` | `SERVER_PORT` | 否 | `8000` | Web 服务监听端口 |
| `spring.servlet.multipart.max-file-size` | `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE` | 否 | `10GB` | 单个上传文件大小上限 |
| `spring.servlet.multipart.max-request-size` | `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE` | 否 | `10GB` | 单次 Multipart 请求总大小上限 |

如需临时改端口启动：

```bash
java -jar target/j-landrive-0.0.1-SNAPSHOT.jar --server.port=9000
# 或者使用环境变量
SERVER_PORT=9000 java -jar target/j-landrive-0.0.1-SNAPSHOT.jar
```

## 已知限制与故障排查

### 1. 安全与网络信任域

- **无鉴权设计**：项目当前未内置登录认证，局域网内可触达该端口的用户均具备浏览与下载权限。
- **使用建议**：严禁直接映射至公网；建议仅在可信家庭/办公局域网或 VPN (如 WireGuard / Tailscale) 内运行。

### 2. 上传与路径限制

- **根视图写保护**：在根驱动器列表视图（`/`）禁止直接上传文件，必须点击进入具体磁盘或文件夹后方可执行上传。
- **重名处理**：同一目录下上传同名文件时，后端会自动在文件名后追加序号（如 `file(1).txt`），避免静默覆盖。

### 3. 下载与多线程下载器 (IDM) 机制

- **多选压缩预准备**：批量打包采用两阶段预处理机制（`/api/download/zip/prepare` 生成临时 token），临时 ZIP 缓存有效期为 5 分钟，避免 IDM 等多线程工具发起探测连接时重复全量压缩。

### 4. 端口占用排查

若启动报 `Port 8000 is already in use`：

```bash
# Linux / macOS
lsof -i :8000 | awk 'NR>1 {print $2}' | xargs kill -9

# Windows (PowerShell)
Get-NetTCPConnection -LocalPort 8000 | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

## 技术栈

- **后端**：Spring Boot 3.5.x / Java 21 / Hutool
- **前端**：Vue 3 / Element Plus（全内联静态资源）
- **打包工具**：Maven 3 / jpackage / GraalVM Native Build Tools

## 许可证

[MIT License](LICENSE)
