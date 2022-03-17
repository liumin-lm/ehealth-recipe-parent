package recipe.mq.kafka;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.util.Properties;

public class KafkaPublisherString {
    private KafkaProducer<String, byte[]> producer;
    private Properties props;

    public KafkaPublisherString() {
    }

    public void setProps(Properties props) {
        this.props = props;
        this.producer = new KafkaProducer(props);
    }

    public void publish(String topic, String key, Object message) throws IOException {
        key = StringUtils.isEmpty(key) ? null : key;
        this.producer.send(new ProducerRecord(topic, key, message));
    }

    public void publish(String topic, Object message) throws IOException {
        this.producer.send(new ProducerRecord(topic, message));
    }
}
