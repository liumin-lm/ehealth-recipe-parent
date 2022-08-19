package recipe.dao;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeBeforeOrder;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.log4j.Logger;
import recipe.dao.comment.ExtendDao;

import java.util.List;

/**
 * 预下单DAO
 * @author zgy
 */
@RpcSupportDAO
public abstract class RecipeBeforeOrderDAO extends
        HibernateSupportDelegateDAO<RecipeBeforeOrder> implements ExtendDao<RecipeBeforeOrder>{

    public static final Logger LOGGER = Logger.getLogger(RecipeBeforeOrderDAO.class);

    public RecipeBeforeOrderDAO() {
        super();
        this.setEntityName(RecipeBeforeOrder.class.getName());
        this.setKeyField("id");
    }

    @Override
    public boolean updateNonNullFieldByPrimaryKey(RecipeBeforeOrder recipeBeforeOrder) {
        return updateNonNullFieldByPrimaryKey(recipeBeforeOrder, "id");
    }

    /**
     * 根据机构id及his处方编码查询预下单信息
     * @param organId
     * @param recipeCode
     * @return
     */
    @DAOMethod(sql = "from RecipeBeforeOrder where organId=:organId and recipeCode=:recipeCode ")
    public abstract RecipeBeforeOrder getByOrganIdAndRecipeCode(@DAOParam("organId") Integer organId, @DAOParam("recipeCode") String  recipeCode);

    /**
     * 根据操作人mpiId查询预下单信息
     * @param mpiId
     * @return
     */
    @DAOMethod(sql = "from RecipeBeforeOrder where operMpiId=:mpiId ")
    public abstract List<RecipeBeforeOrder> findByMpiId(@DAOParam("mpiId") String mpiId);

    /**
     * 根据机构id与 his单号获取预下单列表
     * @param organId
     * @param recipeCodes
     * @return
     */
    @DAOMethod(sql = "from RecipeBeforeOrder where recipeCode in (:recipeCodes) and organId=:organId")
    public abstract List<RecipeBeforeOrder> getByOrganIdAndRecipeCodes(@DAOParam("organId") Integer organId, @DAOParam("recipeCodes") List<String> recipeCodes);
}