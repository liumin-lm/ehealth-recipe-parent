package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.EmrDetailDTO;
import com.ngari.recipe.dto.EmrDetailValueDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import eh.cdr.api.service.IDocIndexService;
import eh.cdr.api.vo.MedicalDetailBean;
import eh.cdr.api.vo.MedicalInfoBean;
import eh.cdr.api.vo.response.EmrConfigRes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import recipe.client.DocIndexClient;
import recipe.client.DoctorClient;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeEmrComment;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.util.ByteUtils;
import recipe.util.ValidateUtil;

import java.util.List;

/**
 * 电子病历
 *
 * @author yinsheng
 * @date 2020\8\18 0018 08:57
 */
@Service
public class EmrRecipeManager extends BaseManager {
    private static final Logger logger = LoggerFactory.getLogger(EmrRecipeManager.class);
    @Autowired
    private DocIndexClient docIndexClient;
    @Autowired
    private DoctorClient doctorClient;
    @Autowired
    private RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    /**
     * 根据病历id 获取 电子病例明细对象
     *
     * @param docIndexId 电子病历id
     * @return
     */
    public MedicalDetailBean getEmrDetails(Integer docIndexId) {
        return docIndexClient.getEmrMedicalDetail(docIndexId);
    }

    /**
     * 根据复诊id 获取 电子病例明细对象
     *
     * @param clinicId 复诊id
     * @return
     */
    public MedicalDetailBean getEmrDetailsByClinicId(Integer clinicId) {
        return docIndexClient.getEmrDetailsByClinicId(clinicId);
    }

