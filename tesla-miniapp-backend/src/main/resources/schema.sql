-- =======================================================================
-- Tesla MiniApp Backend Database Schema
-- 用户相关表，不影响TeslaMate原有数据
-- =======================================================================

-- 微信用户表
CREATE TABLE IF NOT EXISTS wechat_users (
    id BIGSERIAL PRIMARY KEY,
    openid VARCHAR(100) UNIQUE NOT NULL,
    union_id VARCHAR(100),
    session_key VARCHAR(100),
    nickname VARCHAR(100),
    avatar_url TEXT,
    gender INTEGER,
    country VARCHAR(50),
    province VARCHAR(50),
    city VARCHAR(50),
    language VARCHAR(10),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 用户车辆绑定关系表
CREATE TABLE IF NOT EXISTS user_car_bindings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES wechat_users(id) ON DELETE CASCADE,
    car_id BIGINT NOT NULL REFERENCES cars(id) ON DELETE CASCADE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    alias_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(user_id, car_id)
);

-- 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_wechat_users_openid ON wechat_users(openid);
CREATE INDEX IF NOT EXISTS idx_wechat_users_union_id ON wechat_users(union_id);
CREATE INDEX IF NOT EXISTS idx_user_car_bindings_user_id ON user_car_bindings(user_id);
CREATE INDEX IF NOT EXISTS idx_user_car_bindings_car_id ON user_car_bindings(car_id);
CREATE INDEX IF NOT EXISTS idx_user_car_bindings_primary ON user_car_bindings(user_id, is_primary) WHERE is_primary = TRUE;

-- 更新时间触发器
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_wechat_users_updated_at ON wechat_users;
CREATE TRIGGER update_wechat_users_updated_at BEFORE UPDATE ON wechat_users 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_user_car_bindings_updated_at ON user_car_bindings;
CREATE TRIGGER update_user_car_bindings_updated_at BEFORE UPDATE ON user_car_bindings 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 插入示例数据（可选）
-- INSERT INTO wechat_users (openid, nickname) VALUES ('test_openid_001', '测试用户1') ON CONFLICT DO NOTHING;