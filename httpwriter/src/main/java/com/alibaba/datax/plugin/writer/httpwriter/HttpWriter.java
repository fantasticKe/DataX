package com.alibaba.datax.plugin.writer.httpwriter;

import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.spi.Writer;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.writer.httpwriter.util.HttpClientPoolHelper;
import com.alibaba.datax.plugin.writer.httpwriter.util.HttpConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author maokeluo
 * @description 多隆镇楼，bug退散🙏🙏🙏
 * 以http的形式传输数据
 * @date 2019/12/19
 */
public class HttpWriter extends Writer {

    public static class Job extends Writer.Job {
        //提供多级JSON配置信息无损存储,采用json的形式
        private Configuration originalConfig;

        @Override
        public List<Configuration> split(int mandatoryNumber) {
            List<Configuration> list = new ArrayList<>();
            for (int i = 0; i < mandatoryNumber; i++) {
                //拷贝当前Configuration，注意，这里使用了深拷贝，避免冲突
                list.add(originalConfig.clone());
            }
            return list;
        }

        @Override
        public void init() {
            this.originalConfig = super.getPluginJobConf();
        }

        @Override
        public void destroy() {

        }
    }

    public static class Task extends Writer.Task {

        private static final Logger logger = LoggerFactory.getLogger(Task.class);

        private HttpClientPoolHelper httpClientPoolHelper;

        private String targetUrl;

        private String params;

        private JSONArray columns;

        public RateLimiter limiter;

        public String expectFiled;

        public String expectStatus;

        @Override
        public void startWrite(RecordReceiver lineReceiver) {
            if (Strings.isNullOrEmpty(targetUrl) || Strings.isNullOrEmpty(params))
                throw DataXException.asDataXException(HttpWriterErrorCode.ILLEGAL_PARAM,
                        HttpWriterErrorCode.ILLEGAL_PARAM.getDescription());
            Record record;
            while ((record = lineReceiver.getFromReader()) != null) {
                if (columns.size() != record.getColumnNumber())
                    throw DataXException.asDataXException(HttpWriterErrorCode.ILLEGAL_VALUE,
                            HttpWriterErrorCode.ILLEGAL_VALUE.getDescription() + ",读出字段个数:" + record.getColumnNumber() + ",配置字段个数:" + columns.size());
                JSONObject json = new JSONObject();
                for (int i = 0; i < columns.size(); i++) {
                    Column column = record.getColumn(i);
                    String columnName = columns.getJSONObject(i).getString(KeyConstant.COLUMN_NAME);
                    String columnType = columns.getJSONObject(i).getString(KeyConstant.COLUMN_TYPE).toUpperCase();
                    Object rawData = column.getRawData();
                    if (Objects.isNull(rawData)) {
                        json.put(columnName, null);
                        continue;
                    }
                    assignment(json, columnType, columnName, rawData.toString());
                }
                if (Objects.nonNull(limiter)) limiter.acquire();
                try {
                    JSONObject data = new JSONObject();
                    data.put(params, json);
                    String postRequest = httpClientPoolHelper.postRequest(targetUrl, data);
                    if (StringUtils.isNoneEmpty(expectFiled, expectStatus)) {
                        Object o = JSON.parseObject(postRequest).get(expectFiled);
                        if (!expectStatus.equalsIgnoreCase(o.toString())) {
                            logger.warn(json.toJSONString());
                        }
                    }
                } catch (Exception e) {
                    logger.error("post request failed:{}, {}", targetUrl, json, e);
                }
            }
        }