    /**
     * 根据老处方获取一个新的电子病历数据 有复诊就关联复诊
     *
     * @param recipeId 处方id
     * @param clinicId 复诊id
     * @return
     */
    public MedicalDetailBean copyEmrDetails(Integer recipeId, Integer clinicId) {
        if (ValidateUtil.integerIsEmpty(recipeId)) {
            return null;
        }
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            return null;
        }
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        return docIndexClient.copyEmrDetails(recipe, recipeExtend, clinicId);
    }

    /**
     * 保存电子病历 主要用于兼容线下处方
     *
     * @param recipe    处方
     * @param recipeExt 处方扩展
     */
    public void saveMedicalInfo(Recipe recipe, RecipeExtend recipeExt) {
        logger.info("EmrRecipeManager saveMedicalInfo recipe:{},recipeExt:{}", JSONUtils.toString(recipe), JSONUtils.toString(recipeExt));
        if (null != recipeExt.getDocIndexId()) {
            return;
        }
        try {
            String doctorName = doctorClient.getDoctor(recipe.getDoctor()).getName();
            docIndexClient.addMedicalInfo(recipe, recipeExt, doctorName);
            logger.info("EmrRecipeManager saveMedicalInfo end recipeExt={}", recipeExt.getDocIndexId());
        } catch (Exception e) {
            logger.error("EmrRecipeManager saveMedicalInfo 电子病历保存失败", e);
        }
    }

    /**
     * 写入电子病例 药品信息
     * 更新电子病例 为已经使用状态
     * 更新处方 诊断信息
     *
     * @param recipeId 处方id
     * @param docId    病历索引id
     */
    public void upDocIndex(Integer recipeId, Integer docId) {
        logger.info("EmrRecipeManager addDrugToDOC recipeId={} docId={}", recipeId, docId);
        if (null == docId || null == recipeId) {
            return;
        }
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipeId is null");
        }
        //写入电子病例 药品信息
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipeId);
        //替换下药品拼接名
        recipeDetailList.forEach(a -> a.setDrugName(DrugManager.dealWithRecipeDrugName(a, recipe.getRecipeType(), recipe.getClinicOrgan())));
        docIndexClient.saveRpDetail(recipe, recipeExtend, recipeDetailList, docId);
        //更新电子病例 为已经使用状态
        docIndexClient.updateEmrStatus(recipe, docId, recipe.getClinicId());
        updateDisease(recipeId);
    }

    /**
     * 更新诊断信息
     *
     * @param recipeId
     */
    public void updateDisease(Integer recipeId) {
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        if (null == recipeExtend) {
            return;
        }
        //更新 处方诊断信息
        EmrDetailDTO emrDetail = docIndexClient.getEmrDetails(recipeExtend.getDocIndexId());
        if (StringUtils.isEmpty(emrDetail.getOrganDiseaseName())) {
            return;
        }
        Recipe recipeUpdate = new Recipe();
        recipeUpdate.setRecipeId(recipeId);
        recipeUpdate.setOrganDiseaseName(emrDetail.getOrganDiseaseName());
        recipeUpdate.setOrganDiseaseId(emrDetail.getOrganDiseaseId());
        recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        logger.info("EmrRecipeManager updateDisease recipeId={},recipeUpdate={}", recipeId, JSON.toJSONString(recipeUpdate));
    }

    /**
     * 将药品信息移出病历
     *
     * @param recipeId 处方
     */
    public void deleteRecipeDetailsFromDoc(Integer recipeId) {
        docIndexClient.deleteRecipeDetailsFromDoc(recipeId);
    }

    /**
     * todo 新方法 docIndexClient.getEmrDetails
     * 查询电子病例，主要用于兼容老数据结构
     *
     * @param recipe       处方
     * @param recipeExtend 处方扩展
     */
    @Deprecated
    public static void getMedicalInfo(Recipe recipe, RecipeExtend recipeExtend) {
        if (null == recipeExtend) {
            logger.info("EmrRecipeManager getMedicalInfo RecipeId={}", recipe.getRecipeId());
            return;
        }
        List<EmrConfigRes> detail = getEmrDetailDTO(recipeExtend.getDocIndexId());
        if (CollectionUtils.isEmpty(detail)) {
            return;
        }
        for (EmrConfigRes detailDTO : detail) {
            if (null == detailDTO) {
                logger.warn("EmrRecipeManager getMedicalInfo detailDTO is null");
                continue;
            }
            String value = detailDTO.getValue();
            if (StringUtils.isEmpty(value)) {
                continue;
            }
            String type = detailDTO.getType();
            if (!RecipeEmrComment.TEXT_AREA.equals(type) && !RecipeEmrComment.MULTI_SEARCH.equals(type)) {
                logger.warn("EmrRecipeManager getMedicalInfo detail={}", JSONUtils.toString(detail));
                continue;
            }
            String key = detailDTO.getKey();
            if (RecipeEmrComment.COMPLAIN.equals(key) && StringUtils.isEmpty(recipeExtend.getMainDieaseDescribe())) {
                recipeExtend.setMainDieaseDescribe(value);
                continue;
            }
            if (RecipeEmrComment.CURRENT_MEDICAL_HISTORY.equals(key) && StringUtils.isEmpty(recipeExtend.getCurrentMedical())) {
                recipeExtend.setCurrentMedical(value);
                continue;
            }
            if (RecipeEmrComment.PAST_MEDICAL_HISTORY.equals(key) && StringUtils.isEmpty(recipeExtend.getHistroyMedical())) {
                recipeExtend.setHistroyMedical(value);
                continue;
            }
            if (RecipeEmrComment.MEDICAL_HISTORY.equals(key) && StringUtils.isEmpty(recipeExtend.getHistoryOfPresentIllness())) {
                recipeExtend.setHistoryOfPresentIllness(value);
                continue;
            }
            if (RecipeEmrComment.ALLERGY_HISTORY.equals(key) && StringUtils.isEmpty(recipeExtend.getAllergyMedical())) {
                recipeExtend.setAllergyMedical(value);
                continue;
            }
            if (RecipeEmrComment.PHYSICAL_EXAMINATION.equals(key) && StringUtils.isEmpty(recipeExtend.getPhysicalCheck())) {
                recipeExtend.setPhysicalCheck(value);
                continue;
            }
            if (RecipeEmrComment.PROCESSING_METHOD.equals(key) && StringUtils.isEmpty(recipeExtend.getHandleMethod())) {
                recipeExtend.setHandleMethod(value);
                continue;
            }
            if (RecipeEmrComment.REMARK.equals(key) && (StringUtils.isEmpty(recipe.getMemo()) || "无".equals(recipe.getMemo()))) {
                recipe.setMemo(value);
                continue;
            }
            try {
                /**诊断 ，中医症候特殊处理*/
                getMultiSearch(detailDTO, recipe, recipeExtend);
            } catch (Exception e) {
                logger.error("EmrRecipeManager getMultiSearch error detailDTO={}", JSON.toJSONString(detailDTO));
            }
        }
        logger.debug("EmrRecipeManager getMultiSearch recipe={}", JSONUtils.toString(recipe));
    }


    /**
     * 获取 电子病例明细对象
     *
     * @param docIndexId 电子病历id
     * @return
     */
    private static List<EmrConfigRes> getEmrDetailDTO(Integer docIndexId) {
        if (ValidateUtil.integerIsEmpty(docIndexId)) {
            return null;
        }
        IDocIndexService docIndexService = AppContextHolder.getBean("ecdr.docIndexService", IDocIndexService.class);
        MedicalInfoBean medicalInfoBean;
        try {
            medicalInfoBean = docIndexService.getMedicalInfoByDocIndexIdV2(docIndexId);
        } catch (Exception e) {
            logger.error("EmrRecipeManager getMedicalInfo getMedicalInfoByDocIndexId DocIndexId = {} msg = {}", docIndexId, e.getMessage(), e);
            return null;
        }

        if (null == medicalInfoBean) {
            return null;
        }

        MedicalDetailBean medicalDetailBean = medicalInfoBean.getMedicalDetailBean();
        if (null == medicalDetailBean) {
            return null;
        }
        List<EmrConfigRes> emrConfigRes = medicalDetailBean.getDetailList();
        if (CollectionUtils.isEmpty(emrConfigRes)) {
            return null;
        }
        return emrConfigRes;
    }

    /**
     * 查询时组织特殊字段
     *
     * @param detail
     * @param recipe
     * @param recipeExtend
     */
    private static void getMultiSearch(EmrConfigRes detail, Recipe recipe, RecipeExtend recipeExtend) {
        /**诊断 ，中医症候特殊处理*/
        if (!RecipeEmrComment.MULTI_SEARCH.equals(detail.getType())) {
            return;
        }
        List<EmrDetailValueDTO> values = JSON.parseArray(detail.getValue(), EmrDetailValueDTO.class);
        if (CollectionUtils.isEmpty(values)) {
            return;
        }
        StringBuilder names = new StringBuilder();
        StringBuilder ids = new StringBuilder();
        if (RecipeEmrComment.DIAGNOSIS.equals(detail.getKey())) {
            values.forEach(b -> {
                appendDecollator(names, b.getName(), ByteUtils.SEMI_COLON_EN);
                appendDecollator(ids, b.getCode(), ByteUtils.SEMI_COLON_EN);
            });
            if (StringUtils.isEmpty(recipe.getOrganDiseaseName()) && !ValidateUtil.isEmpty(names)) {
                recipe.setOrganDiseaseName(ByteUtils.subString(names));
            }
            if (StringUtils.isEmpty(recipe.getOrganDiseaseId()) && !ValidateUtil.isEmpty(ids)) {
                recipe.setOrganDiseaseId(ByteUtils.subString(ids));
            }
        } else if (RecipeEmrComment.TCM_SYNDROME.equals(detail.getKey())) {
            values.forEach(b -> {
                appendDecollator(names, b.getName(), ByteUtils.SEMI_COLON_EN);
                appendDecollator(ids, b.getCode(), ByteUtils.SEMI_COLON_EN);
            });
            if (StringUtils.isEmpty(recipeExtend.getSymptomName()) && !ValidateUtil.isEmpty(names)) {
                recipeExtend.setSymptomName(ByteUtils.subString(names));
            }
            if (StringUtils.isEmpty(recipeExtend.getSymptomId()) && !ValidateUtil.isEmpty(ids)) {
                recipeExtend.setSymptomId(ByteUtils.subString(ids));
            }
        } else {
            logger.warn("EmrRecipeManager getMultiSearch detail={}", JSONUtils.toString(detail));
        }
    }

    /**
     * 根据值 和拼接符 拼接字符串
     *
     * @param stringBuilder 保存对象
     * @param value         值
     * @param decollator    拼接符
     */
    private static void appendDecollator(StringBuilder stringBuilder, String value, String decollator) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        stringBuilder.append(value).append(decollator);
    }
}
