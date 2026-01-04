-- 表：用户位置历史
CREATE TABLE IF NOT EXISTS im_lbs_location_history
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    user_id
    BIGINT
    NOT
    NULL,
    longitude
    DOUBLE
    PRECISION
    NOT
    NULL,
    latitude
    DOUBLE
    PRECISION
    NOT
    NULL,
    geohash
    VARCHAR
(
    12
),
    create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- 索引：按用户与时间（降序）查询历史位置
CREATE INDEX IF NOT EXISTS idx_loc_user_time
    ON im_lbs_location_history (user_id, create_time DESC);

-- 索引：按 geohash 查询
CREATE INDEX IF NOT EXISTS idx_loc_geohash
    ON im_lbs_location_history (geohash);

COMMENT
ON TABLE im_lbs_location_history IS 'LBS - 用户位置历史表，记录用户上报的经纬度、geohash 与时间，供轨迹回溯/热力图/历史查询使用';

COMMENT
ON COLUMN im_lbs_location_history.id IS '主键，自增（BIGSERIAL）';
COMMENT
ON COLUMN im_lbs_location_history.user_id IS '用户 ID';
COMMENT
ON COLUMN im_lbs_location_history.longitude IS '经度（WGS84，double precision）';
COMMENT
ON COLUMN im_lbs_location_history.latitude IS '纬度（WGS84，double precision）';
COMMENT
ON COLUMN im_lbs_location_history.geohash IS 'Geohash（长度建议 6~12，用于快速空间分区/模糊查询）';
COMMENT
ON COLUMN im_lbs_location_history.create_time IS '记录创建时间（带时区，默认 CURRENT_TIMESTAMP）';

