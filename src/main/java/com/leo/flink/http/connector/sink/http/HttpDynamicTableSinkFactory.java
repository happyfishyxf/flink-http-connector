package com.leo.flink.http.connector.sink.http;

import com.leo.flink.http.connector.config.HttpConnectorConfig;
import com.leo.flink.http.connector.config.HttpConfigConstants;
import com.leo.flink.http.connector.config.HttpConfigOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.DynamicTableSinkFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.factories.SerializationFormatFactory;
import org.apache.flink.table.types.DataType;

import java.util.*;

@Slf4j
public class HttpDynamicTableSinkFactory implements DynamicTableSinkFactory {
    private static final String CONNECTOR_NAME = "http-sink";


    @Override
    public String factoryIdentifier() {
        return CONNECTOR_NAME;
    }

    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
        final FactoryUtil.TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);

        helper.validateExcept(HttpConfigConstants.CONFIG_KEY_HEADER_PRE);

        final ReadableConfig options = helper.getOptions();

        checkFormat(options.get(FactoryUtil.FORMAT));
        final String method = checkHttpMethod(options.getOptional(HttpConfigOptions.METHOD));
        Map<String, String> headers = extractHeaders(context.getCatalogTable().getOptions());
        final String url = options.get(HttpConfigOptions.URL);

        HttpConnectorConfig config = new HttpConnectorConfig();
        // 直接塞进去
        config.setOptions(options);

        config.setUrl(url);
        config.setMethod(method);
        config.setHeaders(headers);
        config.setConnectTimeout(options.get(HttpConfigOptions.CONNECT_TIMEOUT));
        config.setReadTimeout(options.get(HttpConfigOptions.READ_TIMEOUT));
        config.setUseHttps(options.get(HttpConfigOptions.USE_HTTPS));
        config.setLogFail(options.get(HttpConfigOptions.LOG_FAIL));
        config.setLogSuccess(options.get(HttpConfigOptions.LOG_SUCCESS));

        if (log.isDebugEnabled()) {
            log.debug("Http Connector Config: {}", config);
        }

        //
        final EncodingFormat<SerializationSchema<RowData>> encodingFormat =
                helper.discoverEncodingFormat(SerializationFormatFactory.class, FactoryUtil.FORMAT);
        final DataType productDataType = context.getCatalogTable().getSchema().toPhysicalRowDataType();

        log.info("Create DynamicTableSink with: {} {} {}", url, method, encodingFormat);
        return new HttpDynamicTableSink(config, encodingFormat, productDataType);
    }

    private String checkHttpMethod(Optional<String> optional) {
        String method = optional.orElse("");
        if (!HttpConfigConstants.ALLOW_METHODS.contains(method)) {
            throw new IllegalArgumentException("Invalid option 'method'. Only 'POST|GET|DELETE|PUT' supported");
        }
        return method;
    }

    private Map<String, String> extractHeaders(Map<String, String> options) {
        Map<String, String> headers = new HashMap<String, String>(8);
        options.forEach((key, value) -> {
            if (key.startsWith(HttpConfigConstants.CONFIG_KEY_HEADER_PRE)) {
                String headKey = key.substring(HttpConfigConstants.CONFIG_KEY_HEADER_PRE.length());
                headers.put(headKey, value);
            }
        });
        return headers;
    }

    private void checkFormat(String format) {
        if (!"json".equals(format)) {
            throw new IllegalArgumentException("Invalid option 'format'. It is expected to be 'json'.");
        }
    }


    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> options = new HashSet<ConfigOption<?>>();
        options.add(HttpConfigOptions.URL);
        options.add(FactoryUtil.FORMAT);

        return options;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        Set<ConfigOption<?>> options = new HashSet<ConfigOption<?>>();

        options.add(HttpConfigOptions.AUTHENTICATION_ENABLED);
        options.add(HttpConfigOptions.AUTHENTICATION_URL);
        options.add(HttpConfigOptions.AUTHENTICATION_REQ_BODY);
        options.add(HttpConfigOptions.AUTHENTICATION_RES_KEY);
        options.add(HttpConfigOptions.AUTHORIZATION_KEY_PREFIX);

        options.add(HttpConfigOptions.METHOD);
        options.add(HttpConfigOptions.CONNECT_TIMEOUT);
        options.add(HttpConfigOptions.READ_TIMEOUT);
        options.add(HttpConfigOptions.USE_HTTPS);
        options.add(HttpConfigOptions.LOG_FAIL);
        options.add(HttpConfigOptions.LOG_SUCCESS);



        return options;
    }
}
