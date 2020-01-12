package com.alibaba.datax.plugin.writer.kafkawriter;

import com.alibaba.datax.common.spi.ErrorCode;

/**
 * @author maokeluo
 * @description 多隆镇楼，bug退散🙏🙏🙏
 *
 * @date 2020/1/12
 */
public enum KafkaWriterErrorCode implements ErrorCode {

    CONFIG_PARAMS_REQUIRED("CONFIG_PARAMS_REQUIRED", "必填参数缺失"),
    ILLEGAL_VALUE("ILLEGAL_VALUE", "数据不匹配");

    private final String code;
    private final String description;

    KafkaWriterErrorCode(String code, String description) {
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
