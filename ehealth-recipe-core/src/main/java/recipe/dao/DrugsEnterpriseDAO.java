package recipe.dao;

import com.ngari.recipe.entity.DrugsEnterprise;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.StatelessSession;

import java.util.HashMap;
import java.util.List;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2016/6/6.
 */
@RpcSupportDAO
public abstract class DrugsEnterpriseDAO extends HibernateSupportDelegateDAO<DrugsEnterprise> {

    private static final Log logger = LogFactory.getLog(DrugsEnterpriseDAO.class);

    public DrugsEnterpriseDAO() {
        super();
        this.setEntityName(DrugsEnterprise.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod
    public abstract DrugsEnterprise getById(int id);

    @DAOMethod(sql = "select t from DrugsEnterprise t, OrganAndDrugsepRelation s where t.id=s.drugsEnterpriseId and t.status=1 " +
            "and s.organId=:organId and t.payModeSupport <> 0 order by t.sort, t.id")
    public abstract List<DrugsEnterprise> findByOrganId(@DAOParam("organId") Integer organId);

    @DAOMethod(sql = "select t from DrugsEnterprise t, OrganAndDrugsepRelation s where t.id=s.drugsEnterpriseId and t.status=1 " +
            "and s.organId=:organId and t.payModeSupport in :payModeSupport order by t.sort, t.id")
    public abstract List<DrugsEnterprise> findByOrganIdAndPayModeSupport(@DAOParam("organId") Integer organId, @DAOParam("payModeSupport") List<Integer> payModeSupport);

    @DAOMethod(sql = "update DrugsEnterprise set token=:token, lastModify=current_timestamp() where id=:id and status=1 ")
    public abstract void updateTokenById(@DAOParam("id") Integer id, @DAOParam("token") String token);

    @DAOMethod(sql = "select payModeSupport from DrugsEnterprise where id = :id")
    public abstract Integer getPayModeSupportById(@DAOParam("id") Integer id);

    @DAOMethod(sql = "select account from DrugsEnterprise where id = :id")
    public abstract String getAccountById(@DAOParam("id") Integer id);

    @DAOMethod(sql = "select tel from DrugsEnterprise where id = :id")
    public abstract String getTelById(@DAOParam("id") Integer id);

    /**
     * 选出需要更新token的数据
     *
     * @return
     */
    @DAOMethod(sql = "select id from DrugsEnterprise where status=1")
    public abstract List<Integer> findNeedUpdateIds();


    @DAOMethod(sql = "select t from DrugsEnterprise t where t.status=:status", limit = 0)
    public abstract List<DrugsEnterprise> findAllDrugsEnterpriseByStatus(@DAOParam("status") Integer status);


    @DAOMethod(sql = "select t from DrugsEnterprise t where t.name = :name")
    public abstract List<DrugsEnterprise> findAllDrugsEnterpriseByName(@DAOParam("name") String name);

    /**
     * 根据药企名称分页查询药企
     *
     * @param name
     * @param start
     * @param limit
     * @return
     */
    public QueryResult<DrugsEnterprise> queryDrugsEnterpriseResultByStartAndLimit(final String name, final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<DrugsEnterprise>> action = new AbstractHibernateStatelessResultAction<QueryResult<DrugsEnterprise>>() {
            @SuppressWarnings("unchecked")
            public void execute(StatelessSession ss) throws DAOException {
                long total = 0;
                StringBuilder hql = new StringBuilder("FROM DrugsEnterprise d WHERE 1=1 ");
                HashMap<String, Object> params = new HashMap<String, Object>();
                if (!StringUtils.isEmpty(name)) {
                    hql.append(" and d.name like :name");
                    params.put("name", "%" + name + "%");
                }
                hql.append(" order by d.createDate desc ");

                Query query = ss.createQuery("SELECT count(*) " + hql.toString());
                query.setProperties(params);
                total = (long) query.uniqueResult();//获取总条数

                query = ss.createQuery("SELECT d " + hql.toString());
                query.setProperties(params);
                query.setFirstResult(start);
                query.setMaxResults(limit);
                setResult(new QueryResult<DrugsEnterprise>(total, query.getFirstResult(), query.getMaxResults(), query.list()));
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    /**
     * @param account
     * @return
     */
    @DAOMethod(sql = "from DrugsEnterprise where status=1 and account=:account", limit = 1)
    public abstract DrugsEnterprise getByAccount(@DAOParam("account") String account);
}
