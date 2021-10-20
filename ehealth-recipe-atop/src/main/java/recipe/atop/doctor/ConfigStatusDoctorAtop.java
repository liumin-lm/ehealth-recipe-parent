package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.vo.ConfigStatusCheckVO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IConfigStatusBusinessService;
import recipe.enumerate.status.RecipeOrderStatusEnum;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 获取配置状态服务入口类
 *
 * @author fuzi
 */
@RpcBean("configStatusAtop")
public class ConfigStatusDoctorAtop extends BaseAtop {
    @Autowired
    private IConfigStatusBusinessService configStatusService;

    /**
     * 根据位置查询状态数据
     *
     * @param location
     * @return
     */
    @RpcService
    public List<ConfigStatusCheckVO> getConfigStatus(Integer location) {
        logger.info("ConfigStatusService getConfigStatus location = {}", location);
        try {
            List<ConfigStatusCheckVO> configStatusCheckList = configStatusService.getConfigStatus(location);
            logger.info("ConfigStatusService getConfigStatus configStatusCheckList = {}", JSON.toJSONString(configStatusCheckList));
            return configStatusCheckList;
        } catch (Exception e) {
            logger.error("ConfigStatusService getConfigStatus error", e);
        }
        return null;
    }

    /**
     * 根据位置与源状态查询数据
     *
     * @param location
     * @param source
     * @return
     */
    @RpcService
    public List<ConfigStatusCheckVO> getConfigStatusBySource(Integer location, Integer source, Integer organId) {
        logger.info("ConfigStatusService getConfigStatus location = {}", location);
        try {
            List<ConfigStatusCheckVO> configStatusCheckList = configStatusService.findByLocationAndSource(location, source);
            if (new Integer(1003991).equals(organId)) {
                return configStatusCheckList.stream().filter(configStatusCheckVO -> !(RecipeOrderStatusEnum.ORDER_STATUS_PROCEED_SHIPPING.getType().equals(configStatusCheckVO.getTarget())
                        || RecipeOrderStatusEnum.ORDER_STATUS_DONE.getType().equals(configStatusCheckVO.getTarget()))).collect(Collectors.toList());
            }
            logger.info("ConfigStatusService getConfigStatus configStatusCheckList = {}", JSON.toJSONString(configStatusCheckList));
            return configStatusCheckList;
        } catch (Exception e) {
            logger.error("ConfigStatusService getConfigStatus error", e);
        }
        return null;
    }
}
