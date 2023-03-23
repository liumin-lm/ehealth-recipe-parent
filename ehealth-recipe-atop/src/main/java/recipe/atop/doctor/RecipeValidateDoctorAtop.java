package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.DoSignRecipeDTO;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.dto.RecipeDTO;
import com.ngari.recipe.dto.RecipeDetailDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.vo.CaseHistoryVO;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
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
import recipe.core.api.patient.IOfflineRecipeBusinessService;
import recipe.enumerate.type.RecipeDrugFormTypeEnum;
import recipe.enumerate.type.RecipeTypeEnum;
import recipe.util.ObjectCopyUtils;
import recipe.util.RecipeUtil;
import recipe.util.ValidateUtil;
import recipe.vo.ResultBean;
import recipe.vo.doctor.ConfigOptionsVO;
import recipe.vo.doctor.ValidateDetailVO;
import recipe.vo.second.MedicalDetailVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 处方校验服务入口类
 *
 * @author fuzi
 */
@RpcBean("recipeDetailAtop")
public class RecipeValidateDoctorAtop extends BaseAtop {

    @Autowired
    private IRecipeDetailBusinessService recipeDetailService;
    @Autowired
    private IRevisitBusinessService iRevisitBusinessService;
    @Autowired
    private IRecipeBusinessService recipeBusinessService;
    @Autowired
    private IStockBusinessService iStockBusinessService;
    @Autowired
    private IOfflineRecipeBusinessService offlineRecipeBusinessService;

    /**
     * 长处方标识 0 不是
     */
    private static final String IS_LONG_RECIPE_FALSE = "0";

    /**
     * 校验线上线下 药品数据 用于续方需求
     *
     * @param validateDetailVO 药品数据VO
     * @return 处方明细
     */
    @RpcService
    public ValidateDetailVO validateDetailV1(ValidateDetailVO validateDetailVO) {
        validateAtop(validateDetailVO.getOrganId(), validateDetailVO.getRecipeType(), validateDetailVO.getRecipeExtendBean(), validateDetailVO.getRecipeDetails());
        validateDetailVO.setLongRecipe(!IS_LONG_RECIPE_FALSE.equals(validateDetailVO.getRecipeExtendBean().getIsLongRecipe()));
        //剂型转换
        Integer recipeDrugForm = null;
        if (RecipeUtil.isTcmType(validateDetailVO.getRecipeType())) {
            if (ValidateUtil.integerIsEmpty(validateDetailVO.getRecipeDrugForm())) {
                recipeDrugForm = RecipeDrugFormTypeEnum.TCM_DECOCTION_PIECES.getType();
                validateDetailVO.setRecipeDrugForm(RecipeDrugFormTypeEnum.TCM_DECOCTION_PIECES.getType());
            } else if (RecipeDrugFormTypeEnum.TCM_FORMULA_CREAM_FORMULA.getType().equals(validateDetailVO.getRecipeDrugForm())) {
                //使用剂型是膏方类型必须把 药房权限同时勾选饮片类型 产品同意 人为控制
                recipeDrugForm = RecipeDrugFormTypeEnum.TCM_FORMULA_CREAM_FORMULA.getType();
                validateDetailVO.setRecipeDrugForm(RecipeDrugFormTypeEnum.TCM_DECOCTION_PIECES.getType());
            } else {
                recipeDrugForm = validateDetailVO.getRecipeDrugForm();
            }
        }
        //药房提取
        String pharmacyCode = validateDetailVO.getRecipeDetails().stream().filter(validateDetail -> StringUtils.isNotEmpty(validateDetail.getPharmacyCode()))
                .findFirst().map(RecipeDetailBean::getPharmacyCode).orElse(null);
        validateDetailVO.setPharmacyCode(pharmacyCode);
        Integer pharmacyId = validateDetailVO.getRecipeDetails().stream().filter(validateDetail -> !ValidateUtil.integerIsEmpty(validateDetail.getPharmacyId()))
                .findFirst().map(RecipeDetailBean::getPharmacyId).orElse(null);
        validateDetailVO.setPharmacyId(pharmacyId);
        validateDetailVO.getRecipeDetails().forEach(a -> {
            a.setPharmacyId(null);
            a.setPharmacyName(null);
            a.setPharmacyCode(null);
        });
        //出参处理
        ValidateDetailVO validateDetail = recipeDetailService.continueRecipeValidateDrug(validateDetailVO);
        validateDetail.setRecipeDrugForm(recipeDrugForm);
        return validateDetail;
    }


