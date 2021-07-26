package recipe.dao;

import com.google.common.collect.Maps;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.SaleDrugList;
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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.joda.time.DateTime;
import org.springframework.util.ObjectUtils;
import recipe.dao.bean.DrugListAndSaleDrugList;
import recipe.util.LocalStringUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author yuyun
 */
@RpcSupportDAO
public abstract class SaleDrugListDAO extends HibernateSupportDelegateDAO<SaleDrugList>
        implements DBDictionaryItemLoader<SaleDrugList> {

    private static final Integer ALL_DRUG_FLAG = 9;

    public SaleDrugListDAO() {
        super();
        this.setEntityName(SaleDrugList.class.getName());
        this.setKeyField("organDrugId");
    }

    /**
     * 根据机构id及药品id列表获取数量
     *
     * @param organId
     * @param drugId
     * @return
     */
    @DAOMethod(sql = "select count(id) from SaleDrugList where status=1 and organId=:organId and drugId in :drugId")
    public abstract Long getCountByOrganIdAndDrugIds(@DAOParam("organId") int organId, @DAOParam("drugId") List<Integer> drugId);
    /**
     * 根据机构id及药品id列表
     *
     * @param organId
     * @param drugIds
     * @return
     */
    public  List<SaleDrugList> getByOrganIdAndDrugIds(@DAOParam("organId") int organId, @DAOParam("drugIds") List<Integer> drugIds){
        HibernateStatelessResultAction<List<SaleDrugList> > action = new AbstractHibernateStatelessResultAction<List<SaleDrugList> >() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "from SaleDrugList where status=1 and organId=:organId and drugId in :drugIds";
                Map<String, Object> param = Maps.newHashMap();
                param.put("organId", organId);
                param.put("drugIds", drugIds);
                Query query = ss.createQuery(hql);
                query.setProperties(param);
                List list = query.list();
                setResult(list);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    };

    /**
     * 设置某些药品为无效
     *
     * @param organId
     * @param drugId
     */
    @DAOMethod(sql = "update SaleDrugList set status=0 where organId=:organId and drugId in :drugId")
    public abstract void updateInvalidByOrganIdAndDrugIds(@DAOParam("organId") int organId, @DAOParam("drugId") List<Integer> drugId);

    /**
     * 一键禁用
     *
     * @param organId
     */
    @DAOMethod(sql = "update SaleDrugList set status=0 where organId=:organId ")
    public abstract void updateInvalidByOrganId(@DAOParam("organId") int organId);


    /**
     * 设置某些药品为有效
     *
     * @param organId
     * @param drugId
     */
    @DAOMethod(sql = "update SaleDrugList set status=1 where organId=:organId and drugId in :drugId")
    public abstract void updateEffectiveByOrganIdAndDrugIds(@DAOParam("organId") int organId, @DAOParam("drugId") List<Integer> drugId);

    /**
     * 需要同步的药品id，不区分status
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "select drugId from SaleDrugList where organId=:organId group by drugId", limit = 0)
    public abstract List<Integer> findSynchroDrug(@DAOParam("organId") int organId);

    /**
     * 获取指定的药品数据
     * @param organId
     * @param drugIds
     * @return
     */
    @DAOMethod(sql = "from SaleDrugList where organId=:organId and drugId in :drugIds ", limit = 0)
    public abstract List<SaleDrugList> findByOrganIdAndDrugIds(@DAOParam("organId") int organId, @DAOParam("drugIds") List<Integer> drugIds);

    /**
     * 获取指定的药品数据
     * @param organId
     * @param drugId
     * @return
     */
    @DAOMethod(sql = "from SaleDrugList where organId=:organId and drugId=:drugId ")
    public abstract SaleDrugList getByOrganIdAndDrugId(@DAOParam("organId") int organId, @DAOParam("drugId") Integer drugId);


    /**
     * 根据药品编码获取数据
     * @param organId
     * @param drugCodes
     * @return
     */
    @DAOMethod(sql = "from SaleDrugList where organId=:organId and organDrugCode in :drugCodes ", limit = 0)
    public abstract List<SaleDrugList> findByOrganIdAndDrugCodes(@DAOParam("organId") int organId, @DAOParam("drugCodes") List<String> drugCodes);

    /**
     * 根据药品id及机构id获取
     *
     * @param drugId
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from SaleDrugList where drugId=:drugId and organId=:organId")
    public abstract SaleDrugList getByDrugIdAndOrganId(@DAOParam("drugId") int drugId, @DAOParam("organId") int organId);
    /**
     * 根据机构id获取药品id集合
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "select drugId from SaleDrugList where organId=:organId and status=1", limit = 0)
    public abstract List<Integer> findDrugIdByOrganId(@DAOParam("organId") int organId);

    /**
     * 获取药品与配送药企关系 （药品1:药企A,药企B）
     *
     * @param drugIds
     * @param depIds
     * @return
     */
    public Map<Integer, List<String>> findDrugDepRelation(final List<Integer> drugIds, List<Integer> depIds) {
        HibernateStatelessResultAction<List<Object[]>> action =
                new AbstractHibernateStatelessResultAction<List<Object[]>>() {
                    @Override
                    public void execute(StatelessSession ss) throws DAOException {
                        StringBuilder hql = new StringBuilder("select DrugId, GROUP_CONCAT(OrganID) from base_saledruglist " +
                                "where DrugId in :drugIds and OrganID in :depIds and Status=1 GROUP BY DrugId");
                        Query q = ss.createSQLQuery(hql.toString());
                        q.setParameterList("drugIds", drugIds);
                        q.setParameterList("depIds", depIds);
                        setResult(q.list());
                    }
                };

        HibernateSessionTemplate.instance().execute(action);
        List<Object[]> objects = action.getResult();
        Map<Integer, List<String>> relation = Maps.newHashMap();
        for (Object[] obj : objects) {
            Integer drugId = Integer.valueOf(obj[0].toString());
            String depIdStr = LocalStringUtil.toString(obj[1]);
            List<String> depIdList = new ArrayList<>(0);
            if (StringUtils.isNotEmpty(depIdStr)) {
                CollectionUtils.addAll(depIdList, depIdStr.split(","));

            }
            relation.put(drugId, depIdList);
        }
        return relation;
    }

    /**
     * 获取药企与配送药品关系 （药企A:药品1,药品2）
     *
     * @param drugIds
     * @param depIds
     * @return
     */
    public Map<Integer, List<Integer>> findDepDrugRelation(final List<Integer> drugIds, List<Integer> depIds) {
        HibernateStatelessResultAction<List<Object[]>> action =
                new AbstractHibernateStatelessResultAction<List<Object[]>>() {
                    @Override
                    public void execute(StatelessSession ss) throws DAOException {
                        StringBuilder hql = new StringBuilder("select OrganID, GROUP_CONCAT(DrugId) from base_saledruglist " +
                                "where DrugId in :drugIds and OrganID in :depIds and Status=1 GROUP BY OrganID");
                        Query q = ss.createSQLQuery(hql.toString());
                        q.setParameterList("drugIds", drugIds);
                        q.setParameterList("depIds", depIds);
                        setResult(q.list());
                    }
                };

        HibernateSessionTemplate.instance().execute(action);
        List<Object[]> objects = action.getResult();
        Map<Integer, List<Integer>> relation = Maps.newHashMap();
        for (Object[] obj : objects) {
            Integer depId = Integer.valueOf(obj[0].toString());
            String drugIdStr = LocalStringUtil.toString(obj[1]);
            List<Integer> drugIdList = new ArrayList<>(0);
            if (StringUtils.isNotEmpty(drugIdStr)) {
                CollectionUtils.addAll(drugIdList, drugIdStr.split(","));

            }
            relation.put(depId, drugIdList);
        }
        return relation;
    }

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
    public QueryResult<DrugListAndSaleDrugList> querySaleDrugListByOrganIdAndKeyword(final Date startTime, final Date endTime, final Integer organId,
                                                                                     final String drugClass,
                                                                                     final String keyword, final Integer status,
                                                                                     final Integer type, final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<DrugListAndSaleDrugList>> action =
                new AbstractHibernateStatelessResultAction<QueryResult<DrugListAndSaleDrugList>>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void execute(StatelessSession ss) throws DAOException {
                        DateTime dt = new DateTime(endTime);
                        StringBuilder hql = new StringBuilder(" from DrugList d ");
                        if (!ObjectUtils.nullSafeEquals(status, -1)){
                            hql.append(",SaleDrugList o where 1=1 ");
                        }else{
                            hql.append(" where 1=1 ");
                        }
                        if (!StringUtils.isEmpty(drugClass)) {
                            hql.append(" and d.drugClass like :drugClass");
                        }

                        if (!ObjectUtils.isEmpty(type)) {
                            hql.append(" and d.drugType =:drugType");
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
                            if (drugId != null) {
                                hql.append(" or d.drugId =:drugId");
                            }
                            hql.append(")");
                        }
                        if (ObjectUtils.nullSafeEquals(status, 0)) {
                            hql.append(" and d.drugId = o.drugId and o.status = 0 and o.organId =:organId and o.createDt>=:startTime and o.createDt<=:endTime ");
                        } else if (ObjectUtils.nullSafeEquals(status, 1)) {
                            hql.append(" and d.drugId  = o.drugId and o.status = 1 and o.organId =:organId and o.createDt>=:startTime and o.createDt<=:endTime ");
                        } else if (ObjectUtils.nullSafeEquals(status, -1)) {
                            hql.append(" and d.drugId not in (select o.drugId from SaleDrugList o where o.organId =:organId and o.createDt>=:startTime and o.createDt<=:endTime) ");
                        } else if (ObjectUtils.nullSafeEquals(status, ALL_DRUG_FLAG)) {
                            hql.append(" and d.drugId = o.drugId and o.status in (0, 1) and o.organId =:organId and o.createDt>=:startTime and o.createDt<=:endTime ");
                        }
                        if(ObjectUtils.nullSafeEquals(status, ALL_DRUG_FLAG)){
                            hql.append(" and d.status=1 order by o.createDt desc");
                        }else{
                            hql.append(" and d.status=1 order by d.drugId desc");
                        }
                        Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                        if (!StringUtils.isEmpty(drugClass)) {
                            countQuery.setParameter("drugClass", drugClass + "%");
                        }
                        if (!ObjectUtils.isEmpty(type)) {
                            countQuery.setParameter("drugType", type);
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
                        countQuery.setParameter("startTime", startTime);
                        countQuery.setParameter("endTime", dt.plusDays(1).toDate());
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
                        if (!ObjectUtils.isEmpty(type)) {
                            query.setParameter("drugType", type);
                        }
                        if (!ObjectUtils.isEmpty(startTime)){
                            query.setParameter("startTime", startTime);
                        }
                        if (!ObjectUtils.isEmpty(endTime)){
                            query.setParameter("endTime", dt.plusDays(1).toDate());
                        }
                        query.setFirstResult(start);
                        query.setMaxResults(limit);
                        List<DrugList> list = query.list();
                        List<DrugListAndSaleDrugList> result = new ArrayList<DrugListAndSaleDrugList>();
                        for (DrugList drug : list) {
                            SaleDrugList saleDrugList = getByDrugIdAndOrganId(drug.getDrugId(), organId);
                            result.add(new DrugListAndSaleDrugList(drug, saleDrugList));
                        }
                        setResult(new QueryResult<DrugListAndSaleDrugList>(total, query.getFirstResult(), query.getMaxResults(), result));
                    }
                };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 更新药品库存
     *
     * @param drugId
     * @param depId
     * @param inventory
     * @return
     */
    public boolean updateDrugInventory(final Integer drugId, final Integer depId, final BigDecimal inventory) {
        HibernateStatelessResultAction<Boolean> action =
                new AbstractHibernateStatelessResultAction<Boolean>() {
                    @Override
                    public void execute(StatelessSession ss) throws DAOException {
                        StringBuilder hql = new StringBuilder(" update SaleDrugList set lastModify=current_timestamp()," +
                                "status=:status, inventory=:inventory where organId=:depId and drugId=:drugId ");
                        Query q = ss.createQuery(hql.toString());
                        q.setParameter("inventory", inventory);
                        int status = 0;
                        if (inventory.compareTo(BigDecimal.ZERO) > 0) {
                            status = 1;
                        }
                        q.setParameter("status", status);
                        q.setParameter("drugId", drugId);
                        q.setParameter("depId", depId);

                        q.executeUpdate();
                        setResult(true);
                    }
                };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 更新药品库存
     *
     * @param organId  药企
     * @param drugId   药品编码
     * @param useTotalDose 开药数量
     */
    public Integer updateInventoryByOrganIdAndDrugId(final Integer organId, final Integer drugId, final BigDecimal useTotalDose) {
        HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "update SaleDrugList set inventory=(inventory-:useTotalDose) where organId=:organId and drugId =:drugId and inventory >=:useTotalDose";
                Query q = ss.createQuery(hql);
                q.setParameter("organId", organId);
                q.setParameter("drugId", drugId);
                q.setParameter("useTotalDose", useTotalDose);
                setResult(q.executeUpdate());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据药品编码获取数据
     * @param organId
     * @param drugCode
     * @return
     */
    @DAOMethod(sql = "from SaleDrugList where organId=:organId and organDrugCode =:drugCode ")
    public abstract SaleDrugList getByOrganIdAndDrugCode(@DAOParam("organId") int organId, @DAOParam("drugCode") String drugCode);

    /**
     * 根据药品编码获取数据
     * @param organId
     * @param drugCode
     * @return
     */
    @DAOMethod(sql = "from SaleDrugList where organId=:organId and organDrugCode =:drugCode ")
    public abstract List<SaleDrugList> findByOrganIdAndDrugCode(@DAOParam("organId") int organId, @DAOParam("drugCode") String drugCode);


    /**
     * 根据organId查询所有配送药品
     * @param organId  配送机构
     * @return         药品列表
     */
    @DAOMethod(sql = "from SaleDrugList where organId=:organId order by organDrugId")
    public abstract List<SaleDrugList> findByOrganId(@DAOParam("organId") int organId,
                                                     @DAOParam(pageStart = true) int start,
                                                     @DAOParam(pageLimit = true) int limit);

    /**
     * 根据机构id获取药品id集合
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from SaleDrugList where organId = :organId and drugId = :drugId and organDrugCode = :organDrugCode and status = :status ", limit = 0)
    public abstract List<SaleDrugList> findByDrugIdAndOrganIdAndOrganDrugCodeAndStatus(@DAOParam("organId") int organId, @DAOParam("drugId") int drugId, @DAOParam("organDrugCode") String organDrugCode, @DAOParam("status") int status);

    /**
     * 根据drugId查询所有配送药品
     * @param drugId  平台药品id
     * @return         药品列表
     */
    @DAOMethod(sql = "from SaleDrugList where drugId=:drugId and organId in :organIds and status = 1",limit = 0)
    public abstract List<SaleDrugList> findByDrugIdAndOrganIds(@DAOParam("drugId") int drugId,@DAOParam("organIds") List<Integer> organIds);


    @DAOMethod(sql = "from SaleDrugList where drugId=:drugId and organId=:organId and status = 1")
    public abstract SaleDrugList getByDrugIdAndOrganIdAndStatus(@DAOParam("drugId") int drugId, @DAOParam("organId") int organId);


    @DAOMethod(sql = "update SaleDrugList set inventory=:inventory where organId=:organId ")
    public abstract void updateIntroduceByDepId(@DAOParam("organId") int organId, @DAOParam("inventory") Integer inventory);

    /**
     * 根据drugId查询所有配送药品数量
     * @param drugId  平台药品id
     * @return         药品数量
     */
    @DAOMethod(sql = "select count(organDrugId) from SaleDrugList where drugId=:drugId  ",limit = 0)
    public abstract Long getCountByDrugId(@DAOParam("drugId") int drugId);

}
