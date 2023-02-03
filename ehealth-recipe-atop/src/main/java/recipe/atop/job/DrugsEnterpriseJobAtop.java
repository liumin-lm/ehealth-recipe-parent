package recipe.atop.job;

import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IEnterpriseBusinessService;

/**
 * @description：药企定时任务相关
 * @author： whf
 * @date： 2022-04-21 20:38
 */
@RpcBean(value = "drugsEnterpriseJobAtop")
public class DrugsEnterpriseJobAtop extends BaseAtop {

    @Autowired
    private IEnterpriseBusinessService enterpriseBusinessService;
    /**
     * 药企推送失败 的处方重新推送定时任务
     */
    @RpcService
    public void rePushRecipeToDrugsEnterprise() {
        enterpriseBusinessService.rePushRecipeToDrugsEnterprise();
    }

    /**
     * 推送失败订单提醒任务
     */
    @RpcService
    public void pushFailOrderNotify() {
        enterpriseBusinessService.pushFailOrderNotify();
    }
}
