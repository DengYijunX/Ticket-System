package com.ticket.service;

import com.ticket.mapper.TicketCategoryMapper;
import com.ticket.model.entity.TicketCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用启动时自动将 MySQL 库存预热到 Redis
 *
 * 为什么需要预热？
 * 抢票接口的 Lua 脚本直接从 Redis 读库存，如果 key 不存在返回 -1 = "演出尚未开售"
 * 每次 Redis 重启后数据丢失，不做预热整个系统不可用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockPreheatRunner implements CommandLineRunner {

    private final TicketCategoryMapper categoryMapper;
    private final RedisService redisService;

    @Override
    public void run(String... args) {
        List<TicketCategory> categories = categoryMapper.selectList(null);
        for (TicketCategory cat : categories) {
            redisService.preheatStock(cat.getId(), cat.getRemainStock());
        }
        log.info("库存预热完成: {} 个票档加载到 Redis", categories.size());
    }
}
