package recipe.bussutil.drugdisplay;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;

/**
 * created by shiyuping on 2021/3/12
 */
public class DrugNameDisplayUtil {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DrugNameDisplayUtil.class);

    private static IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);

    public static String[] getDrugNameConfigByDrugType(Integer organId, Integer drugType) {
        if (organId != null) {
            String drugNameConfigKey = getDrugNameConfigKey(drugType);
            if (StringUtils.isNotEmpty(drugNameConfigKey)) {
                String[] config = (String[]) configurationService.getConfiguration(organId, drugNameConfigKey);
                LOGGER.info("getDrugNameConfig organId:{},drugType:{},config:{}", organId, drugType, JSONUtils.toString(config));
                return config;
            }
        }
        return null;
    }

    public static String[] getSaleNameConfigByDrugType(Integer organId, Integer drugType) {
        if (organId != null) {
            String saleNameConfigKey = getSaleNameConfigKey(drugType);
            if (StringUtils.isNotEmpty(saleNameConfigKey)) {
                String[] config = (String[]) configurationService.getConfiguration(organId, saleNameConfigKey);
                LOGGER.info("getSaleNameConfig organId:{},drugType:{},config:{}", organId, drugType, JSONUtils.toString(config));
                return config;
            }
        }
        return null;
    }


    public static String getDrugNameConfigKey(Integer drugType) {
        if (drugType != null) {
            switch (drugType) {
                //西药/中成药
                case 1:
                case 2:
                    return DisplayNameEnum.WM_DRUGNAME.getConfigKey();
                //中药
                case 3:
                    return DisplayNameEnum.TCM_DRUGNAME.getConfigKey();
                default:
                    return null;
            }
        }
        return null;
    }

    public static String getSaleNameConfigKey(Integer drugType) {
        if (drugType != null) {
            switch (drugType) {
                //西药/中成药
                case 1:
                case 2:
                    return DisplayNameEnum.WM_SALENAME.getConfigKey();
                default:
                    return null;
            }
        }
        return null;
    }
}
