package com.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.mapper.ShowMapper;
import com.ticket.mapper.ShowSessionMapper;
import com.ticket.mapper.TicketCategoryMapper;
import com.ticket.model.entity.Show;
import com.ticket.model.entity.ShowSession;
import com.ticket.model.entity.TicketCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 演出 Service
 *
 * 查询演出列表、场次、票价等展示信息（读多写少）
 * 这部分不走 Redis，直接查 MySQL（因为查询频率远低于抢票）
 */
@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowMapper showMapper;
    private final ShowSessionMapper sessionMapper;
    private final TicketCategoryMapper categoryMapper;

    /**
     * 获取所有正在售卖中的演出
     */
    public List<Show> getAvailableShows() {
        return showMapper.selectList(
                new LambdaQueryWrapper<Show>()
                        .eq(Show::getStatus, 1)                               // 已上架
                        .le(Show::getSaleStart, LocalDateTime.now())           // 已到开售时间
                        .orderByDesc(Show::getCreateTime)
        );
    }

    /**
     * 获取演出详情（基本信息）
     */
    public Show getShowById(Long showId) {
        return showMapper.selectById(showId);
    }

    /**
     * 获取某个演出的所有场次
     */
    public List<ShowSession> getSessionsByShowId(Long showId) {
        return sessionMapper.selectList(
                new LambdaQueryWrapper<ShowSession>()
                        .eq(ShowSession::getShowId, showId)
                        .orderByAsc(ShowSession::getStartTime)
        );
    }

    /**
     * 获取某个场次的所有票价档次（包含库存信息）
     */
    public List<TicketCategory> getCategoriesBySessionId(Long sessionId) {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<TicketCategory>()
                        .eq(TicketCategory::getSessionId, sessionId)
                        .orderByDesc(TicketCategory::getPrice)
        );
    }
}
