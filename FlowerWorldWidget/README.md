# 花花世界 - macOS Menu Bar Widget

> 你的 AI 聊天助手，常驻 Mac 菜单栏。


## 效果

菜单栏出现一片绿叶图标 `🌿`，点击后弹出聊天窗口，即可直接与 AI 对话。

## 前置要求（用户）

- macOS 13.0 (Ventura) 或更高版本
- **Java 后端已在运行**（默认 `http://localhost:8080`）

## 获取安装包

### 方式一：GitHub Actions 自动构建（推荐，无需 Xcode）

1. 推送代码到 GitHub
2. 进入 GitHub 仓库 → Actions → `Build macOS App` → 最新一次运行结果
3. 在 **Artifacts** 区域下载 `花花世界.zip`
4. 解压得到 `花花世界.app`，拖入 `/Applications/` 即可

### 方式二：自己用 Xcode 构建

需要 Xcode 15+（仅需编译一次，之后可卸载 Xcode）。

```bash
chmod +x build.sh
./build.sh
```

## 运行

1. 确保 **Java 后端** 已在运行（`cd flower-world-backend && java -jar target/flower-world-0.0.1-SNAPSHOT.jar`）
2. 启动 `花花世界.app`
3. 菜单栏出现绿叶图标，点击即可使用
4. 点击 ⚙️ 齿轮可修改后端地址

## 修改后端地址

默认连接 `http://localhost:8080`。如果你的后端运行在不同端口，点击聊天窗口右上角的 ⚙️ 齿轮图标修改地址。

## 工作原理

```
┌──────────────────┐     HTTP      ┌──────────────────┐     OpenAI API     ┌──────────┐
│  花花世界.app    │ ──────────►   │  Java Backend    │ ──────────────►   │ OpenClaw │
│  (Menu Bar)      │ ◄──────────   │  (Spring Boot)   │ ◄──────────────   │ (AI)     │
└──────────────────┘    SSE Stream └──────────────────┘                   └──────────┘
```

- 菜单栏应用使用 WKWebView 加载前端界面
- 前端通过 HTTP 请求后端 API（`/api/chat`、`/api/sessions` 等）
- 后端转发到 OpenAI 兼容的 AI 服务，流式返回结果
