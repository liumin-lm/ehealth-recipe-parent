package recipe.status.factory.orderstatusfactory.impl;

import com.alibaba.fastjson.JSON;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.platform.recipe.mode.RecipeDrugInventoryDTO;
import com.ngari.platform.recipe.mode.RecipeDrugInventoryInfoDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.status.factory.orderstatusfactory.IRecipeOrderStatusService;

import java.util.LinkedList;
import java.util.List;

/**
 * 状态流转基类
 *
 * @author fuzi
 */
public abstract class AbstractRecipeOrderStatus implements IRecipeOrderStatusService {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    protected RecipeOrderDAO recipeOrderDAO;
    @Autowired
    protected RecipeDAO recipeDAO;
    @Autowired
    protected RecipeExtendDAO recipeExtendDAO;
    @Autowired
    protected RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private IRecipeHisService recipeHisService;

    protected Recipe getRecipe(Integer recipeId) {
        return recipeDAO.getByRecipeId(recipeId);
    }

    /**
     * 组织加减库存接口参数
     *
     * @param recipeId
     * @return
     */
    protected RecipeDrugInventoryDTO recipeDrugInventory(Integer recipeId) {
        RecipeDrugInventoryDTO request = new RecipeDrugInventoryDTO();
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        request.setOrganId(recipe.getClinicOrgan());
        request.setRecipeId(recipe.getRecipeId());
        request.setRecipeType(recipe.getRecipeType());
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        if (null != recipeExtend) {
            request.setInvoiceNumber(recipeExtend.getEinvoiceNumber());
        }
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isEmpty(recipeDetailList)) {
            return null;
        }
        List<RecipeDrugInventoryInfoDTO> infoList = new LinkedList<>();
        recipeDetailList.forEach(a -> {
            RecipeDrugInventoryInfoDTO info = new RecipeDrugInventoryInfoDTO();
            info.setCreateDt(a.getCreateDt());
            info.setDrugBatch(a.getDrugBatch());
            info.setDrugCost(a.getDrugCost());
            info.setDrugId(a.getDrugId());
            info.setOrganDrugCode(a.getOrganDrugCode());
            info.setPharmacyId(a.getPharmacyId());
            info.setProducerCode(a.getProducerCode());
            info.setUseTotalDose(a.getUseTotalDose());
            infoList.add(info);
        });
        request.setInfo(infoList);
        return request;
    }

    protected void drugInventory(RecipeDrugInventoryDTO request) {
        logger.info("AbstractRecipeOrderStatus drugInventory request= {}", JSON.toJSONString(request));
        HisResponseTO<Boolean> hisResponse = recipeHisService.drugInventory(request);
        logger.info("AbstractRecipeOrderStatus drugInventory  hisResponse= {}", JSON.toJSONString(hisResponse));
    }

}
