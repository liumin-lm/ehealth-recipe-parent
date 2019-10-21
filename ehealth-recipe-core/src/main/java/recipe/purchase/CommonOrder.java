package recipe.purchase;

import com.google.common.collect.ImmutableMap;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import org.apache.commons.lang3.StringUtils;
import recipe.ApplicationUtils;
import recipe.bean.RecipePayModeSupportBean;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.constant.ErrorCode;
import recipe.dao.RecipeDAO;
import recipe.service.RecipeOrderService;

import java.math.BigDecimal;
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
}
