package recipe.service.common;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.process.service.IRecipeOnLineConsultService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeCommonBaseTO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.common.RecipeStandardReqTO;
import com.ngari.recipe.common.RecipeStandardResTO;
import com.ngari.recipe.dto.EmrDetailDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.process.service.IRecipeOnLineRevisitService;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.aop.LogRecord;
import recipe.bean.CheckYsInfoBean;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.DocIndexClient;
import recipe.constant.*;
import recipe.core.api.IStockBusinessService;
import recipe.dao.*;
import recipe.drugTool.validate.RecipeDetailValidateTool;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.status.WriteHisEnum;
import recipe.enumerate.type.SignImageTypeEnum;
import recipe.enumerate.type.StockCheckSourceTypeEnum;
import recipe.hisservice.HisMqRequestInit;
import recipe.hisservice.RecipeToHisMqService;
import recipe.manager.CaManager;
import recipe.manager.RecipeManager;
import recipe.manager.RevisitManager;
import recipe.manager.StateManager;
import recipe.service.*;
import recipe.thread.CardDataUploadRunable;
import recipe.thread.PushRecipeToHisCallable;
import recipe.thread.RecipeBusiThreadPool;
import recipe.thread.SaveAutoReviewRunnable;
import recipe.util.DigestUtil;
import recipe.util.MapValueUtil;
import recipe.util.RedisClient;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static ctd.persistence.DAOFactory.getDAO;


/**
 * @author： 0184/yu_yun
 * @date： 2018/9/18
 * @description： 处方签名服务
 * @version： 1.0
 */
