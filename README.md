# Tesla 电车助理小程序

基于 TeslaMate 数据的微信小程序，让你随时随地查看 Tesla 车辆状态、充电记录、行程统计等信息。

![Tesla MiniApp](https://img.shields.io/badge/Tesla-MiniApp-red?style=flat-square&logo=tesla)
![Vue 3](https://img.shields.io/badge/Vue-3.x-green?style=flat-square&logo=vue.js)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green?style=flat-square&logo=spring)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

## 📱 功能特性

### 仪表盘
- 🔋 实时电量与续航里程
- 🌡️ 车内外温度显示
- ⚡ 充电状态与功率
- 📍 车辆位置信息
- 📊 本月行驶/充电概览

### 充电记录
- 📋 充电历史列表
- 🏠 家充/超充分类筛选
- 📈 充电统计分析
- 💰 费用与节省计算

### 行程记录
- 🛣️ 行程历史列表
- 📏 里程与能耗统计
- ⏱️ 行驶时间记录

### 数据洞察
- 📊 充电数据分析
- 🚗 驾驶习惯统计
- 💹 效率趋势图表

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                     微信小程序                               │
│              (Vue 3 + TypeScript + uni-app)                 │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTPS
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Nginx 反向代理                            │
│                  (SSL 证书 + 负载均衡)                       │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot 后端                          │
│           (REST API + JWT 认证 + JPA)                       │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                  TeslaMate PostgreSQL                       │
│              (车辆数据 + 位置 + 充电记录)                     │
└─────────────────────────────────────────────────────────────┘
```

## 📁 项目结构

```
tesla-app/
├── tesla-miniapp-backend/     # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com/tesla/miniapp/
│   │       ├── config/        # 配置类 (Security, CORS, etc.)
│   │       ├── controller/    # REST API 控制器
│   │       ├── dto/           # 数据传输对象
│   │       ├── entity/        # JPA 实体
│   │       ├── repository/    # 数据访问层
│   │       └── service/       # 业务逻辑层
│   └── src/main/resources/
│       └── application.yml.example
│
├── tesla-miniapp-frontend/    # uni-app 前端
│   ├── src/
│   │   ├── api/               # API 接口封装
│   │   ├── pages/             # 页面组件
│   │   ├── store/             # Pinia 状态管理
│   │   └── static/            # 静态资源
│   └── .env.example
│
├── docs/                      # 文档
│   └── API_MAPPING.md         # API 映射文档
│
└── README.md
```

## 🚀 快速开始

### 前置要求

- [TeslaMate](https://github.com/teslamate-org/teslamate) 已部署并运行
- Node.js 18+
- Java 17+
- Maven 3.8+
- 微信小程序开发者账号

### 1. 克隆项目

```bash
git clone https://github.com/your-username/tesla-miniapp.git
cd tesla-miniapp
```

### 2. 后端配置

```bash
cd tesla-miniapp-backend

# 复制配置文件
cp src/main/resources/application.yml.example src/main/resources/application.yml

# 编辑配置文件，填入实际值
vim src/main/resources/application.yml
```

需要配置的关键项：
- `spring.datasource.url` - TeslaMate PostgreSQL 数据库地址
- `spring.datasource.username/password` - 数据库账号密码
- `wechat.miniapp.app-id/app-secret` - 微信小程序凭证
- `jwt.secret` - JWT 签名密钥

### 3. 启动后端

```bash
# 开发环境
mvn spring-boot:run

# 或打包后运行
mvn clean package -DskipTests
java -jar target/tesla-miniapp-backend-1.0.0.jar
```

### 4. 前端配置

```bash
cd tesla-miniapp-frontend

# 安装依赖
npm install

# 复制配置文件
cp .env.example .env.development
cp .env.example .env.production

# 编辑配置
vim .env.development   # 本地开发
vim .env.production    # 生产环境
```

### 5. 启动前端

```bash
# 开发模式 (微信小程序)
npm run dev:mp-weixin

# 打包生产版本
npm run build:mp-weixin:prod
```

### 6. 微信开发者工具

1. 打开微信开发者工具
2. 导入项目：`tesla-miniapp-frontend/dist/build/mp-weixin`
3. 在「详情 → 本地设置」勾选「不校验合法域名」(开发调试)
4. 预览或上传

## ⚙️ 配置说明

### 后端配置 (application.yml)

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `spring.datasource.url` | TeslaMate 数据库地址 | `jdbc:postgresql://localhost:5432/teslamate` |
| `wechat.miniapp.app-id` | 微信小程序 AppID | `wx1234567890abcdef` |
| `wechat.miniapp.app-secret` | 微信小程序 AppSecret | `your-app-secret` |
| `jwt.secret` | JWT 签名密钥 | `随机生成的安全密钥` |

### 前端配置 (.env)

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `VITE_API_BASE_URL` | 后端 API 地址 | `https://your-domain.com/api` |
| `VITE_ENV` | 环境标识 | `development` / `production` |

## 📡 API 文档

后端启动后访问 Swagger UI：
- 本地: `http://localhost:8080/api/docs/swagger-ui.html`
- 生产: `https://your-domain.com/api/docs/swagger-ui.html`

主要 API 端点：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/auth/wechat/login` | POST | 微信登录 |
| `/api/vehicles/{carId}/status` | GET | 车辆状态 |
| `/api/charging/stats` | GET | 充电统计 |
| `/api/charging/records` | GET | 充电记录 |
| `/api/trips/list` | GET | 行程列表 |
| `/api/dashboard/monthly` | GET | 月度统计 |

## 🔐 安全注意事项

1. **不要提交敏感配置** - `application.yml` 和 `.env` 已在 `.gitignore` 中
2. **使用环境变量** - 生产环境建议使用环境变量覆盖配置
3. **JWT 密钥** - 使用足够长度的随机字符串
4. **HTTPS** - 生产环境必须使用 HTTPS

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 🙏 致谢

- [TeslaMate](https://github.com/teslamate-org/teslamate) - 优秀的 Tesla 数据记录工具
- [uni-app](https://uniapp.dcloud.net.cn/) - 跨平台应用框架
- [Spring Boot](https://spring.io/projects/spring-boot) - Java 后端框架
