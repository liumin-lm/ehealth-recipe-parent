package recipe.manager;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.ngari.base.organconfig.model.OrganConfigBean;
import com.ngari.his.recipe.mode.RecipeCashPreSettleInfo;
import com.ngari.his.recipe.mode.RecipeCashPreSettleReqTO;
import com.ngari.his.visit.mode.NeedPaymentRecipeReqTo;
import com.ngari.his.visit.mode.NeedPaymentRecipeResTo;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.entity.*;
import coupon.api.request.CouponCalcReq;
import coupon.api.vo.Coupon;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.client.*;
import recipe.constant.ParameterConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeRefundRoleConstant;
import recipe.constant.ReviewTypeConstant;
import recipe.dao.*;
import recipe.enumerate.status.RecipeSourceTypeEnum;
import recipe.enumerate.type.DecoctionDeployTypeEnum;
import recipe.enumerate.type.ExpressFeePayWayEnum;
import recipe.enumerate.type.RecipeOrderFeeTypeEnum;
import recipe.util.LocalStringUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static ctd.persistence.DAOFactory.getDAO;

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
    private ConsultClient consultClient;
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private HisRecipeDAO hisRecipeDAO;
    @Autowired
    private RecipeParameterDao recipeParameterDao;
    @Autowired
    private EnterpriseAddressDAO enterpriseAddressDAO;
    @Autowired
    private RecipeRefundDAO recipeRefundDAO;
    @Autowired
    private CouponClient couponClient;

    @LogRecord
    public void setSHWFAccountFee(RecipeOrder order) {
        //上海外服个性化处理账户支付金额
        String organName = recipeParameterDao.getByName("shwfAccountFee");
        if (StringUtils.isNotEmpty(organName) && LocalStringUtil.hasOrgan(order.getOrganId().toString(), organName)) {
            BigDecimal accountFee = getAccountFee(order.getTotalFee(), order.getMpiId(), order.getOrganId());
            if (null != accountFee) {
                order.setThirdPayType(2);
                order.setThirdPayFee(accountFee.doubleValue());
            }
        }
    }
    /**
     * 处方中药相关费用
     *
     * @param recipeList
     */
    @LogRecord
    public void setRecipeChineseMedicineFee(List<Recipe> recipeList, RecipeOrder order, Map<String, Object> ext) {
        // 总贴数
        Integer totalCopyNum = 0;
        // 中医辨证论治费
        BigDecimal tcmFee = null;
        //  其他费用
        BigDecimal otherFee=order.getOtherFee();
        BigDecimal otherTotalFee = BigDecimal.ZERO;
        // 计入订单价格的代煎费用
        BigDecimal decoctionFee = null;
        // 总代煎费用
        BigDecimal decoctionTotalFee = BigDecimal.ZERO;
        // 未计入订单的代煎费
        BigDecimal notContainDecoctionPrice = BigDecimal.ZERO;
        if (Objects.isNull(otherFee)) {
            otherFee= BigDecimal.ZERO;
        }
        if (CollectionUtils.isEmpty(recipeList)) {
            return;
        }
        // 检验是中药处方
        List<Integer> list = recipeList.stream().map(Recipe::getRecipeType).collect(Collectors.toList());
        if (!list.contains(RecipeBussConstant.RECIPETYPE_TCM)) {
            return;
        }
        Map<String, List<HisRecipe>> hisRecipeMap = null;
        List<String> recipeCodes = recipeList.stream().map(Recipe::getRecipeCode).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(recipeCodes)) {
            logger.info("setRecipeChineseMedicineFee 线下处方 recipeCodes={}", JSONArray.toJSONString(recipeCodes));
            List<HisRecipe> hisRecipes = hisRecipeDAO.findHisRecipeByRecipeCodeAndClinicOrgan(recipeList.get(0).getClinicOrgan(), recipeCodes);
            if (CollectionUtils.isNotEmpty(hisRecipes)) {
                hisRecipeMap = hisRecipes.stream().collect(Collectors.groupingBy(HisRecipe::getRecipeCode));
            }
        }
        List<Integer> recipeIds = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        List<RecipeExtend> recipeExtends = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIds);
        Map<Integer, List<RecipeExtend>> recipeExtMap = recipeExtends.stream().collect(Collectors.groupingBy(RecipeExtend::getRecipeId));
        Map<String, List<HisRecipe>> finalHisRecipeMap = hisRecipeMap;
        for (Recipe recipe : recipeList) {
            logger.info("setRecipeChineseMedicineFee recipeId={}", JSONArray.toJSONString(recipe.getRecipeId()));
            Boolean isCalculateDecoctionFee = false;
            RecipeExtend extend = null;
            if (MapUtils.isNotEmpty(recipeExtMap) && CollectionUtils.isNotEmpty(recipeExtMap.get(recipe.getRecipeId()))) {
                extend = recipeExtMap.get(recipe.getRecipeId()).get(0);
                String doctorIsDecoction = extend.getDoctorIsDecoction();
                String patientIsDecoction = order.getPatientIsDecoction();
                isCalculateDecoctionFee = getIsCalculateDecoctionFee(recipe.getClinicOrgan(), doctorIsDecoction, patientIsDecoction);
            }
            if (Objects.nonNull(recipe.getCopyNum())) {
                totalCopyNum = totalCopyNum + recipe.getCopyNum();
            }
            BigDecimal recipeDecoctionFee = BigDecimal.ZERO;
            if (new Integer(2).equals(recipe.getRecipeSourceType())) {
                logger.info("setRecipeChineseMedicineFee 进入线下处方控制逻辑");
                if (MapUtils.isEmpty(finalHisRecipeMap) || CollectionUtils.isEmpty(finalHisRecipeMap.get(recipe.getRecipeCode()))) {
                    continue;
                }
                HisRecipe hisRecipe = finalHisRecipeMap.get(recipe.getRecipeCode()).get(0);
                if (Objects.nonNull(hisRecipe.getTcmFee())) {
                    tcmFee = hisRecipe.getTcmFee();
                }
                if (Objects.nonNull(hisRecipe.getOtherTotalFee())) {
                    otherTotalFee = hisRecipe.getOtherTotalFee();
                    if(Objects.isNull(otherTotalFee)){
                        otherTotalFee = BigDecimal.ZERO;
                    }
                    otherFee=otherFee.add(otherTotalFee);
                }
                //有代煎总额
                if (Objects.nonNull(hisRecipe.getDecoctionFee())) {
                    recipeDecoctionFee = hisRecipe.getDecoctionFee();
                } else if (hisRecipe.getDecoctionUnitFee() != null && recipe.getCopyNum() != null) {
                    //无代煎总额 需计算代煎总额=帖数*代煎单价
                    //代煎费等于剂数乘以代煎单价
                    //如果是合并处方-多张处方下得累加
                    recipeDecoctionFee = hisRecipe.getDecoctionUnitFee().multiply(BigDecimal.valueOf(recipe.getCopyNum()));
                }
                //有煎法就会从order得到DecoctionUnitPrice
                else if (null != extend && Objects.nonNull(extend.getDecoctionId())) {
                    //代煎费等于剂数乘以代煎单价
                    //如果是合并处方-多张处方下得累加
                    recipeDecoctionFee = getRecipeDecoctionFee(extend, recipe);
                }

            } else {
                logger.info("setRecipeChineseMedicineFee 进入线上处方控制逻辑");
                BigDecimal tcmPrice = configurationClient.getValueCatchReturnBigDecimal(recipe.getClinicOrgan(), "recipeTCMPrice", BigDecimal.ZERO);
                // 说明走平台的中医论证费计算
                if (tcmPrice.compareTo(BigDecimal.ZERO) > -1) {
                    tcmFee = tcmPrice;
                }

                //代煎费等于剂数乘以代煎单价
                //如果是合并处方-多张处方下得累加
                //只有最终选择了代煎才计算收取代煎费，如果是非代煎则隐藏代煎费并且不收代煎费
                //如果配置了患者选择，以患者选择是否代煎计算价格
                recipeDecoctionFee = getRecipeDecoctionFee(extend, recipe);

            }

            if (recipeDecoctionFee.compareTo(BigDecimal.ZERO) == 1) {
                decoctionTotalFee = decoctionTotalFee.add(recipeDecoctionFee);
                if (isCalculateDecoctionFee) {
                    if (Objects.isNull(decoctionFee)) {
                        decoctionFee = BigDecimal.ZERO;
                    }
                    decoctionFee = decoctionFee.add(recipeDecoctionFee);
                } else {
                    notContainDecoctionPrice = notContainDecoctionPrice.add(recipeDecoctionFee);
                }
            }
        }
        order.setCopyNum(totalCopyNum);
        order.setTcmFee(tcmFee);
        order.setDecoctionFee(decoctionFee);
        order.setOtherFee(otherFee);
        // 2022 4-v1 版本产品拿掉代煎单价
