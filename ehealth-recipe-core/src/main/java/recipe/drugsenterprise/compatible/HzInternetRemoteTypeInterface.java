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

    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise);

    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise);

    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise);

    public boolean scanStock(Recipe dbRecipe, DrugsEnterprise dep, List<Integer> drugIds);

    public String appEnterprise(RecipeOrder order);

    public BigDecimal orderToRecipeFee(RecipeOrder order, List<Integer> recipeIds, RecipePayModeSupportBean payModeSupport, BigDecimal recipeFee, Map<String, String> extInfo);

    public void setOrderEnterpriseMsg(Map<String, String> extInfo, RecipeOrder order);

    public void setEnterpriseMsgToOrder(RecipeOrder order, Integer depId, Map<String, String> extInfo);

    public Boolean specialMakeDepList(DrugsEnterprise drugsEnterprise, Recipe dbRecipe);

    public DrugEnterpriseResult sendMsgResultMap(Integer recipeId, Map<String, String> extInfo, DrugEnterpriseResult payResult);

}