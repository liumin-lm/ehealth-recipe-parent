package recipe.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.currentuserinfo.model.SimpleThirdBean;
import com.ngari.base.currentuserinfo.model.SimpleWxAccountBean;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.base.hisconfig.model.HisServiceConfigBean;
import com.ngari.base.hisconfig.service.IHisConfigService;
import com.ngari.base.organconfig.model.OrganConfigBean;
import com.ngari.base.organconfig.service.IOrganConfigService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.base.payment.model.DabaiPayResult;
import com.ngari.base.payment.service.IPaymentService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.bus.coupon.service.ICouponService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.his.recipe.mode.RecipeThirdUrlReqTO;
import com.ngari.his.recipe.service.IRecipeEnterpriseService;
import com.ngari.infra.logistics.mode.CreateLogisticsOrderDto;
import com.ngari.infra.logistics.mode.WriteBackLogisticsOrderDto;
import com.ngari.infra.logistics.service.ILogisticsOrderService;
import com.ngari.patient.dto.AddressDTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.*;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.RecipeAPI;
import com.ngari.recipe.common.RecipeBussResTO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugdistributionprice.model.DrugDistributionPriceBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.recipeorder.model.ApothecaryVO;
import com.ngari.recipe.recipeorder.model.MedicalRespData;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import com.ngari.recipe.recipeorder.service.IRecipeOrderService;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import com.ngari.wxpay.service.INgariPayService;
import coupon.api.service.ICouponBaseService;
import coupon.api.vo.Coupon;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import static ctd.persistence.DAOFactory.getDAO;
import ctd.persistence.exception.DAOException;
import ctd.schema.exception.ValidateException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ctd.util.event.GlobalEventExecFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.PurchaseResponse;
import recipe.bean.RecipePayModeSupportBean;
import recipe.bean.ThirdResultBean;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.common.CommonConstant;
import recipe.common.ResponseUtils;
import recipe.constant.*;
import recipe.dao.*;
import recipe.drugsenterprise.*;
import recipe.easypay.IEasyPayService;
import recipe.factory.status.constant.GiveModeEnum;
import recipe.givemode.business.GiveModeFactory;
import recipe.purchase.PurchaseService;
import recipe.service.common.RecipeCacheService;
import recipe.service.manager.EmrRecipeManager;
import recipe.thread.CardDataUploadRunable;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.ChinaIDNumberUtil;
import recipe.util.MapValueUtil;
import recipe.util.ValidateUtil;
import wnpay.api.model.WnAccountDetail;
import wnpay.api.model.WnAccountSplitParam;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 处方订单管理
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/2/13.
 */
