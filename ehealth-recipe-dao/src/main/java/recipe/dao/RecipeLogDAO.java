package recipe.dao;

import com.ngari.recipe.entity.RecipeLog;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * 处方流程记录
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/4/29.
 */
@RpcSupportDAO
public abstract class RecipeLogDAO extends HibernateSupportDelegateDAO<RecipeLog> {

    private static final Log LOGGER = LogFactory.getLog(RecipeLogDAO.class);

    public RecipeLogDAO() {
        super();
        this.setEntityName(RecipeLog.class.getName());
        this.setKeyField("id");
    }

    public boolean saveRecipeLog(RecipeLog log) {
        save(log);
        return true;
    }

    /**
     * 根据处方id查询
     * @param recipeId
     * @return
     */
    @DAOMethod(orderBy = " id asc")
    public abstract List<RecipeLog> findByRecipeId(Integer recipeId);

}
