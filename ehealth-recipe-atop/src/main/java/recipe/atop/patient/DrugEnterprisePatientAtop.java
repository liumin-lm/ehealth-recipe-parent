package recipe.atop.patient;

import com.alibaba.fastjson.JSONArray;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.patient.IDrugEnterpriseBusinessService;
import recipe.vo.doctor.ValidateDetailVO;

import java.util.List;

/**
 * 药企相关入口
 * @author fuzi
 */
@RpcBean(value = "drugEnterprisePatientAtop")
public class DrugEnterprisePatientAtop extends BaseAtop {

    @Autowired
    private IDrugEnterpriseBusinessService iDrugEnterpriseBusinessService;

    @RpcService
    public List<EnterpriseStock> enterpriseStockList(ValidateDetailVO validateDetailVO) {
        logger.info("DrugEnterprisePatientAtp enterpriseStockList organId:{}.", validateDetailVO);
        validateAtop(validateDetailVO,validateDetailVO.getRecipeBean(),validateDetailVO.getRecipeDetails());
        RecipeBean recipeBean = validateDetailVO.getRecipeBean();
        validateAtop(recipeBean.getRecipeType(),recipeBean.getClinicOrgan());
        try {
            List<EnterpriseStock> result = iDrugEnterpriseBusinessService.enterpriseStockList(validateDetailVO);
            logger.info("DrugPatientAtop findDrugWithEsByPatient result:{}.", JSONArray.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("DrugPatientAtop findDrugWithEsByPatient error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("DrugPatientAtop findDrugWithEsByPatient error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

}
