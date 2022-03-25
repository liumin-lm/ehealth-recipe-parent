package recipe.mq.kafka;

import com.google.common.collect.Sets;
import ctd.mixin.monitor.MonitorSubscriber;
import ctd.net.broadcast.exception.KafkaException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.common.OnsConfig;

import java.util.Arrays;
import java.util.Properties;


/**
 * kafka消息消费
 * 由于框架中"value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"
 * 而canal是org.apache.kafka.common.serialization.StringDeserializer，所以目前不适用canal
 */
public class KafkaDrugsSubscriber implements MonitorSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(KafkaDrugsSubscriber.class);

    private KafkaSubscriberString subscriber;

    @Override
    public void init() {
        //groupid 不同时，消费同一topic，相当于广播消费
        try {
            if (OnsConfig.kafkaSwitch) {
                logger.info("初始化 consumer----------");
                subscriber = KafkaHelperForCommon.createSubscriber(
                        OnsConfig.kafkaServers, "drugList-consumer");
                //订阅业务topic
                subscriber.attach(Sets.newHashSet(
                        OnsConfig.drugListNursingTopic),
                        new KafkaDrugsSyncObserver());
            }
        } catch (KafkaException e) {
            logger.error(e.getMessage(), e);
        }

    }

    @Override
    public void shutdown() {
        if (subscriber != null) {
            subscriber.shutdown();
        }
        logger.info("KafkaOrderSubscriber shutdown");

    }

    public static void main(String[] args) {
        Properties props = new Properties();

        props.put("bootstrap.servers", "172.21.1.142:9092");

        props.put("group.id", "drugList-consumer");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");

        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        consumer.subscribe(Arrays.asList("eh_recipe_feature"));
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
            }
        }
    }
}
