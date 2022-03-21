package recipe.mq.kafka;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import ctd.net.broadcast.Observer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.*;

public class KafkaSubscriberString {
    private static final Logger log = LoggerFactory.getLogger(KafkaSubscriberString.class);
    private Observer observer;
    private KafkaConsumer<String, byte[]> consumer;
    private Properties props;
    private volatile boolean running;
    private Collection<String> topic;
    private ExecutorService exec;
    private ExecutorService taskExec;
    private int threads = 1;
    private final Runnable consumerProcess = new Runnable() {
        @Override
        public void run() {
            KafkaSubscriberString.log.info("consumerProcess with topic{} started.", KafkaSubscriberString.this.topic.toString());
            try {
                while (KafkaSubscriberString.this.running && !Thread.currentThread().isInterrupted()) {
                    try {
                        ConsumerRecords<String, byte[]> records = KafkaSubscriberString.this.consumer.poll(Duration.ofSeconds(1L));
                        if (records.count() != 0) {
                            final CountDownLatch countDownLatch = new CountDownLatch(records.count());
                            Iterator var3 = records.iterator();

                            while (var3.hasNext()) {
                                ConsumerRecord<String, String> record = (ConsumerRecord) var3.next();
                                String recordTopic = record.topic();
                                if (KafkaSubscriberString.this.observer == null) {
                                    KafkaSubscriberString.log.warn("Topic {} has not register observer", recordTopic);
                                }

                                try {
                                    String value = record.value();
                                    KafkaSubscriberString.this.taskExec.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                KafkaSubscriberString.this.observer.onMessage(value);
                                            } catch (Exception var5) {
                                                KafkaSubscriberString.log.error(var5.getMessage(), var5);
                                            } finally {
                                                countDownLatch.countDown();
                                            }

                                        }
                                    });
                                } catch (Exception var15) {
                                    KafkaSubscriberString.log.error("topic[{}] message[{}] codec failed, reconsume later", new Object[]{recordTopic, record.key(), var15});
                                }
                            }

                            countDownLatch.await(20L, TimeUnit.SECONDS);
                        }
                    } catch (Exception var16) {
                        KafkaSubscriberString.log.error(var16.getMessage(), var16);

                        try {
                            Thread.sleep(1000L);
                        } catch (Throwable var14) {
                            ;
                        }
                    }
                }
            } catch (WakeupException var17) {
                KafkaSubscriberString.log.warn("WakeupException ,Closed consumer");
            } catch (Exception var18) {
                KafkaSubscriberString.log.warn("Closed consumer", var18);
            } finally {
                KafkaSubscriberString.this.consumer.close();
            }

        }
    };

    public KafkaSubscriberString() {

    }

    public KafkaSubscriberString(Integer threads) {
        this.threads = threads.intValue();
    }

    public KafkaConsumer<String, byte[]> getConsumer() {
        return this.consumer;
    }

    public void setConsumer(KafkaConsumer<String, byte[]> consumer) {
        this.consumer = consumer;
    }

    public void setProps(Properties props) {
        this.props = props;
        this.consumer = new KafkaConsumer(props);
    }

    public <T> void attach(Collection<String> topic, Observer<T> observer) {
        this.observer = observer;
        this.topic = topic;
        this.consumer.subscribe(topic);
        this.start();
    }

    public void shutdown() {
        this.running = false;
        if (this.consumer != null) {
            this.consumer.wakeup();
        }

        if (this.exec != null && !this.exec.isShutdown()) {
            this.exec.shutdown();

            try {
                if (!this.exec.awaitTermination(5L, TimeUnit.SECONDS)) {
                    this.exec.shutdownNow();
                }
            } catch (InterruptedException var3) {
                Thread.currentThread().interrupt();
            }

            if (this.taskExec != null && !this.taskExec.isShutdown()) {
                this.taskExec.shutdown();

                try {
                    if (!this.taskExec.awaitTermination(5L, TimeUnit.SECONDS)) {
                        this.taskExec.shutdownNow();
                    }
                } catch (InterruptedException var2) {
                    Thread.currentThread().interrupt();
                }

            }
        }
    }

    private void start() {
        this.running = true;
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue();
        this.exec = new ThreadPoolExecutor(1, 1, 180L, TimeUnit.SECONDS, queue, (new ThreadFactoryBuilder()).setNameFormat("drugList-logger-consumer-%d").build());
        this.taskExec = new ThreadPoolExecutor(this.threads, this.threads, 60L, TimeUnit.SECONDS, queue, (new ThreadFactoryBuilder()).setNameFormat("drugList-consumer-%d").build());
        this.exec.execute(this.consumerProcess);
    }

}

