package com.wedding.common;

import lombok.Data;

// 统一返回结构 { code, msg, data } —— code = 200 成功,其余失败,前端不用猜格式。
@Data
public class Result<T> {

    // 200 成功;业务失败 400 / 未登录 401 / 权限不足 403 / 服务器错误 500
    private int code;
    // 给前端看的提示
    private String msg;
    // 真正的业务数据
    private T data;

    // 成功:带上业务数据,code 固定 200
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    // 失败:默认按服务器错误 500 处理
    public static <T> Result<T> fail(String msg) {
        return fail(500, msg);
    }
    // 失败:指定状态码(如业务失败 400),供异常处理器与拦截器复用
    public static <T> Result<T> fail(int code, String msg){
        Result<T> r = new Result<>();
        r.msg = msg;
        r.code=code;
        return r;
    }
}
