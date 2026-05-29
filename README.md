# TicketX — 高并发票务系统

Spring Boot + Redis + RabbitMQ + React 全栈票务系统，支持高并发抢票、防超卖、订单管理、管理后台。

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端 | Java 17, Spring Boot 3.2, MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 (Lua 原子扣库存 / 令牌桶限流 / 启动自动预热) |
| 消息队列 | RabbitMQ 3 (异步下单削峰 / 延迟队列超时取消) |
| 安全 | BCrypt 密码加密 + JWT 无状态认证 + CORS |
| 前端 | React 18 + TypeScript + Vite + Ant Design 5（暗色舞台主题） |
| 部署 | Docker Compose（MySQL / Redis / RabbitMQ） |

## 架构

```
                 ┌──────────┐
                 │  React   │
                 │  Frontend│
                 └────┬─────┘
                      │ HTTP
                 ┌────▼─────┐
                 │  Spring  │
                 │  Boot    │
                 └──┬───┬───┘
        ┌───────────┘   └───────────┐
   ┌────▼────┐   ┌────▼────┐   ┌────▼────┐
   │  Redis  │   │ Rabbit  │   │  MySQL  │
   │ 缓存/锁 │   │   MQ    │   │  持久化  │
   └─────────┘   └─────────┘   └─────────┘
```

## 快速启动

```bash
# 1. 启动基础设施（Docker Desktop 需运行中）
docker compose up -d mysql redis rabbitmq

# 2. 编译并启动后端（主机运行，不走 Docker）
cd backend && mvn clean package -DskipTests
java -jar target/ticket-system-1.0.0-SNAPSHOT.jar

# 3. 启动前端
cd frontend && npm install && npm run dev

# 4. 打开浏览器
# 前端: http://localhost:5173
# 后端: http://localhost:8080
# RabbitMQ 管理台: http://localhost:15672 (guest/guest)
```

## 抢票流程

```
POST /api/ticket/buy (Bearer token)
  │
  ├─ ① Redis 令牌桶限流          → 超频拒绝
  ├─ ② Redis 售罄标记检查        → 已售罄拒绝
  ├─ ③ Redis Lua 原子扣库存      → 库存不足拒绝 + 标记售罄
  ├─ ④ MQ 发送下单消息           → 返回 orderNo
  │
  └─ MQ 消费者（异步）
       ├─ MySQL 写入订单（总价 = 真实票价 × 数量）
       ├─ MySQL remain_stock - 1（乐观锁 WHERE remain_stock > 0）
       ├─ 记录 inventory_log 库存流水审计
       └─ 发送延迟消息（30min TTL）→ 超时自动取消 + 回滚库存
```

## 防超卖三重保障

| 防线 | 机制 | 实现 |
|------|------|------|
| 第一道 | Redis Lua 原子操作 | `GET + DECRBY` 在 Redis 单线程内执行 |
| 第二道 | SQL 乐观锁 | `UPDATE ... WHERE remain_stock > 0` |
| 第三道 | 超时回滚 | RabbitMQ 延迟队列，30 分钟未支付自动归还 |

## API

### 用户

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/user/register` | 注册（BCrypt 加密） | 无 |
| POST | `/api/user/login` | 登录，返回 JWT | 无 |

### 演出

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/show/list` | 在售演出列表 |
| GET | `/api/show/{id}` | 演出详情 |
| GET | `/api/show/{id}/sessions` | 场次列表 |
| GET | `/api/show/session/{sid}/categories` | 票价 + 库存 |

### 抢票 ⭐

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/ticket/buy` | 抢票，返回 orderNo | Bearer token |

### 订单

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/order/list` | 我的订单（含票档名称） | Bearer token |
| GET | `/api/order/status?orderNo=` | 订单状态查询 | 无 |
| POST | `/api/order/pay?orderNo=` | 模拟支付 | 无 |

### 管理后台（需 role=1）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/dashboard` | 数据看板 |
| GET | `/api/admin/shows` | 所有演出（含场次/票档） |
| POST | `/api/admin/show` | 新建演出 |
| GET | `/api/admin/orders` | 所有订单 |

## 项目结构

```
ticket-system/
├── backend/                          # Spring Boot
│   ├── src/main/java/com/ticket/
│   │   ├── controller/               # REST API（User/Show/Ticket/Order/Admin）
│   │   ├── service/
│   │   │   ├── TicketService.java    # 抢票核心 ⭐
│   │   │   ├── RedisService.java     # Lua 脚本 / 限流 / 预热
│   │   │   ├── OrderService.java     # 订单 + 库存扣减 + 回滚
│   │   │   ├── ShowService.java      # 演出查询
│   │   │   ├── UserService.java      # 登录/注册（BCrypt）
│   │   │   └── StockPreheatRunner.java # 启动自动预热
│   │   ├── mq/OrderConsumer.java     # RabbitMQ 消费者
│   │   ├── config/                   # CORS / RabbitMQ / Security / Redis
│   │   ├── mapper/                   # MyBatis-Plus + 自定义 SQL
│   │   ├── model/entity/             # 6 张表实体
│   │   └── common/                   # JWT / Result / OrderStatus / GlobalException
│   └── pom.xml
├── frontend/                         # React + TypeScript + Vite
│   └── src/
│       ├── pages/
│       │   ├── LoginPage.tsx          # 登录/注册
│       │   ├── ShowListPage.tsx       # 演出列表
│       │   ├── ShowDetailPage.tsx     # 演出详情 + 抢票
│       │   ├── OrderListPage.tsx      # 订单列表 + 支付
│       │   └── AdminPage.tsx          # 管理后台
│       ├── api/index.ts              # Axios 封装
│       └── styles/theme.css          # 暗色舞台设计系统
├── docs/                             # 开发文档 + 测试报告（本地）
└── docker-compose.yml
```

## 压测数据

| 指标 | 数值 |
|------|------|
| 并发数 | 300 |
| 成功 | 118 |
| 限流拦截 | 182 |
| 超卖 | **0** |
| 数据一致性 | Redis = MySQL = 订单数 |

> 详见 `docs/01-压测报告.md`

## 配置

```yaml
ticket:
  order:
    pay-timeout-seconds: 1800   # 支付超时（秒），默认 30 分钟
  rate-limit:
    max-per-second: 3           # 每用户每秒最大抢票请求
    burst: 3                    # 令牌桶容量
```
