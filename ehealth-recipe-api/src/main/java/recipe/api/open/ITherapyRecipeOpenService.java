package recipe.api.open;

import com.ngari.recipe.recipe.model.RecipeTherapyDTO;
import ctd.util.annotation.RpcService;

/**
 * 诊疗处方
 *
 * @author yinsheng
 * @date 2021\8\30 0030 10:10
 */
public interface ITherapyRecipeOpenService {

    /**
     * 复诊关闭作废诊疗处方
     *
     * @param bussSource
     * @param clinicId
     * @return
     */
    @RpcService
    boolean abolishTherapyRecipeForRevisitClose(Integer bussSource, Integer clinicId);

    /**
     * HIS作废处方
     *
     * @param organId
     * @param recipeCode
     * @return
     */
    @RpcService
    boolean abolishTherapyRecipeForHis(Integer organId, String recipeCode);

    /**
     * 诊疗处方缴费通知
     *
     * @param organId
     * @param recipeCode
     * @param recipeTherapyDTO
     * @return
     */
    @RpcService
    boolean therapyPayNotice(Integer organId, String recipeCode, RecipeTherapyDTO recipeTherapyDTO);


}
