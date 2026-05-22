package com.ticket.controller;

import com.ticket.common.JwtUtil;
import com.ticket.common.Result;
import com.ticket.model.entity.Order;
import com.ticket.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单 Controller
 *
 * 订单查询相关接口
 * 前端抢票后轮询这个接口查订单状态
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    /**
     * GET /api/order/status?orderNo=202605221234560001
     *
     * 根据订单号查询订单状态
     * 前端抢票后轮询这个接口：待支付 → 已支付 → 已完成
     */
    @GetMapping("/status")
    public Result<Order> getStatus(@RequestParam String orderNo) {
        Order order = orderService.getByOrderNo(orderNo);
        if (order == null) {
            return Result.badRequest("订单不存在");
        }
        return Result.success(order);
    }

    /**
     * GET /api/order/list
     *
     * 获取当前登录用户的所有订单
     */
    @GetMapping("/list")
    public Result<List<Order>> listOrders(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.unauthorized("未登录");
        }
        String token = authHeader.substring(7);
        Long userId = jwtUtil.getUserId(token);
        return Result.success(orderService.getUserOrders(userId));
    }
}
