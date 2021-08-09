package recipe.audit.service;

import com.google.common.base.Splitter;
import com.ngari.base.doctor.model.DoctorBean;
import com.ngari.base.doctor.service.IDoctorService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.his.recipe.mode.*;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.DepartmentService;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.common.RecipeCommonBaseTO;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.dictionary.DictionaryController;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.audit.bean.AutoAuditResult;
import recipe.audit.bean.Issue;
import recipe.audit.bean.PAWebMedicines;
import recipe.audit.bean.PAWebRecipeDanger;
import recipe.bussutil.UsePathwaysFilter;
import recipe.bussutil.UsingRateFilter;
import recipe.constant.RecipeBussConstant;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.manager.EmrRecipeManager;
import recipe.service.RecipeHisService;
import recipe.util.ByteUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * 第三方合理用药
 */
@RpcBean
public class ThirdPartyPrescriptionService implements IntellectJudicialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThirdPartyPrescriptionService.class);

    @Autowired
    private PatientService patientService;
    @Autowired
    private IDoctorService doctorService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private RecipeHisService recipeHisService;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private IConsultExService consultExService;
    @Autowired
    private IRevisitExService revisitExService;
    @Autowired
    private EmploymentService employmentService;
    @Autowired
    private OrganDrugListDAO organDrugListDAO;
    @Autowired
    private IConfigurationCenterUtilsService configService;

    @Override
    @RpcService
    public AutoAuditResult analysis(RecipeBean recipeBean, List<RecipeDetailBean> recipeDetailBeanList) {
        LOGGER.info("analysis param: {}, {}", JSONUtils.toString(recipeBean), JSONUtils.toString(recipeDetailBeanList));
        AutoAuditResult result = new AutoAuditResult();
        if (Objects.isNull(recipeBean) || CollectionUtils.isEmpty(recipeDetailBeanList)) {
            result.setCode(RecipeCommonBaseTO.FAIL);
            result.setMsg("analysis params error");
            return result;
        }
        try {
            PatientDTO patientDTO = Optional.ofNullable(patientService.getPatientByMpiId(recipeBean.getMpiid())).orElseThrow(() -> new DAOException("找不到患者信息"));
            DoctorBean doctorBean = Optional.ofNullable(doctorService.getBeanByDoctorId(recipeBean.getDoctor())).orElseThrow(() -> new DAOException("找不到医生信息"));
            DepartmentDTO departmentDTO = Optional.ofNullable(departmentService.getById(recipeBean.getDepart())).orElseThrow(() -> new DAOException("找不到部门信息"));
            RecipeExtendBean recipeExtendBean = recipeBean.getRecipeExtend();
            if (Objects.isNull(recipeExtendBean) && Objects.nonNull(recipeBean.getRecipeId())) {
                RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeBean.getRecipeId());
                recipeExtendBean = new RecipeExtendBean();
                BeanUtils.copyProperties(recipeExtend, recipeExtendBean);
                Recipe recipeNew = new Recipe();
                ctd.util.BeanUtils.copy(recipeBean, recipeNew);
                EmrRecipeManager.getMedicalInfo(recipeNew, recipeExtend);
                recipeBean.setOrganDiseaseName(recipeNew.getOrganDiseaseName());
                recipeBean.setOrganDiseaseId(recipeNew.getOrganDiseaseId());
            }
            ThirdPartyRationalUseDrugReqTO reqTO;
            ConsultExDTO consultExDTO = null;
            RevisitExDTO revisitExDTO = null;
            reqTO = new ThirdPartyRationalUseDrugReqTO();
            reqTO.setOrganId(recipeBean.getClinicOrgan());
            reqTO.setDeptCode(Objects.nonNull(departmentDTO) ? departmentDTO.getCode() : StringUtils.EMPTY);
            reqTO.setDeptName(Objects.nonNull(departmentDTO) ? departmentDTO.getName() : StringUtils.EMPTY);
            reqTO.setDoctCode(employmentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipeBean.getDoctor(), recipeBean.getClinicOrgan(), recipeBean.getDepart()));
            reqTO.setDoctName(doctorBean.getName());
            if (Objects.nonNull(recipeBean.getClinicId())) {
                if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipeBean.getBussSource())) {
                    revisitExDTO = revisitExService.getByConsultId(recipeBean.getClinicId());
                } else if (RecipeBussConstant.BUSS_SOURCE_WZ.equals(recipeBean.getBussSource())) {
                    consultExDTO = consultExService.getByConsultId(recipeBean.getClinicId());
                }
            }
            reqTO.setThirdPartyBaseData(packThirdPartyBaseData(patientDTO, consultExDTO, revisitExDTO));
            reqTO.setThirdPartyPatientData(packThirdPartyPatientData(patientDTO));
            reqTO.setThirdPartyPrescriptionsData(packThirdPartyPrescriptionData(recipeBean, recipeExtendBean, departmentDTO, doctorBean, recipeDetailBeanList));
            reqTO.setThirdPartyDiagnosisDataList(packThirdPartyDiagnosisData(recipeBean.getOrganDiseaseName(), recipeBean.getOrganDiseaseId()));
            ThirdPartyRationalUseDrugResTO thirdPartyRationalUseDrugResTO = recipeHisService.queryThirdPartyRationalUserDurg(reqTO);
            if (Objects.nonNull(thirdPartyRationalUseDrugResTO)) {
                if (StringUtils.isBlank(thirdPartyRationalUseDrugResTO.getMsg()) && CollectionUtils.isEmpty(thirdPartyRationalUseDrugResTO.getThirdPartyIssuesDataList())) {
                    result.setMsg("系统预审未发现处方问题");
                    result.setCode(RecipeCommonBaseTO.SUCCESS);
                    return result;
                } else if (CollectionUtils.isNotEmpty(thirdPartyRationalUseDrugResTO.getThirdPartyIssuesDataList())) {
                    List<PAWebMedicines> paWebMedicinesList = getPAWebMedicines(thirdPartyRationalUseDrugResTO.getThirdPartyIssuesDataList());
                    result.setMedicines(paWebMedicinesList);
                    List<PAWebRecipeDanger> paWebRecipeDangerList = getPAWebRecipeDangers(thirdPartyRationalUseDrugResTO.getThirdPartyIssuesDataList());
                    result.setRecipeDangers(paWebRecipeDangerList);
                    result.setCode(RecipeCommonBaseTO.FAIL);
//                    Object needInterceptLevel = configService.getConfiguration(recipeBean.getClinicOrgan(), "needInterceptLevel");
//                    if (Objects.nonNull(needInterceptLevel)) {
//                        result.setHighestDrangeLevel((String) needInterceptLevel);
//                    }
                    Object normalFlowLevel = configService.getConfiguration(recipeBean.getClinicOrgan(), "normalFlowLevel");
                    Object medicineReasonLevel = configService.getConfiguration(recipeBean.getClinicOrgan(), "medicineReasonLevel");
                    Object updateRecipeLevel = configService.getConfiguration(recipeBean.getClinicOrgan(), "updateRecipeLevel");
                    result.setNormalFlowLevel(String.valueOf(normalFlowLevel));
                    result.setMedicineReasonLevel(String.valueOf(medicineReasonLevel));
                    result.setUpdateRecipeLevel(String.valueOf(updateRecipeLevel));
                } else if (StringUtils.isNotBlank(thirdPartyRationalUseDrugResTO.getMsg()) && CollectionUtils.isEmpty(thirdPartyRationalUseDrugResTO.getThirdPartyIssuesDataList())) {
                    result.setMsg(thirdPartyRationalUseDrugResTO.getMsg());
                    result.setCode(RecipeCommonBaseTO.FAIL);
                    return result;
                }
            }
            LOGGER.info("analysis result: {}", JSONUtils.toString(result));
            return result;
        } catch (Exception e) {
            LOGGER.error("analysis error, params: {}", JSONUtils.toString(recipeBean), e);
            result.setMsg("智能审方接口异常");
            result.setCode(RecipeCommonBaseTO.ERROR);
            return result;
        }
    }

    /**
     * 封装基本信息
     *
     * @param patientDTO
     * @param consultExDTO
     * @param revisitExDTO
     * @return
     */
    private ThirdPartyBaseData packThirdPartyBaseData(PatientDTO patientDTO, ConsultExDTO consultExDTO, RevisitExDTO revisitExDTO) {
        ThirdPartyBaseData thirdPartyBaseData = new ThirdPartyBaseData();
        if (Objects.nonNull(consultExDTO)) {
            thirdPartyBaseData.setAdmNo(StringUtils.defaultIfBlank(consultExDTO.getRegisterNo(), StringUtils.EMPTY));
        } else if (Objects.nonNull(revisitExDTO)) {
            thirdPartyBaseData.setAdmNo(StringUtils.defaultIfBlank(revisitExDTO.getRegisterNo(), StringUtils.EMPTY));
        }
        thirdPartyBaseData.setName(patientDTO.getPatientName());
        return thirdPartyBaseData;
    }

    /**
     * 封装患者信息
     *
     * @param patientDTO
     * @return
     */
    private ThirdPartyPatientData packThirdPartyPatientData(PatientDTO patientDTO) {
        ThirdPartyPatientData thirdPartyPatientData = new ThirdPartyPatientData();
        thirdPartyPatientData.setIdCard(patientDTO.getIdcard());
        thirdPartyPatientData.setName(patientDTO.getPatientName());
        thirdPartyPatientData.setAddress(patientDTO.getAddress());
        thirdPartyPatientData.setPhone(patientDTO.getMobile());
        thirdPartyPatientData.setGender(patientDTO.getPatientSex());
        thirdPartyPatientData.setAge(String.valueOf(patientDTO.getAge()));
        thirdPartyPatientData.setHeight(StringUtils.defaultIfBlank(patientDTO.getHeight(), StringUtils.EMPTY));
        thirdPartyPatientData.setWeight(StringUtils.defaultIfBlank(patientDTO.getWeight(), StringUtils.EMPTY));
        thirdPartyPatientData.setGender(patientDTO.getPatientSex());
        return thirdPartyPatientData;
    }

    /**
     * 封装处方信息
     *
     * @param recipeBean
     * @param recipeExtendBean
     * @param departmentDTO
     * @param doctorBean
     * @param recipeDetailBeanList
     * @return
     */
    private ThirdPartyPrescriptionsData packThirdPartyPrescriptionData(RecipeBean recipeBean, RecipeExtendBean recipeExtendBean, DepartmentDTO departmentDTO,
                                                                       DoctorBean doctorBean, List<RecipeDetailBean> recipeDetailBeanList) {
        ThirdPartyPrescriptionsData thirdPartyPrescriptionsData = new ThirdPartyPrescriptionsData();
        thirdPartyPrescriptionsData.setId(String.valueOf(recipeBean.getRecipeId()));
        if (Objects.nonNull(recipeExtendBean)) {
            thirdPartyPrescriptionsData.setReason(recipeExtendBean.getHistoryOfPresentIllness());
        }
        thirdPartyPrescriptionsData.setDeptCode(departmentDTO.getCode());
        thirdPartyPrescriptionsData.setDeptName(departmentDTO.getName());
        thirdPartyPrescriptionsData.setDoctCode(doctorBean.getIdNumber());
        thirdPartyPrescriptionsData.setDoctName(doctorBean.getName());
        try {
            thirdPartyPrescriptionsData.setDoctTitle(DictionaryController.instance().get("eh.base.dictionary.JobTitle").getText(doctorBean.getJobTitle()));
        } catch (Exception e) {
            LOGGER.error("analysis packThirdPartyPrescriptionData error, param: {}", doctorBean.getJobTitle(), e);
        }
        List<ThirdPartyMedicinesData> thirdPartyMedicinesDataList = new ArrayList<>();
        recipeDetailBeanList.forEach(recipeDetailBean -> {
            ThirdPartyMedicinesData thirdPartyMedicinesData = new ThirdPartyMedicinesData();
            thirdPartyMedicinesData.setName(recipeDetailBean.getDrugName());
            thirdPartyMedicinesData.setHisCode(recipeDetailBean.getOrganDrugCode());
            if (Objects.nonNull(recipeExtendBean)) {
                thirdPartyMedicinesData.setReason(recipeExtendBean.getHistoryOfPresentIllness());
            }
            if (StringUtils.isNotEmpty(recipeDetailBean.getUseDoseStr())) {
                thirdPartyMedicinesData.setDose(recipeDetailBean.getUseDoseStr());
            } else {
                thirdPartyMedicinesData.setDose((Objects.nonNull(recipeDetailBean.getUseDose())) ? String.valueOf(recipeDetailBean.getUseDose()) : null);
            }
            thirdPartyMedicinesData.setUnit(recipeDetailBean.getUseDoseUnit());
            thirdPartyMedicinesData.setPack(recipeDetailBean.getPack());
            OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(recipeBean.getClinicOrgan(), recipeDetailBean.getOrganDrugCode(), recipeDetailBean.getDrugId());
            if (Objects.nonNull(organDrugList)) {
                thirdPartyMedicinesData.setPackUnit(organDrugList.getUnit());
                thirdPartyMedicinesData.setSpecNum(String.valueOf(organDrugList.getUseDose()));
                thirdPartyMedicinesData.setPrepForm(organDrugList.getDrugForm());
                thirdPartyMedicinesData.setAreaCode(organDrugList.getProducerCode());
                if (StringUtils.isBlank(thirdPartyMedicinesData.getUnit())) {
                    thirdPartyMedicinesData.setUnit(organDrugList.getUseDoseUnit());
                }
            }
            if (StringUtils.isNotBlank(recipeDetailBean.getUsingRate())) {
                thirdPartyMedicinesData.setFreq(UsingRateFilter.filterNgari(recipeBean.getClinicOrgan(), recipeDetailBean.getUsingRate()));
                try {
                    thirdPartyMedicinesData.setFreqName(StringUtils.isNotEmpty(recipeDetailBean.getUsingRateTextFromHis()) ? recipeDetailBean.getUsingRateTextFromHis() : DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(recipeDetailBean.getUsingRate()));
                } catch (Exception e) {
                    LOGGER.error("analysis packThirdPartyPrescriptionData error, param: {}", recipeDetailBean.getUsingRate(), e);
                }
            }
            thirdPartyMedicinesData.setOrganFreq(recipeDetailBean.getOrganUsingRate());
            if (StringUtils.isNotBlank(recipeDetailBean.getUsePathways())) {
                thirdPartyMedicinesData.setPath(UsePathwaysFilter.filterNgari(recipeBean.getClinicOrgan(), recipeDetailBean.getUsePathways()));
            }
            thirdPartyMedicinesData.setOrganPath(recipeDetailBean.getOrganUsePathways());
            thirdPartyMedicinesData.setTotalQty(new BigDecimal(recipeDetailBean.getUseTotalDose()));
            thirdPartyMedicinesData.setDays(String.valueOf(recipeDetailBean.getUseDays()));
            thirdPartyMedicinesData.setSpec(recipeDetailBean.getDrugSpec());
            thirdPartyMedicinesData.setAdminMethod(recipeDetailBean.getMemo());
            thirdPartyMedicinesDataList.add(thirdPartyMedicinesData);
        });
        thirdPartyPrescriptionsData.setThirdPartyMedicinesDataList(thirdPartyMedicinesDataList);
        return thirdPartyPrescriptionsData;
    }

    /**
     * 封装诊断信息
     *
     * @param organDiseaseName
     * @param organDiseaseId
     * @return
     */
    private List<ThirdPartyDiagnosisData> packThirdPartyDiagnosisData(String organDiseaseName, String organDiseaseId) {
        List<ThirdPartyDiagnosisData> thirdPartyDiagnosisDataList = new ArrayList<>();
        if (StringUtils.isNotBlank(organDiseaseName) && StringUtils.isNotBlank(organDiseaseId)) {
            List<String> organDiseaseNameList = Splitter.on(ByteUtils.SEMI_COLON_EN).splitToList(organDiseaseName);
            List<String> organDiseaseIdList = Splitter.on(ByteUtils.SEMI_COLON_EN).splitToList(organDiseaseId);
            if (organDiseaseNameList.size() == organDiseaseIdList.size()) {
                for (int i = 0; i < organDiseaseNameList.size(); i++) {
                    ThirdPartyDiagnosisData thirdPartyDiagnosisData = new ThirdPartyDiagnosisData();
                    thirdPartyDiagnosisData.setName(organDiseaseNameList.get(i));
                    thirdPartyDiagnosisData.setCode(organDiseaseIdList.get(i));
                    // 默认普通诊断
                    thirdPartyDiagnosisData.setType("0");
                    thirdPartyDiagnosisDataList.add(thirdPartyDiagnosisData);
                }
            }
        }
        return thirdPartyDiagnosisDataList;
    }

    /**
     * 解析药品信息
     *
     * @param thirdPartyIssuesDatas
     * @return
     */
    private List<PAWebMedicines> getPAWebMedicines(List<ThirdPartyIssuesData> thirdPartyIssuesDatas) {
        List<PAWebMedicines> paWebMedicinesList = new ArrayList<>();
        thirdPartyIssuesDatas.forEach(thirdPartyIssuesData -> {
            PAWebMedicines paWebMedicines = new PAWebMedicines();
            String name;
            String code;
            if (StringUtils.isNotBlank(thirdPartyIssuesData.getNameA()) && StringUtils.isNotBlank(thirdPartyIssuesData.getNameB())) {
                name = StringUtils.join(thirdPartyIssuesData.getNameA(), "|", thirdPartyIssuesData.getNameB());
            } else {
                name = StringUtils.isNotBlank(thirdPartyIssuesData.getNameA()) ? thirdPartyIssuesData.getNameA() : thirdPartyIssuesData.getNameB();
            }
            if (StringUtils.isNotBlank(thirdPartyIssuesData.getHisCodeA()) && StringUtils.isNotBlank(thirdPartyIssuesData.getHisCodeB())) {
                code = StringUtils.join(thirdPartyIssuesData.getHisCodeA(), "|", thirdPartyIssuesData.getHisCodeB());
            } else {
                code = StringUtils.isNotBlank(thirdPartyIssuesData.getHisCodeA()) ? thirdPartyIssuesData.getHisCodeA() : thirdPartyIssuesData.getHisCodeB();
            }
            paWebMedicines.setName(name);
            paWebMedicines.setCode(code);
            List<Issue> issueList = new ArrayList<>();
            Issue issue = new Issue();
            issue.setLvl(thirdPartyIssuesData.getLvl());
            issue.setLvlCode(thirdPartyIssuesData.getLvlNo());
            issue.setDetail(thirdPartyIssuesData.getSummary());
            issue.setTitle(thirdPartyIssuesData.getType());
            issueList.add(issue);
            paWebMedicines.setIssues(issueList);
            paWebMedicinesList.add(paWebMedicines);
        });
        return paWebMedicinesList;
    }

    /**
     * 解析风险信息
     *
     * @param thirdPartyIssuesDatas
     * @return
     */
    private List<PAWebRecipeDanger> getPAWebRecipeDangers(List<ThirdPartyIssuesData> thirdPartyIssuesDatas) {
        List<PAWebRecipeDanger> paWebRecipeDangerList = new ArrayList<>();
        thirdPartyIssuesDatas.forEach(thirdPartyIssuesData -> {
            PAWebRecipeDanger paWebRecipeDanger = new PAWebRecipeDanger();
            paWebRecipeDanger.setDangerDesc(thirdPartyIssuesData.getSummary());
            paWebRecipeDanger.setDangerDrug(thirdPartyIssuesData.getNameA());
            paWebRecipeDanger.setDangerLevel(thirdPartyIssuesData.getLvlNo());
            paWebRecipeDanger.setDangerType(thirdPartyIssuesData.getType());
            paWebRecipeDanger.setDetailUrl(StringUtils.EMPTY);
            paWebRecipeDangerList.add(paWebRecipeDanger);
        });
        return paWebRecipeDangerList;
    }

    @Override
    public String getDrugSpecification(Integer drugId) {
        return null;
    }
}
