package recipe.drugsenterprise.bean;

import com.ngari.base.BaseAPI;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DepartmentService;
import com.ngari.recipe.hisprescription.model.HospitalDrugDTO;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import eh.utils.ChinaIDNumberUtil;
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
        recipeVo.setHiscardno("");
        recipeVo.setPatientname(hospitalRecipeDTO.getPatientName());
        recipeVo.setIdnumber(hospitalRecipeDTO.getCertificate());
        recipeVo.setMobile(hospitalRecipeDTO.getPatientTel());
        recipeVo.setOuthospno(hospitalRecipeDTO.getRegisterId());
        recipeVo.setEmpsex(hospitalRecipeDTO.getPatientSex());
        try{
            String certificate = ChinaIDNumberUtil.convert15To18(hospitalRecipeDTO.getCertificate());
            Integer age = ChinaIDNumberUtil.getAgeFromIDNumber(certificate);
            Date birthday = ChinaIDNumberUtil.getBirthFromIDNumber(certificate);
            recipeVo.setBirthdate(DateConversion.formatDate(birthday));
            recipeVo.setAge(age+"");
        }catch(Exception e){
            LOGGER.info("YdRecipeVO  获取身份信息出错");
        }
        DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
        IOrganService organService = BaseAPI.getService(IOrganService.class);
        OrganBean organ = null;
        List<OrganBean> organList = organService.findByOrganizeCode(hospitalRecipeDTO.getOrganId());
        if (organList != null) {
            organ = organList.get(0);
        }
        DepartmentDTO departmentDTO = departmentService.get(Integer.parseInt(hospitalRecipeDTO.getDepartId()));
        recipeVo.setVisitdate(hospitalRecipeDTO.getCreateDate());
        recipeVo.setPatienttypename("/");
        recipeVo.setMedicarecategname("/");
        recipeVo.setSignalsourcetypename("/");
        recipeVo.setRegistdeptcode(departmentDTO.getProfessionCode());
        recipeVo.setRegistdeptname(departmentDTO.getName());
        recipeVo.setHospital(organ.getName());
        recipeVo.setRegistdrcode(hospitalRecipeDTO.getDoctorNumber());
        recipeVo.setRegistdrname(hospitalRecipeDTO.getDoctorName());
        recipeVo.setRecipebegindate("");
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
        recipeVo.setDiagcode(hospitalRecipeDTO.getOrganDiseaseId());
        recipeVo.setDiagname(hospitalRecipeDTO.getOrganDiseaseName());

        for (HospitalDrugDTO hospitalDrugDTO : hospitalRecipeDTO.getDrugList()) {
            RecipeDtlVo recipeDtlVo = new RecipeDtlVo();
            recipeDtlVo.setRecipedtlno(hospitalDrugDTO.getRecipedtlno());
            recipeDtlVo.setDrugcode(hospitalDrugDTO.getDrugCode());
            recipeDtlVo.setDrugname(hospitalDrugDTO.getDrugName());
            recipeDtlVo.setProdarea("");
            recipeDtlVo.setFactoryname(hospitalDrugDTO.getProducer());
            recipeDtlVo.setDrugspec(hospitalDrugDTO.getSpecification());
            String usingRate = "";
            String usePathways = "";
            try {
                usingRate = DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(hospitalDrugDTO.getUsingRate());
                usePathways = DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getText(hospitalDrugDTO.getUsePathways());
            } catch (ControllerException e) {
                LOGGER.info("YdRecipeVO  药物使用频率使用途径获取失败 error:{}.", e.getMessage());
            }
            recipeDtlVo.setFreqname(usingRate);
            recipeDtlVo.setSustaineddays(hospitalDrugDTO.getUesDays());
            recipeDtlVo.setClasstypeno("/");
            recipeDtlVo.setClasstypename("");
            recipeDtlVo.setQuantity(new BigDecimal(hospitalDrugDTO.getUseDose()));
            recipeDtlVo.setMinunit("口服");
            recipeDtlVo.setUsagename(usePathways);
            recipeDtlVo.setDosage(usingRate);
            recipeDtlVo.setDosageunit(hospitalDrugDTO.getUseDoseUnit());
            List<RecipeDtlVo> recipeDtlVos = new ArrayList<RecipeDtlVo>();
            recipeDtlVos.add(recipeDtlVo);
            recipeVo.setDetaillist(recipeDtlVos);
        }

        return recipeVo;
    }

}
