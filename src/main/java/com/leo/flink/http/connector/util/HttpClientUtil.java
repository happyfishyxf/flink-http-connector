package com.leo.flink.http.connector.util;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 封装了一些采用HttpClient发送HTTP请求的方法
 *
 * @create Feb 1, 2012 3:02:27 PM
 * @update Oct 8, 2012 3:48:55 PM
 */
public class HttpClientUtil {
    protected static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
    public static final String CODE = "code";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT = "content";

    /**
     * 通过HttpClient 发送java请求
     *
     * @param reqURL
     * @param requestData thinQuery={}
     * @param headers
     * @param method      GET POST
     * @return
     * @throws IOException
     */
    public static Map<String, String> sendHttpRequestByHttpClient(String reqURL, String requestData, Map<String, String> headers, String method) throws IOException {
        if (!headers.containsKey(CONTENT_TYPE)) {
            headers.put(CONTENT_TYPE, "application/json; charset=UTF-8");
        }
        if (headers == null) {
            headers = new HashMap<>();
        }

        if ("GET".equalsIgnoreCase(method)) {
            if (requestData == null || "".equals(requestData)) {
                return sendGetRequest(reqURL, headers, null);
            }
            return sendPostRequest(reqURL, requestData, headers, true);
        }
        if ("POST".equalsIgnoreCase(method)) {
            return sendPostRequest(reqURL, requestData, headers, true);
        }
        if ("PUT".equalsIgnoreCase(method)) {
            return sendPutRequest(reqURL, requestData, headers, true);
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            return sendDELETERequest(reqURL, headers, null);
        }

        Map<String, String> resultMap = new HashMap();
        resultMap.put("code", "512");
        resultMap.put("content-type", "text/plain; charset=UTF-8");
        resultMap.put("content", "Not supported http method currently");
        return resultMap;
    }

    private static Map<String, String> sendHeadRequest(String reqURL) throws IOException {
        String responseContent = null; // 响应内容
        HttpEntity entity = null;
        HttpClient httpClient = HttpClientFactory.concurrentClient(); // 创建默认的httpClient实例
        try {
            HttpHead httpHead = new HttpHead(reqURL); // 创建org.apache.http.client.methods.HttpHead
            HttpResponse response = httpClient.execute(httpHead); // 执行Head请求
            Map<String, String> resultMap = new HashMap();
            resultMap.put("code", response.getStatusLine().getStatusCode() + "");
            resultMap.put("content-type", response.getFirstHeader("content-type").getValue());
            resultMap.put("content", responseContent);
            return resultMap;

        } catch (IOException e) {
            throw e;
        } finally {
            // httpClient.getConnectionManager().shutdown(); // 关闭连接,释放资源
            try {
                EntityUtils.consume(entity); // Consume response content
            } catch (IOException e) {
                throw e;
            }
        }
    }

    /**
     * 发送HTTP_GET请求
     *
     * @param reqURL        请求地址(含参数)
     * @param decodeCharset 解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
     * @return 远程主机响应正文
     */
    public static Map<String, String> sendGetRequest(String reqURL, Map<String, String> headers, String decodeCharset) throws IOException {

        String responseContent = null; // 响应内容
        HttpEntity entity = null;
        HttpClient httpClient = HttpClientFactory.concurrentClient(); // 创建默认的httpClient实例
//        logger.info("create client successfully~");
        HttpGet httpGet = null;
        try {
            httpGet = new HttpGet(reqURL); // 创建org.apache.http.client.methods.HttpGet
//            httpGet.setHeader(HTTP.CONTENT_TYPE, contentType);
            headers.forEach(httpGet::setHeader);
            HttpResponse response = httpClient.execute(httpGet); // 执行GET请求
//            logger.info("execute method successfully~");
            entity = response.getEntity(); // 获取响应实体
            if (null != entity) {
                responseContent = EntityUtils.toString(entity, decodeCharset == null ? "UTF-8" : decodeCharset);
            }
//          logger.info("parse method result successfully~");
            Map<String, String> resultMap = new HashMap();
            if (response.getStatusLine() != null) {
                resultMap.put(CODE, "" + response.getStatusLine().getStatusCode());
            }
//            logger.info("response.getStatusLine():{}", response.getStatusLine().getStatusCode());
            if (response.getFirstHeader("content-type") != null) {
                resultMap.put(CONTENT_TYPE, response.getFirstHeader("content-type").getValue());
            }
//            logger.info("response.getFirstHeader(\"content-type\").getValue():{}", response.getFirstHeader("content-type").getValue());
            resultMap.put(CONTENT, responseContent);
//            logger.info("responseContent:{}", responseContent);

            return resultMap;

        } catch (IOException e) {
            throw e;
        } finally {
            try {
                // 释放连接
                // httpGet.releaseConnection();
                EntityUtils.consume(entity); // Consume response content
            } catch (IOException e) {
                throw e;
            }
        }

    }

