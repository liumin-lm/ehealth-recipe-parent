package recipe.dao;

import com.alibaba.druid.util.StringUtils;
import com.ngari.his.recipe.mode.DrugInfoTO;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.OrganDrugList;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.persistence.support.impl.dictionary.DBDictionaryItemLoader;
import ctd.util.annotation.RpcSupportDAO;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.springframework.util.ObjectUtils;
import recipe.bean.DrugListAndOrganDrugList;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 医疗机构用药目录dao
 * @author yuyun
 */
@RpcSupportDAO
public abstract class OrganDrugListDAO extends
        HibernateSupportDelegateDAO<OrganDrugList> implements
        DBDictionaryItemLoader<OrganDrugList> {

    private static final Integer ALL_DRUG_FLAG = 9;

    public OrganDrugListDAO() {
        super();
        this.setEntityName(OrganDrugList.class.getName());
        this.setKeyField("organDrugId");
    }

    /**
     * 通过drugid获取
     * @param drugIds
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where drugId in (:drugIds)")
    public abstract List<OrganDrugList> findByDrugId(@DAOParam("drugIds") List drugIds);

    /**
     * 通过药品id及机构id获取
     * @param organId
     * @param drugId
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and drugId=:drugId and status=1")
    public abstract OrganDrugList getByOrganIdAndDrugId(@DAOParam("organId") int organId, @DAOParam("drugId") int drugId);

    /**
     * 通过机构id获取
     * @param organId
     * @param start
     * @param limit
     * @return
     */
    @DAOMethod(sql = "select new com.ngari.his.recipe.mode.DrugInfoTO(od.organDrugCode,d.pack,d.unit,od.producerCode) from OrganDrugList od, DrugList d where od.drugId=d.drugId and od.organId=:organId and od.organDrugCode is not null and od.status=1")
    public abstract List<DrugInfoTO> findDrugInfoByOrganId(@DAOParam("organId") int organId, @DAOParam(pageStart = true) int start, @DAOParam(pageLimit = true) int limit);

    /**
     * 通过机构id及药品id列表获取
     * @param organId
     * @param drugIds
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and drugId in (:drugIds) and status=1")
    public abstract List<OrganDrugList> findByOrganIdAndDrugIds(@DAOParam("organId") int organId, @DAOParam("drugIds") List<Integer> drugIds);

    /**
     * 通过机构id及机构药品编码获取
     * @param organId
     * @param drugCodes
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and organDrugCode in (:drugCodes) and status=1")
    public abstract List<OrganDrugList> findByOrganIdAndDrugCodes(@DAOParam("organId") int organId, @DAOParam("drugCodes") List<String> drugCodes);

    /**
     * 通过机构id，药品编码列表获取
     * @param organId
     * @param drugCodes
     * @return
     */
    @DAOMethod(sql = "select d.drugName from OrganDrugList od, DrugList d where od.drugId=d.drugId and od.organId=:organId and od.organDrugCode in (:drugCodes) and od.status=1")
    public abstract List<String> findNameByOrganIdAndDrugCodes(@DAOParam("organId") int organId, @DAOParam("drugCodes") List<String> drugCodes);

    /**
     *  根据organId查询该机构是否存在可用的有效药品。
     */
    public int getCountByOrganIdAndStatus(final List<Integer> organIdList) {
        HibernateStatelessResultAction<Long> action = new AbstractHibernateStatelessResultAction<Long>() {
            public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder();
                hql.append("select count(OrganDrugId) From OrganDrugList where organId in (");
                if (organIdList.size() > 0) {
                    hql.append(organIdList.get(0));
                    for (int i = 1; i < organIdList.size(); i++) {
                        hql.append("," + organIdList.get(i));
                    }
                }
                hql.append(") and status=1");
                Query q = ss.createQuery(hql.toString());
                setResult((Long) q.uniqueResult());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return Integer.parseInt(action.getResult().toString());
    }

    /**
     * 根据医院药品编码 和机构编码查询 医院药品
     * @param organId
     * @param organDrugCode
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and organDrugCode=:organDrugCode")
    public abstract OrganDrugList getByOrganIdAndOrganDrugCode(@DAOParam("organId") int organId, @DAOParam("organDrugCode") String organDrugCode);

    /**
     * 通过药品id及机构id获取
     * @param drugId
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where drugId=:drugId and organId=:organId ")
    public abstract OrganDrugList getByDrugIdAndOrganId(@DAOParam("drugId") int drugId, @DAOParam("organId") int organId);

    /**
     * 通过机构id及药品id更新药品价格
     * @param organId
     * @param drugId
     * @param salePrice
     */
    @DAOMethod(sql = "update OrganDrugList set salePrice=:salePrice where organId=:organId and drugId=:drugId")
    public abstract void updateDrugPrice(@DAOParam("organId") int organId, @DAOParam("drugId") int drugId, @DAOParam("salePrice") BigDecimal salePrice);

    /**
     * 机构药品查询
     *
     * @param organId   机构
     * @param drugClass 药品分类
     * @param keyword   查询关键字:药品序号 or 药品名称 or 生产厂家 or 商品名称 or 批准文号
     * @param start
     * @param limit
     * @return
     * @author houxr
     */
    public QueryResult<DrugListAndOrganDrugList> queryOrganDrugListByOrganIdAndKeyword(final Integer organId,
                                                                                       final String drugClass,
                                                                                       final String keyword, final Integer status,
                                                                                       final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<DrugListAndOrganDrugList>> action =
                new AbstractHibernateStatelessResultAction<QueryResult<DrugListAndOrganDrugList>>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void execute(StatelessSession ss) throws DAOException {
                        StringBuilder hql = new StringBuilder(" from DrugList d where 1=1 ");
                        if (!StringUtils.isEmpty(drugClass)) {
                            hql.append(" and d.drugClass like :drugClass");
                        }
                        Integer drugId = null;
                        if (!StringUtils.isEmpty(keyword)) {
                            try {
                                drugId = Integer.valueOf(keyword);
                            } catch (Throwable throwable) {
                                drugId = null;
                            }
                            hql.append(" and (");
                            hql.append(" d.drugName like :keyword or d.producer like :keyword or d.saleName like :keyword or d.approvalNumber like :keyword ");
                            if (drugId != null){
                                hql.append(" or d.drugId =:drugId");
                            }
                            hql.append(")");
                        }
                        if (ObjectUtils.nullSafeEquals(status, 0)) {
                            hql.append(" and d.drugId in (select o.drugId from OrganDrugList o where o.status = 0 and o.organId =:organId)");
                        } else if (ObjectUtils.nullSafeEquals(status, 1)) {
                            hql.append(" and d.drugId in (select o.drugId from OrganDrugList o where o.status = 1 and o.organId =:organId)");
                        } else if (ObjectUtils.nullSafeEquals(status, -1)) {
                            hql.append(" and d.drugId not in (select o.drugId from OrganDrugList o where o.organId =:organId) ");
                        } else if (ObjectUtils.nullSafeEquals(status, ALL_DRUG_FLAG)) {
                            hql.append(" and d.drugId in (select o.drugId from OrganDrugList o where o.status in (0,1) and o.organId =:organId)");
                        }
                        hql.append(" and d.status=1 order by d.drugId desc");
                        Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                        if (!StringUtils.isEmpty(drugClass)) {
                            countQuery.setParameter("drugClass", drugClass + "%");
                        }
                        if (ObjectUtils.nullSafeEquals(status, 0)
                                || ObjectUtils.nullSafeEquals(status, 1)
                                || ObjectUtils.nullSafeEquals(status, -1)
                                || ObjectUtils.nullSafeEquals(status, 9)) {
                            countQuery.setParameter("organId", organId);
                        }
                        if (drugId != null) {
                            countQuery.setParameter("drugId", drugId);
                        }
                        if (!StringUtils.isEmpty(keyword)) {
                            countQuery.setParameter("keyword", "%" + keyword + "%");
                        }
                        Long total = (Long) countQuery.uniqueResult();

                        Query query = ss.createQuery("select d " + hql.toString());
                        if (!StringUtils.isEmpty(drugClass)) {
                            query.setParameter("drugClass", drugClass + "%");
                        }
                        if (ObjectUtils.nullSafeEquals(status, 0)
                                || ObjectUtils.nullSafeEquals(status, 1)
                                || ObjectUtils.nullSafeEquals(status, -1)
                                || ObjectUtils.nullSafeEquals(status, 9)) {
                            query.setParameter("organId", organId);
                        }
                        if (drugId != null) {
                            query.setParameter("drugId", drugId);
                        }
                        if (!StringUtils.isEmpty(keyword)) {
                            query.setParameter("keyword", "%" + keyword + "%");
                        }
                        query.setFirstResult(start);
                        query.setMaxResults(limit);
                        List<DrugList> list = query.list();
                        List<DrugListAndOrganDrugList> result = new ArrayList<DrugListAndOrganDrugList>();
                        for (DrugList drug : list) {
                            OrganDrugList organDrugList = getByDrugIdAndOrganId(drug.getDrugId(), organId);
                            result.add(new DrugListAndOrganDrugList(drug, organDrugList));
                        }
                        setResult(new QueryResult<DrugListAndOrganDrugList>(total, query.getFirstResult(), query.getMaxResults(), result));
                    }
                };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据机构id获取数量
     * @param organId
     * @return
     */
    @DAOMethod(sql = "select count(*) from OrganDrugList where organId=:organId")
    public abstract long getCountByOrganId(@DAOParam("organId") int organId);

    /**
     * 更新机构id
     * @param newOrganId
     * @param oldOrganId
     */
    @DAOMethod(sql = "update OrganDrugList set organId=:newOrganId where organId=:oldOrganId")
    public abstract void updateOrganIdByOrganId(@DAOParam("newOrganId") int newOrganId, @DAOParam("oldOrganId") int oldOrganId);

    /**
     * 根据药品编码列表更新状态
     * @param organDrugCodeList
     * @param status
     */
    @DAOMethod(sql = "update OrganDrugList set status=:status where organDrugCode in :organDrugCodeList")
    public abstract void updateStatusByOrganDrugCode(@DAOParam("organDrugCodeList") List<String> organDrugCodeList, @DAOParam("status") int status);

    public List<Integer> queryOrganCanRecipe(final List<Integer> organIds, final Integer drugId) {
        HibernateStatelessResultAction<List<Integer>> action = new AbstractHibernateStatelessResultAction<List<Integer>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select DISTINCT o.organId from  DrugList d, OrganDrugList o where " +
                        "o.drugId = d.drugId and d.status = 1 and o.status = 1 ");
                if (null != drugId && drugId > 0) {
                    hql.append("and d.drugId = :drugId ");
                }
                if (null != organIds && organIds.size() > 0) {
                    hql.append("and o.organId in (:organIds) ");
                }
                Query q = ss.createQuery(hql.toString());
                if (null != organIds && organIds.size() > 0) {
                    q.setParameterList("organIds", organIds);
                }
                if (null != drugId && drugId > 0) {
                    q.setParameter("drugId", drugId);
                }
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }
}
