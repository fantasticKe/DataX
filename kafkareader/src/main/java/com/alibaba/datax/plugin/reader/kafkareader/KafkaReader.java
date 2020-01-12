package com.alibaba.datax.plugin.reader.kafkareader;

import com.alibaba.datax.common.element.*;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author maokeluo
 * @description 多隆镇楼，bug退散🙏🙏🙏
 * 从kafka中读取数据
 * @date 2020/1/12
 */
public class KafkaReader extends Reader {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaReader.class);

    public static class Job extends Reader.Job {

        private Configuration originalConfig = null;

        @Override
        public List<Configuration> split(int adviceNumber) {
            List<Configuration> configurations = new ArrayList<>();
            int partitions = this.originalConfig.getInt(KeyConstant.PARTITIONS);
            for (int i = 0; i < partitions; i++) {
                configurations.add(this.originalConfig.clone());
            }
            return configurations;
        }

        @Override
        public void init() {
            this.originalConfig = super.getPluginJobConf();
            String bootstrapServers = this.originalConfig.getString(KeyConstant.BOOTSTRAP_SERVERS);
            if (StringUtils.isEmpty(bootstrapServers))
                throw DataXException.asDataXException(KafkaReaderErrorCode.CONFIG_PARAMS_ERROR,
                        KafkaReaderErrorCode.CONFIG_PARAMS_ERROR.getDescription() + KeyConstant.BOOTSTRAP_SERVERS);

            String topic = this.originalConfig.getString(KeyConstant.TOPIC);
            if (StringUtils.isEmpty(topic))
                throw DataXException.asDataXException(KafkaReaderErrorCode.CONFIG_PARAMS_ERROR,
                        KafkaReaderErrorCode.CONFIG_PARAMS_ERROR.getDescription() + KeyConstant.TOPIC);

            int partitions = this.originalConfig.getInt(KeyConstant.PARTITIONS);
            if (partitions < 1)
                throw DataXException.asDataXException(KafkaReaderErrorCode.CONFIG_PARAMS_ERROR,
                        KafkaReaderErrorCode.CONFIG_PARAMS_ERROR.getDescription() + KeyConstant.PARTITIONS);

            String groupId = this.originalConfig.getString(KeyConstant.GROUP_ID);
            if (StringUtils.isEmpty(groupId))
                throw DataXException.asDataXException(KafkaReaderErrorCode.CONFIG_PARAMS_ERROR,
                        KafkaReaderErrorCode.CONFIG_PARAMS_ERROR.getDescription() + KeyConstant.GROUP_ID);

            List<JSONObject> columns = this.originalConfig.getList(KeyConstant.COLUMN, JSONObject.class);
            if (CollectionUtils.isEmpty(columns))
                throw DataXException.asDataXException(KafkaReaderErrorCode.CONFIG_PARAMS_ERROR,
                        KafkaReaderErrorCode.CONFIG_PARAMS_ERROR.getDescription() + KeyConstant.COLUMN);
            LOGGER.info("KafkaConfig bootstrapServers:{}, topics:{}, partitions:{}, groupId:{}, columns:{}",
                    bootstrapServers, topic, partitions, groupId, columns.toString());
        }

        @Override
        public void preCheck() {
            init();
        }

        @Override
        public void destroy() {

        }
    }

    public static class Task extends Reader.Task {

        private Configuration configuration;

        private KafkaConsumer<String, String> consumer;

        private long lastOffset;

        private String bootstrapServers;

        private String kafkaTopic;

        private String groupId;

        private Map<String, Object> partitionsOffset;

        private int sessionTimeoutMs;

        private List<JSONObject> columns;

        //消费数据条数
        private AtomicInteger count;

        //是否继续消费数据
        private AtomicBoolean flag;

        @Override
        public void startRead(RecordSender recordSender) {
            try {
                while (flag.get()) {
                    ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : consumerRecords) {
                        if (Objects.isNull(record)) continue;
                        try {
                            JSONObject object = JSON.parseObject(record.value());
                            Record buildRecord = buildRecord(recordSender, columns, object);
                            recordSender.sendToWriter(buildRecord);
                            count.addAndGet(1);
                            if (count.get() % 100 == 0) {
                                LOGGER.info("consumer data: {}", count.get());
                            }
                            if (record.offset() >= lastOffset) flag.set(false);
                        } catch (Exception e) {
                            LOGGER.error("parse value failed:{}, {}, {}, {}", record.topic(), record.offset(), record.value(), e.getMessage());
                        }
                    }
                    consumer.commitAsync();
                }
            } catch (CommitFailedException e) {
                LOGGER.error("commit offset failed", e);
            } finally {
                try {
                    //关闭消费者前，使用commitSync，直到提交成成功或者发生无法恢复的错误
                    consumer.commitSync();
                } finally {
                    consumer.close();
                }
            }
        }

        @Override
        public void init() {
            this.count = new AtomicInteger(0);
            this.flag = new AtomicBoolean(true);
            this.configuration = super.getPluginJobConf();
            this.bootstrapServers = configuration.getString(KeyConstant.BOOTSTRAP_SERVERS);
            this.kafkaTopic = configuration.getString(KeyConstant.TOPIC);
            this.groupId = configuration.getString(KeyConstant.GROUP_ID);
            this.partitionsOffset = configuration.getMap(KeyConstant.PARTITIONS_OFFSET);
            this.sessionTimeoutMs = configuration.getInt(KeyConstant.SESSION_TIMEOUT_MS, 60000);
            this.columns = configuration.getList(KeyConstant.COLUMN, JSONObject.class);

            Properties props = initConfig();
            consumer = new KafkaConsumer<>(props);
            List<TopicPartition> partitions = consumer.partitionsFor(kafkaTopic).stream()
                    .map(p -> new TopicPartition(p.topic(), p.partition()))
                    .collect(Collectors.toList());
            consumer.assign(partitions);
            try {
                lastOffset = partitions.stream().map(consumer::position).max(Long::compareTo).orElse(0L);
            }catch (Exception e){
                throw DataXException.asDataXException(KafkaReaderErrorCode.GET_PARTITION_ERROR,
                        KafkaReaderErrorCode.GET_PARTITION_ERROR.getDescription()+e.getMessage());
            }
            if (partitionsOffset != null && !partitionsOffset.isEmpty()) {
                partitions.forEach(tp -> {
                    if (partitionsOffset.containsKey(String.valueOf(tp.partition()))) {
                        String key = String.valueOf(tp.partition());
                        consumer.seek(tp, Long.parseLong(partitionsOffset.get(key).toString()));
                    }
                });
            } else {
                consumer.subscribe(Collections.singleton(kafkaTopic));
            }
        }

        @Override
        public void destroy() {

        }

        private Properties initConfig() {
            Properties props = new Properties();
            props.put("bootstrap.servers", this.bootstrapServers);
            props.put("group.id", this.groupId);
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("enable.auto.commit", "false");
            props.put("session.timeout.ms", this.sessionTimeoutMs);
            return props;
        }

        private Record buildRecord(RecordSender recordSender, List<JSONObject> columns, JSONObject value) {
            Record record = recordSender.createRecord();
            for (JSONObject columnConfig : columns) {
                record.addColumn(buildColumn(columnConfig, value));
            }
            return record;
        }

        private Column buildColumn(JSONObject columnConfig, JSONObject value) {
            String key = columnConfig.getString(KeyConstant.KEY);
            String type = columnConfig.getString(KeyConstant.TYPE).toUpperCase();
            if (!value.containsKey(key)) return new StringColumn();
            Column column = null;
            switch (type) {
                case "INT":
                    column = new LongColumn(value.getInteger(key));
                    break;
                case "LONG":
                    column = new LongColumn(value.getLong(key));
                    break;
                case "DOUBLE":
                    column = new DoubleColumn(value.getDouble(key));
                    break;
                case "JSON":
                    column = new StringColumn(value.getJSONObject(key).toJSONString());
                    break;
                case "JSONARRAY":
                    column = new StringColumn(value.getJSONArray(key).toJSONString());
                    break;
                default:
                    column = new StringColumn(value.get(key).toString());
            }
            return column;
        }
    }
}
