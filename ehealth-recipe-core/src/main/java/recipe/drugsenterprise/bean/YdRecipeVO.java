package recipe.drugsenterprise.bean;

import com.ngari.base.BaseAPI;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.recipe.hisprescription.model.HospitalDrugDTO;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import eh.utils.ChinaIDNumberUtil;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.drugsenterprise.bean.yd.model.RecipeDtlVo;
import recipe.drugsenterprise.bean.yd.model.RecipeVo;
import recipe.util.DateConversion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class YdRecipeVO {

    private static final Logger LOGGER = LoggerFactory.getLogger(YdRecipeVO.class);

    public static RecipeVo getRecipeVo(HospitalRecipeDTO hospitalRecipeDTO){
        RecipeVo recipeVo = new RecipeVo();
        recipeVo.setRecipeno(hospitalRecipeDTO.getRecipeCode());
        recipeVo.setCaseno(hospitalRecipeDTO.getPatientNumber());
        recipeVo.setHiscardno(hospitalRecipeDTO.getClinicId());
        recipeVo.setPatientname(hospitalRecipeDTO.getPatientName());
        recipeVo.setIdnumber(hospitalRecipeDTO.getCertificate());
        recipeVo.setMobile(hospitalRecipeDTO.getPatientTel());
        recipeVo.setOuthospno(hospitalRecipeDTO.getRegisterId());
        recipeVo.setEmpsex("1".equals(hospitalRecipeDTO.getPatientSex()) ? "男" : "女");
        recipeVo.setRecipe_source_flag(hospitalRecipeDTO.getRecipeSourceFlag());
        try {
            String certificate = ChinaIDNumberUtil.convert15To18(hospitalRecipeDTO.getCertificate());
            Integer age = ChinaIDNumberUtil.getAgeFromIDNumber(certificate);
            Date birthday = ChinaIDNumberUtil.getBirthFromIDNumber(certificate);
            recipeVo.setBirthdate(DateConversion.formatDate(birthday));
            recipeVo.setAge(age + "");
        } catch (Exception e) {
            LOGGER.info("YdRecipeVO  获取身份信息出错",e);
        }

        //转换组织结构编码
        Integer clinicOrgan = null;
        OrganBean organ = null;
        try {
            organ = getOrganByOrganId(hospitalRecipeDTO.getOrganId());
            if (null != organ) {
                clinicOrgan = organ.getOrganId();
            }
        } catch (Exception e) {
            LOGGER.warn("createPrescription 查询机构异常，organId={}", hospitalRecipeDTO.getOrganId(), e);
        } finally {
            if (null == clinicOrgan) {
                LOGGER.warn("createPrescription 平台未匹配到该组织机构编码，organId={}", hospitalRecipeDTO.getOrganId());
            }
        }

        recipeVo.setVisitdate(hospitalRecipeDTO.getCreateDate());
        recipeVo.setRecipebegindate(hospitalRecipeDTO.getCreateDate());
        recipeVo.setPatienttypename("/");
        recipeVo.setMedicarecategname("/");
        recipeVo.setSignalsourcetypename("/");
        recipeVo.setHospital(organ.getName());
        recipeVo.setRegistdeptcode(hospitalRecipeDTO.getDepartId());
        recipeVo.setRegistdeptname(hospitalRecipeDTO.getDepartName());
        recipeVo.setRegistdrcode(hospitalRecipeDTO.getDoctorNumber());
        recipeVo.setRegistdrname(hospitalRecipeDTO.getDoctorName());
        recipeVo.setDoctorName(hospitalRecipeDTO.getDoctorName());
        recipeVo.setRecipeenddate("");
        recipeVo.setContactname(hospitalRecipeDTO.getPatientName());
        recipeVo.setContactaddr(hospitalRecipeDTO.getPatientAddress());
        recipeVo.setContactphone("");
        recipeVo.setCountry("");
        recipeVo.setProvince("");
        recipeVo.setCity("");
        recipeVo.setAddress("");
        recipeVo.setPaydate("");
        recipeVo.setPaystatus("1");
        recipeVo.setStoreno("");
        recipeVo.setPatientno(hospitalRecipeDTO.getPatientId());
        recipeVo.setDiagcode(hospitalRecipeDTO.getOrganDiseaseId());
        recipeVo.setDiagname(hospitalRecipeDTO.getOrganDiseaseName());
        List<RecipeDtlVo> recipeDtlVos = new ArrayList<RecipeDtlVo>();
        for (HospitalDrugDTO hospitalDrugDTO : hospitalRecipeDTO.getDrugList()) {
            RecipeDtlVo recipeDtlVo = new RecipeDtlVo();
            recipeDtlVo.setRecipedtlno(hospitalDrugDTO.getRecipedtlno());
            recipeDtlVo.setDrugcode(hospitalDrugDTO.getDrugCode());
            recipeDtlVo.setDrugname(hospitalDrugDTO.getDrugName());
            recipeDtlVo.setProdarea("");
            recipeDtlVo.setFactoryname(hospitalDrugDTO.getProducer());
            recipeDtlVo.setDrugspec(hospitalDrugDTO.getSpecification());
            recipeDtlVo.setFreqname(hospitalDrugDTO.getUsingRate());
            recipeDtlVo.setSustaineddays(hospitalDrugDTO.getUesDays());
            recipeDtlVo.setClasstypeno("/");
            recipeDtlVo.setClasstypename("");
            recipeDtlVo.setQuantity(new BigDecimal(hospitalDrugDTO.getTotal()));
            recipeDtlVo.setMinunit("");
            recipeDtlVo.setUsagename(hospitalDrugDTO.getUsePathways());
            recipeDtlVo.setUnitprice(new BigDecimal(hospitalDrugDTO.getDrugFee()));
            recipeDtlVo.setDosage(hospitalDrugDTO.getUseDose());
            recipeDtlVo.setDosageunit(hospitalDrugDTO.getUseDoseUnit());
            recipeDtlVo.setMeasurement(hospitalDrugDTO.getUseDose());
            recipeDtlVo.setMeasurementunit(hospitalDrugDTO.getUseDoseUnit());
            recipeDtlVo.setDrugunit(hospitalDrugDTO.getDrugUnit());
            recipeDtlVos.add(recipeDtlVo);
        }
        recipeVo.setDetaillist(recipeDtlVos);
        return recipeVo;
    }

    private static OrganBean getOrganByOrganId(String organId) throws Exception {
        IOrganService organService = BaseAPI.getService(IOrganService.class);
        OrganBean organ = null;
        List<OrganBean> organList = organService.findByOrganizeCode(organId);
        if (CollectionUtils.isNotEmpty(organList)) {
            organ = organList.get(0);
        }

        return organ;
    }

}
