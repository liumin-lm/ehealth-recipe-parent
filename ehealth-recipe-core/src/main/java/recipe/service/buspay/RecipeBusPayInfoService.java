package recipe.service.buspay;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.ngari.auth.api.service.IAuthExtraService;
import com.ngari.auth.api.vo.SignNoReq;
import com.ngari.auth.api.vo.SignNoRes;
import com.ngari.base.BaseAPI;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.OrganConfigDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.*;
import com.ngari.recipe.RecipeAPI;
import com.ngari.recipe.common.RecipeBussResTO;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.drugsenterprise.service.IDrugsEnterpriseService;
import com.ngari.recipe.pay.model.WnExtBusCdrRecipeDTO;
import com.ngari.recipe.pay.service.IRecipeBusPayService;
import com.ngari.recipe.recipe.constant.RecipePayTipEnum;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.Base64;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.base.constant.ErrorCode;
import eh.cdr.constant.OrderStatusConstant;
import eh.entity.bus.Order;
import eh.entity.bus.pay.BusTypeEnum;
import eh.entity.bus.pay.ConfirmOrder;
import eh.entity.bus.pay.SimpleBusObject;
import eh.entity.mpi.Patient;
import eh.utils.MapValueUtil;
import eh.wxpay.constant.PayConstant;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import recipe.manager.ButtonManager;
import recipe.serviceprovider.recipe.service.RemoteRecipeService;
import recipe.serviceprovider.recipeorder.service.RemoteRecipeOrderService;
import recipe.third.HztServiceInterface;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * created by shiyuping on 2021/1/25
 * base里的处方业务支付信息放到recipe里处理
 */
@RpcBean
public class RecipeBusPayInfoService implements IRecipeBusPayService {

    private static final Logger log = LoggerFactory.getLogger(RecipeBusPayInfoService.class);

    @Autowired
    private RemoteRecipeOrderService recipeOrderService;

    @Qualifier("remoteRecipeService")
    @Autowired
    private RemoteRecipeService recipeService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private ButtonManager buttonManager;


    private IConfigurationCenterUtilsService utils = BaseAPI.getService(IConfigurationCenterUtilsService.class);

