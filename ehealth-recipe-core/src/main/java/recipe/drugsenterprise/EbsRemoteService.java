package recipe.drugsenterprise;

import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.*;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.EbsBean;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @author yinsheng
 * @date 2020\4\15 0015 14:33
 */
@RpcBean("ebsRemoteService")
public class EbsRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EbsRemoteService.class);

    @Resource
    private RecipeDAO recipeDAO;

    @Resource
    private RecipeDetailDAO recipeDetailDAO;

    @Resource
    private RecipeOrderDAO recipeOrderDAO;

    @Resource
    private RecipeCheckDAO recipeCheckDAO;

    @Resource
    private SaleDrugListDAO saleDrugListDAO;

    @Resource
    private OrganDrugListDAO organDrugListDAO;

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("EbsRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    @RpcService
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("EbsRemoteService.pushRecipeInfo recipeIds:{}", JSONUtils.toString(recipeIds));
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (ObjectUtils.isEmpty(recipeIds)) {
            getDrugEnterpriseResult(result, "处方ID参数为空");
        }
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        if (!CollectionUtils.isEmpty(recipeList)) {
            PatientService patientService = BasicAPI.getService(PatientService.class);
            DoctorService doctorService = BasicAPI.getService(DoctorService.class);
            EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
            OrganService organService = BasicAPI.getService(OrganService.class);
            DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);

            Recipe recipe = recipeList.get(0);
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());

            OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());
            DepartmentDTO departmentDTO = departmentService.get(recipe.getDepart());
            PatientDTO patientDTO = patientService.getByMpiId(recipe.getMpiid());

            EbsBean ebsBean = new EbsBean();
            ebsBean.setPrescripNo(recipe.getRecipeCode());
            ebsBean.setPrescribeDate(recipe.getSignDate().getTime());
            ebsBean.setHospitalCode(organDTO.getOrganizeCode());
            ebsBean.setHospitalName(organDTO.getName());
            ebsBean.setDepartment(departmentDTO.getName());
            ebsBean.setDoctorName(recipe.getDoctorName());
            ebsBean.setName(recipe.getPatientName());
            if (patientDTO != null && new Integer(1).equals(patientDTO.getPatientSex())) {
                ebsBean.setSex(1);
            } else {
                ebsBean.setSex(0);
            }
            ebsBean.setAge(patientDTO.getAge());
            ebsBean.setMobile(patientDTO.getMobile());
            ebsBean.setIdCard(patientDTO.getCertificate());
            ebsBean.setSocialSecurityCard("");
            ebsBean.setAddress("");
            if (recipeOrder != null && new Integer(0).equals(recipeOrder.getOrderType())) {
                ebsBean.setFeeType(0);
            } else {
                ebsBean.setFeeType(1);
            }
            ebsBean.setDistrictName(recipe.getOrganDiseaseName());
            if (recipeOrder != null) {
                ebsBean.setTotalAmount(recipeOrder.getTotalFee());
                ebsBean.setReceiver(recipeOrder.getReceiver());
                ebsBean.setReceiverMobile(recipeOrder.getRecMobile());
                String province = getAddressDic(recipeOrder.getAddress1());
                String city = getAddressDic(recipeOrder.getAddress2());
                String district = getAddressDic(recipeOrder.getAddress3());
                ebsBean.setProvinceName(province);
                ebsBean.setCityName(city);
                ebsBean.setDistrictName(district);
            }


        }
        return null;
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise) {
        return null;
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        return null;
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
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_EBS;
    }

    private DrugEnterpriseResult getDrugEnterpriseResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        LOGGER.info(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
        return result;
    }

    /**
     * 获取区域文本
     * @param area 区域
     * @return     区域文本
     */
    private String getAddressDic(String area) {
        if (StringUtils.isNotEmpty(area)) {
            try {
                return DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(area);
            } catch (ControllerException e) {
                LOGGER.error("getAddressDic 获取地址数据类型失败*****area:" + area);
            }
        }
        return "";
    }
}
