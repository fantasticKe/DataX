package com.alibaba.datax.plugin.reader.kafkareader;

/**
 * @author maokeluo
 * @description 多隆镇楼，bug退散🙏🙏🙏
 * 配置文件Key
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
     * 分区
     */
    public static final String PARTITIONS = "partitions";

    /**
     * 消费者组id
     */
    public static final String GROUP_ID = "groupId";

    /**
     * 各开始消费的offset
     */
    public static final String PARTITIONS_OFFSET = "partitionsOffset";

    /**
     * 会话超时时长
     */
    public static final String SESSION_TIMEOUT_MS = "sessionTimeoutMs";

    /**
     * 指定要发送的数据字段
     */
    public static final String COLUMN = "column";

    /**
     * 指定要发送的数据字段名称
     */
    public static final String KEY = "key";

    /**
     * 指定要发送的数据字段类型
     */
    public static final String TYPE = "type";
}
