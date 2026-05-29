package com.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.common.OrderStatus;
import com.ticket.mapper.InventoryLogMapper;
import com.ticket.mapper.OrderMapper;
import com.ticket.mapper.TicketCategoryMapper;
import com.ticket.model.entity.InventoryLog;
import com.ticket.model.entity.Order;
import com.ticket.model.entity.TicketCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * 订单 Service
 *
 * 职责：
 * 1. 异步创建订单（MQ 消费者调用 createOrder）
 * 2. 订单查询
 * 3. 订单超时取消 + 库存回滚
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final RabbitTemplate rabbitTemplate;
    private final RedisService redisService;
    private final TicketCategoryMapper categoryMapper;
    private final InventoryLogMapper inventoryLogMapper;

    // ========== 下单 ==========

    /**
     * 异步下单：把下单请求发到 MQ，让消费者慢慢处理
     *
     * 为什么不直接写数据库？
     * 抢票高峰期每秒几千个请求，MySQL 扛不住
     * MQ 像一个大坝，拦住洪峰，让下游按自己的节奏处理
     */
    public String createOrderAsync(Long userId, Long sessionId, Long categoryId, int quantity) {
        // 预生成订单号，排在 MQ 消息前面生成
        String orderNo = generateOrderNo();
        String message = orderNo + "," + userId + "," + sessionId + "," + categoryId + "," + quantity;
        rabbitTemplate.convertAndSend("order.exchange", "order.create", message);
        log.info("下单消息已发送到 MQ: {}", message);
        return orderNo;
    }

    /**
     * 实际创建订单（由 MQ 消费者调用）
     *
     * @Transactional 表示这个方法里的数据库操作是一个事务
     * 要么全部成功，要么全部回滚（不会出现扣了库存但没生成订单的情况）
     */
    @Transactional
    public Order createOrder(String orderNo, Long userId, Long sessionId, Long categoryId, int quantity) {

        // 查票价
        TicketCategory category = categoryMapper.selectById(categoryId);
        BigDecimal unitPrice = category != null ? category.getPrice() : BigDecimal.ZERO;

        // 创建订单实体
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setSessionId(sessionId);
        order.setCategoryId(categoryId);
        order.setQuantity(quantity);
        order.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        order.setStatus(OrderStatus.PENDING_PAYMENT);    // 刚创建：待支付

        orderMapper.insert(order);

        // 同步扣减 MySQL 库存
        int affected = categoryMapper.deductStock(categoryId);
        if (affected == 0) {
            throw new RuntimeException("MySQL库存扣减失败，可能库存不足");
        }

        // 记录库存流水（审计）
        InventoryLog logEntity = new InventoryLog();
        logEntity.setCategoryId(categoryId);
        logEntity.setChangeType(1);  // 1=扣减
        logEntity.setQuantity(quantity);
        logEntity.setOrderNo(orderNo);
        inventoryLogMapper.insert(logEntity);

        // 发送超时取消消息到延迟队列（30分钟后自动取消）
        rabbitTemplate.convertAndSend("order.exchange", "order.delay", orderNo);

        log.info("订单已创建: orderNo={}", orderNo);
        return order;
    }

    // ========== 查询 ==========

    /**
     * 根据订单号查询订单
     */
    public Order getByOrderNo(String orderNo) {
        return orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
        );
    }

    /**
     * 查询用户的所有订单
     */
    public List<Order> getUserOrders(Long userId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreateTime)
        );
        // 填充票档名称
        for (Order order : orders) {
            TicketCategory category = categoryMapper.selectById(order.getCategoryId());
            if (category != null) {
                order.setCategoryName(category.getName());
            }
        }
        return orders;
    }

    // ========== 取消订单（超时未支付） ==========

    /**
     * 取消订单 + 回滚库存
     *
     * 用户下单后 X 分钟内没付款，自动取消
     * 同时要把 Redis 和 MySQL 里的库存加回来
     */
    @Transactional
    public void cancelOrder(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order == null) {
            log.warn("取消订单失败，订单不存在: {}", orderNo);
            return;
        }
        // 只有待支付的订单可以取消
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("订单状态不允许取消: orderNo={}, status={}", orderNo, order.getStatus());
            return;
        }

        // 更新订单状态为已取消
        order.setStatus(OrderStatus.CANCELLED);
        orderMapper.updateById(order);

        // 回滚 Redis 库存
        redisService.restoreStock(order.getCategoryId(), order.getQuantity());

        // 回滚 MySQL 库存
        categoryMapper.restoreStock(order.getCategoryId(), order.getQuantity());

        // 记录库存流水（回滚）
        InventoryLog logEntity = new InventoryLog();
        logEntity.setCategoryId(order.getCategoryId());
        logEntity.setChangeType(2);  // 2=回滚
        logEntity.setQuantity(order.getQuantity());
        logEntity.setOrderNo(orderNo);
        inventoryLogMapper.insert(logEntity);

        log.info("订单已取消，库存已回滚: orderNo={}", orderNo);
    }

    // ========== 工具 ==========

    /**
     * 生成唯一订单号
     * 格式：yyyyMMddHHmmss + 4位随机数
     * 后续可以换成雪花算法，MVP 阶段先用这个
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return timestamp + random;
    }
}
