package recipe.dao;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.ngari.his.regulation.entity.RegulationChargeDetailReq;
import com.ngari.his.regulation.entity.RegulationChargeDetailReqTo;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import ctd.util.converter.ConversionUtils;
import eh.billcheck.constant.BillBusFeeTypeEnum;
import eh.billcheck.vo.BillBusFeeVo;
import eh.billcheck.vo.BillDrugFeeVo;
import eh.billcheck.vo.BillRecipeDetailVo;
import eh.billcheck.vo.RecipeBillRequest;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.constant.RecipeBussConstant;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/2/13.
 */
@RpcSupportDAO
public abstract class RecipeOrderDAO extends HibernateSupportDelegateDAO<RecipeOrder> {
    private static final Logger logger = LoggerFactory.getLogger(RecipeOrderDAO.class);

    private static final Map<Integer, String> DRUG_TYPE_TABLE = ImmutableMap.of(1, "西药", 2, "中成药", 3, "中药", 4, "膏方");

    public RecipeOrderDAO() {
        super();
        this.setEntityName(RecipeOrder.class.getName());
        this.setKeyField("orderId");
    }

    /**
     * 根据编号获取有效订单
     *
     * @param orderCode
     * @return
     */
    @DAOMethod(sql = "from RecipeOrder where orderCode=:orderCode")
    public abstract RecipeOrder getByOrderCode(@DAOParam("orderCode") String orderCode);

    /**
     * 根据流水号获取订单
     * @param tradeNo
     * @return
     */
    @DAOMethod
    public abstract RecipeOrder getByOutTradeNo(String tradeNo);

    /**
     * 根据传芳id获取订单编号
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "select order.orderCode from RecipeOrder order, Recipe recipe where order.orderCode=recipe.orderCode and order.effective=1 and recipe.recipeId=:recipeId")
    public abstract String getOrderCodeByRecipeId(@DAOParam("recipeId") Integer recipeId);

    /**
     * 根据处方id获取订单编号
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "select order.orderCode from RecipeOrder order, Recipe recipe where order.orderCode=recipe.orderCode and recipe.recipeId=:recipeId")
    public abstract String getOrderCodeByRecipeIdWithoutCheck(@DAOParam("recipeId") Integer recipeId);

    /**
     * 根据处方id获取订单
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "select order from RecipeOrder order, Recipe recipe where order.orderCode=recipe.orderCode and order.effective=1 and recipe.recipeId=:recipeId")
    public abstract RecipeOrder getOrderByRecipeId(@DAOParam("recipeId") Integer recipeId);

    @DAOMethod(sql = "select order from RecipeOrder order, Recipe recipe where order.orderCode=recipe.orderCode and recipe.recipeId=:recipeId")
    public abstract RecipeOrder getOrderByRecipeIdQuery(@DAOParam("recipeId") Integer recipeId);

    /**
     * 根据处方id获取药企id
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "select order.enterpriseId from RecipeOrder order, Recipe recipe where order.orderCode=recipe.orderCode and recipe.recipeId=:recipeId")
    public abstract Integer getEnterpriseIdByRecipeId(@DAOParam("recipeId") Integer recipeId);

    /**
     * 根据支付标识查询订单集合
     * @param payFlag
     * @return
     */
    @DAOMethod
    public abstract List<RecipeOrder> findByPayFlag(Integer payFlag);

