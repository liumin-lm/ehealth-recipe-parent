package recipe.dao;

import com.alibaba.druid.util.StringUtils;
import com.google.common.collect.Lists;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.drug.model.DepSaleDrugInfo;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.SaleDrugList;
import ctd.persistence.DAOFactory;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.persistence.support.impl.dictionary.DBDictionaryItemLoader;
import ctd.util.BeanUtils;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.joda.time.DateTime;
import org.springframework.util.ObjectUtils;
import recipe.dao.bean.DrugInfoHisBean;
import recipe.dao.bean.DrugListAndOrganDrugList;

import java.math.BigDecimal;
import java.util.*;

/**
 * 医疗机构用药目录dao
 *
 * @author yuyun
 */
@RpcSupportDAO
public abstract class OrganDrugListDAO extends HibernateSupportDelegateDAO<OrganDrugList> implements DBDictionaryItemLoader<OrganDrugList> {

    private static final Integer ALL_DRUG_FLAG = 9;
    private static Logger logger = Logger.getLogger(OrganDrugListDAO.class);

    public OrganDrugListDAO() {
        super();
        setEntityName(OrganDrugList.class.getName());
        setKeyField("organDrugId");
    }

    /**
     * 通过drugid获取
     *
     * @param drugIds
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and drugId in (:drugIds)", limit = 0)
    public abstract List<OrganDrugList> findByOrganIdAndDrugIdWithoutStatus(@DAOParam("organId") int organId, @DAOParam("drugIds") List drugIds);

    /**
     * 通过机构id一键禁用该机构下的所有机构药品
     *
     * @param organId
     */
    @DAOMethod(sql = "update OrganDrugList  a set a.status=:status where a.organId=:organId ")
    public abstract void updateDrugStatus(@DAOParam("organId") int organId, @DAOParam("status") int status);


    /**
     * 通过药品id及机构id获取(已废弃，有可能会获取到多条)
     *
     * @param organId
     * @param drugId
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and drugId=:drugId and status=1")
    @Deprecated
    public abstract OrganDrugList getByOrganIdAndDrugId(@DAOParam("organId") int organId, @DAOParam("drugId") int drugId);


    /**
     * 查询机构药品药房不为空 的机构
     *
     * @return
     */
    @DAOMethod(sql = "select distinct organId from OrganDrugList where pharmacy is not null ", limit = 0)
    public abstract List<Integer> findOrganIdByPharmacy();

    /**
     * 查询机构药品药房不为空 的药品
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and pharmacy is not null ", limit = 0)
    public abstract List<OrganDrugList> findByOrganIdAndPharmacy(@DAOParam("organId") int organId);


    /**
     * 通过药品id和机构id进行批量获取药品信息
     *
     * @param organId    机构id
     * @param drugIdList 药品id 集合
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and drugId in(:drugIdList) and status=1")
    public abstract List<OrganDrugList> findByOrganIdAndDrugIdList(@DAOParam("organId") int organId, @DAOParam("drugIdList") List<Integer> drugIdList);

    /**
     *
     * @param  organId 机构id
     * @param drugIdList 药品id 集合
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and drugId in(:drugIdList) and status=1 and medicalInsuranceControl=1")
    public abstract List<OrganDrugList> findByOrganIdAndDrugAndMedicalIdList(@DAOParam("organId") int organId, @DAOParam("drugIdList") List<Integer> drugIdList);


    /**
     * 通过药品id及机构id获取
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId  and status=1", limit = 0)
    public abstract List<OrganDrugList> findByOrganId(@DAOParam("organId") int organId);

    /**
     * 通过机构id获取
     *
     * @param organId
     * @param start
     * @param limit
     * @return
     */
    @DAOMethod(sql = "select new recipe.dao.bean.DrugInfoHisBean(od.organDrugCode,d.pack,d.unit,od.producerCode,od.pharmacy) " + "from OrganDrugList od, DrugList d where od.drugId=d.drugId and od.organId=:organId and od.organDrugCode is not null and od.status=1")
    public abstract List<DrugInfoHisBean> findDrugInfoByOrganId(@DAOParam("organId") int organId, @DAOParam(pageStart = true) int start, @DAOParam(pageLimit = true) int limit);

    /**
     * 通过机构id及药品id列表获取
     *
     * @param organId
     * @param drugIds
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and drugId in (:drugIds) and status=1")
    public abstract List<OrganDrugList> findByOrganIdAndDrugIds(@DAOParam("organId") int organId, @DAOParam("drugIds") List<Integer> drugIds);

    /**
     * 通过机构id及机构药品编码获取
     *
     * @param organId
     * @param drugCodes
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and organDrugCode in (:drugCodes) and status=1", limit = 0)
    public abstract List<OrganDrugList> findByOrganIdAndDrugCodes(@DAOParam("organId") int organId, @DAOParam("drugCodes") List<String> drugCodes);

    /**
     * 通过机构id，药品编码列表获取
     *
     * @param organId
     * @param drugCodes
     * @return
     */
    @DAOMethod(sql = "select od.drugName from OrganDrugList od where od.organId=:organId and od.organDrugCode in (:drugCodes) and od.status=1")
    public abstract List<String> findNameByOrganIdAndDrugCodes(@DAOParam("organId") int organId, @DAOParam("drugCodes") List<String> drugCodes);

