package recipe.dao;

import com.alibaba.druid.util.StringUtils;
import com.ngari.recipe.entity.ItemList;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.HqlUtils;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import recipe.dao.comment.ExtendDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 项目数据表
 *
 * @author yinsheng
 * @date 2021\8\20 0020 16:52
 */
@RpcSupportDAO
public abstract class ItemListDAO extends HibernateSupportDelegateDAO<ItemList> implements ExtendDao<ItemList> {

    public ItemListDAO() {
        super();
        this.setEntityName(ItemList.class.getName());
        this.setKeyField(SQL_KEY_ID);
    }

    @Override
    public boolean updateNonNullFieldByPrimaryKey(ItemList itemList) {
        return updateNonNullFieldByPrimaryKey(itemList, SQL_KEY_ID);
    }

    public List<ItemList> findItemList(final Integer organId, final Integer status, final String itemName, final int start, final int limit, Integer id, String itemCode) {
        HibernateStatelessResultAction<List<ItemList>> action =
                new AbstractHibernateStatelessResultAction<List<ItemList>>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void execute(StatelessSession ss) throws DAOException {
                        StringBuilder hql = new StringBuilder(" from ItemList where organ_id =:organId and is_deleted = 0 and status = 1  ");
                        if (null != status) {
                            hql.append(" and status = :status ");
                        }
                        if (null != id) {
                            hql.append(" and id like :id ");
                        }
                        if (!StringUtils.isEmpty(itemCode)) {
                            hql.append(" and itemCode like :itemCode ");
                        }
                        if (!StringUtils.isEmpty(itemName)) {
                            hql.append(" and item_name like :itemName ");
                        }
                        hql.append(" order by gmt_create desc ");

                        Query query = ss.createQuery(hql.toString());
                        if (!StringUtils.isEmpty(itemName)) {
                            query.setParameter("itemName", "%" + itemName + "%");
                        }
                        if (null != organId) {
                            query.setParameter("organId", organId);
                        }
                        if (null != status) {
                            query.setParameter("status", status);
                        }
                        if (null != id) {
                            query.setParameter("id", "%" + id + "%");
                        }
                        if (!StringUtils.isEmpty(itemCode)) {
                            query.setParameter("itemCode", "%" + itemCode + "%");
                        }
                        query.setFirstResult(start);
                        query.setMaxResults(limit);
                        List<ItemList> list = query.list();
                        setResult(list);
                    }
                };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public QueryResult<ItemList> pageItemList(final Integer organId, final Integer status, final String itemName, final int start, final int limit, Integer id, String itemCode) {
        StringBuilder hql = new StringBuilder(" from ItemList where organ_id =:organId and is_deleted = 0 and status = 1  ");
        Map<String, Object> paramMap = new HashMap<>();
        if (null != status) {
            hql.append(" and status = :status ");
            paramMap.put("status", status);
        }
        if (null != id) {
            hql.append(" and id like :id ");
            paramMap.put("id", "%" + id + "%");
        }
        if (!StringUtils.isEmpty(itemCode)) {
            hql.append(" and itemCode like :itemCode ");
            paramMap.put("itemCode", "%" + itemCode + "%");
        }
        if (!StringUtils.isEmpty(itemName)) {
            hql.append(" and item_name like :itemName ");
            paramMap.put("itemName", "%" + itemName + "%");
        }
        hql.append(" order by gmt_create desc ");
        return (QueryResult<ItemList>) HqlUtils.execHqlFindQueryResult(hql.toString(), paramMap, start, limit);
    }

    @DAOMethod(sql = "update ItemList set is_deleted = 1 where id =:id ")
    public abstract void delete(@DAOParam("id") Integer id);

    @DAOMethod(sql = "update ItemList set status =:status where id =:id and is_deleted = 0 ")
    public abstract void updateStatus(@DAOParam("id") Integer id, @DAOParam("status") Integer status);

    @DAOMethod(sql = "from ItemList where id =:id and is_deleted = 0 ")
    public abstract ItemList getById(@DAOParam("id") Integer id);
}
