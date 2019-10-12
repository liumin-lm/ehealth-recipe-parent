package recipe.service.common;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.employment.service.IEmploymentService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.common.service.IConsultService;
import com.ngari.consult.process.service.IRecipeOnLineConsultService;
import com.ngari.his.recipe.mode.HisCheckRecipeReqTO;
import com.ngari.his.recipe.mode.RecipeOrderItemTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DepartmentService;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeCommonBaseTO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.common.RecipeStandardReqTO;
import com.ngari.recipe.common.RecipeStandardResTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
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
import recipe.bean.CheckYsInfoBean;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.UsePathwaysFilter;
import recipe.bussutil.UsingRateFilter;
import recipe.constant.*;
import recipe.dao.*;
import recipe.hisservice.HisMqRequestInit;
import recipe.hisservice.RecipeToHisMqService;
import recipe.hisservice.RecipeToHisService;
import recipe.service.*;
import recipe.thread.PushRecipeToHisCallable;
import recipe.thread.RecipeBusiThreadPool;
import recipe.thread.SaveAutoReviewRunable;
import recipe.util.*;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
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
    private RedisClient redisClient;

    @Autowired
    private DrugsEnterpriseService drugsEnterpriseService;

    /**
     * 武昌模式签名方法
     *
     * @param recipeId
     * @param request
     * @return
     */
    @RpcService
    public RecipeStandardResTO<Map> sign(Integer recipeId, RecipeStandardReqTO request) {

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
        if (null == depId && !RecipeBussConstant.GIVEMODE_FREEDOM.equals(giveMode)&& !RecipeBussConstant.GIVEMODE_TO_HOS.equals(giveMode)) {
            response.setMsg("缺少药企编码");
            return response;
        }

        String depName = MapValueUtil.getString(conditions, "depName");
        String pharmacyCode = MapValueUtil.getString(conditions, "pharmacyCode");
        String pharmacyAddress = MapValueUtil.getString(conditions, "pharmacyAddress");
        String patientAddress = MapValueUtil.getString(conditions, "patientAddress");
        String patientTel = MapValueUtil.getString(conditions, "patientTel");
        Integer payMode = null;
        if (null != giveMode) {
            if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode)) {
                //药店取药
                if (StringUtils.isEmpty(pharmacyCode)) {
                    response.setMsg("缺少药店编码");
                    return response;
                }
                payMode = RecipeBussConstant.PAYMODE_TFDS;
            } else if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode)) {
                //配送到家
                if (StringUtils.isEmpty(patientAddress) || StringUtils.isEmpty(patientTel)) {
                    response.setMsg("配送信息不全");
                    return response;
                }
                //校验参数准确性
                if (!RegexUtils.regular(patientTel, RegexEnum.MOBILE)) {
                    response.setMsg("请输入有效手机号码");
                    return response;
                }
                if (StringUtils.length(patientAddress) > 100) {
                    response.setMsg("地址不能超过100个字");
                    return response;
                }
                payMode = RecipeBussConstant.PAYMODE_ONLINE;
            } else if (RecipeBussConstant.GIVEMODE_FREEDOM.equals(giveMode)) {
                //患者自由选择
                depId = null;
                payMode = RecipeBussConstant.PAYMODE_COMPLEX;
            } else if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(giveMode)){
                //到院取药----走九州通补充库存模式
                payMode = RecipeBussConstant.PAYMODE_TO_HOS;
                //推送处方到九州通
                //没有库存就推送九州通
                drugsEnterpriseService.pushHosInteriorSupport(dbRecipe.getRecipeId(),dbRecipe.getClinicOrgan());
                //发送患者没库存消息
                RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_HOSSUPPORT_NOINVENTORY, ObjectCopyUtils.convert(dbRecipe, Recipe.class));
                String memo = "医院保存没库存处方并推送九州通/发送无库存短信成功";
                //日志记录
                RecipeLogService.saveRecipeLog(dbRecipe.getRecipeId(), dbRecipe.getStatus(), dbRecipe.getStatus(), memo);
                //到院取药这里就应该结束了
                response.setCode(RecipeCommonBaseTO.SUCCESS);
                return response;

            } else {
                response.setMsg("缺少取药方式");
                return response;
            }
        } else {
            response.setMsg("缺少取药方式");
            return response;
        }

        //签名
        RecipeBusiThreadPool.submit(new Callable() {
            @Override
            public Object call() throws Exception {
                RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
                try {
                    //生成pdf并签名
                    recipeService.generateRecipePdfAndSign(recipeId);
                } catch (Exception e) {
                    LOG.warn("sign 签名服务异常，recipeId={}", recipeId, e);
                }
                return null;
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
        attrMap.put("payMode", payMode);
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

        //HIS同步处理
        if (!RecipeBussConstant.GIVEMODE_FREEDOM.equals(giveMode)) {
            RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
            RecipeResultBean hisResult = hisService.recipeDrugTake(recipeId, PayConstant.PAY_FLAG_NOT_PAY, null);
            //TODO HIS处理失败暂时略过
//        if (RecipeResultBean.FAIL.equals(hisResult.getCode())) {
//            LOG.warn("sign recipeId=[{}]更改取药方式失败，error={}", recipeId, hisResult.getError());
//            response.setMsg("HIS更改取药方式失败");
//            return response;
//        }
        }

        //根据配置判断是否需要人工审核, 配送到家处理在支付完成后回调 RecipeOrderService finishOrderPay
        if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode) || RecipeBussConstant.GIVEMODE_FREEDOM.equals(giveMode)) {
            Set<String> organIdList = redisClient.sMembers(CacheConstant.KEY_SKIP_YSCHECK_LIST);
            if (CollectionUtils.isNotEmpty(organIdList) && organIdList.contains(dbRecipe.getClinicOrgan().toString())) {
                RecipeCheckService checkService = ApplicationUtils.getRecipeService(RecipeCheckService.class);
                //不用发药师消息
                sendYsCheck = false;
                //跳过人工审核
                CheckYsInfoBean checkResult = new CheckYsInfoBean();
                checkResult.setRecipeId(recipeId);
                checkResult.setCheckDoctorId(dbRecipe.getDoctor());
                checkResult.setCheckOrganId(dbRecipe.getClinicOrgan());
                try {
                    checkService.autoPassForCheckYs(checkResult);
                } catch (Exception e) {
                    LOG.error("sign 药师自动审核失败. recipeId={}", recipeId);
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
     * @param recipeId
     * @param drugIds
     */
    @RpcService
    public void canOpenRecipeDrugs(Integer recipeId,List<Integer> drugIds,Integer giveMode){
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (recipe == null){
            throw new DAOException(ErrorCode.SERVICE_ERROR,"该处方不存在");
        }
        RecipeServiceSub.canOpenRecipeDrugs(recipeId,drugIds);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        boolean isSupport = false;
        //到院取药判断九州通药品是否支持
        if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(giveMode)){
            //取该机构下配置的补充库存药企
            List<DrugsEnterprise> enterpriseList = enterpriseDAO.findByOrganIdAndHosInteriorSupport(recipe.getClinicOrgan());
            for (DrugsEnterprise drugsEnterprise: enterpriseList){
                List<SaleDrugList> saleDruglists = saleDrugListDAO.findByOrganIdAndDrugIds(drugsEnterprise.getId(), drugIds);
                if (CollectionUtils.isNotEmpty(saleDruglists) && saleDruglists.size() == drugIds.size()){
                    //存在即支持
                    isSupport = true;
                    break;
                }
            }

        }else {
            //其他购药方式 药品在支付宝是否支持
            List<SaleDrugList> zfbDrug = saleDrugListDAO.findByOrganIdAndDrugIds(103, drugIds);
            if (CollectionUtils.isNotEmpty(zfbDrug) && zfbDrug.size() == drugIds.size()){
                isSupport = true;
            }
        }
        if (!isSupport){
            DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
            List<DrugList> drugList = drugListDAO.findByDrugIds(drugIds);
            //拼接不支持药品名
            String drugNames = drugList.stream().map(DrugList::getDrugName).collect(Collectors.joining(","));
            throw new DAOException(ErrorCode.SERVICE_ERROR,drugNames+"不能开具在一张处方上！");
        }

    }

    /**
     * 互联网医院项目模式-签名
     *
     * @param recipeBean
     * @param details
     * @return
     */
    @RpcService
    public Map<String, Object> doSignRecipeExt(RecipeBean recipeBean, List<RecipeDetailBean> details) {
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        PatientService patientService = BasicAPI.getService(PatientService.class);

        Map<String, Object> rMap = Maps.newHashMap();
        PatientDTO patient = patientService.get(recipeBean.getMpiid());
        //解决旧版本因为wx2.6患者身份证为null，而业务申请不成功
        if (patient == null || StringUtils.isEmpty(patient.getCertificate())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该患者还未填写身份证信息，不能开处方");
        }
        // 就诊人改造：为了确保删除就诊人后历史处方不会丢失，加入主账号用户id
        PatientDTO requestPatient = patientService.getOwnPatientForOtherProject(patient.getLoginId());
        if (null != requestPatient && null != requestPatient.getMpiId()) {
            recipeBean.setRequestMpiId(requestPatient.getMpiId());
            // urt用于系统消息推送
            recipeBean.setRequestUrt(requestPatient.getUrt());
        }
        recipeBean.setStatus(RecipeStatusConstant.UNSIGN);
        recipeBean.setSignDate(DateTime.now().toDate());
        recipeBean.setRecipeMode(RecipeBussConstant.RECIPEMODE_ZJJGPT);
        Integer recipeId = recipeBean.getRecipeId();

        //生成处方编号，不需要通过HIS去产生
        String recipeCodeStr = "ngari" + DigestUtil.md5For16(recipeBean.getClinicOrgan() +
                recipeBean.getMpiid() + Calendar.getInstance().getTimeInMillis());
        recipeBean.setRecipeCode(recipeCodeStr);
        //根据申请人mpiid，requestMode 获取当前咨询单consultId
        IConsultService iConsultService = ApplicationUtils.getConsultService(IConsultService.class);
        List<Integer> consultIds = iConsultService.findApplyingConsultByRequestMpiAndDoctorId(recipeBean.getRequestMpiId(),
                recipeBean.getDoctor(), RecipeSystemConstant.CONSULT_TYPE_RECIPE);
        Integer consultId = null;
        if (CollectionUtils.isNotEmpty(consultIds)) {
            consultId = consultIds.get(0);
            recipeBean.setClinicId(consultId);
            rMap.put("consultId", consultId);
        }
        //如果是已经暂存过的处方单，要去数据库取状态 判断能不能进行签名操作
        if (null != recipeId && recipeId > 0) {
            Integer status = recipeDAO.getStatusByRecipeId(recipeId);
            if (null == status || status > RecipeStatusConstant.UNSIGN) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "处方单已处理,不能重复签名");
            }
            recipeService.updateRecipeAndDetail(recipeBean, details);
        } else {
            recipeId = recipeService.saveRecipeData(recipeBean, details);
            recipeBean.setRecipeId(recipeId);
        }
        rMap.put("recipeId", recipeId);


        //判断机构是否需要his处方检查
        Set<String> organIdList = redisClient.sMembers(CacheConstant.KEY_HIS_CHECK_LIST);
        if(CollectionUtils.isNotEmpty(organIdList) && organIdList.contains(recipeBean.getClinicOrgan().toString())){
            RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
            boolean b = hisService.hisRecipeCheck(rMap, recipeBean);
            if (!b){
                return rMap;
            }

        }
        //更新审方信息
        RecipeBusiThreadPool.execute(new SaveAutoReviewRunable(recipeBean, details));
        recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.CHECKING_HOS, null);
        rMap.put("signResult", true);
        rMap.put("errorFlag", false);

        //发送HIS处方开具消息
        sendRecipeToHIS(recipeBean);
        //处方开完后发送聊天界面消息 -医院确认中
        if(null != consultId){
            try {
                IRecipeOnLineConsultService recipeOnLineConsultService = ConsultAPI.getService(IRecipeOnLineConsultService.class);
                recipeOnLineConsultService.sendRecipeMsg(consultId,2);
            } catch (Exception e){
                LOG.error("doSignRecipeExt sendRecipeMsg error, type:2, consultId:{}, error:{}", consultId,e);
            }

        }
        LOG.info("doSignRecipeExt execute ok! result={}", JSONUtils.toString(rMap));
        return rMap;
    }

    private void sendRecipeToHIS(RecipeBean recipeBean) {
        //可通过缓存控制是互联网方式发送处方(his来查)还是平台模式发送处方(平台推送)
        Set<String> organIdList = redisClient.sMembers(CacheConstant.KEY_NGARI_SENDRECIPETOHIS_LIST);
        if(CollectionUtils.isNotEmpty(organIdList) && organIdList.contains(recipeBean.getClinicOrgan().toString())){
            //推送处方给his---recipesend
            RecipeBusiThreadPool.submit(new PushRecipeToHisCallable(recipeBean.getRecipeId()));
        }else {
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
    public Map<String, Object> continueSignAfterCheckFailed (RecipeBean recipeBean, List<RecipeDetailBean> details) {
        Map<String, Object> rMap = Maps.newHashMap();
        Integer recipeId = recipeBean.getRecipeId();
        RecipeDAO dao = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = dao.getByRecipeId(recipeId);
        RecipeBean recipeBeanDb = ObjectCopyUtils.convert(recipe, RecipeBean.class);

        //更新审方信息
        RecipeBusiThreadPool.execute(new SaveAutoReviewRunable(recipeBeanDb, details));
        recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.CHECKING_HOS, ImmutableMap.of("distributionFlag", 1));

        //发送HIS处方开具消息
        sendRecipeToHIS(recipeBean);

        LOG.info("continueSignAfterCheckFailed execute ok! recipeId={}",recipeId);
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
        if (null == dbRecipe || dbRecipe.getStatus() != RecipeStatusConstant.CHECKING_HOS) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方不能重试");
        }

        //发送HIS处方开具消息
        RecipeToHisMqService hisMqService = ApplicationUtils.getRecipeService(RecipeToHisMqService.class);
        RecipeBean recipeBean = ObjectCopyUtils.convert(dbRecipe, RecipeBean.class);
        hisMqService.recipeStatusToHis(HisMqRequestInit.initRecipeStatusToHisReq(recipeBean,
                HisBussConstant.TOHIS_RECIPE_STATUS_ADD));

        LOG.info("sendNewRecipeToHIS execute ok! result={}", JSONUtils.toString(resultBean));
        return resultBean;
    }

}
