package recipe.easypay;

import ctd.util.annotation.RpcService;
import wnpay.api.model.WnAccountSplitParam;

/**
 * @author Created by liuxiaofeng on 2020/11/9.
 */
public interface IEasyPayService {

    /**
     * 卫宁分账接口
     * @param wnAccountDetail
     * @return
     */
    @RpcService
    String wnAccountSplitUpload(WnAccountSplitParam wnAccountDetail);
}
