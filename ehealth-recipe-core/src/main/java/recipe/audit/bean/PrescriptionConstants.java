//package recipe.audit.bean;
//
///**
// * 合理用药系统对接参数
// * @author yuyun
// */
//public class PrescriptionConstants {
//
//    private static String WEINING_PA_SERVICE_URL;
//    private static String WEINING_PA_HOS_CODE;
//    public static String NAMESPACE = "WinningPAWebservice";
//
//    /**
//     * 处方预警等业务url
//     * @return
//     */
//    public static String getWeiningPaAddress() {
//        return WEINING_PA_SERVICE_URL + ":820/PAWebService.asmx";
//    }
//
//    /**
//     * 药品详情url
//     * @return
//     */
//    public static String getWeiningPaDetailAddress() {
//        return WEINING_PA_SERVICE_URL + ":880/";
//    }
//
//    public void setWnPaServiceUrl(String url){
//        WEINING_PA_SERVICE_URL = url;
//    }
//
//    public static String getWeiningPaHosCode() {
//        return WEINING_PA_HOS_CODE;
//    }
//
//    public void setWnPaHosCode(String weiningPaHosCode) {
//        WEINING_PA_HOS_CODE = weiningPaHosCode;
//    }
//}