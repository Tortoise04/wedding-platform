# 缘集 · 婚礼策划一站式服务平台

> 基于 Spring Boot + Vue3 + MySQL 的婚礼策划一站式服务平台，**三端一体**（用户 / 商家 / 管理员），共用同一套账号体系。
> 毕业论文选题：《基于 Spring Boot 的婚礼策划一站式服务平台的设计与实现》。

## 一、技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.3.13 · Spring Data JPA(Hibernate) · MySQL 8.0 · JWT(jjwt) |
| 前端 | Vue3 · Vite · Vue Router · Element Plus · axios |
| 编译/运行 | Java 17 编译目标（本机 JDK 23 运行）· Node 24 · npm |

## 二、目录结构

```
wedding-platform/
├── pom.xml                     # 后端 Maven 工程（com.wedding）
├── src/main/java/com/wedding/
│   ├── entity/                 # 实体（18 张表一一对应）
│   ├── repository/             # 数据访问（含 Specification 检索、聚合统计）
│   ├── service/ + service/impl # 业务逻辑
│   ├── controller/             # 三端 + 门户 + 管理端接口
│   ├── dto/                    # 请求/响应对象
│   ├── enums/                  # 订单/审核/争议/处罚等状态枚举
│   ├── spec/                   # 多维度检索的条件工厂
│   ├── annotation/             # 四级权限注解（@RequireLogin/Merchant/Admin）
│   ├── interceptor/            # JWT 拦截器（读标签/取 token/验签/分级 401/403）
│   ├── config/                 # WebMvc 装配
│   ├── common/  exception/  util/
├── src/main/resources/application.yml   # 数据源/端口(18888)/JPA 配置
├── sql/
│   ├── schema.sql              # 建表脚本（18 张表，带中文注释）
│   ├── dev_seed.sql            # 开发种子（3 演示账号 + 6 分类 + 1 演示店铺）
│   └── dev_testdata_service.sql# 10 万行服务测试数据（索引实验用）
└── frontend/                   # Vue3 前端（Vite 工程）
    ├── src/views/              # portal / user / merchant / admin 四端页面
    ├── src/router/             # 路由 + 角色守卫
    ├── src/api/                # axios 封装（自动带 token、解包 Result）
    └── src/store/              # 登录态（token/角色/用户id）
```

## 三、快速启动

### 1. 数据库

MySQL 8.0，库名 `wedding`。首次建库后执行（会先 DROP 同名表，**只在开发环境用**）：

```bash
mysql -u root -p --default-character-set=utf8mb4 -e "source sql/schema.sql"
mysql -u root -p --default-character-set=utf8mb4 -e "source sql/dev_seed.sql"
```

### 2. 配置（数据库口令与 JWT 密钥，不进仓库）

仓库里**没有任何真实凭据**。`application.yml` 只放占位与默认值，真实值走下面两种方式之一：

**方式 A · 环境变量**（优先级最高）

```powershell
$env:DB_URL      = "jdbc:mysql://localhost:3306/wedding?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<你的 MySQL 口令>"
$env:JWT_SECRET  = "<至少 32 字节的随机串>"
```

**方式 B · 本地私有配置文件**

```powershell
copy src\main\resources\application-local.yml.example src\main\resources\application-local.yml
# 然后填入真实口令与密钥；该文件已被 .gitignore 排除
```

> `JWT_SECRET` 留空时每次启动随机生成一把：能跑，但**重启后所有 token 失效**，需重新登录。

### 3. 后端（端口 18888）

命令行需先指定 JDK 17+（默认 JDK 8 会报错）：

```powershell
$env:JAVA_HOME = "<你的 JDK 17+ 安装路径>"
mvn spring-boot:run
```

启动成功后访问 `http://localhost:18888`（推荐直接用 IDEA 运行）。

### 4. 前端（端口 5173）

```powershell
cd frontend
npm install
npm run dev
```

浏览器打开 `http://localhost:5173`。前端通过 Vite 代理把 `/api` 转发到后端 18888，无跨域问题。

## 四、演示账号（密码统一 `123456`）

| 账号 | 角色 | 登录后进入 |
|---|---|---|
| `admin` | 管理员 | 管理端（数据看板 / 审核 / 处罚 / 争议仲裁 / 公告资讯） |
| `merchant` | 商家 | 商家端（经营概览 / 服务管理 / 订单履约 / 评价回复 / 咨询 / 店铺资料） |
| `user` | 用户 | 用户端（订单 / 收藏 / 浏览 / 咨询 / 争议 / 资料） |

> 游客可直接浏览门户（服务检索 / 服务详情 / 商家 / 公告资讯），无需登录。

## 五、核心能力

- **四级权限**：游客 → 登录 → 商家 → 管理员，由 `JwtInterceptor` 读方法注解后分级拦 401/403（等级制：管理员也能进商家接口）。
- **多维度检索**：关键词 / 分类 / 价格区间 / 商家筛选 / 排序 / 分页，Specification 动态拼条件，10 万行下走索引。
- **订单状态机**：PENDING → CONFIRMED → IN_PROGRESS → COMPLETED（分支 CANCELLED），流转留痕 `t_order_status_log`，多表写同一事务。
- **争议仲裁**：发起 → 受理 → 举证 → 裁决（退款 / 关单 / 处罚 / 驳回），与订单状态机解耦。
- **内容治理**：服务/评价/商家三处审核闸门；公告资讯双视角 + 阅读量原子自增；违规处罚三分流（警告/服务下架/封禁）。
- **数据统计**：管理端平台统计 + 商家端店铺经营数据，聚合查询全部交给数据库。

## 六、接口与文档

- 接口清单、数据模型、需求与测试记录属于作者本地毕设文档，未随代码开源；
  仓库内以 `sql/schema.sql`（18 张表带中文注释）与各 `controller` 的方法注释为准。
- 后端启动后可直接看 `show-sql` 输出与 `/api` 接口行为，无需额外文档。

## 七、验收测试

毕设阶段配套了一组接口级验收脚本（Python，仅用标准库 `urllib`），属作者本地资料、未随仓库提供。
自己验收时开一个独立实例即可，与主实例隔离：

```powershell
$env:JAVA_HOME = "<你的 JDK 17+ 安装路径>"
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=18889"
```

## 八、已知收尾项

1. JWT 无状态，改角色/封禁不即时失效，待与"封禁"一并实现（拦截器验签后核一次库）。
2. `EvaluationServiceImpl` 第 89 行残留引导注释待清理。
3. 页面上的服务/商家排序在 10 万行量级下已走索引，但深分页（offset 很大）仍是线性扫描，可改游标分页。
