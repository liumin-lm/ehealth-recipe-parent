package recipe.atop;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.ConfigStatusCheck;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.service.ConfigStatusService;

import java.util.List;

/**
 * 获取配置状态服务入口类
 *
 * @author fuzi
 */
@RpcBean("ConfigStatusAtop")
public class ConfigStatusAtop extends BaseAtop {
    @Autowired
    private ConfigStatusService configStatusService;

    @RpcService
    public List<ConfigStatusCheck> getConfigStatus(int location) {
        logger.info("ConfigStatusService getConfigStatus location = {}", location);
        try {
            List<ConfigStatusCheck> configStatusCheckList = configStatusService.getConfigStatus(location);
            logger.info("ConfigStatusService getConfigStatus configStatusCheckList = {}", JSON.toJSONString(configStatusCheckList));
            return configStatusCheckList;
        } catch (Exception e) {
            logger.error("ConfigStatusService getConfigStatus error", e);
        }
        return null;
    }
}
