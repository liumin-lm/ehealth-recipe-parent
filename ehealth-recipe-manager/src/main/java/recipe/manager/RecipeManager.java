package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.RecipeDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import ctd.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.PatientClient;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;

import java.util.List;

/**
 * 处方
 * @author yinsheng
 * @date 2021\6\30 0030 14:21
 */
@Service
public class RecipeManager {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
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
        Recipe recipes = recipeDAO.getByRecipeId(recipeId);
        recipeDTO.setRecipe(recipes);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        recipeDTO.setRecipeExtend(recipeExtend);
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
        recipeDTO.setRecipeDetails(recipeDetails);
        return recipeDTO;
    }

    /**
     * 获取处方相关信息
     *
     * @param recipeId 处方id
     * @return
     */
    public RecipeInfoDTO getRecipeInfoDTO(Integer recipeId) {
        RecipeInfoDTO recipePdfDTO = (RecipeInfoDTO) getRecipeDTO(recipeId);
        Recipe recipe = recipePdfDTO.getRecipe();
        PatientDTO patientBean = patientClient.getPatient(recipe.getMpiid());
        recipePdfDTO.setPatientBean(patientBean);
        return recipePdfDTO;
    }
}
