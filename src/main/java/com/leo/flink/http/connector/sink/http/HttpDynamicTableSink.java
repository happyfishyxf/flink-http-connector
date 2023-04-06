package com.leo.flink.http.connector.sink.http;

import com.leo.flink.http.connector.config.HttpConnectorConfig;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.SinkFunctionProvider;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;

public class HttpDynamicTableSink implements DynamicTableSink {
    private final HttpConnectorConfig config;
    private final EncodingFormat<SerializationSchema<RowData>> encodingFormat;
    private final DataType productDataType;

    public HttpDynamicTableSink(HttpConnectorConfig config, EncodingFormat<SerializationSchema<RowData>> encodingFormat, DataType productDataType) {
        this.config = config;
        this.encodingFormat = encodingFormat;
        this.productDataType = productDataType;
    }

    @Override
    public ChangelogMode getChangelogMode(ChangelogMode changelogMode) {
        return encodingFormat.getChangelogMode();
    }

    @Override
    public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {
        final SerializationSchema<RowData> runtimeEncoder =
                this.encodingFormat.createRuntimeEncoder(context, this.productDataType);
        final SinkFunction<RowData> runtimeFunction = new HttpSinkFunction<>(
                this.config,
                runtimeEncoder
        );
        return SinkFunctionProvider.of(runtimeFunction);
    }

    @Override
    public DynamicTableSink copy() {
        return new HttpDynamicTableSink(this.config, this.encodingFormat, productDataType);
    }

    @Override
    public String asSummaryString() {
        return "Http Sink";
    }
}
