package com.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.common.JwtUtil;
import com.ticket.common.Result;
import com.ticket.mapper.UserMapper;
import com.ticket.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户 Service
 *
 * 处理登录、注册、身份验证
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 登录
     *
     * @return 成功返回 JWT token，失败返回错误信息
     */
    public Result<String> login(String username, String password) {
        // 根据用户名查用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
        );
        if (user == null) {
            return Result.badRequest("用户名或密码错误");
        }
        // BCrypt 密文比对
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.badRequest("用户名或密码错误");
        }
        // 检查账号状态
        if (user.getStatus() == 0) {
            return Result.badRequest("账号已被禁用");
        }
        // 生成 JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return Result.success(token);
    }

    /**
     * 注册
     */
    public Result<String> register(String username, String password, String phone) {
        // 检查用户名是否已存在
        User exist = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (exist != null) {
            return Result.badRequest("用户名已存在");
        }
        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));  // BCrypt 加密存储
        user.setPhone(phone);
        user.setRole(0);             // 默认普通用户
        user.setStatus(1);           // 默认启用
        userMapper.insert(user);
        // 注册完直接返回 token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return Result.success(token);
    }
}
