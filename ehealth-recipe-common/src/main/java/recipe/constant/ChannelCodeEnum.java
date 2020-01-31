//package recipe.constant;
//
//import com.google.common.collect.Maps;
//
//import java.util.Map;
//
///**
// * created by shiyuping on 2020/1/13
// * 天猫渠道编码枚举
// *
// */
//public enum ChannelCodeEnum {
//    /**
//     * 浙江衢化医院-渠道编码
//     */
//    ZJQHYY("2","100023"),
//    /**
//     * 浙江浙一医院-渠道编码
//     */
//    ZJZYYY("3","ZJZYYY");
//
//    ChannelCodeEnum(String targetPage, String hospitalId) {
//        this.targetPage = targetPage;
//        this.hospitalId = hospitalId;
//    }
//
//    /**
//     * 天猫跳转页面id
//     */
//    private String targetPage;
//    /**
//     * 天猫对应的医院id
//     */
//    private String hospitalId;
//
//    public String getTargetPage() {
//        return targetPage;
//    }
//
//    public void setTargetPage(String targetPage) {
//        this.targetPage = targetPage;
//    }
//
//    public String getHospitalId() {
//        return hospitalId;
//    }
//
//    public void setHospitalId(String hospitalId) {
//        this.hospitalId = hospitalId;
//    }
//
//    public static Map<String,String> getProcessTemplateParams(String channelCode, String outerRxNo, String jkRxNo,String cityCode) {
//        Map<String, String> params = Maps.newHashMap();
//        for (ChannelCodeEnum e : ChannelCodeEnum.values()) {
//            if (e.name().equalsIgnoreCase(channelCode)) {
//                params.put("outerRxNo",outerRxNo);
//                params.put("jkRxNo",jkRxNo);
//                params.put("cityCode",cityCode);
//                params.put("targetPage",e.getTargetPage());
//                params.put("hospitalId",e.hospitalId);
//                params.put("channelCode",channelCode);
//                return params;
//            }
//        }
//        return params;
//    }
//}
