package recipe.atop.doctor;

import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.aop.LogRecord;
import recipe.atop.BaseAtop;
import recipe.core.api.ICaBusinessService;

/**
 * CA相关服务
 *
 * @author 刘敏
 */
@RpcBean(value = "caAtop")
public class CaDoctorAtop extends BaseAtop {


    @Autowired
    private ICaBusinessService caBusinessService;

    /**
     * 医生签名失败后，将处方状态设置成签名失败
     *
     * @param recipeId
     */
    @LogRecord
    @RpcService
    public void signRecipeCAInterruptForStandard(Integer recipeId) {
        caBusinessService.signRecipeCAInterruptForStandard(recipeId);
    }

    /**
     * 药师签名失败后，将处方状态设置成签名失败
     *
     * @param recipeId
     */
    @LogRecord
    @RpcService
    public void checkRecipeCAInterruptForStandard(Integer recipeId) {
        caBusinessService.checkRecipeCAInterruptForStandard(recipeId);
    }

}
