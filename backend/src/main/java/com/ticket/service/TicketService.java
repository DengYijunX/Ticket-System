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
        String rateLimitKey = "rate_limit:user:" + userId;
        if (!redisService.tryAcquire(rateLimitKey, rateLimitBurst, rateLimitMaxPerSecond)) {
            log.warn("用户 {} 被限流", userId);
            return Result.badRequest("操作太频繁，请稍后再试");
        }

        // ---- 第2步：幂等性校验，防止同一用户对同一票档重复提交 ----
        // 用 SETNX 设一个 10 秒的标记，如果已存在则拒绝
        // 与分布式锁不同：标记不主动释放，靠 TTL 过期，保证 10 秒内不会重复下单
        String dedupKey = "dedup:order:" + userId + ":" + categoryId;
        if (!redisService.tryLock(dedupKey, 10)) {
            return Result.badRequest("请勿重复提交");
        }

        // ---- 第3步：检查是否已售罄 ----
        if (redisService.isSoldOut(categoryId)) {
            redisService.unlock(dedupKey);
            return Result.badRequest("已售罄");
        }

        // ---- 第4步：Redis 原子扣库存 ----
        Long result = redisService.deductStock(categoryId, quantity);

        if (result == -1) {
            redisService.unlock(dedupKey);
            return Result.badRequest("演出尚未开售");
        }
        if (result == 0) {
            redisService.markSoldOut(categoryId);
            redisService.unlock(dedupKey);
            return Result.badRequest("库存不足");
        }

        // ---- 第5步：异步下单 ----
        orderService.createOrderAsync(userId, sessionId, categoryId, quantity);
        return Result.success("抢票成功，订单处理中");
    }
}
