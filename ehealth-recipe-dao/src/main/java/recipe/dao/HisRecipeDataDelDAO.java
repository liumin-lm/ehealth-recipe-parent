package recipe.dao;


import com.ngari.recipe.entity.HisRecipeDataDel;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import recipe.dao.comment.ExtendDao;

import java.util.List;

/**
 * @author zgy
 * @date 2021\11\10
 */
@RpcSupportDAO
public abstract class HisRecipeDataDelDAO extends HibernateSupportDelegateDAO<HisRecipeDataDel> implements ExtendDao<HisRecipeDataDel> {

    public HisRecipeDataDelDAO() {
        super();
        this.setEntityName(HisRecipeDataDel.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod
    public abstract List<HisRecipeDataDel> findByRecipeId(Integer recipeId);

    @DAOMethod
    public abstract List<HisRecipeDataDel> findByRecipeIdAndTableName(Integer recipeId, String tableName);
}
