package recipe.audit.service;

import com.ngari.base.doctor.model.DoctorBean;
import com.ngari.base.doctor.service.IDoctorService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.his.recipe.mode.*;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.dto.ProTitleDTO;
import com.ngari.patient.service.DepartmentService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.service.ProTitleService;
import com.ngari.recipe.common.RecipeCommonBaseTO;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.dictionary.DictionaryController;
import ctd.util.AppContextHolder;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.audit.bean.AutoAuditResult;
import recipe.audit.bean.PAWebRecipeDanger;
import recipe.dao.CompareDrugDAO;
import recipe.dao.DrugListDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.manager.EmrRecipeManager;
import recipe.service.RecipeHisService;
import recipe.util.ByteUtils;
import recipe.util.DateConversion;
import recipe.util.DigestUtil;
import recipe.util.LocalStringUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;



/**
 * 杭州逸曜合理用药
 */

@RpcBean
public class HangzhouyiyaoPrescriptionService implements IntellectJudicialService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HangzhouyiyaoPrescriptionService.class);

    @Autowired
    private PatientService patientService;

    @Autowired
    private IDoctorService doctorService;

    @Autowired
    private ProTitleService proTitleService;

    @Autowired
    private CompareDrugDAO compareDrugDAO;

    @Autowired
    private IConfigurationCenterUtilsService configService;

    @Autowired
    private DrugListDAO drugListDAO;

    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    @Override
    @RpcService
    public AutoAuditResult analysis(RecipeBean recipe, List<RecipeDetailBean> recipedetails) {
        AutoAuditResult result = new AutoAuditResult();
        if (null == recipe || CollectionUtils.isEmpty(recipedetails)) {
            result.setCode(RecipeCommonBaseTO.FAIL);
            result.setMsg("参数错误");
            return result;
        }
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        Recipe recipeNew = new Recipe();
        BeanUtils.copy(recipe, recipeNew);
        EmrRecipeManager.getMedicalInfo(recipeNew, recipeExtend);
        recipe.setOrganDiseaseName(recipeNew.getOrganDiseaseName());
        recipe.setOrganDiseaseId(recipeNew.getOrganDiseaseId());
        PatientDTO patient = patientService.getPatientByMpiId(recipe.getMpiid());
        DoctorBean doctor = doctorService.getBeanByDoctorId(recipe.getDoctor());
        ProTitleDTO proTitle = proTitleService.getById(Integer.valueOf(doctor.getProTitle()));
        DepartmentDTO depart = departmentService.getById(recipe.getDepart());

        try {
            HzyyRationalUseDrugReqTO reqTO = new HzyyRationalUseDrugReqTO();
            reqTO.setOrganId(recipe.getClinicOrgan());

            reqTO.setBase(packBaseData(recipe));

            reqTO.setPatient(packPatientData(patient, recipe, depart));

            reqTO.setPrescription(packPrescriptionData(recipe, recipedetails, proTitle, doctor,depart));

            reqTO.setDiagnoses(packDiagnosisData(recipe));
            RecipeHisService recipeHisService = AppContextHolder.getBean("eh.recipeHisService", RecipeHisService.class);
            List<HzyyRationalUseDrugResTO> useDrugResTOS = recipeHisService.queryHzyyRationalUserDurg(reqTO);

            if (CollectionUtils.isEmpty(useDrugResTOS)) {
                result.setCode(RecipeCommonBaseTO.SUCCESS);
                result.setMsg("系统预审未发现处方问题");
                return result;
            }

            List<PAWebRecipeDanger> recipeDangers = new ArrayList<>();
            useDrugResTOS.forEach(item -> {
                PAWebRecipeDanger paWebRecipeDanger = new PAWebRecipeDanger();
                paWebRecipeDanger.setDangerType(item.getAnalysisType());
                paWebRecipeDanger.setDangerLevel(item.getSeverity());
                paWebRecipeDanger.setDangerDrug(item.getDrugName());
                paWebRecipeDanger.setDangerDesc(item.getErrorInfo());
                recipeDangers.add(paWebRecipeDanger);
            });
//            Integer intellectJudicialFlag = (Integer) configService.getConfiguration(recipe.getClinicOrgan(), "intellectJudicialFlag");
//            Object needInterceptLevel = configService.getConfiguration(recipe.getClinicOrgan(), "needInterceptLevel");
//            String highestDrangeLevel = StringUtils.EMPTY;
//            if (!(intellectJudicialFlag == 1 && null != needInterceptLevel
//                    && Integer.valueOf(needInterceptLevel.toString()) > 3)) { //卫宁合理用药配置了等级3以上数据传空
//                highestDrangeLevel = (String) needInterceptLevel;
//            }
//            result.setHighestDrangeLevel(highestDrangeLevel);
            Object normalFlowLevel = configService.getConfiguration(recipe.getClinicOrgan(),"normalFlowLevel");
            Object medicineReasonLevel = configService.getConfiguration(recipe.getClinicOrgan(),"medicineReasonLevel");
            Object updateRecipeLevel = configService.getConfiguration(recipe.getClinicOrgan(),"updateRecipeLevel");
            result.setNormalFlowLevel(String.valueOf(normalFlowLevel));
            result.setMedicineReasonLevel(String.valueOf(medicineReasonLevel));
            result.setUpdateRecipeLevel(String.valueOf(updateRecipeLevel));
            result.setMsg("查询成功");
            result.setRecipeDangers(recipeDangers);
            return result;
        } catch (Exception e) {
            LOGGER.error("杭州逸曜获取合理用药返回失败，recipe = {}", JSONUtils.toString(recipe), e);
            result.setCode(RecipeCommonBaseTO.SUCCESS);
            result.setMsg("系统预审未发现处方问题");
            return result;
        }

    }

    private HzyyBaseData packBaseData(RecipeBean recipe) {
        HzyyBaseData baseData = new HzyyBaseData();
        String recipeTempId = DigestUtil.md5For16(recipe.getClinicOrgan() +
                recipe.getMpiid() + Calendar.getInstance().getTimeInMillis());
        baseData.setEventNo(recipeTempId);
        baseData.setOrganId(recipe.getClinicOrgan());
        baseData.setPatientId(recipe.getMpiid());
        baseData.setSource("门诊");
        return baseData;
    }

    private HzyyPatientData packPatientData(PatientDTO patient, RecipeBean recipe,DepartmentDTO depart) {
        HzyyPatientData patientData = new HzyyPatientData();
        patientData.setSex(patient.getPatientSex().equals("1") ? "M" : "F");
        patientData.setName(patient.getPatientName());
        patientData.setBirthday(DateConversion.getDateFormatter(patient.getBirthday(), DateConversion.YYYY_MM_DD));
//        patientData.setMedCardNo(recipeExtend.getCardNo());
        patientData.setDeptId(String.valueOf(recipe.getDepart()));
        patientData.setIdType("身份证");
        patientData.setIdNo(patient.getIdcard());
        patientData.setPayType("自费");//病人付费类型，如：自费，市医保，省医保等。
        patientData.setHeight(patient.getHeight());//身高
        patientData.setWeight(patient.getWeight());//体重
        patientData.setPhoneNo(patient.getMobile());
        patientData.setEventTime(DateConversion.getDateFormatter(new Date(), DateConversion.DEFAULT_DATE_TIME));
        patientData.setDeptName(depart.getName());
        return patientData;
    }

    private HzyyPrescriptionsData packPrescriptionData(RecipeBean recipe, List<RecipeDetailBean> recipedetails, ProTitleDTO proTitle, DoctorBean doctor,DepartmentDTO department) {
        HzyyPrescriptionsData prescription = new HzyyPrescriptionsData();
        String recipeTempId = DigestUtil.md5For16(recipe.getClinicOrgan() +
                recipe.getMpiid() + Calendar.getInstance().getTimeInMillis());
        prescription.setRecipeId(recipeTempId);
        prescription.setRecipeNo(recipeTempId);
        prescription.setRecipeSource("门诊");
        if (recipe.getRecipeType() != null && recipe.getRecipeType() == 1) {
            prescription.setRecipeType("西药方");
        } else if (recipe.getRecipeType() != null && recipe.getRecipeType() == 2) {
            prescription.setRecipeType("中药方");
        }
        prescription.setDeptId(String.valueOf(recipe.getDepart()));
        prescription.setRecipeDocTitle(proTitle.getText());
        prescription.setRecipeDocId(String.valueOf(recipe.getDoctor()));
        prescription.setRecipeDocName(doctor.getName());
        prescription.setRecipeTime(DateConversion.getDateFormatter(new Date(), DateConversion.DEFAULT_DATE_TIME));
        // prescription.setRecipeFeeTotal(Double.valueOf(recipe.getTotalMoney().toString()));
        prescription.setRecipeStatus("0");
        prescription.setDeptName(department.getName());

        List<HzyyPrescriptionDetailData> detailDatas = new ArrayList<>();

        recipedetails.forEach(recipedetail -> {
            DrugList drugList = drugListDAO.getById(recipedetail.getDrugId());
            HzyyPrescriptionDetailData detailData = new HzyyPrescriptionDetailData();
            String recipeDetailTempId = DigestUtil.md5For16(recipe.getClinicOrgan() +
                    "" + recipedetail.getDrugId());
            detailData.setRecipeItemId(recipeDetailTempId);
            detailData.setRecipeId(recipeTempId);
            detailData.setGroupNo("1");
            Integer targetDrugId = compareDrugDAO.findTargetDrugIdByOriginalDrugId(recipedetail.getDrugId());
            if (ObjectUtils.isEmpty(targetDrugId)) {
                detailData.setDrugId(LocalStringUtil.toString(recipedetail.getDrugId()));
            } else {
                detailData.setDrugId(LocalStringUtil.toString(targetDrugId));
            }
            detailData.setDrugName(recipedetail.getDrugName());
            detailData.setSpecification(recipedetail.getDrugSpec());
            detailData.setManufacturerName(drugList.getProducer());
            detailData.setDrugDose(String.valueOf(drugList.getUseDose()) + drugList.getUseDoseUnit());
            try {
                detailData.setDrugAdminRouteName(DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getText(recipedetail.getUsePathways()));
            } catch (Exception e) {
                LOGGER.error("get UsePathways error", e);
            }
            detailData.setDrugUsingFreq(recipedetail.getUsingRate());

            detailData.setDespensingNum(recipedetail.getUseTotalDose());
            detailData.setPackUnit(drugList.getUnit());
            detailData.setCountUnit(String.valueOf(drugList.getPack()));
            detailData.setSkinTestFlag("0");
            detailData.setDrugReturnFlag("0");
//            detailData.setUnitPrice(Double.valueOf(recipedetail.getDrugCost().toString()));
//            detailData.setFeeTotal(Double.valueOf((recipedetail.getDrugCost().
//                    multiply(new BigDecimal(recipedetail.getUseTotalDose()))).toString()));
            detailData.setPreparation(drugList.getDrugForm());
            detailData.setStartTime(DateConversion.getDateFormatter(new Date(), DateConversion.DEFAULT_DATE_TIME));
            detailData.setEndTime(DateConversion.getDateFormatter(DateConversion.getDateAftXDays(new Date(), 3), DateConversion.DEFAULT_DATE_TIME));
            detailDatas.add(detailData);
        });
        prescription.setPrescriptionItems(detailDatas);
        return prescription;
    }

    private List<HzyyDiagnosisData> packDiagnosisData(RecipeBean recipe) {
        List<HzyyDiagnosisData> diagnoses = new ArrayList<>();
        HzyyDiagnosisData auditDiagnose;
        if (recipe.getOrganDiseaseName().contains(ByteUtils.SEMI_COLON_EN)) {
            String[] a = recipe.getOrganDiseaseName().split(ByteUtils.SEMI_COLON_EN);
            String[] b = recipe.getOrganDiseaseId().split(ByteUtils.SEMI_COLON_EN);
            for (int i = 0; i < a.length; i++) {
                String diagIdTempId = DigestUtil.md5For16(recipe.getClinicOrgan() +
                        "" + b[i]);
                auditDiagnose = new HzyyDiagnosisData();
                auditDiagnose.setDiagCode(b[i]);
                auditDiagnose.setDiagame(a[i]);
                auditDiagnose.setDiagId(diagIdTempId);
                auditDiagnose.setDiagStatus("0");
                auditDiagnose.setDiagDate(DateConversion.getDateFormatter(new Date(), DateConversion.DEFAULT_DATE_TIME));
                diagnoses.add(auditDiagnose);
            }
        } else {
            auditDiagnose = new HzyyDiagnosisData();
            auditDiagnose.setDiagCode(recipe.getOrganDiseaseId());
            auditDiagnose.setDiagame(recipe.getOrganDiseaseName());
            auditDiagnose.setDiagStatus("0");
            auditDiagnose.setDiagDate(DateConversion.getDateFormatter(new Date(), DateConversion.DEFAULT_DATE_TIME));
            diagnoses.add(auditDiagnose);
        }
        return diagnoses;
    }

    @Override
    public String getDrugSpecification(Integer drugId) {
        return null;
    }
}
