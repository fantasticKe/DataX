package com.alibaba.datax.plugin.writer.kafkawriter;

import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.spi.Writer;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * @author maokeluo
 * @description 多隆镇楼，bug退散🙏🙏🙏
 * 写入数据到kafka
 * @date 2020/1/12
 */
public class KafkaWriter extends Writer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaWriter.class);

    public static class Job extends Writer.Job {

        private Configuration originalConfig;

        @Override
        public List<Configuration> split(int mandatoryNumber) {
            List<Configuration> list = new ArrayList<>();
            for (int i = 0; i < mandatoryNumber; i++) {
                list.add(originalConfig.clone());
            }
            return list;
        }

        @Override
        public void init() {
            this.originalConfig = super.getPluginJobConf();
            this.originalConfig.getNecessaryValue(KeyConstant.BOOTSTRAP_SERVERS, KafkaWriterErrorCode.CONFIG_PARAMS_REQUIRED);
            this.originalConfig.getNecessaryValue(KeyConstant.TOPIC, KafkaWriterErrorCode.CONFIG_PARAMS_REQUIRED);
            this.originalConfig.getNecessaryValue(KeyConstant.COLUMN, KafkaWriterErrorCode.CONFIG_PARAMS_REQUIRED);
        }

        @Override
        public void destroy() {

        }
    }

    public static class Task extends Writer.Task {

        private Configuration configuration;

        private Producer<String, String> producer;

        private String topic;

        private JSONArray columns;

        @Override
        public void startWrite(RecordReceiver lineReceiver) {
            Record record;
            while ((record = lineReceiver.getFromReader()) != null) {
                if (columns.size() != record.getColumnNumber())
                    throw DataXException.asDataXException(KafkaWriterErrorCode.ILLEGAL_VALUE,
                            KafkaWriterErrorCode.ILLEGAL_VALUE.getDescription() + ",读出字段个数:" + record.getColumnNumber() + ",配置字段个数:" + columns.size());
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
                producer.send(new ProducerRecord<String, String>(topic, json.toJSONString()));
            }
        }

        @Override
        public void init() {
            this.configuration = super.getPluginJobConf();
            String bootstrapServers = configuration.getString(KeyConstant.BOOTSTRAP_SERVERS);
            this.topic = configuration.getString(KeyConstant.TOPIC);
            String acks = configuration.getString(KeyConstant.ACKS, "all");
            int retries = configuration.getInt(KeyConstant.RETRIES, 0);
            long batchSize = configuration.getLong(KeyConstant.BATCH_SIZE, 16384L);
            long lingerMs = configuration.getLong(KeyConstant.LINGER_MS, 1L);
            columns = JSON.parseArray(configuration.getString(KeyConstant.COLUMN));
            if (CollectionUtils.isEmpty(columns))
                throw DataXException.asDataXException(KafkaWriterErrorCode.CONFIG_PARAMS_REQUIRED,
                        KafkaWriterErrorCode.CONFIG_PARAMS_REQUIRED.getDescription());
            columns.forEach(p -> {
                JSONObject column = (JSONObject) p;
                if (!column.containsKey(KeyConstant.COLUMN_INDEX) || !column.containsKey(KeyConstant.COLUMN_NAME) ||
                        !column.containsKey(KeyConstant.COLUMN_TYPE))
                    throw DataXException.asDataXException(KafkaWriterErrorCode.CONFIG_PARAMS_REQUIRED,
                            KafkaWriterErrorCode.CONFIG_PARAMS_REQUIRED.getDescription());
            });
            Properties props = new Properties();
            props.put("bootstrap.servers", bootstrapServers);
            props.put("acks", acks);
            props.put("retries", retries);
            props.put("batch.size", batchSize);
            props.put("linger.ms", lingerMs);
            producer = new KafkaProducer<>(props);
        }

        @Override
        public void destroy() {
            if (Objects.nonNull(producer)) {
                producer.close();
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
                        LOGGER.error("parse json failed:{}", rawData, e);
                    }
                    break;
                case "JSONARRAY":
                    try {
                        JSONArray array = JSON.parseArray(rawData);
                        json.put(columnName, array);
                    } catch (JSONException e) {
                        json.put(columnName, null);
                        LOGGER.error("parse jsonArray failed:{}", rawData, e);
                    }
                    break;
                default:
                    json.put(columnName, rawData);
            }
        }
    }
}
