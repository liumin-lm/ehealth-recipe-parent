package recipe.api.open;

import ctd.util.annotation.RpcService;
import recipe.vo.second.CheckAddressVo;
import recipe.vo.second.enterpriseOrder.EnterpriseConfirmOrderVO;
import recipe.vo.second.enterpriseOrder.EnterpriseResultBean;

/**
 * @description： 药企open atop
 * @author： whf
 * @date： 2022-05-24 14:10
 */
public interface IEnterpriseOpenAtop {

    @RpcService
    Boolean checkSendAddress(CheckAddressVo checkAddressVo);

    @RpcService
    EnterpriseResultBean confirmOrder (EnterpriseConfirmOrderVO enterpriseConfirmOrderVO);
}
