package recipe.drugsenterprise;

import com.alibaba.fastjson.JSON;
import com.alijk.bqhospital.alijk.conf.TaobaoConf;
import com.google.common.collect.ImmutableMap;
import com.ngari.base.BaseAPI;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.patient.dto.*;
import com.ngari.patient.service.*;
import com.ngari.recipe.entity.*;
import com.qimencloud.api.sceneqimen.request.AlibabaAlihealthPrescriptionStatusSyncRequest;
import com.qimencloud.api.sceneqimen.response.AlibabaAlihealthPrescriptionStatusSyncResponse;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.*;
import com.taobao.api.response.*;
import ctd.account.UserRoleToken;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.utils.DateConversion;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.PurchaseResponse;
import recipe.constant.DrugEnterpriseConstant;
import recipe.constant.ParameterConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.SaleDrugListDAO;
import recipe.drugsenterprise.bean.StandardResultDTO;
import recipe.drugsenterprise.bean.StandardStateDTO;
import recipe.service.common.RecipeCacheService;
import java.text.SimpleDateFormat;
import java.util.*;

import static ctd.util.AppContextHolder.getBean;

/**
 * @description 天猫大药房对接服务
 * @author gmw
 * @date 2019/9/11
 */
@RpcBean("tmdyfRemoteService")
public class TmdyfRemoteService extends AccessDrugEnterpriseService{

    private static final Logger LOGGER = LoggerFactory.getLogger(TmdyfRemoteService.class);

