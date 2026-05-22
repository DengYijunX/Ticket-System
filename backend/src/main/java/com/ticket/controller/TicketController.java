package com.ticket.controller;

import com.ticket.common.JwtUtil;
import com.ticket.common.Result;
import com.ticket.service.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 抢票 Controller ⭐
 *
 * 这是整个系统最核心的接口
 * 高并发压力主要集中在这个接口上
 */
@RestController
@RequestMapping("/api/ticket")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final JwtUtil jwtUtil;

    /**
     * POST /api/ticket/buy
     *
     * 抢票接口
     *
     * 请求头需要带 token（登录后拿到的）：
     *   Authorization: Bearer xxxxx.yyyyy.zzzzz
     *
     * @RequestBody body  {sessionId, categoryId, quantity}
     */
    @PostMapping("/buy")
    public Result<String> buyTicket(
            @RequestBody BuyRequest body,
            HttpServletRequest request) {

        // 从请求头中解析出当前用户ID
        // 简化处理：直接从请求头拿 token（后续可以改成拦截器统一处理）
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.unauthorized("未登录");
        }
        String token = authHeader.substring(7);
        Long userId;
        try {
            userId = jwtUtil.getUserId(token);
        } catch (Exception e) {
            return Result.unauthorized("登录已过期，请重新登录");
        }

        // 调抢票核心逻辑
        return ticketService.buyTicket(userId, body.getSessionId(), body.getCategoryId(), body.getQuantity());
    }

    static class BuyRequest {
        private Long sessionId;
        private Long categoryId;
        private int quantity;
        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}
