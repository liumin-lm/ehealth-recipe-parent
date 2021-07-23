package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.EmrDetail;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.RecipeDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import ctd.util.JSONUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import recipe.client.DocIndexClient;
import recipe.client.PatientClient;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;

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
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private DocIndexClient docIndexClient;

    /**
     * 通过订单号获取该订单下关联的所有处方
     *
     * @param orderCode 订单号
     * @return 处方集合
     */
    public List<Recipe> getRecipesByOrderCode(String orderCode) {
        logger.info("RecipeOrderManager getRecipesByOrderCode orderCode:{}", orderCode);
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(orderCode);
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIdList);
        logger.info("RecipeOrderManager getRecipesByOrderCode recipes:{}", JSON.toJSONString(recipes));
        return recipes;
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
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        recipeDTO.setRecipeExtend(recipeExtend);
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
        recipeDTO.setRecipeDetails(recipeDetails);
        logger.info("RecipeOrderManager getRecipeDTO recipeDTO:{}", JSON.toJSONString(recipeDTO));
        if (null == recipeExtend || !StringUtils.isEmpty(recipe.getOrganDiseaseName())) {
            return recipeDTO;
        }
        Integer docIndexId = recipeExtend.getDocIndexId();
        EmrDetail emrDetail = docIndexClient.getEmrDetails(docIndexId);
        if (null == emrDetail) {
            return recipeDTO;
        }
        recipe.setOrganDiseaseId(emrDetail.getOrganDiseaseId());
        recipe.setOrganDiseaseName(emrDetail.getOrganDiseaseName());
        recipeExtend.setSymptomId(emrDetail.getSymptomId());
        recipeExtend.setSymptomName(emrDetail.getSymptomName());
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

    /**
     * 通过recipeCode批量获取处方信息
     * @param recipeCodeList
     * @param clinicOrgan
     * @return
     */
    public List<Recipe> findByRecipeCodeAndClinicOrgan(List<String> recipeCodeList, Integer clinicOrgan) {
        logger.info("RecipeManager findByRecipeCodeAndClinicOrgan param recipeCodeList:{},clinicOrgan:{}", JSONUtils.toString(recipeCodeList),clinicOrgan);
        List<Recipe> recipes=recipeDAO.findByRecipeCodeAndClinicOrgan(recipeCodeList,clinicOrgan);
        logger.info("RecipeManager findByRecipeCodeAndClinicOrgan res recipes:{}", JSONUtils.toString(recipes));
        return recipes;
    }
}
