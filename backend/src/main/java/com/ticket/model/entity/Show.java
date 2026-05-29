package com.ticket.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 演出表 Entity
 */
@Data
@TableName("`show`")  // show 是 MySQL 关键字，所以加反引号
public class Show {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private String venue;

    private String coverUrl;

    private Integer status;                // 0=下架 1=上架

    private LocalDateTime saleStart;       // 开售时间

    private LocalDateTime saleEnd;         // 停售时间

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 场次数量（非数据库字段） */
    @TableField(exist = false)
    private Integer sessionCount;

    /** 最低票价（非数据库字段） */
    @TableField(exist = false)
    private java.math.BigDecimal minPrice;

    /** 最高票价（非数据库字段） */
    @TableField(exist = false)
    private java.math.BigDecimal maxPrice;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
