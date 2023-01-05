package recipe.dao;

import com.ngari.recipe.vo.FastRecipeReq;
import com.ngari.recipe.entity.FastRecipe;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.collections.CollectionUtils;
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
                        if (CollectionUtils.isNotEmpty(fastRecipeReq.getStatusList())) {
                            hql.append("AND status IN (:statusList) ");
                        }
                        hql.append("order by orderNum , id desc");
                        Query query = ss.createQuery(hql.toString());

                        if (Objects.nonNull(fastRecipeReq.getOrganId())) {
                            query.setParameter("organId", fastRecipeReq.getOrganId());
                        }
                        if (Objects.nonNull(fastRecipeReq.getFastRecipeId())) {
                            query.setParameter("fastRecipeId", fastRecipeReq.getFastRecipeId());
                        }
                        if (CollectionUtils.isNotEmpty(fastRecipeReq.getStatusList())) {
                            query.setParameterList("statusList", fastRecipeReq.getStatusList());
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

    @DAOMethod(sql = "update FastRecipe set stockNum = (stockNum - :num) where id = :mouldId and stockNum >= :num")
    public abstract void updateStockByMouldId(@DAOParam("organId") Integer organId,
                                              @DAOParam("num") Integer num);

    @DAOMethod(sql = "update FastRecipe set saleNum = (saleNum + :num) where id = :mouldId")
    public abstract void addSaleNumByMouldId(@DAOParam("organId") Integer organId,
                                                         @DAOParam("num") Integer num);
}
