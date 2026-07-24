# jshERP 认证与权限体系 — 完整流程图

## 一、登录认证流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                         前端登录页面                                  │
│                                                                     │
│   ┌──────────────────┐              ┌──────────────────────┐        │
│   │  /user/login     │              │ /user/platform-login │        │
│   │  租户登录         │              │ 平台管理员登录        │        │
│   │                  │              │                      │        │
│   │  [multi 模式]    │              │ 用户名               │        │
│   │  公司编码(必填)   │              │ 密码                 │        │
│   │  用户名           │              │ 验证码               │        │
│   │  密码             │              └──────────┬───────────┘        │
│   │  验证码           │                         │                    │
│   │                  │                         │                    │
│   │  [single 模式]   │                         │                    │
│   │  用户名           │                         │                    │
│   │  密码             │                         │                    │
│   │  验证码           │                         │                    │
│   └────────┬─────────┘                         │                    │
│            │                                    │                    │
└────────────┼────────────────────────────────────┼────────────────────┘
             │                                    │
             ▼                                    ▼
┌────────────────────────────┐   ┌────────────────────────────────┐
│  POST /user/login          │   │  POST /platform/login          │
│  UserController.login()    │   │  PlatformAuthController.login()│
└────────────┬───────────────┘   └──────────────┬─────────────────┘
             │                                    │
             ▼                                    ▼
```

## 二、租户登录详细流程 (`/user/login`)

```
UserService.login(companyCode, loginName, password, request)
│
├── 1. resolveTenantCompanyCode(companyCode)
│   │
│   ├── single 模式
│   │   └── return defaultTenantCode（忽略前端传入的 companyCode）
│   │
│   └── multi 模式
│       ├── companyCode 为空 → 抛异常 "请输入公司编码"
│       └── return companyCode
│
├── 2. tenantService.getTenantByTenantCode(effectiveCompanyCode)
│   │
│   ├── 租户不存在 → msgTip: "company not exist"
│   ├── 租户已禁用 → msgTip: "tenant is black"
│   ├── 租户已过期 → msgTip: "tenant is expire"
│   └── 租户正常 → tenantId
│
├── 3. authenticateTenantUser(tenantId, loginName, password)
│   │
│   ├── 查询: WHERE tenant_id = ? AND login_name = ? AND delete_flag != '1'
│   ├── 用户不存在 → 抛 BusinessRunTimeException "用户不存在"
│   ├── 多条记录 → 抛 BusinessRunTimeException "用户数据异常"
│   ├── 用户已禁用 → 抛 BusinessRunTimeException "用户被禁用"
│   ├── 密码错误 → 抛 BusinessRunTimeException "密码错误"
│   ├── 密码需升级 → 静默升级密码哈希
│   └── 认证通过 → return User 对象
│
├── 4. 生成 Token
│   └── token = UUID + "_" + tenantId
│
├── 5. Redis 存储会话
│   ├── token → userId
│   ├── token → loginType = "TENANT"
│   ├── token → tenantId = tenantId
│   └── token → clientIp
│
└── 6. 返回
    ├── token
    ├── user（密码已清空）
    └── pwdSimple（密码是否过于简单）
```

## 三、平台管理员登录详细流程 (`/platform/login`)

```
UserService.platformLogin(loginName, password, request)
│
├── 1. 查询平台用户
│   └── WHERE login_name = ? AND tenant_id IS NULL AND delete_flag != '1'
│   │
│   ├── 用户不存在 → 抛异常 "用户不存在"
│   ├── 多条记录 → 抛异常 "平台账号数据异常"
│   └── 找到唯一用户
│
├── 2. 校验
│   ├── 用户已禁用 → 抛异常 "用户被禁用"
│   ├── 密码错误 → 抛异常 "密码错误"
│   └── 校验通过
│
├── 3. 生成 Token
│   └── token = UUID（无 tenantId 后缀）
│
├── 4. Redis 存储会话
│   ├── token → userId
│   ├── token → loginType = "PLATFORM"
│   └── token → clientIp
│
└── 5. 返回
    ├── token
    ├── user（密码已清空）
    └── pwdSimple
