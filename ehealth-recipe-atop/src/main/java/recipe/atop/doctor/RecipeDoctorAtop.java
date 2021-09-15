package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.RecipeCancelDTO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IRecipeBusinessService;

/**
 * 医生端 处方入口
 *
 * @author fuzi
 */
@RpcBean("recipeDoctorAtop")
public class RecipeDoctorAtop extends BaseAtop {

    @Autowired
    private IRecipeBusinessService recipeBusinessService;

    /**
     * 校验处方能否撤销
     *
     * @param recipeId 处方id
     * @return
     */
    @RpcService
    public RecipeCancelDTO cancelRecipeValidate(Integer recipeId) {
        logger.info("RecipeDoctorAtop cancelRecipeValidate recipeId:{}.", recipeId);
        validateAtop(recipeId);
        try {
            RecipeCancelDTO result = recipeBusinessService.cancelRecipeValidate(recipeId);
            logger.info("RecipeDoctorAtop cancelRecipeValidate result:{}.", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.warn("RecipeDoctorAtop cancelRecipeValidate  error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeDoctorAtop cancelRecipeValidate error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
