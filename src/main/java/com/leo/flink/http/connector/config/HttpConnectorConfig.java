package com.leo.flink.http.connector.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.flink.configuration.ReadableConfig;

import java.io.Serializable;
import java.util.Map;

@Data
@ToString
@NoArgsConstructor
public final class HttpConnectorConfig implements Serializable {
    private String url;
    private String method;
    private Map<String, String> headers;
    private Integer connectTimeout = HttpConfigConstants.DEFAULT_TIMEOUT;
    private Integer readTimeout = HttpConfigConstants.DEFAULT_TIMEOUT;
    private Boolean useHttps = false;
    private Boolean logFail = true;
    private Boolean logSuccess = false;

   private ReadableConfig options;

}