```

## 四、请求认证流程（每个 API 请求）

```
HTTP 请求（Header: X-Access-Token）
│
├── 1. LogCostFilter（Servlet Filter）
│   │
│   ├── 无 token → 放行（由后续拦截器处理未登录）
│   ├── Redis 查 userId → 不存在 → 401
│   ├── 加载 User → 不存在或已删除 → 401
│   ├── Token 中 tenantId ≠ 用户实际 tenantId → 401（防篡改）
│   ├── 非平台用户 → 检查租户状态（禁用/过期 → 401）
│   └── 通过 → 继续
│
├── 2. TenantFeatureInterceptor（Handler Interceptor）
│   │
│   ├── 无 @RequireTenantFeature 注解 → 放行
│   ├── loginType=PLATFORM → 放行（平台管理员跳过）
│   ├── 加载租户已开通的功能模块编码集合
│   ├── 检查注解要求的功能编码是否在集合中
│   ├── 不在 → 403 "该功能模块未开通"
│   └── 在 → 放行
│
├── 3. PlatformAdminInterceptor（Handler Interceptor）
│   │
│   ├── 无 @PlatformAdminOnly 注解（方法级+类级） → 放行
│   ├── Redis 查 loginType ≠ "PLATFORM" → 403
│   ├── isPlatformSuperAdmin(currentUser) = false → 403
│   └── 通过 → 放行
│
└── 4. Controller 方法执行
```

## 五、MyBatis 租户数据隔离

```
每个 SQL 执行前（MyBatis-Plus TenantLineInnerInterceptor）
│
├── 从 X-Access-Token 解析 tenantId
│   └── Tools.getTenantIdByToken(token) → 按 "_" 分割取第二段
│
├── tenantId = 0（平台管理员 token 无后缀）
│   └── 跳过所有表的租户过滤 → 查看全部数据
│
├── 表在 IGNORE_TABLES 中（jsh_function, jsh_tenant, jsh_feature 等）
│   └── 跳过该表的租户过滤 → 全局共享数据
│
└── 其他情况
    └── 自动追加 WHERE tenant_id = {tenantId}
```

## 六、菜单权限过滤流程

```
GET /function/findMenuByPNumber
│
├── 1. 获取当前用户信息
│   └── User userInfo = getCurrentUser()
│
├── 2. 判断是否平台管理员
│   └── isPlatformAdmin = isPlatformSuperAdmin(userInfo)
│       = (tenantId == null && isystem == 1)
│
├── 3. 获取租户已开通的功能模块
│   └── Set<String> tenantFeatureCodes = getTenantFeatureCodes(tenantId)
│       （平台管理员时 tenantId=null，返回空集合）
│
├── 4. 获取当前用户角色对应的功能列表
│   ├── 平台管理员 → 跳过，返回空列表
│   └── 租户用户 → UserRole → RoleFunctions → funIdMap
│
├── 5. getMenuByFunction() 递归构建菜单树
│   │
│   │  对每个 Function 节点：
│   │
│   ├── [平台管理员过滤] 只保留 PLATFORM + SYSTEM 菜单
│   │   if (isPlatformAdmin) {
│   │       if (featureCode != "PLATFORM" && featureCode != "SYSTEM") {
│   │           continue;  // 跳过业务菜单
│   │       }
│   │   }
│   │
│   ├── [租户菜单入口过滤]
│   │   if (isPlatformAdmin) → 通过
│   │   if (用户是租户admin, id==tenantId) → 通过
│   │   if (funIdMap 包含该菜单id) → 通过
│   │   else → 跳过
│   │
│   ├── [TenantFeature 过滤]
│   │   if (!isPlatformAdmin && featureCode 非空) {
│   │       if (tenantFeatureCodes 不包含 featureCode) {
│   │           continue;  // 租户未开通该功能模块
│   │       }
│   │   }
│   │
│   ├── [多级审核过滤] approvalFlag="0" 时跳过 /workflow
│   │
│   └── 递归处理子菜单
│
└── 6. 返回菜单树 JSON
```

## 七、功能模块体系

```
jsh_feature（功能模块定义表）
├── PLATFORM  平台管理    ← 仅平台管理员，不分配给任何租户
├── SYSTEM    系统管理    ← 租户基础能力，默认分配，建议不可取消
├── STOCK     库存管理    ← 按需分配
├── PURCHASE  采购管理    ← 按需分配
├── SALE      销售管理    ← 按需分配
└── FINANCE   财务管理    ← 按需分配

