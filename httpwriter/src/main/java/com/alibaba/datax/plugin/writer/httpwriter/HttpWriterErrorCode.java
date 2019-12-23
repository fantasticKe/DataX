package com.alibaba.datax.plugin.writer.httpwriter;

import com.alibaba.datax.common.spi.ErrorCode;

/**
 * @author maokeluo
 * @description 多隆镇楼，bug退散🙏🙏🙏
 * 错误码枚举
 * @date 2019/12/19
 */
public enum HttpWriterErrorCode implements ErrorCode {
    ILLEGAL_URL_ADDRESS("ILLEGAL_ADDRESS","不合法的URL地址"),
    ILLEGAL_METHOD("ILLEGAL_METHOD", "错误的请求方式,只支持POST请求方式"),
    ILLEGAL_PARAM("ILLEGAL_PARAM", "参数错误"),
    ILLEGAL_COLUMN_CONFIG("ILLEGAL_COLUMN_CONFIG", "列名配置错误"),
    ILLEGAL_VALUE("ILLEGAL_VALUE", "数据不匹配"),
    ILLEGAL_REQUEST("ILLEGAL_REQUEST", "请求失败");

    private final String code;
    private final String description;

    HttpWriterErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return String.format("Code:[%s], Description:[%s].", this.code,
                this.description);
    }
}
