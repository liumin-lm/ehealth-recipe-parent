package recipe.service.common;

import com.ngari.recipe.common.RecipeStandardReqTO;
import com.ngari.recipe.common.RecipeStandardResTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;

import java.util.List;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/18
 * @description： 处方签名服务
 * @version： 1.0
 */
@RpcBean(value = "recipeSignService", mvc_authentication = false)
public class RecipeSignService {

    @RpcService
    public RecipeStandardResTO<Map> sign(Integer recipeId, RecipeStandardReqTO request){
        //TODO 先校验处方是否有效

        //签名

        //生成订单

        //发送消息

        return null;
    }

}
