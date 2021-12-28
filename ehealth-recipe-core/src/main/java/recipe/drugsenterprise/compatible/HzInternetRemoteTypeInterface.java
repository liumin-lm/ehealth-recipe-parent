package recipe.drugsenterprise.compatible;

import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.RecipePayModeSupportBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface HzInternetRemoteTypeInterface {

    DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise);

    DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise);

    DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise);

    boolean scanStock(Recipe dbRecipe, DrugsEnterprise dep, List<Integer> drugIds);

    String appEnterprise(RecipeOrder order);

    BigDecimal orderToRecipeFee(RecipeOrder order, List<Integer> recipeIds, RecipePayModeSupportBean payModeSupport, BigDecimal recipeFee, Map<String, String> extInfo);

    void setOrderEnterpriseMsg(Map<String, String> extInfo, RecipeOrder order);

    void setEnterpriseMsgToOrder(RecipeOrder order, Integer depId, Map<String, String> extInfo);

    Boolean specialMakeDepList(DrugsEnterprise drugsEnterprise, Recipe dbRecipe);

    DrugEnterpriseResult sendMsgResultMap(Integer recipeId, Map<String, String> extInfo, DrugEnterpriseResult payResult);

}