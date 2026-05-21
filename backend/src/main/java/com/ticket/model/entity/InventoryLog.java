package com.ticket.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存变更流水表 Entity
 *
 * 作用：记录每一次库存的扣减和回滚，用于审计和对账
 * 不是核心业务逻辑，但面试时可以讲"数据可追溯"的意识
 */
@Data
@TableName("inventory_log")
public class InventoryLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private Integer changeType;            // 1=扣减 2=回滚 3=增加

    private Integer quantity;

    private String orderNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
