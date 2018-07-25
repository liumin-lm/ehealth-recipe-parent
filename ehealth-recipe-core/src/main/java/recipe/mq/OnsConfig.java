package recipe.mq;

/**
 * Created by Administrator on 2016/6/13 0013.
 */
public class OnsConfig {
    public static boolean onsSwitch;

    public static String recipeTopic;

    public static boolean isOnsSwitch() {
        return onsSwitch;
    }

    public static void setOnsSwitch(boolean onsSwitch) {
        OnsConfig.onsSwitch = onsSwitch;
    }

    public static String getRecipeTopic() {
        return recipeTopic;
    }

    public static void setRecipeTopic(String recipeTopic) {
        OnsConfig.recipeTopic = recipeTopic;
    }
}
