package recipe.drugsenterprise.commonExtendCompatible;

import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.dto.DrugStockAmountDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipeBean;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.RecipePayModeSupportBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

//将默认的手动创建的药企抽象出来，通用不同种的药企流程（类似于：his管理的药企，这种流程上区别于标准流程的药企行为）
//这种药企都是不需要【callSys】区分的，将流程抽离于主流程放置在药企中的
public interface CommonExtendEnterprisesInterface {

    String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId);

    List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag);

    DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise);

    DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise);

    DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise);

    @Deprecated
    boolean scanStock(Recipe dbRecipe, DrugsEnterprise dep, List<Integer> drugIds);

    DrugStockAmountDTO scanEnterpriseDrugStock(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails);

    String appEnterprise(RecipeOrder order);

    BigDecimal orderToRecipeFee(RecipeOrder order, List<Integer> recipeIds, RecipePayModeSupportBean payModeSupport, BigDecimal recipeFee, Map<String, String> extInfo);

    void setOrderEnterpriseMsg(Map<String, String> extInfo, RecipeOrder order);

    void checkRecipeGiveDeliveryMsg(RecipeBean recipeBean, Map<String, Object> map);

    void setEnterpriseMsgToOrder(RecipeOrder order, Integer depId, Map<String, String> extInfo);

    Boolean specialMakeDepList(DrugsEnterprise drugsEnterprise, Recipe dbRecipe);

    DrugEnterpriseResult sendMsgResultMap(Integer recipeId, Map<String, String> extInfo, DrugEnterpriseResult payResult);

}