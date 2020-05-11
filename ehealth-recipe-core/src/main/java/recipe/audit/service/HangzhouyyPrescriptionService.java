package recipe.audit.service;

import com.ngari.base.doctor.model.DoctorBean;
import com.ngari.base.doctor.service.IDoctorService;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.his.recipe.mode.*;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.dto.ProTitleDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.service.ProTitleService;
import com.ngari.recipe.common.RecipeCommonBaseTO;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.Dictionary;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.audit.bean.AutoAuditResult;
import recipe.audit.bean.PAWebRecipeDanger;
import recipe.dao.CompareDrugDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.service.RecipeHisService;
import recipe.util.DateConversion;
import recipe.util.LocalStringUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 杭州逸曜合理用药
 */

@RpcBean
public class HangzhouyyPrescriptionService implements IntellectJudicialService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HangzhouyyPrescriptionService.class);

    @Autowired
    private PatientService patientService;

    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    @Autowired
    private IDoctorService doctorService;

    @Autowired
    private ProTitleService proTitleService;

    @Autowired
    private CompareDrugDAO compareDrugDAO;

    @Autowired
    private OrganDrugListDAO organDrugListDAO;

    @Override
    @RpcService
    public AutoAuditResult analysis(RecipeBean recipe, List<RecipeDetailBean> recipedetails) {
        AutoAuditResult result = null;
        if (null == recipe || CollectionUtils.isEmpty(recipedetails)) {
            result.setCode(RecipeCommonBaseTO.FAIL);
            result.setMsg("参数错误");
            return result;
        }
        PatientDTO patient = patientService.getPatientByMpiId(recipe.getMpiid());
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        DoctorBean doctor = doctorService.getBeanByDoctorId(recipe.getDoctor());
        ProTitleDTO proTitle = proTitleService.getById(Integer.valueOf(doctor.getProTitle()));

        try {
            HzyyRationalUseDrugReqTO reqTO = new HzyyRationalUseDrugReqTO();
            reqTO.setOrganId(recipe.getClinicOrgan());

            reqTO.setBase(packBaseData(recipe));

            reqTO.setPatient(packPatientData(patient, recipe, recipeExtend));

            reqTO.setPrescription(packPrescriptionData(recipe, recipedetails, proTitle, doctor));

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
                paWebRecipeDanger.setDangerType(item.getType());
                paWebRecipeDanger.setDangerLevel(item.getSeverity());
                paWebRecipeDanger.setDangerDrug(item.getDrugName());
                paWebRecipeDanger.setDangerDesc(item.getErrorInfo());
                recipeDangers.add(paWebRecipeDanger);
            });
            result.setMsg("查询成功");
            result.setRecipeDangers(recipeDangers);
            return result;
        } catch (Exception e) {
            LOGGER.error("杭州逸曜获取合理用药返回失败，recipeId = {}", recipe.getRecipeId(), e);
            result.setCode(RecipeCommonBaseTO.SUCCESS);
            result.setMsg("系统预审未发现处方问题");
            return result;
        }

    }

    private HzyyBaseData packBaseData(RecipeBean recipe) {
        HzyyBaseData baseData = new HzyyBaseData();
        baseData.setEventNo(String.valueOf(recipe.getRecipeId()));
        baseData.setOrganId(recipe.getClinicOrgan());
        baseData.setPatientId(recipe.getMpiid());
        baseData.setSource("门诊");
        return baseData;
    }

    private HzyyPatientData packPatientData(PatientDTO patient, RecipeBean recipe, RecipeExtend recipeExtend) {
        HzyyPatientData patientData = new HzyyPatientData();
        patientData.setSex(patient.getPatientSex().equals("1") ? "M" : "F");
        patientData.setName(patient.getPatientName());
        patientData.setBirthday(DateConversion.getDateFormatter(patient.getBirthday(), DateConversion.YYYY_MM_DD));
        patientData.setMedCardNo(recipeExtend.getCardNo());
        patientData.setDeptId(String.valueOf(recipe.getDepart()));
        patientData.setIdType("身份证");
        patientData.setIdNo(patient.getIdcard());
//        patientData.setPayType();//病人付费类型，如：自费，市医保，省医保等。
        patientData.setHeight(patient.getHeight());//身高
        patientData.setWeight(patient.getWeight());//体重
        patientData.setPhoneNo(patient.getMobile());
        return patientData;
    }

    private HzyyPrescriptionsData packPrescriptionData(RecipeBean recipe, List<RecipeDetailBean> recipedetails, ProTitleDTO proTitle, DoctorBean doctor) {
        HzyyPrescriptionsData prescription = new HzyyPrescriptionsData();
        prescription.setRecipeId(String.valueOf(recipe.getRecipeId()));
        prescription.setRecipeNo(String.valueOf(recipe.getRecipeId()));
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
        if (null != recipe.getSendDate()) {
            prescription.setRecipeTime(DateConversion.getDateFormatter(recipe.getSendDate(), DateConversion.DEFAULT_DATE_TIME));
        }
        prescription.setRecipeFeeTotal(Double.valueOf(recipe.getTotalMoney().toString()));
        prescription.setRecipeStatus("0");

        List<HzyyPrescriptionDetailData> detailDatas = new ArrayList<>();

        recipedetails.forEach(recipedetail -> {
            HzyyPrescriptionDetailData detailData = new HzyyPrescriptionDetailData();
            detailData.setRecipeItemId(String.valueOf(recipedetail.getRecipeDetailId()));
            detailData.setRecipeId(String.valueOf(recipedetail.getRecipeId()));
            detailData.setGroupNo("1");
            Integer targetDrugId = compareDrugDAO.findTargetDrugIdByOriginalDrugId(recipedetail.getDrugId());
            OrganDrugList organDrug = organDrugListDAO.getByOrganIdAndOrganDrugCode(recipe.getClinicOrgan(), recipedetail.getOrganDrugCode());
            if (ObjectUtils.isEmpty(targetDrugId)) {
                if (organDrug != null && StringUtils.isNotEmpty(organDrug.getRegulationDrugCode())) {
                    detailData.setDrugId(organDrug.getRegulationDrugCode());
                } else {
                    detailData.setDrugId(LocalStringUtil.toString(recipedetail.getDrugId()));
                }
            } else {
                detailData.setDrugId(LocalStringUtil.toString(targetDrugId));
            }
            detailData.setDrugName(recipedetail.getDrugName());
            detailData.setSpecification(recipedetail.getDrugSpec());
            detailData.setManufacturerName(organDrug.getProducer());
            detailData.setDrugDose(String.valueOf(organDrug.getUseDose()));
            try {
                detailData.setDrugAdminRouteName(DictionaryController.instance().get("eh.base.dictionary.UsePathways").getText(recipedetail.getUsePathways()));
            } catch (Exception e) {
                LOGGER.error("get UsePathways error", e);
            }
            detailData.setDrugUsingFreq(recipedetail.getUsingRate());
            detailData.setDespensingNum(recipedetail.getPack());
            detailData.setPackUnit(recipedetail.getDrugUnit());
            detailData.setCountUnit(String.valueOf(recipedetail.getUseTotalDose()));
            detailData.setUnitPrice(Double.valueOf(recipedetail.getDrugCost().toString()));
            detailData.setFeeTotal(Double.valueOf((recipedetail.getDrugCost().
                    multiply(new BigDecimal(recipedetail.getUseTotalDose()))).toString()));
            detailData.setPreparation(organDrug.getDrugForm());
            detailData.setStartTime(DateConversion.getDateFormatter(recipe.getSignDate(), DateConversion.DEFAULT_DATE_TIME));
            detailData.setEndTime(DateConversion.getDateFormatter(DateConversion.getDateAftXDays(recipe.getSignDate(), 3), DateConversion.DEFAULT_DATE_TIME));
        });
        prescription.setPrescriptionItems(detailDatas);
        return prescription;
    }

    @Override
    public String getDrugSpecification(Integer drugId) {
        return null;
    }
}
