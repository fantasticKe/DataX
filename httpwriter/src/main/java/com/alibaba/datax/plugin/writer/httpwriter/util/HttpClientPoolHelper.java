package com.alibaba.datax.plugin.writer.httpwriter.util;

import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author maokeluo
 * @description 多隆镇楼，bug退散🙏🙏🙏
 * Http工具类 连接池创建
 * @date 2019/12/19
 */
public class HttpClientPoolHelper {

    private static Logger logger = LoggerFactory.getLogger(HttpClientPoolHelper.class);

    private CloseableHttpClient httpClient;

    private RequestConfig requestConfig;


    public HttpClientPoolHelper(HttpConfig httpConfig) {

        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
        LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", plainsf)
                .register("https", sslsf)
                .build();

        //请求重试处理
        HttpRequestRetryHandler httpRequestRetryHandler = (exception, executionCount, context) -> {
            // 重试次数
            if (executionCount >= httpConfig.getRetryCount()) {
                return false;
            }
            // 如果服务器丢掉了连接，那么就重试
            if (exception instanceof NoHttpResponseException) {
                return true;
            }
            // 不要重试SSL握手异常
            if (exception instanceof SSLHandshakeException) {
                return false;
            }
            // 超时
            if (exception instanceof InterruptedIOException) {
                return false;
            }
            // 目标服务器不可达
            if (exception instanceof UnknownHostException) {
                return false;
            }
            // ssl握手异常
            if (exception instanceof SSLException) {
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();

            return !(request instanceof HttpEntityEnclosingRequest);
        };

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);

        cm.setDefaultSocketConfig(SocketConfig.custom().setSoKeepAlive(true).build());

        // 最大连接数
        cm.setMaxTotal(httpConfig.getMaxTotal());
        // 每个路由基础的连接数
        cm.setDefaultMaxPerRoute(httpConfig.getDefaultMaxPerRoute());

        httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setRetryHandler(httpRequestRetryHandler)
                .build();

        // 配置请求的超时设置。单位都是毫秒
        // ConnectTimeout:连接超时
        // SocketTimeout:等待数据超时
        // ConnectionRequestTimeout:从连接管理器请求连接时使用的超时
        //proxy: 代理地址
        HttpHost httpHost = null;
        if (StringUtils.isNotEmpty(httpConfig.getProxy())) {
            httpHost = HttpHost.create(httpConfig.getProxy());
        }
        requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(httpConfig.getConnectionRequestTimeout())
                .setConnectTimeout(httpConfig.getConnectTimeout())
                .setSocketTimeout(httpConfig.getSocketTimeout())
                .setProxy(httpHost)
                .build();
    }

    /**
     * get方式发起请求
     *
     * @param targetUrl 请求地址
     * @return
     * @throws Exception
     */
    public String getRequest(String targetUrl) throws Exception {
        long start = System.currentTimeMillis();

        if (StringUtils.isBlank(targetUrl)) {
            throw new IllegalArgumentException("调用getRequest方法，targetUrl不能为空!");
        }

        String responseResult;
        CloseableHttpResponse response = null;
        try {
            HttpGet httpget = new HttpGet(targetUrl);
            httpget.setConfig(requestConfig);
            response = httpClient.execute(httpget);
            responseResult = EntityUtils.toString(response.getEntity(), "UTF-8");
        } finally {
            // 释放链接
            if (Objects.nonNull(response)) response.close();
        }
        long end = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
            logger.debug("getRequest " + targetUrl + " elapse " + (end - start) + " ms ");
        }
        return responseResult;
    }

    /**
     * @param targetUrl      地址
     * @param requestContent 参数
     * @return
     * @throws Exception
     */
    public String postRequest(String targetUrl, Map<String, Object> requestContent) throws Exception {
        long start = System.currentTimeMillis();
        if (StringUtils.isBlank(targetUrl)) {
            throw new IllegalArgumentException("调用postRequest方法，targetUrl不能为空!");
        }
        String responseResult;
        CloseableHttpResponse response = null;
        try {
            HttpPost httppost = new HttpPost(targetUrl);
            // 设置超时
            httppost.setConfig(requestConfig);
            if (Objects.nonNull(requestContent) && !requestContent.isEmpty()) {
                List<NameValuePair> formParams = new ArrayList<>();
                requestContent.forEach((k, v) -> formParams.add(new BasicNameValuePair(k, String.valueOf(v))));
                UrlEncodedFormEntity uefEntity = new UrlEncodedFormEntity(formParams, "utf-8");
                httppost.setEntity(uefEntity);
            }
            response = httpClient.execute(httppost);
            responseResult = EntityUtils.toString(response.getEntity(), "UTF-8");
        } finally {
            // 释放链接
            if (Objects.nonNull(response)) response.close();
        }
        long end = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
            logger.debug("postRequest " + targetUrl + " elapse " + (end - start) + " ms ");
        }
        return responseResult;
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public RequestConfig getRequestConfig() {
        return requestConfig;
    }
}
