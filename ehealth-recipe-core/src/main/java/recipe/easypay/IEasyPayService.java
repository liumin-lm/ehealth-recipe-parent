package recipe.easypay;

import ctd.util.annotation.RpcService;
import easypay.entity.po.AccountResult;
import wnpay.api.model.WnAccountSplitParam;

import java.util.List;

/**
 * @author Created by liuxiaofeng on 2020/11/9.
 */
public interface IEasyPayService {

    /**
     * 卫宁分账接口
     *
     * @param wnAccountDetail
     * @return
     */
    @RpcService
    String wnAccountSplitUpload(WnAccountSplitParam wnAccountDetail);

    @RpcService
    List<AccountResult> queryPaymentDetailByApplyNo(String applyNo);
}
