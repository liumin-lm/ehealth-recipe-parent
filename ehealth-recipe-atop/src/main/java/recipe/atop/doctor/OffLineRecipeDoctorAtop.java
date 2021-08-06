package recipe.atop.doctor;

import com.ngari.recipe.vo.OffLineRecipeDetailVO;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IRecipeBusinessService;

/**
 * 线下处方服务入口类
 *
 * @date 2021/8/06
 */
@RpcBean("offLineRecipeAtop")
public class OffLineRecipeDoctorAtop extends BaseAtop {
    @Autowired
    private IRecipeBusinessService recipeBusinessService;

    /**
     * 获取线下处方详情
     *
     * @param mpiId       患者ID
     * @param clinicOrgan 机构ID
     * @param recipeCode  处方号码
     * @date 2021/8/06
     */
    @RpcService
    public OffLineRecipeDetailVO getOffLineRecipeDetails(String mpiId, Integer clinicOrgan, String recipeCode) {
        logger.info("OffLineRecipeAtop getOffLineRecipeDetails mpiId={},clinicOrgan={},recipeCode={}", mpiId, clinicOrgan, recipeCode);
        validateAtop(mpiId, clinicOrgan, recipeCode);
        try {
            OffLineRecipeDetailVO result = recipeBusinessService.getOffLineRecipeDetails(mpiId, clinicOrgan, recipeCode);
            logger.info("OffLineRecipeAtop getOffLineRecipeDetails result = {}", JSONUtils.toString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("OffLineRecipeAtop getOffLineRecipeDetails error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("OffLineRecipeAtop getOffLineRecipeDetails error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}