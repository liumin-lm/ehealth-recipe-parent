package recipe.dao;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.common.dto.DepartChargeReportResult;
import com.ngari.common.dto.HosBusFundsReportResult;
import com.ngari.recipe.dto.DoctorRecipeListReqDTO;
import com.ngari.recipe.dto.PatientRecipeListReqDTO;
import com.ngari.recipe.dto.RecipeRefundDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.entity.Symptom;
import com.ngari.recipe.recipe.model.*;
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
import ctd.util.annotation.RpcSupportDAO;
import eh.utils.ValidateUtil;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.StatelessSession;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.aop.LogRecord;
import recipe.constant.ConditionOperator;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.bean.PatientRecipeBean;
import recipe.dao.bean.RecipeListBean;
import recipe.dao.bean.RecipeRollingInfo;
import recipe.dao.comment.ExtendDao;
import recipe.util.DateConversion;
import recipe.util.LocalStringUtil;
import recipe.vo.second.AutomatonCountVO;
import recipe.vo.second.AutomatonVO;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 处方DAO
 *
 * @author yuyun
 */
@RpcSupportDAO
public abstract class RecipeDAO extends HibernateSupportDelegateDAO<Recipe> implements ExtendDao<Recipe> {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    public RecipeDAO() {
        super();
        this.setEntityName(Recipe.class.getName());
        this.setKeyField("recipeId");
    }

    @Override
    public boolean updateNonNullFieldByPrimaryKey(Recipe recipe) {
        return updateNonNullFieldByPrimaryKey(recipe, "recipeId");
    }

    @DAOMethod(sql = "from Recipe where fromflag in (1,2) order by createDate desc")
    public abstract List<Recipe> findRecipeByStartAndLimit(@DAOParam(pageStart = true) int start, @DAOParam(pageLimit = true) int limit);

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
     * 根据订单编号获取处方列表
     *
     * @param orderCode
     * @return
     */
    @DAOMethod(sql = "from Recipe where orderCode=:orderCode order by recipeId desc")
    public abstract List<Recipe> findSortRecipeListByOrderCode(@DAOParam("orderCode") String orderCode);

    /**
     * 根据订单编号获取处方id集合
     *
     * @param orderCode
     * @return
     */
    @DAOMethod(sql = "select recipeId from Recipe where orderCode=:orderCode")
    public abstract List<Integer> findRecipeIdsByOrderCode(@DAOParam("orderCode") String orderCode);

