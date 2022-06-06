package recipe.atop.greenroom;

import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.drugsenterprise.service.IDrugsEnterpriseService;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.service.IRecipeService;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IRecipeDetailBusinessService;
import recipe.vo.greenroom.DrugUsageLabelResp;

import java.util.List;
import java.util.Objects;

/**
 * @Description
 * @Author yzl
 * @Date 2022-06-02
 */
@RpcBean(value = "recipeGmAtop")
public class RecipeGmAtop extends BaseAtop {

    @Autowired
    IRecipeService recipeService;

    @Autowired
    IDrugsEnterpriseService enterpriseService;

    @Autowired
    PatientService patientService;

    @Autowired
    IRecipeDetailBusinessService recipeDetailService;


    /**
     * 运营平台查询处方单用法标签
     *
     * @param recipeId
     * @return
     */
    @RpcService
    public DrugUsageLabelResp queryRecipeDrugUsageLabel(Integer recipeId) {
        DrugUsageLabelResp drugUsageLabelResp = new DrugUsageLabelResp();
        RecipeBean recipeBean = recipeService.getByRecipeId(recipeId);
        Integer enterpriseId = recipeBean.getEnterpriseId();
        if (Objects.isNull(enterpriseId)) {
            throw new DAOException("未查询到对应药企！");
        }
        DrugsEnterpriseBean drugsEnterpriseBean = enterpriseService.getByEnterpriseCode(enterpriseId);
        drugUsageLabelResp.setEnterpriseName(drugsEnterpriseBean.getName());

        PatientDTO patientDTO = patientService.getPatientBeanByMpiId(recipeBean.getMpiid());
        if (Objects.nonNull(patientDTO)) {
            drugUsageLabelResp.setPatientName(patientDTO.getPatientName());
            drugUsageLabelResp.setPatientAge(patientDTO.getAgeString());
            drugUsageLabelResp.setPatientSex(patientDTO.getPatientSex());
        }

        List<RecipeDetailBean> recipeDetailBeans = recipeDetailService.findRecipeDetailsByRecipeId(recipeId);
        if (CollectionUtils.isNotEmpty(recipeDetailBeans)) {
            drugUsageLabelResp.setDrugUsageLabelList(recipeDetailBeans);
        }
        return drugUsageLabelResp;
    }

}
