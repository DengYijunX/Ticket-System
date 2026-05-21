package com.ticket.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户表 对应的 Entity
 *
 * @TableName("user")  → 告诉 MyBatis-Plus 这个类对应数据库的 user 表
 * @Data               → Lombok 自动生成 getter/setter/toString（省掉手写）
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)         // 主键，自增
    private Long id;

    private String username;

    private String password;

    private String phone;

    private String email;

    private Integer role;                 // 0=普通用户  1=管理员

    private Integer status;               // 0=禁用  1=启用

    @TableField(fill = FieldFill.INSERT) // 插入时自动填充
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE) // 插入和更新时自动填充
    private LocalDateTime updateTime;
}
