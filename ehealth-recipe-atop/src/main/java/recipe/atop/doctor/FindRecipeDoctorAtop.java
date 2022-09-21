package recipe.atop.doctor;

import com.ngari.recipe.dto.RecipeRefundDTO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IRecipeBusinessService;
import recipe.vo.greenroom.RecipeRefundInfoReqVO;

import java.util.List;

/**
 * 医生端-查处方服务入口类
 *
 * @author zgy
 * @date 2022/9/20
 */
@RpcBean("findRecipeDoctorAtop")
public class FindRecipeDoctorAtop extends BaseAtop {
    @Autowired
    private IRecipeBusinessService recipeBusinessService;

    /**
     * 医生端-我的数据获取已退费列表
     * @param recipeRefundInfoReqVO
     */
    @RpcService
    public List<RecipeRefundDTO> getRecipeRefundInfo(RecipeRefundInfoReqVO recipeRefundInfoReqVO) {
        validateAtop(recipeRefundInfoReqVO);
        return recipeBusinessService.getRecipeRefundInfo(recipeRefundInfoReqVO);
    }

}
