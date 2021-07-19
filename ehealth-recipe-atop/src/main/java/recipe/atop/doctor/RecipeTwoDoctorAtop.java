package recipe.atop.doctor;

import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.api.open.IRecipeTwoService;
import recipe.constant.ErrorCode;

/**
 * 处方二方服务入口类
 *
 * @author fuzi
 */
@RpcBean("recipeTwoAtop")
public class RecipeTwoDoctorAtop extends BaseAtop {
    @Autowired
    IRecipeTwoService recipeTwoService;

    @RpcService
    public Boolean existUncheckRecipe(Integer bussSource, Integer clinicID) {
        logger.info("existUncheckRecipe bussSource={} clinicID={}", bussSource, clinicID);
        validateAtop(bussSource, clinicID);
        try {
            Boolean result = recipeTwoService.existUncheckRecipe(bussSource, clinicID);
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
