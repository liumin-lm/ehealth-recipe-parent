package recipe.audit.service;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;

/**
 * 合理用药服务入口 服务在用 新方法不再此类新增
 *
 * @author jiangtingfeng
 */
@RpcBean("prescriptionService")
@Deprecated
public class PrescriptionService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PrescriptionService.class);


    /**
     * 获取智能审方开关配置
     *
     * @param organId 机构id
     * @return 0 关闭 1 打开
     */
    @RpcService
    public Integer getIntellectJudicialFlag(Integer organId) {
        IConfigurationCenterUtilsService configurationCenterUtilsService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        Integer intellectJudicialFlag = (Integer) configurationCenterUtilsService.getConfiguration(organId, "intellectJudicialFlag");
        if (intellectJudicialFlag == 2 || intellectJudicialFlag == 3) {
            intellectJudicialFlag = 1;
        }
        LOGGER.info("PrescriptionService getIntellectJudicialFlag  organId = {} , intellectJudicialFlag={}", organId, intellectJudicialFlag);
        return intellectJudicialFlag;
    }


}
