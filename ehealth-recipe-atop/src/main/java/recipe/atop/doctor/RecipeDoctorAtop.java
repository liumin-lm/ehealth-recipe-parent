package recipe.atop.doctor;

import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.api.open.IRecipeAtopService;
import recipe.constant.ErrorCode;
import recipe.core.api.IRecipeBusinessService;

/**
 * 处方服务入口类
 *
 * @Date: 2021/7/19
 * @Author: zhaoh
 */
@RpcBean("recipeAtop")
public class RecipeDoctorAtop extends BaseAtop implements IRecipeAtopService {

    @Autowired
    private IRecipeBusinessService recipeBusinessService;

    @Override
    public Boolean existUncheckRecipe(Integer bussSource, Integer clinicId) {
        logger.info("existUncheckRecipe bussSource={} clinicID={}", bussSource, clinicId);
        validateAtop(bussSource, clinicId);
        try {
            //接口结果 True存在 False不存在
            Boolean result = recipeBusinessService.existUncheckRecipe(bussSource, clinicId);
            logger.info("RecipeTwoAtop existUncheckRecipe result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeTwoAtop existUncheckRecipe error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeTwoAtop existUncheckRecipe error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