    /**
     * 发送HTTP_DELETE请求
     *
     * @param reqURL        请求地址(含参数)
     * @param decodeCharset 解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
     * @return 远程主机响应正文
     */
    public static Map<String, String> sendDELETERequest(String reqURL, Map<String, String> headers, String decodeCharset) throws IOException {

        String responseContent = null; // 响应内容
        HttpClient httpClient = HttpClientFactory.concurrentClient(); // 创建默认的httpClient实例
        HttpEntity entity = null;
        try {
            HttpDelete httpDelete = new HttpDelete(reqURL); // 创建org.apache.http.client.methods.HttpGet
//            httpDelete.setHeader(HTTP.CONTENT_TYPE, contentType);
            headers.forEach(httpDelete::setHeader);

            HttpResponse response = httpClient.execute(httpDelete); // 执行DELETE请求
            entity = response.getEntity(); // 获取响应实体
            if (null != entity) {
                responseContent = EntityUtils.toString(entity, decodeCharset == null ? "UTF-8" : decodeCharset);
            }
            Map<String, String> resultMap = new HashMap();
            resultMap.put("code", response.getStatusLine().getStatusCode() + "");
            resultMap.put("content-type", response.getFirstHeader("content-type").getValue());
            resultMap.put("content", responseContent);
            return resultMap;
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                EntityUtils.consume(entity); // Consume response content
            } catch (IOException e) {
                throw e;
            }
        }

    }

    /**
     * 发送HTTP_POST请求
     *
     * @param isEncoder 用于指明请求数据是否需要UTF-8编码,true为需要 // * @see 该方法为
     *                  <code>sendPostRequest(String,String,boolean,String,String)</code> 的简化方法 // * @see
     *                  该方法在对请求数据的编码和响应数据的解码时,所采用的字符集均为UTF-8 // * @see 当<code>isEncoder=true</code>时,其会自动对
     *                  <code>sendData</code>中的[中文][|][ ]等特殊字符进行<code>URLEncoder.encode(string,"UTF-8")</code>
     */
    public static Map<String, String> sendPostRequest(String reqURL, String sendData, Map<String, String> headers, boolean isEncoder) throws IOException {
        return sendPostRequest(reqURL, sendData, headers, isEncoder, null, null);
    }

    /**
     * 发送HTTP_POST请求
     *
     * @param reqURL        请求地址
     * @param sendData      请求参数,若有多个参数则应拼接成param11=value11¶m22=value22¶m33=value33的形式后, 传入该参数中
     * @param isEncoder     请求数据是否需要encodeCharset编码,true为需要
     * @param encodeCharset 编码字符集,编码请求数据时用之,其为null时默认采用UTF-8解码
     * @param decodeCharset 解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
     * @return 远程主机响应正文 // * @see 该方法会自动关闭连接,释放资源 // * @see 当<code>isEncoder=true</code>时,其会自动对<code>sendData</code>
     * 中的[中文][|][ ]等特殊字符进行<code>URLEncoder.encode(string,encodeCharset)</code>
     */
    private static Map<String, String>  sendPostRequest(String reqURL, String sendData, Map<String, String> headers, boolean isEncoder, String encodeCharset, String decodeCharset) throws IOException {
        String responseContent = null;
        HttpEntity entity = null;
        HttpClient httpClient = HttpClientFactory.concurrentClient();

        HttpPost httpPost = new HttpPost(reqURL);
//        httpPost.setHeader(HTTP.CONTENT_TYPE, contentType);
        headers.forEach(httpPost::setHeader);
        String contentType = headers.getOrDefault(CONTENT_TYPE, "application/json; charset=UTF-8");
        try {
            if (sendData != null && !"".equals(sendData.trim())) {
                if (isEncoder) {
                    httpPost.setEntity(new StringEntity(sendData, getCharSet(encodeCharset, contentType)));
                } else {
                    httpPost.setEntity(new StringEntity(sendData));
                }
            }
            HttpResponse response = httpClient.execute(httpPost);
            entity = response.getEntity();
            if (null != entity) {
                responseContent = EntityUtils.toString(entity, getCharSet(decodeCharset, contentType));
            }

            Map<String, String> resultMap = new HashMap();
            resultMap.put(CODE, response.getStatusLine().getStatusCode() + "");
            resultMap.put(CONTENT_TYPE, response.getFirstHeader("content-type").getValue());
            resultMap.put(CONTENT, responseContent);

            return resultMap;

        } catch (IOException e) {
            throw e;
        } finally {
            try {
                EntityUtils.consume(entity); // Consume response content,close stream
            } catch (IOException e) {
                throw e;
            }
        }
    }


    /**
     * 获取默认的字符集
     *
     * @param charset
     * @param contentType
     * @return
     */
    private static String getCharSet(String charset, String contentType) {
        try {
            String charSet;
            if (StringUtils.isNotBlank(charset)) {
                charSet = charset;
            } else {
                charSet = contentType.indexOf("charset=") == -1
                        ? Charset.defaultCharset().toString()
                        : contentType.substring(contentType.indexOf("charset=") + 8);
            }

            Charset.forName(charSet);

            return charSet;

        } catch (Exception e) {
            return Charset.defaultCharset().toString();
        }

    }

    /**
     * 发送HTTP_PUT请求
     *
     * @param isEncoder 用于指明请求数据是否需要UTF-8编码,true为需要 // * @see 该方法为
     *                  <code>sendPostRequest(String,String,boolean,String,String)</code> 的简化方法 // * @see
     *                  该方法在对请求数据的编码和响应数据的解码时,所采用的字符集均为UTF-8 // * @see 当<code>isEncoder=true</code>时,其会自动对
     *                  <code>sendData</code>中的[中文][|][ ]等特殊字符进行<code>URLEncoder.encode(string,"UTF-8")</code>
     */
    public static Map<String, String> sendPutRequest(String reqURL, String sendData, Map<String, String> headers, boolean isEncoder) throws IOException {
        return sendPutRequest(reqURL, sendData, headers, isEncoder, null, null);
    }

    /**
     * 发送HTTP_PUT请求
     *
     * @param reqURL        请求地址
     * @param sendData      请求参数,若有多个参数则应拼接成param11=value11¶m22=value22¶m33=value33的形式后, 传入该参数中
     * @param isEncoder     请求数据是否需要encodeCharset编码,true为需要
     * @param encodeCharset 编码字符集,编码请求数据时用之,其为null时默认采用UTF-8解码
     * @param decodeCharset 解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
     * @return 远程主机响应正文 // * @see 该方法会自动关闭连接,释放资源 // * @see 当<code>isEncoder=true</code>时,其会自动对<code>sendData</code>
     * 中的[中文][|][ ]等特殊字符进行<code>URLEncoder.encode(string,encodeCharset)</code>
     */
    private static Map<String, String> sendPutRequest(String reqURL, String sendData, Map<String, String> headers, boolean isEncoder, String encodeCharset, String decodeCharset) throws IOException {
        String responseContent = null;
        HttpClient httpClient = HttpClientFactory.concurrentClient();
        HttpPut httpPut = new HttpPut(reqURL);
        HttpEntity entity = null;
//        httpPut.setHeader(HTTP.CONTENT_TYPE, contentType);
        headers.forEach(httpPut::setHeader);
        String contentType = headers.getOrDefault(CONTENT_TYPE, "application/json; charset=UTF-8");
        try {
            if (sendData != null && (!"".equals(sendData.trim()))) {
                if (isEncoder) {
                    httpPut.setEntity(new StringEntity(sendData, getCharSet(encodeCharset, contentType)));
                } else {
                    httpPut.setEntity(new StringEntity(sendData));
                }
            }
            HttpResponse response = httpClient.execute(httpPut);
            entity = response.getEntity();
            if (null != entity) {
                responseContent = EntityUtils.toString(entity, getCharSet(decodeCharset, contentType));
            }
            Map<String, String> resultMap = new HashMap();
            resultMap.put("code", response.getStatusLine().getStatusCode() + "");
            resultMap.put("content-type", response.getFirstHeader("content-type").getValue());
            resultMap.put("content", responseContent);
            return resultMap;
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                EntityUtils.consume(entity); // Consume response content,close stream
            } catch (IOException e) {
                throw e;
            }
        }

    }

    /**
     * 通过httpURLConnection 发送java请求
     *
     * @param reqURL
     * @param requestData
     * @param method
     * @return
     * @throws IOException
     */
    public static Map<String, String> sendHttpRequestByJava(String reqURL, String requestData, String contentType, String method) throws IOException {
        contentType = contentType == null || "".equals(contentType) ? "text/plain; charset=UTF-8" : contentType;
        if ("GET".equalsIgnoreCase(method)) {
            if (requestData == null || "".equals(requestData)) {
                return sendRequestByJavaWithoutBody(reqURL, contentType, "GET");
            }
            return sendRequestByJavaWithBody(reqURL, requestData, contentType, "GET");
        }
        if ("POST".equalsIgnoreCase(method)) {
            if (requestData == null || "".equals(requestData)) {
                return sendRequestByJavaWithoutBody(reqURL, contentType, "POST");
            }
            return sendRequestByJavaWithBody(reqURL, requestData, contentType, "POST");
        }
        if ("PUT".equalsIgnoreCase(method)) {
            if (requestData == null || "".equals(requestData)) {
                return sendRequestByJavaWithoutBody(reqURL, contentType, "PUT");
            }
            return sendRequestByJavaWithBody(reqURL, requestData, contentType, "PUT");
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            if (requestData == null || "".equals(requestData)) {
                return sendRequestByJavaWithoutBody(reqURL, contentType, "DELETE");
            }
            return sendRequestByJavaWithBody(reqURL, requestData, contentType, "DELETE");
        }

        Map<String, String> resultMap = new HashMap();
        resultMap.put("code", "512");
        resultMap.put("content-type", "text/plain; charset=UTF-8");
        resultMap.put("content", "Not supported http method currently");
        return resultMap;
    }

    /**
     * 发送java_noBody请求
     *
     * @param reqURL 请求地址
     * @return 远程主机响应正文`HTTP状态码,如<code>"SUCCESS`200"</code><br>
     * 若通信过程中发生异常则返回"Failed`HTTP状态码",如<code>"Failed`500"</code> // * @see 若发送的<code>params</code>
     * 中含有中文,记得按照双方约定的字符集将中文 <code>URLEncoder.encode(string,encodeCharset)</code>
     * // * @see 本方法默认的连接超时时间为30秒,默认的读取超时时间为30秒
     */
    private static Map<String, String> sendRequestByJavaWithoutBody(String reqURL, String contentType, String method) throws IOException {
        HttpURLConnection httpURLConnection = null;
        InputStream in = null; // 读
        int httpStatusCode = 0; // 远程主机响应的HTTP状态码
        try {
            URL sendUrl = new URL(reqURL);
            httpURLConnection = (HttpURLConnection) sendUrl.openConnection();
            httpURLConnection.setRequestMethod(method);
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setConnectTimeout(3000); // 30秒连接超时
            httpURLConnection.setReadTimeout(50000); // 30秒读取超时
            httpURLConnection.setRequestProperty(HTTP.CONTENT_TYPE, contentType);
            httpURLConnection.connect();

            // 获取HTTP状态码
            httpStatusCode = httpURLConnection.getResponseCode();
            in = httpURLConnection.getInputStream();

            String charset = httpURLConnection.getContentEncoding() == null ? Charset.defaultCharset().toString() : httpURLConnection.getContentEncoding();
            Map<String, String> resultMap = new HashMap();
            resultMap.put("code", httpStatusCode + "");
            resultMap.put("content-type", httpURLConnection.getContentType());
            resultMap.put("content", readLineString(in, charset));
            return resultMap;

        } catch (IOException e) {

            return sendHttpRequestByHttpClient(reqURL, null, ImmutableMap.of(CONTENT_TYPE, contentType), method);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw e;
                }
            }
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }

    /**
     * 发送java_body请求
     *
     * @param reqURL 请求地址
     * @param params 发送到远程主机的正文数据,其数据类型为<code>java.util.Map<String, String></code>
     * @return 远程主机响应正文`HTTP状态码,如<code>"SUCCESS`200"</code><br>
     * 若通信过程中发生异常则返回"Failed`HTTP状态码",如<code>"Failed`500"</code> // * @see 若发送的<code>params</code>
     * 中含有中文,记得按照双方约定的字符集将中文 <code>URLEncoder.encode(string,encodeCharset)</code>
     * // * @see 本方法默认的连接超时时间为30秒,默认的读取超时时间为30秒
     */
    private static Map<String, String> sendPostRequestByJava(String reqURL, Map<String, String> params, String contentType, String method) throws IOException {
        StringBuilder sendData = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sendData.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        if (sendData.length() > 0) {
            sendData.setLength(sendData.length() - 1); // 删除最后一个&符号
        }

        return sendRequestByJavaWithBody(reqURL, sendData.toString(), contentType, method);
    }

    /**
     * 发送java_body请求
     *
     * @param reqURL   请求地址
     * @param sendData 发送到远程主机的正文数据
     * @return 远程主机响应正文`HTTP状态码,如<code>"SUCCESS`200"</code><br>
     * 若通信过程中发生异常则返回"Failed`HTTP状态码",如<code>"Failed`500"</code> // * @see 若发送的<code>sendData</code>
     * 中含有中文,记得按照双方约定的字符集将中文 <code>URLEncoder.encode(string,encodeCharset)</code>
     * // * @see 本方法默认的连接超时时间为30秒,默认的读取超时时间为30秒
     */
    private static Map<String, String> sendRequestByJavaWithBody(String reqURL, String sendData, String contentType, String method) throws IOException {
        HttpURLConnection httpURLConnection = null;
        OutputStream out = null; // 写
        InputStream in = null; // 读
        int httpStatusCode = 0; // 远程主机响应的HTTP状态码
        try {
            URL sendUrl = new URL(reqURL);
            httpURLConnection = (HttpURLConnection) sendUrl.openConnection();
            httpURLConnection.setRequestMethod(method);
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setRequestProperty(HTTP.CONTENT_TYPE, contentType);
            httpURLConnection.setDoOutput(true); // 指示应用程序要将数据写入URL连接,其值默认为false
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setConnectTimeout(3000); // 50秒建立连接超时
            httpURLConnection.setReadTimeout(50000); // 50秒读取超时
            out = httpURLConnection.getOutputStream();
            out.write(sendData.toString().getBytes(getCharSet(null, contentType)));
            // 清空缓冲区,发送数据
            out.flush();
            // 获取HTTP状态码
            httpStatusCode = httpURLConnection.getResponseCode();

            in = httpURLConnection.getInputStream();
            String charset = httpURLConnection.getContentEncoding() == null ? Charset.defaultCharset().toString() : httpURLConnection.getContentEncoding();
            Map<String, String> resultMap = new HashMap();
            resultMap.put("code", httpStatusCode + "");
            resultMap.put("content-type", httpURLConnection.getContentType());
            resultMap.put("content", readLineString(in, charset));
            return resultMap;

        } catch (IOException e) {
            return sendHttpRequestByHttpClient(reqURL, sendData, ImmutableMap.of(CONTENT_TYPE, contentType), method);

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    throw e;
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw e;
                }
            }
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }

    private static String readLineString(InputStream inputStream, String charset) throws IOException {
        String res = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset));
        String str;
        while ((str = reader.readLine()) != null) {
            res += str + "\n";
        }

        return res;
    }

    /**
     * 不可靠，消息体过长会丢失
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    private static String readBytesString(InputStream inputStream) throws IOException {
        byte[] byteDatas = new byte[inputStream.available()];
        int length = 0;
        inputStream.read(byteDatas);
        while ((length = inputStream.available()) > 0) {
            byte[] array = byteDatas;
            byteDatas = new byte[array.length + length];
            System.arraycopy(array, 0, byteDatas, 0, array.length);
            inputStream.read(byteDatas, array.length, length);
        }

        return new String(byteDatas);
    }

    /**
     * 发送HTTPS_POST请求
     * <p/>
     * // * @see 该方法为<code>sendPostSSLRequest(String,Map<String,String>,String,String)</code> 方法的简化方法 // * @see
     * 该方法在对请求数据的编码和响应数据的解码时,所采用的字符集均为UTF-8 // * @see 该方法会自动对<code>params</code>中的[中文][|][ ]等特殊字符进行
     * <code>URLEncoder.encode(string,"UTF-8")</code>
     */
    public static String sendPostSSLRequest(String reqURL, Map<String, String> params) {
        return sendPostSSLRequest(reqURL, params, null, null);
    }

    /**
     * 发送HTTPS_POST请求
     *
     * @param reqURL        请求地址
     * @param params        请求参数
     * @param encodeCharset 编码字符集,编码请求数据时用之,其为null时默认采用UTF-8解码
     * @param decodeCharset 解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
     * @return 远程主机响应正文 // * @see 该方法会自动关闭连接,释放资源 // * @see 该方法会自动对<code>params</code>中的[中文][|][ ]等特殊字符进行
     * <code>URLEncoder.encode(string,encodeCharset)</code>
     */
    public static String sendPostSSLRequest(String reqURL, Map<String, String> params, String encodeCharset, String decodeCharset) {
        String responseContent = "";
        HttpClient httpClient = HttpClientFactory.concurrentClient(); // 创建默认的httpClient实例
        HttpEntity entity = null;

        try {
            HttpPost httpPost = new HttpPost(reqURL);
            encodeCharset = encodeCharset == null ? "UTF-8" : encodeCharset;
            decodeCharset = decodeCharset == null ? "UTF-8" : decodeCharset;
            httpPost.setHeader(HTTP.CONTENT_TYPE, "application/json; charset=" + encodeCharset);
            List<NameValuePair> formParams = new ArrayList<NameValuePair>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(formParams, encodeCharset == null ? "UTF-8" : encodeCharset));

            HttpResponse response = httpClient.execute(httpPost);
            entity = response.getEntity();
            if (null != entity) {
                responseContent = EntityUtils.toString(entity, decodeCharset == null ? "UTF-8" : decodeCharset);

            }
        } catch (Exception e) {
            return "Https POST Communication error msg:\n" + e.getMessage();
        } finally {
            try {
                EntityUtils.consume(entity);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        return responseContent;
    }

    /**
     * 发送HTTPS_POST请求
     *
     * @param reqURL        请求地址
     * @param requestData   请求参数
     * @param encodeCharset 编码字符集,编码请求数据时用之,其为null时默认采用UTF-8解码
     * @param decodeCharset 解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
     * @return 远程主机响应正文 // * @see 该方法会自动关闭连接,释放资源 // * @see 该方法会自动对<code>params</code>中的[中文][|][ ]等特殊字符进行
     * <code>URLEncoder.encode(string,encodeCharset)</code>
     */
    public static String sendPostSSLRequest(String reqURL, String requestData, String encodeCharset, String decodeCharset) {
        String responseContent = "";
        HttpClient httpClient = HttpClientFactory.concurrentClient(); // 创建默认的httpClient实例
        HttpEntity entity = null;

        try {
            HttpPost httpPost = new HttpPost(reqURL);
            encodeCharset = encodeCharset == null ? "UTF-8" : encodeCharset;
            decodeCharset = decodeCharset == null ? "UTF-8" : decodeCharset;
            httpPost.setHeader(HTTP.CONTENT_TYPE, "application/json; charset=" + encodeCharset);
            httpPost.setEntity(new StringEntity(requestData, encodeCharset == null ? "UTF-8" : encodeCharset));
            HttpResponse response = httpClient.execute(httpPost);
            entity = response.getEntity();
            if (null != entity) {
                responseContent = EntityUtils.toString(entity, decodeCharset == null ? "UTF-8" : decodeCharset);
            }

        } catch (Exception e) {
            return "Https POST Communication error msg:\n" + e.getMessage();
        } finally {
            try {
                EntityUtils.consume(entity);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        return responseContent;
    }


    public static void main(String[] args) throws IOException {
        HttpClientUtil.sendHttpRequestByHttpClient("https://graph.qq.com/oauth2.0/token?grant_type=authorization_code&client_id=101498602" + "&client_secret=058001492e99cb4a82b7c713abd11e81" + "&code=27E3E30771119E23E9107B8E051F8DD6" + "&redirect_uri=http%3A%2F%2Fsampletest.sparta.html5.qq.com%2FsamplePlatform%2Fsys%2Fuser%2Flogin", null, null, "get");
        System.out.print("done~");

    }


    /**
     *
     */
    public enum Method {
        POST, GET, DELETE;
    }
}