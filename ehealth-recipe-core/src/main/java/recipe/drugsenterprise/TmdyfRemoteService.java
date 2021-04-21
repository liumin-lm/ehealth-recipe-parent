package recipe.drugsenterprise;

import com.alibaba.fastjson.JSON;
import com.alijk.bqhospital.alijk.conf.TaobaoConf;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.currentuserinfo.model.SimpleWxAccountBean;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.patient.dto.*;
import com.ngari.patient.service.*;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.qimencloud.api.sceneqimen.request.AlibabaAlihealthPrescriptionStatusSyncRequest;
import com.qimencloud.api.sceneqimen.response.AlibabaAlihealthPrescriptionStatusSyncResponse;
import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AlibabaAlihealthOutflowPrescriptionCreateRequest;
import com.taobao.api.request.AlibabaAlihealthOutflowPrescriptionSyncstatusRequest;
import com.taobao.api.request.AlibabaAlihealthOutflowPrescriptionUpdateRequest;
import com.taobao.api.response.AlibabaAlihealthOutflowPrescriptionCreateResponse;
import com.taobao.api.response.AlibabaAlihealthOutflowPrescriptionSyncstatusResponse;
import com.taobao.api.response.AlibabaAlihealthOutflowPrescriptionUpdateResponse;
import ctd.account.UserRoleToken;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import static ctd.util.AppContextHolder.getBean;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.entity.mpi.Patient;
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
import recipe.constant.*;
import recipe.dao.*;
import recipe.drugsenterprise.bean.StandardResultDTO;
import recipe.drugsenterprise.bean.StandardStateDTO;
import recipe.hisservice.HisMqRequestInit;
import recipe.hisservice.RecipeToHisMqService;
import recipe.service.common.RecipeCacheService;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @description 天猫大药房对接服务
 * @author gmw
 * @date 2019/9/11
 */
@RpcBean("tmdyfRemoteService")
public class TmdyfRemoteService extends AccessDrugEnterpriseService{

    private static final Logger LOGGER = LoggerFactory.getLogger(TmdyfRemoteService.class);

    private static final String EXPIRE_TIP = "请重新授权";

