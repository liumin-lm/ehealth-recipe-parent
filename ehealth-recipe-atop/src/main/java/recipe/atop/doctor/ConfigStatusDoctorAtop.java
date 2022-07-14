package recipe.atop.doctor;

import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.vo.ConfigStatusCheckVO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IConfigStatusBusinessService;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;

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
        return configStatusService.getConfigStatus(location);
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
        List<ConfigStatusCheckVO> configStatusCheckList = configStatusService.findByLocationAndSource(location, source);
        if (new Integer(1003991).equals(organId)) {
            return configStatusCheckList.stream().filter(configStatusCheckVO -> !(RecipeOrderStatusEnum.ORDER_STATUS_PROCEED_SHIPPING.getType().equals(configStatusCheckVO.getTarget())
                    || RecipeOrderStatusEnum.ORDER_STATUS_DONE.getType().equals(configStatusCheckVO.getTarget()))).collect(Collectors.toList());
        }
        return configStatusCheckList;
    }

    /**
     * 通过机构ID从运营平台获取购药方式的基本配置项
     *
     * @param organId 机构id
     * @return
     */
    @RpcService
    public List<GiveModeButtonDTO> organGiveMode(Integer organId) {
        List<GiveModeButtonDTO> list = organBusinessService.organGiveMode(organId);
        return list.stream().filter(a -> !RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText().equals(a.getShowButtonKey())).collect(Collectors.toList());
    }
}