    /**
     * 订单是否有效
     *
     * @param orderCode
     * @return
     */
    public boolean isEffectiveOrder(final String orderCode, final Integer payMode) {
        if (StringUtils.isEmpty(orderCode)) {
            return false;
        }

        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("select count(1) from RecipeOrder where orderCode=:orderCode ");
                //医保支付会生成一个无效的临时订单，但是医快付不允许重复发送同一个处方的信息
                if (null == payMode || !RecipeBussConstant.PAYMODE_MEDICAL_INSURANCE.equals(payMode)) {
                    hql.append(" and effective=1 ");
                }
                Query q = ss.createQuery(hql.toString());
                q.setParameter("orderCode", orderCode);

                long count = (Long) q.uniqueResult();
                setResult(count > 0);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 更新订单自定义字段
     *
     * @param orderCode
     * @param changeAttr
     * @return
     */
    public Boolean updateByOrdeCode(final String orderCode, final Map<String, ?> changeAttr) {
        if (null == changeAttr || changeAttr.isEmpty()) {
            return true;
        }

        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update RecipeOrder set ");
                StringBuilder keyHql = new StringBuilder();
                for (String key : changeAttr.keySet()) {
                    keyHql.append("," + key + "=:" + key);
                }
                hql.append(keyHql.toString().substring(1)).append(" where orderCode=:orderCode");
                Query q = ss.createQuery(hql.toString());

                q.setParameter("orderCode", orderCode);
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
     * 根据需要变更的状态获取处方ID集合
     *
     * @param startDt
     * @param endDt
     * @return
     */
    public List<String> getRecipeIdForCancelRecipeOrder(final String startDt, final String endDt) {
        HibernateStatelessResultAction<List<String>> action = new AbstractHibernateStatelessResultAction<List<String>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String sql = "select orderCode from RecipeOrder where createTime between '" + startDt + "' and '" + endDt + "' and status not in (6,7,8) and drugStoreCode is not null";

                Query q = ss.createQuery(sql);
                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据处方关联的订单
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "select order from RecipeOrder order, Recipe recipe where order.orderCode=recipe.orderCode and recipe.recipeId=:recipeId")
    public abstract RecipeOrder getRelationOrderByRecipeId(@DAOParam("recipeId") Integer recipeId);

    /**
     * 根据日期查询已支付或退款订单信息
     *
     * @param time
     * @return
     */
    @DAOMethod(sql = "from RecipeOrder where (refundFlag isNotNull or refundFlag <> 0) and to_days(refundTime) = to_days(:time)")
    public abstract RecipeOrder getPayInfoByTime(@DAOParam("time") Date time);

    /**
     * 根据物流单号查询手机号
     *
     * @param trackingNumber  顺丰物流单号
     * @return 订单信息
     */
    @DAOMethod(sql = "from RecipeOrder where LogisticsCompany = 1 and  trackingNumber =:trackingNumber")
    public abstract RecipeOrder getByTrackingNumber(@DAOParam("trackingNumber") String trackingNumber);

    /**
     * 根据日期查询订单支付和退款信息(只获取实际支付金额不为0的，调用支付平台的)
     *
     * @param startTime
     * @param endTime
     * @param start
     * @param pageSize
     * @return
     */
    public List<BillRecipeDetailVo> getPayAndRefundInfoByTime(Date startTime, Date endTime, int start, int pageSize){
        HibernateStatelessResultAction<List<BillRecipeDetailVo>> action = new AbstractHibernateStatelessResultAction<List<BillRecipeDetailVo>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select * from ( ");
                hql.append("select r.recipeId, r.doctor, o.MpiId, o.PayTime, o.OrganId, r.Depart, o.OutTradeNo, ");
                hql.append("o.OrderType, r.GiveMode, o.PayFlag, o.RegisterFee, o.ExpressFee, o.DecoctionFee, o.AuditFee, ");
                hql.append("o.OtherFee, o.RecipeFee, o.CouponFee, o.PayBackPrice, o.FundAmount, d.name, 0 as billType, o.EnterpriseId from ");
                hql.append("cdr_recipe r INNER JOIN cdr_recipeorder o on r.OrderCode = o.OrderCode LEFT JOIN cdr_drugsenterprise d on d.id = o.EnterpriseId ");
                hql.append("where o.payFlag = 1 and o.payTime between :startTime and :endTime and o.Effective = 1 and o.actualPrice <> 0 ");
                hql.append("UNION ALL ");
                hql.append("select r.recipeId, r.doctor, o.MpiId, o.refundTime as PayTime, o.OrganId, r.Depart, o.OutTradeNo, ");
                hql.append("o.OrderType, r.GiveMode, o.PayFlag, o.RegisterFee, o.ExpressFee, o.DecoctionFee, o.AuditFee, ");
                hql.append("o.OtherFee, o.RecipeFee, o.CouponFee, o.PayBackPrice, o.FundAmount, d.name, 1 as billType, o.EnterpriseId from ");
                hql.append("cdr_recipe r INNER JOIN cdr_recipeorder o on r.OrderCode = o.OrderCode LEFT JOIN cdr_drugsenterprise d on d.id = o.EnterpriseId ");
                hql.append("where (o.refundFlag is Not Null and o.refundFlag <> 0) and o.refundTime between :startTime and :endTime and o.actualPrice <> 0 ");
                hql.append("UNION ALL ");
                hql.append("select r.recipeId, r.doctor, o.MpiId, o.PayTime, o.OrganId, r.Depart, o.OutTradeNo, ");
                hql.append("o.OrderType, r.GiveMode, o.PayFlag, o.RegisterFee, o.ExpressFee, o.DecoctionFee, o.AuditFee, ");
                hql.append("o.OtherFee, o.RecipeFee, o.CouponFee, o.PayBackPrice, o.FundAmount, d.name, 0 as billType, o.EnterpriseId from  ");
                hql.append("cdr_recipe r INNER JOIN cdr_recipeorder o on r.OrderCode = o.OrderCode LEFT JOIN cdr_drugsenterprise d on d.id = o.EnterpriseId ");
                hql.append("where (o.refundFlag is Not Null and o.refundFlag <> 0) and o.payFlag <>1 and o.payTime between :startTime and :endTime and o.actualPrice <> 0 ");
                hql.append(" ) a order by a.recipeId, a.payTime");

                Query q = ss.createSQLQuery(hql.toString());
                q.setParameter("startTime", startTime);
                q.setParameter("endTime", endTime);
                q.setFirstResult(start);
                q.setMaxResults(pageSize);
                List<Object[]> result = q.list();
                List<BillRecipeDetailVo> backList = new ArrayList<>(pageSize);
                if (CollectionUtils.isNotEmpty(result)){
                    BillRecipeDetailVo vo;
                    for (Object[] objs : result) {
                        vo = new BillRecipeDetailVo();
                        vo.setRecipeId(objs[2] == null ? null : (Integer)objs[0]);
                        vo.setMpiId(objs[2] == null ? null : objs[2] + "");
                        vo.setDoctorId(objs[1] == null ? null : (Integer)objs[1]);
                        vo.setRecipeTime(objs[3] == null ? null : (Date)objs[3]);
                        vo.setOrganId(objs[4] == null ? null : (Integer)objs[4]);
                        vo.setDeptId(objs[5] == null ? null : (Integer)objs[5]);
                        vo.setOutTradeNo(objs[6] == null ? null : objs[6] + "");
                        vo.setSettleType(objs[7] == null ? null : Integer.parseInt(objs[7] + ""));
                        vo.setDeliveryMethod(objs[8] == null ? null : Integer.parseInt(objs[8] + ""));
                        vo.setDrugCompany(objs[21] == null ? null : (Integer)objs[21]);
                        vo.setDrugCompanyName(objs[19] == null ? null : objs[19] + "");
                        vo.setPayFlag(objs[9] == null ? null : Integer.parseInt(objs[9] + ""));
                        vo.setAppointFee(objs[10] == null ? null : Double.valueOf(objs[10] + ""));
                        vo.setDeliveryFee(objs[11] == null ? null :Double.valueOf(objs[11] + ""));
                        vo.setDaiJianFee(objs[12] == null ? null : Double.valueOf(objs[12] + ""));
                        vo.setReviewFee(objs[13] == null ? null : Double.valueOf(objs[13] + ""));
                        vo.setOtherFee(objs[14] == null ? null : Double.valueOf(objs[14] + ""));
                        vo.setDrugFee(objs[15] == null ? null : Double.valueOf(objs[15] + ""));
                        vo.setDicountedFee(objs[16] == null ? null : Double.valueOf(objs[16] + ""));
                        vo.setTotalFee(objs[17] == null ? null : Double.valueOf(objs[17] + ""));
                        vo.setMedicarePay(objs[18] == null ? null : Double.valueOf(objs[18] + ""));
                        vo.setBillType(objs[20] == null ? null : Integer.parseInt(objs[20] + ""));
                        vo.setSelfPay(objs[17] == null ? 0.0 : new BigDecimal(objs[17] + "").subtract(new BigDecimal(objs[18] == null ? "0.0" : objs[18] + "")).doubleValue());

                        backList.add(vo);
                    }
                }

                setResult(backList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据药企编号和支付时间查询订单
     * @param enterpriseId    药企编号
     * @param payTime         支付时间
     * @return                订单列表
     */
    public List<RecipeOrder> findRecipeOrderByDepIdAndPayTime(Integer enterpriseId, String payTime){
        HibernateStatelessResultAction<List<RecipeOrder>> action = new AbstractHibernateStatelessResultAction<List<RecipeOrder>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String sql = "select a from RecipeOrder a,Recipe b where a.orderCode = b.orderCode and b.pushFlag = 0 and a.payFlag = 1 and a.effective = 1 and a.status in (3,12)" +
                        " and a.effective = 1 and a.enterpriseId =:enterpriseId ";

                Query q = ss.createQuery(sql);
                q.setParameter("enterpriseId", enterpriseId);
                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据日期查询订单支付和退款信息(只获取实际支付金额不为0的，调用支付平台的)
     *
     * @param ngariOrganIds
     * @param startTime
     * @param endTime
     * @return
     */
    public List<RegulationChargeDetailReqTo> queryRegulationChargeDetailList(final List<Integer> ngariOrganIds, final Date startTime, final Date endTime){
        HibernateStatelessResultAction<List<RegulationChargeDetailReqTo>> action = new AbstractHibernateStatelessResultAction<List<RegulationChargeDetailReqTo>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select r.ClinicOrgan,l.RecipeDetailID,o.PayFlag,r.ClinicID,o.TradeNo,r.RecipeID,r.RecipeType,l.DrugUnit,l.actualSalePrice,l.UseTotalDose,r.Status,drug.medicalDrugCode,l.salePrice from cdr_recipe r LEFT JOIN cdr_recipedetail l ON r.RecipeID = l.recipeId");
                hql.append(" LEFT JOIN base_organdruglist drug on drug.OrganDrugCode=l.OrganDrugCode and drug.OrganID=r.clinicorgan");
                hql.append(" LEFT JOIN cdr_recipeorder o ON r.OrderCode = o.OrderCode");
                hql.append(" WHERE (date(r.CreateDate) between :startTime and :endTime OR date(r.LastModify) between :startTime and :endTime)");
                hql.append(" AND o.Effective = 1");
                hql.append(" AND o.PayFlag > 0");
                hql.append(" AND r.bussSource = 2");
                hql.append(" AND r.ClinicOrgan IN :ngariOrganIds");
                hql.append(" AND l.`Status` =1");
                Query q = ss.createSQLQuery(hql.toString());
                q.setParameter("startTime", startTime);
                q.setParameter("endTime", endTime);
                q.setParameterList("ngariOrganIds",ngariOrganIds);
                logger.info("paramter is startTime:[{}],endTime:[{}],ngariOrganIds[{}]",startTime,endTime,ngariOrganIds);
                List<Object[]> result = q.list();
                List<RegulationChargeDetailReqTo> backList = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(result)){
                    RegulationChargeDetailReqTo vo;
                    for (Object[] objs : result) {
                        vo = new RegulationChargeDetailReqTo();
                        vo.setOrganID(objs[0] == null ? null : (Integer)objs[0]);
                        vo.setRecipeDetailID(objs[1] == null ? null : objs[1] + "");
                        vo.setPayFlag(objs[2] == null ? null : Integer.parseInt(objs[2]+""));
                        vo.setClinicID(objs[3] == null ? null : objs[3]+"");
                        vo.setTradeNo(objs[4] == null ? null : objs[4]+"");
                        vo.setRecipeID(objs[5] == null ? null : objs[5]+"");
                        vo.setRecipeType(objs[6] == null ? null : Integer.parseInt(objs[6]+""));
                        vo.setDrugUnit(objs[7] == null ? null : objs[7] + "");
                        vo.setActualSalePrice(objs[8] == null ? null :(BigDecimal)objs[8]);
                        vo.setUseTotalDose(objs[9] == null ? null : (BigDecimal)objs[9]);
                        vo.setStatus(objs[10] == null ? null :  Integer.parseInt(objs[10]+""));
                        vo.setMedicalDrugCode(objs[11]== null ? null: objs[11]+"");
                        vo.setSalePrice(objs[12] == null ? null : (BigDecimal)objs[12]);
                        backList.add(vo);
                    }
                }
                setResult(backList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    /**
     * 根据物流公司编号与快递单号查询订单编号
     * @param logisticsCompany
     * @param trackingNumber
     * @return
     */
    @DAOMethod(sql = "select orderCode from RecipeOrder order  where order.logisticsCompany=:logisticsCompany and order.trackingNumber=:trackingNumber")
    public abstract String getOrderCodeByLogisticsCompanyAndTrackingNumber(@DAOParam("logisticsCompany") Integer logisticsCompany,
                                                                                @DAOParam("trackingNumber") String trackingNumber);

    /**
     * 根据日期获取电子处方药企配送订单明细
     *
     * @param startTime 开始时间
     * @param endTime 截止时间
     * @param organId 机构ID
     * @param depId 药企ID
     * @return
     */
    public List<Map<String, Object>> queryrecipeOrderDetailed(Date startTime, Date endTime, Integer organId, List<Integer> organIds, Integer depId, Integer drugId, String orderColumn, String orderType, int start, int limit){
        HibernateStatelessResultAction<List<Map<String, Object>>> action = new AbstractHibernateStatelessResultAction<List<Map<String, Object>>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder sql = new StringBuilder();
                StringBuilder sqlPay = new StringBuilder();
                StringBuilder sqlRefund = new StringBuilder();
                if (drugId != null) {
                    sqlPay.append("SELECT r.recipeId, r.patientName, r.MPIID, dep.NAME, r.organName, r.doctorName, r.SignDate as signDate, '支付成功' as payType, o.PayTime as payTime, o.refundTime as refundTime, d.useTotalDose as dose, d.salePrice * d.useTotalDose as ActualPrice");
                    sqlPay.append(" FROM cdr_recipe r INNER JOIN cdr_recipeorder o ON r.OrderCode = o.OrderCode INNER JOIN cdr_recipedetail d ON r.recipeId = d.recipeId LEFT JOIN base_saledruglist s ON d.drugId = s.drugId and o.EnterpriseId = s.OrganID LEFT JOIN cdr_drugsenterprise dep ON o.EnterpriseId = dep.Id ");
                    sqlPay.append(" WHERE r.GiveMode = 1 and ((o.payflag = 1 OR o.refundflag = 1) and o.paytime BETWEEN :startTime  AND :endTime ) ");
                    sqlRefund.append("SELECT r.recipeId, r.patientName, r.MPIID, dep.NAME, r.organName, r.doctorName, r.SignDate as signDate, '退款成功' as payType, o.PayTime as payTime, o.refundTime as refundTime, d.useTotalDose as dose, d.salePrice * (0-d.useTotalDose) as ActualPrice");
                    sqlRefund.append(" FROM cdr_recipe r INNER JOIN cdr_recipeorder o ON r.OrderCode = o.OrderCode INNER JOIN cdr_recipedetail d ON r.recipeId = d.recipeId LEFT JOIN base_saledruglist s ON d.drugId = s.drugId and o.EnterpriseId = s.OrganID LEFT JOIN cdr_drugsenterprise dep ON o.EnterpriseId = dep.Id ");
                    sqlRefund.append(" WHERE r.GiveMode = 1 and (o.refundflag = 1 and o.refundTime BETWEEN :startTime  AND :endTime) ");
                } else {
                    sqlPay.append("SELECT r.recipeId, r.patientName, r.MPIID, dep.NAME, r.organName, r.doctorName, r.SignDate as signDate, '支付成功' as payType, o.PayTime as payTime, o.refundTime as refundTime, 1 as dose, o.RecipeFee as ActualPrice");
                    sqlPay.append(" FROM cdr_recipe r INNER JOIN cdr_recipeorder o ON r.OrderCode = o.OrderCode LEFT JOIN cdr_drugsenterprise dep ON o.EnterpriseId = dep.Id ");
                    sqlPay.append(" WHERE r.GiveMode = 1 and ((o.payflag = 1 OR o.refundflag = 1) and o.paytime BETWEEN :startTime  AND :endTime ) ");
                    sqlRefund.append("SELECT r.recipeId, r.patientName, r.MPIID, dep.NAME, r.organName, r.doctorName, r.SignDate as signDate, '退款成功' as payType, o.PayTime as payTime, o.refundTime as refundTime, 1 as dose, 0-o.RecipeFee as ActualPrice");
                    sqlRefund.append(" FROM cdr_recipe r INNER JOIN cdr_recipeorder o ON r.OrderCode = o.OrderCode LEFT JOIN cdr_drugsenterprise dep ON o.EnterpriseId = dep.Id ");
                    sqlRefund.append(" WHERE r.GiveMode = 1 and (o.refundflag = 1 and o.refundTime BETWEEN :startTime  AND :endTime) ");
                }
                if (organId != null) {
                    sqlPay.append(" and r.clinicOrgan = :organId");
                    sqlRefund.append(" and r.clinicOrgan = :organId");
                } else if (organIds != null && organIds.size() > 0) {
                    sqlPay.append(" and r.clinicOrgan in (:organIds)");
                    sqlRefund.append(" and r.clinicOrgan in (:organIds)");
                }
                if (depId != null) {
                    sqlPay.append(" and o.EnterpriseId = :depId");
                    sqlRefund.append(" and o.EnterpriseId = :depId");
                }
                if (drugId != null) {
                    sqlPay.append(" and d.drugId = :drugId and d.status = 1");
                    sqlRefund.append(" and d.drugId = :drugId and d.status = 1");
                }
                //退款的处方单需要展示两条记录，所以要在取一次
                sql.append("SELECT * from ( ").append(sqlPay).append(" UNION ALL ").append(sqlRefund).append(" ) a");
                if (orderColumn != null) {
                    sql.append(" order by " + orderColumn + " ");
                }
                if(orderType != null){
                    sql.append(orderType);
                }
                Query q = ss.createSQLQuery(sql.toString());
                q.setParameter("startTime", startTime);
                q.setParameter("endTime", endTime);
                if (organId != null) {
                    q.setParameter("organId", organId);
                } else if (organIds != null && organIds.size() > 0) {
                    q.setParameterList("organIds", organIds);
                }
                if (depId != null) {
                    q.setParameter("depId", depId);
                }
                if (drugId != null) {
                    q.setParameter("drugId", drugId);
                }

                q.setFirstResult(start);
                q.setMaxResults(limit);
                List<Object[]> result = q.list();
                List<Map<String, Object>> backList = new ArrayList<>();

                Set<String> mpiIds = Sets.newHashSet();
                if (CollectionUtils.isNotEmpty(result)){

                    //获取全部身份证信息
                    PatientService patientService = BasicAPI.getService(PatientService.class);
                    Map<String, String> patientBeanMap = Maps.newHashMap();
                    for (Object[] obj : result) {
                        if(obj[2] != null){
                            mpiIds.add((String)obj[2]);
                        }
                    }

                    if(0 < mpiIds.size()){
                        List<PatientDTO> patientBeanList = patientService.findByMpiIdIn(new ArrayList<String>(mpiIds));
                        for (PatientDTO p : patientBeanList) {
                            patientBeanMap.put(p.getMpiId(), p.getIdcard());
                        }
                    }

                    Map<String, Object> vo;
                    for (Object[] objs : result) {
                        vo = new HashMap<String, Object>();
                        vo.put("recipeId", objs[0] == null ? null : (Integer)objs[0]);
                        vo.put("patientName", objs[1] == null ? null : (String)objs[1]);
                        vo.put("cardId", objs[2] == null ? null : patientBeanMap.get((String)objs[2]));
                        vo.put("enterpriseName", objs[3] == null ? null : (String)objs[3]);
                        vo.put("organName", objs[4] == null ? null : (String)objs[4]);
                        vo.put("doctorName", objs[5] == null ? null : (String)objs[5]);
                        vo.put("signDate", objs[6] == null ? null : (Date)objs[6]);
                        vo.put("payType", objs[7] == null ? null : objs[7].toString());
                        vo.put("payTime", objs[8] == null ? null : (Date)objs[8]);
                        vo.put("refundTime", objs[9] == null ? null : (Date)objs[9]);
                        vo.put("dose", objs[10] == null ? null : objs[10].toString());
                        vo.put("actualPrice", objs[11] == null ? null : Double.valueOf(objs[11]+""));
                        backList.add(vo);
                    }
                }
                setResult(backList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据日期获取电子处方药企配送订单明细（总计数据）
     *
     * @param startTime 开始时间
     * @param endTime 截止时间
     * @param organId 机构ID
     * @param depId 药企ID
     * @return
     */
    public Map<String, Object> queryrecipeOrderDetailedTotal(Date startTime, Date endTime, Integer organId, List<Integer> organIds, Integer depId, Integer drugId){
        HibernateStatelessResultAction<Map<String, Object>> action = new AbstractHibernateStatelessResultAction<Map<String, Object>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder sql = new StringBuilder();
                StringBuilder sqlPay = new StringBuilder();
                StringBuilder sqlRefund = new StringBuilder();
                if (drugId != null) {
                    sqlPay.append("SELECT count(1) as count, sum(d.salePrice * d.useTotalDose) as totalPrice ");
                    sqlPay.append(" FROM cdr_recipe r INNER JOIN cdr_recipeorder o ON r.OrderCode = o.OrderCode INNER JOIN cdr_recipedetail d ON r.recipeId = d.recipeId LEFT JOIN base_saledruglist s ON d.drugId = s.drugId and o.EnterpriseId = s.OrganID");
                    sqlPay.append(" WHERE r.GiveMode = 1 and ((o.payflag = 1 OR o.refundflag = 1) and o.paytime BETWEEN :startTime  AND :endTime ) ");
                    sqlRefund.append("SELECT count(1) as count, sum(d.salePrice * (0-d.useTotalDose)) as totalPrice ");
                    sqlRefund.append(" FROM cdr_recipe r INNER JOIN cdr_recipeorder o ON r.OrderCode = o.OrderCode INNER JOIN cdr_recipedetail d ON r.recipeId = d.recipeId LEFT JOIN base_saledruglist s ON d.drugId = s.drugId and o.EnterpriseId = s.OrganID");
                    sqlRefund.append(" WHERE r.GiveMode = 1 and (o.refundflag = 1 and o.refundTime BETWEEN :startTime  AND :endTime) ");
                } else {
                    sqlPay.append("SELECT count(1) as count, sum(o.RecipeFee) as totalPrice");
                    sqlPay.append(" FROM cdr_recipe r INNER JOIN cdr_recipeorder o ON r.OrderCode = o.OrderCode ");
                    sqlPay.append(" WHERE r.GiveMode = 1 and ((o.payflag = 1 OR o.refundflag = 1) and o.paytime BETWEEN :startTime  AND :endTime ) ");
                    sqlRefund.append("SELECT count(1) as count, sum(0-o.RecipeFee) as totalPrice");
                    sqlRefund.append(" FROM cdr_recipe r INNER JOIN cdr_recipeorder o ON r.OrderCode = o.OrderCode ");
                    sqlRefund.append(" WHERE r.GiveMode = 1 and (o.refundflag = 1 and o.refundTime BETWEEN :startTime  AND :endTime) ");
                }
                if (organId != null) {
                    sqlPay.append(" and r.clinicOrgan = :organId");
                    sqlRefund.append(" and r.clinicOrgan = :organId");
                } else if (organIds != null && organIds.size() > 0) {
                    sqlPay.append(" and r.clinicOrgan in (:organIds)");
                    sqlRefund.append(" and r.clinicOrgan in (:organIds)");
                }
                if (depId != null) {
                    sqlPay.append(" and o.EnterpriseId = :depId");
                    sqlRefund.append(" and o.EnterpriseId = :depId");
                }
                if (drugId != null) {
                    sqlPay.append(" and d.drugId = :drugId and d.status = 1 ");
                    sqlRefund.append(" and d.drugId = :drugId and d.status = 1 ");
                }

                //退款的处方单需要展示两条记录，所以要在取一次
                sql.append("SELECT sum(count), sum(totalPrice) as totalPrice  from ( ").append(sqlPay).append(" UNION ALL ").append(sqlRefund).append(" ) b");
                Query q = ss.createSQLQuery(sql.toString());
                q.setParameter("startTime", startTime);
                q.setParameter("endTime", endTime);
                if (organId != null) {
                    q.setParameter("organId", organId);
                } else if (organIds != null && organIds.size() > 0) {
                    q.setParameterList("organIds", organIds);
                }
                if (depId != null) {
                    q.setParameter("depId", depId);
                }
                if (drugId != null) {
                    q.setParameter("drugId", drugId);
                }
                List<Object[]> result = q.list();
                Map<String, Object> vo = new HashMap ();
                if (CollectionUtils.isNotEmpty(result)){
                    vo.put("totalNum", result.get(0)[0]);
                    vo.put("totalPrice", result.get(0)[1]);
                }
                setResult(vo);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    /**
     * 根据日期获取电子处方药企配送药品
     *
     * @param startTime 开始时间
     * @param endTime 截止时间
     * @param organId 机构ID
     * @param depId 药企ID
     * @return
     */
    public List<Map<String, Object>> queryrecipeDrug(Date startTime, Date endTime, Integer organId, List<Integer> organIds, Integer depId, Integer recipeId, String orderColumn, String orderType, int start, int limit){
        HibernateStatelessResultAction<List<Map<String, Object>>> action = new AbstractHibernateStatelessResultAction<List<Map<String, Object>>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();

                if (recipeId != null) {
                    hql.append("SELECT d.saleDrugCode, d.drugName, d.producer, s.drugSpec, d.DrugUnit, d.salePrice as price, sum(d.useTotalDose) as dose, sum(d.salePrice * d.useTotalDose) as totalPrice, s.organId, s.DrugId ");
                } else{
                    hql.append("SELECT d.saleDrugCode, d.drugName, d.producer, s.drugSpec, d.DrugUnit, d.salePrice as price, sum(d.useTotalDose) as dose, sum(if(o.refundFlag=1,0,d.salePrice) * d.useTotalDose) as totalPrice, s.organId, s.DrugId ");
                }
                hql.append(" FROM cdr_recipe r INNER JOIN cdr_recipedetail d ON r.recipeId = d.recipeId INNER JOIN cdr_recipeorder o ON o.OrderCode = r.OrderCode ");
                hql.append("  LEFT JOIN base_saledruglist s ON d.drugId = s.drugId and o.EnterpriseId = s.OrganID ");
                hql.append(" WHERE r.GiveMode = 1 and d.status = 1 and ((o.payflag = 1 and o.paytime BETWEEN :startTime  AND :endTime ) OR (o.refundflag = 1 and o.refundTime BETWEEN :startTime  AND :endTime)) ");
                if (organId != null) {
                    hql.append(" and r.clinicOrgan = :organId");
                } else if (organIds != null && organIds.size() > 0) {
                    hql.append(" and r.clinicOrgan in (:organIds)");
                }
                if (depId != null) {
                    hql.append(" and o.EnterpriseId = :depId");
                }
                if (recipeId != null) {
                    hql.append(" and r.recipeId = :recipeId");
                }
                hql.append(" GROUP BY s.drugId, s.OrganID");
                if (orderColumn != null) {
                    hql.append(" order by " + orderColumn + " ");
                }
                if(orderType != null){
                    hql.append(orderType);
                }
                Query q = ss.createSQLQuery(hql.toString());
                q.setParameter("startTime", startTime);
                q.setParameter("endTime", endTime);
                if (organId != null) {
                    q.setParameter("organId", organId);
                } else if (organIds != null && organIds.size() > 0) {
                    q.setParameterList("organIds", organIds);
                }
                if (depId != null) {
                    q.setParameter("depId", depId);
                }
                if (recipeId != null) {
                    q.setParameter("recipeId", recipeId);
                }

                q.setFirstResult(start);
                q.setMaxResults(limit);
                List<Object[]> result = q.list();
                List<Map<String, Object>> backList = new ArrayList<>();

                Set<String> mpiIds = Sets.newHashSet();
                if (CollectionUtils.isNotEmpty(result)){

                    //获取全部身份证信息
                    PatientService patientService = BasicAPI.getService(PatientService.class);
                    Map<String, String> patientBeanMap = Maps.newHashMap();
                    for (Object[] obj : result) {
                        if(obj[2] != null){
                            mpiIds.add((String)obj[2]);
                        }
                    }

                    if(0 < mpiIds.size()){
                        List<PatientDTO> patientBeanList = patientService.findByMpiIdIn(new ArrayList<String>(mpiIds));
                        for (PatientDTO p : patientBeanList) {
                            patientBeanMap.put(p.getMpiId(), p.getCardId());
                        }
                    }

                    Map<String, Object> vo;
                    for (Object[] objs : result) {
                        vo = new HashMap<String, Object>();
                        vo.put("drugCode", objs[0] == null ? null : (String)objs[0]);
                        vo.put("drugName", objs[1] == null ? null : (String)objs[1]);
                        vo.put("producer", objs[2] == null ? null : (String)objs[2]);
                        vo.put("drugSpec", objs[3] == null ? null : (String)objs[3]);
                        vo.put("drugUnit", objs[4] == null ? null : (String)objs[4]);
                        vo.put("price", objs[5] == null ? null : Double.valueOf(objs[5]+""));
                        vo.put("dose", objs[6] == null ? null : objs[6].toString());
                        vo.put("totalPrice", objs[7] == null ? null : Double.valueOf(objs[7]+""));
                        vo.put("enterpriseId", objs[8] == null ? null : objs[8].toString());
                        vo.put("DrugId", objs[9] == null ? null : objs[9].toString());
                        backList.add(vo);
                    }
                }
                setResult(backList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据日期获取电子处方药企配送药品
     *
     * @param startTime 开始时间
     * @param endTime 截止时间
     * @param organId 机构ID
     * @param depId 药企ID
     * @return
     */
    public Map<String, Object> queryrecipeDrugtotal(Date startTime, Date endTime, Integer organId, List<Integer> organIds, Integer depId, Integer recipeId){
        HibernateStatelessResultAction<Map<String, Object>> action = new AbstractHibernateStatelessResultAction<Map<String, Object>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                if (recipeId != null) {
                    hql.append("SELECT count(1), sum(totalPrice) from (SELECT sum(d.salePrice * d.useTotalDose) as totalPrice ");
                } else{
                    hql.append("SELECT count(1), sum(totalPrice) from (SELECT sum(if(o.refundFlag=1,0,d.salePrice) * d.useTotalDose) as totalPrice ");
                }
                hql.append(" FROM cdr_recipe r INNER JOIN cdr_recipedetail d ON r.recipeId = d.recipeId INNER JOIN cdr_recipeorder o ON o.OrderCode = r.OrderCode ");
                hql.append("  LEFT JOIN base_saledruglist s ON d.drugId = s.drugId and o.EnterpriseId = s.OrganID ");
                hql.append(" WHERE r.GiveMode = 1 and d.status = 1 and ((o.payflag = 1 and o.paytime BETWEEN :startTime  AND :endTime ) OR (o.refundflag = 1 and o.refundTime BETWEEN :startTime  AND :endTime)) ");
                if (organId != null) {
                    hql.append(" and r.clinicOrgan = :organId");
                } else if (organIds != null && organIds.size() > 0) {
                    hql.append(" and r.clinicOrgan in (:organIds)");
                }
                if (depId != null) {
                    hql.append(" and o.EnterpriseId = :depId");
                }
                if (recipeId != null) {
                    hql.append(" and r.recipeId = :recipeId");
                }
                hql.append(" GROUP BY s.drugId, s.OrganID) a");
                Query q = ss.createSQLQuery(hql.toString());
                q.setParameter("startTime", startTime);
                q.setParameter("endTime", endTime);
                if (organId != null) {
                    q.setParameter("organId", organId);
                } else if (organIds != null && organIds.size() > 0) {
                    q.setParameterList("organIds", organIds);
                }
                if (depId != null) {
                    q.setParameter("depId", depId);
                }
                if (recipeId != null) {
                    q.setParameter("recipeId", recipeId);
                }
                List<Object[]> result = q.list();

                Map<String, Object> vo = new HashMap ();
                if (CollectionUtils.isNotEmpty(result)){
                    vo.put("totalNum", result.get(0)[0]);
                    vo.put("totalPrice", result.get(0)[1]);
                }
                setResult(vo);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据日期获取电子处方药企配送药品
     *
     * @param startTime 开始时间
     * @param endTime 截止时间
     * @param organId 机构ID
     * @param depId 药企ID
     * @return
     */
    public List<Map<String, Object>> queryrecipeDrugO(Date startTime, Date endTime, Integer organId, List<Integer> organIds, Integer depId, Integer recipeId, String orderColumn, String orderType, int start, int limit){
        HibernateStatelessResultAction<List<Map<String, Object>>> action = new AbstractHibernateStatelessResultAction<List<Map<String, Object>>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {

                StringBuilder sql = new StringBuilder();
                StringBuilder sqlPay = new StringBuilder();
                StringBuilder sqlRefund = new StringBuilder();
                sqlPay.append("SELECT d.saleDrugCode, d.drugName, d.producer, s.drugSpec, d.DrugUnit, d.salePrice as price, sum(d.useTotalDose) as dose, d.salePrice * sum(d.useTotalDose) as totalPrice, s.organId, s.DrugId ");
                sqlPay.append(" FROM cdr_recipe r INNER JOIN cdr_recipedetail d ON r.recipeId = d.recipeId INNER JOIN cdr_recipeorder o ON o.OrderCode = r.OrderCode ");
                sqlPay.append("  LEFT JOIN base_saledruglist s ON d.drugId = s.drugId and o.EnterpriseId = s.OrganID ");
                sqlPay.append(" WHERE r.GiveMode = 1 and d.status = 1 and ((o.payflag = 1 OR o.refundflag = 1) and o.paytime BETWEEN :startTime  AND :endTime ) ");
                sqlRefund.append("SELECT d.saleDrugCode, d.drugName, d.producer, s.drugSpec, d.DrugUnit, d.salePrice as price, sum(d.useTotalDose) as dose, d.salePrice * sum(0-d.useTotalDose) as totalPrice, s.organId, s.DrugId ");
                sqlRefund.append(" FROM cdr_recipe r INNER JOIN cdr_recipedetail d ON r.recipeId = d.recipeId INNER JOIN cdr_recipeorder o ON o.OrderCode = r.OrderCode ");
                sqlRefund.append("  LEFT JOIN base_saledruglist s ON d.drugId = s.drugId and o.EnterpriseId = s.OrganID ");
                sqlRefund.append(" WHERE r.GiveMode = 1 and d.status = 1 and (o.refundflag = 1 and o.refundTime BETWEEN :startTime  AND :endTime) ");
                if (organId != null) {
                    sqlPay.append(" and r.clinicOrgan = :organId");
                    sqlRefund.append(" and r.clinicOrgan = :organId");
                } else if (organIds != null && organIds.size() > 0) {
                    sqlPay.append(" and r.clinicOrgan in (:organIds)");
                    sqlRefund.append(" and r.clinicOrgan in (:organIds)");
                }
                if (depId != null) {
                    sqlPay.append(" and o.EnterpriseId = :depId");
                    sqlRefund.append(" and o.EnterpriseId = :depId");
                }
                if (recipeId != null) {
                    sqlPay.append(" and r.recipeId = :recipeId");
                    sqlRefund.append(" and r.recipeId = :recipeId");
                }
                sqlPay.append(" GROUP BY s.drugId, s.OrganID");
                sqlRefund.append(" GROUP BY s.drugId, s.OrganID");
                //退款的处方单需要展示两条记录，所以要在取一次
                sql.append("SELECT * from ( ").append(sqlPay).append(" UNION ALL ").append(sqlRefund).append(" ) a");
                if (orderColumn != null) {
                    sql.append(" order by " + orderColumn + " ");
                }
                if(orderType != null){
                    sql.append(orderType);
                }
                Query q = ss.createSQLQuery(sql.toString());
                q.setParameter("startTime", startTime);
                q.setParameter("endTime", endTime);
                if (organId != null) {
                    q.setParameter("organId", organId);
                } else if (organIds != null && organIds.size() > 0) {
                    q.setParameterList("organIds", organIds);
                }
                if (depId != null) {
                    q.setParameter("depId", depId);
                }
                if (recipeId != null) {
                    q.setParameter("recipeId", recipeId);
                }

                q.setFirstResult(start);
                q.setMaxResults(limit);
                List<Object[]> result = q.list();
                List<Map<String, Object>> backList = new ArrayList<>();

                Set<String> mpiIds = Sets.newHashSet();
                if (CollectionUtils.isNotEmpty(result)){

                    //获取全部身份证信息
                    PatientService patientService = BasicAPI.getService(PatientService.class);
                    Map<String, String> patientBeanMap = Maps.newHashMap();
                    for (Object[] obj : result) {
                        if(obj[2] != null){
                            mpiIds.add((String)obj[2]);
                        }
                    }

                    if(0 < mpiIds.size()){
                        List<PatientDTO> patientBeanList = patientService.findByMpiIdIn(new ArrayList<String>(mpiIds));
                        for (PatientDTO p : patientBeanList) {
                            patientBeanMap.put(p.getMpiId(), p.getCardId());
                        }
                    }

                    Map<String, Object> vo;
                    for (Object[] objs : result) {
                        vo = new HashMap<String, Object>();
                        vo.put("drugCode", objs[0] == null ? null : (String)objs[0]);
                        vo.put("drugName", objs[1] == null ? null : (String)objs[1]);
                        vo.put("producer", objs[2] == null ? null : (String)objs[2]);
                        vo.put("drugSpec", objs[3] == null ? null : (String)objs[3]);
                        vo.put("drugUnit", objs[4] == null ? null : (String)objs[4]);
                        vo.put("price", objs[5] == null ? null : Double.valueOf(objs[5]+""));
                        vo.put("dose", objs[6] == null ? null : objs[6].toString());
                        vo.put("totalPrice", objs[7] == null ? null : Double.valueOf(objs[7]+""));
                        vo.put("enterpriseId", objs[8] == null ? null : objs[8].toString());
                        vo.put("DrugId", objs[9] == null ? null : objs[9].toString());
                        backList.add(vo);
                    }
                }
                setResult(backList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据日期获取电子处方药企配送药品
     *
     * @param startTime 开始时间
     * @param endTime 截止时间
     * @param organId 机构ID
     * @param depId 药企ID
     * @return
     */
    public Map<String, Object> queryrecipeDrugtotalO(Date startTime, Date endTime, Integer organId, List<Integer> organIds, Integer depId, Integer recipeId){
        HibernateStatelessResultAction<Map<String, Object>> action = new AbstractHibernateStatelessResultAction<Map<String, Object>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {


                StringBuilder sql = new StringBuilder();
                StringBuilder sqlPay = new StringBuilder();
                StringBuilder sqlRefund = new StringBuilder();
                sqlPay.append("SELECT count(1) as count, sum(totalPrice) as totalPrice from (SELECT d.salePrice * sum(d.useTotalDose) as totalPrice ");
                sqlPay.append(" FROM cdr_recipe r INNER JOIN cdr_recipedetail d ON r.recipeId = d.recipeId INNER JOIN cdr_recipeorder o ON o.OrderCode = r.OrderCode ");
                sqlPay.append("  LEFT JOIN base_saledruglist s ON d.drugId = s.drugId and o.EnterpriseId = s.OrganID ");
                sqlPay.append(" WHERE r.GiveMode = 1 and d.status = 1 and ((o.payflag = 1 OR o.refundflag = 1) and o.paytime BETWEEN :startTime  AND :endTime ) ");
                sqlRefund.append("SELECT count(1) as count, sum(totalPrice) as totalPrice from (SELECT d.salePrice * sum(0-d.useTotalDose) as totalPrice ");
                sqlRefund.append(" FROM cdr_recipe r INNER JOIN cdr_recipedetail d ON r.recipeId = d.recipeId INNER JOIN cdr_recipeorder o ON o.OrderCode = r.OrderCode ");
                sqlRefund.append("  LEFT JOIN base_saledruglist s ON d.drugId = s.drugId and o.EnterpriseId = s.OrganID ");
                sqlRefund.append(" WHERE r.GiveMode = 1 and d.status = 1 and (o.refundflag = 1 and o.refundTime BETWEEN :startTime  AND :endTime) ");
                if (organId != null) {
                    sqlPay.append(" and r.clinicOrgan = :organId");
                    sqlRefund.append(" and r.clinicOrgan = :organId");
                } else if (organIds != null && organIds.size() > 0) {
                    sqlPay.append(" and r.clinicOrgan in (:organIds)");
                    sqlRefund.append(" and r.clinicOrgan in (:organIds)");
                }
                if (depId != null) {
                    sqlPay.append(" and o.EnterpriseId = :depId");
                    sqlRefund.append(" and o.EnterpriseId = :depId");
                }
                if (recipeId != null) {
                    sqlPay.append(" and r.recipeId = :recipeId");
                    sqlRefund.append(" and r.recipeId = :recipeId");
                }
                sqlPay.append(" GROUP BY s.drugId, s.OrganID) a");
                sqlRefund.append(" GROUP BY s.drugId, s.OrganID) a");
                //退款的处方单需要展示两条记录，所以要在取一次
                sql.append("SELECT sum(count), sum(totalPrice) as totalPrice  from ( ").append(sqlPay).append(" UNION ALL ").append(sqlRefund).append(" ) b");

                Query q = ss.createSQLQuery(sql.toString());
                q.setParameter("startTime", startTime);
                q.setParameter("endTime", endTime);
                if (organId != null) {
                    q.setParameter("organId", organId);
                } else if (organIds != null && organIds.size() > 0) {
                    q.setParameterList("organIds", organIds);
                }
                if (depId != null) {
                    q.setParameter("depId", depId);
                }
                if (recipeId != null) {
                    q.setParameter("recipeId", recipeId);
                }
                List<Object[]> result = q.list();

                Map<String, Object> vo = new HashMap ();
                if (CollectionUtils.isNotEmpty(result)){
                    vo.put("totalNum", result.get(0)[0]);
                    vo.put("totalPrice", result.get(0)[1]);
                }
                setResult(vo);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public List<BillBusFeeVo> findRecipeFeeList(final RecipeBillRequest recipeBillRequest) {
        HibernateStatelessResultAction<List<BillBusFeeVo>> action = new AbstractHibernateStatelessResultAction<List<BillBusFeeVo>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuffer paySql = new StringBuffer();
                paySql.append("SELECT  r.ClinicOrgan, count(*), sum(IFNULL(o.ActualPrice, 0)) ");
                paySql.append(" FROM cdr_recipe r, cdr_recipeorder o ");
                paySql.append(" WHERE r.OrderCode = o.OrderCode AND o.PayFlag = 1 ");
                paySql.append(" AND  PayTime >= :startTime AND PayTime < :endTime ");
                paySql.append(" GROUP BY r.ClinicOrgan ");
                Query paySqlQuery = ss.createSQLQuery(paySql.toString());
                paySqlQuery.setParameter("startTime", recipeBillRequest.getStartTime());
                paySqlQuery.setParameter("endTime", recipeBillRequest.getEndTime());
                paySqlQuery.setFirstResult(0);
                paySqlQuery.setMaxResults(0);
                List<Object[]> payList = paySqlQuery.list();

                StringBuffer refundSql = new StringBuffer();
                refundSql.append("SELECT r.ClinicOrgan, count(*), sum(IFNULL(o.ActualPrice, 0)) ");
                refundSql.append(" FROM cdr_recipe r, cdr_recipeorder o ");
                refundSql.append(" WHERE r.OrderCode = o.OrderCode AND o.refundFlag = 1 ");
                refundSql.append(" AND  refundTime >= :startTime AND refundTime < :endTime ");
                refundSql.append(" GROUP BY r.ClinicOrgan ");
                Query refundSqlQuery = ss.createSQLQuery(refundSql.toString());
                refundSqlQuery.setParameter("startTime", recipeBillRequest.getStartTime());
                refundSqlQuery.setParameter("endTime", recipeBillRequest.getEndTime());
                refundSqlQuery.setFirstResult(0);
                refundSqlQuery.setMaxResults(0);
                List<Object[]> refundList = refundSqlQuery.list();
                setResult(convertToBBFVList(recipeBillRequest.getAcctDate(), payList, refundList));
            }

            private List<BillBusFeeVo> convertToBBFVList(String acctDate, List<Object[]> payList, List<Object[]> refundList) {
                List<BillBusFeeVo> voList = Lists.newArrayList();
                for(Object[] pos : payList){
                    BillBusFeeVo vo = new BillBusFeeVo();
                    vo.setAcctMonth(acctDate.substring(0, 7));
                    vo.setAcctDate(acctDate);
                    vo.setFeeType(BillBusFeeTypeEnum.RECIPE_ACTUAL_FEE.id());
                    vo.setFeeTypeName(BillBusFeeTypeEnum.RECIPE_ACTUAL_FEE.text());
                    vo.setOrganId(ConversionUtils.convert(pos[0], Integer.class));
                    vo.setPayCount(ConversionUtils.convert(pos[1], Integer.class));
                    vo.setPayAmount(ConversionUtils.convert(pos[2], Double.class));
                    vo.setRefundAmount(0.0);
                    vo.setRefundCount(0);
                    for(Object[] ros : refundList){
                        Integer xo = ConversionUtils.convert(ros[0], Integer.class);
                        if(vo.getOrganId().equals(xo)) {
                            vo.setRefundCount(ConversionUtils.convert(pos[1], Integer.class));
                            vo.setRefundAmount(ConversionUtils.convert(pos[2], Double.class));
                            break;
                        }
                    }
                    vo.setAggregateAmount(vo.getPayAmount()-vo.getRefundAmount());
                    vo.setCreateTime(new Date());
                    vo.setUpdateTime(new Date());
                    voList.add(vo);
                }
                return voList;
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public List<BillDrugFeeVo> findDrugFeeList(final RecipeBillRequest recipeBillRequest) {
        HibernateStatelessResultAction<List<BillDrugFeeVo>> action = new AbstractHibernateStatelessResultAction<List<BillDrugFeeVo>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuffer sql = new StringBuffer();
                sql.append("SELECT r.ClinicOrgan, r.enterpriseId, d.name, r.RecipeType, sum(o.RecipeFee) ");
                sql.append(" FROM cdr_recipe r INNER JOIN cdr_recipeorder o ON (r.OrderCode = o.OrderCode) LEFT JOIN cdr_drugsenterprise d ON (r.EnterpriseId = d.id) ");
                sql.append(" WHERE r.OrderCode = o.OrderCode AND o.Effective = 1 AND o.PayFlag=1 ");
                sql.append(" AND PayTime >= :startTime AND PayTime < :endTime ");
                sql.append(" GROUP BY r.ClinicOrgan, r.enterpriseId, d.name, r.RecipeType");
                Query sqlQuery = ss.createSQLQuery(sql.toString());
                sqlQuery.setParameter("startTime", recipeBillRequest.getStartTime());
                sqlQuery.setParameter("endTime", recipeBillRequest.getEndTime());
                sqlQuery.setFirstResult(0);
                sqlQuery.setMaxResults(0);
                List<Object[]> list = sqlQuery.list();
                setResult(convertToBDFVList(recipeBillRequest.getAcctDate(), list));
            }

            private List<BillDrugFeeVo> convertToBDFVList(String acctDate, List<Object[]> list) {
                List<BillDrugFeeVo> voList = Lists.newArrayList();
                for(Object[] objs : list){
                    BillDrugFeeVo vo = new BillDrugFeeVo();
                    vo.setAcctMonth(acctDate.substring(0, 8));
                    vo.setAcctDate(acctDate);
                    vo.setOrganId(ConversionUtils.convert(objs[0], Integer.class));
                    vo.setDrugCompany(ConversionUtils.convert(objs[1], Integer.class));
                    vo.setDrugCompanyName(ConversionUtils.convert(objs[2], String.class));
                    vo.setDrugType(ConversionUtils.convert(objs[3], Integer.class));
                    vo.setDrugTypeName(DRUG_TYPE_TABLE.get(vo.getDrugType()));
                    vo.setAmount(ConversionUtils.convert(objs[4], Double.class));
                    vo.setCreateTime(new Date());
                    vo.setUpdateTime(new Date());
                    voList.add(vo);
                }
                return voList;
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

}
