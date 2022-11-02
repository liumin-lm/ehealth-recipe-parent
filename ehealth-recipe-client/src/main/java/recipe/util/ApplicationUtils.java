package recipe.util;

import com.ngari.base.BaseAPI;
import com.ngari.consult.ConsultAPI;
import com.ngari.patient.service.BasicAPI;
import ctd.util.AppContextHolder;
import org.apache.commons.lang3.StringUtils;

/**
 * 使用该类获取服务注意(无法获取没有开放成rpc服务的bean，如ScanDrugService)：
 * eh为基础服务域名，默认按照 eh.remoteDoctor 这种服务命名，获取的时候调用为 IDoctorService iDoctorService = ApplicationUtils.getBaseService(IDoctorService.class);
 * <p>
 * cdr为处方服务域名，默认为服务类名首字母小写的形式
 * <p>
 * his为前置机服务域名，默认为服务类名首字母小写的形式
 * <p>
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date: 2017/8/4.
 */
public class ApplicationUtils {

    private static String BASE_DOMAIN = "eh";

    private static String RECIPE_DOMAIN = "eh";

    public static <T> T getBaseService(Class<T> clazz) {
        return BaseAPI.getService(clazz);
    }

    public static <T> T getBasicService(Class<T> clazz) {
        return BasicAPI.getService(clazz);
    }

    public static <T> T getRecipeService(Class<T> clazz) {
        String className = clazz.getSimpleName();
        String serviceName = className.substring(0, 1).toLowerCase() + className.substring(1);
        return AppContextHolder.getBean(RECIPE_DOMAIN + "." + serviceName, clazz);
    }

    public static <T> T getRecipeService(Class<T> clazz, String serviceName) {
        return getService(clazz, RECIPE_DOMAIN, serviceName);
    }

    public static <T> T getConsultService(Class<T> clazz) {
        return ConsultAPI.getService(clazz);
    }

    public static <T> T getService(Class<T> clazz, String serviceName) {
        return getService(clazz, BASE_DOMAIN, serviceName);
    }

    private static <T> T getService(Class<T> clazz, String domain, String serviceName) {
        if (StringUtils.isEmpty(serviceName)) {
            String className = clazz.getSimpleName();
            if (BASE_DOMAIN.equals(domain)) {
                serviceName = "remote" + className.substring(1);
            } else {
                String firstChar = className.substring(0, 1).toLowerCase();
                serviceName = firstChar + className.substring(1);
            }
        }

        return AppContextHolder.getBean(domain + "." + serviceName, clazz);
    }

}
