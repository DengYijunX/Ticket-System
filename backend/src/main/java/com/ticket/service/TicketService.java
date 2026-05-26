package com.ticket.service;

import com.ticket.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 抢票核心 Service
 *
 * 这是整个系统最关键的类，处理高并发下的抢票逻辑
 * 核心思路：把写操作尽量挡在 Redis 层，MySQL 只做最终落盘
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final RedisService redisService;
    private final OrderService orderService;

    @Value("${ticket.rate-limit.max-per-second}")
    private int rateLimitMaxPerSecond;
    @Value("${ticket.rate-limit.burst}")
    private int rateLimitBurst;

    /**
     * 抢票入口
     *
     * 执行顺序：
     *   1. 限流 — 防止刷票
     *   2. Redis 原子扣库存 — 扛住高并发
     *   3. 发 MQ 消息 — 异步下单
     *   4. 返回排队结果给前端
     *
     * @param userId     当前登录用户ID
     * @param sessionId  场次ID
     * @param categoryId 票价档次ID
     * @param quantity   购买数量
     * @return 抢票结果
     */
    public Result<String> buyTicket(Long userId, Long sessionId, Long categoryId, int quantity) {

        // ---- 第1步：限流 ----
        // 按用户限流：同一个用户每秒最多3次请求
        String rateLimitKey = "rate_limit:user:" + userId;
        if (!redisService.tryAcquire(rateLimitKey, rateLimitBurst, rateLimitMaxPerSecond)) {
            log.warn("用户 {} 被限流", userId);
            return Result.badRequest("操作太频繁，请稍后再试");
        }

        // ---- 第2步：检查是否已售罄（内存标记，快速拒绝） ----
        if (redisService.isSoldOut(categoryId)) {
            return Result.badRequest("已售罄");
        }

        // ---- 第3步：Redis 原子扣库存 ----
        // 这一步是扛住高并发的关键
        // Lua 脚本在 Redis 服务端原子执行，不怕并发
        Long result = redisService.deductStock(categoryId, quantity);

        if (result == -1) {
            // 库存 key 不存在，可能还没预热
            return Result.badRequest("演出尚未开售");
        }
        if (result == 0) {
            // 库存不足，标记售罄避免后续无效请求
            redisService.markSoldOut(categoryId);
            return Result.badRequest("库存不足");
        }

        // ---- 第4步：Redis 扣成功 → 异步下单 ----
        String orderNo = orderService.createOrderAsync(userId, sessionId, categoryId, quantity);

        // 返回订单号，前端可据此查询订单状态
        return Result.success(orderNo);
    }
}