    /**
     * 根据organId查询该机构是否存在可用的有效药品。
     */
    public int getCountByOrganIdAndStatus(final List<Integer> organIdList) {
        HibernateStatelessResultAction<Long> action = new AbstractHibernateStatelessResultAction<Long>() {
            @Override
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
     * 根据医院药品编码 和机构编码查询 医院药品------有可能查到多条记录故应废弃
     *
     * @param organId
     * @param organDrugCode
     * @return
     */
    @Deprecated
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and organDrugCode=:organDrugCode and status = 1")
    public abstract OrganDrugList getByOrganIdAndOrganDrugCode(@DAOParam("organId") int organId, @DAOParam("organDrugCode") String organDrugCode);

    /**
     * 根据机构编码 和药房编码模糊查询 医院药品
     *
     * @param organId
     * @param pharmacyId
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and pharmacy like:pharmacyId and status = 1", limit = 0)
    public abstract List<OrganDrugList> findByOrganIdAndPharmacyId(@DAOParam("organId") int organId, @DAOParam("pharmacyId") String pharmacyId);


    /**
     * 根据机构编码 和药品嘱托编码模糊查询 医院药品
     *
     * @param organId
     * @param drugEntrust
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and drugEntrust =:drugEntrust and status = 1", limit = 0)
    public abstract List<OrganDrugList> findByOrganIdAndDrugEntrust(@DAOParam("organId") int organId, @DAOParam("drugEntrust") String drugEntrust);


    /**
     * 根据drugId 和医院药品编码 和机构编码查询 医院药品
     *
     * @param organId
     * @param organDrugCode
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and organDrugCode=:organDrugCode and drugId=:drugId and status = 1")
    public abstract OrganDrugList getByOrganIdAndOrganDrugCodeAndDrugId(@DAOParam("organId") int organId, @DAOParam("organDrugCode") String organDrugCode, @DAOParam("drugId") Integer drugId);

    @DAOMethod(sql = "from OrganDrugList where organId=:organId and organDrugCode=:organDrugCode and drugId=:drugId ")
    public abstract List<OrganDrugList> findByOrganIdAndOrganDrugCodeAndDrugIdWithoutStatus(@DAOParam("organId") int organId, @DAOParam("organDrugCode") String organDrugCode, @DAOParam("drugId") Integer drugId);

    @DAOMethod(sql = "from OrganDrugList where organId=:organId and producerCode=:producerCode and status = 1")
    public abstract OrganDrugList getByOrganIdAndProducerCode(@DAOParam("organId") int organId, @DAOParam("producerCode") String producerCode);


    /**
     * 通过药品id及机构id获取
     *
     * @param drugId
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where drugId=:drugId and organId=:organId ")
    @Deprecated
    public abstract OrganDrugList getByDrugIdAndOrganId(@DAOParam("drugId") int drugId, @DAOParam("organId") int organId);


    /**
     * 通过药品编码及机构id获取
     *
     * @param organDrugCode
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organDrugCode=:organDrugCode and organId=:organId and status = 1 ")
    public abstract List<OrganDrugList> findByOrganDrugCodeAndOrganId(@DAOParam("organDrugCode") String organDrugCode, @DAOParam("organId") int organId);


    /**
     * 通过药品id及机构id获取
     *
     * @param drugId
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where drugId=:drugId and organId=:organId ")
    public abstract List<OrganDrugList> findByDrugIdAndOrganId(@DAOParam("drugId") int drugId, @DAOParam("organId") int organId);

    /**
     * 通过药品id获取
     *
     * @param drugId
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where drugId=:drugId  ")
    public abstract List<OrganDrugList> findByDrugId(@DAOParam("drugId") int drugId);

    public List<OrganDrugList> findByDrugIdAndOrganId(final List<Integer> recipeIds) {
        HibernateStatelessResultAction<List<OrganDrugList>> action = new AbstractHibernateStatelessResultAction<List<OrganDrugList>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                final StringBuilder preparedHql = this.generateRecipeOderHQLforStatisticsN(recipeIds);
                Query q = ss.createSQLQuery(preparedHql.toString());
                List<Object[]> result = q.list();
                List<OrganDrugList> organDrugLists = new ArrayList();
                Map<String, Object> vo = new HashMap();
                for (int i = 0; i < result.size(); i++) {
                    OrganDrugList organDrugList = new OrganDrugList();
                    organDrugList.setDrugId((Integer) result.get(i)[0]);
                    organDrugList.setOrganId((Integer) result.get(i)[1]);
                    organDrugList.setDrugForm((String) result.get(i)[2]);
                    organDrugList.setDrugName((String) result.get(i)[3]);
                    organDrugList.setSaleName((String) result.get(i)[4]);
                    organDrugLists.add(organDrugList);
                }
                setResult(organDrugLists);
            }

            private StringBuilder generateRecipeOderHQLforStatisticsN(List<Integer> recipeIds) {
                StringBuilder hql = new StringBuilder("select ");
                hql.append("bo.drugId,bo.organId,bo.DrugForm,bo.drugName,bo.saleName");
                hql.append(" from cdr_recipe r  ");
                hql.append("LEFT JOIN cdr_recipedetail d ON r.RecipeID = d.RecipeID  ");
                hql.append("LEFT JOIN base_organdruglist bo ON bo.drugId = d.drugId and bo.organId= r.clinicOrgan ");
                hql.append(" where  1= 1 ");
                if (CollectionUtils.isNotEmpty(recipeIds)) {
                    boolean flag = true;
                    for (Integer i : recipeIds) {
                        if (i != null) {
                            if (flag) {
                                hql.append(" and r.recipeId in(");
                                flag = false;
                            }
                            hql.append(i + ",");
                        }
                    }
                    if (!flag) {
                        hql = new StringBuilder(hql.substring(0, hql.length() - 1) + ") ");
                    }
                }
                return hql;
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    /**
     * 通过药品id及机构id获取
     *
     * @param drugIds
     * @param organIds
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where drugId in:drugIds and organId in:organIds")
    public abstract List<OrganDrugList> findByDrugIdsAndOrganIds(@DAOParam("drugIds") List<Integer> drugIds, @DAOParam("organIds") List<Integer> organIds);


    /**
     * 通过药品id及机构id获取
     *
     * @param drugId
     * @param organId
     * @return
     */
    public List<OrganDrugList> findOrganDrugs(final int drugId, final int organId, final Integer status) {
        HibernateStatelessResultAction<List<OrganDrugList>> action = new AbstractHibernateStatelessResultAction<List<OrganDrugList>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from OrganDrugList where drugId=:drugId and organId=:organId ");
                if (status == 0) {
                    hql.append(" and status = 0 ");
                }
                if (status == 1) {
                    hql.append(" and status = 1 ");
                }

                Query query = ss.createQuery(String.valueOf(hql));

                query.setParameter("drugId", drugId);
                query.setParameter("organId", organId);
                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    /**
     * 通过机构id及药品id更新药品价格
     *
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
    public QueryResult<DrugListAndOrganDrugList> queryOrganDrugListByOrganIdAndKeyword(final Integer organId, final String drugClass, final String keyword, final Integer status, final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<DrugListAndOrganDrugList>> action = new AbstractHibernateStatelessResultAction<QueryResult<DrugListAndOrganDrugList>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                if (status == 2) {
                    StringBuilder hql = new StringBuilder(" from DrugList d where 1=1 ");
                    if (!StringUtils.isEmpty(drugClass)) {
                        hql.append(" and d.drugClass like :drugClass");
                    }
                    List<Integer> listOrgan = new ArrayList<>();
                    if (!ObjectUtils.isEmpty(organId)){
                        OrganDTO byOrganId = BasicAPI.getService(OrganService.class).getByOrganId(organId);
                        listOrgan = BasicAPI.getService(OrganService.class).queryOrganByManageUnitList(byOrganId.getManageUnit(), listOrgan);
//                        hql.append(" and ( d.sourceOrgan is null or d.sourceOrgan in:organIds ) ");
                        hql.append(" and ( d.sourceOrgan=0 or d.sourceOrgan is null or d.sourceOrgan in:organIds ) ");
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
                    hql.append(" and d.status=1 order by d.drugId desc");
                    Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                    if (!StringUtils.isEmpty(drugClass)) {
                        countQuery.setParameter("drugClass", drugClass + "%");
                    }
                    if (!ObjectUtils.isEmpty(organId)){
                        countQuery.setParameterList("organIds",listOrgan);
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
                    if (!ObjectUtils.isEmpty(organId)){
                        query.setParameterList("organIds",listOrgan);
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
                    if (!ObjectUtils.isEmpty(list)){
                        for (DrugList drug : list) {
                            DrugListAndOrganDrugList drugListAndOrganDrugList = new DrugListAndOrganDrugList(drug, null);
                            if (!ObjectUtils.isEmpty(drug)){
                                List<OrganDrugList> byDrugIdAndOrganId = findByDrugIdAndOrganId(drug.getDrugId(), organId);
                                if (ObjectUtils.isEmpty(byDrugIdAndOrganId)){
                                    drugListAndOrganDrugList.setCanAssociated(false);
                                }else {
                                    drugListAndOrganDrugList.setCanAssociated(true);
                                }
                            }
                            result.add(drugListAndOrganDrugList);
                        }
                    }
                    setResult(new QueryResult<DrugListAndOrganDrugList>(total, query.getFirstResult(), query.getMaxResults(), result));
                } else {
                    StringBuilder hql = new StringBuilder(" from OrganDrugList a, DrugList b where a.drugId = b.drugId ");
                    if (!StringUtils.isEmpty(drugClass)) {
                        hql.append(" and b.drugClass like :drugClass");
                    }
                    Integer drugId = null;
                    if (!StringUtils.isEmpty(keyword)) {
                        try {
                            drugId = Integer.valueOf(keyword);
                        } catch (Throwable throwable) {
                            drugId = null;
                        }
                        hql.append(" and (");
                        hql.append(" a.drugName like :keyword or a.producer like :keyword or a.saleName like :keyword or b.approvalNumber like :keyword ");
                        if (drugId != null) {
                            hql.append(" or a.drugId =:drugId");
                        }
                        hql.append(")");
                    }
                    if (ObjectUtils.nullSafeEquals(status, 0)) {
                        hql.append(" and a.status = 0 and a.organId =:organId ");
                    } else if (ObjectUtils.nullSafeEquals(status, 1)) {
                        hql.append(" and a.status = 1 and a.organId =:organId ");
                    } else if (ObjectUtils.nullSafeEquals(status, -1)) {
                        hql.append(" and a.organId =:organId ");
                    } else if (ObjectUtils.nullSafeEquals(status, ALL_DRUG_FLAG)) {
                        hql.append(" and a.status in (0, 1) and a.organId =:organId ");
                    }
                    hql.append(" and b.status = 1 order by a.organDrugId desc");
                    Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                    if (!StringUtils.isEmpty(drugClass)) {
                        countQuery.setParameter("drugClass", drugClass + "%");
                    }
                    if (ObjectUtils.nullSafeEquals(status, 0) || ObjectUtils.nullSafeEquals(status, 1) || ObjectUtils.nullSafeEquals(status, -1) || ObjectUtils.nullSafeEquals(status, 9)) {
                        countQuery.setParameter("organId", organId);
                    }
                    if (drugId != null) {
                        countQuery.setParameter("drugId", drugId);
                    }
                    if (!StringUtils.isEmpty(keyword)) {
                        countQuery.setParameter("keyword", "%" + keyword + "%");
                    }
                    Long total = (Long) countQuery.uniqueResult();

                    Query query = ss.createQuery("select a " + hql.toString());
                    if (!StringUtils.isEmpty(drugClass)) {
                        query.setParameter("drugClass", drugClass + "%");
                    }
                    if (ObjectUtils.nullSafeEquals(status, 0) || ObjectUtils.nullSafeEquals(status, 1) || ObjectUtils.nullSafeEquals(status, -1) || ObjectUtils.nullSafeEquals(status, 9)) {
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
                    List<OrganDrugList> list = query.list();
                    List<DrugListAndOrganDrugList> result = new ArrayList<>();
                    DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
                    SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
                    DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                    OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
                    List<Integer> depIds = organAndDrugsepRelationDAO.findDrugsEnterpriseIdByOrganIdAndStatus(organId, 1);
                    DrugList drug;
                    DrugListAndOrganDrugList drugListAndOrganDrugList;
                    List<SaleDrugList> saleDrugLists;
                    for (OrganDrugList organDrugList : list) {
                        //查找drug
                        drug = drugListDAO.getById(organDrugList.getDrugId());
                        drugListAndOrganDrugList = new DrugListAndOrganDrugList();
                        drugListAndOrganDrugList.setDrugList(drug);
                        if (!ObjectUtils.isEmpty(drug)){
                            List<OrganDrugList> byDrugIdAndOrganId = findByDrugIdAndOrganId(drug.getDrugId(), organId);
                            if (ObjectUtils.isEmpty(byDrugIdAndOrganId)){
                                drugListAndOrganDrugList.setCanAssociated(false);
                            }else {
                                drugListAndOrganDrugList.setCanAssociated(true);
                            }
                        }
                        drugListAndOrganDrugList.setOrganDrugList(organDrugList);
                        //查找配送目录---运营平台显示机构药品目录是否可配送
                        if (CollectionUtils.isEmpty(depIds)) {
                            drugListAndOrganDrugList.setCanDrugSend(false);
                        } else {
                            saleDrugLists = saleDrugListDAO.findByDrugIdAndOrganIds(organDrugList.getDrugId(), depIds);
                            if (CollectionUtils.isEmpty(saleDrugLists)) {
                                drugListAndOrganDrugList.setCanDrugSend(false);
                            } else {
                                drugListAndOrganDrugList.setCanDrugSend(true);
                                List<DepSaleDrugInfo> depSaleDrugInfos = Lists.newArrayList();
                                for (SaleDrugList saleDrugList : saleDrugLists) {
                                    DepSaleDrugInfo info = new DepSaleDrugInfo();
                                    info.setDrugEnterpriseId(saleDrugList.getOrganId());
                                    info.setSaleDrugCode(saleDrugList.getOrganDrugCode());
                                    info.setDrugId(saleDrugList.getDrugId());
                                    DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(saleDrugList.getOrganId());
                                    if (enterprise != null) {
                                        info.setDrugEnterpriseName(enterprise.getName());
                                    } else {
                                        info.setDrugEnterpriseName("无");
                                    }
                                    depSaleDrugInfos.add(info);
                                }
                                drugListAndOrganDrugList.setDepSaleDrugInfos(depSaleDrugInfos);
                            }
                        }
                        result.add(drugListAndOrganDrugList);
                    }
                    setResult(new QueryResult<>(total, query.getFirstResult(), query.getMaxResults(), result));
                }
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
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
    public QueryResult<DrugListAndOrganDrugList> queryOrganDrugListByOrganIdAndKeywordAndProducer(final Integer organId, final String drugClass,final String keyword,final String producer,  final Integer status, final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<DrugListAndOrganDrugList>> action = new AbstractHibernateStatelessResultAction<QueryResult<DrugListAndOrganDrugList>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                if (status == 2) {
                    StringBuilder hql = new StringBuilder(" from DrugList d where 1=1 ");
                    if (!StringUtils.isEmpty(drugClass)) {
                        hql.append(" and d.drugClass like :drugClass");
                    }
                    if (!StringUtils.isEmpty(producer)) {
                        hql.append(" and d.producer like :producer");
                    }
                    List<Integer> listOrgan = new ArrayList<>();
                    if (!ObjectUtils.isEmpty(organId)){
                        OrganDTO byOrganId = BasicAPI.getService(OrganService.class).getByOrganId(organId);
                        listOrgan = BasicAPI.getService(OrganService.class).queryOrganByManageUnitList(byOrganId.getManageUnit(), listOrgan);
//                        hql.append(" and ( d.sourceOrgan is null or d.sourceOrgan in:organIds ) ");
                        hql.append(" and ( d.sourceOrgan=0 or d.sourceOrgan is null or d.sourceOrgan in:organIds ) ");
                    }
                    Integer drugId = null;
                    if (!StringUtils.isEmpty(keyword)) {
                        try {
                            drugId = Integer.valueOf(keyword);
                        } catch (Throwable throwable) {
                            drugId = null;
                        }
                        hql.append(" and (");
                        hql.append(" d.drugName like :keyword  or d.saleName like :keyword or d.approvalNumber like :keyword ");
                        if (drugId != null) {
                            hql.append(" or d.drugId =:drugId");
                        }
                        hql.append(")");
                    }
                    hql.append(" and d.status=1 order by d.drugId desc");
                    Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                    if (!StringUtils.isEmpty(drugClass)) {
                        countQuery.setParameter("drugClass", drugClass + "%");
                    }
                    if (!StringUtils.isEmpty(producer)) {
                        countQuery.setParameter("producer", "%" + producer + "%");
                    }
                    if (!ObjectUtils.isEmpty(organId)){
                        countQuery.setParameterList("organIds",listOrgan);
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
                    if (!StringUtils.isEmpty(producer)) {
                        query.setParameter("producer", "%" + producer + "%");
                    }
                    if (!ObjectUtils.isEmpty(organId)){
                        query.setParameterList("organIds",listOrgan);
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
                    if (!ObjectUtils.isEmpty(list)){
                        for (DrugList drug : list) {
                            DrugListAndOrganDrugList drugListAndOrganDrugList = new DrugListAndOrganDrugList(drug, null);
                            if (!ObjectUtils.isEmpty(drug)){
                                List<OrganDrugList> byDrugIdAndOrganId = findByDrugIdAndOrganId(drug.getDrugId(), organId);
                                if (ObjectUtils.isEmpty(byDrugIdAndOrganId)){
                                    drugListAndOrganDrugList.setCanAssociated(false);
                                }else {
                                    drugListAndOrganDrugList.setCanAssociated(true);
                                }
                            }
                            result.add(drugListAndOrganDrugList);
                        }
                    }

                    setResult(new QueryResult<DrugListAndOrganDrugList>(total, query.getFirstResult(), query.getMaxResults(), result));
                } else {
                    StringBuilder hql = new StringBuilder(" from OrganDrugList a, DrugList b where a.drugId = b.drugId ");
                    if (!StringUtils.isEmpty(drugClass)) {
                        hql.append(" and b.drugClass like :drugClass");
                    }
                    Integer drugId = null;
                    if (!StringUtils.isEmpty(keyword)) {
                        try {
                            drugId = Integer.valueOf(keyword);
                        } catch (Throwable throwable) {
                            drugId = null;
                        }
                        hql.append(" and (");
                        hql.append(" a.drugName like :keyword or a.producer like :keyword or a.saleName like :keyword or b.approvalNumber like :keyword ");
                        if (drugId != null) {
                            hql.append(" or a.drugId =:drugId");
                        }
                        hql.append(")");
                    }
                    if (ObjectUtils.nullSafeEquals(status, 0)) {
                        hql.append(" and a.status = 0 and a.organId =:organId ");
                    } else if (ObjectUtils.nullSafeEquals(status, 1)) {
                        hql.append(" and a.status = 1 and a.organId =:organId ");
                    } else if (ObjectUtils.nullSafeEquals(status, -1)) {
                        hql.append(" and a.organId =:organId ");
                    } else if (ObjectUtils.nullSafeEquals(status, ALL_DRUG_FLAG)) {
                        hql.append(" and a.status in (0, 1) and a.organId =:organId ");
                    }
                    hql.append(" and b.status = 1 order by a.organDrugId desc");
                    Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                    if (!StringUtils.isEmpty(drugClass)) {
                        countQuery.setParameter("drugClass", drugClass + "%");
                    }
                    if (ObjectUtils.nullSafeEquals(status, 0) || ObjectUtils.nullSafeEquals(status, 1) || ObjectUtils.nullSafeEquals(status, -1) || ObjectUtils.nullSafeEquals(status, 9)) {
                        countQuery.setParameter("organId", organId);
                    }
                    if (drugId != null) {
                        countQuery.setParameter("drugId", drugId);
                    }
                    if (!StringUtils.isEmpty(keyword)) {
                        countQuery.setParameter("keyword", "%" + keyword + "%");
                    }
                    Long total = (Long) countQuery.uniqueResult();

                    Query query = ss.createQuery("select a " + hql.toString());
                    if (!StringUtils.isEmpty(drugClass)) {
                        query.setParameter("drugClass", drugClass + "%");
                    }
                    if (ObjectUtils.nullSafeEquals(status, 0) || ObjectUtils.nullSafeEquals(status, 1) || ObjectUtils.nullSafeEquals(status, -1) || ObjectUtils.nullSafeEquals(status, 9)) {
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
                    List<OrganDrugList> list = query.list();
                    List<DrugListAndOrganDrugList> result = new ArrayList<>();
                    DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
                    SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
                    DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                    OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
                    List<Integer> depIds = organAndDrugsepRelationDAO.findDrugsEnterpriseIdByOrganIdAndStatus(organId, 1);
                    DrugList drug;
                    DrugListAndOrganDrugList drugListAndOrganDrugList;
                    List<SaleDrugList> saleDrugLists;
                    for (OrganDrugList organDrugList : list) {
                        //查找drug
                        drug = drugListDAO.getById(organDrugList.getDrugId());
                        drugListAndOrganDrugList = new DrugListAndOrganDrugList();
                        drugListAndOrganDrugList.setDrugList(drug);
                        if (!ObjectUtils.isEmpty(drug)){
                            List<OrganDrugList> byDrugIdAndOrganId = findByDrugIdAndOrganId(drug.getDrugId(), organId);
                            if (ObjectUtils.isEmpty(byDrugIdAndOrganId)){
                                drugListAndOrganDrugList.setCanAssociated(false);
                            }else {
                                drugListAndOrganDrugList.setCanAssociated(true);
                            }
                        }
                        drugListAndOrganDrugList.setOrganDrugList(organDrugList);
                        //查找配送目录---运营平台显示机构药品目录是否可配送
                        if (CollectionUtils.isEmpty(depIds)) {
                            drugListAndOrganDrugList.setCanDrugSend(false);
                        } else {
                            saleDrugLists = saleDrugListDAO.findByDrugIdAndOrganIds(organDrugList.getDrugId(), depIds);
                            if (CollectionUtils.isEmpty(saleDrugLists)) {
                                drugListAndOrganDrugList.setCanDrugSend(false);
                            } else {
                                drugListAndOrganDrugList.setCanDrugSend(true);
                                List<DepSaleDrugInfo> depSaleDrugInfos = Lists.newArrayList();
                                for (SaleDrugList saleDrugList : saleDrugLists) {
                                    DepSaleDrugInfo info = new DepSaleDrugInfo();
                                    info.setDrugEnterpriseId(saleDrugList.getOrganId());
                                    info.setSaleDrugCode(saleDrugList.getOrganDrugCode());
                                    info.setDrugId(saleDrugList.getDrugId());
                                    DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(saleDrugList.getOrganId());
                                    if (enterprise != null) {
                                        info.setDrugEnterpriseName(enterprise.getName());
                                    } else {
                                        info.setDrugEnterpriseName("无");
                                    }
                                    depSaleDrugInfos.add(info);
                                }
                                drugListAndOrganDrugList.setDepSaleDrugInfos(depSaleDrugInfos);
                            }
                        }
                        result.add(drugListAndOrganDrugList);
                    }
                    setResult(new QueryResult<>(total, query.getFirstResult(), query.getMaxResults(), result));
                }
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public QueryResult queryOrganDrugAndSaleForOp(final Date startTime, final Date endTime, Integer organId, String drugClass, String keyword, Integer status, final Integer isregulationDrug, final Integer type, int start, int limit, Boolean canDrugSend) {
        HibernateStatelessResultAction<QueryResult<DrugListAndOrganDrugList>> action = new AbstractHibernateStatelessResultAction<QueryResult<DrugListAndOrganDrugList>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql;
                OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
                List<Integer> depIds = organAndDrugsepRelationDAO.findDrugsEnterpriseIdByOrganIdAndStatus(organId, 1);
                if (ObjectUtils.isEmpty(startTime)) {
                    throw new DAOException(DAOException.VALUE_NEEDED, "startTime is require");
                }
                if (ObjectUtils.isEmpty(endTime)) {
                    throw new DAOException(DAOException.VALUE_NEEDED, "endTime is require");
                }
                DateTime dt = new DateTime(endTime);
                //查询机构药品目录是否配送---null的话没有是否配送的筛选条件 或者机构配置到药企为空到话 不从saledruglist里筛选
                if (canDrugSend == null || CollectionUtils.isEmpty(depIds)) {
                    hql = new StringBuilder(" from OrganDrugList a, DrugList b where a.drugId = b.drugId ");
                } else if (canDrugSend) {
                    hql = new StringBuilder(" from OrganDrugList a, DrugList b where a.drugId = b.drugId and a.drugId in (select c.drugId from SaleDrugList c where c.status =1 and c.organId in:depIds) ");
                } else {
                    hql = new StringBuilder(" from OrganDrugList a, DrugList b where a.drugId = b.drugId and a.drugId not in (select c.drugId from SaleDrugList c where c.status =1 and c.organId in:depIds and c.drugId is not null) ");
                }
                if (!StringUtils.isEmpty(drugClass)) {
                    hql.append(" and b.drugClass like :drugClass");
                }
                if (!ObjectUtils.isEmpty(type)) {
                    hql.append(" and b.drugType =:drugType ");
                }
                Integer drugId = null;
                if (!StringUtils.isEmpty(keyword)) {
                    try {
                        drugId = Integer.valueOf(keyword);
                    } catch (Throwable throwable) {
                        drugId = null;
                    }
                    hql.append(" and (");
                    hql.append(" a.drugName like :keyword or a.producer like :keyword or a.saleName like :keyword or b.approvalNumber like :keyword  or a.organDrugCode like :keyword ");
                    if (drugId != null) {
                        hql.append(" or a.drugId =:drugId");
                    }
                    hql.append(")");
                }
                if (!ObjectUtils.isEmpty(startTime) && !ObjectUtils.isEmpty(endTime)) {
                    hql.append(" and a.createDt>=:startTime and a.createDt<=:endTime ");
                }
                if (ObjectUtils.nullSafeEquals(status, 0)) {
                    hql.append(" and a.status = 0 and a.organId =:organId ");
                } else if (ObjectUtils.nullSafeEquals(status, 1)) {
                    hql.append(" and a.status = 1 and a.organId =:organId ");
                } else if (ObjectUtils.nullSafeEquals(status, ALL_DRUG_FLAG)) {
                    hql.append(" and a.status in (0, 1) and a.organId =:organId ");
                } else {
                    hql.append(" and a.organId =:organId ");
                }
                if (isregulationDrug != null) {
                    if (isregulationDrug == 1) {
                        hql.append(" and a.regulationDrugCode is not null and a.regulationDrugCode <>''  ");
                    }
                    if (isregulationDrug == 0) {
                        hql.append(" and a.regulationDrugCode is null ");
                    }
                }
                hql.append(" and b.status = 1 order by a.organDrugId desc");
                Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                if (!StringUtils.isEmpty(drugClass)) {
                    countQuery.setParameter("drugClass", drugClass + "%");
                }
                if (!ObjectUtils.isEmpty(type)) {
                    countQuery.setParameter("drugType", type);
                }
                //if (ObjectUtils.nullSafeEquals(status, 0) || ObjectUtils.nullSafeEquals(status, 1) || ObjectUtils.nullSafeEquals(status, -1) || ObjectUtils.nullSafeEquals(status, 9)) {
                countQuery.setParameter("organId", organId);
                //}
                if (drugId != null) {
                    countQuery.setParameter("drugId", drugId);
                }
                if (!ObjectUtils.isEmpty(startTime)) {
                    countQuery.setParameter("startTime", startTime);
                }
                if (!ObjectUtils.isEmpty(endTime)) {
                    countQuery.setParameter("endTime", dt.plusDays(1).toDate());
                }
                if (canDrugSend != null && CollectionUtils.isNotEmpty(depIds)) {
                    countQuery.setParameterList("depIds", depIds);
                }
                if (!StringUtils.isEmpty(keyword)) {
                    countQuery.setParameter("keyword", "%" + keyword + "%");
                }
                Long total = (Long) countQuery.uniqueResult();

                Query query = ss.createQuery("select a " + hql.toString());
                if (!StringUtils.isEmpty(drugClass)) {
                    query.setParameter("drugClass", drugClass + "%");
                }
                //if (ObjectUtils.nullSafeEquals(status, 0) || ObjectUtils.nullSafeEquals(status, 1) || ObjectUtils.nullSafeEquals(status, -1) || ObjectUtils.nullSafeEquals(status, 9)) {
                query.setParameter("organId", organId);
                //}
                if (drugId != null) {
                    query.setParameter("drugId", drugId);
                }
                if (!ObjectUtils.isEmpty(startTime)) {
                    query.setParameter("startTime", startTime);
                }
                if (!ObjectUtils.isEmpty(endTime)) {
                    query.setParameter("endTime", dt.plusDays(1).toDate());
                }
                if (!StringUtils.isEmpty(keyword)) {
                    query.setParameter("keyword", "%" + keyword + "%");
                }
                if (!ObjectUtils.isEmpty(type)) {
                    query.setParameter("drugType", type);
                }
                if (canDrugSend != null && CollectionUtils.isNotEmpty(depIds)) {
                    query.setParameterList("depIds", depIds);
                }
                query.setFirstResult(start);
                query.setMaxResults(limit);
                List<OrganDrugList> list = query.list();
                List<DrugListAndOrganDrugList> result = new ArrayList<>();
                DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
                SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
                DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);

                DrugList drug;
                DrugListAndOrganDrugList drugListAndOrganDrugList;
                List<SaleDrugList> saleDrugLists;
                if (CollectionUtils.isEmpty(depIds) && canDrugSend != null && canDrugSend) {
                    total = 0L;
                } else {
                    for (OrganDrugList organDrugList : list) {
                        //查找drug
                        drug = drugListDAO.getById(organDrugList.getDrugId());
                        drugListAndOrganDrugList = new DrugListAndOrganDrugList();
                        drugListAndOrganDrugList.setDrugList(drug);
                        if (!ObjectUtils.isEmpty(drug)){
                            List<OrganDrugList> byDrugIdAndOrganId = findByDrugIdAndOrganId(drug.getDrugId(), organId);
                            if (ObjectUtils.isEmpty(byDrugIdAndOrganId)){
                                drugListAndOrganDrugList.setCanAssociated(false);
                            }else {
                                drugListAndOrganDrugList.setCanAssociated(true);
                            }
                        }
                        drugListAndOrganDrugList.setOrganDrugList(organDrugList);
                        //查找配送目录---运营平台显示机构药品目录是否可配送
                        if (CollectionUtils.isEmpty(depIds)) {
                            drugListAndOrganDrugList.setCanDrugSend(false);
                        } else {
                            saleDrugLists = saleDrugListDAO.findByDrugIdAndOrganIds(organDrugList.getDrugId(), depIds);
//                            //支持配送这里不能为false
//                            if (CollectionUtils.isEmpty(saleDrugLists)&& canDrugSend != null&&canDrugSend) {
//                                continue;
//                            }
                            if (CollectionUtils.isEmpty(saleDrugLists)) {
                                drugListAndOrganDrugList.setCanDrugSend(false);
                            } else {
                                drugListAndOrganDrugList.setCanDrugSend(true);
                                List<DepSaleDrugInfo> depSaleDrugInfos = Lists.newArrayList();
                                for (SaleDrugList saleDrugList : saleDrugLists) {
                                    DepSaleDrugInfo info = new DepSaleDrugInfo();
                                    info.setDrugEnterpriseId(saleDrugList.getOrganId());
                                    info.setSaleDrugCode(saleDrugList.getOrganDrugCode());
                                    info.setDrugId(saleDrugList.getDrugId());
                                    DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(saleDrugList.getOrganId());
                                    if (enterprise != null) {
                                        info.setDrugEnterpriseName(enterprise.getName());
                                    } else {
                                        info.setDrugEnterpriseName("无");
                                    }
                                    depSaleDrugInfos.add(info);
                                }
                                drugListAndOrganDrugList.setDepSaleDrugInfos(depSaleDrugInfos);
                            }
                        }
                        result.add(drugListAndOrganDrugList);
                    }
                }
                setResult(new QueryResult<>(total, query.getFirstResult(), query.getMaxResults(), result));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public List<DepSaleDrugInfo> queryDepSaleDrugInfosByDrugId(final Integer organId,final Integer drugId) {
        OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        List<Integer> depIds = organAndDrugsepRelationDAO.findDrugsEnterpriseIdByOrganIdAndStatus(organId, 1);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<SaleDrugList> saleDrugLists;
        List<DepSaleDrugInfo> depSaleDrugInfos = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(depIds)) {
            saleDrugLists = saleDrugListDAO.findByDrugIdAndOrganIds(drugId, depIds);
            for (SaleDrugList saleDrugList : saleDrugLists) {
                DepSaleDrugInfo info = new DepSaleDrugInfo();
                info.setDrugEnterpriseId(saleDrugList.getOrganId());
                info.setSaleDrugCode(saleDrugList.getOrganDrugCode());
                info.setDrugId(saleDrugList.getDrugId());
                DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(saleDrugList.getOrganId());
                if (enterprise != null) {
                    info.setDrugEnterpriseName(enterprise.getName());
                } else {
                    info.setDrugEnterpriseName("无");
                }
                depSaleDrugInfos.add(info);
            }
        }
        return depSaleDrugInfos;
    }

    /**
     * 根据机构id获取数量
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "select count(*) from OrganDrugList where organId=:organId")
    public abstract long getCountByOrganId(@DAOParam("organId") int organId);

    /**
     * 更新机构id
     *
     * @param newOrganId
     * @param oldOrganId
     */
    @DAOMethod(sql = "update OrganDrugList set organId=:newOrganId where organId=:oldOrganId")
    public abstract void updateOrganIdByOrganId(@DAOParam("newOrganId") int newOrganId, @DAOParam("oldOrganId") int oldOrganId);

    /**
     * 根据药品编码列表更新状态
     *
     * @param organDrugCodeList
     * @param status
     */
    @DAOMethod(sql = "update OrganDrugList set status=:status where organDrugCode in :organDrugCodeList")
    public abstract void updateStatusByOrganDrugCode(@DAOParam("organDrugCodeList") List<String> organDrugCodeList, @DAOParam("status") int status);

    public List<Integer> queryOrganCanRecipe(final List<Integer> organIds, final Integer drugId) {
        HibernateStatelessResultAction<List<Integer>> action = new AbstractHibernateStatelessResultAction<List<Integer>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select DISTINCT o.organId from  DrugList d, OrganDrugList o where " + "o.drugId = d.drugId and d.status = 1 and o.status = 1 ");
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

    public Boolean updateOrganDrugListByOrganIdAndOrganDrugCode(final int organId, final String organDrugCode, final Map<String, ?> changeAttr) {
        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update OrganDrugList set lastModify=current_timestamp() ");
                if (null != changeAttr && !changeAttr.isEmpty()) {
                    for (String key : changeAttr.keySet()) {
                        hql.append("," + key + "=:" + key);
                    }
                }
                hql.append(" where organId=:organId and organDrugCode=:organDrugCode");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("organId", organId);
                q.setParameter("organDrugCode", organDrugCode);
                if (null != changeAttr && !changeAttr.isEmpty()) {
                    for (String key : changeAttr.keySet()) {
                        q.setParameter(key, changeAttr.get(key));
                    }
                }
                int flag = q.executeUpdate();
                setResult(flag == 1);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 分页查询所有医院药品数据
     *
     * @param start
     * @param limit
     * @return
     */
    @DAOMethod(sql = "select a from OrganDrugList a, DrugList b where a.drugId=b.drugId and a.status=1", limit = 0)
    public abstract List<OrganDrugList> findAllForPage(@DAOParam(pageStart = true) int start, @DAOParam(pageLimit = true) int limit);

    /**
     * 统计医院药品可用数量
     *
     * @return
     */
    @DAOMethod(sql = "select count(*) from OrganDrugList a, DrugList b where a.drugId=b.drugId and a.status=1")
    public abstract long getUsefulTotal();

    /**
     * 统计医院药品数量
     *
     * @return
     */
    @DAOMethod(sql = "select count(*) from OrganDrugList where organId=:organId")
    public abstract long getTotal(@DAOParam("organId") Integer organId);

    @DAOMethod(sql = "from OrganDrugList where organDrugId in (:organDrugId) ", limit = 0)
    public abstract List<OrganDrugList> findByOrganDrugIds(@DAOParam("organDrugId") List<Integer> organDrugId);

    @DAOMethod(sql = "select organDrugId from OrganDrugList where drugId in (:drugId) ", limit = 0)
    public abstract List<Integer> findOrganDrugIdByDrugIds(@DAOParam("drugId") List<Integer> drugId);

    public Boolean updatePharmacy(final int organId, final String pharmacy) {
        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update OrganDrugList set pharmacyName=:pharmacy");
                hql.append(" where organId=:organId");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("organId", organId);
                q.setParameter("pharmacy", pharmacy);
                int flag = q.executeUpdate();
                setResult(flag == 1);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据机构id删除
     *
     * @param organId
     */
    @DAOMethod(sql = " delete from OrganDrugList where organId =:organId")
    public abstract void deleteByOrganId(@DAOParam("organId") Integer organId);

    /**
     * 根据id删除
     *
     * @param id
     */
    @DAOMethod(sql = " delete from OrganDrugList where id =:id")
    public abstract void deleteById(@DAOParam("id") Integer id);

    /**
     * 根据机构id获取机构药品
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId", limit = 0)
    public abstract List<OrganDrugList> findOrganDrugByOrganId(@DAOParam("organId") int organId);

    /**
     * 根据机构id 和配送药企ID 获取机构药品
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and drugsEnterpriseIds like:drugsEnterpriseIds    ", limit = 0)
    public abstract List<OrganDrugList> findOrganDrugByOrganIdAndDrugsEnterpriseId(@DAOParam("organId") int organId,@DAOParam("drugsEnterpriseIds") String drugsEnterpriseIds);

    public boolean updateData(final OrganDrugList drug) {
        final HashMap<String, Object> map = BeanUtils.map(drug, HashMap.class);
        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update OrganDrugList set lastModify=current_timestamp() ");
                for (String key : map.keySet()) {
                    if (!key.endsWith("Text")) {
                        hql.append("," + key + "=:" + key);
                    }
                }
                hql.append(" where organDrugCode=:organDrugCode and organId=:organId");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("organDrugCode", drug.getOrganDrugCode());
                q.setParameter("organId", drug.getOrganId());
                for (String key : map.keySet()) {
                    if (!key.endsWith("Text")) {
                        q.setParameter(key, map.get(key));
                    }
                }
                int flag = q.executeUpdate();
                setResult(flag == 1);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public Boolean updateOrganDrugById(final int organDrugId, final Map<String, ?> changeAttr) {
        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update DrugListMatch set lastModify=current_timestamp() ");
                if (null != changeAttr && !changeAttr.isEmpty()) {
                    for (String key : changeAttr.keySet()) {
                        hql.append("," + key + "=:" + key);
                    }
                }
                hql.append(" where OrganDrugId=:organDrugId");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("organDrugId", organDrugId);
                if (null != changeAttr && !changeAttr.isEmpty()) {
                    for (String key : changeAttr.keySet()) {
                        q.setParameter(key, changeAttr.get(key));
                    }
                }
                int flag = q.executeUpdate();
                setResult(flag == 1);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 通过organId和创建时间获取
     *
     * @param organId  机构Id
     * @param createDt 创建时间
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and createDt =:createDt and status =1", limit = 0)
    public abstract List<OrganDrugList> findByOrganIdAndCreateDt(@DAOParam("organId") int organId, @DAOParam("createDt") Date createDt);

    /**
     * 通过organId和创建时间获取
     *
     * @param organId       机构Id
     * @param drugId        药品id
     * @param organDrugCode 药品code
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and drugId =:drugId and organDrugCode =:organDrugCode and status = 1", limit = 0)
    public abstract List<OrganDrugList> findByOrganIdAndDrugIdAndOrganDrugCode(@DAOParam("organId") int organId, @DAOParam("drugId") int drugId, @DAOParam("organDrugCode") String organDrugCode);

    /**
     * 通过organId获取本机构 未关联监管平台数据
     *
     * @param organId       机构Id
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and status = 1  and regulationDrugCode is null ", limit = 0)
    public abstract List<OrganDrugList> findByOrganIdAndRegulationDrugCode(@DAOParam("organId") int organId);


    /**
     * 通过organId和创建时间获取
     *
     * @param organId       机构Id
     * @param drugId        药品id
     * @param organDrugCode 药品code
     * @return
     */
    @DAOMethod(sql = "from OrganDrugList where organId=:organId and drugId =:drugId and organDrugCode =:organDrugCode and status =:status", limit = 0)
    public abstract List<OrganDrugList> findByOrganIdAndDrugIdAndOrganDrugCodeAndStatus(@DAOParam("organId") int organId, @DAOParam("drugId") int drugId, @DAOParam("organDrugCode") String organDrugCode, @DAOParam("status") Integer status);

    @DAOMethod(sql = "from OrganDrugList ")
    public abstract List<OrganDrugList> findOrganDrug(@DAOParam(pageStart = true) int start, @DAOParam(pageLimit = true) int limit);

    @DAOMethod(sql = "select DISTINCT organId from OrganDrugList ", limit = 0)
    public abstract List<Integer> findOrganIds();

    @DAOMethod(sql = "update OrganDrugList set usingRateId=:newUsingRate where usingRate=:oldUsingRate and organId=:organId")
    public abstract void updateUsingRateByUsingRate(@DAOParam("organId") Integer organId, @DAOParam("oldUsingRate") String oldUsingRate, @DAOParam("newUsingRate") String newUsingRate);

    @DAOMethod(sql = "update OrganDrugList set usePathwaysId=:newUsePathways where usePathways=:oldUsePathways and organId=:organId")
    public abstract void updateUsePathwaysByUsePathways(@DAOParam("organId") Integer organId, @DAOParam("oldUsePathways") String oldUsePathways, @DAOParam("newUsePathways") String newUsePathways);

    public List<Map<String, Object>> findAllUsingRate() {
        HibernateStatelessResultAction<List<Map<String, Object>>> action = new AbstractHibernateStatelessResultAction<List<Map<String, Object>>>() {

            @Override
            public void execute(StatelessSession ss) throws Exception {

                StringBuilder hql = new StringBuilder("select DISTINCT organId,usingRate from OrganDrugList WHERE organId > 0 AND usingRate != '' AND usingRate is NOT NULL ORDER BY organId");
                Query query = ss.createQuery(hql.toString());
                List<Object[]> objects = query.list();
                List<Map<String, Object>> result = Lists.newArrayList();
                if (!CollectionUtils.isEmpty(objects)) {
                    for (Object[] objects1 : objects) {
                        Integer organId = (Integer) objects1[0];
                        String usingRate = (String) objects1[1];
                        Map<String, Object> map = new HashedMap();
                        map.put("organId", organId);
                        map.put("usingRate", usingRate);
                        result.add(map);
                    }
                }
                setResult(result);
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    public List<Map<String, Object>> findAllUsePathways() {
        HibernateStatelessResultAction<List<Map<String, Object>>> action = new AbstractHibernateStatelessResultAction<List<Map<String, Object>>>() {

            @Override
            public void execute(StatelessSession ss) throws Exception {

                StringBuilder hql = new StringBuilder("select DISTINCT organId,usePathways from OrganDrugList WHERE organId > 0 AND usePathways != '' AND usePathways is NOT NULL ORDER BY organId");
                Query query = ss.createQuery(hql.toString());
                List<Object[]> objects = query.list();
                List<Map<String, Object>> result = Lists.newArrayList();
                if (!CollectionUtils.isEmpty(objects)) {
                    for (Object[] objects1 : objects) {
                        Integer organId = (Integer) objects1[0];
                        String usePathways = (String) objects1[1];
                        Map<String, Object> map = new HashedMap();
                        map.put("organId", organId);
                        map.put("usePathways", usePathways);
                        result.add(map);
                    }
                }
                setResult(result);
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    /**
     * 药品名称模糊查询
     *
     * @param drugName 药品名称
     * @return List<OrganDrugList>
     * @author luf
     */
    public List<OrganDrugList> findByDrugNameLikeNew(final Integer organId, final String drugName, final int start, final int limit) {
        HibernateStatelessResultAction<List<OrganDrugList>> action = new AbstractHibernateStatelessResultAction<List<OrganDrugList>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select a from OrganDrugList a, DrugList b where a.drugId=b.drugId ");
                if (organId != null) {
                    hql.append("and a.organId = :organId ");
                }
                hql.append("and a.status=1 and b.status =1 and (a.drugName like :drugName or a.saleName like :drugName) order by a.organDrugId desc");
                Query q = ss.createQuery(hql.toString());
                if (organId != null) {
                    q.setParameter("organId", organId);
                }
                q.setParameter("drugName", "%" + drugName + "%");
                q.setFirstResult(start);
                q.setMaxResults(limit);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    /**
     * 根据drugId查询所有机构药品数量
     *
     * @param drugId 平台药品id
     * @return 药品数量
     */
    @DAOMethod(sql = "select count(organDrugId) from OrganDrugList where drugId=:drugId  ", limit = 0)
    public abstract Long getCountByDrugId(@DAOParam("drugId") int drugId);


    /**
     * 根据drugId查询所有机构药品数量
     *
     * @param organDrugCode 机构药品编码
     * @return 药品数量
     */
    @DAOMethod(sql = "select count(*) from OrganDrugList where organDrugCode=:organDrugCode  ", limit = 0)
    public abstract Long getCountByOrganDrugCode(@DAOParam("organDrugCode") String organDrugCode);

    /**
     * 根据机构Id和药品编码查询默认嘱托
     *
     * @param organId
     * @param OrganDrugCode
     * @return
     */
    @DAOMethod(sql = "select drugEntrust from OrganDrugList where organId=:organId and OrganDrugCode =:OrganDrugCode")
    public abstract String getDrugEntrustByOrganDrugCodeAndOrganId(@DAOParam("organId") Integer organId, @DAOParam("OrganDrugCode") String OrganDrugCode);

    /**
     * 查询单个药品药品信息的默认嘱托数据
     *
     * @param organDrugCode
     * @return
     */
    @DAOMethod(sql = " select drugEntrust from OrganDrugList where OrganDrugCode=:OrganDrugCode and status = 1 and OrganID=:OrganID")
    public abstract String getDrugEntrustById(@DAOParam("OrganDrugCode") String organDrugCode, @DAOParam("OrganID") int OrganID);


    /**
     * 通过处方id及机构id获取药品是否支持处方下载
     *
     * @param recipeId
     * @param organId
     * @return
     */
    public Integer countIsSupperDownloadRecipe(int organId, Integer recipeId) {
        HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select count(1) from OrganDrugList where OrganID=:organId and drugId in(select drugId  FROM Recipedetail where recipeId=:recipeId) and supportDownloadPrescriptionPad=0 ");
                Query query = ss.createQuery(String.valueOf(hql));
                query.setParameter("organId", organId);
                query.setParameter("recipeId", recipeId);
                Number number = (Number) query.uniqueResult();
                setResult(number.intValue());
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    /**
     * 通过药品id及机构id获取药品是否支持处方下载
     *
     * @param drugIds
     * @param organId
     * @return
     */
    public Integer countIsSupperDownloadRecipeByDrugIds(@DAOParam("organId") Integer organId, @DAOParam("drugIds") Set<Integer> drugIds) {
        HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select count(1) from OrganDrugList where OrganID=:organId and drugId in:drugIds and supportDownloadPrescriptionPad=0 ");
                Query query = ss.createQuery(String.valueOf(hql));
                query.setParameter("organId", organId);
                query.setParameterList("drugIds", drugIds);
                Number number = (Number) query.uniqueResult();
                setResult(number.intValue());
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }


}