    private static final String LEFT = "${";
    private static final String RIGHT = "}";

    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    @Autowired
    private TaobaoConf taobaoConf;

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("TmdyfRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return "有库存";
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        return null;
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

        //获取医院城市编码（6位）
        String cityCode = getHosCityCode(recipe.getClinicOrgan());
        //获取医院渠道编码
        String channelCode = transChannelCode(recipe.getClinicOrgan());
        if (StringUtils.isEmpty(channelCode)){
            LOGGER.warn("not find effective channelCode ={}",channelCode);
            response.setMsg("not find effective channelCode");
            return;
        }


        // 使用中或者已完成状态下的处方单----查看取药信息url
        if(RecipeStatusConstant.USING == recipe.getStatus() || RecipeStatusConstant.FINISH == recipe.getStatus()){
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            if(null == recipeExtend.getRxNo()){
                LOGGER.warn("无法获取天猫对应处方编码");
                response.setMsg("无法获取天猫对应处方编码");
                return ;
            }
            //拼接url模板占位符需要的参数
            Map<String, String> params = getProcessTemplateParams(channelCode,recipe.getRecipeCode(),recipeExtend.getRxNo(),cityCode,response);
            try{
                //查看处方详情单URL--先用配送到家的地址 targetPage不同 targetPage=1
                url = cacheService.getRecipeParam(ParameterConstant.KEY_ALI_O2O_NEW_ADDR, null);
                params.put("targetPage","1");
                //替换占位符
                url = processTemplate(url,params);
            }catch (Exception e){
                LOGGER.error("查看处方详情 get jump url error",e);
                //报错使用原来的地址
                String urlAddr = cacheService.getRecipeParam(ParameterConstant.KEY_ALI_O2O_ADDR, null);
                url = urlAddr + "rxNo=" + recipeExtend.getRxNo() +"&action=getDrugInfo";
            }
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
            /*if(RecipeBussConstant.GIVEMODE_SEND_TO_HOME == recipe.getGiveMode()){
                //配送到家URL
                url = url + "rxNo=" + recipeExtend.getRxNo() +"&action=o2o&cityCode=" + cityCode;
            } else {
                //药店取药取药URL
                url = url + "rxNo=" + recipeExtend.getRxNo() +"&action=offline&cityCode=" + cityCode;
            }*/
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            if(null == recipeExtend.getRxNo()){
                LOGGER.warn("无法获取天猫对应处方编码");
                response.setMsg("无法获取天猫对应处方编码");
                return ;
            }
            //拼接url模板占位符需要的参数
            Map<String, String> params = getProcessTemplateParams(channelCode,recipe.getRecipeCode(),recipeExtend.getRxNo(),cityCode,response);
            //获取阿里健康跳转地址
            if(RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode())){
                try {
                    //配送到家URL
                    url = cacheService.getRecipeParam(ParameterConstant.KEY_ALI_O2O_NEW_ADDR, null);
                    //替换占位符
                    url = processTemplate(url,params);
                }catch (Exception e){
                    LOGGER.error("配送到家 get jump url error",e);
                    //报错使用原来的地址
                    String urlAddr = cacheService.getRecipeParam(ParameterConstant.KEY_ALI_O2O_ADDR, null);
                    url = urlAddr + "rxNo=" + recipeExtend.getRxNo() +"&action=o2o&cityCode=" + cityCode;
                }
            } else {
                try {
                    //药店取药URL--- targetPage不同 targetPage=4
                    url = cacheService.getRecipeParam(ParameterConstant.KEY_ALI_O2O_NEW_ADDR, null);
                    params.put("targetPage","4");
                    //替换占位符
                    url = processTemplate(url,params);
                }catch (Exception e){
                    LOGGER.error("药店取药 get jump url error",e);
                    //报错使用原来的地址
                    String urlAddr = cacheService.getRecipeParam(ParameterConstant.KEY_ALI_O2O_ADDR, null);
                    //药店取药取药URL
                    url = urlAddr + "rxNo=" + recipeExtend.getRxNo() +"&action=offline&cityCode=" + cityCode;
                }
            }
            response.setCode(PurchaseResponse.ORDER);
        }
        response.setOrderUrl(url);
    }

    private String getHosCityCode(Integer clinicOrgan) {
        OrganService organService = BasicAPI.getService(OrganService.class);
        OrganDTO organCode = organService.getByOrganId(clinicOrgan);
        String cityCode;
        if(organCode.getAddrArea().length() >= 4){
            cityCode = organCode.getAddrArea().substring(0,4) + "00";
        } else {
            cityCode = organCode.getAddrArea() + "00";
            for(int i = cityCode.length(); i<6; i++){
                cityCode = cityCode + "0";
            }
        }
        return cityCode;
    }

    private String processTemplate(String tpl, Map params){
        Iterator<Map.Entry> it = params.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry entry = it.next();
            Object v = entry.getValue();
            String val = v == null ? "" : v.toString();
            tpl = tpl.replace(StringUtils.join(LEFT, entry.getKey().toString(), RIGHT), val);
        }
        return tpl;
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, Integer depId) {
        LOGGER.info("推送处方至天猫大药房开始，处方ID：{}.", JSONUtils.toString(recipeIds));
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (ObjectUtils.isEmpty(recipeIds)) {
            getDrugEnterpriseResult(result, "处方ID参数为空");
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        if (!ObjectUtils.isEmpty(recipeList)) {
            for (Recipe dbRecipe : recipeList ) {
                AlibabaAlihealthOutflowPrescriptionCreateRequest request = new AlibabaAlihealthOutflowPrescriptionCreateRequest ();
                AlibabaAlihealthOutflowPrescriptionCreateRequest.PrescriptionOutflowUpdateRequest requestParam = new AlibabaAlihealthOutflowPrescriptionCreateRequest.PrescriptionOutflowUpdateRequest ();
                try{
                    //获取患者信息
                    getPatientInfo(dbRecipe, requestParam);
                    //获取处方信息
                    getRecipeInfo(dbRecipe, requestParam);
                    //获取医生信息
                    getDoctorAndDeptInfo(dbRecipe, requestParam);
                    //封装诊断信息
                    getDiseaseInfo(dbRecipe, requestParam);
                    //药品详情
                    getDetailInfo(dbRecipe, requestParam,depId);
                }catch (Exception e){
                    LOGGER.error("pushRecipeInfo splicingData error.", e);
                    return getDrugEnterpriseResult(result, e.getMessage());
                }
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

    private void getRecipeInfo(Recipe dbRecipe, AlibabaAlihealthOutflowPrescriptionCreateRequest.PrescriptionOutflowUpdateRequest requestParam) {
        //处方编号
        if(null != dbRecipe.getRecipeCode()){
            requestParam.setRxNo(dbRecipe.getRecipeCode());
        } else {
            throw new DAOException("处方编号不能为空");
        }
        requestParam.setVisitId(dbRecipe.getRecipeId() + "");
        //DiagnosticParam 患者主诉
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(dbRecipe.getRecipeId());
        if(null != recipeExtend){
            getMedicalInfo(dbRecipe, recipeExtend);
            requestParam.setMainTell(recipeExtend.getMainDieaseDescribe());   //患者主诉
            requestParam.setProblemNow(recipeExtend.getHistoryOfPresentIllness());   //现病史
            requestParam.setBodyCheck(recipeExtend.getPhysicalCheck());   //一般检查
            //医保备案号
            requestParam.setCardNumber(recipeExtend.getPutOnRecordID());
            //自费 0 商保 1 省医保33 杭州市医保3301 衢州市医保3308 巨化医保3308A
            requestParam.setPatientType(transPatientType(recipeExtend.getPatientType()));
            //医院所属区域代码(结算发生地区域代码)
            requestParam.setInsuranceSettlementRegion(recipeExtend.getHospOrgCodeFromMedical());
            //参保地统筹区
            requestParam.setPatientInsuredRegion(transPatientRegion(recipeExtend.getInsuredArea()));
            //卡类型
            requestParam.setArchivesType(transCardType(recipeExtend.getCardTypeName()));
            //卡号
            requestParam.setArchivesId(recipeExtend.getCardNo());
            //费用类型
            requestParam.setFeeType(transFeeType(recipeExtend.getPatientType()));
        }
        requestParam.setDoctorAdvice(dbRecipe.getMemo());               //医生嘱言
        requestParam.setPlatformCode("ZJSPT");

        //来源-固定值INTERNET_HOSPITAL_PRESCRIPTION---互联网医院处方外配
        //DEPART_PRESCRIPTION门诊处方外配、CONSUMABLES_ADVICE耗材医嘱流转
        requestParam.setSource("INTERNET_HOSPITAL_PRESCRIPTION");
        //模板--INTERNET_HOSPITAL_PRESCRIPTION互联网医院处方笺、
        //------ELECTRONIC_PRESCRIPTION电子处方笺、
        //------DOCTOR_ADVICE医嘱单
        requestParam.setTemplate("INTERNET_HOSPITAL_PRESCRIPTION");

        //渠道、医院（要求固定值"JXZYY"）浙一医院-ZJZYYY 衢化医院-ZJQHYY

        //获取医院渠道编码
        String channelCodeArr = transChannelCode(dbRecipe.getClinicOrgan());
        if (null != channelCodeArr){
            String [] channelCode =channelCodeArr.split("_");
            requestParam.setChannelCode(channelCode[0]);
        } else {
            LOGGER.warn("获取医院渠道编码为null channelCode ={}",channelCodeArr);
        }

        Map<String, String> attributes = new HashMap<String, String>();
        Date expiredTime = DateConversion.getDateAftXDays(dbRecipe.getSignDate(), 3);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String signDateString = simpleDateFormat.format(dbRecipe.getSignDate());
        String expiredTimeString = simpleDateFormat.format(expiredTime);
        attributes.put("prescriptionCreateTime", signDateString);
        attributes.put("prescriptionExpiredTime", expiredTimeString);
        String attributesJson = JSONUtils.toString(attributes);
        requestParam.setAttributes(attributesJson);
    }

    private String transChannelCode(Integer clinicOrgan) {
        RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);
        //key = 2000005_ORGAN_CHANNEL_CODE value=ZJZYYY
        //key = 衢化organId_ORGAN_CHANNEL_CODE value=ZJQHYY
        return cacheService.getRecipeParam(clinicOrgan+"_"+ParameterConstant.KEY_ORGAN_CHANNEL_CODE, "ZJZYYY");

    }

    private Map<String,String> getProcessTemplateParams(String channelCode, String outerRxNo, String jkRxNo,String cityCode, PurchaseResponse response) {
        String[] channel = channelCode.split("_");
        if(channel == null || channel.length < 3 ){
            LOGGER.warn("not find effective channelCode ={}",channelCode);
            response.setMsg("not find effective channelCode");
            return null;
        }
        Map<String, String> params = Maps.newHashMap();
        params.put("outerRxNo",outerRxNo);
        params.put("jkRxNo",jkRxNo);
        params.put("cityCode",cityCode);
        params.put("channelCode",channel[0]);
        params.put("targetPage",channel[1]);
        params.put("hospitalId",channel[2]);
        return params;

    }

    private String transFeeType(String patientType) {
        if (StringUtils.isEmpty(patientType)){
            return "OWN_EXPENSE";
        }
        switch (patientType){
            case "33":
            case "3301":
            case "3308":
            case "3308A":
            case "1":return "MEDICAL_INSURANCE";
            default:return "OWN_EXPENSE";
        }
    }

    private String transPatientRegion(String insuredArea) {
        if (StringUtils.isEmpty(insuredArea)){
            return "";
        }
        switch (insuredArea){
            case "33":return "浙江省本级";
            case "3301":return "杭州市本级";
            case "3302":return "宁波市本级";
            case "3303":return "温州市本级";
            case "3304":return "嘉兴市本级";
            case "3305":return "湖州市本级";
            case "3306":return "绍兴市本级";
            case "3307":return "金华市本级";
            case "3308":return "衢州市本级";
            case "3309":return "舟山市本级";
            case "3310":return "台州市本级";
            case "3325":return "丽水市本级";
            default:return insuredArea;
        }
    }

    private String transCardType(String cardTypeName) {
        if (StringUtils.isEmpty(cardTypeName)){
            return "";
        }
        switch (cardTypeName){
            case "就诊卡":return "VISIT_CARD";
            case "身份证":return "ID_CARD";
            case "医保卡":return "MEDICAL_INSURANCE";
            case "病历号":return "HOSPITAL_MEDICAL_ID";
            case "医保电子凭证":return "MEDICAL_INSURANCE_ELECTRONIC_VOUCHER";
            default:return cardTypeName;
        }
    }

    private String transPatientType(String patientType) {
        if (StringUtils.isEmpty(patientType)){
            return "OWN_EXPENSE";
        }
        switch (patientType){
            case "0":return "OWN_EXPENSE";
            case "33":return "PROVINCE_MEDICAL_INSURANCE";
            case "3301":
            case "3308":
            case "3308A":
                return "CITY_MEDICAL_INSURANCE";
            case "1":return "BUSINESS_INSURANCE";
            default:return patientType;
        }
    }

    private void getDetailInfo(Recipe dbRecipe, AlibabaAlihealthOutflowPrescriptionCreateRequest.PrescriptionOutflowUpdateRequest requestParam,Integer depId) {
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        //OrganDrugListDAO dao = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<Recipedetail> detailList = detailDAO.findByRecipeId(dbRecipe.getRecipeId());
        List<AlibabaAlihealthOutflowPrescriptionCreateRequest.Drugs> drugParams = new ArrayList<>();
        if (!ObjectUtils.isEmpty(detailList)) {
            SaleDrugListDAO saleDrugDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            DrugListDAO drugDAO = DAOFactory.getDAO(DrugListDAO.class);
            //OrganDrugList organDrugList;
            for (int i = 0; i < detailList.size(); i++) {
                //一张处方单可能包含相同的药品purchaseService
                SaleDrugList saleDrugList = saleDrugDAO.getByDrugIdAndOrganId(detailList.get(i).getDrugId(), depId);
                DrugList drugList = drugDAO.getById(detailList.get(i).getDrugId());
                if (ObjectUtils.isEmpty(saleDrugList) || ObjectUtils.isEmpty(drugList)) {
                    throw new DAOException("未找到对应的药品");
                }
                AlibabaAlihealthOutflowPrescriptionCreateRequest.Drugs drugParam = new AlibabaAlihealthOutflowPrescriptionCreateRequest.Drugs();
                if(null != saleDrugList.getDrugSpec()){
                    drugParam.setSpec(saleDrugList.getDrugSpec());        //药品规格
                } else {
                    if (StringUtils.isNotEmpty(drugList.getDrugSpec())) {
                        drugParam.setSpec(drugList.getDrugSpec());
                    } else {
                        throw new DAOException("药品规格不能为空");
                    }
                }
                if(null != detailList.get(i).getUseTotalDose()){
                    drugParam.setTotal(detailList.get(i).getUseTotalDose() + "");    //药品数量
                } else {
                    throw new DAOException("药品数量不能为空");
                }
                drugParam.setDrugName(saleDrugList.getSaleName() == null ? drugList.getSaleName() : saleDrugList.getSaleName());    //药品名称
                if(null != detailList.get(i).getUseDose()){
                    drugParam.setDose(detailList.get(i).getUseDose() + "");    //用量
                } else {
                    throw new DAOException("药品用量不能为空");
                }
                if(null != saleDrugList.getDrugName()){
                    drugParam.setDrugCommonName(saleDrugList.getDrugName());  //药品通用名称
                } else {
                    if (StringUtils.isNotEmpty(drugList.getDrugName())) {
                        drugParam.setDrugCommonName(drugList.getDrugName());
                    } else {
                        throw new DAOException("药品通用名称不能为空");
                    }
                }
                if(null != detailList.get(i).getUseDoseUnit()){
                    drugParam.setDoseUnit(detailList.get(i).getUseDoseUnit());      //用量单位
                } else {
                    throw new DAOException("用量单位不能为空");
                }
                /*//医院药品id
                organDrugList = dao.getByDrugIdAndOrganId(saleDrugList.getDrugId(), dbRecipe.getClinicOrgan());
                if (organDrugList!=null){
                    drugParam.setDrugId(organDrugList.getOrganDrugCode());
                }*/
                drugParam.setDay(detailList.get(i).getUseDays() + "");    //天数
                drugParam.setNote(detailList.get(i).getMemo());    //说明
                drugParam.setTotalUnit(detailList.get(i).getDrugUnit());      //开具单位(盒)
                //价格大于0才显示字段
                if (detailList.get(i).getSalePrice() !=null && detailList.get(i).getSalePrice().intValue()>0){
                    drugParam.setPrice(detailList.get(i).getSalePrice() + "");      //单价
                }
                drugParam.setSpuid(saleDrugList.getOrganDrugCode());
                try {
                    //频次
                    drugParam.setFrequency(StringUtils.isNotEmpty(detailList.get(i).getUsingRateTextFromHis())?detailList.get(i).getUsingRateTextFromHis():DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(detailList.get(i).getUsingRate()));
                    //用法
                    drugParam.setDoseUsage(StringUtils.isNotEmpty(detailList.get(i).getUsePathwaysTextFromHis())?detailList.get(i).getUsePathwaysTextFromHis():DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getText(detailList.get(i).getUsePathways()));
                } catch (ControllerException e) {
                    throw new DAOException("药物使用频率使用途径获取失败");
                }
                drugParams.add(drugParam);
            }
            requestParam.setDrugs(drugParams);
        }
    }

    private void getDiseaseInfo(Recipe dbRecipe, AlibabaAlihealthOutflowPrescriptionCreateRequest.PrescriptionOutflowUpdateRequest requestParam) {
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
            throw new DAOException("诊断信息不能为空");
        }
    }

    private void getDoctorAndDeptInfo(Recipe dbRecipe, AlibabaAlihealthOutflowPrescriptionCreateRequest.PrescriptionOutflowUpdateRequest requestParam) {
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
        DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
        DoctorDTO doctor = doctorService.get(dbRecipe.getDoctor());
        if (!ObjectUtils.isEmpty(doctor)) {
            requestParam.setDoctorId(doctor.getDoctorId() + "");
            if(null != doctor.getName()){
                requestParam.setDoctorName(doctor.getName());
            } else {
                throw new DAOException("就诊医生姓名不能为空");
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
                    throw new DAOException("医生主科室不存在");
                }
            } else {
                throw new DAOException("医生主执业点不存在");
            }
        } else {
            throw new DAOException("医生不存在");
        }
    }

    private void getPatientInfo(Recipe dbRecipe, AlibabaAlihealthOutflowPrescriptionCreateRequest.PrescriptionOutflowUpdateRequest requestParam) {
        //操作人手机号
        Patient patient2 = UserRoleToken.getCurrent().getProperty("patient", Patient.class);
        if(patient2!=null && null != patient2.getMobile()){
            requestParam.setMobilePhone(patient2.getMobile());
        } else {
            throw new DAOException("操作人手机号不能为空");
        }
        //操作人支付宝user_id
        ICurrentUserInfoService userInfoService = AppContextHolder.getBean(
                "eh.remoteCurrentUserInfoService", ICurrentUserInfoService.class);
        SimpleWxAccountBean account = userInfoService.getSimpleWxAccount();
        /*requestParam.setAlipayUserId("2088622513812239");*/
        if (account!=null){
            String openId = account.getOpenId();
            if(null != openId){
                requestParam.setAlipayUserId(openId);
            } else {
                throw new DAOException("操作人支付宝user_id不能为空");
            }
        }

        PatientService patientService = BasicAPI.getService(PatientService.class);
        PatientDTO patient = patientService.get(dbRecipe.getMpiid());
        if (!ObjectUtils.isEmpty(patient)) {
            int patientAge = patient.getBirthday() == null ? 0 : DateConversion
                    .getAge(patient.getBirthday());
            requestParam.setPatientId(dbRecipe.getMpiid());
            if(null != patient.getPatientName()){
                requestParam.setPatientName(patient.getPatientName());
            } else {
                throw new DAOException("患者姓名不能为空");
            }

            requestParam.setAge(patientAge+"");
            if(null != patient.getPatientSex()){
                try {
                    requestParam.setSex(DictionaryController.instance().get("eh.base.dictionary.Gender").getText(patient.getPatientSex()));
                } catch (Exception e) {
                    throw new DAOException("获取患者性别异常");
                }
            } else {
                throw new DAOException("患者性别不能为空");
            }
            requestParam.setAddress(patient.getAddress());
            //患者身份证
            requestParam.setIdCard(patient.getIdcard());
        } else {
            throw new DAOException("患者不存在");
        }
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
        if (RecipeStatusConstant.FINISH == Integer.valueOf(state.getStatus())){
            RecipeToHisMqService hisMqService = ApplicationUtils.getRecipeService(RecipeToHisMqService.class);
            hisMqService.recipeStatusToHis(HisMqRequestInit.initRecipeStatusToHisReq(recipe, HisBussConstant.TOHIS_RECIPE_STATUS_FINISH,"O2O平台处方配送"));
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
