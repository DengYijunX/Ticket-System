package com.ticket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 操作 Service
 *
 * 封装了抢票系统需要的 Redis 操作：
 * 1. 库存预热和扣减（Lua 脚本保证原子性）
 * 2. 限流（令牌桶）
 * 3. 分布式锁
 */
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // ========== 库存相关 ==========

    /**
     * 库存预热：将数据库中的库存加载到 Redis
     * 使用 String 结构，key = "stock:{categoryId}", value = 剩余库存数
     */
    public void preheatStock(Long categoryId, Integer stock) {
        String key = stockKey(categoryId);
        redisTemplate.opsForValue().set(key, stock);
    }

    /**
     * Lua 脚本：原子扣减库存
     *
     * 为什么用 Lua？
     * 因为 Redis 扣库存需要两步：查库存 → 扣减
     * 不用 Lua 的话，这两步之间有并发问题（多个请求同时查到的库存都是1，都扣，就超卖了）
     * Lua 脚本在 Redis 服务端执行，整个脚本是原子的
     *
     * KEYS[1] = stock:123      ← 库存 key
     * ARGV[1] = 1             ← 扣减数量
     *
     * 返回值：1=成功  0=库存不足  -1=key不存在
     */
    public Long deductStock(Long categoryId, int quantity) {
        String lua = """
                local key = KEYS[1]
                local qty = tonumber(ARGV[1])
                local stock = redis.call('GET', key)
                if not stock then
                    return -1  -- key 不存在（可能未预热）
                end
                stock = tonumber(stock)
                if stock >= qty then
                    redis.call('DECRBY', key, qty)
                    return 1   -- 扣减成功
                else
                    return 0   -- 库存不足
                end
                """;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(lua, Long.class);
        return redisTemplate.execute(script, List.of(stockKey(categoryId)), String.valueOf(quantity));
    }

    /**
     * 库存回滚（订单取消时调用）
     */
    public void restoreStock(Long categoryId, int quantity) {
        redisTemplate.opsForValue().increment(stockKey(categoryId), quantity);
    }

    /**
     * 查询 Redis 中的剩余库存
     */
    public Integer getStock(Long categoryId) {
        Object val = redisTemplate.opsForValue().get(stockKey(categoryId));
        return val != null ? (Integer) val : 0;
    }

    // ========== 限流相关（令牌桶） ==========

    /**
     * 令牌桶限流
     *
     * 概念：想象一个桶，每秒往桶里放 N 个令牌
     * 请求来了就拿走一个令牌，有令牌就放行，没有就拒绝
     * 特点是：能应对突发流量（桶里攒的令牌可以一次性用完）
     *
     * @param key    限流 key（可按用户/IP/接口维度）
     * @param maxPermits  桶容量（最多能攒多少令牌）
     * @param rate        每秒放入的令牌数
     * @return true=放行  false=被限流
     */
    public boolean tryAcquire(String key, int maxPermits, int rate) {
        // 用 Redis Lua 实现令牌桶（简化版）
        // 实际可以用 Redisson 的 RRateLimiter，但 Lua 实现更好理解
        String lua = """
                local key = KEYS[1]
                local now = tonumber(ARGV[1])
                local max = tonumber(ARGV[2])
                local rate = tonumber(ARGV[3])
                \s
                local last = redis.call('HGET', key, 'last_time') or now
                local tokens = tonumber(redis.call('HGET', key, 'tokens') or max)
                \s
                local elapsed = now - tonumber(last)
                tokens = math.min(max, tokens + elapsed * rate)
                \s
                if tokens >= 1 then
                    tokens = tokens - 1
                    redis.call('HSET', key, 'tokens', tokens, 'last_time', now)
                    return 1
                else
                    return 0
                end
                """;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(lua, Long.class);
        Long result = redisTemplate.execute(script,
                List.of(key),
                String.valueOf(System.currentTimeMillis() / 1000),
                String.valueOf(maxPermits),
                String.valueOf(rate));
        return Long.valueOf(1).equals(result);
    }

    // ========== 分布式锁（简化版，正式环境用 Redisson） ==========

    /**
     * 获取分布式锁
     * 用 SETNX + 过期时间实现，防止多个服务器同时处理同一条数据
     */
    public boolean tryLock(String key, long expireSeconds) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, "1", expireSeconds, TimeUnit.SECONDS)
        );
    }

    /**
     * 释放分布式锁
     */
    public void unlock(String key) {
        redisTemplate.delete(key);
    }

    // ========== 内存标记（可选优化） ==========

    /**
     * 标记某个档次的票已售罄
     * 这样下次请求不用查 Redis 就能直接返回售罄
     */
    public void markSoldOut(Long categoryId) {
        redisTemplate.opsForValue().set(soldOutKey(categoryId), "1", 1, TimeUnit.HOURS);
    }

    public boolean isSoldOut(Long categoryId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(soldOutKey(categoryId)));
    }

    // ========== Key 管理 ==========

    private String stockKey(Long categoryId) {
        return "stock:" + categoryId;
    }

    private String soldOutKey(Long categoryId) {
        return "sold_out:" + categoryId;
    }
}
