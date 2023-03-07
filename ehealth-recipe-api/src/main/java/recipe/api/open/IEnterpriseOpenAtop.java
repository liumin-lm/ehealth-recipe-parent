package recipe.api.open;

import com.ngari.recipe.drugsenterprise.model.EnterpriseAddressVO;
import ctd.util.annotation.RpcService;
import recipe.vo.second.CheckAddressVo;
import recipe.vo.second.CheckOrderAddressVo;
import recipe.vo.second.enterpriseOrder.EnterpriseConfirmOrderVO;
import recipe.vo.second.enterpriseOrder.EnterpriseDrugVO;
import recipe.vo.second.enterpriseOrder.EnterpriseResultBean;
import recipe.vo.second.enterpriseOrder.EnterpriseSendOrderVO;

import java.util.List;

/**
 * @description： 药企open atop
 * @author： whf
 * @date： 2022-05-24 14:10
 */
public interface IEnterpriseOpenAtop {

    /**
     * 端校验订单配送地址
     * @param checkOrderAddressVo
     * @return
     */
    @RpcService
    Integer checkSendAddressForOrder(CheckOrderAddressVo checkOrderAddressVo);

    /**
     * 端校验订单配送地址
     * @param checkOrderAddressVo
     * @return
     */
    @RpcService
    Integer checkSendAddressForEnterprises(CheckOrderAddressVo checkOrderAddressVo);

    /**
     * 复诊校验订单配送地址
     * @param checkAddressVo
     * @return
     */
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

    /**
     * 药企药品信息同步接口
     * @param enterpriseDrugVOList
     * @return
     */
    @RpcService
    EnterpriseResultBean renewDrugInfo(List<EnterpriseDrugVO> enterpriseDrugVOList);

    /**
     * 第三方药企更新配送地址和配送费用
     * @param enterpriseAddressList
     * @return
     */
    @RpcService
    Boolean setEnterpriseAddressAndPrice(List<EnterpriseAddressVO> enterpriseAddressList);
}
