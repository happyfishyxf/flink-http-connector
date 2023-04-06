package com.leo.flink.http.connector.sink.http;

import com.leo.flink.http.connector.config.HttpConfigConstants;
import com.leo.flink.http.connector.config.HttpConfigOptions;
import com.leo.flink.http.connector.config.HttpConnectorConfig;
import com.leo.flink.http.connector.util.HttpClientUtil;
import com.leo.flink.http.connector.util.JSONObjectTools;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Slf4j
public class HttpSinkFunction<IN> extends RichSinkFunction<IN> {
    private final HttpConnectorConfig config;
    private final SerializationSchema<IN> serializer;

    public HttpSinkFunction(HttpConnectorConfig config, SerializationSchema<IN> serializer) {
        log.info("new HttpSinkFunction");
        this.config = config;
        this.serializer = serializer;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        log.info("open HttpSinkFunction");
    }

    @Override
    public void close() throws Exception {
        super.close();
    }

    /**
     * @param value
     * @param context
     * @throws Exception
     */
    @Override
    public void invoke(IN value, Context context) throws Exception {
        // HttpClient
        invoke2(value);

    }

    /**
     * HttpClient
     *
     * @param value
     * @throws IOException
     */
    private void invoke2(IN value) throws IOException {
        // 是否开启 authentication验证
        boolean authenticationEnabled = this.config.getOptions().get(HttpConfigOptions.AUTHENTICATION_ENABLED);
        if (authenticationEnabled) { // 开启验证
            doAuthentication();
        }

        String url = this.config.getUrl();
        String requestBody = new String(this.serializer.serialize(value));
        // 进行http请求
        Map<String, String> result = HttpClientUtil.sendHttpRequestByHttpClient(url, requestBody, this.config.getHeaders(), "post");

        // 请求成功
        if ("200".equals(result.get(HttpClientUtil.CODE))) {
            if (this.config.getLogSuccess()) {
                log.info("HTTP Response code: {}, RESPONSE: {}, Req headers: {}, Submitted payload: {}, url: {}"
                        , result.get(HttpClientUtil.CODE)
                        , result.get(HttpClientUtil.CONTENT)
                        , JSONObjectTools.toFormatJsonString(this.config.getHeaders())
                        , requestBody
                        , url
                );
            }
        } else {
            if (this.config.getLogFail()) {
                log.info("HTTP Response code: {}, RESPONSE: {}, Req headers: {}, Submitted payload: {}, url: {}"
                        , result.get(HttpClientUtil.CODE)
                        , result.get(HttpClientUtil.CONTENT)
                        , JSONObjectTools.toFormatJsonString(this.config.getHeaders())
                        , requestBody
                        , url
                );
            }
        }
    }

    /**
     * 进行 认证
     */
    private void doAuthentication() throws IOException {
        String authenticationUrl = this.config.getOptions().get(HttpConfigOptions.AUTHENTICATION_URL);
        String authenticationReqBody = this.config.getOptions().get(HttpConfigOptions.AUTHENTICATION_REQ_BODY);
        String authenticationResKey = this.config.getOptions().get(HttpConfigOptions.AUTHENTICATION_RES_KEY);
        String authenticationKeyPrefix = this.config.getOptions().get(HttpConfigOptions.AUTHORIZATION_KEY_PREFIX);

        Map<String, String> result = HttpClientUtil.sendHttpRequestByHttpClient(authenticationUrl, authenticationReqBody, this.config.getHeaders(), "post");

//        String result = doInvoke(authenticationUrl, authenticationReqBody);

        // 请求成功
        if ("200".equals(result.get(HttpClientUtil.CODE))) {

            Map contentMap = JSONObjectTools.jsonToMap(result.get(HttpClientUtil.CONTENT));

            String authenticationValue = contentMap.getOrDefault(authenticationResKey, "").toString();

            // 添加 AUTHORIZATION header
            this.config.getHeaders().put(
                    HttpConfigConstants.CONFIG_KEY_HEADER_AUTHORIZATION,
                    authenticationKeyPrefix == null
                            ? authenticationValue
                            : authenticationKeyPrefix.trim().concat(" ").concat(authenticationValue));

            if (this.config.getLogSuccess()) {
                log.info("HTTP Response code: {}, RESPONSE: {}, Req headers: {}, Submitted payload: {}, url: {}"
                        , result.get(HttpClientUtil.CODE)
                        , result.get(HttpClientUtil.CONTENT)
                        , JSONObjectTools.toFormatJsonString(this.config.getHeaders())
                        , authenticationReqBody
                        , authenticationUrl
                );
            }

            return;
        }

        if (this.config.getLogFail()) {
            log.info("HTTP Response code: {}, RESPONSE: {}, Req headers: {}, Submitted payload: {}, url: {}"
                    , result.get(HttpClientUtil.CODE)
                    , result.get(HttpClientUtil.CONTENT)
                    , JSONObjectTools.toFormatJsonString(this.config.getHeaders())
                    , authenticationReqBody
                    , authenticationUrl
            );
        }
    }

    private String connectGetParameters(byte[] serialize) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map map = objectMapper.readValue(serialize, Map.class);
        StringBuffer sb = new StringBuffer();
        map.forEach((key, value) -> sb.append(key + "=" + value.toString() + "&"));
        return sb.toString();
    }
}
