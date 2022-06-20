package recipe.api.open;

import ctd.util.annotation.RpcService;
import recipe.vo.second.CheckAddressVo;
import recipe.vo.second.enterpriseOrder.EnterpriseConfirmOrderVO;
import recipe.vo.second.enterpriseOrder.EnterpriseResultBean;
import recipe.vo.second.enterpriseOrder.EnterpriseSendOrderVO;

/**
 * @description： 药企open atop
 * @author： whf
 * @date： 2022-05-24 14:10
 */
public interface IEnterpriseOpenAtop {

    @RpcService
    Boolean checkSendAddress(CheckAddressVo checkAddressVo);

    /**
     * 药企确认订单
     * @param enterpriseConfirmOrderVO
     * @return
     */
    @RpcService
    EnterpriseResultBean confirmOrder (EnterpriseConfirmOrderVO enterpriseConfirmOrderVO);

    /**
     * 准备发货
     * @param enterpriseSendOrderVO
     * @return
     */
    @RpcService
    EnterpriseResultBean readySendOrder(EnterpriseSendOrderVO enterpriseSendOrderVO);

    /**
     * 订单发货接口
     * @param enterpriseSendOrderVO
     * @return
     */
    @RpcService
    EnterpriseResultBean sendOrder(EnterpriseSendOrderVO enterpriseSendOrderVO);

    /**
     * 订单完成接口
     * @param enterpriseSendOrderVO
     * @return
     */
    @RpcService
    EnterpriseResultBean finishOrder(EnterpriseSendOrderVO enterpriseSendOrderVO);
}
