package com.ticket.controller;

import com.ticket.common.Result;
import com.ticket.model.entity.Show;
import com.ticket.model.entity.ShowSession;
import com.ticket.model.entity.TicketCategory;
import com.ticket.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 演出 Controller
 *
 * 查演出列表、场次、票价信息（读接口，不涉及写操作）
 */
@RestController
@RequestMapping("/api/show")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    /**
     * GET /api/show/list
     *
     * 获取所有在售演出
     */
    @GetMapping("/list")
    public Result<List<Show>> listShows() {
        return Result.success(showService.getAvailableShows());
    }

    /**
     * GET /api/show/{id}
     *
     * @PathVariable 从 URL 路径中取值
     * 访问 /api/show/1 时 id = 1
     */
    @GetMapping("/{id}")
    public Result<Show> getShow(@PathVariable Long id) {
        return Result.success(showService.getShowById(id));
    }

    /**
     * GET /api/show/{id}/sessions
     *
     * 获取某个演出的所有场次
     */
    @GetMapping("/{id}/sessions")
    public Result<List<ShowSession>> getSessions(@PathVariable Long id) {
        return Result.success(showService.getSessionsByShowId(id));
    }

    /**
     * GET /api/show/session/{sessionId}/categories
     *
     * 获取某个场次的所有票价（含库存和价格）
     * 前端展示用：VIP 1880元 剩余33张
     */
    @GetMapping("/session/{sessionId}/categories")
    public Result<List<TicketCategory>> getCategories(@PathVariable Long sessionId) {
        return Result.success(showService.getCategoriesBySessionId(sessionId));
    }
}