@RpcBean(value = "recipeOrderService")
public class RecipeOrderService extends RecipeBaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeOrderService.class);

    private PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);

    private IHisConfigService iHisConfigService = ApplicationUtils.getBaseService(IHisConfigService.class);

    private RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);

    private static Integer[] showDownloadRecipeStatus = new Integer[]{RecipeStatusConstant.CHECK_PASS_YS, RecipeStatusConstant.RECIPE_DOWNLOADED};

    @Autowired
    private RecipeOrderDAO recipeOrderDAO;

    @Autowired
    private RecipeListService recipeListService;

    @Autowired
    private RecipeService recipeService;

    @Resource
    private DrugsEnterpriseDAO drugsEnterpriseDAO;

    @Autowired
    private RecipeHisService recipeHisService;

    @Autowired
    private RecipeDAO recipeDAO;

    @Autowired
    private HisRecipeService hisRecipeService;

    @Autowired
    private HisRecipeDAO hisRecipeDAO;

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
        if (null != result && RecipeResultBean.FAIL.equals(result.getCode())) {
            throw new DAOException(609, result.getMsg() == null ? "创建订单失败" : result.getMsg());
        }
        if (null != result && RecipeResultBean.SUCCESS.equals(result.getCode()) && null != result.getObject() && result.getObject() instanceof RecipeOrderBean) {
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
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);

        //获取药企信息
        DrugsEnterprise drugsEnterprise = null;
        if (null == depId) {
            OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = getDAO(OrganAndDrugsepRelationDAO.class);
            List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(recipe.getClinicOrgan(), 1);
            drugsEnterprise = drugsEnterprises.get(0);
        } else {
            DrugsEnterpriseDAO drugsEnterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
            drugsEnterprise = drugsEnterpriseDAO.get(depId);
        }

        PurchaseResponse response = ResponseUtils.getFailResponse(PurchaseResponse.class, "");

        //暂时没找到好的控制字段，只能用写死天猫了
        if (!"tmdyf".equals(drugsEnterprise.getAccount())) {
            response.setCode(PurchaseResponse.CHECKWARN);
            return response;
        }

        //根据药企ID获取具体跳转的url地址
        try {
            RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            AccessDrugEnterpriseService remoteService = remoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
            remoteService.getJumpUrl(response, recipe, drugsEnterprise);
        } catch (Exception e) {
            LOGGER.error("获取跳转实现异常--", e);
            response.setCode(CommonConstant.FAIL);
            response.setMsg("获取跳转实现异常--{}" + e);
            return response;
        }

        return response;
    }

    /**
     * 订单创建------前端不会调用这里--前端是通过findConfirmOrderInfoExt调用了这里
     *
     * @param recipeIds 合并处方单ID
     * @param extInfo   {"operMpiId":"当前操作者编码","addressId":"当前选中地址","payway":"支付方式（payway）","payMode":"处方支付方式",
     *                  "decoctionId":"代煎方式", "gfFeeFlag":"1(1：表示需要制作费，0：不需要)", “depId”:"指定药企ID",
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
        IConfigurationCenterUtilsService configurationCenterUtilsService = (IConfigurationCenterUtilsService) AppContextHolder.getBean("eh.configurationCenterUtils");
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
        //date 20200311
        //设置订单的药企关联信息
        RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        DrugsEnterpriseDAO drugsEnterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
        AccessDrugEnterpriseService remoteService = null;
        if (null != order.getEnterpriseId()) {
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
            if (null != drugsEnterprise) {
                remoteService = remoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
                //设置配送费支付方式
                order.setExpressFeePayWay(drugsEnterprise.getExpressFeePayWay());
                //设置期望配送时间块相关信息
//                order.setIsShowExpectSendDate(drugsEnterprise.getIsShowExpectSendDate());
//                order.setExpectSendDateIsContainsWeekend(drugsEnterprise.getExpectSendDateIsContainsWeekend());
//                order.setSendDateText(drugsEnterprise.getSendDateText());
            }
        }
        //货到付款设置配送费为线下支付  并且不是上传运费收费标准方式的时候（这种方式直接显示图片不算运费）
        if (RecipeBussConstant.PAYMODE_COD.equals(payMode) && !new Integer(4).equals(order.getExpressFeePayWay())) {
            //设置配送费支付方式
            order.setExpressFeePayWay(2);
        }

        if (null == remoteService) {
            remoteService = getBean("commonRemoteService", CommonRemoteService.class);
        }
        remoteService.setOrderEnterpriseMsg(extInfo, order);


        order.setRecipeMode(recipeList.get(0).getRecipeMode());
        order.setGiveMode(recipeList.get(0).getGiveMode());

        // 目前paymode传入还是老版本 除线上支付外全都算线下支付,下个版本与前端配合修改
        Integer payModeNew = payMode;
        if(!payMode.equals(1)){
            payModeNew = 2;
        }
        order.setPayMode(payModeNew);
        payModeSupport = setPayModeSupport(order, payMode);
        //校验处方列表是否都能进行配送
        if (RecipeResultBean.SUCCESS.equals(result.getCode())) {
            //date 20200308
            //修改流程 his端预校验返回药企信息，则没有需要排除库存
            List<Recipe> needDelList;
            //获取需要删除的处方对象(可能已处理或者库存不足等情况的处方)
            needDelList = validateRecipeList(result, recipeList, order, payMode, payModeSupport);
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
//                order.setExpressFee(new BigDecimal("-1"));
                order.setTotalFee(BigDecimal.ZERO);
                order.setActualPrice(BigDecimal.ZERO.doubleValue());
                double auditFee = getFee(configurationCenterUtilsService.getConfiguration(firstRecipe.getClinicOrgan(), ParameterConstant.KEY_AUDITFEE));
                //如果是合并处方单，审方费得乘以处方单数
                order.setAuditFee(BigDecimal.valueOf(auditFee).multiply(BigDecimal.valueOf(recipeList.size())));
                double otherServiceFee = getFee(configurationCenterUtilsService.getConfiguration(firstRecipe.getClinicOrgan(), ParameterConstant.KEY_OTHERFEE));
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
        if (StringUtils.isNotEmpty(extInfo.get("hisDepCode"))) {
            order.setHisEnterpriseName(extInfo.get("depName"));
        }

        //设置中药代建费
        Integer decoctionId = MapValueUtil.getInteger(extInfo, "decoctionId");
        if (decoctionId != null) {
            DrugDecoctionWayDao drugDecoctionWayDao = getDAO(DrugDecoctionWayDao.class);
            DecoctionWay decoctionWay = drugDecoctionWayDao.get(decoctionId);
            if (decoctionWay != null && decoctionWay.getDecoctionPrice() != null) {
                order.setDecoctionUnitPrice(BigDecimal.valueOf(decoctionWay.getDecoctionPrice()));
            }
        }
        setCreateOrderResult(result, order, payModeSupport, toDbFlag);
        return result;
    }

    //设置金额
    private double getFee(Object fee) {
        return null != fee ? Double.parseDouble(fee.toString()) : 0d;
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
        } else if (RecipeBussConstant.PAYMODE_TO_HOS.equals(payMode)) {
            payModeSupport.setSupportToHos(true);
            order.setEffective(0);
        } else {
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
    private List<Recipe> validateRecipeList(OrderCreateResult result, List<Recipe> recipeList, RecipeOrder order, Integer payMode, RecipePayModeSupportBean payModeSupport) {
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
                        result.setError("处方单已被撤销");
                    } else {
                        result.setError("处方单已处理");
                    }
                    needDelList.add(recipe);
                    continue;
                }
                Integer depId = recipeService.supportDistributionExt(recipe.getRecipeId(), recipe.getClinicOrgan(), order.getEnterpriseId(), payMode);
                if (null == depId && (payModeSupport.isSupportOnlinePay() || payModeSupport.isSupportCOD() || payModeSupport.isSupportTFDS())) {
                    LOGGER.error("处方id=" + recipe.getRecipeId() + "无法配送。");
                    result.setError("很抱歉，当前库存不足无法结算，请联系客服：" + cacheService.getParam(ParameterConstant.KEY_CUSTOMER_TEL, RecipeSystemConstant.CUSTOMER_TEL));
                    //不能配送需要从处方列表剔除
                    needDelList.add(recipe);
                    continue;
                } else {
                    order.setEnterpriseId(depId);
                }
            }

            //查询是否存在已在其他订单里的处方
            boolean flag = orderDAO.isEffectiveOrder(recipe.getOrderCode());
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
    public void setOrderFee(OrderCreateResult result, RecipeOrder order, List<Integer> recipeIds, List<Recipe> recipeList, RecipePayModeSupportBean payModeSupport, Map<String, String> extInfo, Integer toDbFlag) {
        IOrganConfigService iOrganConfigService = ApplicationUtils.getBaseService(IOrganConfigService.class);
        IConfigurationCenterUtilsService configurationCenterUtilsService = (IConfigurationCenterUtilsService) AppContextHolder.getBean("eh.configurationCenterUtils");
        RecipeDetailDAO recipeDetailDAO = getDAO(RecipeDetailDAO.class);
        OrganConfigBean organConfig = iOrganConfigService.get(order.getOrganId());
        LOGGER.info("进入方法setOrderFee");
        if (null == organConfig) {
            //只有需要真正保存订单时才提示
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("开方机构缺少配置");
            return;
        }
        //当前操作人的编码，用于获取地址列表信息等
        String operMpiId = MapValueUtil.getString(extInfo, "operMpiId");

        //设置挂号费（之前是区分购药方式的，要去区分购药方式来挂号费，现在不区分根据配置项来）
        BigDecimal registerFee = getPriceForRecipeRegister(order.getOrganId());
        if (null != registerFee) {
            order.setRegisterFee(registerFee);
        } else {
            //取base_parameter表数据---默认挂号费10元
            order.setRegisterFee(new BigDecimal(cacheService.getParam(ParameterConstant.KEY_RECIPE_REGISTER_FEE, "0")));
        }

        //设置审方费用
        Recipe firstRecipe = recipeList.get(0);
        //date 20190929
        //审方费判断非不需要审核再去计算
        double auditFee = ReviewTypeConstant.Not_Need_Check == firstRecipe.getReviewType() ? 0d : getFee(configurationCenterUtilsService.getConfiguration(firstRecipe.getClinicOrgan(), ParameterConstant.KEY_AUDITFEE));
        //如果是合并处方单，审方费得乘以处方单数
        order.setAuditFee(BigDecimal.valueOf(auditFee).multiply(BigDecimal.valueOf(recipeList.size())));
        //设置其他服务费用
        double otherServiceFee = getFee(configurationCenterUtilsService.getConfiguration(firstRecipe.getClinicOrgan(), ParameterConstant.KEY_OTHERFEE));
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
        //date 20200311
        //设置订单上的处方价格
        RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        DrugsEnterpriseDAO drugsEnterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
        AccessDrugEnterpriseService remoteService = null;
        if (null != order.getEnterpriseId()) {
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
            if (null != drugsEnterprise) {
                remoteService = remoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
            }
        }
        if (null == remoteService) {
            remoteService = getBean("commonRemoteService", CommonRemoteService.class);
        }
        order.setRecipeFee(remoteService.orderToRecipeFee(order, recipeIds, payModeSupport, recipeFee, extInfo));
        //order.setRecipeFee(recipeFee);

        //膏方代表制作费
        BigDecimal otherFee = BigDecimal.ZERO;
        boolean needCalDecFee = false;
        if (order.getDecoctionUnitPrice() != null) {
            needCalDecFee = (order.getDecoctionUnitPrice().compareTo(BigDecimal.ZERO) == 1) ? true : false;
        }
        //        Integer decoctionFlag = MapValueUtil.getInteger(extInfo, "decoctionFlag");
        //1表示待煎
//        if (Integer.valueOf(1).equals(decoctionFlag)) {
        //待煎单价(代煎费 -1不支持代煎 大于等于0时为代煎费)
//            BigDecimal recipeDecoctionPrice = order.getDecoctionUnitPrice();
//            //根据机构获取代煎费
//            order.setDecoctionUnitPrice(null != recipeDecoctionPrice ? recipeDecoctionPrice : BigDecimal.valueOf(-1));
//            needCalDecFee = (order.getDecoctionUnitPrice().compareTo(BigDecimal.ZERO) == 1) ? true : false;
//        }

        //设置膏方制作费
        Integer gfFeeFlag = MapValueUtil.getInteger(extInfo, "gfFeeFlag");
        //1表示膏方制作费
        if (Integer.valueOf(1).equals(gfFeeFlag)) {
            //制作单价
            BigDecimal gfFeeUnitPrice = organConfig.getRecipeCreamPrice();
            if (null == gfFeeUnitPrice) {
                gfFeeUnitPrice = BigDecimal.ZERO;
            }
//            order.setDecoctionUnitPrice(gfFeeUnitPrice);
            //存在制作单价且大于0
            if (gfFeeUnitPrice.compareTo(BigDecimal.ZERO) == 1) {
                Double totalDose = recipeDetailDAO.getUseTotalDoseByRecipeIds(recipeIds);
                otherFee = gfFeeUnitPrice.multiply(BigDecimal.valueOf(totalDose));
            }
        }

        BigDecimal tcmFee = null;//null表示用户在运营平台没有配置这项费用
        //设置订单代煎费
        Integer totalCopyNum = 0;
        BigDecimal decoctionFee = BigDecimal.ZERO;
        //中药处方数
        int i = 0;
        //是否走平台的中医论证费
        boolean tcmFlag = true;
        for (Recipe recipe : recipeList) {
            if (RecipeBussConstant.RECIPETYPE_TCM.equals(recipe.getRecipeType())) {
                //处理线下转线上的代煎费
                if (new Integer(2).equals(recipe.getRecipeSourceType())) {
                    //表示为线下的处方
                    HisRecipeDAO hisRecipeDAO = DAOFactory.getDAO(HisRecipeDAO.class);
                    HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(recipe.getClinicOrgan(), recipe.getRecipeCode());
                    tcmFlag = false;
                    //设置中医辨证论证费
                    if (hisRecipe != null && hisRecipe.getTcmFee() != null) {
                        tcmFee = hisRecipe.getTcmFee();
                    }
                    if (hisRecipe != null && hisRecipe.getDecoctionFee() != null) {
                        //说明线下处方有代煎费
                        decoctionFee = hisRecipe.getDecoctionFee();
                    } else {
                        //说明线下无代煎费传入,需要判断是否线下传入了贴数
                        if (needCalDecFee && recipe.getCopyNum() != null ) {
                            totalCopyNum = totalCopyNum + recipe.getCopyNum();
                            //代煎费等于剂数乘以代煎单价
                            //如果是合并处方-多张处方下得累加
                            decoctionFee = decoctionFee.add(order.getDecoctionUnitPrice().multiply(BigDecimal.valueOf(recipe.getCopyNum())));
                        }
                    }
                    i++;
                } else {
                    totalCopyNum = totalCopyNum + recipe.getCopyNum();
                    if (needCalDecFee) {
                        //代煎费等于剂数乘以代煎单价
                        //如果是合并处方-多张处方下得累加
                        decoctionFee = decoctionFee.add(order.getDecoctionUnitPrice().multiply(BigDecimal.valueOf(recipe.getCopyNum())));
                    }
                    i++;
                }
            }
        }
        //多个处方，中医辨证论治费收多次!
        //设置中医辨证论治费（中医辨证论治费，所有中药处方都需要增加此收费项目，运营平台增加配置项；若填写了金额，则患者端展示该收费项目；）
        IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        //从opbase配置项获取中医辨证论治费 recipeTCMPrice
        Object findRecipeTCMPrice = configService.getConfiguration(recipeList.get(0).getClinicOrgan(), "recipeTCMPrice");
        if (tcmFlag) {
            //说明走平台的中医论证费计算
            if (findRecipeTCMPrice != null && ((BigDecimal) findRecipeTCMPrice).compareTo(BigDecimal.ZERO) > -1) {
                tcmFee = ((BigDecimal) findRecipeTCMPrice).multiply(new BigDecimal(i));//大于等于0
            }
        }
        LOGGER.info("tcmFee是：{}", tcmFee);
        order.setTcmFee(tcmFee);
        order.setCopyNum(totalCopyNum);
        order.setDecoctionFee(decoctionFee);

        //药店取药不需要地址信息
        if (payModeSupport.isSupportTFDS() || payModeSupport.isSupportDownload() || payModeSupport.isSupportToHos()) {
            order.setAddressCanSend(true);
        } else {
            //设置运费
            //date 2019/12/25
            //调整处方方法调basic
            AddressService addressService = ApplicationUtils.getBasicService(AddressService.class);
            String operAddressId = MapValueUtil.getString(extInfo, "addressId");
            AddressDTO address = null;
            if (StringUtils.isNotEmpty(operAddressId)) {
                address = addressService.get(Integer.parseInt(operAddressId));
            } else {
                LOGGER.info("getDefaultAddressByMpiid mpiid:{}", operMpiId);
                //获取默认收货地址
                address = addressService.getDefaultAddressByMpiid(operMpiId);
                //address = addressService.getLastAddressByMpiId(operMpiId);
                if (address != null) {
                    //判断街道是否完善
                    if (StringUtils.isEmpty(address.getStreetAddress())) {
                        address = null;
                    } else {
                        //判断默认收货地址是否在可配送范围内,若没在配送范围内，则不返回收货地址
                        EnterpriseAddressService enterpriseAddressService = ApplicationUtils.getRecipeService(EnterpriseAddressService.class);
                        int flag = enterpriseAddressService.allAddressCanSendForOrder(order.getEnterpriseId(), address.getAddress1(), address.getAddress2(), address.getAddress3());
                        if (0 != flag) {
                            address = null;
                        }
                    }
                }
            }
            LOGGER.info("setOrderFee mpiid:{} address:{}", operMpiId, address);
            //此字段前端已不使用
            order.setAddressCanSend(false);
            Recipe recipe = recipeList.get(0);
            HisRecipeDAO hisRecipeDAO = DAOFactory.getDAO(HisRecipeDAO.class);
            HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(recipe.getClinicOrgan(), recipe.getRecipeCode());
            if (new Integer(2).equals(recipe.getRecipeSource())) {
                if (StringUtils.isNotEmpty(operAddressId)) {
                    //表示患者重新修改了地址
                    //运费在这里面设置
                    setOrderaAddress(result, order, recipeIds, payModeSupport, extInfo, toDbFlag, drugsEnterpriseDAO, address);
                } else {
                    if (hisRecipe != null && StringUtils.isNotEmpty(hisRecipe.getSendAddr())) {
                        order.setReceiver(hisRecipe.getReceiverName());
                        order.setRecMobile(hisRecipe.getReceiverTel());
                        order.setAddressCanSend(true);
                        order.setAddress4(hisRecipe.getSendAddr());
                    }else {
                        //运费在这里面设置
                        setOrderaAddress(result, order, recipeIds, payModeSupport, extInfo, toDbFlag, drugsEnterpriseDAO, address);
                    }
                }
            } else {
                //运费在这里面设置
                setOrderaAddress(result, order, recipeIds, payModeSupport, extInfo, toDbFlag, drugsEnterpriseDAO, address);
            }
        }

        //}
        order.setTotalFee(countOrderTotalFeeByRecipeInfo(order, firstRecipe, payModeSupport));
        //判断计算扣掉运费的总金额----等于线下支付----总计要先算上运费，实际支付时再不支付运费
        BigDecimal totalFee;
        //配送到家并且线下支付
        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        if (new Integer(2).equals(order.getExpressFeePayWay()) && RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
            if (order.getExpressFee() != null && order.getTotalFee().compareTo(order.getExpressFee()) > -1) {
                totalFee = order.getTotalFee().subtract(order.getExpressFee());
            } else {
                totalFee = order.getTotalFee();
            }
        } else {
            totalFee = order.getTotalFee();
        }
        //计算优惠券价格
        ICouponBaseService couponService = AppContextHolder.getBean("voucher.couponBaseService", ICouponBaseService.class);
        if (isUsefulCoupon(order.getCouponId())) {
            Coupon coupon = couponService.lockCouponById(order.getCouponId(), order.getTotalFee());
            LOGGER.info("RecipeOrderService use coupon , coupon info: {}.", JSONUtils.toString(coupon));
            if (coupon != null) {
                order.setCouponName(coupon.getCouponName());
                order.setCouponFee(coupon.getDiscountAmount());
                order.setCouponDesc(coupon.getCouponDesc());
            }
            if (totalFee.compareTo(order.getCouponFee()) > -1) {
                order.setActualPrice(totalFee.subtract(order.getCouponFee()).doubleValue());
            } else {
                order.setActualPrice(totalFee.doubleValue());
            }
        } else {
            if (payMode != RecipeBussConstant.PAYMODE_ONLINE && !RecipeServiceSub.isJSOrgan(order.getOrganId())) {

                if (RecipeBussConstant.PAYMODE_TO_HOS.equals(payMode)) {
                    PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
                    //卫宁付
                    if (purchaseService.getToHosPayConfig(firstRecipe.getClinicOrgan())) {
                        order.setActualPrice(totalFee.doubleValue());
                    } else {
                        //此时的实际费用是不包含药品费用的
                        order.setActualPrice(order.getAuditFee().doubleValue());
                    }
                } else {
                    if (RecipeBussConstant.PAYMODE_TFDS.equals(payMode)) {
                        //药店取药的
                        Integer depId = order.getEnterpriseId();
                        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
                        if (drugsEnterprise != null && drugsEnterprise.getStorePayFlag() != null && drugsEnterprise.getStorePayFlag() == 1) {
                            //storePayFlag = 1 表示线上支付但到店取药
                            order.setActualPrice(totalFee.doubleValue());
                        } else {
                            //此时的实际费用是不包含药品费用的
                            order.setActualPrice(order.getAuditFee().doubleValue());
                        }
                    } else {
                        //此时的实际费用是不包含药品费用的
                        order.setActualPrice(order.getAuditFee().doubleValue());
                    }
                }
            } else {
                order.setActualPrice(totalFee.doubleValue());
            }
        }
    }

    public Boolean dealWithOrderInfo(Map<String, String> map, RecipeOrder order, Recipe recipe) {
        Map<String, Object> orderInfo = Maps.newHashMap();
        if (StringUtils.isNotEmpty(map.get("preSettleTotalAmount"))) {
            orderInfo.put("preSettleTotalAmount", new Double(map.get("preSettleTotalAmount")));
        }
        if (StringUtils.isNotEmpty(map.get("fundAmount"))) {
            orderInfo.put("fundAmount", new Double(map.get("fundAmount")));
        }
        if (StringUtils.isNotEmpty(map.get("cashAmount"))) {
            orderInfo.put("cashAmount", new Double(map.get("cashAmount")));
        }
        if (StringUtils.isNotEmpty(map.get("hisSettlementNo"))) {
            orderInfo.put("hisSettlementNo", map.get("hisSettlementNo"));
        }
        if (StringUtils.isNotEmpty(map.get("registerNo"))) {
            orderInfo.put("registerNo", map.get("registerNo"));
        }

        if (StringUtils.isNotEmpty(map.get("payAmount"))) {
            //如果预结算返回自付金额不为空优先取这个金额做支付，保证能和his对账上
            orderInfo.put("ActualPrice", new BigDecimal(map.get("payAmount")).doubleValue());
            orderInfo.put("TotalFee", new BigDecimal(map.get("payAmount")).doubleValue());
        } else if (StringUtils.isNotEmpty(map.get("preSettleTotalAmount"))) {
            //如果有预结算返回的金额，则处方实际费用预结算返回的金额代替处方药品金额（his总金额(药品费用+挂号费用)+平台费用(除药品费用以外其他费用的总计)）
            //需要重置下订单费用，有可能患者一直预结算不支付导致金额叠加
            BigDecimal totalFee = countOrderTotalFeeByRecipeInfo(order, recipe, setPayModeSupport(order, PayModeGiveModeUtil.getPayMode(order.getPayMode(),recipe.getGiveMode())));
            if (new Integer(2).equals(order.getExpressFeePayWay()) && RecipeBussConstant.PAYMODE_ONLINE.equals(order.getPayMode())) {
                if (order.getExpressFee() != null && totalFee.compareTo(order.getExpressFee()) > -1) {
                    totalFee = totalFee.subtract(order.getExpressFee());
                }
            }
            BigDecimal priceTemp = totalFee.subtract(order.getRecipeFee());
            orderInfo.put("ActualPrice", new BigDecimal(map.get("preSettleTotalAmount")).add(priceTemp).doubleValue());
            orderInfo.put("TotalFee", new BigDecimal(map.get("preSettleTotalAmount")).add(priceTemp).doubleValue());
        }
        return recipeOrderDAO.updateByOrdeCode(order.getOrderCode(), orderInfo);
    }


    private void setOrderaAddress(OrderCreateResult result, RecipeOrder order, List<Integer> recipeIds, RecipePayModeSupportBean payModeSupport, Map<String, String> extInfo, Integer toDbFlag, DrugsEnterpriseDAO drugsEnterpriseDAO, AddressDTO address) {
        if (null != address) {
            //可以在参数里传递快递费
            String paramExpressFee = MapValueUtil.getString(extInfo, "expressFee");
            //保存地址,费用信息
            BigDecimal expressFee;
            if (StringUtils.isNotEmpty(paramExpressFee)) {
                expressFee = new BigDecimal(paramExpressFee);
            } else {
                //优化快递费用获取，当费用是从第三方获取需要取第三方接口返回的快递费用
                DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
                if (drugsEnterprise != null && new Integer(1).equals(drugsEnterprise.getExpressFeeType())) {
                    //获取地址信息
                    String address1 = address.getAddress1();  //省
                    String address2 = address.getAddress2();  //市
                    String address3 = address.getAddress3();  //区
                    String address4 = address.getAddress4();  //详细地址
                    String phone = address.getRecMobile();
                    Map<String, Object> parames = new HashMap<>();
                    parames.put("province", getAddressDic(address1));
                    parames.put("city", getAddressDic(address2));
                    parames.put("district", getAddressDic(address3));
                    parames.put("depId", order.getEnterpriseId());
                    parames.put("recipeId", recipeIds.get(0));
                    parames.put("provinceCode", address1);
                    parames.put("address", address4);
                    parames.put("phone", phone);
                    RemoteDrugEnterpriseService drugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
                    Map<String, Object> expressFeeResult = drugEnterpriseService.getExpressFee(parames);
                    if ("0".equals(expressFeeResult.get("expressFeeType").toString())) {
                        //需要从平台获取
                        expressFee = getExpressFee(order.getEnterpriseId(), address.getAddress3());
                    } else {
                        expressFee = new BigDecimal(expressFeeResult.get("expressFee").toString());
                    }
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
            order.setStreetAddress(address.getStreetAddress());
            order.setAddress4(address.getAddress4());

            try {
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
                LOGGER.error("setOrderFee--", e);
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

    private BigDecimal getPriceForRecipeRegister(Integer organId) {
        IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        return (BigDecimal) configurationService.getConfiguration(organId, "priceForRecipeRegister");
    }

    public BigDecimal reCalculateRecipeFee(Integer enterpriseId, List<Integer> recipeIds, Map<String, String> extInfo) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
        SaleDrugListDAO saleDrugListDAO = getDAO(SaleDrugListDAO.class);
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
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
                        total = total.add(saleDrug.getPrice().multiply(new BigDecimal(drugIdCountRel.get(saleDrug.getDrugId()))).divide(BigDecimal.ONE, 2, RoundingMode.UP));
                    }
                    //重置药企处方价格
                    recipeFee = total;
                } catch (Exception e) {
                    LOGGER.error("setOrderFee 重新计算药企ID为[{}]的结算价格出错. drugIds={}", enterpriseId, JSONUtils.toString(drugIds), e);
                }
            }
        } else if (null != enterprise && Integer.valueOf(1).equals(enterprise.getSettlementMode())) {
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
                if (null != resTO) {
                    order = resTO.getData();
                } else {
                    LOGGER.info("reCalculateRecipeFee createBlankOrder order is null.");
                    return null;
                }
            }
            return order.getTotalFee();
        }
    }

    public void setCreateOrderResult(OrderCreateResult result, RecipeOrder order, RecipePayModeSupportBean payModeSupport, Integer toDbFlag) {
        if (payModeSupport.isSupportMedicalInsureance()) {
            result.setCouponType(null);
        } else if (payModeSupport.isSupportCOD()) {
            result.setCouponType(null);
        } else if (payModeSupport.isSupportTFDS()) {
            result.setCouponType(null);
        } else if (payModeSupport.isSupportComplex()) {
            result.setCouponType(null);
        } else {
            result.setCouponType(5);
        }

        setAppOtherMessage(order);
        result.setObject(ObjectCopyUtils.convert(order, RecipeOrderBean.class));
        if (RecipeResultBean.SUCCESS.equals(result.getCode()) && 1 == toDbFlag && null != order.getOrderId()) {
            result.setOrderCode(order.getOrderCode());
            result.setBusId(order.getOrderId());
        }

        LOGGER.info("createOrder finish. result={}", JSONUtils.toString(result));
    }


    /**
     * @param order
     * @return void
     * @method setAppOtherMessage
     * @description 添加设置app端的额外信息
     * @date: 2019/11/12
     * @author: JRK
     */
    private void setAppOtherMessage(RecipeOrder order) {

        //date 20200311
        //更改订单展示药企信息
        if (null != order && order.getEnterpriseId() != null) {
            RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            DrugsEnterpriseDAO drugsEnterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
            if (drugsEnterprise != null) {
                AccessDrugEnterpriseService remoteService = remoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
                //药品匹配成功标识
                order.setEnterpriseName(remoteService.appEnterprise(order));
            }
        }

        //设置送货地址
        if (null != order && (null != order.getAddress1() && null != order.getAddress2() && null != order.getAddress3())) {
            CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
            order.setCompleteAddress(commonRemoteService.getCompleteAddress(order));
        } else {
            //对北京互联网处方流转模式处理
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            List<Integer> recipeIdList = JSONUtils.parse(order.getRecipeIdList(), List.class);
            Recipe recipe = null;
            if (CollectionUtils.isNotEmpty(recipeIdList)) {
                recipe = recipeDAO.getByRecipeId(recipeIdList.get(0));
            }

            if (recipe != null && new Integer(2).equals(recipe.getRecipeSource())) {
                HisRecipeDAO hisRecipeDAO = DAOFactory.getDAO(HisRecipeDAO.class);
                HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(recipe.getClinicOrgan(), recipe.getRecipeCode());
                if (hisRecipe != null && StringUtils.isNotEmpty(hisRecipe.getSendAddr())) {
                    order.setCompleteAddress(hisRecipe.getSendAddr());
                }
            } else {
                LOGGER.info("当前订单的配送地址信息不全！");
            }
        }
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
                        ysqUrl = ysqUrl + "PreTitle/Details?appid=" + appid + "&inbillno=" + firstRecipe.getClinicOrgan() + YsqRemoteService.YSQ_SPLIT + firstRecipe.getRecipeCode() + "&gysCode=" + MapValueUtil.getString(extInfo, "gysCode") + "&sendMethod=" + MapValueUtil.getString(extInfo, "sendMethod") + "&payMethod=" + MapValueUtil.getString(extInfo, "payMethod");
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
    public boolean saveOrderToDB(RecipeOrder order, List<Recipe> recipeList, Integer payMode, OrderCreateResult result, RecipeDAO recipeDAO, RecipeOrderDAO orderDAO) {
        LOGGER.info("recipeOrder saveOrderToDB recipeList={}", JSON.toJSONString(recipeList));
        List<Integer> recipeIds = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        //订单类型设置默认值
        if (null == order.getOrderType()) {
            order.setOrderType(0);
        }
        try {
            createOrderToDB(order, recipeIds, orderDAO, recipeDAO);
        } catch (DAOException e) {
            //如果小概率造成orderCode重复，则修改并重试
            LOGGER.warn("createOrder orderCode={}", order.getOrderCode(), e);
            try {
                order.setOrderCode(getOrderCode(order.getMpiId()));
                createOrderToDB(order, recipeIds, orderDAO, recipeDAO);
            } catch (DAOException e1) {
                LOGGER.error("createOrder again orderCode={}", order.getOrderCode(), e1);
                result.setCode(RecipeResultBean.FAIL);
                result.setMsg("保存订单系统错误");
                return false;
            }
        }

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
        updateRecipeInfo(false, result, recipeIds, recipeInfo, null);
        return true;
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
            result = cancelOrder(getDAO(RecipeOrderDAO.class).getByOrderCode(orderCode), status, true);
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
            result = cancelOrder(getDAO(RecipeOrderDAO.class).getOrderByRecipeId(recipeId), status, true);
        }

        return result;
    }

    /**
     * 审方后置根据处方单号取消订单
     *
     * @param recipeId
     * @param status
     * @return
     */
    public RecipeResultBean cancelOrderByRecipeId(Integer recipeId, Integer status, Boolean canCancelOrderCode) {
        RecipeResultBean result = RecipeResultBean.getSuccess();
        if (null == recipeId || null == status) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("缺少参数");
        }

        if (RecipeResultBean.SUCCESS.equals(result.getCode())) {
            result = cancelOrder(getDAO(RecipeOrderDAO.class).getOrderByRecipeId(recipeId), status, canCancelOrderCode);
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
            result = cancelOrder(getDAO(RecipeOrderDAO.class).get(orderId), status, true);
        }

        return result;
    }

    /**
     * 取消订单
     *
     * @param order canCancelOrderCode 能否将处方里的OrderCode设置成null
     * @return
     */
    public RecipeResultBean cancelOrder(RecipeOrder order, Integer status, Boolean canCancelOrderCode) {
        LOGGER.info("RecipeOrderService cancelOrder  order= {}，order= {}，canCancelOrderCode= {}", JSON.toJSONString(order), status, canCancelOrderCode);
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
                        LOGGER.error("cancelOrder unlock coupon error. couponId={}, error={}", order.getCouponId(), e.getMessage(), e);
                    }
                }
                this.updateOrderInfo(order.getOrderCode(), orderAttrMap, result);

                //有可能自动取消的走到这里
                //如果有正在进行中的合并处方单应该还原
                RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
                List<Integer> recipeIdList = JSONUtils.parse(order.getRecipeIdList(), List.class);
                //合并处方订单取消
                List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIdList);
                if (status.equals(OrderStatusConstant.CANCEL_MANUAL)) {
                    //订单手动取消，处方单可以进行重新支付
                    //更新处方的orderCode

                    for (Recipe recipe : recipes) {
                        if (recipe != null) {
                            recipeDAO.updateOrderCodeToNullByOrderCodeAndClearChoose(order.getOrderCode(), recipe);
                        }
                        try {
                            //对于来源于HIS的处方单更新hisRecipe的状态
                            if (recipe != null) {
                                HisRecipeDAO hisRecipeDAO = getDAO(HisRecipeDAO.class);
                                HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(recipe.getClinicOrgan(), recipe.getRecipeCode());
                                if (hisRecipe != null) {
                                    hisRecipeDAO.updateHisRecieStatus(recipe.getClinicOrgan(), recipe.getRecipeCode(), 1);
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.info("RecipeOrderService.cancelOrder 来源于HIS的处方单更新hisRecipe的状态失败,error:{}.", e.getMessage(), e);
                        }
                    }
                } else {
                    //有可能退款的数据走到这里来他的canCancelOrderCode为false
                    if (canCancelOrderCode) {
                        for (Recipe recipe : recipes) {
                            //未支付定时取消的处方单orderCode不设置成空
                            if (RecipeStatusConstant.NO_PAY != recipe.getStatus()) {
                                recipeDAO.updateOrderCodeToNullByOrderCodeAndClearChoose(order.getOrderCode(), recipe);
                            }
                        }
                        //date 20200330
                        //调用支付平台取消支付接口
                        RecipeOrder orderNow;
                        INgariPayService payService = AppDomainContext.getBean("eh.payService", INgariPayService.class);
                        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
                        if (null != recipeIdList.get(0)) {
                            orderNow = orderDAO.getByOrderCode(order.getOrderCode());
                            //判断订单是否是单边账的
                            if (0 == orderNow.getPayFlag() && StringUtils.isNotEmpty(orderNow.getOutTradeNo()) && StringUtils.isNotEmpty(orderNow.getWxPayWay()) && StringUtils.isNotEmpty(orderNow.getPayOrganId())) {
                                Map<String, Object> backResult = payService.payCancel(BusTypeEnum.RECIPE.getCode(), orderNow.getOrderId().toString());
                                if (null != backResult && eh.wxpay.constant.PayConstant.RESULT_SUCCESS.equals(backResult.get("code"))) {
                                    for (Recipe recipe : recipes) {
                                        LOGGER.info("RecipeOrderService.cancelOrder 取消的订单对应的处方{}成功.", recipe.getRecipeId());
                                        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "当前处方" + recipe.getRecipeId() + "取消的订单对应的接口成功");
                                    }
                                }
                            }
                        } else {
                            LOGGER.info("RecipeOrderService.cancelOrder 取消的订单处方id为空.");
                        }
                    }
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
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
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

            //如果订单是到院取药，获取His的处方单支付状态，并更新
            //订单有效
            if (CollectionUtils.isNotEmpty(recipeList) && order.getEffective() == 1) {
                for (Recipe recipeItem : recipeList) {
                    //到院取药  && recipeItem.getStatus() == 2
                    if (recipeItem.getGiveMode() == 2 && recipeItem.getPayFlag() == 1) {
                        Integer query = recipeHisService.getRecipeSinglePayStatusQuery(recipeItem.getRecipeId());
                        if (query != null && query == eh.cdr.constant.RecipeStatusConstant.HAVE_PAY) {
                            recipeItem.setStatus(eh.cdr.constant.RecipeStatusConstant.HAVE_PAY);
                        }
                    }
                }
            }
            //更新处方recipe的status

            Map<Integer, String> enterpriseAccountMap = Maps.newHashMap();
            if (CollectionUtils.isNotEmpty(recipeList)) {
                //设置地址，先取处方单address4的值，没有则取订单地址
                if (StringUtils.isNotEmpty(recipeList.get(0).getAddress4())) {
                    order.setCompleteAddress(recipeList.get(0).getAddress4());
                } else {
                    order.setCompleteAddress(commonRemoteService.getCompleteAddress(order));
                }

                RecipeDetailDAO detailDAO = getDAO(RecipeDetailDAO.class);
                RecipeExtendDAO recipeExtendDAO = getDAO(RecipeExtendDAO.class);

                PatientRecipeDTO prb;
                List<Recipedetail> recipedetails;
                for (Recipe recipe : recipeList) {
                    RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                    EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
                    prb = new PatientRecipeDTO();
                    prb.setRecipeId(recipe.getRecipeId());
                    prb.setOrganDiseaseName(recipe.getOrganDiseaseName());
                    prb.setMpiId(recipe.getMpiid());
                    prb.setSignDate(recipe.getSignDate());
                    prb.setPatientName(patientService.getNameByMpiId(recipe.getMpiid()));
                    prb.setStatusCode(recipe.getStatus());

                    Integer payModeNew = PayModeGiveModeUtil.getPayMode(order.getPayMode(), recipe.getGiveMode());
                    prb.setPayMode(payModeNew);
                    prb.setRecipeType(recipe.getRecipeType());
                    prb.setRecipeMode(recipe.getRecipeMode());
                    prb.setChemistSignFile(recipe.getChemistSignFile());
                    prb.setSignFile(recipe.getSignFile());
                    prb.setDoctorName(recipe.getDoctorName());
                    prb.setRecipeCode(recipe.getRecipeCode());
                    RecipeBean recipeBean = ObjectCopyUtils.convert(recipe, RecipeBean.class);
                    if (StringUtils.isNotEmpty(order.getGiveModeText())) {
                        recipeBean.setGiveModeText(order.getGiveModeText());
                    } else {
                        recipeBean.setGiveModeText(GiveModeFactory.getGiveModeBaseByRecipe(recipe).getGiveModeTextByRecipe(recipe));
                    }
                    prb.setRecipe(recipeBean);
                    prb.setPatient(patientService.getByMpiId(recipe.getMpiid()));
                    try {
                        prb.setDepartName(DictionaryController.instance().get("eh.base.dictionary.Depart").getText(recipe.getDepart()));
                    } catch (ControllerException e) {
                        LOGGER.warn("getOrderDetailById 字典转化异常");
                    }
                    //药品详情
                    recipedetails = detailDAO.findByRecipeId(recipe.getRecipeId());
                    String className = Thread.currentThread().getStackTrace()[2].getClassName();
                    String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
                    //药品显示名处理
                    for (Recipedetail recipedetail : recipedetails) {
                        //药品名历史数据处理
                        if (StringUtils.isEmpty(recipedetail.getDrugDisplaySplicedName())) {
                            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndOrganDrugCodeAndDrugIdWithoutStatus(recipe.getClinicOrgan(), recipedetail.getOrganDrugCode(), recipedetail.getDrugId());
                            recipedetail.setDrugDisplaySplicedName(DrugNameDisplayUtil.dealwithRecipedetailName(organDrugLists, recipedetail, recipe.getRecipeType()));
                        }
                    }
                    //获取处方详情
                    prb.setRecipeDetail(ObjectCopyUtils.convert(recipedetails, RecipeDetailBean.class));
                    boolean isReturnRecipeDetail = recipeListService.isReturnRecipeDetail(recipe.getRecipeId());
                    if (("getOrderDetail".equals(methodName) && "recipe.service.RecipeOrderService".equals(className))) {
                        if (!isReturnRecipeDetail) {
                            List<RecipeDetailBean> recipeDetailVOs = prb.getRecipeDetail();
                            if (recipeDetailVOs != null && recipeDetailVOs.size() > 0) {
                                for (int j = 0; j < recipeDetailVOs.size(); j++) {
                                    recipeDetailVOs.get(j).setDrugName(null);
                                    recipeDetailVOs.get(j).setDrugSpec(null);
                                }
                            }
                        }
                    }
                    //返回是否隐方
                    prb.setIsHiddenRecipeDetail(!isReturnRecipeDetail);
                    //返回处方拓展信息
                    RecipeExtendBean recipeExtendBean = ObjectCopyUtils.convert(recipeExtend, RecipeExtendBean.class);
                    prb.setRecipeExtend(recipeExtendBean);
                    if (RecipeStatusConstant.CHECK_PASS == recipe.getStatus() && OrderStatusConstant.READY_PAY.equals(order.getStatus())) {
                        prb.setRecipeSurplusHours(RecipeServiceSub.getRecipeSurplusHours(recipe.getSignDate()));
                    }

                    //添加处方的取药窗口
                    OrganService organService = BasicAPI.getService(OrganService.class);
                    OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());
                    //取处方详情中的药品的取药窗口信息
                    // 更改为从处方扩展表中获取取药窗口信息
                    if (!Objects.isNull(recipeExtend) && StringUtils.isNotEmpty( recipeExtend.getPharmNo())) {
                        prb.setGetDrugWindow(organDTO.getName() + recipeExtend.getPharmNo() + "取药窗口");
                    }
                    prb.setOrganId(recipe.getClinicOrgan());
                    prb.setRecipeType(recipe.getRecipeType());
                    prb.setPayFlag(recipe.getPayFlag());
                    //根据运营平台配置的选项获取生成二维码的字段
                    try {
                        IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
                        Integer qrTypeForRecipe = (Integer) configurationService.getConfiguration(recipe.getClinicOrgan(), "getQrTypeForRecipe");

                        switch (qrTypeForRecipe) {
                            case 1:
                                //无
                                break;
                            case 2:
                                //就诊卡号
                                if (StringUtils.isNotEmpty(recipeExtend.getCardNo())) {
                                    prb.setQrName(recipeExtend.getCardNo());
                                }
                                break;
                            case 3:
                                if (StringUtils.isNotEmpty(recipeExtend.getRegisterID())) {
                                    prb.setQrName(recipeExtend.getRegisterID());
                                }
                                break;
                            case 4:
                                if (StringUtils.isNotEmpty(recipe.getPatientID())) {
                                    prb.setQrName(recipe.getPatientID());
                                }
                                break;
                            case 5:
                                if (StringUtils.isNotEmpty(recipe.getRecipeCode())) {
                                    prb.setQrName(recipe.getRecipeCode());
                                }
                                break;
                            default:
                                break;
                        }
                    } catch (Exception e) {
                        LOGGER.error("获取运营平台处方支付配置异常", e);
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

            RecipeOrderBean orderBean = ObjectCopyUtils.convert(order, RecipeOrderBean.class);

            BigDecimal needFee = new BigDecimal(0.00);
            //当处方状态为已完成时
            if (RecipeStatusConstant.FINISH == recipeList.get(0).getStatus()) {
                //实付款 (当处方状态为已完成时，实付款=总金额-优惠金额 同时将需付款设置为0）特殊处理：线下支付，不会将金额回写到处方，只会回写状态
                orderBean.setActualPrice(orderBean.getTotalFee().subtract(orderBean.getCouponFee()).doubleValue());
            } else {
                // 需支付
                // 当payflag=0 未支付时 需支付=订单总金额-优惠金额
                // 当payflag=1已支付，2退款中，3退款成功，4支付失败时 需支付=订单总金额-实付款-优惠金额
                try {
                    LOGGER.info("getOrderDetailById needFee orderCode:{} ,order:{}", order.getOrderCode(), JSONUtils.toString(order));
                    if (PayConstant.PAY_FLAG_NOT_PAY == orderBean.getPayFlag()) {
                        needFee = orderBean.getTotalFee().subtract(orderBean.getCouponFee());
                    } else {
                        needFee = orderBean.getTotalFee().subtract(orderBean.getCouponFee()).subtract(new BigDecimal(Double.toString(orderBean.getActualPrice())));
                    }
                } catch (Exception e) {
                    LOGGER.error("getOrderDetailById needFee计算需支付 error :{}", e);
                }
            }
            orderBean.setNeedFee(needFee.compareTo(BigDecimal.ZERO) >= 0 ? needFee : BigDecimal.ZERO);

            if (order.getEnterpriseId() != null) {
                DrugsEnterpriseDAO drugsEnterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
                DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
                if (drugsEnterprise != null) {
                    orderBean.setEnterpriseName(drugsEnterprise.getName());
                    orderBean.setTransFeeDetail(drugsEnterprise.getTransFeeDetail());
                    orderBean.setTel(drugsEnterprise.getTel());
                    // 药企物流对接方式
                    orderBean.setLogisticsType(drugsEnterprise.getLogisticsType());
                    orderBean.setSendType(drugsEnterprise.getSendType());
                }

                //如果扩展表指定了配送商名称，那就用扩展表的为主替换掉药企表的（杭州互联网新加逻辑）
                RecipeExtendDAO RecipeExtendDAO = getDAO(RecipeExtendDAO.class);
                RecipeExtend recipeExtend = RecipeExtendDAO.getByRecipeId(recipeList.get(0).getRecipeId());
                if (recipeExtend != null && recipeExtend.getDeliveryName() != null && StringUtils.isEmpty(order.getHisEnterpriseName())) {
                    orderBean.setEnterpriseName(recipeExtend.getDeliveryName());
                }
                //date 20200312
                //订单详情展示his推送信息
                //date  20200320
                //添加判断配送药企his信息只有配送方式才有
                if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipeList.get(0).getGiveMode()) && StringUtils.isNotEmpty(order.getHisEnterpriseName())) {

                    orderBean.setEnterpriseName(order.getHisEnterpriseName());
                }
            }
            orderBean.setList(patientRecipeBeanList);
            result.setObject(orderBean);
            // 支付完成后跳转到订单详情页需要加挂号费服务费可配置
            result.setExt(RecipeUtil.getParamFromOgainConfig(order, recipeList));
            //在扩展内容中设置下载处方签的判断
            getDownConfig(result, order, recipeList);
            //在扩展内容中添加展示审核金额
            getShowAuditFeeAndTips(result, order, recipeList);
            /*//在扩展内容中添加医保结算金额明细数据----已经要求卫宁互联网在支付回调memo字段里拼接好格式返回了所以此处不要了
            getShowMedicalRespData(result,recipeList);*/
        } else {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("不存在ID为" + orderId + "的订单");
        }

        return result;
    }

    private void getShowMedicalRespData(RecipeResultBean result, List<Recipe> recipeList) {
        try {
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            Map<String, Object> ext = result.getExt();
            if (null == ext) {
                ext = Maps.newHashMap();
            }
            if (CollectionUtils.isNotEmpty(recipeList)) {
                Recipe nowRecipe = recipeList.get(0);
                if (null != nowRecipe) {
                    RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(nowRecipe.getRecipeId());
                    if (recipeExtend != null && StringUtils.isNotEmpty(recipeExtend.getMedicalSettleData())) {
                        MedicalRespData data = new MedicalRespData();
                        List<String> list = Splitter.on("|").splitToList(recipeExtend.getMedicalSettleData());
                        if (list.size() >= 42) {
                            //本年账户支付 9
                            data.setCurrentAccountPayment(transBigDecimal(list.get(8)));
                            //历年账户支付 10
                            data.setAccountPaymentHistory(transBigDecimal(list.get(9)));
                            //本年账户余额 18
                            data.setCurrentAccountBalance(transBigDecimal(list.get(17)));
                            //历年账户余额 19
                            data.setAccountBalanceHistory(transBigDecimal(list.get(18)));
                            //基金支付     12
                            data.setFundPayment(transBigDecimal(list.get(11)));
                            //起付标准累计 24
                            data.setAccumulativeMinimumPaymentStandard(transBigDecimal(list.get(23)));
                            //自费         16
                            data.setSelfPayment(transBigDecimal(list.get(15)));
                            //其中历年账户（自费部分）40
                            data.setAnnualAccountsBySelfPayment(transBigDecimal(list.get(39)));
                            //自理         15
                            data.setSelfCare(transBigDecimal(list.get(14)));
                            //其中历年账户（自理部分）39
                            data.setAnnualAccountsBySelfCare(transBigDecimal(list.get(38)));
                            //自负         11+14+31+42
                            data.setSelfFinancing(transBigDecimal(list.get(10)).add(transBigDecimal(list.get(13))).add(transBigDecimal(list.get(30))).add(transBigDecimal(list.get(41))));
                            //其中历年账户（自负部分）  11
                            data.setAnnualAccountsBySelfFinancing(transBigDecimal(list.get(10)));
                            //合计   8
                            data.setTotal(transBigDecimal(list.get(7)));
                        }
                        ext.put("medicalRespData", data);
                        LOGGER.info("getShowMedicalRespData data={}", JSONUtils.toString(data));
                        result.setExt(ext);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("getShowMedicalRespData error", e);
        }
    }

    private BigDecimal transBigDecimal(String s) {
        if (StringUtils.isEmpty(s)) {
            s = "0.00";
        }
        return new BigDecimal(s);
    }

    /**
     * @param result     返回结果集
     * @param order      返回的订单
     * @param recipeList 订单关联的处方列表
     * @return void
     * @method getShowAuditFee
     * @description 展示审核金额按钮的判断
     * @date: 2019/9/20
     * @author: JRK
     */
    public void getShowAuditFeeAndTips(RecipeResultBean result, RecipeOrder order, List<Recipe> recipeList) {
        IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        Map<String, Object> ext = result.getExt();
        if (null == ext) {
            ext = Maps.newHashMap();
        }
        Boolean showAuditFee = false;
        if (CollectionUtils.isNotEmpty(recipeList)) {
            Recipe nowRecipe = recipeList.get(0);
            if (null != nowRecipe) {
                //判断时候需要展示审方费用：
                //当不是不需要审核
                showAuditFee = ReviewTypeConstant.Not_Need_Check != nowRecipe.getReviewType() && (null != configurationService.getConfiguration(nowRecipe.getClinicOrgan(), "auditFee") || 0 > BigDecimal.ZERO.compareTo(order.getAuditFee()));
                //添加文案提示的
                getOrderTips(ext, nowRecipe, order);
                //设置页面上提示文案的颜色信息
                //添加一次审核不通过的判断，等价于待审核
                Integer recipestatus = nowRecipe.getStatus();
                if (RecipecCheckStatusConstant.First_Check_No_Pass == nowRecipe.getCheckStatus()) {
                    recipestatus = RecipeStatusConstant.READY_CHECK_YS;
                }
                RecipeTipesColorTypeEnum colorType = RecipeTipesColorTypeEnum.fromRecipeStatus(recipestatus);
                if (null != colorType) {
                    ext.put("tipsType", colorType.getShowType());
                }
            }
        }

        ext.put("showAuditFee", showAuditFee ? "1" : "0");
        result.setExt(ext);
    }

    /**
     * @param ext
     * @param nowRecipe
     * @return void
     * @method getOrderTips
     * @description 获取订单的提示
     * @date: 2019/9/29
     * @author: JRK
     */
    private void getOrderTips(Map<String, Object> ext, Recipe nowRecipe, RecipeOrder order) {
        if (nowRecipe.getRecipeMode() == RecipeBussConstant.RECIPEMODE_ZJJGPT) {
            ext.put("tips", RecipeServiceSub.getTipsByStatusForPatient(nowRecipe, order));
        } else {
            PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
            ext.put("tips", purchaseService.getTipsByStatusForPatient(nowRecipe, order));
        }
    }

    /**
     * @param result     返回结果集
     * @param order      返回的订单
     * @param recipeList 订单关联的处方列表
     * @return void
     * @method getDownConfig
     * @description 下载处方签展示按钮的判断
     * @date: 2019/9/20
     * @author: JRK
     */
    public void getDownConfig(RecipeResultBean result, RecipeOrder order, List<Recipe> recipeList) {
        //判断是否展示下载处方签按钮：1.在下载处方购药方式
        //2.是否是后置，后置：判断审核是否审核通过状态
        //3.不是后置:判断实际金额是否为0：为0则ordercode关联则展示，不为0支付则展示
        Map<String, Object> ext = result.getExt();
        if (null == ext) {
            ext = Maps.newHashMap();
        }
        String isDownload = "0";
        if (CollectionUtils.isNotEmpty(recipeList)) {
            Recipe nowRecipe = recipeList.get(0);
            if (RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(nowRecipe.getGiveMode())) {
                if (ReviewTypeConstant.Postposition_Check == nowRecipe.getReviewType()) {
                    if (Arrays.asList(showDownloadRecipeStatus).contains(nowRecipe.getStatus())) {
                        isDownload = "1";
                    }
                } else if (ReviewTypeConstant.Not_Need_Check == nowRecipe.getReviewType() && RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(nowRecipe.getGiveMode()) && RecipeStatusConstant.FINISH != nowRecipe.getStatus()) {
                    //这里当是不需审核，且选择的下载处方的购药方式的时候，没有产生订单，且不是完成状态，直接判断没有选定购药方式
                    if (1 == nowRecipe.getChooseFlag()) {
                        isDownload = "1";
                    }
                } else {
                    if (null != nowRecipe.getOrderCode() && null != order && RecipeStatusConstant.FINISH != nowRecipe.getStatus()) {
                        if (0 == order.getActualPrice() || (0 < order.getActualPrice() && 1 == nowRecipe.getPayFlag())) {
                            isDownload = "1";
                        }
                    }
                }
            }
        }

        ext.put("isDownload", isDownload);
        result.setExt(ext);
    }


    /**
     * 健康端获取订单详情
     *
     * @param orderCode
     * @return
     */
    @RpcService
    public RecipeResultBean getOrderDetail(String orderCode) {
        if (StringUtils.isEmpty(orderCode)) {
            throw new DAOException(eh.base.constant.ErrorCode.SERVICE_ERROR, "该处方单信息已变更，请退出重新获取处方信息。");
        }
        checkGetOrderDetail(orderCode);
        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
        RecipeOrder order = orderDAO.getByOrderCode(orderCode);
        if (order != null) {
            checkUserHasPermission((Integer) JSONUtils.parse(order.getRecipeIdList(), List.class).get(0));
            return this.getOrderDetailById(order.getOrderId());
        } else {
            throw new DAOException(eh.base.constant.ErrorCode.SERVICE_ERROR, "该处方单信息已变更，请退出重新获取处方信息。");
        }
    }

    /**
     * 校验待支付订单数据是否发生变化
     * @param orderCode
     */
    private void checkGetOrderDetail(String orderCode) {
        try{
            LOGGER.info("checkGetOrderDetail orderCode:{}", orderCode);
            //线下处方目前一个订单只会对应一个处方
            List<Recipe> recipes=recipeDAO.findRecipeByOrdercode(orderCode);
            Recipe recipe=recipes.get(0);
            if(recipe==null ||recipe.getRecipeSourceType()!=2){
                return;
            }
            String cardId=patientService.getPatientBeanByMpiId(recipe.getMpiid()).getCardId();
            PatientService patientService = BasicAPI.getService(PatientService.class);
            PatientDTO patientDTO = patientService.getPatientBeanByMpiId(recipe.getMpiid());
            if (StringUtils.isNotEmpty(cardId)) {
                patientDTO.setCardId(cardId);
            } else {
                patientDTO.setCardId("");
            }
            if (null == patientDTO) {
                throw new DAOException(609, "患者信息不存在");
            }
            HisResponseTO<List<QueryHisRecipResTO>> responseTO = hisRecipeService.queryData(recipe.getClinicOrgan(),patientDTO,6,1);
            List<QueryHisRecipResTO> hisRecipeTO=responseTO.getData();
            if(CollectionUtils.isEmpty(hisRecipeTO)){
                LOGGER.info("checkGetOrderDetail hisRecipeTO==null orderCode:{}", orderCode);
                throw new DAOException(700, "该处方单信息已变更，请退出重新获取处方信息。");
            }
            Set<String> deleteSetRecipeCode = new HashSet<>();
            AtomicReference<Boolean> existThisRecipeCode= new AtomicReference<>(false);
            hisRecipeTO.forEach(a -> {
                if(StringUtils.isNotEmpty(a.getRecipeCode()) &&a.getRecipeCode().equals(recipe.getRecipeCode())){
                    existThisRecipeCode.set(true);
                    HisRecipe hisRecipe=hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(a.getClinicOrgan(),a.getRecipeCode());
                    if(hisRecipe==null){
                        LOGGER.info("checkGetOrderDetail hisRecipe==null orderCode:{}", orderCode);
                        throw new DAOException(700, "该处方单信息已变更，请退出重新获取处方信息。");
                    }
                    if(hisRecipe.getStatus()!=2){
                        //中药判断tcmFee发生变化,删除数据
                        BigDecimal tcmFee =  a.getTcmFee() ;
                        if((tcmFee != null && tcmFee.compareTo(hisRecipe.getTcmFee())!= 0) || (tcmFee == null && hisRecipe.getTcmFee() != null)){
                            LOGGER.info("checkGetOrderDetail tcmFee no equal, deleteSetRecipeCode add orderCode:{},tcmFee:{},hisRecipe.getTcmFee();{}", orderCode,tcmFee,hisRecipe.getTcmFee());
                            deleteSetRecipeCode.add(hisRecipe.getRecipeCode());
                        }
                    }
                }
            });
            //删除
            hisRecipeService.deleteSetRecipeCode(recipe.getClinicOrgan(), deleteSetRecipeCode);
            if (existThisRecipeCode.get()==false ||
                    (deleteSetRecipeCode == null&&deleteSetRecipeCode.size()>0)) {
                LOGGER.info("checkGetOrderDetail 处方已经被删除或处方发生变化 orderCode:{}", orderCode);
                throw new DAOException(700, "该处方单信息已变更，请退出重新获取处方信息。");
            }
        }catch (Exception e){
            e.printStackTrace();
            LOGGER.info("checkGetOrderDetail orderCode:{},error:{}", orderCode, e.getMessage());
        }

    }

    /**
     * 获取订单详情
     * 新接口findByLocationAndSource
     *
     * @param giveMode
     * @return
     */
    @RpcService
    @Deprecated
    public Map<Integer, String> getOrderStatusEnum(Integer giveMode) {
        HashMap<Integer, String> map = new HashMap<>();
        if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode)) {
            map.put(OrderStatusConstant.READY_SEND, "待配送");
            map.put(OrderStatusConstant.SENDING, "配送中");
        } else {
            map.put(OrderStatusConstant.HAS_DRUG, "待取药");
            map.put(OrderStatusConstant.FAIL, "取药失败");
        }
        map.put(OrderStatusConstant.FINISH, "已完成");

        return map;
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
            return null;
        }
        DrugDistributionPriceService priceService = ApplicationUtils.getRecipeService(DrugDistributionPriceService.class);
        DrugDistributionPriceBean expressFee = priceService.getDistributionPriceByEnterpriseIdAndAddrArea(enterpriseId, address);
        if (null != expressFee) {
            return expressFee.getDistributionPrice();
        }

        return null;
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
    public RecipeResultBean finishOrderPayWithoutPay(String orderCode, Integer payMode) {
        return finishOrderPayImpl(orderCode, PayConstant.PAY_FLAG_NOT_PAY, payMode);
    }

    public RecipeResultBean finishOrderPayImpl(String orderCode, int payFlag, Integer payMode) {
        LOGGER.info("finishOrderPayImpl is get! orderCode={} ,payFlag = {}", orderCode, payFlag);
        RecipeResultBean result = RecipeResultBean.getSuccess();
        RecipeOrder order = recipeOrderDAO.getByOrderCode(orderCode);
        Map<String, Object> attrMap = Maps.newHashMap();
        attrMap.put("payFlag", payFlag);
        //date 20190919
        //根据不同的购药方式设置订单的状态
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
        List<Recipe> recipes = recipeDAO.findRecipeListByOrderCode(orderCode);
        if (CollectionUtils.isNotEmpty(recipes)) {
            Recipe nowRecipe = recipes.get(0);
            Integer reviewType = nowRecipe.getReviewType();
            Integer giveMode = nowRecipe.getGiveMode();
            //首先判断是否支付成功调用，还是支付前调用
            if (PayConstant.PAY_FLAG_PAY_SUCCESS == payFlag) {
                //支付成功后
                attrMap.put("payTime", Calendar.getInstance().getTime());
                attrMap.put("status", getPayStatus(reviewType, giveMode, nowRecipe));
                attrMap.put("effective", 1);
                //退款标记
                attrMap.put("refundFlag", 0);
                //date 20191017
                //添加使用优惠券(支付后释放)
                useCoupon(nowRecipe, payMode);
                sendTfdsMsg(nowRecipe, payMode, orderCode);
                //支付成功后，对来源于HIS的处方单状态更新为已处理
                updateHisRecieStatus(recipes);
                purchaseService.setRecipePayWay(order);
            } else if (PayConstant.PAY_FLAG_NOT_PAY == payFlag && null != order) {
                attrMap.put("status", getPayStatus(reviewType, giveMode, nowRecipe));
                //支付前调用
                //todo--特殊处理---江苏省健康APP----到院取药线上支付药品费用---后续优化
                if (0 == order.getActualPrice() && !RecipeServiceSub.isJSOrgan(nowRecipe.getClinicOrgan())) {
                    //date 20191017
                    //添加使用优惠券（不需支付，释放）
                    useCoupon(nowRecipe, payMode);
                    sendTfdsMsg(nowRecipe, payMode, orderCode);
                } else {
                    attrMap.put("status", OrderStatusConstant.READY_PAY);
                }
                attrMap.put("effective", 1);
            }
        }
        updateOrderInfo(orderCode, attrMap, result);

        //处理处方单相关
        if (RecipeResultBean.SUCCESS.equals(result.getCode()) && CollectionUtils.isNotEmpty(recipes)) {
            if (PayConstant.PAY_FLAG_PAY_SUCCESS == payFlag && null != order && 0 < order.getActualPrice()) {
                RecipeMsgService.batchSendMsg(recipes.get(0), RecipeStatusConstant.HAVE_PAY);
            }
            Map<String, Object> recipeInfo = Maps.newHashMap();
            recipeInfo.put("payFlag", payFlag);
            recipeInfo.put("payMode", payMode);

            List<Integer> recipeIds = recipes.stream().map(Recipe::getRecipeId).distinct().collect(Collectors.toList());
            // 平台物流对接--物流下单逻辑--且处方购药方式为配送到家
            try {
                if (PayConstant.PAY_FLAG_PAY_SUCCESS == payFlag && null != order && CollectionUtils.isNotEmpty(recipes) && GiveModeEnum.GIVE_MODE_HOME_DELIVERY.getType().equals(recipes.get(0).getGiveMode())) {
                    LOGGER.info("基础服务物流下单,支付回调订单信息={}", JSONObject.toJSONString(order));
                    createLogisticsOrder(orderCode, order, recipes);
                }
            } catch (Exception e) {
                LOGGER.error("基础服务物流下单.error=", e);
            }
            updateRecipeInfo(true, result, recipeIds, recipeInfo, order.getRecipeFee());
            //(异步的过程，不影响主流程)
            GlobalEventExecFactory.instance().getExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 支付成功后调支付平台记账
                        if (PayConstant.PAY_FLAG_PAY_SUCCESS == payFlag && null != order) {
                            try {
                                handleRecipeSplit(order, recipes);
                            } catch (Exception e) {
                                LOGGER.error("支付回调处方记账业务异常，error=", e);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("异步支付回调处方记账业务异常，error=", e);
                    }
                }
            });
        }
        //健康卡数据上传
        RecipeBusiThreadPool.execute(new CardDataUploadRunable(recipes.get(0).getClinicOrgan(), recipes.get(0).getMpiid(), "030102"));
        return result;
    }

    /**
     * 处方记账处理
     *
     * @param order
     * @param recipes
     */
    private void handleRecipeSplit(RecipeOrder order, List<Recipe> recipes) {
        WnAccountSplitParam wnSplitParam = new WnAccountSplitParam();
        // 记账基本信息
        getSplitBaseInfo(order, recipes, wnSplitParam);
        // 记账业务详情
        List<JSONObject> feeList = getSplitFeeInfo(order);
        wnSplitParam.setBusDetail(feeList);
        // 记账账户信息
        getSplitAccountInfo(order, wnSplitParam, recipes);
        LOGGER.info("支付回调支付平台记账入参={}", JSONObject.toJSONString(wnSplitParam));
        IEasyPayService easyPayService = AppContextHolder.getBean("easypay.payService", IEasyPayService.class);
        String splitResult = easyPayService.wnAccountSplitUpload(wnSplitParam);
        LOGGER.info("支付回调支付平台记账结果={}", splitResult);
    }

    /**
     * 处方分账基础信息
     *
     * @param order
     * @param recipes
     * @param wnSplitParam
     * @return
     */
    private void getSplitBaseInfo(RecipeOrder order, List<Recipe> recipes, WnAccountSplitParam wnSplitParam) {
        // 商户订单号
        wnSplitParam.setOutTradeNo(order.getOutTradeNo());
        Recipe recipe = recipes.get(0);
        // 交易金额 总的需要分账的金额
        BigDecimal payAmount = new BigDecimal(order.getActualPrice().toString());
        wnSplitParam.setAmount(payAmount);
        // 业务类型 5-处方
        wnSplitParam.setBusType("5");
        // 患者姓名
        PatientService patientService = BasicAPI.getService(PatientService.class);
        PatientDTO patientDTO = patientService.getPatientByMpiId(recipe.getMpiid());
        if (patientDTO != null) {
            wnSplitParam.setPatientName(patientDTO.getPatientName());
        }
        // 就诊卡号
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if (recipeExtend != null) {
            wnSplitParam.setCardNo(recipeExtend.getCardNo());
        }
        // 问诊科室
        DepartmentService service = BasicAPI.getService(DepartmentService.class);
        DepartmentDTO departmentDTO = service.getById(recipe.getDepart());
        if (departmentDTO != null) {
            wnSplitParam.setDepartId(departmentDTO.getCode());
        }
    }

    /**
     * 获取处方记账账户信息
     * 账户类型 平台-1、医院-2、药店/药企-3、 医生-4、 药师-5
     *
     * @param order
     * @param wnSplitParam
     * @param recipes
     */
    private void getSplitAccountInfo(RecipeOrder order, WnAccountSplitParam wnSplitParam, List<Recipe> recipes) {
        Recipe recipe = recipes.get(0);
        // 医院编码
        wnSplitParam.setYydm(recipe.getClinicOrgan() + "");
        // 分账方编码
        String splitNumber = "";
        // 分账方类型
        Integer splitType = null;
        // 分账方名称
        String splitName = "";
        // getPayeeCode:0平台，1机构，2药企根据getPayeeCode获取对应角色编码、类型、名称
        // 账户类型 平台-1、医院/机构-2、药店/药企-3、 医生-4、 药师-5
        switch (order.getPayeeCode()) {
            case 0:
                splitNumber = RecipeSystemConstant.SPLIT_NO_PLATFORM;
                splitType = 1;
                splitName = RecipeSystemConstant.SPLIT_NAME_PLATFORM;
                break;
            case 1:
                OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
                OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());
                splitNumber = organDTO.getOrganId() + "";
                splitType = 2;
                splitName = organDTO.getName();
                break;
            case 2:
                splitNumber = order.getEnterpriseId() + "";
                splitType = 3;
                DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
                if (null != enterprise) {
                    splitName = enterprise.getName();
                }
                break;
            default:
                break;
        }
        wnSplitParam.setFromName(splitName);
        wnSplitParam.setFromType(splitType + "");
        wnSplitParam.setFromNo(splitNumber);
        // 分账明细 : 处方无法确定各项金额对应的分账比例金额,所以收款方=参与方=分账方 分账金额=总支付金额
        List<WnAccountDetail> splitList = new ArrayList<>();
        WnAccountDetail splitDTO = new WnAccountDetail();
        splitDTO.setAmount(new BigDecimal(order.getActualPrice().toString()));
        splitDTO.setAccountName(splitName);
        splitDTO.setAccountNo(splitNumber);
        splitDTO.setAccountType(splitType + "");
        splitList.add(splitDTO);

        wnSplitParam.setSplitDetail(splitList);
    }

    /**
     * 获取处方记账业务详情
     * 业务详情 : type 1-药费；2-挂号费；3-审方费；4-配送 amount对应金额
     *
     * @param order
     */
    private List<JSONObject> getSplitFeeInfo(RecipeOrder order) {
        BigDecimal payAmount = new BigDecimal(order.getActualPrice().toString());
        List<JSONObject> feeList = new ArrayList<>();
        // 审方费
        BigDecimal auditFee = order.getAuditFee();
        if (null != auditFee && auditFee.compareTo(BigDecimal.ZERO) != 0) {
            JSONObject auditDTO = new JSONObject();
            auditDTO.put("type", RecipeFeeEnum.AUDIT_FEE.getFeeType());
            auditDTO.put("amount", auditFee);
            feeList.add(auditDTO);
        }
        // 药费
        BigDecimal drugAmount = payAmount.subtract(auditFee == null ? new BigDecimal(0) : auditFee);
        JSONObject drugDTO = new JSONObject();
        drugDTO.put("type", RecipeFeeEnum.DRUG_FEE.getFeeType());
        drugDTO.put("amount", drugAmount);
        feeList.add(drugDTO);
        // 挂号费
        if (null != order.getRegisterFee() && order.getRegisterFee().compareTo(BigDecimal.ZERO) != 0) {
            JSONObject registerDTO = new JSONObject();
            registerDTO.put("type", RecipeFeeEnum.REGISTER_FEE.getFeeType());
            registerDTO.put("amount", order.getRegisterFee());
            feeList.add(registerDTO);
        }
        // 配送费
        if (null != order.getExpressFee() && order.getExpressFee().compareTo(BigDecimal.ZERO) != 0) {
            JSONObject expressDTO = new JSONObject();
            expressDTO.put("type", RecipeFeeEnum.EXPRESS_FEE.getFeeType());
            expressDTO.put("amount", order.getRegisterFee());
            feeList.add(expressDTO);
        }
        return feeList;
    }

    private void createLogisticsOrder(String orderCode, RecipeOrder order, List<Recipe> recipeS) {
        // 获取处方药企物流对接方式-仅平台对接物流方式走基础服务物流下单流程
        DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
        if (null != enterprise && enterprise.getLogisticsType() != null && enterprise.getLogisticsType().equals(DrugEnterpriseConstant.LOGISTICS_PLATFORM)) {
            String trackingNumber;
            try {
                ILogisticsOrderService logisticsOrderService = AppContextHolder.getBean("infra.logisticsOrderService", ILogisticsOrderService.class);
                CreateLogisticsOrderDto logisticsOrder = getCreateLogisticsOrderDto(order, recipeS.get(0), enterprise);
                LOGGER.info("基础服务物流下单入参={}", JSONObject.toJSONString(logisticsOrder));
                trackingNumber = logisticsOrderService.addLogisticsOrder(logisticsOrder);
            } catch (Exception e) {
                LOGGER.error("基础服务物流下单异常，发起退款流程 orderId={}，异常=", order.getOrderId(), e);
//                RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
////                recipeService.wxPayRefundForRecipe(6, recipe.getRecipeId(), "物流下单失败");
//                for(int i=0; i<recipeS.size(); i++){
//                    recipeService.wxPayRefundForRecipe(6, recipeS.get(i).getRecipeId(), "物流下单失败");
//                }
                return;
            }
            LOGGER.info("基础服务物流下单结果={}", trackingNumber);
            if (StringUtils.isNotBlank(trackingNumber)) {
                for (int i = 0; i < recipeS.size(); i++) {
                    Recipe recipe = recipeS.get(i);
                    RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "基础服务物流下单成功");
                    // 修改状态为待配送
                    Map<String, Object> paramMap = new HashedMap();
                    paramMap.put("recipeId", recipe.getRecipeId());
                    ThirdEnterpriseCallService callService = ApplicationUtils.getRecipeService(ThirdEnterpriseCallService.class, "takeDrugService");
                    ThirdResultBean resultBean = callService.readyToSend(paramMap);
                    LOGGER.info("基础服务物流下单成功,修改状态为待配送修改参数={},修改结果={}", paramMap, JSONObject.toJSONString(resultBean));
                }

                // 下单成功更新物流单号、物流公司
                Map<String, Object> orderAttrMap = new HashedMap();
                orderAttrMap.put("LogisticsCompany", enterprise.getLogisticsCompany());

                orderAttrMap.put("TrackingNumber", trackingNumber);
                recipeOrderDAO.updateByOrdeCode(orderCode, orderAttrMap);
                RecipeMsgService.batchSendMsg(recipeS.get(0).getRecipeId(), RecipeMsgEnum.EXPRESSINFO_REMIND.getStatus());
                LOGGER.info("基础服务物流下单成功，更新物流单号={},物流公司={},orderId={}", trackingNumber, enterprise.getLogisticsCompany(), order.getOrderId());
            } else {
                // 下单失败发起退款，退款原因=物流下单失败
                LOGGER.info("基础服务物流下单失败，发起退款流程 orderId={}", order.getOrderId());
//                RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
////                recipeService.wxPayRefundForRecipe(6, recipe.getRecipeId(), "物流下单失败");
//                for(int i=0; i<recipeS.size(); i++){
//                    recipeService.wxPayRefundForRecipe(6, recipeS.get(i).getRecipeId(), "物流下单失败");
//                }
            }
        } else if (null != enterprise && enterprise.getLogisticsType() != null && enterprise.getLogisticsType().equals(DrugEnterpriseConstant.LOGISTICS_ENT_HIS)) {
            //药企对接-无回写接口:将处方信息传给基础服务线
            ILogisticsOrderService logisticsOrderService = AppContextHolder.getBean("infra.logisticsOrderService", ILogisticsOrderService.class);
            WriteBackLogisticsOrderDto orderDto = getWriteBackLogisticsOrderDto(order, recipeS.get(0), enterprise);
            LOGGER.info("基础服务物流下单入参 req={}", JSONUtils.toString(orderDto));
            String res = logisticsOrderService.writeBackLogisticsOrder(orderDto);
            LOGGER.info("基础服务物流下单结果 res={}", res);
        }
    }

    private WriteBackLogisticsOrderDto getWriteBackLogisticsOrderDto(RecipeOrder order, Recipe recipe, DrugsEnterprise enterprise) {
        WriteBackLogisticsOrderDto orderDto = new WriteBackLogisticsOrderDto();
        // 机构id
        orderDto.setOrganId(recipe.getClinicOrgan());
        // 业务类型
        orderDto.setBusinessType(DrugEnterpriseConstant.BUSINESS_TYPE);
        // 业务编码
        orderDto.setBusinessNo(order.getOrderCode());
        // 物流公司编码
        orderDto.setLogisticsCode("1003");
        //纳里收件人主键
        orderDto.setUserId(order.getReceiver());
        // 收件人名称
        orderDto.setAddresseeName(order.getReceiver());
        // 收件人手机号
        orderDto.setAddresseePhone(order.getRecMobile());
        // 收件省份
        orderDto.setAddresseeProvince(getAddressDic(order.getAddress1()));
        // 收件城市
        orderDto.setAddresseeCity(getAddressDic(order.getAddress2()));
        // 收件镇/区
        orderDto.setAddresseeDistrict(getAddressDic(order.getAddress3()));
        // 收件人街道
        orderDto.setAddresseeStreet(getAddressDic(order.getStreetAddress()));
        // 收件详细地址
        orderDto.setAddresseeAddress(order.getAddress4());
        //寄托物名称
        orderDto.setDepositumName(DrugEnterpriseConstant.DEPOSITUM_NAME);
        //运单号
        orderDto.setWaybillNo(recipe.getRecipeCode());
        //运单费用
        orderDto.setWaybillFee(order.getExpressFee());

        RecipeExtendDAO recipeExtendDAO = getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        //门诊号
        orderDto.setOutpatientNumber(recipeExtend.getRegisterID());
        return orderDto;
    }

    private CreateLogisticsOrderDto getCreateLogisticsOrderDto(RecipeOrder order, Recipe recipe, DrugsEnterprise enterprise) {
        CreateLogisticsOrderDto logisticsOrder = new CreateLogisticsOrderDto();
        // 机构id
        logisticsOrder.setOrganId(recipe.getClinicOrgan());
        // 平台用户id
        logisticsOrder.setUserId(recipe.getMpiid());
        // 业务类型
        logisticsOrder.setBusinessType(DrugEnterpriseConstant.BUSINESS_TYPE);
        // 业务编码
        logisticsOrder.setBusinessNo(order.getOrderCode());
        // 快递编码
        logisticsOrder.setLogisticsCode(enterprise.getLogisticsCompany() + "");
        // 寄件人姓名
        logisticsOrder.setConsignorName(enterprise.getConsignorName());
        // 寄件人手机号
        logisticsOrder.setConsignorPhone(enterprise.getConsignorMobile());
        // 寄件人省份
        logisticsOrder.setConsignorProvince(getAddressDic(enterprise.getConsignorProvince()));
        // 寄件人城市
        logisticsOrder.setConsignorCity(getAddressDic(enterprise.getConsignorCity()));
        // 寄件人区域
        logisticsOrder.setConsignorDistrict(getAddressDic(enterprise.getConsignorDistrict()));
        // 寄件人街道
        logisticsOrder.setConsignorStreet(getAddressDic(enterprise.getConsignorStreet()));
        // 寄件人详细地址
        logisticsOrder.setConsignorAddress(enterprise.getConsignorAddress());
        // 收件人名称
        logisticsOrder.setAddresseeName(order.getReceiver());
        // 收件人手机号
        logisticsOrder.setAddresseePhone(order.getRecMobile());
        // 收件省份
        logisticsOrder.setAddresseeProvince(getAddressDic(order.getAddress1()));
        // 收件城市
        logisticsOrder.setAddresseeCity(getAddressDic(order.getAddress2()));
        // 收件镇/区
        logisticsOrder.setAddresseeDistrict(getAddressDic(order.getAddress3()));
        // 收件人街道
        logisticsOrder.setAddresseeStreet(getAddressDic(order.getStreetAddress()));
        // 收件详细地址
        logisticsOrder.setAddresseeAddress(order.getAddress4());
        // 寄托物名称
        logisticsOrder.setDepositumName(DrugEnterpriseConstant.DEPOSITUM_NAME);
        // 就诊人信息
        try {
            IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);
            PatientBean patientBean = iPatientService.get(recipe.getMpiid());
            if (patientBean != null) {
                // 就诊人名称
                logisticsOrder.setPatientName(patientBean.getPatientName());
                // 就诊人手机号
                logisticsOrder.setPatientPhone(patientBean.getMobile());
                // 就诊人身份证
                String cardNo = StringUtils.isNotBlank(patientBean.getIdcard()) ? patientBean.getIdcard() : patientBean.getIdcard2();
                if (StringUtils.isNotBlank(cardNo) && cardNo.length() > 18) {
                    cardNo = null;
                }
                logisticsOrder.setPatientIdentityCardNo(cardNo);

            }
            // 挂号序号
            if (recipe.getClinicId() != null) {
                IRevisitExService iRevisitExService = RevisitAPI.getService(IRevisitExService.class);
                RevisitExDTO consultExDTO = iRevisitExService.getByConsultId(recipe.getClinicId());
                if (consultExDTO != null) {
                    logisticsOrder.setOutpatientNumber(consultExDTO.getRegisterNo());
                }
            }
        } catch (Exception e) {
            LOGGER.error("基础服务物流下单非必填信息获取异常：", e);
        }
        return logisticsOrder;
    }

    /**
     * 对来源于HIS的处方单状态更新为已处理
     *
     * @param recipes
     */
    public void updateHisRecieStatus(List<Recipe> recipes) {
        try {
            if (!CollectionUtils.isEmpty(recipes)) {
                HisRecipeDAO hisRecipeDAO = getDAO(HisRecipeDAO.class);
                for (Recipe recipe : recipes) {
                    if (recipe == null) {
                        continue;
                    }
                    HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(recipe.getClinicOrgan(), recipe.getRecipeCode());
                    if (hisRecipe != null) {
                        hisRecipeDAO.updateHisRecieStatus(recipe.getClinicOrgan(), recipe.getRecipeCode(), 2);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.info("updateHisRecieStatus 来源于HIS的处方单更新hisRecipe的状态失败,recipeId:{},{}.", JSONUtils.toString(recipes), e);
        }
    }


    /**
     * @param nowRecipe 处方
     * @param payMode   支付方式
     * @return void
     * @method useCoupon
     * @description 使用优惠券
     * @date: 2019/10/17
     * @author: JRK
     */
    private void useCoupon(Recipe nowRecipe, Integer payMode) {
        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
        RecipeOrder order = recipeOrderDAO.getByOrderCode(nowRecipe.getOrderCode());
        if (RecipeBussConstant.PAYMODE_ONLINE.equals(order.getPayMode()) && isUsefulCoupon(order.getCouponId())) {
            ICouponBaseService couponService = AppContextHolder.getBean("voucher.couponBaseService", ICouponBaseService.class);
            couponService.useCouponById(order.getCouponId());
        }
    }

    //药店有库存或者无库存备货给患者推送消息
    private void sendTfdsMsg(Recipe nowRecipe, Integer payMode, String orderCode) {
        //药店取药推送
        LOGGER.info("sendTfdsMsg nowRecipeId:{}.payMode:{}.orderCode:{}.", JSONUtils.toString(nowRecipe.getRecipeId()),JSONUtils.toString(payMode),JSONUtils.toString(orderCode));
        if (RecipeBussConstant.PAYMODE_TFDS.equals(payMode) && nowRecipe.getReviewType() != ReviewTypeConstant.Postposition_Check) {
            RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            DrugsEnterpriseDAO drugsEnterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
            RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
            RecipeOrder order = recipeOrderDAO.getByOrderCode(orderCode);
            //这里去的是订单中存的药企信息
            if (order.getEnterpriseId() == null) {
                LOGGER.info("审方前置或者不审核-药店取药-药企为空");
            } else {
                DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(nowRecipe.getEnterpriseId());
                DrugEnterpriseResult result = remoteDrugService.scanStock(nowRecipe.getRecipeId(), drugsEnterprise);
                boolean scanFlag = result.getCode().equals(DrugEnterpriseResult.SUCCESS) ? true : false;
                LOGGER.info("sendTfdsMsg sacnFlag: {}.", scanFlag);
                if (scanFlag) {
                    //表示需要进行库存校验并且有库存
                    RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_DRUG_HAVE_STOCK, nowRecipe);
                } else if (drugsEnterprise.getCheckInventoryFlag() == 2) {
                    //表示无库存但是药店可备货
                    RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_DRUG_NO_STOCK_READY, nowRecipe);
                }
            }
        } else if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(nowRecipe.getGiveMode())
                && !nowRecipe.getReviewType().equals(ReviewTypeConstant.Postposition_Check)
        ) {
            // 支付成功 到院取药 推送消息 审方前置
            RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_HOS_TAKE_MEDICINE, nowRecipe);
        }

    }


    /**
     * @param reviewType 审核方式
     * @param giveMode   购药方式
     * @return int 订单的修改状态
     * @method getPayStatus
     * @description 获取订单的处理的状态
     * @date: 2019/9/20
     * @author: JRK
     */
    private int getPayStatus(Integer reviewType, Integer giveMode, Recipe nowRecipe) {
        int payStatus = 0;
        //支付成功、支付前不需要支付时判断审核方式
        if (ReviewTypeConstant.Postposition_Check == reviewType) {
            //后置
            payStatus = OrderStatusConstant.READY_CHECK;
        } else {
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
     * @param
     * @return
     */
    @RpcService
    public RecipeResultBean finishOrder(String orderCode, Map<String, Object> orderAttr) {
        RecipeResultBean result = RecipeResultBean.getSuccess();
        if (StringUtils.isEmpty(orderCode)) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("缺少参数");
        }
        RecipeOrder order = recipeOrderDAO.getByOrderCode(orderCode);

        if (RecipeResultBean.SUCCESS.equals(result.getCode())) {
            Map<String, Object> attrMap = Maps.newHashMap();
            attrMap.put("effective", 1);
            attrMap.put("payFlag", PayConstant.PAY_FLAG_PAY_SUCCESS);
            if (RecipeBussConstant.PAYMODE_OFFLINE.equals(order.getPayMode())) {
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

    @RpcService
    public String getThirdUrl(Integer recipeId) {
        SkipThirdBean skipThirdBean = getThirdUrlNew(recipeId);
        if (skipThirdBean != null && StringUtils.isNotEmpty(skipThirdBean.getUrl())) {
            return skipThirdBean.getUrl();
        }
        return "";

    }

    /**
     * 从微信模板消息跳转时 先获取一下是否需要跳转第三方地址
     * 或者处方审核成功后推送处方卡片消息时点击跳转(互联网)
     *
     * @return
     */
    @RpcService
    public SkipThirdBean getThirdUrlNew(Integer recipeId) {
        SkipThirdBean skipThirdBean = new SkipThirdBean();
        if (null == recipeId) {
            return new SkipThirdBean();
        }
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);

        Recipe recipe = recipeDAO.get(recipeId);
        if (recipe.getClinicOrgan() == 1005683) {
            return getUrl(recipe);
        }
        if (null != recipe && recipe.getEnterpriseId() != null) {
            DrugsEnterpriseDAO dao = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            DrugsEnterprise drugsEnterprise = dao.getById(recipe.getEnterpriseId());
            if (drugsEnterprise != null && "bqEnterprise".equals(drugsEnterprise.getAccount())) {
                return getUrl(recipe);
            }
            RecipeOrder order = recipeOrderDAO.getOrderByRecipeId(recipeId);
            if (null == order) {
                return skipThirdBean;
            }
        }
        return skipThirdBean;
    }

    private SkipThirdBean getUrl(Recipe recipe) {
        SkipThirdBean skipThirdBean = new SkipThirdBean();
        String thirdUrl = "";
        if (null != recipe) {
            PatientDTO patient = patientService.get(recipe.getMpiid());
            PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
            if (patient != null) {
                patientBaseInfo.setPatientName(patient.getPatientName());
                patientBaseInfo.setCertificateType(patient.getCertificateType());
                patientBaseInfo.setCertificate(patient.getCertificate());
                patientBaseInfo.setMobile(patient.getMobile());
                patientBaseInfo.setPatientID(recipe.getPatientID());
                patientBaseInfo.setMpi(recipe.getRequestMpiId());
                // 黄河医院获取药企患者id
                try {
                    ICurrentUserInfoService userInfoService = AppContextHolder.getBean("eh.remoteCurrentUserInfoService", ICurrentUserInfoService.class);
                    SimpleWxAccountBean account = userInfoService.getSimpleWxAccount();
                    LOGGER.info("querySimpleWxAccountBean account={}", JSONObject.toJSONString(account));
                    if (null != account) {
                        if (account instanceof SimpleThirdBean) {
                            SimpleThirdBean stb = (SimpleThirdBean) account;
                            patientBaseInfo.setTid(stb.getTid());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("黄河医院获取药企用户tid异常", e);
                }
            }
            PatientBaseInfo userInfo = new PatientBaseInfo();
            if (StringUtils.isNotEmpty(recipe.getRequestMpiId())) {
                PatientDTO user = patientService.get(recipe.getRequestMpiId());
                if (user != null) {
                    userInfo.setPatientName(user.getPatientName());
                    userInfo.setCertificate(user.getCertificate());
                    userInfo.setCertificateType(user.getCertificateType());
                    userInfo.setMobile(user.getMobile());
                }
            }
            IRecipeEnterpriseService hisService = AppDomainContext.getBean("his.iRecipeEnterpriseService", IRecipeEnterpriseService.class);
            RecipeThirdUrlReqTO req = new RecipeThirdUrlReqTO();
            req.setOrganId(recipe.getClinicOrgan());
            req.setPatient(patientBaseInfo);
            req.setUser(userInfo);
            req.setRecipeCode(String.valueOf(recipe.getRecipeId()));
            HisResponseTO<String> response;
            // 从复诊获取患者渠道id
            String patientChannelId = "";
            try {
                if (recipe.getClinicId() != null) {
                    IRevisitExService exService = RevisitAPI.getService(IRevisitExService.class);
                    LOGGER.info("queryPatientChannelId req={}", recipe.getClinicId());
                    RevisitExDTO revisitExDTO = exService.getByConsultId(recipe.getClinicId());
                    if (revisitExDTO != null) {
                        LOGGER.info("queryPatientChannelId res={}", JSONObject.toJSONString(revisitExDTO));
                        patientChannelId = revisitExDTO.getProjectChannel();
                        req.setPatientChannelId(patientChannelId);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("queryPatientChannelId error:", e);
            }
            try {
                //获取民科机构登记号
                req.setOrgCode(RecipeServiceSub.getMinkeOrganCodeByOrganId(recipe.getClinicOrgan()));
                LOGGER.info("getRecipeThirdUrl request={}", JSONUtils.toString(req));
                response = hisService.getRecipeThirdUrl(req);
                LOGGER.info("getRecipeThirdUrl res={}", JSONUtils.toString(response));
                if (response != null && "200".equals(response.getMsgCode())) {
                    thirdUrl = response.getData();
                    //前置机传过来的可能是json字符串也可能是非json
                    try {
                        skipThirdBean = JSONObject.parseObject(thirdUrl, SkipThirdBean.class);
                    } catch (Exception e) {
                        //说明不是标准的JSON格式
                        skipThirdBean.setUrl(thirdUrl);
                    }
                } else {
                    throw new DAOException(609, "获取第三方跳转链接异常");
                }
            } catch (Exception e) {
                LOGGER.error("getRecipeThirdUrl error ", e);
                throw new DAOException(609, "获取第三方跳转链接异常");
            }
        }
        return skipThirdBean;
    }

    /**
     * 临沭东软对接慢病处方流转平台跳转第三方地址
     *
     * @return
     */
    @RpcService
    public String getLSRecipeThirdUrl(Integer recipeId) {
        Assert.notNull(recipeId, "recipeId must be not null");
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(recipeId);
        String thirdUrl = "";
        if (null != recipe) {
            PatientDTO patient = patientService.get(recipe.getMpiid());
            if (patient != null) {
                PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
                patientBaseInfo.setPatientName(patient.getPatientName());
                patientBaseInfo.setCertificateType(1);
                patientBaseInfo.setCertificate(patient.getIdcard());
                patientBaseInfo.setMobile(patient.getMobile());
                IRecipeEnterpriseService hisService = AppDomainContext.getBean("his.iRecipeEnterpriseService", IRecipeEnterpriseService.class);
                RecipeThirdUrlReqTO req = new RecipeThirdUrlReqTO();
                req.setOrganId(recipe.getClinicOrgan());
                req.setPatient(patientBaseInfo);
                LOGGER.info("getLSRecipeThirdUrl request={}", JSONUtils.toString(req));
                HisResponseTO<String> response;
                try {
                    response = hisService.getRecipeThirdUrl(req);
                    LOGGER.info("getLSRecipeThirdUrl res={}", JSONUtils.toString(response));
                    if (response != null) {
                        thirdUrl = response.getData();
                    }
                } catch (Exception e) {
                    LOGGER.error("getLSRecipeThirdUrl error ", e);
                }
            }
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
        if (!payModeSupport.isSupportDownload()) {
            full = full.add(order.getRecipeFee());
        }

        //配送费
        //有配送费并且配送费支付方式为不等于第三方支付或者不等于上传运费收费标准时才计入支付金额
        if (null != order.getExpressFee() && !(new Integer(3).equals(order.getExpressFeePayWay()) || new Integer(4).equals(order.getExpressFeePayWay()))) {
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
        if (null != recipe && ReviewTypeConstant.Not_Need_Check != recipe.getReviewType() && null != order.getAuditFee()) {
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
        if (CollectionUtils.isNotEmpty(recipes) && ReviewTypeConstant.Not_Need_Check != recipes.get(0).getReviewType() && null != order.getAuditFee()) {
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
    private Integer createOrderToDB(RecipeOrder order, List<Integer> recipeIds, RecipeOrderDAO orderDAO, RecipeDAO recipeDAO) throws DAOException {
        order = orderDAO.save(order);
        if (null != order.getOrderId()) {
            recipeDAO.updateOrderCodeByRecipeIds(recipeIds, order.getOrderCode());
        }
        return order.getOrderId();
    }

    private void saveOrderInfo(Integer recipeId, Map<String, Object> paramMap) {
        Map<String, Object> orderAttr = new HashMap<>();
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (recipe != null) {
            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
            String logisticsCompany = MapValueUtil.getString(paramMap, "logisticsCompany");
            String trackingNumber = MapValueUtil.getString(paramMap, "trackingNumber");
            orderAttr.put("logisticsCompany", StringUtils.isEmpty(logisticsCompany) ? null : Integer.valueOf(logisticsCompany));
            orderAttr.put("trackingNumber", trackingNumber);
            orderService.updateOrderInfo(recipe.getOrderCode(), orderAttr, null);
        }
    }

    /**
     * 更新处方订单信息(状态维护)
     *
     * @param recipeId
     * @param attrMap
     * @return
     */
    @RpcService
    public RecipeResultBean updateOrderStatus(Integer recipeId, Map<String, Object> attrMap) {

        RecipeResultBean resultBean = RecipeResultBean.getSuccess();

        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);

        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        //状态转化
        Integer status2 = RecipeStatusToOrderEnum.getValue((Integer) attrMap.get("status"));
        attrMap.put("sender", "system");
        attrMap.put("sendTime", new Date());
        attrMap.put("recipeId", recipeId);
        String trackingNumber = MapValueUtil.getString(attrMap, "trackingNumber");
        if (StringUtils.isNotEmpty(trackingNumber)) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByTrackingNumber(trackingNumber);
            if (recipeOrder != null) {
                //说明已经存在则无法保存
                resultBean.setCode(RecipeResultBean.FAIL);
                resultBean.setMsg("该物流单号已经存在，请确认重新填写!");
                return resultBean;
            }
        }
        ThirdEnterpriseCallService thirdEnterpriseCallService = new ThirdEnterpriseCallService();
        if (1 == recipe.getGiveMode() && status2 != null) {
            if (RecipeStatusConstant.IN_SEND == status2) {
                resultBean = thirdEnterpriseCallService.toSend(attrMap);
                saveOrderInfo(recipeId, attrMap);
            } else if (RecipeStatusConstant.FINISH == status2) {
                resultBean = thirdEnterpriseCallService.finishRecipe(attrMap);
                saveOrderInfo(recipeId, attrMap);
            } else if (RecipeStatusConstant.RECIPE_FAIL == status2) {
                resultBean = thirdEnterpriseCallService.RecipeFall(attrMap);
            }
        } else if (3 == recipe.getGiveMode() && status2 != null) {
            if (RecipeStatusConstant.FINISH == status2) {
                attrMap.put("result", "1");
                resultBean = thirdEnterpriseCallService.recordDrugStoreResult(attrMap);
            } else if (RecipeStatusConstant.RECIPE_FAIL == status2) {
                resultBean = thirdEnterpriseCallService.RecipeFall(attrMap);
            }
        }
        return resultBean;
    }

    /**
     * 更新订单信息addDrugsEnterprise
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
                LOGGER.error("订单更新失败");
            }
        } catch (Exception e) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("订单更新失败," + e.getMessage());
            LOGGER.error("订单更新失败,{}", e.getMessage(), e);
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
    private RecipeResultBean updateRecipeInfo(boolean saveFlag, RecipeResultBean result, List<Integer> recipeIds, Map<String, Object> recipeInfo, BigDecimal recipeFee) {
        LOGGER.info("recipeOrder updateRecipeInfo recipeIds={}", JSON.toJSONString(recipeIds));
        if (null == result) {
            result = RecipeResultBean.getSuccess();
        }

        if (CollectionUtils.isEmpty(recipeIds)) {
            LOGGER.error("updateRecipeInfo param recipeIds size is zero. result={}", JSONUtils.toString(result));
            return result;
        }

        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);

        for (Integer recipeId : recipeIds) {
            Integer payFlag = MapValueUtil.getInteger(recipeInfo, "payFlag");
            if (!Arrays.asList(PayConstant.PAY_FLAG_PAY_SUCCESS, PayConstant.PAY_FLAG_NOT_PAY).contains(payFlag)) {
                continue;
            }
            RecipeResultBean resultBean = recipeService.updateRecipePayResultImplForOrder(saveFlag, recipeId, payFlag, recipeInfo, recipeFee);
            if (RecipeResultBean.FAIL.equals(resultBean.getCode())) {
                result.setCode(RecipeResultBean.FAIL);
                result.setError(resultBean.getError());
                break;
            }
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

    /*
     * @description 订单人脸识别faceToken存储
     * @author gaomw
     * @date 2019/12/13
     * @param [recipeId]
     * @return recipe.bean.DrugEnterpriseResult
     */
    @RpcService
    public RecipeResultBean saveSmkFaceToken(String orderCode, String smkFaceToken) {

        RecipeResultBean result = this.updateOrderInfo(orderCode, ImmutableMap.of("smkFaceToken", smkFaceToken), null);
        return result;
    }

    @RpcService
    public void recipeMedicInsurSettleUpdateOrder(MedicInsurSettleSuccNoticNgariReqDTO request) {
        // 更新处方订单数据
        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
        RecipeOrder recipeOrder = recipeOrderDAO.getOrderByRecipeId(Integer.valueOf(request.getRecipeId()));
        recipeOrder.setPayOrganId(request.getOrganId().toString());
        recipeOrder.setPayFlag(1);
        recipeOrder.setActualPrice(Optional.ofNullable(request.getTotalAmount()).orElse(0.00));
        recipeOrder.setFundAmount(Optional.ofNullable(request.getFundAmount()).orElse(0.00));
        recipeOrder.setCashAmount(Optional.ofNullable(request.getCashAmount()).orElse(0.00));
        recipeOrder.setTradeNo(request.getInsuTSN());
        recipeOrder.setStatus(3);
        recipeOrder.setSettleOrderNo(request.getPayOrderNo());
        recipeOrder.setPayTime(request.getSettlingTime());
        recipeOrderDAO.update(recipeOrder);
        // 更新处方数据
        List<Integer> recipeIds = Arrays.asList(Integer.valueOf(request.getRecipeId()));
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        recipeDAO.updateOrderCodeByRecipeIds(recipeIds, recipeOrder.getOrderCode());
        // 更新处方详情数据 保存his发票号
        RecipeDetailDAO recipeDetailDAO = getDAO(RecipeDetailDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(Integer.valueOf(request.getRecipeId()));
        if (CollectionUtils.isNotEmpty(recipedetails)) {
            recipedetails.forEach(item -> {
                item.setPatientInvoiceNo(request.getInvoiceId());
                item.setLastModify(new Date());
                recipeDetailDAO.update(item);
            });
        }
        // 处方推送到药企
        RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        remoteDrugEnterpriseService.pushSingleRecipeInfo(Integer.valueOf(request.getRecipeId()));
    }

    @RpcService
    public Boolean updateApothecaryByOrderId(ApothecaryVO apothecary) throws ValidateException {
        if (null == apothecary || null == apothecary.getOrderId()) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "订单不存在");
        }
        ChinaIDNumberUtil.isValidIDNumber(apothecary.getDispensingApothecaryIdCard());
        try {
            recipeOrderDAO.updateApothecaryByOrderId(apothecary.getOrderId(), apothecary.getDispensingApothecaryName(), apothecary.getDispensingApothecaryIdCard());
            return true;
        } catch (Exception e) {
            LOGGER.error("updateApothecaryByOrderId apothecaryVO :{}", JSONUtils.toString(apothecary), e);
            return false;
        }
    }

    private String getAddressDic(String area) {
        if (StringUtils.isNotEmpty(area)) {
            try {
                return DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(area);
            } catch (ControllerException e) {
                LOGGER.error("getAddressDic 获取地址数据类型失败*****area:" + area, e);
            }
        }
        return "";
    }
}
