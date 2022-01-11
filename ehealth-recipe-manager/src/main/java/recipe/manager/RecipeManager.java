package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.schema.annotation.DesensitizationsType;
import ctd.util.DesensitizationsUtil;
import ctd.util.JSONUtils;
import eh.base.constant.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.*;
import recipe.common.CommonConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeLogDAO;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.RecipeShowQrConfigEnum;
import recipe.util.DictionaryUtil;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 处方
 *
 * @author yinsheng
 * @date 2021\6\30 0030 14:21
 */
@Service
public class RecipeManager extends BaseManager {
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private DocIndexClient docIndexClient;
    @Resource
    private IConfigurationClient configurationClient;
    @Resource
    private OfflineRecipeClient offlineRecipeClient;
    @Autowired
    private RevisitClient revisitClient;
    @Autowired
    private EnterpriseManager enterpriseManager;

    /**
     * 保存处方信息
     *
     * @param recipe 处方信息
     * @return
     */
    public Recipe saveRecipe(Recipe recipe) {
        if (ValidateUtil.integerIsEmpty(recipe.getRecipeId())) {
            recipe.setCreateDate(new Date());
            recipe = recipeDAO.save(recipe);
        } else {
            recipe = recipeDAO.update(recipe);
        }
        logger.info("RecipeManager saveRecipe recipe:{}", JSONUtils.toString(recipe));
        return recipe;
    }

    /**
     * 保存处方扩展信息
     *
     * @param recipeExtend 处方扩展信息
     * @param recipe       处方信息
     * @return
     */
    public RecipeExtend saveRecipeExtend(RecipeExtend recipeExtend, Recipe recipe) {
        if (!ValidateUtil.integerIsEmpty(recipe.getClinicId())) {
            RevisitExDTO revisitExDTO = revisitClient.getByClinicId(recipe.getClinicId());
            recipeExtend.setCardNo(revisitExDTO.getCardId());
        }
        if (ValidateUtil.integerIsEmpty(recipeExtend.getRecipeId())) {
            recipeExtend.setRecipeId(recipe.getRecipeId());
            recipeExtend = recipeExtendDAO.save(recipeExtend);
        } else {
            recipeExtend = recipeExtendDAO.update(recipeExtend);
        }
        logger.info("RecipeManager saveRecipeExtend recipeExtend:{}", JSONUtils.toString(recipeExtend));
        return recipeExtend;
    }


    /**
     * 获取处方信息
     *
     * @param recipeCode
     * @param clinicOrgan
     * @return
     */
    public Recipe getByRecipeCodeAndClinicOrgan(String recipeCode, Integer clinicOrgan) {
        logger.info("RecipeManager getByRecipeCodeAndClinicOrgan param recipeCode:{},clinicOrgan:{}", recipeCode, clinicOrgan);
        Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(recipeCode, clinicOrgan);
        logger.info("RecipeManager getByRecipeCodeAndClinicOrgan res recipe:{}", JSONUtils.toString(recipe));
        return recipe;
    }


    /**
     * 查询处方信息
     *
     * @param recipeId
     * @return
     */
    public Recipe getRecipeById(Integer recipeId) {
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (StringUtils.isEmpty(recipe.getOrganDiseaseId())) {
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
            EmrDetailDTO emrDetail = docIndexClient.getEmrDetails(recipeExtend.getDocIndexId());
            recipe.setOrganDiseaseId(emrDetail.getOrganDiseaseId());
            recipe.setOrganDiseaseName(emrDetail.getOrganDiseaseName());
            recipe.setMemo(emrDetail.getMemo());
        }
        return recipe;
    }

