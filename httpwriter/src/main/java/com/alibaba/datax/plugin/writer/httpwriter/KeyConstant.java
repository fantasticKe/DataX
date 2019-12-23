package com.alibaba.datax.plugin.writer.httpwriter;

/**
 * @author maokeluo
 * @description 多隆镇楼，bug退散🙏🙏🙏
 * 配置文件Key
 * @date 2019/12/23
 */
public class KeyConstant {

    /**
     * 目标url地址
     */
    public static final String TARGET_URL = "targetUrl";

    /**
     * 参数名称
     */
    public static final String PARAM = "param";

    /**
     * 每秒请求数限制
     */
    public static final String LIMIT = "limit";

    /**
     * http请求失败重试次数
     */
    public static final String HTTP_CONFIG_RETRY_COUNT = "httpConfig.retryCount";

    /**
     * 连接池最大连接数
     */
    public static final String HTTP_CONFIG_MAX_TOTAL = "httpConfig.maxTotal";

    /**
     * 每个路由基础的最大连接数
     */
    public static final String HTTP_CONFIG_DEFAULT_MAX_PER_ROUTE = "httpConfig.defaultMaxPerRoute";

    /**
     * 从连接管理器请求连接时使用的超时
     */
    public static final String HTTP_CONFIG_CONNECTION_REQUEST_TIMEOUT = "httpConfig.connectionRequestTimeout";

    /**
     * 连接超时
     */
    public static final String HTTP_CONFIG_CONNECT_TIMEOUT = "httpConfig.connectTimeout";

    /**
     * 等待数据超时
     */
    public static final String HTTP_CONFIG_SOCKET_TIMEOUT = "httpConfig.socketTimeout";

    /**
     * 代理地址
     */
    public static final String HTTP_CONFIG_PROXY = "httpConfig.proxy";

    /**
     * 输出字段
     */
    public static final String COLUMN = "column";

    /**
     * 列下标
     */
    public static final String COLUMN_INDEX = "index";

    /**
     * 列名
     */
    public static final String COLUMN_NAME = "name";
    
    /**
     * 每个列的类型
     */
    public static final String COLUMN_TYPE = "type";
}
