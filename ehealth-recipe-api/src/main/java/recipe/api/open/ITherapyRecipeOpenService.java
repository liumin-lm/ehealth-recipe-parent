package recipe.api.open;

import com.ngari.recipe.recipe.model.RecipeTherapyDTO;
import com.ngari.recipe.recipe.model.RecipeTherapyOpVO;
import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcService;
import recipe.vo.doctor.RecipeTherapyVO;
import com.ngari.recipe.recipe.model.RecipeTherapyOpQueryVO;

import java.util.List;

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

    /**
     * 根据复诊获取 诊疗处方
     *
     * @param clinicId 复诊id
     * @return
     */
    @RpcService(mvcDisabled = true)
    List<RecipeTherapyVO> findTherapyByClinicId(Integer clinicId);


    /**
     * 运营平台展示诊疗处方列表
     *
     * @param recipeTherapyOpQueryVO
     * @return
     *
     */
    @RpcService
    QueryResult<RecipeTherapyOpVO> findTherapyByInfo(RecipeTherapyOpQueryVO recipeTherapyOpQueryVO);

}