@RpcBean("recipeSignService")
public class RecipeSignService {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RecipeSignService.class);
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private StateManager stateManager;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private RecipeDetailValidateTool recipeDetailValidateTool;
    @Autowired
    private DrugsEnterpriseService drugsEnterpriseService;
    @Autowired
    private IStockBusinessService drugEnterpriseBusinessService;
    @Autowired
    private RevisitManager revisitManager;
    @Autowired
    private DocIndexClient docIndexClient;
    @Resource
    private CaManager caManager;
    @Autowired
    private CreatePdfFactory createPdfFactory;
    @Autowired
    private RecipeManager recipeManager;

    /**
     * 武昌模式签名方法
     *
     * @param recipeId
     * @param request
     * @return
     */
    @RpcService
    @LogRecord
    public RecipeStandardResTO<Map> sign(Integer recipeId, RecipeStandardReqTO request) {
        LOG.info("RecipeSignService sign recipeId:{},request:{}", recipeId, JSON.toJSONString(request));
        RecipeStandardResTO<Map> response = RecipeStandardResTO.getRequest(Map.class);
        response.setCode(RecipeCommonBaseTO.FAIL);
        //TODO 先校验处方是否有效
        if (null == recipeId) {
            response.setMsg("处方单ID为空");
            return response;
        }

        Recipe dbRecipe = recipeDAO.get(recipeId);
        if (null == dbRecipe) {
            response.setMsg("没有该处方单");
            return response;
        }

        if (RecipeStatusConstant.UNSIGN != dbRecipe.getStatus()) {
            response.setMsg("处方单已签名");
            return response;
        }

        //查询订单
        RecipeOrder order = getDAO(RecipeOrderDAO.class).getByOrderCode(dbRecipe.getOrderCode());
        if (null == order) {
            response.setMsg("订单不存在");
            return response;
        }

        //配送数据校验
        Map<String, Object> conditions = request.getConditions();
        Integer giveMode = MapValueUtil.getInteger(conditions, "giveMode");
        Integer depId = MapValueUtil.getInteger(conditions, "depId");
        if (null == depId && !RecipeBussConstant.GIVEMODE_FREEDOM.equals(giveMode) && !RecipeBussConstant.GIVEMODE_TO_HOS.equals(giveMode)) {
            response.setMsg("缺少药企编码");
            return response;
        }

        String depName = MapValueUtil.getString(conditions, "depName");
        String pharmacyCode = MapValueUtil.getString(conditions, "pharmacyCode");
        String pharmacyAddress = MapValueUtil.getString(conditions, "pharmacyAddress");
        String patientAddress = MapValueUtil.getString(conditions, "patientAddress");
        String patientTel = MapValueUtil.getString(conditions, "patientTel");
        Integer payMode = null;
        Integer newPayMode = null;
        if (null != giveMode) {
            if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode)) {
                //药店取药
                if (StringUtils.isEmpty(pharmacyCode)) {
                    response.setMsg("缺少药店编码");
                    return response;
                }
                payMode = RecipeBussConstant.PAYMODE_TFDS;
                newPayMode = RecipeBussConstant.PAYMODE_OFFLINE;
            } else if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode)) {
                //配送到家
                if (StringUtils.isEmpty(patientAddress) || StringUtils.isEmpty(patientTel)) {
                    response.setMsg("配送信息不全");
                    return response;
                }
                //校验参数准确性
                if (!RegexEnum.regular(patientTel, RegexEnum.MOBILE)) {
                    response.setMsg("请输入有效手机号码");
                    return response;
                }
                if (StringUtils.length(patientAddress) > 100) {
                    response.setMsg("地址不能超过100个字");
                    return response;
                }
                payMode = RecipeBussConstant.PAYMODE_ONLINE;
                newPayMode = RecipeBussConstant.PAYMODE_ONLINE;
            } else if (RecipeBussConstant.GIVEMODE_FREEDOM.equals(giveMode)) {
                //患者自由选择
                depId = null;
                payMode = RecipeBussConstant.PAYMODE_COMPLEX;
                newPayMode = RecipeBussConstant.PAYMODE_OFFLINE;
            } else if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(giveMode)) {
                //到院取药----走九州通补充库存模式----这里直接推送--不需要审核
                payMode = RecipeBussConstant.PAYMODE_TO_HOS;
                newPayMode = RecipeBussConstant.PAYMODE_OFFLINE;
                //武昌模式到院取药推送处方到九州通
                //没有库存就推送九州通
                drugsEnterpriseService.pushHosInteriorSupport(dbRecipe.getRecipeId(), dbRecipe.getClinicOrgan());
                //发送患者没库存消息
                RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_HOSSUPPORT_NOINVENTORY, ObjectCopyUtils.convert(dbRecipe, Recipe.class));
                String memo = "医院保存没库存处方并推送九州通/发送无库存短信成功";
                //日志记录
                RecipeLogService.saveRecipeLog(dbRecipe.getRecipeId(), dbRecipe.getStatus(), dbRecipe.getStatus(), memo);
            } else {
                response.setMsg("缺少取药方式");
                return response;
            }
        } else {
            response.setMsg("缺少取药方式");
            return response;
        }

        //签名
        RecipeBusiThreadPool.execute(() -> {
            try {
                //生成pdf并签名
                Recipe recipe = recipeDAO.getByRecipeId(recipeId);
                createPdfFactory.queryPdfOssId(recipe);
                createPdfFactory.updatePdfToImg(recipe.getRecipeId(), SignImageTypeEnum.SIGN_IMAGE_TYPE_DOCTOR.getType());
            } catch (Exception e) {
                LOG.error("sign 签名服务异常，recipeId={}", recipeId, e);
            }
        });

        //修改订单
        if (StringUtils.isEmpty(dbRecipe.getOrderCode())) {
            //订单在接收HIS处方时生成
            response.setMsg("处方订单不存在");
            return response;
        } else {
            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);

            //为确保通知能送达用户手机需要重置下手机信息
            if (StringUtils.isEmpty(patientTel)) {
                RecipeOrder dbOrder = DAOFactory.getDAO(RecipeOrderDAO.class).getByOrderCode(dbRecipe.getOrderCode());
                //优先从order表中获取，his上传处方时会记录his上传的患者手机号
                if (null != dbOrder && StringUtils.isNotEmpty(dbOrder.getRecMobile())) {
                    patientTel = dbOrder.getRecMobile();
                } else {
                    PatientService patientService = BasicAPI.getService(PatientService.class);
                    PatientDTO patient = patientService.get(dbRecipe.getMpiid());
                    if (null != patient) {
                        patientTel = patient.getMobile();
                        patientAddress = patient.getAddress();
                    } else {
                        LOG.warn("sign 患者不存在，可能导致短信无法通知. recipeId={}, mpiId={}", recipeId, dbRecipe.getMpiid());
                    }
                }
            }

            // 修改订单一些参数
            Map<String, Object> orderAttr = Maps.newHashMap();
            orderAttr.put("enterpriseId", depId);
            //未支付不知道支付方式
            orderAttr.put("wxPayWay", "-1");
            //使订单生效
            orderAttr.put("effective", 1);
            orderAttr.put("receiver", dbRecipe.getPatientName());
            orderAttr.put("address4", patientAddress);
            orderAttr.put("recMobile", patientTel);
            orderAttr.put("drugStoreName", depName);
            orderAttr.put("drugStoreAddr", pharmacyAddress);
            orderAttr.put("payMode", newPayMode);
            RecipeResultBean resultBean = orderService.updateOrderInfo(dbRecipe.getOrderCode(), orderAttr, null);
            if (RecipeResultBean.SUCCESS.equals(resultBean.getCode())) {
                LOG.info("sign 订单更新成功 recipeId={}, orderCode={}", recipeId, dbRecipe.getOrderCode());
            } else {
                LOG.warn("sign 订单更新失败. recipeId={}, orderCode={}", recipeId, dbRecipe.getOrderCode());
                response.setMsg("处方订单更新错误");
                return response;
            }
        }

        //修改订单成功后再去更新处方状态及配送信息等，使接口可重复调用
        Map<String, Object> attrMap = Maps.newHashMap();
        attrMap.put("giveMode", giveMode);
        attrMap.put("enterpriseId", depId);
        attrMap.put("chooseFlag", 1);
        //不做失效前提醒
        attrMap.put("remindFlag", 1);

        /**
         * 药店取药和自由选择都流转到药师审核，审核完成推送给药企
         */
        boolean sendYsCheck = false;
        Integer status = RecipeStatusConstant.CHECK_PASS;
        if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode) || RecipeBussConstant.GIVEMODE_FREEDOM.equals(giveMode)) {
            status = RecipeStatusConstant.READY_CHECK_YS;
            sendYsCheck = true;
        }
        recipeDAO.updateRecipeInfoByRecipeId(recipeId, status, attrMap);

        //根据配置判断是否需要人工审核, 配送到家处理在支付完成后回调 RecipeOrderService finishOrderPay
        if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode) || RecipeBussConstant.GIVEMODE_FREEDOM.equals(giveMode)) {
            Set<String> organIdList = redisClient.sMembers(CacheConstant.KEY_SKIP_YSCHECK_LIST);
            if (CollectionUtils.isNotEmpty(organIdList) && organIdList.contains(dbRecipe.getClinicOrgan().toString())) {
                RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
                //不用发药师消息
                sendYsCheck = false;
                //跳过人工审核
                CheckYsInfoBean checkResult = new CheckYsInfoBean();
                checkResult.setRecipeId(recipeId);
                checkResult.setCheckDoctorId(dbRecipe.getDoctor());
                checkResult.setCheckOrganId(dbRecipe.getClinicOrgan());
                List<Integer> checkerList = recipeManager.getDocIdInTime(dbRecipe.getClinicOrgan());
                LOG.info("sign checkerList:{}", JSON.toJSONString(checkerList));
                if (CollectionUtils.isNotEmpty(checkerList) && checkerList.size() > 0) {
                    createPdfFactory.updateCheckNamePdfESign(recipeId, checkerList.get(0));
                } else {
                    createPdfFactory.updateCheckNamePdfESign(recipeId, dbRecipe.getDoctor());
                }

                try {
                    recipeService.autoPassForCheckYs(checkResult);
                } catch (Exception e) {
                    LOG.error("sign 药师自动审核失败. recipeId={}", recipeId, e);
                    RecipeLogService.saveRecipeLog(recipeId, dbRecipe.getStatus(), status,
                            "sign 药师自动审核失败:" + e.getMessage());
                }
            }
        }

        //设置其他参数
        response.setData(ImmutableMap.of("orderId", order.getOrderId()));

        //日志记录
        RecipeLogService.saveRecipeLog(recipeId, dbRecipe.getStatus(), status, "sign 完成 giveMode=" + giveMode);
        response.setCode(RecipeCommonBaseTO.SUCCESS);

        //推送身边医生消息
        if (sendYsCheck) {
            RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_YS_READYCHECK_4HIS, dbRecipe);
        }
        return response;
    }

    /**
     * 武昌模式判断药品能否开在一张处方单上
     *
     * @param recipeId
     * @param drugIds
     */
    @RpcService
    public void canOpenRecipeDrugs(Integer recipeId, List<Integer> drugIds, Integer giveMode) {
        LOG.info("RecipeSignService.canOpenRecipeDrugs recipeId:{},drugIds:{},giveMode:{}.", recipeId, JSONUtils.toString(drugIds), giveMode);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (recipe == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方不存在");
        }
        RecipeServiceSub.canOpenRecipeDrugs(recipe.getClinicOrgan(), recipeId, drugIds);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        boolean isSupport = false;
        //到院取药判断九州通药品是否支持
        if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(giveMode)) {
            //取该机构下配置的补充库存药企
            List<DrugsEnterprise> enterpriseList = enterpriseDAO.findByOrganIdAndHosInteriorSupport(recipe.getClinicOrgan());
            for (DrugsEnterprise drugsEnterprise : enterpriseList) {
                List<SaleDrugList> saleDruglists = saleDrugListDAO.findByOrganIdAndDrugIds(drugsEnterprise.getId(), drugIds);
                if (CollectionUtils.isNotEmpty(saleDruglists) && saleDruglists.size() == drugIds.size()) {
                    //存在即支持
                    isSupport = true;
                    break;
                }
            }

        } else {
            //其他购药方式 药品在支付宝是否支持
            List<SaleDrugList> zfbDrug = saleDrugListDAO.findByOrganIdAndDrugIds(103, drugIds);
            if (CollectionUtils.isNotEmpty(zfbDrug) && zfbDrug.size() == drugIds.size()) {
                isSupport = true;
            }
        }
        if (!isSupport) {
            DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
            List<DrugList> drugList = drugListDAO.findByDrugIds(drugIds);
            //拼接不支持药品名
            String drugNames = drugList.stream().map(DrugList::getDrugName).collect(Collectors.joining(","));
            throw new DAOException(ErrorCode.SERVICE_ERROR, drugNames + "不能开具在一张处方上！");
        }

    }


    /**
     * 签名服务（新）
     *
     * @param recipeBean     处方
     * @param detailBeanList 详情
     * @param continueFlag   校验标识
     * @return Map<String, Object>
     */
    @RpcService
    public Map<String, Object> doSignRecipeNew(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList, int continueFlag) {
        LOG.info("RecipeSignService.doSignRecipeNew param: recipeBean={} detailBean={} continueFlag={}", JSONUtils.toString(recipeBean), JSONUtils.toString(detailBeanList), continueFlag);
        recipeDetailValidateTool.validateMedicalChineDrugNumber(recipeBean, recipeBean.getRecipeExtend(), detailBeanList);
        caManager.setCaPassWord(recipeBean.getClinicOrgan(), recipeBean.getDoctor(), recipeBean.getCaPassword());
        Map<String, Object> rMap = new HashMap<String, Object>();
        rMap.put("signResult", true);
        try {
            recipeBean.setDistributionFlag(continueFlag);
            //第一步暂存处方（处方状态未签名）
            doSignRecipeSave(recipeBean, detailBeanList);
            //第二步预校验
            if (continueFlag == 0) {
                //his处方预检查
                boolean b = hisRecipeCheck(rMap, recipeBean);
                if (!b) {
                    rMap.put("signResult", false);
                    rMap.put("recipeId", recipeBean.getRecipeId());
                    rMap.put("errorFlag", true);
                    return rMap;
                }
            }
            //第三步校验库存
            Integer appointEnterpriseType = recipeBean.getRecipeExtend().getAppointEnterpriseType();
            if ((continueFlag == 0 || continueFlag == 4) && ValidateUtil.integerIsEmpty(appointEnterpriseType)
                    && ValidateUtil.integerIsEmpty(recipeBean.getVersion())) {
                rMap = drugEnterpriseBusinessService.enterpriseStockMap(recipeBean.getRecipeId(), StockCheckSourceTypeEnum.DOCTOR_STOCK.getType());
                boolean signResult = Boolean.parseBoolean(rMap.get("signResult").toString());
                if (!signResult) {
                    return rMap;
                }
            }
            //更新审方信息recipeTempMark
            RecipeBusiThreadPool.execute(new SaveAutoReviewRunnable(recipeBean, detailBeanList));
            //发送HIS处方开具消息
            sendRecipeToHIS(recipeBean);
            //处方开完后发送聊天界面消息 -医院确认中
            Integer consultId = recipeBean.getClinicId();
            if (null != consultId && !RecipeBussConstant.BUSS_SOURCE_WLZX.equals(recipeBean.getBussSource())) {
                try {
                    if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipeBean.getBussSource())) {
                        IRecipeOnLineRevisitService recipeOnLineConsultService = RevisitAPI.getService(IRecipeOnLineRevisitService.class);
                        recipeOnLineConsultService.sendRecipeMsg(consultId, 2);
                    } else if (RecipeBussConstant.BUSS_SOURCE_WZ.equals(recipeBean.getBussSource())) {
                        IRecipeOnLineConsultService recipeOnLineConsultService = ConsultAPI.getService(IRecipeOnLineConsultService.class);
                        recipeOnLineConsultService.sendRecipeMsg(consultId, 2);
                    }
                } catch (Exception e) {
                    LOG.error("doSignRecipeExt sendRecipeMsg error, type:2, consultId:{}, error:", consultId, e);
                }
            }
            //健康卡数据上传
            RecipeBusiThreadPool.execute(new CardDataUploadRunable(recipeBean.getClinicOrgan(), recipeBean.getMpiid(), "010106"));
            revisitManager.saveRevisitTracesList(recipeDAO.get(recipeBean.getRecipeId()));
        } catch (Exception e) {
            LOG.error("doSignRecipeNew error", e);
            throw new DAOException(recipe.constant.ErrorCode.SERVICE_ERROR, e.getMessage());
        }
        rMap.put("bussSource", recipeBean.getBussSource());
        rMap.put("recipeId", recipeBean.getRecipeId());
        rMap.put("consultId", recipeBean.getClinicId());
        rMap.put("signResult", true);
        rMap.put("errorFlag", false);
        rMap.put("canContinueFlag", "0");
        LOG.info("doSignRecipeNew execute ok! rMap:" + JSONUtils.toString(rMap));
        return rMap;
    }

    /**
     * 签名服务（处方存储）
     *
     * @param recipeBean 处方
     * @param details    详情
     * @return
     */
    @RpcService
    public Integer doSignRecipeSave(RecipeBean recipeBean, List<RecipeDetailBean> details) {
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        PatientService patientService = BasicAPI.getService(PatientService.class);

        Map<String, Object> rMap = Maps.newHashMap();
        PatientDTO patient = patientService.get(recipeBean.getMpiid());
        //解决旧版本因为wx2.6患者身份证为null，而业务申请不成功
        if (patient == null || StringUtils.isEmpty(patient.getCertificate())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该患者还未填写身份证信息，不能开处方");
        }
        // 就诊人改造：为了确保删除就诊人后历史处方不会丢失，加入主账号用户id
        //bug#46436 本人就诊人被删除保存不了导致后续微信模板消息重复推送多次
        List<PatientDTO> requestPatients = patientService.findOwnPatientLimitOne(patient.getLoginId());
        if (CollectionUtils.isNotEmpty(requestPatients)) {
            PatientDTO requestPatient = requestPatients.get(0);
            if (null != requestPatient && null != requestPatient.getMpiId()) {
                recipeBean.setRequestMpiId(requestPatient.getMpiId());
                // urt用于系统消息推送
                recipeBean.setRequestUrt(requestPatient.getUrt());
            }
        }
        //判断机构是否需要his处方检查 ---运营平台机构配置
        if (null != recipeBean.getRecipeExtend()) {
            EmrDetailDTO emrDetailDTO = docIndexClient.getEmrDetails(recipeBean.getRecipeExtend().getDocIndexId());
            recipeBean.setOrganDiseaseName(emrDetailDTO.getOrganDiseaseName());
            recipeBean.setOrganDiseaseId(emrDetailDTO.getOrganDiseaseId());
        }
        recipeBean.setStatus(RecipeStatusConstant.UNSIGN);
        recipeBean.setSignDate(DateTime.now().toDate());
        recipeBean.setRecipeMode(RecipeBussConstant.RECIPEMODE_ZJJGPT);
        Integer recipeId = recipeBean.getRecipeId();

        //生成处方编号，不需要通过HIS去产生
        String recipeCodeStr = "ngari" + DigestUtil.md5For16(recipeBean.getClinicOrgan() +
                recipeBean.getMpiid() + Calendar.getInstance().getTimeInMillis());
        recipeBean.setRecipeCode(recipeCodeStr);

        boolean optimize = recipeService.openRecipeOptimize(recipeBean);
        //配置开启，根据有效的挂号序号进行判断
        if (!optimize) {
            LOG.error("ErrorCode.SERVICE_ERROR:erroCode={}", ErrorCode.SERVICE_ERROR);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "当前患者就诊信息已失效，无法进行开方。");
        }
        //recipeManager.isOpenRecipeNumber(recipeBean.getClinicId(), recipeBean.getClinicOrgan(), recipeBean.getRecipeId());
        //如果是已经暂存过的处方单，要去数据库取状态 判断能不能进行签名操作
        details.stream().filter(a -> "无特殊煎法".equals(a.getMemo())).forEach(a -> a.setMemo(""));
        if (null != recipeId && recipeId > 0) {
            Integer status = recipeDAO.getStatusByRecipeId(recipeId);
            if (null == status || (status > RecipeStatusConstant.UNSIGN && status != RecipeStatusConstant.HIS_FAIL)) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "处方单已处理,不能重复签名");
            }
            recipeService.updateRecipeAndDetail(recipeBean, details);
        } else {
            recipeId = recipeService.saveRecipeData(recipeBean, details);
            recipeBean.setRecipeId(recipeId);
        }
        rMap.put("recipeId", recipeId);
        return recipeId;
    }

    @RpcService
    public boolean hisRecipeCheck(Map<String, Object> rMap, RecipeBean recipeBean) {
        try {
            IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            Boolean hisRecipeCheckFlag = (Boolean) configurationService.getConfiguration(recipeBean.getClinicOrgan(), "hisRecipeCheckFlag");
            if (hisRecipeCheckFlag) {
                RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
                boolean checkResult = hisService.hisRecipeCheck(rMap, recipeBean);
                if (checkResult) {
                    rMap.put("canContinueFlag", 0);
                } else {
                    Boolean allowContinueMakeFlag = (Boolean) configurationService.getConfiguration(recipeBean.getClinicOrgan(), "allowContinueMakeRecipe");
                    //date 20200706
                    //允许继续处方:不进行校验/进行校验且校验通过0 ，进行校验校验不通过允许通过4，进行校验校验不通过不允许通过-1
                    if (allowContinueMakeFlag) {
                        rMap.put("canContinueFlag", 4);
                        rMap.put("msg", rMap.get("errorMsg"));
                    } else {
                        rMap.put("canContinueFlag", -1);
                        rMap.put("msg", rMap.get("errorMsg"));
                    }
                    RecipeLogService.saveRecipeLog(recipeBean.getRecipeId(), recipeBean.getStatus(), recipeBean.getStatus(), "处方预校验失败");
                }
                LOG.info("当前处方预校验返回结果map{}", rMap);
                return checkResult;
            }
        } catch (Exception e) {
            LOG.error("hisRecipeCheck error recipeId:{}", recipeBean.getRecipeId(), e);
            rMap.put("signResult", false);
            rMap.put("errorFlag", true);
            rMap.put("errorMsg", "his处方检查异常");
            rMap.put("canContinueFlag", -1);
            rMap.put("msg", "his处方检查异常");
            return false;
        }
        rMap.put("canContinueFlag", 0);
        LOG.info("当前处方预校验返回结果map{}", rMap);
        return true;
    }

    private void sendRecipeToHIS(RecipeBean recipeBean) {
        Recipe recipe = new Recipe();
        recipe.setRecipeId(recipeBean.getRecipeId());
        recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_CHECKING_HOS.getType());
        recipe.setWriteHisState(WriteHisEnum.WRITE_HIS_STATE_SUBMIT.getType());
        recipeDAO.updateNonNullFieldByPrimaryKey(recipe);
        stateManager.updateRecipeState(recipeBean.getRecipeId(), RecipeStateEnum.PROCESS_STATE_SUBMIT, RecipeStateEnum.NONE);
        //可通过缓存控制是互联网方式发送处方(his来查)还是平台模式发送处方(平台推送)
        Set<String> organIdList = redisClient.sMembers(CacheConstant.KEY_NGARI_SENDRECIPETOHIS_LIST);
        if (CollectionUtils.isNotEmpty(organIdList) && organIdList.contains(recipeBean.getClinicOrgan().toString())) {
            RecipeBusiThreadPool.execute(new PushRecipeToHisCallable(recipeBean.getRecipeId()));
        } else {
            //MQ推送处方开成功消息
            RecipeToHisMqService hisMqService = ApplicationUtils.getRecipeService(RecipeToHisMqService.class);
            hisMqService.recipeStatusToHis(HisMqRequestInit.initRecipeStatusToHisReq(recipeBean,
                    HisBussConstant.TOHIS_RECIPE_STATUS_ADD));
        }

    }

    /**
     * 互联网医院项目-处方检查不通过后点击继续签名处理
     *
     * @param recipeBean
     * @param details
     * @return
     */
    @RpcService
    @Deprecated
    public Map<String, Object> continueSignAfterCheckFailed(RecipeBean recipeBean, List<RecipeDetailBean> details) {

        Map<String, Object> rMap = Maps.newHashMap();
        Integer recipeId = recipeBean.getRecipeId();
        if (!RecipeServiceSub.isNotHZInternet(recipeBean.getClinicOrgan())) {
            rMap.put("signResult", false);
            rMap.put("recipeId", recipeId);
            rMap.put("errorFlag", true);
            rMap.put("errorMsg", "预校验失败,无法继续签名");
            return rMap;
        }
        RecipeDAO dao = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = dao.getByRecipeId(recipeId);
        RecipeBean recipeBeanDb = ObjectCopyUtils.convert(recipe, RecipeBean.class);

        //更新审方信息
        RecipeBusiThreadPool.execute(new SaveAutoReviewRunnable(recipeBeanDb, details));
        //发送HIS处方开具消息
        sendRecipeToHIS(recipeBean);
        //健康卡数据上传
        RecipeBusiThreadPool.execute(new CardDataUploadRunable(recipeBean.getClinicOrgan(), recipeBean.getMpiid(), "010106"));
        LOG.info("continueSignAfterCheckFailed execute ok! recipeId={}", recipeId);
        rMap.put("signResult", true);
        rMap.put("recipeId", recipeId);
        rMap.put("errorFlag", false);
        return rMap;
    }

    /**
     * 互联网医院项目模式-重试签名
     *
     * @param recipeId
     * @return
     */
    @RpcService
    public RecipeResultBean sendNewRecipeToHIS(Integer recipeId) {
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();

        Recipe dbRecipe = recipeDAO.getByRecipeId(recipeId);
        //date 20191127
        //重试功能添加his写入失败的处方
        //目前仅 医院确认中、his上传处方失败、医保上传失败状态下可以重试处方
        if (null == dbRecipe || canNoRetryStatus(dbRecipe.getStatus())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方不能重试");
        }
        // recipeManager.isOpenRecipeNumber(dbRecipe.getClinicId(), dbRecipe.getClinicOrgan(), recipeId);
        //获取处方回写单号  提示推送成功，否则继续推送
        String recipeCode = dbRecipe.getRecipeCode();
        if (StringUtils.isNotEmpty(recipeCode)) {
            resultBean.setCode(RecipeResultBean.PUSHSUCCESS);
            resultBean.setMsg("处方已推送成功");
        } else {
            resultBean.setCode(RecipeResultBean.SUCCESS);
            resultBean.setMsg("已重新提交医院系统");
        }
        LOG.info("sendNewRecipeToHIS before His! dbRecipe={}", JSONUtils.toString(dbRecipe));
        //发送HIS处方开具消息
        RecipeToHisMqService hisMqService = ApplicationUtils.getRecipeService(RecipeToHisMqService.class);
        RecipeBean recipeBean = ObjectCopyUtils.convert(dbRecipe, RecipeBean.class);
        hisMqService.recipeStatusToHis(HisMqRequestInit.initRecipeStatusToHisReq(recipeBean,
                HisBussConstant.TOHIS_RECIPE_STATUS_ADD));

        LOG.info("sendNewRecipeToHIS execute ok! result={}", JSONUtils.toString(resultBean));
        return resultBean;
    }

    /**
     * 是否不能重试处方的状态
     *
     * @param status
     * @return true-不能重试  flase-可以重试
     */
    private boolean canNoRetryStatus(Integer status) {
        boolean flag;
        switch (status) {
            case RecipeStatusConstant.CHECKING_HOS:
            case RecipeStatusConstant.HIS_FAIL:
            case RecipeStatusConstant.RECIPE_MEDICAL_FAIL:
                flag = false;
                break;
            default:
                flag = true;
        }
        return flag;
    }
}
