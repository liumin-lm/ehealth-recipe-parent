package recipe.enumerate.type;

/**
 * 煎法选择配置
 * @author yinsheng
 * @date 2021\11\08 0029 19:29
 */
public enum DecoctionDeployTypeEnum {

    DECOCTION_DEPLOY_NO("0", "无"),
    DECOCTION_DEPLOY_DOCTOR("1", "医生选择"),
    DECOCTION_DEPLOY_PATIENT("2", "医生选择");

    private String type;
    private String name;

    DecoctionDeployTypeEnum(String type, String name){
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
