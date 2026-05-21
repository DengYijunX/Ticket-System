package com.ticket.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 场次表 Entity
 *
 * 一个演出有多个场次：比如周杰伦深圳站有6月15日和6月16日两场
 */
@Data
@TableName("show_session")
public class ShowSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long showId;                   // 关联的演出ID

    private String name;                   // 场次名称："2026-06-15 19:30场"

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
