# Tesla 电车助理 - 微信小程序前端

基于 UniApp + Vue3 + TypeScript + Pinia 的跨平台小程序。

## 技术栈

- **框架**: UniApp + Vue 3
- **语言**: TypeScript
- **状态管理**: Pinia
- **样式**: SCSS
- **构建工具**: Vite

## 项目结构

```
src/
├── api/                    # API 接口封装
│   ├── request.ts          # 请求工具
│   ├── auth.ts             # 认证接口
│   ├── vehicle.ts          # 车辆接口
│   ├── charging.ts         # 充电接口
│   └── trips.ts            # 行程接口
├── components/             # 公共组件
├── pages/                  # 页面
│   ├── login/              # 登录页
│   ├── dashboard/          # 控制面板
│   ├── charging/           # 充电记录
│   ├── trips/              # 行程记录
│   └── insights/           # 数据洞察
├── store/                  # 状态管理
│   ├── user.ts             # 用户状态
│   └── vehicle.ts          # 车辆状态
├── styles/                 # 样式文件
│   ├── variables.scss      # 设计变量
│   ├── mixins.scss         # 混合宏
│   └── global.scss         # 全局样式
├── utils/                  # 工具函数
├── static/                 # 静态资源
├── App.vue                 # 根组件
└── main.ts                 # 入口文件
```

## 快速开始

### 安装依赖

```bash
npm install
```

### 开发调试

```bash
# 微信小程序
npm run dev:mp-weixin

# H5 网页
npm run dev:h5

# App
npm run dev:app
```

### 构建发布

```bash
# 微信小程序
npm run build:mp-weixin

# H5 网页
npm run build:h5

# App
npm run build:app
```

## 页面说明

### 登录页 (login)
- 微信授权登录
- 车辆选择绑定
- 用户协议确认

### 控制面板 (dashboard)
- 电池电量显示
- 续航里程
- 充电状态
- 温度信息
- 最近记录

### 充电记录 (charging)
- 充电统计概览
- 家充/超充筛选
- 充电记录列表
- 节省油费计算

### 行程记录 (trips)
- 行程统计
- 路线显示
- 能耗分析

### 数据洞察 (insights)
- 核心指标展示
- 充电分析
- 行程分析
- 燃油车对比

## 设计规范

### 颜色
- 主色: `#006e37` (Tesla 绿)
- 背景: `#f8f9fa`
- 文字: `#1a1a1a`, `#666666`, `#999999`
- 成功: `#4caf50`
- 警告: `#ff9800`
- 错误: `#f44336`

### 字体大小 (rpx)
- xs: 20
- sm: 24
- base: 28
- md: 32
- lg: 36
- xl: 44
- 2xl: 56
- 3xl: 72

### 间距 (rpx)
- xs: 8
- sm: 16
- md: 24
- lg: 32
- xl: 48
- 2xl: 64
- 3xl: 96

## API 配置

修改 `src/api/request.ts` 中的 `BASE_URL`:

```typescript
const BASE_URL = 'http://your-api-server:8080/api'
```

## 微信小程序配置

修改 `manifest.json` 中的 AppID:

```json
{
  "mp-weixin": {
    "appid": "your-wechat-appid"
  }
}
```

## TabBar 图标

需要在 `src/static/icons/` 目录下放置以下图标:
- dashboard.png / dashboard-active.png
- route.png / route-active.png  
- charging.png / charging-active.png
- insights.png / insights-active.png

建议尺寸: 81x81 px

## 注意事项

1. 首次使用需要微信授权登录
2. 登录后需要选择绑定的 Tesla 车辆
3. 数据来源于 TeslaMate 后台服务
4. 实时数据通过轮询更新（不会增加车辆电耗）

## License

MIT
