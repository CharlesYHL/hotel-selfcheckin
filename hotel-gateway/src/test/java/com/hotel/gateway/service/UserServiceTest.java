package com.hotel.gateway.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.gateway.mapper.UserMapper;
import com.hotel.gateway.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService 单元测试")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User buildUser(String userId, String username, String rawPassword, String role) {
        User u = new User();
        u.setUserId(userId);
        u.setUsername(username);
        u.setPassword(new BCryptPasswordEncoder().encode(rawPassword));
        u.setName("测试用户");
        u.setRole(role);
        u.setStatus(1);
        return u;
    }

    @Test
    @DisplayName("正确用户名密码登录成功")
    void authenticate_Success() {
        User u = buildUser("U001", "admin", "admin123", "ROLE_ADMIN");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);

        Optional<User> result = userService.authenticate("admin", "admin123");

        assertTrue(result.isPresent());
        assertEquals("U001", result.get().getUserId());
        assertEquals("ROLE_ADMIN", result.get().getRole());
    }

    @Test
    @DisplayName("错误密码登录失败")
    void authenticate_WrongPassword() {
        User u = buildUser("U001", "admin", "admin123", "ROLE_ADMIN");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(u);

        Optional<User> result = userService.authenticate("admin", "wrong");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("不存在的用户登录失败")
    void authenticate_UserNotFound() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        Optional<User> result = userService.authenticate("ghost", "any");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("通过 userId 查找用户")
    void findById_Success() {
        User u = buildUser("U002", "member", "member123", "ROLE_MEMBER");
        when(userMapper.selectById("U002")).thenReturn(u);

        Optional<User> result = userService.findById("U002");

        assertTrue(result.isPresent());
        assertEquals("member", result.get().getUsername());
    }

    @Test
    @DisplayName("查找不存在的 userId 返回空")
    void findById_NotFound() {
        when(userMapper.selectById("U999")).thenReturn(null);

        Optional<User> result = userService.findById("U999");

        assertTrue(result.isEmpty());
    }
}
