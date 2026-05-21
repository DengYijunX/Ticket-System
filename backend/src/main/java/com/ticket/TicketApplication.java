package com.ticket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 *
 * @SpringBootApplication    → Spring Boot 启动注解（组合了 @Configuration + @EnableAutoConfiguration + @ComponentScan）
 * @MapperScan("com.ticket.mapper") → 扫描 Mapper 接口，让 Spring 管理它们
 */
@SpringBootApplication
@MapperScan("com.ticket.mapper")
public class TicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketApplication.class, args);
    }
}
