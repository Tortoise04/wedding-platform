package com.wedding.dto;

import lombok.Data;

// 发送咨询消息的请求体(用户与商家共用):只有消息内容,身份由服务端判定。
@Data
public class SendMessageRequest {

    // 消息内容(非空)
    private String content;
}
