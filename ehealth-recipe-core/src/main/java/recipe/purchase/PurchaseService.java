package recipe.purchase;

import com.google.common.collect.Lists;
import com.ngari.base.BaseAPI;
import com.ngari.base.hisconfig.service.IHisConfigService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.bus.hosrelation.model.HosrelationBean;
import com.ngari.bus.hosrelation.service.IHosrelationService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.patient.mode.PatientQueryRequestTO;
import com.ngari.his.patient.service.IPatientHisService;
import com.ngari.his.recipe.mode.MedicInsurSettleApplyReqTO;
import com.ngari.his.recipe.mode.MedicInsurSettleApplyResTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DepListBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.constant.RecipeDistributionFlagEnum;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.SkipThirdReqVO;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import recipe.ApplicationUtils;
import recipe.bean.PltPurchaseResponse;
import recipe.constant.*;
import recipe.dao.*;
import recipe.enumerate.status.OfflineToOnlineEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.GiveModeTextEnum;
import recipe.manager.EmrRecipeManager;
import recipe.manager.EnterpriseManager;
import recipe.manager.HisRecipeManager;
import recipe.service.*;
import recipe.util.MapValueUtil;
import recipe.util.RedisClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/18
 * @description： 购药入口类
 * @version： 1.0
 */
