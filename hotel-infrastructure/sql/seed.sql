-- ============================================================
-- 酒店自助入住系统 - 初始化数据
-- ============================================================

USE hotel_db;

-- 酒店
INSERT INTO sys_hotel (hotel_id, hotel_code, hotel_name, province, city, district, address, contact_phone, star_level)
VALUES ('H001', 'HZ001', '杭州西湖希尔顿酒店', '浙江省', '杭州市', '西湖区', '西湖区文一路100号', '0571-88888888', 5);

-- 楼层
INSERT INTO sys_floor (floor_id, hotel_id, floor_no, floor_name, floor_type, sort_order)
VALUES
    ('F01', 'H001', '1', '1楼', 1, 1),
    ('F02', 'H001', '2', '2楼', 1, 2),
    ('F03', 'H001', '3', '3楼', 1, 3),
    ('F04', 'H001', '5', '5楼', 1, 4),
    ('F05', 'H001', '6', '6楼', 1, 5);

-- 房型
INSERT INTO sys_room_type (room_type_id, hotel_id, room_type_code, room_type_name, base_capacity, max_capacity, bed_type, room_area, max_rooms)
VALUES
    ('RT01', 'H001', 'STD', '标准间', 2, 2, '大床', 25.00, 50),
    ('RT02', 'H001', 'DLX', '豪华间', 2, 3, '大床', 35.00, 30),
    ('RT03', 'H001', 'TWN', '双床间', 2, 4, '双床', 30.00, 40),
    ('RT04', 'H001', 'STE', '套房', 2, 4, '大床', 60.00, 10);

-- 房间
INSERT INTO rom_room (room_id, room_no, hotel_id, room_type_id, floor_id, floor_no, direction, room_status, max_guest, is_active)
VALUES
    ('R101', '101', 'H001', 'RT01', 'F01', '1', '南', 1, 2, 1),
    ('R102', '102', 'H001', 'RT01', 'F01', '1', '南', 1, 2, 1),
    ('R103', '103', 'H001', 'RT02', 'F01', '1', '南', 1, 3, 1),
    ('R201', '201', 'H001', 'RT03', 'F02', '2', '东', 1, 4, 1),
    ('R202', '202', 'H001', 'RT03', 'F02', '2', '东', 1, 4, 1),
    ('R301', '301', 'H001', 'RT04', 'F03', '3', '南', 1, 4, 1),
    ('R501', '501', 'H001', 'RT02', 'F04', '5', '北', 1, 3, 1),
    ('R601', '601', 'H001', 'RT04', 'F05', '6', '南', 1, 4, 1);

-- 房价
INSERT INTO rom_price (price_id, room_type_id, hotel_id, date_type, rack_rate, sell_rate, member_rate, vip_rate, start_date, end_date)
VALUES
    ('P01', 'RT01', 'H001', 1, 399.00, 299.00, 279.00, 259.00, '2024-01-01', '2026-12-31'),
    ('P02', 'RT02', 'H001', 1, 599.00, 499.00, 469.00, 439.00, '2024-01-01', '2026-12-31'),
    ('P03', 'RT03', 'H001', 1, 499.00, 399.00, 379.00, 359.00, '2024-01-01', '2026-12-31'),
    ('P04', 'RT04', 'H001', 1, 999.00, 899.00, 859.00, 799.00, '2024-01-01', '2026-12-31');

-- 会员等级
INSERT INTO mem_level (level_id, level_code, level_name, level_type, min_points, max_points, points_rate, advance_hours, late_checkout, free_upgrade)
VALUES
    ('LV1', 'NORMAL', '普通会员', 1, 0, 999, 1.0, 0, 0, 0),
    ('LV2', 'SILVER', '银卡会员', 2, 1000, 4999, 1.2, 12, 1, 0),
    ('LV3', 'GOLD', '金卡会员', 3, 5000, 9999, 1.5, 24, 2, 1),
    ('LV4', 'PLATINUM', '白金会员', 4, 10000, 29999, 2.0, 48, 4, 1),
    ('LV5', 'DIAMOND', '钻石会员', 5, 30000, NULL, 3.0, 72, 6, 1);

-- 字典数据
INSERT INTO sys_dict (dict_id, dict_type, dict_code, dict_label, dict_value, sort_order)
VALUES
    ('D01', 'room_status', '1', '空房', 'VACANT', 1),
    ('D02', 'room_status', '2', '占用', 'OCCUPIED', 2),
    ('D03', 'room_status', '3', '维修', 'MAINTENANCE', 3),
    ('D04', 'room_status', '4', '清洁', 'CLEANING', 4),
    ('D05', 'order_status', '1', '待支付', 'PENDING', 1),
    ('D06', 'order_status', '2', '已支付', 'PAID', 2),
    ('D07', 'order_status', '3', '已排房', 'ASSIGNED', 3),
    ('D08', 'order_status', '4', '已入住', 'CHECKED_IN', 4),
    ('D09', 'payment_type', '1', '微信', 'WECHAT', 1),
    ('D10', 'payment_type', '2', '支付宝', 'ALIPAY', 2),
    ('D11', 'payment_type', '3', '现金', 'CASH', 3);

-- 系统用户
-- 密码均为 BCrypt 加密: admin123 / member123 / staff123
INSERT INTO sys_user (user_id, username, password, name, role, phone, status)
VALUES
    ('U001', 'admin', '$2a$10$IVDDpl0a3/ywQbL4R/ollOJkYjborI1i9GTXTQK6TDtE/f/bnXM1i', '系统管理员', 'ROLE_ADMIN', '13800000001', 1),
    ('U002', 'member', '$2a$10$grgU86zaghrNlYLkfCJ0DOpwfs2H2t70U6YaE64/4PqEFlXeHLSZu', '张三', 'ROLE_MEMBER', '13800000002', 1),
    ('U003', 'staff', '$2a$10$y3aTL1zC4MUjG6Ks2ZU96.rbLgcZ7eUfVV555A6yEKu6o4deMqmHS', '前台小王', 'ROLE_STAFF', '13800000003', 1);
