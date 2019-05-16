package recipe.service.common;

import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;

/**
 * @author： 0184/yu_yun
 * @date： 2019/5/16
 * @description： 处方配置信息获取
 * @version： 1.0
 */
@RpcBean("recipeConfigService")
public class RecipeConfigService {

    /**
     *
     * @param appId
     * @return
     */
    @RpcService
    public String getRecipeMode(String appId){

        return "";
    }
}
