package recipe.audit.service;

import com.alibaba.fastjson.JSONObject;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.recipe.common.RecipeCommonBaseTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.audit.bean.*;
import recipe.audit.pawebservice.PAWebServiceLocator;
import recipe.audit.pawebservice.PAWebServiceSoap12Stub;
import recipe.constant.CacheConstant;
import recipe.constant.RecipeSystemConstant;
import recipe.dao.CompareDrugDAO;
import recipe.util.DateConversion;
import recipe.util.DigestUtil;
import recipe.util.LocalStringUtil;
import recipe.util.RedisClient;

import javax.xml.rpc.holders.IntHolder;
import javax.xml.rpc.holders.StringHolder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 描述：卫宁智能审方
 * @author yinsheng
 * @date 2019\10\9 0009 10:33
 */
public class WinningPrescriptionService implements IntellectJudicialService{

    /** logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(WinningPrescriptionService.class);

    @Autowired
    private RedisClient redisClient;

    /**
     * 早期使用接口，不能删除
     * @param recipe
     * @param recipedetails
     * @return
     */
    @RpcService
    @Deprecated
    public String getPAAnalysis(RecipeBean recipe, List<RecipeDetailBean> recipedetails){
        return null;
    }

    /**
     * 互联网医院使用返回格式
     * @param recipe
     * @param recipedetails
     * @return
     */
    @Override
    public AutoAuditResult analysis(RecipeBean recipe, List<RecipeDetailBean> recipedetails){
        AutoAuditResult result = null;
        if(null == recipe || CollectionUtils.isEmpty(recipedetails)){
            result = new AutoAuditResult();
            result.setCode(RecipeCommonBaseTO.FAIL);
            result.setMsg("参数错误");
            return result;
        }
        //检查缓存配置情况
        String isAutoReview = redisClient.hget(CacheConstant.KEY_CONFIG_RCP_AUTO_REVIEW, recipe.getClinicOrgan().toString());

        if(StringUtils.isEmpty(isAutoReview) || "true".equalsIgnoreCase(isAutoReview)){
            try {
                result = analysisImpl(recipe, recipedetails);
            } catch (Exception e) {
                LOGGER.warn("analysis error. recipe={}, detail={}",
                        JSONUtils.toString(recipe), JSONUtils.toString(recipedetails));
                result = new AutoAuditResult();
                result.setCode(RecipeCommonBaseTO.FAIL);
                result.setMsg("抱歉，系统异常，无预审结果");
            }
        }else{
            result = new AutoAuditResult();
            result.setCode(RecipeCommonBaseTO.SUCCESS);
            result.setMsg("智能预审未开通，无预审结果");
        }

        return result;
    }

    /**
     * 医生在开处方时，校验合理用药
     *
     * @param recipe        处方信息
     * @param recipedetails 处方详情
     * @return 返回null表示用药没有异常，反之表示有异常，前端弹框提示，提示信息为返回的字符串信息
     * @throws Exception
     */
    public AutoAuditResult analysisImpl(RecipeBean recipe, List<RecipeDetailBean> recipedetails) throws Exception {
        String errorMsg = "抱歉，系统异常，无预审结果";
        AutoAuditResult result = new AutoAuditResult();
        result.setCode(RecipeCommonBaseTO.FAIL);

        PAWebServiceSoap12Stub binding;
        IntHolder getPAResultsResult = new IntHolder();
        StringHolder uiResults = new StringHolder();
        StringHolder hisResults = new StringHolder();
        BaseData baseData = new BaseData();
        DetailsData detailsData = new DetailsData();

        // 拼接第三方（卫宁）请求参数
        try {
            getParams(baseData, detailsData, recipe, recipedetails);
        } catch (Exception e) {
            LOGGER.warn("analysisImpl getParams error. ", e);
            result.setMsg(errorMsg);
            return result;
        }
        String baseDateToString = JSONUtils.toString(baseData);
        String detailsDataToString = JSONUtils.toString(detailsData);
        int funId = 1006;
        LOGGER.info("analysisImpl funId={}, baseDate={}, detailsDate={}", funId, baseDateToString, detailsDataToString);
        try {
            binding = (PAWebServiceSoap12Stub) new PAWebServiceLocator().getPAWebServiceSoap12();
            if (binding != null) {
                // Time out after a minute
                binding.setTimeout(20000);
                binding.getPAResults(funId, baseDateToString, detailsDataToString, getPAResultsResult, uiResults, hisResults);
            }
        } catch (Exception e) {
            LOGGER.warn("analysisImpl funId={} error. ", funId, e);
            result.setMsg(errorMsg);
            return result;
        }

        if(null == uiResults && StringUtils.isEmpty(uiResults.value)){
            LOGGER.warn("analysisImpl funId={} response is null. ", funId);
            result.setMsg(errorMsg);
            return result;
        }
        LOGGER.info("analysisImpl funId={}, response={}", funId, uiResults.value);

        List<PAWebMedicines> medicines = null;
        String brief = null;
        try {
            // 将字符串转化成java对象
            PAWebResponse response = JSONObject.parseObject(uiResults.value, PAWebResponse.class);
            medicines = response.getMedicines();
            brief = response.getBrief();
        } catch (Exception e) {
            LOGGER.warn("analysisImpl funId={} covert to PAWebResponse error.", funId);
            result.setMsg(errorMsg);
            return result;
        }
        if (CollectionUtils.isNotEmpty(medicines)) {
            result.setMedicines(medicines);
            result.setMsg(brief);
        }else{
            result.setCode(RecipeCommonBaseTO.SUCCESS);
            result.setMsg("系统预审未发现处方问题");
        }

        return result;
    }


