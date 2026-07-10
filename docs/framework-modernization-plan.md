# jshERP 全栈框架现代化升级计划

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
