package com.alibaba.datax.plugin.writer.kafkawriter;

/**
 * @author maokeluo
 * @description 多隆镇楼，bug退散🙏🙏🙏
 *
 * @date 2020/1/12
 */
public class KeyConstant {

    /**
     * kafka集群地址
     */
    public static final String BOOTSTRAP_SERVERS = "bootstrapServers";

    /**
     * 订阅主题
     */
    public static final String TOPIC = "topic";

    /**
     * 请求完成确认数
     */
    public static final String ACKS = "acks";

    /**
     * 失败重试次数
     */
    public static final String RETRIES = "retries";

    /**
     * 间隔大小
     */
    public static final String LINGER_MS = "lingerMs";
    /**
     * 批大小
     */
    public static final String BATCH_SIZE = "batchSize";

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
