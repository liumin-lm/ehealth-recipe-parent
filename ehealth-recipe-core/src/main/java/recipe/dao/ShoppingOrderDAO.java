package recipe.dao;

import com.ngari.recipe.entity.ShoppingOrder;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.hibernate.Query;
import org.hibernate.StatelessSession;

import java.util.List;
import java.util.Map;

/**
 * @author liuya
 */
@RpcSupportDAO
public abstract class ShoppingOrderDAO extends HibernateSupportDelegateDAO<ShoppingOrder> {
    public ShoppingOrderDAO() {
        super();
        this.setEntityName(ShoppingOrder.class.getName());
        this.setKeyField("orderId");
    }

    /**
     * 通过mpiId及订单编号获取
     * @param mpiId
     * @param orderCode
     * @return
     */
    @DAOMethod(sql = "from ShoppingOrder where mpiId=:mpiId and orderCode=:orderCode")
    public abstract ShoppingOrder getByMpiIdAndOrderCode(@DAOParam("mpiId") String mpiId, @DAOParam("orderCode") String orderCode);

    /**
     * 根据查询条件查找订单列表
     * @param changeAttr
     * @param start
     * @param limit
     * @return
     */
    public List<ShoppingOrder> findShoppingOrdersWithConditions(final Map<String, ?> changeAttr, final int start, final int limit){
        HibernateStatelessResultAction<List<ShoppingOrder>> action = new AbstractHibernateStatelessResultAction<List<ShoppingOrder>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from ShoppingOrder where 1=1 ");
                if (null != changeAttr && !changeAttr.isEmpty()) {
                    for (String key : changeAttr.keySet()) {
                        hql.append(" and " + key + "=:" + key);
                    }
                }
                hql.append(" order by lastModify desc");
                Query q = ss.createQuery(hql.toString());
                if (null != changeAttr && !changeAttr.isEmpty()) {
                    for (String key : changeAttr.keySet()) {
                        q.setParameter(key, changeAttr.get(key));
                    }
                }
                q.setFirstResult(start);
                q.setMaxResults(limit);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();

    }

}
