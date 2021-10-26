package recipe.dao;

import com.google.common.collect.Maps;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
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

    private static final Log LOGGER = LogFactory.getLog(DrugsEnterpriseDAO.class);

    public DrugsEnterpriseDAO() {
        super();
        this.setEntityName(DrugsEnterprise.class.getName());
        this.setKeyField("id");
    }

    /**
     * 根据id获取
     *
     * @param id
     * @return
     */
    @DAOMethod
    public abstract DrugsEnterprise getById(int id);

    /**
     * 批量获取药企名称
     *
     * @param ids
     * @return
     */
    @DAOMethod
    public abstract List<DrugsEnterprise> findByIdIn(List<Integer> ids);

    /**
     * 根据organId查找
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "select t from DrugsEnterprise t, OrganAndDrugsepRelation s where t.id=s.drugsEnterpriseId and t.status=1 " +
            "and s.organId=:organId and t.payModeSupport <> 0 order by t.sort, t.id")
    public abstract List<DrugsEnterprise> findByOrganId(@DAOParam("organId") Integer organId);

    /**
     * 根据organID获取自建药企
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from DrugsEnterprise where status=1 and organId=:organId and createType=0  ")
    public abstract List<DrugsEnterprise> findByOrganIdZj(@DAOParam("organId") Integer organId);

    /**
     * 根据name获取自建药企
     * @param name
     * @return
     */
    @DAOMethod(sql = "from DrugsEnterprise where  name=:name")
    public abstract DrugsEnterprise getByName(@DAOParam("name") String name);


    /**
     * 根据name获取自建药企
     * @param enterpriseCode
     * @return
     */
    @DAOMethod(sql = "from DrugsEnterprise where  enterpriseCode=:enterpriseCode")
    public abstract DrugsEnterprise getByEnterpriseCode(@DAOParam("enterpriseCode") String enterpriseCode);

    /**
    /**
     * 根据机构id及配送模式支持获取
     * @param organId
     * @param payModeSupport
     * @return
     */
    @DAOMethod(sql = "select t from DrugsEnterprise t, OrganAndDrugsepRelation s where t.id=s.drugsEnterpriseId and t.status=1 " +
            "and s.organId=:organId and t.payModeSupport in :payModeSupport order by t.sort, t.id")
    public abstract List<DrugsEnterprise> findByOrganIdAndPayModeSupport(@DAOParam("organId") Integer organId, @DAOParam("payModeSupport") List<Integer> payModeSupport);

    /**
     * 根据机构id及配送模式支持获取
     * @param organId
     * @param payModeSupport
     * @return
     */
    @DAOMethod(sql = "select count(*) from DrugsEnterprise t, OrganAndDrugsepRelation s where t.id=s.drugsEnterpriseId and t.status=1 " +
            "and s.organId=:organId and t.payModeSupport in :payModeSupport and t.sendType = :sendType")
    public abstract Long getCountByOrganIdAndPayModeSupportAndSendType(@DAOParam("organId") Integer organId, @DAOParam("payModeSupport") List<Integer> payModeSupport, @DAOParam("sendType") Integer sendType);

    /**
     * 根据机构id，配送模式支持，省直医保支持获取
     * @param organId
     * @param payModeSupport
     * @return
     */
    @DAOMethod(sql = "select t from DrugsEnterprise t, OrganAndDrugsepRelation s where t.id=s.drugsEnterpriseId and t.status=1 and t.medicalInsuranceSupport=1 " +
            "and s.organId=:organId and t.payModeSupport in :payModeSupport order by t.sort, t.id")
    public abstract List<DrugsEnterprise> findByOrganIdAndOther(@DAOParam("organId") Integer organId, @DAOParam("payModeSupport") List<Integer> payModeSupport);

    /**
     * 根据机构ID获取存在补充库存的药企机构
     * @param organId
     * @return
     */
    @DAOMethod(sql = "select t from DrugsEnterprise t, OrganAndDrugsepRelation s where t.id=s.drugsEnterpriseId and t.status=1 " +
            "and s.organId=:organId and t.hosInteriorSupport=1 order by t.sort, t.id")
    public abstract List<DrugsEnterprise> findByOrganIdAndHosInteriorSupport(@DAOParam("organId") Integer organId);

    /**
     * 根据机构id及配送模式支持获取
     * @param organId
     * @param payModeSupport
     * @return
     */
    @DAOMethod(sql = "select t from DrugsEnterprise t, OrganAndDrugsepRelation s where t.id=s.drugsEnterpriseId and t.status=1 " +
            "and s.organId=:organId and t.payModeSupport in :payModeSupport and t.sendType = :sendType order by t.sort, t.id")
    public abstract List<DrugsEnterprise> findByOrganIdAndPayModeSupportAndSendType(@DAOParam("organId") Integer organId, @DAOParam("payModeSupport") List<Integer> payModeSupport, @DAOParam("sendType") Integer sendType);

    /**
     * 根据机构id，配送模式支持，省直医保支持获取
     * @param organId
     * @param payModeSuppor
     * @return
     */
    @DAOMethod(sql = "select t from DrugsEnterprise t, OrganAndDrugsepRelation s where t.id=s.drugsEnterpriseId and t.status=1 and t.medicalInsuranceSupport=1 " +
            "and s.organId=:organId and t.payModeSupport in :payModeSupport and t.sendType = :sendType order by t.sort, t.id")
    public abstract List<DrugsEnterprise> findByOrganIdAndOtherAndSendType(@DAOParam("organId") Integer organId, @DAOParam("payModeSupport") List<Integer> payModeSuppor, @DAOParam("sendType") Integer sendType);


    /**
     * 根据id设置调用接口标识
     * @param id
     * @param token
     */
    @DAOMethod(sql = "update DrugsEnterprise set token=:token, lastModify=current_timestamp() where id=:id and status=1 ")
    public abstract void updateTokenById(@DAOParam("id") Integer id, @DAOParam("token") String token);

    /**
     * 根据id获取配送模式
     * @param id
     * @return
     */
    @DAOMethod(sql = "select payModeSupport from DrugsEnterprise where id = :id")
    public abstract Integer getPayModeSupportById(@DAOParam("id") Integer id);

    /**
     * 根据id获取药企在平台的账户
     * @param id
     * @return
     */
    @DAOMethod(sql = "select account from DrugsEnterprise where id = :id")
    public abstract String getAccountById(@DAOParam("id") Integer id);

    /**
     * 根据id获取药企联系电话
     * @param id
     * @return
     */
    @DAOMethod(sql = "select tel from DrugsEnterprise where id = :id")
    public abstract String getTelById(@DAOParam("id") Integer id);

    /**
     * 选出需要更新token的数据
     *
     * @return
     */
    @DAOMethod(sql = "select id from DrugsEnterprise where status=1 and updateTokenFlag = 1 ", limit = 0)
    public abstract List<Integer> findNeedUpdateIds();

    /**
     * 根据状态获取药企
     * @param status
     * @return
     */
    @DAOMethod(sql = "select t from DrugsEnterprise t where t.status=:status", limit = 0)
    public abstract List<DrugsEnterprise> findAllDrugsEnterpriseByStatus(@DAOParam("status") Integer status);

    /**
     * 根据名字获取药企
     * @param name
     * @return
     */
    @DAOMethod(sql = "select t from DrugsEnterprise t where t.name = :name")
    public abstract List<DrugsEnterprise> findAllDrugsEnterpriseByName(@DAOParam("name") String name);
    /**
     * 根据机构Id获取药企
     * @param organId
     * @return
     */
    @DAOMethod(sql = "select t from DrugsEnterprise t where t.organId = :organId")
    public abstract List<DrugsEnterprise> findAllDrugsEnterpriseByOrhanId(@DAOParam("organId") Integer organId);

    /**
     * 根据药企名称分页查询药企
     *
     * @param name
     * @param start
     * @param limit
     * @return
     */
    public QueryResult<DrugsEnterprise> queryDrugsEnterpriseResultByStartAndLimit(final String name, final Integer createType,final Integer organId , final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<DrugsEnterprise>> action = new AbstractHibernateStatelessResultAction<QueryResult<DrugsEnterprise>>() {
            @SuppressWarnings("unchecked")
            public void execute(StatelessSession ss) throws DAOException {
                long total = 0;
                StringBuilder hql = new StringBuilder("FROM DrugsEnterprise d WHERE 1=1 ");
                HashMap<String, Object> params = Maps.newHashMap();
                if (!StringUtils.isEmpty(name)) {
                    hql.append(" and d.name like :name");
                    params.put("name", "%" + name + "%");
                }
                if (null != createType) {
                    hql.append(" and d.createType = :createType");
                    params.put("createType", createType);
                }
                if (null != organId) {
                    hql.append(" and d.organId = :organId");
                    params.put("organId", organId);
                }

                hql.append(" order by d.createDate desc ");

                Query query = ss.createQuery("SELECT count(*) " + hql.toString());
                query.setProperties(params);
                //获取总条数
                total = (long) query.uniqueResult();

                query = ss.createQuery("SELECT d " + hql.toString());
                query.setProperties(params);
                query.setFirstResult(start);
                query.setMaxResults(limit);
                setResult(new QueryResult<DrugsEnterprise>(total, query.getFirstResult(), query.getMaxResults(), query.list()));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据药企名称分页查询药企(非第三方应用)
     *
     * @param name
     * @param start
     * @param limit
     * @return
     */
    public QueryResult<DrugsEnterprise> queryDrugsEnterpriseResultByOrganId(final String name, final Integer createType,final Integer organId ,List<Integer> ids , final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<DrugsEnterprise>> action = new AbstractHibernateStatelessResultAction<QueryResult<DrugsEnterprise>>() {
            @SuppressWarnings("unchecked")
            public void execute(StatelessSession ss) throws DAOException {
                long total = 0;
                StringBuilder hql = new StringBuilder("FROM DrugsEnterprise d WHERE 1=1 ");
                HashMap<String, Object> params = Maps.newHashMap();
                if (!StringUtils.isEmpty(name)) {
                    hql.append(" and d.name like :name");
                    params.put("name", "%" + name + "%");
                }
                if (null != createType) {
                    hql.append(" and d.createType = :createType");
                    params.put("createType", createType);
                }
                if (ids != null) {
                    hql.append(" and d.organId in :ids ");
                    params.put("ids", ids);
                }
                if (null != organId) {
                    hql.append(" and d.organId = :organId");
                    params.put("organId", organId);
                }
                hql.append(" order by d.createDate desc ");

                Query query = ss.createQuery("SELECT count(*) " + hql.toString());
                query.setProperties(params);
                //获取总条数
                total = (long) query.uniqueResult();

                query = ss.createQuery("SELECT d " + hql.toString());
                query.setProperties(params);
                query.setFirstResult(start);
                query.setMaxResults(limit);
                setResult(new QueryResult<DrugsEnterprise>(total, query.getFirstResult(), query.getMaxResults(), query.list()));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    /**
     * 根据药企名称分页查询药企
     *
     * @param manageUnit
     * @param ids
     * @return
     */
    public QueryResult<DrugsEnterpriseBean> queryDrugsEnterpriseResultByManageUnit(String manageUnit, List<Integer> ids ,final Integer status) {
        HibernateStatelessResultAction<QueryResult<DrugsEnterpriseBean>> action = new AbstractHibernateStatelessResultAction<QueryResult<DrugsEnterpriseBean>>() {
            @SuppressWarnings("unchecked")
            public void execute(StatelessSession ss) throws DAOException {
                long total = 0;
                StringBuilder hql = new StringBuilder("FROM DrugsEnterprise d WHERE 1=1 ");
                HashMap<String, Object> params = Maps.newHashMap();
                if (manageUnit == null) {
                    hql.append(" and d.organId in :ids ");
                    params.put("ids", ids);
                }
                if (manageUnit != null && manageUnit.startsWith("yq")) {
                    hql.append(" and d.manageUnit like :manageUnit ");
                    params.put("manageUnit",  manageUnit + "%");
                }
                if (status != null) {
                    hql.append(" and d.status =:status ");
                    params.put("status", status);
                }

                hql.append(" order by d.createDate desc ");

                Query query = ss.createQuery("SELECT count(*) " + hql.toString());
                query.setProperties(params);
                //获取总条数
                total = (long) query.uniqueResult();

                query = ss.createQuery("SELECT d " + hql.toString());
                query.setProperties(params);
                List<DrugsEnterpriseBean> list = query.list();
                setResult(new QueryResult<DrugsEnterpriseBean>(total, 0, (int) total, list));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据账户获取药企
     * @param account
     * @return
     */
    @DAOMethod(sql = "from DrugsEnterprise where status=1 and account=:account", limit = 1)
    public abstract DrugsEnterprise getByAccount(@DAOParam("account") String account);

    /**
     * 根据appKey获取药企
     * @param appKey
     * @return
     */
    @DAOMethod(sql = "from DrugsEnterprise where status=1 and appKey=:appKey")
    public abstract List<DrugsEnterprise> findByAppKey(@DAOParam("appKey") String appKey);

    /**
     * 根据appKey获取药企
     * @param appKey
     * @return
     */
    @DAOMethod(sql = "from DrugsEnterprise where status=1 and appKey=:appKey", limit = 1)
    public abstract DrugsEnterprise getByAppKey(@DAOParam("appKey") String appKey);

    @DAOMethod(sql = "update DrugsEnterprise set manageUnit=:manageUnit where id=:id ")
    public abstract void updateManageUnitById(@DAOParam("id") Integer id,@DAOParam("manageUnit") String manageUnit);

    @DAOMethod(sql = "from DrugsEnterprise where manageUnit=:manageUnit", limit = 1)
    public abstract DrugsEnterprise getByManageUnit(@DAOParam("manageUnit") String manageUnit);
}
