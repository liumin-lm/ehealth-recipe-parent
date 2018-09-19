package recipe.service.common;

import com.ngari.recipe.common.RecipeStandardReqTO;
import com.ngari.recipe.common.RecipeStandardResTO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;

import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/18
 * @description： 处方签名服务
 * @version： 1.0
 */
@RpcBean("recipeSignService")
public class RecipeSignService {

    @RpcService
    public RecipeStandardResTO<Map> sign(RecipeStandardReqTO request){

        return null;
    }

}