    public List<Recipe> findByRecipeIds(List<Integer> recipeIds) {
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIds);
        logger.info("RecipeManager findByRecipeIds recipeIds:{}, recipes:{}", JSON.toJSONString(recipeIds), JSON.toJSONString(recipes));
        return recipes;
    }


    /**
     * 根据业务类型(咨询/复诊)和业务单号(咨询/复诊单号)获取处方信息
     *
     * @param bussSource 咨询/复诊
     * @param clinicId   咨询/复诊单号
     * @return 处方列表
     */
    public List<Recipe> findWriteHisRecipeByBussSourceAndClinicId(Integer bussSource, Integer clinicId) {
        logger.info("RecipeManager findWriteHisRecipeByBussSourceAndClinicId param bussSource:{},clinicId:{}", bussSource, clinicId);
        List<Recipe> recipes = recipeDAO.findWriteHisRecipeByBussSourceAndClinicId(bussSource, clinicId);
        logger.info("RecipeManager findWriteHisRecipeByBussSourceAndClinicId recipes:{}.", JSON.toJSONString(recipes));
        return recipes;
    }

    /**
     * 获取有效的处方单
     *
     * @param bussSource
     * @param clinicId
     * @return
     */
    public List<Recipe> findEffectiveRecipeByBussSourceAndClinicId(Integer bussSource, Integer clinicId) {
        logger.info("RecipeManager findRecipeByBussSourceAndClinicId param bussSource:{},clinicId:{}", bussSource, clinicId);
        List<Recipe> recipes = recipeDAO.findEffectiveRecipeByBussSourceAndClinicId(bussSource, clinicId);
        logger.info("RecipeManager findEffectiveRecipeByBussSourceAndClinicId recipes:{}.", JSON.toJSONString(recipes));
        return recipes;
    }


    /**
     * 获取处方相关信息 并且 字典转换
     *
     * @param recipeId 处方id
     * @return
     */
    public RecipeInfoDTO getRecipeInfoDictionary(Integer recipeId) {
        RecipeInfoDTO recipeInfoDTO = getRecipeInfoDTO(recipeId);
        PatientDTO patientBean = recipeInfoDTO.getPatientBean();
        if (StringUtils.isNotEmpty(patientBean.getPatientSex())) {
            patientBean.setPatientSex(DictionaryUtil.getDictionary("eh.base.dictionary.Gender", String.valueOf(patientBean.getPatientSex())));
        }
        return recipeInfoDTO;
    }

    /**
     * 获取处方相关信息
     *
     * @param recipeId 处方id
     * @return
     */
    public RecipeInfoDTO getRecipeInfoDTO(Integer recipeId) {
        RecipeDTO recipeDTO = getRecipeDTO(recipeId);
        RecipeInfoDTO recipeInfoDTO = new RecipeInfoDTO();
        BeanUtils.copyProperties(recipeDTO, recipeInfoDTO);
        Recipe recipe = recipeInfoDTO.getRecipe();
        PatientDTO patientBean = patientClient.getPatientEncipher(recipe.getMpiid());
        recipeInfoDTO.setPatientBean(patientBean);
        logger.info("RecipeOrderManager getRecipeInfoDTO patientBean:{}", JSON.toJSONString(patientBean));
        return recipeInfoDTO;
    }

    /**
     * 获取处方相关信息 补全数据
     *
     * @param recipeId 处方id
     * @return
     */
    @Override
    public RecipeDTO getRecipeDTO(Integer recipeId) {
        RecipeDTO recipeDTO = super.getRecipeDTO(recipeId);
        RecipeExtend recipeExtend = recipeDTO.getRecipeExtend();
        if (null == recipeExtend) {
            return recipeDTO;
        }
        recipeExtend.setCardTypeName(DictionaryUtil.getDictionary("eh.mpi.dictionary.CardType", recipeExtend.getCardType()));
        Integer docIndexId = recipeExtend.getDocIndexId();
        EmrDetailDTO emrDetail = docIndexClient.getEmrDetails(docIndexId);
        if (StringUtils.isEmpty(emrDetail.getOrganDiseaseId())) {
            return recipeDTO;
        }
        Recipe recipe = recipeDTO.getRecipe();
        recipe.setOrganDiseaseId(emrDetail.getOrganDiseaseId());
        recipe.setOrganDiseaseName(emrDetail.getOrganDiseaseName());
        recipe.setMemo(emrDetail.getMemo());
        recipeExtend.setSymptomId(emrDetail.getSymptomId());
        recipeExtend.setSymptomName(emrDetail.getSymptomName());
        recipeExtend.setAllergyMedical(emrDetail.getAllergyMedical());
        if (!ValidateUtil.integerIsEmpty(recipe.getClinicId()) && StringUtils.isEmpty(recipeExtend.getCardNo())) {
            RevisitExDTO consultExDTO = revisitClient.getByClinicId(recipe.getClinicId());
            if (null != consultExDTO) {
                recipeExtend.setCardNo(consultExDTO.getCardId());
                recipeExtend.setCardType(consultExDTO.getCardType());
            }
        }
        //当卡类型是医保卡的时候，调用端的配置判断是否开启，如果开启，则调用端提供的工具进行卡号的展示
        if ("2".equals(recipeExtend.getCardType())) {
            boolean hospitalCardLengthControl = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "hospitalCardLengthControl", false);
            if (hospitalCardLengthControl && StringUtils.isNotBlank(recipeExtend.getCardNo()) && recipeExtend.getCardNo().length() == 28) {
                recipeExtend.setCardNo(recipeExtend.getCardNo().substring(0, 10));
            }
        }
        recipeExtend.setCardNo(DesensitizationsUtil.instance().processField(DesensitizationsType.HEALTHCARD.getType(), recipeExtend.getCardNo()));
        return recipeDTO;
    }


    /**
     * 获取到院取药凭证
     *
     * @param recipe       处方信息
     * @param recipeExtend 处方扩展信息
     * @return 取药凭证
     */
    public String getToHosProof(Recipe recipe, RecipeExtend recipeExtend, RecipeOrder order) {
        String qrName = "";
        try {
            OrganDrugsSaleConfig organDrugsSaleConfig = enterpriseManager.getOrganDrugsSaleConfig(order.getOrganId(), order.getEnterpriseId());
            RecipeShowQrConfigEnum qrConfigEnum = RecipeShowQrConfigEnum.getEnumByType(organDrugsSaleConfig.getTakeDrugsVoucher());
            switch (qrConfigEnum) {
                case CARD_NO:
                    //就诊卡号
                    if (StringUtils.isNotEmpty(recipeExtend.getCardNo())) {
                        qrName = recipeExtend.getCardNo();
                    }
                    break;
                case REGISTER_ID:
                    if (StringUtils.isNotEmpty(recipeExtend.getRegisterID())) {
                        qrName = recipeExtend.getRegisterID();
                    }
                    break;
                case PATIENT_ID:
                    if (StringUtils.isNotEmpty(recipe.getPatientID())) {
                        qrName = recipe.getPatientID();
                    }
                    break;
                case MEDICAL_RECORD_NUMBER:
                    //病历号
                    if (StringUtils.isNotEmpty(recipeExtend.getMedicalRecordNumber())) {
                        qrName = recipeExtend.getMedicalRecordNumber();
                    }
                    break;
                case RECIPE_CODE:
                    if (StringUtils.isNotEmpty(recipe.getRecipeCode())) {
                        qrName = recipe.getRecipeCode();
                    }
                    break;
                case TAKE_DRUG_CODE:
                    qrName = offlineRecipeClient.queryMedicineCode(recipe.getClinicOrgan(), recipe.getRecipeId(), recipe.getRecipeCode());
                    break;
                case SERIALNUMBER:
                    qrName = offlineRecipeClient.queryRecipeSerialNumber(recipe.getClinicOrgan(), recipe.getPatientName(), recipe.getPatientID(), recipeExtend.getRegisterID());
                default:
                    break;
            }
        } catch (Exception e) {
            logger.error("RecipeManager getToHosProof error", e);
        }
        return qrName;
    }

    /**
     * 获取医生撤销处方时间和原因
     *
     * @param recipeId
     * @return
     */
    public RecipeCancelDTO getCancelReasonForPatient(int recipeId) {
        RecipeCancelDTO recipeCancel = new RecipeCancelDTO();
        String cancelReason = "";
        Date cancelDate = null;
        RecipeLogDAO recipeLogDAO = DAOFactory.getDAO(RecipeLogDAO.class);
        List<RecipeLog> recipeLogs = recipeLogDAO.findByRecipeIdAndAfterStatus(recipeId, RecipeStatusConstant.REVOKE);
        if (CollectionUtils.isNotEmpty(recipeLogs)) {
            cancelReason = recipeLogs.get(0).getMemo();
            cancelDate = recipeLogs.get(0).getModifyDate();
        }
        recipeCancel.setCancelDate(cancelDate);
        recipeCancel.setCancelReason(cancelReason);
        logger.info("getCancelReasonForPatient recipeCancel:{}", JSONUtils.toString(recipeCancel));
        return recipeCancel;
    }

    /**
     * 根据订单号查询处方列表
     *
     * @param orderCode orderCode
     * @return List<Recipe>
     */
    public List<Recipe> findRecipeByOrderCode(String orderCode) {
        return recipeDAO.findRecipeListByOrderCode(orderCode);
    }

    /**
     * 更新推送his返回信息处方数据
     *
     * @param recipeResult 处方结果
     * @param recipeId     处方id
     * @param pushType     推送类型: 1：提交处方，2:撤销处方
     */
    public void updatePushHisRecipe(Recipe recipeResult, Integer recipeId, Integer pushType) {
        if (null == recipeResult) {
            return;
        }
        if (!CommonConstant.RECIPE_PUSH_TYPE.equals(pushType)) {
            return;
        }
        Recipe updateRecipe = new Recipe();
        updateRecipe.setRecipeId(recipeId);
        updateRecipe.setPatientID(recipeResult.getPatientID());
        updateRecipe.setRecipeCode(recipeResult.getRecipeCode());
        recipeDAO.updateNonNullFieldByPrimaryKey(updateRecipe);
        logger.info("RecipeManager updatePushHisRecipe updateRecipe:{}.", JSON.toJSONString(updateRecipe));
    }

    /**
     * 更新推送his返回信息处方扩展数据
     *
     * @param recipeExtendResult 处方扩展结果
     * @param recipeId           处方id
     * @param pushType           推送类型: 1：提交处方，2:撤销处方
     */
    public void updatePushHisRecipeExt(RecipeExtend recipeExtendResult, Integer recipeId, Integer pushType) {
        if (null == recipeExtendResult) {
            return;
        }
        if (!CommonConstant.RECIPE_PUSH_TYPE.equals(pushType)) {
            return;
        }
        RecipeExtend updateRecipeExt = new RecipeExtend();
        updateRecipeExt.setRecipeId(recipeId);
        updateRecipeExt.setRegisterID(recipeExtendResult.getRegisterID());
        updateRecipeExt.setMedicalType(recipeExtendResult.getMedicalType());
        updateRecipeExt.setMedicalTypeText(recipeExtendResult.getMedicalTypeText());
        updateRecipeExt.setRecipeCostNumber(recipeExtendResult.getRecipeCostNumber());
        updateRecipeExt.setHisDiseaseSerial(recipeExtendResult.getHisDiseaseSerial());
        updateRecipeExt.setMedicalRecordNumber(recipeExtendResult.getMedicalRecordNumber());
        recipeExtendDAO.updateNonNullFieldByPrimaryKey(updateRecipeExt);
        logger.info("RecipeManager updatePushHisRecipeExt updateRecipeExt:{}.", JSON.toJSONString(updateRecipeExt));
    }

    /**
     * 校验开处方单数限制
     * todo 废弃 其他接口中的引用 ，目前提供前端调用接口，暂时保留老代码支持兼容老app使用
     *
     * @param clinicId 复诊id
     * @param organId  机构id
     * @return true 可开方
     */
    @Deprecated
    public Boolean isOpenRecipeNumber(Integer clinicId, Integer organId, Integer recipeId) {
        logger.info("RecipeManager isOpenRecipeNumber clinicId: {},organId: {}", clinicId, organId);
        if (ValidateUtil.integerIsEmpty(clinicId)) {
            return true;
        }
        //运营平台没有处方单数限制，默认可以无限进行开处方
        Integer openRecipeNumber = configurationClient.getValueCatch(organId, "openRecipeNumber", 99);
        logger.info("RecipeManager isOpenRecipeNumber openRecipeNumber={}", openRecipeNumber);
        if (ValidateUtil.integerIsEmpty(openRecipeNumber)) {
            saveRecipeLog(recipeId, RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS, RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS, "开方张数已超出医院限定范围，不能继续开方。");
            throw new DAOException(ErrorCode.SERVICE_ERROR, "开方张数0已超出医院限定范围，不能继续开方。");
        }
        //查询当前复诊存在的有效处方单
        List<Recipe> recipeCount = recipeDAO.findRecipeClinicIdAndStatus(clinicId, RecipeStatusEnum.RECIPE_REPEAT_COUNT);
        if (CollectionUtils.isEmpty(recipeCount)) {
            return true;
        }
        List<Integer> recipeIds;
        if (ValidateUtil.integerIsEmpty(recipeId)) {
            recipeIds = recipeCount.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        } else {
            recipeIds = recipeCount.stream().filter(a -> !a.getRecipeId().equals(recipeId)).map(Recipe::getRecipeId).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(recipeIds)) {
            return true;
        }
        logger.info("RecipeManager isOpenRecipeNumber recipeCount={}", recipeIds.size());
        if (recipeIds.size() >= openRecipeNumber) {
            saveRecipeLog(recipeId, RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS, RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS, "开方张数已超出医院限定范围，不能继续开方。");
            throw new DAOException(ErrorCode.SERVICE_ERROR, "开方张数已超出医院限定范围，不能继续开方。");
        }
        return true;
    }

    /**
     * 根据复诊id获取处方明细，并排除 特定处方id
     *
     * @param clinicId 复诊id
     * @param recipeId 特定处方id
     * @return 处方明细
     */
    public List<Integer> findRecipeByClinicId(Integer clinicId, Integer recipeId, List<Integer> status) {
        List<Recipe> recipeList = recipeDAO.findRecipeClinicIdAndStatus(clinicId, status);
        logger.info("RecipeManager findRecipeByClinicId recipeList:{}", JSON.toJSONString(recipeList));
        if (CollectionUtils.isEmpty(recipeList)) {
            return null;
        }
        List<Integer> recipeIds;
        if (ValidateUtil.integerIsEmpty(recipeId)) {
            recipeIds = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        } else {
            recipeIds = recipeList.stream().filter(a -> !a.getRecipeId().equals(recipeId)).map(Recipe::getRecipeId).collect(Collectors.toList());
        }
        return recipeIds;
    }

}
