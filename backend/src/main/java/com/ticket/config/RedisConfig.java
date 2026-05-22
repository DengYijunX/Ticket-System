package com.ticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 *
 * 配置 RedisTemplate 的序列化方式
 * 默认的 JdkSerializationRedisSerializer 存的数据人类不可读
 * 改成 String + JSON 序列化，方便调试和查看
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // key 用字符串序列化（方便存成可读的 key）
        template.setKeySerializer(new StringRedisSerializer());
        // value 用 JSON 序列化
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        // Hash 的 key 和 value 也用同样方式
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
