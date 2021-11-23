package recipe.atop.patient;

import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.patient.IDrugEnterpriseBusinessService;
import recipe.util.ObjectCopyUtils;
import recipe.vo.doctor.ValidateDetailVO;

import java.util.List;

/**
 * 药企相关入口
 *
 * @author fuzi
 */
@RpcBean(value = "drugEnterprisePatientAtop")
public class DrugEnterprisePatientAtop extends BaseAtop {

    @Autowired
    private IDrugEnterpriseBusinessService iDrugEnterpriseBusinessService;


    /**
     * 医生指定药企列表
     *
     * @param validateDetailVO
     * @return
     */
    @RpcService
    public List<EnterpriseStock> enterpriseStockList(ValidateDetailVO validateDetailVO) {
        validateAtop(validateDetailVO, validateDetailVO.getRecipeBean(), validateDetailVO.getRecipeDetails());
        RecipeBean recipeBean = validateDetailVO.getRecipeBean();
        validateAtop(recipeBean.getRecipeType(), recipeBean.getClinicOrgan());
        List<RecipeDetailBean> recipeDetails = validateDetailVO.getRecipeDetails();
        boolean organDrugCode = recipeDetails.stream().anyMatch(a -> StringUtils.isEmpty(a.getOrganDrugCode()));
        if (organDrugCode) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "医院配置药品存在编号为空的数据");
        }
        Recipe recipe = ObjectCopyUtils.convert(recipeBean, Recipe.class);
        List<Recipedetail> detailList = ObjectCopyUtils.convert(validateDetailVO.getRecipeDetails(), Recipedetail.class);
        List<EnterpriseStock> result = iDrugEnterpriseBusinessService.stockList(recipe, detailList);
        result.forEach(a -> {
            a.setDrugsEnterprise(null);
            a.setDrugInfoList(null);
        });
        return result;
    }

}
