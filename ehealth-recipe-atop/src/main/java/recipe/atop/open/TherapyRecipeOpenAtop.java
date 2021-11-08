package recipe.atop.open;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.recipe.model.RecipeTherapyDTO;
import com.ngari.recipe.vo.ItemListVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.api.open.ITherapyRecipeOpenService;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.constant.PageInfoConstant;
import recipe.core.api.doctor.ITherapyRecipeBusinessService;
import recipe.enumerate.status.TherapyStatusEnum;
import recipe.enumerate.type.TherapyCancellationTypeEnum;
import recipe.util.ValidateUtil;

import java.util.List;

/**
 * 提供复诊关闭调用
 *
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
            recipeTherapyDTO.setTherapyCancellationType(TherapyCancellationTypeEnum.HIS_ABOLISH.getType());
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

    /**
     * 运营平台搜索诊疗项目
     *
     * @param itemListVO itemListVO
     * @return List<ItemListVO>
     */
    @RpcService
    public List<ItemListVO> searchItemListByKeyWord(ItemListVO itemListVO) {
        validateAtop(itemListVO, itemListVO.getOrganId());
        try {
            if (ValidateUtil.integerIsEmpty(itemListVO.getStart())) {
                itemListVO.setStart(PageInfoConstant.PAGE_NO);
            }
            if (ValidateUtil.integerIsEmpty(itemListVO.getLimit())) {
                itemListVO.setLimit(PageInfoConstant.PAGE_SIZE);
            }
            List<ItemListVO> result = therapyRecipeBusinessService.searchItemListByKeyWord(itemListVO);
            logger.info("TherapyRecipeOpenAtop searchItemListByKeyWord result:{}.", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.warn("TherapyRecipeOpenAtop searchItemListByKeyWord  error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeOpenAtop searchItemListByKeyWord  error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

}
