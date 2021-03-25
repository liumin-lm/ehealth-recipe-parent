package recipe.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.common.dto.DepartChargeReportResult;
import com.ngari.common.dto.HosBusFundsReportResult;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.entity.*;
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
import eh.recipeaudit.api.IRecipeCheckService;
import eh.recipeaudit.model.RecipeCheckBean;
import eh.recipeaudit.util.RecipeAuditAPI;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.StatelessSession;
import org.hibernate.type.LongType;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.constant.*;
import recipe.dao.bean.PatientRecipeBean;
import recipe.dao.bean.RecipeRollingInfo;
import recipe.dao.comment.ExtendDao;
import recipe.util.DateConversion;
import recipe.util.LocalStringUtil;
import recipe.util.SqlOperInfo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * 根据订单编号获取处方id集合
     *
     * @param orderCode
     * @return
     */
    @DAOMethod(sql = "select recipeId from Recipe where orderCode=:orderCode")
    public abstract List<Integer> findRecipeIdsByOrderCode(@DAOParam("orderCode") String orderCode);

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
    public abstract List<Recipe> findByRecipeIds(@DAOParam("recipeIds") List<Integer> recipeIds);

    /**
     * 查询所有处方
     *
     * @param recipeCode
     * @param clinicOrgan
     * @return
     */
    @DAOMethod(sql = "from Recipe where recipeCode=:recipeCode and clinicOrgan=:clinicOrgan")
    public abstract Recipe getByRecipeCodeAndClinicOrganWithAll(@DAOParam("recipeCode") String recipeCode, @DAOParam("clinicOrgan") Integer clinicOrgan);

    @DAOMethod(sql = "from Recipe where recipeCode in (:recipeCodeList) and clinicOrgan=:clinicOrgan")
    public abstract List<Recipe> findByRecipeCodeAndClinicOrgan(@DAOParam("recipeCodeList") List<String> recipeCodeList, @DAOParam("clinicOrgan") Integer clinicOrgan);

    @DAOMethod(sql = "select COUNT(*) from Recipe where  clinicOrgan=:organId and  PayFlag =:payFlag and status in (2,8)")
    public abstract Long getUnfinishedRecipe(@DAOParam("organId") Integer organId, @DAOParam("payFlag") Integer payFlag);

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
     * 根据订单编号更新订单编号为空
     *
     * @param orderCode
     */
    @DAOMethod(sql = "update Recipe set orderCode=null where orderCode=:orderCode")
    public abstract void updateOrderCodeToNullByOrderCode(@DAOParam("orderCode") String orderCode);

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
                Query q = ss.createQuery("from Recipe where Status=:status and ClinicID=:clinicId order by RecipeID desc");
                q.setParameter("clinicId", clinicId);
                q.setParameter("status", status);
                q.setMaxResults(1);
                setResult((Recipe) q.uniqueResult());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    ;

    /**
     * 根据订单编号更新订单编号为空
     *
     * @param orderCode
     */
    public void updateOrderCodeToNullByOrderCodeAndClearChoose(String orderCode, Recipe recipe) {
        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update Recipe set ");
                //非北京互联网模式设置为null
                if (!new Integer(2).equals(recipe.getRecipeSource())) {
                    hql.append(" giveMode = null, ");
                }
                hql.append(" orderCode=null ,chooseFlag=0, status = 2 where orderCode=:orderCode");
                Query q = ss.createQuery(hql.toString());

                q.setParameter("orderCode", orderCode);
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
                hql.append("r.depart from cdr_recipe r left join cdr_recipeorder o on r.orderCode=o.orderCode where o.status=5 and r.clinicOrgan=:organId");
                if (depart != null) {
                    hql.append(" and r.depart =:depart");
                }
                hql.append(" and (r.signDate between :start and :end) GROUP BY r.depart ");
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
    public List<HosBusFundsReportResult> findRecipeByOrganIdAndCreateTime(Integer organId, Date createTime, Date endTime) {
        final String start = DateConversion.getDateFormatter(createTime, DateConversion.DEFAULT_DATE_TIME);
        final String end = DateConversion.getDateFormatter(endTime, DateConversion.DEFAULT_DATE_TIME);
        AbstractHibernateStatelessResultAction<List<HosBusFundsReportResult>> action = new AbstractHibernateStatelessResultAction<List<HosBusFundsReportResult>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select IFNULL(sum(o.cashAmount),0),IFNULL(sum(o.fundAmount),0) from cdr_recipe r left join cdr_recipeorder o");
                hql.append(" on r.orderCode=o.orderCode where o.status=5 and r.clinicOrgan=" + organId);
                hql.append(" and (r.signDate between '" + start + "' and  '" + end + "')");
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
                if (recipedetails != null) {
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
     * 修改医院确认中的处方状态
     *
     * @param recipeIds
     */
    @DAOMethod(sql = "update Recipe set status = :status where recipeId = :recipeId and status = :beforStatus")
    public abstract void updateStatusByRecipeIdAndStatus(@DAOParam("status") Integer status, @DAOParam("recipeIds") Integer recipeIds, @DAOParam("beforStatus") Integer beforStatus);

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
                    hql.append(" and fromflag = 1 and status =" + RecipeStatusConstant.CHECK_PASS + " and giveMode is null or ( status in (8,24) and reviewType = 1 and signDate between '" + startDt + "' and '" + endDt + "' )");
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
                StringBuilder hql = new StringBuilder("from Recipe where (status in (8,24) and reviewType = 1 and invalidTime is not null and invalidTime between '" + startDt + "' and '" + endDt + "') " +
                        " or (fromflag in (1,2) and status =" + RecipeStatusConstant.CHECK_PASS + " and payFlag=0 and payMode is not null and orderCode is not null and invalidTime is not null and invalidTime between '" + startDt + "' and '" + endDt + "') " +
                        " or (fromflag = 1 and payMode is null and status = 2 and invalidTime is not null and invalidTime between '" + startDt + "' and '" + endDt + "') ");
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
    public abstract Long getRecipeCountByMpi(@DAOParam("fromFlag") Integer fromFlag, @DAOParam("mpiId") String mpiId, @DAOParam("clinicOrgan") Integer clinicOrgan, @DAOParam("recipeType") Integer recipeType, @DAOParam("recipeCode") String recipeCode);

    /**
     * 判断来自his的处方单是否存在
     *
     * @param recipe 处方Object
     * @return
     */
    public Boolean mpiExistRecipeByMpiAndFromFlag(Recipe recipe) {
        return this.getRecipeCountByMpi(0, recipe.getMpiid(), recipe.getClinicOrgan(), recipe.getRecipeType(), recipe.getRecipeCode()) > 0;
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
                hql.append("select r from Recipe r, RecipeOrder o where r.orderCode=o.orderCode " + " and r.checkDateYs between '" + startDt + "' and '" + endDt + "' and r.status=" + RecipeStatusConstant.CHECK_NOT_PASS_YS + " and o.effective=1 ");
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
                hql.append("select r from Recipe r where r.giveMode=" + RecipeBussConstant.GIVEMODE_SEND_TO_HOME + " and r.sendDate between '" + startDt + "' and '" + endDt + "' and r.status=" + RecipeStatusConstant.IN_SEND);
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
    public QueryResult<Map> findRecipesByInfo(final Integer organId, final Integer status, final Integer doctor, final String patientName, final Date bDate, final Date eDate, final Integer dateType, final Integer depart, final int start, final int limit, List<Integer> organIds, Integer giveMode, Integer sendType, Integer fromflag, Integer recipeId, Integer enterpriseId, Integer checkStatus, Integer payFlag, Integer orderType, Integer refundNodeStatus) {
        this.validateOptionForStatistics(status, doctor, patientName, bDate, eDate, dateType, start, limit);
        final StringBuilder sbHql = this.generateRecipeOderHQLforStatistics(organId, status, doctor, patientName, dateType, depart, organIds, giveMode, sendType, fromflag, recipeId, enterpriseId, checkStatus, payFlag, orderType, refundNodeStatus);
        logger.info("findRecipesByInfo sbHql:{}", sbHql.toString());
        final PatientService patientService = BasicAPI.getService(PatientService.class);
        HibernateStatelessResultAction<QueryResult<Map>> action = new AbstractHibernateStatelessResultAction<QueryResult<Map>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                // 查询总记录数
                SQLQuery sqlQuery = ss.createSQLQuery("SELECT count(*) AS count FROM (" + sbHql + ") k").addScalar("count", LongType.INSTANCE);
                sqlQuery.setParameter("startTime", sdf.format(bDate));
                sqlQuery.setParameter("endTime", sdf.format(eDate));
                Long total = (Long) sqlQuery.uniqueResult();

                // 查询结果
                Query query = ss.createSQLQuery(sbHql.append(" order by recipeId DESC").toString()).addEntity(Recipe.class);
                query.setParameter("startTime", sdf.format(bDate));
                query.setParameter("endTime", sdf.format(eDate));
                query.setFirstResult(start);
                query.setMaxResults(limit);
                List<Recipe> recipeList = query.list();

                List<Map> maps = new ArrayList<Map>();
                if (recipeList != null) {
                    RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
                    DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                    for (Recipe recipe : recipeList) {
                        Map<String, Object> map = Maps.newHashMap();
                        PatientDTO patientBean;
                        try {
                            patientBean = patientService.get(recipe.getMpiid());
                        } catch (Exception e) {
                            patientBean = new PatientDTO();
                        }
                        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
                        RecipeOrder order = recipeOrderDAO.getOrderByRecipeIdQuery(recipe.getRecipeId());
                        if (order == null) {
                            order = new RecipeOrder();
                            //跟前端约定好这个字段一定会给的，所以定义了-1作为无支付类型
                            order.setOrderType(-1);
                            order.setPayFlag(recipe.getPayFlag());
                        }
                        if (order != null && order.getOrderType() == null) {
                            order.setOrderType(0);
                            recipe.setPayFlag(order.getPayFlag());
                        }
                        //处方审核状态处理
                        Integer checkStatus2 = getCheckResultByPending(recipe);
                        recipe.setCheckStatus(checkStatus2);
                        BeanUtils.map(recipe, map);
                        //map.putAll(JSONObject.parseObject(JSON.toJSONString(recipe)));

                        map.put("recipeOrder", order);
                        map.put("detailCount", recipeDetailDAO.getCountByRecipeId(recipe.getRecipeId()));
                        Integer enterpriseId = recipe.getEnterpriseId();
                        if (enterpriseId != null) {
                            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(enterpriseId);
                            if (null != drugsEnterprise) {
                                map.put("drugsEnterprise", drugsEnterprise.getName());
                            }
                        }
                        if (null != order) {
                            map.put("payDate", order.getPayTime());
                        } else {
                            map.put("payDate", null);
                        }
                        map.put("patient", patientBean);

                        // 处方退费状态
                        // 经过表结构讨论，当前不做大修改，因此将退费状态字段RefundNodeStatus放在了RecipeExtend表
                        RecipeExtend recipeExtend = getRecipeRefundNodeStatus(recipe);

                        // 注意：若不使用BeanUtils.map转换，而直接放对象，
                        // 会造成opbase QueryResult<Map>端枚举值的Text字段不会自动生成
                        // 还有一种方式是在opbase QueryResult<Map>端循环读取然后重新设置（张宪强的回答）
                        if (recipeExtend != null) {
                            Map<String, Object> recipeExtendMap = Maps.newHashMap();
                            BeanUtils.map(recipeExtend, recipeExtendMap);
                            map.put("recipeExtend", recipeExtendMap);
                        }

                        maps.add(map);
                    }
                }
                logger.info("findRecipesByInfo maps:{}", JSONUtils.toString(maps));
                setResult(new QueryResult<Map>(total, query.getFirstResult(), query.getMaxResults(), maps));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    // 改为批量查询提高效率
    private RecipeExtend getRecipeRefundNodeStatus(Recipe recipe) {
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        return recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
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
//    public List<Recipe> findRecipeByFlag(final List<Integer> organ, final int flag, final int start, final int limit) {
//        final int notPass = 2;
//        final int all = 3;
//        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
//            @Override
//            public void execute(StatelessSession ss) throws Exception {
//                StringBuilder hql = new StringBuilder();
//                //0是待药师审核
//                if (flag == 0) {
//                    hql.append("from Recipe where clinicOrgan in (:organ)  and checkMode<3 and status = " + RecipeStatusConstant.READY_CHECK_YS);
//                }
//                //1是审核通过
//                else if (flag == 1) {
//                    hql.append("select distinct r from Recipe r,RecipeCheck rc where r.recipeId = rc.recipeId  and r.checkMode<3  and r.clinicOrgan in (:organ)" +
//                            "and (rc.checkStatus = 1 or (rc.checkStatus=0 and r.supplementaryMemo is not null)) and r.status not in (9,31)");
//                }
//                //2是审核未通过
//                else if (flag == notPass) {
//                    hql.append("select distinct r from Recipe r,RecipeCheck rc where r.recipeId = rc.recipeId  and r.checkMode<3  and r.clinicOrgan in (:organ)" +
//                            "and rc.checkStatus = 0 and rc.checker is not null and r.supplementaryMemo is null and r.status not in (9,31)");
//                }
//                //3是全部---0409小版本要包含待审核或者审核后已撤销的处方
//                else if (flag == all) {
//                    hql.append("select r.* from cdr_recipe r where r.clinicOrgan in (:organ) and r.checkMode<3   and (r.status in (8,31) or r.checkDateYs is not null or (r.status = 9 and (select l.beforeStatus from cdr_recipe_log l where l.recipeId = r.recipeId and l.afterStatus =9 ORDER BY l.Id desc limit 1) in (8,15,7,2))) ");
//                } else {
//                    throw new DAOException(ErrorCode.SERVICE_ERROR, "flag is invalid");
//                }
//                hql.append("order by signDate desc");
//                Query q;
//                if (flag == all) {
//                    q = ss.createSQLQuery(hql.toString()).addEntity(Recipe.class);
//                } else {
//                    q = ss.createQuery(hql.toString());
//                }
//                q.setParameterList("organ", organ);
//                q.setFirstResult(start);
//                q.setMaxResults(limit);
//                setResult(q.list());
//            }
//        };
//        HibernateSessionTemplate.instance().execute(action);
//        return action.getResult();
//    }
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
                            "WHERE cre.canUrgentAuditRecipe is not null and r.clinicOrgan in (:organ) and r.checkMode<2 and r.status = 8 and  (recipeType in(:recipeTypes) or grabOrderStatus=1) " +
                            "ORDER BY canUrgentAuditRecipe desc, signdate asc");
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
                    hql.append("select r.* from cdr_recipe r where r.clinicOrgan in (:organ) and r.checkMode<2   and (r.status in (8,27,31) or r.checkDateYs is not null or (r.status = 9 and (select l.beforeStatus from cdr_recipe_log l where l.recipeId = r.recipeId and l.afterStatus =9 ORDER BY l.Id desc limit 1) in (8,15,7,2)))  and  (recipeType in(:recipeTypes) or grabOrderStatus=1) order by signDate desc");
                } else {
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "flag is invalid");
                }
                /*if (flag == 0 || flag == all) {
                        hql.append(" and  (recipeType in(:recipeTypes) or grabOrderStatus=1) ");
                }*/
                //hql.append("order by signDate desc");
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
     * @param dateType    时间类型（0：开方时间，1：审核时间）
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
                System.out.println(preparedHql);
                Query query = ss.createSQLQuery(sbHql.append(" order by r.recipeId DESC").toString()).addEntity(RecipeExportDTO.class).addEntity(RecipeOrderExportDTO.class).addEntity(RecipeDetailExportDTO.class);
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
     * @param dateType    时间类型（0：开方时间，1：审核时间）
     * @return QueryResult<Map>
     */
    public List<Object[]> findRecipesByInfoForExcel(RecipesQueryVO recipesQueryVO) {
        this.validateOptionForStatistics(recipesQueryVO);
        final StringBuilder sbHql = this.generateRecipeMsgHQLforStatistics(recipesQueryVO);
        HibernateStatelessResultAction<List<Object[]>> action = new AbstractHibernateStatelessResultAction<List<Object[]>>() {
            @Override
            public void execute(StatelessSession ss) {
                LOGGER.info("RecipeDAO findRecipesByInfoForExcel sbHql = {} ", sbHql);
                Query query = ss.createSQLQuery(sbHql.append(" GROUP BY r.recipeId order by r.recipeId DESC").toString()).addEntity(RecipeInfoExportDTO.class);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                query.setParameter("startTime", sdf.format(recipesQueryVO.getBDate()));
                query.setParameter("endTime", sdf.format(recipesQueryVO.getEDate()));
                List<Object[]> list = query.list();
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
     * @return HashMap<String                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               ,                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               Integer>
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

    private StringBuilder generateRecipeOderHQLforStatistics(Integer organId, Integer status, Integer doctor, String mpiId, Integer dateType, Integer depart, final List<Integer> requestOrgans, Integer giveMode, Integer sendType, Integer fromflag, Integer recipeId, Integer enterpriseId, Integer checkStatus, Integer payFlag, Integer orderType, Integer refundNodeStatus) {
//        StringBuilder hql = new StringBuilder("select r.* from cdr_recipe r LEFT JOIN cdr_recipeorder o on r.orderCode=o.orderCode LEFT JOIN cdr_recipecheck c ON r.recipeID=c.recipeId where 1=1");
        StringBuilder hql = new StringBuilder("select r.*  from cdr_recipe r ");
        hql.append(" LEFT JOIN cdr_recipeorder o on r.orderCode = o.orderCode ");
        hql.append(" LEFT JOIN cdr_recipe_ext re ON r.RecipeID = re.recipeId ");
        hql.append(" where 1=1");
        //new StringBuilder("select r.recipeId,o.orderCode from cdr_recipe r LEFT JOIN cdr_recipeorder o on r.orderCode=o.orderCode LEFT JOIN cdr_recipecheck c ON r.recipeID=c.recipeId where 1=1 ");

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
//            case 0:
//                //开方时间
//                hql.append(" and DATE_FORMAT(r.createDate,'yyyy-MM-dd HH:mm:ss') >= DATE_FORMAT(:startTime,'yyyy-MM-dd HH:mm:ss')"
//                        + " and DATE_FORMAT(r.createDate,'yyyy-MM-dd HH:mm:ss') <= DATE_FORMAT(:endTime,'yyyy-MM-dd HH:mm:ss') ");
//                break;
//            case 1:
//                //审核时间
//                hql.append(" and DATE_FORMAT(r.checkDate,'yyyy-MM-dd HH:mm:ss') >= DATE_FORMAT(:startTime,'yyyy-MM-dd HH:mm:ss')"
//                        + " and DATE_FORMAT(r.checkDate,'yyyy-MM-dd HH:mm:ss') <= DATE_FORMAT(:endTime,'yyyy-MM-dd HH:mm:ss') ");
//                break;
            case 0:
                //开方时间
                hql.append(" and r.createDate BETWEEN :startTime" + " and :endTime ");
                break;
            case 1:
                //审核时间
                hql.append(" and r.CheckDateYs BETWEEN :startTime" + " and :endTime ");
                break;
            case 2:
                //审核时间
                hql.append(" and o.payTime BETWEEN :startTime" + " and :endTime ");
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
//        if (patientName != null && !StringUtils.isEmpty(patientName.trim())) {
//            hql.append(" and r.patientName='").append(patientName).append("'");
//        }
        if (depart != null) {
            hql.append(" and r.depart=").append(depart);
        }
        if (giveMode != null) {
            hql.append(" and r.giveMode=").append(giveMode);
        }
        if (null != sendType) {
            hql.append(" and o.send_type=").append(sendType);
        }

        if (fromflag != null) {
            hql.append(" and r.fromflag=").append(fromflag);
        }
        if (recipeId != null) {
            hql.append(" and r.recipeId=").append(recipeId);
        }

        if (mpiId != null) {
            hql.append(" and r.mpiid='").append(mpiId + "'");
        }
        if (enterpriseId != null) {
            hql.append(" and r.enterpriseId=").append(enterpriseId);
        }

        if (checkStatus != null) {
//            checkResult 0:未审核 1:通过 2:不通过 3:二次签名 4:失效
            switch (checkStatus) {
                case 0:
                    hql.append(" and r.status =").append(8);
                    break;
                case 1:
//                    hql.append(" and r.status =").append(1);
                    hql.append(" and r.status=2");
                    break;
                case 2:
//                    hql.append(" and r.checkStatus =").append(0).append(" and r.checker is not null ");
                    hql.append(" and r.status=15");
                    break;
                case 3:
                    hql.append(" and r.supplementaryMemo is not null ");
                    break;
                case 4:
                    hql.append(" and r.status = ").append(9);
                    break;
            }
        }

        if (payFlag != null) {
            if (payFlag == 0) {
                hql.append(" and r.payFlag=").append(payFlag);
            } else {
                hql.append(" and o.payFlag=").append(payFlag);
            }
        }
        if (orderType != null) {
            if (orderType == 0) {
                hql.append(" and o.orderType=").append(0);
            } else {
                hql.append(" and o.orderType in (1,2,3,4) ");
            }
        }
        if (refundNodeStatus != null) {
            hql.append(" and re.refundNodeStatus=").append(refundNodeStatus);
        }
        return hql;
    }

    private StringBuilder generateRecipeMsgHQLforStatistics(RecipesQueryVO recipesQueryVO) {
//        StringBuilder hql = new StringBuilder("select r.recipeId,r.patientName,r.Mpiid,r.organName,r.depart,r.doctor,r.organDiseaseName,r.totalMoney,r.checker,r.checkDateYs,r.fromflag,r.status,o.payTime, r.doctorName, sum(cr.useTotalDose) sumDose ,o.send_type sendType ,o.outTradeNo  from cdr_recipe r LEFT JOIN cdr_recipeorder o on r.orderCode=o.orderCode LEFT JOIN cdr_recipecheck c ON r.recipeID=c.recipeId left join cdr_recipedetail cr on cr.recipeId = r.recipeId and cr.status =1  where 1=1 ");
        StringBuilder hql = new StringBuilder("select r.recipeId,r.patientName,r.Mpiid mpiId,r.organName,r.depart,r.doctor,r.organDiseaseName,r.totalMoney,r.checker,r.checkDateYs,r.fromflag,r.status,o.payTime, r.doctorName, sum(cr.useTotalDose) sumDose ,o.send_type sendType ,o.outTradeNo ,o.cashAmount,o.fundAmount,o.orderType from cdr_recipe r LEFT JOIN cdr_recipeorder o on r.orderCode=o.orderCode left join cdr_recipedetail cr on cr.recipeId = r.recipeId and cr.status =1  where 1=1 ");
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
                //审核时间
                hql.append(" and o.payTime BETWEEN :startTime" + " and :endTime ");
                break;
            default:
                break;
        }
        if (null != recipesQueryVO.getStatus()) {
            hql.append(" and r.status =").append(recipesQueryVO.getStatus());
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
        if (null != recipesQueryVO.getFromFlag()) {
            hql.append(" and r.fromflag=").append(recipesQueryVO.getFromFlag());
        }
        if (null != recipesQueryVO.getRecipeId()) {
            hql.append(" and r.recipeId=").append(recipesQueryVO.getRecipeId());
        }

        if (StringUtils.isNotBlank(recipesQueryVO.getPatientName())) {
            hql.append(" and r.mpiid='").append(recipesQueryVO.getPatientName() + "'");
        }
        if (null != recipesQueryVO.getEnterpriseId()) {
            hql.append(" and r.enterpriseId=").append(recipesQueryVO.getEnterpriseId());
        }
        //date 20201012 bug 修改导出处方业务数据的时候没有添加配送方式筛选
        if (null != recipesQueryVO.getSendType()) {
            hql.append(" and o.send_type=").append(recipesQueryVO.getSendType());
        }
        //checkResult 0:未审核 1:通过 2:不通过 3:二次签名 4:失效
        if (null != recipesQueryVO.getCheckStatus()) {
            switch (recipesQueryVO.getCheckStatus()) {
                case 0:
                    hql.append(" and r.status =").append(8);
                    break;
                case 1:
//                    hql.append(" and c.checkStatus =").append(1);
                    hql.append(" and r.status=2");
                    break;
                case 2:
//                    hql.append(" and c.checkStatus =").append(0).append(" and r.checker is not null ");
                    hql.append(" and r.status=15");
                    break;
                case 3:
                    hql.append(" and r.supplementaryMemo is not null ");
                    break;
                case 4:
                    hql.append(" and r.status = ").append(9);
                    break;
            }
        }

        if (null != recipesQueryVO.getPayFlag()) {
            if (recipesQueryVO.getPayFlag() == 0) {
                hql.append(" and r.payFlag=").append(recipesQueryVO.getPayFlag());
            } else {
                hql.append(" and o.payFlag=").append(recipesQueryVO.getPayFlag());
            }
        }
        if (recipesQueryVO.getOrderType() != null) {
            if (recipesQueryVO.getOrderType() == 0) {
                hql.append(" and o.orderType=").append(0);
            } else {
                hql.append(" and o.orderType in (1,2,3,4) ");
            }
        }
        return hql;
    }

    private StringBuilder generateRecipeOderHQLforStatisticsN(RecipesQueryVO recipesQueryVO) {
        StringBuilder hql = new StringBuilder("select ");
        hql.append("o.orderId,o.address1,o.address2,o.address3,o.address4,o.streetAddress,o.receiver,o.send_type,o.RecMobile,o.CreateTime,o.ExpressFee,o.OrderCode,o.Status,o.ActualPrice,o.TotalFee,o.EnterpriseId,o.ExpectSendDate,o.ExpectSendTime,o.PayFlag,o.PayTime,o.TradeNo,o.RecipeIdList,");
        hql.append("r.recipeId,r.mpiid,r.patientID,r.doctor,r.organName,r.organDiseaseName,r.doctorName,r.patientName,r.status,r.depart,r.fromflag,r.giveMode,");
        hql.append("d.recipeDetailId,d.drugName,d.drugSpec,d.drugUnit,d.salePrice,d.actualSalePrice,d.saleDrugCode,d.producer,d.licenseNumber,d.useDose,d.useDoseUnit,d.usePathways,d.usingRate,d.useTotalDose");
        hql.append(" from cdr_recipe r LEFT JOIN cdr_recipeorder o on r.orderCode=o.orderCode ");
        hql.append("LEFT JOIN cdr_recipedetail d ON r.RecipeID = d.RecipeID and d.Status= 1 ");
        hql.append(" where 1=1 ");
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
                hql.append(" and r.createDate >= :startTime" + " and r.createDate <= :endTime ");
                break;
            case 1:
                //审核时间
                hql.append(" and r.checkDate >= :startTime" + " and r.checkDate <= :endTime ");
                break;
            case 2:
                //审核时间
                hql.append(" and o.payTime >= :startTime" + " and o.payTime <= :endTime ");
                break;
            default:
                break;
        }
        if (null != recipesQueryVO.getStatus()) {
            hql.append(" and r.status =").append(recipesQueryVO.getStatus());
        }
        if (null != recipesQueryVO.getDoctor()) {
            hql.append(" and r.doctor=").append(recipesQueryVO.getDoctor());
        }
        //根据患者姓名  精确查询
        if (!StringUtils.isEmpty(recipesQueryVO.getPatientName().trim())) {
            hql.append(" and r.patientName='").append(recipesQueryVO.getPatientName()).append("'");
        }
        if (null != recipesQueryVO.getDepart()) {
            hql.append(" and r.depart=").append(recipesQueryVO.getDepart());
        }
        if (null != recipesQueryVO.getGiveMode()) {
            hql.append(" and r.giveMode=").append(recipesQueryVO.getGiveMode());
        }
        if (null != recipesQueryVO.getFromFlag()) {
            hql.append(" and r.fromflag=").append(recipesQueryVO.getFromFlag());
        }
        if (null != recipesQueryVO.getRecipeId()) {
            hql.append(" and r.recipeId=").append(recipesQueryVO.getRecipeId());
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
                        //map.putAll(JSONObject.parseObject(JSON.toJSONString(recipe)));
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
        if (organSymptomIdsTemp == null || organSymptomIdsTemp.size() == 0) {
            return Lists.newArrayList();
        }
        List<Integer> organSymptomIds = Stream.of(organSymptomIdsTemp.toArray(new String[organSymptomIdsTemp.size()])).map(Integer::parseInt).collect(Collectors.toList());
        HibernateStatelessResultAction<List<Symptom>> action = new AbstractHibernateStatelessResultAction<List<Symptom>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select a from Symptom a where " + "  a.symptomId in (:organSymptomIds) ");
                Query q = ss.createQuery(hql.toString());
                q.setParameterList("organSymptomIds", organSymptomIds);
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
        for (int i = 0; i < organSymptomIds.size(); i++) {
            for (int x = 0; x < list.size(); x++) {
                if (list.get(x).getSymptomId().equals(organSymptomIds.get(i))) {
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

    /**
     * 查找指定医生和患者间开的处方单列表
     *
     * @param doctorId
     * @param mpiId
     * @param start
     * @param limit
     * @return
     */
    public List<Recipe> findRecipeListByDoctorAndPatient(final Integer doctorId, final String mpiId, final Integer start, final Integer limit) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "from Recipe where mpiid=:mpiid and doctor=:doctor and status > " + RecipeStatusConstant.UNSIGN + " and status not in (" + RecipeStatusConstant.CHECKING_HOS + ", " + RecipeStatusConstant.DELETE + ")" + " order by createDate desc";
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

    @DAOMethod(sql = "select recipeId from Recipe where clinicOrgan in:organIds and status =8 and fromflag = 1")
    public abstract List<Integer> findReadyAuditRecipeIdsByOrganIds(@DAOParam("organIds") List<Integer> organIds);

    @DAOMethod(sql = "from Recipe where recipeSourceType = 2 and orderCode = :orderCode", limit = 0)
    public abstract List<Recipe> findRecipeByOrdercode(@DAOParam("orderCode") String orderCode);


    /**
     * 监管平台需要同步数据
     *
     * @param startDate 开始时间
     * @param endDate   结束时间
     */
    public List<Recipe> findSyncRecipeList(final String startDate, final String endDate) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                //TODO 药师未审核的数据暂时不上传
                StringBuilder hql = new StringBuilder("from Recipe where fromflag=1 and signDate between '" + startDate + "' and '" + endDate + "' and checker is not null ");
                Query query = ss.createQuery(hql.toString());


                setResult(query.list());
            }
        };

        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }

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
                    hql.append("and checkDateYs between '" + startDate + "' and '" + endDate + "' and clinicOrgan =:organId and syncFlag =0 and checker is not null");
                } else {
                    hql.append("and lastModify between '" + startDate + "' and '" + endDate + "' and clinicOrgan =:organId and syncFlag =0 and status not in (0,10,11,16)");
                }
                Query query = ss.createQuery(hql.toString());
                query.setParameter("organId", organId);
                setResult(query.list());
            }
        };

        HibernateSessionTemplate.instance().executeReadOnly(action);
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
    public List<Recipe> findSyncRecipeListByOrganIdForSH(final Integer organId, final String startDate, final String endDate, final Boolean updateFlag) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from Recipe r where fromflag=1 and clinicOrgan =:organId and syncFlag =0" + " and ( (r.createDate between '" + startDate + "' and '" + endDate + "') ");
                //是否包含更新时间为指定时间范围内
                if (updateFlag) {
                    hql.append(" or (r.lastModify between '" + startDate + "' and '" + endDate + "')  )");
                } else {
                    hql.append(")");
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
                String sql = "SELECT\n" + "\to.dispensingApothecaryName AS dispensingApothecaryName,\n" + "\tcount(recipeId) AS recipeCount,\n" + "\tsum(totalMoney) totalMoney\n" + "FROM\n" + "\tcdr_recipe r\n" + "LEFT JOIN cdr_recipeorder o ON (r.ordercode = o.ordercode)\n" + "WHERE\n" + "\tr.ordercode IS NOT NULL\n" + "AND o.OrganId = :organId\n" + (StringUtils.isNotEmpty(doctorName) ? "AND o.dispensingApothecaryName like :dispensingApothecaryName\n" : "") + "AND o.status in (" + orderStatus + ")\n" + "AND o.dispensingStatusAlterTime BETWEEN '" + startDate + "'\n" + "AND '" + endDate + "'\n" + (StringUtils.isNotEmpty(recipeType) ? "AND r.recipeType in (:recipeType)\n" : "") + "GROUP BY\n" + "\to.dispensingApothecaryName";
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
                /*String sql = "SELECT\n" +
                        "\trd.drugId,\n" +
                        "\trd.drugName,\n" +
                        "\trd.drugSpec,\n" +
                        "\trd.drugUnit,\n" +
                        "\tcast(sum(rd.useTotalDose) as SIGNED) AS count,\n" +
                        "\trd.drugCost,\n" +
                        "\tSUM(rd.saleprice) AS countMoney,\n" +
                        "\tCASE bd.drugtype WHEN 1 THEN '西药' WHEN 2 THEN '中成药' ELSE '中草药' " +
                        "END AS drugtype,\n" +
                        "END AS cr.recipeId\n" +
                        "FROM\n" +
                        "\tcdr_recipe cr\n" +
                        "LEFT JOIN cdr_recipedetail rd ON (cr.RecipeID = rd.RecipeID)\n" +
                        "LEFT JOIN base_druglist bd ON (rd.drugId = bd.drugid)\n" +
                        "LEFT JOIN cdr_recipeorder co ON (cr.ordercode = co.ordercode)\n" +
                        "WHERE\n" +
                        "\tco.dispensingTime BETWEEN '" + startDate + " '\n" +
                        "AND '" + endDate + "'\n" +
                        "AND ClinicOrgan = :organId\n" +
                        "AND rd.drugCost is not null\n" +
                        "AND co.`Status` IN (13,14,15)\n" +
                        "AND rd.useTotalDose is not null\n" +
                        (drugType == 0 ? " " : "AND bd.drugtype IN (:drugType)\n") +
                        "GROUP BY\n" +
                        "\tdrugId,cr.recipeId\n";*/
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
     * @param limit
     * @return
     */
    public List<Recipe> findRecipeListByDoctorAndPatientAndStatusList(final Integer doctorId, final String mpiId, final Integer start, final Integer limit, final List<Integer> statusList) {
        Long beginTime = new Date().getTime();
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "from Recipe where mpiid=:mpiid  ";
                if (doctorId != null) {
                    hql += " and doctor=:doctor";
                }
                hql += " and status IN (:statusList) order by createDate desc ";
                Query query = ss.createQuery(hql);
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

    public List<Recipe> findNoPayRecipeListByPatientNameAndDate(String patientName, Integer organId, Date startDate, Date endDate) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from Recipe where patientName =:patientName and SignDate between :startDate and :endDate ");
                if (organId != null) {
                    hql.append(" and ClinicOrgan =:organId");
                }
                hql.append(" and PayFlag = 0 order by recipeId desc");
                Query query = ss.createQuery(hql.toString());
                if (organId != null) {
                    query.setParameter("organId", organId);
                }
                query.setParameter("patientName", patientName);
                query.setParameter("startDate", startDate);
                query.setParameter("endDate", endDate);
                setResult(query.list());
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
     * 通过复诊业务来源调用查询处方状态
     *
     * @param bussSource
     * @param clinicId
     * @return
     */
    @DAOMethod(sql = "from Recipe where bussSource=:bussSource and clinicId=:clinicId")
    public abstract List<Recipe> findRecipeStatusByBussSourceAndClinicId(@DAOParam("bussSource") Integer bussSource, @DAOParam("clinicId") Integer clinicId);


    /**
     * @param bussSource
     * @param clinicId
     * @param Status
     * @return
     */
    @DAOMethod(sql = "from Recipe where bussSource=:bussSource and clinicId=:clinicId and status not in(9,13,14)")
    public abstract List<Recipe> findRecipeStatusLoseByBussSourceAndClinicId(@DAOParam("bussSource") Integer bussSource, @DAOParam("clinicId") Integer clinicId, @DAOParam("status") Integer Status);

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

    public List<Recipe> findReadyToSendRecipeByDepId(final Integer enterpriseId) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select a from Recipe a, RecipeOrder b where a.orderCode = b.orderCode and b.enterpriseId=:enterpriseId and b.status = 3 and b.payFlag = 1 and b.effective=1 ");

                Query query = ss.createQuery(hql.toString());

                query.setParameter("enterpriseId", enterpriseId);
                setResult(query.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
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
                    ;
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
                hql.append("from Recipe r where doctor=:doctorId and fromflag=1 and status!=10  ");
                //通过条件查询status
                if (tapStatus == null || tapStatus == 0) {
                    ;
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
     * @param searchFlag   1-审方医生 2-患者姓名 3-病历号
     * @param start
     * @param limit
     * @return
     * @author zhongzx
     */
    public List<Recipe> searchRecipe(final Set<Integer> organs, final Integer searchFlag, final String searchString, final Integer start, final Integer limit) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select distinct r from Recipe r");
                if (0 == searchFlag) {
                    hql.append(" where r.doctorName like:searchString ");
                } else if (2 == searchFlag) {
                    hql.append(" where r.patientName like:searchString ");
                } else if (3 == searchFlag) {
                    hql.append(" where r.patientID like:searchString ");
                } else {
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "searchFlag is invalid");
                }
                hql.append("and (r.checkDateYs is not null or r.status = 8) " + "and r.clinicOrgan in (:organs) order by r.signDate desc");

                Query q = ss.createQuery(hql.toString());
                q.setParameter("searchString", "%" + searchString + "%");
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

    @DAOMethod(sql = "from Recipe where recipeId in(:recipeIds) and clinicOrgan in(:organIds)")
    public abstract List<Recipe> findByRecipeAndOrganId(@DAOParam("recipeIds") List<Integer> recipeIds, @DAOParam("organIds") Set<Integer> organIds);

    /**
     * 根据需要变更的状态获取处方ID集合
     *
     * @param cancelStatus
     * @return
     */
    public List<Recipe> getRecipeListForSignCancelRecipe(final String startDt, final String endDt) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder sql = new StringBuilder("select * from cdr_recipe where status in (30, 26) and signDate between '" + startDt + "' and '" + endDt + "' ");
                sql.append("UNION ALL ");
                //date 20200922 添加药师CA未签名过期
                sql.append("select * from cdr_recipe where status in (31, 27 , 32) and reviewType = 1  and signDate between '" + startDt + "' and '" + endDt + "' ");
                Query q = ss.createSQLQuery(sql.toString()).addEntity(Recipe.class);
                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod(sql = "select count(*) from Recipe")
    public abstract Long getCountByAll();

    @DAOMethod(limit = 0)
    public abstract List<Recipe> findByClinicOrgan(Integer clinicOrgan);


    public List<Recipe> findRecipeForDoc(final Integer organId) {
        HibernateStatelessResultAction<List<Recipe>> action = new AbstractHibernateStatelessResultAction<List<Recipe>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "select r from Recipe r, RecipeExtend o where r.recipeId=o.recipeId  and r.clinicOrgan =:organId and o.docIndexId is not null AND r.organDiseaseId is null";
                Query q = ss.createQuery(hql);
                q.setParameter("organId", organId);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);

        return action.getResult();
    }

    @DAOMethod(sql = "from Recipe where recipeCode in (:recipeCodeList) and clinicOrgan=:clinicOrgan and mpiid=:mpiid and payFlag!=1")
    public abstract List<Recipe> findByRecipeCodeAndClinicOrganAndMpiid(@DAOParam("recipeCodeList") List<String> recipeCodeList, @DAOParam("clinicOrgan") Integer clinicOrgan, @DAOParam("mpiid") String mpiid);

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

//    @DAOMethod(sql = "from Recipe where clinicOrgan in(:organIds) and  recipeType in(:recipeTypes) and  checkMode<3  and status not in (9,31)  and checkOrgan IS NOT NULL")
//    public abstract List<Recipe> queryRecipeInfoByOrganAndRecipeType(@DAOParam("organIds") List<Integer> organIds,
//                                                        @DAOParam("recipeTypes") List<Integer> recipeTypes);

    /**
     * 获取待审核列表审核结果
     *
     * @param recipe checkResult 0:未审核 1:通过 2:不通过 3:二次签名 4:失效
     * @return
     */
    private Integer getCheckResultByPending(Recipe recipe) {
        Integer checkResult = 0;
        Integer status = recipe.getStatus();
        //date 20191127
        //添加前置判断:当审核方式是不需要审核则返回通过审核状态
        if (ReviewTypeConstant.Not_Need_Check == recipe.getReviewType()) {
            return RecipePharmacistCheckConstant.Check_Pass;
        }
        if (eh.cdr.constant.RecipeStatusConstant.REVOKE == status) {
            return RecipePharmacistCheckConstant.Check_Failure;
        }
        if (eh.cdr.constant.RecipeStatusConstant.READY_CHECK_YS == status) {
            checkResult = RecipePharmacistCheckConstant.Already_Check;
        } else {
            if (StringUtils.isNotEmpty(recipe.getSupplementaryMemo())) {
                checkResult = RecipePharmacistCheckConstant.Second_Sign;
            } else {
                IRecipeCheckService recipeCheckService = RecipeAuditAPI.getService(IRecipeCheckService.class, "recipeCheckServiceImpl");
                RecipeCheckBean recipeCheckBean = recipeCheckService.getNowCheckResultByRecipeId(recipe.getRecipeId());
                //有审核记录就展示
                if (recipeCheckBean != null) {
                    if (null != recipeCheckBean.getChecker() && RecipecCheckStatusConstant.First_Check_No_Pass == recipeCheckBean.getCheckStatus()) {
                        checkResult = RecipePharmacistCheckConstant.Check_Pass;
                    } else if (null != recipeCheckBean.getChecker() && RecipecCheckStatusConstant.Check_Normal == recipeCheckBean.getCheckStatus()) {
                        checkResult = RecipePharmacistCheckConstant.Check_No_Pass;
                    }
                }
            }
        }

        return checkResult;
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
    @DAOMethod(sql = "from Recipe where clinicOrgan=:clinicOrgan and mpiId=:mpiId order by signDate DESC")
    public abstract List<Recipe> queryRecipeInfoByMpiIdAndOrganId(@DAOParam("mpiId") String mpiId, @DAOParam("clinicOrgan") Integer clinicOrgan, @DAOParam(pageStart = true) int start, @DAOParam(pageLimit = true) int limit);

//    /**
//     * 根据患者id和机构id查询对应的未支付的订单
//     *
//     * @param mpiId
//     * @param organId
//     * @return
//     */
//    @DAOMethod(sql = "From RecipeOrder  where mpiId = :mpiId AND organId = :organId AND orderCode is not null and payFlag =0")
//    public abstract List<RecipeOrder> queryOrderCodeUnpaid(@DAOParam("mpiId") String mpiId, @DAOParam("organId") Integer organId);


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
    @DAOMethod(sql = "from Recipe where clinicId=:clinicId and status not in(-1,15,9,0,13,14,16)")
    public abstract List<Recipe> findRecipeCountByClinicIdAndValidStatus(@DAOParam("clinicId") Integer clinicId);

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
//                Query query = ss.createQuery(hql.toString());
//                setResult(query.list());
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
     * @return
     */
    @DAOMethod(sql = "select new Recipe(recipeId,clinicOrgan,recipeType) from Recipe where  checkMode<2 and status =8 and  createDate>:date",limit = 0)
    public abstract List<Recipe>  findToAuditPlatformRecipe(@DAOParam("date") Date date);

}
