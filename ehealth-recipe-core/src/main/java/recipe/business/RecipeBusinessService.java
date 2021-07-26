package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.follow.utils.ObjectCopyUtil;
import com.ngari.his.recipe.mode.OutPatientRecipeReq;
import com.ngari.his.recipe.mode.OutRecipeDetailReq;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.dto.DiseaseInfoDTO;
import com.ngari.recipe.dto.OutPatientRecipeDTO;
import com.ngari.recipe.dto.OutRecipeDetailDTO;
import com.ngari.recipe.recipe.model.OutPatientRecipeVO;
import com.ngari.recipe.vo.*;
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

    @Autowired
    private PatientService patientService;

    /**
     * 获取线下门诊处方诊断信息
     * @param patientInfoVO 患者信息
     * @return  诊断列表
     */
    @Override
    public List<DiseaseInfoDTO> getOutRecipeDisease(PatientInfoVO patientInfoVO) {
        return outPatientRecipeManager.getOutRecipeDisease(patientInfoVO.getOrganId(), patientInfoVO.getPatientName(), patientInfoVO.getRegisterID(), patientInfoVO.getPatientId());
    }

    /**
     * 查询门诊处方信息
     * @param outPatientRecipeReqVO 患者信息
     * @return  门诊处方列表
     */
    @Override
    public List<OutPatientRecipeVO> queryOutPatientRecipe(OutPatientRecipeReqVO outPatientRecipeReqVO) {
        logger.info("OutPatientRecipeService queryOutPatientRecipe outPatientRecipeReq:{}.", JSON.toJSONString(outPatientRecipeReqVO));
        OutPatientRecipeReq outPatientRecipeReq = ObjectCopyUtil.convert(outPatientRecipeReqVO, OutPatientRecipeReq.class);
        List<OutPatientRecipeDTO> outPatientRecipeDTOS = outPatientRecipeManager.queryOutPatientRecipe(outPatientRecipeReq);
        return ObjectCopyUtil.convert(outPatientRecipeDTOS, OutPatientRecipeVO.class);
    }

    /**
     * 获取门诊处方详情信息
     * @param outRecipeDetailReqVO 门诊处方信息
     * @return 图片或者PDF链接等
     */
    @Override
    public OutRecipeDetailVO queryOutRecipeDetail(OutRecipeDetailReqVO outRecipeDetailReqVO) {
        logger.info("OutPatientRecipeService queryOutPatientRecipe queryOutRecipeDetail:{}.", JSON.toJSONString(outRecipeDetailReqVO));
        OutRecipeDetailReq outRecipeDetailReq = ObjectCopyUtil.convert(outRecipeDetailReqVO, OutRecipeDetailReq.class);
        OutRecipeDetailDTO outRecipeDetailDTO = outPatientRecipeManager.queryOutRecipeDetail(outRecipeDetailReq);
        return ObjectCopyUtil.convert(outRecipeDetailDTO, OutRecipeDetailVO.class);
    }

    /**
     * 校验当前就诊人是否有效
     * @param outPatientReqVO 当前就诊人信息
     * @return 是否有效
     */
    @Override
    public boolean checkCurrentPatient(OutPatientReqVO outPatientReqVO){
        logger.info("OutPatientRecipeService checkCurrentPatient outPatientReqVO:{}.", JSON.toJSONString(outPatientReqVO));
        return true;
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
