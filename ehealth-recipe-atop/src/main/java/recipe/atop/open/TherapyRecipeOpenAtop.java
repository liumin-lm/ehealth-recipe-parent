package recipe.atop.open;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.recipe.model.RecipeTherapyDTO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.api.open.ITherapyRecipeOpenService;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.doctor.ITherapyRecipeBusinessService;
import recipe.enumerate.status.TherapyStatusEnum;

/**
 * 提供复诊关闭调用
 * @author yinsheng
 * @date 2021\8\30 0030 10:09
 */
@RpcBean("remoteTherapyRecipeOpenService")
public class TherapyRecipeOpenAtop extends BaseAtop implements ITherapyRecipeOpenService {

    @Autowired
    private ITherapyRecipeBusinessService therapyRecipeBusinessService;

    @Override
    public boolean abolishTherapyRecipeForRevisitClose(Integer bussSource, Integer clinicId) {
        logger.info("TherapyRecipeOpenAtop abolishTherapyRecipeForRevisitClose bussSource={} clinicID={}", bussSource, clinicId);
        validateAtop(bussSource, clinicId);
        try {
            //接口结果 True存在 False不存在
            Boolean result = therapyRecipeBusinessService.abolishTherapyRecipeForRevisitClose(bussSource, clinicId);
            logger.info("TherapyRecipeOpenAtop abolishTherapyRecipeForRevisitClose result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.error("TherapyRecipeOpenAtop abolishTherapyRecipeForRevisitClose error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeOpenAtop abolishTherapyRecipeForRevisitClose error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    @Override
    public boolean abolishTherapyRecipeForHis(Integer organId, String recipeCode) {
        logger.info("TherapyRecipeOpenAtop abolishTherapyRecipeForHis organId={} recipeCode={}", organId, recipeCode);
        validateAtop(organId, recipeCode);
        try {
            RecipeTherapyDTO recipeTherapyDTO = new RecipeTherapyDTO();
            recipeTherapyDTO.setStatus(TherapyStatusEnum.HADECANCEL.getType());
            recipeTherapyDTO.setTherapyCancellationType(2);
            Boolean result = therapyRecipeBusinessService.updateTherapyRecipe(organId, recipeCode, recipeTherapyDTO);
            logger.info("TherapyRecipeOpenAtop abolishTherapyRecipeForHis result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.error("TherapyRecipeOpenAtop abolishTherapyRecipeForHis error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeOpenAtop abolishTherapyRecipeForHis error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    @Override
    public boolean therapyPayNotice(Integer organId, String recipeCode, RecipeTherapyDTO recipeTherapyDTO) {
        logger.info("TherapyRecipeOpenAtop therapyPayNotice organId={} recipeCode={}, recipeTherapyDTO:{}.", organId, recipeCode, JSON.toJSON(recipeTherapyDTO));
        validateAtop(organId, recipeCode, recipeTherapyDTO);
        try {
            recipeTherapyDTO.setStatus(TherapyStatusEnum.HADEPAY.getType());
            Boolean result = therapyRecipeBusinessService.updateTherapyRecipe(organId, recipeCode, recipeTherapyDTO);
            logger.info("TherapyRecipeOpenAtop therapyPayNotice result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.error("TherapyRecipeOpenAtop therapyPayNotice error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeOpenAtop therapyPayNotice error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }


}
