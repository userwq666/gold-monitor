# 金价监控系统

基于 Spring Boot + Vue3 的金价监控与邮件告警系统。通过 AkShare 获取上交所 SGE Au99.99 实时金价，根据自定义规则触发邮件通知。

## 功能

- **金价抓取** — SGE Au99.99 官方实时价（AkShare），每 1 分钟一次；多源银行金条价备用（xxapi.cn）
- **多源切换** — 下拉选择上交所/银行金条/回收金价/品牌金价，卡片+图表联动
- **四种告警规则**：
  - 阈值通知：价格穿越设定线时触发（穿线逻辑，不重复）
  - 涨跌幅通知：较上次触发价变化超阈值时触发
  - 极值通知：创 7 日内新高或新低（每天一次）
  - 定时报告：到达设定时间推送（每天一次）
- **发件号池** — 轮转发送邮箱账号，规避单号频率限制
- **通知记录** — 每次触发记录精确价格和来源
- **历史走势** — Chart.js 折线图，告警点红色标记
- **JWT 认证** — 管理员添加账号，登录认证

## 技术栈

| 层 | 技术 |
|------|------|
| 后端 | Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA |
| 前端 | Vue 3, Vue Router, Axios, Chart.js |
| 数据库 | SQLite (Hibernate ddl-auto) |
| 数据源 | AkShare (SGE 官方) + xxapi.cn (银行金条) |
| 邮件 | JavaMail + 号池轮转 (QQ/163) |

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- Maven 3.8+
- Python 3 + `pip install akshare`

### 生产运行

```bash
# 1. 安装 Python 依赖
pip install akshare

# 2. 构建前端
cd frontend && npm install && npm run build

# 3. 复制前端产物
Copy-Item -Path frontend/dist -Destination src/main/resources/static -Recurse -Force

# 4. 构建并启动
mvn clean package -DskipTests
java -jar target/gold-monitor-1.0.0.jar
```

访问 `http://localhost:8080`，默认管理员 `admin` / `admin123`。

## 配置

编辑 `src/main/resources/application.yml`：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `crawler.interval-minutes` | 抓取间隔 | `1` |
| `crawler.enabled` | 启动时自动抓取 | `false` |
| `app.data-retention-days` | 数据保留天数 | `90` |
| `ADMIN_PASSWORD` | 管理员初始密码（环境变量） | `admin123` |
| `JWT_SECRET` | JWT 签名密钥（环境变量） | 内置值 |

### 邮箱配置

在管理页 → **发件号池** 添加邮箱账号，密码栏填 **SMTP 授权码**（非登录密码）。

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/change-password` | 修改密码 |
| GET | `/api/health` | 健康检查 |
| GET | `/api/gold-prices/latest` | 最新金价 |
| GET | `/api/gold-prices/history` | 历史金价 |
| GET | `/api/gold-prices/quotes` | 多源报价 |
| POST | `/api/gold-prices/fetch` | 手动抓取 |
| GET | `/api/alert-rules` | 告警规则列表 |
| POST | `/api/alert-rules` | 添加规则 |
| PUT | `/api/alert-rules/{id}` | 更新规则 |
| DELETE | `/api/alert-rules/{id}` | 删除规则 |
| GET | `/api/notifications` | 通知记录 |
| POST | `/api/notifications/test` | 发送测试邮件 |
| POST | `/api/crawler/toggle` | 开关自动抓取 |
| GET | `/api/admin/recipients` | 收件人列表 |
| POST | `/api/admin/recipients` | 添加收件人 |
| GET | `/api/admin/senders` | 发件号池 |
| POST | `/api/admin/senders` | 添加发件账号 |
| GET | `/api/admin/users` | 用户列表 |
| POST | `/api/admin/users` | 添加用户 |

### 告警规则类型

| type | 说明 | 必填字段 |
|------|------|----------|
| `THRESHOLD` | 穿线触发 | `threshold`, `direction` (ABOVE/BELOW) |
| `CHANGE` | 涨跌幅触发 | `threshold` (变动金额，元) |
| `EXTREME` | 7 日极值 | 无 |
| `SCHEDULED` | 定时报告 | `time` (HH:mm) |

### 告警逻辑

- **阈值**：上一次记录在线下、当前跨越线上才触发
- **涨跌幅**：基准为上次触发时的价格，变化超阈值触发并重置基准
- **极值**：比对 7 日历史（排除近 10 分钟），每天最多一次
- **定时**：当前时间 ≥ 设定时间，每天一次
- 所有规则**按来源独立对比**

## 数据库表

| 表 | 说明 |
|-----|------|
| `users` | 登录用户 |
| `recipients` | 邮件收件人 |
| `sender_mails` | 发件邮箱号池 |
| `mail_config` | 系统配置（首选来源等） |
| `gold_prices` | 金价历史 |
| `alert_rules` | 告警规则 |
| `notification_logs` | 通知记录 |
