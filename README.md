# 金价监控系统 (Gold Monitor)

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-brightgreen.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

基于 Spring Boot + Vue3 的实时金价监控与智能邮件告警系统。通过 AkShare 获取上交所 SGE Au99.99 实时金价，结合多种数据源和自定义规则实现精准的价格追踪和通知。

## 🌟 功能特性

### 📊 实时金价监控
- **多源数据采集**：主用 AkShare 获取上交所 SGE Au99.99 官方实时价（每分钟更新）
- **备用数据源**：支持银行金条价、回收金价、品牌金价等多维度价格数据
- **智能切换机制**：下拉选择不同数据源，卡片展示与图表联动实时更新

### 🔔 智能告警系统
提供四种灵活的告警规则类型：

| 规则类型 | 触发条件 | 特点 |
|---------|---------|------|
| 阈值告警 | 价格穿越设定线 | 穿线逻辑，避免重复触发 |
| 涨跌幅告警 | 较上次触发价变化超阈值 | 动态基准，自动重置 |
| 极值告警 | 创7日内新高或新低 | 每日限一次，防止频繁打扰 |
| 定时报告 | 到达设定时间推送 | 每日固定时间发送 |

### 📧 邮件通知系统
- **发件号池轮转**：多个邮箱账号轮流发送，规避单账号频率限制
- **精确记录追踪**：每次触发都记录精确价格和数据来源
- **可视化历史走势**：Chart.js 折线图展示，告警点红色标记突出显示

### 🔐 安全认证
- JWT Token 认证机制
- 管理员权限控制
- 密码修改功能

## 🛠️ 技术架构

### 后端技术栈
- **核心框架**：Java 17, Spring Boot 3.2.5
- **安全框架**：Spring Security + JWT
- **数据访问**：Spring Data JPA
- **数据库**：SQLite (轻量级嵌入式数据库)
- **邮件服务**：JavaMail Sender
- **网页解析**：Jsoup

### 前端技术栈
- **核心框架**：Vue 3 Composition API
- **路由管理**：Vue Router 4
- **HTTP客户端**：Axios
- **图表库**：Chart.js 4.x + vue-chartjs
- **构建工具**：Vite 5.x

### 数据源
- **主要数据源**：AkShare (SGE 官方数据)
- **备用数据源**：xxapi.cn (银行金条等多元报价)

## 🚀 快速开始

### 环境要求
- JDK 17+
- Node.js 18+ 
- Maven 3.8+
- Python 3.x + pip

### 安装步骤

#### 1. 克隆项目
```bash
git clone <repository-url>
cd gold-monitor
```

#### 2. 安装 Python 依赖
```bash
pip install akshare
```

#### 3. 构建前端
```bash
cd frontend
npm install
npm run build
```

#### 4. 部署前端资源
```powershell
# Windows PowerShell
Copy-Item -Path frontend/dist -Destination src/main/resources/static -Recurse -Force
```

#### 5. 构建并启动后端
```bash
mvn clean package -DskipTests
java -jar target/gold-monitor-1.0.0.jar
```

#### 6. 访问应用
打开浏览器访问 `http://localhost:8080`

默认管理员账户：`admin` / `admin123`（建议首次登录后立即修改密码）

## ⚙️ 配置说明

### 应用配置 (`application.yml`)

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 服务端口 | `8080` |
| `crawler.enabled` | 启动时是否自动抓取 | `false` |
| `crawler.interval-minutes` | 抓取间隔(分钟) | `1` |
| `app.data-retention-days` | 数据保留天数 | `90` |
| `jwt.secret` | JWT签名密钥 | 内置测试密钥 |
| `jwt.expiration` | Token有效期(毫秒) | `86400000`(24小时) |

### 环境变量
| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `ADMIN_PASSWORD` | 管理员初始密码 | `admin123` |
| `JWT_SECRET` | JWT签名密钥 | 内置值 |
| `DB_PATH` | 数据库路径 | `./data/gold-monitor.db` |

