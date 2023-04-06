package com.leo.flink.http.connector.config;

import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@NoArgsConstructor
public final class HttpConfigConstants {
    public static final String CONFIG_KEY_CONNECTOR = "connector";
    public static final String CONFIG_KEY_FORMAT = "format";

    // AUTHENTICATION和AUTHORIZATION 相关字段
    public static final String CONFIG_KEY_AUTHENTICATION_ENABLED = "authentication-enabled";
    public static final String CONFIG_KEY_AUTHENTICATION_URL = "authentication-url";
    public static final String CONFIG_KEY_AUTHENTICATION_REQ_BODY = "authentication-req-body";
    public static final String CONFIG_KEY_AUTHENTICATION_RES_KEY = "authentication-res-key";
    public static final String CONFIG_KEY_AUTHORIZATION_KEY_PREFIX = "authorization-key-prefix";


    public static final String CONFIG_KEY_URL = "url";
    public static final String CONFIG_KEY_METHOD = "method";
    public static final String CONFIG_KEY_HEADER_PRE = "http.header.";

    public static final String CONFIG_KEY_HEADER_AUTHORIZATION = "Authorization";
    public static final String CONFIG_KEY_CONN_TIMEOUT = "connect.timeout";
    public static final String CONFIG_KEY_READ_TIMEOUT = "read.timeout";
    public static final String CONFIG_KEY_USER_HTTPS = "use-https";
    public static final String CONFIG_KEY_CERT_SERVER = "https.cert.server";
    public static final String CONFIG_KEY_CERT_CLIENT = "https.cert.client";
    public static final String CONFIG_KEY_KEY_CLIENT = "https.key.client";
    public static final String CONFIG_KEY_CERT_ALLOW_SELFSIGN = "https.cert.allow.self.sign";
    public static final String CONFIG_KEY_LOG_FAIL = "http.log.fail";
    public static final String CONFIG_KEY_LOG_SUCCESS = "http.log.success";

    public static final int DEFAULT_TIMEOUT = 5000;
    public static final List<String> ALLOW_METHODS = Arrays.asList("GET", "POST", "PUT", "DELETE");


}
