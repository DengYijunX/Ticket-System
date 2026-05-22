package com.ticket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 操作 Service
 *
 * 封装了抢票系统需要的 Redis 操作：
 * 1. 库存预热和扣减（Lua 脚本保证原子性）
 * 2. 限流（令牌桶）
 * 3. 分布式锁
 *
 * 为什么用 StringRedisTemplate 而不是 RedisTemplate<Object>？
 * StringRedisTemplate 不做 JSON 序列化，Lua 脚本的参数不会被包引号
 */
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate stringRedisTemplate;

    // ========== 库存相关 ==========

    /**
     * 库存预热：将数据库中的库存加载到 Redis
     */
    public void preheatStock(Long categoryId, Integer stock) {
        stringRedisTemplate.opsForValue().set(stockKey(categoryId), String.valueOf(stock));
    }

    /**
     * Lua 脚本：原子扣减库存
     *
     * 返回值：1=成功  0=库存不足  -1=key不存在
     */
    public Long deductStock(Long categoryId, int quantity) {
        String lua = """
                local key = KEYS[1]
                local qty = tonumber(ARGV[1])
                local stock = redis.call('GET', key)
                if not stock then
                    return -1
                end
                stock = tonumber(stock)
                if stock >= qty then
                    redis.call('DECRBY', key, qty)
                    return 1
                else
                    return 0
                end
                """;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(lua, Long.class);
        return stringRedisTemplate.execute(script, List.of(stockKey(categoryId)), String.valueOf(quantity));
    }

    /**
     * 库存回滚（订单取消时调用）
     */
    public void restoreStock(Long categoryId, int quantity) {
        stringRedisTemplate.opsForValue().increment(stockKey(categoryId), quantity);
    }

    /**
     * 查询 Redis 中的剩余库存
     */
    public Integer getStock(Long categoryId) {
        String val = stringRedisTemplate.opsForValue().get(stockKey(categoryId));
        return val != null ? Integer.parseInt(val) : 0;
    }

    // ========== 限流相关（令牌桶） ==========

    /**
     * 令牌桶限流
     */
    public boolean tryAcquire(String key, int maxPermits, int rate) {
        String lua = """
                local key = KEYS[1]
                local now = tonumber(ARGV[1])
                local max = tonumber(ARGV[2])
                local rate = tonumber(ARGV[3])

                local last_str = redis.call('HGET', key, 'last_time')
                local tokens_str = redis.call('HGET', key, 'tokens')

                local last = tonumber(last_str) or now
                local tokens = tonumber(tokens_str) or max

                local elapsed = now - last
                tokens = math.min(max, tokens + elapsed * rate)

                if tokens >= 1 then
                    tokens = tokens - 1
                    redis.call('HSET', key, 'tokens', tokens, 'last_time', now)
                    return 1
                else
                    return 0
                end
                """;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(lua, Long.class);
        Long result = stringRedisTemplate.execute(script,
                List.of(key),
                String.valueOf(System.currentTimeMillis() / 1000),
                String.valueOf(maxPermits),
                String.valueOf(rate));
        return Long.valueOf(1).equals(result);
    }

    // ========== 分布式锁 ==========

    public boolean tryLock(String key, long expireSeconds) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue().setIfAbsent(key, "1", expireSeconds, TimeUnit.SECONDS)
        );
    }

    public void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    // ========== 内存标记 ==========

    public void markSoldOut(Long categoryId) {
        stringRedisTemplate.opsForValue().set(soldOutKey(categoryId), "1", 1, TimeUnit.HOURS);
    }

    public boolean isSoldOut(Long categoryId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(soldOutKey(categoryId)));
    }

    // ========== Key 管理 ==========

    private String stockKey(Long categoryId) {
        return "stock:" + categoryId;
    }

    private String soldOutKey(Long categoryId) {
        return "sold_out:" + categoryId;
    }
}
