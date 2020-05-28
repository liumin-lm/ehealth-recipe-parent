package recipe.audit.service;

import com.ngari.base.doctor.model.DoctorBean;
import com.ngari.base.doctor.service.IDoctorService;
import com.ngari.his.recipe.mode.*;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.DepartmentService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.common.RecipeCommonBaseTO;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.audit.bean.AutoAuditResult;
import recipe.audit.bean.Issue;
import recipe.audit.bean.PAWebMedicines;
import recipe.bussutil.UsePathwaysFilter;
import recipe.bussutil.UsingRateFilter;
import recipe.dao.RecipeExtendDAO;
import recipe.service.RecipeHisService;

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

    @Override
    @RpcService
    public AutoAuditResult analysis(RecipeBean recipeBean, List<RecipeDetailBean> recipeDetailBeanList) {
        AutoAuditResult result = new AutoAuditResult();
        if (Objects.isNull(recipeBean) || CollectionUtils.isEmpty(recipeDetailBeanList)) {
            result.setCode(RecipeCommonBaseTO.FAIL);
            result.setMsg("analysis params error");
            return result;
        }
        PatientDTO patientDTO = Optional.ofNullable(patientService.getPatientByMpiId(recipeBean.getMpiid())).orElseThrow(() -> new DAOException("找不到患者信息"));
        DoctorBean doctorBean = Optional.ofNullable(doctorService.getBeanByDoctorId(recipeBean.getDoctor())).orElseThrow(() -> new DAOException("找不到医生信息"));
        DepartmentDTO departmentDTO = Optional.ofNullable(departmentService.getById(recipeBean.getDepart())).orElseThrow(() -> new DAOException("找不到部门信息"));
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeBean.getRecipeId());
        ThirdPartyRationalUseDrugReqTO reqTO;
        try {
            reqTO = new ThirdPartyRationalUseDrugReqTO();
            reqTO.setOrganId(recipeBean.getClinicOrgan());
            reqTO.setDeptCode((null != departmentDTO) ? departmentDTO.getCode() : StringUtils.EMPTY);
            reqTO.setDeptName((null != departmentDTO) ? departmentDTO.getName() : StringUtils.EMPTY);
            reqTO.setDoctCode(recipeBean.getDoctorName());
            reqTO.setDoctName(doctorBean.getIdNumber());
            reqTO.setThirdPartyBaseData(packThirdPartyBaseData(recipeExtend, patientDTO));
            reqTO.setThirdPartyPatientData(packThirdPartyPatientData(patientDTO));
            reqTO.setThirdPartyPrescriptionsData(packThirdPartyPrescriptionData(recipeBean, recipeExtend, departmentDTO, doctorBean, recipeDetailBeanList));
            ThirdPartyRationalUseDrugResTO thirdPartyRationalUseDrugResTO = recipeHisService.queryThirdPartyRationalUserDurg(reqTO);
            if (Objects.nonNull(thirdPartyRationalUseDrugResTO)) {
                result.setCode(RecipeCommonBaseTO.SUCCESS);
                result.setMsg("系统预审未发现处方问题");
                return result;
            }
            List<PAWebMedicines> paWebMedicinesList = new ArrayList<>();
            thirdPartyRationalUseDrugResTO.getThirdPartyIssuesDataList().forEach(thirdPartyIssuesData -> {
                PAWebMedicines paWebMedicines = new PAWebMedicines();
                String name;
                if (StringUtils.isNotBlank(thirdPartyIssuesData.getNameA()) && StringUtils.isNotBlank(thirdPartyIssuesData.getNameB())) {
                    name = StringUtils.join(thirdPartyIssuesData.getNameA(), thirdPartyIssuesData.getNameB());
                } else {
                    name = StringUtils.isNotBlank(thirdPartyIssuesData.getNameA()) ? thirdPartyIssuesData.getNameA() : thirdPartyIssuesData.getNameB();
                }
                paWebMedicines.setName(name);
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
            result.setMedicines(paWebMedicinesList);
            result.setMsg("查询成功");
            return result;
        } catch (Exception e) {
            LOGGER.error("analysis error, params: {}", JSONUtils.toString(recipeBean), e);
            result.setCode(RecipeCommonBaseTO.SUCCESS);
            result.setMsg("系统预审未发现处方问题");
            return result;
        }
    }

    /**
     * 封装基本信息
     *
     * @param recipeExtend
     * @param patientDTO
     * @return
     */
    private ThirdPartyBaseData packThirdPartyBaseData(RecipeExtend recipeExtend, PatientDTO patientDTO) {
        ThirdPartyBaseData thirdPartyBaseData = new ThirdPartyBaseData();
        thirdPartyBaseData.setAdmNo(recipeExtend.getRegisterNo());
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
        return thirdPartyPatientData;
    }

    /**
     * 封装处方信息
     *
     * @param recipeBean
     * @param recipeExtend
     * @param departmentDTO
     * @param doctorBean
     * @param recipeDetailBeanList
     * @return
     */
    private ThirdPartyPrescriptionsData packThirdPartyPrescriptionData(RecipeBean recipeBean, RecipeExtend recipeExtend, DepartmentDTO departmentDTO,
                                                                       DoctorBean doctorBean, List<RecipeDetailBean> recipeDetailBeanList) {
        ThirdPartyPrescriptionsData thirdPartyPrescriptionsData = new ThirdPartyPrescriptionsData();
        thirdPartyPrescriptionsData.setId(String.valueOf(recipeBean.getRecipeId()));
        thirdPartyPrescriptionsData.setReason(recipeExtend.getHisSettlementNo());
        thirdPartyPrescriptionsData.setDeptCode(departmentDTO.getCode());
        thirdPartyPrescriptionsData.setDeptName(departmentDTO.getName());
        thirdPartyPrescriptionsData.setDoctCode(doctorBean.getIdNumber());
        thirdPartyPrescriptionsData.setDoctName(recipeBean.getDoctorName());
        List<ThirdPartyMedicinesData> thirdPartyMedicinesDataList = new ArrayList<>();
        recipeDetailBeanList.forEach(recipeDetailBean -> {
            ThirdPartyMedicinesData thirdPartyMedicinesData = new ThirdPartyMedicinesData();
            thirdPartyMedicinesData.setName(recipeDetailBean.getDrugName());
            thirdPartyMedicinesData.setHisCode(recipeDetailBean.getDrugCode());
            thirdPartyMedicinesData.setReason(recipeExtend.getHistoryOfPresentIllness());
            thirdPartyMedicinesData.setUnit(recipeDetailBean.getUseDoseUnit());
            if (StringUtils.isNotEmpty(recipeDetailBean.getUseDoseStr())) {
                thirdPartyMedicinesData.setDose(recipeDetailBean.getUseDoseStr());
            } else {
                thirdPartyMedicinesData.setDose((null != recipeDetailBean.getUseDose()) ? Double.toString(recipeDetailBean.getUseDose()) : null);
            }
            thirdPartyMedicinesData.setFreq(UsingRateFilter.filterNgari(recipeBean.getClinicOrgan(), recipeDetailBean.getUsingRate()));
            thirdPartyMedicinesData.setPath(UsePathwaysFilter.filterNgari(recipeBean.getClinicOrgan(), recipeDetailBean.getUsePathways()));
            thirdPartyMedicinesData.setTotalQty(new BigDecimal(recipeDetailBean.getUseTotalDose()));
            thirdPartyMedicinesData.setDays(String.valueOf(recipeDetailBean.getUseDays()));
            thirdPartyMedicinesData.setNeedAlert(recipeDetailBean.getOrganDrugCode());
            thirdPartyMedicinesDataList.add(thirdPartyMedicinesData);
        });
        thirdPartyPrescriptionsData.setThirdPartyMedicinesDataList(thirdPartyMedicinesDataList);
        return thirdPartyPrescriptionsData;
    }

    @Override
    public String getDrugSpecification(Integer drugId) {
        return null;
    }
}
