package recipe.atop.open;

import ctd.util.annotation.RpcBean;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.api.open.IEnterpriseOpenAtop;
import recipe.atop.BaseAtop;
import recipe.core.api.IDrugsEnterpriseBusinessService;
import recipe.vo.second.CheckAddressVo;
import recipe.vo.second.enterpriseOrder.EnterpriseConfirmOrderVO;
import recipe.vo.second.enterpriseOrder.EnterpriseResultBean;
import recipe.vo.second.enterpriseOrder.EnterpriseSendOrderVO;

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

    @Override
    public EnterpriseResultBean confirmOrder (EnterpriseConfirmOrderVO enterpriseConfirmOrderVO) {
        if (StringUtils.isEmpty(enterpriseConfirmOrderVO.getAppKey())) {
            return EnterpriseResultBean.getFail("appKey为空");
        }
        return enterpriseBusinessService.confirmOrder(enterpriseConfirmOrderVO);
    }

    @Override
    public EnterpriseResultBean sendOrder(EnterpriseSendOrderVO enterpriseSendOrderVO) {
        if (StringUtils.isEmpty(enterpriseSendOrderVO.getOrderCode())) {
            return EnterpriseResultBean.getFail("订单编号为空");
        }
        return enterpriseBusinessService.sendOrder(enterpriseSendOrderVO);
    }

}
