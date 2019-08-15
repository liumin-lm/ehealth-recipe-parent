package recipe.constant;

/**
* @Description: HdFindSupportDepStatusEnum 枚举华东查找药店列表的响应状态枚举
* @Author: JRK
* @Date: 2019/7/24
*/
public enum HdFindSupportDepStatusEnum {

    SUCCESS("200", "处理成功"),

    NO_USABLE_DRUG_FAIL("1001", "处方内有药品在平台内不存在"),

    NO_USABLE_DEP_FAIL("1002", "距离范围内没有可用药房列表"),

    NO_USABLE_STOCKDEP_FAIL("1003", "距离范围内没有库存足的药房列表"),

    EMPTY_PARAMETER("1004", "参数存在空值"),

    TOKEN_EXPIRE("401", "token过期"),

    DEFAULT("default", "系统错误");

    private String code;

    private String mean;

    HdFindSupportDepStatusEnum(String code, String mean) {
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
     * @return recipe.constant.HdFindSupportDepStatusEnum
     */
    public static HdFindSupportDepStatusEnum fromCode(String code) {
        for (HdFindSupportDepStatusEnum e : HdFindSupportDepStatusEnum.values()) {
            if (code.equalsIgnoreCase(e.getCode())) {
                return e;
            }
        }
        return HdFindSupportDepStatusEnum.DEFAULT;
    }
}