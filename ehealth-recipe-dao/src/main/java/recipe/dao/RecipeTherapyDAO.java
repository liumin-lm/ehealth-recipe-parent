package recipe.dao;

import com.ngari.recipe.entity.RecipeTherapy;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import recipe.dao.comment.ExtendDao;

/**
 * 诊疗处方数据表
 *
 * @author fuzi
 */
@RpcSupportDAO
public abstract class RecipeTherapyDAO extends HibernateSupportDelegateDAO<RecipeTherapy> implements ExtendDao<RecipeTherapy> {

    public RecipeTherapyDAO() {
        super();
        this.setEntityName(RecipeTherapy.class.getName());
        this.setKeyField(SQL_KEY_ID);
    }


    @Override
    public boolean updateNonNullFieldByPrimaryKey(RecipeTherapy recipeTherapy) {
        return updateNonNullFieldByPrimaryKey(recipeTherapy, SQL_KEY_ID);
    }

    /**
     * 根据id查询诊疗处方数据
     *
     * @param id 诊疗id
     * @return
     */
    @DAOMethod
    public abstract RecipeTherapy getById(int id);

    /**
     * 根据处方id查询诊疗处方数据
     *
     * @param recipeId 处方id
     * @return
     */
    @DAOMethod
    public abstract RecipeTherapy getByRecipeId(Integer recipeId);
}