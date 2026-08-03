# YUEWEIERP

YUEWEIERP 是一个基于 Spring Boot 和 Vue 2 的多租户进销存系统。项目包含采购、销售、零售、仓储、财务、商品资料、报表和系统管理等业务模块，并使用 PostgreSQL 与 Redis 提供数据存储和缓存能力。

## 技术栈

| 层级 | 组件 |
| --- | --- |
| 后端 | JDK 17、Spring Boot 2.7.18、MyBatis-Plus 3.5.7、PageHelper |
| 数据与缓存 | PostgreSQL 15、Redis 7（兼容 Redis 6.2+） |
| 前端 | Vue 2.7、Vue Router 3、Vuex 3、Ant Design Vue 1.5 |
| 构建与部署 | Maven 3.9+、Node.js 20.17+、Docker Compose |

## 主要功能

- 采购：请购、采购订单、采购入库、采购退货及转单。
- 销售与零售：销售订单、销售出库、销售退货、零售出库和零售退货。
- 仓储：其他出入库、调拨、组装、拆卸、库存流水和库存报表。
- 财务：收款、付款、收入、支出、转账、账户与往来统计。
- 基础资料与权限：商品、分类、单位、客户、供应商、仓库、用户、角色、菜单和按钮权限。
- 多租户：业务数据按租户隔离；平台管理员仅管理平台侧功能。
- 库存预警：安全库存报表；开启“强审核”后，单据审核/反审核会检查受影响的商品和仓库，并向拥有库存预警报表权限的用户发送系统通知。

## 项目结构

```text
jshERP/
├── jshERP-boot/                 # Spring Boot 后端
│   ├── docs/                     # PostgreSQL 初始化与租户初始化脚本
│   ├── src/main/java/            # Controller、Service、Entity、Mapper
│   ├── src/main/resources/       # application.yml 与 MyBatis XML
│   └── src/test/java/            # 单元测试与 API 回归测试
├── jshERP-web/                  # Vue 2 前端
│   ├── src/api/                  # API 调用
│   ├── src/config/               # 基础路由配置
│   ├── src/views/                # 页面组件
│   └── src/store/                # Vuex 状态与动态菜单
├── docker-compose.yml            # PostgreSQL、Redis、后端、前端
├── Dockerfile                    # 后端镜像构建文件
└── README.md
```

## 快速启动（Docker Compose）

这是推荐的本地体验方式。首次启动会初始化一个 PostgreSQL 数据卷，并自动执行：

- `jshERP-boot/docs/jsh_erp_pg.sql`：基础表、菜单、字典与平台配置；
- `jshERP-boot/docs/02_initial_tenant.sql`：首个租户及租户管理员。

```bash
docker compose up -d --build
docker compose ps
```

默认访问地址：

| 服务 | 地址 |
| --- | --- |
| 前端 | `http://localhost:${WEB_PORT}`（默认端口 `80`） |
| 后端 API | `http://localhost:${APP_PORT}/jshERP-boot`（默认端口 `9999`） |
| API 文档 | `http://localhost:${APP_PORT}/jshERP-boot/doc.html` |
| PostgreSQL | `localhost:${DB_PORT}`（默认端口 `5432`） |
| Redis | `localhost:${REDIS_PORT}`（默认端口 `6379`） |

Compose 可通过根目录 `.env` 覆盖端口、数据库用户和密码。默认数据库密码、Redis 密码仅适合本地开发，部署前务必修改。

> `jsh_erp_pg.sql` 会先删除并重建表，仅用于新库初始化。已有业务库升级时不要直接执行该文件；应按版本逐项执行新增表/字段迁移，并先完成备份。

## 自动备份与恢复

Docker Compose 会启动独立的 `backup` 服务。默认按 `Asia/Shanghai` 时区每天 `02:30` 执行一次全量逻辑备份，并保留最近 30 天的备份目录。

- 数据库：`pg_dump` 自定义格式，文件为 `database.dump`；
- 文件：上传附件和已安装插件，文件为 `files.tar.gz`；
- 校验：每份备份都有 `SHA256SUMS` 和 `MANIFEST.txt`；
- 位置：项目根目录 `backups/<YYYYMMDD_HHMMSS>/`。该目录不提交到 Git，应定期复制到另一块磁盘、NAS 或对象存储。

