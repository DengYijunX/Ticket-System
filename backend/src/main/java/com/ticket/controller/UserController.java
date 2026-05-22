package com.ticket.controller;

import com.ticket.common.Result;
import com.ticket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户 Controller
 *
 * @RestController     → 这个类处理 HTTP 请求，返回 JSON（不是页面）
 * @RequestMapping     → 这个类的所有接口都以 /api/user 开头
 * @RequiredArgsConstructor → 用构造方法注入 UserService（Spring 推荐的注入方式）
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * POST /api/user/login
     *
     * 登录接口
     * @RequestBody 把请求体里的 JSON 转成 LoginRequest 对象
     * 前端传：{"username":"admin", "password":"123456"}
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginRequest req) {
        return userService.login(req.getUsername(), req.getPassword());
    }

    /**
     * POST /api/user/register
     *
     * 注册接口
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterRequest req) {
        return userService.register(req.getUsername(), req.getPassword(), req.getPhone());
    }

    // ---- 内部类：请求参数对象 ----

    /**
     * 为什么用内部类？
     * 每个接口需要的参数不一样，单独定义类更清晰
     * 如果参数少也可以直接用 @RequestParam 一个个写，但参数多了代码很难看
     */
    static class LoginRequest {
        private String username;
        private String password;
        // getter/setter（必须要有，否则 Spring 没法把 JSON 转成对象）
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    static class RegisterRequest {
        private String username;
        private String password;
        private String phone;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
}
