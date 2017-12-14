package recipe.dao;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeCheck;
import com.ngari.recipe.entity.RecipeCheckDetail;
import recipe.constant.ErrorCode;
import ctd.persistence.DAOFactory;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;

import org.hibernate.Query;
import org.hibernate.StatelessSession;

import java.util.List;
import java.util.Set;

/**
 * Created by zhongzx on 2016/10/25 0025.
 * 审方dao
 */
@RpcSupportDAO
public abstract class RecipeCheckDAO extends HibernateSupportDelegateDAO<RecipeCheck> {

    public RecipeCheckDAO() {
        super();
        this.setEntityName(RecipeCheck.class.getName());
        this.setKeyField("checkId");
    }

    /**
     * 保存审方记录和详情记录
     * zhongzx
     *
     * @param recipeCheck
     * @param recipeCheckDetails
     */
    public void saveRecipeCheckAndDetail(RecipeCheck recipeCheck, List<RecipeCheckDetail> recipeCheckDetails) {
        recipeCheck = save(recipeCheck);
        RecipeCheckDetailDAO
                recipeCheckDetailDAO = DAOFactory.getDAO(RecipeCheckDetailDAO.class);
        if (null != recipeCheckDetails) {
            for (RecipeCheckDetail recipeCheckDetail : recipeCheckDetails) {
                recipeCheckDetail.setCheckId(recipeCheck.getCheckId());
                recipeCheckDetailDAO.save(recipeCheckDetail);
            }
        }
    }

    /**
     * 根据处方Id查询不通过审核记录
     *
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "from RecipeCheck where recipeId=:recipeId and checkStatus=0")
    public abstract RecipeCheck getByRecipeIdAndCheckStatus(@DAOParam("recipeId") Integer recipeId);

    /**
     * 药师搜索方法 开方医生 审方医生 患者姓名 患者patientId
     *
     * @param organs
     * @param searchString
     * @param searchFlag   0-开方医生 1-审方医生 2-患者姓名 3-病历号
     * @param start
     * @param limit
     * @return
     * @author zhongzx
     */
    public List<Recipe> searchRecipe(final Set<Integer> organs,
                                     final Integer searchFlag, final String searchString,
                                     final Integer start, final Integer limit) {
        HibernateStatelessResultAction<List<Recipe>> action =
            new AbstractHibernateStatelessResultAction<List<Recipe>>() {
                @Override
                public void execute(StatelessSession ss)
                    throws Exception {
                    StringBuilder hql =
                        new StringBuilder("select distinct r from Recipe r");
                    if (0 == searchFlag || 1 == searchFlag) {
                        hql.append(" where r.doctorName like:searchString ");
                    }
                    else if (2 == searchFlag) {
                        hql.append(" where r.patientName like:searchString ");
                    }
                    else if (3 == searchFlag) {
                        hql.append(" where r.patientID like:searchString ");
                    }
                    else {
                        throw new DAOException(ErrorCode.SERVICE_ERROR,
                            "searchFlag is invalid");
                    }
                    hql.append("and (r.checkDateYs is not null or r.status = 8) " +
                        "and r.clinicOrgan in (:organs) order by r.signDate desc");
                    Query q = ss.createQuery(hql.toString());

                    q.setParameter("searchString", "%" + searchString + "%");
                    q.setParameterList("organs", organs);
                    if (null != start && null != limit) {
                        q.setFirstResult(start);
                        q.setMaxResults(limit);
                    }
                    setResult(q.list());
                }
            };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    @DAOMethod(sql = "from RecipeCheck where recipeId=:recipeId order by checkDate desc")
    public abstract List<RecipeCheck> findByRecipeId(@DAOParam("recipeId") Integer recipeId);
}
