package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.EmrDetailDTO;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.RecipeDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.*;
import recipe.util.DictionaryUtil;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
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
    public RecipeDTO getRecipeDTO(Integer recipeId) {
        logger.info("RecipeOrderManager getRecipeDTO recipeId:{}", recipeId);
        RecipeDTO recipeDTO = new RecipeDTO();
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        recipeDTO.setRecipe(recipe);
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
        recipeDTO.setRecipeDetails(recipeDetails);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        recipeDTO.setRecipeExtend(recipeExtend);
        if (StringUtils.isNotEmpty(recipeExtend.getCardNo())) {
            logger.info("RecipeOrderManager getRecipeDTO recipeDTO:{}", JSON.toJSONString(recipeDTO));
            return recipeDTO;
        }
        if (ValidateUtil.integerIsEmpty(recipe.getClinicId())) {
            return recipeDTO;
        }
        RevisitExDTO consultExDTO = revisitClient.getByClinicId(recipe.getClinicId());
        if (null != consultExDTO) {
            recipeExtend.setCardNo(consultExDTO.getCardId());
            recipeExtend.setCardType(consultExDTO.getCardType());
        }
        logger.info("RecipeOrderManager getRecipeDTO recipeDTO:{}", JSON.toJSONString(recipeDTO));
        return recipeDTO;
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
        PatientDTO patientBean = patientClient.getPatient(recipe.getMpiid());
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
        logger.info("RecipeManager getByRecipeCodeAndClinicOrgan param recipeCode:{},clinicOrgan:{}", recipeCode,clinicOrgan);
        Recipe recipe=recipeDAO.getByRecipeCodeAndClinicOrgan(recipeCode,clinicOrgan);
        logger.info("RecipeManager getByRecipeCodeAndClinicOrgan res recipe:{}", JSONUtils.toString(recipe));
        return recipe;
    }

    public Recipe getRecipeById(Integer recipeId){
        return recipeDAO.getByRecipeId(recipeId);
    }

    /**
     * 获取到院取药凭证
     * @param recipe  处方信息
     * @param recipeExtend 处方扩展信息
     * @return 取药凭证
     */
    public String getToHosProof(Recipe recipe, RecipeExtend recipeExtend){
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
                    qrName = offlineRecipeClient.queryRecipeSerialNumber(recipe.getClinicOrgan(),recipe.getPatientName(),recipe.getPatientID(),recipeExtend.getRegisterID());
                default:
                    break;
            }
        } catch (Exception e) {
            logger.error("RecipeManager getToHosProof error", e);
        }
        return qrName;
    }

}
