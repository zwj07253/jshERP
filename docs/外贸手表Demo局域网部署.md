# 外贸手表 Demo 局域网部署

## 目标

在同一台服务器上保留现有 `master` 正式环境，同时运行外贸手表 Demo。Demo 使用独立的 Docker Compose 项目、PostgreSQL 数据库、Redis 数据和持久化卷，不修改正式环境的容器或数据。

部署完成后，局域网内通过以下地址访问：

```text
http://服务器IP:18089
```

例如服务器 IP 为 `192.168.1.20`，访问地址就是 `http://192.168.1.20:18089`。

## 端口与隔离边界

| 项目 | 正式环境（示例） | 外贸 Demo |
| --- | --- | --- |
| Web | `8088` | `18089` |
| PostgreSQL | 现有端口 | 不对宿主机开放 |
| Redis | 现有端口 | 不对宿主机开放 |
| 后端 API | 现有端口 | 不对宿主机开放 |

Demo 仅将 Nginx 的 Web 端口暴露到局域网。浏览器访问 `/jshERP-boot` 时，Nginx 会在 Docker 内部转发给 Demo 后端，因此不会误连到正式系统。

## 首次部署

以下命令在 Linux 服务器执行。请以实际仓库地址和目录替换示例值。

```bash
git clone <仓库地址> /opt/jshERP-foreign-trade
cd /opt/jshERP-foreign-trade
git checkout foreign_trade
cp .env.demo.example .env.demo
```

编辑 `.env.demo`，必须替换 `DEMO_DB_PASSWORD` 与 `DEMO_REDIS_PASSWORD` 为仅用于 Demo 的强密码；必要时调整 `DEMO_WEB_PORT`。

```bash
nano .env.demo
docker compose --env-file .env.demo -f docker-compose.demo.yml config
docker compose --env-file .env.demo -f docker-compose.demo.yml up -d --build
docker compose --env-file .env.demo -f docker-compose.demo.yml ps
```

首次启动会初始化独立的 PostgreSQL，并自动导入基础 ERP 数据与外贸手表 Demo 数据。初始化脚本只会在 Demo 数据卷首次创建时执行。

## 验收

```bash
curl -I http://127.0.0.1:18089
docker compose --env-file .env.demo -f docker-compose.demo.yml logs --tail=100 app
```

局域网内使用 `http://服务器IP:18089` 登录，演示账号可使用 `jsh / 123456`。如服务器开启了防火墙，需要仅向公司内网网段放行 TCP `18089`，不要映射或公开数据库、Redis、后端端口。

## 日常更新

```bash
cd /opt/jshERP-foreign-trade
git fetch origin
git checkout foreign_trade
git pull --ff-only origin foreign_trade
docker compose --env-file .env.demo -f docker-compose.demo.yml up -d --build
```

更新只作用于名为 `jsh-foreign-trade-demo` 的 Compose 项目，不影响服务器上的正式 `master` 容器。

## 停止、重启与清理

```bash
# 查看状态
docker compose --env-file .env.demo -f docker-compose.demo.yml ps

# 重启 Demo
docker compose --env-file .env.demo -f docker-compose.demo.yml restart

# 停止 Demo，保留演示数据
docker compose --env-file .env.demo -f docker-compose.demo.yml down

# 删除 Demo 及其全部演示数据（不可恢复，执行前确认）
docker compose --env-file .env.demo -f docker-compose.demo.yml down -v
```

## 常见问题

- 页面打不开：确认 `docker compose ... ps` 中 `web` 为运行状态，并检查服务器防火墙是否允许局域网访问 `18089`。
- 数据没有更新：初始化 SQL 仅对新建数据库数据卷生效。需要重新生成演示数据时，先确认无需保留数据，再执行 `down -v` 后重新 `up -d --build`。
- 与正式环境冲突：不要使用原有 `docker-compose.yml` 启动 Demo；必须使用 `docker-compose.demo.yml` 和 `.env.demo`。
