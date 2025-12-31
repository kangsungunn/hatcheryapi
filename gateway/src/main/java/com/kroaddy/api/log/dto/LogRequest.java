package com.kroaddy.api.log.dto;

import lombok.Data;

@Data
public class LogRequest {
    private String action;
    private String url;
    private Integer tokenLength;
}