@RpcBean(value = "purchaseService")
public class PurchaseService {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PurchaseService.class);

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    @Autowired
    private RecipeOrderDAO recipeOrderDAO;

    @Autowired
    private HisRecipeDAO hisRecipeDAO;

    @Autowired
    private RecipeOrderService recipeOrderService;
    @Autowired
    private EnterpriseManager enterpriseManager;
    @Autowired
    HisRecipeManager hisRecipeManager;

    @Autowired
    @Qualifier("basic.patientService")
    PatientService patientService;


    @Autowired
    HisRecipeDetailDAO hisRecipeDetailDAO;

    /**
     * 获取可用购药方式------------已废弃---已改造成从处方单详情里获取
     *
     * @param recipeId 处方单ID
     * @param mpiId    患者mpiId
     * @return 响应
     */
    @RpcService
    public PltPurchaseResponse showPurchaseMode(Integer recipeId, String mpiId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeListService recipeListService = ApplicationUtils.getRecipeService(RecipeListService.class);
        PltPurchaseResponse result = new PltPurchaseResponse();
        if (StringUtils.isNotEmpty(mpiId)) {
            Map<String, Object> map = recipeListService.getLastestPendingRecipe(mpiId);
            List<Map> recipes = (List<Map>) map.get("recipes");
            if (CollectionUtils.isNotEmpty(recipes)) {
                RecipeBean recipeBean = (RecipeBean) recipes.get(0).get("recipe");
                recipeId = recipeBean.getRecipeId();
            }
        }
        Recipe dbRecipe = recipeDAO.get(recipeId);
        if (null == dbRecipe) {
            return result;
        }
        //TODO 配送到家和药店取药默认可用
        result.setSendToHome(true);
        result.setTfds(true);
        //到院取药判断
        try {
            IHisConfigService iHisConfigService = ApplicationUtils.getBaseService(IHisConfigService.class);
            OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
            boolean hisStatus = iHisConfigService.isHisEnable(dbRecipe.getClinicOrgan());
            //机构设置，是否可以到院取药
            //date 20191022,修改到院取药配置项
            boolean flag = RecipeServiceSub.getDrugToHos(recipeId, dbRecipe.getClinicOrgan());
            if (RecipeDistributionFlagEnum.DEFAULT.getType().equals(dbRecipe.getDistributionFlag())
                    && hisStatus && flag) {
                result.setToHos(true);
            }
        } catch (Exception e) {
            LOG.warn("showPurchaseMode 到院取药判断 exception. recipeId={}", recipeId, e);
        }
        return result;
    }

    /**
     * 根据对应的购药方式展示对应药企
     *
     * @param recipeIds 处方ID
     * @param payModes  购药方式
     */
    @RpcService
    public RecipeResultBean filterSupportDepList(List<Integer> recipeIds, List<Integer> payModes, Map<String, String> extInfo) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        if (CollectionUtils.isEmpty(recipeIds)) {
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("处方ids不存在");
            return resultBean;
        }
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        //为了计算合并处方药品费
        extInfo.put("recipeIds", String.join(",", recipeIds.stream().map(String::valueOf).collect(Collectors.toList())));
        List<DepDetailBean> depListBeanList = Lists.newArrayList();
        DepListBean depListBean = new DepListBean();
        for (Recipe dbRecipe : recipeList) {
            if (null == dbRecipe) {
                resultBean.setCode(RecipeResultBean.FAIL);
                resultBean.setMsg("处方不存在");
                return resultBean;
            }

            if (CollectionUtils.isEmpty(payModes)) {
                resultBean.setCode(RecipeResultBean.FAIL);
                resultBean.setMsg("参数错误");
                return resultBean;
            }
            //处方单状态不是待处理 or 处方单已被处理
            boolean dealFlag = checkRecipeIsUser(dbRecipe, resultBean);
            if (dealFlag) {
                return resultBean;
            }

            try {
                /*for (Integer i : payModes) {
                    IPurchaseService purchaseService = getService(i);
                    //如果涉及到多种购药方式合并成一个列表，此处需要进行合并
                    resultBean = purchaseService.findSupportDepList(dbRecipe, extInfo);
                }*/
                // 根据paymode 替换givemode
                Integer giveMode = PayModeGiveModeUtil.getGiveMode(payModes.get(0));

                IPurchaseService purchaseService = getService(giveMode);
                resultBean = purchaseService.findSupportDepList(dbRecipe, extInfo);
                //有一个不成功就返回
                if (!RecipeResultBean.SUCCESS.equals(resultBean.getCode())) {
                    return resultBean;
                }
                depListBean = (DepListBean) resultBean.getObject();
                if (depListBean != null) {
                    //多个合并处方支持的药企列表取交集
                    //第二个之后的如果没有的就没有
                    if (CollectionUtils.isEmpty(depListBean.getList())) {
                        return resultBean;
                    } else {
                        //当取第一个药企列表时先放入list再与后面的取交集
                        if (CollectionUtils.isEmpty(depListBeanList)) {
                            depListBeanList.addAll(depListBean.getList());
                        } else {
                            //交集需要处理
                            depListBeanList.retainAll(depListBean.getList());
                            //his管理的药企费用这里处理
                            //如果存在交集则取一次交集加一次费用
                            if (CollectionUtils.isNotEmpty(depListBeanList) && StringUtils.isNotEmpty(depListBeanList.get(0).getHisDepCode())) {
                                Map<String, BigDecimal> stringObjectMap = depListBean.getList().stream().collect(Collectors.toMap(DepDetailBean::getHisDepCode, DepDetailBean::getHisDepFee));
                                for (DepDetailBean depDetailBean : depListBeanList) {
                                    if (depDetailBean.getHisDepFee() != null && StringUtils.isNotEmpty(depDetailBean.getHisDepCode()) && stringObjectMap.get(depDetailBean.getHisDepCode()) != null) {
                                        depDetailBean.setHisDepFee(depDetailBean.getHisDepFee().add(stringObjectMap.get(depDetailBean.getHisDepCode())));
                                    }
                                }
                            }
                            //有可能前两个没取到交集直接结束
                            if (CollectionUtils.isEmpty(depListBeanList)) {
                                break;
                            }
                        }
                    }
                }

            } catch (Exception e) {
                LOG.error("filterSupportDepList error", e);
                throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
            }

        }
        //重新组装
        if (depListBean != null) {
            if (CollectionUtils.isNotEmpty(depListBeanList) && depListBeanList.size() == 1) {
                depListBean.setSigle(true);
            } else {
                depListBean.setSigle(false);
            }
            depListBean.setList(depListBeanList);
            resultBean.setObject(depListBean);
            LOG.info("filterSupportDepList recipeIds={} resultBean={}", recipeIds, JSONUtils.toString(resultBean));
        }
        //患者选择购药方式后,将处方推送到前置机
        if (CollectionUtils.isNotEmpty(recipeList)) {
            SkipThirdReqVO skipThirdReqVO = new SkipThirdReqVO();
            try {
                skipThirdReqVO.setOrganId(recipeList.get(0).getClinicOrgan());
                skipThirdReqVO.setRecipeIds(recipeIds);
                Integer giveMode = PayModeGiveModeUtil.getGiveMode(payModes.get(0));
                skipThirdReqVO.setGiveMode(GiveModeTextEnum.getGiveModeText(giveMode));
            } catch (Exception e) {
                LOG.error("filterSupportDepList error msg ", e);
            }
            enterpriseManager.uploadRecipeInfoToThird(skipThirdReqVO.getOrganId(), skipThirdReqVO.getGiveMode(), skipThirdReqVO.getRecipeIds());
        }
        return resultBean;
    }

    /**
     * 重新包装一个方法供前端调用----由于原order接口与统一支付接口order方法名相同
     *
     * @param recipeId
     * @param extInfo
     * @return
     */
    @RpcService
    public OrderCreateResult orderForRecipe(Integer recipeId, Map<String, String> extInfo) {
        OrderCreateResult orderCreateResult = checkOrderInfo(Arrays.asList(recipeId), extInfo);
        if (RecipeResultBean.CHECKFAIL == orderCreateResult.getCode()) {
            return orderCreateResult;
        }
        return order(Arrays.asList(recipeId), extInfo);
    }

    /**
     * 为了兼容老接口重新写了一个orderForRecipe
     *
     * @param recipeIds
     * @param extInfo
     * @return
     */
    @RpcService
    public OrderCreateResult orderForRecipeNew(List<Integer> recipeIds, Map<String, String> extInfo) {
        OrderCreateResult orderCreateResult = checkOrderInfo(recipeIds, extInfo);
        if (RecipeResultBean.CHECKFAIL == orderCreateResult.getCode()) {
            return orderCreateResult;
        }
        return order(recipeIds, extInfo);
    }

    /**
     * 确认订单校验
     *
     * @param recipeIds
     * @param extInfo
     * @return
     */
    private OrderCreateResult checkOrderInfo(List<Integer> recipeIds, Map<String, String> extInfo) {
        //在确认订单页，用户点击提交订单，需要再次判断该处方单状态，若更新了诊断或药品信息或者删除了处方或者处方已经支付，则提示患者该处方已做变更，需要重新进入处理。
        OrderCreateResult result = new OrderCreateResult(RecipeResultBean.SUCCESS);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        //合并处方处理
        Recipe dbRecipe;
        RecipeExtend recipeExtend;
        HisRecipe hisRecipe;
        for (Integer recipeId : recipeIds) {
            dbRecipe = recipeDAO.get(recipeId);
            hisRecipe = hisRecipeDAO.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(dbRecipe.getMpiid(), dbRecipe.getClinicOrgan(), dbRecipe.getRecipeCode());
            recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
            //判断是否删除处方
            if (null == dbRecipe) {
                result.setCode(RecipeResultBean.CHECKFAIL);
                result.setMsg("该处方单信息已变更，请退出重新获取处方信息。");
                LOG.info("checkOrderInfo recipeId:{} 处方不存在", recipeId);
                return result;
            }
            if (!new Integer(2).equals(dbRecipe.getRecipeSourceType())) {
                continue;
            }
            if (null == hisRecipe) {
                result.setCode(RecipeResultBean.CHECKFAIL);
                result.setMsg("该处方单信息已变更，请退出重新获取处方信息。");
                LOG.info("checkOrderInfo recipeId:{} hisRecipe已被删除", recipeId);
                return result;
            }
            //判断是订单是否已支付
            if (StringUtils.isNotEmpty(dbRecipe.getOrderCode())) {
                RecipeOrder order = recipeOrderDAO.getByOrderCode(dbRecipe.getOrderCode());
                if (new Integer(1).equals(order.getPayFlag())) {
                    result.setCode(RecipeResultBean.CHECKFAIL);
                    result.setMsg("该处方单信息已变更，请退出重新获取处方信息。");
                    LOG.info("checkOrderInfo recipeId:{} 您的订单已支付", recipeId);
                }
            }
            //判断诊断和药品信息是否已更改
            PatientDTO patientDTO = patientService.getPatientBeanByMpiId(dbRecipe.getMpiid());
            if (null == patientDTO) {
                throw new DAOException(609, "患者信息不存在");
            }
            HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos = hisRecipeManager.queryData(dbRecipe.getClinicOrgan(), patientDTO, null, OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getType(), dbRecipe.getRecipeCode());
            if (null == hisRecipeInfos || CollectionUtils.isEmpty(hisRecipeInfos.getData())) {
                result.setCode(RecipeResultBean.CHECKFAIL);
                result.setMsg("该处方单信息已变更，请退出重新获取处方信息。");
                LOG.info("checkOrderInfo recipeId:{} hisRecipeInfos.getData() is null", recipeId);
            }
            if (!CollectionUtils.isNotEmpty(hisRecipeInfos.getData())) {
                result.setCode(RecipeResultBean.CHECKFAIL);
                result.setMsg("该处方单信息已变更，请退出重新获取处方信息。");
                LOG.info("checkOrderInfo recipeId:{} hisRecipeInfos.getData() is null", recipeId);
                return result;
            }
            QueryHisRecipResTO queryHisRecipResTO = hisRecipeInfos.getData().get(0);
            if (queryHisRecipResTO == null) {
                result.setCode(RecipeResultBean.CHECKFAIL);
                result.setMsg("该处方单信息已变更，请退出重新获取处方信息。");
                LOG.info("checkOrderInfo recipeId:{} queryHisRecipResTO is null", recipeId);
            }
            //诊断
            if (!covertData(queryHisRecipResTO.getDisease()).equals(covertData(hisRecipe.getDisease())) || !covertData(queryHisRecipResTO.getDiseaseName()).equals(covertData(hisRecipe.getDiseaseName()))) {
                result.setCode(RecipeResultBean.CHECKFAIL);
                result.setMsg("该处方单信息已变更，请退出重新获取处方信息。");
                LOG.info("checkOrderInfo recipeId:{} 诊断信息不一致");
            }
            //药品详情变更或数据是否由他人生成
            List<Integer> hisRecipeIds = new ArrayList<>();
            Map<String, HisRecipe> hisRecipeMap = new HashMap<>();
            hisRecipeIds.add(hisRecipe.getHisRecipeID());
            List<HisRecipeDetail> hisRecipeDetailList = hisRecipeDetailDAO.findByHisRecipeIds(hisRecipeIds);
            hisRecipeMap.put(hisRecipe.getRecipeCode(), hisRecipe);
            Set<String> deleteSetRecipeCode = hisRecipeManager.obtainDeleteRecipeCodes(hisRecipeInfos.getData(), hisRecipeMap, hisRecipeDetailList, dbRecipe.getMpiid());
            if (!CollectionUtils.isEmpty(deleteSetRecipeCode)) {
                result.setCode(RecipeResultBean.CHECKFAIL);
                result.setMsg("该处方单信息已变更，请退出重新获取处方信息。");
                LOG.info("checkOrderInfo recipeId:{} 药品详情已变更或数据已经由他人生成", recipeId);
            }
            EmrRecipeManager.getMedicalInfo(dbRecipe, recipeExtend);
        }
        return result;
    }

    private String covertData(String str) {
        if (StringUtils.isEmpty(str)) {
            return "";
        } else {
            return str;
        }
    }

    /**
     * @param recipeIds
     * @param extInfo   参照RecipeOrderService createOrder定义
     *                  {"operMpiId":"当前操作者编码","addressId":"当前选中地址","payway":"支付方式（payway）","payMode":"处方支付方式",
     *                  "decoctionFlag":"1(1：代煎，0：不代煎)", "gfFeeFlag":"1(1：表示需要制作费，0：不需要)", “depId”:"指定药企ID",
     *                  "expressFee":"快递费","gysCode":"药店编码","sendMethod":"送货方式","payMethod":"支付方式","appId":"公众号ID",
     *                  "calculateFee":"1(1:需要，0:不需要),"logisticsCompany":"物流公司"}
     *                  <p>
     *                  ps: decoctionFlag是中药处方时设置为1，gfFeeFlag是膏方时设置为1
     *                  gysCode, sendMethod, payMethod 字段为钥世圈字段，会在findSupportDepList接口中给出
     *                  payMode 如果钥世圈有供应商是多种方式支持，就传0
     *                  orderType, 1表示省医保
     * @return
     */
    @RpcService
    public OrderCreateResult order(List<Integer> recipeIds, Map<String, String> extInfo) {
        LOG.info("order param: recipeId={},extInfo={}", JSONUtils.toString(recipeIds), JSONUtils.toString(extInfo));
        OrderCreateResult result = new OrderCreateResult(RecipeResultBean.SUCCESS);

        String recipeIdStr = StringUtils.join(recipeIds, "_");
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe dbRecipe = recipeDAO.get(recipeIds.get(0));
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        if (null == dbRecipe) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("处方不存在");
            return result;
        }
        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        if (null == payMode) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("缺少购药方式");
            return result;
        }
        //处方单状态不是待处理 or 处方单已被处理
        boolean dealFlag = checkRecipeIsDeal(recipeList, result, extInfo);
        if (dealFlag) {
            return result;
        }

        //判断是否存在订单
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        if (StringUtils.isNotEmpty(dbRecipe.getOrderCode())) {
            RecipeOrder order = orderDAO.getByOrderCode(dbRecipe.getOrderCode());
            if (1 == order.getEffective()) {
                result.setOrderCode(order.getOrderCode());
                result.setBusId(order.getOrderId());
                result.setObject(ObjectCopyUtils.convert(order, RecipeOrderBean.class));
                result.setCode(RecipeResultBean.FAIL);
                result.setMsg("您有正在进行中的订单");
                unLock(recipeIdStr);
                return result;
            }
        }

        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        IHisConfigService iHisConfigService = ApplicationUtils.getBaseService(IHisConfigService.class);

        boolean hisStatus = iHisConfigService.isHisEnable(dbRecipe.getClinicOrgan());
        for (Integer recipeId : recipeIds) {
            try {
                //判断院内是否已取药，防止重复购买
                //date 20191022到院取药取配置项
                boolean flag = RecipeServiceSub.getDrugToHos(recipeId, dbRecipe.getClinicOrgan());
                //是否支持医院取药 true：支持
                //该医院不对接HIS的话，则不需要进行该校验
                if (flag && hisStatus) {
                    String backInfo = recipeService.searchRecipeStatusFromHis(recipeId, 1);
                    if (StringUtils.isNotEmpty(backInfo)) {
                        result.setCode(RecipeResultBean.FAIL);
                        result.setMsg(backInfo);
                        return result;
                    }
                }
            } catch (Exception e) {
                LOG.warn("order searchRecipeStatusFromHis exception. recipeId={}", recipeId, e);
            }
        }

        //判断是否存在分布式锁
        boolean unlock = lock(recipeIdStr);
        if (!unlock) {
            //存在锁则需要返回
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("您有正在进行中的订单");
            return result;
        } else {
            //设置默认超时时间 30s
            redisClient.setex(CacheConstant.KEY_RCP_BUSS_PURCHASE_LOCK + recipeIdStr, 30L);
        }

        try {
            // 根据paymode 换算givemode

            Integer giveMode = PayModeGiveModeUtil.getGiveMode(payMode);
            IPurchaseService purchaseService = getService(giveMode);
            result = purchaseService.order(recipeList, extInfo);
        } catch (Exception e) {
            LOG.error("order error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        } finally {
            //订单创建完解锁
            unLock(recipeIdStr);
        }

        return result;
    }

    public void updateRecipeDetail(Integer recipeId) {
        try {
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);
            SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            if (recipe.getEnterpriseId() != null) {
                DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipe.getEnterpriseId());
                //结算方式 0:药店价格 1:医院价格
                int settlementMode = 0;
                if (drugsEnterprise != null && drugsEnterprise.getSettlementMode() != null && drugsEnterprise.getSettlementMode() == 1) {
                    settlementMode = 1;
                }
                for (Recipedetail recipedetail : recipedetails) {
                    SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), drugsEnterprise.getId());
                    LOG.info("PayModeOnline.updateRecipeDetail recipeId:{},saleDrugList:{}.", recipeId, JSONUtils.toString(saleDrugList));

                    //记录药企购药时用的医院目录的价格还是用的药企目录的价格
                    recipedetail.setSettlementMode(settlementMode);

                    if (saleDrugList != null) {
                        if (settlementMode == 0) {
                            //可能取的是药企配送目录里的价格，得重新设置药品总价
                            BigDecimal price = saleDrugList.getPrice();
                            recipedetail.setActualSalePrice(price);
                            //线下转线上不处理药品总价
                            if (!RecipeBussConstant.OFFLINE_TO_ONLINE.equals(recipe.getRecipeSourceType())) {
                                recipedetail.setDrugCost(price.multiply(new BigDecimal(recipedetail.getUseTotalDose())).divide(BigDecimal.ONE, 3, RoundingMode.UP));
                            }
                        } else if (settlementMode == 1) {
                            recipedetail.setActualSalePrice(recipedetail.getSalePrice());
                        }


                        if (StringUtils.isEmpty(saleDrugList.getOrganDrugCode())) {
                            recipedetail.setSaleDrugCode(saleDrugList.getDrugId() + "");
                        } else {
                            recipedetail.setSaleDrugCode(saleDrugList.getOrganDrugCode());
                        }
                    }
                    recipeDetailDAO.update(recipedetail);
                }
            }
        } catch (Exception e) {
            LOG.error("PayModeOnline.updateRecipeDetail error recipeId:{}.", recipeId, e);
        }
    }

    public boolean getPayOnlineConfig(Integer clinicOrgan) {
        Integer payModeOnlinePayConfig;
        try {
            IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            payModeOnlinePayConfig = (Integer) configurationService.getConfiguration(clinicOrgan, "payModeOnlinePayConfig");
        } catch (Exception e) {
            LOG.error("获取运营平台处方支付配置异常", e);
            return false;
        }
        //1平台付 2卫宁付
        if (new Integer(2).equals(payModeOnlinePayConfig)) {
            return true;
        }
        return false;
    }

    public boolean getToHosPayConfig(Integer clinicOrgan) {
        Integer payModeToHosOnlinePayConfig;
        try {
            IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            payModeToHosOnlinePayConfig = (Integer) configurationService.getConfiguration(clinicOrgan, "payModeToHosOnlinePayConfig");
        } catch (Exception e) {
            LOG.error("获取运营平台处方支付配置异常", e);
            return false;
        }
        //1平台付 2卫宁付
        if (new Integer(2).equals(payModeToHosOnlinePayConfig)) {
            return true;
        }
        return false;
    }

    public IPurchaseService getService(Integer payMode) {
        PurchaseEnum[] list = PurchaseEnum.values();
        String serviceName = null;
        for (PurchaseEnum e : list) {
            if (e.getPayMode().equals(payMode)) {
                serviceName = e.getServiceName();
                break;
            }
        }

        IPurchaseService purchaseService = null;
        if (StringUtils.isNotEmpty(serviceName)) {
            purchaseService = AppContextHolder.getBean(serviceName, IPurchaseService.class);
        }

        return purchaseService;
    }

    /**
     * 检查处方是否已被处理
     *
     * @param recipes 处方
     * @param result  结果
     * @return true 已被处理
     */
    private boolean checkRecipeIsDeal(List<Recipe> recipes, RecipeResultBean result, Map<String, String> extInfo) {
        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        for (Recipe dbRecipe : recipes) {
            if (dbRecipe.getStatus() == RecipeStatusConstant.REVOKE) {
                throw new DAOException(eh.base.constant.ErrorCode.SERVICE_ERROR, "处方单已被撤销");
            }
            //此时如果处方状态为待审核则说明药师端已经撤销了处方审核结果
            if (dbRecipe.getStatus() == RecipeStatusConstant.READY_CHECK_YS) {
                throw new DAOException(eh.base.constant.ErrorCode.SERVICE_ERROR, "处方审核结果已被撤销");
            }
            if (RecipeStatusConstant.CHECK_PASS != dbRecipe.getStatus() || 1 == dbRecipe.getChooseFlag()) {
                result.setCode(RecipeResultBean.FAIL);
                result.setMsg("处方单已被处理");
                //判断是否已到院取药，查看 HisCallBackService *RecipesFromHis 方法处理
                if (Integer.valueOf(1).equals(dbRecipe.getPayFlag())) {
                    if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(dbRecipe.getGiveMode()) && RecipeBussConstant.PAYMODE_TFDS == payMode) {
                        result.setCode(2);
                        result.setMsg("您已到院自取药品，无法提交药店取药");
                    } else if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(dbRecipe.getGiveMode()) && RecipeBussConstant.PAYMODE_ONLINE == payMode) {
                        result.setCode(3);
                        result.setMsg("您已到院自取药品，无法进行配送");
                    } else if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(dbRecipe.getGiveMode())) {
                        result.setCode(4);
                        result.setMsg(dbRecipe.getOrderCode());
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 获取处方详情单文案
     *
     * @param recipe 处方
     * @param order  订单
     * @return 文案
     */
    public String getTipsByStatusForPatient(Recipe recipe, RecipeOrder order) {
        RecipeRefundDAO recipeRefundDAO = DAOFactory.getDAO(RecipeRefundDAO.class);
        Integer status = recipe.getStatus();

        Integer payFlag = recipe.getPayFlag();
        String orderCode = recipe.getOrderCode();
        if (order == null) {
            RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
            order = recipeOrderDAO.getByOrderCode(orderCode);
        }

        String tips;
        switch (RecipeStatusEnum.getRecipeStatusEnum(status)) {
            case RECIPE_STATUS_READY_CHECK_YS:
                tips = "请耐心等待药师审核";
                if (ReviewTypeConstant.Preposition_Check.equals(recipe.getReviewType())) {
                    String reason = RecipeServiceSub.getCancelReasonForChecker(recipe.getRecipeId());
                    if (StringUtils.isNotEmpty(reason)) {
                        tips = reason;
                    }
                }
                break;
            case RECIPE_STATUS_CHECK_PASS:
                String invalidTime = getInvalidTime(recipe);
                if (StringUtils.isNotEmpty(orderCode) && payFlag == 0 && order.getActualPrice() > 0) {
                    tips = "订单待支付，请于" + invalidTime + "内完成购药，否则处方将失效";
                } else if (StringUtils.isEmpty(orderCode)) {
                    tips = "处方单待处理，请于" + invalidTime + "内完成购药，否则处方将失效";
                } else {
                    Integer giveMode = recipe.getGiveMode();
                    if (giveMode.equals(5)) {
                        giveMode = PayModeGiveModeUtil.getGiveMode(giveMode);
                    }
                    IPurchaseService purchaseService = getService(giveMode);
                    tips = purchaseService.getTipsByStatusForPatient(recipe, order);
                }
                break;
            case RECIPE_STATUS_NO_PAY:
                tips = "处方单未支付，已失效";
                break;
            case RECIPE_STATUS_NO_OPERATOR:
                tips = "处方单未处理，已失效";
                break;
            case RECIPE_STATUS_CHECK_NOT_PASS_YS:
                if (RecipecCheckStatusConstant.Check_Normal.equals(recipe.getCheckStatus())) {
                    tips = "处方审核不通过，请联系开方医生";
                    break;
                } else {
                    tips = "请耐心等待药师审核";
                    break;
                }
            case RECIPE_STATUS_REVOKE:
                if (CollectionUtils.isNotEmpty(recipeRefundDAO.findRefundListByRecipeIdAndNode(recipe.getRecipeId()))) {
                    tips = "由于患者申请退费成功，该处方已取消。";
                } else {
                    tips = "由于医生已撤销，该处方单已失效，请联系医生";
                    //20200519 zhangx 是否展示退款按钮(重庆大学城退款流程)，前端调用patientRefundForRecipe
                    //原设计：处方单待处理状态，患者未下单时可撤销，重庆大学城流程，支付完未配送可撤销，
                    if (null != order) {
                        tips = "该处方单已失效";
                    }
                }
                break;
            case RECIPE_STATUS_RECIPE_DOWNLOADED:
                tips = "已下载处方笺";
                break;
            case RECIPE_STATUS_USING:
                tips = "处理中";
                break;
            case RECIPE_STATUS_DELETE:
                tips = "处方单已删除";
                break;
            case RECIPE_STATUS_HIS_FAIL:
                tips = "处方单同步his写入失败";
                break;
            case REVIEW_DRUG_FAIL:
                tips = "已取消";
                break;
            case RECIPE_STATUS_DONE_DISPENSING:
                tips = "药品已发药";
                break;
            case RECIPE_STATUS_DECLINE:
                tips = "药品已拒发";
                break;
            case RECIPE_STATUS_DRUG_WITHDRAWAL:
                tips = "药品已退药";
                break;
            case RECIPE_STATUS_SIGN_ING_CODE_PHA:
            case RECIPE_STATUS_SIGN_NO_CODE_PHA:
            case RECIPE_STATUS_SIGN_ERROR_CODE_PHA:
                tips = "请耐心等待药师审核";
                break;
            case RECIPE_STATUS_FINISH:
                //特应性处理:下载处方，不需要审核,不更新payMode
                if (ReviewTypeConstant.Not_Need_Check.equals(recipe.getReviewType()) && RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(recipe.getGiveMode())) {
                    tips = "订单完成";
                    break;
                }
            default:
                IPurchaseService purchaseService = getService(recipe.getGiveMode());
                if (null == purchaseService) {
                    tips = "";
                } else {
                    tips = purchaseService.getTipsByStatusForPatient(recipe, order);
                }
        }
        return tips;
    }

    private String getInvalidTime(Recipe recipe) {
        String invalidTime = "3日";
        try {
            if (null != recipe.getInvalidTime()) {
                Date now = new Date();
                long nd = 1000 * 24 * 60 * 60;
                long nh = 1000 * 60 * 60;
                long nm = 1000 * 60;
                long ns = 1000;
                long diff = recipe.getInvalidTime().getTime() - now.getTime();
                // 处方已到失效时间，失效定时任务未执行（每30分钟执行一次）
                if (diff <= 0) {
                    invalidTime = "30分钟";
                } else {
                    long day = diff / nd;
                    long hour = diff % nd / nh;
                    long min = diff % nd % nh / nm;
                    long sec = diff % nd % nh % nm / ns;
                    if (day <= 0 && hour <= 0 && min <= 0 && sec > 0) {
                        invalidTime = "1分钟";
                    } else {
                        hour = hour + (day * 24);
                        invalidTime = hour > 0 ? (hour + "小时") : "";
                        invalidTime = min > 0 ? (invalidTime + min + "分钟") : (invalidTime + "");
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("失效时间倒计时计算异常，recipeid={}", recipe.getRecipeId(), e);
        }
        return invalidTime;
    }

    /**
     * 获取订单的状态
     *
     * @param recipe 处方详情
     * @return 订单状态
     */
    public Integer getOrderStatus(Recipe recipe) {
        if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode())) {
            return OrderStatusConstant.READY_SEND;
        } else {
            Integer giveMode = recipe.getGiveMode();
            if (recipe.getGiveMode().equals(5)) {
                giveMode = PayModeGiveModeUtil.getGiveMode(recipe.getGiveMode());
            }
            IPurchaseService purchaseService = getService(giveMode);
            return purchaseService.getOrderStatus(recipe);
        }
    }

    /**
     * 检查处方是否已被处理
     *
     * @param dbRecipe 处方
     * @param result   结果
     * @return true 已被处理
     */
    private boolean checkRecipeIsUser(Recipe dbRecipe, RecipeResultBean result) {
        if (dbRecipe.getStatus() == RecipeStatusConstant.REVOKE) {
            throw new DAOException(eh.base.constant.ErrorCode.SERVICE_ERROR, "处方单已被撤销");
        }
        //此时如果处方状态为待审核则说明药师端已经撤销了处方审核结果
        if (dbRecipe.getStatus() == RecipeStatusConstant.READY_CHECK_YS) {
            throw new DAOException(eh.base.constant.ErrorCode.SERVICE_ERROR, "处方审核结果已被撤销");
        }
        if (RecipeStatusEnum.getCheckShowFlag(dbRecipe.getStatus())) {
            throw new DAOException(eh.base.constant.ErrorCode.SERVICE_ERROR, "处方正在审核中");
        }
        if (RecipeStatusConstant.CHECK_PASS != dbRecipe.getStatus()
                || 1 == dbRecipe.getChooseFlag()) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("处方单已被处理");
            //判断是否已到院取药，查看 HisCallBackService *RecipesFromHis 方法处理
            if (Integer.valueOf(1).equals(dbRecipe.getPayFlag())) {
                if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(dbRecipe.getGiveMode())) {
                    result.setMsg("您已到院自取药品，无法选择其他购药方式");
                }
            }
            return true;
        }
        return false;
    }

    private boolean lock(String recipeId) {
        return redisClient.setNX(CacheConstant.KEY_RCP_BUSS_PURCHASE_LOCK + recipeId, "true");
    }

    private boolean unLock(String recipeId) {
        return redisClient.setex(CacheConstant.KEY_RCP_BUSS_PURCHASE_LOCK + recipeId, 1L);
    }

    /**
     * 判断是否是慢病医保患者
     *
     * @param recipeId
     * @return
     */
    public Boolean isMedicareSlowDiseasePatient(Integer recipeId) {
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        if (recipeExtend != null) {
            //3慢病医保
            if ("3".equals(recipeExtend.getPatientType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 配送到家判断是否是医保患者
     *
     * @return
     */
    public Boolean isMedicarePatient(Integer organId, String mpiId) {
        //获取his患者信息判断是否医保患者
        IPatientHisService iPatientHisService = AppContextHolder.getBean("his.iPatientHisService", IPatientHisService.class);
        PatientService patientService = BasicAPI.getService(PatientService.class);
        PatientDTO patient = patientService.get(mpiId);
        if (patient == null) {
            throw new DAOException(eh.base.constant.ErrorCode.SERVICE_ERROR, "平台查询不到患者信息");
        }
        PatientQueryRequestTO req = new PatientQueryRequestTO();
        req.setOrgan(organId);
        req.setPatientName(patient.getPatientName());
        req.setCertificateType(patient.getCertificateType());
        req.setCertificate(patient.getCertificate());
        try {
            HisResponseTO<PatientQueryRequestTO> response = iPatientHisService.queryPatient(req);
            LOG.info("isMedicarePatient response={}", JSONUtils.toString(response));
            if (response != null) {
                PatientQueryRequestTO data = response.getData();
                if (data != null && "2".equals(data.getPatientType())) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.error("isMedicarePatient error" + e);
            throw new DAOException(eh.base.constant.ErrorCode.SERVICE_ERROR, "查询患者信息异常，请稍后重试");
        }
        return false;
    }

    /**
     * 医保结算申请（预结算）---仅医保小程序使用
     *
     * @return
     */
    @RpcService
    public MedicInsurSettleApplyResTO recipeMedicInsurPreSettle(Map<String, Object> map) {
        try {
            Integer organId = MapUtils.getInteger(map, "organId");
            Integer recipeId = MapUtils.getInteger(map, "recipeId"); //平台处方id
            String mpiId = MapUtils.getString(map, "mpiId");
            Args.notBlank(mpiId, "mpiId");
            Args.notNull(organId, "organId");
            Args.notNull(recipeId, "recipeId");
            String redisKey = CacheConstant.KEY_MEDIC_INSURSETTLE_APPlY + recipeId;
            Object object = redisClient.get(redisKey);
            if (null != object) {
                LOG.info("缓存命中，获取缓存,key = {}", redisKey);
                MedicInsurSettleApplyResTO medicInsurSettleApplyResTO = (MedicInsurSettleApplyResTO) object;
                return medicInsurSettleApplyResTO;
            }
            PatientService patientService = BasicAPI.getService(PatientService.class);
            PatientDTO patient = patientService.get(mpiId);
            OrganService organService = BasicAPI.getService(OrganService.class);
            OrganDTO organ = organService.getByOrganId(organId);
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe dbRecipe = recipeDAO.get(recipeId);
            if (null == dbRecipe) {
                throw new DAOException("未查询到处方记录");
            }
            if (null == dbRecipe.getClinicId()) {
                throw new DAOException("未查询到复诊记录");
            }
            IHosrelationService iHosrelationService = BaseAPI.getService(IHosrelationService.class);
            HosrelationBean hosrelationBean = iHosrelationService.getByBusIdAndBusType(dbRecipe.getClinicId(), 3);
            RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
            MedicInsurSettleApplyReqTO reqTO = new MedicInsurSettleApplyReqTO();
            reqTO.setOrganId(organId);
            reqTO.setOrganName(Optional.ofNullable(organ.getShortName()).orElse(""));
            reqTO.setPatientName(patient.getPatientName());
            reqTO.setCertId(patient.getIdcard());
            reqTO.setRecipeId(recipeId.toString());
            reqTO.setRecipeCode(dbRecipe.getRecipeCode());
            reqTO.setClinicId(Optional.ofNullable(dbRecipe.getClinicId().toString()).orElse(""));
            reqTO.setRegisterId(null == hosrelationBean ? "" : hosrelationBean.getRegisterId());
            MedicInsurSettleApplyResTO medicInsurSettleApplyResTO = hisService.recipeMedicInsurPreSettle(reqTO);
//            MedicInsurSettleApplyResTO medicInsurSettleApplyResTO = new MedicInsurSettleApplyResTO();
//            medicInsurSettleApplyResTO.setVisitNo("72787424.34115312");
            redisClient.set(redisKey, medicInsurSettleApplyResTO);
            redisClient.setex(redisKey, 7 * 24 * 60 * 60); //设置超时时间7天
            return medicInsurSettleApplyResTO;
        } catch (Exception e) {
            LOG.error("recipeMedicInsurPreSettle error,param = {}", JSONUtils.toString(map
            ), e);
            if (e instanceof DAOException) {
                throw new DAOException(e.getMessage());
            } else {
                throw new DAOException("医保结算申请失败");
            }
        }
    }

    public void setRecipePayWay(RecipeOrder recipeOrder) {
        try {
//            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
//            Recipe recipe = recipeDAO.findRecipeListByOrderCode(recipeOrder.getOrderCode()).get(0);
            RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
//            IPurchaseService purchaseService = getService(recipe.getPayMode());
            if ("111".equals(recipeOrder.getWxPayWay())) {
                recipeOrder.setPayMode(1);
                recipeOrderDAO.update(recipeOrder);
            }
//            else {
//                purchaseService.setRecipePayWay(recipeOrder);
//            }
        } catch (Exception e) {
            LOG.info("setRecipePayWay error msg:{}.", e.getMessage());
        }
    }

}
