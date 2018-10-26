package recipe.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcService;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import recipe.constant.ConditionOperator;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.bean.PatientRecipeBean;
import recipe.dao.bean.RecipeRollingInfo;
import recipe.util.DateConversion;
import recipe.util.SqlOperInfo;

import java.math.BigDecimal;
import java.util.*;

/**
 * 处方DAO
 *
 * @author yuyun
 */
@RpcSupportDAO
public abstract class RecipeDAO extends HibernateSupportDelegateDAO<Recipe> {

    private static final Log LOGGER = LogFactory.getLog(RecipeDAO.class);

    public RecipeDAO() {
        super();
        this.setEntityName(Recipe.class.getName());
        this.setKeyField("recipeId");
    }

    /**
     * 根据id获取
     *
     * @param recipeId
     * @return
     */
    @DAOMethod
    public abstract Recipe getByRecipeId(int recipeId);

    /**
     * 根据订单编号获取处方列表
     *
     * @param orderCode
     * @return
     */
    @DAOMethod(sql = "from Recipe where orderCode=:orderCode")
    public abstract List<Recipe> findRecipeListByOrderCode(@DAOParam("orderCode") String orderCode);

    /**
     * 根据订单编号获取处方id集合
     *
     * @param orderCode
     * @return
     */
    @DAOMethod(sql = "select recipeId from Recipe where orderCode=:orderCode")
    public abstract List<Integer> findRecipeIdsByOrderCode(@DAOParam("orderCode") String orderCode);

    /**
     * 根据处方id集合获取处方集合
     *
     * @param recipeIds
     * @return
     */
    @DAOMethod(sql = "from Recipe where recipeId in :recipeIds")
    public abstract List<Recipe> findByRecipeIds(@DAOParam("recipeIds") List<Integer> recipeIds);

    /**
     * 通过交易流水号获取
     *
     * @param tradeNo
     * @return
     */
    @DAOMethod
    public abstract Recipe getByOutTradeNo(String tradeNo);

    /**
     * 根据处方id获取状态
     *
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "select status from Recipe where recipeId=:recipeId")
    public abstract Integer getStatusByRecipeId(@DAOParam("recipeId") Integer recipeId);

    /**
     * 根据处方id查询开方机构
     *
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "select clinicOrgan from Recipe where recipeId=:recipeId")
    public abstract Integer getOrganIdByRecipeId(@DAOParam("recipeId") Integer recipeId);

    /**
     * 通过支付标识查询处方集合
     *
     * @param payFlag
     * @return
     */
    @DAOMethod
    public abstract List<Recipe> findByPayFlag(Integer payFlag);

    /**
     * 根据订单编号及开放机构查询处方
     *
     * @param recipeCode
     * @param clinicOrgan
     * @return
     */
    @DAOMethod(sql = "from Recipe where recipeCode=:recipeCode and clinicOrgan=:clinicOrgan and fromflag=1")
    public abstract Recipe getByRecipeCodeAndClinicOrgan(@DAOParam("recipeCode") String recipeCode,
                                                         @DAOParam("clinicOrgan") Integer clinicOrgan);

    /**
     * 查询所有处方
     *
     * @param recipeCode
     * @param clinicOrgan
     * @return
     */
    @DAOMethod(sql = "from Recipe where recipeCode=:recipeCode and clinicOrgan=:clinicOrgan")
    public abstract Recipe getByRecipeCodeAndClinicOrganWithAll(@DAOParam("recipeCode") String recipeCode,
                                                                @DAOParam("clinicOrgan") Integer clinicOrgan);

    /**
     * 根据处方来源源处方号及处方来源机构查询处方详情
     *
     * @param originRecipeCode
     * @param originClinicOrgan
     * @return
     */
    @DAOMethod
    public abstract Recipe getByOriginRecipeCodeAndOriginClinicOrgan(String originRecipeCode, Integer originClinicOrgan);

    /**
     * 根据医生id处方id获取处方集合
     *
     * @param doctorId
     * @param recipeId
     * @param start
     * @param limit
     * @return
     */
    @DAOMethod(sql = "from Recipe where doctor=:doctorId and fromflag=1 and recipeId<:recipeId and status!=10 order by createDate desc ")
    public abstract List<Recipe> findRecipesForDoctor(@DAOParam("doctorId") Integer doctorId, @DAOParam("recipeId") Integer recipeId,
                                                      @DAOParam(pageStart = true) int start, @DAOParam(pageLimit = true) int limit);

    /**
     * 根据处方id集合更新订单编号
     *
     * @param recipeIds
     * @param orderCode
     */
    @DAOMethod(sql = "update Recipe set orderCode=:orderCode where recipeId in :recipeIds")
    public abstract void updateOrderCodeByRecipeIds(@DAOParam("recipeIds") List<Integer> recipeIds, @DAOParam("orderCode") String orderCode);

    /**
     * 根据订单编号更新订单编号为空
     *
     * @param orderCode
     */
    @DAOMethod(sql = "update Recipe set orderCode=null where orderCode=:orderCode")
    public abstract void updateOrderCodeToNullByOrderCode(@DAOParam("orderCode") String orderCode);

