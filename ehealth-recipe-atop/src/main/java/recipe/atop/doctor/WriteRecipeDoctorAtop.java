package recipe.atop.doctor;

import com.ngari.patient.dto.HealthCardDTO;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.dto.OutPatientRecordResDTO;
import com.ngari.recipe.dto.WriteDrugRecipeDTO;
import com.ngari.recipe.entity.DoctorCommonPharmacy;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.*;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IRecipeBusinessService;
import recipe.core.api.IRevisitBusinessService;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.SignEnum;
import recipe.enumerate.status.WriteHisEnum;
import recipe.util.ValidateUtil;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.doctor.ValidateDetailVO;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 开处方服务入口类
 *
 * @author fuzi
 */
@RpcBean("writeRecipeDoctorAtop")
public class WriteRecipeDoctorAtop extends BaseAtop {

    @Autowired
    private IRevisitBusinessService iRevisitBusinessService;

    @Autowired
    private IRecipeBusinessService recipeBusinessService;

    /**
     * 获取院内门诊
     *
     * @param mpiId    患者唯一标识
     * @param organId  机构ID
     * @param doctorId 医生ID
     * @return 院内门诊处方列表
     */
    @RpcService
    public List<WriteDrugRecipeDTO> findWriteDrugRecipeByRevisitFromHis(String mpiId, Integer organId, Integer doctorId) {
        return iRevisitBusinessService.findWriteDrugRecipeByRevisitFromHis(mpiId, organId, doctorId);
    }

    /**
     * 医生二次确认药师审核结果-不通过
     *
     * @param recipeId 处方id
     * @return
     */
    @RpcService
    public Boolean confirmAgain(Integer recipeId) {
        return recipeBusinessService.confirmAgain(recipeId);
    }

    /**
     * 获取有效门诊记录
     *
     * @param mpiId    患者唯一标识
     * @param organId  机构ID
     * @param doctorId 医生ID
     * @return 门诊记录
     */
    @RpcService
    public OutPatientRecordResDTO findOutPatientRecordFromHis(String mpiId, Integer organId, Integer doctorId) {
        return recipeBusinessService.findOutPatientRecordFromHis(mpiId, organId, doctorId);
    }

    /**
     * 靶向药拆方，无靶向药 返回空
     * todo 新街口 splitDrugRecipeV1
     *
     * @param validateDetailVO 处方信息
     * @return 处方组号
     */
    @Deprecated
    @RpcService
    public String splitDrugRecipe(ValidateDetailVO validateDetailVO) {
        validateAtop(validateDetailVO, validateDetailVO.getRecipeBean(), validateDetailVO.getRecipeDetails());
        RecipeBean recipeBean = validateDetailVO.getRecipeBean();
        if (StringUtils.isEmpty(recipeBean.getGroupCode())) {
            String uuid = UUID.randomUUID().toString();
            recipeBean.setGroupCode(uuid);
        }
        recipeBean.setRecipeExtend(validateDetailVO.getRecipeExtendBean());
        recipeBean.setTargetedDrugType(1);
        return recipeBusinessService.splitDrugRecipe(recipeBean, validateDetailVO.getRecipeDetails());
    }

    /**
     * 靶向药拆方，无靶向药 返回空
     *
     * @param validateDetailVO 处方信息
     * @return 处方id
     */
    @RpcService
    public List<Integer> splitDrugRecipeV1(ValidateDetailVO validateDetailVO) {
        String groupCode = this.splitDrugRecipe(validateDetailVO);
        return this.recipeByGroupCode(groupCode, 1);
    }

    /**
     * 查询同组处方
     *
     * @param groupCode 处方组号
     * @param type      0： 默认全部 1：查询暂存，2查询可撤销处方
     * @return 处方id集合
     */
    @RpcService
    public List<Integer> recipeByGroupCode(String groupCode, Integer type) {
        validateAtop(groupCode);
        return recipeBusinessService.recipeByGroupCode(groupCode, type);
    }

