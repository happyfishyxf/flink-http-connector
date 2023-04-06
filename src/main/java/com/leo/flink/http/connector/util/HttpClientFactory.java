/**
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.leo.flink.http.connector.util;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BestMatchSpecFactory;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.impl.cookie.BrowserCompatSpecFactory;
import org.apache.http.pool.PoolStats;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

/**
 * httpclient创建工厂类
 * Date: 16/4/25
 * Time: 下午9:12
 */
public class HttpClientFactory {

    private static Logger logger = LoggerFactory.getLogger(HttpClientFactory.class);
    /**
     * 最终执行方法
     */
    private static CloseableHttpClient concurrentClient;

    /**
     * 避免双检锁问题
     */
    static {
        try {
            //采用绕过验证的方式处理https请求
            SSLContext sslcontext = createIgnoreVerifySSL();
            // 设置协议http和https对应的处理socket链接工厂的对象
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.INSTANCE).register("https", new SSLConnectionSocketFactory(sslcontext)).build();

            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            cm.setMaxTotal(50); // 最多50个线程同时请求
            cm.setDefaultMaxPerRoute(50); // 一个链接最多10个线程同时请求
            cm.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(35000).build());
            // 定制cookie策略
            CookieSpecProvider easySpecProvider = new CookieSpecProvider() {
                public BrowserCompatSpec create(HttpContext context) {
                    return new BrowserCompatSpec() {
                        @SuppressWarnings("unused")
                        public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
                        }
                    };
                }
            };

            Registry<CookieSpecProvider> r = RegistryBuilder.<CookieSpecProvider>create().register(CookieSpecs.BEST_MATCH, new BestMatchSpecFactory()).register(CookieSpecs.BROWSER_COMPATIBILITY, new BrowserCompatSpecFactory()).register("easy", easySpecProvider).build();
            RequestConfig requestConfig = RequestConfig.custom().setCookieSpec("easy").setSocketTimeout(10 * 60 * 1000).setConnectTimeout(10 * 60 * 1000).setConnectionRequestTimeout(35000).build();

            concurrentClient = HttpClients.custom().setDefaultCookieSpecRegistry(r).setDefaultRequestConfig(requestConfig).setConnectionManager(cm).build();

            // 启动一个daemon 线程来实现连接池监控
            startMonitor(cm);


        } catch (Exception e) {
            logger.error("Errors occurs when create httpClient,{}", e.getMessage());
        }
    }

    /**
     * 启动连接池监控和回收
     *
     * @param cm
     */
    private static void startMonitor(PoolingHttpClientConnectionManager cm) {
        Thread monitor = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60 * 5 * 1000);
                    cm.closeExpiredConnections();
                    cm.closeIdleConnections(5 * 60 * 1000, TimeUnit.MILLISECONDS);

                    PoolStats poolStats = cm.getTotalStats();
                    logger.info("Pool stats: {}", poolStats);

                } catch (Exception e) {
                    logger.error("Some errors occur when monitor running...");
                }
            }
        });
        monitor.setDaemon(true);
        monitor.start();
    }

    /**
     * 线程安全请求客户端
     *
     * @return　多连接客户端
     */
    public static CloseableHttpClient concurrentClient() {
        return concurrentClient;
    }

    /**
     * 单线程客户端
     *
     * @return 单链接客户端
     */
    public static CloseableHttpClient newClient() {
        return HttpClients.custom().build();
    }


    /**
     * 绕过验证
     *
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    public static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sc = SSLContext.getInstance("SSLv3");

        // 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
        X509TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate, String paramString) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate, String paramString) throws CertificateException {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        sc.init(null, new TrustManager[]{trustManager}, null);

        return sc;
    }
}
