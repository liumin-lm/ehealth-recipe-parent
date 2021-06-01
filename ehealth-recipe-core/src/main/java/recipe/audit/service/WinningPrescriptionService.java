package recipe.audit.service;

import com.alibaba.fastjson.JSONObject;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.common.RecipeCommonBaseTO;
import com.ngari.recipe.entity.RecipeExtend;
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
import recipe.bean.RecipeGiveModeButtonRes;
import recipe.constant.CacheConstant;
import recipe.constant.RecipeSystemConstant;
import recipe.dao.CompareDrugDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeParameterDao;
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

import static recipe.service.manager.EmrRecipeManager.getMedicalInfo;

/**
 * 描述：卫宁智能审方
 *
 * @author yinsheng
 * @date 2019\10\9 0009 10:33
 */
public class WinningPrescriptionService implements IntellectJudicialService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WinningPrescriptionService.class);

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private IConfigurationCenterUtilsService configService;

    @Autowired
    private RecipeParameterDao recipeParameterDao;

    @Autowired
    private OrganService organService;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    /**
     * 早期使用接口，不能删除
     *
     * @param recipe
     * @param recipedetails
     * @return
     */
    @RpcService
    @Deprecated
    public String getPAAnalysis(RecipeBean recipe, List<RecipeDetailBean> recipedetails) {
        return null;
    }

    /**
     * 互联网医院使用返回格式
     *
     * @param recipe
     * @param recipedetails
     * @return
     */
    @Override
    public AutoAuditResult analysis(RecipeBean recipe, List<RecipeDetailBean> recipedetails) {
        AutoAuditResult result = null;
        if (null == recipe || CollectionUtils.isEmpty(recipedetails)) {
            result = new AutoAuditResult();
            result.setCode(RecipeCommonBaseTO.FAIL);
            result.setMsg("参数错误");
            return result;
        }
        //检查缓存配置情况
        String isAutoReview = redisClient.hget(CacheConstant.KEY_CONFIG_RCP_AUTO_REVIEW, recipe.getClinicOrgan().toString());

        if (StringUtils.isEmpty(isAutoReview) || "true".equalsIgnoreCase(isAutoReview)) {
            try {
                RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                getMedicalInfo(recipe, recipeExtend);
                result = analysisImpl(recipe, recipedetails);
            } catch (Exception e) {
                LOGGER.warn("analysis error. recipe={}, detail={}",
                        JSONUtils.toString(recipe), JSONUtils.toString(recipedetails));
                result = new AutoAuditResult();
                result.setCode(RecipeCommonBaseTO.FAIL);
                result.setMsg("抱歉，系统异常，无预审结果");
            }
        } else {
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
        PAWebServiceSoap12Stub binding2;
        IntHolder getPAResultsResult = new IntHolder();
        StringHolder uiResults = new StringHolder();
        StringHolder hisResults = new StringHolder();
        StringHolder uiResults2 = new StringHolder();
        StringHolder hisResults2 = new StringHolder();

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

            Boolean invokeRecipeAnalysis = (Boolean)configService.getConfiguration(recipe.getClinicOrgan(),"InvokeRecipeAnalysis");
            String invokeUrl = "http://103.38.233.27:820/PAWebService.asmx";

            OrganDTO organ = organService.getByOrganId(recipe.getClinicOrgan());
            if (null != organ.getHospitalCode()) {
                String paramValue = recipeParameterDao.getByName(organ.getHospitalCode() + "_winning_recipecheck");
                if (StringUtils.isNotBlank(paramValue)) {
                    invokeUrl = paramValue;
                    baseData.setHospCode(organ.getHospitalCode());
                    baseDateToString = JSONUtils.toString(baseData);
                }
            }

            if(invokeRecipeAnalysis){
                java.net.URL endpoint;
                endpoint = new java.net.URL(invokeUrl);
                binding2 = (PAWebServiceSoap12Stub) new PAWebServiceLocator().getPAWebServiceSoap12(endpoint);
                if (binding2 != null) {
                    binding2.setTimeout(20000);
                    binding2.getPAResults(1007, baseDateToString, detailsDataToString, getPAResultsResult, uiResults2, hisResults2);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("analysisImpl funId={} error. ", funId, e);
            result.setMsg(errorMsg);

            return result;
        }

        if (null == uiResults && StringUtils.isEmpty(uiResults.value)) {
            LOGGER.warn("analysisImpl funId={} response is null. ", funId);
            result.setMsg(errorMsg);
            return result;
        }

        LOGGER.info("analysisImpl funId={}, 1006-response={}", funId, uiResults.value);

        LOGGER.info("analysisImpl funId={}, 1007-response={}", funId, hisResults2.value);

        List<PAWebMedicines> medicines = new ArrayList<>();
        String brief = null;
        List<PAWebRecipeDanger> recipeDangers = new ArrayList<>();
        try {
            // 将字符串转化成java对象
            PAWebResponse response = JSONObject.parseObject(uiResults.value, PAWebResponse.class);
            medicines = response.getMedicines();
            brief = response.getBrief();
            if(null != hisResults2 && StringUtils.isNotEmpty(hisResults2.value)){
                PAWebRecipeResponse recipeResponse = JSONObject.parseObject(hisResults2.value,PAWebRecipeResponse.class);
                List<PAWebRecipe> issues = recipeResponse.getIssues();
                if(CollectionUtils.isNotEmpty(issues)){
                    issues.forEach(item->{
                        PAWebRecipeDanger paWebRecipeDanger = new PAWebRecipeDanger(item,recipeResponse.getDetailID());
                        recipeDangers.add(paWebRecipeDanger);
                    });
                }
            }
        } catch (Exception e) {
            LOGGER.warn("analysisImpl funId={} error.", funId ,e);
            result.setMsg(errorMsg);
            return result;
        }
        if (CollectionUtils.isNotEmpty(medicines) || CollectionUtils.isNotEmpty(recipeDangers)) {
            result.setMedicines(medicines);
            result.setMsg(brief);
            result.setRecipeDangers(recipeDangers);
        } else if(CollectionUtils.isEmpty(medicines) && CollectionUtils.isEmpty(recipeDangers)) {
            result.setCode(RecipeCommonBaseTO.SUCCESS);
            result.setMsg("系统预审未发现处方问题");
        }
//        Object needInterceptLevel = configService.getConfiguration(recipe.getClinicOrgan(),"needInterceptLevel");
//        result.setHighestDrangeLevel((String)needInterceptLevel);
        Object normalFlowLevel = configService.getConfiguration(recipe.getClinicOrgan(),"normalFlowLevel");
        Object medicineReasonLevel = configService.getConfiguration(recipe.getClinicOrgan(),"medicineReasonLevel");
        Object updateRecipeLevel = configService.getConfiguration(recipe.getClinicOrgan(),"updateRecipeLevel");
        result.setNormalFlowLevel(String.valueOf(normalFlowLevel));
        result.setMedicineReasonLevel(String.valueOf(medicineReasonLevel));
        result.setUpdateRecipeLevel(String.valueOf(updateRecipeLevel));
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
        if (null == patient) {
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

        // 患者信息
        AuditPatient auditPatient = new AuditPatient();
        auditPatient.setDiagnoses(diagnoses);
        auditPatient.setLisForms(lisForms);
        auditPatient.setName(patient.getPatientName());
        auditPatient.setBirthDate(DateConversion.formatDate(patient.getBirthday()));
        auditPatient.setGender(DictionaryController.instance().get("eh.base.dictionary.Gender").getText(patient.getPatientSex()));

        // 过敏源
        if (CollectionUtils.isNotEmpty(recipe.getAllergies())) {
            List<AuditAllergy> auditAllergys = new ArrayList<>();
            recipe.getAllergies().forEach(item -> {
                AuditAllergy auditAllergy = new AuditAllergy();
                auditAllergy.setCode(item.getCode());
                auditAllergy.setName(item.getName());
                auditAllergy.setType(item.getType());
                auditAllergys.add(auditAllergy);
            });
            auditPatient.setAllergies(auditAllergys);
        }


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
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        // 药品信息
        List<AuditMedicine> medicines = new ArrayList<>();
        AuditMedicine medicine;
        for (RecipeDetailBean recipedetail : recipedetails) {
            medicine = new AuditMedicine();
            medicine.setName(recipedetail.getDrugName());
//            Integer targetDrugId = compareDrugDAO.findTargetDrugIdByOriginalDrugId(recipedetail.getDrugId());
//            if (ObjectUtils.isEmpty(targetDrugId)) {
//                OrganDrugList organDrug = organDrugListDAO.getByOrganIdAndOrganDrugCode(recipe.getClinicOrgan(), recipedetail.getOrganDrugCode());
//                if (organDrug != null && StringUtils.isNotEmpty(organDrug.getRegulationDrugCode())) {
//                    medicine.setHisCode(organDrug.getRegulationDrugCode());
//                } else {
//                    medicine.setHisCode(LocalStringUtil.toString(recipedetail.getDrugId()));
//                }
//            } else {
//                medicine.setHisCode(LocalStringUtil.toString(targetDrugId));
//            }
            medicine.setHisCode(String.valueOf(recipedetail.getDrugId()));
            medicine.setSpec(recipedetail.getDrugSpec());
            medicine.setGroup(recipedetail.getDrugGroup());
            medicine.setUnit(recipedetail.getUseDoseUnit());
            medicine.setDose(LocalStringUtil.toString(recipedetail.getUseDose()));
            medicine.setFreq(recipedetail.getUsingRate());
            medicine.setPath(recipedetail.getUsePathways());
            medicine.setDays(LocalStringUtil.toString(recipedetail.getUseDays()));
            medicine.setNeedAlert(RecipeSystemConstant.IS_NEED_ALERT);
            medicine.setDispenseUnit(recipedetail.getDrugUnit());
            medicine.setDispenseAmount(String.valueOf(recipedetail.getPack()));
            if (null != recipe.getSignDate()) {
                medicine.setBegin(DateConversion.getDateFormatter(recipe.getSignDate(), DateConversion.DEFAULT_DATE_TIME));
                medicine.setEnd(DateConversion.getDateFormatter(DateConversion.getDateAftXDays(recipe.getSignDate(), 3),
                        DateConversion.DEFAULT_DATE_TIME));
            }
            medicine.setGroup("1"); //组别默认传"1"
            medicines.add(medicine);
        }
        prescription.setMedicines(medicines);
        prescriptions.add(prescription);

        // 数据详情
        detailsData.setTime(DateConversion.formatDateTimeWithSec(new Date()));
        detailsData.setHospFlag(RecipeSystemConstant.HOSPFLAG_FOR_OP);
        detailsData.setAdmType(RecipeSystemConstant.COMMON_ADM_TYPE);
        detailsData.setAdmNo(recipe.getClinicOrgan() + "-" + recipeTempId);
        detailsData.setPatient(auditPatient);
        detailsData.setPrescriptions(prescriptions);
    }

    /**
     * 获取药品说明页面URL
     *
     * @param drugId
     * @return
     */
    @Override
    public String getDrugSpecification(Integer drugId) {
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

        if (null == uiResults && StringUtils.isEmpty(uiResults.value)) {
            LOGGER.warn("getDrugSpecification funId={} response is null. ", funId);
            return null;
        }
        LOGGER.info("getDrugSpecification funId={}, response={}", funId, uiResults.value);

        String url = null;
        try {
            // 将字符串转化成java对象
            JSONObject json = JSONObject.parseObject(uiResults.value);
            Object nameObj = json.get("FileName");
            if (null == nameObj || StringUtils.isEmpty(nameObj.toString())) {
                return null;
            }
            url = json.get("Link").toString();
            url = PrescriptionConstants.getWeiningPaDetailAddress() + url;
        } catch (Exception e) {
            LOGGER.warn("getDrugSpecification funId={} covert to Object error.", funId,e);
            return null;
        }

        return url;
    }

    public static void main(String[] args) {
        PAWebServiceSoap12Stub binding;
        PAWebServiceSoap12Stub binding2;
        IntHolder getPAResultsResult = new IntHolder();
        StringHolder uiResults = new StringHolder();
        StringHolder hisResults = new StringHolder();
        StringHolder uiResults2 = new StringHolder();
        StringHolder hisResults2 = new StringHolder();
        String baseDateToString = "{\"source\":0,\"hospCode\":\"2019\",\"deptCode\":\"53532\",\"doctCode\":\"189419\"}";
        String detailsDataToString = "{\"time\":\"2020-09-17 21:30:39\",\"hospFlag\":\"op\",\"admType\":\"100\",\"admNo\":\"1004091-d7014bb5849ad758\",\"patient\":{\"name\":\"谢军銮\",\"birthDate\":\"1963-01-21\",\"gender\":\"女\",\"diagnoses\":[{\"type\":\"2\",\"code\":\"J18.900\",\"name\":\"肺炎\"}],\"lisForms\":[]},\"prescriptions\":[{\"id\":\"d7014bb5849ad758\",\"isCur\":\"1\",\"isNew\":\"1\",\"presTime\":\"20200917213039\",\"doctCode\":\"189419\",\"deptCode\":\"53532\",\"medicines\":[{\"name\":\"头孢克洛颗粒\",\"hisCode\":\"105073\",\"spec\":\"0.125g\",\"group\":\"1\",\"unit\":\"g\",\"dose\":\"5.0\",\"freq\":\"BID\",\"path\":\"03\",\"days\":\"3\",\"needAlert\":\"1\",\"dispenseUnit\":\"盒\",\"dispenseAmount\":\"null\"}]}]}";
//        String baseDateToString2 = "{\"Source\":0,\"HospCode\":\"2019\",\"DeptCode\":\"1014\",\"DeptName\":\"产科\",\"DoctCode\":\"00\",\"DoctName\":\"supervisor\",\"DoctType\":\"01\"}";
//        String detailsDataToString2 = "{\"Time\":\"2020-04-21 12:47:01\",\"HospFlag\":\"ip\",\"AdmType\":\"400\",\"AdmNo\":\"96711\",\"LisAdmNo\":\"96711\",\"BedNo\":\"001\",\"AreaCode\":\"1227\",\"InHosNo\":\"0089300\",\"Patient\":{\"Name\":\"查力思\",\"BirthDate\":\"2001-08-12\",\"Gender\":\"女\",\"Weight\":\"\",\"Height\":\"\",\"IdCard\":\"511025200108124940\",\"MRCard\":\"69237\",\"CardType\":\"9\",\"CardNo\":\"00854751\",\"PTime\":\"\",\"PUnit\":\"\",\"IsInfant\":\"0\",\"Allergies\":[],\"Diagnoses\":[{\"Type\":\"2\",\"Name\":\"先兆临产\",\"Code\":\"O47.900x002\"},{\"Type\":\"2\",\"Name\":\"先兆临产\",\"Code\":\"O47.900x002\"},{\"Type\":\"2\",\"Name\":\"宫内孕40+2周G1P0   头位  正常妊娠监督\",\"Code\":\"Z34.900\"},{\"Type\":\"2\",\"Name\":\"妊娠期轻度贫血\",\"Code\":\"O99.000x042\"},{\"Type\":\"2\",\"Name\":\"头盆不称\",\"Code\":\"O33.900x002\"},{\"Type\":\"2\",\"Name\":\"先兆临产\",\"Code\":\"O47.900x002\"},{\"Type\":\"2\",\"Name\":\"宫内孕40+3周G1P0   头位  正常妊娠监督\",\"Code\":\"Z34.900\"},{\"Type\":\"2\",\"Name\":\"妊娠期轻度贫血\",\"Code\":\"O99.000x042\"},{\"Type\":\"2\",\"Name\":\"头盆不称\",\"Code\":\"O33.900x002\"},{\"Type\":\"2\",\"Name\":\"持续性枕后位\",\"Code\":\"O32.803\"},{\"Type\":\"2\",\"Name\":\"宫内孕40+3周G1P1试产失败后剖宫产\",\"Code\":\"O66.401\"},{\"Type\":\"2\",\"Name\":\"妊娠期轻度贫血\",\"Code\":\"O99.000x042\"},{\"Type\":\"2\",\"Name\":\"单一活产  男婴  3430g\",\"Code\":\"Z37.000\"}],\"LisForms\":[]},\"Prescriptions\":[{\"Id\":\"L1848730\",\"Reason\":\"\",\"IsCur\":\"1\",\"IsNew\":\"0\",\"PresType\":\"L\",\"PresTime\":\"2020-04-19 23:24:13\",\"DoctCode\":\"0806\",\"DoctName\":\"石霞霞\",\"DeptCode\":\"1014\",\"DeptName\":\"产科\",\"PharCode\":\"4007\",\"PharName\":\"中心药房\",\"Category\":0,\"IsToGo\":0,\"Medicines\":[{\"Ordinal\":\"0\",\"Name\":\"氯化钠注射液(0.9%)\",\"HisCode\":\"740\",\"MedicareCode\":\"\",\"ApprovalNo\":\"\",\"Spec\":\"100ml:0.9g/瓶\",\"Group\":\"1848730\",\"Reason\":null,\"Unit\":\"ml\",\"Dose\":\"100.000\",\"Freq\":\"02\",\"Path\":\"01\",\"Begin\":\"2020-04-19 23:22:04\",\"End\":null,\"Days\":\"0\",\"PydNo\":null,\"LinkGroup\":null,\"NeedAlert\":\"0\",\"IsSkinTest\":0,\"Notes\":\"\"}]},{\"Id\":\"L1848731\",\"Reason\":\"\",\"IsCur\":\"1\",\"IsNew\":\"0\",\"PresType\":\"L\",\"PresTime\":\"2020-04-19 23:24:14\",\"DoctCode\":\"0806\",\"DoctName\":\"石霞霞\",\"DeptCode\":\"1014\",\"DeptName\":\"产科\",\"PharCode\":\"4007\",\"PharName\":\"中心药房\",\"Category\":0,\"IsToGo\":0,\"Medicines\":[{\"Ordinal\":\"1\",\"Name\":\"注射用头孢哌酮钠舒巴坦钠\",\"HisCode\":\"3062\",\"MedicareCode\":\"\",\"ApprovalNo\":\"\",\"Spec\":\"1.5g*1支/支\",\"Group\":\"1848730\",\"Reason\":null,\"Unit\":\"g\",\"Dose\":\"3.000\",\"Freq\":\"02\",\"Path\":\"01\",\"Begin\":\"2020-04-19 23:22:04\",\"End\":null,\"Days\":\"0\",\"PydNo\":null,\"LinkGroup\":null,\"NeedAlert\":\"0\",\"IsSkinTest\":0,\"Notes\":\"\"}]},{\"Id\":\"L1847464\",\"Reason\":\"\",\"IsCur\":\"1\",\"IsNew\":\"0\",\"PresType\":\"L\",\"PresTime\":\"2020-04-18 23:38:29\",\"DoctCode\":\"1108\",\"DoctName\":\"董娟利\",\"DeptCode\":\"1014\",\"DeptName\":\"产科\",\"PharCode\":\"4007\",\"PharName\":\"中心药房\",\"Category\":0,\"IsToGo\":0,\"Medicines\":[{\"Ordinal\":\"2\",\"Name\":\"益母草注射液\",\"HisCode\":\"2529\",\"MedicareCode\":\"\",\"ApprovalNo\":\"\",\"Spec\":\"1ml*1支/支\",\"Group\":\"\",\"Reason\":null,\"Unit\":\"ml\",\"Dose\":\"2.000\",\"Freq\":\"02\",\"Path\":\"06\",\"Begin\":\"2020-04-18 23:36:24\",\"End\":null,\"Days\":\"0\",\"PydNo\":null,\"LinkGroup\":null,\"NeedAlert\":\"0\",\"IsSkinTest\":0,\"Notes\":\"\"}]},{\"Id\":\"L1847465\",\"Reason\":\"\",\"IsCur\":\"1\",\"IsNew\":\"0\",\"PresType\":\"L\",\"PresTime\":\"2020-04-18 23:38:30\",\"DoctCode\":\"1108\",\"DoctName\":\"董娟利\",\"DeptCode\":\"1014\",\"DeptName\":\"产科\",\"PharCode\":\"4007\",\"PharName\":\"中心药房\",\"Category\":0,\"IsToGo\":0,\"Medicines\":[{\"Ordinal\":\"3\",\"Name\":\"氯化钠注射液(0.9%)\",\"HisCode\":\"748\",\"MedicareCode\":\"\",\"ApprovalNo\":\"\",\"Spec\":\"500ml:4.5g/瓶\",\"Group\":\"1847465\",\"Reason\":null,\"Unit\":\"ml\",\"Dose\":\"500.000\",\"Freq\":\"01\",\"Path\":\"01\",\"Begin\":\"2020-04-18 23:36:24\",\"End\":null,\"Days\":\"0\",\"PydNo\":null,\"LinkGroup\":null,\"NeedAlert\":\"0\",\"IsSkinTest\":0,\"Notes\":\"\"}]},{\"Id\":\"L1847466\",\"Reason\":\"\",\"IsCur\":\"1\",\"IsNew\":\"0\",\"PresType\":\"L\",\"PresTime\":\"2020-04-18 23:38:31\",\"DoctCode\":\"1108\",\"DoctName\":\"董娟利\",\"DeptCode\":\"1014\",\"DeptName\":\"产科\",\"PharCode\":\"4007\",\"PharName\":\"中心药房\",\"Category\":0,\"IsToGo\":0,\"Medicines\":[{\"Ordinal\":\"4\",\"Name\":\"缩宫素注射液\",\"HisCode\":\"499\",\"MedicareCode\":\"\",\"ApprovalNo\":\"\",\"Spec\":\"10U:1ml/支\",\"Group\":\"1847465\",\"Reason\":null,\"Unit\":\"U\",\"Dose\":\"10.000\",\"Freq\":\"01\",\"Path\":\"01\",\"Begin\":\"2020-04-18 23:36:24\",\"End\":null,\"Days\":\"0\",\"PydNo\":null,\"LinkGroup\":null,\"NeedAlert\":\"0\",\"IsSkinTest\":0,\"Notes\":\"\"}]},{\"Id\":\"L2\",\"Reason\":\"\",\"IsCur\":\"1\",\"IsNew\":\"1\",\"PresType\":\"L\",\"PresTime\":\"2020-04-21 12:46:51\",\"DoctCode\":\"00\",\"DoctName\":\"supervisor\",\"DeptCode\":\"1014\",\"DeptName\":\"产科\",\"PharCode\":\"4007\",\"PharName\":\"中心药房\",\"Category\":0,\"IsToGo\":0,\"Medicines\":[{\"Ordinal\":\"5\",\"Name\":\"苯磺酸氨氯地平片\",\"HisCode\":\"3202\",\"MedicareCode\":\"\",\"ApprovalNo\":\"\",\"Spec\":\"5mg*7片/盒\",\"Group\":\"\",\"Reason\":null,\"Unit\":\"mg\",\"Dose\":\"5\",\"Freq\":\"\",\"Path\":\"\",\"Begin\":\"2020-04-21 12:47:01\",\"End\":null,\"Days\":\"1\",\"PydNo\":null,\"LinkGroup\":null,\"NeedAlert\":\"1\",\"IsSkinTest\":0,\"Notes\":\"\"}]}]}";
        try {
            String invokeUrl = "http://101.37.187.40:820/PAWebService.asmx";
            java.net.URL endpoint;
            endpoint = new java.net.URL(invokeUrl);
            binding = (PAWebServiceSoap12Stub) new PAWebServiceLocator().getPAWebServiceSoap12(endpoint);
//            binding2 = (PAWebServiceSoap12Stub) new PAWebServiceLocator().getPAWebServiceSoap12(endpoint);
            if (binding != null) {
                binding.setTimeout(20000);
                binding.getPAResults(1006, baseDateToString, detailsDataToString, getPAResultsResult, uiResults, hisResults);
                System.out.println("--1006-baseData--" + baseDateToString);
                System.out.println("--1006-detailData--" + detailsDataToString);
                System.out.println("--1006-HISResultData--" + JSONUtils.toString(hisResults));
            }
//            if (binding2 != null) {
//                binding2.setTimeout(20000);
//                binding2.getPAResults(1007, baseDateToString, detailsDataToString, getPAResultsResult, uiResults2, hisResults2);
//                System.out.println("--1007-baseData--" + baseDateToString);
//                System.out.println("--1007-detailData--" + detailsDataToString);
//                System.out.println("--1007-HISResultData--" + JSONUtils.toString(hisResults2));
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
