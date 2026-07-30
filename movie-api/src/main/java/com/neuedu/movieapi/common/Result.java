package com.neuedu.movieapi.common;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一响应结果封装
 *
 * @param <T> 数据类型
 */
@Data
@Accessors(chain = true)  // 支持链式调用
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    // ------ 状态码常量（可扩展为枚举） ------
    public static final String SUCCESS_CODE = "200";
    public static final String ERROR_CODE   = "500";
    public static final String SUCCESS_MSG  = "操作成功";
    public static final String ERROR_MSG    = "操作失败";

    private String code;
    private String message;
    private T data;
    private Long timestamp;      // 时间戳（毫秒），方便前端处理

    // ------ 成功响应（常用） ------
    public static <T> Result<T> success() {
        return new Result<T>()
                .setCode(SUCCESS_CODE)
                .setMessage(SUCCESS_MSG)
                .setTimestamp(System.currentTimeMillis());
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>()
                .setCode(SUCCESS_CODE)
                .setMessage(SUCCESS_MSG)
                .setData(data)
                .setTimestamp(System.currentTimeMillis());
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<T>()
                .setCode(SUCCESS_CODE)
                .setMessage(message)
                .setData(data)
                .setTimestamp(System.currentTimeMillis());
    }

    public static Result<String> success(String message) {
        return new Result<String>()
                .setCode(SUCCESS_CODE)
                .setMessage(message)
                .setTimestamp(System.currentTimeMillis());
    }

    // ------ 错误响应（常用） ------
    public static <T> Result<T> error() {
        return new Result<T>()
                .setCode(ERROR_CODE)
                .setMessage(ERROR_MSG)
                .setTimestamp(System.currentTimeMillis());
    }

    public static Result<String> error(String message) {
        return new Result<String>()
                .setCode(ERROR_CODE)
                .setMessage(message)
                .setTimestamp(System.currentTimeMillis());
    }

    public static <T> Result<T> error(String code, String message) {
        return new Result<T>()
                .setCode(code)
                .setMessage(message)
                .setTimestamp(System.currentTimeMillis());
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<T>()
                .setCode(code+"")
                .setMessage(message)
                .setTimestamp(System.currentTimeMillis());
    }



    // ------ 判断响应是否成功（便于前端/调用方判断） ------
    public boolean isSuccess() {
        return SUCCESS_CODE.equals(this.code);
    }
    public boolean isError() {
        return !isSuccess();
    }


}