    /**
     * 获取处方状态 与 同组处方id
     *
     * @param recipeId recipe
     * @param type     0： 默认全部 1：查询暂存，2查询可撤销处方
     */
    @RpcService
    public RecipeBean recipeInfo(Integer recipeId, Integer type) {
        validateAtop(recipeId);
        Recipe recipe = recipeBusinessService.getByRecipeId(recipeId);
        RecipeBean recipeBean = ObjectCopyUtils.convert(recipe, RecipeBean.class);
        recipeBean.setSubStateText(RecipeStateEnum.getRecipeStateEnum(recipe.getSubState()).getName());
        if (ValidateUtil.validateObjects(recipeBean.getGroupCode())) {
            return recipeBean;
        }
        List<Integer> recipeIdList = recipeBusinessService.recipeByGroupCode(recipeBean.getGroupCode(), type);
        List<Integer> recipeIds = recipeIdList.stream().filter(a -> !a.equals(recipeBean.getRecipeId())).collect(Collectors.toList());
        recipeBean.setGroupRecipeIdList(recipeIds);
        return recipeBean;
    }

    /**
     * 获取事前提醒
     * @param advanceWarningReqDTO
     */
    @RpcService
    public AdvanceWarningResVO getAdvanceWarning(AdvanceWarningReqVO advanceWarningReqDTO){
        validateAtop(advanceWarningReqDTO.getRecipeId());
        return recipeBusinessService.getAdvanceWarning(advanceWarningReqDTO);
    }

    /**
     * 医生端开方获取患者卡信息
     * @param mpiId
     * @return
     */
    @RpcService
    public List<HealthCardDTO> findByCardOrganAndMpiId(String mpiId){
        return recipeBusinessService.findByCardOrganAndMpiId(mpiId);
    }

    /**
     * 医生端开处方：获取煎法关联用药途径和用药频次
     *
     * @param decoctionId
     * @return
     */
    @RpcService
    public RateAndPathwaysVO queryRateAndPathwaysByDecoctionId(Integer organId, Integer decoctionId, Integer doctorId) {
        validateAtop(organId, decoctionId, doctorId);
        return recipeBusinessService.queryRateAndPathwaysByDecoctionId(organId, decoctionId);
    }

    /**
     * 医生端开处方：获取煎法关联服用要求
     *
     * @param decoctionId
     * @return
     */
    @RpcService
    public List<RequirementsForTakingVO> getRequirementsForTakingByDecoctionId(Integer organId, Integer decoctionId) {
        validateAtop(organId);
        return recipeBusinessService.findRequirementsForTakingByDecoctionId(organId, decoctionId);
    }

    /**
     * 查询医生选择的常用默认药房
     *
     * @param organId
     * @return
     */
    @RpcService
    @Deprecated
    public DoctorCommonPharmacy findDoctorCommonPharmacyByOrganIdAndDoctorId(Integer organId,Integer doctorId ) {
        validateAtop(organId, doctorId);
        return recipeBusinessService.findDoctorCommonPharmacyByOrganIdAndDoctorId(organId,doctorId);
    }

    /**
     * 保存医生选择的常用默认药房
     * @param doctorCommonPharmacy
     */
    @RpcService
    @Deprecated
    public void saveDoctorCommonPharmacy(DoctorCommonPharmacy doctorCommonPharmacy) {
        validateAtop(doctorCommonPharmacy);
      //  recipeBusinessService.saveDoctorCommonPharmacy(doctorCommonPharmacy);
    }

    /**
     * 暂存处方接口
     *
     * @param recipeInfoVO
     */
    @RpcService
    public void stagingRecipe(RecipeInfoVO recipeInfoVO) {
        validateAtop(recipeInfoVO, recipeInfoVO.getRecipeBean());
        validateAtop(recipeInfoVO.getRecipeBean().getDoctor(), recipeInfoVO.getRecipeBean().getClinicOrgan());
        RecipeBean recipeBean = recipeInfoVO.getRecipeBean();
        if (!ValidateUtil.integerIsEmpty(recipeBean.getRecipeId())) {
            Recipe recipe = recipeBusinessService.getRecipe(recipeBean.getRecipeId());
            // 只有暂存状态才可以修改
            if (!WriteHisEnum.NONE.getType().equals(recipe.getWriteHisState()) || !SignEnum.NONE.getType().equals(recipe.getDoctorSignState())) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "当前处方不是暂存状态,不能操作");
            }
        }
        recipeBusinessService.stagingRecipe(recipeInfoVO);
    }
}
