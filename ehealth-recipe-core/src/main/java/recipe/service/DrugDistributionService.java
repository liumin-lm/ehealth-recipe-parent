package recipe.service;

import com.alijk.bqhospital.alijk.conf.TaobaoConf;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.ngari.base.employment.model.EmploymentBean;
import com.ngari.base.employment.service.IEmploymentService;
import com.ngari.base.sysparamter.service.ISysParamterService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.UpdateTakeDrugWayReqTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.common.utils.VerifyUtils;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import com.taobao.api.response.AlibabaAlihealthRxPrescriptionGetResponse;
import ctd.account.UserRoleToken;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.base.constant.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.DeptOrderDTO;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.PurchaseRequest;
import recipe.bean.PurchaseResponse;
import recipe.common.CommonConstant;
import recipe.common.ResponseUtils;
import recipe.constant.CacheConstant;
import recipe.constant.ParameterConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.*;
import recipe.drugsenterprise.AccessDrugEnterpriseService;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.hisservice.RecipeToHisService;
import recipe.third.IWXServiceInterface;
import recipe.util.RedisClient;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author： 0184/yu_yun
 * @date： 2019/3/2
 * @description： 药品配送服务
 * @version： 1.0
 */
@RpcBean(value = "drugDistributionService")
public class DrugDistributionService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DrugDistributionService.class);

    @Autowired
    private RedisClient redisClient;
    @Autowired
    private TaobaoConf taobaoConf;

    /**
     * 是否需要进行淘宝授权，暂不考虑session过期问题
     *
     * @return
     */
    @RpcService
    public boolean authorization(String loginId) {
        // 从redis获取token 判断是否需要授权信息
        String session = redisClient.get(CacheConstant.KEY_DEPT_ALI_SESSION+loginId);
        if (StringUtils.isNotEmpty(session)) {
            return true;
        }

        return false;
    }

    /**
     * 互联网-购药按钮
     * <p>
     * 成功返回：{"code":"000"}，前端不处理
     * 失败返回：{"code":"001"}，前端提示 msg 的内容
     * 需要授权返回：{"code":"002"}，前端根据 authUrl 跳转地址
     * 展示订单列表返回：{"code":"003"}，前端根据 orderList 展示详情
     * 需要跳转下单返回：{"code":"004"}，前端根据 orderUrl 跳转下单页
     * 到院取药成功：{"code":"005"}，弹出MSG信息
     *
     * @param request
     */
    @RpcService
    public PurchaseResponse purchase(PurchaseRequest request) {
        LOGGER.info("purchase req={}", JSONUtils.toString(request));
        //默认通知his取药方式,3-未知，2-医院取药，1-物流配送，3-药店取药
        String deliveryType = "3";
        String val = redisClient.get(CacheConstant.KEY_SWITCH_PURCHASE_ON);
        PurchaseResponse response = ResponseUtils.getFailResponse(PurchaseResponse.class, "");
        if (StringUtils.isEmpty(val) || "false".equals(val)) {
            LOGGER.warn("purchase 处方未开通购药服务. cachevalue={}", val);
            response.setMsg("处方未开通购药服务");
            return response;
        }

        //校验参数
        try {
            Multimap<String, String> detailVerifyMap = VerifyUtils.verify(request);
            if (!detailVerifyMap.keySet().isEmpty()) {
                response.setMsg(detailVerifyMap.toString());
                return response;
            }
        } catch (Exception e) {
            LOGGER.error("purchase 请求对象异常数据. PurchaseRequest={}", JSONUtils.toString(request), e);
            response.setMsg("请求对象异常数据");
            return response;
        }

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(request.getRecipeId());
        if (null == recipe) {
            response.setMsg("处方不存在");
            return response;
        }
        if (recipe.getStatus() == RecipeStatusConstant.REVOKE){
            throw new DAOException(eh.base.constant.ErrorCode.SERVICE_ERROR, "处方单已被撤销");
        }

        //根据药企ID获取药企信息
        DrugsEnterprise drugsEnterprise = null;
        if(null == request.getDepId()){
            OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
            List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(recipe.getClinicOrgan(), 1);
            drugsEnterprise = drugsEnterprises.get(0);
        } else {
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            drugsEnterprise = drugsEnterpriseDAO.get(request.getDepId());
        }

        if (null == drugsEnterprise) {
            LOGGER.warn("purchase aldyf 药企不存在");
            response.setMsg("该处方无法配送");
            return response;
        }


        String loginId = UserRoleToken.getCurrent().getUserId();

        if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(request.getType()) ||
            RecipeBussConstant.GIVEMODE_TFDS.equals(request.getType())) {

            //未授权且发起方式为 到院取药 之外的方式需要进行授权操作(天猫大药房除外)
            if (!"tmdyf".equals(drugsEnterprise.getAccount()) && !authorization(loginId)) {
                response.setCode(PurchaseResponse.AUTHORIZATION);
                response.setMsg("用户需要鉴权");
                try {
                    ISysParamterService iSysParamterService = ApplicationUtils.getBaseService(ISysParamterService.class);
                    IWXServiceInterface wxService = AppContextHolder.getBean("wx.wxService", IWXServiceInterface.class);

                    //配置回调地址
                    String param = iSysParamterService.getParam(ParameterConstant.KEY_TAOBAO_AUTHORIZATION_ADDR, null);
                    response.setAuthUrl(MessageFormat.format(param, taobaoConf.getAppkey(),
                        wxService.urlJoin()+"/taobao/callBack_code", loginId+"$"+request.getRecipeId()+"$"+request.getAppId()));
                    LOGGER.info("DrugDistributionService.purchase AuthUrl:{}.", response.getAuthUrl());
                } catch (Exception e) {
                    LOGGER.error("purchase 组装授权页出错. loginId={}", loginId, e);
                    response.setCode(CommonConstant.FAIL);
                    response.setMsg("跳转授权页面失败");
                }
                return response;
            }

            if(RecipeBussConstant.GIVEMODE_TFDS.equals(request.getType())){
                deliveryType = "2";
            } else {
                deliveryType = "1";
            }
            if (1 == recipe.getChooseFlag() && RecipeBussConstant.GIVEMODE_SEND_TO_HOME == recipe.getGiveMode()) {
                //已使用到院取药方式
                response.setMsg("该处方单已使用，无法再次使用哦！");
                return response;
            }

            //判断处方能否购买
            SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            //date 20200921 修改【his管理的药企】不用校验配送药品，由预校验结果
            if(new Integer(0).equals(RecipeServiceSub.getOrganEnterprisesDockType(recipe.getClinicOrgan()))){
                List<Integer> drugIdList = detailDAO.findDrugIdByRecipeId(request.getRecipeId());
                Long count = saleDrugListDAO.getCountByOrganIdAndDrugIds(drugsEnterprise.getId(), drugIdList);
                if (count.intValue() != drugIdList.size()) {
                    LOGGER.warn("purchase aldyf saleDrugList药品存货不足，无法购药. drugIdList={}", JSONUtils.toString(drugIdList));
                    response.setMsg("药品存货不足，无法购药");
                    return response;
                }
            }

            recipe.setGiveMode(request.getType());
//            DrugEnterpriseResult result = queryPrescription(recipe.getRecipeCode(), drugsEnterprise);

            RemoteDrugEnterpriseService remoteDrugEnterpriseService =
                ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);

            //根据药企ID获取具体跳转的url地址
            AccessDrugEnterpriseService remoteService = remoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
            remoteService.getJumpUrl(response, recipe, drugsEnterprise);
            if(PurchaseResponse.ORDER.equals(response.getCode())){
                //更新平台处方
                recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), ImmutableMap.of("giveMode", request.getType()));
            }

        } else if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(request.getType())) {
            if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
                response.setMsg("该处方单已使用，无法再次使用哦！");
                return response;
            }
            deliveryType = "0";
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(request.getRecipeId());
            if (!("HdVirtualdyf".equals(drugsEnterprise.getAccount()) ||
                    "hzInternet".equals(drugsEnterprise.getAccount()))){
                if (recipeExtend == null
                        || StringUtils.isEmpty(recipeExtend.getCardTypeName())
                        || StringUtils.isEmpty(recipeExtend.getCardNo())) {
                    response.setMsg("无就诊卡信息,无法医院取药");
                    return response;
                }
            }
            //说明处方没有其他途径购买的情况
            if (1 == recipe.getChooseFlag() && RecipeBussConstant.GIVEMODE_TO_HOS == recipe.getGiveMode()) {
                if ("HdVirtualdyf".equals(drugsEnterprise.getAccount()) ||
                        "hzInternet".equals(drugsEnterprise.getAccount())){
                    getMedicalMsg(response, recipe);
                } else {
                    response.setMsg("请携带就诊卡 " + recipeExtend.getCardNo());
                }
                response.setCode(PurchaseResponse.TO_HOS_SUCCESS);
                return response;
            } else {
                //已授权的情况下需要去系统查询处方使用状态
                if(authorization(loginId) && "aldyf".equals(drugsEnterprise.getAccount())) {
                    DrugEnterpriseResult result = queryPrescription(recipe.getRecipeCode(), drugsEnterprise);
                    if (null == result.getObject()) {
                        //说明处方获取失败
                        LOGGER.warn("purchase queryPrescription error. recipeId={}", request.getRecipeId());
                        response.setMsg("");
                        //没有处方进行处方推送
                        RemoteDrugEnterpriseService remoteDrugEnterpriseService =
                                ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
                        remoteDrugEnterpriseService.pushSingleRecipeInfoWithDepId(request.getRecipeId(), drugsEnterprise.getId());
                        LOGGER.info("purchase 到院取药发起推送. recipeId={}", request.getRecipeId());
                        PurchaseResponse subResponse = purchase(request);
                        return subResponse;
                    }
                    AlibabaAlihealthRxPrescriptionGetResponse aliResponse = (AlibabaAlihealthRxPrescriptionGetResponse) result.getObject();
                    AlibabaAlihealthRxPrescriptionGetResponse.RxPrescription rxPrescription = aliResponse.getModel();
                    if (null != rxPrescription && rxPrescription.getUsable()) {
                        //该处方也未被药企使用，可以到院取药
                        response.setMsg("请携带就诊卡 " + recipeExtend.getCardNo());
                        response.setCode(CommonConstant.SUCCESS);
                    } else {
                        LOGGER.info("purchase recipe use. recipeId={}", request.getRecipeId());
                        response.setMsg("该处方单已使用，无法再次使用哦！");
                        return response;
                    }
                }else{
                    if (!("HdVirtualdyf".equals(drugsEnterprise.getAccount()) ||
                            "hzInternet".equals(drugsEnterprise.getAccount()))){
                        //该处方未推送到药企，可以到院取药
                        response.setMsg("请携带就诊卡 " + recipeExtend.getCardNo());
                        response.setCode(CommonConstant.SUCCESS);
                    } else {
                        getMedicalMsg(response, recipe);
                    }

                }
            }
        }
        LOGGER.info("response:{}.", JSONUtils.toString(response));
        //取药方式进行HIS推送
        if (CommonConstant.SUCCESS.equals(response.getCode())) {
            RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
            OrganService organService = BasicAPI.getService(OrganService.class);
            try{
                UpdateTakeDrugWayReqTO updateTakeDrugWayReqTO = new UpdateTakeDrugWayReqTO();
                updateTakeDrugWayReqTO.setClinicOrgan(recipe.getClinicOrgan());
                updateTakeDrugWayReqTO.setRecipeID(recipe.getRecipeCode());
                updateTakeDrugWayReqTO.setOrganID(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));
                if (recipe.getClinicId() != null) {
                    updateTakeDrugWayReqTO.setClinicID(recipe.getClinicId().toString());
                }
                //患者信息处理
                PatientService patientService = BasicAPI.getService(PatientService.class);
                PatientDTO patient = patientService.get(recipe.getMpiid());
                if (patient == null){
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "平台查询不到患者信息");
                }
                //患者信息
                PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
                patientBaseInfo.setCertificateType(patient.getCertificateType());
                patientBaseInfo.setCertificate(patient.getCertificate());
                patientBaseInfo.setPatientName(patient.getPatientName());
                patientBaseInfo.setPatientID(recipe.getPatientID());
                updateTakeDrugWayReqTO.setPatientBaseInfo(patientBaseInfo);
                //取药方式
                updateTakeDrugWayReqTO.setDeliveryType(deliveryType);
                //审方药师工号和姓名
                if (recipe.getChecker()!=null){
                    IEmploymentService iEmploymentService = ApplicationUtils.getBaseService(IEmploymentService.class);
                    EmploymentBean primaryEmp = iEmploymentService.getPrimaryEmpByDoctorId(recipe.getChecker());
                    if (primaryEmp != null){
                        updateTakeDrugWayReqTO.setCheckerId(primaryEmp.getJobNumber());
                    }
                    DoctorService doctorService = BasicAPI.getService(DoctorService.class);
                    DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getChecker());
                    if (doctorDTO!=null){
                        updateTakeDrugWayReqTO.setCheckerName(doctorDTO.getName());
                    }
                }
                //处方总金额
                updateTakeDrugWayReqTO.setPayment(recipe.getActualPrice());
                //支付状态-这里默认未支付
                updateTakeDrugWayReqTO.setPayFlag(0);
                //支付方式
                if ("0".equals(deliveryType)) {
                    updateTakeDrugWayReqTO.setPayMode(RecipeBussConstant.PAYMODE_TO_HOS.toString());
                } else if ("1".equals(deliveryType)) {
                    updateTakeDrugWayReqTO.setPayMode(RecipeBussConstant.PAYMODE_ONLINE.toString());
                } else {
                    updateTakeDrugWayReqTO.setPayMode(RecipeBussConstant.PAYMODE_TFDS.toString());
                }
                HisResponseTO hisResult = service.updateTakeDrugWay(updateTakeDrugWayReqTO);
                //更新平台处方
                recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), ImmutableMap.of("giveMode", request.getType(), "chooseFlag", 1));

                response.setCode(PurchaseResponse.TO_HOS_SUCCESS);
                LOGGER.info("取药方式更新通知his. param={},result={}", JSONUtils.toString(updateTakeDrugWayReqTO), JSONUtils.toString(hisResult));
            }catch (Exception e){
                LOGGER.error("取药方式更新 error ",e);
            }
        }
        return response;
    }

    private void getMedicalMsg(PurchaseResponse response, Recipe recipe) {
        if (RecipeStatusConstant.CHECK_PASS == recipe.getStatus()) {
            Integer consultId = recipe.getClinicId();
            Integer medicalFlag = 0;
            IRevisitExService consultExService = RevisitAPI.getService(IRevisitExService.class);
            if (consultId != null) {
                RevisitExDTO consultExDTO = consultExService.getByConsultId(consultId);
                if (consultExDTO != null) {
                    medicalFlag = consultExDTO.getMedicalFlag();
                }
            }
            RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
            String tips ;
            if (1 == medicalFlag) {
                tips = "您是医保病人，请到医院支付取药，医院取药窗口取药：";
            } else {
                tips = "请到医院支付取药，医院取药窗口：";
            }
            OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());

            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());


            if (!Objects.isNull(recipeExtend) && StringUtils.isNotEmpty(recipeExtend.getPharmNo())) {
                String pharmNo = recipeExtend.getPharmNo();
                if(StringUtils.isNotEmpty(pharmNo)){
                    tips += "["+ organDTO.getName() + "" + pharmNo + "取药窗口]";
                }else {
                    tips += "["+ organDTO.getName() + "取药窗口]";
                }
            }
