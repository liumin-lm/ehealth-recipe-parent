package recipe.presettle;

import ctd.util.AppContextHolder;
import org.apache.commons.lang3.StringUtils;
import recipe.presettle.settle.IRecipeSettleService;

/**
 * created by shiyuping on 2020/12/1
 * @author shiyuping
 */
public enum RecipeOrderTypeEnum {

    /**
     * 平台常规流程
     */
    COMMON_SELF(0,"普通自费","","cashSettleService"),
    /**
     * 主要省直长三角医保会用到
     */
    PROVINCIAL_MEDICAL(1,"省直医保","medicalPreSettleService","medicalSettleService"),
    /**
     * 主要是杭州互联网会用到，预结算在平台预结算，结算由卫宁互联网去结算
     */
    HANGZHOU_MEDICAL(2,"杭州市医保","HZMedicalPreSettleService",""),
    /**
     * 省医保小程序会用到-但是预结算的节点不同0暂时没放到这里来
     */
    PROVINCIAL_MEDICAL_APPLETS(3,"省医保小程序","",""),
    /**
     * 这种订单类型提交处方订单时暂时不会用到，主要是卫宁付回调回来的时候用到，
     * 有可能在平台属于普通自费订单但是通过卫宁付做的预结算卫宁付返回的医保订单
     */
    OTHER_MEDICAL(4,"其他医保","",""),
    /**
     * 目前主要是省中会用到-走自费预结算
     */
    HOSPITAL_SELF(5,"医院自费","cashPreSettleService","cashSettleService");


    /**订单类型*/
    private Integer type;
    /**订单类型名称*/
    private String name;
    /**订单类型对应预结算服务名*/
    private String preSettleServiceName;
    /**订单类型对应结算服务名*/
    private String settleServiceName;

    RecipeOrderTypeEnum(Integer type, String name, String preSettleServiceName, String settleServiceName) {
        this.type = type;
        this.name = name;
        this.preSettleServiceName = preSettleServiceName;
        this.settleServiceName = settleServiceName;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPreSettleServiceName() {
        return preSettleServiceName;
    }

    public void setPreSettleServiceName(String preSettleServiceName) {
        this.preSettleServiceName = preSettleServiceName;
    }

    public String getSettleServiceName() {
        return settleServiceName;
    }

    public void setSettleServiceName(String settleServiceName) {
        this.settleServiceName = settleServiceName;
    }

    public static IRecipePreSettleService getPreSettleService(Integer orderType){
        IRecipePreSettleService recipePreSettleService = null;
        for (RecipeOrderTypeEnum e : RecipeOrderTypeEnum.values()) {
            if (e.type.equals(orderType)) {
                if (StringUtils.isNotEmpty(e.getPreSettleServiceName())) {
                    recipePreSettleService = AppContextHolder.getBean(e.getPreSettleServiceName(), IRecipePreSettleService.class);
                    break;
                }
            }
        }
        return recipePreSettleService;
    }

    public static IRecipeSettleService getSettleService(Integer orderType){
        IRecipeSettleService recipeSettleService = null;
        for (RecipeOrderTypeEnum e : RecipeOrderTypeEnum.values()) {
            if (e.type.equals(orderType)) {
                if (StringUtils.isNotEmpty(e.getSettleServiceName())) {
                    recipeSettleService = AppContextHolder.getBean(e.getSettleServiceName(), IRecipeSettleService.class);
                    break;
                }
            }
        }
        return recipeSettleService;
    }
}