    /**
     * 拆方校验
     *
     * @param validateDetailVO
     */
    @RpcService
    public String validateSplitRecipe(ValidateDetailVO validateDetailVO) {
        validateAtop(validateDetailVO, validateDetailVO.getRecipeBean(), validateDetailVO.getRecipeExtendBean(), validateDetailVO.getRecipeDetails());
        if (RecipeUtil.isTcmType(validateDetailVO.getRecipeBean().getRecipeType())) {
            return null;
        }
        //判断数量
        if (validateDetailVO.getRecipeDetails().size() > 5) {
            return "因为【处方的药品数量>5个】，需要进行拆分，请确认";
        }
        String msg = recipeDetailService.validateSplitRecipe(validateDetailVO);
        if (StringUtils.isNotEmpty(msg)) {
            return msg;
        }
        RecipeDTO recipeDTO = new RecipeDTO();
        recipeDTO.setRecipe(ObjectCopyUtils.convert(validateDetailVO.getRecipeBean(), Recipe.class));
        recipeDTO.setRecipeDetails(ObjectCopyUtils.convert(validateDetailVO.getRecipeDetails(), Recipedetail.class));
        recipeDTO.setRecipeExtend(ObjectCopyUtils.convert(validateDetailVO.getRecipeExtendBean(), RecipeExtend.class));
        List<EnterpriseStock> result = iStockBusinessService.stockList(recipeDTO);
        boolean stock = result.stream().anyMatch(EnterpriseStock::getStock);
        //查看库存是否满足
        if (!stock) {
            return "因为【无一个供药方可以供应所有药品】，需要进行拆分，请确认";
        }
        return null;
    }

    /**
     * his处方 预校验
     *
     * @param recipeId 处方id
     */
    @RpcService
    public DoSignRecipeDTO hisRecipeCheck(Integer recipeId) {
        validateAtop(recipeId);
        RecipeDTO recipeDTO = recipeBusinessService.getRecipeDTO(recipeId);
        validateAtop(recipeDTO.getRecipe(), recipeDTO.getRecipeExtend(), recipeDTO.getRecipeDetails());
        DoSignRecipeDTO doSignRecipe = offlineRecipeBusinessService.hisRecipeCheck(recipeDTO);
        if (null == doSignRecipe) {
            doSignRecipe = new DoSignRecipeDTO();
            doSignRecipe.setSignResult(true);
            doSignRecipe.setCanContinueFlag("0");
            return doSignRecipe;
        }
        enterpriseBusinessService.checkRecipeGiveDeliveryMsg(doSignRecipe, ObjectCopyUtils.convert(recipeDTO.getRecipe(), RecipeBean.class));
        doSignRecipe.setMap(null);
        return doSignRecipe;
    }

