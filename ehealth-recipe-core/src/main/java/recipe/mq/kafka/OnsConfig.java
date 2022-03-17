package recipe.mq.kafka;

/**
 * ons配置
 */
public class OnsConfig {
    public static boolean onsSwitch;

    public static String kafkaServers = "172.21.1.142:9092";


    /**
     * 药品topic
     */
    public static String drugListNursingTopic = "eh_recipe_encrypt";


    public void setOnsSwitch(boolean onsSwitch) {
        OnsConfig.onsSwitch = onsSwitch;
    }


    public void setDrugListNursingTopic(String drugListNursingTopic) {
        OnsConfig.drugListNursingTopic = drugListNursingTopic;
    }

    public void setKafkaServers(String kafkaServers) {
        OnsConfig.kafkaServers = kafkaServers;
    }

}
