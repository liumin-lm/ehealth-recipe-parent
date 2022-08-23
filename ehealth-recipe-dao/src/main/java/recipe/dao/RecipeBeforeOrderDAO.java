package recipe.dao;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeBeforeOrder;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import recipe.dao.comment.ExtendDao;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    @DAOMethod(sql = "from RecipeBeforeOrder where operMpiId=:mpiId and deleteFlag = 0")
    public abstract List<RecipeBeforeOrder> findByMpiId(@DAOParam("mpiId") String mpiId);

    /**
     * 根据机构id与 his单号获取预下单列表
     * @param organId
     * @param recipeCodes
     * @return
     */
    public List<RecipeBeforeOrder> getByOrganIdAndRecipeCodes(@DAOParam("organId") Integer organId, @DAOParam("recipeCodes") List<String> recipeCodes){
        HibernateStatelessResultAction<List<RecipeBeforeOrder>> action = new AbstractHibernateStatelessResultAction<List<RecipeBeforeOrder>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String sql = "from RecipeBeforeOrder where organId=:organId and recipeCode in (:recipeCodes) and deleteFlag = 0" ;
                Query q = ss.createQuery(sql);
                q.setParameter("organId", organId);
                q.setParameterList("recipeCodes", recipeCodes);
                List list = q.list();
                setResult(list);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    };

    /**
     * 根据处方单号查询有效的预下单信息
     * @param mpiId
     * @return
     */
    @DAOMethod(sql = "from RecipeBeforeOrder where recipeId=:recipeId and deleteFlag = 0")
    public abstract RecipeBeforeOrder getRecipeBeforeOrderByRecipeId(@DAOParam("recipeId") Integer recipeId);

    /**
     * 修改删除标识
     * @param idList
     */
    @DAOMethod(sql = "update RecipeBeforeOrder set deleteFlag=1 where id in (:idList)")
    public abstract void updateDeleteFlag(@DAOParam("idList")List<Integer> idList);

    /**
     * 根据处方id修改删除标识
     * @param recipeIds
     */
    @DAOMethod(sql = "update RecipeBeforeOrder set deleteFlag=1 where recipeId in (:recipeIds)")
    public abstract void updateDeleteFlagByRecipeId(@DAOParam("recipeIds")List<Integer> recipeIds) ;

    /**
     * 根据处方单号批量查询有效的预下单信息
     * @param mpiId
     * @return
     */
    @DAOMethod(sql = "from RecipeBeforeOrder where recipeCode in (:recipeCodes) and organId in (:organIds) and deleteFlag = 0")
    public abstract List<RecipeBeforeOrder> findByRecipeCodesAndOrganIds(@DAOParam("recipeCodes") List<String> recipeCode,@DAOParam("organIds") Set<Integer> organIds);

    @DAOMethod(sql = "from RecipeBeforeOrder where recipeId=:recipeId operMpiId=:mpiId and deleteFlag = 0")
    public abstract RecipeBeforeOrder getRecipeBeforeOrderByRecipeIdAndMpiId(@DAOParam("recipeId") Integer recipeId, @DAOParam("mpiId") String mpiId);
}