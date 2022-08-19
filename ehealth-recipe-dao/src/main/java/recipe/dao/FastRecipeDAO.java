package recipe.dao;

import com.alibaba.druid.util.StringUtils;
import com.ngari.recipe.dto.FastRecipeReq;
import com.ngari.recipe.entity.FastRecipe;
import com.ngari.recipe.entity.ItemList;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import eh.utils.ValidateUtil;
import org.hibernate.Query;
import org.hibernate.StatelessSession;

import java.util.List;
import java.util.Objects;

/**
 * @Description
 * @Author yzl
 * @Date 2022-08-16
 */
@RpcSupportDAO
public abstract class FastRecipeDAO extends HibernateSupportDelegateDAO<FastRecipe> {

    public FastRecipeDAO() {
        super();
        this.setEntityName(FastRecipe.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = "FROM FastRecipe WHERE clinicOrgan = :organId order by orderNum desc", limit = 0)
    public abstract List<FastRecipe> findFastRecipeListByOrganId(@DAOParam("organId") Integer organId);

    public List<FastRecipe> findFastRecipeListByParam(FastRecipeReq fastRecipeReq) {
        HibernateStatelessResultAction<List<FastRecipe>> action =
                new AbstractHibernateStatelessResultAction<List<FastRecipe>>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void execute(StatelessSession ss) throws DAOException {
                        StringBuilder hql = new StringBuilder("FROM FastRecipe where 1=1 ");
                        if (Objects.nonNull(fastRecipeReq.getOrganId())) {
                            hql.append("AND clinicOrgan = :organId ");
                        }
                        if (Objects.nonNull(fastRecipeReq.getFastRecipeId())) {
                            hql.append("AND id = :fastRecipeId ");
                        }
                        hql.append("order by orderNum  ");
                        Query query = ss.createQuery(hql.toString());

                        if (Objects.nonNull(fastRecipeReq.getOrganId())) {
                            query.setParameter("organId", fastRecipeReq.getOrganId());
                        }
                        if (Objects.nonNull(fastRecipeReq.getFastRecipeId())) {
                            query.setParameter("fastRecipeId", fastRecipeReq.getFastRecipeId());
                        }
                        if (Objects.nonNull(fastRecipeReq.getStart()) && Objects.nonNull(fastRecipeReq.getLimit())) {
                            query.setFirstResult(fastRecipeReq.getStart());
                            query.setMaxResults(fastRecipeReq.getLimit());
                        }
                        setResult(query.list());
                    }
                };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }
}
