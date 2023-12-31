package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.patient.dto.HealthCardDTO;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.dto.OutPatientRecordResDTO;
import com.ngari.recipe.dto.RecipeDTO;
import com.ngari.recipe.dto.WriteDrugRecipeDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.*;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IRecipeBusinessService;
import recipe.core.api.IRecipeDetailBusinessService;
import recipe.core.api.IRevisitBusinessService;
import recipe.core.api.IStockBusinessService;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.SignEnum;
import recipe.enumerate.status.WriteHisEnum;
import recipe.util.RecipeBusiThreadPool;
import recipe.util.ValidateUtil;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.doctor.ValidateDetailVO;

import java.util.ArrayList;
import java.util.Collections;
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
    private IRecipeDetailBusinessService recipeDetailBusinessService;
    @Autowired
    private IRecipeBusinessService recipeBusinessService;
    @Autowired
    private IStockBusinessService iStockBusinessService;

    /**
     * 暂存处方接口
     *
     * @param recipeInfoVO
     */
    @RpcService
    public Integer stagingRecipe(RecipeInfoVO recipeInfoVO) {
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
        List<RecipeDetailBean> recipeDetails = recipeInfoVO.getRecipeDetails();
        if (CollectionUtils.isNotEmpty(recipeDetails)) {
            boolean recipeDetailId = recipeDetails.stream().anyMatch(a -> !ValidateUtil.integerIsEmpty(a.getRecipeDetailId()));
            if (ValidateUtil.integerIsEmpty(recipeBean.getRecipeId()) && recipeDetailId) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "药品入参错误");
            }
        }
        if (!ValidateUtil.integerIsEmpty(recipeInfoVO.getRecipeBean().getRecipeId()) && null != recipeInfoVO.getRecipeExtendBean()) {
            recipeInfoVO.getRecipeExtendBean().setRecipeId(recipeInfoVO.getRecipeBean().getRecipeId());
        }
        return recipeBusinessService.stagingRecipe(recipeInfoVO);
    }

    /**
     * 暂存并返回处方详情
     *
     * @param recipeInfoVO
     * @return
     */
    @RpcService
    public RecipeInfoVO stagingRecipeAndGet(RecipeInfoVO recipeInfoVO) {
        Integer recipeId = stagingRecipe(recipeInfoVO);
        RecipeDTO recipeDTO = recipeBusinessService.getRecipeDTO(recipeId);
        RecipeInfoVO recipeInfo = new RecipeInfoVO();
        recipeInfo.setRecipeBean(ObjectCopyUtils.convert(recipeDTO.getRecipe(), RecipeBean.class));
        recipeInfo.setRecipeExtendBean(ObjectCopyUtils.convert(recipeDTO.getRecipeExtend(), RecipeExtendBean.class));
        recipeInfo.setRecipeDetails(ObjectCopyUtils.convert(recipeDTO.getRecipeDetails(), RecipeDetailBean.class));
        return recipeInfo;
    }

    /**
     * 处方签名
     *
     * @param recipeId 处方id
     * @param type     1 平台，2互联网
     * @return
     */
    @RpcService
    public Integer signRecipe(Integer recipeId, Integer type) {
        validateAtop(recipeId, type);
        RecipeDTO recipeDTO = recipeBusinessService.getRecipeDTO(recipeId);
        logger.info("WriteRecipeDoctorAtop signRecipe recipeDTO={}", JSON.toJSONString(recipeDTO));
        validateAtop(recipeDTO, recipeDTO.getRecipe(), recipeDTO.getRecipeExtend(), recipeDTO.getRecipeDetails());
        Recipe recipeBean = recipeDTO.getRecipe();
        validateAtop(recipeBean.getClinicOrgan(), recipeBean.getRecipeId(), recipeBean.getDoctor(), recipeBean.getRecipeSupportGiveMode());
        List<Recipedetail> recipeDetails = recipeDTO.getRecipeDetails();
        recipeDetails.forEach(a -> validateAtop(a.getRecipeId(), a.getRecipeDetailId()));
        return recipeBusinessService.signRecipe(recipeDTO, type);
    }

    /**
     * 处方拆方
     *
     * @param recipeInfoVO
     */
    @RpcService
    public String splitRecipe(RecipeInfoVO recipeInfoVO) {
        validateAtop(recipeInfoVO, recipeInfoVO.getRecipeBean(), recipeInfoVO.getRecipeExtendBean(), recipeInfoVO.getRecipeDetails());
        Integer recipeId = recipeInfoVO.getRecipeBean().getRecipeId();
        if (StringUtils.isEmpty(recipeInfoVO.getRecipeBean().getGroupCode())) {
            String uuid = UUID.randomUUID().toString();
            recipeInfoVO.getRecipeBean().setGroupCode(uuid);
        }
        recipeInfoVO.getRecipeExtendBean().setRecipeId(null);
        recipeInfoVO.getRecipeBean().setRecipeId(null);
        recipeInfoVO.getRecipeBean().setTargetedDrugType(0);
        RecipeBusiThreadPool.execute(() -> {
            //智能拆方知识库规则-拆分药品
            List<List<RecipeDetailBean>> retailsList = recipeDetailBusinessService.splitRecipe(recipeInfoVO);
            //算法拆方，拆分可下单处方
            List<List<RecipeDetailBean>> retailsSplitList = new ArrayList<>();
            retailsList.forEach(a -> {
                RecipeDTO recipeDTO = new RecipeDTO();
                recipeDTO.setRecipe(recipe.util.ObjectCopyUtils.convert(recipeInfoVO.getRecipeBean(), Recipe.class));
                recipeDTO.setRecipeDetails(recipe.util.ObjectCopyUtils.convert(a, Recipedetail.class));
                recipeDTO.setRecipeExtend(recipe.util.ObjectCopyUtils.convert(recipeInfoVO.getRecipeExtendBean(), RecipeExtend.class));
                retailsSplitList.addAll(iStockBusinessService.retailsSplitList(recipeDTO));
            });
            logger.info("WriteRecipeDoctorAtop splitRecipe retailsSplitList={}", JSON.toJSONString(retailsSplitList));
            //生成暂存处方
            retailsSplitList.forEach(a -> {
                if (CollectionUtils.isEmpty(a)) {
                    return;
                }
                a.forEach(b -> {
                    b.setRecipeDetailId(null);
                    b.setRecipeId(null);
                });
                boolean targetedDrugType = a.stream().anyMatch(b -> Integer.valueOf(1).equals(b.getTargetedDrugType()));
                if (targetedDrugType) {
                    recipeInfoVO.getRecipeBean().setTargetedDrugType(1);
                }
                recipeInfoVO.setRecipeDetails(a);
                this.stagingRecipe(recipeInfoVO);
                logger.info("WriteRecipeDoctorAtop splitRecipe recipeInfoVO={}", JSON.toJSONString(recipeInfoVO));
            });
            if (!ValidateUtil.integerIsEmpty(recipeId)) {
                recipeBusinessService.deleteByRecipeIds(Collections.singletonList(recipeId));
                logger.info("WriteRecipeDoctorAtop splitRecipe recipeId ={}", recipeId);
            }
        });
        //返回同组处方id
        return recipeInfoVO.getRecipeBean().getGroupCode();
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
    @Deprecated
    public List<Integer> splitDrugRecipeV1(ValidateDetailVO validateDetailVO) {
        String groupCode = this.splitDrugRecipe(validateDetailVO);
        return this.recipeByGroupCode(groupCode, 1);
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

}
