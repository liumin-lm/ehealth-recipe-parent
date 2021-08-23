package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeLog;
import com.ngari.recipe.entity.RecipeRefund;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.*;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeLogDAO;
import recipe.dao.RecipeRefundDAO;
import recipe.dao.RecipeTherapyDAO;
import recipe.util.DictionaryUtil;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

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
    private RecipeRefundDAO recipeRefundDAO;
    @Autowired
    private RecipeTherapyDAO recipeTherapyDAO;

    public Recipe saveRecipe(Recipe recipe) {
        if (ValidateUtil.integerIsEmpty(recipe.getRecipeId())) {
            recipe = recipeDAO.save(recipe);
        } else {
            recipe = recipeDAO.update(recipe);
        }
        return recipe;
    }


    public RecipeExtend saveRecipeExtend(RecipeExtend recipeExtend, Recipe recipe) {
        if (ValidateUtil.integerIsEmpty(recipeExtend.getRecipeId())) {
            recipeExtend.setRecipeId(recipe.getRecipeId());
            recipeExtend = recipeExtendDAO.save(recipeExtend);
        } else {
            recipeExtend = recipeExtendDAO.update(recipeExtend);
        }
        return recipeExtend;
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
        RecipeExtend recipeExtend = recipeDTO.getRecipeExtend();
        if (null == recipeExtend) {
            return recipeInfoDTO;
        }
        recipeExtend.setCardTypeName(DictionaryUtil.getDictionary("eh.mpi.dictionary.CardType", recipeExtend.getCardType()));
        Integer docIndexId = recipeExtend.getDocIndexId();
        EmrDetailDTO emrDetail = docIndexClient.getEmrDetails(docIndexId);
        if (null == emrDetail) {
            return recipeInfoDTO;
        }
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
        logger.info("RecipeOrderManager getRecipeInfoDTO patientBean:{}", JSON.toJSONString(patientBean));
        return recipeInfoDTO;
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
        Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(recipeCode, clinicOrgan);
        logger.info("RecipeManager getByRecipeCodeAndClinicOrgan res recipe:{}", JSONUtils.toString(recipe));
        return recipe;
    }

    public Recipe getRecipeById(Integer recipeId) {
        return recipeDAO.getByRecipeId(recipeId);
    }

    /**
     * 通过recipeCode批量获取处方信息
     *
     * @param recipeCodeList
     * @param clinicOrgan
     * @return
     */
    public List<Recipe> findByRecipeCodeAndClinicOrgan(List<String> recipeCodeList, Integer clinicOrgan) {
        logger.info("RecipeManager findByRecipeCodeAndClinicOrgan param recipeCodeList:{},clinicOrgan:{}", JSONUtils.toString(recipeCodeList), clinicOrgan);
        List<Recipe> recipes = recipeDAO.findByRecipeCodeAndClinicOrgan(recipeCodeList, clinicOrgan);
        logger.info("RecipeManager findByRecipeCodeAndClinicOrgan res recipes:{}", JSONUtils.toString(recipes));
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

    public List<Recipe> findEffectiveRecipeByBussSourceAndClinicId(Integer bussSource, Integer clinicId) {
        logger.info("RecipeManager findRecipeByBussSourceAndClinicId param bussSource:{},clinicId:{}", bussSource, clinicId);
        List<Recipe> recipes = recipeDAO.findEffectiveRecipeByBussSourceAndClinicId(bussSource, clinicId);
        logger.info("RecipeManager findEffectiveRecipeByBussSourceAndClinicId recipes:{}.", JSON.toJSONString(recipes));
        return recipes;
    }

    /**
     * 获取到院取药凭证
     *
     * @param recipe       处方信息
     * @param recipeExtend 处方扩展信息
     * @return 取药凭证
     */
    public String getToHosProof(Recipe recipe, RecipeExtend recipeExtend) {
        String qrName = "";
        try {
            Integer qrTypeForRecipe = configurationClient.getValueCatchReturnInteger(recipe.getClinicOrgan(), "getQrTypeForRecipe", 1);
            switch (qrTypeForRecipe) {
                case 1:
                    break;
                case 2:
                    //就诊卡号
                    if (StringUtils.isNotEmpty(recipeExtend.getCardNo())) {
                        qrName = recipeExtend.getCardNo();
                    }
                    break;
                case 3:
                    if (StringUtils.isNotEmpty(recipeExtend.getRegisterID())) {
                        qrName = recipeExtend.getRegisterID();
                    }
                    break;
                case 4:
                    if (StringUtils.isNotEmpty(recipe.getPatientID())) {
                        qrName = recipe.getPatientID();
                    }
                    break;
                case 5:
                    if (StringUtils.isNotEmpty(recipe.getRecipeCode())) {
                        qrName = recipe.getRecipeCode();
                    }
                    break;
                case 6:
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
     * 获取处方撤销时间和原因
     *
     * @param recipeId
     * @return
     */
    public RecipeCancel getCancelReasonForPatient(int recipeId) {
        RecipeCancel recipeCancel = new RecipeCancel();
        String cancelReason = "";
        Date cancelDate = null;
        List<RecipeRefund> recipeRefunds = recipeRefundDAO.findRefundListByRecipeId(recipeId);
        if (CollectionUtils.isNotEmpty(recipeRefunds)) {
            cancelReason = "由于患者申请退费成功，该处方已取消。";
        } else {
            RecipeLogDAO recipeLogDAO = DAOFactory.getDAO(RecipeLogDAO.class);
            List<RecipeLog> recipeLogs = recipeLogDAO.findByRecipeIdAndAfterStatusDesc(recipeId, RecipeStatusConstant.REVOKE);
            if (CollectionUtils.isNotEmpty(recipeLogs)) {
                cancelReason = "开方医生已撤销处方,撤销原因:" + recipeLogs.get(0).getMemo();
                cancelDate = recipeLogs.get(0).getModifyDate();
            }
        }
        recipeCancel.setCancelDate(cancelDate);
        recipeCancel.setCancelReason(cancelReason);
        return recipeCancel;
    }
}
