package recipe.purchase;

import com.google.common.collect.ImmutableMap;
import com.ngari.base.BaseAPI;
import com.ngari.base.hisconfig.service.IHisConfigService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.bus.hosrelation.model.HosrelationBean;
import com.ngari.bus.hosrelation.service.IHosrelationService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.his.patient.mode.PatientQueryRequestTO;
import com.ngari.his.patient.service.IPatientHisService;
import com.ngari.his.recipe.mode.MedicInsurSettleApplyReqTO;
import com.ngari.his.recipe.mode.MedicInsurSettleApplyResTO;
import com.ngari.his.recipe.mode.MedicInsurSettleSuccNoticNgariReqTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import coupon.api.service.ICouponBaseService;
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
import recipe.ApplicationUtils;
import recipe.bean.PltPurchaseResponse;
import recipe.constant.*;
import recipe.dao.*;
import recipe.service.RecipeHisService;
import recipe.service.RecipeListService;
import recipe.service.RecipeService;
import recipe.service.RecipeServiceSub;
import recipe.thread.PushRecipeToHisCallable;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.MapValueUtil;
import recipe.util.RedisClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ctd.persistence.DAOFactory.getDAO;

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
            if (Integer.valueOf(0).equals(dbRecipe.getDistributionFlag())
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
     * @param recipeId 处方ID
     * @param payModes 购药方式
     */
    @RpcService
    public RecipeResultBean filterSupportDepList(Integer recipeId, List<Integer> payModes, Map<String, String> extInfo) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        Recipe dbRecipe = recipeDAO.get(recipeId);
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

        for (Integer i : payModes) {
            IPurchaseService purchaseService = getService(i);
            //如果涉及到多种购药方式合并成一个列表，此处需要进行合并
            resultBean = purchaseService.findSupportDepList(dbRecipe, extInfo);
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
        return order(recipeId, extInfo);
    }

    /**
     * @param recipeId
     * @param extInfo  参照RecipeOrderService createOrder定义
     *                 {"operMpiId":"当前操作者编码","addressId":"当前选中地址","payway":"支付方式（payway）","payMode":"处方支付方式",
     *                 "decoctionFlag":"1(1：代煎，0：不代煎)", "gfFeeFlag":"1(1：表示需要制作费，0：不需要)", “depId”:"指定药企ID",
     *                 "expressFee":"快递费","gysCode":"药店编码","sendMethod":"送货方式","payMethod":"支付方式","appId":"公众号ID",
     *                 "calculateFee":"1(1:需要，0:不需要)"}
     *                 <p>
     *                 ps: decoctionFlag是中药处方时设置为1，gfFeeFlag是膏方时设置为1
     *                 gysCode, sendMethod, payMethod 字段为钥世圈字段，会在findSupportDepList接口中给出
     *                 payMode 如果钥世圈有供应商是多种方式支持，就传0
     *                 orderType, 1表示省医保
     * @return
     */
    @RpcService
    public OrderCreateResult order(Integer recipeId, Map<String, String> extInfo) {
        LOG.info("order param: recipeId={},extInfo={}", recipeId, JSONUtils.toString(extInfo));
        OrderCreateResult result = new OrderCreateResult(RecipeResultBean.SUCCESS);

        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise dep = drugsEnterpriseDAO.get(MapValueUtil.getInteger(extInfo, "depId"));
        //订单类型-1省医保
        Integer orderType = MapValueUtil.getInteger(extInfo, "orderType");
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe dbRecipe = recipeDAO.get(recipeId);
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
        //预结算
        //非省直医保才走自费结算
        //省医保不走自费结算
        if (!(orderType != null && (orderType == 1 || orderType == 3))) {
            //目前省中和上海六院走自费预结算---上海六院改成机构配置--获取配送到家支付机构配置-平台付才走
            //首先配的不是卫宁付
            if (!getPayOnlineConfig(dbRecipe.getClinicOrgan())) {
                if ((dep != null && new Integer(1).equals(dep.getIsHosDep()))
                        || (dbRecipe.getClinicOrgan() == 1000899)) {
                    RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
                    Map<String, Object> scanResult = hisService.provincialCashPreSettle(recipeId, payMode);
                    if (!("200".equals(scanResult.get("code")))) {
                        result.setCode(RecipeResultBean.FAIL);
                        if (scanResult.get("msg") != null) {
                            result.setMsg(scanResult.get("msg").toString());
                        }
                        return result;
                    }
                }
            }

        }

        //处方单状态不是待处理 or 处方单已被处理
        boolean dealFlag = checkRecipeIsDeal(dbRecipe, result, extInfo);
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
                unLock(recipeId);
                return result;
            }
        }

        OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        IHisConfigService iHisConfigService = ApplicationUtils.getBaseService(IHisConfigService.class);
        try {
            //判断院内是否已取药，防止重复购买
            //date 20191022到院取药取配置项
            boolean flag = RecipeServiceSub.getDrugToHos(recipeId, dbRecipe.getClinicOrgan());
            boolean hisStatus = iHisConfigService.isHisEnable(dbRecipe.getClinicOrgan());
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

        //判断是否存在分布式锁
        boolean unlock = lock(recipeId);
        if (!unlock) {
            //存在锁则需要返回
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("您有正在进行中的订单");
            return result;
        } else {
            //设置默认超时时间 30s
            redisClient.setex(CacheConstant.KEY_RCP_BUSS_PURCHASE_LOCK + recipeId, 30L);
        }

        try {
            IPurchaseService purchaseService = getService(payMode);
            result = purchaseService.order(dbRecipe, extInfo);
        } catch (Exception e) {
            LOG.error("order error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        } finally {
            //订单创建完解锁
            unLock(recipeId);
            //此处将HIS处方状态进行调整
            try{
                //对于来源于HIS的处方单更新hisRecipe的状态
                HisRecipeDAO hisRecipeDAO = getDAO(HisRecipeDAO.class);
                HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(dbRecipe.getClinicOrgan(), dbRecipe.getRecipeCode());
                if (hisRecipe != null) {
                    hisRecipeDAO.updateHisRecieStatus(dbRecipe.getClinicOrgan(), dbRecipe.getRecipeCode(), 2);
                }
            }catch (Exception e){
                LOG.info("RecipeOrderService.cancelOrder 来源于HIS的处方单更新hisRecipe的状态失败,recipeId:{},{}.", dbRecipe.getRecipeId(), e.getMessage());
            }
        }

        return result;
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
     * @param dbRecipe 处方
     * @param result   结果
     * @return true 已被处理
     */
    private boolean checkRecipeIsDeal(Recipe dbRecipe, RecipeResultBean result, Map<String, String> extInfo) {
        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        if (dbRecipe.getStatus() == RecipeStatusConstant.REVOKE){
            throw new DAOException(eh.base.constant.ErrorCode.SERVICE_ERROR, "处方单已被撤销");
        }
        if (RecipeStatusConstant.CHECK_PASS != dbRecipe.getStatus()
                || 1 == dbRecipe.getChooseFlag()) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("处方单已被处理");
            //判断是否已到院取药，查看 HisCallBackService *RecipesFromHis 方法处理
            if (Integer.valueOf(1).equals(dbRecipe.getPayFlag())) {
                if (RecipeBussConstant.PAYMODE_TO_HOS.equals(dbRecipe.getPayMode()) && RecipeBussConstant.PAYMODE_TFDS == payMode) {
                    result.setCode(2);
                    result.setMsg("您已到院自取药品，无法提交药店取药");
                } else if (RecipeBussConstant.PAYMODE_TO_HOS.equals(dbRecipe.getPayMode()) && RecipeBussConstant.PAYMODE_ONLINE == payMode) {
                    result.setCode(3);
                    result.setMsg("您已到院自取药品，无法进行配送");
                } else if (RecipeBussConstant.PAYMODE_ONLINE.equals(dbRecipe.getPayMode())) {
                    result.setCode(4);
                    result.setMsg(dbRecipe.getOrderCode());
                }
            }
            return true;
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
        Integer status = recipe.getStatus();
        Integer payMode = recipe.getPayMode();
        Integer payFlag = recipe.getPayFlag();
        String orderCode = recipe.getOrderCode();
        if (order == null) {
            RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
            order = recipeOrderDAO.getByOrderCode(orderCode);
        }
        String tips;
        switch (status) {
            case RecipeStatusConstant.READY_CHECK_YS:
                tips = "请耐心等待药师审核";
                break;
            case RecipeStatusConstant.CHECK_PASS:
                if (StringUtils.isNotEmpty(orderCode) && payFlag == 0 && order.getActualPrice() > 0) {
                    tips = "订单待支付，请于收到处方的3日内处理完成，否则处方将失效";
                } else if (StringUtils.isEmpty(orderCode)) {
                    tips = "处方单待处理，请于收到处方的3日内完成购药，否则处方将失效";
                } else {
                    IPurchaseService purchaseService = getService(payMode);
                    tips = purchaseService.getTipsByStatusForPatient(recipe, order);
                }
                break;
            case RecipeStatusConstant.NO_PAY:
                tips = "处方单未支付，已失效";
                break;
            case RecipeStatusConstant.NO_OPERATOR:
                tips = "处方单未处理，已失效";
                break;
            case RecipeStatusConstant.CHECK_NOT_PASS_YS:
                if (RecipecCheckStatusConstant.Check_Normal == recipe.getCheckStatus()) {
                    tips = "处方审核不通过，请联系开方医生";
                    break;
                } else {
                    tips = "请耐心等待药师审核";
                    break;
                }
            case RecipeStatusConstant.REVOKE:
                tips = "由于医生已撤销，该处方单已失效，请联系医生";
                break;
            case RecipeStatusConstant.RECIPE_DOWNLOADED:
                tips = "已下载处方笺";
                break;
            case RecipeStatusConstant.USING:
                tips = "处理中";
                break;
            //date 2019/10/16
            //添加处方状态文案，已删除，同步his失败
            case RecipeStatusConstant.DELETE:
                tips = "处方单已删除";
                break;
            case RecipeStatusConstant.HIS_FAIL:
                tips = "处方单同步his写入失败";
                break;
            case RecipeStatusConstant.FINISH:
                //特应性处理:下载处方，不需要审核,不更新payMode
                if (ReviewTypeConstant.Not_Need_Check == recipe.getReviewType() && RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(recipe.getGiveMode())) {
                    tips = "订单完成";
                    break;
                }
            default:
                IPurchaseService purchaseService = getService(payMode);
                if (null == purchaseService) {
                    tips = "";
                } else {
                    tips = purchaseService.getTipsByStatusForPatient(recipe, order);
                }
        }
        return tips;
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
            IPurchaseService purchaseService = getService(recipe.getPayMode());
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
        if (dbRecipe.getStatus() == RecipeStatusConstant.REVOKE){
            throw new DAOException(eh.base.constant.ErrorCode.SERVICE_ERROR, "处方单已被撤销");
        }
        if (RecipeStatusConstant.CHECK_PASS != dbRecipe.getStatus()
                || 1 == dbRecipe.getChooseFlag()) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("处方单已被处理");
            //判断是否已到院取药，查看 HisCallBackService *RecipesFromHis 方法处理
            if (Integer.valueOf(1).equals(dbRecipe.getPayFlag())) {
                if (RecipeBussConstant.PAYMODE_TO_HOS.equals(dbRecipe.getPayMode())) {
                    result.setMsg("您已到院自取药品，无法选择其他购药方式");
                }
            }
            return true;
        }
        //下面这块逻辑是针对市三(医保患者)选择配送到家或者(非医保患者)已经选择了到院取药使用
        //市三的到院取药依然使用的互联网的接口purchase
        //去掉市三个性化流程改造成平台流程
        /*if (RecipeStatusConstant.CHECK_PASS == dbRecipe.getStatus()) {
            Integer consultId = dbRecipe.getClinicId();
            Integer medicalFlag = 0;
            IConsultExService consultExService = ApplicationUtils.getConsultService(IConsultExService.class);
            if (consultId != null) {
                ConsultExDTO consultExDTO = consultExService.getByConsultId(consultId);
                if (consultExDTO != null) {
                    medicalFlag = consultExDTO.getMedicalFlag();
                }
            }
            RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
            if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(dbRecipe.getRecipeMode()) && (RecipeExtendConstant.MEDICAL_FALG_YES == medicalFlag || dbRecipe.getChooseFlag() == 1)) {
                OrganDTO organDTO = organService.getByOrganId(dbRecipe.getClinicOrgan());
                List<Recipedetail> detailList = detailDAO.findByRecipeId(dbRecipe.getRecipeId());
                result.setCode(RecipeResultBean.FAIL);
                String tips;
                if (RecipeExtendConstant.MEDICAL_FALG_YES == medicalFlag) {
                    tips = "您是医保病人，请到医院支付取药，医院取药窗口：";
                } else {
                    tips = "请到医院支付取药，医院取药窗口：";
                }
                if (CollectionUtils.isNotEmpty(detailList)) {
                    String pharmNo = detailList.get(0).getPharmNo();
                    if (StringUtils.isNotEmpty(pharmNo)) {
                        tips += "[" + organDTO.getName() + "" + pharmNo + "取药窗口]";
                    } else {
                        tips += "[" + organDTO.getName() + "取药窗口]";
                    }
                }
                result.setMsg(tips);
                return true;
            }
        }*/
        return false;
    }

    private boolean lock(Integer recipeId) {
        return redisClient.setNX(CacheConstant.KEY_RCP_BUSS_PURCHASE_LOCK + recipeId, "true");
    }

    private boolean unLock(Integer recipeId) {
        return redisClient.setex(CacheConstant.KEY_RCP_BUSS_PURCHASE_LOCK + recipeId, 1L);
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
     * 医保结算申请（预结算）
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


}
