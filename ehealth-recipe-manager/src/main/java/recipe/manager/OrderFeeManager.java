package recipe.manager;

import com.ngari.base.organconfig.model.OrganConfigBean;
import com.ngari.his.recipe.mode.RecipeCashPreSettleInfo;
import com.ngari.his.recipe.mode.RecipeCashPreSettleReqTO;
import com.ngari.his.visit.mode.NeedPaymentRecipeReqTo;
import com.ngari.his.visit.mode.NeedPaymentRecipeResTo;
import com.ngari.platform.recipe.mode.EnterpriseResTo;
import com.ngari.recipe.dto.OrderFeeSetCondition;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.entity.*;
import coupon.api.vo.Coupon;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.client.*;
import recipe.constant.ErrorCode;
import recipe.constant.ParameterConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.ReviewTypeConstant;
import recipe.dao.DrugDistributionPriceDAO;
import recipe.enumerate.status.RecipeSourceTypeEnum;
import recipe.enumerate.type.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订单费用计算
 *
 * @author yinsheng
 * @date 2022\2\14 0030 17:22
 */
@Service
public class OrderFeeManager extends BaseManager {

    @Autowired
    private RecipeRedisClient recipeRedisClient;
    @Autowired
    private RecipeDetailManager recipeDetailManager;
    @Autowired
    private DrugDistributionPriceDAO drugDistributionPriceDAO;
    @Autowired
    private CouponClient couponClient;
    @Autowired
    private OfflineRecipeClient offlineRecipeClient;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private ConsultClient consultClient;

    public RecipeOrder setOrderFee(RecipeOrder order, List<Recipe> recipeList, OrderFeeSetCondition condition) {
        if (null == order || CollectionUtils.isEmpty(recipeList)) {
            return order;
        }
        //设置挂号费
        setRegisterFee(order);
        //设置审方费
        setAuditFee(order, recipeList);
        //设置其他费用
        setOtherFee(order);
        //设置处方费用
        setRecipeFee(order, recipeList, condition.getPayModeSupportFlag());
        //设置代煎费
        setDecoctionFee(order, recipeList);
        //设置中医辨证论治费
        setTcmFee(order, recipeList);
        // 设置处方代缴费用
        setRecipePaymentFee(order, recipeList);
        //设置配送费
        return order;

    }

    /**
     * 处方代缴费用
     *
     * @param order
     * @param recipeList
     */
    @LogRecord
    public void setRecipePaymentFee(RecipeOrder order, List<Recipe> recipeList) {
        // 获取机构配置是否需要代缴费用
        Boolean isNeedRecipePaymentFee = configurationClient.getValueBooleanCatch(order.getOrganId(), "isNeedRecipePaymentFee", false);
        logger.info("setRecipePaymentFee isNeedRecipePaymentFee={}",isNeedRecipePaymentFee);
        if (!isNeedRecipePaymentFee) {
            return;
        }
        // 获取处方 类型
        Recipe recipe = recipeList.get(0);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());

