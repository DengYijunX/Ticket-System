package com.ticket.common;

import lombok.Data;

/**
 * 通用 API 返回结果
 *
 * 所有接口都返回这个格式，前端统一处理：
 * {
 *   "code": 200,       // 状态码
 *   "message": "成功",  // 提示信息
 *   "data": { ... }    // 实际数据
 * }
 */
@Data
public class Result<T> {

    private int code;
    private String message;
    private T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 成功返回（带数据）
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "成功", data);
    }

    // 成功返回（无数据）
    public static <T> Result<T> success() {
        return new Result<>(200, "成功", null);
    }

    // 失败返回
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    // 常用快捷方式
    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null);
    }
}
