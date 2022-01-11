package recipe.core.api;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.WriteDrugRecipeTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.WriteDrugRecipeDTO;
import recipe.vo.second.RevisitRecipeTraceVo;

import java.util.List;

/**
 * 处方复诊处理接口
 *
 * @author liumin
 * @date 2021\7\16 0016 17:16
 */
public interface IRevisitBusinessService {

    /**
     * 复诊处方追溯详情
     *
     * @param recipeId 处方ID
     * @param clinicId 复诊ID
     * @return
     */
    List<RevisitRecipeTraceVo> revisitRecipeTrace(Integer recipeId, Integer clinicId);

    /**
     * 复诊处方追溯列表数据处理
     *
     * @param startTime
     * @param endTime
     * @param recipeIds
     * @param organId
     */
    void handDealRevisitTraceRecipe(String startTime, String endTime, List<Integer> recipeIds, Integer organId);

    /**
     * 校验有效复诊单
     * <p>
     * isUnderwayRevisit配置：
     * 1、不判断任何状态；
     * 2、只有存在进行中复诊（复诊单号）才可以开方；
     * 3、只有存在进行中复诊（挂号序号）才可以开方；
     *
     * @param recipe 处方信息
     * @return 能否开方 true： 能
     */
    Boolean revisitValidate(Recipe recipe);

    /**
     * 获取院内门诊
     *
     * @param mpiId 患者唯一标识
     * @param organId  机构ID
     * @param doctorId  医生ID
     * @return 院内门诊
     */
    List<WriteDrugRecipeDTO> findWriteDrugRecipeByRevisitFromHis(String mpiId, Integer organId, Integer doctorId) throws Exception;
}
