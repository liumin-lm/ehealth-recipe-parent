package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.follow.utils.ObjectCopyUtil;
import com.ngari.his.recipe.mode.OutPatientRecipeReq;
import com.ngari.recipe.dto.DiseaseInfoDTO;
import com.ngari.recipe.dto.OutPatientRecipeDTO;
import com.ngari.recipe.recipe.model.OutPatientRecipeVO;
import com.ngari.recipe.vo.OutPatientRecipeReqVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.IRecipeBusinessService;
import recipe.dao.RecipeDAO;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.manager.OutPatientRecipeManager;

import java.util.Arrays;
import java.util.List;

/**
 * 处方业务核心逻辑处理类
 *
 * @author yinsheng
 * @date 2021\7\16 0016 17:30
 */
@Service
public class RecipeBusinessService extends BaseService implements IRecipeBusinessService {

    //药师审核不通过状态集合 供getUncheckRecipeByClinicId方法使用
    private final List<Integer> UncheckedStatus = Arrays.asList(RecipeStatusEnum.RECIPE_STATUS_UNCHECK.getType(), RecipeStatusEnum.RECIPE_STATUS_READY_CHECK_YS.getType(),
            RecipeStatusEnum.RECIPE_STATUS_SIGN_ERROR_CODE_PHA.getType(), RecipeStatusEnum.RECIPE_STATUS_SIGN_ING_CODE_DOC.getType(), RecipeStatusEnum.RECIPE_STATUS_SIGN_ING_CODE_PHA.getType(),
            RecipeStatusEnum.RECIPE_STATUS_SIGN_NO_CODE_PHA.getType());

    @Autowired
    private RecipeDAO recipeDAO;

    @Autowired
    private OutPatientRecipeManager outPatientRecipeManager;

    @Override
    public List<DiseaseInfoDTO> getOutRecipeDisease(Integer organId, String patientName, String registerID, String patientId) {
        return outPatientRecipeManager.getOutRecipeDisease(organId, patientName, registerID, patientId);
    }

    @Override
    public List<OutPatientRecipeVO> queryOutPatientRecipe(OutPatientRecipeReqVO outPatientRecipeReqVO) {
        logger.info("OutPatientRecipeService queryOutPatientRecipe outPatientRecipeReq:{}.", JSON.toJSONString(outPatientRecipeReqVO));
        OutPatientRecipeReq outPatientRecipeReq = ObjectCopyUtil.convert(outPatientRecipeReqVO, OutPatientRecipeReq.class);
        List<OutPatientRecipeDTO> outPatientRecipeDTOS = outPatientRecipeManager.queryOutPatientRecipe(outPatientRecipeReq);
        //TODO 业务逻辑处理
        return ObjectCopyUtil.convert(outPatientRecipeDTOS, OutPatientRecipeVO.class);
    }

    /**
     * @Description: 查询未审核处方个数
     * @Param: bussSource 处方来源
     * @Param: clinicId  复诊ID
     * @Param: recipeStatus  未审核状态List
     * @return:
     * @Date: 2021/7/20
     */
    private Long getUncheckRecipeByClinicId(Integer bussSource, Integer clinicId, List<Integer> recipeStatus) {
        logger.info("getUncheckRecipeByClinicID bussSource={},clinicID={},recipeStatus={}", bussSource, clinicId, recipeStatus);
        Long recipesCount = recipeDAO.getRecipeCountByBussSourceAndClinicIdAndStatus(bussSource, clinicId, recipeStatus);
        logger.info("getUncheckRecipeByClinicID recipesCount={}", recipesCount);
        return recipesCount;
    }

    /**
     * @Description: 根据bussSource和clinicID查询是否存在药师审核未通过的处方
     * @Param: bussSource
     * @Param: clinicID
     * @return: true存在  false不存在
     * @Date: 2021/7/16
     */
    @Override
    public Boolean existUncheckRecipe(Integer bussSource, Integer clinicId) {
        //获取处方状态为药师审核不通过的处方个数
        Long uncheckRecipeList = getUncheckRecipeByClinicId(bussSource, clinicId, UncheckedStatus);
        Integer uncheckCount = uncheckRecipeList.intValue();
        return uncheckCount != 0;
    }
}