    /**
     * 参数构造
     *
     * @param baseData
     * @param detailsData
     * @param recipe
     * @param recipedetails
     * @throws ControllerException
     */
    public void getParams(BaseData baseData, DetailsData detailsData, RecipeBean recipe,
                          List<RecipeDetailBean> recipedetails) throws Exception {

        IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);
        PatientBean patient = iPatientService.get(recipe.getMpiid());
        if(null == patient){
            throw new DAOException("患者不存在");
        }

        baseData.setDeptCode(LocalStringUtil.toString(recipe.getDepart()));
        baseData.setDoctCode(LocalStringUtil.toString(recipe.getDoctor()));
        baseData.setDoctName(recipe.getDoctorName());
        baseData.setHospCode(PrescriptionConstants.getWeiningPaHosCode());

        // 诊断信息
        List<AuditDiagnose> diagnoses = new ArrayList<>();
        // 多个诊断的情况
        String s = "；";
        if (recipe.getOrganDiseaseName().contains(s)) {
            String[] a = recipe.getOrganDiseaseName().split(s);
            String[] b = recipe.getOrganDiseaseId().split(s);
            AuditDiagnose auditDiagnose;
            for (int i = 0; i < a.length; i++) {
                auditDiagnose = new AuditDiagnose();
                auditDiagnose.setType(RecipeSystemConstant.IDC10_DIAGNOSE_TYPE);
                auditDiagnose.setCode(b[i]);
                auditDiagnose.setName(a[i]);
                diagnoses.add(auditDiagnose);
            }
        } else {
            AuditDiagnose auditDiagnose = new AuditDiagnose();
            auditDiagnose.setType(RecipeSystemConstant.IDC10_DIAGNOSE_TYPE);
            auditDiagnose.setCode(recipe.getOrganDiseaseId());
            auditDiagnose.setName(recipe.getOrganDiseaseName());
            diagnoses.add(auditDiagnose);
        }

        // 检查单信息
        List<AuditLisForm> lisForms = new ArrayList<>();

        // 过敏源
        List<AuditAllergy> auditAllergys = new ArrayList<>();

        // 患者信息
        AuditPatient auditPatient = new AuditPatient();
        auditPatient.setAllergies(auditAllergys);
        auditPatient.setDiagnoses(diagnoses);
        auditPatient.setLisForms(lisForms);
        auditPatient.setName(patient.getPatientName());
        auditPatient.setBirthDate(DateConversion.formatDate(patient.getBirthday()));
        auditPatient.setGender(DictionaryController.instance().get("eh.base.dictionary.Gender").getText(patient.getPatientSex()));

        // 处方信息
        List<AuditPrescription> prescriptions = new ArrayList<>();

        String recipeTempId = DigestUtil.md5For16(recipe.getClinicOrgan() +
                recipe.getMpiid() + Calendar.getInstance().getTimeInMillis());

        AuditPrescription prescription = new AuditPrescription();
        //处方当前都没ID生成
        prescription.setId(recipeTempId);
        prescription.setPresTime(DateConversion.formatDateTime(new Date()));
        prescription.setIsCur(RecipeSystemConstant.IS_CURRENT_PRESCRIPTION);
        prescription.setIsNew(RecipeSystemConstant.IS_NEW_PRESCRIPTION);
        prescription.setDoctCode(LocalStringUtil.toString(recipe.getDoctor()));
        prescription.setDoctName(recipe.getDoctorName());
        prescription.setDeptCode(LocalStringUtil.toString(recipe.getDepart()));

