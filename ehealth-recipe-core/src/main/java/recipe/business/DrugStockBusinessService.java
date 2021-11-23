package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.DoSignRecipeDTO;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.IOrganBusinessService;
import recipe.core.api.patient.IDrugEnterpriseBusinessService;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.manager.EnterpriseManager;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.ListValueUtil;
import recipe.util.MapValueUtil;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * todo 之后把 查询库存相关 写在 对应的 机构类和 药企类里
 *
 * @description： 药品库存业务 service
 * @author： whf
 * @date： 2021-07-19 15:41
 */
@Service
@Deprecated
public class DrugStockBusinessService extends BaseService {
    @Resource
    private RecipeDAO recipeDAO;
    @Resource
    private RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private EnterpriseManager enterpriseManager;
    @Autowired
    private IDrugEnterpriseBusinessService iDrugEnterpriseBusinessService;
    @Autowired
    private IOrganBusinessService organBusinessService;

    public Map<String, Object> enterpriseStock(Integer recipeId) {
        Recipe recipe = recipeDAO.get(recipeId);
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
        //医院库存
        EnterpriseStock organStock = organBusinessService.organStock(recipe, recipeDetails);
        //药企库存
        List<EnterpriseStock> enterpriseStock = iDrugEnterpriseBusinessService.enterpriseStockCheck(recipe, recipeDetails);

        DoSignRecipeDTO doSignRecipe = new DoSignRecipeDTO(true, false, null, "", recipeId, null);
        //未配置药企 / 医院
        if (null == organStock && CollectionUtils.isEmpty(enterpriseStock)) {
            enterpriseManager.doSignRecipe(doSignRecipe, null, "抱歉，机构未配置购药方式，无法开处方");
            return MapValueUtil.beanToMap(doSignRecipe);
        }
        //未配置药企 医院无库存
        if (CollectionUtils.isEmpty(enterpriseStock) && null != organStock && !organStock.getStock()) {
            enterpriseManager.doSignRecipe(doSignRecipe, organStock.getDrugName(), "药品门诊药房库存不足，请更换其他药品后再试");
        }
        //未配置医院 药企无库存
        if (CollectionUtils.isNotEmpty(enterpriseStock) && null == organStock) {
            boolean stock = enterpriseStock.stream().anyMatch(EnterpriseStock::getStock);
            if (!stock) {
                List<String> enterpriseDrugName = stockEnterprise(enterpriseStock);
                enterpriseManager.doSignRecipe(doSignRecipe, enterpriseDrugName, "药品库存不足，请更换其他药品后再试");
            }
        }
        //校验医院和药企
        if (CollectionUtils.isNotEmpty(enterpriseStock) && null != organStock) {
            boolean stockEnterprise = enterpriseStock.stream().anyMatch(EnterpriseStock::getStock);
            //医院有库存 药企无库存
            if (!stockEnterprise && organStock.getStock()) {
                List<String> enterpriseDrugName = stockEnterprise(enterpriseStock);
                doSignRecipe.setCanContinueFlag("2");
                enterpriseManager.doSignRecipe(doSignRecipe, enterpriseDrugName, "药品配送药企库存不足，该处方仅支持到院取药，无法药企配送，是否继续？");
            }
            //医院无库存 药企有库存
            if (stockEnterprise && !organStock.getStock()) {
                doSignRecipe.setCanContinueFlag("1");
                enterpriseManager.doSignRecipe(doSignRecipe, organStock.getDrugName(), "药品医院库存不足，该处方仅支持药企配送，无法到院取药，是否继续？");
            }
            //医院无库存 药企无库存
            if (!stockEnterprise && !organStock.getStock()) {
                enterpriseManager.doSignRecipe(doSignRecipe, organStock.getDrugName(), "药品库存不足，请更换其他药品后再试");
            }
        }
        //保存药品购药方式
        saveGiveMode(recipeId, organStock, enterpriseStock);
        return MapValueUtil.beanToMap(doSignRecipe);
    }


    private List<String> stockEnterprise(List<EnterpriseStock> enterpriseStock) {
        List<List<String>> groupList = new LinkedList<>();
        enterpriseStock.forEach(a -> groupList.add(a.getDrugName()));
        return ListValueUtil.minIntersection(groupList);
    }

    /**
     * * 异步保存处方购药方式
     *
     * @param recipeId
     * @param organStock
     * @param enterpriseStock
     */
    private void saveGiveMode(Integer recipeId, EnterpriseStock organStock, List<EnterpriseStock> enterpriseStock) {
        RecipeBusiThreadPool.execute(() -> {
            logger.info("DrugStockBusinessService saveGiveMode start recipeId={}", recipeId);
            List<GiveModeButtonDTO> giveModeButton = new LinkedList<>();
            if (null != organStock && organStock.getStock()) {
                giveModeButton.addAll(organStock.getGiveModeButton());
            }
            enterpriseStock.stream().filter(EnterpriseStock::getStock).forEach(a -> giveModeButton.addAll(a.getGiveModeButton()));
            if (CollectionUtils.isEmpty(giveModeButton)) {
                return;
            }
            Set<Integer> recipeGiveMode = giveModeButton.stream().map(GiveModeButtonDTO::getType).collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(recipeGiveMode)) {
                String join = StringUtils.join(recipeGiveMode, ",");
                Recipe recipe = new Recipe();
                recipe.setRecipeId(recipeId);
                recipe.setRecipeSupportGiveMode(join);
                recipeDAO.updateNonNullFieldByPrimaryKey(recipe);
            }
            logger.info("DrugStockBusinessService saveGiveMode 异步保存处方购药方式 {},{}", recipeId, JSON.toJSONString(recipeGiveMode));
        });
    }

}
