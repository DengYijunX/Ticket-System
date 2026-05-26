# TicketX - 高并发票务系统

Spring Boot + Redis + RabbitMQ + React 全栈票务系统，支持高并发抢票场景。

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端 | Java 17, Spring Boot 3.2, MyBatis-Plus 3.5 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 (Lua 原子扣库存 / 令牌桶限流) |
| 消息队列 | RabbitMQ 3 (异步下单 / 延迟队列超时取消) |
| 安全 | BCrypt 密码加密 + JWT 无状态认证 |
| 前端 | React 18 + TypeScript + Vite + Ant Design 5 |
| 部署 | Docker Compose |

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

### 前提
- Docker Desktop（运行中）
- 本地 JAR 已编译：`cd backend && mvn clean package -DskipTests`

```bash
# 1. 启动基础设施 + 后端
docker compose up -d

# 2. 启动前端
cd frontend && npm install && npm run dev

# 3. 打开浏览器
# http://localhost:5173
```

### 一键全部启动（包含前端）
```bash
cd frontend && npm install && npm run build
# 然后将 frontend/dist 放到 nginx 或直接 vite preview
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
  └─ MQ 消费者
       ├─ MySQL 写入订单
       ├─ MySQL remain_stock - 1（乐观锁 WHERE remain_stock > 0）
       ├─ 记录 inventory_log 审计
       └─ 发送延迟消息（30min TTL）→ 超时自动取消 + 回滚库存
```

## 防超卖三重保障

| 防线 | 机制 | 实现 |
|------|------|------|
| 第一道 | Redis Lua 原子操作 | `GET + DECRBY` 在 Redis 单线程内执行 |
| 第二道 | SQL 乐观锁 | `UPDATE ... WHERE remain_stock > 0` |
| 第三道 | 超时回滚 | MQ 延迟队列，30 分钟未支付自动归还 |

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/register` | 注册 |
| POST | `/api/user/login` | 登录，返回 JWT |
| GET | `/api/show/list` | 演出列表 |
| GET | `/api/show/{id}` | 演出详情 |
| GET | `/api/show/{id}/sessions` | 场次列表 |
| GET | `/api/show/session/{sid}/categories` | 票价 + 库存 |
| POST | `/api/ticket/buy` | 抢票（需 Bearer token） |
| GET | `/api/order/list` | 我的订单 |

## 项目结构

```
ticket-system/
├── backend/                    # Spring Boot
│   ├── src/main/java/com/ticket/
│   │   ├── controller/         # REST API
│   │   ├── service/            # 业务逻辑（TicketService 抢票核心）
│   │   ├── mq/                 # RabbitMQ 消费者
│   │   ├── config/             # CORS / RabbitMQ / Security / Redis
│   │   ├── mapper/             # MyBatis-Plus + 自定义 SQL
│   │   ├── model/entity/       # 6 张表实体
│   │   └── common/             # JWT / Result / OrderStatus
│   └── pom.xml
├── frontend/                   # React + TypeScript + Vite
│   └── src/
│       ├── pages/              # Login / ShowList / ShowDetail / OrderList
│       ├── api/                # Axios 封装
│       └── styles/             # 暗色舞台设计系统
└── docker-compose.yml
```

## 配置

所有业务参数在 `application.yml` 统一管理：

```yaml
ticket:
  order:
    pay-timeout-seconds: 1800   # 支付超时（秒）
  rate-limit:
    max-per-second: 3           # 每秒最大请求
    burst: 3                    # 令牌桶容量
```
