package recipe.atop.patient;

import com.alibaba.fastjson.JSONArray;
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
import recipe.core.api.IOrganBusinessService;
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
    @Autowired
    private IOrganBusinessService organBusinessService;

    @RpcService
    public List<EnterpriseStock> enterpriseStockList(ValidateDetailVO validateDetailVO) {
        logger.info("DrugEnterprisePatientAtop enterpriseStockList organId:{}.", validateDetailVO);
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
        try {
            //药企库存
            List<EnterpriseStock> result = iDrugEnterpriseBusinessService.enterpriseStockCheck(recipe, detailList);
            //医院库存
            EnterpriseStock enterpriseStock = organBusinessService.organStock(recipe, detailList);
            result.add(enterpriseStock);
            logger.info("DrugEnterprisePatientAtop enterpriseStockList result:{}.", JSONArray.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.warn("DrugEnterprisePatientAtop enterpriseStockList error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("DrugEnterprisePatientAtop enterpriseStockList error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

}
