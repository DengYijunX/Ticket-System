package com.ticket.common;

/**
 * 订单状态常量
 *
 * 把所有状态数字定义在这里，业务代码里用 OrderStatus.PENDING_PAYMENT 代替 0
 * 好处：别人看代码不用猜 0/1/2 代表什么
 */
public interface OrderStatus {

    /** 待支付（已锁定库存，等待用户付款） */
    int PENDING_PAYMENT = 0;

    /** 已支付 */
    int PAID = 1;

    /** 已取消（超时未支付或用户主动取消） */
    int CANCELLED = 2;

    /** 已退款 */
    int REFUNDED = 3;

    /** 已完成（已出票/已消费） */
    int COMPLETED = 4;
}
