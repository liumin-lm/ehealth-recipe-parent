package recipe.dao;

import com.ngari.recipe.entity.EnterpriseAddress;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 药企配送地址DAO
 * @author zhongzx/0004
 * @date 2016/6/8
 */
@RpcSupportDAO
public abstract class EnterpriseAddressDAO extends HibernateSupportDelegateDAO<EnterpriseAddress> {

    private static final Log LOGGER = LogFactory.getLog(EnterpriseAddressDAO.class);


    /**
     * 根据id和地址获取药企配送地址
     *
     * @param enterpriseId
     * @param address
     * @return
     */
    @DAOMethod(sql = "select count(*) from EnterpriseAddress where enterpriseId=:enterpriseId and address=:address")
    public abstract Long getCountByEnterpriseIdAndAddress(@DAOParam("enterpriseId") Integer enterpriseId, @DAOParam("address") String address);

    /**
     * 添加药企配送地址
     *
     * @param enterpriseAddress
     * @return
     */
    public EnterpriseAddress addEnterpriseAddress(EnterpriseAddress enterpriseAddress) {
        if (null == enterpriseAddress) {
            throw new DAOException(DAOException.VALUE_NEEDED, "EnterpriseAddress Object is null");
        }

        if (ObjectUtils.isEmpty(enterpriseAddress.getEnterpriseId())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "EnterpriseId is null");
        }

        if (ObjectUtils.isEmpty(enterpriseAddress.getAddress())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "Address is null");
        }
        Long size = getCountByEnterpriseIdAndAddress(enterpriseAddress.getEnterpriseId(), enterpriseAddress.getAddress());
        if (null != size && size > 0) {
            throw new DAOException(DAOException.VALUE_NEEDED, "Enterprise Address exist");
        }
        enterpriseAddress.setCreateTime(new Date());
        enterpriseAddress.setLastModify(new Date());
        return this.save(enterpriseAddress);
    }

    /**
     * 更新药企配送地址
     *
     * @param addressList
     * @return
     */
    public List<EnterpriseAddress> updateListEnterpriseAddress(final List<EnterpriseAddress> addressList) {
        if (null == addressList) {
            throw new DAOException(DAOException.VALUE_NEEDED, "addressList is null");
        }
        List<EnterpriseAddress> newList = new ArrayList<>();
        for (EnterpriseAddress enterpriseAddress : addressList) {
            EnterpriseAddress target = get(enterpriseAddress.getId());
            target.setEnterpriseId(enterpriseAddress.getEnterpriseId());
            target.setStatus(enterpriseAddress.getStatus());
            target.setAddress(enterpriseAddress.getAddress());
            target.setLastModify(new Date());
            target = this.update(target);
            newList.add(target);
        }
        return newList;
    }

    /**
     * 删除药企配送地址
     *
     * @param enterpriseId
     */
    public void deleteEnterpriseAddress(final Integer enterpriseId) {
        if (null == enterpriseId) {
            throw new DAOException(DAOException.VALUE_NEEDED, "enterpriseId is null");
        }
        this.remove(enterpriseId);
    }

    @DAOMethod(sql="delete from EnterpriseAddress  where enterpriseId=:enterpriseId")
    public abstract void deleteByEnterpriseId(@DAOParam("enterpriseId") Integer enterpriseId);
    /**
     * 删除药企配送区域地址
     *
     * @param ids
     */
    public void deleteEnterpriseAddressById(final List<Integer> ids) {
        if (ids.size() == 0) {
            throw new DAOException(DAOException.VALUE_NEEDED, " id is null");
        }
        for (Integer id : ids) {
            this.remove(id);
        }
    }

    /**
     * 根据药企Id  + 使用状态 查询能够配送的地址
     *
     * @param enterpriseId
     * @param status
     * @return
     */
    @DAOMethod(sql = "From EnterpriseAddress where enterpriseId=:enterpriseId and Status=:status", limit = 0)
    public abstract List<EnterpriseAddress> findByEnterpriseIdAndStatus(@DAOParam("enterpriseId") Integer enterpriseId, @DAOParam("status") Integer status);

    /**
     * 根据药企Id 查询能够配送的地址
     * zhongzx
     *
     * @param enterpriseId 药企Id
     * @return
     */
    public List<EnterpriseAddress> findByEnterPriseId(final Integer enterpriseId) {
        LOGGER.info("findByEnterPriseId: 药企ID[" + enterpriseId + "]");
        if (null == enterpriseId) {
            throw new DAOException(DAOException.VALUE_NEEDED, "enterpriseId is needed");
        }
        HibernateStatelessResultAction<List<EnterpriseAddress>> action = new AbstractHibernateStatelessResultAction<List<EnterpriseAddress>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from EnterpriseAddress where enterpriseId =:enterpriseId and status = 1");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("enterpriseId", enterpriseId);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 查询药企配送地址
     *
     * @param enterpriseId
     * @param status
     * @param start
     * @param limit
     * @return
     */
    public QueryResult<EnterpriseAddress> queryEnterpriseAddressByLimitAndStart(final Integer enterpriseId,
                                                                                final Integer status,
                                                                                final int start, final int limit) {
        LOGGER.info("查询药企配送地址queryEnterpriseAddressByLimitAndStart:[enterpriseId=" + enterpriseId + ";status="
                + status + ";start=" + start + ";limit=" + limit);
        HibernateStatelessResultAction<QueryResult<EnterpriseAddress>> action = new AbstractHibernateStatelessResultAction<QueryResult<EnterpriseAddress>>() {
            @SuppressWarnings("unchecked")
            public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder("FROM EnterpriseAddress e WHERE 1=1 ");
                if (enterpriseId != null) {
                    hql.append(" AND e.enterpriseId=:enterpriseId");
                }
                if (status != null) {
                    hql.append(" AND e.status=:status");
                }

                Query query = ss.createQuery("SELECT count(*) " + hql.toString());
                if (enterpriseId != null) {
                    query.setParameter("enterpriseId", enterpriseId);
                }
                if (status != null) {
                    query.setParameter("status", status);
                }
                Long total = (Long) query.uniqueResult();

                query = ss.createQuery("SELECT e " + hql.toString());
                if (enterpriseId != null) {
                    query.setParameter("enterpriseId", enterpriseId);
                }
                if (status != null) {
                    query.setParameter("status", status);
                }
                query.setFirstResult(start);
                query.setMaxResults(limit);
                setResult(new QueryResult<EnterpriseAddress>(total, query.getFirstResult(), query.getMaxResults(), query.list()));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }
}
