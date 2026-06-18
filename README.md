# J-LanDrive

一个用于局域网内浏览、下载和上传本机文件的轻量 Web 工具。

## 功能

- 文件浏览：查看本机磁盘和目录
- 单文件下载：直接下载
- 多选下载：自动打包 ZIP
- 快速上传：上传到当前目录，支持重名自动改名
- 快捷入口：桌面、文档、下载等常用目录

## 运行环境

- Java 21+
- Maven 3.x

## 构建

```bash
mvn clean package -DskipTests
```

## 运行

```bash
java -jar target/j-landrive-0.0.1-SNAPSHOT.jar
```

默认端口：`8000`

访问地址：

```text
http://<你的IP地址>:8000
```

## 上传说明

- 需要先进入具体目录
- 根视图不允许直接上传
- 重名文件会自动追加序号

## 下载说明

- 单文件下载会直接返回原文件
- 多文件或目录会打包为 ZIP
- 已做中文文件名兼容处理

## 安全提示

该项目默认不带认证，局域网内可访问到它的人，也能浏览和下载你暴露出来的文件。
建议只在可信网络中使用，后续最好加访问口令或目录白名单。

## Windows 打包

- `package.ps1`
- `package-native.ps1`

## 技术栈

- Spring Boot 3.5.x
- Java 21
- Vue 3
- Element Plus
- Hutool

## 许可证

MIT
