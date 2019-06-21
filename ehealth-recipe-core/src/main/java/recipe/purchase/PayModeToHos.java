package recipe.purchase;

import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.persistence.DAOFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import recipe.constant.RecipeBussConstant;
import recipe.dao.RecipeDetailDAO;

import java.util.List;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/20
 * @description： 到院取药方式实现
 * @version： 1.0
 */
public class PayModeToHos implements IPurchaseService{

    @Override
    public RecipeResultBean findSupportDepList(Recipe dbRecipe, Map<String, String> extInfo) {
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);

        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        Integer recipeId = dbRecipe.getRecipeId();
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        StringBuilder sb = new StringBuilder();

        if(CollectionUtils.isNotEmpty(detailList)){
            String pharmNo = detailList.get(0).getPharmNo();
            if(StringUtils.isNotEmpty(pharmNo)){
                sb.append("到院自取需去医院取药窗口取药："+pharmNo);
            }else {
                sb.append("选择到院自取后，需去医院取药窗口取药");
            }
        }

        resultBean.setMsg(sb.toString());
        return resultBean;
    }

    @Override
    public OrderCreateResult order(Recipe dbRecipe, Map<String, String> extInfo) {
       return null;
    }

    @Override
    public Integer getPayMode() {
        return RecipeBussConstant.PAYMODE_TO_HOS;
    }

    @Override
    public String getServiceName() {
        return "payModeToHosService";
    }
}
