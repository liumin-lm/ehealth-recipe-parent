package recipe.dao;

import com.ngari.recipe.drugsenterprise.model.DrugEnterpriseLogisticsBean;
import com.ngari.recipe.entity.EnterpriseAddress;
import com.ngari.recipe.entity.EnterpriseDecoctionAddress;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.BeanUtils;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;

import java.util.Date;
import java.util.List;

/**
 * @description： 药企煎法配送dao
 * @author： whf
 * @date： 2022-04-07 10:46
 */
@RpcSupportDAO
public abstract class EnterpriseDecoctionAddressDAO extends HibernateSupportDelegateDAO<EnterpriseDecoctionAddress> {
    private static final Log LOGGER = LogFactory.getLog(EnterpriseDecoctionAddressDAO.class);

    /**
     * 删除
     *
     * @param organId
     * @param enterpriseId
     * @param decoctionId
     */
    @DAOMethod(sql = "delete from EnterpriseDecoctionAddress  where enterpriseId=:enterpriseId and decoctionId=:decoctionId and organId=:organId")
    public abstract void deleteEnterpriseDecoctionAddress(@DAOParam("organId") Integer organId, @DAOParam("enterpriseId") Integer enterpriseId, @DAOParam("decoctionId") Integer decoctionId);

    /**
     * 批量写入
     * @param enterpriseDecoctionAddresses
     * @return
     */
    public Boolean addEnterpriseDecoctionAddressList(List<EnterpriseDecoctionAddress> enterpriseDecoctionAddresses) {
        if (CollectionUtils.isEmpty(enterpriseDecoctionAddresses)) {
            return true;
        }

        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                Transaction transaction = ss.beginTransaction();
                enterpriseDecoctionAddresses.forEach(enterpriseDecoctionAddress -> {

                    enterpriseDecoctionAddress.setCreateTime(new Date());
                    enterpriseDecoctionAddress.setModifyTime(new Date());
                    ss.insert(enterpriseDecoctionAddress);

                });
                transaction.commit();
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();

    }

    @DAOMethod(sql = "from EnterpriseDecoctionAddress  where enterpriseId=:enterpriseId and decoctionId=:decoctionId and organId=:organId", limit = 0)
    public abstract List<EnterpriseDecoctionAddress> findEnterpriseDecoctionAddressList(@DAOParam("organId") Integer organId, @DAOParam("enterpriseId") Integer enterpriseId, @DAOParam("decoctionId") Integer decoctionId);


    @DAOMethod(sql = "from EnterpriseDecoctionAddress  where enterpriseId=:enterpriseId and decoctionId=:decoctionId and organId=:organId and status=1", limit = 0)
    public abstract List<EnterpriseDecoctionAddress> findEnterpriseDecoctionAddressListAndStatus(@DAOParam("organId") Integer organId, @DAOParam("enterpriseId") Integer enterpriseId, @DAOParam("decoctionId") Integer decoctionId);

    @DAOMethod(sql = "from EnterpriseDecoctionAddress  where enterpriseId=:enterpriseId and organId=:organId and status=1", limit = 0)
    public abstract List<EnterpriseDecoctionAddress> findEnterpriseDecoctionAddressListByOrganIdAndEntId(@DAOParam("organId") Integer organId, @DAOParam("enterpriseId")Integer enterpriseId);

    public List<EnterpriseDecoctionAddress> findEnterpriseDecoctionAddressListByArea(Integer organId, Integer enterpriseId, Integer decoctionId, String area){
        HibernateStatelessResultAction<List<EnterpriseDecoctionAddress>> action = new AbstractHibernateStatelessResultAction<List<EnterpriseDecoctionAddress>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from EnterpriseDecoctionAddress where  enterpriseId=:enterpriseId and decoctionId=:decoctionId and organId=:organId and address like :area");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("enterpriseId", enterpriseId);
                q.setParameter("decoctionId", decoctionId);
                q.setParameter("organId", organId);
                q.setParameter("area", area + "%");
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    };

    public void cancelEnterpriseDecoctionAddress(Integer organId, Integer enterpriseId, Integer decoctionId) {

        HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("UPDATE cdr_enterprise_decoction_address set  status = 0 where enterprise_id=:enterpriseId and decoction_id=:decoctionId and organ_id=:organId");
                SQLQuery sQLQuery = ss.createSQLQuery(hql.toString());
                sQLQuery.setParameter("enterpriseId", enterpriseId);
                sQLQuery.setParameter("decoctionId", decoctionId);
                sQLQuery.setParameter("organId", organId);
                sQLQuery.executeUpdate();
            }
        };
        HibernateSessionTemplate.instance().execute(action);
    }
}