    /**
     * 第一次创建订单时会调用到
     * 第二次提交订单时也会调用到
     *
     * @param busType 业务类型
     * @param busId   业务id  第一次创建订单时会调用到此时busId为-1，第二次为已经生成的订单id
     * @param extInfo 扩展信息
     * @return
     */
    @Override
    @RpcService
    public ConfirmOrder obtainConfirmOrder(String busType, Integer busId, Map<String, String> extInfo) {
        //先判断处方是否已创建订单
        RecipeOrderBean order = null;
        RecipeExtendBean recipeExtend = null;
        Integer recipeId = null;
        //创建订单时的调用
        if (busId == -1) {
            //合并支付改造--创建临时订单时获取处方id列表
            String recipeIds = MapValueUtil.getString(extInfo, "recipeId");
            if (StringUtils.isNotEmpty(recipeIds)) {
                List<String> recipeIdString = Splitter.on(",").splitToList(recipeIds);
                List<Integer> recipeIdLists = recipeIdString.stream().map(Integer::valueOf).collect(Collectors.toList());
                busId = recipeIdLists.get(0);
                order = recipeOrderService.getOrderByRecipeId(busId);
                if (null == order) {
                    //这里为了组装创建订单时的一些订单数据--此时还未生成处方订单
                    RecipeBussResTO<RecipeOrderBean> resTO = recipeOrderService.createBlankOrder(recipeIdLists, extInfo);
                    if (null != resTO) {
                        order = resTO.getData();
                    } else {
                        log.info("obtainConfirmOrder createBlankOrder order is null.");
                        return null;
                    }
                }
            }
        } else {
            //提交订单后的调用获取订单信息
            order = recipeOrderService.get(busId);
        }
        if (order == null) {
            log.info("RecipeBusPayService.obtainConfirmOrder order is null. busId={}", busId);
            return null;
        }
        //获取处方扩展信息
        if (StringUtils.isNotEmpty(order.getRecipeIdList())) {
            List<Integer> recipeIdList = JSONUtils.parse(order.getRecipeIdList(), List.class);
            recipeId = recipeIdList.get(0);
            recipeExtend = recipeService.findRecipeExtendByRecipeId(recipeId);
        }
        if (recipeId == null) {
            log.info("RecipeBusPayService.obtainConfirmOrder recipeId is null. busId={}", busId);
            return null;
        }

        //his管理的药企处理
        //如果扩展信息中有处方流转平台返回的药企则要以此优先
        if (recipeExtend != null && recipeExtend.getDeliveryCode() != null) {
            extInfo.put("depId", recipeExtend.getDeliveryCode());
        }
        //如果扩展信息中有处方流转平台返回的药企则要以此优先
        if (recipeExtend != null && recipeExtend.getDeliveryName() != null) {
            order.setEnterpriseName(recipeExtend.getDeliveryName());
        }
        //date 20200312
        //订单详情展示his推送信息
        //date  20200320
        //添加判断配送药企his信息只有配送方式才有
        if (StringUtils.isNotEmpty(extInfo.get("hisDepCode"))) {
            order.setEnterpriseName(extInfo.get("depName"));
        }

        ConfirmOrder confirmOrder = new ConfirmOrder();
        confirmOrder.setBusId(busId);
        confirmOrder.setBusType(busType);
        BusTypeEnum busTypeEnum = BusTypeEnum.fromCode(busType);
        if (busTypeEnum != null) {
            confirmOrder.setBusTypeName(busTypeEnum.getDesc());
        }
        confirmOrder.setCouponId(order.getCouponId());
        //计算优惠的金额=订单总金额-订单实际金额
        BigDecimal discountAmount = order.getTotalFee().subtract(BigDecimal.valueOf(order.getActualPrice()));
        //配送到家并且运费线下支付的情况：优惠金额要再减去运费
        if (new Integer(2).equals(order.getExpressFeePayWay()) && new Integer(1).equals(MapValueUtil.getInteger(extInfo, "payMode"))) {
            if (order.getExpressFee() != null && order.getTotalFee().compareTo(order.getExpressFee()) > -1) {
                discountAmount = discountAmount.subtract(order.getExpressFee());
            }
        }
        confirmOrder.setDiscountAmount(discountAmount.toString());
        confirmOrder.setOrderAmount(order.getTotalFee().stripTrailingZeros().toPlainString());
        confirmOrder.setActualPrice(BigDecimal.valueOf(order.getActualPrice()).stripTrailingZeros().toPlainString());
        confirmOrder.setBusObject(order);
        //设置confirmOrder的扩展信息ext----一些配置信息
        confirmOrder.setExt(setConfirmOrderExtInfo(order, recipeId, extInfo, recipeExtend));
        log.info("obtainConfirmOrder recipeId:{} res ={}", recipeId, JSONUtils.toString(confirmOrder));
        return confirmOrder;
    }

