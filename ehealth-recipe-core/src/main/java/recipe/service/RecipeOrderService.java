package recipe.service;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.address.model.AddressBean;
import com.ngari.base.address.service.IAddressService;
import com.ngari.base.hisconfig.model.HisServiceConfigBean;
import com.ngari.base.hisconfig.service.IHisConfigService;
import com.ngari.base.organconfig.model.OrganConfigBean;
import com.ngari.base.organconfig.service.IOrganConfigService;
import com.ngari.base.payment.model.DabaiPayResult;
import com.ngari.base.payment.service.IPaymentService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.bus.coupon.model.CouponBean;
import com.ngari.bus.coupon.service.ICouponService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.RecipeAPI;
import com.ngari.recipe.common.RecipeBussResTO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugdistributionprice.model.DrugDistributionPriceBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.PatientRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import com.ngari.recipe.recipeorder.service.IRecipeOrderService;
import coupon.api.service.ICouponBaseService;
import coupon.api.vo.Coupon;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.PurchaseResponse;
import recipe.bean.RecipePayModeSupportBean;
import recipe.bussutil.RecipeUtil;
import recipe.common.CommonConstant;
import recipe.common.ResponseUtils;
import recipe.constant.*;
import recipe.dao.*;
import recipe.drugsenterprise.AccessDrugEnterpriseService;
import recipe.drugsenterprise.CommonRemoteService;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.drugsenterprise.YsqRemoteService;
import recipe.purchase.PurchaseService;
import recipe.service.common.RecipeCacheService;
import recipe.util.MapValueUtil;
import recipe.util.ValidateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * 处方订单管理
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/2/13.
 */