    public List<Integer> findDoctorIdSortByCount(final String startDt, final String endDt,
                                                 final List<Integer> organs, final List<Integer> testDocIds,
                                                 final int start, final int limit) {
        HibernateStatelessResultAction<List<Integer>> action = new AbstractHibernateStatelessResultAction<List<Integer>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select r.doctor from Recipe r where");
                if (CollectionUtils.isNotEmpty(organs)) {
                    hql.append(" r.clinicOrgan in (:organs) and ");
                }
                if (CollectionUtils.isNotEmpty(testDocIds)) {
                    hql.append(" r.doctor not in (:testDocIds) and ");
                }
                hql.append(" r.signDate between '" + startDt + "' and '" + endDt + "' and r.status=" + RecipeStatusConstant.FINISH +
                        " GROUP BY r.doctor ORDER BY count(r.doctor) desc");

                Query q = ss.createQuery(hql.toString());

                if (CollectionUtils.isNotEmpty(organs)) {
                    q.setParameterList("organs", organs);
                }
                if (CollectionUtils.isNotEmpty(testDocIds)) {
                    q.setParameterList("testDocIds", testDocIds);
                }

                q.setMaxResults(limit);
                q.setFirstResult(start);

                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public List<RecipeRollingInfo> findLastesRecipeList(final String startDt, final String endDt,
                                                        final List<Integer> organs, final List<Integer> testDocIds,
                                                        final int start, final int limit) {
        HibernateStatelessResultAction<List<RecipeRollingInfo>> action = new AbstractHibernateStatelessResultAction<List<RecipeRollingInfo>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select new recipe.dao.bean.RecipeRollingInfo(r.clinicOrgan,r.depart,r.doctor,r.mpiid) from Recipe r where ");
                if (CollectionUtils.isNotEmpty(organs)) {
                    hql.append(" r.clinicOrgan in (:organs) and ");
                }
                if (CollectionUtils.isNotEmpty(testDocIds)) {
                    hql.append(" r.doctor not in (:testDocIds) and ");
                }
                hql.append("r.signDate between '" + startDt + "' and '" + endDt + "' order by r.recipeId desc ");

                Query q = ss.createQuery(hql.toString());

                if (CollectionUtils.isNotEmpty(organs)) {
                    q.setParameterList("organs", organs);
                }

                if (CollectionUtils.isNotEmpty(testDocIds)) {
                    q.setParameterList("testDocIds", testDocIds);
                }

                q.setMaxResults(limit);
                q.setFirstResult(start);

                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 处方单列表保存
     *
     * @param recipe
     * @return
     * @desc 保存的是从his导入过来的处方数据
     * @author LF
     */
    public Recipe saveRecipe(Recipe recipe) {
        LOGGER.info("处方单列表保存:" + JSONUtils.toString(recipe));
        return save(recipe);
    }

    public List<Integer> findPendingRecipes(final List<String> allMpiIds, final Integer status,
                                            final int start, final int limit) {
        HibernateStatelessResultAction<List<Integer>> action = new AbstractHibernateStatelessResultAction<List<Integer>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select r.recipeId from cdr_recipe r left join cdr_recipeorder o on r.orderCode=o.orderCode where r.status=:status " +
                        "and r.chooseFlag=0 and (o.effective is null or o.effective=0 ) and r.mpiid in :mpiIds order by r.signDate desc");
                Query q = ss.createSQLQuery(hql.toString());
                q.setParameterList("mpiIds", allMpiIds);
                q.setParameter("status", status);
                q.setMaxResults(limit);
                q.setFirstResult(start);
                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    public List<PatientRecipeBean> findOtherRecipesForPatient(final List<String> mpiIdList, final List<Integer> notInRecipeIds,
                                                              final int start, final int limit) {
        HibernateStatelessResultAction<List<PatientRecipeBean>> action = new AbstractHibernateStatelessResultAction<List<PatientRecipeBean>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select s.type,s.recordCode,s.recordId,s.mpiId,s.diseaseName,s.status,s.fee," +
                        "s.recordDate,s.couponId,s.medicalPayFlag,s.recipeType,s.organId from (");
                hql.append("SELECT 1 as type,null as couponId, t.MedicalPayFlag as medicalPayFlag, t.RecipeID as recordCode,t.RecipeID as recordId," +
                        "t.MPIID as mpiId,t.OrganDiseaseName as diseaseName,t.Status,t.TotalMoney as fee," +
                        "t.SignDate as recordDate,t.RecipeType as recipeType,t.ClinicOrgan as organId FROM cdr_recipe t " +
                        "left join cdr_recipeorder k on t.OrderCode=k.OrderCode ");
                hql.append("WHERE t.MPIID IN (:mpiIdList) and (k.Effective is null or k.Effective = 0) ")
                        .append("and (t.ChooseFlag=1 or (t.ChooseFlag=0 and t.Status=" + RecipeStatusConstant.CHECK_PASS + ")) ");
                if (CollectionUtils.isNotEmpty(notInRecipeIds)) {
                    hql.append("and t.RecipeID not in (:notInRecipeIds) ");
                }
                hql.append("UNION ALL ");
                hql.append("SELECT 2 as type,o.CouponId as couponId, 0 as medicalPayFlag, " +
                        "o.OrderCode as recordCode,o.OrderId as recordId,o.MpiId as mpiId,'' as diseaseName," +
                        "o.Status,o.ActualPrice as fee,o.CreateTime as recordDate,0 as recipeType, o.OrganId FROM cdr_recipeorder o " +
                        "WHERE o.MpiId IN (:mpiIdList) and o.Effective = 1 ");
                hql.append(") s ORDER BY s.recordDate desc");
                Query q = ss.createSQLQuery(hql.toString());
                q.setParameterList("mpiIdList", mpiIdList);
                if (CollectionUtils.isNotEmpty(notInRecipeIds)) {
                    q.setParameterList("notInRecipeIds", notInRecipeIds);
                }
                q.setMaxResults(limit);
                q.setFirstResult(start);
                List<Object[]> result = q.list();
                List<PatientRecipeBean> backList = new ArrayList<>(limit);
                if (CollectionUtils.isNotEmpty(result)) {
                    PatientRecipeBean patientRecipeBean;
                    for (Object[] objs : result) {
                        patientRecipeBean = new PatientRecipeBean();
                        patientRecipeBean.setRecordType(objs[0].toString());
                        patientRecipeBean.setRecordCode(objs[1].toString());
                        patientRecipeBean.setRecordId(Integer.parseInt(objs[2].toString()));
                        patientRecipeBean.setMpiId(objs[3].toString());
                        patientRecipeBean.setOrganDiseaseName(objs[4].toString());
                        patientRecipeBean.setStatusCode(Integer.parseInt(objs[5].toString()));
                        patientRecipeBean.setTotalMoney(new BigDecimal(objs[6].toString()));
                        patientRecipeBean.setSignDate((Date) objs[7]);
                        if (null != objs[8]) {
                            patientRecipeBean.setCouponId(Integer.parseInt(objs[8].toString()));
                        }
                        if (null != objs[9]) {
                            patientRecipeBean.setMedicalPayFlag(Integer.parseInt(objs[9].toString()));
                        }
                        patientRecipeBean.setRecipeType(Integer.parseInt(objs[10].toString()));
                        patientRecipeBean.setOrganId(Integer.parseInt(objs[11].toString()));

                        backList.add(patientRecipeBean);
                    }
                }

                setResult(backList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 获取处方总数
     *
     * @param doctorId
     * @param recipeStatus
     * @param conditionOper
     * @param containDel    true 包含删除的记录  false  去除删除的处方记录
     * @return
     */
    public int getCountByDoctorIdAndStatus(final int doctorId, final List<Integer> recipeStatus,
                                           final String conditionOper, final boolean containDel) {
        HibernateStatelessResultAction<Long> action = new AbstractHibernateStatelessResultAction<Long>() {
            public void execute(StatelessSession ss) throws DAOException {
                String hql = getBaseHqlByConditions(false, recipeStatus, conditionOper, containDel);
                if (StringUtils.isNotEmpty(hql)) {
                    Query q = ss.createQuery(hql.toString());
                    q.setParameter("doctorId", doctorId);
                    setResult((Long) q.uniqueResult());
                } else {
                    setResult(0L);
                }
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return Integer.parseInt(action.getResult().toString());
    }

    /**
     * 获取不同状态的处方
     *
     * @param doctorId
     * @param recipeStatus  0新开处方 1复核确认（待审核） 2审核完成 3已支付 4开始配送（开始配药） 5配送完成（发药完成） -1审核未通过 9取消
     * @param conditionOper hql where操作符
     * @param startIndex    分页起始下标
     * @param limit         分页限制条数
     * @param mark          0 新处方  1 历史处方
     * @return
     */
    public List<Recipe> findByDoctorIdAndStatus(final int doctorId, final List<Integer> recipeStatus, final String conditionOper,
                                                final boolean containDel, final int startIndex, final int limit, final int mark) {

        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder();
                String hql1 = getBaseHqlByConditions(true, recipeStatus, conditionOper, containDel);
                String orderHql;
                if (0 == mark) {
                    orderHql = "lastModify desc";
                } else {
                    orderHql = "signDate desc";
                }
                hql.append(hql1 + " order by " + orderHql);
                Query q = ss.createQuery(hql.toString());
                q.setParameter("doctorId", doctorId);
                q.setFirstResult(startIndex);
                q.setMaxResults(limit);
                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据条件获取基本hql
     *
     * @param getDetail
     * @param recipeStatus
     * @param conditionOper
     * @param containDel
     * @return
     */
    private String getBaseHqlByConditions(boolean getDetail, List<Integer> recipeStatus,
                                          String conditionOper, boolean containDel) {
        StringBuilder hql = new StringBuilder();
        if (!getDetail) {
            hql.append("select count(id)");
        }
        hql.append(" From Recipe where doctor=:doctorId and fromflag=1 ");
        hql.append(getStatusHql(recipeStatus, conditionOper, null));
        if (!containDel) {
            hql.append(" and status!=" + RecipeStatusConstant.DELETE);
        }

        return hql.toString();
    }

    private String getStatusHql(List<Integer> recipeStatus, String conditionOper, String orHql) {
        StringBuilder statusHql = new StringBuilder();
        if (null != recipeStatus) {
            if (1 == recipeStatus.size()) {
                statusHql.append(" and (status" + conditionOper + recipeStatus.get(0));
            } else if (recipeStatus.size() > 1) {
                StringBuilder statusHql1 = new StringBuilder();
                for (Integer stat : recipeStatus) {
                    statusHql1.append("," + stat);
                }
                if (statusHql1.length() > 0) {
                    statusHql.append(" and (status " + ConditionOperator.IN + " (" + statusHql1.substring(1) + ")");
                }
            }

            if (StringUtils.isNotEmpty(orHql)) {
                statusHql.append(" or " + orHql);
            }

            statusHql.append(" ) ");
        }

        return statusHql.toString();
    }

    /**
     * 保存或修改处方
     *
     * @param recipe
     * @param recipedetails
     * @param update        true 修改，false 保存
     */
    public Integer updateOrSaveRecipeAndDetail(final Recipe recipe, final List<Recipedetail> recipedetails, final boolean update) {
        HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
            public void execute(StatelessSession ss) throws DAOException {
                Recipe dbRecipe;
                if (update) {
                    dbRecipe = update(recipe);
                } else {
                    dbRecipe = save(recipe);
                }

                RecipeDetailDAO
                        recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
                for (Recipedetail detail : recipedetails) {
                    if (!update) {
                        detail.setRecipeId(dbRecipe.getRecipeId());
                    }
                    recipeDetailDAO.save(detail);
                }

                setResult(dbRecipe.getRecipeId());
            }
        };
        HibernateSessionTemplate.instance().executeTrans(action);

        return action.getResult();
    }

    /**
     * 根据处方ID修改处方状态，最后更新时间和自定义需要修改的字段
     *
     * @param status
     * @param recipeId
     * @param changeAttr 需要级联修改的其他字段
     * @return
     */
    public Boolean updateRecipeInfoByRecipeId(final int recipeId, final int status, final Map<String, ?> changeAttr) {
        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update Recipe set status=:status ");
                if (null != changeAttr && !changeAttr.isEmpty()) {
                    for (String key : changeAttr.keySet()) {
                        hql.append("," + key + "=:" + key);
                    }
                }

                hql.append(" where recipeId=:recipeId");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("status", status);
                q.setParameter("recipeId", recipeId);
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
     * 更新处方自定义字段
     *
     * @param recipeId
     * @param changeAttr
     * @return
     */
    public Boolean updateRecipeInfoByRecipeId(final int recipeId, final Map<String, ?> changeAttr) {
        if (null == changeAttr || changeAttr.isEmpty()) {
            return true;
        }

        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update Recipe set ");
                StringBuilder keyHql = new StringBuilder();
                for (String key : changeAttr.keySet()) {
                    keyHql.append("," + key + "=:" + key);
                }
                hql.append(keyHql.toString().substring(1)).append(" where recipeId=:recipeId");
                Query q = ss.createQuery(hql.toString());

                q.setParameter("recipeId", recipeId);
                for (String key : changeAttr.keySet()) {
                    q.setParameter(key, changeAttr.get(key));
                }

                int flag = q.executeUpdate();
                setResult(flag == 1);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据条件查询sql
     *
     * @param searchAttr
     * @return
     */
    public List<Recipe> findRecipeListWithConditions(final List<SqlOperInfo> searchAttr) {
        if (CollectionUtils.isEmpty(searchAttr)) {
            return null;
        }

        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from Recipe where 1=1 ");
                for (SqlOperInfo info : searchAttr) {
                    hql.append(" and " + info.getHqlCondition());
                }
                Query q = ss.createQuery(hql.toString());
                for (SqlOperInfo info : searchAttr) {
                    if (ConditionOperator.BETWEEN.equals(info.getOper())) {
                        q.setParameter(info.getKey() + SqlOperInfo.BETWEEN_START, info.getValue());
                        q.setParameter(info.getKey() + SqlOperInfo.BETWEEN_END, info.getExtValue());
                    } else {
                        q.setParameter(info.getKey(), info.getValue());
                    }

                }
                q.setMaxResults(20);

                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 批量修改处方提醒标志位
     *
     * @param recipeIds
     */
    @DAOMethod(sql = "update Recipe set remindFlag=1 where recipeId in :recipeIds")
    public abstract void updateRemindFlagByRecipeId(@DAOParam("recipeIds") List<Integer> recipeIds);

    /**
     * 批量修改处方推送药企标志位
     *
     * @param recipeIds
     */
    @DAOMethod(sql = "update Recipe set pushFlag=1 where recipeId in :recipeIds")
    public abstract void updatePushFlagByRecipeId(@DAOParam("recipeIds") List<Integer> recipeIds);

    /**
     * 根据需要变更的状态获取处方ID集合
     *
     * @param cancelStatus
     * @return
     */
    public List<Recipe> getRecipeListForCancelRecipe(final int cancelStatus, final String startDt, final String endDt) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from Recipe where fromflag=1 and signDate between '" + startDt + "' and '" + endDt + "' ");
                if (cancelStatus == RecipeStatusConstant.NO_PAY) {
                    //超过3天未支付
                    hql.append(" and status=" + RecipeStatusConstant.CHECK_PASS
                            + " and payFlag=0 and payMode=" + RecipeBussConstant.PAYMODE_ONLINE + " and orderCode is not null ");
                } else if (cancelStatus == RecipeStatusConstant.NO_OPERATOR) {
                    //超过3天未操作
                    hql.append(" and status=" + RecipeStatusConstant.CHECK_PASS
                            + " and chooseFlag=0 ");
                }
                Query q = ss.createQuery(hql.toString());
                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 处方失效前一天提醒
     *
     * @param cancelStatus
     * @return
     */
    public List<Recipe> getRecipeListForRemind(final int cancelStatus, final String startDt, final String endDt) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select new Recipe(recipeId,signDate) from Recipe where fromflag=1 and signDate between " +
                        "'" + startDt + "' and '" + endDt + "' ");
                if (cancelStatus == RecipeStatusConstant.PATIENT_NO_OPERATOR) {
                    //未操作
                    hql.append(" and status=" + RecipeStatusConstant.CHECK_PASS +
                            " and remindFlag=0 and chooseFlag=0 ");
                } else if (cancelStatus == RecipeStatusConstant.PATIENT_NO_PAY) {
                    //选择了医院取药-到院支付
                    hql.append(" and status=" + RecipeStatusConstant.CHECK_PASS +
                            " and payMode=" + RecipeBussConstant.PAYMODE_TO_HOS + " and remindFlag=0 and chooseFlag=1 and payFlag=0 ");
                } else if (cancelStatus == RecipeStatusConstant.PATIENT_NODRUG_REMIND) {
                    //选择了到店取药
                    hql.append(" and status=" + RecipeStatusConstant.CHECK_PASS_YS +
                            " and payMode=" + RecipeBussConstant.PAYMODE_TFDS + " and remindFlag=0 and chooseFlag=1 ");
                }
                Query q = ss.createQuery(hql.toString());
                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 获取3天以内医院取药的处方单，需要定时从HIS获取状态
     *
     * @return
     */
    public List<Recipe> getRecipeStatusFromHis(final String startDt, final String endDt) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from Recipe where " +
                        " (status=" + RecipeStatusConstant.HAVE_PAY + " or (status=" + RecipeStatusConstant.CHECK_PASS +
                        " and signDate between '" + startDt + "' and '" + endDt + "' ))");
                Query q = ss.createQuery(hql.toString());
                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * zhongzx
     * 查询药师审核平台待审核、已审核、或者所有的处方单的总条数
     *
     * @param organ
     * @param flag  标志位
     * @return
     */
    public Long getRecipeCountByFlag(final List<Integer> organ, final int flag) {
        //2是全部
        final int all = 2;
        HibernateStatelessResultAction<Long> action = new AbstractHibernateStatelessResultAction<Long>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                //0是待药师审核
                if (flag == 0) {
                    hql.append("select count(*) from Recipe where clinicOrgan in (:organ) and status = 8 ");
                }
                //1是已审核
                else if (flag == 1) {
                    hql.append("select count(*) from Recipe where clinicOrgan in (:organ) and checkDateYs is not null ");
                } else if (flag == all) {
                    hql.append("select count(*) from Recipe where clinicOrgan in (:organ) and (status = 8 or checkDateYs is not null) ");
                } else {
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "flag is invalid");
                }
                Query q = ss.createQuery(hql.toString());
                q.setParameterList("organ", organ);
                setResult((Long) q.uniqueResult());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * zhongzx
     * 查询药师审核平台待审核、审核通过、审核未通过、或者所有的处方单
     *
     * @param organ
     * @param flag  标志位 0-待审核 1-审核通过 2-审核未通过 3-全部
     * @param start
     * @param limit
     * @return
     */
    public List<Recipe> findRecipeByFlag(final List<Integer> organ, final int flag, final int start, final int limit) {
        final int notPass = 2;
        final int all = 3;
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                //0是待药师审核
                if (flag == 0) {
                    hql.append("from Recipe where clinicOrgan in (:organ) and status = " + RecipeStatusConstant.READY_CHECK_YS);
                }
                //1是审核通过
                else if (flag == 1) {
                    hql.append("select distinct r from Recipe r,RecipeCheck rc where r.recipeId = rc.recipeId and r.clinicOrgan in (:organ)" +
                            "and (rc.checkStatus = 1 or (rc.checkStatus=0 and r.supplementaryMemo is not null)) ");
                }
                //2是审核未通过
                else if (flag == notPass) {
                    hql.append("select distinct r from Recipe r,RecipeCheck rc where r.recipeId = rc.recipeId and r.clinicOrgan in (:organ)" +
                            "and rc.checkStatus = 0 and r.supplementaryMemo is null ");
                }
                //2是全部
                else if (flag == all) {
                    hql.append("from Recipe where clinicOrgan in (:organ) and (status = 8 or checkDateYs is not null) ");
                } else {
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "flag is invalid");
                }
                hql.append("order by signDate desc");
                Query q = ss.createQuery(hql.toString());
                q.setParameterList("organ", organ);
                q.setFirstResult(start);
                q.setMaxResults(limit);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * chuwei
     * 查询药师审核平台待审核的处方单
     *
     * @param organ
     * @return
     */
    public Boolean checkIsExistUncheckedRecipe(final int organ) {
        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "select count(*) from Recipe where clinicOrgan=:organ and status = 8";
                Query q = ss.createQuery(hql);
                q.setParameter("organ", organ);

                Long count = (Long) q.uniqueResult();
                if (count > 0) {
                    setResult(true);
                } else {
                    setResult(false);
                }

            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    private void validateOptionForStatistics(Integer status, Integer doctor, String patientName, Date bDate, Date eDate, Integer dateType,
                                             final int start, final int limit) {
        if (dateType == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "dateType is required");
        }
        if (bDate == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "统计开始时间不能为空");
        }
        if (eDate == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "统计结束时间不能为空");
        }

    }

    /**
     * 患者的来自his的处方单
     *
     * @param fromFlag    来源0his 1平台
     * @param mpiId       患者
     * @param clinicOrgan 开方机构
     * @param recipeType  处方类型:1西药 2中成药
     * @param recipeCode  处方号码
     * @return
     */
    @DAOMethod(sql = "SELECT COUNT(*) FROM Recipe where fromFlag=:fromFlag and mpiId=:mpiId and ClinicOrgan=:clinicOrgan and recipeType =:recipeType and recipeCode=:recipeCode ")
    public abstract Long getRecipeCountByMpi(@DAOParam("fromFlag") Integer fromFlag,
                                             @DAOParam("mpiId") String mpiId,
                                             @DAOParam("clinicOrgan") Integer clinicOrgan,
                                             @DAOParam("recipeType") Integer recipeType,
                                             @DAOParam("recipeCode") String recipeCode);

    /**
     * 判断来自his的处方单是否存在
     *
     * @param recipe 处方Object
     * @return
     */
    public Boolean mpiExistRecipeByMpiAndFromFlag(Recipe recipe) {
        return this.getRecipeCountByMpi(0, recipe.getMpiid(),
                recipe.getClinicOrgan(), recipe.getRecipeType(), recipe.getRecipeCode()) > 0;
    }

    /**
     * 患者mpiId修改更新信息
     *
     * @param newMpiId
     * @param oldMpiId
     * @return
     */
    public Integer updatePatientInfoForRecipe(final String newMpiId, final String oldMpiId) {
        HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update Recipe set mpiid=:mpiid where " +
                        "mpiid=:oldMpiid");

                Query q = ss.createQuery(hql.toString());
                q.setParameter("mpiid", newMpiId);
                q.setParameter("oldMpiid", oldMpiId);
                Integer recipeCount = q.executeUpdate();

                hql = new StringBuilder("update RecipeOrder set mpiId=:mpiid where " +
                        "mpiId=:oldMpiid");
                q = ss.createQuery(hql.toString());
                q.setParameter("mpiid", newMpiId);
                q.setParameter("oldMpiid", oldMpiId);
                q.executeUpdate();

                setResult(recipeCount);
            }
        };
        HibernateSessionTemplate.instance().executeTrans(action);
        return action.getResult();
    }

    /**
     * 查询过期的药师审核不通过，需要医生二次确认的处方
     *
     * @param startDt
     * @param endDt
     * @return
     */
    public List<Recipe> findCheckNotPassNeedDealList(final String startDt, final String endDt) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select r from Recipe r, RecipeOrder o where r.orderCode=o.orderCode " +
                        " and r.checkDateYs between '" + startDt + "' and '" + endDt + "' and r.status=" + RecipeStatusConstant.CHECK_NOT_PASS_YS +
                        " and o.effective=1 ");
                Query q = ss.createQuery(hql.toString());
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);

        return action.getResult();
    }

    /**
     * 查询过期的沒有確認收貨的處方單
     *
     * @param startDt
     * @param endDt
     * @return
     */
    public List<Recipe> findNotConfirmReceiptList(final String startDt, final String endDt) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select r from Recipe r where r.payMode=" + RecipeBussConstant.PAYMODE_COD +
                        " and r.sendDate between '" + startDt + "' and '" + endDt + "' and r.status=" + RecipeStatusConstant.IN_SEND);
                Query q = ss.createQuery(hql.toString());
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);

        return action.getResult();
    }

    /**
     * 查询处方列表
     *
     * @param status      处方状态
     * @param doctor      开方医生
     * @param patientName 患者姓名（原为患者主键 mpiid）
     * @param bDate       开始时间
     * @param eDate       结束时间
     * @param dateType    时间类型（0：开方时间，1：审核时间）
     * @param start       分页开始index
     * @param limit       分页长度
     * @return QueryResult<Map>
     */
    public QueryResult<Map> findRecipesByInfo(final Integer organId, final Integer status, final Integer doctor, final String patientName, final Date bDate, final Date eDate, final Integer dateType,
                                              final Integer depart, final int start, final int limit, List<Integer> organIds, Integer giveMode, Integer fromflag,Integer recipeId) {
        this.validateOptionForStatistics(status, doctor, patientName, bDate, eDate, dateType, start, limit);
        final StringBuilder preparedHql = this.generateHQLforStatistics(organId, status, doctor, patientName, dateType, depart, organIds, giveMode, fromflag,recipeId);
        final PatientService patientService = BasicAPI.getService(PatientService.class);
        HibernateStatelessResultAction<QueryResult<Map>> action =
                new AbstractHibernateStatelessResultAction<QueryResult<Map>>() {
                    public void execute(StatelessSession ss) throws DAOException {
                        StringBuilder sbHql = preparedHql;
                        Query countQuery = ss.createQuery("select count(*) " + sbHql.toString());
                        countQuery.setDate("startTime", bDate);
                        countQuery.setDate("endTime", eDate);
                        Long total = (Long) countQuery.uniqueResult();
                        Query query = ss.createQuery(sbHql.append(" order by recipeId DESC").toString());
                        query.setDate("startTime", bDate);
                        query.setDate("endTime", eDate);
                        query.setFirstResult(start);
                        query.setMaxResults(limit);
                        List<Recipe> recipeList = query.list();
                        List<Map> maps = new ArrayList<Map>();
                        if (recipeList != null) {

                            RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
                            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                            for (Recipe recipe : recipeList) {
                                Map<String, Object> map = Maps.newHashMap();
                                BeanUtils.map(recipe, map);
                                map.put("detailCount", recipeDetailDAO.getCountByRecipeId(recipe.getRecipeId()));
                                PatientDTO patientBean;
                                try {
                                    patientBean = patientService.get(recipe.getMpiid());
                                } catch (Exception e) {
                                    patientBean = new PatientDTO();
                                }
                                map.put("patient", patientBean);
                                Integer enterpriseId = recipe.getEnterpriseId();
                                if (enterpriseId != null) {
                                    DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(enterpriseId);
                                    if(null != drugsEnterprise) {
                                        map.put("drugsEnterprise", drugsEnterprise.getName());
                                    }
                                }
                                maps.add(map);
                            }
                        }
                        setResult(new QueryResult<Map>(total, query.getFirstResult(), query.getMaxResults(), maps));
                    }
                };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 查询处方列表
     *
     * @param status      处方状态
     * @param doctor      开方医生
     * @param patientName 患者姓名（原为患者主键 mpiid）
     * @param bDate       开始时间
     * @param eDate       结束时间
     * @param dateType    时间类型（0：开方时间，1：审核时间）
     * @return QueryResult<Map>
     */
    public List<Map> findRecipesByInfoForExcel(final Integer organId, final Integer status, final Integer doctor, final String patientName, final Date bDate, final Date eDate, final Integer dateType,
                                               final Integer depart, List<Integer> organIds, Integer giveMode, Integer fromflag,Integer recipeId) {
        this.validateOptionForStatistics(status, doctor, patientName, bDate, eDate, dateType, 0, Integer.MAX_VALUE);
        final StringBuilder preparedHql = this.generateHQLforStatistics(organId, status, doctor, patientName, dateType, depart, organIds, giveMode, fromflag,recipeId);
        final PatientService patientService = BasicAPI.getService(PatientService.class);
        final DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        HibernateStatelessResultAction<List<Map>> action =
                new AbstractHibernateStatelessResultAction<List<Map>>() {
                    public void execute(StatelessSession ss) throws DAOException {
                        StringBuilder sbHql = preparedHql;
                        Query query = ss.createQuery(sbHql.append(" order by recipeId DESC").toString());
                        query.setDate("startTime", bDate);
                        query.setDate("endTime", eDate);
                        List<Recipe> recipeList = query.list();
                        Set<String> mpiIds = Sets.newHashSet();
                        Set<Integer> doctorIds = Sets.newHashSet();
                        Map<String, PatientDTO> patientBeanMap = Maps.newHashMap();
                        Map<Integer, DoctorDTO> doctorBeanMap = Maps.newHashMap();
                        List<Map> maps = new ArrayList<Map>();
                        if (recipeList != null) {
                            for (Recipe recipe : recipeList) {
                                mpiIds.add(recipe.getMpiid());
                                doctorIds.add(recipe.getDoctor());
                            }
                            List<PatientDTO> patientBeanList = patientService.findByMpiIdIn(new ArrayList<String>(mpiIds));
                            List<DoctorDTO> doctorBeen = Lists.newArrayList();
                            if (doctorIds.size() > 0) {
                                doctorBeen = doctorService.findDoctorList(new ArrayList<>(doctorIds));
                            }
                            for (PatientDTO p : patientBeanList) {
                                patientBeanMap.put(p.getMpiId(), p);
                            }
                            if (doctorBeen != null && doctorBeen.size() > 0) {
                                for (DoctorDTO d : doctorBeen) {
                                    doctorBeanMap.put(d.getDoctorId(), d);
                                }
                            }
                            RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
                            for (Recipe recipe : recipeList) {
                                String mpiId = recipe.getMpiid();
                                Integer doctorId = recipe.getDoctor();
                                PatientDTO patient = patientBeanMap.get(mpiId);
                                DoctorDTO doctor = doctorBeanMap.get(doctorId);
                                Map<String, Object> map = Maps.newHashMap();
                                BeanUtils.map(recipe, map);
                                map.put("detailCount", recipeDetailDAO.getCountByRecipeId(recipe.getRecipeId()));
                                if (patient != null) {
                                    map.put("patientName", patient.getPatientName());
                                    map.put("patientMobile", patient.getMobile());
                                }
                                if (doctor != null) {
                                    map.put("doctorMobile", doctor.getMobile());
                                }
                                maps.add(map);
                            }
                        }
                        setResult(maps);
                    }
                };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据状态统计
     *
     * @param status   处方状态
     * @param doctor   开方医生
     * @param mpiid    患者主键
     * @param bDate    开始时间
     * @param eDate    结束时间
     * @param dateType 时间类型（0：开方时间，1：审核时间）
     * @param start    分页开始index
     * @param limit    分页长度
     * @return HashMap<String, Integer>
     */
    public HashMap<String, Integer> getStatisticsByStatus(final Integer organId, final Integer status, final Integer doctor, final String mpiid, final Date bDate, final Date eDate, final Integer dateType,
                                                          final Integer depart, final int start, final int limit, List<Integer> organIds, Integer giveMode, Integer fromflag,Integer recipeId) {
        this.validateOptionForStatistics(status, doctor, mpiid, bDate, eDate, dateType, start, limit);
        final StringBuilder preparedHql = this.generateHQLforStatistics(organId, status, doctor, mpiid,dateType, depart, organIds, giveMode, fromflag,recipeId);
        HibernateStatelessResultAction<HashMap<String, Integer>> action = new AbstractHibernateStatelessResultAction<HashMap<String, Integer>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws Exception {
                long total = 0;
                StringBuilder hql = preparedHql;
                hql.append(" group by status ");
                Query query = ss.createQuery("select status, count(recipeId) as count " + hql.toString());
                query.setDate("startTime", bDate);
                query.setDate("endTime", eDate);
                List<Object[]> tfList = query.list();
                HashMap<String, Integer> mapStatistics = Maps.newHashMap();
                if (tfList.size() > 0) {
                    for (Object[] hps : tfList) {
                        if (hps[0] != null && !StringUtils.isEmpty(hps[0].toString())) {
                            String status = hps[0].toString();
                            String statusName = DictionaryController.instance()
                                    .get("eh.cdr.dictionary.RecipeStatus").getText(status);
                            mapStatistics.put(statusName, Integer.parseInt(hps[1].toString()));
                        }
                    }
                }
                setResult(mapStatistics);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    private StringBuilder generateHQLforStatistics(Integer organId,
                                                   Integer status, Integer doctor, String patientName, Integer dateType,
                                                   Integer depart, final List<Integer> requestOrgans, Integer giveMode, Integer fromflag,Integer recipeId) {
        StringBuilder hql = new StringBuilder(" from Recipe where 1=1 ");

        //默认查询所有
        if (CollectionUtils.isNotEmpty(requestOrgans)) {
            // 添加申请机构条件
            boolean flag = true;
            for (Integer i : requestOrgans) {
                if (i != null) {
                    if (flag) {
                        hql.append(" and clinicOrgan in(");
                        flag = false;
                    }
                    hql.append(i + ",");
                }
            }
            if (!flag) {
                hql = new StringBuilder(hql.substring(0,
                        hql.length() - 1) + ") ");
            }
        }
        if (organId != null) {
            hql.append(" and clinicOrgan =" + organId);
        }
        switch (dateType) {
            case 0:
                //开方时间
                hql.append(" and DATE(createDate)>=DATE(:startTime)"
                        + " and DATE(createDate)<=DATE(:endTime) ");
                break;
            case 1:
                //审核时间
                hql.append(" and DATE(checkDate)>=DATE(:startTime)"
                        + " and DATE(checkDate)<=DATE(:endTime) ");
                break;
            default:
                break;
        }
        if (status != null) {
            hql.append(" and status =").append(status);
        }
        if (doctor != null) {
            hql.append(" and doctor=").append(doctor);
        }
        //根据患者姓名  精确查询
        if (patientName != null && !StringUtils.isEmpty(patientName.trim())) {
            hql.append(" and patientName='").append(patientName).append("'");
        }
        if (depart != null) {
            hql.append(" and depart=").append(depart);
        }
        if (giveMode != null) {
            hql.append(" and giveMode=").append(giveMode);
        }
        if (fromflag != null) {
            hql.append(" and fromflag=").append(fromflag);
        }
        if (recipeId != null){
            hql.append(" and recipeId=").append(recipeId);
        }
        return hql;
    }

    /**
     * 根据电话号码获取处方单
     *
     * @param mpis
     * @return
     */
    public List<Map> queryRecipesByMobile(final List<String> mpis) {
        //final IPatientService patientService = ApplicationUtils.getBaseService(IPatientService.class);
        HibernateStatelessResultAction<List<Map>> action = new AbstractHibernateStatelessResultAction<List<Map>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select r from Recipe r where DATE(r.createDate)>=DATE(:startTime) and DATE(r.createDate)<=DATE(:endTime) and r.mpiid in(:mpis)");
                Query query = ss.createQuery(hql.toString());
                query.setParameterList("mpis", mpis);
                query.setParameter("endTime", new Date());
                query.setParameter("startTime", DateUtils.addMonths(new Date(), -3));
                List<Recipe> recipeList = query.list();
                List<Map> maps = new ArrayList<Map>();
                if (recipeList != null) {
                    RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
                    for (Recipe recipe : recipeList) {
                        Map<String, Object> map = Maps.newHashMap();
                        BeanUtils.map(recipe, map);
                        map.put("detailCount", recipeDetailDAO.getCountByRecipeId(recipe.getRecipeId()));
                        //map.put("patient",patientService.get(recipe.getMpiid()));
                        maps.add(map);
                    }
                }
                setResult(maps);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public List<Integer> findDoctorIdsByStatus(final Integer status) {
        HibernateStatelessResultAction<List<Integer>> action = new AbstractHibernateStatelessResultAction<List<Integer>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select r.doctor from Recipe r where r.status=:status group by r.doctor order by count(r.doctor) desc");
                Query query = ss.createQuery(hql.toString());
                query.setParameter("status", status);
                List<Integer> doctorIds = query.list();
                setResult(doctorIds);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public List<String> findPatientMpiIdForOp(final List<String> mpiIds, final List<Integer> organIds) {
        HibernateStatelessResultAction<List<String>> action = new AbstractHibernateStatelessResultAction<List<String>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select DISTINCT r.mpiid FROM Recipe r WHERE r.mpiid in(:mpiIds) ");
                if (null != organIds && organIds.size() > 0) {
                    hql.append(" and r.clinicOrgan in (:organIds) ");
                }
                Query query = ss.createQuery(hql.toString());
                query.setParameterList("mpiIds", mpiIds);
                if (null != organIds && organIds.size() > 0) {
                    query.setParameterList("organIds", organIds);
                }
                List<String> mpiIds = query.list();
                setResult(mpiIds);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public List<String> findCommonDiseasByDoctorAndOrganId(final int doctorId, final int organId) {

        final String endDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(0), DateConversion.DEFAULT_DATE_TIME);
        final String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(90), DateConversion.DEFAULT_DATE_TIME);
        //查询医生三个月内开的药品数据
        HibernateStatelessResultAction<List<String>> action = new AbstractHibernateStatelessResultAction<List<String>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "SELECT r.organDiseaseId FROM Recipe r WHERE " +
                        "r.doctor = :doctorId AND r.clinicOrgan = :organId AND r.createDate between '" + startDt + "' and '" + endDt + "'";
                Query query = ss.createQuery(hql);
                query.setParameter("doctorId", doctorId);
                query.setParameter("organId", organId);
                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        //单独处理多诊断的处方单
        List<String> list = action.getResult();
        if (list == null || list.size() == 0) {
            return null;
        }
        Map<String, Integer> diseaseMap = Maps.newHashMap();
        //循环计算每一个诊断的次数
        for (String s : list) {
            String[] strings = s.split(";");
            if (strings != null && strings.length > 0) {
                for (String s1 : strings) {
                    Integer i = diseaseMap.get(s1);
                    if (i == null) {
                        diseaseMap.put(s1, 1);
                    } else {
                        diseaseMap.put(s1, i + 1);
                    }
                }
            }
        }
        //将map装换为list并按开诊断个数排序
        List<Map.Entry<String, Integer>> entryList = new ArrayList<Map.Entry<String, Integer>>(diseaseMap.entrySet());
        Collections.sort(entryList, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue() - o1.getValue();
            }
        });
        List<String> diseaseIds = Lists.newArrayList();
        Iterator<Map.Entry<String, Integer>> iterator = entryList.iterator();
        while (iterator.hasNext()) {
            diseaseIds.add(iterator.next().getKey());
        }
        return diseaseIds;
    }


    /**
     * 查询指定医生开过处方的患者Id列表
     *
     * @param doctorId
     * @param start
     * @return
     */
    public List<String> findHistoryMpiIdsByDoctorId(final int doctorId, final Integer start, final Integer limit) {

        HibernateStatelessResultAction<List<String>> action = new AbstractHibernateStatelessResultAction<List<String>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "SELECT r.mpiid FROM Recipe r WHERE " +
                        "r.doctor = :doctorId AND r.patientStatus = 1 group by r.mpiid order by r.createDate desc";
                Query query = ss.createQuery(hql);
                query.setParameter("doctorId", doctorId);
                if (null != start && null != limit) {
                    query.setFirstResult(start);
                    query.setMaxResults(limit);
                }
                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);

        List<String> mpiIds = action.getResult();
        return mpiIds;
    }

    /**
     * 同步患者状态
     *
     * @param mpiId
     */
    @DAOMethod(sql = "update Recipe set patientStatus=0 where mpiId =:mpiId")
    public abstract void updatePatientStatusByMpiId(@DAOParam("mpiId") String mpiId);

    /**
     * 查找指定医生和患者间开的处方单列表
     *
     * @param doctorId
     * @param mpiId
     * @param start
     * @param limit
     * @return
     */
    public List<Recipe> findRecipeListByDoctorAndPatient(final Integer doctorId, final String mpiId,
                                                          final Integer start, final Integer limit) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "from Recipe where mpiid=:mpiid and doctor=:doctor and status > " +
                        RecipeStatusConstant.UNSIGN + " and status not in (" + RecipeStatusConstant.CHECKING_HOS +
                        ", " + RecipeStatusConstant.DELETE + ")" +
                        " order by createDate desc";
                Query query = ss.createQuery(hql);
                query.setParameter("doctor", doctorId);
                query.setParameter("mpiid", mpiId);
                if (null != start && null != limit) {
                    query.setFirstResult(start);
                    query.setMaxResults(limit);
                }
                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);

        List<Recipe> recipes = action.getResult();
        return recipes;
    }

    /**
     * 获取HOS历史处方
     * @param doctorId
     * @param mpiId
     * @param clinicOrgan
     * @param start
     * @param limit
     * @return
     */
    public List<Recipe> findHosRecipe(final Integer doctorId, final String mpiId, final Integer clinicOrgan,
                                                         final Integer start, final Integer limit) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "from Recipe where mpiid=:mpiid and doctor=:doctor and clinicOrgan=:clinicOrgan order by createDate desc";
                Query query = ss.createQuery(hql);
                query.setParameter("doctor", doctorId);
                query.setParameter("mpiid", mpiId);
                query.setParameter("clinicOrgan", clinicOrgan);
                if (null != start && null != limit) {
                    query.setFirstResult(start);
                    query.setMaxResults(limit);
                }
                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);

        List<Recipe> recipes = action.getResult();
        return recipes;
    }

    /**
     * 根据日期范围，机构归类的业务量（天，月）
     *
     * @param startDate 起始日期
     * @param endDate   结束日期
     */
    public HashMap<Integer, Long> getCountByDateAreaGroupByOrgan(final String startDate, final String endDate) {
        HibernateStatelessResultAction<HashMap<Integer, Long>> action = new AbstractHibernateStatelessResultAction<HashMap<Integer, Long>>() {
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder(
                        "select clinicOrgan, count(*) as count from Recipe a  where a.fromflag=1 and a.createDate between :startDate and :endDate  group by clinicOrgan");
                Query query = ss.createQuery(hql.toString());
                Date dStartDate = DateConversion.convertFromStringDate(startDate);
                Date dEndDate = DateConversion.convertFromStringDate(endDate);
                Date sTime = DateConversion.firstSecondsOfDay(dStartDate);
                Date eTime = DateConversion.lastSecondsOfDay(dEndDate);
                query.setParameter("startDate", sTime);
                query.setParameter("endDate", eTime);
                List<Object[]> oList = query.list();
                HashMap<Integer, Long> organCount = Maps.newHashMap();
                for (Object[] co : oList) {
                    organCount.put((Integer) co[0], (Long) co[1]);
                }
                setResult(organCount);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据日期范围，机构归类的业务量（小时）
     *
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return
     */
    public HashMap<Object, Integer> getCountByHourAreaGroupByOrgan(final Date startDate, final Date endDate) {
        HibernateStatelessResultAction<HashMap<Object, Integer>> action = new AbstractHibernateStatelessResultAction<HashMap<Object, Integer>>() {
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder(
                        "select count(*) as count, HOUR(a.createDate) as hour from Recipe a  where a.fromflag=1 and a.createDate between :startDate and :endDate  group by HOUR(a.createDate)");
                Query query = ss.createQuery(hql.toString());
                query.setParameter("startDate", startDate);
                query.setParameter("endDate", endDate);
                List<Object[]> oList = query.list();
                HashMap<Object, Integer> organCount = Maps.newHashMap();
                for (Object[] co : oList) {
                    organCount.put(co[1], ((Long) co[0]).intValue());
                }
                setResult(organCount);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    //根据机构归类的业务量
    public HashMap<Integer, Long> getCountGroupByOrgan() {
        HibernateStatelessResultAction<HashMap<Integer, Long>> action = new AbstractHibernateStatelessResultAction<HashMap<Integer, Long>>() {
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder(
                        "select clinicOrgan, count(*) as count from Recipe a where a.fromflag=1 group by clinicOrgan");
                Query query = ss.createQuery(hql.toString());
                List<Object[]> oList = query.list();
                HashMap<Integer, Long> organCount = new HashMap<Integer, Long>();
                for (Object[] co : oList) {
                    organCount.put((Integer) co[0], (Long) co[1]);
                }
                setResult(organCount);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public HashMap<Integer, Long> getRecipeRequestCountGroupByDoctor() {
        HibernateStatelessResultAction<HashMap<Integer, Long>> action = new AbstractHibernateStatelessResultAction<HashMap<Integer, Long>>() {
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder(
                        "select doctor, count(*) as count from Recipe a where a.doctor > 0 and a.fromflag=1  group by doctor");
                Query query = ss.createQuery(hql.toString());
                List<Object[]> oList = query.list();
                HashMap<Integer, Long> organCount = new HashMap<Integer, Long>();
                for (Object[] co : oList) {
                    organCount.put((Integer) co[0], (Long) co[1]);
                }
                setResult(organCount);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public List<String> findAllMpiIdsFromHis(){
        HibernateStatelessResultAction<List<String>> action = new AbstractHibernateStatelessResultAction<List<String>>() {
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder(
                        "select mpiid from Recipe where fromflag=0 group by mpiid");
                Query query = ss.createQuery(hql.toString());

                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }
}