    private Map<String, String> setConfirmOrderExtInfo(RecipeOrderBean order, Integer recipeId, Map<String, String> extInfo, RecipeExtendBean recipeExtend) {
        IDrugsEnterpriseService drugsEnterpriseService = RecipeAPI.getService(IDrugsEnterpriseService.class);
        Map<String, String> map = Maps.newHashMap();
        //返回是否医保处方单
        if (order != null && order.getRegisterNo() != null) {
            map.put("medicalType", "1");
        } else {
            map.put("medicalType", "0");
        }
        //医保金额
        Double fundAmount = order.getFundAmount() == null ? 0.00 : order.getFundAmount();
        //自费金额=实际金额-医保金额
        BigDecimal cashAmount = BigDecimal.valueOf(order.getActualPrice()).subtract(BigDecimal.valueOf(fundAmount));
        map.put("fundAmount", fundAmount + "");
        map.put("cashAmount", cashAmount + "");
        // 加载确认订单页面的时候需要将页面属性字段做成可配置的
        Integer organId = order.getOrganId();
        if (null != organId) {
            OrganConfigDTO organConfig = BasicAPI.getService(OrganConfigService.class).get(organId);
            if (null != organConfig) {
                map.put("serviceChargeDesc", organConfig.getServiceChargeDesc());
                map.put("serviceChargeRemark", organConfig.getServiceChargeRemark());
            }
            //设置其他费用文案
            double otherFee = 0.0;
            //date 2019/10/23
            //添加非空判断
            if (null != utils.getConfiguration(organId, "otherFee") && null != utils.getConfiguration(organId, "otherServiceChargeDesc") && null != utils.getConfiguration(organId, "otherServiceChargeRemark")) {
                otherFee = Double.parseDouble(utils.getConfiguration(organId, "otherFee").toString());
                if (otherFee > 0.0) {
                    map.put("otherServiceChargeDesc", utils.getConfiguration(organId, "otherServiceChargeDesc").toString());
                    map.put("otherServiceChargeRemark", utils.getConfiguration(organId, "otherServiceChargeRemark").toString());
                }
            }

            //获取展示窗口调试信息,取处方详情中的药品的取药窗口信息
            //List<RecipeDetailBean> details = recipeService.findRecipeDetailsByRecipeId(recipeId);
            OrganService organService = BasicAPI.getService(OrganService.class);
            OrganDTO organDTO = organService.getByOrganId(organId);
            //取处方详情中的药品的取药窗口信息
            // 取药窗口修改为从扩展表中获取
            if (!Objects.isNull(recipeExtend) && StringUtils.isNotEmpty(recipeExtend.getPharmNo())) {
                map.put("getDrugWindow", organDTO.getName() + recipeExtend.getPharmNo() + "取药窗口");
            }

            //设置支付提示信息，根据提示处方信息获取具体的支付提示文案
            RecipeBean nowRecipeBean = recipeService.getByRecipeId(recipeId);
            Integer depId = MapValueUtil.getInteger(extInfo, "depId");
            Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
            DrugsEnterpriseBean drugsEnterpriseBean = null;
            if (depId != null) {
                drugsEnterpriseBean = drugsEnterpriseService.get(depId);
            }

            if (null != nowRecipeBean) {
                Boolean notNeedCheckFee = (0 == nowRecipeBean.getReviewType() || 0 == order.getAuditFee().compareTo(BigDecimal.ZERO));
                log.info("fromRecipeModeAndPayModeAndNotNeedCheckFee recipeId:{} recipeMode:{} payMode:{} notNeedCheckFee:{}", recipeId, nowRecipeBean.getRecipeMode(), payMode, notNeedCheckFee);
                RecipePayTipEnum tipEnum = RecipePayTipEnum.fromRecipeModeAndPayModeAndNotNeedCheckFee(nowRecipeBean.getRecipeMode(), payMode, notNeedCheckFee);
                //支付提示信息
                String payTip = tipEnum.getPayTip();
                log.info("fromRecipeModeAndPayModeAndNotNeedCheckFee recipeId:{} payTip:{} ", recipeId, payTip);
                //替换支付提示信息中的¥为¥+具体审核费
                payTip = null == payTip ? null : payTip.replace("¥", "¥" + order.getAuditFee().toString());

                // 20201117 如果是到院取药，则药品费用支付提示使用配置文案提示
                if (tipEnum.equals(RecipePayTipEnum.To_Hos_Need_CheckFee) || tipEnum.equals(RecipePayTipEnum.To_Hos_No_CheckFee)) {
                    String hisDrugPayTipCfg = (String) utils.getConfiguration(organId, "getHisDrugPayTip");
                    payTip = tipEnum.getToHosPayTip(hisDrugPayTipCfg, order.getAuditFee());
                }

                //这里应该是药店取药支付方式为1时不展示支付提示信息
                if (drugsEnterpriseBean != null && Integer.valueOf(1).equals(drugsEnterpriseBean.getStorePayFlag())) {
                    map.put("payTip", "");
                    map.put("payNote", "");
                } else {
                    map.put("payTip", payTip);
                    map.put("payNote", tipEnum.getPayNote());
                }
                //date 20200610
                //判断当配置的药师审核金额为空时不展示
                //不需要审方审核费默认为0否则就从运营平台配置从取审核费
                String showAuditFee = 0 != nowRecipeBean.getReviewType() ? (null != utils.getConfiguration(nowRecipeBean.getClinicOrgan(), "auditFee") ? "1" : "0") : "0";
                map.put("showAuditFee", showAuditFee);
                //data 2019/10/17
                //处方审核方式 0不需要审方 1审方前置 2审方后置
                map.put("reviewType", nowRecipeBean.getReviewType().toString());
            }
            //药店取药 支付方式
            if (new Integer(4).equals(payMode) && drugsEnterpriseBean != null) {
                //@ItemProperty(alias = "0:不支付药品费用，1:全部支付 【 1线上支付  非1就是线下支付】")
                map.put("storePayFlag", drugsEnterpriseBean.getStorePayFlag() == null ? null : drugsEnterpriseBean.getStorePayFlag().toString());
            }
            // 杭州互联网 支付按钮
            Integer payButton = buttonManager.getPayButton(nowRecipeBean.getClinicOrgan(), recipeExtend.getCardType(), "0".equals(recipeExtend.getMedicalType()));
            map.put("payButton", payButton.toString());
        }
        return map;
    }