jsh_function（菜单表）→ feature_code 字段关联
├── 系统管理 (SYSTEM)
│   ├── 用户管理 (SYSTEM)
│   ├── 角色管理 (SYSTEM)
│   ├── 日志管理 (SYSTEM)
│   ├── 租户管理 (PLATFORM)  ← 只有平台管理员可见
│   ├── 平台配置 (PLATFORM)
│   ├── 系统配置 (PLATFORM)
│   └── 菜单管理 (PLATFORM)
├── 商品管理 (STOCK)
├── 采购管理 (PURCHASE)
├── 销售管理 (SALE)
├── 财务管理 (FINANCE)
└── 报表查询 (STOCK)

jsh_tenant_feature（租户功能授权表）
├── 租户 A001: SYSTEM ✓, STOCK ✓, PURCHASE ✓, SALE ✓, FINANCE ✓
├── 租户 B001: SYSTEM ✓, STOCK ✓, PURCHASE ✓, SALE ✓
└── 租户 C001: SYSTEM ✓, STOCK ✓, PURCHASE ✓

租户可用菜单 = TenantFeature ∩ RoleFunctions
平台管理员菜单 = PLATFORM + SYSTEM
```

## 八、部署模式配置

```
application.yml
├── tenant.login-mode: multi | single
├── tenant.default-tenant-code: ""
│
├── [multi 模式]
│   ├── 登录页显示：公司编码 + 用户名 + 密码
│   ├── /user/login: companyCode 必填
│   ├── 支持多公司共用一套系统
│   └── 示例: A001 + admin + 123456
│
└── [single 模式]
    ├── 登录页显示：用户名 + 密码（隐藏公司编码）
    ├── /user/login: 后端自动使用 defaultTenantCode
    ├── 私有化部署，单公司使用
    └── 示例: admin + 123456 → 自动使用 A001 租户

平台管理员登录：两种模式都通过 /platform/login
```

## 九、Token 结构

```
租户用户 Token:
┌──────────────────────────────────┬──────────────┐
│          UUID (32字符)           │  _tenantId   │
│  例: a1b2c3d4e5f6...             │  _1001       │
└──────────────────────────────────┴──────────────┘
  Tools.getTenantIdByToken() → 按 "_" 分割 → 1001

平台管理员 Token:
┌──────────────────────────────────┐
│          UUID (32字符)           │
│  例: x9y8z7w6v5u4...             │
└──────────────────────────────────┘
  Tools.getTenantIdByToken() → 无 "_" → 返回 0L
  → MyBatis 租户过滤全部跳过
```

## 十、数据流向总结

```
用户输入
  │
  ▼
前端 Login.vue / PlatformLogin.vue
  │
  ▼
后端 Controller（/user/login 或 /platform/login）
  │
  ├── 解析 companyCode → tenantId
  ├── 查询 User（按 tenantId + loginName）
  ├── 校验密码
  ├── 生成 Token → 存入 Redis（userId, loginType, tenantId）
  │
  ▼
后续请求携带 X-Access-Token
  │
  ├── LogCostFilter → 验证 token 有效性 + 租户状态
  ├── MyBatis 拦截器 → 自动追加 WHERE tenant_id = ?
  ├── TenantFeatureInterceptor → 检查功能模块开通
  ├── PlatformAdminInterceptor → 检查平台管理员身份
  │
  ▼
Controller 业务逻辑
  │
  ▼
Service → Mapper → SQL（已自动带 tenant_id 过滤）
  │
  ▼
返回数据（只包含当前租户的数据）
```