    /**
     * 校验his 药品规则，靶向药，大病医保，抗肿瘤药物等
     *
     * @param validateDetailVO 药品信息
     * @return
     */
    @RpcService
    public List<RecipeDetailBean> validateHisDrugRule(ValidateDetailVO validateDetailVO) {
        logger.info("RecipeValidateDoctorAtop validateHisDrugRule validateDetailVO ：{}", JSONUtils.toString(validateDetailVO));
        validateAtop(validateDetailVO, validateDetailVO.getRecipeDetails(), validateDetailVO.getVersion(), validateDetailVO.getRecipeBean(), validateDetailVO.getRecipeExtendBean());
        List<RecipeDetailDTO> recipeDetailDTO = ObjectCopyUtils.convert(validateDetailVO.getRecipeDetails(), RecipeDetailDTO.class);
        recipeDetailDTO.forEach(a -> a.setValidateHisStatus(0));
        if (validateDetailVO.getVersion().equals(1)) {
            return ObjectCopyUtils.convert(recipeDetailDTO, RecipeDetailBean.class);
        }
        Recipe recipe = ObjectCopyUtils.convert(validateDetailVO.getRecipeBean(), Recipe.class);
        validateAtop(recipe.getClinicOrgan(), recipe.getDoctor(), recipe.getDepart());
        // 校验his 药品规则，靶向药，大病医保，抗肿瘤药物等
        List<RecipeDetailDTO> recipeDetail = recipeDetailDTO.stream().filter(a -> !ValidateUtil.integerIsEmpty(a.getDrugId())).collect(Collectors.toList());
        List<RecipeDetailDTO> result = recipeDetailService.validateHisDrugRule(recipe, recipeDetail, validateDetailVO.getRecipeExtendBean().getRegisterID(), validateDetailVO.getDbType());
        //返回数据处理
        List<RecipeDetailDTO> recipeDetailLose = recipeDetailDTO.stream().filter(a -> ValidateUtil.integerIsEmpty(a.getDrugId())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(recipeDetailLose)) {
            recipeDetailLose.forEach(a -> {
                a.setValidateHisStatus(-1);
                a.setValidateHisStatusText("不在可开方目录或者对应药房内");
            });
            result.addAll(recipeDetailLose);
        }
        return ObjectCopyUtils.convert(result, RecipeDetailBean.class);
    }

    /**
     * 复杂逻辑配置项处理
     * 由于判断配置项逻辑 对于前端复杂，由后端统一处理返回结果，此接口仅仅处理复杂逻辑判断
     *
     * @param validateDetailVO
     * @return
     */
    @RpcService
    public List<ConfigOptionsVO> validateConfigOptions(ValidateDetailVO validateDetailVO) {
        validateAtop(validateDetailVO.getRecipeDetails(), validateDetailVO.getRecipeType(), validateDetailVO.getOrganId());
        List<RecipeDetailBean> recipeDetails = validateDetailVO.getRecipeDetails();
        recipeDetails.forEach(a -> validateAtop(a.getSalePrice()));
        return recipeDetailService.validateConfigOptions(validateDetailVO);
    }

