package recipe.dao;

import com.ngari.recipe.entity.RecipeOrderBill;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

/**
 * @author Created by liuxiaofeng on 2020/10/26.
 */
@RpcSupportDAO
public abstract class RecipeOrderBillDAO extends HibernateSupportDelegateDAO<RecipeOrderBill> {

    public RecipeOrderBillDAO() {
        super();
        this.setEntityName(RecipeOrderBill.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = "from RecipeOrderBill where recipe_order_code =:recipeOrderCode")
    public abstract RecipeOrderBill getRecipeOrderBillByOrderCode(@DAOParam("recipeOrderCode") String recipeOrderCode);
}
