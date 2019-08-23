package recipe.constant;

/**
* @Description: HdScanStockStatusEnum 华东查找药品库存的响应状态枚举
* @Author: JRK
* @Date: 2019/7/30
*/
public enum HdScanStockStatusEnum {

    SUCCESS("200", "处理成功"),

    NO_USABLE_DRUG_FAIL("1001", "药品编码和日期条件不能同时为空"),

    NO_USABLE_DEP_FAIL("1002", "少输入了开始或者结束日期"),

    NO_USABLE_STOCKDEP_FAIL("1003", "日期跨度大于31天"),

    EMPTY_PARAMETER("1004", "日期格式不正确"),

    TOKEN_EXPIRE("401", "token过期"),

    DEFAULT("default", "系统错误");

    private String code;

    private String mean;

    HdScanStockStatusEnum(String code, String mean) {
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
    public static HdScanStockStatusEnum fromCode(String code) {
        for (HdScanStockStatusEnum e : HdScanStockStatusEnum.values()) {
            if (code.equalsIgnoreCase(e.getCode())) {
                return e;
            }
        }
        return HdScanStockStatusEnum.DEFAULT;
    }
}