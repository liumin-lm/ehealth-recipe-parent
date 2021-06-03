package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.commonrecipe.model.CommonDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDrugDTO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.service.CommonRecipeService;
import recipe.util.ValidateUtil;

import java.util.List;

/**
 * 常用方服务入口类
 *
 * @author fuzi
 */
@RpcBean("commonRecipeAtop")
public class CommonRecipeAtop extends BaseAtop {
    @Autowired
    private CommonRecipeService commonRecipeService;

    /**
     * 获取常用方列表
     *
     * @param recipeType 处方类型
     * @param doctorId   医生id
     * @param organId    机构id
     * @param start      开始
     * @param limit      分页条数
     * @return ResultBean
     */
    @RpcService
    public List<CommonRecipeDTO> commonRecipeList(Integer organId, Integer doctorId, List<Integer> recipeType, int start, int limit) {
        logger.info("CommonRecipeAtop commonRecipeList organId = {},doctorId = {},recipeType = {},start = {},limit = {}"
                , organId, doctorId, recipeType, start, limit);
        if (null == doctorId && null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        try {
            List<CommonRecipeDTO> result = commonRecipeService.commonRecipeList(organId, doctorId, recipeType, start, limit);
            logger.info("CommonRecipeAtop commonRecipeList result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.warn("CommonRecipeAtop commonRecipeList error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("CommonRecipeAtop commonRecipeList error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 新增或更新常用方  选好药品后将药品加入到常用处方
     *
     * @param commonRecipeDTO 常用方
     * @param drugListDTO     常用方药品
     */
    @RpcService
    public void addCommonRecipe(CommonRecipeDTO commonRecipeDTO, List<CommonRecipeDrugDTO> drugListDTO) {
        logger.info("CommonRecipeAtop addCommonRecipe commonRecipeDTO = {},drugListDTO = {}", JSON.toJSONString(commonRecipeDTO), JSON.toJSONString(drugListDTO));
        if (null == commonRecipeDTO && null == drugListDTO) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        try {
            commonRecipeService.addCommonRecipe(commonRecipeDTO, commonRecipeDTO.getCommonRecipeExt(), drugListDTO);
        } catch (DAOException e1) {
            logger.warn("CommonRecipeAtop addCommonRecipe error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("CommonRecipeAtop addCommonRecipe error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 删除常用方
     *
     * @param commonRecipeId 常用方Id
     */
    @RpcService
    public void deleteCommonRecipe(Integer commonRecipeId) {
        logger.info("CommonRecipeAtop deleteCommonRecipe commonRecipeId = {}", commonRecipeId);
        if (ValidateUtil.integerIsEmpty(commonRecipeId)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        try {
            commonRecipeService.deleteCommonRecipe(commonRecipeId);
        } catch (DAOException e1) {
            logger.warn("CommonRecipeAtop deleteCommonRecipe error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("CommonRecipeAtop deleteCommonRecipe error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }


    public List<CommonDTO> offlineCommon(Integer doctorId) {
        logger.info("CommonRecipeAtop offlineCommon doctorId = {}", doctorId);
        if (ValidateUtil.integerIsEmpty(doctorId)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        try {
            List<CommonDTO> result = commonRecipeService.offlineCommon(doctorId);
            logger.info("CommonRecipeAtop offlineCommon result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.warn("CommonRecipeAtop offlineCommon error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("CommonRecipeAtop offlineCommon error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
