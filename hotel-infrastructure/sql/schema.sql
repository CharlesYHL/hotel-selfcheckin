-- ============================================================
-- 酒店自助入住系统 - 数据库表结构
-- 数据库: hotel_db
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS hotel_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hotel_db;

-- ----------------------------
-- 1. 酒店管理模块
-- ----------------------------

CREATE TABLE sys_hotel (
    hotel_id        VARCHAR(32) NOT NULL COMMENT '酒店ID',
    hotel_code      VARCHAR(20) NOT NULL COMMENT '酒店编码',
    hotel_name      VARCHAR(100) NOT NULL COMMENT '酒店名称',
    hotel_type      TINYINT NOT NULL DEFAULT 1 COMMENT '酒店类型: 1-直营, 2-加盟',
    brand_id        VARCHAR(32) COMMENT '品牌ID',
    province        VARCHAR(20) COMMENT '省份',
    city            VARCHAR(20) COMMENT '城市',
    district        VARCHAR(50) COMMENT '区县',
    address         VARCHAR(200) COMMENT '详细地址',
    longitude       DECIMAL(10,6) COMMENT '经度',
    latitude        DECIMAL(10,6) COMMENT '纬度',
    contact_phone   VARCHAR(20) COMMENT '联系电话',
    star_level      TINYINT COMMENT '星级',
    check_in_start  TIME DEFAULT '14:00:00' COMMENT '最早入住时间',
    check_out_end   TIME DEFAULT '12:00:00' COMMENT '最晚退房时间',
    timezone        VARCHAR(20) DEFAULT 'Asia/Shanghai' COMMENT '时区',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (hotel_id),
    UNIQUE KEY uk_hotel_code (hotel_code),
    KEY idx_status (status),
    KEY idx_city (city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店表';

CREATE TABLE sys_room_type (
    room_type_id    VARCHAR(32) NOT NULL COMMENT '房型ID',
    hotel_id        VARCHAR(32) NOT NULL COMMENT '酒店ID',
    room_type_code  VARCHAR(20) NOT NULL COMMENT '房型编码',
    room_type_name  VARCHAR(50) NOT NULL COMMENT '房型名称',
    room_type_short VARCHAR(20) COMMENT '房型简称',
    description     VARCHAR(500) COMMENT '房型描述',
    base_capacity   INT NOT NULL DEFAULT 2 COMMENT '基础可入住人数',
    max_capacity    INT NOT NULL DEFAULT 4 COMMENT '最大可入住人数',
    bed_type        VARCHAR(20) COMMENT '床型',
    bed_size        VARCHAR(20) COMMENT '床尺寸',
    room_area       DECIMAL(8,2) COMMENT '房间面积',
    floor_range     VARCHAR(50) COMMENT '楼层范围',
    max_rooms       INT NOT NULL DEFAULT 100 COMMENT '该房型最大房间数',
    sort_order      INT NOT NULL DEFAULT 0 COMMENT '排序',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (room_type_id),
    UNIQUE KEY uk_type_code (hotel_id, room_type_code),
    KEY idx_hotel (hotel_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房型表';

CREATE TABLE rom_price (
    price_id        VARCHAR(32) NOT NULL COMMENT '价格ID',
    room_type_id    VARCHAR(32) NOT NULL COMMENT '房型ID',
    hotel_id        VARCHAR(32) NOT NULL COMMENT '酒店ID',
    date_type       TINYINT NOT NULL DEFAULT 1 COMMENT '日期类型: 1-平日, 2-周末, 3-节假日',
    price_date      DATE COMMENT '指定日期',
    rack_rate       DECIMAL(10,2) NOT NULL COMMENT '挂牌价',
    sell_rate       DECIMAL(10,2) NOT NULL COMMENT '售价',
    cost_rate       DECIMAL(10,2) COMMENT '成本价',
    member_rate     DECIMAL(10,2) COMMENT '会员价',
    vip_rate        DECIMAL(10,2) COMMENT 'VIP价',
    currency        VARCHAR(10) DEFAULT 'CNY' COMMENT '货币',
    start_date      DATE NOT NULL COMMENT '生效开始日期',
    end_date        DATE NOT NULL COMMENT '生效结束日期',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (price_id),
    KEY idx_room_type (room_type_id),
    KEY idx_date (date_type, price_date),
    KEY idx_period (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房价表';

CREATE TABLE sys_floor (
    floor_id        VARCHAR(32) NOT NULL COMMENT '楼层ID',
    hotel_id        VARCHAR(32) NOT NULL COMMENT '酒店ID',
    floor_no        VARCHAR(10) NOT NULL COMMENT '楼层号',
    floor_name      VARCHAR(20) COMMENT '楼层名称',
    floor_type      TINYINT COMMENT '楼层类型: 1-客房, 2-公共, 3-办公',
    sort_order      INT NOT NULL DEFAULT 0,
    status          TINYINT NOT NULL DEFAULT 1,
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (floor_id),
    UNIQUE KEY uk_hotel_floor (hotel_id, floor_no),
    KEY idx_hotel (hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼层表';

CREATE TABLE rom_room (
    room_id         VARCHAR(32) NOT NULL COMMENT '房间ID',
    room_no         VARCHAR(20) NOT NULL COMMENT '房间号',
    hotel_id        VARCHAR(32) NOT NULL COMMENT '酒店ID',
    room_type_id    VARCHAR(32) NOT NULL COMMENT '房型ID',
    floor_id        VARCHAR(32) NOT NULL COMMENT '楼层ID',
    floor_no        VARCHAR(10) NOT NULL COMMENT '楼层号',
    building        VARCHAR(20) COMMENT '楼栋',
    unit            VARCHAR(10) COMMENT '单元',
    room_status     TINYINT NOT NULL DEFAULT 1 COMMENT '房态: 1-空房, 2-占用, 3-维修, 4-清洁, 5-预订, 6-封房',
    direction       VARCHAR(20) COMMENT '朝向',
    window_type     VARCHAR(20) COMMENT '窗户类型',
    acreage         DECIMAL(8,2) COMMENT '面积',
    bed_type        VARCHAR(20) COMMENT '床型配置',
    max_guest       INT NOT NULL DEFAULT 2 COMMENT '最大入住人数',
    is_smoke_free   TINYINT DEFAULT 1 COMMENT '是否禁烟',
    is_active       TINYINT NOT NULL DEFAULT 1 COMMENT '是否可用',
    sort_order      INT NOT NULL DEFAULT 0,
    remark          VARCHAR(200) COMMENT '备注',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (room_id),
    UNIQUE KEY uk_hotel_room_no (hotel_id, room_no),
    KEY idx_hotel (hotel_id),
    KEY idx_room_type (room_type_id),
    KEY idx_status (room_status, is_active),
    KEY idx_floor (floor_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间表';

-- ----------------------------
-- 2. 订单模块
-- ----------------------------

CREATE TABLE ord_order (
    order_id        VARCHAR(32) NOT NULL COMMENT '订单ID',
    order_no        VARCHAR(32) NOT NULL COMMENT '订单号',
    hotel_id        VARCHAR(32) NOT NULL COMMENT '酒店ID',
    member_id       VARCHAR(32) COMMENT '会员ID',
    source_type     TINYINT NOT NULL COMMENT '订单来源: 1-APP, 2-小程序, 3-前台, 4-OTA',
    source_channel  VARCHAR(50) COMMENT '来源渠道',
    room_type_id    VARCHAR(32) NOT NULL COMMENT '房型ID',
    room_type_name  VARCHAR(50) NOT NULL COMMENT '房型名称',
    check_in_date   DATE NOT NULL COMMENT '入住日期',
    check_out_date  DATE NOT NULL COMMENT '退房日期',
    nights          INT NOT NULL COMMENT '入住晚数',
    room_count      INT NOT NULL DEFAULT 1 COMMENT '房间数量',
    adults          INT NOT NULL DEFAULT 1 COMMENT '成人数',
    children        INT NOT NULL DEFAULT 0 COMMENT '儿童数',
    contact_name    VARCHAR(50) COMMENT '联系人姓名',
    contact_phone   VARCHAR(20) COMMENT '联系人电话',
    special_request VARCHAR(500) COMMENT '特殊要求',
    order_amount    DECIMAL(12,2) NOT NULL COMMENT '订单金额',
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
    paid_amount     DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '已付金额',
    due_amount      DECIMAL(12,2) NOT NULL COMMENT '待付金额',
    currency        VARCHAR(10) DEFAULT 'CNY',
    order_status    TINYINT NOT NULL DEFAULT 1 COMMENT '订单状态: 1-待支付, 2-已支付, 3-已排房, 4-已入住, 5-已完成, 6-已取消, 7-已退款',
    pay_expire_time DATETIME COMMENT '支付过期时间',
    paid_time       DATETIME COMMENT '支付时间',
    cancel_time     DATETIME COMMENT '取消时间',
    cancel_reason   VARCHAR(200) COMMENT '取消原因',
    remark          VARCHAR(500) COMMENT '备注',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_hotel (hotel_id),
    KEY idx_member (member_id),
    KEY idx_status (order_status),
    KEY idx_check_in (check_in_date, check_out_date),
    KEY idx_phone (contact_phone),
    KEY idx_create_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

CREATE TABLE ord_order_item (
    item_id         VARCHAR(32) NOT NULL COMMENT '明细ID',
    order_id        VARCHAR(32) NOT NULL COMMENT '订单ID',
    room_type_id    VARCHAR(32) NOT NULL COMMENT '房型ID',
    stay_date       DATE NOT NULL COMMENT '入住日期',
    rack_rate       DECIMAL(10,2) NOT NULL COMMENT '挂牌价',
    sell_rate       DECIMAL(10,2) NOT NULL COMMENT '售价',
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠',
    room_no         VARCHAR(20) COMMENT '房间号',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (item_id),
    KEY idx_order (order_id),
    KEY idx_stay_date (stay_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

CREATE TABLE ord_order_log (
    log_id          VARCHAR(32) NOT NULL COMMENT '日志ID',
    order_id        VARCHAR(32) NOT NULL COMMENT '订单ID',
    order_no        VARCHAR(32) NOT NULL COMMENT '订单号',
    from_status     TINYINT COMMENT '变更前状态',
    to_status       TINYINT NOT NULL COMMENT '变更后状态',
    operator_id     VARCHAR(32) COMMENT '操作人ID',
    operator_name   VARCHAR(50) COMMENT '操作人',
    operator_type   TINYINT COMMENT '操作类型: 1-系统, 2-员工, 3-会员',
    change_remark   VARCHAR(500) COMMENT '变更说明',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_order (order_id),
    KEY idx_create_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态变更记录表';

-- ----------------------------
-- 3. 入住模块
-- ----------------------------

CREATE TABLE chk_checkin (
    checkin_id      VARCHAR(32) NOT NULL COMMENT '入住ID',
    checkin_no      VARCHAR(32) NOT NULL COMMENT '入住单号',
    order_id        VARCHAR(32) NOT NULL COMMENT '订单ID',
    order_no        VARCHAR(32) NOT NULL COMMENT '订单号',
    hotel_id        VARCHAR(32) NOT NULL COMMENT '酒店ID',
    member_id       VARCHAR(32) COMMENT '会员ID',
    room_id         VARCHAR(32) NOT NULL COMMENT '房间ID',
    room_no         VARCHAR(20) NOT NULL COMMENT '房间号',
    room_type_id    VARCHAR(32) NOT NULL COMMENT '房型ID',
    room_type_name  VARCHAR(50) NOT NULL COMMENT '房型名称',
    check_in_time   DATETIME NOT NULL COMMENT '入住时间',
    check_out_time  DATETIME NOT NULL COMMENT '预计退房时间',
    actual_checkout_time DATETIME COMMENT '实际退房时间',
    adults          INT NOT NULL DEFAULT 1 COMMENT '成人数',
    children        INT NOT NULL DEFAULT 0 COMMENT '儿童数',
    card_no         VARCHAR(50) COMMENT '门卡号',
    total_amount    DECIMAL(12,2) NOT NULL COMMENT '消费总额',
    paid_amount     DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '已付金额',
    due_amount      DECIMAL(12,2) NOT NULL COMMENT '待付金额',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-入住中, 2-已续住, 3-已退房, 4-取消入住',
    checkin_channel TINYINT NOT NULL DEFAULT 1 COMMENT '入住渠道: 1-APP自助, 2-前台办理, 3-OTA',
    verify_status   TINYINT NOT NULL DEFAULT 1 COMMENT '身份核验: 1-待核验, 2-已通过, 3-未通过, 4-待人工',
    verify_remark   VARCHAR(200) COMMENT '核验说明',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (checkin_id),
    UNIQUE KEY uk_checkin_no (checkin_no),
    KEY idx_order (order_id),
    KEY idx_room (room_id, status),
    KEY idx_hotel (hotel_id),
    KEY idx_member (member_id),
    KEY idx_status (status),
    KEY idx_checkout_time (check_out_time),
    KEY idx_checkin_time (check_in_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入住记录表';

CREATE TABLE chk_guest (
    guest_id        VARCHAR(32) NOT NULL COMMENT '入住人ID',
    checkin_id      VARCHAR(32) NOT NULL COMMENT '入住ID',
    hotel_id        VARCHAR(32) NOT NULL COMMENT '酒店ID',
    guest_name      VARCHAR(50) NOT NULL COMMENT '姓名',
    guest_type      TINYINT NOT NULL DEFAULT 1 COMMENT '类型: 1-主入住人, 2-同行人',
    id_card_type    TINYINT NOT NULL DEFAULT 1 COMMENT '证件类型: 1-身份证, 2-护照, 3-港澳通行证',
    id_card_no      VARCHAR(50) NOT NULL COMMENT '证件号码',
    id_card_hash    VARCHAR(200) COMMENT '证件号哈希',
    id_card_encrypted TEXT COMMENT '证件号加密',
    key_version     INT COMMENT '密钥版本',
    salt            VARCHAR(50) COMMENT '哈希盐值',
    id_card_masked  VARCHAR(20) COMMENT '脱敏证件号',
    gender          TINYINT COMMENT '性别: 1-男, 2-女',
    birth_date      DATE COMMENT '出生日期',
    phone           VARCHAR(20) COMMENT '联系电话',
    nationality     VARCHAR(50) COMMENT '国籍',
    address         VARCHAR(200) COMMENT '住址',
    verify_status   TINYINT NOT NULL DEFAULT 1 COMMENT '核验状态: 1-待核验, 2-已通过, 3-未通过, 4-待人工',
    verify_message  VARCHAR(200) COMMENT '核验结果',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (guest_id),
    KEY idx_checkin (checkin_id),
    KEY idx_id_card_hash (id_card_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入住人信息表';

CREATE TABLE chk_extension (
    extension_id    VARCHAR(32) NOT NULL COMMENT '续住ID',
    checkin_id      VARCHAR(32) NOT NULL COMMENT '入住ID',
    hotel_id        VARCHAR(32) NOT NULL COMMENT '酒店ID',
    room_id         VARCHAR(32) NOT NULL COMMENT '房间ID',
    extend_from     DATETIME NOT NULL COMMENT '原退房时间',
    extend_to       DATETIME NOT NULL COMMENT '新退房时间',
    extend_days     INT NOT NULL COMMENT '续住天数',
    nightly_rate    DECIMAL(10,2) NOT NULL COMMENT '日均房价',
    extend_amount   DECIMAL(10,2) NOT NULL COMMENT '续住金额',
    pay_status      TINYINT NOT NULL DEFAULT 1 COMMENT '支付状态: 1-待支付, 2-已支付',
    payment_id      VARCHAR(32) COMMENT '支付记录ID',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (extension_id),
    KEY idx_checkin (checkin_id),
    KEY idx_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='续住记录表';

CREATE TABLE chk_checkout (
    checkout_id     VARCHAR(32) NOT NULL COMMENT '退房ID',
    checkin_id      VARCHAR(32) NOT NULL COMMENT '入住ID',
    hotel_id        VARCHAR(32) NOT NULL COMMENT '酒店ID',
    room_id         VARCHAR(32) NOT NULL COMMENT '房间ID',
    room_no         VARCHAR(20) NOT NULL COMMENT '房间号',
    check_in_time   DATETIME NOT NULL COMMENT '入住时间',
    check_out_time  DATETIME NOT NULL COMMENT '退房时间',
    nights          INT NOT NULL COMMENT '入住晚数',
    room_amount     DECIMAL(10,2) NOT NULL COMMENT '房费',
    extra_amount    DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '附加费',
    total_amount    DECIMAL(10,2) NOT NULL COMMENT '总计',
    paid_amount     DECIMAL(10,2) NOT NULL COMMENT '已付金额',
    refund_amount   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '退款金额',
    due_amount      DECIMAL(10,2) NOT NULL COMMENT '需付/退金额',
    checkout_type   TINYINT NOT NULL DEFAULT 1 COMMENT '退房方式: 1-APP自助, 2-前台办理',
    checkout_status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常, 2-需补款, 3-需退款',
    card_return     TINYINT NOT NULL DEFAULT 1 COMMENT '门卡归还',
    item_check      TINYINT NOT NULL DEFAULT 1 COMMENT '物品检查: 1-正常, 2-损坏, 3-缺失',
    remark          VARCHAR(500) COMMENT '备注',
    operator_id     VARCHAR(32) COMMENT '操作人ID',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (checkout_id),
    KEY idx_checkin (checkin_id),
    KEY idx_room (room_id),
    KEY idx_hotel (hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退房记录表';

-- ----------------------------
-- 4. 门卡模块
-- ----------------------------

CREATE TABLE crd_card (
    card_id         VARCHAR(32) NOT NULL COMMENT '门卡ID',
    card_no         VARCHAR(50) NOT NULL COMMENT '卡号',
    hotel_id        VARCHAR(32) NOT NULL COMMENT '酒店ID',
    checkin_id      VARCHAR(32) NOT NULL COMMENT '入住ID',
    room_id         VARCHAR(32) NOT NULL COMMENT '房间ID',
    room_no         VARCHAR(20) NOT NULL COMMENT '房间号',
    guest_id        VARCHAR(32) COMMENT '入住人ID',
    valid_from      DATETIME NOT NULL COMMENT '生效时间',
    valid_to        DATETIME NOT NULL COMMENT '失效时间',
    card_type       TINYINT NOT NULL DEFAULT 1 COMMENT '卡类型: 1-新制, 2-补办, 3-延期',
    card_status     TINYINT NOT NULL DEFAULT 1 COMMENT '卡状态: 1-有效, 2-已过期, 3-已挂失, 4-已注销',
    open_count      INT NOT NULL DEFAULT 0 COMMENT '开门次数',
    last_open_time  DATETIME COMMENT '最后开门时间',
    lost_time       DATETIME COMMENT '挂失时间',
    cancel_time     DATETIME COMMENT '注销时间',
    qr_code         VARCHAR(100) COMMENT '二维码',
    qr_expire_time  DATETIME COMMENT '二维码过期时间',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (card_id),
    UNIQUE KEY uk_card_no (card_no),
    KEY idx_checkin (checkin_id),
    KEY idx_room (room_id, card_status),
    KEY idx_valid_to (valid_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门卡表';

CREATE TABLE crd_card_log (
    log_id          VARCHAR(32) NOT NULL COMMENT '日志ID',
    card_id         VARCHAR(32) NOT NULL COMMENT '门卡ID',
    card_no         VARCHAR(50) NOT NULL COMMENT '卡号',
    hotel_id        VARCHAR(32) NOT NULL COMMENT '酒店ID',
    room_id         VARCHAR(32) NOT NULL COMMENT '房间ID',
    room_no         VARCHAR(20) NOT NULL COMMENT '房间号',
    action_type     TINYINT NOT NULL COMMENT '操作类型: 1-制作, 2-开门, 3-挂失, 4-补办, 5-延期, 6-注销',
    open_device     VARCHAR(50) COMMENT '开门设备',
    open_result     TINYINT COMMENT '开门结果: 1-成功, 0-失败',
    fail_reason     VARCHAR(100) COMMENT '失败原因',
    operator_id     VARCHAR(32) COMMENT '操作人ID',
    operator_type   TINYINT COMMENT '操作人类型: 1-系统, 2-员工, 3-客人',
    remark          VARCHAR(200) COMMENT '备注',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_card (card_id),
    KEY idx_room (room_id),
    KEY idx_action (action_type),
    KEY idx_create_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门卡操作记录表';

-- ----------------------------
-- 5. 支付模块
-- ----------------------------

CREATE TABLE pay_payment (
    payment_id      VARCHAR(32) NOT NULL COMMENT '支付ID',
    payment_no      VARCHAR(32) NOT NULL COMMENT '支付流水号',
    order_id        VARCHAR(32) NOT NULL COMMENT '订单ID',
    order_no        VARCHAR(32) NOT NULL COMMENT '订单号',
    hotel_id        VARCHAR(32) NOT NULL COMMENT '酒店ID',
    member_id       VARCHAR(32) COMMENT '会员ID',
    business_type   TINYINT NOT NULL COMMENT '业务类型: 1-订单支付, 2-续住支付, 3-押金, 4-消费',
    business_id     VARCHAR(32) COMMENT '关联业务ID',
    payment_type    TINYINT NOT NULL COMMENT '支付方式: 1-微信, 2-支付宝, 3-现金, 4-银行卡, 5-会员余额, 6-其他',
    pay_channel     VARCHAR(50) COMMENT '支付渠道',
    amount          DECIMAL(12,2) NOT NULL COMMENT '支付金额',
    currency        VARCHAR(10) DEFAULT 'CNY',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-待支付, 2-支付中, 3-已支付, 4-已退款, 5-支付失败, 6-已取消',
    trade_no        VARCHAR(64) COMMENT '第三方交易号',
    pay_time        DATETIME COMMENT '支付时间',
    expire_time     DATETIME COMMENT '过期时间',
    refund_id       VARCHAR(32) COMMENT '退款ID',
    refund_amount   DECIMAL(12,2) COMMENT '退款金额',
    refund_time     DATETIME COMMENT '退款时间',
    refund_reason   VARCHAR(200) COMMENT '退款原因',
    client_ip       VARCHAR(50) COMMENT '客户端IP',
    payment_params  TEXT COMMENT '支付参数(JSON)',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (payment_id),
    UNIQUE KEY uk_payment_no (payment_no),
    KEY idx_order (order_id),
    KEY idx_trade_no (trade_no),
    KEY idx_status (status),
    KEY idx_pay_time (pay_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

CREATE TABLE pay_refund (
    refund_id       VARCHAR(32) NOT NULL COMMENT '退款ID',
    refund_no       VARCHAR(32) NOT NULL COMMENT '退款流水号',
    payment_id      VARCHAR(32) NOT NULL COMMENT '原支付ID',
    order_id        VARCHAR(32) NOT NULL COMMENT '订单ID',
    hotel_id        VARCHAR(32) NOT NULL COMMENT '酒店ID',
    refund_type     TINYINT NOT NULL COMMENT '退款类型: 1-整单退款, 2-部分退款',
    refund_amount   DECIMAL(12,2) NOT NULL COMMENT '退款金额',
    refund_reason   VARCHAR(200) NOT NULL COMMENT '退款原因',
    refund_status   TINYINT NOT NULL DEFAULT 1 COMMENT '退款状态: 1-处理中, 2-已完成, 3-失败',
    refund_time     DATETIME COMMENT '退款完成时间',
    operator_id     VARCHAR(32) COMMENT '操作人ID',
    operator_name   VARCHAR(50) COMMENT '操作人',
    remark          VARCHAR(500) COMMENT '备注',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (refund_id),
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_payment (payment_id),
    KEY idx_order (order_id),
    KEY idx_status (refund_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录表';

-- ----------------------------
-- 6. 会员模块
-- ----------------------------

CREATE TABLE mem_member (
    member_id       VARCHAR(32) NOT NULL COMMENT '会员ID',
    member_no       VARCHAR(32) NOT NULL COMMENT '会员号',
    hotel_id        VARCHAR(32) COMMENT '酒店ID',
    openid          VARCHAR(100) COMMENT '微信OpenID',
    unionid         VARCHAR(100) COMMENT '微信UnionID',
    nickname        VARCHAR(50) COMMENT '昵称',
    avatar          VARCHAR(200) COMMENT '头像URL',
    member_name     VARCHAR(50) COMMENT '姓名',
    gender          TINYINT COMMENT '性别: 1-男, 2-女',
    phone           VARCHAR(20) COMMENT '手机号',
    email           VARCHAR(100) COMMENT '邮箱',
    id_card_no      VARCHAR(50) COMMENT '身份证号',
    level_id        VARCHAR(32) NOT NULL COMMENT '会员等级ID',
    level_name      VARCHAR(20) NOT NULL COMMENT '等级名称',
    total_points    BIGINT NOT NULL DEFAULT 0 COMMENT '累计积分',
    available_points BIGINT NOT NULL DEFAULT 0 COMMENT '可用积分',
    total_stay      INT NOT NULL DEFAULT 0 COMMENT '累计入住次数',
    total_nights    INT NOT NULL DEFAULT 0 COMMENT '累计入住晚数',
    total_consume   DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '累计消费金额',
    balance         DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '账户余额',
    first_hotel_id  VARCHAR(32) COMMENT '首次入住酒店',
    first_stay_date DATE COMMENT '首次入住日期',
    last_hotel_id   VARCHAR(32) COMMENT '最近入住酒店',
    last_stay_date  DATE COMMENT '最近入住日期',
    birthday        DATE COMMENT '生日',
    member_source   TINYINT COMMENT '来源: 1-APP注册, 2-线下登记, 3-OTA转化',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    register_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id),
    UNIQUE KEY uk_member_no (member_no),
    UNIQUE KEY uk_phone (phone),
    KEY idx_openid (openid),
    KEY idx_unionid (unionid),
    KEY idx_level (level_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员表';

CREATE TABLE mem_level (
    level_id        VARCHAR(32) NOT NULL COMMENT '等级ID',
    level_code      VARCHAR(20) NOT NULL COMMENT '等级编码',
    level_name      VARCHAR(20) NOT NULL COMMENT '等级名称',
    level_type      TINYINT NOT NULL DEFAULT 1 COMMENT '等级类型: 1-基础, 2-银卡, 3-金卡, 4-白金, 5-钻石',
    min_points      BIGINT NOT NULL DEFAULT 0 COMMENT '最低积分门槛',
    max_points      BIGINT DEFAULT NULL COMMENT '最高积分上限',
    points_rate     DECIMAL(4,2) NOT NULL DEFAULT 1.0 COMMENT '积分倍率',
    discount_rate   DECIMAL(4,2) COMMENT '折扣率',
    advance_hours   INT NOT NULL DEFAULT 0 COMMENT '提前预订小时数',
    late_checkout   INT NOT NULL DEFAULT 0 COMMENT '延迟退房小时数',
    free_upgrade    TINYINT NOT NULL DEFAULT 0 COMMENT '是否可免费升级',
    free_cancel     TINYINT NOT NULL DEFAULT 0 COMMENT '是否可免费取消',
    priority_checkin TINYINT NOT NULL DEFAULT 0 COMMENT '是否有优先入住',
    sort_order      INT NOT NULL DEFAULT 0,
    status          TINYINT NOT NULL DEFAULT 1,
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (level_id),
    UNIQUE KEY uk_level_code (level_code),
    KEY idx_type (level_type),
    KEY idx_points (min_points)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员等级配置表';

CREATE TABLE mem_points_log (
    log_id          VARCHAR(32) NOT NULL COMMENT '记录ID',
    member_id       VARCHAR(32) NOT NULL COMMENT '会员ID',
    hotel_id        VARCHAR(32) COMMENT '酒店ID',
    points          BIGINT NOT NULL COMMENT '积分(正负)',
    balance_before  BIGINT NOT NULL COMMENT '变更前余额',
    balance_after   BIGINT NOT NULL COMMENT '变更后余额',
    business_type   TINYINT NOT NULL COMMENT '业务类型: 1-入住, 2-消费, 3-退款扣减, 4-兑换, 5-过期, 6-调整',
    business_id     VARCHAR(32) COMMENT '关联业务ID',
    order_id        VARCHAR(32) COMMENT '关联订单ID',
    order_no        VARCHAR(32) COMMENT '关联订单号',
    checkin_id      VARCHAR(32) COMMENT '关联入住ID',
    description     VARCHAR(200) COMMENT '说明',
    operator_id     VARCHAR(32) COMMENT '操作人ID',
    expire_date     DATE COMMENT '过期日期',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_member (member_id),
    KEY idx_business (business_type),
    KEY idx_create_time (created_time),
    KEY idx_expire (expire_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分记录表';

-- ----------------------------
-- 7. 系统配置模块
-- ----------------------------

CREATE TABLE sys_config (
    config_id       VARCHAR(32) NOT NULL COMMENT '配置ID',
    hotel_id        VARCHAR(32) NOT NULL COMMENT '酒店ID',
    config_key      VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value    TEXT COMMENT '配置值',
    config_type     VARCHAR(50) COMMENT '配置类型',
    description     VARCHAR(200) COMMENT '说明',
    sort_order      INT NOT NULL DEFAULT 0,
    status          TINYINT NOT NULL DEFAULT 1,
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (config_id),
    UNIQUE KEY uk_hotel_key (hotel_id, config_key),
    KEY idx_hotel (hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店配置表';

CREATE TABLE sys_dict (
    dict_id         VARCHAR(32) NOT NULL COMMENT '字典ID',
    dict_type       VARCHAR(50) NOT NULL COMMENT '字典类型',
    dict_code       VARCHAR(50) NOT NULL COMMENT '字典编码',
    dict_label      VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value      VARCHAR(100) NOT NULL COMMENT '字典值',
    sort_order      INT NOT NULL DEFAULT 0,
    status          TINYINT NOT NULL DEFAULT 1,
    remark          VARCHAR(200) COMMENT '备注',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (dict_id),
    UNIQUE KEY uk_type_code (dict_type, dict_code),
    KEY idx_type (dict_type),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典表';

CREATE TABLE sys_operation_log (
    log_id          VARCHAR(32) NOT NULL COMMENT '日志ID',
    hotel_id        VARCHAR(32) COMMENT '酒店ID',
    module          VARCHAR(50) COMMENT '模块',
    business_type   VARCHAR(50) COMMENT '业务类型',
    business_id     VARCHAR(32) COMMENT '业务ID',
    operator_id     VARCHAR(32) COMMENT '操作人ID',
    operator_name   VARCHAR(50) COMMENT '操作人',
    operator_type   TINYINT COMMENT '操作类型: 1-系统, 2-员工, 3-会员',
    request_method  VARCHAR(10) COMMENT '请求方法',
    request_url     VARCHAR(200) COMMENT '请求URL',
    request_params  TEXT COMMENT '请求参数',
    response_code   VARCHAR(10) COMMENT '响应码',
    response_data   TEXT COMMENT '响应数据',
    error_msg       VARCHAR(500) COMMENT '错误信息',
    client_ip       VARCHAR(50) COMMENT '客户端IP',
    user_agent      VARCHAR(500) COMMENT 'UserAgent',
    execution_time  INT COMMENT '执行时间(ms)',
    created_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_hotel (hotel_id),
    KEY idx_operator (operator_id),
    KEY idx_business (business_type, business_id),
    KEY idx_create_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ----------------------------
-- 8. Saga 持久化表 (缺陷7)
-- ----------------------------

CREATE TABLE saga_log (
    saga_id        VARCHAR(64) NOT NULL COMMENT 'Saga 唯一标识',
    saga_type      VARCHAR(50) NOT NULL COMMENT 'Saga 类型',
    order_id       VARCHAR(32) COMMENT '关联订单号',
    status         VARCHAR(20) NOT NULL COMMENT 'RUNNING/COMPLETED/COMPENSATING/COMPENSATED/FAILED',
    current_step   INT NOT NULL DEFAULT 0 COMMENT '当前步骤序号',
    step_records   TEXT COMMENT '步骤记录 JSON',
    error_message  VARCHAR(500) COMMENT '错误信息',
    retry_count    INT NOT NULL DEFAULT 0 COMMENT '补偿重试次数',
    start_time     DATETIME NOT NULL COMMENT '开始时间',
    update_time    DATETIME NOT NULL COMMENT '更新时间',
    end_time       DATETIME COMMENT '结束时间',
    PRIMARY KEY (saga_id),
    KEY idx_order (order_id),
    KEY idx_status (status),
    KEY idx_update_time (update_time),
    KEY idx_running (status, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Saga 执行日志表';

-- ============================================================
-- 系统用户表（认证）
-- ============================================================

CREATE TABLE sys_user (
    user_id        VARCHAR(32) NOT NULL COMMENT '用户ID',
    username       VARCHAR(50) NOT NULL COMMENT '用户名',
    password       VARCHAR(200) NOT NULL COMMENT '密码(BCrypt)',
    name           VARCHAR(50) NOT NULL COMMENT '姓名',
    role           VARCHAR(50) NOT NULL COMMENT '角色: ROLE_ADMIN, ROLE_STAFF, ROLE_MEMBER',
    phone          VARCHAR(20) COMMENT '手机号',
    email          VARCHAR(100) COMMENT '邮箱',
    avatar         VARCHAR(200) COMMENT '头像',
    status         TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常, 0-禁用',
    last_login_time DATETIME COMMENT '最后登录时间',
    created_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username),
    KEY idx_role (role),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ============================================================
-- 索引优化 (常用查询)
-- ============================================================

CREATE INDEX idx_order_phone_date ON ord_order(contact_phone, check_in_date, check_out_date);
CREATE INDEX idx_room_type_status ON rom_room(hotel_id, room_type_id, room_status, is_active);
CREATE INDEX idx_checkin_hotel_date ON chk_checkin(hotel_id, check_in_time, status);
CREATE INDEX idx_payment_order_status ON pay_payment(order_id, status);