@RpcBean("recipeOrderService")
public class RecipeOrderService extends RecipeBaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeOrderService.class);

    private PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);

    private IHisConfigService iHisConfigService = ApplicationUtils.getBaseService(IHisConfigService.class);

    private RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);

    private static Integer[] showDownloadRecipeStatus = new Integer[]{RecipeStatusConstant.CHECK_PASS_YS, RecipeStatusConstant.RECIPE_DOWNLOADED};

    /**
     * 处方结算时创建临时订单
     *
     * @param recipeIds
     * @param extInfo
     * @return
     */
    public RecipeOrderBean createBlankOrder(List<Integer> recipeIds, Map<String, String> extInfo) {
        OrderCreateResult result = createOrder(recipeIds, extInfo, 0);
        RecipeOrderBean order = null;
        if (null != result && RecipeResultBean.SUCCESS.equals(result.getCode()) &&
                null != result.getObject() && result.getObject() instanceof RecipeOrderBean) {
            order = (RecipeOrderBean) result.getObject();
        }
        return order;
    }

    /*
     * @description 获取订单信息跳转地址（互联网）
     * @author gmw
     * @date 2019/9/25
     * @param [recipeId]
     * @return void
     */
    @RpcService
    public PurchaseResponse getRecipeOrderUrl(int recipeId, Integer depId) {

        LOGGER.info("获取订单信息跳转地址开始，处方ID：{}.", recipeId);
        //获取处方信息
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);

        //获取药企信息
        DrugsEnterprise drugsEnterprise = null;
        if(null == depId){
            OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
            List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(recipe.getClinicOrgan(), 1);
            drugsEnterprise = drugsEnterprises.get(0);
        } else {
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            drugsEnterprise = drugsEnterpriseDAO.get(depId);
        }

        PurchaseResponse response = ResponseUtils.getFailResponse(PurchaseResponse.class, "");

        //暂时没找到好的控制字段，只能用写死天猫了
        if(!"tmdyf".equals(drugsEnterprise.getAccount())){
            response.setCode(PurchaseResponse.CHECKWARN);
            return response;
        }

        //根据药企ID获取具体跳转的url地址
        try {
            RemoteDrugEnterpriseService remoteDrugEnterpriseService =
                ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            AccessDrugEnterpriseService remoteService = remoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
            remoteService.getJumpUrl(response, recipe, drugsEnterprise);
        } catch (Exception e) {
            LOGGER.warn("获取跳转实现异常--{}", e);
            response.setCode(CommonConstant.FAIL);
            response.setMsg("获取跳转实现异常--{}" +  e);
            return response;
        }

        return response;
    }

    /**
     * 订单创建
     *
     * @param recipeIds 合并处方单ID
     * @param extInfo   {"operMpiId":"当前操作者编码","addressId":"当前选中地址","payway":"支付方式（payway）","payMode":"处方支付方式",
     *                  "decoctionFlag":"1(1：代煎，0：不代煎)", "gfFeeFlag":"1(1：表示需要制作费，0：不需要)", “depId”:"指定药企ID",
     *                  "expressFee":"快递费","gysCode":"药店编码","sendMethod":"送货方式","payMethod":"支付方式","appId":"公众号ID",
     *                  "calculateFee":"1(1:需要，0:不需要)"}
     *                  <p>
     *                  ps: decoctionFlag是中药处方时设置为1，gfFeeFlag是膏方时设置为1
     *                  gysCode, sendMethod, payMethod 字段为钥世圈字段，会在findSupportDepList接口中给出
     *                  payMode 如果钥世圈有供应商是多种方式支持，就传0
     * @param toDbFlag  0不存入数据库 ， 1存入数据库
     * @return
     */
    @RpcService
    public OrderCreateResult createOrder(List<Integer> recipeIds, Map<String, String> extInfo, Integer toDbFlag) {
        LOGGER.info("createOrder param: dbflag={}, ids={}, extInfo={}", toDbFlag, JSONUtils.toString(recipeIds), JSONUtils.toString(extInfo));
        IConfigurationCenterUtilsService configurationCenterUtilsService = (IConfigurationCenterUtilsService)AppContextHolder.getBean("eh.configurationCenterUtils");
        OrderCreateResult result = new OrderCreateResult(RecipeResultBean.SUCCESS);
        RecipeOrder order = null;
        RecipePayModeSupportBean payModeSupport = null;
        if (CollectionUtils.isEmpty(recipeIds)) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("缺少处方ID参数");
            return result;
        }

        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);

        order = new RecipeOrder();
        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        if (null == payMode) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("缺少支付方式");
            return result;
        }

        //设置订单初始数据
        order.setRecipeIdList(JSONUtils.toString(recipeIds));
        String payway = MapValueUtil.getString(extInfo, "payway");
        if (1 == toDbFlag && (StringUtils.isEmpty(payway))) {
            LOGGER.error("支付信息不全 extInfo={}", JSONUtils.toString(extInfo));
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("支付信息不全");
            return result;
        }
        order.setWxPayWay(payway);
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        if (CollectionUtils.isEmpty(recipeList)) {
            LOGGER.error("处方对象不存在 ids={}", JSONUtils.toString(recipeIds));
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("处方不存在");
            return result;
        }

        //指定了药企的话需要传该字段
        Integer depId = MapValueUtil.getInteger(extInfo, "depId");
        order.setEnterpriseId(depId);
        order.setRecipeMode(recipeList.get(0).getRecipeMode());
        order.setGiveMode(recipeList.get(0).getGiveMode());
        payModeSupport = setPayModeSupport(order, payMode);
        //校验处方列表是否都能进行配送
        if (RecipeResultBean.SUCCESS.equals(result.getCode())) {
            //获取需要删除的处方对象(可能已处理或者库存不足等情况的处方)
            List<Recipe> needDelList = validateRecipeList(result, recipeList, order, payMode, payModeSupport);
            //null特殊处理，表示该处方的订单已生成，可以直接支付
            if (null == needDelList) {
                RecipeOrder dbOrder = orderDAO.getByOrderCode(result.getOrderCode());
                setCreateOrderResult(result, dbOrder, payModeSupport, toDbFlag);
                return result;
            }
            //过滤无法合并的处方单
            if (CollectionUtils.isNotEmpty(needDelList)) {
                LOGGER.info("createOrder delList size={} ", needDelList.size());
                recipeList.removeAll(needDelList);
                if (CollectionUtils.isEmpty(recipeList)) {
                    LOGGER.error("createOrder 需要合并的处方单size为0.");
                    result.setCode(RecipeResultBean.FAIL);
                    //当前只处理了单个处方的返回
                    result.setMsg(result.getError());
                }
            }
        }

        if (RecipeResultBean.SUCCESS.equals(result.getCode())) {
            Recipe firstRecipe = recipeList.get(0);
            // 暂时还是设置成处方单的患者，不然用户历史处方列表不好查找
            order.setMpiId(firstRecipe.getMpiid());
            order.setOrganId(firstRecipe.getClinicOrgan());
            order.setOrderCode(this.getOrderCode(order.getMpiId()));
            order.setStatus(OrderStatusConstant.READY_PAY);
            //设置订单各种费用和配送地址
            Integer calculateFee = MapValueUtil.getInteger(extInfo, "calculateFee");
            if (null == calculateFee || Integer.valueOf(1).equals(calculateFee)) {
                setOrderFee(result, order, recipeIds, recipeList, payModeSupport, extInfo, toDbFlag);
            } else {
                order.setRecipeFee(BigDecimal.ZERO);
                order.setCouponFee(BigDecimal.ZERO);
                order.setRegisterFee(BigDecimal.ZERO);
                order.setExpressFee(BigDecimal.ZERO);
                order.setTotalFee(BigDecimal.ZERO);
                order.setActualPrice(BigDecimal.ZERO.doubleValue());
                double auditFee = Double.parseDouble( configurationCenterUtilsService.getConfiguration(firstRecipe.getClinicOrgan(), ParameterConstant.KEY_AUDITFEE).toString());
                order.setAuditFee(BigDecimal.valueOf(auditFee));
                double otherServiceFee = Double.parseDouble(configurationCenterUtilsService.getConfiguration(firstRecipe.getClinicOrgan(), ParameterConstant.KEY_OTHERFEE).toString());
                order.setOtherFee(BigDecimal.valueOf(otherServiceFee));
            }
            if (RecipeResultBean.SUCCESS.equals(result.getCode()) && 1 == toDbFlag) {
                boolean saveFlag = saveOrderToDB(order, recipeList, payMode, result, recipeDAO, orderDAO);
                if (saveFlag) {
                    if (payModeSupport.isSupportMedicalInsureance()) {
                        //医保支付处理
                        //往大白发送处方信息
                        String backInfo = applyMedicalInsurancePay(order, recipeList);
                        if (StringUtils.isEmpty(backInfo)) {
                            result.setMsg(cacheService.getParam(ParameterConstant.KEY_RECIPE_MEDICALPAY_TIP));
                        } else {
                            result.setCode(RecipeResultBean.FAIL);
                            result.setMsg("医保支付返回," + backInfo);
                        }
                    } else if (payModeSupport.isSupportCOD() || payModeSupport.isSupportTFDS() || payModeSupport.isSupportComplex()) {
                        //货到付款 | 药店取药 处理
                        if (RecipeBussConstant.FROMFLAG_PLATFORM.equals(firstRecipe.getFromflag())) {
                            //平台处方先发送处方数据
                            sendRecipeAfterCreateOrder(recipeList, result, extInfo);
                        }
                    }
                }
            }
        }
        setCreateOrderResult(result, order, payModeSupport, toDbFlag);
        return result;
    }

    /**
     * 设置支付方式处理
     *
     * @param order
     * @param payMode
     * @return
     */
    public RecipePayModeSupportBean setPayModeSupport(RecipeOrder order, Integer payMode) {
        RecipePayModeSupportBean payModeSupport = new RecipePayModeSupportBean();
        if (RecipeBussConstant.PAYMODE_MEDICAL_INSURANCE.equals(payMode)) {
            payModeSupport.setSupportMedicalInsureance(true);
            //在收到医快付支付消息返回前，该订单处于无效状态
            order.setEffective(0);
        } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode)) {
            //在收到用户确认userConfirm消息返回前，该订单处于无效状态
            payModeSupport.setSupportCOD(true);
            order.setEffective(0);
        } else if (RecipeBussConstant.PAYMODE_TFDS.equals(payMode)) {
            //在收到用户确认userConfirm消息返回前，该订单处于无效状态
            payModeSupport.setSupportTFDS(true);
            order.setEffective(0);
        } else if (RecipeBussConstant.PAYMODE_COMPLEX.equals(payMode)) {
            payModeSupport.setSupportComplex(true);
            order.setEffective(0);
        } else if (RecipeBussConstant.PAYMODE_DOWNLOAD_RECIPE.equals(payMode)) {
            payModeSupport.setSupportDownload(true);
            order.setEffective(0);
        } else if (RecipeBussConstant.PAYMODE_TO_HOS.equals(payMode)){
            payModeSupport.setSupportToHos(true);
            order.setEffective(0);
        }else {
            payModeSupport.setSupportOnlinePay(true);
            order.setEffective(1);
        }

        return payModeSupport;
    }

    /**
     * 校验处方数据
     *
     * @param result
     * @param recipeList
     * @param order
     * @param payMode
     * @param payModeSupport
     * @param payMode
     * @param payModeSupport
     * @return
     */
    private List<Recipe> validateRecipeList(OrderCreateResult result, List<Recipe> recipeList, RecipeOrder order,
                                            Integer payMode, RecipePayModeSupportBean payModeSupport) {
        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);

        List<Recipe> needDelList = new ArrayList<>(10);
        // 多处方处理仍需要重构
        for (Recipe recipe : recipeList) {
            //平台处方才需要校验配送药企
            if (1 == recipe.getFromflag()) {
                //判断处方状态是否还能进行支付
                if (!Integer.valueOf(RecipeStatusConstant.CHECK_PASS).equals(recipe.getStatus())) {
                    LOGGER.error("处方id=" + recipe.getRecipeId() + "不是待处理状态。");
                    if (RecipeStatusConstant.REVOKE == recipe.getStatus()) {
                        result.setError("由于医生已撤销，该处方单已失效，请联系医生");
                    } else {
                        result.setError("处方单已处理");
                    }
                    needDelList.add(recipe);
                    continue;
                }
                Integer depId = recipeService.supportDistributionExt(recipe.getRecipeId(), recipe.getClinicOrgan(),
                        order.getEnterpriseId(), payMode);
                if (null == depId && ( payModeSupport.isSupportOnlinePay() || payModeSupport.isSupportCOD()|| payModeSupport.isSupportTFDS()) ) {
                    LOGGER.error("处方id=" + recipe.getRecipeId() + "无法配送。");
                    result.setError("很抱歉，当前库存不足无法结算，请联系客服：" +
                            cacheService.getParam(ParameterConstant.KEY_CUSTOMER_TEL, RecipeSystemConstant.CUSTOMER_TEL));
                    //不能配送需要从处方列表剔除
                    needDelList.add(recipe);
                    continue;
                } else {
                    order.setEnterpriseId(depId);
                }
            }

            //查询是否存在已在其他订单里的处方
            boolean flag = orderDAO.isEffectiveOrder(recipe.getOrderCode(), payMode);
            if (flag) {
                if (payModeSupport.isSupportMedicalInsureance()) {
                    LOGGER.error("处方id=" + recipe.getRecipeId() + "已经发送给医保。");
                    //再重复发送一次医快付消息
                    RecipeOrder dbOrder = orderDAO.getByOrderCode(recipe.getOrderCode());
                    String backInfo = applyMedicalInsurancePay(dbOrder, recipeList);
                    if (StringUtils.isEmpty(backInfo)) {
                        result.setError(cacheService.getParam(ParameterConstant.KEY_RECIPE_MEDICALPAY_TIP));
                    } else {
                        result.setError("医保支付返回:" + backInfo);
                    }
                    //订单有效需要从处方列表剔除
                    needDelList.add(recipe);
                } else {
                    LOGGER.error("处方id=" + recipe.getRecipeId() + "订单已存在。");
//                    result.setError("处方单已结算");
                    //已存在订单，则直接返回
                    // 此处只考虑了单个处方支付时 重复点击支付的情况，多处方情况应当校验订单内包含的处方是否与当前合并支付的处方一致
                    result.setOrderCode(recipe.getOrderCode());
                    needDelList = null;
                    break;
                }
            } else {
                //如果该处方单没有关联订单，又是点击医保支付，则需要通过商户号查询原先的订单号关联之前的订单
                if (payModeSupport.isSupportMedicalInsureance()) {
                    String outTradeNo = recipe.getOutTradeNo();
                    if (StringUtils.isNotEmpty(outTradeNo)) {
                        RecipeOrder dbOrder = orderDAO.getByOutTradeNo(outTradeNo);
                        if (null != dbOrder) {
                            recipeDAO.updateOrderCodeByRecipeIds(Collections.singletonList(recipe.getRecipeId()), dbOrder.getOrderCode());
                            String backInfo = applyMedicalInsurancePay(dbOrder, recipeList);
                            if (StringUtils.isEmpty(backInfo)) {
                                result.setError(cacheService.getParam(ParameterConstant.KEY_RECIPE_MEDICALPAY_TIP));
                            } else {
                                result.setError("医保支付返回:" + backInfo);
                            }
                            //订单有效需要从处方列表剔除
                            needDelList.add(recipe);
                        }
                    }
                }
            }

        }

        return needDelList;
    }

    /**
     * 设置处方费用
     *
     * @param result
     * @param order
     * @param recipeIds
     * @param recipeList
     * @param payModeSupport
     * @param extInfo
     * @param toDbFlag
     */
    public void setOrderFee(OrderCreateResult result, RecipeOrder order, List<Integer> recipeIds,
                             List<Recipe> recipeList, RecipePayModeSupportBean payModeSupport,
                             Map<String, String> extInfo, Integer toDbFlag) {
        IOrganConfigService iOrganConfigService = ApplicationUtils.getBaseService(IOrganConfigService.class);
        IConfigurationCenterUtilsService configurationCenterUtilsService = (IConfigurationCenterUtilsService)AppContextHolder.getBean("eh.configurationCenterUtils");
        RecipeDetailDAO recipeDetailDAO = getDAO(RecipeDetailDAO.class);
        OrganConfigBean organConfig = iOrganConfigService.get(order.getOrganId());

        if (null == organConfig) {
            //只有需要真正保存订单时才提示
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("开方机构缺少配置");
            return;
        }
        //当前操作人的编码，用于获取地址列表信息等
        String operMpiId = MapValueUtil.getString(extInfo, "operMpiId");

        //设置挂号费（之前是区分购药方式的，要去区分购药方式来挂号费，现在不区分根据配置项来）
        BigDecimal registerFee = organConfig.getPriceForRecipeRegister();
        if (null != registerFee) {
            order.setRegisterFee(registerFee);
        } else {
            order.setRegisterFee(new BigDecimal(cacheService.getParam(ParameterConstant.KEY_RECIPE_REGISTER_FEE, "0")));
        }

        //设置审方费用
        Recipe firstRecipe = recipeList.get(0);
        //date 20190929
        //审方费判断非不需要审核再去计算
        double auditFee = ReviewTypeConstant.Not_Need_Check == firstRecipe.getReviewType() ? 0d : Double.parseDouble(configurationCenterUtilsService.getConfiguration(firstRecipe.getClinicOrgan(), ParameterConstant.KEY_AUDITFEE).toString());
        order.setAuditFee(BigDecimal.valueOf(auditFee));
        //设置其他服务费用
        double otherServiceFee = Double.parseDouble(configurationCenterUtilsService.getConfiguration(firstRecipe.getClinicOrgan(), ParameterConstant.KEY_OTHERFEE).toString());
        order.setOtherFee(BigDecimal.valueOf(otherServiceFee));

        //设置优惠券信息
        Integer couponId = MapValueUtil.getInteger(extInfo, "couponId");
        order.setCouponId(couponId);
        //设置处方总费用
        BigDecimal recipeFee = BigDecimal.ZERO;
        for (Recipe recipe : recipeList) {
            if (null != recipe) {
                if (null != recipe.getTotalMoney()) {
                    recipeFee = recipeFee.add(recipe.getTotalMoney());
                }
            }
        }
        //药企是需要自己结算费用的，需要重新设置
        //在线支付才需要重新计算
        //药店取药，货到付款也需要重新计算药品金额
        if ((payModeSupport.isSupportCOD() || payModeSupport.isSupportTFDS()|| payModeSupport.isSupportOnlinePay()) && null != order.getEnterpriseId()) {
            recipeFee = reCalculateRecipeFee(order.getEnterpriseId(), recipeIds, null);
        }
        order.setRecipeFee(recipeFee);

        //中药表示待煎费，膏方代表制作费
        BigDecimal otherFee = BigDecimal.ZERO;
        //设置订单代煎费
        Integer totalCopyNum = 0;
        boolean needCalDecFee = false;
        Integer decoctionFlag = MapValueUtil.getInteger(extInfo, "decoctionFlag");
        //1表示待煎
        if (Integer.valueOf(1).equals(decoctionFlag)) {
            //待煎单价(代煎费 -1不支持代煎 大于等于0时为代煎费)
            BigDecimal recipeDecoctionPrice = organConfig.getRecipeDecoctionPrice();
            //根据机构获取代煎费
            order.setDecoctionUnitPrice(null != recipeDecoctionPrice ? recipeDecoctionPrice : BigDecimal.valueOf(-1));
            needCalDecFee = (order.getDecoctionUnitPrice().compareTo(BigDecimal.ZERO) == 1) ? true : false;
        }

        //设置膏方制作费
        Integer gfFeeFlag = MapValueUtil.getInteger(extInfo, "gfFeeFlag");
        //1表示膏方制作费
        if (Integer.valueOf(1).equals(gfFeeFlag)) {
            //制作单价
            BigDecimal gfFeeUnitPrice = organConfig.getRecipeCreamPrice();
            if (null == gfFeeUnitPrice) {
                gfFeeUnitPrice = BigDecimal.ZERO;
            }
            order.setDecoctionUnitPrice(gfFeeUnitPrice);
            //存在制作单价且大于0
            if (gfFeeUnitPrice.compareTo(BigDecimal.ZERO) == 1) {
                Double totalDose = recipeDetailDAO.getUseTotalDoseByRecipeIds(recipeIds);
                otherFee = gfFeeUnitPrice.multiply(BigDecimal.valueOf(totalDose));
            }
        }

        for (Recipe recipe : recipeList) {
            if (RecipeBussConstant.RECIPETYPE_TCM.equals(recipe.getRecipeType())) {
                totalCopyNum = totalCopyNum + recipe.getCopyNum();
                if (needCalDecFee) {
                    //代煎费等于剂数乘以代煎单价
                    otherFee = otherFee.add(order.getDecoctionUnitPrice().multiply(BigDecimal.valueOf(recipe.getCopyNum())));
                }
            }
        }
        order.setCopyNum(totalCopyNum);
        order.setDecoctionFee(otherFee);
        //药店取药不需要地址信息
        if (payModeSupport.isSupportTFDS() || payModeSupport.isSupportDownload() || payModeSupport.isSupportToHos()) {
            order.setAddressCanSend(true);
            order.setExpressFee(BigDecimal.ZERO);
        } else {
            //设置运费
            IAddressService addressService = ApplicationUtils.getBaseService(IAddressService.class);
            String operAddressId = MapValueUtil.getString(extInfo, "addressId");
            AddressBean address = null;
            if (StringUtils.isNotEmpty(operAddressId)) {
                address = addressService.get(Integer.parseInt(operAddressId));
            } else {
                address = addressService.getLastAddressByMpiId(operMpiId);
            }
            //此字段前端已不使用
            order.setAddressCanSend(false);
            order.setExpressFee(BigDecimal.ZERO);
            if (null != address) {
                //可以在参数里传递快递费
                String paramExpressFee = MapValueUtil.getString(extInfo, "expressFee");
                //保存地址,费用信息
                BigDecimal expressFee = null;
                if (payModeSupport.isSupportMedicalInsureance()) {
                    expressFee = BigDecimal.ZERO;
                } else {
                    if (StringUtils.isNotEmpty(paramExpressFee)) {
                        expressFee = new BigDecimal(paramExpressFee);
                    } else {
                        expressFee = getExpressFee(order.getEnterpriseId(), address.getAddress3());
                    }
                }

                order.setExpressFee(expressFee);
                order.setReceiver(address.getReceiver());
                order.setRecMobile(address.getRecMobile());
                order.setRecTel(address.getRecTel());
                order.setZipCode(address.getZipCode());
                order.setAddressID(address.getAddressId());
                order.setAddress1(address.getAddress1());
                order.setAddress2(address.getAddress2());
                order.setAddress3(address.getAddress3());
                order.setAddress4(address.getAddress4());

                try {
                    Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
                    //校验地址是否可以配送
                        EnterpriseAddressService enterpriseAddressService = ApplicationUtils.getRecipeService(EnterpriseAddressService.class);
                        int flag = enterpriseAddressService.allAddressCanSendForOrder(order.getEnterpriseId(), address.getAddress1(), address.getAddress2(), address.getAddress3());
                        if (0 == flag) {
                            order.setAddressCanSend(true);
                        } else {
                            boolean b = 1 == toDbFlag && (payModeSupport.isSupportMedicalInsureance() || payModeSupport.isSupportOnlinePay());
                            if (b) {
                                //只有需要真正保存订单时才提示
                                result.setCode(RecipeResultBean.FAIL);
                                result.setMsg("该地址无法配送");
                            }
                        }
                } catch (Exception e) {
                    result.setCode(RecipeResultBean.FAIL);
                    result.setMsg(e.getMessage());
                }
            } else {
                //只有需要真正保存订单时才提示
                if (1 == toDbFlag) {
                    result.setCode(RecipeResultBean.NO_ADDRESS);
                    result.setMsg("没有配送地址");
                }
            }
        }
        order.setTotalFee(countOrderTotalFeeByRecipeInfo(order, firstRecipe, payModeSupport));
        //计算优惠券价格
        ICouponBaseService couponService = AppContextHolder.getBean("voucher.couponBaseService",ICouponBaseService.class);
        if (isUsefulCoupon(order.getCouponId())) {
            Coupon coupon = couponService.lockCouponById(order.getCouponId(), order.getTotalFee());
            LOGGER.info("RecipeOrderService use coupon , coupon info: {}.", JSONUtils.toString(coupon));
            if (coupon != null) {
                order.setCouponName(coupon.getCouponName());
                order.setCouponFee(coupon.getDiscountAmount());
            }
            if (order.getTotalFee().compareTo(order.getCouponFee()) > -1) {
                order.setActualPrice(order.getTotalFee().subtract(order.getCouponFee()).doubleValue());
            } else {
                order.setActualPrice(order.getTotalFee().doubleValue());
            }
        } else {
            Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
            if (payMode != RecipeBussConstant.PAYMODE_ONLINE) {
                //此时的实际费用是不包含药品费用的
                order.setActualPrice(order.getAuditFee().doubleValue());
            } else {
                order.setActualPrice(order.getTotalFee().doubleValue());
            }
        }
    }

    public BigDecimal reCalculateRecipeFee(Integer enterpriseId, List<Integer> recipeIds, Map<String, String> extInfo) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = getDAO(RecipeDetailDAO.class);

        DrugsEnterprise enterprise = drugsEnterpriseDAO.get(enterpriseId);
        BigDecimal recipeFee = BigDecimal.ZERO;
        if (null != enterprise && Integer.valueOf(0).equals(enterprise.getSettlementMode())) {
            List<Recipedetail> details = recipeDetailDAO.findByRecipeIds(recipeIds);
            Map<Integer, Double> drugIdCountRel = Maps.newHashMap();
            for (Recipedetail recipedetail : details) {
                Integer drugId = recipedetail.getDrugId();
                if (drugIdCountRel.containsKey(drugId)) {
                    drugIdCountRel.put(drugId, drugIdCountRel.get(recipedetail.getDrugId()) + recipedetail.getUseTotalDose());
                } else {
                    drugIdCountRel.put(drugId, recipedetail.getUseTotalDose());
                }
            }
            List<Integer> drugIds = Lists.newArrayList(drugIdCountRel.keySet());
            List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIds(enterpriseId, drugIds);
            if (CollectionUtils.isNotEmpty(saleDrugLists)) {
                BigDecimal total = BigDecimal.ZERO;
                try {
                    for (SaleDrugList saleDrug : saleDrugLists) {
                        //保留3位小数
                        total = total.add(saleDrug.getPrice().multiply(new BigDecimal(drugIdCountRel.get(saleDrug.getDrugId())))
                                .divide(BigDecimal.ONE, 3, RoundingMode.UP));
                    }
                    //重置药企处方价格
                    recipeFee = total;
                } catch (Exception e) {
                    LOGGER.warn("setOrderFee 重新计算药企ID为[{}]的结算价格出错. drugIds={}", enterpriseId,
                            JSONUtils.toString(drugIds), e);
                }
            }
        }else if (null != enterprise && Integer.valueOf(1).equals(enterprise.getSettlementMode())){
            List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
            for (Recipe recipe : recipeList) {
                if (null != recipe) {
                    if (null != recipe.getTotalMoney()) {
                        recipeFee = recipeFee.add(recipe.getTotalMoney());
                    }
                }
            }
        }
        if (extInfo == null) {
            //说明是重新计算药企处方费用的
            return recipeFee;
        } else {
            //优惠券调用，返回总费用
            //先判断处方是否已创建订单
            IRecipeOrderService orderService = RecipeAPI.getService(IRecipeOrderService.class);
            RecipeOrderBean order = orderService.getOrderByRecipeId(recipeIds.get(0));
            if (null == order) {
                RecipeBussResTO<RecipeOrderBean> resTO = orderService.createBlankOrder(recipeIds, extInfo);
                if(null != resTO){
                    order = resTO.getData();
                }else{
                    LOGGER.info("reCalculateRecipeFee createBlankOrder order is null.");
                    return null;
                }
            }
            return order.getTotalFee();
        }
    }

    public void setCreateOrderResult(OrderCreateResult result, RecipeOrder order, RecipePayModeSupportBean payModeSupport,
                                      Integer toDbFlag) {
        if (payModeSupport.isSupportMedicalInsureance()) {
            result.setCouponType(null);
        } else if (payModeSupport.isSupportCOD()) {
            result.setCouponType(null);
        } else if (payModeSupport.isSupportTFDS()) {
            result.setCouponType(null);
        } else if (payModeSupport.isSupportComplex()) {
            result.setCouponType(null);
        } else {
            //CouponBusTypeEnum
            //COUPON_BUSTYPE_RECIPE_HOME_PAYONLINE(5,CouponConstant.COUPON_BUSTYPE_RECIPE,CouponConstant.COUPON_SUBTYPE_RECIPE_HOME_PAYONLINE,"电子处方-配送到家-在线支付"),
            result.setCouponType(5);
        }
        result.setObject(ObjectCopyUtils.convert(order, RecipeOrderBean.class));
        if (RecipeResultBean.SUCCESS.equals(result.getCode()) && 1 == toDbFlag && null != order.getOrderId()) {
            result.setOrderCode(order.getOrderCode());
            result.setBusId(order.getOrderId());
        }

        LOGGER.info("createOrder finish. result={}", JSONUtils.toString(result));
    }

    /**
     * 创建订单后发送处方给药企及后续处理
     *
     * @param recipeList
     * @param result
     * @return
     */
    private OrderCreateResult sendRecipeAfterCreateOrder(List<Recipe> recipeList, OrderCreateResult result, Map<String, String> extInfo) {
        // 暂时支持单处方处理
        Recipe firstRecipe = recipeList.get(0);
        RemoteDrugEnterpriseService service = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        DrugEnterpriseResult result1 = service.pushSingleRecipeInfo(firstRecipe.getRecipeId());
        if (RecipeResultBean.SUCCESS.equals(result1.getCode())) {
            //钥世圈要求需要跳转页面，组装url
            if (DrugEnterpriseConstant.COMPANY_YSQ.equals(result1.getDrugsEnterprise().getCallSys())) {
                String ysqUrl = cacheService.getParam(ParameterConstant.KEY_YSQ_SKIP_URL);
                // 测试地址处理
                String test = "test";
                if (result1.getDrugsEnterprise().getAccount().contains(test)) {
                    ysqUrl = cacheService.getParam(ParameterConstant.KEY_YSQ_SKIP_URL + "_TEST");
                }
                if (StringUtils.isNotEmpty(ysqUrl)) {
                    PatientDTO patient = null;
                    if (StringUtils.isNotEmpty(firstRecipe.getMpiid())) {
                        patient = patientService.get(firstRecipe.getMpiid());
                    }
                    if (null == patient) {
                        result.setCode(RecipeResultBean.FAIL);
                        result.setMsg("患者不存在");
                    } else {
                        String appid = MapValueUtil.getString(extInfo, "appId");

                        /*Map<String, Object> ysqParamMap = new HashedMap();
                        ysqParamMap.put("mobile", patient.getMobile());
                        ysqParamMap.put("name", patient.getPatientName());
                        List<Map<String, String>> ysqCards = new ArrayList<>(1);
                        Map<String, String> ysqSubParamMap = new HashedMap();
                        ysqSubParamMap.put("card_name", patient.getPatientName());
                        ysqSubParamMap.put("id_card", "");
                        ysqSubParamMap.put("card_mobile", patient.getMobile());
                        ysqSubParamMap.put("card_gender", "");
                        ysqSubParamMap.put("card_no", "");

                        ysqCards.add(ysqSubParamMap);
                        ysqParamMap.put("cards", ysqCards);*/

                        ysqUrl = ysqUrl + "PreTitle/Details?appid=" + appid + "&inbillno=" +
                                firstRecipe.getClinicOrgan() + YsqRemoteService.YSQ_SPLIT + firstRecipe.getRecipeCode()
                                + "&gysCode=" + MapValueUtil.getString(extInfo, "gysCode")
                                + "&sendMethod=" + MapValueUtil.getString(extInfo, "sendMethod") + "&payMethod=" + MapValueUtil.getString(extInfo, "payMethod");
                        result.setMsg(ysqUrl);
                    }
                }
            }
        } else {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("推送药企失败");
            result.setError(result1.getMsg());
        }

        return result;
    }

    /**
     * 保存订单
     *
     * @param order
     * @param recipeList
     * @param result
     * @param recipeDAO
     * @param orderDAO
     * @return
     */
    public boolean saveOrderToDB(RecipeOrder order, List<Recipe> recipeList, Integer payMode,
                                  OrderCreateResult result, RecipeDAO recipeDAO, RecipeOrderDAO orderDAO) {
        List<Integer> recipeIds = FluentIterable.from(recipeList).transform(new Function<Recipe, Integer>() {
            @Override
            public Integer apply(Recipe recipe) {
                return recipe.getRecipeId();
            }
        }).toList();
        boolean saveFlag = true;
        try {
            createOrderToDB(order, recipeIds, orderDAO, recipeDAO);
        } catch (DAOException e) {
            LOGGER.warn("createOrder orderCode={}, error={}. ", order.getOrderCode(), e.getMessage());
            saveFlag = false;
        } finally {
            //如果小概率造成orderCode重复，则修改并重试
            if (!saveFlag) {
                try {
                    order.setOrderCode(getOrderCode(order.getMpiId()));
                    createOrderToDB(order, recipeIds, orderDAO, recipeDAO);
                    saveFlag = true;
                } catch (DAOException e) {
                    LOGGER.warn("createOrder again orderCode={}, error={}. ", order.getOrderCode(), e.getMessage());
                    saveFlag = false;
                    result.setCode(RecipeResultBean.FAIL);
                    result.setMsg("保存订单系统错误");
                }
            }
        }

        if (saveFlag) {
            //支付处理
            Map<String, Object> recipeInfo = Maps.newHashMap();
            if (RecipeBussConstant.PAYMODE_COMPLEX.equals(payMode)) {
                recipeInfo.put("payMode", null);
            } else {
                recipeInfo.put("payMode", payMode);
            }
            recipeInfo.put("payFlag", PayConstant.PAY_FLAG_NOT_PAY);
            recipeInfo.put("enterpriseId", order.getEnterpriseId());
            //更新处方信息
            this.updateRecipeInfo(false, result, recipeIds, recipeInfo);
        }

        return saveFlag;
    }

    /**
     * 医保支付请求数据
     *
     * @param order
     * @param recipeList
     * @return
     */
    private String applyMedicalInsurancePay(RecipeOrder order, List<Recipe> recipeList) {
        LOGGER.info("applyMedicalInsurancePayForRecipe start, params: orderId[{}],recipeIds={}", order.getOrderId(), order.getRecipeIdList());
        if (CollectionUtils.isEmpty(recipeList)) {
            return "处方对象为空";
        }

        String backInfo = "";
        boolean flag = true;
        try {
            Recipe recipe = recipeList.get(0);
            if (null != recipe) {
                flag = judgeIsSupportMedicalInsurance(recipe.getMpiid(), recipe.getClinicOrgan());
                if (flag) {
                    RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
                    PatientDTO patient = patientService.get(recipe.getMpiid());
                    String outTradeNo = (StringUtils.isEmpty(order.getOutTradeNo())) ? BusTypeEnum.RECIPE.getApplyNo() : order.getOutTradeNo();
                    HisServiceConfigBean hisServiceConfig = iHisConfigService.getHisConfigByOrganId(recipe.getClinicOrgan());
                    Map<String, Object> httpRequestParam = Maps.newHashMap();
                    httpRequestParam.put("mrn", recipe.getPatientID());
                    httpRequestParam.put("id_card_no", patient.getCertificate());
                    httpRequestParam.put("cfhs", new String[]{recipe.getRecipeCode()});
                    httpRequestParam.put("hospital_code", hisServiceConfig.getYkfPlatHospitalCode());
                    httpRequestParam.put("partner_trade_no", outTradeNo);
                    //回调地址在BASE端设置
                    httpRequestParam.put("callback_url", "");
                    httpRequestParam.put("need_app_notify", "1");
                    httpRequestParam.put("is_show_result", "1");
                    IPaymentService iPaymentService = ApplicationUtils.getBaseService(IPaymentService.class);
                    DabaiPayResult pr = iPaymentService.applyDaBaiPay(httpRequestParam);

                    Map<String, Object> orderInfo = Maps.newHashMap();
                    orderInfo.put("outTradeNo", outTradeNo);
                    String zero = "0";
                    // 50011  合作者交易号：[xxx]已存在
                    String number = "50011";
                    if (zero.equals(pr.getCode())) {
                        String tradeNo = pr.getData().getTrade_no();
                        orderInfo.put("tradeNo", tradeNo);
                        RecipeResultBean resultBean = this.updateOrderInfo(order.getOrderCode(), orderInfo, null);
                        if (RecipeResultBean.FAIL.equals(resultBean.getCode())) {
                            backInfo = "订单更新失败";
                        }
                        if (StringUtils.isEmpty(backInfo)) {
                            //订单更新成功，更新处方单字段，用于 医保支付-自费支付-医保支付 过程时寻找之前的订单
                            boolean recipeSave = recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), ImmutableMap.of("outTradeNo", outTradeNo));
                            if (!recipeSave) {
                                backInfo = "处方单更新失败";
                            }
                        }
                    } else if (number.equals(pr.getCode())) {
                        backInfo = "";
                    } else {
                        backInfo = pr.getMessage();
                    }
                } else {
                    backInfo = "医院配置无法进行医保支付";
                }
            } else {
                backInfo = "处方对象不存在";
            }
        } catch (Exception e) {
            LOGGER.error("applyMedicalInsurancePay error, orderId[{}], error ", order.getOrderId(), e);
            backInfo = "医保接口异常";
        }
        return backInfo;
    }

    /**
     * 判断能否进行医保支付
     *
     * @param mpiId
     * @param organId
     * @return
     */
    private boolean judgeIsSupportMedicalInsurance(String mpiId, Integer organId) {
        HisServiceConfigBean hisServiceConfig = iHisConfigService.getHisConfigByOrganId(organId);
        PatientDTO patient = patientService.get(mpiId);
        if (ValidateUtil.blankString(patient.getPatientType()) || String.valueOf(PayConstant.PAY_TYPE_SELF_FINANCED).equals(patient.getPatientType())) {
            LOGGER.info("judgeIsSupportMedicalInsurance patient not support, mpiId[{}]", patient.getMpiId());
            return false;
        }
        if (ValidateUtil.isNotTrue(hisServiceConfig.getSupportMedicalInsurance())) {
            LOGGER.info("judgeIsSupportMedicalInsurance hisServiceConfig not support, id[{}]", hisServiceConfig.getId());
            return false;
        }
        return true;
    }

    /**
     * 根据订单编号取消订单
     *
     * @param orderCode
     * @param status
     * @return
     */
    @RpcService
    public RecipeResultBean cancelOrderByCode(String orderCode, Integer status) {
        RecipeResultBean result = RecipeResultBean.getSuccess();
        if (StringUtils.isEmpty(orderCode) || null == status) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("缺少参数");
        }

        if (RecipeResultBean.SUCCESS.equals(result.getCode())) {
            result = cancelOrder(getDAO(RecipeOrderDAO.class).getByOrderCode(orderCode), status);
        }

        return result;
    }

    /**
     * 根据处方单号取消订单
     *
     * @param recipeId
     * @param status
     * @return
     */
    @RpcService
    public RecipeResultBean cancelOrderByRecipeId(Integer recipeId, Integer status) {
        RecipeResultBean result = RecipeResultBean.getSuccess();
        if (null == recipeId || null == status) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("缺少参数");
        }

        if (RecipeResultBean.SUCCESS.equals(result.getCode())) {
            result = cancelOrder(getDAO(RecipeOrderDAO.class).getOrderByRecipeId(recipeId), status);
        }

        return result;
    }

    /**
     * @param orderId
     * @param status
     * @return
     */
    @RpcService
    public RecipeResultBean cancelOrderById(Integer orderId, Integer status) {
        RecipeResultBean result = RecipeResultBean.getSuccess();
        if (null == orderId || null == status) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("缺少参数");
        }

        if (RecipeResultBean.SUCCESS.equals(result.getCode())) {
            result = cancelOrder(getDAO(RecipeOrderDAO.class).get(orderId), status);
        }

        return result;
    }

    /**
     * 取消订单
     *
     * @param order
     * @return
     */
    public RecipeResultBean cancelOrder(RecipeOrder order, Integer status) {
        RecipeResultBean result = RecipeResultBean.getSuccess();
        if (null == order || null == status) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("缺少参数");
        }

        if (RecipeResultBean.SUCCESS.equals(result.getCode())) {
            ICouponService couponService = ApplicationUtils.getBaseService(ICouponService.class);

            Map<String, Object> orderAttrMap = Maps.newHashMap();
            orderAttrMap.put("effective", 0);
            orderAttrMap.put("status", status);
//            orderAttrMap.put("finishTime", Calendar.getInstance().getTime());

            if (null != order) {
                //解锁优惠券
                if (isUsefulCoupon(order.getCouponId())) {
                    try {
                        couponService.unlockCoupon(order.getCouponId());
                        orderAttrMap.put("couponId", null);
                    } catch (Exception e) {
                        LOGGER.error("cancelOrder unlock coupon error. couponId={}, error={}", order.getCouponId(), e.getMessage());
                    }
                }
                this.updateOrderInfo(order.getOrderCode(), orderAttrMap, result);

                if (status.equals(OrderStatusConstant.CANCEL_MANUAL)) {
                    //订单手动取消，处方单可以进行重新支付
                    //更新处方的orderCode
                    RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
                    recipeDAO.updateOrderCodeToNullByOrderCodeAndClearChoose(order.getOrderCode());
                }
            }
        }

        return result;
    }

    @RpcService
    public RecipeResultBean getOrderDetailById(Integer orderId) {
        RecipeResultBean result = RecipeResultBean.getSuccess();
        if (null == orderId) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("缺少参数");
        }

        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);

        RecipeOrder order = orderDAO.get(orderId);
        if (null != order) {
            List<PatientRecipeDTO> patientRecipeBeanList = new ArrayList<>(10);
            List<Recipe> recipeList = null;
            if (1 == order.getEffective()) {
                recipeList = recipeDAO.findRecipeListByOrderCode(order.getOrderCode());
                if (CollectionUtils.isEmpty(recipeList) && StringUtils.isNotEmpty(order.getRecipeIdList())) {
                    //如果没有数据，则使用RecipeIdList字段
                    List<Integer> recipeIdList = JSONUtils.parse(order.getRecipeIdList(), List.class);
                    if (CollectionUtils.isNotEmpty(recipeIdList)) {
                        recipeList = recipeDAO.findByRecipeIds(recipeIdList);
                    }
                }
            } else {
                //如果是已取消的单子，则需要从order表的RecipeIdList字段取历史处方单
                if (StringUtils.isNotEmpty(order.getRecipeIdList())) {
                    List<Integer> recipeIdList = JSONUtils.parse(order.getRecipeIdList(), List.class);
                    if (CollectionUtils.isNotEmpty(recipeIdList)) {
                        recipeList = recipeDAO.findByRecipeIds(recipeIdList);
                    }
                }
            }

            Map<Integer, String> enterpriseAccountMap = Maps.newHashMap();
            if (CollectionUtils.isNotEmpty(recipeList)) {
                //设置地址，先取处方单address4的值，没有则取订单地址
                if (StringUtils.isNotEmpty(recipeList.get(0).getAddress4())) {
                    order.setCompleteAddress(recipeList.get(0).getAddress4());
                } else {
                    order.setCompleteAddress(commonRemoteService.getCompleteAddress(order));
                }

                RecipeDetailDAO detailDAO = getDAO(RecipeDetailDAO.class);
                PatientRecipeDTO prb;
                List<Recipedetail> recipedetails;
                for (Recipe recipe : recipeList) {
                    prb = new PatientRecipeDTO();
                    prb.setRecipeId(recipe.getRecipeId());
                    prb.setOrganDiseaseName(recipe.getOrganDiseaseName());
                    prb.setMpiId(recipe.getMpiid());
                    prb.setSignDate(recipe.getSignDate());
                    prb.setPatientName(patientService.getNameByMpiId(recipe.getMpiid()));
                    prb.setStatusCode(recipe.getStatus());
                    prb.setPayMode(recipe.getPayMode());
                    prb.setRecipeType(recipe.getRecipeType());
                    prb.setRecipeMode(recipe.getRecipeMode());
                    prb.setChemistSignFile(recipe.getChemistSignFile());
                    //药品详情
                    recipedetails = detailDAO.findByRecipeId(recipe.getRecipeId());
                    prb.setRecipeDetail(ObjectCopyUtils.convert(recipedetails, RecipeDetailBean.class));
                    if (RecipeStatusConstant.CHECK_PASS == recipe.getStatus()
                            && OrderStatusConstant.READY_PAY.equals(order.getStatus())) {
                        prb.setRecipeSurplusHours(RecipeServiceSub.getRecipeSurplusHours(recipe.getSignDate()));
                    }
                    //添加处方的取药窗口
                    OrganService organService = BasicAPI.getService(OrganService.class);
                    OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());
                    //取处方详情中的药品的取药窗口信息
                    if(CollectionUtils.isNotEmpty(recipedetails) && null != recipedetails.get(0).getPharmNo()){
                        prb.setGetDrugWindow(organDTO.getName() + recipedetails.get(0).getPharmNo() + "取药窗口");
                    }

                    patientRecipeBeanList.add(prb);

                    if (1 == order.getEffective()) {
                        String account = enterpriseAccountMap.get(order.getEnterpriseId());
                        if (StringUtils.isEmpty(account)) {
                            account = remoteDrugEnterpriseService.getDepAccount(order.getEnterpriseId());
                            enterpriseAccountMap.put(order.getEnterpriseId(), account);
                        }
                        //钥世圈处理
                        if (DrugEnterpriseConstant.COMPANY_YSQ.equals(account)) {
                            //前端在code=1的情况下会去判断msg是否为空，不为空则进行页面跳转，所以此处成功情况下msg不要赋值
                            result.setMsg(remoteDrugEnterpriseService.getYsqOrderInfoUrl(recipe));
                        }
                    }
                }
            }
            if (order.getEnterpriseId() != null) {
                DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
                order.setEnterpriseName(drugsEnterprise.getName());
            }
            RecipeOrderBean orderBean = ObjectCopyUtils.convert(order, RecipeOrderBean.class);
            orderBean.setList(patientRecipeBeanList);
            ICouponBaseService couponService = AppContextHolder.getBean("voucher.couponBaseService",ICouponBaseService.class);
            if(null != orderBean.getCouponId()){
                Coupon coupon = couponService.getCouponById(orderBean.getCouponId());
                if(null != coupon){
                    orderBean.setCouponDesc(coupon.getCouponDesc());
                }
            }

            result.setObject(orderBean);
            // 支付完成后跳转到订单详情页需要加挂号费服务费可配置
            result.setExt(RecipeUtil.getParamFromOgainConfig(order));
            //在扩展内容中设置下载处方签的判断
            getDownConfig(result, order, recipeList);
            //在扩展内容中添加展示审核金额
            getShowAuditFeeAndTips(result, order, recipeList);
        } else {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("不存在ID为" + orderId + "的订单");
        }

        return result;
    }

    /**
     * @method  getShowAuditFee
     * @description 展示审核金额按钮的判断
     * @date: 2019/9/20
     * @author: JRK
     * @param result 返回结果集
     * @param order 返回的订单
     * @param recipeList 订单关联的处方列表
     * @return void
     */
    public void getShowAuditFeeAndTips(RecipeResultBean result, RecipeOrder order, List<Recipe> recipeList) {

        Map<String, String> ext = result.getExt();
        if(null == ext){
            ext = Maps.newHashMap();
        }
        Boolean showAuditFee = false;
        if(CollectionUtils.isNotEmpty(recipeList)){
            Recipe nowRecipe = recipeList.get(0);
            if(null != nowRecipe){
                //判断时候需要展示审方费用：
                //当不是不需要审核
                showAuditFee = ReviewTypeConstant.Not_Need_Check != nowRecipe.getReviewType();
                //添加文案提示的
                getOrderTips(ext, nowRecipe, order);
                //设置页面上提示文案的颜色信息
                //添加一次审核不通过的判断，等价于待审核
                Integer recipestatus = nowRecipe.getStatus();
                if(RecipecCheckStatusConstant.First_Check_No_Pass ==nowRecipe.getCheckStatus()){
                    recipestatus = RecipeStatusConstant.READY_CHECK_YS;
                }
                RecipeTipesColorTypeEnum colorType = RecipeTipesColorTypeEnum.fromRecipeStatus(recipestatus);
                if(null != colorType){
                    ext.put("tipsType", colorType.getShowType());
                }
            }
        }

        ext.put("showAuditFee", showAuditFee ?  "1" : "0");
        result.setExt(ext);
    }

    /**
     * @method  getOrderTips
     * @description 获取订单的提示
     * @date: 2019/9/29
     * @author: JRK
     * @param ext
     * @param nowRecipe
     * @return void
     */
    private void getOrderTips(Map<String, String> ext, Recipe nowRecipe, RecipeOrder order) {
        if (nowRecipe.getRecipeMode() == RecipeBussConstant.RECIPEMODE_ZJJGPT) {
            ext.put("tips", RecipeServiceSub.getTipsByStatusForPatient(nowRecipe, order));
        } else {
            PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
            ext.put("tips", purchaseService.getTipsByStatusForPatient(nowRecipe, order));
        }
    }

    /**
     * @method  getDownConfig
     * @description 下载处方签展示按钮的判断
     * @date: 2019/9/20
     * @author: JRK
     * @param result 返回结果集
     * @param order 返回的订单
     * @param recipeList 订单关联的处方列表
     * @return void
     */
    public void getDownConfig(RecipeResultBean result, RecipeOrder order, List<Recipe> recipeList) {
        //判断是否展示下载处方签按钮：1.在下载处方购药方式
        //2.是否是后置，后置：判断审核是否审核通过状态
        //3.不是后置:判断实际金额是否为0：为0则ordercode关联则展示，不为0支付则展示
        Map<String, String> ext = result.getExt();
        if(null == ext){
            ext = Maps.newHashMap();
        }
        String isDownload = "0";
        if(CollectionUtils.isNotEmpty(recipeList)){
            Recipe nowRecipe = recipeList.get(0);
            if(RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(nowRecipe.getGiveMode())){
                if(ReviewTypeConstant.Postposition_Check == nowRecipe.getReviewType()){
                    if(Arrays.asList(showDownloadRecipeStatus).contains(nowRecipe.getStatus())){
                        isDownload = "1";
                    }
                }else if(ReviewTypeConstant.Not_Need_Check == nowRecipe.getReviewType() && RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(nowRecipe.getGiveMode()) && RecipeStatusConstant.FINISH != nowRecipe.getStatus()){
                    //这里当是不需审核，且选择的下载处方的购药方式的时候，没有产生订单，且不是完成状态，直接判断没有选定购药方式
                    if(1 == nowRecipe.getChooseFlag()){
                        isDownload = "1";
                    }
                }else{
                    if(null != nowRecipe.getOrderCode() && null != order && RecipeStatusConstant.FINISH != nowRecipe.getStatus()){
                        if(0 == order.getActualPrice() || (0 < order.getActualPrice() && 1 == nowRecipe.getPayFlag()))
                            isDownload = "1";
                    }
                }
            }
        }

        ext.put("isDownload", isDownload);
        result.setExt(ext);
    }


    /**
     * 获取订单详情
     *
     * @param orderCode
     * @return
     */
    @RpcService
    public RecipeResultBean getOrderDetail(String orderCode) {
        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
        RecipeOrder order = orderDAO.getByOrderCode(orderCode);
        if (order != null){
            checkUserHasPermission((Integer)JSONUtils.parse(order.getRecipeIdList(), List.class).get(0));
        }
        return this.getOrderDetailById(order.getOrderId());
    }

    /**
     * 获取运费
     *
     * @param enterpriseId
     * @param address
     * @return
     */
    private BigDecimal getExpressFee(Integer enterpriseId, String address) {
        if (null == enterpriseId || StringUtils.isEmpty(address)) {
            return BigDecimal.ZERO;
        }
        DrugDistributionPriceService priceService = ApplicationUtils.getRecipeService(DrugDistributionPriceService.class);
        DrugDistributionPriceBean expressFee = priceService.getDistributionPriceByEnterpriseIdAndAddrArea(enterpriseId, address);
        if (null != expressFee) {
            return expressFee.getDistributionPrice();
        }

        return BigDecimal.ZERO;
    }

    /**
     * 订单支付完成后调用 (包括支付完成和退款都会调用)
     *
     * @param orderCode
     * @param payFlag
     * @return
     */
    @RpcService
    public RecipeResultBean finishOrderPay(String orderCode, int payFlag, Integer payMode) {
        if (null == payMode) {
            payMode = RecipeBussConstant.PAYMODE_ONLINE;
        }
        return finishOrderPayImpl(orderCode, payFlag, payMode);
    }

    /**
     * 线下支付模式调用
     *
     * @param orderCode
     * @return
     */
    @RpcService
    public RecipeResultBean  finishOrderPayWithoutPay(String orderCode, Integer payMode) {
        return finishOrderPayImpl(orderCode, PayConstant.PAY_FLAG_NOT_PAY, payMode);
    }

    public RecipeResultBean finishOrderPayImpl(String orderCode, int payFlag, Integer payMode) {
        LOGGER.info("finishOrderPayImpl is get! orderCode={}", orderCode);
        RecipeResultBean result = RecipeResultBean.getSuccess();

        if (RecipeResultBean.SUCCESS.equals(result.getCode())) {
            Map<String, Object> attrMap = Maps.newHashMap();
            attrMap.put("payFlag", payFlag);
            //date 20190919
            //根据不同的购药方式设置订单的状态
            RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
            List<Integer> recipeIds = recipeDAO.findRecipeIdsByOrderCode(orderCode);
            int payStatus = 0;
            int noPayStatus = 0;

            if(null != recipeIds){
                Recipe nowRecipe = recipeDAO.get(recipeIds.get(0));
                if(null != nowRecipe){
                    Integer reviewType = nowRecipe.getReviewType();
                    Integer giveMode = nowRecipe.getGiveMode();
                    //首先判断是否支付成功调用，还是支付前调用
                    if (PayConstant.PAY_FLAG_PAY_SUCCESS == payFlag) {
                        //支付成功后
                        payStatus = getPayStatus(reviewType, giveMode, nowRecipe);
                        attrMap.put("payTime", Calendar.getInstance().getTime());
                        attrMap.put("status", payStatus);
                        attrMap.put("effective", 1);
                        //date 20191017
                        //添加使用优惠券(支付后释放)
                        useCoupon(nowRecipe, payMode);
                        sendTfdsMsg(nowRecipe, payMode, orderCode);
                    } else if (PayConstant.PAY_FLAG_NOT_PAY == payFlag) {
                        //支付前调用
                        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
                        RecipeOrder order = recipeOrderDAO.getByOrderCode(orderCode);
                        if(null != order){
                            if(0 == order.getActualPrice()){
                                noPayStatus = getPayStatus(reviewType, giveMode, nowRecipe);
                                //date 20191017
                                //添加使用优惠券（不需支付，释放）
                                useCoupon(nowRecipe, payMode);
                                sendTfdsMsg(nowRecipe, payMode, orderCode);
                            }else{
                                noPayStatus = OrderStatusConstant.READY_PAY;
                            }
                            attrMap.put("effective", 1);
                            attrMap.put("status", noPayStatus);
                        }
                    }
                }
            }


            this.updateOrderInfo(orderCode, attrMap, result);
        }

        //处理处方单相关
        if (RecipeResultBean.SUCCESS.equals(result.getCode())) {
            RecipeDAO recipeDAO = getDAO(RecipeDAO.class);

            Map<String, Object> recipeInfo = Maps.newHashMap();
            recipeInfo.put("payFlag", payFlag);
            recipeInfo.put("payMode", payMode);
            List<Integer> recipeIds = recipeDAO.findRecipeIdsByOrderCode(orderCode);
            this.updateRecipeInfo(true, result, recipeIds, recipeInfo);
        }

        return result;
    }

    /**
     * @method  useCoupon
     * @description 使用优惠券
     * @date: 2019/10/17
     * @author: JRK
     * @param nowRecipe 处方
    * @param payMode 支付方式
     * @return void
     */
    private void useCoupon(Recipe nowRecipe, Integer payMode){
        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
        RecipeOrder order = recipeOrderDAO.getByOrderCode(nowRecipe.getOrderCode());
        if (nowRecipe.getPayMode() == RecipeBussConstant.PAYMODE_ONLINE && isUsefulCoupon(order.getCouponId())) {
            ICouponBaseService couponService = AppContextHolder.getBean("voucher.couponBaseService",ICouponBaseService.class);
            couponService.useCouponById(order.getCouponId());
        }
    }

    //药店有库存或者无库存备货给患者推送消息
    private void sendTfdsMsg(Recipe nowRecipe, Integer payMode, String orderCode) {
        //药店取药推送
        LOGGER.info("sendTfdsMsg nowRecipe:{}.", JSONUtils.toString(nowRecipe));
        if (RecipeBussConstant.PAYMODE_TFDS.equals(payMode) && nowRecipe.getReviewType() != 2) {
            RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
            RecipeOrder order = recipeOrderDAO.getByOrderCode(orderCode);
            //这里去的是订单中存的药企信息
            if (order.getEnterpriseId() == null) {
                LOGGER.info("审方前置或者不审核-药店取药-药企为空");
            } else {
                DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(nowRecipe.getEnterpriseId());
                boolean scanFlag = remoteDrugService.scanStock(order.getEnterpriseId(), drugsEnterprise);
                if (scanFlag) {
                    //表示需要进行库存校验并且有库存
                    RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_DRUG_HAVE_STOCK, nowRecipe);
                } else if (drugsEnterprise.getCheckInventoryFlag() == 2) {
                    //表示无库存但是药店可备货
                    RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_DRUG_NO_STOCK_READY, nowRecipe);
                }
            }
        }

    }

    /**
     * @method  getPayStatus
     * @description 获取订单的处理的状态
     * @date: 2019/9/20
     * @author: JRK
     * @param reviewType 审核方式
     * @param giveMode 购药方式
     * @return int 订单的修改状态
     */
    private int getPayStatus(Integer reviewType, Integer giveMode, Recipe nowRecipe) {
        int payStatus = 0;
        //支付成功、支付前不需要支付时判断审核方式
        if(ReviewTypeConstant.Postposition_Check == reviewType){
            //后置
            payStatus = OrderStatusConstant.READY_CHECK;
        }else{
            //前置、不需要审核，根据购药方式判断
//            if(RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode) ||
//                    RecipeBussConstant.GIVEMODE_TO_HOS.equals(giveMode) ||
//                    RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(giveMode)){
//                payStatus = OrderStatusConstant.READY_GET_DRUG;
//            }else if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode)){
//                payStatus = OrderStatusConstant.READY_SEND;
//            }
            //修改成根据购药方式来
            PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
            payStatus = purchaseService.getOrderStatus(nowRecipe);
        }
        return payStatus;
    }

    /**
     * 完成订单
     *
     * @param orderCode
     * @param payMode
     * @return
     */
    @RpcService
    public RecipeResultBean finishOrder(String orderCode, Integer payMode, Map<String, Object> orderAttr) {
        RecipeResultBean result = RecipeResultBean.getSuccess();
        if (StringUtils.isEmpty(orderCode)) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("缺少参数");
        }

        if (RecipeResultBean.SUCCESS.equals(result.getCode())) {
            Map<String, Object> attrMap = Maps.newHashMap();
            attrMap.put("effective", 1);
            attrMap.put("payFlag", PayConstant.PAY_FLAG_PAY_SUCCESS);
            if (RecipeBussConstant.PAYMODE_COD.equals(payMode) || RecipeBussConstant.PAYMODE_TFDS.equals(payMode)) {
                attrMap.put("payTime", Calendar.getInstance().getTime());
            }
            attrMap.put("finishTime", Calendar.getInstance().getTime());
            attrMap.put("status", OrderStatusConstant.FINISH);
            if (null != orderAttr) {
                attrMap.putAll(orderAttr);
            }
            this.updateOrderInfo(orderCode, attrMap, result);
        }

        return result;
    }

    /**
     * 从微信模板消息跳转时 先获取一下是否需要跳转第三方地址
     *
     * @return
     */
    @RpcService
    public String getThirdUrl(Integer recipeId) {
        String thirdUrl = "";
        if (null == recipeId) {
            return thirdUrl;
        }
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
        RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);

        Recipe recipe = recipeDAO.get(recipeId);
        if (null != recipe) {
            RecipeOrder order = recipeOrderDAO.getOrderByRecipeId(recipeId);
            if (null == order) {
                return thirdUrl;
            }

            //钥世圈处理
            /*if (DrugEnterpriseConstant.COMPANY_YSQ.equals(remoteDrugEnterpriseService.getDepAccount(order.getEnterpriseId()))) {
                thirdUrl = remoteDrugEnterpriseService.getYsqOrderInfoUrl(recipe);
            }*/
        }
        return thirdUrl;
    }

    /**
     * 根据处方单ID获取订单编号
     *
     * @param recipeId
     * @return
     */
    public String getOrderCodeByRecipeId(Integer recipeId) {
        if (null == recipeId) {
            return null;
        }

        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
        return recipeOrderDAO.getOrderCodeByRecipeId(recipeId);
    }

    /**
     * 业务类型1位+时间戳后10位+随机码4位
     *
     * @return
     */
    public String getOrderCode(String mpiId) {
        StringBuilder orderCode = new StringBuilder();
        orderCode.append(BussTypeConstant.RECIPE);
        String time = Long.toString(Calendar.getInstance().getTimeInMillis());
        orderCode.append(time.substring(time.length() - 10));
        orderCode.append(new Random().nextInt(9000) + 1000);
        return orderCode.toString();
    }

    /**
     * 获取处方总额(未优惠前)
     *
     * @param order
     * @return
     */
    public BigDecimal countOrderTotalFee(RecipeOrder order) {
        return countOrderTotalFeeWithCoupon(null, order);
    }

    public BigDecimal countOrderTotalFeeByRecipeInfo(RecipeOrder order, Recipe recipe, RecipePayModeSupportBean payModeSupport) {
        BigDecimal full = BigDecimal.ZERO;
        //date 20191015
        //添加判断，当处方选择购药方式是下载处方，不计算药品费用
        //处方费用
        if(!payModeSupport.isSupportDownload()) {
            full = full.add(order.getRecipeFee());
        }

        //配送费
        if (null != order.getExpressFee()) {
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

       //审方费,计算当审方模式不是不需要你审方才计算
        if (null != recipe && ReviewTypeConstant.Not_Need_Check != recipe.getReviewType()
                && null != order.getAuditFee()) {
            full = full.add(order.getAuditFee());
        }

        //其他服务费
        if (null != order.getOtherFee()) {
            full = full.add(order.getOtherFee());
        }

        return full.divide(BigDecimal.ONE, 3, RoundingMode.UP);
    }

    /**
     * 获取带优惠金额的处方总额
     *
     * @param recipeFee 优惠后的处方金额
     * @param order
     * @return
     */
    public BigDecimal countOrderTotalFeeWithCoupon(BigDecimal recipeFee, RecipeOrder order) {
        BigDecimal full = BigDecimal.ZERO;

        //处方费用
        if (null != recipeFee) {
            full = full.add(recipeFee);
        } else {
            full = full.add(order.getRecipeFee());
        }

        //配送费
        if (null != order.getExpressFee()) {
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

        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        List<Recipe> recipes = recipeDAO.getRecipeListByOrderCodes(Arrays.asList(order.getOrderCode()));
        //审方费,计算当审方模式不是不需要你审方才计算
        if (CollectionUtils.isNotEmpty(recipes) && ReviewTypeConstant.Not_Need_Check != recipes.get(0).getReviewType() && null != order.getAuditFee() ) {
            full = full.add(order.getAuditFee());
        }

        //其他服务费
        if (null != order.getOtherFee()) {
            full = full.add(order.getOtherFee());
        }

        return full.divide(BigDecimal.ONE, 3, RoundingMode.UP);
    }

    /**
     * 保存订单并修改处方所属订单
     *
     * @param order
     * @param recipeIds
     * @param orderDAO
     * @param recipeDAO
     * @throws DAOException
     */
    private Integer createOrderToDB(RecipeOrder order, List<Integer> recipeIds,
                                    RecipeOrderDAO orderDAO, RecipeDAO recipeDAO) throws DAOException {
        order = orderDAO.save(order);
        if (null != order.getOrderId()) {
            recipeDAO.updateOrderCodeByRecipeIds(recipeIds, order.getOrderCode());
        }

        return order.getOrderId();
    }

    /**
     * 更新订单信息
     *
     * @param orderCode
     * @param attrMap
     * @param result
     * @return
     */
    public RecipeResultBean updateOrderInfo(String orderCode, Map<String, ?> attrMap, RecipeResultBean result) {
        if (null == result) {
            result = RecipeResultBean.getSuccess();
        }

        if (StringUtils.isEmpty(orderCode) || null == attrMap) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("缺少参数");
            return result;
        }

        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);

        try {
            boolean flag = orderDAO.updateByOrdeCode(orderCode, attrMap);
            if (!flag) {
                result.setCode(RecipeResultBean.FAIL);
                result.setError("订单更新失败");
            }
        } catch (Exception e) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("订单更新失败," + e.getMessage());
        }

        return result;
    }

    /**
     * 根据订单编号更新处方信息
     *
     * @param saveFlag
     * @param result
     * @param recipeIds
     * @param recipeInfo
     * @return
     */
    private RecipeResultBean updateRecipeInfo(boolean saveFlag, RecipeResultBean result,
                                              List<Integer> recipeIds, Map<String, Object> recipeInfo) {
        if (null == result) {
            result = RecipeResultBean.getSuccess();
        }
        if (CollectionUtils.isNotEmpty(recipeIds)) {
            RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
            RecipeResultBean resultBean;
            for (Integer recipeId : recipeIds) {
                Integer payFlag = MapValueUtil.getInteger(recipeInfo, "payFlag");
                if (Integer.valueOf(PayConstant.PAY_FLAG_PAY_SUCCESS).equals(payFlag)
                        || Integer.valueOf(PayConstant.PAY_FLAG_NOT_PAY).equals(payFlag)) {

                    resultBean = recipeService.updateRecipePayResultImplForOrder(saveFlag, recipeId, payFlag, recipeInfo);
                    if (RecipeResultBean.FAIL.equals(resultBean.getCode())) {
                        result.setCode(RecipeResultBean.FAIL);
                        result.setError(resultBean.getError());
                        break;
                    }
                }
            }
        } else {
            LOGGER.error("updateRecipeInfo param recipeIds size is zero. result={}", JSONUtils.toString(result));
        }

        return result;
    }

    /**
     * 判断优惠券是否有用
     *
     * @param couponId
     * @return
     */
    public boolean isUsefulCoupon(Integer couponId) {
        if (null != couponId && couponId > 0) {
            return true;
        }

        return false;
    }

}
