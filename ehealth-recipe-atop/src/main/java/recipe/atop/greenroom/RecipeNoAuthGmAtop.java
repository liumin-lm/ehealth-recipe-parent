package recipe.atop.greenroom;

import com.google.common.collect.Lists;
import com.ngari.recipe.entity.FastRecipe;
import com.ngari.recipe.entity.FastRecipeDetail;
import com.ngari.recipe.vo.FastRecipeDetailVO;
import com.ngari.recipe.vo.FastRecipeReq;
import com.ngari.recipe.vo.FastRecipeVO;
import ctd.util.BeanUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.utils.BeanCopyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IFastRecipeBusinessService;

import java.util.List;

@RpcBean(value = "recipeNoAuthGmAtop", mvc_authentication = false)
public class RecipeNoAuthGmAtop extends BaseAtop {
    @Autowired
    IFastRecipeBusinessService fastRecipeService;

    /**
     * 快捷购药 运营平台查询药方详情
     *
     * @param fastRecipeReq
     * @return
     */
    @RpcService
    public FastRecipeVO getFastRecipeByFastRecipeId(FastRecipeReq fastRecipeReq) {
        validateAtop(fastRecipeReq, fastRecipeReq.getFastRecipeId());
        fastRecipeReq.setStatusList(Lists.newArrayList(1, 2));
        List<FastRecipe> fastRecipeList = fastRecipeService.findFastRecipeListByParam(fastRecipeReq);
        if (CollectionUtils.isNotEmpty(fastRecipeList)) {
            FastRecipeVO fastRecipeVO = BeanUtils.map(fastRecipeList.get(0), FastRecipeVO.class);
            List<FastRecipeDetail> fastRecipeDetailList = fastRecipeService.findFastRecipeDetailsByFastRecipeId(fastRecipeList.get(0).getId());
            fastRecipeVO.setFastRecipeDetailList(BeanCopyUtils.copyList(fastRecipeDetailList, FastRecipeDetailVO::new));
            if (Integer.valueOf(3).equals(fastRecipeDetailList.get(0).getType())) {
                fastRecipeVO.setSecrecyFlag(1);
            } else {
                fastRecipeVO.setSecrecyFlag(2);
            }
            return fastRecipeVO;
        } else {
            return null;
        }
    }
}

