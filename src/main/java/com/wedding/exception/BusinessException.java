package com.wedding.exception;

// 业务异常:表示"业务规则不满足",用法 throw new BusinessException("密码错误")。
//
// 继承 RuntimeException(非受检):中间层不必逐层 try-catch,直接抛到顶层由 GlobalExceptionHandler 统一接住。
public class BusinessException extends RuntimeException {

    // 异常提示会被原样返回给前端,应写对用户友好的文案
    public BusinessException(String message) {
        super(message);
    }
}
