package com.ticket.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.common.Result;
import com.ticket.mapper.*;
import com.ticket.model.entity.*;
import com.ticket.service.OrderService;
import com.ticket.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ShowMapper showMapper;
    private final ShowSessionMapper sessionMapper;
    private final TicketCategoryMapper categoryMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final OrderService orderService;
    private final RedisService redisService;

    // ========== 数据看板 ==========

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = new HashMap<>();

        data.put("totalUsers", userMapper.selectCount(null));
        data.put("totalShows", showMapper.selectCount(null));
        data.put("totalOrders", orderMapper.selectCount(null));

        // 今日新增订单
        long todayOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getCreateTime, LocalDateTime.now().toLocalDate())
        );
        data.put("todayOrders", todayOrders);

        // 总收入（已支付 + 已完成）
        List<Order> paidOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().in(Order::getStatus, 1, 4)
        );
        BigDecimal revenue = paidOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("totalRevenue", revenue);

        return Result.success(data);
    }

    // ========== 演出管理 ==========

    @GetMapping("/shows")
    public Result<List<Map<String, Object>>> listShows() {
        List<Show> rawShows = showMapper.selectList(
                new LambdaQueryWrapper<Show>().orderByDesc(Show::getCreateTime)
        );
        List<Map<String, Object>> result = new ArrayList<>();

        for (Show show : rawShows) {
            Map<String, Object> showMap = new HashMap<>();
            showMap.put("id", show.getId());
            showMap.put("title", show.getTitle());
            showMap.put("description", show.getDescription());
            showMap.put("venue", show.getVenue());
            showMap.put("status", show.getStatus());
            showMap.put("createTime", show.getCreateTime());

            // 查询场次
            List<ShowSession> sessions = sessionMapper.selectList(
                    new LambdaQueryWrapper<ShowSession>()
                            .eq(ShowSession::getShowId, show.getId())
                            .orderByAsc(ShowSession::getStartTime)
            );
            List<Map<String, Object>> sessionList = new ArrayList<>();
            for (ShowSession s : sessions) {
                Map<String, Object> sm = new HashMap<>();
                sm.put("id", s.getId());
                sm.put("name", s.getName());
                sm.put("startTime", s.getStartTime());

                // 查询票档
                List<TicketCategory> cats = categoryMapper.selectList(
                        new LambdaQueryWrapper<TicketCategory>()
                                .eq(TicketCategory::getSessionId, s.getId())
                                .orderByDesc(TicketCategory::getPrice)
                );
                sm.put("categories", cats);
                sessionList.add(sm);
            }
            showMap.put("sessions", sessionList);
            result.add(showMap);
        }
        return Result.success(result);
    }

    @PostMapping("/show")
    @Transactional
    public Result<String> createShow(@RequestBody ShowRequest body) {
        Show show = new Show();
        show.setTitle(body.getTitle());
        show.setDescription(body.getDescription());
        show.setVenue(body.getVenue());
        show.setStatus(1);
        show.setSaleStart(LocalDateTime.now());
        showMapper.insert(show);

        // 创建场次和票档
        for (SessionReq s : body.getSessions()) {
            ShowSession session = new ShowSession();
            session.setShowId(show.getId());
            session.setName(s.getName());
            session.setStartTime(s.getStartTime());
            sessionMapper.insert(session);

            for (CategoryReq c : s.getCategories()) {
                TicketCategory cat = new TicketCategory();
                cat.setSessionId(session.getId());
                cat.setName(c.getName());
                cat.setPrice(c.getPrice());
                cat.setTotalStock(c.getTotalStock());
                cat.setRemainStock(c.getTotalStock());
                categoryMapper.insert(cat);

                // 库存预热到 Redis（抢票时 Redis 扣库存依赖这个）
                redisService.preheatStock(cat.getId(), c.getTotalStock());
            }
        }
        return Result.success("创建成功");
    }

    @PutMapping("/show")
    @Transactional
    public Result<String> updateShow(@RequestBody Show show) {
        showMapper.updateById(show);
        return Result.success("更新成功");
    }

    @DeleteMapping("/show/{id}")
    @Transactional
    public Result<String> deleteShow(@PathVariable Long id) {
        // 删除场次 + 票档（级联）
        List<ShowSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ShowSession>().eq(ShowSession::getShowId, id)
        );
        for (ShowSession s : sessions) {
            categoryMapper.delete(new LambdaQueryWrapper<TicketCategory>()
                    .eq(TicketCategory::getSessionId, s.getId()));
        }
        sessionMapper.delete(new LambdaQueryWrapper<ShowSession>().eq(ShowSession::getShowId, id));
        showMapper.deleteById(id);
        return Result.success("删除成功");
    }

    // ========== 订单管理 ==========

    @GetMapping("/orders")
    public Result<List<Order>> listOrders() {
        return Result.success(orderMapper.selectList(
                new LambdaQueryWrapper<Order>().orderByDesc(Order::getCreateTime)
        ));
    }

    @PostMapping("/order/{id}/cancel")
    public Result<String> cancelOrder(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) return Result.badRequest("订单不存在");
        orderService.cancelOrder(order.getOrderNo());
        return Result.success("已退款");
    }

    // ========== 请求体 DTO ==========

    static class ShowRequest {
        private String title;
        private String description;
        private String venue;
        private List<SessionReq> sessions;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getVenue() { return venue; }
        public void setVenue(String venue) { this.venue = venue; }
        public List<SessionReq> getSessions() { return sessions; }
        public void setSessions(List<SessionReq> sessions) { this.sessions = sessions; }
    }

    static class SessionReq {
        private String name;
        private LocalDateTime startTime;
        private List<CategoryReq> categories;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public List<CategoryReq> getCategories() { return categories; }
        public void setCategories(List<CategoryReq> categories) { this.categories = categories; }
    }

    static class CategoryReq {
        private String name;
        private BigDecimal price;
        private int totalStock;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public int getTotalStock() { return totalStock; }
        public void setTotalStock(int totalStock) { this.totalStock = totalStock; }
    }
}