    /**
     * 校验处方药品配置时间
     *
     * @param validateDetailVO 药品数据VO
     * @return
     */
    @RpcService
    public List<RecipeDetailBean> useDayValidate(ValidateDetailVO validateDetailVO) {
        logger.info("RecipeValidateDoctorAtop useDayValidate validateDetailVO {}", JSON.toJSONString(validateDetailVO));
        validateAtop(validateDetailVO.getOrganId(), validateDetailVO.getRecipeType(), validateDetailVO.getRecipeExtendBean(), validateDetailVO.getRecipeDetails());
        validateDetailVO.setLongRecipe(!IS_LONG_RECIPE_FALSE.equals(validateDetailVO.getRecipeExtendBean().getIsLongRecipe()));
        try {
            List<RecipeDetailBean> result = recipeDetailService.useDayValidate(validateDetailVO);
            logger.info("RecipeValidateDoctorAtop useDayValidate result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeValidateDoctorAtop useDayValidate error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeDetailAtop useDayValidate error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 检验处方药品超量
     *
     * @param validateDetailVO 药品数据VO
     * @return 处方药品明细
     */
    @RpcService
    public List<RecipeDetailBean> drugSuperScalarValidate(ValidateDetailVO validateDetailVO) {
        validateAtop(validateDetailVO.getOrganId());
        if (RecipeUtil.isTcmType(validateDetailVO.getRecipeType()) || CollectionUtils.isEmpty(validateDetailVO.getRecipeDetails())) {
            return validateDetailVO.getRecipeDetails();
        }
        return recipeDetailService.drugSuperScalarValidate(validateDetailVO);
    }


    /**
     * 校验有效复诊单
     *
     * @param mpiId    患者id
     * @param doctorId 医生id
     * @param organId  机构id
     * @return
     */
    @RpcService
    @Deprecated
    public Boolean revisitValidate(String mpiId, Integer doctorId, Integer organId) {
        logger.info("RecipeValidateDoctorAtop revisitValidate mpiId: {},doctorId :{},organId :{}", mpiId, doctorId, organId);
        validateAtop(mpiId, doctorId, organId);
        Recipe recipe = new Recipe();
        recipe.setMpiid(mpiId);
        recipe.setDoctor(doctorId);
        recipe.setClinicOrgan(organId);
        try {
            Boolean result = iRevisitBusinessService.revisitValidate(recipe);
            logger.info("RecipeValidateDoctorAtop revisitValidate result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.warn("RecipeValidateDoctorAtop revisitValidate error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeValidateDoctorAtop revisitValidate error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 重复处方规则校验
     *
     * @param validateDetailVO 当前处方药品
     * @return
     */
    @RpcService
    public ResultBean<String> validateRepeatRecipe(ValidateDetailVO validateDetailVO) {
        validateAtop(validateDetailVO.getRecipeBean(), validateDetailVO.getRecipeBean().getClinicOrgan(), validateDetailVO.getRecipeDetails());
        if (ValidateUtil.integerIsEmpty(validateDetailVO.getRecipeBean().getClinicId())) {
            ResultBean<String> resultBean = new ResultBean<>();
            resultBean.setBool(true);
            return resultBean;
        }
        //校验复诊下重复药品数量
        if(!RecipeTypeEnum.RECIPETYPE_TCM.getType().equals(validateDetailVO.getRecipeBean().getRecipeType())) {
            // 中药不校验开药上限
            ResultBean<String> detail = recipeDetailService.validateRepeatRecipeDetail(validateDetailVO);
            if (!detail.isBool()) {
                return detail;
            }
        }
        //校验复诊下重复处方
        return recipeDetailService.validateRepeatRecipe(validateDetailVO);
    }

    /**
     * 校验开处方单数限制
     *
     * @param clinicId 复诊id
     * @param organId  机构id
     * @return true 可开方
     */
    @RpcService
    public Boolean validateOpenRecipeNumber(Integer clinicId, Integer organId, Integer recipeId) {
        logger.info("RecipeValidateDoctorAtop validateOpenRecipeNumber clinicId ：{},organId ：{},recipeId ：{}", clinicId, organId, recipeId);
        validateAtop(organId);
        if (ValidateUtil.integerIsEmpty(clinicId)) {
            return true;
        }
        return recipeBusinessService.validateOpenRecipeNumber(clinicId, organId, recipeId);
    }

    /**
     * 获取电子病历数据
     *
     * @param caseHistoryVO 电子病历查询对象
     */
    @RpcService
    public MedicalDetailVO getDocIndexInfoV1(CaseHistoryVO caseHistoryVO) {
        validateAtop(caseHistoryVO, caseHistoryVO.getActionType());
        if (ValidateUtil.integerIsEmpty(caseHistoryVO.getClinicId())
                && ValidateUtil.integerIsEmpty(caseHistoryVO.getRecipeId())
                && ValidateUtil.integerIsEmpty(caseHistoryVO.getDocIndexId())) {
            return new MedicalDetailVO();
        }
        MedicalDetailVO result = recipeBusinessService.getDocIndexInfo(caseHistoryVO);
        if (null == result) {
            return null;
        }
        caseHistoryVO.setDocIndexId(result.getDocIndexId());
        if (ValidateUtil.validateObjects(caseHistoryVO.getDepartId(), caseHistoryVO.getDoctorId())) {
            return result;
        }
        recipeBusinessService.updateDocIndexInfo(caseHistoryVO);
        return result;
    }


}
