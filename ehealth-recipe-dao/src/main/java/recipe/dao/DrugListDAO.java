package recipe.dao;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.DrugSources;
import com.ngari.recipe.entity.OrganDrugList;
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
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import recipe.util.DateConversion;

import java.util.*;

/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 */
@RpcSupportDAO
public abstract class DrugListDAO extends HibernateSupportDelegateDAO<DrugList>
        implements DBDictionaryItemLoader<DrugList> {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DrugListDAO.class);


    public DrugListDAO() {
        super();
        this.setEntityName(DrugList.class.getName());
        this.setKeyField("drugId");
    }

    /**
     * 根据药品Id获取药品记录
     *
     * @param drugId 药品id
     * @return
     * @author yaozh
     */
    public DrugList getById(final int drugId) {
        HibernateStatelessResultAction<DrugList> action = new AbstractHibernateStatelessResultAction<DrugList>() {
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder("from DrugList where drugId=:drugId");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("drugId", drugId);
                Object dbObj = q.uniqueResult();
                if (dbObj instanceof DrugList) {
                    DrugList drug = (DrugList) dbObj;
                    setDrugDefaultInfo(drug);
                    setResult(drug);
                } else {
                    setResult(null);
                }
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    /**
     * 药品目录搜索服务
     *
     * @param drugName 药品名称、品牌名
     * @param start    分页起始位置
     * @param limit    每页限制条数
     * @return List<DrugList>
     * zhongzx 加 organId,drugType
     * @author luf
     * <p>
     * ---旧方法搜索 不建议使用 新搜索searchDrugListWithES
     */
    public List<DrugList> findDrugListsByNameOrCode(final Integer organId, final Integer drugType, final String drugName,
                                                    final Integer start, final Integer limit, final List<Integer> ids) {
        HibernateStatelessResultAction<List<DrugList>> action = new AbstractHibernateStatelessResultAction<List<DrugList>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder("select d From OrganDrugList o,DrugList d where d.drugId = o.drugId "
                        + "and d.status=1 and o.status=1 "
                        + "and (d.drugName like :drugName or d.pyCode like :drugName or d.saleName like :drugName");
                if (null != ids && ids.size() != 0) {
                    hql.append(" or d.drugId in (:ids)) ");
                } else {
                    hql.append(") ");
                }
                if (organId != null) {
                    hql.append("and o.organId=:organId ");
                }
                if (drugType != null) {
                    hql.append("and d.drugType=:drugType ");
                }
                hql.append("order by d.pyCode");
                Query q = ss.createQuery(hql.toString());
                if (null != ids && ids.size() != 0) {
                    q.setParameterList("ids", ids);
                }
                if (null != organId) {
                    q.setParameter("organId", organId);
                }
                if (null != drugType) {
                    q.setParameter("drugType", drugType);
                }
                q.setParameter("drugName", "%" + drugName + "%");
                if (null != start && null != limit) {
                    q.setFirstResult(start);
                    q.setMaxResults(limit);
                }
                List<DrugList> drugListList = q.list();
                for (DrugList drug : drugListList) {
                    setDrugDefaultInfo(drug);
                }
                setResult(drugListList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据别名进行查询 药品目录
     *
     * @param organId
     * @param drugType
     * @param alias
     * @return ----旧方法搜索 不建议使用 新搜索searchDrugListWithES
     */
    public List<Integer> findDrugListsByAlias(final Integer organId, final Integer drugType, final String alias) {
        HibernateStatelessResultAction<List<DrugList>> action = new AbstractHibernateStatelessResultAction<List<DrugList>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder("select d From OrganDrugList o,DrugList d,DrugAlias a "
                        + "where d.drugId = o.drugId and a.drugId = d.drugId "
                        + "and (a.drugName like :alias or a.pyCode like :alias) "
                        + "and d.status=1 and o.status=1 ");
                if (null != organId) {
                    hql.append("and o.organId=:organId ");
                }
                if (null != drugType) {
                    hql.append("and d.drugType=:drugType ");
                }
                hql.append("order by d.pyCode");
                Query q = ss.createQuery(hql.toString());
                if (null != organId) {
                    q.setParameter("organId", organId);
                }
                if (null != drugType) {
                    q.setParameter("drugType", drugType);
                }
                q.setParameter("alias", "%" + alias + "%");
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        List<DrugList> drugList = action.getResult();
        //取出根据别名查询得到的drugId 作为去DrugList表中查询的入参
        List<Integer> idList = new ArrayList<>();
        for (DrugList d : drugList) {
            idList.add(d.getDrugId());
        }
        return idList;
    }


    /**
     * 根据机构（药品分类）查询药品目录列表
     *
     * @param organId   医疗机构代码
     * @param drugClass 药品分类
     * @param start     分页起始位置
     * @param limit     每页限制条数
     * @return List<DrugList>
     * zhongzx 加 drugType
     * @author luf
     */
    public List<DrugList> findDrugListsByOrganOrDrugClass(
            final Integer organId, final Integer drugType, final String drugClass, final Integer start,
            final Integer limit) {
        HibernateStatelessResultAction<List<DrugList>> action = new AbstractHibernateStatelessResultAction<List<DrugList>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                StringBuffer hql = new StringBuffer(
                        "select d From DrugList d, OrganDrugList o where d.drugId=o.drugId and d.status=1 and o.status=1 ");
                if (!StringUtils.isEmpty(drugClass)) {
                    hql.append("and d.drugClass=:drugClass ");
                }
                if (null != drugType) {
                    hql.append("and d.drugType=:drugType ");
                }
                if (null != organId) {
                    hql.append("and o.organId=:organId ");
                }
                hql.append("order by allPyCode");
                Query q = ss.createQuery(hql.toString());
                if (!StringUtils.isEmpty(drugClass)) {
                    q.setParameter("drugClass", drugClass);
                }
                if (null != drugType) {
                    q.setParameter("drugType", drugType);
                }
                if (null != organId) {
                    q.setParameter("organId", organId);
                }
                if (null != start && null != limit) {
                    q.setFirstResult(start);
                    q.setMaxResults(limit);
                }
                List<DrugList> drugListList = q.list();
                for (DrugList drug : drugListList) {
                    setDrugDefaultInfo(drug);
                }
                setResult(drugListList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    /**
     * 常用药品列表服务(start,limit)
     *
     * @param doctor 开方医生
     * @param start  分页开始位置
     * @param limit  每页限制条数
     * @return List<DrugList>
     * zhongzx 加 organId,drugType
     * @author luf
     */
    public List<OrganDrugList> findCommonDrugListsWithPage(final int doctor, final int organId, final int drugType,final String pharmacyId,
                                                      final int start, final int limit) {
        HibernateStatelessResultAction<List<OrganDrugList>> action = new AbstractHibernateStatelessResultAction<List<OrganDrugList>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                String hql = "select o From DrugList a, Recipe b,Recipedetail c, OrganDrugList o where "
                        + "b.doctor=:doctor and a.status=1 and a.drugId=c.drugId and b.clinicOrgan=:organId "
                        + "and a.drugType=:drugType and b.createDate>=:halfYear and o.drugId=a.drugId and o.organId=:organId and o.status=1 "
                        + "and b.recipeId=c.recipeId group by c.drugId order by count(*) desc";
                Query q = ss.createQuery(hql);
                q.setParameter("doctor", doctor);
                q.setParameter("organId", organId);
                q.setParameter("drugType", drugType);
                q.setParameter("halfYear", DateConversion.getMonthsAgo(6));
                q.setFirstResult(start);
                q.setMaxResults(limit);
                List<OrganDrugList> drugListList = q.list();
                //X药品里有A药房B药房，医生开了个A药房的X，选了B药房的常用药里也要有X药出来
                if (StringUtils.isNotEmpty(pharmacyId)){
                    Iterator<OrganDrugList> iterator = drugListList.iterator();
                    while(iterator.hasNext()){
                        boolean canAddDrug = false;
                        OrganDrugList drug = iterator.next();
                        if (StringUtils.isNotEmpty(drug.getPharmacy())){
                            //过滤掉不在此药房内的药
                            List<String> pharmacyIds = Splitter.on(",").splitToList(drug.getPharmacy());
                            if (pharmacyIds.contains(pharmacyId)){
                                canAddDrug =true;
                            }
                        }
                        if (!canAddDrug){
                            iterator.remove();
                        }
                    }
                }
                setResult(drugListList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    /**
     * 去数据库查询对应机构所有有效药品对应的分类
     * zhongzx
     *
     * @param organId
     * @param drugType
     * @return
     */
    public List<DrugList> findDrugClassByDrugList(final Integer organId, final Integer drugType, final String parentKey, final Integer start, final Integer limit) {

        //查询出所有有效药品 根据药品分类drugClass进行分组
        HibernateStatelessResultAction<List<DrugList>> action = new AbstractHibernateStatelessResultAction<List<DrugList>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select d From DrugList d,OrganDrugList o where "
                        + "d.status=1 and o.status=1 and d.drugId=o.drugId ");
                if (!StringUtils.isEmpty(parentKey)) {
                    hql.append("and d.drugClass like :parentKey ");
                }
                if (null != organId) {
                    hql.append("and o.organId=:organId ");
                }
                if (null != drugType) {
                    hql.append("and d.drugType=:drugType ");
                }
                hql.append("group by drugClass order by count(*) desc");
                Query q = ss.createQuery(hql.toString());
                if (!StringUtils.isEmpty(parentKey)) {
                    q.setParameter("parentKey", parentKey + "%");
                }
                if (null != organId) {
                    q.setParameter("organId", organId);
                }
                if (null != drugType) {
                    q.setParameter("drugType", drugType);
                }
                if (null != start && null != limit) {
                    q.setFirstResult(start);
                    q.setMaxResults(limit);
                }
                List<DrugList> drugListList = q.list();
                for (DrugList drug : drugListList) {
                    setDrugDefaultInfo(drug);
                }
                setResult(drugListList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public void check(final String drugName, final String drugSpec, final String unit, final String drugForm, final  String producer){
        if (StringUtils.isEmpty(drugSpec)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugSpec不能为空!");
        }
        if (StringUtils.isEmpty(drugName)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugName不能为空!");
        }
        if (StringUtils.isEmpty(unit)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "unit不能为空!");
        }
       /* if (StringUtils.isEmpty(drugForm)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugForm不能为空!");
        }*/
        if (StringUtils.isEmpty(producer)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "producer不能为空!");
        }
    }

    /**
     * 自动匹配搜索
     * renfuhao
     *
     * @return
     */
    public List<DrugList> findDrugMatchAutomatic(final String drugName, final String saleName, final String drugSpec, final String unit, final String drugForm, final  String producer) {
        check(drugName,drugSpec, unit, drugForm, producer);
        //查询出所有有效药品 根据药品分类drugClass进行分组
        HibernateStatelessResultAction<List<DrugList>> action = new AbstractHibernateStatelessResultAction<List<DrugList>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select d From DrugList d where sourceOrgan is null  "
                        + " and d.status=1  ");

                if (!StringUtils.isEmpty(saleName)) {
                    hql.append("and d.drugName =:drugName and d.saleName =:saleName ");
                }else {
                    hql.append("and d.drugName =:drugName ");
                }
                hql.append("and d.drugSpec=:drugSpec  and d.unit=:unit  and d.producer=:producer ");
                if (!StringUtils.isEmpty(drugForm)) {
                    hql.append(" and d.drugForm=:drugForm ");
                }
                Query q = ss.createQuery(hql.toString());
                if (!StringUtils.isEmpty(saleName)) {
                    q.setParameter("saleName", saleName );
                }
                q.setParameter("drugName", drugName );
                q.setParameter("drugSpec", drugSpec);
                q.setParameter("unit", unit);
                if (!StringUtils.isEmpty(drugForm)) {
                    q.setParameter("drugForm", drugForm);
                }
                q.setParameter("producer", producer);
                List<DrugList> drugListList = q.list();
                setResult(drugListList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 自动匹配搜索 机构
     * renfuhao
     *
     * @return
     */
    public List<DrugList> findDrugMatchAutomaticOrgan(final String drugName, final String saleName, final String drugSpec, final String unit, final String drugForm, final  String producer,final  Integer sourceOrgan) {
        check(drugName,drugSpec, unit, drugForm, producer);
        //查询出所有有效药品 根据药品分类drugClass进行分组
        HibernateStatelessResultAction<List<DrugList>> action = new AbstractHibernateStatelessResultAction<List<DrugList>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select d From DrugList d where   "
                        + "  d.status=1  ");

                if (!StringUtils.isEmpty(saleName)) {
                    hql.append("and d.drugName =:drugName and d.saleName =:saleName ");
                }else {
                    hql.append("and d.drugName =:drugName ");
                }
                hql.append("and d.drugSpec=:drugSpec  and d.unit=:unit  and d.producer=:producer ");
                if (!StringUtils.isEmpty(drugForm)) {
                    hql.append(" and d.drugForm=:drugForm ");
                }

                if (!ObjectUtils.isEmpty(sourceOrgan)) {
                    hql.append(" and d.sourceOrgan=:sourceOrgan ");
                }
                Query q = ss.createQuery(hql.toString());
                if (!StringUtils.isEmpty(saleName)) {
                    q.setParameter("saleName", saleName );
                }
                q.setParameter("drugName", drugName );
                q.setParameter("drugSpec", drugSpec);
                q.setParameter("unit", unit);
                if (!StringUtils.isEmpty(drugForm)) {
                    q.setParameter("drugForm", drugForm);
                }
                if (!ObjectUtils.isEmpty(sourceOrgan)) {
                    q.setParameter("sourceOrgan", sourceOrgan);
                }
                q.setParameter("producer", producer);
                List<DrugList> drugListList = q.list();
                setResult(drugListList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    /**
     * 供 employmentdao-findEffEmpWithDrug 调用
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "select count(*) From DrugList d,OrganDrugList o where o.organId=:organId and d.status=1 and o.status=1 and d.drugId=o.drugId")
    public abstract Long getEffectiveDrugNum(@DAOParam("organId") int organId);

    /**
     * 获取某种药品类型的可用数量
     *
     * @param organId
     * @param drugType
     * @return
     */
    @DAOMethod(sql = "select count(*) From DrugList d,OrganDrugList o where o.organId=:organId and d.status=1 and o.status=1 and d.drugId=o.drugId and d.drugType=:drugType")
    public abstract Long getSpecifyNum(@DAOParam("organId") int organId, @DAOParam("drugType") int drugType);

    /**
     * ps:调用该方法不会设置用药频次等默认值
     *
     * @param drugIds
     * @return
     */
    @DAOMethod(sql = "from DrugList where drugId in (:drugIds) and status=1",limit=0)
    public abstract List<DrugList> findByDrugIds(@DAOParam("drugIds") Collection<Integer> drugIds);

    /**
     * ps:调用该方法不会设置用药频次等默认值
     *
     * @param drugIds
     * @return
     */
    @DAOMethod(sql = "from DrugList where drugId in (:drugIds)",limit=0)
    public abstract List<DrugList> findByDrugIdsAndStatus(@DAOParam("drugIds") Collection<Integer> drugIds);

    /**
     *
     * 查询平台基础库删除的药
     * @param drugIds
     * @return
     */
    @DAOMethod(sql = "from DrugList where drugId in (:drugIds) and status=0")
    public abstract List<DrugList> findByDrugIdsforDel(@DAOParam("drugIds") List<Integer> drugIds);

    /**
     * ps:调用该方法不会设置用药频次等默认值
     *
     * @return
     */
    @DAOMethod(sql = "from DrugList where 1=1 ", limit = 0)
    public abstract List<DrugList> findAll();

    /**
     * 分页查询所有基础药品库数据
     * @param start
     * @param limit
     * @return
     */
    @DAOMethod(sql = "from DrugList where status=1 and ( sourceOrgan is null or sourceOrgan=0  ) order by drugId")
    public abstract List<DrugList> findAllForPage(@DAOParam(pageStart = true) int start,
                                                   @DAOParam(pageLimit = true) int limit);

    /**
     * 统计基础药品库总数
     * @return
     */
    @DAOMethod(sql = "select count(*) from DrugList where status=1 and sourceOrgan is null")
    public abstract long getTotalWithBase();

    /**
     * 根据organid获取
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = " select d from DrugList d,SaleDrugList s where d.drugId=s.drugId and s.status=1 and s.organId=:organId ", limit = 9999)
    public abstract List<DrugList> findDrugsByDepId(@DAOParam("organId") Integer organId);

    public DrugList findByDrugIdAndOrganId(final int drugId) {

        HibernateStatelessResultAction<DrugList> action = new AbstractHibernateStatelessResultAction<DrugList>() {
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder("select distinct d from DrugList d,OrganDrugList o where d.drugId=:drugId " +
                        "and d.drugId = o.drugId and o.status =1");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("drugId", drugId);
                Object dbObj = q.uniqueResult();
                if (dbObj instanceof DrugList) {
                    DrugList drug = (DrugList) dbObj;
                    setDrugDefaultInfo(drug);
                    setResult(drug);
                } else {
                    setResult(null);
                }
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 设置药品默认的一些数据
     *
     * @param drug
     */
    public void setDrugDefaultInfo(DrugList drug) {
        //设置默认值---取消默认值，bug#27581----运营平台药品数据与医生开方药品填充不一致
        /*if (StringUtils.isEmpty(drug.getUsingRate())) {
            //每日三次
            drug.setUsingRate("tid");
        }
        if (StringUtils.isEmpty(drug.getUsePathways())) {
            //口服
            drug.setUsePathways("po");
        }*/
        if (null == drug.getUseDose()) {
            //根据规格来设置
            double useDose = 0d;
            String drugSpec = drug.getDrugSpec();
            if (StringUtils.isNotEmpty(drugSpec)) {
                String[] info = drugSpec.split("\\*");
                try {
                    useDose = Double.parseDouble(info[0]);
                } catch (NumberFormatException e) {
                    StringBuilder useDose1 = new StringBuilder(10);
                    if (StringUtils.isNotEmpty(info[0])) {
                        char[] chars = info[0].toCharArray();
                        for (char c : chars) {
                            //48-57在ascii中对应 0-9，46为.
                            boolean b = (c >= 48 && c <= 57) || c == 46;
                            if (b) {
                                useDose1.append(c);
                            } else {
                                //遇到中间有其他字符则只取前面的数据
                                break;
                            }
                        }
                    }

                    if (useDose1.length() > 0) {
                        useDose = Double.parseDouble(useDose1.toString());
                    } else {
                        useDose = 0d;
                    }
                }
            }
            drug.setUseDose(useDose);
        }
    }

    /**
     * 药品名模糊查询 药品
     *
     * @param name
     * @return
     * @author zhongzx
     */
    public QueryResult<DrugList> findDrugListByName(final String name,final String producer,final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<DrugList>> action = new AbstractHibernateStatelessResultAction<QueryResult<DrugList>>() {
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder("from DrugList where status=1 and isStandardDrug=1 ");
                if (!ObjectUtils.isEmpty(name)) {
                    hql.append(" and ( drugName like:name or saleName like:name  )");
                }
                if (!ObjectUtils.isEmpty(producer)) {
                    hql.append(" and producer like:producer ");
                }
                hql.append(" order by createDt desc");
                Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                if (!ObjectUtils.isEmpty(name)) {
                    countQuery.setParameter("name", "%" + name + "%");
                }
                if (!ObjectUtils.isEmpty(producer)) {
                    countQuery.setParameter("producer", "%" + producer + "%");
                }
                Long total = (Long) countQuery.uniqueResult();

                Query q = ss.createQuery(hql.toString());
                if (!ObjectUtils.isEmpty(name)) {
                    q.setParameter("name", "%" + name + "%");
                }
                if (!ObjectUtils.isEmpty(producer)) {
                    q.setParameter("producer", "%" + producer + "%");
                }
                q.setFirstResult(start);
                q.setMaxResults(limit);
                List<DrugList> list = q.list();
                setResult(new QueryResult<DrugList>(total, q.getFirstResult(), q.getMaxResults(), list));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    /**
     * 查询所选药品是否存在对应标准药品
     * @param drugName
     * @param drugType
     * @param drugSpec
     * @param producer
     * @param drugForm
     * @return
     */
    @DAOMethod(sql = "from DrugList where drugName =:drugName and drugType=:drugType and drugSpec=:drugSpec and producer=producer and drugForm=drugForm and isStandardDrug=1 and  status= 1")
    public abstract List<DrugList> findStandardDrug(@DAOParam("drugName") String drugName,@DAOParam("drugType") Integer drugType,@DAOParam("drugSpec") String drugSpec,@DAOParam("producer") String producer,@DAOParam("drugForm") String drugForm);

    @DAOMethod(sql = "from DrugList where drugName =:drugName and drugType=:drugType and drugSpec=:drugSpec and producer=producer and sourceOrgan is null   and isStandardDrug=1 and  status= 1")
    public abstract List<DrugList> findStandardDrugDrugFrom(@DAOParam("drugName") String drugName,@DAOParam("drugType") Integer drugType,@DAOParam("drugSpec") String drugSpec,@DAOParam("producer") String producer);


    /**
     * 运营平台 药品查询服务
     *
     * @param drugClass 药品分类
     * @param status    药品状态
     * @param keyword   查询关键字:药品名称 or 生产厂家 or 商品名称 or 批准文号 or drugId
     * @param start     分页起始位置
     * @param limit     每页限制条数
     * @return QueryResult<DrugList>
     * @author houxr
     */
    public QueryResult<DrugList> queryDrugListsByDrugNameAndStartAndLimit(final String drugClass, final String keyword,
                                                                              final Integer status, final Integer sourceOrgan, Integer type,final Integer isStandardDrug,final String producer,
                                                                              final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<DrugList>> action = new AbstractHibernateStatelessResultAction<QueryResult<DrugList>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder("From DrugList where 1=1   ");
                if (!StringUtils.isEmpty(drugClass)) {
                    hql.append(" and drugClass like :drugClass");
                }

                if (!StringUtils.isEmpty(producer)) {
                    hql.append(" and producer like :producer");
                }
                Integer drugId = null;
                if (!StringUtils.isEmpty(keyword)) {
                    try {
                        drugId = Integer.valueOf(keyword);
                    } catch (Throwable throwable) {
                        drugId = null;
                    }
                    hql.append(" and (");
                    hql.append(" drugName like :keyword or saleName like :keyword or approvalNumber like :keyword or drugCode like :keyword ");
                    if (drugId != null) {
                        hql.append(" or drugId =:drugId");
                    }
                    hql.append(")");
                }
                if (!ObjectUtils.isEmpty(status)) {
                    hql.append(" and status =:status");
                }
                if (!ObjectUtils.isEmpty(type)) {
                    hql.append(" and drugType =:drugType");
                }
                if (!ObjectUtils.isEmpty(isStandardDrug)) {
                    hql.append(" and isStandardDrug =:isStandardDrug ");
                }
                if (!ObjectUtils.isEmpty(sourceOrgan)) {
                    if (sourceOrgan == 0){
                        hql.append(" and ( sourceOrgan is null or sourceOrgan=0 )   ");
                    }else {
                        hql.append(" and sourceOrgan =:sourceOrgan ");
                    }
                }

                hql.append(" order by createDt desc");
                Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                if (!ObjectUtils.isEmpty(status)) {
                    countQuery.setParameter("status", status);
                }
                if (!ObjectUtils.isEmpty(type)) {
                    countQuery.setParameter("drugType", type);
                }
                if (!ObjectUtils.isEmpty(isStandardDrug)) {
                    countQuery.setParameter("isStandardDrug", isStandardDrug);
                }
                if (!ObjectUtils.isEmpty(sourceOrgan)) {
                    if (sourceOrgan != 0){
                        countQuery.setParameter("sourceOrgan", sourceOrgan);
                    }
                }
                if (drugId != null) {
                    countQuery.setParameter("drugId", drugId);
                }
                if (!StringUtils.isEmpty(keyword)) {
                    countQuery.setParameter("keyword", "%" + keyword + "%");
                }
                if (!StringUtils.isEmpty(drugClass)) {
                    countQuery.setParameter("drugClass", drugClass + "%");
                }
                if (!StringUtils.isEmpty(producer)) {
                    countQuery.setParameter("producer", "%" +producer + "%");
                }
                Long total = (Long) countQuery.uniqueResult();

                Query query = ss.createQuery(hql.toString());
                if (!ObjectUtils.isEmpty(status)) {
                    query.setParameter("status", status);
                }
                if (!ObjectUtils.isEmpty(type)) {
                    query.setParameter("drugType", type);
                }
                if (!ObjectUtils.isEmpty(isStandardDrug)) {
                    query.setParameter("isStandardDrug", isStandardDrug);
                }
                if (drugId != null) {
                    query.setParameter("drugId", drugId);
                }
                if (!ObjectUtils.isEmpty(sourceOrgan)) {
                    if (sourceOrgan != 0){
                        query.setParameter("sourceOrgan", sourceOrgan);
                    }
                }
                if (!StringUtils.isEmpty(keyword)) {
                    query.setParameter("keyword", "%" + keyword + "%");
                }
                if (!StringUtils.isEmpty(drugClass)) {
                    query.setParameter("drugClass", drugClass + "%");
                }
                if (!StringUtils.isEmpty(producer)) {
                    query.setParameter("producer", "%" +producer + "%");
                }
                query.setFirstResult(start);
                query.setMaxResults(limit);
                List<DrugList> lists = query.list();
                //LOGGER.info("查寻通用药品001:{}"+ JSONUtils.toString(lists));
                List<DrugList> lists2 = Lists.newArrayList();
                if (lists != null && lists.size() > 0){
                    for (DrugList list : lists) {
                        DrugSourcesDAO dao = DAOFactory.getDAO(DrugSourcesDAO.class);
                        if (list.getSourceOrgan() != null){
                            List<DrugSources> byDrugSourcesId = dao.findByDrugSourcesId(list.getSourceOrgan());
                            if (byDrugSourcesId != null && byDrugSourcesId.size() > 0 ){
                                list.setSourceOrganText(byDrugSourcesId.get(0).getDrugSourcesName());
                            }else {
                                list.setSourceOrganText("平台通用");
                            }
                        }else {
                            list.setSourceOrganText("平台通用");
                        }
                        lists2.add(list);
                    }
                }
                //LOGGER.info("查寻通用药品002:{}"+JSONUtils.toString(lists2));
                setResult(new QueryResult<DrugList>(total, query.getFirstResult(), query.getMaxResults(), lists2));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 商品名模糊查询 药品
     *
     * @param name
     * @return
     * @author zhongzx
     */
    public DrugList queryBySaleNameLike(final String name) {
        HibernateStatelessResultAction<DrugList> action = new AbstractHibernateStatelessResultAction<DrugList>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from DrugList where saleName like :name");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("name", "%" + name + "%");
                List<DrugList> list = q.list();
                if (null != list && list.size() > 0) {
                    setResult(list.get(0));
                }
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 患者端 首页药品推荐列表 按开具的次数的多少降序排列
     *
     * @param start 每页开始
     * @param limit 每页数量
     * @return
     * @author zhongzx
     */
    public List<DrugList> queryDrugList(final Integer start, final Integer limit) {
        HibernateStatelessResultAction<List<DrugList>> action = new AbstractHibernateStatelessResultAction<List<DrugList>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select d, count(d.drugId) as drugNum from DrugList d, OrganDrugList o, " +
                        "Recipedetail rp, Recipe r where r.recipeId = rp.recipeId and r.status =6 and rp.status = 1 " +
                        "and d.drugId = rp.drugId and d.drugId = o.drugId and d.status =1 and o.status = 1 and o.organId = 1 " +
                        "group by d.drugId order by drugNum desc");
                // 2017/3/13 0013 暂时只展示邵逸夫的药品
                Query q = ss.createQuery(hql.toString());
                if (start != null && limit != null) {
                    q.setMaxResults(limit);
                    q.setFirstResult(start);
                }
                List<Object[]> list = q.list();
                List<DrugList> drugList = new ArrayList<>();
                for (Object[] obj : list) {
                    DrugList d = (DrugList) obj[0];
                    drugList.add(d);
                }
                setResult(drugList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * @param drugId
     * @param changeAttr
     * @return
     */
    public Boolean updateDrugListInfoById(final int drugId, final Map<String, ?> changeAttr) {
        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update DrugList set lastModify=current_timestamp() ");
                if (null != changeAttr && !changeAttr.isEmpty()) {
                    for (String key : changeAttr.keySet()) {
                        hql.append("," + key + "=:" + key);
                    }
                }
                hql.append(" where drugId=:drugId");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("drugId", drugId);
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
     * 药品获取医院价格
     *
     * @param dList
     */
    public void getHospitalPrice(Integer organId, List<DrugList> dList) {
        List drugIds = new ArrayList();
        for (DrugList drugList : dList) {
            if (null != drugList) {
                drugIds.add(drugList.getDrugId());
            }
        }

        OrganDrugListDAO dao = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<OrganDrugList> organDrugList = dao.findByOrganIdAndDrugIds(organId, drugIds);
        // 设置医院价格
        for (DrugList drugList : dList) {
            for (OrganDrugList odlist : organDrugList) {
                if (null != drugList && null != odlist && drugList.getDrugId().equals(odlist.getDrugId())) {
                    drugList.setHospitalPrice(odlist.getSalePrice());
                    break;
                }
            }
        }
    }

    /**
     * 商品名匹配药品
     * @param name
     * @return
     */
    public List<DrugList> findBySaleNameLike(final String name) {
        HibernateStatelessResultAction<List<DrugList>> action = new AbstractHibernateStatelessResultAction<List<DrugList>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from DrugList where status = 1 and sourceOrgan is null and saleName like :name");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("name", "%" + name + "%");
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 商品名 药品名 编号 生产厂家 匹配药品 搜索专用
     * @param input
     * @param producer
     * @return
     */
    public List<DrugList> findBySaleNameLikeSearch(final Integer organId,final String input,final String producer) {
        HibernateStatelessResultAction<List<DrugList>> action = new AbstractHibernateStatelessResultAction<List<DrugList>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from DrugList where status = 1  ");
                if (input != null){
                    hql.append(" and (  saleName like :input or drugId like:input or drugName like:input   )   ");
                }
                if (producer != null){
                    hql.append(" and producer like :producer   ");
                }
                Query q = ss.createQuery(hql.toString());
                if (input != null){
                    q.setParameter("input", "%" + input + "%");
                }
                if (producer != null){
                    q.setParameter("producer", "%" + producer + "%");
                }
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod
    public abstract List<DrugList> findByDrugName(String drugName);

    /**
     * 药品名或者商品名匹配药品
     * @param name
     * @return
     */
    public List<DrugList> findBySaleNameOrDrugNameLike(final String name) {
        HibernateStatelessResultAction<List<DrugList>> action = new AbstractHibernateStatelessResultAction<List<DrugList>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from DrugList where sourceOrgan is null and Status = 1 and (saleName like :name or drugName like :name )");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("name", "%" + name + "%");
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 匹配搜索药品
     * @param drugName 药品名称
     * @param saleName 商品名称
     * @param drugSpec 药品规格
     * @param producer 生产厂家
     * @return         药品列表
     */
    public List<DrugList> findDrugListByNameOrSpec(final String drugName, final String saleName, final String drugSpec, final String producer) {
        HibernateStatelessResultAction<List<DrugList>> action = new AbstractHibernateStatelessResultAction<List<DrugList>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from DrugList where sourceOrgan is null and Status = 1 ");
                if (StringUtils.isNotEmpty(drugName)) {
                    hql.append(" and drugName like :drugName ");
                }
                if (StringUtils.isNotEmpty(saleName)) {
                    hql.append(" and saleName like :saleName ");
                }
                if (StringUtils.isNotEmpty(drugSpec)) {
                    hql.append(" and drugSpec like :drugSpec ");
                }
                if (StringUtils.isNotEmpty(producer)) {
                    hql.append(" and producer like :producer ");
                }
                Query q = ss.createQuery(hql.toString());
                if (StringUtils.isNotEmpty(drugName)) {
                    q.setParameter("drugName", "%" + drugName + "%");
                }
                if (StringUtils.isNotEmpty(saleName)) {
                    q.setParameter("saleName", "%" + saleName + "%");
                }
                if (StringUtils.isNotEmpty(drugSpec)) {
                    q.setParameter("drugSpec", "%" + drugSpec + "%");
                }
                if (StringUtils.isNotEmpty(producer)) {
                    q.setParameter("producer", "%" + producer + "%");
                }

                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod(sql = "from DrugList where sourceOrgan =:sourceOrgan")
    public abstract List<DrugList> findDrugListBySourceOrgan(@DAOParam("sourceOrgan") int sourceOrgan,@DAOParam(pageStart = true) int start,
                                                             @DAOParam(pageLimit = true) int limit);

    @DAOMethod(sql = "from DrugList where drugId in (:drugIds)",limit = 0)
    public abstract List<DrugList> findByDrugIdsWithOutStatus(@DAOParam("drugIds")List<Integer> drugIds);

    public DrugList findValidByDrugId(Integer drugId){
        HibernateStatelessResultAction<DrugList> action = new AbstractHibernateStatelessResultAction<DrugList>() {
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder("from DrugList where drugId=:drugId and status=1");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("drugId", drugId);
                DrugList drugList = (DrugList) q.uniqueResult();
                setResult(drugList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod(sql = "SELECT DISTINCT usingRate FROM DrugList WHERE (sourceOrgan = '' OR sourceOrgan IS NULL)",limit = 0)
    public abstract List<String> findUsingRateOfAll();

    @DAOMethod(sql = "SELECT DISTINCT usePathways FROM DrugList WHERE (sourceOrgan = '' OR sourceOrgan IS NULL)",limit = 0)
    public abstract List<String> findUsePathwaysOfAll();

    @DAOMethod(sql = "update DrugList set usingRateId=:newUsingRate where usingRate=:oldUsingRate")
    public abstract void updateUsingRateByUsingRate(@DAOParam("oldUsingRate") String oldUsingRate ,@DAOParam("newUsingRate") String newUsingRate);


    @DAOMethod(sql = "update DrugList set usePathwaysId=:newUsePathways where usePathways=:oldUsePathways")
    public abstract void updateUsePathwaysByUsePathways(@DAOParam("oldUsePathways") String oldUsePathways ,@DAOParam("newUsePathways") String newUsePathways);

    @DAOMethod(sql = " from DrugList where drugName=:drugName and saleName=:saleName  and drugType=:drugType  and producer=:producer  and drugSpec=:drugSpec and sourceOrgan=:sourceOrgan and status = 1",limit = 0)
    public abstract List<DrugList> findRepeatDrugList(@DAOParam("drugName") String drugName,@DAOParam("saleName") String saleName,@DAOParam("drugType") Integer drugType, @DAOParam("producer") String producer, @DAOParam("drugSpec") String drugSpec , @DAOParam("sourceOrgan") Integer sourceOrgan);

    @DAOMethod(sql = " from DrugList where drugName=:drugName and saleName=:saleName  and drugType=:drugType  and producer=:producer  and drugSpec=:drugSpec and status = 1",limit = 0)
    public abstract List<DrugList> findRepeatDrugListNoOrgan(@DAOParam("drugName") String drugName,@DAOParam("saleName") String saleName,@DAOParam("drugType") Integer drugType, @DAOParam("producer") String producer, @DAOParam("drugSpec") String drugSpec);


}
