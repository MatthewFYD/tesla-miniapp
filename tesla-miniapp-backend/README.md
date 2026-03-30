# Tesla MiniApp Backend

基于 SpringBoot 的微信小程序后端服务，对接 TeslaMate 系统提供 Tesla 车辆数据 API。

## 技术栈

- **框架**: Spring Boot 3.2.5
- **数据库**: PostgreSQL (TeslaMate)
- **缓存**: Redis  
- **认证**: JWT + 微信小程序授权
- **文档**: OpenAPI 3 (Swagger)
- **构建**: Maven

## 核心功能

### 1. 用户认证模块
- 微信小程序登录授权
- JWT 令牌管理
- 用户会话管理

### 2. 车辆管理模块  
- 车辆状态实时查询
- 用户车辆绑定管理
- 主要车辆设置

### 3. 充电数据模块
- 充电记录查询
- 充电统计分析
- 家庭/超充分类
- 费用与环保计算

### 4. 数据服务层
- TeslaMate 数据库集成
- 数据缓存优化
- 实时数据处理

## 项目结构

```
src/main/java/com/tesla/miniapp/
├── config/              # 配置类
│   ├── AppConfig.java
│   ├── SecurityConfig.java
│   └── WechatProperties.java
├── controller/          # REST 控制器
│   ├── AuthController.java
│   ├── VehicleController.java
│   └── ChargingController.java
├── dto/                # 数据传输对象
│   ├── ApiResponse.java
│   ├── LoginResponse.java
│   ├── VehicleStatusDto.java
│   └── ChargingStatsDto.java
├── entity/             # JPA 实体类
│   ├── Car.java
│   ├── ChargingProcess.java
│   ├── Position.java
│   ├── WechatUser.java
│   └── UserCarBinding.java
├── repository/         # 数据访问层
│   ├── CarRepository.java
│   ├── ChargingProcessRepository.java
│   └── WechatUserRepository.java
├── service/           # 业务逻辑层
│   ├── AuthService.java
│   ├── VehicleService.java
│   ├── ChargingService.java
│   └── WechatApiService.java
├── security/          # 安全模块
│   ├── JwtAuthenticationFilter.java
│   └── JwtAuthenticationEntryPoint.java
├── util/              # 工具类
│   └── JwtUtil.java
└── exception/         # 异常处理
    └── GlobalExceptionHandler.java
```

## 数据库设计

### 用户相关表（新增）
```sql
-- 微信用户表
CREATE TABLE wechat_users (
    id BIGSERIAL PRIMARY KEY,
    openid VARCHAR(100) UNIQUE NOT NULL,
    union_id VARCHAR(100),
    session_key VARCHAR(100),
    nickname VARCHAR(100),
    avatar_url TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 用户车辆绑定关系表
CREATE TABLE user_car_bindings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES wechat_users(id),
    car_id BIGINT REFERENCES cars(id),
    is_primary BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    alias_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);
```

### TeslaMate 原有表（只读）
- `cars` - 车辆信息
- `charging_processes` - 充电记录
- `positions` - 位置信息
- `drives` - 行程记录

## API 接口

### 认证接口
```
POST /auth/wechat/login     # 微信登录
POST /auth/refresh          # 刷新令牌
GET  /auth/check           # 检查令牌
POST /auth/logout          # 登出
```

### 车辆接口  
```
GET  /vehicles/list                 # 获取车辆列表
GET  /vehicles/primary             # 获取主要车辆
GET  /vehicles/{id}/status         # 获取车辆状态
POST /vehicles/{id}/bind           # 绑定车辆
DELETE /vehicles/{id}/unbind       # 解绑车辆
PUT  /vehicles/{id}/primary        # 设置主要车辆
```

### 充电接口
```
GET /charging/records              # 获取充电记录
GET /charging/records/range        # 按时间范围查询
GET /charging/ongoing              # 正在进行的充电
GET /charging/recent               # 最近充电记录
GET /charging/stats                # 充电统计
GET /charging/stats/monthly        # 月度统计
```

## 配置说明

### application.yml 主要配置
```yaml
spring:
  datasource:
    url: jdbc:postgresql://101.133.153.116:5432/teslamate
    username: teslamate  
    password: 123456

wechat:
  miniapp:
    app-id: ${WECHAT_APP_ID}
    app-secret: ${WECHAT_APP_SECRET}

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000  # 24小时
```

## 部署运行

### 1. 环境准备
```bash
# 安装 Java 17+
# 安装 Maven 3.8+
# 确保 TeslaMate PostgreSQL 可访问
# 准备 Redis 服务
```

### 2. 配置环境变量
```bash
export WECHAT_APP_ID="你的微信小程序AppID"
export WECHAT_APP_SECRET="你的微信小程序AppSecret"  
export JWT_SECRET="你的JWT密钥"
```

### 3. 构建运行
```bash
# 编译项目
mvn clean compile

# 运行应用
mvn spring-boot:run

# 或打包后运行
mvn clean package -DskipTests
java -jar target/miniapp-backend-1.0.0.jar
```

### 4. 访问地址
- 应用地址: http://localhost:8080/api
- API文档: http://localhost:8080/api/docs/swagger-ui.html
- 健康检查: http://localhost:8080/api/actuator/health

## 数据流设计

```
微信小程序客户端
    ↓ (HTTPS + JWT)
Spring Boot API 服务
    ↓ (JPA)
TeslaMate PostgreSQL
    ↓ (实时数据)
Tesla 车辆系统
```

## 安全特性

- JWT 令牌认证
- 微信官方授权验证
- CORS 跨域配置
- SQL 注入防护
- 敏感数据脱敏
- 请求频率限制

## 性能优化

- Redis 缓存热点数据
- JPA 查询优化
- 数据库连接池配置
- 分页查询支持
- 异步处理支持

## 监控指标

- 健康检查端点
- JVM 性能指标
- 数据库连接状态
- 缓存命中率
- API 响应时间

## 开发说明

### 添加新接口步骤
1. 在 `entity` 包创建实体类
2. 在 `repository` 包创建数据访问接口
3. 在 `service` 包实现业务逻辑
4. 在 `controller` 包创建 REST 接口
5. 添加相应的 DTO 类
6. 更新 API 文档

### 数据库集成注意事项
- TeslaMate 数据库为只读访问
- 用户数据存储在新增表中
- 避免修改 TeslaMate 原有表结构
- 定期备份用户数据

## 扩展计划

- [ ] 实时数据推送 (WebSocket/SSE)
- [ ] 车辆远程控制集成
- [ ] 多租户支持
- [ ] 数据导出功能
- [ ] 告警通知系统
- [ ] 移动端消息推送

## 支持联系

如有问题可联系开发团队或查看项目文档。