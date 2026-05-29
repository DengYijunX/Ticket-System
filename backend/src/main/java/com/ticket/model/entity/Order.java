package com.ticket.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单表 Entity
 *
 * status 状态机：
 *   0=待支付 → 1=已支付 → 4=已完成
 *           ↘ 2=已取消
 *           3=已退款
 */
@Data
@TableName("`order`")  // order 是 MySQL 关键字，加反引号
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;                // 订单号（唯一）

    private Long userId;

    private Long sessionId;

    private Long categoryId;

    private Integer quantity;              // 购买数量

    private BigDecimal totalAmount;        // 总金额

    private Integer status;                // 0=待支付 1=已支付 2=已取消 3=已退款 4=已完成

    /** 票档名称（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private String categoryName;

    private LocalDateTime payTime;         // 支付时间

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
