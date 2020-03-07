package recipe.ca.factory;

import com.ngari.base.BaseAPI;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ca.CAInterface;
import recipe.ca.impl.ShanghaiCAImpl;
import recipe.ca.impl.ShanxiCAImpl;

/**
 * 根据不同的机构获取机构对应的实现
 * CA工厂类
 */
public class CommonCAFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonCAFactory.class);

    public CAInterface useCAFunction(Integer organId) {
        LOGGER.info("useCAFunction start in organId={}", organId);
        IConfigurationCenterUtilsService configurationService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        String thirdCASign = (String) configurationService.getConfiguration(organId, "thirdCASign");
        LOGGER.info("useCAFunction thirdCASign={}", thirdCASign);
        if ("shanxiCA".equals(thirdCASign)) {
            return new ShanxiCAImpl();
        } else if ("shanghaiCA".equals(thirdCASign)) {
            return new ShanghaiCAImpl();
        } else {
            LOGGER.info("没有找到对应的CA配置，请检查运营平台的配置是否正确thirdCASign=", thirdCASign);
            return null;
        }
    }

}
