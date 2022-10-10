package recipe.atop.greenroom;

import com.ngari.patient.dto.OrganDTO;
import com.ngari.recipe.entity.SaleDrugList;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IDrugBusinessService;
import recipe.vo.greenroom.OrganConfigVO;

import java.util.List;

/**
 * @description： 运营平台机构药品
 * @author： 刘敏
 * @date： 2022-05-23 9:45
 */
@RpcBean(value = "organDrugGmAtop")
public class OrganDrugGmAtop extends BaseAtop {

    @Autowired
    private IDrugBusinessService iDrugBusinessService;

    /**
     * 查询药品同步机构配置
     * @param organId
     * @return
     */
    @RpcService
    public OrganConfigVO getConfigByOrganId(Integer organId) {
        validateAtop(organId);
        return iDrugBusinessService.getConfigByOrganId(organId);
    }

    /**
     * 更新药品同步机构配置
     * @param organConfigVO
     * @return
     */
    @RpcService
    public OrganConfigVO updateOrganConfig(OrganConfigVO organConfigVO) {
        validateAtop(organConfigVO.getOrganId());
        return iDrugBusinessService.updateOrganConfig(organConfigVO);
    }


}
