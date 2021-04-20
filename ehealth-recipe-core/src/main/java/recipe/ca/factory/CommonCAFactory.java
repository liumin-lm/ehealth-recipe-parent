//package recipe.ca.factory;
//
//import com.ngari.base.BaseAPI;
//import com.ngari.base.property.service.IConfigurationCenterUtilsService;
//import ctd.util.annotation.RpcBean;
//import ctd.util.annotation.RpcService;
//import eh.utils.params.ParamUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import recipe.ca.CAInterface;
//import recipe.ca.impl.ShanghaiCAImpl;
//import recipe.ca.impl.ShanxiCAImpl;
//import recipe.ca.impl.ShenzhenImp;
//import recipe.ca.impl.TianjinCAImpl;
//
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * 根据不同的机构获取机构对应的实现
// * CA工厂类
// */
//@RpcBean
//public class CommonCAFactory {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(CommonCAFactory.class);
//
//    /**
//     * 运营平台机构CA配置
//     */
//    private static final String THIRD_CA_SIGN = "thirdCASign";
//
//    /**
//     * 陕西CA
//     */
//    private static final String CA_TYPE_SHANXI= "shanxiCA";
//    /**
//     * 上海CA
//     */
//    private static final String CA_TYPE_SHANGHAI= "shanghaiCA";
//    /**
//     * 天津CA
//     */
//    public static final String CA_TYPE_TIANJIN= "tianjinCA";
//
//    public static final String CA_TYPE_SHENZHEN= "shenzhenCA";
//
//    public static final Map<String, CAInterface> map = new HashMap<>();
//
//    static {
//        map.put(CA_TYPE_SHANXI, new ShanxiCAImpl());
//        map.put(CA_TYPE_SHANGHAI, new ShanghaiCAImpl());
//        map.put(CA_TYPE_TIANJIN, new TianjinCAImpl());
//        map.put(CA_TYPE_SHENZHEN, new ShenzhenImp());
//    }
//
//    @RpcService
//    public CAInterface useCAFunction(Integer organId) {
//        try {
//            IConfigurationCenterUtilsService configurationService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
//            String thirdCASign = (String) configurationService.getConfiguration(organId, THIRD_CA_SIGN);
//            //上海儿童特殊处理
//            String value = ParamUtils.getParam("SH_CA_ORGANID_WHITE_LIST");
//            List<String> caList = Arrays.asList(value.split(","));
//            if (caList.contains(organId+"")) {
//                thirdCASign = "shanghaiCA";
//            }
//            LOGGER.info("useCAFunction in organId ={} ,CA 模式 ={}", organId, thirdCASign);
//            return map.get(thirdCASign);
//        } catch (Exception e) {
//            e.printStackTrace();
//            LOGGER.error("useCAFunction in organId ={} ,获取CA机构配置异常",organId,e);
//
//        }
//        return null;
//    }
//
////   public static CAInterface useCAFunction(Integer organId) {
////        try {
////            IConfigurationCenterUtilsService configurationService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
////            String thirdCASign = (String) configurationService.getConfiguration(organId, THIRD_CA_SIGN);
////            //上海儿童特殊处理
////            String value = ParamUtils.getParam("SH_CA_ORGANID_WHITE_LIST");
////            LOGGER.info("useCAFunction value ={}=", value);
////            if (value.indexOf(organId+"") >= 0) {
////                thirdCASign = "shanghaiCA";
////            }
////            LOGGER.info("useCAFunction in organId ={} ,CA 模式 ={}", organId, thirdCASign);
////            return map.get(thirdCASign);
////        } catch (Exception e) {
////            e.printStackTrace();
////            LOGGER.error("useCAFunction in organId ={} ,获取CA机构配置异常{}",organId,e);
////
////        }
////        return null;
////    }
//
//
//}
