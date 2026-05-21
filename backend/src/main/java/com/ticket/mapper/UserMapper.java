package com.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.model.entity.User;

/**
 * User 表的 Mapper 接口
 *
 * 继承 BaseMapper<User> 后，自动获得 insert/delete/update/select 等方法
 * 如果有特殊查询（比如复杂的多表联查），可以在这里加自定义方法
 */
public interface UserMapper extends BaseMapper<User> {
}
