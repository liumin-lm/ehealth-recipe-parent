package recipe.common;

/**
 * Created by Administrator on 2016/6/13 0013.
 */
public class OnsConfig {
    public static boolean onsSwitch;

    public static String basicInfoTopic;

    public static String hisCdrinfo;

    public static String dbModifyTopic;

    public static String sessionTopic;
    /**
     * 电子病历删除Topic
     */
    public static String emrRecipe;

    /**
     * 处方失效延迟消息topic
     */
    public static String recipeDelayTopic;

    public static String revisitTraceTopic;

    public static String revisitStatusNotify;

    public static boolean kafkaSwitch;

    public static String kafkaServers;

    public static String kafkaGroup;

    public static String easyPayTopic;

    public static String statusChangeTopic;


    /**
     * 药品topic
     */
    public static String drugListNursingTopic;

    /**
     * 门诊缴费支付信息回调topic
     */
    public static String paymentReportTopic;


    public void setKafkaSwitch(boolean kafkaSwitch) {
        OnsConfig.kafkaSwitch = kafkaSwitch;
    }


    public void setDrugListNursingTopic(String drugListNursingTopic) {
        OnsConfig.drugListNursingTopic = drugListNursingTopic;
    }

    public void setPaymentReportTopic(String paymentReportTopic) {
        OnsConfig.paymentReportTopic = paymentReportTopic;
    }

    public void setKafkaServers(String kafkaServers) {
        OnsConfig.kafkaServers = kafkaServers;
    }

    public void setKafkaGroup(String kafkaGroup) {
        OnsConfig.kafkaGroup = kafkaGroup;
    }


    public void setRevisitStatusNotify(String revisitStatusNotify) {
        OnsConfig.revisitStatusNotify = revisitStatusNotify;
    }

    public void setRevisitTraceTopic(String revisitTraceTopic) {
        OnsConfig.revisitTraceTopic = revisitTraceTopic;
    }

    public void setStatusChangeTopic(String statusChangeTopic) {
        OnsConfig.statusChangeTopic = statusChangeTopic;
    }

    public void setRecipeDelayTopic(String recipeDelayTopic) {
        OnsConfig.recipeDelayTopic = recipeDelayTopic;
    }

    public void setBasicInfoTopic(String basicInfoTopic) {
        OnsConfig.basicInfoTopic = basicInfoTopic;
    }


    public void setOnsSwitch(String onsSwitch) {
        OnsConfig.onsSwitch = Boolean.parseBoolean(onsSwitch);
    }

    public void setHisCdrinfo(String hisCdrinfo) {
        OnsConfig.hisCdrinfo = hisCdrinfo;
    }

    public void setDbModifyTopic(String dbModifyTopic) {
        OnsConfig.dbModifyTopic = dbModifyTopic;
    }

    public void setEmrRecipe(String emrRecipe) {
        OnsConfig.emrRecipe = emrRecipe;
    }

    public void setSessionTopic(String sessionTopic) {
        OnsConfig.sessionTopic = sessionTopic;
    }

    public void setEasyPayTopic(String easyPayTopic) {
        OnsConfig.easyPayTopic = easyPayTopic;
    }


}
