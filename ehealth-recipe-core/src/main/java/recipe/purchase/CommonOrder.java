package recipe.purchase;

import com.google.common.collect.ImmutableMap;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.GiveModeShowButtonVO;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.persistence.DAOFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.RecipePayModeSupportBean;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.dao.RecipeDAO;
import recipe.givemode.business.GiveModeFactory;
import recipe.givemode.business.IGiveModeBase;
import recipe.service.RecipeOrderService;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yinsheng
 * @date 2019\9\10 0010 13:48
 */
public class CommonOrder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonOrder.class);

    public static void createDefaultOrder(Map<String, String> extInfo, OrderCreateResult result, RecipeOrder order, RecipePayModeSupportBean payModeSupport, List<Recipe> recipeList, Integer calculateFee) {
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        List<Integer> recipeIds = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        //设置确认订单页购药方式的key
        String giveModeKey = MapValueUtil.getString(extInfo, "giveModeKey");
        order.setGiveModeKey(giveModeKey);
        order.setGiveModeText(getGiveModeText(recipeList.get(0).getClinicOrgan(), giveModeKey));
        if (null == calculateFee || Integer.valueOf(1).equals(calculateFee)) {
            orderService.setOrderFee(result, order, recipeIds, recipeList, payModeSupport, extInfo, 1);
        } else {
            //设置默认值
            order.setExpressFee(BigDecimal.ZERO);
            order.setTotalFee(BigDecimal.ZERO);
            order.setRecipeFee(BigDecimal.ZERO);
            order.setCouponFee(BigDecimal.ZERO);
            order.setRegisterFee(BigDecimal.ZERO);
            order.setActualPrice(BigDecimal.ZERO.doubleValue());
        }
    }

    //订单完成更新pdf中的取药标签
    public static void finishGetDrugUpdatePdf(Integer recipeId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(recipeId);
        //更新pdf
        if(null == recipe || null == recipe.getChemistSignFile()){
            return;
        }
        try {
            String newPfd = CreateRecipePdfUtil.transPdfIdForRecipePdf(recipe.getChemistSignFile());
            if (StringUtils.isNotEmpty(newPfd)){
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of("ChemistSignFile",newPfd));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static String getGiveModeText(Integer organId, String key){
        try {
            IGiveModeBase giveModeBase = GiveModeFactory.getGiveModeBaseByRecipe(new Recipe());
            GiveModeShowButtonVO giveModeShowButtonVO = giveModeBase.getGiveModeSettingFromYypt(organId);
            Map configurations = giveModeShowButtonVO.getGiveModeButtons().stream().collect(Collectors.toMap(GiveModeButtonBean::getShowButtonKey, GiveModeButtonBean::getShowButtonName));
            return (String)configurations.get(key);
        } catch (Exception e) {
            LOGGER.error("getGiveModeText organId:{}, key:{}.", organId, key);
        }
        return "";
    }
}