    private static final String EXPIRE_TIP = "请重新授权";

    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    @Autowired
    private TaobaoConf taobaoConf;

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("TmdyfRemoteService tokenUpdateImpl not implement.");
    }

    /*
     * @description 获取天猫大药房的跳转url并且发送处方
     * @author gmw
     * @date 2019/9/12
     * @param
     * @return
     */
    @Override
    public void getJumpUrl(PurchaseResponse response, Recipe recipe, DrugsEnterprise drugsEnterprise) {
        LOGGER.info("获取跳转地址开始，处方ID：{}.", recipe.getRecipeId());
        RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);
        String url = cacheService.getRecipeParam(ParameterConstant.KEY_ALI_O2O_ADDR, null);
        if(url == null){
            LOGGER.warn("未获取O2O跳转url,请检查数据库配置");
            response.setMsg("未获取O2O跳转url,请检查数据库配置");
            return ;
        }

        // 查看取药信息url
        if(RecipeStatusConstant.USING == recipe.getStatus() || RecipeStatusConstant.FINISH == recipe.getStatus()){
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            if(null == recipeExtend.getRxNo()){
                LOGGER.warn("无法获取天猫对应处方编码");
                response.setMsg("无法获取天猫对应处方编码");
                return ;
            }
            url = url + "rxNo=" + recipeExtend.getRxNo() +"&action=getDrugInfo";
            response.setCode(PurchaseResponse.JUMP);
        } else {
            if (0 == recipe.getPushFlag()) {
                //处方未推送，进行处方推送
                DrugEnterpriseResult result = pushRecipeInfo(Collections.singletonList(recipe.getRecipeId()), drugsEnterprise.getId());
                if(DrugEnterpriseResult.FAIL == result.getCode()){
                    LOGGER.warn("该处方无法配送--" + result.getMsg(), recipe.getRecipeId());
                    response.setMsg(PurchaseResponse.CHECKWARN);
                    response.setMsg(result.getMsg());
                    return;
                }
            }

            //获取医院城市编码（6位）
            OrganService organService = BasicAPI.getService(OrganService.class);
            OrganDTO organCode = organService.getByOrganId(recipe.getClinicOrgan());
            String cityCode = null;
            if(organCode.getAddrArea().length() >= 4){
                cityCode = organCode.getAddrArea().substring(0,4) + "00";
            } else {
                cityCode = organCode.getAddrArea() + "00";
                for(int i = cityCode.length(); i<6; i++){
                    cityCode = cityCode + "0";
                }
            }

            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            if(null == recipeExtend.getRxNo()){
                LOGGER.warn("无法获取天猫对应处方编码");
                response.setMsg("无法获取天猫对应处方编码");
                return ;
            }

            if(RecipeBussConstant.GIVEMODE_SEND_TO_HOME == recipe.getGiveMode()){
                //配送到家URL
                url = url + "rxNo=" + recipeExtend.getRxNo() +"&action=o2o&cityCode=" + cityCode;
            } else {
                //药店取药取药URL
                url = url + "rxNo=" + recipeExtend.getRxNo() +"&action=offline&cityCode=" + cityCode;
            }
            response.setCode(PurchaseResponse.ORDER);
        }

        response.setOrderUrl(url);
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        return null;
    }

    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, Integer depId) {

        LOGGER.info("推送处方至天猫大药房开始，处方ID：{}.", JSONUtils.toString(recipeIds));
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (ObjectUtils.isEmpty(recipeIds)) {
            getDrugEnterpriseResult(result, "处方ID参数为空");
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
//        Integer depId = enterprise.getId();

        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);

        if (!ObjectUtils.isEmpty(recipeList)) {
            PatientService patientService = BasicAPI.getService(PatientService.class);
            DoctorService doctorService = BasicAPI.getService(DoctorService.class);
            EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
            DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
//            ICurrentUserInfoService userInfoService = BasicAPI.getService(ICurrentUserInfoService.class);

            ICurrentUserInfoService userInfoService = AppContextHolder.getBean(
                "eh.remoteCurrentUserInfoService", ICurrentUserInfoService.class);
            for (Recipe dbRecipe : recipeList ) {
//                String loginId = patientService.getLoginIdByMpiId(dbRecipe.getRequestMpiId());
//                String accessToken = aldyfRedisService.getTaobaoAccessToken(loginId);
//                if (ObjectUtils.isEmpty(accessToken)) {
//                    return getDrugEnterpriseResult(result, EXPIRE_TIP);
//                }
//                alihealthHospitalService.setTopSessionKey(accessToken);
//                LOGGER.info("获取到accessToken:{}, loginId:{}", accessToken, loginId);


                AlibabaAlihealthOutflowPrescriptionCreateRequest request = new AlibabaAlihealthOutflowPrescriptionCreateRequest ();
                AlibabaAlihealthOutflowPrescriptionCreateRequest.PrescriptionOutflowUpdateRequest requestParam = new AlibabaAlihealthOutflowPrescriptionCreateRequest.PrescriptionOutflowUpdateRequest ();


                //操作人手机号
                PatientDTO patient2 = UserRoleToken.getCurrent().getProperty("patient", PatientDTO.class);
                if(null != patient2.getMobile()){
                    requestParam.setMobilePhone(patient2.getMobile());
                } else {
                    return getDrugEnterpriseResult(result, "操作人手机号不能为空");
                }

                //操作人支付宝user_id
                String openId = userInfoService.getSimpleWxAccount().getOpenId();
                if(null != openId){
                    requestParam.setAlipayUserId(openId);
                } else {
                    return getDrugEnterpriseResult(result, "操作人支付宝user_id不能为空");
                }

                //获取患者信息
                PatientDTO patient = patientService.get(dbRecipe.getMpiid());

                if (!ObjectUtils.isEmpty(patient)) {
                    int patientAge = patient.getBirthday() == null ? 0 : DateConversion
                            .getAge(patient.getBirthday());
                    requestParam.setPatientId(dbRecipe.getMpiid());
                    if(null != patient.getPatientName()){
                        requestParam.setPatientName(patient.getPatientName());
                    } else {
                        return getDrugEnterpriseResult(result, "患者姓名不能为空");
                    }

                    requestParam.setAge(patientAge+"");
                    if(null != patient.getPatientSex()){
                        try {
                            requestParam.setSex(DictionaryController.instance().get("eh.base.dictionary.Gender").getText(patient.getPatientSex()));
                        } catch (Exception e) {
                            return getDrugEnterpriseResult(result, "获取患者性别异常");
                        }
                    } else {
                        return getDrugEnterpriseResult(result, "患者性别不能为空");
                    }
                    requestParam.setAddress(patient.getAddress());

//                  requestParam.setIdNumber(patient.getIdcard());
                } else {
                    return getDrugEnterpriseResult(result, "患者不存在");
                }
                //获取处方信息
                requestParam.setVisitId(dbRecipe.getRecipeId() + "");

                //获取医生信息
                DoctorDTO doctor = doctorService.get(dbRecipe.getDoctor());
                if (!ObjectUtils.isEmpty(doctor)) {
                    requestParam.setDoctorId(doctor.getDoctorId() + "");
                    if(null != patient.getPatientName()){
                        requestParam.setDoctorName(doctor.getName());
                    } else {
                        return getDrugEnterpriseResult(result, "就诊医生姓名不能为空");
                    }
                    //科室信息
                    EmploymentDTO employment = employmentService.getPrimaryEmpByDoctorId(dbRecipe.getDoctor());
                    if (!ObjectUtils.isEmpty(employment)) {
                        Integer departmentId = employment.getDepartment();
                        DepartmentDTO departmentDTO = departmentService.get(departmentId);
                        if (!ObjectUtils.isEmpty(departmentDTO)) {
                            requestParam.setDetpId(departmentDTO.getDeptId() + "");
                            requestParam.setDeptName(StringUtils.isEmpty(departmentDTO.getName())?"全科":departmentDTO.getName());
                        } else {
                            return getDrugEnterpriseResult(result, "医生主执业点不存在");
                        }
                    } else {
                        return getDrugEnterpriseResult(result, "医生主执业点不存在");
                    }
                } else {
                    return getDrugEnterpriseResult(result, "医生不存在");
                }

                //DiagnosticParam 患者主诉
                RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(dbRecipe.getRecipeId());
                if(null != recipeExtend){
                    requestParam.setMainTell(recipeExtend.getMainDieaseDescribe());   //患者主诉
                    requestParam.setProblemNow(recipeExtend.getHistoryOfPresentIllness());   //现病史
                    requestParam.setBodyCheck(recipeExtend.getPhysicalCheck());   //一般检查
                }
                requestParam.setDoctorAdvice(dbRecipe.getMemo());               //医生嘱言
                requestParam.setPlatformCode("ZJSPT");
                //封装诊断信息
                if(null != dbRecipe.getOrganDiseaseId() && null != dbRecipe.getOrganDiseaseId()){
                    List<AlibabaAlihealthOutflowPrescriptionCreateRequest.Diagnose> diagnose = new ArrayList<>();
                    String [] diseaseIds = dbRecipe.getOrganDiseaseId().split(",");
                    String [] diseaseNames = dbRecipe.getOrganDiseaseName().split(",");
                    for (int i = 0; i < diseaseIds.length; i++) {
                        AlibabaAlihealthOutflowPrescriptionCreateRequest.Diagnose disease = new AlibabaAlihealthOutflowPrescriptionCreateRequest.Diagnose ();
                        disease.setIcdCode(diseaseIds[i]);
                        disease.setIcdName(diseaseNames[i]);
                        diagnose.add(disease);
                    }
                    requestParam.setDiagnoses(diagnose);
                } else {
                    return getDrugEnterpriseResult(result, "诊断信息不能为空");
                }

                //药品详情
                RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
                List<Recipedetail> detailList = detailDAO.findByRecipeId(dbRecipe.getRecipeId());
                List<AlibabaAlihealthOutflowPrescriptionCreateRequest.Drugs> drugParams = new ArrayList<>();
                if (!ObjectUtils.isEmpty(detailList)) {
                    SaleDrugListDAO saleDrugDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
                    for (int i = 0; i < detailList.size(); i++) {
                        //一张处方单可能包含相同的药品purchaseService
                        SaleDrugList saleDrugList = saleDrugDAO.getByDrugIdAndOrganId(detailList.get(i).getDrugId(), depId);
                        if (ObjectUtils.isEmpty(saleDrugList)) {
                            return getDrugEnterpriseResult(result, "未找到对应的saleDrugList");
                        }
                        AlibabaAlihealthOutflowPrescriptionCreateRequest.Drugs drugParam = new AlibabaAlihealthOutflowPrescriptionCreateRequest.Drugs();
                        if(null != saleDrugList.getDrugSpec()){
                            drugParam.setSpec(saleDrugList.getDrugSpec());        //药品规格
                        } else {
                            return getDrugEnterpriseResult(result, "药品规格不能为空");
                        }
                        if(null != detailList.get(i).getUseTotalDose()){
                            drugParam.setTotal(detailList.get(i).getUseTotalDose() + "");    //药品数量
                        } else {
                            return getDrugEnterpriseResult(result, "药品数量不能为空");
                        }
                        drugParam.setDrugName(saleDrugList.getSaleName());    //药品名称
                        if(null != detailList.get(i).getUseDose()){
                            drugParam.setDose(detailList.get(i).getUseDose() + "");    //用量
                        } else {
                            return getDrugEnterpriseResult(result, "药品用量不能为空");
                        }
                        if(null != saleDrugList.getDrugName()){
                            drugParam.setDrugCommonName(saleDrugList.getDrugName());  //药品通用名称
                        } else {
                            return getDrugEnterpriseResult(result, "药品通用名称不能为空");
                        }
                        if(null != detailList.get(i).getUseDoseUnit()){
                            drugParam.setDoseUnit(detailList.get(i).getUseDoseUnit());      //用量单位
                        } else {
                            return getDrugEnterpriseResult(result, "用量单位不能为空");
                        }
                        drugParam.setDrugId(saleDrugList.getOrganDrugCode());
                        drugParam.setDay(detailList.get(i).getUseDays() + "");    //天数
                        drugParam.setNote(detailList.get(i).getMemo());    //说明
                        drugParam.setTotalUnit(detailList.get(i).getDrugUnit());      //开具单位(盒)
                        drugParam.setPrice(detailList.get(i).getSalePrice() + "");      //单价
                        drugParam.setSpuid(saleDrugList.getOrganDrugCode());
                        try {
                            //频次
                            drugParam.setFrequency(DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(detailList.get(i).getUsingRate()));
                            //用法
                            drugParam.setDoseUsage(DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getText(detailList.get(i).getUsePathways()));
                        } catch (ControllerException e) {
                            return getDrugEnterpriseResult(result, "药物使用频率使用途径获取失败");
                        }

                        drugParams.add(drugParam);
                    }
                    requestParam.setDrugs(drugParams);
                }

                //获取处方信息
                //处方编号
                if(null != dbRecipe.getRecipeCode()){
                    requestParam.setRxNo(dbRecipe.getRecipeCode());
                } else {
                    return getDrugEnterpriseResult(result, "处方编号不能为空");
                }
                //处方类型直接设空


                //费用类型
                requestParam.setFeeType("OWN_EXPENSE");

                //渠道、医院（要求固定值"JXZYY"）
                requestParam.setChannelCode("ZJZYYY");

                Map<String, String> attributes = new HashMap<String, String>();
                Date expiredTime = DateConversion.getDateAftXDays(dbRecipe.getSignDate(), 3);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                String signDateString = simpleDateFormat.format(dbRecipe.getSignDate());
                String expiredTimeString = simpleDateFormat.format(expiredTime);
                attributes.put("prescriptionCreateTime", signDateString);
                attributes.put("prescriptionExpiredTime", expiredTimeString);
                String attributesJson = JSONUtils.toString(attributes);
                requestParam.setAttributes(attributesJson);

//              requestParam.setVisitTime(dbRecipe.getSignDate());

                LOGGER.info("requestParam 处方信息:{}.", getJsonLog(requestParam));
                request.setCreateRequest(requestParam);

                try{
                    TaobaoClient client = new DefaultTaobaoClient(this.taobaoConf.getUrl(), this.taobaoConf.getAppkey(), this.taobaoConf.getSecret());
                    AlibabaAlihealthOutflowPrescriptionCreateResponse rsp = client.execute(request);
                    LOGGER.info("上传处方，{}", getJsonLog(rsp));

                    if (StringUtils.isEmpty(rsp.getSubCode())) {

                        LOGGER.info("推送处方至成功：", rsp.getServiceResult().getData());
                        //说明成功,更新处方标志
                        recipeDAO.updateRecipeInfoByRecipeId(dbRecipe.getRecipeId(), ImmutableMap.of("pushFlag", 1));
                        recipeExtendDAO.updateRecipeExInfoByRecipeId(dbRecipe.getRecipeId(), ImmutableMap.of("rxNo", rsp.getServiceResult().getData()));
                    } else {
                        return getDrugEnterpriseResult(result, rsp.getSubMsg());
                    }
                }catch (Exception e){
                    LOGGER.info("推送处方失败{}", dbRecipe.getRecipeId(), e);
                    return getDrugEnterpriseResult(result, e.getMessage());
                }
            }
        }

        return result;
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_ALDYF;
    }

    /*
     * @description 推送药企处方状态，由于只是个别药企需要实现，故有默认实现
     * @author gmw
     * @date 2019/9/18
     * @param rxId  recipeCode
     * @param status  status
     * @return recipe.bean.DrugEnterpriseResult
     */
    @RpcService
    @Override
    public DrugEnterpriseResult updatePrescriptionStatus(String rxId, int status) {
        LOGGER.info("更新处方状态");
        DrugEnterpriseResult drugEnterpriseResult = new DrugEnterpriseResult(DrugEnterpriseResult.SUCCESS);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe dbRecipe = recipeDAO.getByRecipeCode(rxId);
        if (ObjectUtils.isEmpty(dbRecipe)) {
            LOGGER.info("处方不存在{}.", rxId);
            return getDrugEnterpriseResult(drugEnterpriseResult, "处方不存在");
        }


        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(dbRecipe.getRecipeId());
        //调用扁鹊接口改变处方状态
        AlibabaAlihealthOutflowPrescriptionSyncstatusRequest request = new AlibabaAlihealthOutflowPrescriptionSyncstatusRequest();
        AlibabaAlihealthOutflowPrescriptionSyncstatusRequest.SyncPrescriptionStatusRequest requestParam =
            new AlibabaAlihealthOutflowPrescriptionSyncstatusRequest.SyncPrescriptionStatusRequest ();
        if(null != recipeExtend && null != recipeExtend.getRxNo()){
            requestParam.setRxNo(recipeExtend.getRxNo());
        } else {
            drugEnterpriseResult.setMsg("无法获取天猫对应处方编码");
            drugEnterpriseResult.setCode(DrugEnterpriseResult.FAIL);
        }
        requestParam.setStatus(RecipeStatusEnum.getValue(status + ""));
        request.setSyncStatusRequest(requestParam);
        try {
            TaobaoClient client = new DefaultTaobaoClient(this.taobaoConf.getUrl(), this.taobaoConf.getAppkey(), this.taobaoConf.getSecret());
            AlibabaAlihealthOutflowPrescriptionSyncstatusResponse rsp = client.execute(request);

            LOGGER.info("更新处方状态，{}", getJsonLog(rsp.getServiceResult()));
            if (StringUtils.isEmpty(rsp.getSubCode())) {
                //说明成功
                drugEnterpriseResult.setObject(rsp.getServiceResult().getData());
                drugEnterpriseResult.setCode(DrugEnterpriseResult.SUCCESS);
            } else {
                String errorMsg = rsp.getSubMsg();
                if ("53".equals(rsp.getServiceResult().getErrCode())) {
                    errorMsg = EXPIRE_TIP;
                }
                drugEnterpriseResult.setMsg(errorMsg);
                drugEnterpriseResult.setCode(DrugEnterpriseResult.FAIL);
            }
        } catch (ApiException e) {
            e.printStackTrace();
        }

        return drugEnterpriseResult;
    }

    /*
     * @description 淘宝处方状态变更
     * @author gmw
     * @date 2019/9/19
     * @param requestParam
     * @return java.lang.String
     */
    @RpcService
    public String changeState(String requestParam) {

        LOGGER.info("收到天猫更新处方请求，开始--{}" + requestParam);
        //获取入参
        AlibabaAlihealthPrescriptionStatusSyncRequest aRequest = JSON.parseObject(
            requestParam, AlibabaAlihealthPrescriptionStatusSyncRequest.class);
        //出参对象
        AlibabaAlihealthPrescriptionStatusSyncResponse response = new AlibabaAlihealthPrescriptionStatusSyncResponse();
        AlibabaAlihealthPrescriptionStatusSyncResponse.ResultDo resultDo = new AlibabaAlihealthPrescriptionStatusSyncResponse.ResultDo();

        //业务逻辑
        StandardStateDTO state = new StandardStateDTO ();

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        if(null == aRequest.getRxNo()){
            resultDo.setSuccess(false);
            resultDo.setErrorMessage("rnNO can not be null");
            resultDo.setErrorCode("500");
            response.setResult(resultDo);
            LOGGER.warn("参数异常--{}",JSON.toJSONString(response));
            return JSON.toJSONString(response);
        }
        List<Integer> recipeIds = recipeExtendDAO.findRecipeIdsByRxNo(aRequest.getRxNo());
        Recipe recipe = null;
        if(null != recipeIds && recipeIds.size() != 0){
            recipe = recipeDAO.getByRecipeId(recipeIds.get(0));
            state.setRecipeCode(recipe.getRecipeCode());
            //天猫的机构编码和纳里不一致,直接用纳里的机构编码
            state.setClinicOrgan(null == recipe.getClinicOrgan() ? null : recipe.getClinicOrgan() +"");
        } else {
            resultDo.setSuccess(true);
            resultDo.setErrorMessage("invalid rnNO: "+aRequest.getRxNo()+"，can not get recipeInfo");
            resultDo.setErrorCode("500");
            response.setResult(resultDo);
            LOGGER.warn("参数异常--{}",JSON.toJSONString(response));
            return JSON.toJSONString(response);
        }

        if(null != aRequest.getStatus()){
            state.setStatus(RecipeStatusEnum.getKey(aRequest.getStatus()));
        } else {
            resultDo.setSuccess(false);
            resultDo.setErrorMessage("status can not be null");
            resultDo.setErrorCode("500");
            response.setResult(resultDo);
            LOGGER.warn("参数异常--{}",JSON.toJSONString(response));
            return JSON.toJSONString(response);
        }

        //没有业务含义，仅满足校验用
        state.setOrganId(aRequest.getHospitalId());
        state.setAccount("tmdyf");

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        state.setDate(simpleDateFormat.format(new Date()));


        StandardEnterpriseCallService distributionService = getBean("distributionService", StandardEnterpriseCallService.class);
        StandardResultDTO resulta = distributionService.changeState(Collections.singletonList(state));

        if(StandardResultDTO.SUCCESS == resulta.getCode()){
            resultDo.setSuccess(true);
        } else {
            resultDo.setSuccess(true);
            resultDo.setErrorMessage(resulta.getMsg());
            resultDo.setErrorCode("500");
        }

        LOGGER.info("天猫更新处方请求执行结束");
        response.setResult(resultDo);
        return JSON.toJSONString(response);
    }

    /**
     * 更新处方医保备案号
     * @param recipeCode 处方号
     * @param medicalInsuranceRecord 医保备案号
     * @return
     */
    @RpcService
    public DrugEnterpriseResult updateMedicalInsuranceRecord(String recipeCode, String medicalInsuranceRecord) {
        LOGGER.info("更新处方医保备案号 start");
        DrugEnterpriseResult drugEnterpriseResult = new DrugEnterpriseResult(DrugEnterpriseResult.SUCCESS);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe dbRecipe = recipeDAO.getByRecipeCode(recipeCode);
        if (ObjectUtils.isEmpty(dbRecipe)) {
            LOGGER.info("处方不存在 recipeCode={}.", recipeCode);
            return getDrugEnterpriseResult(drugEnterpriseResult, "处方不存在");
        }
        AlibabaAlihealthOutflowPrescriptionUpdateRequest request = new AlibabaAlihealthOutflowPrescriptionUpdateRequest();

        AlibabaAlihealthOutflowPrescriptionUpdateRequest.PrescriptionOutflowUpdateRequest requestParam = new AlibabaAlihealthOutflowPrescriptionUpdateRequest.PrescriptionOutflowUpdateRequest();
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(dbRecipe.getRecipeId());
        if(null != recipeExtend && null != recipeExtend.getRxNo()){
            //与阿里对应的处方号
            requestParam.setRxNo(recipeExtend.getRxNo());
        } else {
            return getDrugEnterpriseResult(drugEnterpriseResult, "无法获取阿里对应处方编码");
        }
        //医保备案号
        requestParam.setCardNumber(medicalInsuranceRecord);
        //医保
        requestParam.setFeeType("MEDICAL_INSURANCE");
        requestParam.setSyncHisResult(true);
        request.setUpdateRequest(requestParam);
        LOGGER.info("更新处方医保备案号，request ={}", getJsonLog(request));
        try {
            TaobaoClient client = new DefaultTaobaoClient(this.taobaoConf.getUrl(), this.taobaoConf.getAppkey(), this.taobaoConf.getSecret());
            AlibabaAlihealthOutflowPrescriptionUpdateResponse rsp = client.execute(request);

            LOGGER.info("更新处方医保备案号，res ={}", getJsonLog(rsp.getServiceResult()));
            if (StringUtils.isEmpty(rsp.getSubCode())) {
                //说明成功
                drugEnterpriseResult.setObject(rsp.getServiceResult().getData());
                drugEnterpriseResult.setCode(DrugEnterpriseResult.SUCCESS);
            } else {
                String errorMsg = rsp.getSubMsg();
                drugEnterpriseResult.setMsg(errorMsg);
                drugEnterpriseResult.setCode(DrugEnterpriseResult.FAIL);
            }
        } catch (ApiException e) {
            LOGGER.error("更新处方医保备案号错误，recipeId= {}", dbRecipe.getRecipeId(),e);
            return getDrugEnterpriseResult(drugEnterpriseResult, "更新处方医保备案号异常");
        }
        return drugEnterpriseResult;
    }

