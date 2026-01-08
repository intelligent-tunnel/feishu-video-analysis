package com.geekonup.service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口返回状态码对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusCode {
    
    /**
     * 状态码
     */
    private Integer code;
    
    /**
     * 状态消息
     */
    private String message;
}

