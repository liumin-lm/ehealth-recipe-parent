package recipe.constant;

/**
* @Description: HdHttpUrlEnum 华东药企方法请求的枚举
* @Author: JRK
* @Date: 2019/7/24
*/
public enum HdHttpUrlEnum {

    PUSH_RECIPE_INFO("pushRecipeInfo", "rxDoc/prescriptionReceive", "处方推送请求"),

    SCAN_STOCK("scanStock", "drugstore/getAvailableDrugstoreList", "查询药品库存请求"),

    FIND_SUPPORT_DEP("findSupportDep", "drugstore/getAvailableDrugstoreList", "查询可用药店请求"),

    SEND_SCAN_STOCK("sendScanStock", "inventory/getAvailableSumSww", "查询药品库存请求");
    private String methodName;

    private String url;

    private String msg;

    HdHttpUrlEnum(String methodName, String url, String msg) {
        this.methodName = methodName;
        this.url = url;
        this.msg = msg;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * @method  fromMethodName
     * @description 根据methodName获得枚举类
     * @date: 2019/7/24
     * @author: JRK
     * @param methodName 枚举的methodName
     * @return recipe.constant.HdHttpUrlEnum
     */
    public static HdHttpUrlEnum fromMethodName(String methodName) {
        for (HdHttpUrlEnum e : HdHttpUrlEnum.values()) {
            if (methodName.equalsIgnoreCase(e.getMethodName())) {
                return e;
            }
        }
        return null;
    }
}