        NeedPaymentRecipeReqTo needPayment = new NeedPaymentRecipeReqTo();
        needPayment.setCardNo(recipeExtend.getCardNo());
        needPayment.setCardType(recipeExtend.getCardType());
        needPayment.setDepartId(recipe.getDepart());
        needPayment.setOrganId(recipe.getClinicOrgan());
        needPayment.setDoctorId(recipe.getDoctor());
        needPayment.setPatientID(recipe.getPatientID());
        needPayment.setPatientName(recipe.getPatientName());
        needPayment.setRegisterID(recipeExtend.getRegisterID());
        List<String> code = recipeList.stream().map(Recipe::getRecipeCode).collect(Collectors.toList());
        needPayment.setRecipeCode(code);
        NeedPaymentRecipeResTo recipePaymentFee = consultClient.getRecipePaymentFee(needPayment);
        if (Objects.isNull(recipePaymentFee)) {
            return;
        }
        if (recipePaymentFee.getRegisterFee().compareTo(BigDecimal.ZERO) > 0) {
            // 挂号费
            order.setRegisterFee(recipePaymentFee.getRegisterFee());
            order.setRegisterFeeNo(recipePaymentFee.getRegisterFeeNo());
        }
        if (recipePaymentFee.getTcmFee().compareTo(BigDecimal.ZERO) > 0) {
            // 中医辨证费
            order.setTcmFee(recipePaymentFee.getTcmFee());
            order.setTcmFeeNo(recipePaymentFee.getTcmFeeNo());
        }
    }


    /**
     * 订单设置挂号费
     *
     * @param order 订单
     */
    public void setRegisterFee(RecipeOrder order) {
        //获取挂号费配置项
        BigDecimal registerFee = configurationClient.getValueCatchReturnBigDecimal(order.getOrganId(), "priceForRecipeRegister", null);
        if (null != registerFee) {
            order.setRegisterFee(registerFee);
        } else {
            order.setRegisterFee(new BigDecimal(recipeRedisClient.getParam(ParameterConstant.KEY_RECIPE_REGISTER_FEE, "0")));
        }
    }

    /**
     * 订单设置审方费
     *
     * @param order      订单
     * @param recipeList 处方列表
     */
    public void setAuditFee(RecipeOrder order, List<Recipe> recipeList) {
        if (CollectionUtils.isEmpty(recipeList)) {
            return;
        }
        //设置审方费用
        Recipe firstRecipe = recipeList.get(0);
        if (ReviewTypeConstant.Not_Need_Check.equals(firstRecipe.getReviewType())) {
            //不需要审方
            return;
        }
        double auditFee = ReviewTypeConstant.Not_Need_Check == firstRecipe.getReviewType() ? 0d : configurationClient.getValueCatchReturnDouble(firstRecipe.getClinicOrgan(), ParameterConstant.KEY_AUDITFEE, 0d);
        //如果是合并处方单，审方费得乘以处方单数
        order.setAuditFee(BigDecimal.valueOf(auditFee).multiply(BigDecimal.valueOf(recipeList.size())));
    }

    /**
     * 设置其他费用
     *
     * @param order 订单
     */
    public void setOtherFee(RecipeOrder order) {
        //设置其他服务费用
        BigDecimal otherFee = configurationClient.getValueCatchReturnBigDecimal(order.getOrganId(), ParameterConstant.KEY_OTHERFEE, BigDecimal.ZERO);
        order.setOtherFee(otherFee);
    }

    /**
     * 订单设置处方费用：默认为医院药品总费用，如果为药企配送，药品单价根据药企药企结算类型决定
     *
     * @param order              订单
     * @param recipeList         处方列表
     * @param payModeSupportFlag 是否为配送类型
     */
    public void setRecipeFee(RecipeOrder order, List<Recipe> recipeList, Boolean payModeSupportFlag) {
        BigDecimal recipeFee = BigDecimal.ZERO;
        List<BigDecimal> totalMoneyList = recipeList.stream().map(Recipe::getTotalMoney).collect(Collectors.toList());
        List<Integer> recipeIdList = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        //默认的药品总费用为机构药品价格计算的总费用
        totalMoneyList.forEach(item -> {
            if (null != item) {
                recipeFee.add(item);
            }
        });
        order.setRecipeFee(recipeFee);
        //购药方式为配送相关并且药企ID不为空
        if (payModeSupportFlag && null != order.getEnterpriseId()) {
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
            if (null != drugsEnterprise && SettlementModeTypeEnum.SETTLEMENT_MODE_ENT.getType().equals(drugsEnterprise.getSettlementMode())) {
                BigDecimal totalMoney = BigDecimal.ZERO;
                //药品费用取药企药品价格计算
                List<Recipedetail> recipeDetailList = recipeDetailManager.findRecipeDetails(recipeIdList);
                List<Integer> drugList = recipeDetailList.stream().map(Recipedetail::getDrugId).collect(Collectors.toList());
                List<SaleDrugList> saleDrugList = saleDrugListDAO.findByOrganIdAndDrugIds(drugsEnterprise.getId(), drugList);
                Map<Integer, SaleDrugList> saleDrugListMap = saleDrugList.stream().collect(Collectors.toMap(SaleDrugList::getDrugId, a -> a, (k1, k2) -> k1));
                for (Recipedetail recipeDetail : recipeDetailList) {
                    BigDecimal price = saleDrugListMap.get(recipeDetail.getDrugId()).getPrice();
                    totalMoney = totalMoney.add(price.multiply(new BigDecimal(recipeDetail.getUseTotalDose())).divide(BigDecimal.ONE, 2, BigDecimal.ROUND_HALF_UP));
                }
                order.setRecipeFee(totalMoney);
            }
        }
    }

    /**
     * TODO 膏方费目前没有用到
     *
     * @param order        订单
     * @param recipeIdList 处方列表
     */
    public void setGfFee(RecipeOrder order, List<Integer> recipeIdList) {
        OrganConfigBean organConfig = organClient.getOrganConfigBean(order.getOrganId());
        BigDecimal gfTotalMoney = BigDecimal.ZERO;
        //制作单价
        BigDecimal gfFeeUnitPrice = organConfig.getRecipeCreamPrice();
        if (null == gfFeeUnitPrice) {
            gfFeeUnitPrice = BigDecimal.ZERO;
        }
        //存在制作单价且大于0
        if (gfFeeUnitPrice.compareTo(BigDecimal.ZERO) == 1) {
            Double totalDose = recipeDetailDAO.getUseTotalDoseByRecipeIds(recipeIdList);
            gfTotalMoney = gfFeeUnitPrice.multiply(BigDecimal.valueOf(totalDose));
        }
    }

    /**
     * 设置订单代煎费
     *
     * @param order      订单
     * @param recipeList 处方列表
     */
    public void setDecoctionFee(RecipeOrder order, List<Recipe> recipeList) {
        //获取中药处方
        List<Recipe> tcmRecipeList = recipeList.stream().filter(recipe -> RecipeBussConstant.RECIPETYPE_TCM.equals(recipe.getRecipeType())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tcmRecipeList)) {
            return;
        }
        BigDecimal decoctionFee = BigDecimal.ZERO;
        //线下中药处方
        List<Recipe> offLineRecipeList = tcmRecipeList.stream().filter(recipe -> RecipeSourceTypeEnum.OFFLINE_RECIPE.getType().equals(recipe.getRecipeSourceType())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(offLineRecipeList)) {
            //说明为线下转线上处方
            decoctionFee = setOffLineRecipeDecoctionFee(order.getOrganId(), offLineRecipeList);
        } else {
            //计算线上处方代煎费，因中药不能和中成药西药合开，所以tcmRecipeList为线上中药列表
            if (null != order.getDecoctionUnitPrice() && (order.getDecoctionUnitPrice().compareTo(BigDecimal.ZERO) == 1)) {
                decoctionFee = setOnLineRecipeDecoctionFee(order.getDecoctionUnitPrice(), tcmRecipeList);
            }
        }
        order.setDecoctionFee(decoctionFee);
    }

    /**
     * 设置中医辨证论证费
     *
     * @param order      订单
     * @param recipeList 处方列表
     */
    public void setTcmFee(RecipeOrder order, List<Recipe> recipeList) {
        //获取中药处方
        List<Recipe> tcmRecipeList = recipeList.stream().filter(recipe -> RecipeBussConstant.RECIPETYPE_TCM.equals(recipe.getRecipeType())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tcmRecipeList)) {
            return;
        }
        BigDecimal tcmFee;
        //线下中药处方
        List<Recipe> offLineRecipeList = tcmRecipeList.stream().filter(recipe -> RecipeSourceTypeEnum.OFFLINE_RECIPE.getType().equals(recipe.getRecipeSourceType())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(offLineRecipeList)) {
            tcmFee = setOffLineTcmFee(order.getOrganId(), offLineRecipeList);
        } else {
            tcmFee = setOnLineTcmFee(order.getOrganId(), tcmRecipeList);
        }
        order.setTcmFee(tcmFee);
        order.setCopyNum(tcmRecipeList.size());
    }

    /**
     * 获取配送费
     *
     * @param enterpriseResTo
     * @return
     */
    public BigDecimal getExpressFee(EnterpriseResTo enterpriseResTo) {
        logger.info("OrderFeeManager getExpressFee enterpriseResTo:{}.", JSONUtils.toString(enterpriseResTo));
        if (null == enterpriseResTo || StringUtils.isEmpty(enterpriseResTo.getDepId())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "参数为空");
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(Integer.parseInt(enterpriseResTo.getDepId()));
        if (null == drugsEnterprise) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "获取药企失败");
        }

        if (ExpressFeeTypeEnum.EXPRESS_FEE_OFFLINE.getType().equals(drugsEnterprise.getExpressFeeType())) {
            //运费从第三方获取
            return enterpriseClient.getExpressFee(enterpriseResTo);
        } else {
            //运费从平台获取
            return getPlatformExpressFee(Integer.parseInt(enterpriseResTo.getDepId()), enterpriseResTo.getAddress());
        }
    }

    /**
     * 设置优惠券费用
     *
     * @param order
     */
    public void setCouponFee(RecipeOrder order) {
        Coupon coupon = couponClient.getCouponById(order.getCouponId(), order.getTotalFee());
        if (null == coupon) {
            return;
        }
        order.setCouponName(coupon.getCouponName());
        order.setCouponFee(coupon.getDiscountAmount());
        order.setCouponDesc(coupon.getCouponDesc());
    }

    /**
     * 设置总费用
     *
     * @param order                订单
     * @param recipeFeeContainFlag 处方费用是否包含在总费用标志
     */
    public void setTotalFee(RecipeOrder order, Boolean recipeFeeContainFlag, Boolean express) {
        BigDecimal full = BigDecimal.ZERO;
        //添加判断，当处方选择购药方式是下载处方，不计算药品费用
        //处方费用
        if (recipeFeeContainFlag) {
            full = full.add(order.getRecipeFee());
        }
        //快递费
        if (null != order.getExpressFee() && ExpressFeePayWayEnum.ONLINE.getType().equals(order.getExpressFeePayWay())) {
            full = full.add(order.getExpressFee());
        }
        //挂号费
        if (null != order.getRegisterFee()) {
            full = full.add(order.getRegisterFee());
        }
        //代煎费
        if (null != order.getDecoctionFee()) {
            full = full.add(order.getDecoctionFee());
        }
        //审方费
        if (null != order.getAuditFee()) {
            full = full.add(order.getAuditFee());
        }
        //其他服务费
        if (null != order.getOtherFee()) {
            full = full.add(order.getOtherFee());
        }
        //中医辨证论治费
        if (null != order.getTcmFee()) {
            full = full.add(order.getTcmFee());
        }
        order.setTotalFee(full.divide(BigDecimal.ONE, 3, RoundingMode.UP));
    }

    /**
     * 设置实际支付费用
     *
     * @param order 订单
     * @return 订单
     */
    public void setActualFee(RecipeOrder order) {
        //首先看一下是否有优惠费用
        if (null != order.getCouponFee() && order.getTotalFee().compareTo(order.getCouponFee()) > -1) {
            order.setActualPrice(order.getTotalFee().subtract(order.getCouponFee()).doubleValue());
        } else {
            order.setActualPrice(order.getTotalFee().doubleValue());
        }
    }

    /**
     * 获取账户支付金额
     *
     * @param totalFee 总金额
     * @param mpiId    处方列表
     * @return 账户支付金额
     */
    public BigDecimal getAccountFee(BigDecimal totalFee, String mpiId, Integer organId) {
        if (null == totalFee || StringUtils.isEmpty(mpiId)) {
            return null;
        }
        PatientDTO patientDTO = patientClient.getPatientDTO(mpiId);
        if (null == patientDTO) {
            return null;
        }
        RecipeCashPreSettleReqTO request = new RecipeCashPreSettleReqTO();
        request.setCertificate(patientDTO.getCertificate());
        request.setTotalFee(totalFee);
        request.setMobile(patientDTO.getMobile());
        request.setPatientName(patientDTO.getPatientName());
        request.setSex("男");
        request.setIdcard(patientDTO.getIdcard());
        request.setBirthday(new Date());
        request.setClinicOrgan(organId);
        RecipeCashPreSettleInfo recipeCashPreSettleInfo = offlineRecipeClient.recipeCashPreSettle(request);
        if (null != recipeCashPreSettleInfo && StringUtils.isNotEmpty(recipeCashPreSettleInfo.getZhzf())) {
            return new BigDecimal(recipeCashPreSettleInfo.getZhzf());
        }
        return null;
    }

    /**
     * 从平台获取快递费 如：341001 先获取341001获取不到，则3410->34
     *
     * @param enterpriseId 药企ID
     * @param addrArea     地址
     * @return 配送费
     */
    public BigDecimal getPlatformExpressFee(Integer enterpriseId, String addrArea) {
        logger.info("OrderFeeManager getPlatformExpressFee enterpriseId:{}, addrArea:{}.", enterpriseId, addrArea);
        DrugDistributionPrice drugDistributionPrice;
        int length = addrArea.length();
        do {
            drugDistributionPrice = drugDistributionPriceDAO.getByEnterpriseIdAndAddrArea(enterpriseId, addrArea.substring(0, length));
            if (null != drugDistributionPrice) {
                break;
            }
        } while ((length = length - 2) > 0);
        logger.info("OrderFeeManager getPlatformExpressFee drugDistributionPrice:{}.", JSONUtils.toString(drugDistributionPrice));
        if (null == drugDistributionPrice) {
            return null;
        }
        return drugDistributionPrice.getDistributionPrice();
    }

    /**
     * 设置线上中医辨证论证费
     *
     * @param organId       机构ID
     * @param tcmRecipeList 中药处方
     * @return 中医辨证论证费
     */
    private BigDecimal setOnLineTcmFee(Integer organId, List<Recipe> tcmRecipeList) {
        BigDecimal recipeTCMPrice = configurationClient.getValueCatchReturnBigDecimal(organId, "recipeTCMPrice", BigDecimal.ZERO);
        if (null != recipeTCMPrice && recipeTCMPrice.compareTo(BigDecimal.ZERO) > -1) {
            return recipeTCMPrice.multiply(new BigDecimal(tcmRecipeList.size()));
        }
        return BigDecimal.ZERO;
    }

    /**
     * 设置线下中医辨证论证费
     *
     * @param organId           机构ID
     * @param offLineRecipeList 中药处方
     * @return 中医辨证论证费
     */
    private BigDecimal setOffLineTcmFee(Integer organId, List<Recipe> offLineRecipeList) {
        List<String> recipeCodeList = offLineRecipeList.stream().map(Recipe::getRecipeCode).collect(Collectors.toList());
        //查询线下处方
        List<HisRecipe> hisRecipeList = hisRecipeDAO.findHisRecipeByRecipeCodeAndClinicOrgan(organId, recipeCodeList);
        BigDecimal tcmFee = BigDecimal.ZERO;
        for (HisRecipe hisRecipe : hisRecipeList) {
            if (null == hisRecipe.getTcmFee()) {
                continue;
            }
            tcmFee = tcmFee.add(hisRecipe.getTcmFee());
        }
        return tcmFee;
    }

    /**
     * 设置线上处方代煎费
     *
     * @param decoctionUnitPrice 代煎单价
     * @param tcmRecipeList      处方列表
     * @return 代煎费
     */
    private BigDecimal setOnLineRecipeDecoctionFee(BigDecimal decoctionUnitPrice, List<Recipe> tcmRecipeList) {
        BigDecimal decoctionFee = BigDecimal.ZERO;
        for (Recipe recipe : tcmRecipeList) {
            decoctionFee = decoctionFee.add(decoctionUnitPrice.multiply(BigDecimal.valueOf(recipe.getCopyNum())));
        }
        return decoctionFee;
    }

    /**
     * 获取线下处方代煎费
     *
     * @param organId           机构ID
     * @param offLineRecipeList 线下处方列表
     * @return 线下处方代煎费
     */
    private BigDecimal setOffLineRecipeDecoctionFee(Integer organId, List<Recipe> offLineRecipeList) {
        String decoctionDeploy = configurationClient.getValueCatchReturnArr(organId, "decoctionDeploy", "0");
        //如果配置项不是医生选择则不进行计算
        if (!DecoctionDeployTypeEnum.DECOCTION_DEPLOY_DOCTOR.getType().equals(decoctionDeploy)) {
            return null;
        }
        BigDecimal decoctionFee = BigDecimal.ZERO;
        List<String> recipeCodeList = offLineRecipeList.stream().map(Recipe::getRecipeCode).collect(Collectors.toList());
        //查询线下处方
        List<HisRecipe> hisRecipeList = hisRecipeDAO.findHisRecipeByRecipeCodeAndClinicOrgan(organId, recipeCodeList);
        for (HisRecipe hisRecipe : hisRecipeList) {
            if (StringUtils.isEmpty(hisRecipe.getDecoctionText())) {
                continue;
            }
            //首先获取总的代煎费，如果没有传总代煎费就手动计算
            if (null != hisRecipe.getDecoctionFee()) {
                decoctionFee = decoctionFee.add(hisRecipe.getDecoctionFee());
            } else {
                if (hisRecipe.getDecoctionUnitFee() != null && StringUtils.isNotEmpty(hisRecipe.getTcmNum())) {
                    decoctionFee = decoctionFee.add(hisRecipe.getDecoctionUnitFee().multiply(BigDecimal.valueOf(Integer.parseInt(hisRecipe.getTcmNum()))));
                }
            }
        }
        return decoctionFee;
    }

    //设置金额
    private double getFee(Object fee) {
        return null != fee ? Double.parseDouble(fee.toString()) : 0d;
    }
}
