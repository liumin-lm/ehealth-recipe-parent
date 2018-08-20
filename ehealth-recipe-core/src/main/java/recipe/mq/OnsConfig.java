package recipe.mq;

import static com.ngari.patient.constant.OnsConfig.patientTopic;

/**
 * Created by Administrator on 2016/6/13 0013.
 */
public class OnsConfig {
    public static boolean onsSwitch;

    public static String basicInfoTopic;

    public static boolean isOnsSwitch() {
        return onsSwitch;
    }

    public static void setOnsSwitch(boolean onsSwitch) {
        OnsConfig.onsSwitch = onsSwitch;
    }

    public static String getBasicInfoTopic() {
        return basicInfoTopic;
    }

    public static void setBasicInfoTopic(String basicInfoTopic) {
        OnsConfig.basicInfoTopic = basicInfoTopic;
    }
}
