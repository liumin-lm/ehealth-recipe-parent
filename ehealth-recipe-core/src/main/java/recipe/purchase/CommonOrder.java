package recipe.purchase;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import recipe.ApplicationUtils;
import recipe.bean.RecipePayModeSupportBean;
import recipe.service.RecipeOrderService;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author yinsheng
 * @date 2019\9\10 0010 13:48
 */
public class CommonOrder {

    public static void createDefaultOrder(Map<String, String> extInfo, OrderCreateResult result, RecipeOrder order, RecipePayModeSupportBean payModeSupport, List<Recipe> recipeList, Integer calculateFee) {
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        Recipe recipe = recipeList.get(0);
        if (null == calculateFee || Integer.valueOf(1).equals(calculateFee)) {
            orderService.setOrderFee(result, order, Arrays.asList(recipe.getRecipeId()), recipeList, payModeSupport, extInfo, 1);
            if (StringUtils.isNotEmpty(extInfo.get("recipeFee"))) {
                order.setRecipeFee(MapValueUtil.getBigDecimal(extInfo, "recipeFee"));
                order.setActualPrice(Double.parseDouble(extInfo.get("recipeFee")));
            }
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
}
