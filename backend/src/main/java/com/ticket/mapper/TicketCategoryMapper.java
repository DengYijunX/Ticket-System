package com.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.model.entity.TicketCategory;

/**
 * 注意：这个 Mapper 会有自定义方法
 * 秒杀场景下需要操作的 SQL 不止基础 CRUD
 */
public interface TicketCategoryMapper extends BaseMapper<TicketCategory> {

    /**
     * 扣减库存（乐观锁实现）
     * SQL: UPDATE ticket_category SET remain_stock = remain_stock - 1
     *      WHERE id = ? AND remain_stock > 0
     *
     * 返回受影响的行数：>0 表示扣成功，=0 表示库存不足
     * 这个 SQL 需要写在 XML 里，后面会配
     */
    int deductStock(Long categoryId);
}
