# 金价监控系统

基于 Spring Boot + Vue3 的个人金价监控与邮件告警系统。定时抓取上海黄金交易所 Au99.99 金价，根据自定义规则触发邮件通知。

## 功能

- **金价抓取** — 定时爬取 Au99.99 国内金价，交易时段每 5 分钟一次
- **四种告警规则**：
  - 阈值通知：金价高于/低于指定价格时触发
  - 涨跌幅通知：相较上次记录变动超过指定金额时触发
  - 极值通知：达到 7 日内新高或新低时触发
  - 定时报告：每日指定时间推送金价日报
- **通知记录** — 每次触发时记录价格快照和发送详情
- **历史走势** — 近 7 日金价折线图
- **收件人管理** — 支持多个收件邮箱
- **多用户管理** — 管理员添加账号，JWT 登录认证

## 技术栈

| 层 | 技术 |
|------|------|
| 后端 | Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA |
| 前端 | Vue 3, Vue Router, Axios, Chart.js |
| 数据库 | SQLite |
| 数据源 | 新浪财经公开接口（爬虫） |
| 邮件 | QQ邮箱/163邮箱 SMTP |

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- Maven 3.8+

### 开发运行

```bash
# 1. 启动后端
cd backend
mvn spring-boot:run

# 2. 新终端，启动前端（开发模式，端口 3000）
cd frontend
npm install
npm run dev
```

访问 `http://localhost:3000`，默认管理员账号 `admin`，密码 `admin123`。

### 生产运行

```bash
# 1. 构建前端
cd frontend
npm install
npm run build

# 2. 复制前端产物到后端静态目录
Copy-Item -Path "frontend/dist" -Destination "backend/src/main/resources/static" -Recurse -Force

# 3. 构建后端
cd backend
mvn clean package -DskipTests

# 4. 启动（端口 8080）
java -jar target/gold-monitor-1.0.0.jar
```

访问 `http://服务器IP:8080`。

## 配置

编辑 `backend/src/main/resources/application.yml`，或通过环境变量覆盖：

| 环境变量 | 说明 | 默认值 |
|----------|------|--------|
| `MAIL_HOST` | SMTP 服务器地址 | `smtp.qq.com` |
| `MAIL_PORT` | SMTP 端口 | `587` |
| `MAIL_USERNAME` | SMTP 邮箱账号 | - |
| `MAIL_PASSWORD` | SMTP 邮箱密码/授权码 | - |
| `ADMIN_PASSWORD` | 管理员初始密码 | `admin123` |
| `JWT_SECRET` | JWT 签名密钥（生产环境务必修改） | 内置默认值 |
| `DB_PATH` | SQLite 数据库文件路径 | `./data/gold-monitor.db` |

### QQ邮箱 SMTP 配置

1. 登录 QQ邮箱 → 设置 → 账户 → 开启 SMTP 服务
2. 生成授权码（作为 `MAIL_PASSWORD` 使用）
3. 启动时设置环境变量：
   ```bash
   set MAIL_USERNAME=yourQQ@qq.com
   set MAIL_PASSWORD=yourAuthCode
   java -jar target/gold-monitor-1.0.0.jar
   ```

## API 接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录获取 token | 否 |
| POST | `/api/auth/change-password` | 修改密码 | 是 |
| GET | `/api/health` | 健康检查 | 否 |
| GET | `/api/gold-prices/latest` | 获取最新金价 | 是 |
| GET | `/api/gold-prices/history?since=` | 获取历史金价 | 是 |
| POST | `/api/gold-prices/fetch` | 手动抓取金价 | 是 |
| GET | `/api/alert-rules` | 获取告警规则列表 | 是 |
| POST | `/api/alert-rules` | 创建告警规则 | 是 |
| PUT | `/api/alert-rules/{id}` | 更新告警规则 | 是 |
| DELETE | `/api/alert-rules/{id}` | 删除告警规则 | 是 |
| GET | `/api/notifications` | 获取通知记录 | 是 |
| POST | `/api/notifications/test` | 发送测试邮件 | 是 |
| GET | `/api/admin/recipients` | 获取收件人列表 | 是 |
| POST | `/api/admin/recipients` | 添加收件人 | 是 |
| PUT | `/api/admin/recipients/{id}` | 更新收件人 | 是 |
| DELETE | `/api/admin/recipients/{id}` | 删除收件人 | 是 |
| GET | `/api/admin/users` | 获取用户列表 | 是 |
| POST | `/api/admin/users` | 添加用户 | 是 |
| DELETE | `/api/admin/users/{id}` | 删除用户 | 是 |

### 告警规则类型

| type | 说明 | 必填字段 |
|------|------|----------|
| `THRESHOLD` | 阈值触发 | `threshold`（价格）, `direction`（ABOVE/BELOW） |
| `CHANGE` | 涨跌幅触发 | `threshold`（变动金额） |
| `EXTREME` | 7日极值触发 | 无 |
| `SCHEDULED` | 定时报告 | `time`（格式 HH:mm） |

登录示例：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## 数据文件

SQLite 数据库默认存储在 `./data/gold-monitor.db`，包含以下表：

- `users` — 登录用户
- `recipients` — 邮件收件人
- `gold_prices` — 金价历史数据
- `alert_rules` — 告警规则
- `notification_logs` — 通知发送记录

数据默认保留 90 天，可通过 `app.data-retention-days` 调整。

## 数据库表

### gold_prices
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| price | DECIMAL(10,2) | 金价（元/克） |
| source | VARCHAR | 数据来源 |
| unit | VARCHAR | 单位 |
| fetch_time | DATETIME | 抓取时间 |

### alert_rules
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR | 规则名称 |
| type | VARCHAR | 规则类型（THRESHOLD/CHANGE/EXTREME/SCHEDULED） |
| threshold | DECIMAL(10,2) | 阈值 |
| direction | VARCHAR | 方向（ABOVE/BELOW） |
| time | VARCHAR | 定时时间（HH:mm） |
| enabled | BOOLEAN | 是否启用 |
| last_triggered_at | DATETIME | 最后触发时间 |