    @Override
    @RpcService
    public SimpleBusObject getSimpleBusObject(Integer busId) {
        RecipeOrderBean order = recipeOrderService.get(busId);
        SimpleBusObject simpleBusObject = new SimpleBusObject();
        simpleBusObject.setSubBusType("8");
        if (null == order) {
            log.info("recipeOrder表中未查询到记录，尝试从recipe表中查询,busId[{}]", busId);
            RecipeBean recipe = recipeService.getByRecipeId(busId);
            simpleBusObject.setBusId(busId);
            simpleBusObject.setCouponId(recipe.getCouponId());
            simpleBusObject.setMpiId(recipe.getMpiid());
            simpleBusObject.setOrganId(recipe.getClinicOrgan());
            simpleBusObject.setPayFlag(recipe.getPayFlag());
            simpleBusObject.setBusObject(recipe);
            //date 20200402
            //添加字段
            simpleBusObject.setOnlineRecipeId(null != recipe.getClinicId() ? recipe.getClinicId().toString() : null);
            simpleBusObject.setRecipeId(null != busId ? busId.toString() : null);
            simpleBusObject.setHisRecipeId(recipe.getRecipeCode());
            simpleBusObject.setPatId(recipe.getPatientID());
            //date 20210701
            //添加字段
            if (null != recipe.getDepart()) {
                Integer departId = recipe.getDepart();
                simpleBusObject.setDepartId(departId.toString());
                String departName = departmentService.getNameById(departId);
                simpleBusObject.setDepartName(StringUtils.isNotEmpty(departName) ? departName : "");
            }
        } else {
            simpleBusObject.setBusId(busId);
            simpleBusObject.setPrice(order.getTotalFee().stripTrailingZeros().doubleValue());
            simpleBusObject.setActualPrice(BigDecimal.valueOf(order.getActualPrice()).stripTrailingZeros().doubleValue());
            simpleBusObject.setCouponId(order.getCouponId());
            simpleBusObject.setCouponName(order.getCouponName());
            simpleBusObject.setMpiId(order.getMpiId());
            simpleBusObject.setOrganId(order.getOrganId());
            simpleBusObject.setOutTradeNo(order.getOutTradeNo());
            simpleBusObject.setPayFlag(order.getPayFlag());
            simpleBusObject.setBusObject(order);
            //省医保只支付自费金额
            if (order.getOrderType() != null && order.getOrderType() == 1) {
                Double fundAmount = order.getFundAmount() == null ? 0.00 : order.getFundAmount();
                simpleBusObject.setActualPrice(new Double(BigDecimal.valueOf(order.getActualPrice()).subtract(BigDecimal.valueOf(fundAmount)) + ""));

            }
            List<Integer> recipeIdList = JSONUtils.parse(order.getRecipeIdList(), List.class);
            RecipeBean recipeBean = recipeService.getByRecipeId(recipeIdList.get(0));
            //获取就诊卡号--一般来说处方里已经保存了复诊里的就诊卡号了取不到再从复诊里取
            simpleBusObject.setMrn(getMrnForRecipe(recipeBean));
            //由于bug#70621新增卡号卡类型字段
            RecipeExtendBean recipeExtend = recipeService.findRecipeExtendByRecipeId(recipeBean.getRecipeId());
            if (recipeExtend != null) {
                simpleBusObject.setCardId(recipeExtend.getCardNo());
                simpleBusObject.setCardType(recipeExtend.getCardType());
            }
            //杭州互联网流程
            if (order.getRegisterNo() != null) {
                //杭州市互联网医保支付
                HealthCardService healthCardService = BasicAPI.getService(HealthCardService.class);
                //杭州市互联网医院监管中心 管理单元eh3301
                OrganService organService = BasicAPI.getService(OrganService.class);
                OrganDTO organDTO = organService.getByManageUnit("eh3301");
                //获取市民卡
                if (organDTO != null) {
                    String mrn = healthCardService.getMedicareCardId(order.getMpiId(), organDTO.getOrganId());
                    simpleBusObject.setMrn(mrn);
                }
                simpleBusObject.setSettleType("0");
                simpleBusObject.setRegisterNo(order.getRegisterNo());
                simpleBusObject.setHisSettlementNo(order.getHisSettlementNo());            //医保结算信息
                HztServiceInterface HztService = AppContextHolder.getBean("wx.hangztSmkService", HztServiceInterface.class);
                String accessToken = HztService.findSMKTokenForPay(order.getMpiId(), "hzsmk.ios");
                simpleBusObject.setFaceToken(order.getSmkFaceToken());
                simpleBusObject.setAccessToken(accessToken);

            } else {
                simpleBusObject.setSettleType("1");
            }
            //date 20200402
            //添加字段
            if (null != recipeBean) {
                simpleBusObject.setOnlineRecipeId(null != recipeBean.getClinicId() ? recipeBean.getClinicId().toString() : null);
                simpleBusObject.setRecipeId(null != recipeBean.getRecipeId() ? recipeBean.getRecipeId().toString() : null);
                simpleBusObject.setHisRecipeId(recipeBean.getRecipeCode());
                simpleBusObject.setPatId(recipeBean.getPatientID());
                //date 20210701
                //添加字段
                if (null != recipeBean.getDepart()) {
                    Integer departId = recipeBean.getDepart();
                    simpleBusObject.setDepartId(departId.toString());
                    String departName = departmentService.getNameById(departId);
                    simpleBusObject.setDepartName(StringUtils.isNotEmpty(departName) ? departName : "");
                }
            }

        }
        log.info("结算simpleBusObject={}", JSONUtils.toString(simpleBusObject));
        return simpleBusObject;
    }

