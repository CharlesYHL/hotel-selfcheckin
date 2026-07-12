package com.hotel.gateway.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.gateway.mapper.UserMapper;
import com.hotel.gateway.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Optional<User> authenticate(String username, String password) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getStatus, 1)
        );
        if (user == null) {
            return Optional.empty();
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    public Optional<User> findById(String userId) {
        return Optional.ofNullable(userMapper.selectById(userId));
    }
}