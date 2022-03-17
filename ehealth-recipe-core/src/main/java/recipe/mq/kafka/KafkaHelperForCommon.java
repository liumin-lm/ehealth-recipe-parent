package recipe.mq.kafka;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.sun.management.OperatingSystemMXBean;
import ctd.net.broadcast.exception.KafkaException;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Properties;

public class KafkaHelperForCommon {
    private static final Logger log = LoggerFactory.getLogger(ctd.net.broadcast.kafka.KafkaHelper.class);
    private static final String SYS_PROP_KAFKA_SERVERS = "kafka.servers";
    private static final String SYS_PROP_NS_PUBLIC = "apollo.ns_public";
    private static final String PUBLIC_NAMESPACE = "DEV.publicUrl";
    private static final String COMMONORDER_GROUPID = "drugList-consumer";
    private static String rpcStatLogTopic;
    private static final String RPC_STAT_LOG_TOPIC = "rpc.statLog.topic";
    private static KafkaPublisherString publisher;
    private static KafkaSubscriberString subscriber;

    public KafkaHelperForCommon() {
    }

    public static KafkaPublisherString getPublisher() {
        initKafkaProducer("");
        return publisher;
    }

    public static KafkaPublisherString getPublisher(String kafkaServers) {
        initKafkaProducer(kafkaServers);
        return publisher;
    }

    public void setPublisher(KafkaPublisherString publisher) {
        publisher = publisher;
    }

    public static KafkaSubscriberString getSubscriber() throws KafkaException {
        initKafkaConsumer("");
        return subscriber;
    }

    public void setSubscriber(KafkaSubscriberString subscriber) {
        subscriber = subscriber;
    }

    public static void publish(String topic, String key, Object message) {
        initKafkaProducer("");

        try {
            publisher.publish(topic, key, message);
        } catch (IOException var4) {
            log.error(var4.getMessage(), var4);
        }

    }

    public static void publish(String topic, Object message) {
        initKafkaProducer("");

        try {
            publisher.publish(topic, message);
        } catch (IOException var3) {
            log.error(var3.getMessage(), var3);
        }

    }

    public static void publish(String kafkaServers, String topic, String key, Object message) {
        initKafkaProducer(kafkaServers);

        try {
            publisher.publish(topic, key, message);
        } catch (IOException var5) {
            log.error(var5.getMessage(), var5);
        }

    }

    private static void initKafkaProducer(String kafkaServers) {
        if (publisher == null) {
            Properties props = new Properties();
            if (StringUtils.isEmpty(kafkaServers)) {
                kafkaServers = getKafkaServers();
            }

            if (StringUtils.isEmpty(kafkaServers)) {
                log.error("kafka servers not found,the rpc log will not be sent,please check!");
                throw new IllegalArgumentException("kafka bootstrap urls not configured,please check the kafka.servers configure");
            }

            props.put("bootstrap.servers", kafkaServers);
            props.put("key.serializer", StringSerializer.class.getName());
            props.put("value.serializer", StringSerializer.class.getName());
            props.put("acks", "0");
            props.put("retries", Integer.valueOf(0));
            props.put("max.block.ms", Integer.valueOf(6000));
            props.put("batch.size", Integer.valueOf(16384));
            OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            if ("aarch64".equals(os.getArch())) {
                props.put("compression.type", "gzip");
            } else {
                props.put("compression.type", "snappy");
            }

            publisher = new KafkaPublisherString();
            publisher.setProps(props);
        }

    }

    private static void initKafkaConsumer(String kafkaServers) throws KafkaException {
        if (subscriber == null) {
            Properties props = createConsumerConfig(kafkaServers, "drugList-consumer");
            subscriber = new KafkaSubscriberString();
            subscriber.setProps(props);
        }

    }

    private static Properties createConsumerConfig(String kafkaServers, String groupId) throws KafkaException {
        Properties props = new Properties();
        if (StringUtils.isEmpty(kafkaServers)) {
            kafkaServers = getKafkaServers();
        }

        if (StringUtils.isEmpty(kafkaServers)) {
            log.error("kafka servers not found,the subscriber will not work,please check!");
            throw new KafkaException(KafkaException.KAFKA_SERVER_NOT_FOUNT, "kafka servers not found,the subscriber will not work,please check!");
        } else {
            props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
            props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
            props.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
            props.setProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "25000");
            props.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");
            props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
            props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
            props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
            return props;
        }
    }

    public static KafkaSubscriberString createSubscriber() throws KafkaException {
        KafkaSubscriberString subscriber = new KafkaSubscriberString();
        subscriber.setProps(createConsumerConfig("", COMMONORDER_GROUPID));
        return subscriber;
    }

    public static KafkaSubscriberString createSubscriber(int threads) throws KafkaException {
        KafkaSubscriberString subscriber = new KafkaSubscriberString(threads);
        subscriber.setProps(createConsumerConfig("", COMMONORDER_GROUPID));
        return subscriber;
    }

    public static KafkaSubscriberString createSubscriber(int threads, String groupId) throws KafkaException {
        KafkaSubscriberString subscriber = new KafkaSubscriberString(threads);
        subscriber.setProps(createConsumerConfig("", groupId));
        return subscriber;
    }

    public static KafkaSubscriberString createSubscriber(String groupId) throws KafkaException {
        KafkaSubscriberString subscriber = new KafkaSubscriberString();
        subscriber.setProps(createConsumerConfig("", groupId));
        return subscriber;
    }

    public static KafkaSubscriberString createSubscriber(String kafkaServers, String groupId) throws KafkaException {
        KafkaSubscriberString subscriber = new KafkaSubscriberString();
        subscriber.setProps(createConsumerConfig(kafkaServers, groupId));
        return subscriber;
    }

    private static String getKafkaServers() {
        return getApolloConfig(SYS_PROP_KAFKA_SERVERS);
    }

    public static String getApolloConfig(String key) {
        String v = System.getProperty(key);
        if (StringUtils.isBlank(v)) {
            Map<String, String> env = System.getenv();
            v = (String) env.get(key);
            Config config;
            if (StringUtils.isBlank(v)) {
                config = ConfigService.getAppConfig();
                v = config.getProperty(key, "");
            }

            if (StringUtils.isBlank(v)) {
                config = ConfigService.getConfig(getApolloPublicNamespace());
                v = config.getProperty(key, "");
            }
        }

        if (StringUtils.isEmpty(v)) {
            log.error("the key [{}] value is empty,please check", key);
        }

        return v;
    }

    public static String getApolloPublicNamespace() {
        String v = System.getProperty("apollo.ns_public");
        if (StringUtils.isBlank(v)) {
            Map<String, String> env = System.getenv();
            v = (String) env.get("apollo.ns_public");
            if (StringUtils.isBlank(v)) {
                return "DEV.publicUrl";
            }
        }

        return v;
    }

    public static String getApolloConfig(String key, String defalutValue) {
        String value = getApolloConfig(key);
        return StringUtils.isEmpty(value) ? defalutValue : value;
    }

    public static String getRpcStatLogTopic() {
        if (StringUtils.isEmpty(rpcStatLogTopic)) {
            rpcStatLogTopic = getApolloConfig("rpc.statLog.topic", "rpc.statLog.default");
        }

        return rpcStatLogTopic;
    }



}

