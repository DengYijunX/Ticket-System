package com.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.mapper.ShowMapper;
import com.ticket.mapper.ShowSessionMapper;
import com.ticket.mapper.TicketCategoryMapper;
import com.ticket.model.entity.Show;
import com.ticket.model.entity.ShowSession;
import com.ticket.model.entity.TicketCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 演出 Service — 多级缓存策略
 *
 * 查询路径：Redis → MySQL → 回写 Redis
 * 缓存 TTL：演出 5min / 场次+票档 10min（读多写少，短 TTL 够用）
 * 面试可讲：热点数据缓存、缓存穿透保护、缓存一致性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowMapper showMapper;
    private final ShowSessionMapper sessionMapper;
    private final TicketCategoryMapper categoryMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration SHOW_TTL = Duration.ofMinutes(5);
    private static final Duration SESSION_TTL = Duration.ofMinutes(10);

    // ========== 演出 ==========

    public List<Show> getAvailableShows() {
        String key = "cache:show:list";
        List<Show> cached = getCachedList(key, Show.class);
        if (cached != null) return cached;

        List<Show> shows = showMapper.selectList(
                new LambdaQueryWrapper<Show>()
                        .eq(Show::getStatus, 1)
                        .le(Show::getSaleStart, LocalDateTime.now())
                        .orderByDesc(Show::getCreateTime)
        );
        // 填充场次数和价格区间
        for (Show show : shows) {
            List<ShowSession> sessions = getSessionsByShowId(show.getId());
            show.setSessionCount(sessions.size());
            if (!sessions.isEmpty()) {
                List<TicketCategory> cats = getCategoriesBySessionId(sessions.get(0).getId());
                if (!cats.isEmpty()) {
                    show.setMaxPrice(cats.get(0).getPrice()); // 价格从高到低排，第一个是最高价
                    show.setMinPrice(cats.get(cats.size() - 1).getPrice());
                }
            }
        }
        setCache(key, shows, SHOW_TTL);
        return shows;
    }

    public Show getShowById(Long showId) {
        String key = "cache:show:" + showId;
        Show cached = getCached(key, Show.class);
        if (cached != null) return cached;

        Show show = showMapper.selectById(showId);
        if (show != null) setCache(key, show, SHOW_TTL);
        return show;
    }

    // ========== 场次 ==========

    public List<ShowSession> getSessionsByShowId(Long showId) {
        String key = "cache:session:" + showId;
        List<ShowSession> cached = getCachedList(key, ShowSession.class);
        if (cached != null) return cached;

        List<ShowSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ShowSession>()
                        .eq(ShowSession::getShowId, showId)
                        .orderByAsc(ShowSession::getStartTime)
        );
        setCache(key, sessions, SESSION_TTL);
        return sessions;
    }

    // ========== 票档 ==========

    public List<TicketCategory> getCategoriesBySessionId(Long sessionId) {
        String key = "cache:category:" + sessionId;
        List<TicketCategory> cached = getCachedList(key, TicketCategory.class);
        if (cached != null) return cached;

        List<TicketCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<TicketCategory>()
                        .eq(TicketCategory::getSessionId, sessionId)
                        .orderByDesc(TicketCategory::getPrice)
        );
        setCache(key, categories, SESSION_TTL);
        return categories;
    }

    // ========== 缓存工具 ==========

    private <T> T getCached(String key, Class<T> clazz) {
        Object val = redisTemplate.opsForValue().get(key);
        if (val == null) return null;
        // GenericJackson2JsonRedisSerializer 反序列化时可能返回 LinkedHashMap
        if (clazz.isInstance(val)) return clazz.cast(val);
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(val), clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getCachedList(String key, Class<T> clazz) {
        Object val = redisTemplate.opsForValue().get(key);
        if (val instanceof List<?> list && !list.isEmpty()) {
            if (clazz.isInstance(list.get(0))) return (List<T>) list;
        }
        return null;
    }

    private void setCache(String key, Object data, Duration ttl) {
        redisTemplate.opsForValue().set(key, data, ttl);
        log.debug("缓存写入: {}", key);
    }
}