//        order.setDecoctionUnitPrice(decoctionTotalFee.divide(BigDecimal.valueOf(totalCopyNum)));
        ext.put("notContainDecoctionPrice", notContainDecoctionPrice);
        ext.put("decoctionTotalFee", decoctionTotalFee);
        logger.info("setRecipeChineseMedicineFee 处方中药相关费用 totalCopyNum={},tcmFee={},decoctionFee={},notContainDecoctionPrice={},decoctionTotalFee={}", totalCopyNum, tcmFee, decoctionFee, notContainDecoctionPrice, decoctionTotalFee);

    }

    /**
     * 计算单张处方代煎费
     *
     * @param extend
     * @param recipe
     * @return
     */
    private BigDecimal getRecipeDecoctionFee(RecipeExtend extend, Recipe recipe) {
        Integer decoctionId = null;
        if (null != extend && StringUtils.isNotEmpty(extend.getDecoctionId())) {
            decoctionId = Integer.valueOf(extend.getDecoctionId());
        }
        logger.info("decoctionId:{}", decoctionId);
        if (decoctionId != null) {
            DrugDecoctionWayDao drugDecoctionWayDao = getDAO(DrugDecoctionWayDao.class);
            DecoctionWay decoctionWay = drugDecoctionWayDao.get(decoctionId);
            logger.info("decoctionWay:{}", JSONArray.toJSONString(decoctionWay));
            if (decoctionWay != null && decoctionWay.getDecoctionPrice() != null) {
                BigDecimal decoctionPrice = BigDecimal.valueOf(decoctionWay.getDecoctionPrice());
                return decoctionPrice.multiply(BigDecimal.valueOf(recipe.getCopyNum()));
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * 是否计算代煎费
     *
     * @return
     */
    private Boolean getIsCalculateDecoctionFee(Integer organId, String doctorIsDecoction, String patientIsDecoction) {
        logger.info("getIsCalculateDecoctionFee doctorIsDecoction:{},patientIsDecoction:{}", JSONUtils.toString(doctorIsDecoction), JSONUtils.toString(patientIsDecoction));
        Boolean result = false;
        //下单的时候会order.setPatientIsDecoction(MapValueUtil.getString(extInfo, "patientIsDecoction"));
        //有订单 eg:提交订单orderForRecipeNew
        String decoctionDeploy = configurationClient.getValueCatchReturnArr(organId, "decoctionDeploy", "");

        if (("2".equals(decoctionDeploy) || "3".equals(decoctionDeploy)) && StringUtils.isNotEmpty(patientIsDecoction)) {
            if ("1".equals(patientIsDecoction)) {
                result = true;
            }
            //没有订单 且不是提交订单  首次进入确认订单页 findConfirmOrderInfoExt
        } else if (StringUtils.isNotEmpty(doctorIsDecoction) && "1".equals(doctorIsDecoction)) {
            result = true;
        }
        logger.info("getIsCalculateDecoctionFee result:{}", JSONUtils.toString(result));
        return result;
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
        List<String> needRecipePaymentFeeType = configurationClient.getValueListCatch(order.getOrganId(), "NeedRecipePaymentFeeType", null);
        logger.info("setRecipePaymentFee needRecipePaymentFeeType={}", JSONUtils.toString(needRecipePaymentFeeType));
        if (CollectionUtils.isEmpty(needRecipePaymentFeeType)) {
            return;
        }
        // 获取处方 类型
        Recipe recipe = recipeList.get(0);
        if (!needRecipePaymentFeeType.contains(recipe.getBussSource().toString())) {
            return;
        }
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        PatientDTO patientDTO = patientClient.getPatientDTO(recipe.getMpiid());
        NeedPaymentRecipeReqTo needPayment = new NeedPaymentRecipeReqTo();
        needPayment.setCardNo(recipeExtend.getCardNo());
        needPayment.setCardType(recipeExtend.getCardType());
        needPayment.setDepartId(recipe.getDepart());
        needPayment.setOrganId(recipe.getClinicOrgan());
        needPayment.setDoctorId(recipe.getDoctor());
        needPayment.setPatientID(recipe.getPatientID());
        needPayment.setPatientName(recipe.getPatientName());
        needPayment.setRegisterID(recipeExtend.getRegisterID());
        needPayment.setSeries(recipeExtend.getSeries());
        List<String> code = recipeList.stream().map(Recipe::getRecipeCode).collect(Collectors.toList());
        needPayment.setRecipeCode(code);
        if (Objects.nonNull(patientDTO)) {
            needPayment.setIdCard(patientDTO.getIdcard());
        }
        NeedPaymentRecipeResTo recipePaymentFee = consultClient.getRecipePaymentFee(needPayment);
        if (Objects.isNull(recipePaymentFee)) {
            return;
        }
        if (null != recipePaymentFee.getRegisterFee() && recipePaymentFee.getRegisterFee().compareTo(BigDecimal.ZERO) > 0) {
            // 挂号费
            order.setRegisterFee(recipePaymentFee.getRegisterFee());
            order.setRegisterFeeNo(recipePaymentFee.getRegisterFeeNo());
        }
        if (null != recipePaymentFee.getTcmFee() && recipePaymentFee.getTcmFee().compareTo(BigDecimal.ZERO) > 0) {
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
     * 订单设置处方费用
     *
     * @param order              订单
     */
    public BigDecimal setRecipeFee(RecipeOrder order) {
        BigDecimal recipeFee = order.getRecipeFee();
        // 订单类型不走预结算不处理
        List<Integer> orderTypes = Lists.newArrayList(1, 2, 5);
        if(!orderTypes.contains(order.getOrderType())){
            return recipeFee;
        }
        List<String> preSettleContainOrderFee = configurationClient.getValueListCatch(order.getOrganId(), "PreSettleContainOrderFee", null);
        logger.info("setRecipeFee needRecipePaymentFeeType={}", JSONUtils.toString(preSettleContainOrderFee));
        if (CollectionUtils.isEmpty(preSettleContainOrderFee)) {
            return recipeFee;
        }
        if(Objects.isNull(order.getPreSettletotalAmount())){
            return recipeFee;
        }

        // 预结算返回费用包含挂号费
        BigDecimal recipeFeeNew = new BigDecimal(order.getPreSettletotalAmount());
        if (preSettleContainOrderFee.contains(RecipeOrderFeeTypeEnum.REGISTER_FEE.getType()) && Objects.nonNull(order.getRegisterFee())) {
            recipeFeeNew = recipeFeeNew.subtract(order.getRegisterFee());
        }
        // 预结算返回费用包含中医辨证论治费
        if (preSettleContainOrderFee.contains(RecipeOrderFeeTypeEnum.TCM_FEE.getType()) && Objects.nonNull(order.getTcmFee())) {
            recipeFeeNew = recipeFeeNew.subtract(order.getTcmFee());
        }
        // 预结算返回费用包含中医辨证论治费
        if (preSettleContainOrderFee.contains(RecipeOrderFeeTypeEnum.DECOCTION_FEE.getType()) && Objects.nonNull(order.getDecoctionFee())) {
            recipeFeeNew = recipeFeeNew.subtract(order.getDecoctionFee());
        }
        return recipeFeeNew.setScale(2,BigDecimal.ROUND_HALF_UP);

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
     * 设置配送费减免
     *
     * @param order
     * @return
     */
    @LogRecord
    public void setExpressFee(RecipeOrder order) {
        //快递费线上支付的需要计算是否满足包邮
        if (null != order.getExpressFee() && null != order.getEnterpriseId() && StringUtils.isNotEmpty(order.getAddress3())) {
            String addrArea = order.getAddress3();
            EnterpriseAddress enterpriseAddress;
            int length = addrArea.length();
            do {
                enterpriseAddress = enterpriseAddressDAO.getByEnterpriseIdAndAddress(order.getEnterpriseId(), addrArea.substring(0, length));
                if (Objects.nonNull(enterpriseAddress) && Objects.nonNull(enterpriseAddress.getBuyFreeShipping())) {
                    if (order.getRecipeFee().compareTo(enterpriseAddress.getBuyFreeShipping()) > -1) {
                        order.setExpressFee(BigDecimal.ZERO);
                    }
                    break;
                }
            } while ((length = length - 2) > 0);
            logger.info("OrderFeeManager setExpressFee enterpriseAddress:{}.", JSONUtils.toString(enterpriseAddress));
        }
    }

    /**
     * 设置优惠券费用
     *
     * @param order
     */
    public void setCouponFee(RecipeOrder order, Recipe recipe) {
        if (null == order.getCouponId() || order.getCouponId() <= 0) {
            return;
        }
        CouponCalcReq couponCalcReq = CouponCalcReq.builder().couponId(order.getCouponId()).departId(recipe.getDepart()).doctorId(recipe.getDoctor())
                .drugsEnterpriseId(order.getEnterpriseId()).orderAmount(order.getTotalFee()).partAmount(order.getExpressFee())
                .build();
        Coupon coupon = couponClient.getCouponByRecipeOrder(couponCalcReq);
        if (null == coupon) {
            return;
        }
        order.setCouponName(coupon.getCouponName());
        order.setCouponFee(coupon.getDiscount());
        order.setCouponDesc(coupon.getCouponDesc());
        order.setActualPrice(order.getTotalFee().subtract(order.getCouponFee()).doubleValue());

        // 锁定优惠券
        couponClient.lockCouponByBus(couponCalcReq);
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


    public void recipeReFundSave(String orderCode, RecipeRefund recipeRefund) {
        //处理合并支付问题
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(orderCode);
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIdList);
        recipes.forEach(recipe -> {
            recipeRefund.setBusId(recipe.getRecipeId());
            recipeRefund.setOrganId(recipe.getClinicOrgan());
            recipeRefund.setMpiid(recipe.getMpiid());
            recipeRefund.setPatientName(recipe.getPatientName());
            Boolean doctorReviewRefund = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "doctorReviewRefund", false);
            if (doctorReviewRefund) {
                recipeRefund.setDoctorId(recipe.getDoctor());
            }
            String memo = null;
            try {
                memo = DictionaryController.instance().get("eh.cdr.dictionary.RecipeRefundNode").getText(recipeRefund.getNode()) +
                        DictionaryController.instance().get("eh.cdr.dictionary.RecipeRefundCheckStatus").getText(recipeRefund.getStatus());
            } catch (ControllerException e) {
                logger.error("recipeReFundSave-未获取到处方单信息. recipeId={}, node={}, recipeRefund={}", recipe, JSONUtils.toString(recipeRefund));
                throw new DAOException("退费相关字典获取失败");
            }
            recipeRefund.setMemo(memo);
            if (recipeRefund.getNode() == RecipeRefundRoleConstant.RECIPE_REFUND_ROLE_PATIENT) {
                recipeRefund.setStatus(0);
                recipeRefund.setMemo("患者发起退费申请");
            }
            recipeRefund.setNode(recipeRefund.getNode());
            recipeRefund.setStatus(recipeRefund.getStatus());
            recipeRefund.setApplyTime(new Date());
            recipeRefund.setCheckTime(new Date());
            //保存记录
            recipeRefundDAO.saveRefund(recipeRefund);
        });
    }

    public Integer getRecipeRefundNode(Integer recipeId, Integer organId) {
        Boolean doctorReviewRefund = configurationClient.getValueBooleanCatch(organId, "doctorReviewRefund", false);
        List<RecipeRefund> recipeRefundList = recipeRefundDAO.findRefundListByRecipeId(recipeId);
        if (doctorReviewRefund && CollectionUtils.isNotEmpty(recipeRefundList) && new Integer(-1).equals(recipeRefundList.get(0).getNode())) {
            return 1;
        }
        return 2;
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
        EnterpriseAddress enterpriseAddress;
        int length = addrArea.length();
        do {
             enterpriseAddress = enterpriseAddressDAO.getByEnterpriseIdAndAddress(enterpriseId, addrArea.substring(0, length));
            if (Objects.nonNull(enterpriseAddress) && Objects.nonNull( enterpriseAddress.getDistributionPrice())) {
                break;
            }
        } while ((length = length - 2) > 0);
        logger.info("OrderFeeManager getPlatformExpressFee drugDistributionPrice:{}.", JSONUtils.toString(enterpriseAddress));
        if (null == enterpriseAddress) {
            return null;
        }
        return enterpriseAddress.getDistributionPrice();
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
