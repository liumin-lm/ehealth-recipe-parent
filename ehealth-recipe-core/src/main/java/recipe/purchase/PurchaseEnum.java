package recipe.purchase;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/18
 * @description： 购药方式枚举，通过该类找到对应的实现类
 * @version： 1.0
 */
public enum PurchaseEnum {

    GIVEMODE_SEND_TO_HOME(1, "payModeOnlineService"),
    GIVEMODE_TO_HOS(2, "payModeToHosService"),
    GIVEMODE_TFDS(3, "payModeTFDSService"),
//    PAYMODE_TFDS(4, "payModeTFDSService"),
    PAYMODE_DOWNLOAD_RECIPE(6, "payModeDownloadService");


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
