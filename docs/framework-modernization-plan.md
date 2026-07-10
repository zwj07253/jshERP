# jshERP 全栈框架现代化升级计划

<<<<<<< Updated upstream
## 目标与结论

- 后端目标为 Spring Boot 4.1.x + Java 25 LTS。Spring Boot 4.1 支持 Java 17–26，但不从 Spring Boot 2.7 直接跨越到 4.1；按照官方建议，先升级至最新 Spring Boot 3.5.x，再升级至 4.1.x。[Spring Boot 要求](https://docs.spring.io/spring-boot/system-requirements.html) · [Spring Boot 4 迁移指南](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) · [Java 支持周期](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)
- 前端目标为 Vue 3.5.x、Vite 8.1.x、Vue Router 5.x、Pinia、Ant Design Vue 4.x；通过 Vue 3 migration build 和 Vue Router 4 过渡，保持现有界面、交互、路由地址和业务流程。[Vue migration build](https://v3-migration.vuejs.org/migration-build) · [Vue Router 迁移](https://router.vuejs.org/guide/migration/) · [Vite 8.1](https://vite.dev/blog/announcing-vite8-1)
- 彻底移除仅兼容 Spring Boot 2.x 的 `spring-brick`，将插件改造为独立服务；ERP 保留插件注册、授权和管理控制面，代理数据面与插件进程隔离。
- 保持现有 PostgreSQL 业务数据、约 365 个业务路由的 URL、参数、分页结构和关键 JSON 字段兼容；旧插件二进制、IE11 支持和未登录状态码属于明确管理的兼容例外。
- 最终交付顺序为“决策与测试基线 → 认证安全与数据库版本化 → 插件解耦 → Spring Boot 3.5 → Java 25 → Spring Boot 4.1 → Vue 渐进迁移 → 基础设施正式切换 → 模块化与可观测性收尾”。基础设施兼容验证从第一阶段开始，不等到最终切换时才执行。
- 每个阶段必须可构建、可启动、可验收并有独立回滚点；阶段验收未通过不得进入下一阶段。

## 当前架构基线

| 范围 | 当前状态 | 主要风险 |
| --- | --- | --- |
| 后端 | Spring Boot 2.7.18、Java 17、274 个 Java 文件、约 72 个文件含 `javax.*` 引用 | 直接跨到 Boot 4 会叠加 Jakarta、Spring 7、Jackson 3 和 JDK 变化 |
| 服务结构 | 单体按技术层分包，`DepotHeadService`、`DepotItemService`、`MaterialService` 等类体量较大；存在服务自注入和循环依赖 | `allow-circular-references`、`allow-bean-definition-overriding` 掩盖模块边界问题 |
| 数据访问 | 61 个 MyBatis Mapper XML，同时使用 PageHelper、MyBatis-Plus 分页插件和 JSqlParser | 升级时可能发生 SQL 重写、分页重复拦截和 JSqlParser 版本冲突 |
| 认证与租户 | 自定义 Servlet Filter、Redis 不透明 Token、客户端 MD5 密码；租户 ID 从 Token 文本后缀解析 | 认证、授权和租户上下文耦合；未登录返回 HTTP 500；密码及会话机制需要升级 |
| Redis | 登录 Session 与缓存共用 Redis，用户下线逻辑存在 `KEYS *` 扫描 | 数据量增加后可能阻塞 Redis；缓存与会话缺少明确命名空间和生命周期 |
| 前端 | Vue 2.7.16、242 个 Vue 组件、0 个 TypeScript 文件；大量 `slot-scope`、`scopedSlots`、事件总线和全局 API | Vue、组件库、路由和构建工具如果一次迁移，故障定位困难 |
| 前端依赖 | `vue-template-compiler` 2.6.10 与 Vue 2.7.16 不一致；Axios、jQuery、Vue CLI 等版本较旧 | 当前基线本身不完全确定，且依赖替换可能改变请求与页面行为 |
| 测试 | 已有 117 个 REST Assured 用例，最近报告为 116/117；测试直接访问本机运行中的应用 | 测试依赖共享环境和固定管理员账号，尚不能作为隔离、确定性的 CI 护栏 |
| 构建 | Docker 后端构建跳过测试，前端使用 `npm install --legacy-peer-deps`；lock 文件被忽略 | 构建不可完全复现，依赖变化可能未经评审进入制品 |
| 数据与部署 | Compose 使用 PostgreSQL 15、Redis 7，尚未引入 Flyway/Liquibase；存在默认密码 | 数据库增量变更和跨大版本回滚缺少机器可执行的版本链 |
| 可观测性 | 数据库和 Redis 有基础健康检查，应用缺少统一 Actuator、指标和链路追踪 | 升级后的性能退化、插件故障和慢 SQL 难以及时定位 |

## 升级原则与兼容边界

- 不重写库存、财务和业务模型，不在本轮拆分业务微服务；后端保持单体部署，但逐步形成可验证的模块化单体边界。
- 每次只改变一个主要维度。框架、JDK、前端运行时、构建工具、数据库大版本分别设置验收门，不在同一发布单元中同时切换。
- 数据库变更采用 expand/contract：先增加兼容字段、表或索引，再切换代码，确认无旧版本访问后才清理旧结构。
- 业务 API 默认向后兼容；升级前后以 OpenAPI、黄金 JSON、HTTP 状态、响应头、错误码、分页和日期/金额格式共同判定，不能只比较 OpenAPI 文档。
- 明确允许的破坏性变化如下：
  - 旧 `spring-brick` JAR 二进制格式停止支持，插件必须迁移为独立服务和清单 v2。
  - Vue 3 不支持 IE11。目标浏览器默认为 Chrome、Edge、Firefox、Safari 当前及前两个主要版本；若业务仍强制要求 IE11，前端 Vue 3 阶段必须暂停并重新决策。
  - 未登录响应最终从 HTTP 500 + `loginOut` 迁移为 HTTP 401 + 统一响应体；兼容期内前端同时识别两种响应，外部调用方需按发布说明迁移。
  - 旧 MD5 登录协议只保留固定迁移窗口，窗口结束后关闭，不长期双轨运行。
- 所有依赖使用正式稳定版，不采用 alpha、beta、RC 或实验 API；精确补丁版本写入 Maven 配置、前端 lock 文件和镜像 digest。

## 阶段 0：决策门与可重复基线

### 实施内容

- 确认并记录三项发布决策：目标浏览器矩阵、未登录 500 → 401 的兼容期限、PostgreSQL 迁移的 RPO/RTO 与最大停机窗口。
- 对插件现有公开路径逐一分类为“必须登录”“签名 Webhook”“完全公开”，禁止继续通过模糊 URL 前缀维护匿名白名单。
- 将 `vue-template-compiler` 与 Vue 统一到 2.7.16，先保证当前 Vue 2 构建基线稳定。
- 统一使用 npm，提交确定性的 `package-lock.json`，移除根目录和前端目录对 lock 文件的忽略；Docker 和 CI 统一使用 `npm ci`，禁止 `--legacy-peer-deps` 作为长期方案。
- 增加 Maven Wrapper、Maven Enforcer、Node `engines` 和 `.node-version`；Node 22 使用最新受支持补丁且不得低于 Vite 8 要求的 22.12。
- 建立 CI 流水线：后端编译、单元测试、集成测试、REST Assured、前端 lint/typecheck/build、Vitest、Playwright、契约对比、依赖与镜像扫描、Secret 扫描和 SBOM。
- Docker 制品构建可以不在镜像层重复执行全部测试，但只有同一提交的 CI 全绿后才能生成或发布镜像；不得把 `-DskipTests` 当作质量验收结果。
- 把现有 REST Assured 套件保留为黑盒兼容套件，同时新增可隔离的 Testcontainers 测试数据和自动清理机制；测试账号、端口和密码全部配置化。
- 先解决当前 116/117 中的未登录状态码决策，使升级基线达到 117/117；测试结果由 CI 自动生成，文档不再人工维护互相冲突的通过率。
- 固化 OpenAPI、365 个路由清单、核心表结构与行数、租户数据、库存余额、应收应付、账户余额、单据关联、关键报表结果和导入导出/PDF 黄金文件。
- 记录关键接口 p50/p95/p99、吞吐量、错误率、JVM、数据库连接池、Redis 和慢 SQL 基线。
- 对当前高风险旧依赖先做安全修复或风险接受记录，不等待框架升级阶段统一处理。

### 退出标准

- 当前 Spring Boot 2.7 + Java 17 + Vue 2 环境可由全新工作目录通过一条流水线确定性构建。
- P0/P1/P2 共 117 个既有回归用例全部通过，测试数据可重复创建和清理。
- OpenAPI、数据校验、视觉截图、性能指标和制品摘要均形成带版本号的基线工件。

## 阶段 1：认证安全、租户上下文与数据库版本化

### 实施内容

- 在 Spring Boot 2.7 上引入统一安全层，优先采用 Spring Security；用标准过滤链替代自定义认证 Filter 中分散的白名单、状态码和异常处理。
- 兼容保留 `X-Access-Token`，但 Token 只作为不透明会话标识；`userId`、`tenantId`、角色和权限必须从服务端验证后的会话/安全上下文获取，不再从 Token 字符串后缀推导租户。
- 抽取 `CurrentUserContext`、`TenantContext`、`AuthorizationService` 和 `AuditService`，降低各业务 Service 对 `UserService`、`LogService` 的循环依赖。
- 未登录响应兼容期同时支持旧 500 与新 401；新前端先兼容两者，随后后端切换为 401，最后在约定期限移除旧分支。
- 新登录协议通过 TLS 传输原始密码，由服务端使用 Argon2id、bcrypt 或符合组织要求的自适应算法存储；禁止继续使用快速 MD5 作为最终密码摘要。[OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- 密码迁移使用带版本标识的双验证窗口：保留旧摘要仅用于旧协议验证，新协议登录成功后写入新哈希；未在窗口内迁移的账号强制重置，窗口结束后删除旧摘要和旧协议。
- “记住密码”改为可撤销、可轮换、有限期限的长期登录令牌，浏览器不得持久化明文密码或可直接重放的密码等价值。
- 增加登录限速、失败锁定/退避、CSRF/CORS 策略、安全响应头、审计日志和敏感操作二次确认；明确管理员与普通租户权限边界。
- Redis Key 使用业务命名空间和版本前缀；用户到 Session 建立 Set/索引，删除 Session 不再执行 `KEYS *`，必要的批量遍历使用 `SCAN`。[Redis KEYS](https://redis.io/docs/latest/commands/keys/)
- 引入 Flyway 作为数据库结构版本源；以当前 PostgreSQL 结构建立只读校验过的 baseline，后续插件表、认证字段、索引和约束全部通过版本化迁移交付。
- Flyway 脚本禁止修改已发布版本；修复使用新的前向迁移。CI 同时验证空库初始化、现有库 baseline、连续升级和失败恢复。

### 退出标准

- 所有数据库访问都从已认证的 `TenantContext` 取得租户，跨租户负向测试通过。
- 新旧登录协议兼容测试、密码迁移、注销、并发会话、权限和审计测试通过；浏览器不再保存明文密码。
- Flyway 能从空库安装，也能在现有数据库上无损建立基线并执行增量迁移。
- `allow-circular-references` 尚可临时保留，但认证、租户和审计链路不再形成循环依赖。

## 阶段 2：在 Spring Boot 2.7 上替换插件机制

### 实施内容

- 新增持久化插件注册表，记录插件 ID、名称、版本、服务地址、健康检查路径、前端入口、能力列表、启用状态、配置版本和密钥引用；密钥本体不得进入数据库普通字段或 API 响应。
- 插件由 Docker、Kubernetes 或运维系统独立部署；ERP 不访问 Docker Socket，也不负责启动或销毁插件进程。
- ERP 作为控制面负责注册、注销、健康状态、授权策略和启停路由。数据面优先交给独立网关；若过渡期仍由 ERP 代理，必须封装在独立 `PluginProxy` 边界内，以便后续无业务改动地抽离。
- 保留 `/plugin/list`、`start`、`stop`、`uninstall`、`checkByPluginId` 等接口及旧响应外形；建立兼容 DTO，继续提供前端依赖的 `pluginDescriptor.pluginId` 等嵌套字段。“启动/停止”改为启用或禁用路由并在文档中说明语义变化。
- 保留 `/api/plugin/{pluginId}/**` 前缀。插件身份令牌采用短期签名格式，包含 `iss`、`aud`、`sub/userId`、`tenantId`、`roles/capabilities`、`requestId`、`iat`、`exp`、`jti` 和密钥版本；支持密钥轮换、时钟偏差和重放检测。
- 代理加入目标地址与端口白名单、DNS 重绑定防护、私网/环回地址策略、重定向校验、请求与响应头白名单、请求体限制、连接/读取/总超时、并发限制、熔断、限流和审计。
- 明确 multipart 上传、流式下载、SSE、WebSocket 和大响应的支持范围；只有幂等请求允许自动重试，禁止对创建单据等非幂等请求盲目重试。
- 插件前端使用独立 URL/iframe，配置 CSP `frame-src`、iframe `sandbox`、允许来源、版本化 `postMessage` 协议和一次性 Token 交换；主系统长期 Token 不暴露给 iframe。
- 定义插件清单 v2：`id`、`name`、`version`、`apiBaseUrl`、`healthPath`、`frontendUrl`、`capabilities`、`protocolVersion`。原上传接口可继续接收 multipart 清单包，但不再接受可执行 JAR。
- 使用模拟插件完成双轨验证；确认所有已部署插件已迁移或下线后，才能删除 `spring-brick`、旧配置类、插件目录挂载和类加载逻辑。

### 退出标准

- 插件注册、健康检查、授权代理、启停、超时、熔断、非法目标、签名轮换和插件宕机隔离测试通过。
- 主应用在插件慢响应、超大请求、连接耗尽和异常退出时仍保持健康。
- 旧插件管理页面无须改路由即可使用兼容 DTO；主进程中不再加载第三方插件代码。

## 阶段 3：后端分段升级至 Spring Boot 4.1

### 3A. Spring Boot 2.7 → 3.5，保持 Java 17

- 先升级到最新 Spring Boot 3.5.x 和兼容依赖，不同时切换 JDK；完成 Servlet、Annotation、Mail 等 `javax.* → jakarta.*` 迁移，`javax.imageio` 等仍属于 JDK 的包保持不变。
- 使用 Boot 3 对应的 MyBatis-Plus、SpringDoc 2.x、PostgreSQL 驱动、Fastjson2、Lombok、REST Assured 等正式版本，优先由 Spring Boot BOM 管理。
- 对 PageHelper、MyBatis-Plus Pagination 和 JSqlParser 做兼容性验证并只保留一套分页拦截路径；禁止两个分页插件同时处理同一查询。
- 为动态排序字段建立服务端白名单，审计 Mapper XML 中的 `${column}`、`${orderByClause}` 等文本替换，避免升级 SQL 解析器后扩大注入风险。
- 替换 Apache HttpClient 4、JXL、旧 Commons IO、iText 5 等过时组件；导入导出、邮件、PDF 和 HTTP 调用使用黄金文件验证格式不变。
- 移除重复的日志桥接和手工版本覆盖，验证日志字符集、堆栈和 MDC/requestId 保持可观测。
- 用职责拆分和构造器注入消除循环引用与 Bean 覆盖，删除 `allow-circular-references` 和 `allow-bean-definition-overriding`。

### 3B. Spring Boot 3.5 上切换 Java 25

- 使用 Maven Toolchains、Maven 3.9.x 和 Java 25 构建/运行；先保持业务代码与 Boot 版本不变，单独验证编译器、反射、日期时间、字符集、字体/PDF、文件系统和容器行为。
- Maven Compiler 使用明确的 `release`，开发机、CI、Docker builder 和 runtime 使用一致的 JDK 补丁版本与架构。
- 对启动时间、堆内存、GC、核心接口延迟和批量报表进行与 Java 17 的对比，异常时可以独立回退到 Java 17。

### 3C. Spring Boot 3.5 → 4.1，保持 Java 25

- 将父 BOM、Maven 插件和容器镜像升级到最新 Spring Boot 4.1.x；按 Boot 4 模块化要求更新 WebMVC、测试、Mail、Redis 等 Starter。
- MyBatis-Plus 使用 Boot 4 Starter，SpringDoc 使用与 Boot 4 匹配的 3.x；所有非 Boot BOM 管理的依赖必须有明确的 Boot 4/Spring 7 兼容证据或最小可复现实验。
- 适配 Spring Boot 4 配置属性、Spring Data Redis、Servlet 6.1、Tomcat 11、Spring Framework 7 和测试模块变化。
- 迁移至 Jackson 3，并对日期、时区、BigDecimal、null、枚举、Map 顺序和异常响应做黄金 JSON 对比；Jackson 2 兼容模块只允许作为有删除期限的临时过渡。
- 迁移完成后移除属性迁移器、Classic Starter、Jackson 2 兼容层和其他临时依赖。

### 后端阶段退出标准

- 每个子阶段均通过全部 API、数据、并发、插件和性能回归，且可以单独回退到上一个子阶段。
- `/jshERP-boot`、`X-Access-Token` 兼容窗口、分页结构、业务错误码和 JSON 字段满足兼容规则。
- 无循环依赖/Bean 覆盖开关，无高危依赖漏洞，无双重分页拦截和未说明的依赖版本覆盖。

## 阶段 4：前端渐进迁移至 Vue 3 + Vite 8

### 4A. Vue 2.7 兼容清理

- 在 Vue 2.7 上先迁移已支持的新插槽语法，清理 `slot-scope`、旧具名插槽、`.sync`、全局过滤器、事件总线、`Vue.prototype` 和依赖私有 VNode API 的代码。
- 为 API 客户端、权限、动态菜单、路由生成、缓存、多页签、表格/表单、上传、打印和预览建立单元及组件测试。
- 对常用 Ant Design Vue 表格、表单、弹窗和选择器建立少量适配封装，减少 242 个页面直接承受组件库 API 变化。

### 4B. Vue 3 migration build + Vue Router 4

- 使用 `@vue/compat` 和 `@vue/compiler-sfc` 迁移入口至 `createApp`，以组件为单位关闭兼容开关；所有 compat warning 必须被登记并逐步清零。
- Vue Router 先从 3.x 迁到 4.x，适配 `createRouter`、history、动态 `addRoute`、导航守卫和 catch-all 路由；保持现有 URL、菜单和多页签行为。
- Vuex 升至可在 Vue 3 运行的版本；Pinia 与 Vuex 可在迁移期间并存，但同一状态只能有一个事实来源。
- Ant Design Vue 升至 4.x，复刻主题、布局尺寸、表单校验、表格筛选/排序、弹窗焦点和键盘交互。

### 4C. Pinia、TypeScript 与依赖替换

- 以用户、权限、字典、应用布局等 Store 为单位从 Vuex 迁至 Pinia；完成一个模块的调用方迁移后再删除对应 Vuex 模块。[Pinia 迁移指南](https://pinia.vuejs.org/cookbook/migration-vuex.html)
- 底座、Store、路由、API 客户端和新代码使用 TypeScript；现有业务页面保留 Options API，避免一次性重写全部组件。
- Viser 替换为 ECharts/Vue-ECharts，CodeMirror 迁至 6，拖拽使用 Vue 3 版本，地区组件改为 Ant Design Cascader，裁剪使用 Cropper.js，剪贴板使用浏览器 API，移除 jQuery。
- `vue-ls` 替换为带类型、版本和过期策略的持久化适配层；Token、用户资料和偏好设置使用不同安全级别，禁止保存密码。

### 4D. 移除兼容层并切换 Vite 8.1 / Router 5

- compat warning 清零后移除 `@vue/compat`，再升级 Vue Router 5；Router 5 作为 Router 4 之后的独立小阶段验证。
- 用 Vite 8.1 替换 Vue CLI/Webpack，保持端口 3000、`@` 系列别名、`/jshERP-boot` 开发代理、生产 gzip/brotli 和静态资源路径。
- 明确 `build.target` 和目标浏览器；不得依赖 Vite 默认目标隐式改变支持范围。
- 增加 `vue-tsc`、ESLint、Vitest 和 Playwright，统一 `npm ci`；构建产物记录 source revision、依赖摘要和 Node 版本。

### 前端阶段退出标准

- 242 个现有组件对应的业务路由、菜单、权限、多页签、国际化、打印、预览、导入导出和 API 行为通过回归。
- 关键页面视觉差异在批准阈值内，键盘操作、焦点、表单校验和错误提示一致。
- 无 compat warning、Vuex 残留、jQuery、明文密码缓存或未说明的 Vue 2 依赖。

## 阶段 5：基础设施兼容验证与正式切换

### 兼容矩阵

以下组合从阶段 0 起持续执行，最终切换前必须全部有明确结论：

| 应用 | PostgreSQL / Redis | 目的 |
| --- | --- | --- |
| 旧应用 | PostgreSQL 15 / Redis 7 | 当前兼容基线 |
| 旧应用 | PostgreSQL 17 / Redis 8 | 隔离验证基础设施变化 |
| 新应用 | PostgreSQL 15 / Redis 7 | 隔离验证应用框架变化 |
| 新应用 | PostgreSQL 17 / Redis 8 | 最终生产组合 |

### PostgreSQL 15 → 17

- 不允许只修改镜像标签并复用 PostgreSQL 15 数据卷。使用新 PostgreSQL 17 数据卷，并根据停机窗口选择 dump/restore、`pg_upgrade` 或逻辑复制。[PostgreSQL 17 pg_upgrade](https://www.postgresql.org/docs/17/pgupgrade.html)
- 升级前执行 `pg_upgrade --check` 或等价预检，核对扩展、collation、编码、时区、角色、权限、sequence、large object、失效索引和磁盘空间。
- 在全量演练中记录备份、迁移、校验和回切耗时；升级后执行统计信息重建、必要的扩展升级与重建脚本，并比较关键 SQL 执行计划和慢查询。
- “保留旧卷”只在新库未接受写入前构成直接回滚。新库开始写入后，回退必须使用停写窗口、反向同步或从备份恢复，并明确可接受的数据损失 RPO。

### Redis 7 → 8

- 明确 Redis 中哪些是可重建缓存、登录 Session、验证码、锁和持久状态；不同用途使用独立命名空间，条件允许时使用独立实例/数据库。
- 优先以新的 Redis 8 数据卷启动；缓存重新生成，Session 在发布窗口统一失效并要求重新登录，避免把旧序列化数据作为长期兼容负担。
- 验证 Lua 锁脚本、TTL、过期通知、连接池、故障恢复、持久化配置和内存淘汰策略；不得将 Redis 锁作为数据库唯一一致性保障。

### 容器与运维

- 采用用户指定版本线：Java 25 LTS、Node 22、PostgreSQL 17、Redis 8；镜像锁定最新安全补丁及不可变 digest。
- Node 22 已进入 Maintenance LTS，并计划于 2027-04-30 结束支持；若本次仍按 Node 22 交付，必须在正式切换前复核，并建立最迟在 EOL 前迁移 Node 24 的后续项。[Node.js Release Schedule](https://github.com/nodejs/release#release-schedule)
- 更新 Docker Compose、Nginx 和部署清单；应用、Web、插件网关、数据库和 Redis 均提供 startup/readiness/liveness 状态。
- 增加优雅停机、连接池排空、CPU/内存/PID 限制、非 root 用户、只读根文件系统、临时目录和最小 Linux capabilities。
- 移除镜像、Compose 和 `application.yml` 中的默认密码；`.env` 只作为本地配置，提交无敏感值的 `.env.example`，生产使用 Secret 管理并执行密码轮换。

### 退出标准

- 全新安装、现有数据升级、备份恢复、容器重启、节点故障、插件故障和数据库回切演练通过。
- 数据核对、性能基线、错误率和资源用量满足验收阈值，且运维人员能按文档在规定 RTO 内执行恢复。

## 阶段 6：模块化单体与可观测性收尾

- 保持单体部署，按身份与租户、商品、采购、销售、零售、库存、财务、报表和插件平台建立 package-by-feature 边界。
- 将大型 Service 按命令、查询和跨域编排拆分；事务只由应用服务边界开启，避免每个 CRUD 方法零散声明事务。
- 使用 ArchUnit 或等价架构测试限制模块依赖，禁止业务模块直接依赖 Web、Redis、插件代理或其他模块内部 Mapper。
- 为库存审核、反审核、单据编号、账户余额和重复请求增加并发一致性、幂等和故障注入测试；优先使用数据库唯一约束、条件更新和行锁保证事实一致性。
- 引入 Spring Boot Actuator、Micrometer 和 OpenTelemetry/兼容追踪，统一 `requestId`、`traceId`、`userId`、`tenantId` 和 `pluginId` 日志上下文；敏感字段必须脱敏。
- 建立应用、JVM、HTTP、数据库连接池、慢 SQL、Redis、插件代理和业务核对指标的仪表盘与告警。
- 增加 Maven/前端依赖自动更新策略、SBOM、许可证检查、镜像扫描和制品签名；升级后的依赖维护进入常态流程。

## 公共接口与兼容规则

- 上下文路径保持 `/jshERP-boot`，认证头在迁移窗口继续支持 `X-Access-Token`；业务 API 路径、参数名、分页 `total/rows`、业务错误码和关键 JSON 字段默认不变。
- 未登录状态码按“前端先兼容 500/401 → 后端切换 401 → 移除旧 500 分支”的顺序迁移；发布说明必须标记对外部调用方的影响。
- 数据库业务表采用增量迁移，禁止重建生产库或通过手工 SQL 跳过 Flyway；删除字段和表必须晚于旧应用停止访问。
- 插件 API 前缀保持 `/api/plugin/{pluginId}/**`；插件只接收最小必要身份和能力，不传递主系统原始 Token。
- 插件管理接口保留旧 `code/data` 及 `pluginDescriptor` 外形；新增字段只增加不删除，旧 JAR 二进制不兼容属于已批准例外。
- 前端路由地址和动态菜单数据保持兼容；IE11 停止支持属于已批准例外，其他浏览器变化必须经过产品确认。

## 测试与验收矩阵

- 后端单元/组件测试：权限、租户上下文、业务校验、事务、序列、库存和财务计算。
- 后端集成测试：Testcontainers 覆盖 PostgreSQL 15/17、Redis 7/8 和 Flyway 空库/升级路径。
- 黑盒回归：现有 P0/P1/P2 REST Assured 全量执行，测试环境、账号和数据自动创建并隔离。
- 契约测试：比较 OpenAPI、路由清单、请求参数、HTTP 状态、响应头、业务错误码、分页和黄金 JSON。
- 前端测试：Vitest 覆盖 Store、权限、请求拦截器和共享组件；Playwright 覆盖登录、菜单、采购、销售、零售、库存、财务、报表、多租户、国际化和插件页面。
- 视觉测试：关键页面按目标浏览器截图对比，设置批准阈值并人工复核有意义的差异。
- 插件测试：健康检查、签名、租户透传、能力授权、禁用、超时、熔断、非法目标、密钥轮换、iframe 通信和插件宕机隔离。
- 数据测试：表与索引、行数、租户数据、库存余额、应收应付、账户余额、sequence 和单据关联完整性。
- 并发测试：重复审核/反审核、并发出入库、账户余额、单据编号、幂等请求和锁超时；测试结果必须与业务账实核对。
- 格式测试：Excel 导入导出、PDF、打印、图片、邮件和日期/金额格式与黄金文件对比。
- 性能测试：关键接口 p95 相对基线退化不超过 10%，同时约束 p99、错误率、吞吐量、CPU、内存、连接池和慢 SQL；插件故障不得拖垮主系统。
- 安全测试：认证绕过、跨租户访问、越权、SQL 注入、SSRF、文件上传、Token 重放、CSP/iframe、Secret 与依赖漏洞。
- 部署测试：全新安装、逐版本升级、备份恢复、蓝绿/滚动切换、优雅停机、容器重启和数据库回切。

## 发布与回滚

- 每个阶段生成独立 Git 标签、后端/前端镜像、SBOM、数据库迁移包、测试报告和回滚说明，不在同一不可分割提交中混合多个主要阶段。
- 数据库优先使用向前修复；应用回滚只能回到仍兼容当前数据库结构的版本。涉及 contract 清理的版本发布前必须确认旧应用已停止。
- 插件、认证新协议和前端新入口使用功能开关或灰度策略；先验证少量租户，再扩大范围。
- 回滚演练必须包含操作人、触发条件、步骤、预计耗时、数据损失边界和验证 SQL，不能只写“恢复旧卷”。
- 发布期间持续监控错误率、登录失败、跨租户拒绝、库存/财务核对、慢 SQL、Redis 和插件健康；触发阈值后按预案停止或回退。

## 假设与默认项

- Java 25、Node 22、PostgreSQL 17、Redis 8 是用户指定版本线；分别使用实施时最新受支持补丁。Node 22 的 EOL 风险按阶段 5 处理。
- 仓库不包含 workflow、stock-check、produce 等插件源码，因此本计划交付插件协议、代理、兼容层和模拟插件；真实插件由源码维护方另行迁移。
- 不调整现有库存和财务计算规则，不在升级期间引入业务微服务拆分；模块化重构不得改变外部业务行为。
- Spring Boot 3.5、Java 25、Spring Boot 4.1、Vue migration build、Vue Router 4/5 和 Vite 8 分别设置验收点，任一阶段失败都可以回到上一个已验收制品。
=======
## 总结

- 将后端升级至 Spring Boot 4.1.x + Java 25 LTS；Spring Boot 4.1 支持 Java 17–26，Java 25 是当前 LTS。[Spring Boot 要求](https://docs.spring.io/spring-boot/system-requirements.html) · [Java 支持周期](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)
- 将前端升级至 Vue 3.5.x、Vite 8.1.x、Vue Router 5.x、Pinia、Ant Design Vue 4.x；保留现有界面、交互、路由地址和业务流程。Vue 官方推荐 Vue 3 使用 Vite 和 Pinia。[Vue 迁移建议](https://v3-migration.vuejs.org/recommendations) · [Vite 8.1](https://vite.dev/blog/announcing-vite8-1)
- 彻底移除仅兼容 Spring Boot 2.x 的 `spring-brick`，改为独立插件服务注册、鉴权和代理机制。
- 保持现有 PostgreSQL 数据以及约 365 个业务路由的 URL、参数、响应结构向后兼容；插件二进制格式除外。
- 按“测试护栏 → 插件解耦 → 后端升级 → 前端迁移 → 基础设施升级”顺序交付，每阶段保持可构建、可启动、可回滚。

## 实施变更

### 1. 建立升级基线

- 固化当前 OpenAPI、数据库结构、核心表行数、库存与财务汇总结果，作为兼容性基准。
- 为现有登录、采购、销售、零售、调拨、库存、财务、租户和报表流程建立 API 契约及 Playwright 浏览器基线。
- 为关键页面保存截图基线，确保框架迁移不改变布局和主要交互。
- 建立后端构建、前端类型检查、单元测试、端到端测试和依赖安全扫描流水线。

### 2. 先在 Spring Boot 2.7 上替换插件机制

- 新增持久化插件注册表，记录插件 ID、名称、版本、服务地址、健康检查路径、前端入口、能力列表、启用状态和密钥引用。
- 插件由 Docker 或运维系统独立部署；ERP 只负责注册、注销、健康检查、启停路由、鉴权和请求转发，不接触 Docker Socket。
- 保留 `/plugin/list`、`start`、`stop`、`uninstall`、`checkByPluginId` 等现有管理接口及响应外形；“启动/停止”改为启用或禁用代理路由。
- 保留 `/api/plugin/{pluginId}/**` 访问前缀，通过短期签名令牌传递用户、租户、角色和请求 ID，并加入目标地址白名单、超时、熔断、请求体限制及 SSRF 防护。
- 插件前端采用独立 URL/iframe 接入，避免与主应用 Vue 版本和依赖树耦合。
- 定义插件清单 v2：`id`、`name`、`version`、`apiBaseUrl`、`healthPath`、`frontendUrl`、`capabilities`。原上传接口可继续接收 multipart 清单包，但旧 spring-brick JAR 不承诺二进制兼容。
- 使用模拟插件完成注册、健康检查、鉴权代理、启停、异常隔离测试后，删除 `spring-brick`、旧配置类和类加载逻辑。

### 3. 后端升级至 Spring Boot 4.1

- 将父 BOM、Maven 插件和容器镜像升级到 Spring Boot 4.1.x、Java 25、Maven 3.9.x，并锁定实施时的最新正式补丁版本。
- 完成 Servlet、Annotation、Mail 等 `javax.* → jakarta.*` 迁移；`javax.imageio` 等仍属于 JDK 的包保持不变。
- 升级 MyBatis-Plus、PageHelper、SpringDoc、PostgreSQL 驱动、Fastjson2、Lombok、REST Assured 等依赖；优先使用 Spring Boot BOM，移除重复或冲突的日志版本覆盖。
- 替换 Apache HttpClient 4、JXL、旧 Commons IO、iText 5 等过时组件；保持导入导出、邮件、PDF 和 HTTP 调用的现有业务格式。
- 消除 `allow-circular-references` 和 `allow-bean-definition-overriding` 依赖，通过拆分职责或构造器注入修复循环引用。
- 适配 Spring Boot 4 配置属性、Spring Data Redis、SpringDoc/OpenAPI、Jackson 3 及嵌入式 Tomcat 变化。
- 所有既有业务 API 保持上下文路径 `/jshERP-boot`、认证头、分页结构、错误码和 JSON 字段兼容。

### 4. 前端迁移至 Vue 3

- 用 Vite 8.1 替换 Vue CLI/Webpack，保持端口 3000、`@` 系列别名、`/jshERP-boot` 开发代理和生产 gzip/brotli 构建能力。
- 迁移入口至 `createApp`，Vue Router 迁至 5.x，Vuex 模块迁至 Pinia，`vue-ls` 替换为统一持久化适配层。
- 底座、Store、路由、API 客户端和新代码使用 TypeScript；现有页面先保留 Options API，避免一次性改写 242 个组件。
- 系统性迁移 Vue 2 语法：具名插槽、`slot-scope`、`.sync`、生命周期、全局过滤器、事件总线、`Vue.prototype`、动态组件及表格 `scopedSlots`。
- Ant Design Vue 升至 4.x 并复刻现有主题变量、布局尺寸和表单交互。
- 替换不兼容依赖：Viser 使用 ECharts/Vue-ECharts，CodeMirror 迁至 6，拖拽使用 Vue 3 版本，地区组件改为 Ant Design Cascader，裁剪使用 Cropper.js，剪贴板使用浏览器 API，移除 jQuery。
- 保留动态菜单、权限控制、多页签、国际化、打印、图片预览、导入导出和所有现有 API 调用行为。
- 增加 `vue-tsc`、ESLint、Vitest，并提交确定性的 `package-lock.json`，统一使用 `npm ci`。

### 5. 基础设施与数据升级

- 采用用户指定版本线：Java 25 LTS、Node 22 LTS、PostgreSQL 17、Redis 8；镜像锁定到实施时最新安全补丁及不可变 digest。
- PostgreSQL 15 → 17 使用完整备份、预检、演练恢复、正式迁移、数据校验的流程；升级后使用 PostgreSQL 17 当前补丁版，并保留旧卷作为回滚点。[PostgreSQL 支持版本](https://www.postgresql.org/support/versioning/)
- Redis 7 → 8 前验证序列化兼容；缓存可重建，现有登录 Session 允许在发布窗口失效并要求重新登录。
- 更新 Docker Compose、健康检查、资源限制和 Nginx 配置；应用、插件代理、数据库、Redis 均提供明确健康状态。
- 移除镜像及 Compose 中的默认密码，将仓库内 `.env` 改为本地配置，提交无敏感值的 `.env.example`，发布时轮换数据库和 Redis 密码。
- 更新 README、开发启动、生产部署、数据库迁移、插件 v2 接入及回滚文档。

## 公共接口与兼容规则

- 业务 API、数据库业务表、上下文路径和前端路由默认全部向后兼容；新增插件注册表只做增量迁移。
- 插件 API 前缀保持 `/api/plugin/{pluginId}/**`，插件收到的身份令牌包含 `userId`、`tenantId`、`roles`、`requestId` 和过期时间。
- 插件管理接口继续使用统一 `code/data` 响应；新增显式注册和健康状态字段时只增加字段，不删除旧字段。
- 唯一允许的破坏性变化是旧 spring-brick JAR 二进制格式；旧插件需重构为独立服务并提供 v2 清单。
- 所有依赖使用正式稳定版，不采用 alpha、beta、RC 或实验 API；精确补丁版本写入 Maven 配置、package lock 和镜像 digest。

## 测试与验收

- 后端：编译测试、单元测试、Testcontainers PostgreSQL 17/Redis 8 集成测试，以及现有 P0/P1/P2 REST Assured 全量回归。
- 契约：升级前后对比 OpenAPI、请求参数、HTTP 状态、业务错误码、分页及关键 JSON 响应。
- 前端：Vitest 覆盖 Store、权限、请求拦截器和共享组件；Playwright 覆盖登录、菜单、采购、销售、库存、财务、报表、多租户、国际化及插件页面。
- 插件：验证健康检查、身份与租户透传、禁用路由、超时、熔断、非法目标地址和插件宕机不拖垮主系统。
- 数据：迁移前后核对表数量、行数、租户数据、库存余额、应收应付、账户余额及单据关联完整性。
- 部署：验证全新安装、现有数据升级、备份恢复、容器重启、插件故障隔离和数据库回滚。
- 验收标准：所有既有 P0/P1/P2 测试通过，核心页面视觉与业务行为一致，无业务 API 契约破坏，无高危依赖漏洞，关键接口 p95 延迟相对基线退化不超过 10%。

## 假设与默认项

- Node 22、PostgreSQL 17 是用户明确指定的版本线，即使它们不是当前最大主版本；分别使用各自最新受支持补丁。
- 仓库不包含 workflow、stock-check、produce 等插件源码，因此本计划交付插件协议、代理与模拟插件；真实插件需由其源码维护方另行迁移。
- 不重写业务模型、不调整现有库存和财务计算规则、不引入微服务拆分；只有插件从主进程中隔离。
- 每个阶段单独验收并保留回滚点，阶段未通过不得继续升级下一层。
>>>>>>> Stashed changes
