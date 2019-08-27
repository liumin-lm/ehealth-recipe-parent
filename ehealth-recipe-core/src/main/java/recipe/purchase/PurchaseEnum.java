package recipe.purchase;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/18
 * @description： 购药方式枚举，通过该类找到对应的实现类
 * @version： 1.0
 */
public enum PurchaseEnum {

    PAYMODE_ONLINE(1, "payModeOnlineService"),
    PAYMODE_COD(2, ""),
    PAYMODE_TO_HOS(3, "payModeToHosService"),
    PAYMODE_TFDS(4, "payModeTFDSService"),
    PAYMODE_MEDICAL_INSURANCE(5, "payModeOnlineService"),
    PAYMODE_DOWNLOAD(6, "payModeDownloadService");


    private Integer payMode;

    private String serviceName;

    PurchaseEnum(Integer payMode, String serviceName) {
        this.payMode = payMode;
        this.serviceName = serviceName;
    }

    public Integer getPayMode() {
        return payMode;
    }

    public String getServiceName() {
        return serviceName;
    }


}
