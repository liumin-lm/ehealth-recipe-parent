package recipe.atop.open;

import ctd.util.annotation.RpcBean;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.api.open.IEnterpriseOpenAtop;
import recipe.atop.BaseAtop;
import recipe.core.api.IDrugsEnterpriseBusinessService;
import recipe.vo.second.CheckAddressVo;

/**
 * @description： 药企openatop
 * @author： whf
 * @date： 2022-05-24 14:09
 */
@RpcBean("enterpriseOpenAtop")
public class EnterpriseOpenAtop  extends BaseAtop implements IEnterpriseOpenAtop {
    @Autowired
    private IDrugsEnterpriseBusinessService enterpriseBusinessService;
    @Override
    public Boolean checkSendAddress(CheckAddressVo checkAddressVo) {
        validateAtop(checkAddressVo, checkAddressVo.getOrganId());
        return enterpriseBusinessService.checkSendAddress(checkAddressVo);
    }
}
