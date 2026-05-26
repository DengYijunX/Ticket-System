package com.ticket.mq;

import com.rabbitmq.client.Channel;
import com.ticket.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单消息消费者
 *
 * 从 MQ 队列中取出下单消息，逐步创建订单
 * 消费者的处理速度决定了系统的吞吐量
 * （消息积压说明消费者处理太慢，需要加更多消费者）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderService orderService;

    /**
     * 监听下单队列
     *
     * 当队列中有新消息时，这个方法会被自动调用
     * 消息格式："userId,sessionId,categoryId,quantity"
     *
     * @RabbitListener 告诉 Spring 这个方法要监听哪个队列
     */
    @RabbitListener(queues = "order.queue")
    public void handleOrderCreate(String message, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            // 解析消息：orderNo,userId,sessionId,categoryId,quantity
            String[] parts = message.split(",");
            String orderNo = parts[0];
            Long userId = Long.parseLong(parts[1]);
            Long sessionId = Long.parseLong(parts[2]);
            Long categoryId = Long.parseLong(parts[3]);
            int quantity = Integer.parseInt(parts[4]);

            // 创建订单（订单号已预生成）
            orderService.createOrder(orderNo, userId, sessionId, categoryId, quantity);

            // 手动确认消息已被处理
            // 如果不确认，MQ 会认为消息没被处理，会重发
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("订单创建失败: {}", message, e);
            try {
                // 处理失败：拒绝消息，不重新入队（避免死循环）
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                log.error("MQ 确认失败", ex);
            }
        }
    }

    /**
     * 监听延迟死信队列（超时未支付的订单）
     *
     * 消息进入 delayQueue 后等待 30 分钟
     * 如果 30 分钟内没有被消费（就是超时了），自动转发到这个监听器
     */
    @RabbitListener(queues = "order.delay.dead")
    public void handleOrderDelay(String orderNo, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            orderService.cancelOrder(orderNo);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("订单超时取消失败: {}", orderNo, e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                log.error("MQ 确认失败", ex);
            }
        }
    }
}