    /**
     * 根据机构和时间获取his处方号
     *
     * @param organId
     * @param start
     * @param end
     * @return
     */
    public List<Recipe> findRecipeCodesByOrderIdAndTime(final Integer organId, final Date start, final Date end) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "from Recipe where  RecipeCode is not null and ClinicOrgan=:organId and createDate>=:start and createDate<=:end ";
                Query q = ss.createQuery(hql);
                q.setParameter("organId", organId);
                q.setParameter("start", start);
                q.setParameter("end", end);
                q.setFirstResult(0);
                q.setMaxResults(100);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }


    /**
     * @param ids        in 语句集合对象
     * @param splitCount in 语句中出现的条件个数
     * @param field      in 语句对应的数据库查询字段
     * @return
     */
    private static String getSqlIn(List<Integer> ids, int splitCount, String field) {
        splitCount = Math.min(splitCount, 1000);
        int len = ids.size();
        int size = len % splitCount;
        if (size == 0) {
            size = len / splitCount;
        } else {
            size = (len / splitCount) + 1;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            int fromIndex = i * splitCount;
            int toIndex = Math.min(fromIndex + splitCount, len);

            String yjdNbr = StringUtils.defaultIfEmpty(StringUtils.join(ids.subList(fromIndex, toIndex), ","), "");
            if (i != 0) {
                builder.append(" or ");
            }
            builder.append(field).append(" in (").append(yjdNbr).append(")");
        }
        return StringUtils.defaultIfEmpty(builder.toString(), field + " in ('')");
    }

    /**
     * 审核完成之后，若患者还未支付
     *
     * @param payFlag
     * @param reviewType
     * @return
     */
    @DAOMethod(sql = "from Recipe where TO_DAYS(NOW()) - TO_DAYS(createDate) <= valueDays and Status =2  and PayFlag =:payFlag and ReviewType =:reviewType", limit = 0)
    public abstract List<Recipe> findByPayFlagAndReviewType(@DAOParam("payFlag") Integer payFlag, @DAOParam("reviewType") Integer reviewType);

    /**
     * 根据处方id获取状态
     *
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "select status from Recipe where recipeId=:recipeId")
    public abstract Integer getStatusByRecipeId(@DAOParam("recipeId") Integer recipeId);

    /**
     * 根据订单编号及开放机构查询处方
     *
     * @param recipeCode
     * @param clinicOrgan
     * @return
     */
    @DAOMethod(sql = "from Recipe where recipeCode=:recipeCode and clinicOrgan=:clinicOrgan and fromflag in (1,2)")
    public abstract Recipe getByRecipeCodeAndClinicOrgan(@DAOParam("recipeCode") String recipeCode, @DAOParam("clinicOrgan") Integer clinicOrgan);

    /**
     * 根据订单编号及开放机构查询处方
     *
     * @param recipeCode
     * @param clinicOrgan
     * @return
     */
    @DAOMethod(sql = "from Recipe where recipeCode=:recipeCode and clinicOrgan=:clinicOrgan and fromflag in (1,2,0)")
    public abstract Recipe getByHisRecipeCodeAndClinicOrgan(@DAOParam("recipeCode") String recipeCode, @DAOParam("clinicOrgan") Integer clinicOrgan);

    /**
     * 根据处方id集合获取处方集合
     *
     * @param recipeIds
     * @return
     */
    @DAOMethod(sql = "from Recipe where recipeId in :recipeIds order by recipeId desc", limit = 0)
    public abstract List<Recipe> findByRecipeIds(@DAOParam("recipeIds") Collection<Integer> recipeIds);

    @DAOMethod(sql = "from Recipe where orderCode in (:orderCodeList)", limit = 0)
    public abstract List<Recipe> findByOrderCode(@DAOParam("orderCodeList") Collection<String> orderCodeList);

    /**
     * 查询所有处方
     *
     * @param recipeCode
     * @param clinicOrgan
     * @return
     */
    @DAOMethod(sql = "from Recipe where recipeCode=:recipeCode and clinicOrgan=:clinicOrgan")
    public abstract Recipe getByRecipeCodeAndClinicOrganWithAll(@DAOParam("recipeCode") String recipeCode, @DAOParam("clinicOrgan") Integer clinicOrgan);

    @DAOMethod(sql = "from Recipe where recipeCode=:recipeCode and clinicOrgan=:clinicOrgan and mpiid=:mpiid")
    public abstract Recipe getByRecipeCodeAndClinicOrganAndMpiid(@DAOParam("recipeCode") String recipeCode, @DAOParam("clinicOrgan") Integer clinicOrgan,@DAOParam("mpiid") String mpiid);

    @DAOMethod(sql = "from Recipe where recipeCode in (:recipeCodeList) and clinicOrgan=:clinicOrgan")
    public abstract List<Recipe> findByRecipeCodeAndClinicOrgan(@DAOParam("recipeCodeList") List<String> recipeCodeList, @DAOParam("clinicOrgan") Integer clinicOrgan);

    @DAOMethod(sql = "select COUNT(*) from Recipe where  clinicOrgan=:organId and  PayFlag =:payFlag and status in (2,8)")
    public abstract Long getUnfinishedRecipe(@DAOParam("organId") Integer organId, @DAOParam("payFlag") Integer payFlag);

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
    public abstract List<Recipe> findRecipesForDoctor(@DAOParam("doctorId") Integer doctorId, @DAOParam("recipeId") Integer recipeId, @DAOParam(pageStart = true) int start, @DAOParam(pageLimit = true) int limit);

    /**
     * 根据处方id集合更新订单编号
     *
     * @param recipeIds
     * @param orderCode
     */
    @DAOMethod(sql = "update Recipe set orderCode=:orderCode where recipeId in :recipeIds")
    public abstract void updateOrderCodeByRecipeIds(@DAOParam("recipeIds") List<Integer> recipeIds, @DAOParam("orderCode") String orderCode);

    /**
     * 根据处方id集合重置药企推送状态
     */
    @DAOMethod(sql = "update Recipe set PushFlag = 0 where recipeId in :recipeIds")
    public abstract void updateRecipePushFlag(@DAOParam("recipeIds") List<Integer> recipeIds);

    @DAOMethod(sql = "update Recipe set giveDate=:finishDate,status=6,giveFlag=1 where recipeId in :recipeIds")
    public abstract void updateRecipeFinishInfoByRecipeIds(@DAOParam("recipeIds") List<Integer> recipeIds, @DAOParam("finishDate") Date finishDate);

    @DAOMethod(sql = "from Recipe where groupCode=:groupCode")
    public abstract List<Recipe> findRecipeByGroupCode(@DAOParam("groupCode") String groupCode);

    @DAOMethod(sql = "from Recipe where groupCode=:groupCode and status in (:status) ")
    public abstract List<Recipe> findRecipeByGroupCodeAndStatus(@DAOParam("groupCode") String groupCode, @DAOParam("status") List<Integer> status);

    /**
     * 根据 第三方id 与 状态 获取最新处方id
     *
     * @param clinicId 第三方关联id （目前只有复诊）
     * @param status   处方状态
     * @return
     */
    public Recipe getByClinicIdAndStatus(@DAOParam("clinicId") Integer clinicId, @DAOParam("status") Integer status) {
        if (null == clinicId || null == status) {
            return null;
        }
        HibernateStatelessResultAction<Recipe> action = new AbstractHibernateStatelessResultAction<Recipe>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                Query q = ss.createQuery("from Recipe where recipeSourceType  in (1,2) and Status=:status and ClinicID=:clinicId order by RecipeID desc");
                q.setParameter("clinicId", clinicId);
                q.setParameter("status", status);
                q.setMaxResults(1);
                setResult((Recipe) q.uniqueResult());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据订单编号更新订单编号为空
     *
     * @param orderCode
     */
    public void updateOrderCodeToNullByOrderCodeAndClearChoose(String orderCode, Recipe recipe, int flag,boolean canCancelOrderCode) {
        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update Recipe set ");

                //药师
                if (flag == 2) {
                    hql.append(" status = 8, ");
                } else {
                    hql.append(" status = 2, ");
                }
                if(canCancelOrderCode){
                    //非北京互联网模式设置为null
                    if (!new Integer(2).equals(recipe.getRecipeSource())) {
                        hql.append(" giveMode = null, ");
                    }
                    hql.append(" orderCode=null ,");
                }
                hql.append(" chooseFlag=0 where orderCode=:orderCode");
                Query q = ss.createQuery(hql.toString());

                q.setParameter("orderCode", orderCode);
                q.executeUpdate();
            }
        };
        HibernateSessionTemplate.instance().execute(action);
    }

    /**
     * 根据处方单号更新订单编号为空
     *
     * @param
     */
    public void updateOrderCodeToNullByRecipeId(Recipe recipe, int flag,boolean canCancelOrderCode) {
        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update Recipe set ");

                //药师
                if (flag == 2) {
                    hql.append(" status = 8, ");
                } else {
                    hql.append(" status = 2, ");
                }
                if(canCancelOrderCode){
                    //非北京互联网模式设置为null
                    if (!new Integer(2).equals(recipe.getRecipeSource())) {
                        hql.append(" giveMode = null, ");
                    }
                    hql.append(" orderCode=null ,");
                }
                hql.append(" chooseFlag=0 where recipeId=:recipeId");
                Query q = ss.createQuery(hql.toString());

                q.setParameter("recipeId", recipe.getRecipeId());
                q.executeUpdate();
            }
        };
        HibernateSessionTemplate.instance().execute(action);
    }

    public void updateOrderCodeToNullByRecipeIdAndStatus(Recipe recipe, int flag,boolean canCancelOrderCode) {
        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update Recipe set ");

                hql.append(" status = 6, ");
                if(canCancelOrderCode){
                    //非北京互联网模式设置为null
                    if (!new Integer(2).equals(recipe.getRecipeSource())) {
                        hql.append(" giveMode = null, ");
                    }
                    hql.append(" orderCode=null ,");
                }
                hql.append(" chooseFlag=0 where recipeId=:recipeId");
                Query q = ss.createQuery(hql.toString());

                q.setParameter("recipeId", recipe.getRecipeId());
                q.executeUpdate();
            }
        };
        HibernateSessionTemplate.instance().execute(action);
    }

    /**
     * 根据处方id批量删除
     *
     * @param recipeIds
     */
    @DAOMethod(sql = "delete from Recipe where recipeId in (:recipeIds)")
    public abstract void deleteByRecipeIds(@DAOParam("recipeIds") List<Integer> recipeIds);

    public List<Integer> findDoctorIdSortByCount(final String startDt, final String endDt, final List<Integer> organs, final List<Integer> testDocIds, final int start, final int limit) {
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
                hql.append(" r.signDate between '" + startDt + "' and '" + endDt + "' and r.status=" + RecipeStatusConstant.FINISH + " GROUP BY r.doctor ORDER BY count(r.doctor) desc");

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

    public List<RecipeRollingInfo> findLastesRecipeList(final String startDt, final String endDt, final List<Integer> organs, final List<Integer> testDocIds, final int start, final int limit) {
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

    /**
     * 保存或更新recipe
     *
     * @param recipe
     */
    public Recipe saveOrUpdate(Recipe recipe) {
        if (null == recipe.getRecipeId()) {
            return save(recipe);
        } else {
            return update(recipe);
        }
    }

    public List<Integer> findPendingRecipes(final List<String> allMpiIds, final Integer status, final int start, final int limit) {
        HibernateStatelessResultAction<List<Integer>> action = new AbstractHibernateStatelessResultAction<List<Integer>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select r.recipeId from cdr_recipe r left join cdr_recipeorder o on r.orderCode=o.orderCode where r.status=:status " + "and r.chooseFlag=0 and (o.effective is null or o.effective=0 ) and r.mpiid in :mpiIds order by r.signDate desc");
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


    public List<PatientRecipeBean> findOtherRecipesForPatient(final List<String> mpiIdList, final List<Integer> notInRecipeIds, final int start, final int limit) {
        HibernateStatelessResultAction<List<PatientRecipeBean>> action = new AbstractHibernateStatelessResultAction<List<PatientRecipeBean>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select s.type,s.recordCode,s.recordId,s.mpiId,s.diseaseName,s.status,s.fee," + "s.recordDate,s.couponId,s.medicalPayFlag,s.recipeType,s.organId,s.recipeMode,s.giveMode from (");
                hql.append("SELECT 1 as type,null as couponId, t.MedicalPayFlag as medicalPayFlag, t.RecipeID as recordCode,t.RecipeID as recordId," + "t.MPIID as mpiId,t.OrganDiseaseName as diseaseName,t.Status,t.TotalMoney as fee," + "t.SignDate as recordDate,t.RecipeType as recipeType,t.ClinicOrgan as organId,t.recipeMode as recipeMode,t.giveMode as giveMode FROM cdr_recipe t " + "left join cdr_recipeorder k on t.OrderCode=k.OrderCode ");
                hql.append("WHERE t.MPIID IN (:mpiIdList) and (k.Effective is null or k.Effective = 0) ").append("and (t.ChooseFlag=1 or (t.ChooseFlag=0 and t.Status=" + RecipeStatusConstant.CHECK_PASS + ")) ");
                if (CollectionUtils.isNotEmpty(notInRecipeIds)) {
                    hql.append("and t.RecipeID not in (:notInRecipeIds) ");
                }
                hql.append("UNION ALL ");
                hql.append("SELECT 2 as type,o.CouponId as couponId, 0 as medicalPayFlag, " + "o.OrderCode as recordCode,o.OrderId as recordId,o.MpiId as mpiId,'' as diseaseName," + "o.Status,o.ActualPrice as fee,o.CreateTime as recordDate,0 as recipeType, o.OrganId, 'ngarihealth' as recipeMode,w.GiveMode AS giveMode FROM cdr_recipeorder o JOIN cdr_recipe w ON o.OrderCode = w.OrderCode " + "AND o.MpiId IN (:mpiIdList) and o.Effective = 1 ");
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
                        patientRecipeBean.setRecipeMode(objs[12].toString());
                        if (null != objs[13]) {
                            patientRecipeBean.setGiveMode(Integer.parseInt(objs[13].toString()));
                        }
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
     * 根据开方时间查询处方订单药品表
     *
     * @param organId
     * @param depart
     * @param createTime
     * @return
     */
    public List<DepartChargeReportResult> findRecipeByOrganIdAndCreateTimeAnddepart(Integer organId, Integer depart, Date createTime, Date endTime) {
        final String start = DateConversion.getDateFormatter(createTime, DateConversion.DEFAULT_DATE_TIME);
        final String end = DateConversion.getDateFormatter(endTime, DateConversion.DEFAULT_DATE_TIME);
        AbstractHibernateStatelessResultAction<List<DepartChargeReportResult>> action = new AbstractHibernateStatelessResultAction<List<DepartChargeReportResult>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                //处方金额
                hql.append("select sum(case when r.recipeType =1 then IFNULL(o.payBackPrice,0) ELSE 0 end) westMedFee,");
                hql.append("sum(case when r.recipeType =2 then IFNULL(o.payBackPrice,0) ELSE 0 end) chinesePatentMedFee,");
                hql.append("sum(case when r.recipeType =3 then IFNULL(o.payBackPrice,0) ELSE 0 end) chineseMedFee,");
                hql.append("r.depart,sum(IFNULL(o.fundAmount,0)) medicalFee,sum(IFNULL(o.cashAmount,0)) personalFee, sum( IFNULL( o.child_medical_fee, 0 ) ) childMedicalFee,sum( IFNULL( o.family_medical_fee, 0 ) ) familyMedicalFee from cdr_recipe r left join cdr_recipeorder o on r.orderCode=o.orderCode where r.clinicOrgan=:organId");
                if (depart != null) {
                    hql.append(" and r.depart =:depart");
                }
                hql.append(" and (o.payTime between :start and :end) GROUP BY r.depart ");
                Query q = ss.createSQLQuery(hql.toString());
                q.setParameter("organId", organId);
                if (depart != null) {
                    q.setParameter("depart", depart);
                }
                q.setParameter("start", start);
                q.setParameter("end", end);
                List<Object[]> result = q.list();
                List<DepartChargeReportResult> backList = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(result)) {
                    DepartChargeReportResult recipeOrderFeeVO;
                    for (Object[] objs : result) {
                        //参数组装
                        recipeOrderFeeVO = new DepartChargeReportResult();
                        //西药费
                        recipeOrderFeeVO.setWestMedFee(objs[0] == null ? new BigDecimal(0) : new BigDecimal(objs[0].toString()));
                        //中成药费
                        recipeOrderFeeVO.setChinesePatentMedFee(objs[1] == null ? new BigDecimal(0) : new BigDecimal(objs[1].toString()));
                        //中草药费
                        recipeOrderFeeVO.setChineseMedFee(objs[2] == null ? new BigDecimal(0) : new BigDecimal(objs[2].toString()));
                        //科室id
                        recipeOrderFeeVO.setDepartId(objs[3] == null ? null : Integer.valueOf(objs[3].toString()));
                        //医保金额
                        recipeOrderFeeVO.setMedicalFee(objs[4] == null ? new BigDecimal(0) : new BigDecimal(objs[4].toString()));
                        //自费金额
                        recipeOrderFeeVO.setPersonalFee(objs[5] == null ? new BigDecimal(0) : new BigDecimal(objs[5].toString()));
                        recipeOrderFeeVO.setChildMedicalFee(objs[6] == null ? new BigDecimal(0) : new BigDecimal(objs[6].toString()));
                        recipeOrderFeeVO.setFamilyMedicalFee(objs[7] == null ? new BigDecimal(0) : new BigDecimal(objs[7].toString()));
                        //科室名称
                        if (recipeOrderFeeVO.getDepartId() != null) {
                            recipeOrderFeeVO.setDepartName(DictionaryController.instance().get("eh.base.dictionary.Depart").getText(recipeOrderFeeVO.getDepartId()));
                        }
                        backList.add(recipeOrderFeeVO);
                    }
                }
                setResult(backList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据开方时间查询处方订单药品表
     *
     * @param organId
     * @param depart
     * @param createTime
     * @return
     */
    public List<DepartChargeReportResult> findRefundRecipeByOrganIdAndCreateTimeAnddepart(Integer organId, Integer depart, Date createTime, Date endTime) {
        final String start = DateConversion.getDateFormatter(createTime, DateConversion.DEFAULT_DATE_TIME);
        final String end = DateConversion.getDateFormatter(endTime, DateConversion.DEFAULT_DATE_TIME);
        AbstractHibernateStatelessResultAction<List<DepartChargeReportResult>> action = new AbstractHibernateStatelessResultAction<List<DepartChargeReportResult>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                //处方金额
                hql.append("select sum(case when r.recipeType =1 then IFNULL(o.payBackPrice,0) ELSE 0 end) westMedFee,");
                hql.append("sum(case when r.recipeType =2 then IFNULL(o.payBackPrice,0) ELSE 0 end) chinesePatentMedFee,");
                hql.append("sum(case when r.recipeType =3 then IFNULL(o.payBackPrice,0) ELSE 0 end) chineseMedFee,");
                hql.append("r.depart,sum(IFNULL(o.fundAmount,0)) medicalFee,sum(IFNULL(o.cashAmount,0)) personalFee,sum( IFNULL( o.child_medical_fee, 0 ) ) childMedicalFee,sum( IFNULL( o.family_medical_fee, 0 ) )familyMedicalFee  from cdr_recipe r left join cdr_recipeorder o on r.orderCode=o.orderCode where  r.clinicOrgan=:organId");
                hql.append(" and o.PayFlag = 3");
                if (depart != null) {
                    hql.append(" and r.depart =:depart");
                }
                hql.append(" and (o.refundTime between :start and :end) GROUP BY r.depart ");
                Query q = ss.createSQLQuery(hql.toString());
                q.setParameter("organId", organId);
                if (depart != null) {
                    q.setParameter("depart", depart);
                }
                q.setParameter("start", start);
                q.setParameter("end", end);
                List<Object[]> result = q.list();
                List<DepartChargeReportResult> backList = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(result)) {
                    DepartChargeReportResult recipeOrderFeeVO;
                    for (Object[] objs : result) {
                        //参数组装
                        recipeOrderFeeVO = new DepartChargeReportResult();
                        //西药费
                        recipeOrderFeeVO.setWestMedFee(objs[0] == null ? new BigDecimal(0) : new BigDecimal(objs[0].toString()));
                        //中成药费
                        recipeOrderFeeVO.setChinesePatentMedFee(objs[1] == null ? new BigDecimal(0) : new BigDecimal(objs[1].toString()));
                        //中草药费
                        recipeOrderFeeVO.setChineseMedFee(objs[2] == null ? new BigDecimal(0) : new BigDecimal(objs[2].toString()));
                        //科室id
                        recipeOrderFeeVO.setDepartId(objs[3] == null ? null : Integer.valueOf(objs[3].toString()));
                        //医保金额
                        recipeOrderFeeVO.setMedicalFee(objs[4] == null ? new BigDecimal(0) : new BigDecimal(objs[4].toString()));
                        //自费金额
                        recipeOrderFeeVO.setPersonalFee(objs[5] == null ? new BigDecimal(0) : new BigDecimal(objs[5].toString()));
                        recipeOrderFeeVO.setChildMedicalFee(objs[6] == null ? new BigDecimal(0) : new BigDecimal(objs[6].toString()));
                        recipeOrderFeeVO.setFamilyMedicalFee(objs[7] == null ? new BigDecimal(0) : new BigDecimal(objs[7].toString()));
                        //科室名称
                        if (recipeOrderFeeVO.getDepartId() != null) {
                            recipeOrderFeeVO.setDepartName(DictionaryController.instance().get("eh.base.dictionary.Depart").getText(recipeOrderFeeVO.getDepartId()));
                        }
                        backList.add(recipeOrderFeeVO);
                    }
                }
                setResult(backList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 处方医疗费
     *
     * @param organId
     * @param createTime
     * @param endTime
     * @return
     */
    public List<HosBusFundsReportResult> findRecipeByOrganIdAndPayTime(Integer organId, Date createTime, Date endTime) {
        final String start = DateConversion.getDateFormatter(createTime, DateConversion.DEFAULT_DATE_TIME);
        final String end = DateConversion.getDateFormatter(endTime, DateConversion.DEFAULT_DATE_TIME);
        AbstractHibernateStatelessResultAction<List<HosBusFundsReportResult>> action = new AbstractHibernateStatelessResultAction<List<HosBusFundsReportResult>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select IFNULL(sum(o.cashAmount),0),IFNULL(sum(o.fundAmount),0) from cdr_recipe r left join cdr_recipeorder o");
                hql.append(" on r.orderCode=o.orderCode where r.clinicOrgan=" + organId);
                hql.append(" and (o.payTime between '" + start + "' and  '" + end + "')");
                LOGGER.info(hql.toString());
                Query q = ss.createSQLQuery(hql.toString());
                List<Object[]> result = q.list();
                List<HosBusFundsReportResult> backList = new ArrayList<>();

                if (CollectionUtils.isNotEmpty(result)) {
                    HosBusFundsReportResult ho;
                    HosBusFundsReportResult.MedFundsDetail medFee;
                    for (Object[] objs : result) {
                        ho = new HosBusFundsReportResult();
                        //参数组装
                        medFee = new HosBusFundsReportResult.MedFundsDetail();
                        //自费
                        medFee.setPersonalAmount(new BigDecimal(objs[0].toString()));
                        //医保
                        medFee.setMedicalAmount(new BigDecimal(objs[1].toString()));
                        medFee.setTotalAmount(medFee.getPersonalAmount().add(medFee.getMedicalAmount()));
                        ho.setMedFee(medFee);
                        backList.add(ho);
                    }
                }
                setResult(backList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 处方医疗费
     *
     * @param organId
     * @param createTime
     * @param endTime
     * @return
     */
    public List<HosBusFundsReportResult> findRecipeRefundByOrganIdAndRefundTime(Integer organId, Date createTime, Date endTime) {
        final String start = DateConversion.getDateFormatter(createTime, DateConversion.DEFAULT_DATE_TIME);
        final String end = DateConversion.getDateFormatter(endTime, DateConversion.DEFAULT_DATE_TIME);
        AbstractHibernateStatelessResultAction<List<HosBusFundsReportResult>> action = new AbstractHibernateStatelessResultAction<List<HosBusFundsReportResult>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select IFNULL(sum(o.cashAmount),0),IFNULL(sum(o.fundAmount),0) from cdr_recipe r left join cdr_recipeorder o");
                hql.append(" on r.orderCode=o.orderCode where r.clinicOrgan=" + organId);
                hql.append(" and (o.refundTime between '" + start + "' and  '" + end + "')");
                hql.append(" and o.PayFlag = 3");
                Query q = ss.createSQLQuery(hql.toString());
                List<Object[]> result = q.list();
                List<HosBusFundsReportResult> backList = new ArrayList<>();

                if (CollectionUtils.isNotEmpty(result)) {
                    HosBusFundsReportResult ho;
                    HosBusFundsReportResult.MedFundsDetail medFee;
                    for (Object[] objs : result) {
                        ho = new HosBusFundsReportResult();
                        //参数组装
                        medFee = new HosBusFundsReportResult.MedFundsDetail();
                        //自费
                        medFee.setPersonalAmount(new BigDecimal(objs[0].toString()));
                        //医保
                        medFee.setMedicalAmount(new BigDecimal(objs[1].toString()));
                        medFee.setTotalAmount(medFee.getPersonalAmount().add(medFee.getMedicalAmount()));
                        ho.setMedFee(medFee);
                        backList.add(ho);
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
    public int getCountByDoctorIdAndStatus(final int doctorId, final List<Integer> recipeStatus, final String conditionOper, final boolean containDel) {
        HibernateStatelessResultAction<Long> action = new AbstractHibernateStatelessResultAction<Long>() {
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                String hql = getBaseHqlByConditions(false, recipeStatus, conditionOper, containDel);
                if (StringUtils.isNotEmpty(hql)) {
                    Query q = ss.createQuery(hql);
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
    public List<Recipe> findByDoctorIdAndStatus(final int doctorId, final List<Integer> recipeStatus, final String conditionOper, final boolean containDel, final int startIndex, final int limit, final int mark) {

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
    private String getBaseHqlByConditions(boolean getDetail, List<Integer> recipeStatus, String conditionOper, boolean containDel) {
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
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                Recipe dbRecipe;
                if (update) {
                    dbRecipe = update(recipe);
                } else {
                    dbRecipe = save(recipe);
                }

                RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
                if (CollectionUtils.isNotEmpty(recipedetails)) {
                    for (Recipedetail detail : recipedetails) {
                        //date 20200601
                        //修改
                        if (!update) {
                            detail.setRecipeId(dbRecipe.getRecipeId());
                        }
                        recipeDetailDAO.save(detail);
                    }
                }
                setResult(dbRecipe.getRecipeId());
            }
        };
        HibernateSessionTemplate.instance().executeTrans(action);
        return action.getResult();
    }

    /**
     * 根据处方ID修改处方状态，最后更新时间和自定义需要修改的字段
     * todo map更新方式作废 新方法 updateNonNullFieldByPrimaryKey
     *
     * @param status
     * @param recipeId
     * @param changeAttr 需要级联修改的其他字段
     * @return
     */
    @Deprecated
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
     * todo map更新方式作废 新方法 updateNonNullFieldByPrimaryKey
     * 更新处方自定义字段
     *
     * @param recipeId
     * @param changeAttr
     * @return
     */
    @Deprecated
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
                StringBuilder hql = new StringBuilder("from Recipe where invalidTime is null and signDate between '" + startDt + "' and '" + endDt + "' ");
                if (cancelStatus == RecipeStatusConstant.NO_PAY) {
                    //超过3天未支付，支付模式修改
                    //添加状态列表判断，从状态待处理添加签名失败，签名中
                    hql.append(" and fromflag in (1,2) and status =" + RecipeStatusConstant.CHECK_PASS + " and payFlag=0 and giveMode is not null and orderCode is not null ");
                } else if (cancelStatus == RecipeStatusConstant.NO_OPERATOR) {
                    //超过3天未操作,添加前置未操作的判断 后置待处理或者前置待审核和医保上传确认中
                    //添加状态列表判断，从状态待处理添加签名失败，签名中
                    hql.append(" and fromflag = 1 and status =" + RecipeStatusConstant.CHECK_PASS + " and chooseFlag = 0  or ( status in (8,24) and reviewType = 1 and signDate between '" + startDt + "' and '" + endDt + "' )");
                }
                Query q = ss.createQuery(hql.toString());
                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据失效时间查询失效处方列表
     *
     * @param startDt
     * @param endDt
     * @return
     */
    public List<Recipe> getInvalidRecipeListByInvalidTime(final String startDt, final String endDt) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from Recipe where (status in (8,24) and reviewType = 1 and invalidTime is not null and invalidTime between '" + startDt + "' and '" + endDt + "') " + " or (fromflag in (1,2) and status =" + RecipeStatusConstant.CHECK_PASS + " and payFlag=0 and giveMode is not null and orderCode is not null and invalidTime is not null and invalidTime between '" + startDt + "' and '" + endDt + "') " + " or (fromflag = 1 and chooseFlag = 0 and status = 2 and invalidTime is not null and invalidTime between '" + startDt + "' and '" + endDt + "') ");
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
                StringBuilder hql = new StringBuilder("select new Recipe(recipeId,signDate) from Recipe where fromflag=1 and signDate between " + "'" + startDt + "' and '" + endDt + "' ");
                if (cancelStatus == RecipeStatusConstant.PATIENT_NO_OPERATOR) {
                    //未操作
                    hql.append(" and status=" + RecipeStatusConstant.CHECK_PASS + " and remindFlag=0 and chooseFlag=0 ");
                } else if (cancelStatus == RecipeStatusConstant.PATIENT_NO_PAY) {
                    //选择了医院取药-到院支付
                    hql.append(" and status=" + RecipeStatusConstant.CHECK_PASS + " and giveMode=" + RecipeBussConstant.GIVEMODE_TO_HOS + " and remindFlag=0 and chooseFlag=1 and payFlag=0 ");
                } else if (cancelStatus == RecipeStatusConstant.PATIENT_NODRUG_REMIND) {
                    //选择了到店取药
                    hql.append(" and status=" + RecipeStatusConstant.CHECK_PASS_YS + " and giveMode=" + RecipeBussConstant.GIVEMODE_TFDS + " and remindFlag=0 and chooseFlag=1 ");
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
                StringBuilder hql = new StringBuilder("from Recipe where " + " (status=" + RecipeStatusConstant.HAVE_PAY + " or (status=" + RecipeStatusConstant.CHECK_PASS + " and signDate between '" + startDt + "' and '" + endDt + "' ))");
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

    @DAOMethod(sql = "from Recipe where mpiId=:mpiId and recipeCode=:recipeCode and clinicOrgan=:clinicOrgan and fromflag in (1,2,0)")
    public abstract Recipe getByHisRecipeCodeAndClinicOrganAndMpiid(@DAOParam("mpiId") String mpiId, @DAOParam("recipeCode") String recipeCode, @DAOParam("clinicOrgan") Integer clinicOrgan);


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

    @Deprecated
    private void validateOptionForStatistics(Integer status, Integer doctor, String patientName, Date bDate, Date eDate, Integer dateType, final int start, final int limit) {
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

    private void validateOptionForStatistics(RecipesQueryVO recipesQueryVO) {
        if (null == recipesQueryVO.getDateType()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "dateType is required");
        }
        if (null == recipesQueryVO.getBDate()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "统计开始时间不能为空");
        }
        if (null == recipesQueryVO.getEDate()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "统计结束时间不能为空");
        }

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
                StringBuilder hql = new StringBuilder("update Recipe set mpiid=:mpiid where " + "mpiid=:oldMpiid");

                Query q = ss.createQuery(hql.toString());
                q.setParameter("mpiid", newMpiId);
                q.setParameter("oldMpiid", oldMpiId);
                Integer recipeCount = q.executeUpdate();

                hql = new StringBuilder("update RecipeOrder set mpiId=:mpiid where " + "mpiId=:oldMpiid");
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
                hql.append("select r from Recipe r where r.giveMode=" + RecipeBussConstant.GIVEMODE_SEND_TO_HOME + " and r.sendDate between '" + startDt + "' and '" + endDt + "' and r.status=" + RecipeStatusConstant.IN_SEND);
                Query q = ss.createQuery(hql.toString());
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);

        return action.getResult();
    }


    /**
     *
     * 运营平台-业务查询-处方业务-处方列表查询
     * @param recipesQueryVO
     * @return
     */
    public QueryResult<Recipe> findRecipesByInfoV2(RecipesQueryVO recipesQueryVO) {
        this.validateOptionForStatistics(recipesQueryVO);
        final StringBuilder sbHql = this.generateRecipeOderHQLforStatistics(recipesQueryVO);
        final StringBuilder sbHqlCount = this.generateRecipeOderHQLforStatisticsCount(recipesQueryVO);
        logger.info("RecipeDAO findRecipesByInfo sbHql:{}", sbHql.toString());
        HibernateStatelessResultAction<QueryResult<Recipe>> action = new AbstractHibernateStatelessResultAction<QueryResult<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date bDate = recipesQueryVO.getBDate();
                Date eDate = recipesQueryVO.getEDate();

                // 查询总记录数
                SQLQuery sqlQuery = ss.createSQLQuery(sbHqlCount.toString());
                sqlQuery.setParameter("startTime", sdf.format(bDate));
                sqlQuery.setParameter("endTime", sdf.format(eDate));
                Long total = Long.valueOf(String.valueOf((sqlQuery.uniqueResult())));
                // 查询结果
                Query query = ss.createSQLQuery(sbHql.append(" order by recipeId DESC").toString())
                        .addEntity(Recipe.class);
                query.setParameter("startTime", sdf.format(bDate));
                query.setParameter("endTime", sdf.format(eDate));
                query.setFirstResult(recipesQueryVO.getStart());
                query.setMaxResults(recipesQueryVO.getLimit());
                List<Recipe> recipeList = query.list();

                setResult(new QueryResult<>(total, query.getFirstResult(), query.getMaxResults(), recipeList));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }



    public List<Recipe> findRecipeByFlag(final List<Integer> organ, List<Integer> recipeIds, List<Integer> recipeTypes, final int flag, final int start, final int limit) {
        final int notPass = 2;
        final int all = 3;
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                //0是待药师审核
                if (flag == 0) {
                    //hql.append("from Recipe where clinicOrgan in (:organ)  and checkMode<2 and status = " + RecipeStatusConstant.READY_CHECK_YS + " and  (recipeType in(:recipeTypes) or grabOrderStatus=1)");
                    hql.append("SELECT\n" +
                            "\tr.*\n" +
                            "FROM\n" +
                            "\tcdr_recipe r\n" +
                            "LEFT JOIN cdr_recipe_ext cre ON r.recipeid = cre.recipeid\n" +
                            "WHERE cre.canUrgentAuditRecipe is not null and r.clinicOrgan in (:organ) and r.checkMode<2 and r.status in (8,32) and  (recipeType in(:recipeTypes) or grabOrderStatus=1) " +
                            "ORDER BY canUrgentAuditRecipe desc, r.grabOrderStatus DESC, signdate asc");
                }
                //1是审核通过  2是审核未通过
                else if (flag == 1 || flag == notPass) {
                    hql.append("from Recipe where clinicOrgan in (:organ) and ");
                    hql.append(getSqlIn(recipeIds, 300, "recipeId") + " order by signDate desc");
                }
                //4是未签名
                else if (flag == 4) {
                    hql.append("from Recipe where clinicOrgan in (:organ) and status = " + RecipeStatusConstant.SIGN_NO_CODE_PHA + " order by signDate desc");
                }

                //3是全部---0409小版本要包含待审核或者审核后已撤销的处方
                else if (flag == all) {
                    hql.append("select r.* from cdr_recipe r where r.clinicOrgan in (:organ) and r.checkMode<2   and (r.status in (8,27,31,32) or r.checkDateYs is not null or (r.status = 9 and (select l.beforeStatus from cdr_recipe_log l where l.recipeId = r.recipeId and l.afterStatus =9 ORDER BY l.Id desc limit 1) in (8,15,7,2)))  and  (recipeType in(:recipeTypes) or grabOrderStatus=1) and r.reviewType != 0 order by signDate desc");
                } else {
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "flag is invalid");
                }
                Query q;
                if (flag == all || flag == 0) {
                    q = ss.createSQLQuery(hql.toString()).addEntity(Recipe.class);
                } else {
                    q = ss.createQuery(hql.toString());
                }
                q.setParameterList("organ", organ);
                if (flag == 0 || flag == all) {
                    q.setParameterList("recipeTypes", recipeTypes);
                }
                q.setFirstResult(start);
                q.setMaxResults(limit);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 查询药师审核的总数
     */
    public Long findRecipeCountByFlag(final List<Integer> organ, List<Integer> recipeIds, List<Integer> recipeTypes, final int flag, final int start, final int limit) {
        final int notPass = 2;
        final int all = 3;
        HibernateStatelessResultAction<Long> action = new AbstractHibernateStatelessResultAction<Long>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                //0是待药师审核
                if (flag == 0) {
                    //hql.append("from Recipe where clinicOrgan in (:organ)  and checkMode<2 and status = " + RecipeStatusConstant.READY_CHECK_YS + " and  (recipeType in(:recipeTypes) or grabOrderStatus=1)");
                    hql.append("SELECT\n" +
                            "\tcount(r.recipeid)\n" +
                            "FROM\n" +
                            "\tcdr_recipe r\n" +
                            "LEFT JOIN cdr_recipe_ext cre ON r.recipeid = cre.recipeid\n" +
                            "WHERE cre.canUrgentAuditRecipe is not null and r.clinicOrgan in (:organ) and r.checkMode<2 and r.status = 8 and  (recipeType in(:recipeTypes) or grabOrderStatus=1) " +
                            "ORDER BY canUrgentAuditRecipe desc, signdate asc");
                }
                //1是审核通过  2是审核未通过
                else if (flag == 1 || flag == notPass) {
                    hql.append("select count(*) from cdr_recipe where clinicOrgan in (:organ) and ");
                    hql.append(getSqlIn(recipeIds, 300, "recipeId") + " order by signDate desc");
                }
                //4是未签名
                else if (flag == 4) {
                    hql.append("select count(*) from cdr_recipe where clinicOrgan in (:organ) and status = " + RecipeStatusConstant.SIGN_NO_CODE_PHA + " order by signDate desc");
                }

                //3是全部---0409小版本要包含待审核或者审核后已撤销的处方
                else if (flag == all) {
                    hql.append("select count(r.recipeid) from cdr_recipe r where r.clinicOrgan in (:organ) and r.checkMode<2   and (r.status in (8,31) or r.checkDateYs is not null or (r.status = 9 and (select l.beforeStatus from cdr_recipe_log l where l.recipeId = r.recipeId and l.afterStatus =9 ORDER BY l.Id desc limit 1) in (8,15,7,2)))  and  (recipeType in(:recipeTypes) or grabOrderStatus=1) order by signDate desc");
                } else {
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "flag is invalid");
                }

                Query q;
                /*if (flag == all || flag == 0) {

                } else {
                    q = ss.createQuery(hql.toString());
                }*/
                q = ss.createSQLQuery(hql.toString());
                q.setParameterList("organ", organ);
                if (flag == 0 || flag == all) {
                    q.setParameterList("recipeTypes", recipeTypes);
                }
                BigInteger count = (BigInteger) q.uniqueResult();
                setResult(count.longValue());
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
     * @param dateType    时间类型（0：开方时间，1：审核时间，2：支付时间，3：发药时间）
     * @return QueryResult<Map>
     */
    public List<Object[]> findRecipesByInfoForExcelN(RecipesQueryVO recipesQueryVO) {
        this.validateOptionForStatistics(recipesQueryVO);
        final StringBuilder preparedHql = this.generateRecipeOderHQLforStatisticsN(recipesQueryVO);
        logger.info("findRecipesByInfoForExcelN-sql={}", preparedHql.toString());
        HibernateStatelessResultAction<List<Object[]>> action = new AbstractHibernateStatelessResultAction<List<Object[]>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder sbHql = preparedHql;
                Query query = ss.createSQLQuery(sbHql.append(" order by r.recipeId DESC").toString())
                        .addEntity(RecipeExportDTO.class)
                        .addEntity(RecipeOrderExportDTO.class)
                        .addEntity(RecipeDetailExportDTO.class);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                query.setParameter("startTime", sdf.format(recipesQueryVO.getBDate()));
                query.setParameter("endTime", sdf.format(recipesQueryVO.getEDate()));

                setResult(query.list());
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
     * @param dateType    时间类型（0：开方时间，1：审核时间，2：支付时间，3：发药时间）
     * @return QueryResult<Map>
     */
    public List<RecipeInfoExportDTO> findRecipesByInfoForExcel(RecipesQueryVO recipesQueryVO) {
        this.validateOptionForStatistics(recipesQueryVO);
        final StringBuilder sbHql = this.generateRecipeMsgHQLforStatisticsV1(recipesQueryVO);
        HibernateStatelessResultAction<List<RecipeInfoExportDTO>> action = new AbstractHibernateStatelessResultAction<List<RecipeInfoExportDTO>>() {
            @Override
            public void execute(StatelessSession ss) {
                Query query = ss.createSQLQuery(sbHql.append(" GROUP BY r.recipeId order by r.recipeId DESC").toString())
                        .addEntity(RecipeInfoExportDTO.class);
                LOGGER.info("RecipeDAO findRecipesByInfoForExcel sbHql = {} ", sbHql);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                query.setParameter("startTime", sdf.format(recipesQueryVO.getBDate()));
                query.setParameter("endTime", sdf.format(recipesQueryVO.getEDate()));
                List<RecipeInfoExportDTO> list = query.list();
                setResult(list);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 运营平台-业务查询-审方业务导出
     *
     */
    public List<RecipeAuditInfoExportDTO> findRecipeAuditInfoForExcel(RecipesQueryVO recipesQueryVO) {
        this.validateOptionForStatistics(recipesQueryVO);
        final StringBuilder sbHql = this.generateRecipeAuditHQLforStatistics(recipesQueryVO);
        HibernateStatelessResultAction<List<RecipeAuditInfoExportDTO>> action = new AbstractHibernateStatelessResultAction<List<RecipeAuditInfoExportDTO>>() {
            @Override
            public void execute(StatelessSession ss) {
                Query query = ss.createSQLQuery(sbHql.append(" GROUP BY r.recipeId order by r.recipeId DESC").toString())
                        .addEntity(RecipeAuditInfoExportDTO.class);
                LOGGER.info("RecipeDAO findRecipeAuditInfoForExcel sbHql = {} ", sbHql);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                query.setParameter("startTime", sdf.format(recipesQueryVO.getBDate()));
                query.setParameter("endTime", sdf.format(recipesQueryVO.getEDate()));
                List<RecipeAuditInfoExportDTO> list = query.list();
                setResult(list);
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
    public HashMap<String, Integer> getStatisticsByStatus(final Integer organId, final Integer status, final Integer doctor, final String mpiid, final Date bDate, final Date eDate, final Integer dateType, final Integer depart, final int start, final int limit, List<Integer> organIds, Integer giveMode, Integer fromflag, Integer recipeId) {
        this.validateOptionForStatistics(status, doctor, mpiid, bDate, eDate, dateType, start, limit);
        final StringBuilder preparedHql = this.generateHQLforStatistics(organId, status, doctor, mpiid, dateType, depart, organIds, giveMode, fromflag, recipeId);
        HibernateStatelessResultAction<HashMap<String, Integer>> action = new AbstractHibernateStatelessResultAction<HashMap<String, Integer>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = preparedHql;
                hql.append(" group by r.status ");
                Query query = ss.createQuery("select r.status, count(r.recipeId) as count " + hql.toString());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                query.setTimestamp("startTime", sdf.parse(sdf.format(bDate)));
                query.setTimestamp("endTime", sdf.parse(sdf.format(eDate)));
                List<Object[]> tfList = query.list();
                HashMap<String, Integer> mapStatistics = Maps.newHashMap();
                if (tfList.size() > 0) {
                    for (Object[] hps : tfList) {
                        if (hps[0] != null && !StringUtils.isEmpty(hps[0].toString())) {
                            String status = hps[0].toString();
                            String statusName = DictionaryController.instance().get("eh.cdr.dictionary.RecipeStatus").getText(status);
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

    private StringBuilder generateHQLforStatistics(Integer organId, Integer status, Integer doctor, String patientName, Integer dateType, Integer depart, final List<Integer> requestOrgans, Integer giveMode, Integer fromflag, Integer recipeId) {
        StringBuilder hql = new StringBuilder(" from Recipe r,RecipeOrder o  where 1=1 and r.orderCode=o.orderCode ");

        //默认查询所有
        if (CollectionUtils.isNotEmpty(requestOrgans)) {
            // 添加申请机构条件
            boolean flag = true;
            for (Integer i : requestOrgans) {
                if (i != null) {
                    if (flag) {
                        hql.append(" and r.clinicOrgan in(");
                        flag = false;
                    }
                    hql.append(i + ",");
                }
            }
            if (!flag) {
                hql = new StringBuilder(hql.substring(0, hql.length() - 1) + ") ");
            }
        }
        if (organId != null) {
            hql.append(" and r.clinicOrgan =" + organId);
        }
        switch (dateType) {
            case 0:
                //开方时间
                hql.append(" and r.createDate >= :startTime" + " and r.createDate <= :endTime ");
                break;
            case 1:
                //审核时间
                hql.append(" and r.checkDate >= :startTime" + " and r.checkDate <= :endTime ");
                break;
            default:
                break;
        }
        if (status != null) {
            hql.append(" and r.status =").append(status);
        }
        if (doctor != null) {
            hql.append(" and r.doctor=").append(doctor);
        }
        //根据患者姓名  精确查询
        if (patientName != null && !StringUtils.isEmpty(patientName.trim())) {
            hql.append(" and r.patientName='").append(patientName).append("'");
        }
        if (depart != null) {
            hql.append(" and r.depart=").append(depart);
        }
        if (giveMode != null) {
            hql.append(" and r.giveMode=").append(giveMode);
        }
        if (fromflag != null) {
            hql.append(" and r.fromflag=").append(fromflag);
        }
        if (recipeId != null) {
            hql.append(" and r.recipeId=").append(recipeId);
        }
        return hql;
    }

    /**
     * 运营平台-业务查询-处方列表查询sql
     * @param recipesQueryVO
     * @return
     */
    private StringBuilder generateRecipeOderHQLforStatistics(RecipesQueryVO recipesQueryVO) {
        StringBuilder hql = new StringBuilder("select r.*  from cdr_recipe r ");
        hql.append(" LEFT JOIN cdr_recipeorder o on r.orderCode = o.orderCode ");
        hql.append(" LEFT JOIN cdr_recipe_ext re ON r.RecipeID = re.recipeId ");
        hql.append(" where (r.recipeSourceType in (1,2) or r.recipeSourceType is null) ");
        hql.append(" and (r.delete_flag = 0 or r.delete_flag  is null)");
        hql.append(" and r.process_state != 0 ");
        return generateRecipeOderWhereHQLforStatistics(hql,recipesQueryVO);
    }

    /**
     * 运营平台-业务查询-处方列表(总数)查询sql
     * @param recipesQueryVO
     * @return
     */
    private StringBuilder generateRecipeOderHQLforStatisticsCount(RecipesQueryVO recipesQueryVO) {
        StringBuilder hql = new StringBuilder("select count(1)  from cdr_recipe r ");
        hql.append(" LEFT JOIN cdr_recipeorder o on r.orderCode = o.orderCode ");
        hql.append(" LEFT JOIN cdr_recipe_ext re ON r.RecipeID = re.recipeId ");
        hql.append(" where (r.recipeSourceType in (1,2) or r.recipeSourceType is null) ");
        hql.append(" and (r.delete_flag = 0 or r.delete_flag  is null)");
        hql.append(" and r.process_state != 0 ");
        return generateRecipeOderWhereHQLforStatistics(hql,recipesQueryVO);
    }

    /**
     * 运营平台-业务查询-处方列表查询条件组装
     * @param recipesQueryVO
     * @return
     */
    private StringBuilder generateRecipeOderWhereHQLforStatistics(StringBuilder hql,RecipesQueryVO recipesQueryVO) {
        //默认查询所有
        if (CollectionUtils.isNotEmpty(recipesQueryVO.getOrganIds())) {
            // 添加申请机构条件
            boolean flag = true;
            for (Integer i : recipesQueryVO.getOrganIds()) {
                if (i != null) {
                    if (flag) {
                        hql.append(" and r.clinicOrgan in(");
                        flag = false;
                    }
                    hql.append(i + ",");
                }
            }
            if (!flag) {
                hql = new StringBuilder(hql.substring(0, hql.length() - 1) + ") ");
            }
        }
        if (null != recipesQueryVO.getOrganId()) {
            hql.append(" and r.clinicOrgan =" + recipesQueryVO.getOrganId());
        }
        switch (recipesQueryVO.getDateType()) {
            case 0:
                //开方时间
                hql.append(" and r.createDate BETWEEN :startTime" + " and :endTime ");
                break;
            case 1:
                //审核时间
                hql.append(" and r.CheckDateYs BETWEEN :startTime" + " and :endTime ");
                break;
            case 2:
                //支付时间
                hql.append(" and o.payTime BETWEEN :startTime" + " and :endTime ");
                break;
            case 3:
                //发药时间
                hql.append(" and o.dispensingTime BETWEEN :startTime" + " and :endTime ");
                break;
            case 4:
                //配送时间
                hql.append(" and r.startSendDate BETWEEN :startTime" + " and :endTime ");
                break;
            default:
                break;
        }
        if (null != recipesQueryVO.getStatus()) {
            //由于已取消状态较多，使用其中一个值查询所有已取消的状态
            if (new Integer(11).equals(recipesQueryVO.getStatus())) {
                hql.append(" and r.status in (11,12,13,14,17,19,20,25)");
            } else {
                hql.append(" and r.status =").append(recipesQueryVO.getStatus());
            }
        }
        if (null != recipesQueryVO.getProcessState()) {
            hql.append(" and r.process_state =").append(recipesQueryVO.getProcessState());
        }
        if (null != recipesQueryVO.getDoctor()) {
            hql.append(" and r.doctor=").append(recipesQueryVO.getDoctor());
        }
        if (null != recipesQueryVO.getDepart()) {
            hql.append(" and r.depart=").append(recipesQueryVO.getDepart());
        }
        if (null != recipesQueryVO.getGiveMode()) {
            hql.append(" and r.giveMode=").append(recipesQueryVO.getGiveMode());
        }
        if (null != recipesQueryVO.getSendType()) {
            hql.append(" and o.send_type=").append(recipesQueryVO.getSendType());
        }

        hql.append(" and r.fromflag in (1,2) ");

        if (null != recipesQueryVO.getRecipeId()) {
            hql.append(" and r.recipeId=").append(recipesQueryVO.getRecipeId());
        }

        if (null != recipesQueryVO.getMpiid()) {
            hql.append(" and r.mpiid='").append(recipesQueryVO.getMpiid() + "'");
        }
        if (null != recipesQueryVO.getEnterpriseId()) {
            hql.append(" and r.enterpriseId=").append(recipesQueryVO.getEnterpriseId());
        }

        if (null != recipesQueryVO.getCheckStatus()) {
            switch (recipesQueryVO.getCheckStatus()) {
                case 0:
                    hql.append(" and r.checkFlag=0 ");
                    break;
                case 1:
                    hql.append(" and r.checkFlag=1 ");
                    break;
                case 2:
                    hql.append(" and r.checkFlag=2 ");
                    break;
                case 3:
                    hql.append(" and r.checkFlag in(0,1,2) ");
                    break;
            }
        }

        Integer payFlag=recipesQueryVO.getPayFlag();
        if (null != payFlag) {
            if (payFlag == 0) {
                hql.append(" and r.payFlag=").append(payFlag);
            } else {
                hql.append(" and o.payFlag=").append(payFlag);
            }
        }
        if (null != recipesQueryVO.getOrderType()) {
            if (recipesQueryVO.getOrderType() == 0) {
                hql.append(" and o.orderType in (0,5)  ");
            } else {
                hql.append(" and o.orderType in (1,2,3,4) ");
            }
        }
        if (null != recipesQueryVO.getRefundNodeStatus()) {
            hql.append(" and re.refundNodeStatus=").append(recipesQueryVO.getRefundNodeStatus());
        }
        if (null != recipesQueryVO.getRecipeType()) {
            hql.append(" and r.recipeType=").append(recipesQueryVO.getRecipeType());
        }
        if (null != recipesQueryVO.getBussSource()) {
            switch (recipesQueryVO.getBussSource()) {
                case 1:
                    hql.append(" and r.bussSource=1 ");
                    break;
                case 2:
                    hql.append(" and r.bussSource=2 ");
                    break;
                case 3:
                    hql.append(" and r.bussSource=3 ");
                    break;
                case 5:
                    hql.append(" and r.bussSource=5 ");
                    break;
            }
        }
        if (null != recipesQueryVO.getRecipeBusinessType()) {
            hql.append(" and re.recipe_business_type= ").append(recipesQueryVO.getRecipeBusinessType());
        }
        if (null != recipesQueryVO.getFastRecipeFlag()) {
            if(Integer.valueOf(0).equals(recipesQueryVO.getFastRecipeFlag())){
                hql.append(" and (r.fast_recipe_flag = 0 or r.fast_recipe_flag is null)");
            }else {
                hql.append(" and r.fast_recipe_flag = ").append(recipesQueryVO.getFastRecipeFlag());
            }
        }

        //autoCheckFlag：0不需要审核 1自动审方 2药师审方
        Integer autoCheckFlag=recipesQueryVO.getAutoCheckFlag();
        if (null != autoCheckFlag) {
            switch (autoCheckFlag) {
                case 0:
                    hql.append(" and r.reviewType=0");
                    break;
                case 1:
                    hql.append(" and r.reviewType>0 and re.auto_check=1 and r.CheckDateYs is not null");
                    break;
                case 2:
                    hql.append(" and r.reviewType>0 and (re.auto_check=0 or re.auto_check is null) and r.CheckDateYs is not null");
                    break;
            }
        }
        if(StringUtils.isNotEmpty(recipesQueryVO.getRecipeCode())){
            hql.append(" and r.recipeCode = '").append(recipesQueryVO.getRecipeCode()).append("'");
        }
        LOGGER.info("generateRecipeOderWhereHQLforStatistics hql:{}", hql);
        return hql;
    }



    /**
     * 运营平台-处方查询-处方列表导出sql查询
     * @param recipesQueryVO
     * @return
     */
    private StringBuilder generateRecipeMsgHQLforStatisticsV1(RecipesQueryVO recipesQueryVO) {
        StringBuilder hql = new StringBuilder("select r.recipeId,r.patientName,r.Mpiid mpiId,r.organName,r.depart,r.doctor,r.organDiseaseName," +
                "o.recipeFee,r.totalMoney,r.checker,r.checkDateYs,r.fromflag,r.status,r.process_state  processState,r.sub_state  subState,o.payTime, r.doctorName, r.giveUser, o.dispensingTime, " +
                "sum(cr.useTotalDose) sumDose ,o.send_type sendType ,o.outTradeNo ,o.cashAmount,o.fundAmount,o.orderType,r.recipeType,r.bussSource," +
                "r.recipeCode,re.recipe_business_type as recipeBusinessType, " +
                "o.drugStoreName,o.enterpriseId,r.fast_recipe_flag as fastRecipeFlag, " +
                "r.offline_recipe_name as offlineRecipeName,r.copyNum " +
                " from cdr_recipe r " +
                " LEFT JOIN cdr_recipeorder o on r.orderCode=o.orderCode " +
                " left join cdr_recipedetail cr on cr.recipeId = r.recipeId  and cr.status =1 " +
                " left join cdr_recipe_ext re on re.recipeId = r.recipeId " +
                " where (r.recipeSourceType in (1,2) or r.recipeSourceType is null) and (r.delete_flag = 0 or r.delete_flag  is null) ");

        return generateRecipeOderWhereHQLforStatistics(hql,recipesQueryVO);
    }

    /**
     * 运营平台-处方查询-审方信息列表导出sql查询
     * @param recipesQueryVO
     * @return
     */
    private StringBuilder generateRecipeAuditHQLforStatistics(RecipesQueryVO recipesQueryVO) {
        StringBuilder hql = new StringBuilder("select r.recipeId,r.recipeCode,r.patientName, " +
                "r.mpiid,r.doctor,r.doctorName,r.signDate,r.clinicOrgan,r.organName, " +
                "r.checkerText,r.CheckDateYs,r.checkFlag,r.reviewType,re.auto_check as autoCheck " +
                " from cdr_recipe r " +
                " LEFT JOIN cdr_recipeorder o on r.orderCode=o.orderCode " +
                " left join cdr_recipe_ext re on re.recipeId = r.recipeId " +
                " where (r.recipeSourceType in (1,2) or r.recipeSourceType is null) and (r.delete_flag = 0 or r.delete_flag  is null) ");

        return generateRecipeOderWhereHQLforStatistics(hql,recipesQueryVO);
    }

    /**
     * 运营平台-处方查询-配送订单列表导出sql查询
     * @param recipesQueryVO
     * @return
     */
    private StringBuilder generateRecipeOderHQLforStatisticsN(RecipesQueryVO recipesQueryVO) {
        StringBuilder hql = new StringBuilder("select ");
        hql.append("o.orderId,o.address1,o.address2,o.address3,o.address4,address5Text,o.streetAddress,o.receiver,o.send_type,o.RecMobile,o.CreateTime,o.ExpressFee,o.OrderCode,o.Status,o.ActualPrice,o.TotalFee,o.EnterpriseId,o.ExpectSendDate,o.ExpectSendTime,o.PayFlag,o.PayTime,o.TradeNo,o.RecipeIdList,o.dispensingTime,o.drugStoreName, ");
        hql.append("r.recipeId,r.mpiid,r.patientID,r.doctor,r.organName,r.organDiseaseName,r.doctorName,r.patientName,r.status,r.depart,r.fromflag,r.giveMode,r.recipeType,r.giveUser,r.bussSource,r.recipeCode,re.recipe_business_type as recipeBusinessType,r.clinicOrgan , ");
        hql.append("d.recipeDetailId,d.drugName,d.drugSpec,d.drugUnit,d.salePrice,d.actualSalePrice,d.saleDrugCode,d.producer,d.licenseNumber,d.useDose,d.useDoseUnit,d.usePathways,d.usingRate,d.useTotalDose,d.drugId ,d.organDrugCode,d.his_return_sale_price ");
        hql.append(" ,re.decoctionId,re.decoctionText ");

        hql.append(" from cdr_recipe r LEFT JOIN cdr_recipeorder o on r.orderCode=o.orderCode ");
        hql.append("LEFT JOIN cdr_recipedetail d ON r.RecipeID = d.RecipeID")
                .append(" LEFT JOIN cdr_recipe_ext re on re.recipeId = r.recipeId ");
        hql.append(" where d.Status= 1 and (r.recipeSourceType in (1,2) or r.recipeSourceType is null) and (r.delete_flag = 0 or r.delete_flag  is null)  ");

        return generateRecipeOderWhereHQLforStatistics(hql,recipesQueryVO);
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
                    RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
                    for (Recipe recipe : recipeList) {
                        Map<String, Object> map = Maps.newHashMap();
                        BeanUtils.map(recipe, map);
                        //map.putAll(JSONObject.parseObject(JSON.toJSONString(recipe)));
                        map.put("detailCount", recipeDetailDAO.getCountByRecipeId(recipe.getRecipeId()));
                        //map.put("patient",patientService.get(recipe.getMpiid()));

                        if(recipe.getOrderCode()!=null){
                            RecipeOrder order=recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                            if(order!=null){
                                map.put("payTime",order.getPayTime());
                            }
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
                String hql = "SELECT r.organDiseaseId FROM Recipe r WHERE " + "r.doctor = :doctorId AND r.clinicOrgan = :organId AND r.organDiseaseId is not null AND r.createDate between '" + startDt + "' and '" + endDt + "'";
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
            if (StringUtils.isNotEmpty(s)) {
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
     * 通过挂号序号和机构ID获取his处方号合集
     *
     * @param registerId 挂号序号
     * @param organId    机构ID
     * @return 处方号合集
     */
    public List<Recipe> findByRecipeCodeAndRegisterIdAndOrganId(final String registerId, final Integer organId) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "SELECT r FROM Recipe r ,RecipeExtend re WHERE " + "r.recipeId=re.recipeId and  r.recipeCode is not null  and re.registerID = :registerId AND r.clinicOrgan = :organId  ";
                Query query = ss.createQuery(hql);
                query.setParameter("registerId", registerId);
                query.setParameter("organId", organId);
                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        List<Recipe> list = action.getResult();
        return list;
    }

    public List<String> findCommonSymptomIdByDoctorAndOrganId(final int doctorId, final int organId) {

        final String endDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(0), DateConversion.DEFAULT_DATE_TIME);
        final String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(90), DateConversion.DEFAULT_DATE_TIME);
        //查询医生三个月内开的数据
        HibernateStatelessResultAction<List<String>> action = new AbstractHibernateStatelessResultAction<List<String>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "SELECT re.symptomId FROM Recipe r ,RecipeExtend re WHERE " + "r.recipeId=re.recipeId and r.doctor = :doctorId AND r.clinicOrgan = :organId AND re.symptomId is not null AND r.createDate between '" + startDt + "' and '" + endDt + "'";
                Query query = ss.createQuery(hql);
                query.setParameter("doctorId", doctorId);
                query.setParameter("organId", organId);
                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        //单独处理多证候的处方单
        List<String> list = action.getResult();
        if (list == null || list.size() == 0) {
            return null;
        }
        Map<String, Integer> diseaseMap = Maps.newHashMap();
        //循环计算每一个诊断的次数
        for (String s : list) {
            if (StringUtils.isNotEmpty(s)) {
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
     * 查询医生对应机构 常用证候 最多显示10条
     * liumin
     *
     * @param doctor
     * @param organId
     * @param start
     * @param limit
     * @return
     */
    public List<Symptom> findCommonSymptomByDoctorAndOrganId(final int doctor, final int organId, final int start, final int limit) {
        final List<String> organSymptomIdsTemp = findCommonSymptomIdByDoctorAndOrganId(doctor, organId);
        logger.info("findCommonSymptomByDoctorAndOrganId-organSymptomIdsTemp={}", JSON.toJSONString(organSymptomIdsTemp));
        if (organSymptomIdsTemp == null || organSymptomIdsTemp.size() == 0) {
            return Lists.newArrayList();
        }
        HibernateStatelessResultAction<List<Symptom>> action = new AbstractHibernateStatelessResultAction<List<Symptom>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                Query q = ss.createQuery("select a from Symptom a where a.organId = :organId and a.symptomCode in (:organSymptomIdsTemp) ");
                q.setParameter("organId", organId);
                q.setParameterList("organSymptomIdsTemp", organSymptomIdsTemp);
                q.setFirstResult(start);
                q.setMaxResults(limit);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        List<Symptom> list = action.getResult();
        if (null == list || list.size() == 0) {
            return Lists.newArrayList();
        }
        //排序
        List<Symptom> symptoms = Lists.newArrayList();
        for (int i = 0; i < organSymptomIdsTemp.size(); i++) {
            for (int x = 0; x < list.size(); x++) {
                if (list.get(x).getSymptomCode().equals(organSymptomIdsTemp.get(i))) {
                    symptoms.add(list.get(x));
                }
            }
        }
        return symptoms;
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
                String hql = "SELECT r.mpiid FROM Recipe r WHERE " + "r.doctor = :doctorId AND r.patientStatus = 1 group by r.mpiid order by r.createDate desc";
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

    @DAOMethod(sql = "update Recipe set status=:status,sendDate=:sendDate,sender=:sender,remindFlag=1 where recipeId in (:recipeIds)")
    public abstract void updateSendInfoByRecipeIds(@DAOParam("recipeIds") List<Integer> recipeIds, @DAOParam("sendDate") String sendDate,
                                                   @DAOParam("sender") String sender, @DAOParam("status") Integer status);

    /**
     * 获取HOS历史处方
     *
     * @param doctorId
     * @param mpiId
     * @param clinicOrgan
     * @param start
     * @param limit
     * @return
     */
    public List<Recipe> findHosRecipe(final Integer doctorId, final String mpiId, final Integer clinicOrgan, final Integer start, final Integer limit) {
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
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select clinicOrgan, count(*) as count from Recipe a  where a.fromflag=1 and a.createDate between :startDate and :endDate  group by clinicOrgan");
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
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select count(*) as count, HOUR(a.createDate) as hour from Recipe a  where a.fromflag=1 and a.createDate between :startDate and :endDate  group by HOUR(a.createDate)");
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
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select clinicOrgan, count(*) as count from Recipe a where a.fromflag=1 group by clinicOrgan");
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
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select doctor, count(*) as count from Recipe a where a.doctor > 0 and a.fromflag=1  group by doctor");
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

    public List<String> findAllMpiIdsFromHis() {
        HibernateStatelessResultAction<List<String>> action = new AbstractHibernateStatelessResultAction<List<String>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select mpiid from Recipe where fromflag=0 group by mpiid");
                Query query = ss.createQuery(hql.toString());

                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 查询所有待审核处方单
     */
    @DAOMethod(sql = "from Recipe where status = 8 and fromflag = 1", limit = 0)
    public abstract List<Recipe> findAllReadyAuditRecipe();

    @DAOMethod(sql = "select recipeId from Recipe where clinicOrgan in:organIds and status =8 and fromflag = 1 and checkMode = 1 ")
    public abstract List<Integer> findReadyAuditRecipeIdsByOrganIds(@DAOParam("organIds") List<Integer> organIds);

    @DAOMethod(sql = "from Recipe where recipeSourceType = :recipeSourceType and orderCode = :orderCode", limit = 0)
    public abstract List<Recipe> findRecipeByOrderCodeAndSourceType(@DAOParam("orderCode") String orderCode,
                                                                    @DAOParam("recipeSourceType") Integer recipeSourceType);

    @DAOMethod(sql = "from Recipe where orderCode = :orderCode", limit = 0)
    public abstract List<Recipe> findRecipeByOrderCode(@DAOParam("orderCode") String orderCode);


    public List<Recipe> findRecipeListForStatus(final int status, final String startDt, final String endDt) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from Recipe where signDate between '" + startDt + "' and '" + endDt + "' ");
                hql.append(" and fromflag = 1 and status=:status");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("status", status);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod
    public abstract Recipe getByRecipeCode(String recipeCode);


    public QueryResult<Recipe> findRecipeListByMpiID(final String mpiId, final Integer organId, final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<Recipe>> action = new AbstractHibernateStatelessResultAction<QueryResult<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "from Recipe where requestMpiId=:mpiid and clinicOrgan =:clinicOrgan and status in (2,6,8,9,12,14,15) order by createDate desc";
                Query query = ss.createQuery(hql);
                query.setParameter("mpiid", mpiId);
                query.setParameter("clinicOrgan", organId);
                query.setFirstResult(start);
                query.setMaxResults(limit);

                Query countQuery = ss.createQuery("select count(*) " + hql);
                countQuery.setParameter("mpiid", mpiId);
                countQuery.setParameter("clinicOrgan", organId);
                Long total = (Long) countQuery.uniqueResult();
                List<Recipe> lists = query.list();
                setResult(new QueryResult<Recipe>(total, query.getFirstResult(), query.getMaxResults(), lists));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * maoze
     * 杨柳郡专用
     *
     * @param mpiId
     * @param organId
     * @param start
     * @param limit
     * @return
     */
    public QueryResult<Recipe> findRecipeListByMpiIDForYang(final String mpiId, final Integer organId, final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<Recipe>> action = new AbstractHibernateStatelessResultAction<QueryResult<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "from Recipe where requestMpiId=:mpiid  and status in (2,3,4,5,6,7,8,9,12,13,14,15,40,41,42) order by createDate desc";
                Query query = ss.createQuery(hql);
                query.setParameter("mpiid", mpiId);
                query.setFirstResult(start);
                query.setMaxResults(limit);

                Query countQuery = ss.createQuery("select count(*) " + hql);
                countQuery.setParameter("mpiid", mpiId);
                Long total = (Long) countQuery.uniqueResult();
                List<Recipe> lists = query.list();
                setResult(new QueryResult<Recipe>(total, query.getFirstResult(), query.getMaxResults(), lists));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 自助机查询处方信息 使用新状态
     *
     * @param mpiId
     * @param start
     * @param limit
     * @return
     */
    public QueryResult<Recipe> findRecipeToZiZhuJi(final String mpiId, final List<Integer> processStateList, final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<Recipe>> action = new AbstractHibernateStatelessResultAction<QueryResult<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "from Recipe where mpiid=:mpiid  and processState in (:processStateList) order by createDate desc";
                Query query = ss.createQuery(hql);
                query.setParameter("mpiid", mpiId);
                query.setParameterList("processStateList", processStateList);
                query.setFirstResult(start);
                query.setMaxResults(limit);

                Query countQuery = ss.createQuery("select count(*) " + hql);
                countQuery.setParameter("mpiid", mpiId);
                countQuery.setParameterList("processStateList", processStateList);
                Long total = (Long) countQuery.uniqueResult();
                List<Recipe> lists = query.list();
                setResult(new QueryResult<Recipe>(total, query.getFirstResult(), query.getMaxResults(), lists));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public List<Recipe> findRecipeListForDate(final List<Integer> organList, final String startDt, final String endDt) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from Recipe where lastModify between '" + startDt + "' and '" + endDt + "' ");
                hql.append(" and fromflag = 1 and clinicOrgan in:organList and status > 0 and status < 16");
                Query q = ss.createQuery(hql.toString());
                q.setParameterList("organList", organList);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod(sql = "select signFile from Recipe where patientID =:patientId and signFile is not null and fromflag = 1")
    public abstract List<String> findSignFileIdByPatientId(@DAOParam("patientId") String patientId);

    /**
     * 根据需要变更的状态获取处方集合
     *
     * @param orderCodes
     * @return
     */
    public List<Recipe> getRecipeListByOrderCodes(final List<String> orderCodes) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from Recipe where  status in (7,8) and giveMode = 3 ");
                if (CollectionUtils.isNotEmpty(orderCodes)) {
                    hql.append(" and orderCode in (");
                    for (String orderCode : orderCodes) {
                        hql.append("'").append(orderCode).append("',");
                    }
                    hql.deleteCharAt(hql.lastIndexOf(","));
                    hql.append(")");
                }
                Query q = ss.createQuery(hql.toString());
                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 监管平台反查接口
     *
     * @param organId
     * @param startDate
     * @param endDate
     * @return
     */
    public List<Recipe> findSyncRecipeListByOrganId(final Integer organId, final String startDate, final String endDate, final Boolean checkFlag) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from Recipe where fromflag=1 ");
                //是否查的是已审核数据
                if (checkFlag) {
                    hql.append("and checkDateYs between '" + startDate + "' and '" + endDate + "' and clinicOrgan =:organId and checker is not null");
                } else {
                    hql.append("and SignDate between '" + startDate + "' and '" + endDate + "' and clinicOrgan =:organId and status not in (0,10,11,16)");
                }
                Query query = ss.createQuery(hql.toString());
                query.setParameter("organId", organId);
                setResult(query.list());
            }
        };

        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    public List<PatientRecipeBean> findTabStatusRecipesForPatient(final List<String> mpiIdList, final int start, final int limit, final List<Integer> recipeStatusList, final List<Integer> orderStatusList, final List<Integer> specialStatusList, final String tabStatus) {
        HibernateStatelessResultAction<List<PatientRecipeBean>> action = new AbstractHibernateStatelessResultAction<List<PatientRecipeBean>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select s.type,s.recordCode,s.recordId,s.mpiId,s.diseaseName,s.status,s.fee," + "s.recordDate,s.couponId,s.medicalPayFlag,s.recipeType,s.organId,s.recipeMode,s.giveMode, s.recipeSource,s.payFlag ,s.recipeId from (");
                hql.append("SELECT 1 as type,null as couponId, t.MedicalPayFlag as medicalPayFlag, t.RecipeID as recordCode,t.RecipeID as recordId," + "t.MPIID as mpiId,t.OrganDiseaseName as diseaseName,(case when (t.reviewType = 1 and t.checkStatus = 1 and t.status = 15) then 8 else t.Status end) as Status,t.TotalMoney as fee," + "t.SignDate as recordDate,t.RecipeType as recipeType,t.ClinicOrgan as organId,t.recipeMode as recipeMode,t.giveMode as giveMode, t.recipeSource as recipeSource ,t.payFlag as payFlag,t.recipeId FROM cdr_recipe t " + "left join cdr_recipeorder k on t.OrderCode=k.OrderCode ");
                hql.append("WHERE t.MPIID IN (:mpiIdList) and (k.Effective is null or k.Effective = 0) and t.recipeSourceType = 1 ");
                //添加前置的逻辑：前置时，一次审核不通过，处方判定为待审核，需要在待处理列表中，显示状态为待处理
                if ("ongoing".equals(tabStatus)) {
                    //进行中：加入前置一次审核不通过的作为待审核的处方
                    //hql.append(" and (t.Status IN (:recipeStatusList) or (t.reviewType = 1 and t.checkStatus = 1 and t.status = 15))");
                    //date 20191017
                    //去掉互联网待审核的，sql根据平台和互联网逻辑分开,前半部分是平台的，后半部分是互联网的（互联网“处方部分”状态只展示为待处理的）
                    hql.append(" and ((t.recipeMode != 'zjjgpt' && t.Status IN (:recipeStatusList) or (t.reviewType = 1 and t.checkStatus = 1 and t.status = 15)) or (t.recipeMode = 'zjjgpt' and t.Status in (2, 22)))");
                } else {
                    //已处理：排除一次审核不通过的
                    hql.append(" and t.Status IN (:recipeStatusList) and t.checkStatus != 1 ");
                }


                hql.append("UNION ALL ");
                if (CollectionUtils.isNotEmpty(specialStatusList)) {
                    hql.append("SELECT 2 as type,o.CouponId as couponId, 0 as medicalPayFlag, " + "o.OrderCode as recordCode,o.OrderId as recordId,o.MpiId as mpiId,'' as diseaseName," + "o.Status,o.ActualPrice as fee,o.CreateTime as recordDate,0 as recipeType, o.OrganId, 'ngarihealth' as recipeMode,w.GiveMode AS giveMode, w.recipeSource as recipeSource ,w.payFlag as payFlag,w.recipeId FROM cdr_recipeorder o JOIN cdr_recipe w ON o.OrderCode = w.OrderCode " + "AND o.MpiId IN (:mpiIdList) and o.Effective = 1 and o.Status IN (:orderStatusList) and w.Status NOT IN (:specialStatusList) and w.recipeSourceType = 1 ");
                    hql.append("UNION ALL ");
                    hql.append("SELECT 1 as type,null as couponId, t.MedicalPayFlag as medicalPayFlag, t.RecipeID as recordCode,t.RecipeID as recordId," + "t.MPIID as mpiId,t.OrganDiseaseName as diseaseName,t.Status,(case when k.Effective is null then t.TotalMoney else k.ActualPrice end) as fee," + "t.SignDate as recordDate,t.RecipeType as recipeType,t.ClinicOrgan as organId,t.recipeMode as recipeMode,t.giveMode as giveMode, t.recipeSource as recipeSource ,t.payFlag as payFlag,t.recipeId FROM cdr_recipe t " + "left join cdr_recipeorder k on t.OrderCode=k.OrderCode " + "WHERE t.MpiId IN (:mpiIdList) and t.Status IN (:specialStatusList) and t.recipeSourceType = 1 ");
                } else {
                    hql.append("SELECT 2 as type,o.CouponId as couponId, 0 as medicalPayFlag, " + "o.OrderCode as recordCode,o.OrderId as recordId,o.MpiId as mpiId,'' as diseaseName," + "o.Status,o.ActualPrice as fee,o.CreateTime as recordDate,0 as recipeType, o.OrganId, 'ngarihealth' as recipeMode,w.GiveMode AS giveMode, w.recipeSource as recipeSource ,w.payFlag as payFlag,w.recipeId FROM cdr_recipeorder o JOIN cdr_recipe w ON o.OrderCode = w.OrderCode " + "AND o.MpiId IN (:mpiIdList) and o.Effective = 1 and o.Status IN (:orderStatusList) and w.recipeSourceType = 1 ");

                }
                //添加下载处方的状态o
                hql.append(") s ORDER BY s.recordDate desc");

                Query q = ss.createSQLQuery(hql.toString());
                q.setParameterList("mpiIdList", mpiIdList);
                q.setParameterList("orderStatusList", orderStatusList);
                q.setParameterList("recipeStatusList", recipeStatusList);
                if (CollectionUtils.isNotEmpty(specialStatusList)) {
                    q.setParameterList("specialStatusList", specialStatusList);
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
                        if (null != objs[4]) {
                            patientRecipeBean.setOrganDiseaseName(objs[4].toString());
                        }
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
                        patientRecipeBean.setRecipeMode(objs[12].toString());
                        if (null != objs[13]) {
                            patientRecipeBean.setGiveMode(Integer.parseInt(objs[13].toString()));
                        }
                        if (null != objs[14]) {
                            patientRecipeBean.setRecipeSource(Integer.parseInt(objs[14].toString()));
                        }
                        if (null != objs[15]) {
                            patientRecipeBean.setPayFlag(Integer.parseInt(objs[15].toString()));
                        }
                        if (null != objs[16]) {
                            patientRecipeBean.setRecipeId(Integer.parseInt(objs[16].toString()));
                        }
                        backList.add(patientRecipeBean);
                    }
                }

                setResult(backList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public List<PatientRecipeBean> findTabStatusRecipesForPatientNew(final List<String> mpiIdList, final int start, final int limit, final List<Integer> recipeStatusList, final List<Integer> orderStatusList, final String tabStatus, final List<Integer> recipeIdWithoutHisAndPayList) {
        HibernateStatelessResultAction<List<PatientRecipeBean>> action = new AbstractHibernateStatelessResultAction<List<PatientRecipeBean>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();

                if ("onready".equals(tabStatus)) {
                    hql.append("select 1 as type,t.RecipeID as recordCode,t.RecipeID as recordId,t.MPIID,t.OrganDiseaseName as diseaseName,(case when (t.reviewType = 1 and t.checkStatus = 1 and t.status = 15) then 8 else t.Status end) as status,t.TotalMoney as fee," + "t.SignDate as recordDate,'' as couponId,t.MedicalPayFlag,t.RecipeType,t.ClinicOrgan as organId,t.recipeMode,t.giveMode,t.recipeSource,t.payFlag,t.recipeId from cdr_recipe t ");
                    hql.append("WHERE t.MPIID IN (:mpiIdList) and t.recipeSourceType = 1 and ((t.recipeMode != 'zjjgpt' && t.Status IN (:recipeStatusList) or (t.reviewType = 1 and t.checkStatus = 1 and t.status = 15)) or (t.recipeMode = 'zjjgpt' and t.Status in (2, 22))) and t.OrderCode is null ORDER BY t.SignDate desc");
                } else if ("ongoing".equals(tabStatus)) {
                    hql.append("select 2 as type,o.OrderCode as recordCode,o.OrderId as recordId,o.MpiId,'' as diseaseNam,o.Status,o.ActualPrice as fee," + "o.CreateTime as recordDate,o.CouponId,0 as medicalPayFlag,w.recipeType,o.OrganId,w.recipeMode,w.GiveMode,w.recipeSource,w.payFlag ,w.recipeId from ");
                    hql.append("cdr_recipeorder o JOIN cdr_recipe w ON o.OrderCode = w.OrderCode " + "AND o.MpiId IN (:mpiIdList) and o.Effective = 1 and o.Status IN (:orderStatusList) and w.recipeSourceType = 1 ");
                    hql.append("ORDER BY o.CreateTime desc");
                } else {
                    hql.append("select s.type,s.recordCode,s.recordId,s.mpiId,s.diseaseName,s.status,s.fee," + "s.recordDate,s.couponId,s.medicalPayFlag,s.recipeType,s.organId,s.recipeMode,s.giveMode, s.recipeSource,s.payFlag ,s.recipeId from (");
                    hql.append("SELECT 1 as type,null as couponId, t.MedicalPayFlag as medicalPayFlag, t.RecipeID as recordCode,t.RecipeID as recordId," + "t.MPIID as mpiId,t.OrganDiseaseName as diseaseName, t.Status as Status,t.TotalMoney as fee," + "t.SignDate as recordDate,t.RecipeType as recipeType,t.ClinicOrgan as organId,t.recipeMode as recipeMode,t.giveMode as giveMode, t.recipeSource as recipeSource ,t.payFlag as payFlag,t.recipeId FROM cdr_recipe t " + "left join cdr_recipeorder k on t.OrderCode=k.OrderCode ");
                    hql.append("WHERE t.MPIID IN (:mpiIdList) and (k.Effective is null or k.Effective = 0) and t.recipeSourceType = 1 and t.Status IN (:recipeStatusList) and t.checkStatus != 1 ");
                    hql.append("UNION ALL ");
                    hql.append("SELECT 2 as type,o.CouponId as couponId, 0 as medicalPayFlag, " + "o.OrderCode as recordCode,o.OrderId as recordId,o.MpiId as mpiId,'' as diseaseName," + "o.Status,o.ActualPrice as fee,o.CreateTime as recordDate,0 as recipeType, o.OrganId, 'ngarihealth' as recipeMode,w.GiveMode AS giveMode, w.recipeSource as recipeSource ,w.payFlag as payFlag,w.recipeId FROM cdr_recipeorder o JOIN cdr_recipe w ON o.OrderCode = w.OrderCode " + "AND o.MpiId IN (:mpiIdList) and o.Effective = 1 and o.Status IN (:orderStatusList) and w.recipeSourceType = 1 ");
                    hql.append(") s ");
                    if (CollectionUtils.isNotEmpty(recipeIdWithoutHisAndPayList)) {
                        hql.append("where s.recipeId not in(:recipeIdWithoutHisAndPayList)");
                    }
                    hql.append("ORDER BY s.recordDate desc");
                }

                Query q = ss.createSQLQuery(hql.toString());
                q.setParameterList("mpiIdList", mpiIdList);
                if ("onready".equals(tabStatus)) {
                    q.setParameterList("recipeStatusList", recipeStatusList);
                } else if ("ongoing".equals(tabStatus)) {
                    q.setParameterList("orderStatusList", orderStatusList);
                } else {
                    q.setParameterList("orderStatusList", orderStatusList);
                    q.setParameterList("recipeStatusList", recipeStatusList);
                    if (CollectionUtils.isNotEmpty(recipeIdWithoutHisAndPayList)) {
                        q.setParameterList("recipeIdWithoutHisAndPayList", recipeIdWithoutHisAndPayList);
                    }
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
                        if (null != objs[4]) {
                            patientRecipeBean.setOrganDiseaseName(objs[4].toString());
                        }
                        patientRecipeBean.setStatusCode(Integer.parseInt(objs[5].toString()));
                        patientRecipeBean.setTotalMoney(new BigDecimal(objs[6].toString()));
                        patientRecipeBean.setSignDate((Date) objs[7]);
                        if (null != objs[8] && StringUtils.isNotEmpty(objs[8].toString())) {
                            patientRecipeBean.setCouponId(Integer.parseInt(objs[8].toString()));
                        }
                        if (null != objs[9]) {
                            patientRecipeBean.setMedicalPayFlag(Integer.parseInt(objs[9].toString()));
                        }
                        patientRecipeBean.setRecipeType(Integer.parseInt(objs[10].toString()));
                        patientRecipeBean.setOrganId(Integer.parseInt(objs[11].toString()));
                        patientRecipeBean.setRecipeMode(objs[12].toString());
                        if (null != objs[13]) {
                            patientRecipeBean.setGiveMode(Integer.parseInt(objs[13].toString()));
                        }
                        if (null != objs[14]) {
                            patientRecipeBean.setRecipeSource(Integer.parseInt(objs[14].toString()));
                        }
                        if (null != objs[15]) {
                            patientRecipeBean.setPayFlag(Integer.parseInt(objs[15].toString()));
                        }
                        if (null != objs[16]) {
                            patientRecipeBean.setRecipeId(Integer.parseInt(objs[16].toString()));
                        }
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
     * 药房工作量
     *
     * @param organId
     * @return
     */
    public List<WorkLoadTopDTO> findRecipeByOrderCodegroupByDis(Integer organId, String orderStatus, Integer start, Integer limit, String startDate, String endDate, String doctorName, String recipeType) {
        HibernateStatelessResultAction<List<WorkLoadTopDTO>> action = new AbstractHibernateStatelessResultAction<List<WorkLoadTopDTO>>() {
            @Override
            public void execute(StatelessSession statelessSession) throws Exception {
                String sql = "SELECT\n" + "\to.dispensingApothecaryName AS dispensingApothecaryName,\n" + "\tcount(recipeId) AS recipeCount,\n" + "\tsum(totalMoney) totalMoney\n" + "FROM\n" + "\tcdr_recipe r\n" + "LEFT JOIN cdr_recipeorder o ON (r.ordercode = o.ordercode)\n" + "WHERE\n" + "\tr.ordercode IS NOT NULL\n" + "AND o.OrganId = :organId\n" + (StringUtils.isNotEmpty(doctorName) ? "AND o.dispensingApothecaryName like :dispensingApothecaryName\n" : "") + "AND o.status in " +
                        " (" + orderStatus + ")\n" + "AND o.dispensingStatusAlterTime BETWEEN '" + startDate + "'\n" + "AND '" + endDate + "'\n" + (StringUtils.isNotEmpty(recipeType) ? "AND r.recipeType in (:recipeType)\n" : "") + "GROUP BY\n" + "\to.dispensingApothecaryName";
                Query q = statelessSession.createSQLQuery(sql);
                q.setParameter("organId", organId);
                if (StringUtils.isNotEmpty(doctorName)) {
                    q.setParameter("dispensingApothecaryName", "%" + doctorName + "%");
                }
                if (StringUtils.isNotEmpty(recipeType)) {
                    q.setParameter("recipeType", recipeType);
                }
                if (start != null && limit != null) {
                    q.setFirstResult(start);
                    q.setMaxResults(limit);
                }
                List<Object[]> result = q.list();
                List<WorkLoadTopDTO> vo = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(result)) {
                    for (Object[] objects : result) {
                        WorkLoadTopDTO workLoadTopDTO = new WorkLoadTopDTO();
                        workLoadTopDTO.setDispensingApothecaryName(objects[0] == null ? "" : objects[0].toString());
                        workLoadTopDTO.setRecipeCount(Integer.valueOf(objects[1].toString()));
                        workLoadTopDTO.setTotalMoney(new BigDecimal(String.valueOf(objects[2])).setScale(2, BigDecimal.ROUND_HALF_UP));
                        vo.add(workLoadTopDTO);
                    }
                }
                setResult(vo);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 药房工作量 带退费的
     *
     * @param organId
     * @return
     */
    public List<WorkLoadTopDTO> findRecipeByOrderCodegroupByDisWithRefund(Integer organId, String orderStatus, Integer start, Integer limit, String startDate, String endDate, String doctorName, String recipeType) {
        HibernateStatelessResultAction<List<WorkLoadTopDTO>> action = new AbstractHibernateStatelessResultAction<List<WorkLoadTopDTO>>() {
            @Override
            public void execute(StatelessSession statelessSession) throws Exception {
                String sql = "SELECT\n" + "\to.dispensingApothecaryName AS dispensingApothecaryName,\n" + "\tcount(recipeId) AS recipeCount,\n" + "\tsum(0) totalMoney\n" + "FROM\n" + "\tcdr_recipe r\n" + "LEFT JOIN cdr_recipeorder o ON (r.ordercode = o.ordercode)\n" + "WHERE\n" + "\tr.ordercode IS NOT NULL\n" + "AND o.OrganId = :organId\n" + (StringUtils.isNotEmpty(doctorName) ? "AND o.dispensingApothecaryName like :dispensingApothecaryName\n" : "") + "AND o.status in (" + orderStatus + ")\n" + "AND o.dispensingTime is not null\n" + "AND o.dispensingStatusAlterTime BETWEEN '" + startDate + "'\n" + "AND '" + endDate + "'\n" + (StringUtils.isNotEmpty(recipeType) ? "AND r.recipeType in (:recipeType)\n" : "") + "GROUP BY\n" + "\to.dispensingApothecaryName";
                Query q = statelessSession.createSQLQuery(sql);
                q.setParameter("organId", organId);
                if (StringUtils.isNotEmpty(doctorName)) {
                    q.setParameter("dispensingApothecaryName", "%" + doctorName + "%");
                }
                if (StringUtils.isNotEmpty(recipeType)) {
                    q.setParameter("recipeType", recipeType);
                }
                if (start != null && limit != null) {
                    q.setFirstResult(start);
                    q.setMaxResults(limit);
                }
                List<Object[]> result = q.list();
                List<WorkLoadTopDTO> vo = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(result)) {
                    for (Object[] objects : result) {
                        WorkLoadTopDTO workLoadTopDTO = new WorkLoadTopDTO();
                        workLoadTopDTO.setDispensingApothecaryName(objects[0] == null ? "" : objects[0].toString());
                        workLoadTopDTO.setRecipeCount(Integer.valueOf(objects[1].toString()));
                        workLoadTopDTO.setTotalMoney(new BigDecimal(String.valueOf(objects[2])).setScale(2, BigDecimal.ROUND_HALF_UP));
                        vo.add(workLoadTopDTO);
                    }
                }
                setResult(vo);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 发药月报
     *
     * @param organId
     * @return
     */
    public List<PharmacyMonthlyReportDTO> findRecipeDetialCountgroupByDepart(Integer organId, String depart, String recipeType, String startDate, String endDate, Boolean isAll, Integer start, Integer limit) {
        HibernateStatelessResultAction<List<PharmacyMonthlyReportDTO>> action = new AbstractHibernateStatelessResultAction<List<PharmacyMonthlyReportDTO>>() {
            @Override
            public void execute(StatelessSession statelessSession) throws Exception {
                String sql = "select cr.depart, sum(cr.totalMoney) as totalMoney, count(cr.RECIPEID) as count,sum(cr.totalMoney)/count(cr.RECIPEID) AS avgMoney from cdr_recipe cr LEFT JOIN cdr_recipeorder co ON (cr.ordercode = co.ordercode)  where co.dispensingTime BETWEEN '" + startDate + "'\n" + "\t\tAND '" + endDate + "' and cr.ClinicOrgan =:organId and cr.totalMoney is not null AND co.STATUS IN (4,5,13)" + (StringUtils.isNotEmpty(recipeType) ? " AND cr.recipeType in (:recipeType)\n" : "");
                if (StringUtils.isNotEmpty(depart)) {
                    sql += " and depart='" + depart + "'";
                }
                if (!isAll) {
                    sql = sql + " group by depart";
                }
                Query q = statelessSession.createSQLQuery(sql);
                q.setParameter("organId", organId);
                if (StringUtils.isNotEmpty(recipeType)) {
                    q.setParameter("recipeType", recipeType);
                }
                if (start != null && limit != null) {
                    q.setFirstResult(start);
                    q.setMaxResults(limit);
                }
                List<Object[]> result = q.list();
                List<PharmacyMonthlyReportDTO> vo = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(result)) {
                    for (Object[] objects : result) {
                        if (Integer.valueOf(String.valueOf(objects[2])) > 0) {
                            PharmacyMonthlyReportDTO value = new PharmacyMonthlyReportDTO();
                            value.setDepart(Integer.valueOf(String.valueOf(objects[0])));
                            value.setTotalMoney(new BigDecimal(String.valueOf(objects[1])).setScale(2, BigDecimal.ROUND_HALF_UP));
                            value.setRecipeCount(Integer.valueOf(String.valueOf(objects[2])));
                            value.setAvgMoney(new BigDecimal(String.valueOf(objects[3])).setScale(2, BigDecimal.ROUND_HALF_UP));
                            vo.add(value);
                        }
                    }
                }
                setResult(vo);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    /**
     * 发药排行
     *
     * @param organId
     * @return
     */
    public List<PharmacyTopDTO> findDrugCountOrderByCountOrMoneyCountGroupByDrugId(Integer organId, Integer drugType, String orderStatus, String startDate, String endDate, Integer order, Integer start, Integer limit) {
        HibernateStatelessResultAction<List<PharmacyTopDTO>> action = new AbstractHibernateStatelessResultAction<List<PharmacyTopDTO>>() {
            @Override
            public void execute(StatelessSession statelessSession) throws Exception {
                String sql = "SELECT\n" + "\trd.OrganDrugCode,\n" + "\trd.drugName,\n" + "\trd.drugSpec,\n" + "\trd.drugUnit,\n" + "\tcast(\n" + "\t\tsum(rd.useTotalDose) AS SIGNED\n" + "\t) AS count,\n" + "\trd.saleprice,\n" + "\tSUM(rd.drugCost) AS countMoney,\n" + "\tCASE bd.drugtype\n" + "WHEN 1 THEN\n" + "\t'西药'\n" + "WHEN 2 THEN\n" + "\t'中成药'\n" + "ELSE\n" + "\t'中草药'\n" + "END AS drugtype\n" + "FROM\n" + "\tcdr_recipedetail rd\n" + " LEFT JOIN base_druglist bd ON (rd.drugId = bd.drugid)\n" + "WHERE\n" + "\trd.recipeid IN (\n" + "\t\tSELECT\n" + "\t\t\trecipeId\n" + "\t\tFROM\n" + "\t\t\tcdr_recipe cr\n" + "\t\tLEFT JOIN cdr_recipeorder co ON (cr.ordercode = co.ordercode)\n" + "\t\tWHERE\n" + "\t\t\tco.dispensingTime BETWEEN '" + startDate + "  '\n" + "\t\tAND '" + endDate + "'\n" + "\t\tAND ClinicOrgan = :organId\n" + "\t\tAND rd.drugCost IS NOT NULL\n" + "\t\tAND co.`Status` IN (" + orderStatus + ")\n" + (drugType == 0 ? " " : "AND bd.drugtype IN (:drugType)\n") + "\t)  AND rd.STATUS=1 GROUP BY\n" + "\tOrganDrugCode\n";
                if (order == 1) {
                    sql += "order by SUM(rd.saleprice) desc";
                }
                if (order == 2) {
                    sql += "order by SUM(rd.saleprice) asc";
                }
                if (order == 3) {
                    sql += "order by sum(rd.useTotalDose) desc";
                }
                if (order == 4) {
                    sql += "order by sum(rd.useTotalDose) asc";
                }
                LOGGER.info("findDrugCountOrderByCountOrMoneyCountGroupByDrugId sql : " + sql);
                Query q = statelessSession.createSQLQuery(sql);
                q.setParameter("organId", organId);
                if (drugType != 0) {
                    q.setParameter("drugType", drugType);
                }
                if (start != null && limit != null) {
                    q.setFirstResult(start);
                    q.setMaxResults(limit);
                }
                List<Object[]> result = q.list();
                List<PharmacyTopDTO> vo = new ArrayList<>();

                if (CollectionUtils.isNotEmpty(result)) {
                    for (Object[] objects : result) {
                        PharmacyTopDTO value = new PharmacyTopDTO();
                        value.setDrugId(String.valueOf(objects[0]));
                        value.setDrugName(String.valueOf(objects[1]));
                        value.setDrugSpec(String.valueOf(objects[2]));
                        value.setDrugUnit(String.valueOf(objects[3]));
                        value.setCount(String.valueOf(objects[4]));
                        value.setDrugCost(new BigDecimal(String.valueOf(objects[5])).setScale(2, BigDecimal.ROUND_HALF_UP));
                        value.setCountMoney(new BigDecimal(String.valueOf(objects[6])).setScale(2, BigDecimal.ROUND_HALF_UP));
                        value.setDrugtype(String.valueOf(objects[7]));
                        vo.add(value);
                    }
                }
                setResult(vo);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 发药查询整体
     *
     * @param organId
     * @return
     */
    public List<RecipeDrugDetialReportDTO> findRecipeDrugDetialReport(Integer organId, String startDate, String endDate, String drugName, String cardNo, String patientName, String billNumber, String recipeId, String orderStatus, Integer depart, String doctorName, String dispensingApothecaryName, Integer recipeType, Integer start, Integer limit) {
        HibernateStatelessResultAction<List<RecipeDrugDetialReportDTO>> action = new AbstractHibernateStatelessResultAction<List<RecipeDrugDetialReportDTO>>() {
            @Override
            public void execute(StatelessSession statelessSession) throws Exception {
                String sql = "SELECT\n" + "\tcrb.bill_number,\n" + "\tcr.recipeId,\n" + "\tcr.depart,\n" + "\tcr.patientName,\n" + "\tDATE_FORMAT(co.dispensingTime, '%Y-%m-%d %k:%i:%s') as sendDate,\n" + "\tCASE co.STATUS WHEN 13 THEN '已发药' WHEN 14 THEN '已拒发' WHEN 15 THEN '已退药' WHEN 4 THEN '配送中' WHEN 5 THEN '已完成' ELSE '' END AS STATUS,\n" + "\tco.dispensingApothecaryName as sendApothecaryName,\n" + "\tco.dispensingApothecaryName as dispensingApothecaryName,\n" + "\t'' AS dispensingWindow,\n" + "\tcr.doctorName,\n" + "\tcr.totalMoney,\n" + "\tCASE cr.RecipeType WHEN 1 THEN '西药' WHEN 2 THEN '中成药' WHEN 3 THEN '中药' ELSE '膏方' END AS RecipeType,\n" + "\tDATE_FORMAT(cr.CreateDate, '%Y-%m-%d %k:%i:%s') as CreateDate,\n" + "\tDATE_FORMAT(co.PayTime, '%Y-%m-%d %k:%i:%s') as PayTime\n" + "FROM\n" + "\tcdr_recipe cr\n" + "LEFT JOIN cdr_recipedetail rd ON (cr.RecipeID = rd.RecipeID)\n" + "LEFT JOIN cdr_recipeorder co ON cr.ordercode = co.ordercode\n" + "LEFT JOIN cdr_recipeorder_bill crb ON crb.recipe_order_code = co.OrderCode\n" + "LEFT JOIN cdr_recipe_ext cre ON cre.recipeId = cr.RecipeID\n" + "LEFT JOIN base_druglist bd ON (rd.drugId = bd.drugid)\n" + "WHERE\n" + "\tcr.ClinicOrgan = :organId\n" + "AND (\n" + "\tco. STATUS IN (" + orderStatus + ")\n" + ")\n" + "AND co.dispensingStatusAlterTime BETWEEN '" + startDate + "'\n" + "AND '" + endDate + "'" + (StringUtils.isNotEmpty(cardNo) ? " AND cre.cardNo = :cardNo" : "") + (StringUtils.isNotEmpty(patientName) ? " AND cr.patientName like :patientName" : "") + (StringUtils.isNotEmpty(billNumber) ? " AND crb.bill_number = :billNumber" : "") + (StringUtils.isNotEmpty(recipeId) ? " AND cr.recipeId = :recipeId" : "") + (recipeType != null ? " AND cr.recipeType = :recipeType" : "") + (StringUtils.isNotEmpty(dispensingApothecaryName) ? " AND co.dispensingApothecaryName like :dispensingApothecaryName" : "") + (StringUtils.isNotEmpty(doctorName) ? " AND cr.doctorName like :doctorName" : "") + (StringUtils.isNotEmpty(drugName) ? " AND rd.DrugName like :drugName" : "") + (depart != null ? " AND cr.depart = :depart" : "");
                sql += " group by cr.recipeId";
                Query q = statelessSession.createSQLQuery(sql);
                q.setParameter("organId", organId);
                if (StringUtils.isNotEmpty(cardNo)) {
                    q.setParameter("cardNo", cardNo);
                }
                if (StringUtils.isNotEmpty(patientName)) {
                    q.setParameter("patientName", "%" + patientName + "%");
                }
                if (StringUtils.isNotEmpty(billNumber)) {
                    q.setParameter("billNumber", billNumber);
                }
                if (StringUtils.isNotEmpty(recipeId)) {
                    q.setParameter("recipeId", recipeId);
                }
                if (recipeType != null) {
                    q.setParameter("recipeType", recipeType);
                }
                if (StringUtils.isNotEmpty(dispensingApothecaryName)) {
                    q.setParameter("dispensingApothecaryName", "%" + dispensingApothecaryName + "%");
                }
                if (StringUtils.isNotEmpty(doctorName)) {
                    q.setParameter("doctorName", "%" + doctorName + "%");
                }
                if (depart != null) {
                    q.setParameter("depart", depart);
                }
                if (StringUtils.isNotEmpty(drugName)) {
                    q.setParameter("drugName", "%" + drugName + "%");
                }
                if (start != null && limit != null) {
                    q.setFirstResult(start);
                    q.setMaxResults(limit);
                }
                List<Object[]> result = q.list();
                List<RecipeDrugDetialReportDTO> vo = new ArrayList<>();

                if (CollectionUtils.isNotEmpty(result)) {
                    for (Object[] objects : result) {
                        RecipeDrugDetialReportDTO value = new RecipeDrugDetialReportDTO();
                        value.setBillNumber(String.valueOf(objects[0] == null ? "" : objects[0]));
                        value.setRecipeId(Integer.valueOf(String.valueOf(objects[1])));
                        value.setDepart(Integer.valueOf(String.valueOf(objects[2])));
                        value.setPatientName(String.valueOf(objects[3]));
                        value.setSendDate(objects[4] == null ? "" : String.valueOf(objects[4]));
                        value.setStatus(String.valueOf(objects[5]));
                        value.setSendApothecaryName(objects[6] == null ? "" : String.valueOf(objects[6]));
                        value.setDispensingApothecaryName(objects[7] == null ? "" : String.valueOf(objects[7]));
                        value.setDispensingWindow(objects[8] == null ? "" : String.valueOf(objects[8]));
                        value.setDoctorName(String.valueOf(objects[9]));
                        value.setTotalMoney(Double.valueOf(String.valueOf(objects[10])));
                        value.setRecipeType(String.valueOf(objects[11]));
                        value.setCreateDate(String.valueOf(objects[12]));
                        value.setPayTime(String.valueOf(objects[13]));
                        vo.add(value);
                    }
                }
                setResult(vo);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 发药查询 单个
     *
     * @param recipeId
     * @return
     */
    public List<Map<String, Object>> findRecipeDrugDetialByRecipeId(Integer recipeId) {
        HibernateStatelessResultAction<List<Map<String, Object>>> action = new AbstractHibernateStatelessResultAction<List<Map<String, Object>>>() {
            @Override
            public void execute(StatelessSession statelessSession) throws Exception {
                String sql = "SELECT\n" + "\tcrb.bill_number,\n" + "\tcr.patientName,\n" + "\tcre.medicalTypeText\n" + "AS patientType,\n" + " co.dispensingTime,\n" + " cr.recipeId,\n" + " cr.doctorName,\n" + " co.dispensingApothecaryName AS sendApothecaryName,\n" + " co.dispensingApothecaryName AS dispensingApothecaryName,\n" + " '' AS memo,\n" + " cre.cardNo,\n" + " crt.drugSpec,\n" + " crt.useDose,\n" + " crt.usingRate,\n" + " crt.usePathwaysText,\n" + " crt.drugCost,\n" + " crt.drugName,\n" + " crt.useTotalDose,\n" + " crt.drugunit,\n" + " crt.producer,\n" + " cr.OrganDiseaseName,\n" + " cr.MPIID,\n" + " crt.DosageUnit\n" + "FROM\n" + "\tcdr_recipe cr\n" + "LEFT JOIN cdr_recipe_ext cre ON (cr.RecipeID = cre.recipeId)\n" + "LEFT JOIN cdr_recipeorder co ON cr.ordercode = co.ordercode\n" + "LEFT JOIN cdr_recipedetail crt ON crt.RecipeID = cre.recipeId\n" + "LEFT JOIN cdr_recipeorder_bill crb ON crb.recipe_order_code = co.OrderCode\n" + "WHERE\n" + "\tcr.RecipeID = :recipeId AND crt.status = 1";
                Query q = statelessSession.createSQLQuery(sql);
                LOGGER.info("findRecipeDrugDetialByRecipeId sql : " + sql);
                q.setParameter("recipeId", recipeId);
                List<Object[]> result = q.list();
                List<Map<String, Object>> vo = new ArrayList<>();

                if (CollectionUtils.isNotEmpty(result)) {
                    for (Object[] objects : result) {
                        Map<String, Object> value = new HashMap<>();
                        value.put("billNumber", objects[0] == null ? "" : objects[0]);
                        value.put("patientName", objects[1]);
                        value.put("patientType", objects[2]);
                        value.put("createDate", objects[3]);
                        value.put("recipeId", objects[4]);
                        value.put("doctorName", objects[5]);
                        value.put("sendApothecaryName", objects[6] == null ? "" : objects[6]);
                        value.put("dispensingApothecaryName", objects[7] == null ? "" : objects[7]);
                        value.put("memo", objects[8] == null ? "" : objects[8]);
                        value.put("cardNo", objects[9] == null ? "" : objects[9]);
                        value.put("drugSpec", objects[10] == null ? "" : objects[10]);
                        value.put("useDose", objects[11] == null ? "" : objects[11]);
                        value.put("usingRate", objects[12] == null ? "" : objects[12]);
                        value.put("usePathwaysText", objects[13] == null ? "" : objects[13]);
                        value.put("drugCost", objects[14] == null ? "" : objects[14]);
                        value.put("drugName", objects[15] == null ? "" : objects[15]);
                        value.put("sendNumber", objects[16] == null ? "" : objects[16]);
                        value.put("dosageUnit", objects[17] == null ? "" : objects[17]);
                        value.put("producer", objects[18] == null ? "" : objects[18]);
                        value.put("organDiseaseName", objects[19] == null ? "" : objects[19]);
                        value.put("MPIID", objects[20] == null ? "" : objects[20]);
                        value.put("unit", objects[21] == null ? "" : objects[21]);
                        vo.add(value);
                    }
                }
                setResult(vo);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    /**
     * 获取his写入失败且未支付的订单号
     *
     * @param mpiIdList
     * @return
     */
    @DAOMethod(sql = "select recipeId from Recipe where orderCode is null and status = 11 and mpiid in(:allMpiIds)")
    public abstract List<Integer> findRecipeIdWithoutHisAndPay(@DAOParam("allMpiIds") List<String> mpiIdList);


    /**
     * 获取挂号序号和处方id对应关系
     *
     * @param mpiIdList
     * @param start
     * @param limit
     * @param recipeStatusList
     * @param mergeRecipeWay
     * @return
     */
    public Map<String, List<Integer>> findRecipeIdAndRegisterIdRelation(final List<String> mpiIdList, final int start, final int limit, final List<Integer> recipeStatusList, List<Integer> orderStatusList, String tabStatus, String mergeRecipeWay) {
        HibernateStatelessResultAction<Map<String, List<Integer>>> action = new AbstractHibernateStatelessResultAction<Map<String, List<Integer>>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                //通过机构配置里配置的id来分组获取合并处方
                hql.append("select ");
                hql.append(mergeRecipeWay);
                hql.append(",group_concat(d.RecipeID ORDER BY d.RecipeID desc) as ids from cdr_recipe d,cdr_recipe_ext e ");
                hql.append("where d.RecipeID = e.recipeId and d.MPIID in(:mpiIdList) and d.`Status` in (:recipeStatusList) and d.recipeSourceType = 1 ");
                if ("onready".equals(tabStatus)) {
                    hql.append("and d.OrderCode is null ");
                } else if ("ongoing".equals(tabStatus)) {
                    hql.append("and d.OrderCode is not null ");
                }
                //在获取的处方id集合里用最大id排序--根据慢病、挂号序号、机构id分组
                hql.append("GROUP BY d.ClinicOrgan,");
                //进行中的处方需要细化到已经合并了的处方单
                if ("ongoing".equals(tabStatus)) {
                    hql.append("d.OrderCode,");
                }
                hql.append(mergeRecipeWay);
                hql.append(" ORDER BY SUBSTRING_INDEX(group_concat(d.RecipeID ORDER BY d.RecipeID desc),',',1) desc");

                Query q = ss.createSQLQuery(hql.toString());
                q.setParameterList("mpiIdList", mpiIdList);
                q.setParameterList("recipeStatusList", recipeStatusList);

                q.setMaxResults(limit);
                q.setFirstResult(start);
                List<Object[]> result = q.list();
                Map<String, List<Integer>> registerIdAndRecipeIds = new HashMap<>(limit);
                if (CollectionUtils.isNotEmpty(result)) {
                    //相同挂号序号的情况下
                    int i = 0;
                    String registerId;
                    for (Object[] objs : result) {
                        //挂号序号为空的情况 用-1表示无挂号序号的情况
                        //因为Map.put会覆盖相同key的数据这里如果是ongoing根据OrderCode分组后会有多个相同的挂号序号也需要类似下面的处理
                        if (objs[mergeRecipeWay.split(",").length - 1] == null) {
                            registerId = "-1" + "," + i;
                        } else {
                            registerId = objs[mergeRecipeWay.split(",").length - 1].toString() + "," + i;
                        }
                        //根据机构配置的id的长度获取,例e.registerId,e.chronicDiseaseName 则取第三个参数是处方id列表
                        String recipeIdStr = LocalStringUtil.toString(objs[mergeRecipeWay.split(",").length]);
                        List<Integer> recipeIdList = Lists.newArrayList();
                        if (StringUtils.isNotEmpty(recipeIdStr)) {
                            CollectionUtils.addAll(recipeIdList, (Integer[]) ConvertUtils.convert(recipeIdStr.split(","), Integer.class));

                        }
                        registerIdAndRecipeIds.put(registerId, recipeIdList);
                        ++i;
                    }
                }

                setResult(registerIdAndRecipeIds);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 获取挂号序号和处方id对应关系----已结束中tab页
     *
     * @param mpiIdList
     * @param start
     * @param limit
     * @param recipeStatusList
     * @param mergeRecipeWay
     * @return
     */
    public Map<String, List<Integer>> findRecipeIdAndRegisterIdRelationByIsover(final List<String> mpiIdList, final int start, final int limit, final List<Integer> recipeStatusList, List<Integer> orderStatusList, String mergeRecipeWay) {
        HibernateStatelessResultAction<Map<String, List<Integer>>> action = new AbstractHibernateStatelessResultAction<Map<String, List<Integer>>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                //已结束的tab页应该拆成有订单和无订单的合并
                hql.append("select groupField,ids from (");
                //有订单
                hql.append("select ");
                hql.append(mergeRecipeWay);
                //取最后一个的别名
                hql.append(" as groupField");
                hql.append(",group_concat(d.RecipeID ORDER BY d.RecipeID desc) as ids from cdr_recipe d,cdr_recipe_ext e ");
                hql.append("where d.RecipeID = e.recipeId and d.MPIID in(:mpiIdList) and d.`Status` in (:recipeStatusList) and d.recipeSourceType = 1 and d.OrderCode is not null ");
                hql.append("GROUP BY d.ClinicOrgan,d.OrderCode,");
                hql.append(mergeRecipeWay);
                hql.append(" UNION ALL ");
                //无订单
                hql.append("select ");
                hql.append(mergeRecipeWay);
                hql.append(" as groupField");
                hql.append(",d.RecipeID as ids from cdr_recipe d,cdr_recipe_ext e ");
                hql.append("where d.RecipeID = e.recipeId and d.MPIID in(:mpiIdList) and d.`Status` in (:recipeWithoutHisAndPayStatusList) and d.recipeSourceType = 1 and d.OrderCode is null ");
                hql.append(") s ORDER BY SUBSTRING_INDEX(ids,',',1) desc");

                Query q = ss.createSQLQuery(hql.toString());
                q.setParameterList("mpiIdList", mpiIdList);
                q.setParameterList("recipeStatusList", recipeStatusList);
                q.setParameterList("recipeWithoutHisAndPayStatusList", recipeStatusList.stream().filter(a -> !a.equals(11)).collect(Collectors.toList()));

                q.setMaxResults(limit);
                q.setFirstResult(start);
                List<Object[]> result = q.list();
                Map<String, List<Integer>> registerIdAndRecipeIds = new HashMap<>(limit);
                if (CollectionUtils.isNotEmpty(result)) {
                    int i = 0;
                    String registerId;
                    for (Object[] objs : result) {
                        //挂号序号为空的情况 用-1表示无挂号序号的情况
                        //因为Map.put会覆盖相同key的数据这里如果是ongoing根据OrderCode分组后会有多个相同的挂号序号也需要类似下面的处理
                        if (objs[0] == null) {
                            registerId = "-1" + "," + i;
                        } else {
                            registerId = objs[0].toString() + "," + i;
                        }
                        //根据机构配置的id的长度获取,例e.registerId,e.chronicDiseaseName 则取第三个参数是处方id列表
                        String recipeIdStr = LocalStringUtil.toString(objs[1]);
                        List<Integer> recipeIdList = Lists.newArrayList();
                        if (StringUtils.isNotEmpty(recipeIdStr)) {
                            CollectionUtils.addAll(recipeIdList, (Integer[]) ConvertUtils.convert(recipeIdStr.split(","), Integer.class));

                        }
                        registerIdAndRecipeIds.put(registerId, recipeIdList);
                        ++i;
                    }
                }

                setResult(registerIdAndRecipeIds);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod(sql = "select distinct doctor from Recipe where doctor is not null", limit = 0)
    public abstract List<Integer> findDoctorIdByHistoryRecipe();

    /**
     * 查找指定医生和患者间开的处方单列表
     *
     * @param doctorId
     * @param mpiId
     * @param start
     * @param limit iii
     * @return
     */
    public List<Recipe> findRecipeListByDoctorAndPatientAndStatusList(final Integer doctorId, final String mpiId, final Integer start, final Integer limit, final List<Integer> statusList,String startDate,String endDate) {
        Long beginTime = new Date().getTime();
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "from Recipe where mpiid=:mpiid  ";
                if (org.apache.commons.lang3.StringUtils.isNotEmpty(startDate)) {
                    hql += " and  createDate>= :startTime";
                }
                if (org.apache.commons.lang3.StringUtils.isNotEmpty(endDate)) {
                    hql += " and createDate <= :endTime";
                }
                if (doctorId != null) {
                    hql += " and doctor=:doctor";
                }
                hql += " and status IN (:statusList) and recipeSourceType in (1,2) and checkStatus!=1 order by createDate desc ";
                Query query = ss.createQuery(hql);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                if (!com.alibaba.druid.util.StringUtils.isEmpty(startDate)) {
                    query.setTimestamp("startTime", sdf.parse(startDate));
                }
                if (!com.alibaba.druid.util.StringUtils.isEmpty(endDate)) {
                    query.setTimestamp("endTime", sdf.parse(endDate));
                }
                if (doctorId != null) {
                    query.setParameter("doctor", doctorId);
                }
                query.setParameter("mpiid", mpiId);
                query.setParameterList("statusList", statusList);
                if (null != start && null != limit) {
                    query.setFirstResult(start);
                    query.setMaxResults(limit);
                }
                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);

        List<Recipe> recipes = action.getResult();
        Long totalConsumedTime = new Date().getTime() - beginTime;
        LOGGER.info("findRecipeListByDoctorAndPatientAndStatusList cost:{}", totalConsumedTime);
        return recipes;
    }

    /**
     * 查找指定患者+机构+指定状态的处方
     *
     * @param doctorId
     * @param mpiId
     * @param start
     * @param limit
     * @return
     */
    @LogRecord
    public List<Recipe> findRecipeListByDoctorAndPatientAndStatusListAndOrganId(final Integer doctorId, final String mpiId, final Integer start, final Integer limit, final List<Integer> statusList,String startDate,String endDate,Integer organId) {
        Long beginTime = new Date().getTime();
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "from Recipe where mpiid=:mpiid  ";
                if (org.apache.commons.lang3.StringUtils.isNotEmpty(startDate)) {
                    hql += " and  createDate>= :startTime";
                }
                if (org.apache.commons.lang3.StringUtils.isNotEmpty(endDate)) {
                    hql += " and createDate <= :endTime";
                }
                if (doctorId != null) {
                    hql += " and doctor=:doctor";
                }
                if (organId != null) {
                    hql += " and clinicOrgan=:clinicOrgan";
                }
                hql += " and processState IN (:statusList) and recipeSourceType in (1,2)  and checkStatus!=1  order by createDate desc ";
                logger.info("hql:{}",hql);
                Query query = ss.createQuery(hql);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                if (!com.alibaba.druid.util.StringUtils.isEmpty(startDate)) {
                    query.setTimestamp("startTime", sdf.parse(startDate));
                }
                if (!com.alibaba.druid.util.StringUtils.isEmpty(endDate)) {
                    query.setTimestamp("endTime", sdf.parse(endDate));
                }
                if (doctorId != null) {
                    query.setParameter("doctor", doctorId);
                }
                if (organId != null) {
                    query.setParameter("clinicOrgan", organId);
                }
                query.setParameter("mpiid", mpiId);
                query.setParameterList("statusList", statusList);
                if (null != start && null != limit) {
                    query.setFirstResult(start);
                    query.setMaxResults(limit);
                }
                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);

        List<Recipe> recipes = action.getResult();
        Long totalConsumedTime = new Date().getTime() - beginTime;
        LOGGER.info("findRecipeListByDoctorAndPatientAndStatusList cost:{}", totalConsumedTime);
        return recipes;
    }

    /**
     * 查找指定科室和患者间开的处方单待处理列表
     *
     * @param depId
     * @param mpiId
     * @param startDate
     * @param endDate
     * @return
     */
    public List<Recipe> findRecipeListByDeptAndPatient(final Integer depId, final String mpiId, final String startDate, final String endDate) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "from Recipe where mpiid=:mpiid and Depart=:Depart and SignDate between :startDate and :endDate" + " and PayFlag = 0 and status in (2,8) order by createDate desc";
                Query query = ss.createQuery(hql);
                query.setParameter("Depart", depId);
                query.setParameter("mpiid", mpiId);
                if (null != startDate && null != endDate) {
                    query.setParameter("startDate", startDate);
                    query.setParameter("endDate", endDate);
                }
                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);

        List<Recipe> recipes = action.getResult();
        return recipes;
    }


    public List<Recipe> findDowloadedRecipeToFinishList(final String startDate, final String endDate) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from Recipe where fromflag in (1,2) and status = 18 and lastModify between '" + startDate + "' and '" + endDate + "'");

                Query query = ss.createQuery(hql.toString());

                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 查询过期的药师审核不通过，需要医生二次确认的处方
     * 处方中，审核标记位是一次审核不通过状态的
     *
     * @param startDt
     * @param endDt
     * @return
     */
    public List<Recipe> findFirstCheckNoPass(final String startDt, final String endDt) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select r from Recipe r where " + " r.signDate between '" + startDt + "' and '" + endDt + "' and r.status=" + RecipeStatusConstant.CHECK_NOT_PASS_YS + " and r.checkStatus = 1 ");
                Query q = ss.createQuery(hql.toString());
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);

        return action.getResult();
    }

    /**
     * 根据配送商和处方号更新是否已推送药企
     *
     * @param enterpriseId 药企ID
     * @param recipeIds    处方单号
     */
    @DAOMethod(sql = "update Recipe set pushFlag=1 where enterpriseId=:enterpriseId and recipeId in (:recipeIds)")
    public abstract void updateRecipeByDepIdAndRecipes(@DAOParam("enterpriseId") Integer enterpriseId, @DAOParam("recipeIds") List recipeIds);

    /**
     * 根据配送商和处方号更新是否已推送药企(支持相同appKey的多个药企)
     *
     * @param enterpriseId 药企ID
     * @param recipeIds    处方单号
     */
    @DAOMethod(sql = "update Recipe set pushFlag=1 where enterpriseId in (:enterpriseIds) and recipeId in (:recipeIds)")
    public abstract void updateRecipeByDepIdsAndRecipes(@DAOParam("enterpriseIds") List<Integer> enterpriseIds, @DAOParam("recipeIds") List<Integer> recipeIds);

    @DAOMethod(sql = "update Recipe set status=9 where orderCode=:orderCode ")
    public abstract void updateStatusByOrderCode(@DAOParam("orderCode") String orderCode);

    public long getCountByOrganAndDeptIds(Integer organId, List<Integer> deptIds, Integer plusDays) {
        AbstractHibernateStatelessResultAction<Long> action = new AbstractHibernateStatelessResultAction<Long>() {
            @Override
            public void execute(StatelessSession statelessSession) throws Exception {
                StringBuffer sql = new StringBuffer();
                sql.append("select count(RecipeID) from Recipe where depart in :deptIds and ClinicOrgan= :organId and DATE_FORMAT(CreateDate,'%Y-%m-%d')=:appointDate AND STATUS IN (2,3,4,5,6,7) ");


                Query query = statelessSession.createQuery(sql.toString());

                query.setParameterList("deptIds", deptIds);

                query.setInteger("organId", organId);
                query.setDate("appointDate", LocalDate.now().plusDays(plusDays).toDate());

                long num = (long) query.uniqueResult();

                setResult(num);


            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();

    }


    @DAOMethod(sql = "from Recipe where checkMode =:checkMode and status = 8 and reviewType in (1,2)")
    public abstract List<Recipe> findReadyCheckRecipeByCheckMode(@DAOParam("checkMode") Integer checkMode);

    @DAOMethod(sql = "from Recipe where clinicOrgan in:organIds and checkMode =:checkMode and status = 8 and reviewType in (1,2)")
    public abstract List<Recipe> findReadyCheckRecipeByOrganIdsCheckMode(@DAOParam("organIds") List<Integer> organIds, @DAOParam("checkMode") Integer checkMode);

    public List<Object[]> countRecipeIncomeGroupByDeptId(Date startDate, Date endDate, Integer organId) {
        HibernateStatelessResultAction<List<Object[]>> action = new AbstractHibernateStatelessResultAction<List<Object[]>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select sum(TotalMoney),Depart from Recipe where CreateDate between :startDate and :endDate and ClinicOrgan=: organId GROUP BY Depart");
                Query query = ss.createQuery(hql.toString());
                query.setParameter("organId", organId);
                query.setParameter("startDate", startDate);
                query.setParameter("endDate", endDate);
                setResult(query.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    /**
     * 根据科室统计复诊挂号收入
     *
     * @param startDate
     * @param endDate
     * @param organId
     * @return
     */
    @DAOMethod(sql = "select sum(totalMoney) from Recipe where clinicOrgan = :organId AND payFlag = 1 AND createDate BETWEEN :startDate AND :endDate AND bussSource = 2 AND depart in :deptIds")
    public abstract BigDecimal getRecipeIncome(@DAOParam("organId") Integer organId, @DAOParam("startDate") Date startDate, @DAOParam("endDate") Date endDate, @DAOParam("deptIds") List<Integer> deptIds);


    /**
     * 通过复诊业务来源调用查询写入HIS处方
     *
     * @param bussSource
     * @param clinicId
     * @return
     */
    @DAOMethod(sql = "from Recipe where bussSource=:bussSource and recipeSourceType in (1,2) and clinicId=:clinicId and recipeCode != null ")
    public abstract List<Recipe> findWriteHisRecipeByBussSourceAndClinicId(@DAOParam("bussSource") Integer bussSource, @DAOParam("clinicId") Integer clinicId);

    /**
     * 通过复诊业务来源调用查询诊疗处方
     *
     * @param bussSource
     * @param clinicId
     * @return
     */
    @DAOMethod(sql = "from Recipe where bussSource=:bussSource and clinicId=:clinicId and recipeSourceType = 3 ")
    public abstract List<Recipe> findTherapyRecipeByBussSourceAndClinicId(@DAOParam("bussSource") Integer bussSource, @DAOParam("clinicId") Integer clinicId);

    /**
     * @param bussSource
     * @param clinicId
     * @return
     */
    @DAOMethod(sql = "from Recipe where bussSource=:bussSource and recipeSourceType in (1,2) and clinicId=:clinicId and status in(2,3,4,5,6,7,8)")
    public abstract List<Recipe> findEffectiveRecipeByBussSourceAndClinicId(@DAOParam("bussSource") Integer bussSource, @DAOParam("clinicId") Integer clinicId);

    /**
     * @param bussSource
     * @param clinicId
     * @return
     */
    @DAOMethod(sql = "from Recipe where bussSource=:bussSource and clinicId=:clinicId  and recipeSourceType in (1,2) ")
    public abstract List<Recipe> findRecipeByBussSourceAndClinicId(@DAOParam("bussSource") Integer bussSource, @DAOParam("clinicId") Integer clinicId);

    /**
     * 根据 二方id 查询处方列表全部数据
     *
     * @param clinicId   二方业务id
     * @param bussSource 开处方来源 1问诊 2复诊(在线续方) 3网络门诊
     * @return
     */
    @DAOMethod(sql = "from Recipe where bussSource=:bussSource and clinicId=:clinicId and process_state!=8 order by createDate desc")
    public abstract List<Recipe> findRecipeAllByBussSourceAndClinicId(@DAOParam("bussSource") Integer bussSource, @DAOParam("clinicId") Integer clinicId);


    @DAOMethod(sql = "from Recipe where recipeId=:recipeId  and bussSource =2")
    public abstract List<Recipe> findRecipeByRecipeId(@DAOParam("recipeId") Integer recipeId);

    @DAOMethod
    public abstract List<Recipe> findByClinicId(Integer consultId);


    public List<Object[]> findMsgByparameters(Date startTime, Date endTime, Integer organId) {
        HibernateStatelessResultAction<List> action = new AbstractHibernateStatelessResultAction<List>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuffer sql = new StringBuffer("SELECT r.ClinicID,DATE(r.CreateDate) FROM cdr_recipe r LEFT JOIN cdr_recipeorder o ON r.OrderCode = o.OrderCode " + "WHERE r.CreateDate >= :startTime and r.CreateDate <= :endTime and r.bussSource=2 AND r.ClinicOrgan=:organId AND o.Effective=1 and o.PayFlag=1");
                Query query = ss.createSQLQuery(sql.toString());
                query.setParameter("startTime", startTime);
                query.setParameter("endTime", endTime);
                query.setParameter("organId", organId);
                List<Object[]> list = query.list();
                setResult(list);
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    public List<Object[]> findMsgByparametersByOrganIds(Date startTime, Date endTime, List<Integer> organId) {
        HibernateStatelessResultAction<List> action = new AbstractHibernateStatelessResultAction<List>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuffer sql = new StringBuffer("SELECT r.ClinicID,DATE(r.CreateDate) FROM cdr_recipe r LEFT JOIN cdr_recipeorder o ON r.OrderCode = o.OrderCode " + "WHERE r.CreateDate >= :startTime and r.CreateDate <= :endTime and r.bussSource=2 AND r.ClinicOrgan in :organId AND o.Effective=1 and o.PayFlag=1");
                Query query = ss.createSQLQuery(sql.toString());
                query.setParameter("startTime", startTime);
                query.setParameter("endTime", endTime);
                query.setParameterList("organId", organId);
                List<Object[]> list = query.list();
                setResult(list);
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    public List<Recipe> findRecipesByTabstatusForDoctor(final Integer doctorId, final Integer recipeId, final int start, final int limit, final Integer tapStatus) {

        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("from Recipe r where doctor=:doctorId and fromflag=1 and recipeId<:recipeId and status!=10  ");
                //通过条件查询status
                if (tapStatus == null || tapStatus == 0) {
                } else if (tapStatus == 1) {
                    hql.append("and status= " + RecipeStatusConstant.UNSIGN);//未签名
                } else if (tapStatus == 2) {
                    hql.append("and status not in(" + RecipeStatusConstant.UNSIGN + "," + RecipeStatusConstant.CHECK_NOT_PASS_YS + "," + RecipeStatusConstant.CHECK_NOT_PASS + "," + RecipeStatusConstant.HIS_FAIL + "," + RecipeStatusConstant.NO_DRUG + "," + RecipeStatusConstant.NO_PAY + "," + RecipeStatusConstant.NO_OPERATOR + "," + RecipeStatusConstant.RECIPE_MEDICAL_FAIL + "," + RecipeStatusConstant.EXPIRED + "," + RecipeStatusConstant.NO_MEDICAL_INSURANCE_RETURN + "," + RecipeStatusConstant.FINISH + "," + RecipeStatusConstant.REVOKE + ") ");
                } else if (tapStatus == 3) {
                    hql.append("and status in (" + RecipeStatusConstant.CHECK_NOT_PASS_YS + "," + RecipeStatusConstant.CHECK_NOT_PASS + ") ");//审核未通过
                } else if (tapStatus == 4) {
                    hql.append("and status in (" + RecipeStatusConstant.HIS_FAIL + "," + RecipeStatusConstant.NO_DRUG + "," + RecipeStatusConstant.NO_PAY + "," + RecipeStatusConstant.NO_OPERATOR + "," + RecipeStatusConstant.RECIPE_MEDICAL_FAIL + "," + RecipeStatusConstant.EXPIRED + "," + RecipeStatusConstant.NO_MEDICAL_INSURANCE_RETURN + "," + RecipeStatusConstant.FINISH + "," + RecipeStatusConstant.REVOKE + ") ");//[ 已结束 ]：包括 [ 已取消 ]、[ 已完成 ]、[ 已撤销 ]
                }
                hql.append("order by createDate desc ");
                Query query = ss.createQuery(hql.toString());
                query.setParameter("doctorId", doctorId);
                query.setParameter("recipeId", recipeId);
                query.setFirstResult(start);
                query.setMaxResults(limit);

                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);

        List<Recipe> recipes = action.getResult();
        return recipes;
    }

    public List<Recipe> findRecipesByTabstatusForDoctorNew(final Integer doctorId, final int start, final int limit, final Integer tapStatus) {

        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("from Recipe r where doctor=:doctorId and fromflag=1 and status!=10 and recipeSourceType in(1,2) ");
                //通过条件查询status
                if (tapStatus == null || tapStatus == 0) {
                } else if (tapStatus == 1) {
                    hql.append("and status= " + RecipeStatusConstant.UNSIGN);//未签名
                } else if (tapStatus == 2) {
                    hql.append("and status not in(" + RecipeStatusConstant.UNSIGN + "," + RecipeStatusConstant.CHECK_NOT_PASS_YS + "," + RecipeStatusConstant.CHECK_NOT_PASS + "," + RecipeStatusConstant.HIS_FAIL + "," + RecipeStatusConstant.NO_DRUG + "," + RecipeStatusConstant.NO_PAY + "," + RecipeStatusConstant.NO_OPERATOR + "," + RecipeStatusConstant.RECIPE_MEDICAL_FAIL + "," + RecipeStatusConstant.EXPIRED + "," + RecipeStatusConstant.NO_MEDICAL_INSURANCE_RETURN + "," + RecipeStatusConstant.FINISH + "," + RecipeStatusConstant.REVOKE + ") ");
                } else if (tapStatus == 3) {
                    hql.append("and status in (" + RecipeStatusConstant.CHECK_NOT_PASS_YS + "," + RecipeStatusConstant.CHECK_NOT_PASS + ") ");//审核未通过
                } else if (tapStatus == 4) {
                    hql.append("and status in (" + RecipeStatusConstant.HIS_FAIL + "," + RecipeStatusConstant.NO_DRUG + "," + RecipeStatusConstant.NO_PAY + "," + RecipeStatusConstant.NO_OPERATOR + "," + RecipeStatusConstant.RECIPE_MEDICAL_FAIL + "," + RecipeStatusConstant.EXPIRED + "," + RecipeStatusConstant.NO_MEDICAL_INSURANCE_RETURN + "," + RecipeStatusConstant.FINISH + "," + RecipeStatusConstant.REVOKE + ") ");//[ 已结束 ]：包括 [ 已取消 ]、[ 已完成 ]、[ 已撤销 ]
                }
                hql.append("order by createDate desc ");
                Query query = ss.createQuery(hql.toString());
                query.setParameter("doctorId", doctorId);
                query.setFirstResult(start);
                query.setMaxResults(limit);

                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);

        List<Recipe> recipes = action.getResult();
        return recipes;
    }


    @DAOMethod(sql = "from Recipe where mpiid =:mpiId")
    public abstract List<Recipe> findByMpiId(@DAOParam("mpiId") String mpiId);

    @DAOMethod(sql = "from Recipe where RecipeID =:recipeId and EnterpriseId =:depId")
    public abstract Recipe getByRecipeIdAndEnterpriseId(@DAOParam("depId") Integer depId, @DAOParam("recipeId") Integer recipeId);

    /**
     * 药师搜索方法 开方医生 审方医生 患者姓名 患者patientId
     *
     * @param organs
     * @param searchString
     * @param start
     * @param limit
     * @return
     * @author zhongzx
     */
    public List<Recipe> searchRecipeByDepartName(final Set<Integer> organs, final String searchString, final List<Integer> departIds, final Integer start, final Integer limit) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select r from Recipe r ");
                hql.append("where (r.checkDateYs is not null or r.status = 8) ");
                hql.append("and r.clinicOrgan in (:organs) ");
                if (CollectionUtils.isNotEmpty(departIds)) {
                    hql.append("and (r.appointDepartName LIKE :searchString or r.depart in (:departIds)) ");
                } else {
                    hql.append("and r.appointDepartName LIKE :searchString ");
                }
                hql.append("order by r.signDate desc ");

                Query q = ss.createQuery(hql.toString());
                q.setParameter("searchString", searchString + "%");
                q.setParameterList("organs", organs);
                if (CollectionUtils.isNotEmpty(departIds)) {
                    q.setParameterList("departIds", departIds);
                }
                if (null != start && null != limit) {
                    q.setFirstResult(start);
                    q.setMaxResults(limit);
                }
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    public List<Recipe> searchRecipe(final Set<Integer> organs, final Integer searchFlag, final String searchString, final Integer start, final Integer limit) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select r from Recipe r");
                if (0 == searchFlag) {
                    hql.append(" where r.doctorName like :searchString ");
                } else if (2 == searchFlag) {
                    hql.append(" where r.patientName like :searchString ");
                } else if (3 == searchFlag) {
                    hql.append(" where CAST(r.recipeId as string) like :searchString ");
                } else {
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "searchFlag is invalid");
                }
                hql.append("and r.checkMode < 2 AND r.clinicOrgan in (:organs) and r.auditState in (1,2,3,4,5,6,7) order by r.signDate desc");

                Query q = ss.createQuery(hql.toString());
                if (3 == searchFlag) {
                    int recipeId;
                    try {
                        recipeId = Integer.parseInt(searchString);
                    } catch (NumberFormatException e) {
                        recipeId = -1;
                    }
                    q.setParameter("searchString", recipeId + "%");
                } else {
                    q.setParameter("searchString", searchString + "%");
                }
                q.setParameterList("organs", organs);
                if (null != start && null != limit) {
                    q.setFirstResult(start);
                    q.setMaxResults(limit);
                }
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod(sql = "from Recipe where recipeId in(:recipeIds) and clinicOrgan in(:organIds)", limit = 0)
    public abstract List<Recipe> findByRecipeAndOrganId(@DAOParam("recipeIds") List<Integer> recipeIds, @DAOParam("organIds") Set<Integer> organIds);

    /**
     * 根据需要变更的状态获取处方ID集合
     *
     * @param
     * @return
     */
    public List<Recipe> getRecipeListForSignCancelRecipe(final String startDt, final String endDt) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder sql = new StringBuilder("select * from cdr_recipe where status in (30, 26) and (fast_recipe_flag is null or fast_recipe_flag !=1) and signDate between '" + startDt + "' and '" + endDt + "' ");
                sql.append("UNION ALL ");
                //date 20200922 添加药师CA未签名过期
                sql.append("select * from cdr_recipe where status in (31, 27, 32) and (fast_recipe_flag is null or fast_recipe_flag !=1) and reviewType = 1 and signDate between '" + startDt + "' and '" + endDt + "' ");
                Query q = ss.createSQLQuery(sql.toString()).addEntity(Recipe.class);
                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod(sql = "select count(*) from Recipe")
    public abstract Long getCountByAll();

    /**
     * @param organIds
     * @param type     0 包含1和2   1医生强制通过  2不包含医生强制通过
     * @return
     */
    public List<Integer> queryRecipeIdByOrgan(List<Integer> organIds, List<Integer> recipeTypes, Integer type) {
        HibernateStatelessResultAction<List<Integer>> action = new AbstractHibernateStatelessResultAction<List<Integer>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select recipeId from Recipe where clinicOrgan in(:organIds) and  checkMode<3  and status not in (9,31)  and checkOrgan IS NOT NULL");
                hql.append(" and  recipeType in(:recipeTypes)");
                if (type.equals(1)) {
                    hql.append(" and  supplementaryMemo IS NOT NULL");
                } else if (type.equals(2)) {
                    hql.append(" and  supplementaryMemo IS NULL");
                }
                Query q = ss.createQuery(hql.toString());
                q.setParameterList("organIds", organIds);
                q.setParameterList("recipeTypes", recipeTypes);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);

        return action.getResult();
    }

    public List<Recipe> queryRecipeInfoByOrganAndRecipeType(List<Integer> organIds, List<Integer> recipeTypes, Date date) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select new Recipe(recipeId,supplementaryMemo) from Recipe where clinicOrgan in(:organIds)  and  checkMode<3");
                hql.append("  and status not in (9,31,32) and checkOrgan IS NOT NULL and createDate>:date");
                Query q = ss.createQuery(hql.toString());
                q.setParameterList("organIds", organIds);
                q.setParameter("date", date);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);

        return action.getResult();
    }

    /**
     * 根据 患者id和机构id进行查询处方单，并且根据开放时间进行倒序
     *
     * @param mpiId       患者id
     * @param clinicOrgan 机构id
     * @param start
     * @param limit
     * @return
     */
    @DAOMethod(sql = "from Recipe where clinicOrgan=:clinicOrgan and mpiId=:mpiId order by signDate DESC", limit = 0)
    public abstract List<Recipe> queryRecipeInfoByMpiIdAndOrganId(@DAOParam("mpiId") String mpiId, @DAOParam("clinicOrgan") Integer clinicOrgan, @DAOParam(pageStart = true) int start, @DAOParam(pageLimit = true) int limit);

    @DAOMethod(sql = "from Recipe where clinicOrgan in(:clinicOrgans) and mpiId=:mpiId order by signDate DESC", limit = 0)
    public abstract List<Recipe> queryRecipeInfoByMpiIdAndOrganIds(@DAOParam("mpiId") String mpiId, @DAOParam("clinicOrgans") List<Integer> clinicOrgans, @DAOParam(pageStart = true) int start, @DAOParam(pageLimit = true) int limit);

    public Integer getNumCanMergeRecipeByMergeRecipeWay(String mpiId, String registerId, Integer organId, String mergeRecipeWay, String chronicDiseaseName) {
        HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder sql = new StringBuilder("select COUNT(d.RecipeID) from cdr_recipe d,cdr_recipe_ext e where d.RecipeID = e.recipeId ");
                sql.append("and e.registerID =:registerId and d.ClinicOrgan =:organId and d.MPIID =:mpiId and d.status = 2 and d.orderCode is null ");
                if ("e.registerId,e.chronicDiseaseName".equals(mergeRecipeWay)) {
                    if (StringUtils.isNotEmpty(chronicDiseaseName)) {
                        sql.append("and e.chronicDiseaseName =:chronicDiseaseName ");
                    } else {
                        sql.append("and e.chronicDiseaseName is null ");
                    }
                    sql.append("GROUP BY e.registerID,d.ClinicOrgan,e.chronicDiseaseName");
                } else {
                    sql.append("GROUP BY e.registerID,d.ClinicOrgan");
                }
                Query q = ss.createSQLQuery(sql.toString());
                q.setParameter("registerId", registerId);
                q.setParameter("organId", organId);
                q.setParameter("mpiId", mpiId);
                if ("e.registerId,e.chronicDiseaseName".equals(mergeRecipeWay) && StringUtils.isNotEmpty(chronicDiseaseName)) {
                    q.setParameter("chronicDiseaseName", chronicDiseaseName);
                }
                if (q.uniqueResult() == null) {
                    setResult(0);
                } else {
                    Number number = (Number) q.uniqueResult();
                    setResult(number.intValue());
                }
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 复诊Id查询当前有效的处方单
     *
     * @param clinicId
     * @return
     */
    @DAOMethod(sql = "from Recipe where clinicId=:clinicId and status not in(:status)")
    public abstract List<Recipe> findRecipeClinicIdAndStatus(@DAOParam("clinicId") Integer clinicId, @DAOParam("status") List<Integer> status);

    @DAOMethod(sql = "from Recipe where clinicId=:clinicId and status not in(:status) and process_state not in(:processState)")
    public abstract List<Recipe> findRecipeClinicIdAndStatusAndProcessState(@DAOParam("clinicId") Integer clinicId, @DAOParam("status") List<Integer> status, @DAOParam("processState") List<Integer> processState);

    @DAOMethod(sql = "from Recipe where clinicId=:clinicId and process_state in(:processState)")
    public abstract List<Recipe> findRecipeClinicIdAndProcessState(@DAOParam("clinicId") Integer clinicId, @DAOParam("processState") List<Integer> processState);

    @DAOMethod(sql = "from Recipe where clinicId=:clinicId and bussSource=:bussSource  and status in(:status)")
    public abstract List<Recipe> findRecipeClinicIdAndStatusV1(@DAOParam("clinicId") Integer clinicId, @DAOParam("bussSource") Integer bussSource, @DAOParam("status") List<Integer> status);

    /**
     * 处方数据  处方明细数据
     *
     * @param organId
     * @param startDate
     * @param endDate
     * @return
     */
    public List<Recipe> findRecipeListByOrganIdAndTime(final Integer organId, final String startDate, final String endDate) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("SELECT * from cdr_recipe r where fromflag=1 and clinicOrgan =:organId and r.status>0" + " and ( (r.signDate between '" + startDate + "' and '" + endDate + "') ");
                hql.append(")");
                Query q = ss.createSQLQuery(hql.toString());
                q.setParameter("organId", organId);
                ((SQLQuery) q).addEntity(Recipe.class);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    /**
     * 查询所有待审核平台审方
     *
     * @return
     */
    @DAOMethod(sql = "select new Recipe(recipeId,clinicOrgan,recipeType) from Recipe where  checkMode<2 and status =8 and  createDate>:date", limit = 0)
    public abstract List<Recipe> findToAuditPlatformRecipe(@DAOParam("date") Date date);

    /**
     * 查询已经支付过的处方
     *
     * @param clinicOrgan
     * @param recipeCodeList
     * @return
     */
    @DAOMethod(sql = " From Recipe where clinicOrgan=:clinicOrgan and recipeCode in (:recipeCodeList) and payFlag in(1,2,3) ")
    public abstract List<Recipe> findRecipeByRecipeCodeAndClinicOrgan(@DAOParam("clinicOrgan") int clinicOrgan, @DAOParam("recipeCodeList") List<String> recipeCodeList);

    @DAOMethod(sql = " From Recipe where clinicOrgan=:clinicOrgan and recipeId in (:recipeIdList) and status in(14) ")
    public abstract List<Recipe> findRecipeByRecipeIdAndClinicOrgan(@DAOParam("clinicOrgan") int clinicOrgan, @DAOParam("recipeIdList") List<Integer> recipeIdList);

    @DAOMethod(sql = " From Recipe where (mpiId =:mpiId or requestMpiId =:mpiId) and recipeSourceType = 1 and orderCode is null and status = 2 ")
    public abstract List<Recipe> findRecipeByMpiId(@DAOParam("mpiId") String mpiId);


    /**
     * 根据处方状态批量查询处方
     *
     * @param allMpiIds
     * @param start
     * @param limit
     */
    public List<RecipeListBean> findOnReadyRecipeListByMPIId(List<String> allMpiIds, Integer start, Integer limit, String tabStatus, List<Integer> recipeStatus, Date startTime, Date endTime) {
        HibernateStatelessResultAction<List<RecipeListBean>> action = new AbstractHibernateStatelessResultAction<List<RecipeListBean>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                boolean timeSearchFlag = Objects.nonNull(startTime) && Objects.nonNull(endTime);

                hql.append("select * from (");
                hql.append("SELECT r.RecipeID,r.orderCode,(CASE WHEN ( r.reviewType = 1 AND r.checkStatus = 1 AND r.STATUS = 15 ) THEN" +
                        " 8 ELSE r.STATUS " +
                        "END ) AS STATUS,r.patientName,r.fromflag,r.recipeCode,r.doctorName,r.recipeType,r.organDiseaseName, " +
                        "r.clinicOrgan,r.organName,r.signFile,r.chemistSignFile,r.signDate,r.recipeMode,r.recipeSource,r.mpiid,r.depart, " +
                        "r.enterpriseId,e.registerID,e.chronicDiseaseName,o.OrderId,IFNULL(o.CreateTime,r.signDate) as time ," +
                        "o.Status as orderStatus,r.GiveMode,o.PayMode,r.process_state,r.sub_state, r.targeted_drug_type,r.medical_flag,r.reviewType " +
                        " FROM cdr_recipe r left join cdr_recipeorder o on r.OrderCode = o.OrderCode left join " +
                        " cdr_recipe_ext e  on r.RecipeID = e.recipeId " +
                        " WHERE r.mpiid IN (:allMpiIds) AND r.recipeSourceType = 1");
                if (timeSearchFlag) {
                    hql.append(" AND r.CreateDate > :startTime AND r.CreateDate < :endTime ");
                }
                if ("onready".equals(tabStatus)) {
                    hql.append(" AND ((r.recipeMode != 'zjjgpt' && r.STATUS IN ( :recipeStatus ) OR ( r.reviewType != 0 AND r.checkStatus = 1 AND r.STATUS = 15 ) " +
                            ") OR ( r.recipeMode = 'zjjgpt' AND r.STATUS IN ( 2, 22 ) ) ) ");
                    hql.append(" AND r.orderCode IS NULL ");
                }
                hql.append(" ) a ORDER BY a.time DESC ");

                Query q = ss.createSQLQuery(hql.toString());
                q.setParameterList("allMpiIds", allMpiIds);
                q.setParameterList("recipeStatus", recipeStatus);
                if (timeSearchFlag) {
                    q.setParameter("startTime", startTime);
                    q.setParameter("endTime", endTime);
                }

                logger.info("findRecipeListByMPIId hql={}", hql.toString());
                List<Object[]> result = q.list();
                logger.info("findOnReadyRecipeListByMPIId result:{}", JSON.toJSONString(result));
                List<RecipeListBean> backList = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(result)) {
                    RecipeListBean recipeListBean;
                    for (Object[] objs : result) {
                        recipeListBean = new RecipeListBean();
                        recipeListBean.setRecipeId(Integer.valueOf(objs[0].toString()));
                        if (null != objs[1]) {
                            recipeListBean.setOrderCode(objs[1].toString());
                        }
                        if (null != objs[2]) {
                            recipeListBean.setStatus(Integer.valueOf(objs[2].toString()));
                        }
                        if (null != objs[3]) {
                            recipeListBean.setPatientName(objs[3].toString());
                        }
                        if (null != objs[4]) {
                            recipeListBean.setFromFlag(Integer.valueOf(objs[4].toString()));
                        }
                        if (null != objs[5]) {
                            recipeListBean.setRecipeCode(objs[5].toString());
                        }
                        if (null != objs[6]) {
                            recipeListBean.setDoctorName(objs[6].toString());
                        }
                        if (null != objs[7]) {
                            recipeListBean.setRecipeType(Integer.valueOf(objs[7].toString()));
                        }
                        if (null != objs[8]) {
                            recipeListBean.setOrganDiseaseName(objs[8].toString());
                        }
                        if (null != objs[9]) {
                            recipeListBean.setClinicOrgan(Integer.valueOf(objs[9].toString()));
                        }
                        if (null != objs[10]) {
                            recipeListBean.setOrganName(objs[10].toString());
                        }
                        if (null != objs[11]) {
                            recipeListBean.setSignFile(objs[11].toString());
                        }
                        if (null != objs[12]) {
                            recipeListBean.setChemistSignFile(objs[12].toString());
                        }
                        if (null != objs[13]) {
                            recipeListBean.setSignDate((Date) objs[13]);
                        }
                        if (null != objs[14]) {
                            recipeListBean.setRecipeMode(objs[14].toString());
                        }
                        if (null != objs[15]) {
                            recipeListBean.setRecipeSource(Integer.valueOf(objs[15].toString()));
                        }
                        if (null != objs[16]) {
                            recipeListBean.setMpiid(objs[16].toString());
                        }
                        if (null != objs[17]) {
                            recipeListBean.setDepart(Integer.valueOf(objs[17].toString()));
                        }
                        if (null != objs[18]) {
                            recipeListBean.setEnterpriseId(Integer.valueOf(objs[18].toString()));
                        }
                        if (null != objs[19]) {
                            recipeListBean.setRegisterID(objs[19].toString());
                        }
                        if (null == objs[19]) {
                            recipeListBean.setRegisterID("-1");
                        }
                        if (null != objs[20]) {
                            recipeListBean.setChronicDiseaseName(objs[20].toString());
                        }
                        if (null == objs[20]) {
                            recipeListBean.setChronicDiseaseName("-1");
                        }
                        if (null != objs[21]) {
                            recipeListBean.setOrderId(Integer.valueOf(objs[21].toString()));
                        }
                        if (null != objs[23]) {
                            recipeListBean.setOrderStatus(Integer.valueOf(objs[23].toString()));
                        }
                        if (null != objs[24]) {
                            recipeListBean.setGiveMode(Integer.valueOf(objs[24].toString()));
                        }
                        if (null != objs[25]) {
                            recipeListBean.setPayMode(Integer.valueOf(objs[25].toString()));
                        }
                        if (null != objs[26]) {
                            recipeListBean.setProcessState(Integer.valueOf(objs[26].toString()));
                        }
                        if (null != objs[27]) {
                            recipeListBean.setSubState(Integer.valueOf(objs[27].toString()));
                        }
                        if (null != objs[28]) {
                            recipeListBean.setTargetedDrugType(Integer.valueOf(objs[28].toString()));
                        }
                        if (null != objs[29]) {
                            recipeListBean.setMedicalFlag(Integer.valueOf(objs[29].toString()));
                        }
                        if (null != objs[30]) {
                            recipeListBean.setReviewType(Integer.valueOf(objs[30].toString()));
                        }

                        backList.add(recipeListBean);
                    }
                }

                setResult(backList);
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据处方状态批量查询处方
     *
     * @param allMpiIds
     * @param start
     * @param limit
     */
    public List<RecipeListBean> findRecipeListByMPIId(List<String> allMpiIds, Integer start, Integer limit, String tabStatus, List<Integer> recipeStatus, Date startTime, Date endTime) {
        HibernateStatelessResultAction<List<RecipeListBean>> action = new AbstractHibernateStatelessResultAction<List<RecipeListBean>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                boolean timeSearchFlag = Objects.nonNull(startTime) && Objects.nonNull(endTime);

                hql.append("select * from (");
                hql.append("SELECT r.RecipeID,r.orderCode,(CASE WHEN ( r.reviewType = 1 AND r.checkStatus = 1 AND r.STATUS = 15 ) THEN" +
                        " 8 ELSE r.STATUS " +
                        "END ) AS STATUS,r.patientName,r.fromflag,r.recipeCode,r.doctorName,r.recipeType,r.organDiseaseName, " +
                        "r.clinicOrgan,r.organName,r.signFile,r.chemistSignFile,r.signDate,r.recipeMode,r.recipeSource,r.mpiid,r.depart, " +
                        "r.enterpriseId,e.registerID,e.chronicDiseaseName,o.OrderId,IFNULL(o.CreateTime,r.signDate) as time ," +
                        "o.Status as orderStatus,r.GiveMode,o.PayMode,r.process_state,r.sub_state, r.targeted_drug_type,r.medical_flag " +
                        " FROM cdr_recipe r left join cdr_recipeorder o on r.OrderCode = o.OrderCode left join " +
                        " cdr_recipe_ext e  on r.RecipeID = e.recipeId " +
                        " WHERE r.mpiid IN (:allMpiIds) AND r.recipeSourceType = 1");
                if (timeSearchFlag) {
                    hql.append(" AND r.CreateDate > :startTime AND r.CreateDate < :endTime ");
                }
                if ("onready".equals(tabStatus)) {
                    hql.append(" AND ((r.recipeMode != 'zjjgpt' && r.STATUS IN ( :recipeStatus ) OR ( r.reviewType != 0 AND r.checkStatus = 1 AND r.STATUS = 15 ) " +
                            ") OR ( r.recipeMode = 'zjjgpt' AND r.STATUS IN ( 2, 22 ) ) ) ");
                    hql.append(" AND r.orderCode IS NULL ");
                } else if ("ongoing".equals(tabStatus)) {
                    hql.append(" AND (( r.STATUS IN ( :recipeStatus ) ) or (r.reviewType = 2 AND r.checkStatus = 1 AND r.STATUS = 15)) ");
                } else if ("isover".equals(tabStatus)) {
                    hql.append(" AND r.STATUS IN ( :recipeStatus ) AND r.RecipeID not in (select RecipeID from cdr_recipe where  reviewType != 0 AND checkStatus = 1 AND STATUS = 15)   ");
                }
                if ("ongoing".equals(tabStatus)) {
                    hql.append(" UNION ALL SELECT r.RecipeID,r.orderCode,r.STATUS,r.patientName,r.fromflag,r.recipeCode,r.doctorName,r.recipeType,r.organDiseaseName,r.clinicOrgan," +
                            " r.organName,r.signFile,r.chemistSignFile,r.signDate,r.recipeMode,r.recipeSource,r.mpiid,r.depart,r.enterpriseId,e.registerID,e.chronicDiseaseName," +
                            " o.OrderId,IFNULL( o.CreateTime, r.signDate ) AS time,o.STATUS AS orderStatus,r.GiveMode,o.PayMode ,r.process_state,r.sub_state, r.targeted_drug_type,r.medical_flag " +
                            " FROM " +
                            " cdr_recipe r " +
                            " LEFT JOIN cdr_recipeorder o ON r.OrderCode = o.OrderCode " +
                            " LEFT JOIN cdr_recipe_ext e ON r.RecipeID = e.recipeId " +
                            " WHERE " +
                            " r.STATUS IN ( 2,8,27,31,32 ) " +
                            " AND r.mpiid IN ( :allMpiIds )" +
                            " AND r.recipeSourceType = 1 " +
                            " AND r.orderCode IS NOT NULL");
                    if (timeSearchFlag) {
                        hql.append(" AND r.CreateDate > :startTime AND r.CreateDate < :endTime ");
                    }
                }
                hql.append(" ) a ORDER BY a.time DESC LIMIT :start,:limit");

                Query q = ss.createSQLQuery(hql.toString());
                q.setParameterList("allMpiIds", allMpiIds);
                q.setParameterList("recipeStatus", recipeStatus);
                q.setParameter("start", start);
                q.setParameter("limit", limit);
                if (timeSearchFlag) {
                    q.setParameter("startTime", startTime);
                    q.setParameter("endTime", endTime);
                }

                logger.info("findRecipeListByMPIId hql={}", hql.toString());
                List<Object[]> result = q.list();
                List<RecipeListBean> backList = new ArrayList<>(limit);
                if (CollectionUtils.isNotEmpty(result)) {
                    RecipeListBean recipeListBean;
                    for (Object[] objs : result) {
                        recipeListBean = new RecipeListBean();
                        recipeListBean.setRecipeId(Integer.valueOf(objs[0].toString()));
                        if (null != objs[1]) {
                            recipeListBean.setOrderCode(objs[1].toString());
                        }
                        if (null != objs[2]) {
                            recipeListBean.setStatus(Integer.valueOf(objs[2].toString()));
                        }
                        if (null != objs[3]) {
                            recipeListBean.setPatientName(objs[3].toString());
                        }
                        if (null != objs[4]) {
                            recipeListBean.setFromFlag(Integer.valueOf(objs[4].toString()));
                        }
                        if (null != objs[5]) {
                            recipeListBean.setRecipeCode(objs[5].toString());
                        }
                        if (null != objs[6]) {
                            recipeListBean.setDoctorName(objs[6].toString());
                        }
                        if (null != objs[7]) {
                            recipeListBean.setRecipeType(Integer.valueOf(objs[7].toString()));
                        }
                        if (null != objs[8]) {
                            recipeListBean.setOrganDiseaseName(objs[8].toString());
                        }
                        if (null != objs[9]) {
                            recipeListBean.setClinicOrgan(Integer.valueOf(objs[9].toString()));
                        }
                        if (null != objs[10]) {
                            recipeListBean.setOrganName(objs[10].toString());
                        }
                        if (null != objs[11]) {
                            recipeListBean.setSignFile(objs[11].toString());
                        }
                        if (null != objs[12]) {
                            recipeListBean.setChemistSignFile(objs[12].toString());
                        }
                        if (null != objs[13]) {
                            recipeListBean.setSignDate((Date) objs[13]);
                        }
                        if (null != objs[14]) {
                            recipeListBean.setRecipeMode(objs[14].toString());
                        }
                        if (null != objs[15]) {
                            recipeListBean.setRecipeSource(Integer.valueOf(objs[15].toString()));
                        }
                        if (null != objs[16]) {
                            recipeListBean.setMpiid(objs[16].toString());
                        }
                        if (null != objs[17]) {
                            recipeListBean.setDepart(Integer.valueOf(objs[17].toString()));
                        }
                        if (null != objs[18]) {
                            recipeListBean.setEnterpriseId(Integer.valueOf(objs[18].toString()));
                        }
                        if (null != objs[19]) {
                            recipeListBean.setRegisterID(objs[19].toString());
                        }
                        if (null == objs[19]) {
                            recipeListBean.setRegisterID("-1");
                        }
                        if (null != objs[20]) {
                            recipeListBean.setChronicDiseaseName(objs[20].toString());
                        }
                        if (null == objs[20]) {
                            recipeListBean.setChronicDiseaseName("-1");
                        }
                        if (null != objs[21]) {
                            recipeListBean.setOrderId(Integer.valueOf(objs[21].toString()));
                        }
                        if (null != objs[23]) {
                            recipeListBean.setOrderStatus(Integer.valueOf(objs[23].toString()));
                        }
                        if (null != objs[24]) {
                            recipeListBean.setGiveMode(Integer.valueOf(objs[24].toString()));
                        }
                        if (null != objs[25]) {
                            recipeListBean.setPayMode(Integer.valueOf(objs[25].toString()));
                        }
                        if (null != objs[26]) {
                            recipeListBean.setProcessState(Integer.valueOf(objs[26].toString()));
                        }
                        if (null != objs[27]) {
                            recipeListBean.setSubState(Integer.valueOf(objs[27].toString()));
                        }
                        if (null != objs[28]) {
                            recipeListBean.setTargetedDrugType(Integer.valueOf(objs[28].toString()));
                        }
                        if (null != objs[29]) {
                            recipeListBean.setMedicalFlag(Integer.valueOf(objs[29].toString()));
                        }

                        backList.add(recipeListBean);
                    }
                }

                setResult(backList);
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod(sql = "from Recipe where  orderCode is null and status = 2 and invalidTime >:currentTime", limit = 0)
    public abstract List<Recipe> findInvalidRecipeByOrganId(@DAOParam("currentTime") Date currentTime);

    @DAOMethod(sql = "from Recipe where orderCode is not null and PayFlag = 0 and invalidTime >:currentTime ", limit = 0)
    public abstract List<Recipe> findInvalidOrderByOrganId( @DAOParam("currentTime") Date currentTime);


    /**
     * 根据复诊ID查询状态为药师未审核的处方个数
     *
     * @param bussSource 处方来源
     * @param ClinicID   复诊ID
     * @date 2021/7/19
     */
    @DAOMethod(sql = "SELECT COUNT(1) FROM Recipe WHERE bussSource=:bussSource AND ClinicID=:ClinicID AND status IN (:recipeStatus)")
    public abstract Long getRecipeCountByBussSourceAndClinicIdAndStatus(@DAOParam("bussSource") Integer bussSource, @DAOParam("ClinicID") Integer ClinicID, @DAOParam("recipeStatus") List<Integer> recipeStatus);

    /**
     * @param startTime
     * @param endTime
     * @param recipeIds
     * @param organId
     * @return
     */
    public List<Recipe> queryRevisitTrace(String startTime, String endTime, List<Integer> recipeIds, Integer organId) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select r from Recipe r where clinicId is not null and bussSource =2  ");
                if (StringUtils.isNotEmpty(startTime)) {
                    hql.append(" and r.createDate >= :startTime");
                }
                if (StringUtils.isNotEmpty(endTime)) {
                    hql.append(" and r.createDate <= :endTime");
                }
                if (organId != null) {
                    hql.append(" and organId =:organId ");
                }
                if (CollectionUtils.isNotEmpty(recipeIds)) {
                    hql.append(" and recipeId in(:recipeIds) ");
                }
                Query query = ss.createQuery(hql.toString());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                query.setTimestamp("startTime", sdf.parse(startTime));
                query.setTimestamp("endTime", sdf.parse(endTime));
                if (organId != null) {
                    query.setParameter("organId", organId);
                }
                if (CollectionUtils.isNotEmpty(recipeIds)) {
                    query.setParameterList("recipeIds", recipeIds);
                }
                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 迁移数据使用
     *
     * @return
     */
    public List<RecipeOrder> findMoveData() {
        HibernateStatelessResultAction<List<RecipeOrder>> action = new AbstractHibernateStatelessResultAction<List<RecipeOrder>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "select\n" +
                        "  o.OrderCode,\n" +
                        "  r.RecipeID,\n" +
                        "  e.fundAmount,\n" +
                        "  e.preSettleTotalAmount,\n" +
                        "  e.cashAmount\n" +
                        "from\n" +
                        "  cdr_recipe r,\n" +
                        "  cdr_recipe_ext e,\n" +
                        "  cdr_recipeorder o\n" +
                        "where\n" +
                        "  r.recipeId = e.recipeId\n" +
                        "  AND o.Ordercode = r.orderCode\n" +
                        "  AND e.preSettleTotalAmount != 0\n" +
                        "  AND e.preSettleTotalAmount != o.preSettleTotalAmount\n" +
                        "  AND o.preSettleTotalAmount = 0";
                Query q = ss.createSQLQuery(hql);
                List<Object[]> result = q.list();
                List<RecipeOrder> backList = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(result)) {
                    RecipeOrder recipeListBean;
                    for (Object[] objs : result) {
                        recipeListBean = new RecipeOrder("");
                        if (null != objs[0]) {
                            recipeListBean.setOrderCode(objs[0].toString());
                        }
                        if (null != objs[1]) {
                            recipeListBean.setOrderId(Integer.valueOf(objs[1].toString()));
                        }
                        if (null != objs[2]) {
                            recipeListBean.setFundAmount(Double.valueOf(objs[2].toString()));
                        }
                        if (null != objs[3]) {
                            recipeListBean.setPreSettletotalAmount(Double.valueOf(objs[3].toString()));
                        }
                        if (null != objs[4]) {
                            recipeListBean.setCashAmount(Double.valueOf(objs[4].toString()));
                        }
                        backList.add(recipeListBean);
                    }
                }

                setResult(backList);

            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * @param startTime
     * @param endTime
     * @param recipeIds
     * @param organId
     * @return
     */
    public List<Recipe> queryRecipeByTimeAndRecipeIdsAndOrganId(String startTime, String endTime, List<Integer> recipeIds, Integer organId) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select r from Recipe r where 1=1  ");
                if (StringUtils.isNotEmpty(startTime)) {
                    hql.append(" and r.createDate >= :startTime");
                }
                if (StringUtils.isNotEmpty(endTime)) {
                    hql.append(" and r.createDate <= :endTime");
                }
                if (organId != null) {
                    hql.append(" and organId =:organId ");
                }
                if (CollectionUtils.isNotEmpty(recipeIds)) {
                    hql.append(" and recipeId in(:recipeIds) ");
                }
                Query query = ss.createQuery(hql.toString());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                if (StringUtils.isNotEmpty(startTime)) {
                    query.setTimestamp("startTime", sdf.parse(startTime));
                }
                if (StringUtils.isNotEmpty(endTime)) {
                    query.setTimestamp("endTime", sdf.parse(endTime));
                }
                if (organId != null) {
                    query.setParameter("organId", organId);
                }
                if (CollectionUtils.isNotEmpty(recipeIds)) {
                    query.setParameterList("recipeIds", recipeIds);
                }
                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod(sql = " FROM Recipe WHERE status in (:status) AND invalidTime >= :invalidTime", limit = 0)
    public abstract List<Recipe> findRecipesByStatusAndInvalidTime(@DAOParam("status") List<Integer> status, @DAOParam("invalidTime") Date invalidTime);

    public List<Recipe> findRecipesByClinicOrganAndMpiId(Integer clinicOrgan, String mpiId, Date startTime, Date endTime){
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select r from Recipe r where clinicOrgan =:clinicOrgan and mpiId =:mpiId and orderCode is not null");
                if (startTime != null) {
                    hql.append(" and r.createDate >= :startTime");
                }
                if (endTime != null) {
                    hql.append(" and r.createDate <= :endTime");
                }
                Query query = ss.createQuery(hql.toString());
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                if (startTime != null) {
                    query.setParameter("startTime", startTime);
                }
                if (endTime != null) {
                    query.setParameter("endTime", endTime);
                }
                query.setParameter("clinicOrgan", clinicOrgan);
                query.setParameter("mpiId", mpiId);
                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    public List<Recipe> findRecipeAuditByFlag(final List<Integer> organ, List<Integer> recipeTypes, Integer checker, final int flag, final int start, final int limit, String startTime, String endTime) {
        final int all = 3;
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                //0是待药师审核
                if (flag == 0) {
                    //hql.append("from Recipe where clinicOrgan in (:organ)  and checkMode<2 and status = " + RecipeStatusConstant.READY_CHECK_YS + " and  (recipeType in(:recipeTypes) or grabOrderStatus=1)");
                    hql.append("SELECT\n" +
                            "\tr.*\n" +
                            "FROM\n" +
                            "\tcdr_recipe r\n" +
                            "LEFT JOIN cdr_recipe_ext cre ON r.recipeid = cre.recipeid\n" +
                            "WHERE cre.canUrgentAuditRecipe is not null and r.clinicOrgan in (:organ) and r.checkMode<2 and  r.audit_state = 1 and  (r.recipeType in(:recipeTypes) or r.grabOrderStatus=1) ");
                    if (StringUtils.isNoneBlank(startTime) && StringUtils.isNoneBlank(endTime)) {
                        hql.append(" and  r.CreateDate >= :startTime and  r.CreateDate <= :endTime");
                    }
                    hql.append(" ORDER BY canUrgentAuditRecipe desc, r.grabOrderStatus DESC, signdate asc");
                }
                //1是审核通过
                else if (flag == 1) {
                    hql.append("from Recipe  where clinicOrgan in (:organ)  and Checker = :checker  and audit_state = 5 ");
                    if (StringUtils.isNoneBlank(startTime) && StringUtils.isNoneBlank(endTime)) {
                        hql.append(" and CreateDate >= :startTime and CreateDate <= :endTime");
                    }
                    hql.append(" order by signDate desc");
                }
                //2是审核未通过
                else if (flag == 2) {
                    hql.append("from Recipe where clinicOrgan in (:organ)  and Checker = :checker  and audit_state in (3,4,6) ");
                    if (StringUtils.isNoneBlank(startTime) && StringUtils.isNoneBlank(endTime)) {
                        hql.append(" and CreateDate >= :startTime and CreateDate <= :endTime");
                    }
                    hql.append(" order by signDate desc");
                }
                //3是全部---0409小版本要包含待审核或者审核后已撤销的处方
                else if (flag == all) {
                    hql.append("select r.* from cdr_recipe r where r.clinicOrgan in (:organ) and r.checkMode<2   and r.audit_state in (1,2,3,4,5,6,7)  and  (r.recipeType in(:recipeTypes) or r.grabOrderStatus=1) and r.reviewType != 0 ");
                    if (startTime != null && endTime != null) {
                        hql.append(" and r.CreateDate >= :startTime and r.CreateDate <= :endTime");
                    }
                    hql.append(" order by r.signDate desc");
                } else {
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "flag is invalid");
                }
                Query q;
                if (flag == all || flag == 0) {
                    q = ss.createSQLQuery(hql.toString()).addEntity(Recipe.class);
                } else {
                    q = ss.createQuery(hql.toString());
                }
                q.setParameterList("organ", organ);
                if (flag == 0 || flag == all) {
                    q.setParameterList("recipeTypes", recipeTypes);
                } else {
                    q.setParameter("checker", checker);
                }
                if (StringUtils.isNoneBlank(startTime) && StringUtils.isNoneBlank(endTime)) {
                    q.setParameter("startTime", startTime);
                    q.setParameter("endTime", endTime);
                }
                q.setFirstResult(start);
                q.setMaxResults(limit);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 查询药师审核的总数
     */
    public Long findRecipeAuditCountByFlag(final List<Integer> organ, List<Integer> recipeTypes, Integer checker, final int flag, String startTime, String endTime) {
        final int all = 3;
        HibernateStatelessResultAction<Long> action = new AbstractHibernateStatelessResultAction<Long>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                //0是待药师审核
                if (flag == 0) {
                    //hql.append("from Recipe where clinicOrgan in (:organ)  and checkMode<2 and status = " + RecipeStatusConstant.READY_CHECK_YS + " and  (recipeType in(:recipeTypes) or grabOrderStatus=1)");
                    hql.append("SELECT\n" +
                            "\tcount(r.recipeid)\n" +
                            "FROM\n" +
                            "\tcdr_recipe r\n" +
                            "LEFT JOIN cdr_recipe_ext cre ON r.recipeid = cre.recipeid\n" +
                            "WHERE cre.canUrgentAuditRecipe is not null and r.clinicOrgan in (:organ) and r.checkMode<2 and r.audit_state = 1 and  (recipeType in(:recipeTypes) or grabOrderStatus=1) ");
                }
                //1是审核通过
                else if (flag == 1) {
                    hql.append("select count(*) from cdr_recipe r where r.clinicOrgan in (:organ) and r.Checker = :checker and r.audit_state = 5 ");
                }
                //2是审核未通过
                else if (flag == 2) {
                    hql.append("select count(*) from cdr_recipe r where r.clinicOrgan in (:organ) and r.Checker = :checker and r.audit_state in (3,4,6) ");
                }

                //3是全部---0409小版本要包含待审核或者审核后已撤销的处方
                else if (flag == all) {
                    hql.append("select count(*) from cdr_recipe r where r.clinicOrgan in (:organ) and r.checkMode<2   and r.audit_state in (1,2,3,4,5,6,7)  and  (r.recipeType in(:recipeTypes) or r.grabOrderStatus=1) and r.reviewType != 0 ");
                } else {
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "flag is invalid");
                }

                if (StringUtils.isNoneBlank(startTime) && StringUtils.isNoneBlank(endTime)) {
                    hql.append(" and r.CreateDate >= :startTime and r.CreateDate <= :endTime");
                }

                Query q;
                /*if (flag == all || flag == 0) {

                } else {
                    q = ss.createQuery(hql.toString());
                }*/
                q = ss.createSQLQuery(hql.toString());
                q.setParameterList("organ", organ);
                if (flag == 0 || flag == all) {
                    q.setParameterList("recipeTypes", recipeTypes);
                } else {
                    q.setParameter("checker", checker);
                }
                if (StringUtils.isNoneBlank(startTime) && StringUtils.isNoneBlank(endTime)) {
                    q.setParameter("startTime", startTime);
                    q.setParameter("endTime", endTime);
                }
                BigInteger count = (BigInteger) q.uniqueResult();
                setResult(count.longValue());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public List<Recipe> findRecipeByMpiidAndrecipeStatus(final String mpiid, final List<Integer> recipeStatus, Integer terminalType,Integer organId) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "SELECT r FROM Recipe r ,RecipeExtend re WHERE r.recipeId=re.recipeId  and mpiid=:mpiid  ";
                if (terminalType!=null) {
                    hql += " and  re.terminalType= :terminalType";
                }
                if (organId!=null) {
                    hql += " and  r.clinicOrgan= :organId";
                }
                if(CollectionUtils.isNotEmpty(recipeStatus)){
                    hql += " and r.status IN (:recipeStatus)  order by createDate desc ";
                }
                Query query = ss.createQuery(hql);

                if (terminalType != null) {
                    query.setParameter("terminalType", terminalType);
                }
                if (organId != null) {
                    query.setParameter("organId", organId);
                }
                query.setParameter("mpiid", mpiid);
                if(CollectionUtils.isNotEmpty(recipeStatus)){
                    query.setParameterList("recipeStatus", recipeStatus);
                }

                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);

        List<Recipe> recipes = action.getResult();
        if(CollectionUtils.isNotEmpty(recipes)){
            LOGGER.info("size:{}",recipes.size());
        }
        return recipes;
    }

    public Integer countByPatientAndOrgan(String mpiid, Integer clinicOrgan){
        HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder sql = new StringBuilder("SELECT\n" +
                        "\tCOUNT( d.RecipeID ) \n" +
                        "FROM\n" +
                        "\tcdr_recipe d\n" +
                        "\n" +
                        "WHERE\n" +
                        "\t d.ClinicOrgan =:organId and d.MPIID =:mpiId\n" +
                        "\tAND d.STATUS = 2 \n" +
                        "\tAND d.orderCode IS NULL");

                Query q = ss.createSQLQuery(sql.toString());
                q.setParameter("organId", clinicOrgan);
                q.setParameter("mpiId", mpiid);

                if (q.uniqueResult() == null) {
                    setResult(0);
                } else {
                    Number number = (Number) q.uniqueResult();
                    setResult(number.intValue());
                }
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public Integer countByAutomaton(Recipe recipe, String startTime, String endTime, Integer terminalType,
                                    List<String> terminalIds, List<Integer> processState) {
        HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
            @Override
            public void execute(StatelessSession ss) {
                String sql = createHqlBySearch(recipe, terminalType, terminalIds, processState, startTime, endTime);
                Query query = ss.createSQLQuery("select count(1) " + sql);
                createQueryBySearch(query, recipe, terminalType, terminalIds, processState, startTime, endTime);
                setResult(null == query.uniqueResult() ? 0 : ((Number) query.uniqueResult()).intValue());
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    public List<Recipe> findAutomatonList(Recipe recipe, String startTime, String endTime, Integer terminalType,
                                          List<String> terminalIds, List<Integer> processState, Integer start, Integer limit) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) {
                String sql = createHqlBySearch(recipe, terminalType, terminalIds, processState, startTime, endTime);
                Query query = ss.createSQLQuery("select r.*" + sql).addEntity(Recipe.class);
                createQueryBySearch(query, recipe, terminalType, terminalIds, processState, startTime, endTime);
                query.setFirstResult(start);
                query.setMaxResults(limit);
                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    private String createHqlBySearch(Recipe recipe, Integer terminalType, List<String> terminalIds, List<Integer> processState, String startTime, String endTime) {
        StringBuilder hql = new StringBuilder("from cdr_recipe r INNER JOIN cdr_recipe_ext re ON r.RecipeID = re.recipeId  WHERE 1=1 ");
        if (null != recipe.getClinicOrgan()) {
            hql.append(" and r.ClinicOrgan = :clinicOrgan");
        }
        if (ValidateUtil.notNullAndZeroInteger(recipe.getRecipeId())) {
            hql.append(" and r.RecipeID = :recipeId");
        }
        if (null != recipe.getPayFlag()) {
            hql.append(" and r.PayFlag = :payFlag");
        }
        if (null != recipe.getMedicalFlag()) {
            hql.append(" and r.medical_flag = :medicalFlag");
        }
        if (ValidateUtil.notNullAndZeroInteger(terminalType)) {
            hql.append(" and re.terminal_type = :terminalType");
        }
        if (CollectionUtils.isNotEmpty(terminalIds)) {
            hql.append(" and re.terminal_id in(:terminalIds)");
        }
        if (CollectionUtils.isNotEmpty(processState)) {
            hql.append(" and r.process_state in(:processState)");
        }
        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)) {
            hql.append(" and r.createDate between :startTime and :endTime ");
        }
        hql.append(" order by r.createDate desc ");
        return hql.toString();
    }

    private void createQueryBySearch(Query query, Recipe recipe, Integer terminalType, List<String> terminalIds, List<Integer> processState, String startTime, String endTime) {
        if (null != recipe.getClinicOrgan()) {
            query.setParameter("clinicOrgan", recipe.getClinicOrgan());
        }
        if (ValidateUtil.notNullAndZeroInteger(recipe.getRecipeId())) {
            query.setParameter("recipeId", recipe.getRecipeId());
        }
        if (null != recipe.getPayFlag()) {
            query.setParameter("payFlag", recipe.getPayFlag());
        }
        if (null != recipe.getMedicalFlag()) {
            query.setParameter("medicalFlag", recipe.getMedicalFlag());
        }
        if (ValidateUtil.notNullAndZeroInteger(terminalType)) {
            query.setParameter("terminalType", terminalType);
        }
        if (CollectionUtils.isNotEmpty(terminalIds)) {
            query.setParameterList("terminalIds", terminalIds);
        }
        if (CollectionUtils.isNotEmpty(processState)) {
            query.setParameterList("processState", processState);
        }
        if (StringUtils.isNotEmpty(startTime)) {
            query.setParameter("startTime", startTime);
        }
        if (StringUtils.isNotEmpty(endTime)) {
            query.setParameter("endTime", endTime);
        }
    }

    public Integer getRecipeRefundCount(Integer doctorId,Date startTime,Date endTime){
        HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder str = new StringBuilder("select count(*)");
                StringBuilder hqlCount = generateRecipeRefundParameter(str, startTime, endTime);
                logger.info("getRecipeRefundInfo hqlCount={}",JSONUtils.toString(hqlCount));
                Query queryCount = ss.createSQLQuery(hqlCount.toString());
                queryCount.setParameter("doctorId", doctorId);
                if (startTime != null && endTime != null) {
                    queryCount.setParameter("startTime",startTime);
                    queryCount.setParameter("endTime",endTime);
                }
                setResult(null == queryCount.uniqueResult() ? 0 : ((Number) queryCount.uniqueResult()).intValue());
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    public List<RecipeRefundDTO> getRecipeRefundInfo(Integer doctorId,Date startTime,Date endTime,Integer start,Integer limit){
        HibernateStatelessResultAction<List<RecipeRefundDTO>> action = new AbstractHibernateStatelessResultAction<List<RecipeRefundDTO>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder str = new StringBuilder("select r.recipeID,r.patientName,r.RecipeType,r.CreateDate,o.cancelReason as reason");
                StringBuilder hql = generateRecipeRefundParameter(str, startTime, endTime);
                hql.append(" order by r.CreateDate desc");
                logger.info("getRecipeRefundInfo hql={}",JSONUtils.toString(hql));
                Query query = ss.createSQLQuery(hql.toString()).addEntity(RecipeRefundDTO.class);
                query.setParameter("doctorId", doctorId);
                if (startTime != null && endTime != null) {
                    query.setParameter("startTime",startTime);
                    query.setParameter("endTime",endTime);
                }
                query.setFirstResult(start);
                query.setMaxResults(limit);
                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    private StringBuilder generateRecipeRefundParameter(StringBuilder hql,Date startTime, Date endTime){
        String str = " from cdr_recipe r left join cdr_recipeorder o on r.orderCode = o.orderCode " +
                " where r.doctor =:doctorId and o.payFlag = 3 ";
        hql.append(str);
        if (startTime != null && endTime != null) {
            hql.append("and r.CreateDate between :startTime and :endTime");
        }
        return hql;
    }

    public List<Recipe> getFastRecipeListForSignFail(String startDt, String endDt) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder sql = new StringBuilder("select * from cdr_recipe where status in (30, 26, 31, 27, 32) and fast_recipe_flag = 1 and signDate between '" + startDt + "' and '" + endDt + "' ");
                Query q = ss.createSQLQuery(sql.toString()).addEntity(Recipe.class);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod(sql = "from Recipe where mpiid =:mpiId and clinicOrgan=:organId and writeHisState != 3 and processState=3 and recipeSourceType=1")
    public abstract List<Recipe> findByOrganIdAndMpiId(@DAOParam("organId") Integer organId, @DAOParam("mpiId") String mpiId);

    @DAOMethod(sql = "update Recipe set pushFlag = 1 where clinicOrgan=:organId and pushFlag = 0 and enterpriseId=:depId and signDate between :startDt and :endDt ", limit = 0)
    public abstract void updateRecipeByOrganIdAndPushFlag(@DAOParam("organId") Integer organId, @DAOParam("depId") Integer depId,
                                                          @DAOParam("startDt") Date startDt, @DAOParam("endDt") Date endDt);

    public  Integer getRecipeCountForAutomaton(AutomatonVO param){
        HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
            @Override
            public void execute(StatelessSession ss) {
                String sql = createHqlForAutomaton( param.getTerminalType(), param.getTerminalIds(), param.getProcessStateList(), param.getStartTime(), param.getEndTime());
                Query query = ss.createSQLQuery("select count(1) " + sql);
                createQueryBySearch(query, new Recipe(), param.getTerminalType(), param.getTerminalIds(), param.getProcessStateList(), param.getStartTime(), param.getEndTime());
                setResult(null == query.uniqueResult() ? 0 : ((Number) query.uniqueResult()).intValue());
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

    private String createHqlForAutomaton(Integer terminalType, List<String> terminalIds, List<Integer> processState, String startTime, String endTime) {
        StringBuilder hql = new StringBuilder(" from cdr_recipe r INNER JOIN cdr_recipe_ext re ON r.RecipeID = re.recipeId  WHERE 1=1 ");
        if (terminalType!=null) {
            hql.append(" and  re.terminal_type= :terminalType");
        }
        if (CollectionUtils.isNotEmpty(terminalIds)) {
            hql.append(" and re.terminal_id in(:terminalIds)");
        }
        if (CollectionUtils.isNotEmpty(processState)) {
            hql.append(" and r.process_state in(:processState)");
        }

        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)) {
            hql.append(" and date(createDate)  between :startTime and :endTime   ");
        }
        return hql.toString();
    }


    public  List<AutomatonCountVO> findRecipeTop5ForAutomaton(AutomatonVO param){
        HibernateStatelessResultAction<List<AutomatonCountVO>> action = new AbstractHibernateStatelessResultAction<List<AutomatonCountVO>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String sql = createHqlForAutomaton( param.getTerminalType(), param.getTerminalIds(), param.getProcessStateList(), param.getStartTime(), param.getEndTime());
                Query query = ss.createSQLQuery("select clinicOrgan, count(1) " + sql+" group by r.clinicOrgan order by count(1) desc limit 5");
                createQueryBySearch(query, new Recipe(), param.getTerminalType(), param.getTerminalIds(), param.getProcessStateList(), param.getStartTime(), param.getEndTime());
                List<Object[]> oList = query.list();
                List<AutomatonCountVO> automatonCountVOS=new ArrayList<>();
                for (Object[] co : oList) {
                    AutomatonCountVO automatonCountVO=new AutomatonCountVO();
                    automatonCountVO.setOrganId(null==co[0]?null:Integer.valueOf(String.valueOf(co[0]) ));
                    automatonCountVO.setCount(null==co[1]?0:Integer.valueOf(String.valueOf(co[1]) ));
                    automatonCountVOS.add(automatonCountVO);
                }
                setResult(automatonCountVOS);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public  HashMap<String, Integer> findRecipeEveryDayForAutomaton(AutomatonVO param){
        HibernateStatelessResultAction<HashMap<String, Integer>> action = new AbstractHibernateStatelessResultAction<HashMap<String, Integer>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                //select date(createDate) time ,count(1) count from cdr_recipe where date(createDate)  between '2022-11-28' and '2022-12-02'  group by date(createDate)  order by createdate desc
                String sql = createHqlForAutomaton( param.getTerminalType(), param.getTerminalIds(), param.getProcessStateList(), param.getStartTime(), param.getEndTime());
                Query query = ss.createSQLQuery("select date(createDate) time ,count(1) count  " + sql+"  group by date(createDate)  order by createdate desc ");
                createQueryBySearch(query, new Recipe(), param.getTerminalType(), param.getTerminalIds(), param.getProcessStateList(), param.getStartTime(), param.getEndTime());
                List<Object[]> oList = query.list();
                HashMap<String, Integer> timeCount = new HashMap<String, Integer>();
                SimpleDateFormat sformat = new SimpleDateFormat(DateConversion.YYYY_MM_DD);
                for (Object[] co : oList) {
                    timeCount.put( null== co[0] ? null: sformat.format(co[0]), null==co[1]?0:Integer.valueOf(String.valueOf(co[1]) ));
                }
                setResult(timeCount);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public QueryResult<Recipe> queryPageForCommonOrder(Date startDate, Date endDate, Integer start, Integer limit) {
        HibernateStatelessResultAction<QueryResult<Recipe>> action = new AbstractHibernateStatelessResultAction<QueryResult<Recipe>>() {
            @Override
            @SuppressWarnings("unchecked")
            public void execute(StatelessSession ss) throws DAOException {
                int total = 0;
                StringBuilder hql = new StringBuilder(" from Recipe");
                if (startDate != null) {

                    hql.append(" where signDate >= :startTime ");
                }
                if (endDate != null) {
                    hql.append(" AND signDate <= :endTime ");
                }
                Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                countQuery.setParameter("endTime", endDate);
                countQuery.setParameter("startTime", startDate);
                total = ((Long) countQuery.uniqueResult()).intValue();// 获取总条数

                hql.append(" order by recipeId desc");
                Query query = ss.createQuery(hql.toString());
                query.setParameter("endTime", endDate);
                query.setParameter("startTime", startDate);
                query.setFirstResult(start);
                query.setMaxResults(limit);
                List<Recipe> resList = query.list();
                QueryResult<Recipe> qResult = new QueryResult<Recipe>(total, query.getFirstResult(),
                        query.getMaxResults(), resList);
                setResult(qResult);
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        QueryResult<Recipe> result = action.getResult();
        return result;

    }

    @DAOMethod(sql = "FROM Recipe where clinicOrgan IN :organIds AND auditState = 1 AND createDate > :startTime and createDate < :endTime")
    public abstract List<Recipe> findAuditOverTimeRecipeList(@DAOParam("startTime") Date startTime,
                                                             @DAOParam("endTime") Date endTime,
                                                             @DAOParam("organIds") List<Integer> organIds);

    public List<Recipe> findDoctorRecipeListV1(List<Integer> doctorIds, DoctorRecipeListReqDTO doctorRecipeListReqDTO) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder sql = new StringBuilder("select * from cdr_recipe where doctor in :doctorId and clinicOrgan=:organId and recipeSourceType =:recipeSourceType and process_state = 1 ");
                String keyWord = doctorRecipeListReqDTO.getKeyWord();
                Integer recipeType = doctorRecipeListReqDTO.getRecipeDrugType();
                Integer recipeDrugForm = doctorRecipeListReqDTO.getRecipeDrugForm();
                if (StringUtils.isNotEmpty(keyWord)) {
                    sql.append(" and ( offline_recipe_name like :keyWord ");
                    if (Objects.nonNull(recipeType)) {
                        sql.append(" or recipeType=:recipeType ");
                    }
                    if (Objects.nonNull(recipeDrugForm)) {
                        sql.append(" or recipe_drug_form=:recipeDrugForm ");
                    }
                    sql.append(") ");
                }
                sql.append(" order by recipeId desc ");
                Query q = ss.createSQLQuery(sql.toString()).addEntity(Recipe.class);
                q.setParameterList("doctorId", doctorIds);
                q.setParameter("organId", doctorRecipeListReqDTO.getOrganId());
                q.setParameter("recipeSourceType", doctorRecipeListReqDTO.getRecipeType());
                if (StringUtils.isNotEmpty(keyWord)) {
                    q.setParameter("keyWord", "%" + keyWord + "%");
                }
                if (Objects.nonNull(recipeType)) {
                    q.setParameter("recipeType", recipeType);
                }
                if (Objects.nonNull(recipeDrugForm)) {
                    q.setParameter("recipeDrugForm", recipeDrugForm);
                }
                q.setFirstResult(doctorRecipeListReqDTO.getStart());
                q.setMaxResults(doctorRecipeListReqDTO.getLimit());
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod(sql = "FROM Recipe where clinicOrgan = :organId AND status = 0 AND clinicId = :clinicId")
    public abstract List<Recipe> findTempRecipeByClinicId(@DAOParam("organId") Integer organId,
                                                          @DAOParam("clinicId") Integer clinicId);

    public List<Recipe> getByChargeIdAndOrganId(String recipeCode, Integer organId){
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select r.*  from cdr_recipe r ");
                hql.append(" LEFT JOIN cdr_recipe_ext re ON r.RecipeID = re.recipeId ");
                hql.append(" where r.clinicOrgan = :organId ");
                hql.append(" and  re.charge_id = :recipeCode ");
                hql.append(" and (r.delete_flag = 0 or r.delete_flag  is null)");
                Query q = ss.createSQLQuery(hql.toString()).addEntity(Recipe.class);
                q.setParameter("organId", organId);
                q.setParameter("recipeCode", recipeCode);

                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    };

    public  List<Recipe> getByRecipeCodeLikeAndPayFlag(@DAOParam("recipeCode")String recipeCode, @DAOParam("organId")Integer organId){
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder sql = new StringBuilder("select * from cdr_recipe where clinicOrgan=:organId and PayFlag =0 ");
                sql.append(" and recipeCode like :recipeCode ");
                Query q = ss.createSQLQuery(sql.toString()).addEntity(Recipe.class);
                q.setParameter("organId", organId);
                q.setParameter("recipeCode", "%" + recipeCode + "%");
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    };

    @DAOMethod(sql = "from Recipe where orderCode is not null and fastRecipeFlag = 1 and offlineRecipeName is null ", limit = 0)
    public abstract List<Recipe> findFastRecipeList();

    public List<Recipe> getByChargeItemCodeAndOrganId(String recipeCode, Integer organId){
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select r.*  from cdr_recipe r ");
                hql.append(" LEFT JOIN cdr_recipe_ext re ON r.RecipeID = re.recipeId ");
                hql.append(" where r.clinicOrgan = :organId ");
                hql.append(" and  re.charge_item_code like :recipeCode ");
                hql.append(" and (r.delete_flag = 0 or r.delete_flag  is null)");
                Query q = ss.createSQLQuery(hql.toString()).addEntity(Recipe.class);
                q.setParameter("organId", organId);
                q.setParameter("recipeCode", "%" + recipeCode + "%");

                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    };

    public List<Recipe> findPatientRecipeList(PatientRecipeListReqDTO req){
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("SELECT * FROM cdr_recipe WHERE ClinicOrgan =  :organId ");
                hql.append(" AND MPIID = :mpiId  ");
                hql.append(" AND bussSource in (:bussSource)  ");
                hql.append(" AND SignDate BETWEEN :startTime AND :endTime ");
                hql.append(" AND process_state IN ( :recipeState)  ");
                hql.append(" AND recipeSourceType IN ( 1,2)  ");
                hql.append(" and (delete_flag = 0 or delete_flag  is null)");
                Query q = ss.createSQLQuery(hql.toString()).addEntity(Recipe.class);
                q.setParameter("organId", req.getOrganId());
                q.setParameter("mpiId", req.getMpiId());
                q.setParameter("startTime", req.getStartTime());
                q.setParameter("endTime", req.getEndTime());
                q.setParameterList("recipeState", req.getRecipeState());
                q.setParameterList("bussSource", req.getBussSource());

                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    };


}