    /**
     * 获取支付传给卫宁付用的就诊卡号
     * 一般来说处方里已经保存了复诊里的就诊卡号了取不到再从复诊里取
     *
     * @param recipeBean
     * @return //-1表示获取不到身份证，默认用身份证获取患者信息
     */
    private String getMrnForRecipe(RecipeBean recipeBean) {
        RecipeExtendBean recipeExtend = recipeService.findRecipeExtendByRecipeId(recipeBean.getRecipeId());
        String mrn = null;
        if (StringUtils.isNotEmpty(recipeExtend.getCardNo())) {
            mrn = recipeExtend.getCardNo();
        } else {
            //复诊
            if (new Integer(2).equals(recipeBean.getBussSource())) {
                IRevisitExService revisitExService = RevisitAPI.getService(IRevisitExService.class);
                if (recipeBean.getClinicId() != null) {
                    RevisitExDTO revisitExDTO = revisitExService.getByConsultId(recipeBean.getClinicId());
                    if (revisitExDTO != null && revisitExDTO.getCardId() != null) {
                        //就诊卡号
                        mrn = revisitExDTO.getCardId();
                    }
                }
                //咨询
            } else if (new Integer(1).equals(recipeBean.getBussSource())) {
                IConsultExService consultExService = ConsultAPI.getService(IConsultExService.class);
                if (recipeBean.getClinicId() != null) {
                    ConsultExDTO consultExDTO = consultExService.getByConsultId(recipeBean.getClinicId());
                    if (consultExDTO != null && consultExDTO.getCardId() != null) {
                        //就诊卡号
                        mrn = consultExDTO.getCardId();
                    }
                }
            }
        }

        //-1表示获取不到身份证，默认用身份证获取患者信息
        if (StringUtils.isEmpty(mrn)) {
            mrn = "-1";
        }
        return mrn;
    }

    @Override
    @RpcService
    public void onOrder(Order order) {
        log.info("onOrder-入参:{}", JSONObject.toJSONString(order));
        RecipeOrderBean recipeOrder = recipeOrderService.get(order.getBusId());
        Map<String, Object> changeAttr = new HashMap<>();
        changeAttr.put("wxPayWay", order.getPayway());
        changeAttr.put("outTradeNo", order.getOutTradeNo());
        changeAttr.put("payOrganId", order.getPayOrganId());
        //date 2020 0706 更新处方订单支付账号类型
        changeAttr.put("payeeCode", null != order.getOrganType() ? new Integer(order.getOrganType()) : null);
        recipeOrderService.updateOrderInfo(recipeOrder.getOrderCode(), changeAttr);
    }

