# Description

Flink sink connector for HTTP/HTTPS

* Use Httpclient to connect to the web server.
* SSL verification shielded when request with https.
* Two phrased authentication and authorization supported with some options


## dependencies
* Java 8+
* Maven 3+
* Flink 1.12+
* Httpclient 4.5.13
* GSON 2.7+

## Use Case 

``` flink table sql
       CREATE TABLE http_sink (
            chat_id        VARCHAR,
            msg_type       VARCHAR,
            card Map<VARCHAR,ARRAY<Map<VARCHAR,VARCHAR>>>
        ) WITH (
                 'connector' = 'http-sink',
                 'url' = 'https://xxxxx/open-apis/message/v4/send/',

                 'format' = 'json',
                 'method' = 'POST',
                 'http.log.success' = 'true',
                 'http.log.fail' = 'true',
                 'http.header.Content-Type' = 'application/json; charset=utf-8',

                  --  是否开启二段 认证，其余参数暂时同主请求
                 'authentication-enabled'='true', --  是否开启
                 'authentication-url' = 'https://xxxxx/open-apis/auth/v3/tenant_access_token/internal/', --  认证url
                 'authentication-res-key' = 'tenant_access_token', --  认证结果中 提取字段，此字段会和authorization-key-prefix拼接后作为  authorization header值
                 'authentication-req-body' = '{
                       "app_id": "XXXX",
                       "app_secret": "XXXXX"
                   }',
                  'authorization-key-prefix' = 'Bearer' --  authorization认证头的前缀 
       )
;
```

## Connector Options
### HTTP Sink
| Option                   | Required | Description/Value                                                                                                   |
|--------------------------|----------|---------------------------------------------------------------------------------------------------------------------|
| connector                | required | Specify what connector to use. Can only be _'http-sink'_.                                                           |
| url                      | required | The base URL that should be use for HTTP requests. For example _http(s)://localhost:8080/client_.                   |
| format                   | required | Can only be _'json'_.                                                                                               |
| method                   | required | Specify which HTTP method to use in the request. The value should be set either to `GET`,`DELETE`, `POST` or `PUT`. |
| http.log.fail            | optional | `true` or `false`, default  `true`, When response status != 200, log error msg.                                   |
| http.log.success         | optional | `true` or `false`, default  `false`, When response status == 200, log response body.                              |
| http.header.*            | optional | any http/https header is supported                                                                                  |
| authentication-enabled   | optional | `true` or `false`, default `false`, whether authentication enabled or not                                           |
| authentication-url       | optional | The base URL that should be use for authentication requests ,required when authentication enabled                   |
| authentication-res-key   | optional | The authentication key that should be extracted from authentication response ,required when authentication enabled  |
| authentication-req-body  | optional | The authentication request body that should be sent to server ,required when authentication enabled                 |
| authorization-key-prefix | optional | The authorization header key prefix  that should be concacted with the value of authentication-res-key from server  |
