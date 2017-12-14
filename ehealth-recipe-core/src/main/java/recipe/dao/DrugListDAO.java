package recipe.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.searchservice.model.DrugSearchTO;
import com.ngari.base.searchservice.service.ISearchService;
import com.ngari.recipe.entity.DrugList;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryItem;
import ctd.dictionary.service.DictionaryLocalService;
import ctd.dictionary.service.DictionarySliceRecordSet;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.persistence.support.impl.dictionary.DBDictionaryItemLoader;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcService;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import recipe.util.ApplicationUtils;
import recipe.util.DateConversion;
import recipe.util.RecipeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 */
@RpcSupportDAO(serviceId = "drugList")
public abstract class DrugListDAO extends HibernateSupportDelegateDAO<DrugList>
        implements DBDictionaryItemLoader<DrugList> {

    /**
     * logger
     */
    private static final Logger logger = LoggerFactory.getLogger(DrugListDAO.class);

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
    @RpcService
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
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    /**
     * 根据药品Id获取药品记录
     * (不包括机构没有配置的药品)
     * @param drugId 药品id
     * @return
     * @author yaozh
     */
    @RpcService
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
        HibernateSessionTemplate.instance().executeReadOnly(action);
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
        HibernateSessionTemplate.instance().executeReadOnly(action);
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
        HibernateSessionTemplate.instance().executeReadOnly(action);
        List<DrugList> drugList = action.getResult();
        //取出根据别名查询得到的drugId 作为去DrugList表中查询的入参
        List<Integer> idList = new ArrayList<>();
        for (DrugList d : drugList) {
            idList.add(d.getDrugId());
        }
        return idList;
    }

    /**
     * zhongzx
     * 搜索药品 使用es新方式搜索
     *
     * @return
     */
    public List<DrugList> searchDrugListWithES(Integer organId, Integer drugType, String drugName,
                                               Integer start, Integer limit) {
        ISearchService searchService = ApplicationUtils.getBaseService(ISearchService.class);

        DrugSearchTO searchTO = new DrugSearchTO();
        searchTO.setDrugName(StringUtils.isEmpty(drugName) ? "" : drugName.toLowerCase());
        searchTO.setOrgan(null == organId ? "" : String.valueOf(organId));
        searchTO.setDrugType(null == drugType ? "" : String.valueOf(drugType));
        searchTO.setStart(start);
        searchTO.setLimit(limit);
        logger.info("searchDrugListWithES DrugSearchTO={} ", JSONUtils.toString(searchTO));
        List<String> drugInfo = searchService.findDrugList(searchTO);
        List<DrugList> dList = new ArrayList<>(drugInfo.size());
        // 将String转化成DrugList对象返回给前端
        if (CollectionUtils.isNotEmpty(drugInfo)) {
            for (String s : drugInfo) {
                DrugList drugList = null;
                try {
                    drugList = JSONUtils.parse(s, DrugList.class);
                } catch (Exception e) {
                    logger.error("searchDrugListWithES parse error.  String=" + s);
                }
                //该高亮字段给微信端使用:highlightedField
                //该高亮字段给ios前端使用:highlightedFieldForIos
                if (null != drugList && StringUtils.isNotEmpty(drugList.getHighlightedField())) {
                    drugList.setHighlightedFieldForIos(getListByHighlightedField(drugList.getHighlightedField()));
                }
                dList.add(drugList);
            }

            logger.info("searchDrugListWithES result DList.size = " + dList.size());
            RecipeUtil.getHospitalPrice(dList);
        } else {
            logger.info("searchDrugListWithES result isEmpty! drugName = " + drugName);
        }

        return dList;
    }

    /**
     * 用正则截取指定标记间的字符串
     *
     * @param highlightedField
     * @return
     */
    public List<String> getListByHighlightedField(String highlightedField) {
        List list = new ArrayList();

        Pattern p = Pattern.compile("(?<=<em>).+?(?=</em>)");
        Matcher m = p.matcher(highlightedField);
        while (m.find()) {
            list.add(m.group().trim());
        }
//        logger.info("highlightedField is " + list.toString());
        return list;
    }

    /**
     * 药品目录搜索服务（每页限制10条）
     *
     * @param drugName 药品名称
     * @param start    分页起始位置
     * @return List<DrugList>
     * zhongzx 加 organId,drugType
     * @author luf
     */
    @RpcService
    public List<DrugList> findDrugListsByNameOrCodePageStaitc(
            final int organId, final int drugType, final String drugName, final int start) {
        return searchDrugListWithES(organId, drugType, drugName, start, 10);
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
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    /**
     * 药品分类下的全部药品列表服务
     * （全部药品 drugClass 入参为空字符串）
     *
     * @param organId   医疗机构代码
     * @param drugClass 药品分类
     * @param start     分页起始位置
     * @return List<DrugList>
     * zhongzx 加 drugType
     * @author luf
     */
    @RpcService
    public List<DrugList> findAllInDrugClassByOrgan(int organId, int drugType,
                                                    String drugClass, int start) {
        List<DrugList> dList = findDrugListsByOrganOrDrugClass(organId, drugType, drugClass, start,
                10);
        // 添加医院价格
        if (!dList.isEmpty()) {
            RecipeUtil.getHospitalPrice(dList);
        }
        return dList;
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
    public List<DrugList> findCommonDrugListsWithPage(final int doctor, final int organId, final int drugType,
                                                      final int start, final int limit) {
        HibernateStatelessResultAction<List<DrugList>> action = new AbstractHibernateStatelessResultAction<List<DrugList>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                String hql = new String("select a From DrugList a, Recipe b,Recipedetail c, OrganDrugList o where "
                        + "b.doctor=:doctor and a.status=1 and a.drugId=c.drugId and b.clinicOrgan=:organId "
                        + "and a.drugType=:drugType and b.createDate>=:halfYear and o.drugId=a.drugId and o.organId=:organId and o.status=1 "
                        + "and b.recipeId=c.recipeId group by c.drugId order by count(*) desc");
                Query q = ss.createQuery(hql);
                q.setParameter("doctor", doctor);
                q.setParameter("organId", organId);
                q.setParameter("drugType", drugType);
                q.setParameter("halfYear", DateConversion.getMonthsAgo(6));
                q.setFirstResult(start);
                q.setMaxResults(limit);
                List<DrugList> drugListList = q.list();
                for (DrugList drug : drugListList) {
                    setDrugDefaultInfo(drug);
                }
                setResult(drugListList);
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    /**
     * 常用药品列表服务
     *
     * @param doctor 开方医生
     * @return List<DrugList>
     * zhongzx 加 organId,drugType
     * @author luf
     */
    @RpcService
    public List<DrugList> findCommonDrugLists(int doctor, int organId, int drugType) {
        List<DrugList> dList = this.findCommonDrugListsWithPage(doctor, organId, drugType, 0, 20);
        // 添加医院价格
        if (!dList.isEmpty()) {
            RecipeUtil.getHospitalPrice(dList);
        }
        return dList;
    }

    /**
     * 获取药品类别
     *
     * @param parentKey 父节点值
     * @param sliceType --0所有子节点 1所有叶子节点 2所有文件夹节点 3所有子级节点 4所有子级叶子节点 5所有子级文件夹节点
     * @return List<DictionaryItem>
     * @author luf
     */
    @RpcService
    public List<DictionaryItem> getDrugClass(String parentKey, int sliceType) {
        DictionaryLocalService ser = AppContextHolder.getBean("dictionaryService", DictionaryLocalService.class);
        List<DictionaryItem> list = new ArrayList<DictionaryItem>();
        try {
            DictionarySliceRecordSet var = ser.getSlice(
                    "eh.base.dictionary.DrugClass", parentKey, sliceType, "",
                    0, 1000);
            list = var.getItems();

        } catch (ControllerException e) {
            logger.error("getDrugClass() error : " + e);
        }
        return list;
    }

    /**
     * 获取一个药品类别下面的第一子集和第二子集，重新组装
     *
     * @param parentKey 父级
     * @return
     * @author zhangx
     * @date 2015-12-7 下午7:42:26
     */
    @RpcService
    public List<HashMap<String, Object>> findDrugClass(String parentKey) {
        List<HashMap<String, Object>> returnList = new ArrayList<HashMap<String, Object>>();

        List<DictionaryItem> list = getDrugClass(parentKey, 3);
        for (DictionaryItem dictionaryItem : list) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("key", dictionaryItem.getKey());
            map.put("text", dictionaryItem.getText());
            map.put("leaf", dictionaryItem.isLeaf());
            map.put("index", dictionaryItem.getIndex());
            map.put("mcode", dictionaryItem.getMCode());
            map.put("child", getDrugClass(dictionaryItem.getKey(), 3));
            returnList.add(map);
        }
        return returnList;
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
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    /**
     * 获得 对应机构 对应药品类型 存在有效药品目录的某级药品类目。
     * zhongzx
     *
     * @param organId
     * @param drugType
     * @return
     */
    public List<DictionaryItem> findDrugClassByDrugType(Integer organId, Integer drugType, String parentKey) {

        //从数据库进行筛选
        List<DrugList> drugList = findDrugClassByDrugList(organId, drugType, parentKey, null, null);
        List<DictionaryItem> allItemList = getDrugClass(parentKey, 3);
        List<DictionaryItem> itemList = new ArrayList<>();

        for (DictionaryItem item : allItemList) {
            for (DrugList d : drugList) {
                //根据药品类目 是不是以 某级类目的key值开头的 来判断
                if (d.getDrugClass().startsWith(item.getKey())) {
                    itemList.add(item);
                    break;
                }
            }
        }
        //现在 按照字典的录入顺序显示
        //类目按照拼音正序排序
        /*final Collator collator = Collator.getInstance(java.util.Locale.CHINA);
        Collections.sort(itemList, new Comparator<DictionaryItem>() {
            @Override
            public int compare(DictionaryItem o1, DictionaryItem o2) {
                return collator.compare(o1.getText(), o2.getText());
            }
        });*/
        return itemList;
    }

    /**
     * 查找存在有效药品的 类目(第一级类目传空)
     * zhongzx
     *
     * @param parentKey
     * @return
     */
    @RpcService
    public List<DictionaryItem> findChildByDrugClass(Integer organId, Integer drugType, String parentKey) {
        return findDrugClassByDrugType(organId, drugType, parentKey);
    }

    /**
     * 获取存在有效药品目录的一级、二级、三级类目(西药)；一级、二级（中成药）
     * zhongzx
     *
     * @param organId
     * @param drugType
     * @return
     */
    @RpcService
    public List<HashMap<String, Object>> findAllClassByDrugType(int organId, int drugType) {
        List<HashMap<String, Object>> returnList = new ArrayList<HashMap<String, Object>>();

        //先获得一级有效类目
        List<DictionaryItem> firstList = findChildByDrugClass(organId, drugType, "");

        for (DictionaryItem first : firstList) {
            List<HashMap<String, Object>> childList = Lists.newArrayList();
            HashMap<String, Object> map = Maps.newHashMap();
            map.put("key", first.getKey());
            map.put("text", first.getText());
            map.put("leaf", first.isLeaf());
            map.put("index", first.getIndex());
            map.put("mcode", first.getMCode());
            map.put("child", childList);
            List<DictionaryItem> list = findChildByDrugClass(organId, drugType, first.getKey());
            if (null != list && list.size() != 0) {
                for (DictionaryItem dictionaryItem : list) {
                    HashMap<String, Object> map1 = Maps.newHashMap();
                    map1.put("key", dictionaryItem.getKey());
                    map1.put("text", dictionaryItem.getText());
                    map1.put("leaf", dictionaryItem.isLeaf());
                    map1.put("index", dictionaryItem.getIndex());
                    map1.put("mcode", dictionaryItem.getMCode());
                    //如果是中成药 就不用判断是否有第三级类目 它只有二级类目
                    if (drugType == 1) {
                        //判断是否有第三级类目 如果有则显示 如果没有 以第二类目的名称命名生成一个第三子类
                        List<DictionaryItem> grandchild = findChildByDrugClass(organId, drugType, dictionaryItem.getKey());
                        if (null != grandchild && 0 != grandchild.size()) {
                            map1.put("grandchild", grandchild);
                        } else {
                            List one = new ArrayList();
                            one.add(dictionaryItem);
                            map1.put("grandchild", one);
                        }
                    }
                    childList.add(map1);
                }
            } else {
                HashMap<String, Object> map1 = Maps.newHashMap();
                map1.put("key", first.getKey());
                map1.put("text", first.getText());
                map1.put("leaf", first.isLeaf());
                map1.put("index", first.getIndex());
                map1.put("mcode", first.getMCode());
                childList.add(map1);
            }
            returnList.add(map);
        }
        return returnList;
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
    @DAOMethod(sql = "from DrugList where drugId in (:drugIds) and status=1")
    public abstract List<DrugList> findByDrugIds(@DAOParam("drugIds") List<Integer> drugIds);

    /**
     * ps:调用该方法不会设置用药频次等默认值
     *
     * @return
     */
    @DAOMethod(sql = "from DrugList where 1=1 ", limit = 0)
    public abstract List<DrugList> findAll();


    @DAOMethod(sql = " select d from DrugList d,SaleDrugList s where d.drugId=s.drugId and s.status=1 and s.organId=:organId ", limit = 9999)
    public abstract List<DrugList> findDrugsByDepId(@DAOParam("organId") Integer organId);


    /**
     * 设置药品默认的一些数据
     *
     * @param drug
     */
    private void setDrugDefaultInfo(DrugList drug) {
        //设置默认值
        if (StringUtils.isEmpty(drug.getUsingRate())) {
            //每日三次
            drug.setUsingRate("tid");
        }
        if (StringUtils.isEmpty(drug.getUsePathways())) {
            //口服
            drug.setUsePathways("po");
        }
        if (null == drug.getUseDose()) {
            //根据规格来设置
            double useDose = 0d;
            String drugSpec = drug.getDrugSpec();
            if (StringUtils.isNotEmpty(drugSpec)) {
                String[] info = drugSpec.split("\\*");
                try {
                    useDose = Double.parseDouble(info[0]);
                } catch (NumberFormatException e) {
                    StringBuilder _useDose = new StringBuilder(10);
                    if (StringUtils.isNotEmpty(info[0])) {
                        char[] chars = info[0].toCharArray();
                        for (char c : chars) {
                            //48-57在ascii中对应 0-9，46为.
                            if ((c >= 48 && c <= 57) || c == 46) {
                                _useDose.append(c);
                            } else {
                                //遇到中间有其他字符则只取前面的数据
                                break;
                            }
                        }
                    }

                    if (_useDose.length() > 0) {
                        useDose = Double.parseDouble(_useDose.toString());
                    } else {
                        useDose = 0d;
                    }
                }
            }
            drug.setUseDose(useDose);
        }
    }

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
                                                                          final Integer status,
                                                                          final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<DrugList>> action = new AbstractHibernateStatelessResultAction<QueryResult<DrugList>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder("From DrugList where 1=1");
                if (!StringUtils.isEmpty(drugClass)) {
                    hql.append(" and drugClass like :drugClass");
                }
                Integer drugId = null;
                if (!StringUtils.isEmpty(keyword)) {
                    try {
                        drugId = Integer.valueOf(keyword);
                    } catch (Throwable throwable) {
                        drugId = null;
                    }
                    hql.append(" and (");
                    hql.append(" drugName like :keyword or producer like :keyword or saleName like :keyword or approvalNumber like :keyword ");
                    if (drugId != null){
                        hql.append(" or drugId =:drugId");
                    }
                    hql.append(")");
                }
                if (!ObjectUtils.isEmpty(status)) {
                    hql.append(" and status =:status");
                }
                hql.append(" order by createDt desc");
                Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                if (!ObjectUtils.isEmpty(status)) {
                    countQuery.setParameter("status", status);
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
                Long total = (Long) countQuery.uniqueResult();

                Query query = ss.createQuery(hql.toString());
                if (!ObjectUtils.isEmpty(status)) {
                    query.setParameter("status", status);
                }
                if (drugId != null) {
                    query.setParameter("drugId", drugId);
                }
                if (!StringUtils.isEmpty(keyword)) {
                    query.setParameter("keyword", "%" + keyword + "%");
                }
                if (!StringUtils.isEmpty(drugClass)) {
                    query.setParameter("drugClass", drugClass + "%");
                }
                query.setFirstResult(start);
                query.setMaxResults(limit);
                List<DrugList> lists = query.list();
                setResult(new QueryResult<DrugList>(total, query.getFirstResult(), query.getMaxResults(), lists));
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
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
        HibernateSessionTemplate.instance().executeReadOnly(action);
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
                        "group by d.drugId order by drugNum desc");// TODO: 2017/3/13 0013 暂时只展示邵逸夫的药品
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
        HibernateSessionTemplate.instance().executeReadOnly(action);
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

}