//    /**
//     *
//     * @param rxId  处⽅Id
//     * @param queryOrder  是否查询订单
//     * @return 处方单
//     */
//    @Override
//    public DrugEnterpriseResult queryPrescription(String rxId, Boolean queryOrder) {
//        PatientService patientService = BasicAPI.getService(PatientService.class);
//        OrganService organService = BasicAPI.getService(OrganService.class);
//        DrugEnterpriseResult drugEnterpriseResult = new DrugEnterpriseResult(DrugEnterpriseResult.SUCCESS);
//        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
//        Recipe dbRecipe = recipeDAO.getByRecipeCode(rxId);
//        if (ObjectUtils.isEmpty(dbRecipe)) {
//            return getDrugEnterpriseResult(drugEnterpriseResult, "处方不存在");
//        }
//        String outHospitalId = organService.getOrganizeCodeByOrganId(dbRecipe.getClinicOrgan());
//        if (StringUtils.isEmpty(outHospitalId)) {
//            return getDrugEnterpriseResult(drugEnterpriseResult, "医院的外部编码不能为空");
//        }
//        String loginId = patientService.getLoginIdByMpiId(dbRecipe.getRequestMpiId());
//        String accessToken = aldyfRedisService.getTaobaoAccessToken(loginId);
//        if (ObjectUtils.isEmpty(accessToken)) {
//            return getDrugEnterpriseResult(drugEnterpriseResult, EXPIRE_TIP);
//        }
//        LOGGER.info("获取到accessToken:{}, loginId:{},{},{}", accessToken, loginId, rxId, outHospitalId);
//        alihealthHospitalService.setTopSessionKey(accessToken);
//        AlibabaAlihealthRxPrescriptionGetRequest prescriptionGetRequest = new AlibabaAlihealthRxPrescriptionGetRequest();
//        prescriptionGetRequest.setRxId(rxId);
//        prescriptionGetRequest.setOutHospitalId(outHospitalId);
//        BaseResult<AlibabaAlihealthRxPrescriptionGetResponse> responseBaseResult = alihealthHospitalService.queryPrescription(prescriptionGetRequest);
//        LOGGER.info("查询处方，{}", getJsonLog(responseBaseResult));
//        getAldyfResult(drugEnterpriseResult, responseBaseResult);
//        return drugEnterpriseResult;
//    }

    /**
     * 返回调用信息
     * @param result DrugEnterpriseResult
     * @param msg     提示信息
     * @return DrugEnterpriseResult
     */
    private DrugEnterpriseResult getDrugEnterpriseResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
        LOGGER.info("TmdyfRemoteService-getDrugEnterpriseResult提示信息：{}.", msg);
        return result;
    }

    private Integer getClinicOrganByOrganId(String organId, String clinicOrgan) throws Exception {
        Integer co = null;
        if (StringUtils.isEmpty(clinicOrgan)) {
            IOrganService organService = BaseAPI.getService(IOrganService.class);

            List<OrganBean> organList = organService.findByOrganizeCode(organId);
            if (CollectionUtils.isNotEmpty(organList)) {
                co = organList.get(0).getOrganId();
            }
        } else {
            co = Integer.parseInt(clinicOrgan);
        }
        return co;
    }

    private static String getJsonLog(Object object) {
        return JSONUtils.toString(object);
    }
}
