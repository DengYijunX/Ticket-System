package com.ticket.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票价档次表 Entity
 *
 * 一个场次下有多个票价档次：VIP(1880元, 100张)、一等座(1280元, 200张)
 * remain_stock 是扣库存的核心字段
 */
@Data
@TableName("ticket_category")
public class TicketCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;                // 关联场次ID

    private String name;                   // "VIP" / "一等座" / "二等座"

    private BigDecimal price;              // 价格

    private Integer totalStock;            // 总库存

    private Integer remainStock;           // 剩余库存（秒杀的核心竞争资源）

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
