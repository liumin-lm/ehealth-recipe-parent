package recipe.mq;

/**
 * Created by Administrator on 2016/6/13 0013.
 */
public class OnsConfig {
    public static boolean onsSwitch;

    public static String basicInfoTopic;

    public static String hisCdrinfo;

    public static String dbModifyTopic;
    /**
     * 电子病历删除Topic
     */
    public static String emrRecipe;


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
}
