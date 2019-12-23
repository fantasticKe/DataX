package com.alibaba.datax.plugin.writer.httpwriter.util;

/**
 * @author maokeluo
 * @description 多隆镇楼，bug退散🙏🙏🙏
 * 配置项
 * @date 2019/12/20
 */
public class HttpConfig {
    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 最大连接数
     */
    private int maxTotal;

    /**
     * 每个路由基础的最大连接数
     */
    private int defaultMaxPerRoute;

    /**
     * 从连接管理器请求连接时使用的超时
     */
    private int connectionRequestTimeout;

    /**
     * 连接超时
     */
    private int connectTimeout;

    /**
     * 等待数据超时
     */
    private int socketTimeout;

    /**
     * 代理地址
     */
    private String proxy;

    public HttpConfig() {
    }

    private HttpConfig(Builder builder) {
        setRetryCount(builder.retryCount);
        setMaxTotal(builder.maxTotal);
        setDefaultMaxPerRoute(builder.defaultMaxPerRoute);
        setConnectionRequestTimeout(builder.connectionRequestTimeout);
        setConnectTimeout(builder.connectTimeout);
        setSocketTimeout(builder.socketTimeout);
        setProxy(builder.proxy);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public int getDefaultMaxPerRoute() {
        return defaultMaxPerRoute;
    }

    public void setDefaultMaxPerRoute(int defaultMaxPerRoute) {
        this.defaultMaxPerRoute = defaultMaxPerRoute;
    }

    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public static final class Builder {
        private int retryCount;
        private int maxTotal;
        private int defaultMaxPerRoute;
        private int connectionRequestTimeout;
        private int connectTimeout;
        private int socketTimeout;
        private String proxy;

        private Builder() {
        }

        public Builder retryCount(int val) {
            retryCount = val;
            return this;
        }

        public Builder maxTotal(int val) {
            maxTotal = val;
            return this;
        }

        public Builder defaultMaxPerRoute(int val) {
            defaultMaxPerRoute = val;
            return this;
        }

        public Builder connectionRequestTimeout(int val) {
            connectionRequestTimeout = val;
            return this;
        }

        public Builder connectTimeout(int val) {
            connectTimeout = val;
            return this;
        }

        public Builder socketTimeout(int val) {
            socketTimeout = val;
            return this;
        }

        public Builder proxy(String val) {
            proxy = val;
            return this;
        }

        public HttpConfig build() {
            return new HttpConfig(this);
        }
    }
}