        CompareDrugDAO compareDrugDAO = DAOFactory.getDAO(CompareDrugDAO.class);
        // 药品信息
        List<AuditMedicine> medicines = new ArrayList<>();
        AuditMedicine medicine;
        for (RecipeDetailBean recipedetail : recipedetails) {
            medicine = new AuditMedicine();
            medicine.setName(recipedetail.getDrugName());
            Integer targetDrugId = compareDrugDAO.findTargetDrugIdByOriginalDrugId(recipedetail.getDrugId());
            if (ObjectUtils.isEmpty(targetDrugId)) {
                medicine.setHisCode(LocalStringUtil.toString(recipedetail.getDrugId()));
            } else {
                medicine.setHisCode(LocalStringUtil.toString(targetDrugId));
            }
            medicine.setSpec(recipedetail.getDrugSpec());
            medicine.setGroup(recipedetail.getDrugGroup());
            medicine.setUnit(recipedetail.getUseDoseUnit());
            medicine.setDose(LocalStringUtil.toString(recipedetail.getUseDose()));
            medicine.setFreq(recipedetail.getUsingRate());
            medicine.setPath(recipedetail.getUsePathways());
            medicine.setDays(LocalStringUtil.toString(recipedetail.getUseDays()));
            medicine.setNeedAlert(RecipeSystemConstant.IS_NEED_ALERT);
            medicines.add(medicine);
        }
        prescription.setMedicines(medicines);
        prescriptions.add(prescription);

        // 数据详情
        detailsData.setTime(DateConversion.formatDateTimeWithSec(new Date()));
        detailsData.setHospFlag(RecipeSystemConstant.HOSPFLAG_FOR_OP);
        detailsData.setAdmType(RecipeSystemConstant.COMMON_ADM_TYPE);
        detailsData.setAdmNo(recipe.getClinicOrgan()+"-"+ recipeTempId);
        detailsData.setPatient(auditPatient);
        detailsData.setPrescriptions(prescriptions);
    }

    /**
     * 获取药品说明页面URL
     * @param drugId
     * @return
     */
    @Override
    public String getDrugSpecification(Integer drugId){
        CompareDrugDAO compareDrugDAO = DAOFactory.getDAO(CompareDrugDAO.class);
        PAWebServiceSoap12Stub binding;
        IntHolder getPAResultsResult = new IntHolder();
        StringHolder uiResults = new StringHolder();
        StringHolder hisResults = new StringHolder();
        BaseData baseData = new BaseData();
        baseData.setHospCode(PrescriptionConstants.getWeiningPaHosCode());

        DetailsData detailsData = new DetailsData();
        detailsData.setHospFlag(RecipeSystemConstant.HOSPFLAG_FOR_OP);
        Integer targetDrugId = compareDrugDAO.findTargetDrugIdByOriginalDrugId(drugId);
        if (ObjectUtils.isEmpty(targetDrugId)) {
            detailsData.setMedHisCode(drugId.toString());
        } else {
            detailsData.setMedHisCode(targetDrugId.toString());
        }

        String baseDateToString = JSONUtils.toString(baseData);
        String detailsDataToString = JSONUtils.toString(detailsData);
        int funId = 1004;
        LOGGER.info("getDrugSpecification funId={}, baseDate={}, detailsDate={}", funId, baseDateToString, detailsDataToString);
        try {
            binding = (PAWebServiceSoap12Stub) new PAWebServiceLocator().getPAWebServiceSoap12();
            if (binding != null) {
                // Time out after a minute
                binding.setTimeout(20000);
                binding.getPAResults(funId, baseDateToString, detailsDataToString, getPAResultsResult, uiResults, hisResults);
            }
        } catch (Exception e) {
            LOGGER.warn("getDrugSpecification funId={} error. ", funId, e);
            return null;
        }

        if(null == uiResults && StringUtils.isEmpty(uiResults.value)){
            LOGGER.warn("getDrugSpecification funId={} response is null. ", funId);
            return null;
        }
        LOGGER.info("getDrugSpecification funId={}, response={}", funId, uiResults.value);

        String url = null;
        try {
            // 将字符串转化成java对象
            JSONObject json = JSONObject.parseObject(uiResults.value);
            Object nameObj = json.get("FileName");
            if(null == nameObj || StringUtils.isEmpty(nameObj.toString())){
                return null;
            }
            url = json.get("Link").toString();
            url = PrescriptionConstants.getWeiningPaDetailAddress()+url;
        } catch (Exception e) {
            LOGGER.warn("getDrugSpecification funId={} covert to Object error.", funId);
            return null;
        }

        return url;
    }
}
