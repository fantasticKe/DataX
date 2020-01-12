package com.alibaba.datax.plugin.reader.kafkareader;

import com.alibaba.datax.common.spi.ErrorCode;

/**
 * @author maokeluo
 * @description 多隆镇楼，bug退散🙏🙏🙏
 *
 * @date 2020/1/12
 */
public enum KafkaReaderErrorCode implements ErrorCode {
    CONFIG_PARAMS_ERROR("CONFIG_PARAMS_ERROR", "参数配置错误: "),
    GET_PARTITION_ERROR("GET_PARTITION_ERROR", "获取offset信息失败:");

    private final String code;
    private final String description;

    KafkaReaderErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
