package recipe.prescription.service;

import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.LoggerFactory;
import recipe.constant.RecipeSystemConstant;
import recipe.dao.DrugListDAO;
import recipe.prescription.bean.*;
import recipe.util.ApplicationUtils;
import recipe.util.DateConversion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 合理用药服务
 * @author jiangtingfeng
 */
@RpcBean("prescriptionService")
public class PrescriptionService {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PrescriptionService.class);

    /**
     * 医生在开处方时，校验合理用药
     * @param recipe 处方信息
     * @param recipedetails 处方详情
     * @return 返回null表示用药没有异常，反之表示有异常，前端弹框提示，提示信息为返回的字符串信息
     * @throws Exception
     */
    @RpcService
    public String getPAAnalysis(Recipe recipe, List<Recipedetail> recipedetails) throws Exception {
        /*PAWebServiceSoap12Stub binding;
        IntHolder getPAResultsResult = new IntHolder();
        StringHolder uiResults = new StringHolder();
        StringHolder hisResults = new StringHolder();
        BaseData baseData = new BaseData();
        DetailsData detailsData = new DetailsData();

        // 拼接第三方（卫宁）请求参数
        getParams(baseData,detailsData,recipe,recipedetails);
        String baseDateToString = JSONUtils.toString(baseData);
        String detailsDataToString = JSONUtils.toString(detailsData);

        logger.info("request for PAWebService:"+baseDateToString + detailsDataToString);
        try {
            binding = (PAWebServiceSoap12Stub) new PAWebServiceLocator().getPAWebServiceSoap12();
            if(binding!=null){
                // Time out after a minute
                binding.setTimeout(20000);
                binding.getPAResults(1006, baseDateToString, detailsDataToString, getPAResultsResult, uiResults, hisResults);
            }
        } catch (javax.xml.rpc.ServiceException jre) {
            if(jre.getLinkedCause()!=null) {
                logger.error(jre.getLinkedCause().getMessage());
            }
            return null;
        }
        logger.info("response from PAWebService:" + uiResults.value);
        // 将字符串转化成java对象
        JSONObject json = JSONObject.parseObject(uiResults.value);
        if (null == json) {
            return null;
        }
        PAWebResponse res = json.toJavaObject(PAWebResponse.class);
        List<PAWebMedicines> medicines = res.getMedicines();
        if (CollectionUtils.isNotEmpty(medicines) && CollectionUtils.isNotEmpty(medicines.get(0).getIssues())) {
            String drugName = medicines.get(0).getIssues().get(0).getNameA();
            String detal = medicines.get(0).getIssues().get(0).getDetail().replaceAll("\r\n","");
            return drugName + detal;
        }*/
        return null;
    }


    /**
     * 参数构造
     * @param baseData
     * @param detailsData
     * @param recipe
     * @param recipedetails
     * @throws ControllerException
     */
    public void getParams(BaseData baseData, DetailsData detailsData, Recipe recipe,
                          List<Recipedetail> recipedetails) throws ControllerException {

        IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);
        PatientBean patient = iPatientService.get(recipe.getMpiid());

        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        baseData.setDeptCode(String.valueOf(recipe.getDepart()));
        baseData.setDoctCode(String.valueOf(recipe.getDoctor()));
        baseData.setDoctName(recipe.getDoctorName());
        baseData.setHospCode(RecipeSystemConstant.WEINING_HOSPCODE);

        // 过敏源
        List<AuditAllergy> auditAllergys = new ArrayList<>();

        // 诊断信息
        List<AuditDiagnose> diagnoses = new ArrayList<>();
        // 多个诊断的情况
        String s = ";";
        if (recipe.getOrganDiseaseName().contains(s)) {
            String[] a = recipe.getOrganDiseaseName().split(s);
            String[] b = recipe.getOrganDiseaseId().split(s);
            for (int i =0; i< a.length; i++) {
                AuditDiagnose auditDiagnose = new AuditDiagnose();
                auditDiagnose.setType(RecipeSystemConstant.IDC10_DIAGNOSE_TYPE);
                auditDiagnose.setCode(b[i]);
                auditDiagnose.setName(a[i]);
                diagnoses.add(auditDiagnose);
            }
        }
        else{
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
        auditPatient.setAllergies(auditAllergys);
        auditPatient.setDiagnoses(diagnoses);
        auditPatient.setLisForms(lisForms);
        auditPatient.setName(patient.getPatientName());
        auditPatient.setBirthDate(DateConversion.formatDate(patient.getBirthday()));
        auditPatient.setGender(DictionaryController.instance().get("eh.base.dictionary.Gender").getText(patient.getPatientSex()));

        // 药品信息
        List<AuditMedicine> medicines = new ArrayList<>();
        for (Recipedetail recipedetail : recipedetails) {
            DrugList drug = drugListDAO.get(recipedetail.getDrugId());
            AuditMedicine medicine = new AuditMedicine();
            medicine.setName(drug.getDrugName());
            medicine.setHisCode(String.valueOf(recipedetail.getDrugId()));
            medicine.setGroup(recipedetail.getDrugGroup());
            medicine.setUnit(drug.getUseDoseUnit());
            medicine.setDose(String.valueOf(recipedetail.getUseDose()));
            medicine.setFreq(recipedetail.getUsingRate());
            medicine.setPath(recipedetail.getUsePathways());
            medicine.setNeedAlert(RecipeSystemConstant.IS_NEED_ALERT);
            medicines.add(medicine);
        }

        // 处方信息
        List<AuditPrescription> prescriptions = new ArrayList<>();
        AuditPrescription prescription = new AuditPrescription();
        prescription.setPresTime(DateConversion.formatDateTime(new Date()));
        prescription.setIsCur(RecipeSystemConstant.IS_CURRENT_PRESCRIPTION);
        prescription.setIsNew(RecipeSystemConstant.IS_NEW_PRESCRIPTION);
        prescription.setMedicines(medicines);
        prescriptions.add(prescription);

        // 数据详情
        detailsData.setTime(DateConversion.formatDateTimeWithSec(new Date()));
        detailsData.setHospFlag(RecipeSystemConstant.HOSPFLAG_FOR_OP);
        detailsData.setAdmType(RecipeSystemConstant.COMMON_ADM_TYPE);
        detailsData.setAdmNo(DateConversion.formatDateTime(new Date()));
        detailsData.setPatient(auditPatient);
        detailsData.setPrescriptions(prescriptions);
    }
}