        @Override
        public void init() {
            Configuration configuration = super.getPluginJobConf();
            targetUrl = configuration.getNecessaryValue(KeyConstant.TARGET_URL, HttpWriterErrorCode.ILLEGAL_URL_ADDRESS);
            params = configuration.getNecessaryValue(KeyConstant.PARAM, HttpWriterErrorCode.ILLEGAL_PARAM);
            Integer limitCount = configuration.getInt(KeyConstant.LIMIT);
            expectFiled = configuration.getString(KeyConstant.EXPECT_FIELD);
            expectStatus = configuration.getString(KeyConstant.EXPECT_STATUS);
            if (Objects.nonNull(limitCount)) limiter = RateLimiter.create(limitCount);
            int retryCount = configuration.getInt(KeyConstant.HTTP_CONFIG_RETRY_COUNT, 3);
            int maxTotal = configuration.getInt(KeyConstant.HTTP_CONFIG_MAX_TOTAL, 200);
            int defaultMaxPerRoute = configuration.getInt(KeyConstant.HTTP_CONFIG_DEFAULT_MAX_PER_ROUTE, 200);
            int connectionRequestTimeout = configuration.getInt(KeyConstant.HTTP_CONFIG_CONNECTION_REQUEST_TIMEOUT, 2000);
            int connectTimeout = configuration.getInt(KeyConstant.HTTP_CONFIG_CONNECT_TIMEOUT, 500);
            int socketTimeout = configuration.getInt(KeyConstant.HTTP_CONFIG_SOCKET_TIMEOUT, 2000);
            String proxy = configuration.getString(KeyConstant.HTTP_CONFIG_PROXY);
            HttpConfig httpConfig = HttpConfig.newBuilder().retryCount(retryCount).maxTotal(maxTotal)
                    .defaultMaxPerRoute(defaultMaxPerRoute).connectionRequestTimeout(connectionRequestTimeout)
                    .connectTimeout(connectTimeout).socketTimeout(socketTimeout).proxy(proxy)
                    .build();
            httpClientPoolHelper = new HttpClientPoolHelper(httpConfig);
            columns = JSONArray.parseArray(configuration.getString(KeyConstant.COLUMN));
            if (CollectionUtils.isEmpty(columns))
                throw DataXException.asDataXException(HttpWriterErrorCode.ILLEGAL_COLUMN_CONFIG,
                        HttpWriterErrorCode.ILLEGAL_COLUMN_CONFIG.getDescription());
            columns.forEach(p -> {
                JSONObject column = (JSONObject) p;
                if (!column.containsKey(KeyConstant.COLUMN_INDEX) || !column.containsKey(KeyConstant.COLUMN_NAME) ||
                        !column.containsKey(KeyConstant.COLUMN_TYPE))
                    throw DataXException.asDataXException(HttpWriterErrorCode.ILLEGAL_COLUMN_CONFIG,
                            HttpWriterErrorCode.ILLEGAL_COLUMN_CONFIG.getDescription());
            });
        }

        @Override
        public void destroy() {
            CloseableHttpClient httpClient = httpClientPoolHelper.getHttpClient();
            if (Objects.nonNull(httpClient)) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    logger.error("close http client failed:", e);
                }
            }
        }

        private static void assignment(JSONObject json, String columnType, String columnName, String rawData) {
            switch (columnType) {
                case "INT":
                    json.put(columnName, Integer.parseInt(rawData));
                    break;
                case "LONG":
                    json.put(columnName, Long.parseLong(rawData));
                    break;
                case "DOUBLE":
                    json.put(columnName, Double.parseDouble(rawData));
                    break;
                case "BOOL":
                    json.put(columnName, Boolean.parseBoolean(rawData));
                    break;
                case "JSON":
                    try {
                        JSONObject object = JSON.parseObject(rawData);
                        json.put(columnName, object);
                    } catch (JSONException e) {
                        json.put(columnName, null);
                        logger.error("parse json failed:{}", rawData, e);
                    }
                    break;
                case "JSONARRAY":
                    try {
                        JSONArray array = JSON.parseArray(rawData);
                        json.put(columnName, array);
                    } catch (JSONException e) {
                        json.put(columnName, null);
                        logger.error("parse jsonArray failed:{}", rawData, e);
                    }
                    break;
                default:
                    json.put(columnName, rawData);
            }
        }
    }

}
