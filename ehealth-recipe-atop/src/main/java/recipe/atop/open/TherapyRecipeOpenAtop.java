package recipe.atop.open;

import ctd.persistence.exception.DAOException;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.api.open.ITherapyRecipeOpenService;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.doctor.ITherapyRecipeBusinessService;

/**
 * 提供复诊关闭调用
 * @author yinsheng
 * @date 2021\8\30 0030 10:09
 */
public class TherapyRecipeOpenAtop extends BaseAtop implements ITherapyRecipeOpenService {

    @Autowired
    private ITherapyRecipeBusinessService therapyRecipeBusinessService;

    @Override
    public boolean abolishTherapyRecipeForRevisitClose(Integer bussSource, Integer clinicId) {
        logger.info("abolishTherapyRecipeForRevisitClose bussSource={} clinicID={}", bussSource, clinicId);
        validateAtop(bussSource, clinicId);
        try {
            //接口结果 True存在 False不存在
            Boolean result = therapyRecipeBusinessService.abolishTherapyRecipeForRevisitClose(bussSource, clinicId);
            logger.info("RecipeOpenAtop existUncheckRecipe result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeOpenAtop existUncheckRecipe error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeOpenAtop existUncheckRecipe error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
