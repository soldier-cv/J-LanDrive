# 变更日志 (CHANGELOG)

本文件记录 J-LanDrive 的所有重要版本演进与变更。
格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，并遵循 [语义化版本 2.0.0](https://semver.org/lang/zh-CN/)。

## Unreleased

### Added
- 补齐 GitHub Actions 工作流：`.github/workflows/ci.yml`（编译检测）、`release.yml`（tag 触发构建发版）、`sync-gitee.yml`（国内镜像同步）。
- 补齐品牌 Favicon 图标链：新增 `favicon.ico` 与 `apple-touch-icon.png`，并在前端入口 `index.html` 引入。
- 补齐工程规范文件：新增 `.editorconfig`（统一缩进与换行）与 `.gitattributes`（处理跨平台文本换行）。
- 新增 `LICENSE`（MIT 授权）与写实化规约 `AGENTS.md`。

### Changed
- 重构 `.gitignore`：完善构建目录（`dist/`、`target/`）、凭据与环境变量、IDE 配置规则。
- 完善 `README.md`：按五段式重构，补充生产常驻部署、配置映射表、Windows 打包及故障排查说明。

## [0.0.1] - 2026-06-23

### Fixed
- 修复 IDM 等多线程下载器请求 ZIP 包时文件名不一致及多次压缩问题：引入两阶段预压缩机制（`/api/download/zip/prepare` 生成 5 分钟有效 Token，由专用流式端点下载并自动清理）。

### Added
- 首次发布局域网文件共享服务：
  - 基于 Spring Boot 3 + Vue 3 的轻量文件浏览器。
  - 支持单文件直链下载与多文件批量打包 ZIP。
  - 支持进入具体目录后的大文件（上限 10GB）上传与防重名递增重命名。
  - 提供 Windows 平台 `package.ps1` (jpackage) 与 `package-native.ps1` (GraalVM native) 打包脚本。
