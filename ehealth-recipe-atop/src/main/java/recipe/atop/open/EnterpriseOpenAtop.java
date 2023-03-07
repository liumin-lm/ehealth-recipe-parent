package recipe.atop.open;

import com.ngari.recipe.drugsenterprise.model.EnterpriseAddressVO;
import ctd.util.annotation.RpcBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.api.open.IEnterpriseOpenAtop;
import recipe.atop.BaseAtop;
import recipe.core.api.IEnterpriseBusinessService;
import recipe.util.DateConversion;
import recipe.vo.patient.AddrAreaVO;
import recipe.vo.patient.AddressAreaVo;
import recipe.vo.second.CheckAddressVo;
import recipe.vo.second.CheckOrderAddressVo;
import recipe.vo.second.enterpriseOrder.EnterpriseConfirmOrderVO;
import recipe.vo.second.enterpriseOrder.EnterpriseDrugVO;
import recipe.vo.second.enterpriseOrder.EnterpriseResultBean;
import recipe.vo.second.enterpriseOrder.EnterpriseSendOrderVO;

import java.util.Date;
import java.util.List;

/**
 * @description： 药企openatop
 * @author： whf
 * @date： 2022-05-24 14:09
 */
@RpcBean("enterpriseOpenAtop")
public class EnterpriseOpenAtop  extends BaseAtop implements IEnterpriseOpenAtop {
    @Autowired
    private IEnterpriseBusinessService enterpriseBusinessService;

    @Override
    public Integer checkSendAddressForOrder(CheckOrderAddressVo checkAddressVo) {
        validateAtop(checkAddressVo, checkAddressVo.getEnterpriseId());
        return enterpriseBusinessService.checkSendAddressForOrder(checkAddressVo);
    }

    @Override
    public Integer checkSendAddressForEnterprises(CheckOrderAddressVo checkOrderAddressVo) {
        validateAtop(checkOrderAddressVo, checkOrderAddressVo.getEnterpriseIds());
        return enterpriseBusinessService.checkSendAddressForEnterprises(checkOrderAddressVo);
    }

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
    public EnterpriseResultBean readySendOrder(EnterpriseSendOrderVO enterpriseSendOrderVO) {
        if (StringUtils.isEmpty(enterpriseSendOrderVO.getOrderCode())) {
            return EnterpriseResultBean.getFail("订单编号为空");
        }
        enterpriseSendOrderVO.setSendDate(DateConversion.getDateFormatter(new Date(), DateConversion.DEFAULT_DATE_TIME));
        return enterpriseBusinessService.readySendOrder(enterpriseSendOrderVO);
    }

    @Override
    public EnterpriseResultBean sendOrder(EnterpriseSendOrderVO enterpriseSendOrderVO) {
        if (StringUtils.isEmpty(enterpriseSendOrderVO.getOrderCode())) {
            return EnterpriseResultBean.getFail("订单编号为空");
        }
        return enterpriseBusinessService.sendOrder(enterpriseSendOrderVO);
    }

    @Override
    public EnterpriseResultBean finishOrder(EnterpriseSendOrderVO enterpriseSendOrderVO) {
        if (StringUtils.isEmpty(enterpriseSendOrderVO.getOrderCode())) {
            return EnterpriseResultBean.getFail("订单编号为空");
        }
        return enterpriseBusinessService.finishOrder(enterpriseSendOrderVO);
    }

    @Override
    public EnterpriseResultBean renewDrugInfo(List<EnterpriseDrugVO> enterpriseDrugVOList){
        if (CollectionUtils.isEmpty(enterpriseDrugVOList)) {
            return EnterpriseResultBean.getFail("入参为空");
        }
        return enterpriseBusinessService.renewDrugInfo(enterpriseDrugVOList);
    }

    @Override
    public Boolean setEnterpriseAddressAndPrice(List<EnterpriseAddressVO> enterpriseAddressList) {
        return enterpriseBusinessService.setEnterpriseAddressAndPrice(enterpriseAddressList);
    }

}