    @Override
    @RpcService
    public boolean checkCanPay(Integer busId) {
        RecipeOrderBean order = recipeOrderService.get(busId);
        if ((new Integer(1).equals(order.getEffective()) && OrderStatusConstant.READY_PAY.equals(order.getStatus()) && PayConstant.PAY_FLAG_NOT_PAY == order.getPayFlag()) || (new Integer(1).equals(order.getRefundFlag()))) {
            //允许支付
            log.info("允许支付");
        } else {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该订单已处理，请刷新后重试");
        }
        return true;
    }

    @Override
    @RpcService
    public WnExtBusCdrRecipeDTO newWnExtBusCdrRecipe(RecipeOrderBean recipeOrder, Patient patient) {
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        RecipeBean recipeBean = recipeService.getByRecipeId(recipeIdList.get(0));
        log.info("newWnExtBusCdrRecipe.recipeBean={}", JSONObject.toJSONString(recipeBean));
        IRevisitExService revisitExService = RevisitAPI.getService(IRevisitExService.class);
        RevisitExDTO consultExDTO = revisitExService.getByConsultId(recipeBean.getClinicId());
        WnExtBusCdrRecipeDTO wnExtBusCdrRecipe = new WnExtBusCdrRecipeDTO();
        wnExtBusCdrRecipe.setAction("PUTMZSYT");
        wnExtBusCdrRecipe.setHzxm(patient.getPatientName());
        wnExtBusCdrRecipe.setPatid(recipeBean.getPatientID());
        //昆明医科大学第一附属医院要求个性化传patid
        if ("1000352".equals(String.valueOf(recipeBean.getClinicOrgan()))) {
            wnExtBusCdrRecipe.setPatid(recipeBean.getMpiid());
        }
        wnExtBusCdrRecipe.setZje(String.valueOf(recipeOrder.getActualPrice()));
        wnExtBusCdrRecipe.setYbdm("");
        // 科室代码
        AppointDepartService appointDepartService = BasicAPI.getService(AppointDepartService.class);
        AppointDepartDTO appointDepart = appointDepartService.findByOrganIDAndDepartID(recipeBean.getClinicOrgan(), recipeBean.getDepart());
        log.info("查询挂号科室代码入参={},{},结果={}", recipeBean.getClinicOrgan(), recipeBean.getDepart(), JSONObject.toJSONString(appointDepart));
        wnExtBusCdrRecipe.setKsdm(appointDepart != null ? appointDepart.getAppointDepartCode() : "");
        String registerId;
        String cardId = "";
        String insureTypeCode = "";
        String mtTypeCode = "";
        //医保类型名称
        String insureTypeName = "";
        if (consultExDTO != null) {
            registerId = consultExDTO.getRegisterNo();
            cardId = null == consultExDTO.getCardId() ? "" : consultExDTO.getCardId();
            insureTypeCode = null == consultExDTO.getInsureTypeCode() ? "" : consultExDTO.getInsureTypeCode();
            mtTypeCode = null == consultExDTO.getMtTypeCode() ? "" : consultExDTO.getMtTypeCode();
            insureTypeName = null == consultExDTO.getInsureTypeName() ? "" : consultExDTO.getInsureTypeName();

        } else {
            registerId = "";
        }
        String chronicDiseaseFlag = "";
        String chronicDiseaseCode = "";
        String chronicDiseaseName = "";
        String complication = "";
        RecipeExtendBean extend = recipeService.findRecipeExtendByRecipeId(recipeIdList.get(0));
        if (extend != null) {
            if (StringUtils.isEmpty(registerId)) {
                registerId = extend.getRegisterID();
            }
            chronicDiseaseFlag = extend.getChronicDiseaseFlag() == null ? "" : extend.getChronicDiseaseFlag();
            chronicDiseaseCode = extend.getChronicDiseaseCode() == null ? "" : extend.getChronicDiseaseCode();
            chronicDiseaseName = extend.getChronicDiseaseName() == null ? "" : extend.getChronicDiseaseName();
            complication = extend.getComplication() == null ? "" : extend.getComplication();
        }

        wnExtBusCdrRecipe.setGhxh(registerId);
        //合并处方--序号合集处理
        List<String> costNumbers = new ArrayList<>();
        for (Integer recipeId : recipeIdList) {
            RecipeBean recipe = recipeService.getByRecipeId(recipeId);
            RecipeExtendBean recipeExtendBean = recipeService.findRecipeExtendByRecipeId(recipeId);
            String costNumber = StringUtils.isBlank(recipeExtendBean.getRecipeCostNumber()) ? recipe.getRecipeCode() : recipe.getRecipeCostNumber();
            costNumbers.add(costNumber);
        }
        String recipeCode = String.join(",", costNumbers);
        wnExtBusCdrRecipe.setCfxhhj(StringUtils.isBlank(recipeCode) ? "" : recipeCode);
        //个性化处理
        // 40378 【实施】【天津市武清人民医院】【A】-复诊界面增加医保类别及医保卡号选项，唤起收银台时传给互联网进行医保挂号、缴费
        //运营平台新增配置 配置了则上传
        String[] clinicFields = (String[]) utils.getConfiguration(recipeOrder.getOrganId(), "clinicFieldsForRecipeConsult");
        List<String> clinicFieldList = Arrays.asList(clinicFields);
        log.info("the clinicFields=[{}]，organId=[{}]", JSONUtils.toString(clinicFieldList), recipeOrder.getOrganId());
        if (clinicFieldList.size() > 0 && clinicFieldList.contains("1")) {
            StringBuilder builder = new StringBuilder();
            builder.append("<MedCardNo>");
            builder.append(cardId);
            builder.append("</MedCardNo>");
            builder.append("<MedCardPwd>111111</MedCardPwd>");
            builder.append("<InsureTypeCode>");
            builder.append(insureTypeCode);
            builder.append("</InsureTypeCode>");
            builder.append("<MtTypeCode>");
            builder.append(mtTypeCode);
            builder.append("</MtTypeCode>");
            builder.append("<InsureTypeName>");
            builder.append(insureTypeName);
            builder.append("</InsureTypeName>");
            builder.append("<ChronicDiseaseFlag>");
            builder.append(chronicDiseaseFlag);
            builder.append("</ChronicDiseaseFlag>");
            builder.append("<ChronicDiseaseCode>");
            builder.append(chronicDiseaseCode);
            builder.append("</ChronicDiseaseCode>");
            builder.append("<ChronicDiseaseName>");
            builder.append(chronicDiseaseName);
            builder.append("</ChronicDiseaseName>");
            builder.append("<Complication>");
            builder.append(complication);
            builder.append("</Complication>");
            wnExtBusCdrRecipe.setYbrc(ctd.util.Base64.encodeToString(builder.toString().getBytes(), 2));
            log.info("newWnExtBusCdrRecipe builder={}", builder.toString());
        } else {
            //医保入参--处方人脸识别token-base64加密
            if (extend != null && StringUtils.isNotEmpty(extend.getMedicalSettleData())) {
                wnExtBusCdrRecipe.setYbrc(Base64.encodeToString(extend.getMedicalSettleData().getBytes(), 1));
                log.info("newWnExtBusCdrRecipe medicalSettleData={}", extend.getMedicalSettleData());
            }
            //获取封装郑州医保签发号，异常不能影响正常调用流程
            try {
                zhengzhouMedicalSet(recipeOrder, patient, wnExtBusCdrRecipe);
            } catch (Exception e) {
                log.info("newWnExtBusCdrRecipe 获取封装郑州医保签发号异常)");
            }

        }
        //获取医保支付开关机构配置
        IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        //获取医保支付流程配置（1-无 2-省医保/杭州市医保 3-长三角）
        Integer provincialMedicalPayFlag = (Integer) configService.getConfiguration(recipeBean.getClinicOrgan(), "provincialMedicalPayFlag");
        //医生选择了自费时，强制设置为自费 或者医保支付开关打开并且不是医保支付的时候强制自费或者患者自己选择自费
        if (extend != null && extend.getMedicalType() != null && "0".equals(extend.getMedicalType()) || (new Integer(0).equals(recipeOrder.getOrderType()) && new Integer(2).equals(provincialMedicalPayFlag))) {
            wnExtBusCdrRecipe.setIszfjs("1");
        }
        //配送信息
        Map<String, String> map = Maps.newHashMap();
        //增加新的处方支付类型,该种类型为直接调用卫宁收银台时传给收银台的PSXX传空
        if (new Integer(1).equals(recipeBean.getRecipePayType())) {
            wnExtBusCdrRecipe.setPsxx(map);
        } else {
            //配送类型0院内现场取药不配送 1 医院药房负责配送 2第三方平台配送
            String pslx;
            IDrugsEnterpriseService drugsEnterpriseService = RecipeAPI.getService(IDrugsEnterpriseService.class);
            switch (recipeBean.getPayMode()) {
                case 1:
                    //默认医院配送
                    pslx = "1";
                    Integer depId = recipeOrder.getEnterpriseId();
                    if (depId != null) {
                        DrugsEnterpriseBean enterpriseBean = drugsEnterpriseService.get(depId);
                        if (enterpriseBean != null && enterpriseBean.getSendType() == 2) {
                            //药企配送
                            pslx = "2";
                        }
                    }
                    break;
                case 3:
                    pslx = "0";
                    break;
                case 4:
                    //药店取药
                    pslx = "3";
                    break;
                default:
                    pslx = "";
                    break;
            }
            map.put("pslx", pslx);
            //患者唯一号
            map.put("patid", recipeBean.getPatientID() == null ? "" : recipeBean.getPatientID());
            //挂号序号
            map.put("ghxh", registerId == null ? "" : registerId);
            //配送地址
            map.put("psdz", recipeService.getRecipeOrderCompleteAddress(recipeOrder));
            //电话
            map.put("lxdh", recipeOrder.getRecMobile() == null ? "" : recipeOrder.getRecMobile());
            //配送日期，为空默认当前日期
            map.put("psrq", recipeOrder.getExpectSendDate() == null ? "" : recipeOrder.getExpectSendDate());
            //配送处方序号合集
            map.put("cfxhhj", StringUtils.isBlank(recipeCode) ? "" : recipeCode);
            //配送备注说明
            map.put("pssm", "");
            //收件人姓名
            map.put("sjrxm", recipeOrder.getReceiver() == null ? recipeBean.getPatientName() : recipeOrder.getReceiver());
            //配送费，如果是12元，就传12.00
            if (recipeOrder.getExpressFeePayWay() == null || recipeOrder.getExpressFeePayWay() == 1) {
                if (recipeOrder.getExpressFee() != null && recipeOrder.getExpressFee().compareTo(BigDecimal.ZERO) == 1) {
                    DecimalFormat df1 = new DecimalFormat("0.00");
                    map.put("psf", df1.format(recipeOrder.getExpressFee()));
                }
            }
            wnExtBusCdrRecipe.setPsxx(map);
        }
        log.info("newWnExtBusCdrRecipe wnExtBusCdrRecipe={}", JSONUtils.toString(wnExtBusCdrRecipe));
        return wnExtBusCdrRecipe;
    }

    private void zhengzhouMedicalSet(RecipeOrderBean recipeOrder, Patient patient, WnExtBusCdrRecipeDTO wnExtBusCdrRecipe) {
        IAuthExtraService authExtraService = AppContextHolder.getBean("mi.authExtraService", IAuthExtraService.class);
        SignNoReq req = new SignNoReq();
        req.setMpiId(patient.getMpiId());
        req.setBusType("recipe");
        req.setBusId(recipeOrder.getOrderId());
        Map<String, Object> extraData = new HashMap<String, Object>();
        extraData.put("moduleUrl", "eh.wx.health.patientRecipe.OrderDetail");
        extraData.put("cid", recipeOrder.getOrderId() + "");
        req.setExtraData(extraData);
        SignNoRes res = authExtraService.getSignNoAndSaveData(req);
        if (res != null && res.getYbrc() != null) {
            String ybrc = JSONUtils.toString(res);
            log.info("zhengzhouMedicalSet ybrc={}", ybrc);
//                wnExtBusCdrRecipe.setYbrc(Base64.encodeToString(ybrc.getBytes("UTF-8"), 1));
            wnExtBusCdrRecipe.setYbrc(res.getYbrc());

        }
    }
}
