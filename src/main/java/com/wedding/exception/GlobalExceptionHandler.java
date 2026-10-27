package com.wedding.exception;

import com.wedding.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// 全局异常处理器 —— 系统里唯一"catch 后把结果返回给用户"的地方。
//
// @RestControllerAdvice:所有 Controller 抛出的异常都先到这里统一转成 JSON,service / controller 从此只管 throw。
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 业务异常:把 message 原样返回前端(给用户看的,如"密码错误")
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(400,e.getMessage());
    }

    // 上传文件超限 —— Spring 按 multipart 配置在进 Controller 之前就拦掉,同样属客户端错误,
    // 不接住就会掉进 500 兜底,用户看到的是"系统繁忙"而不是"文件太大"。
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        log.warn("上传文件超限: {}", e.getMessage());
        // 走到这里说明请求体已经超过 yml 的 multipart 上限(50MB/60MB),Controller 里的分档判断还没轮到,
        // 所以文案不分类型,直接报两档额度,免得传 60MB 视频的用户看到"文件不能超过 5MB"而困惑
        return Result.fail(400, "文件超出上传大小限制(视频 50MB,其它 5MB)");
    }

    // 兜底:其余没预料到的异常(空指针、数据库异常……)。
    // 注意:堆栈只打日志,不返回前端(避免泄露内部细节)。
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500,"系统繁忙,请稍后再试");
    }

    // 请求参数不合法 —— 缺必填参数 / 类型不对 / 请求体格式错。
    //
    // ⚠️ 这类异常由 Spring MVC 在"进入 Controller 之前"抛出,曾一路落进上面的兜底 → 返回 500;
    // 它们其实是客户端错误,必须在兜底之前单独接住返回 400
    // (业务失败 400 / 未登录 401 / 权限不足 403 / 服务器错误 500)。
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public Result<Void> handleBadRequest(Exception e) {
        String detail;
        if (e instanceof MissingServletRequestParameterException missing) {
            detail = "缺少必填参数: " + missing.getParameterName();
        } else if (e instanceof MethodArgumentTypeMismatchException mismatch) {
            detail = "参数类型不正确: " + mismatch.getName();
        } else {
            detail = "请求体格式不正确";
        }
        log.warn("请求参数不合法: {}", e.getMessage());
        return Result.fail(400, detail);
    }
}