可在根目录 `.env` 中调整：

```dotenv
BACKUP_TIME=02:30
BACKUP_RETENTION_DAYS=30
TZ=Asia/Shanghai
```

需要立刻创建一份备份时执行：

```bash
docker compose run --rm backup /usr/local/bin/backup.sh
```

恢复前必须先停止业务写入并确认目标数据库允许被覆盖。数据库归档可用以下命令恢复（将目录名替换为实际备份目录）：

```bash
docker compose run --rm backup sh -c 'pg_restore --host=postgres --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" --clean --if-exists /backup/20260101_023000/database.dump'
```

附件和插件恢复应在停止 `app` 服务后进行，并以同一份备份中的 `files.tar.gz` 覆盖对应 Docker 数据卷；恢复完成后再启动 `app`。建议至少每月在非生产环境做一次完整恢复演练。

## 本地开发

### 1. 准备依赖

- JDK 17
- Maven 3.9+
- PostgreSQL 15+
- Redis 6.2+
- Node.js 20.17+

默认后端连接配置在 `jshERP-boot/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/jsh_erp
    username: postgres
    password: Postgres@123
  redis:
    host: 127.0.0.1
    port: 6379
    password: 1234abcd
```

请按本地环境修改，密码不要提交到代码仓库。

### 2. 初始化数据库

```bash
createdb -U postgres jsh_erp
psql -U postgres -d jsh_erp -f jshERP-boot/docs/jsh_erp_pg.sql
psql -U postgres -d jsh_erp -f jshERP-boot/docs/02_initial_tenant.sql
```

### 3. 启动后端

```bash
cd jshERP-boot
mvn clean package -DskipTests
java -jar target/jshERP.jar
```

后端默认端口为 `9999`，上下文路径为 `/jshERP-boot`。

### 4. 启动前端

```bash
cd jshERP-web
npm install
npm run serve
```

开发服务器默认运行在 http://localhost:3000，并将 `/jshERP-boot` 请求代理到 `http://localhost:9999`。

## 登录与权限

初始化脚本提供平台管理员和首个租户管理员。具体账号由 `02_initial_tenant.sql` 决定；首次部署前应检查并修改该脚本中的租户名称、登录名和初始密码。

菜单和按钮权限分别配置。转单类操作除了来源单据的权限外，还需要目标单据的新增权限。修改角色权限后应重新登录，使前端权限缓存刷新。

## 库存预警通知

库存安全阈值维护在商品的仓库库存设置中：

- 当前库存低于最低安全库存时触发 LOW 预警；
- 当前库存高于最高安全库存时触发 HIGH 预警；
- 阈值为空或为 `0` 时对应方向不启用；
- 同一租户、商品、仓库和预警方向只在“正常 → 越限”时通知一次；恢复正常后再次越限会重新通知。

自动通知仅在系统开启“强审核”时生效，因为此时库存随审核/反审核改变。通知对象必须拥有“库存预警报表”菜单权限；若启用仓库权限，还必须拥有对应仓库权限。顶部铃铛首次载入只显示未读数量，后续每 20 秒轮询一次并对新消息弹窗提示。

## 测试与构建

```bash
# 编译主代码和测试代码
cd jshERP-boot
mvn test-compile

# 执行指定单元测试
mvn -Dtest=StockWarningServiceTest test

# 执行全部测试（部分 API 回归测试要求本地后端已启动）
mvn test

# 构建前端生产包
cd ../jshERP-web
npm run build
```

后端 `mvn package` 同时生成可执行 JAR 和 `dist/jshERP-bin.zip` 部署包。

## 常见问题

### Docker 启动后数据库没有重新初始化

PostgreSQL 初始化脚本只会在数据卷首次创建时执行。若要重新创建本地测试数据，应先确认不需要保留数据，再停止服务并删除对应 Docker 数据卷。

### 修改了菜单或按钮权限但页面没有变化

退出后重新登录。前端会按用户缓存菜单和按钮权限。

### API 回归测试提示无法连接

`ApiTestBase` 默认请求 `http://localhost:9999/jshERP-boot`。先启动后端与 PostgreSQL、Redis，再执行 API 测试。

## License

本项目采用 [Apache License 2.0](LICENSE)。
