package recipe.constant;

/**
* @Description: HdPushRecipeStatusEnum 枚举华东推送处方的响应状态枚举
* @Author: JRK
* @Date: 2019/7/24
*/
public enum HdPushRecipeStatusEnum {

    SUCCESS("200", "处理成功"),

    NO_USABLE_DRUG_FAIL("1002", "处方内有药品在平台内不存在"),

    DEP_UNAVAILABLE_FAIL("1001", "药店不存在或者已经停用"),

    ACCEPT_FAIL("1003", "接收异常"),

    TOKEN_EXPIRE("401", "token过期"),

    DEFAULT("default", "系统错误");

    private String code;

    private String mean;

    HdPushRecipeStatusEnum(String code, String mean) {
        this.code = code;
        this.mean = mean;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMean() {
        return mean;
    }

    public void setMean(String mean) {
        this.mean = mean;
    }
    /**
     * @method  fromCode
     * @description 根据code获得枚举类
     * @date: 2019/7/24
     * @author: JRK
     * @param code 根据的枚举code
     * @return recipe.constant.HdPushRecipeStatusEnum
     */
    public static HdPushRecipeStatusEnum fromCode(String code) {
        for (HdPushRecipeStatusEnum e : HdPushRecipeStatusEnum.values()) {
            if (code.equalsIgnoreCase(e.getCode())) {
                return e;
            }
        }
        return HdPushRecipeStatusEnum.DEFAULT;
    }
}