package com.ticket.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 *
 * 定义了消息的流转路径：
 *   生产者 → Exchange → Binding → Queue → 消费者
 *
 * 流程：
 *   TicketService 发消息到 "order.exchange"
 *   Exchange 根据 routing key 找到对应的 Queue
 *   Queue 里的消息等待消费者取走处理
 */
@Configuration
public class RabbitMQConfig {

    /** 下单队列：存放待创建的订单消息 */
    public static final String ORDER_QUEUE = "order.queue";
    /** 交换机名称 */
    public static final String ORDER_EXCHANGE = "order.exchange";
    /** 路由键 */
    public static final String ORDER_ROUTING_KEY = "order.create";

    /** 延迟队列：用于订单超时取消 */
    public static final String DELAY_QUEUE = "order.delay.queue";
    /** 延迟消息的 routing key */
    public static final String DELAY_ROUTING_KEY = "order.delay";

    /**
     * 交换机：Direct 类型
     * Direct = 直接根据 routing key 把消息发给对应队列
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    /**
     * 下单队列
     */
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE).build();
    }

    /**
     * 把队列绑定到交换机
     * 带了 order.create 标签的消息 → order.queue
     */
    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue())
                .to(orderExchange())
                .with(ORDER_ROUTING_KEY);
    }

    // ========== 延迟队列（订单超时处理） ==========

    /**
     * 延迟队列
     *
     * 消息进入这个队列后，消费者不会立即收到
     * 而是等 TTL（存活时间）到了之后，自动转发到死信队列
     * 死信消费者收到后执行订单取消
     *
     * 效果：下单后30分钟没付款，自动取消
     */
    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE)
                // 消息存活时间（毫秒）：30分钟 = 30 * 60 * 1000
                .withArgument("x-message-ttl", 30 * 60 * 1000)
                // 过期后转发到哪个死信交换机
                .withArgument("x-dead-letter-exchange", ORDER_EXCHANGE)
                // 死信的路由 key
                .withArgument("x-dead-letter-routing-key", "order.delay")
                .build();
    }

    /**
     * 延迟队列的消费者（处理超时取消订单）
     */
    @Bean
    public Queue delayDeadQueue() {
        return QueueBuilder.durable("order.delay.dead").build();
    }

    @Bean
    public Binding delayDeadBinding() {
        return BindingBuilder.bind(delayDeadQueue())
                .to(orderExchange())
                .with(DELAY_ROUTING_KEY);
    }
}
