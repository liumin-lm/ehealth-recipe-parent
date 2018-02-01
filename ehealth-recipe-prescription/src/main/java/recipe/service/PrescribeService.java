package recipe.service;

import com.ngari.recipe.common.RecipeBussResTO;
import com.ngari.recipe.common.RecipeCommonResTO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;

/**
 * @author： 0184/yu_yun
 * @date： 2018/1/31
 * @description： 开方服务
 * @version： 1.0
 */
@RpcBean("prescribeService")
public class PrescribeService {

    /**
     * 创建处方
     * @param recipeInfo
     * @return
     */
    @RpcService
    public RecipeBussResTO createPrescription(String recipeInfo){


        return RecipeBussResTO.getSuccessResponse(null);
    }



}