### 邮箱配置
在管理页面的"发件号池"中添加邮箱账号：
- 支持 QQ邮箱、163邮箱等主流服务商
- 密码栏填写 **SMTP授权码**（非登录密码）
- 建议配置多个账号以实现轮转发送

## 📡 API 接口文档

### 认证相关
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/change-password` | 修改密码 |

### 金价数据
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/gold-prices/latest` | 获取最新金价 |
| GET | `/api/gold-prices/history` | 获取历史金价 |
| GET | `/api/gold-prices/quotes` | 获取多源报价 |
| POST | `/api/gold-prices/fetch` | 手动触发抓取 |

### 告警规则
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/alert-rules` | 获取告警规则列表 |
| POST | `/api/alert-rules` | 创建告警规则 |
| PUT | `/api/alert-rules/{id}` | 更新告警规则 |
| DELETE | `/api/alert-rules/{id}` | 删除告警规则 |

### 通知管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/notifications` | 获取通知记录 |
| POST | `/api/notifications/test` | 发送测试邮件 |

### 系统管理
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/crawler/toggle` | 开关自动抓取 |
| GET | `/api/admin/recipients` | 获取收件人列表 |
| POST | `/api/admin/recipients` | 添加收件人 |
| GET | `/api/admin/senders` | 获取发件号池 |
| POST | `/api/admin/senders` | 添加发件账号 |
| GET | `/api/admin/users` | 获取用户列表 |
| POST | `/api/admin/users` | 添加用户 |

### 告警规则详细说明

#### 规则类型参数
| type | 说明 | 必填字段 |
|------|------|----------|
| `THRESHOLD` | 阈值告警 | `threshold`, `direction`(ABOVE/BELOW) |
| `CHANGE` | 涨跌幅告警 | `threshold`(变动金额，单位：元) |
| `EXTREME` | 极值告警 | 无额外必填字段 |
| `SCHEDULED` | 定时报告 | `time`(格式：HH:mm) |

#### 告警触发逻辑
- **阈值告警**：检测价格是否从线下穿越到线上（或相反），避免在同一价位重复触发
- **涨跌幅告警**：以上次触发价格为基准，当变化幅度超过设定阈值时触发并重置基准
- **极值告警**：对比过去7天历史数据（排除最近10分钟噪音），识别新高或新低，每日最多触发一次
- **定时报告**：到达设定时间点触发，每日最多一次
- 所有规则均按数据源独立计算和对比

## 🗄️ 数据库设计

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `users` | 用户账户 | id, username, password, role |
| `recipients` | 邮件收件人 | id, email, name, active |
| `sender_mails` | 发件邮箱池 | id, email, smtp_password, active |
| `mail_config` | 邮件配置 | id, config_key, config_value |
| `gold_prices` | 金价历史 | id, source, price, timestamp |
| `alert_rules` | 告警规则 | id, type, threshold, active |
| `notification_logs` | 通知日志 | id, rule_id, content, sent_at |

## 🏗️ 项目结构

```
gold-monitor/
├── src/main/java/com/goldmonitor/
│   ├── config/          # 配置类
│   ├── controller/      # REST控制器
│   ├── dto/            # 数据传输对象
│   ├── model/          # 实体模型
│   ├── repository/     # 数据访问层
│   ├── scheduler/      # 定时任务
│   ├── security/       # 安全组件
│   └── service/        # 业务逻辑层
├── src/main/python/    # Python爬虫脚本
├── frontend/           # Vue3前端项目
│   ├── src/
│   │   ├── api/        # API调用
│   │   ├── components/ # 可复用组件
│   │   ├── views/      # 页面视图
│   │   └── router/     # 路由配置
│   └── ...
└── data/               # SQLite数据库文件
```

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request 来改进本项目！

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- [AkShare](https://github.com/akfamily/akshare) - 优秀的金融数据接口库
- [Spring Boot](https://spring.io/projects/spring-boot) - 强大的Java开发框架
- [Vue.js](https://vuejs.org/) - 渐进式JavaScript框架
