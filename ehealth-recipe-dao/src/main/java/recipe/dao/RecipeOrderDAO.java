package recipe.dao;

import com.ngari.recipe.entity.RecipeOrder;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import eh.billcheck.vo.BillRecipeDetailVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import recipe.constant.RecipeBussConstant;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/2/13.
 */
@RpcSupportDAO
public abstract class RecipeOrderDAO extends HibernateSupportDelegateDAO<RecipeOrder> {

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
     * 根据日期查询订单支付和退款信息
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
                hql.append("o.OtherFee, o.RecipeFee, o.CouponFee, o.TotalFee, o.FundAmount, d.name, 0 as billType from ");
                hql.append("cdr_recipe r INNER JOIN cdr_recipeorder o on r.OrderCode = o.OrderCode LEFT JOIN cdr_drugsenterprise d on d.id = o.EnterpriseId ");
                hql.append("where o.payFlag = 1 and o.payTime between :startTime and :endTime and o.Effective = 1 ");
                hql.append("UNION ALL ");
                hql.append("select r.recipeId, r.doctor, o.MpiId, o.refundTime as PayTime, o.OrganId, r.Depart, o.OutTradeNo, ");
                hql.append("o.OrderType, r.GiveMode, o.PayFlag, o.RegisterFee, o.ExpressFee, o.DecoctionFee, o.AuditFee, ");
                hql.append("o.OtherFee, o.RecipeFee, o.CouponFee, o.TotalFee, o.FundAmount, d.name, 1 as billType from ");
                hql.append("cdr_recipe r INNER JOIN cdr_recipeorder o on r.OrderCode = o.OrderCode LEFT JOIN cdr_drugsenterprise d on d.id = o.EnterpriseId ");
                hql.append("where (o.refundFlag is Not Null and o.refundFlag <> 0) and o.refundTime between :startTime and :endTime ");
                hql.append("UNION ALL ");
                hql.append("select r.recipeId, r.doctor, o.MpiId, o.PayTime, o.OrganId, r.Depart, o.OutTradeNo, ");
                hql.append("o.OrderType, r.GiveMode, o.PayFlag, o.RegisterFee, o.ExpressFee, o.DecoctionFee, o.AuditFee, ");
                hql.append("o.OtherFee, o.RecipeFee, o.CouponFee, o.TotalFee, o.FundAmount, d.name, 0 as billType from  ");
                hql.append("cdr_recipe r INNER JOIN cdr_recipeorder o on r.OrderCode = o.OrderCode LEFT JOIN cdr_drugsenterprise d on d.id = o.EnterpriseId ");
                hql.append("where (o.refundFlag is Not Null and o.refundFlag <> 0) and o.payFlag <>1 and o.payTime between :startTime and :endTime ");
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
                        vo.setDrugCompany(objs[5] == null ? null : (Integer)objs[5]);
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

}
