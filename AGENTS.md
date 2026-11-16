# J-LanDrive — Agent 工作规约

## 0. 项目定位

- **定位**：轻量级局域网文件共享工具，支持跨设备文件浏览、单文件/多选打包 ZIP 下载、防重名大文件上传与 Windows 离线打包。
- **代码仓库**：`https://github.com/soldier-cv/J-LanDrive.git`
- **默认分支**：`master`

## 1. 运行环境事实

- **工作目录**：`/opt/code/J-LanDrive-repo`
- **监听端口**：`8000`（HTTP），验证方式：`curl -sI http://127.0.0.1:8000/favicon.ico`
- **依赖真源**：无外部数据库、Redis 或对象存储服务，直接读写宿主机本地文件系统（`File.listRoots()`、`new File(path)`）。
- **配置真源**：`src/main/resources/application.properties`（端口 `server.port=8000`，上传限制 10GB），不依赖 `.env`。
- **前端静态资源**：`src/main/resources/static/index.html` 与 `src/main/resources/static/vendor/`（Vue 3 + Element Plus，全本地打包，无外部 CDN 依赖）。
- **产物入口**：`target/j-landrive-0.0.1-SNAPSHOT.jar`，启动命令：`java -jar target/j-landrive-0.0.1-SNAPSHOT.jar`；Windows 独立产物：`dist/J-LanDrive`。

## 2. 硬规则

1. **禁止在根目录（`/`）允许直接上传文件**
   - **原因**：根目录 `/` 对应操作系统驱动器根列表（如 Windows 的 `C:\`、`D:\` 或 Linux 根挂载点），直接上传会导致系统根目录污染或权限异常。
   - **验证方式**：`git grep -n "根目录不允许直接上传" src/main/java/com/jlandrive/service/FileService.java` 必须命中保护逻辑。
2. **批量 ZIP 下载必须维持两阶段 Token 缓存（PreparedZip）机制**
   - **原因**：多线程下载器（如 IDM）会在建立下载任务时发起探测请求。直接动态生成 ZIP 会造成多次重复压缩和文件名时间戳不一致。
   - **验证方式**：`git grep -n "prepareZipDownload" src/main/java/com/jlandrive/` 必须保留令牌生成与流关闭释放逻辑。
3. **界面严禁堆砌需求说明式文案（§0.2 界面文本纪律）**
   - **原因**：需求描述、修复原因只进代码注释或 CHANGELOG，严禁呈现在前端页面中给普通用户看。
   - **验证方式**：`grep -nE '修复|为了满足|本功能实现|需求' src/main/resources/static/index.html | grep -v '<meta' | grep -v 'title'` 输出必须为空。
4. **禁止引入任何外部远程 CDN 资源**
   - **原因**：本项目定位纯内网/离线局域网传输，所有前端 JS/CSS 脚本必须严格内置于 `src/main/resources/static/vendor/`。
   - **验证方式**：`grep -nE 'https?://(cdn|unpkg|cdnjs|jsdelivr)' src/main/resources/static/index.html` 输出必须为空。
5. **打包产物与凭据绝对禁止入库**
   - **原因**：`target/`、`dist/` 编译包或临时文件入库会迅速污染代码仓库。
   - **验证方式**：`git ls-files | grep -E '\.jar$|\.exe$|\.env$|\.pem$|\.key$|^dist/|^target/'` 必须为空。

## 3. 开发与验证流程

1. **开工第一步**：
   ```bash
   git fetch origin && git pull --ff-only origin master
   ```
   多环境协作开发前必须确认本地基线对齐远端。
2. **编译打包**：
   ```bash
   mvn clean package -DskipTests
   ```
3. **本地启动与冒烟验证**：
   ```bash
   java -jar target/j-landrive-0.0.1-SNAPSHOT.jar &
   PID=$!
   sleep 3
   curl -sI http://127.0.0.1:8000/ | grep "HTTP/1.1 200"
   curl -sI http://127.0.0.1:8000/favicon.ico | grep "HTTP/1.1 200"
   kill -9 $PID
   ```
4. **提交规约**：使用 Conventional Commits 格式，单次提交只聚焦单一职责。

## 4. 已知坑

- **IDM 多线程下载 ZIP 名称变动**：IDM 会拆分多个连接拉取文件。若每次请求由时间戳生成临时 zip，各连接文件名不一致导致下载器报错。解决方案为 `/api/download/zip/prepare` 先行生成缓存并在下载完成后删除。
- **中文文件名在 Content-Disposition 中截断乱码**：直接使用 ASCII 引号文件名会导致现代浏览器乱码。必须在 `FileService` 中使用 RFC 5987 编码标准 `filename*=UTF-8''...`。
- **大文件上传被 Spring 默认限制截断**：Spring Boot 默认单文件上限仅 1MB，大文件上传报 `MaxUploadSizeExceededException`。必须保持 `application.properties` 中 `spring.servlet.multipart.max-file-size=10GB`。
- **Windows 盘符路径反斜杠转义**：Windows 路径包含 `\`，必须统一经过标准化规整，避免拼接导致路径穿越或找不到目标文件。

## 5. 需求台账 / 决策日志

- `2026-06-23`：完成局域网文件共享核心功能（磁盘根驱动器遍历、直链下载、多选 ZIP 压缩打包、10GB 大文件上传）。
- `2026-06-23`：排查并解决 IDM 等并发下载器请求 ZIP 包时名称不一致问题，建立两阶段预压缩 Token 机制（提交 `e3e2193`）。
- `2026-11-16`：完成仓库工程规范治理，补齐 GitHub Actions 流水线、Favicon 图标链与工程元文件，按真实业务形态写实 AGENTS.md。