//            List<Recipedetail> detailList = detailDAO.findByRecipeId(recipe.getRecipeId());
//            if(CollectionUtils.isNotEmpty(detailList)){
//                String pharmNo = detailList.get(0).getPharmNo();
//                if(StringUtils.isNotEmpty(pharmNo)){
//                    tips += "["+ organDTO.getName() + "" + pharmNo + "取药窗口]";
//                }else {
//                    tips += "["+ organDTO.getName() + "取药窗口]";
//                }
//            }
            response.setMsg(tips);
            response.setCode(CommonConstant.SUCCESS);
        }
    }

    /**
     * 查询药企内处方
     *
     * @param recipeCode
     * @param drugsEnterprise
     * @return
     */
    private DrugEnterpriseResult queryPrescription(String recipeCode, DrugsEnterprise drugsEnterprise) {
        //查询处方，返回处方不存在，则进行推送操作
        RemoteDrugEnterpriseService remoteDrugEnterpriseService =
                ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        AccessDrugEnterpriseService drugEnterpriseService = remoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
        return drugEnterpriseService.queryPrescription(recipeCode, true);
    }

    @RpcService
    public void updatehisdrug(Integer recipeId,String deliveryType) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
        OrganService organService = BasicAPI.getService(OrganService.class);
        UpdateTakeDrugWayReqTO updateTakeDrugWayReqTO = new UpdateTakeDrugWayReqTO();
        updateTakeDrugWayReqTO.setClinicOrgan(recipe.getClinicOrgan());
        updateTakeDrugWayReqTO.setRecipeID(recipe.getRecipeCode());
        updateTakeDrugWayReqTO.setOrganID(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));
        if (recipe.getClinicId() != null) {
            updateTakeDrugWayReqTO.setClinicID(recipe.getClinicId().toString());
        }
        updateTakeDrugWayReqTO.setDeliveryType(deliveryType);
        HisResponseTO hisResult = service.updateTakeDrugWay(updateTakeDrugWayReqTO);
        LOGGER.info("取药方式更新通知his. param={},result={}", JSONUtils.toString(updateTakeDrugWayReqTO), JSONUtils.toString(hisResult));
    }


    /**
     *  查看订单
     * @param request
     * @return
     */
    @RpcService
    public PurchaseResponse findOrder(PurchaseRequest request) {
        PurchaseResponse response = ResponseUtils.getFailResponse(PurchaseResponse.class, "");
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(request.getRecipeId());
        if (null == recipe) {
            response.setMsg("处方不存在");
            return response;
        }
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getByAccount("aldyf");
        if (null == drugsEnterprise) {
            LOGGER.warn("purchase aldyf 药企不存在");
            response.setMsg("aldyf 药企不存在");
            return response;
        }
        DrugEnterpriseResult result = queryPrescription(recipe.getRecipeCode(), drugsEnterprise);
        if (null == result.getObject()) {
            response.setMsg("查询不到处方订单");
            return response;
        }
        AlibabaAlihealthRxPrescriptionGetResponse aliResponse = (AlibabaAlihealthRxPrescriptionGetResponse) result.getObject();
        AlibabaAlihealthRxPrescriptionGetResponse.RxPrescription rxPrescription = aliResponse.getModel();
        if (null != rxPrescription) {
            if (!rxPrescription.getUsable()) {
                //已使用处方展示订单信息
                List<AlibabaAlihealthRxPrescriptionGetResponse.RxOrderInfo> rxOrderInfoList = rxPrescription.getRxOrderList();
                if (CollectionUtils.isNotEmpty(rxOrderInfoList)) {
                    List<DeptOrderDTO> deptOrderDTOList = new ArrayList<>(rxOrderInfoList.size());
                    DeptOrderDTO deptOrderDTO;
                    for (AlibabaAlihealthRxPrescriptionGetResponse.RxOrderInfo rxOrderInfo : rxOrderInfoList) {
                        deptOrderDTO = new DeptOrderDTO();
                        deptOrderDTO.setOrderCode(rxOrderInfo.getBizOrderId());
                        deptOrderDTO.setStatus(rxOrderInfo.getStatus());
                        deptOrderDTO.setOrderDetailUrl(rxOrderInfo.getBizOrderDetailUrl());
                        deptOrderDTOList.add(deptOrderDTO);
                    }
                    response.setOrderList(deptOrderDTOList);
                    response.setCode(PurchaseResponse.ORDER_DETAIL);
                    return response;
                }
            }
        }
        return response;
    }
}
