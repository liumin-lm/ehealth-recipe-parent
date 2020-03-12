package recipe.dao;

import com.ngari.recipe.entity.HisRecipeDetail;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * @author yinsheng
 * @date 2020\3\10 0010 20:26
 */
@RpcSupportDAO
public abstract class HisRecipeDetailDAO extends HibernateSupportDelegateDAO<HisRecipeDetail> {

    public HisRecipeDetailDAO() {
        super();
        this.setEntityName(HisRecipeDetail.class.getName());
        this.setKeyField("hisrecipedetailID");
    }

    @DAOMethod(sql = "from HisRecipeDetail where hisRecipeId=:hisRecipeId and status=1")
    public abstract List<HisRecipeDetail> findByHisRecipeId(@DAOParam("hisRecipeId") int hisRecipeId);
}
