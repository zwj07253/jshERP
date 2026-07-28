# YUEWEIERP Web

YUEWEIERP 的前端项目，基于 Vue 2、Vue Router、Vuex 和 Ant Design Vue 构建。后端默认 API 前缀为 `/jshERP-boot`，菜单与按钮权限由登录用户的后端授权数据动态生成。

## 技术栈

| 组件 | 当前版本/用途 |
| --- | --- |
| Vue | 2.7.16 |
| Vue Router | 3.x，基础路由与动态业务路由 |
| Vuex | 3.x，用户、权限和标签页状态 |
| Ant Design Vue | 1.5.2，界面组件 |
| Axios | 0.18.x，HTTP 请求 |
| Vue CLI | 3.x，开发与生产构建 |
| Node.js | 推荐 20.17.0 |

## 安装与启动

```bash
cd jshERP-web
npm install
npm run serve
```

开发服务器默认地址为 http://localhost:3000。`vue.config.js` 已配置代理：

```text
/jshERP-boot  ->  http://localhost:9999
```

因此启动前端前，请确认后端已在本机 `9999` 端口运行。

也可以使用 Yarn：

```bash
yarn install
yarn serve
```

## 生产构建

```bash
npm run build
```

产物输出到 `dist/`。生产构建会关闭 source map，并为满足阈值的 JS、CSS、Less 资源生成压缩文件。

项目提供 [Dockerfile](Dockerfile)：构建阶段使用 `node:20.17.0-alpine`，运行阶段使用 `nginx:1.25-alpine`。在仓库根目录执行以下命令可同时启动前端、后端、PostgreSQL 和 Redis：

```bash
docker compose up -d --build
```

Compose 将前端映射到 `WEB_PORT`；未设置时为 http://localhost（端口 `80`）。项目根目录 `.env` 可以覆盖这个端口。

## 目录说明

```text
src/
├── api/            # 后端接口封装
├── assets/         # 静态资源
├── components/     # 公共组件与布局组件
├── config/         # 基础路由配置
├── router/         # Vue Router 实例
├── store/          # Vuex 模块
├── utils/          # 请求、菜单转换等工具
└── views/          # 业务页面
```

常用别名由 `vue.config.js` 提供：`@` 指向 `src`，另有 `@api`、`@assets`、`@comp` 与 `@views`。

## 菜单、路由与权限

基础路由定义在 `src/config/router.config.js`，只包含登录、首页和 404 等页面。用户登录后，`src/permission.js` 会请求后端菜单数据并通过 `generateIndexRouter` 转换为业务路由，再调用 `router.addRoutes()` 注册。

这意味着大多数业务页面不是在 `router.config.js` 中手工静态登记，而是由后端菜单配置决定。新增业务页面通常需要同时完成：

1. 新建或更新 `src/views` 中的页面组件；
2. 在后端菜单数据中配置页面 URL、组件路径和角色权限；
3. 为角色分配菜单和需要的按钮权限；
4. 退出并重新登录，刷新菜单与按钮权限缓存。

更详细的路由字段说明见 [src/router/README.md](src/router/README.md)。

## 通知

顶部铃铛组件位于 `src/components/tools/HeaderNotice.vue`。它读取当前用户的未读系统消息：

- 首次载入只显示未读数量，不对历史消息重复弹窗；
- 后续每 20 秒轮询一次；
- 新产生的未读消息会显示通知弹窗；
- 用户点击消息后会标记为已读并打开详情。

库存预警消息由后端在强审核模式下创建；前端只负责显示，不负责判断库存阈值。

## 常用命令

```bash
# 开发服务器
npm run serve

# 生产构建
npm run build
```

如果依赖安装或构建出现版本问题，请先确认 Node.js 为 20.17.0；不要同时混用多个包管理器生成的锁文件。
