package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.commonrecipe.model.CommonDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDTO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.doctor.ICommonRecipeBusinessService;

import java.util.LinkedList;
import java.util.List;

/**
 * 常用方服务入口类
 *
 * @author fuzi
 */
@RpcBean("commonRecipeAtop")
public class CommonRecipeDoctorAtop extends BaseAtop {
    @Autowired
    private ICommonRecipeBusinessService commonRecipeService;

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
    @Deprecated
    public List<CommonRecipeDTO> commonRecipeList(Integer organId, Integer doctorId, List<Integer> recipeType, int start, int limit) {
        logger.info("CommonRecipeAtop commonRecipeList organId = {},doctorId = {},recipeType = {},start = {},limit = {}"
                , organId, doctorId, recipeType, start, limit);
        validateAtop(doctorId, organId);
        try {
            List<CommonDTO> resultNew = commonRecipeService.commonRecipeList(organId, doctorId, recipeType, start, limit);
            logger.info("CommonRecipeAtop commonRecipeList resultNew = {}", JSON.toJSONString(resultNew));
            List<CommonRecipeDTO> result = new LinkedList<>();
            resultNew.forEach(a -> {
                CommonRecipeDTO commonRecipeDTO = a.getCommonRecipeDTO();
                commonRecipeDTO.setCommonRecipeExt(a.getCommonRecipeExt());
                commonRecipeDTO.setCommonDrugList(a.getCommonRecipeDrugList());
                result.add(commonRecipeDTO);
            });
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
     * 获取常用方列表
     *
     * @param recipeType 处方类型
     * @param doctorId   医生id
     * @param organId    机构id
     * @param start      开始
     * @param limit      分页条数
     * @return 常用方列表
     */
    @RpcService
    public List<CommonDTO> commonRecipeListV1(Integer organId, Integer doctorId, List<Integer> recipeType, int start, int limit) {
        logger.info("CommonRecipeAtop commonRecipeListV1 organId = {},doctorId = {},recipeType = {},start = {},limit = {}"
                , organId, doctorId, recipeType, start, limit);
        validateAtop(doctorId, organId);
        try {
            List<CommonDTO> result = commonRecipeService.commonRecipeList(organId, doctorId, recipeType, start, limit);
            logger.info("CommonRecipeAtop commonRecipeListV1 result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.warn("CommonRecipeAtop commonRecipeListV1 error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("CommonRecipeAtop commonRecipeListV1 error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 新增或更新常用方  选好药品后将药品加入到常用处方
     *
     * @param common 常用方
     */
    @RpcService
    public void saveCommonRecipe(CommonDTO common) {
        logger.info("CommonRecipeAtop addCommonRecipe common = {}", JSON.toJSONString(common));
        validateAtop("常用方必填参数为空", common, common.getCommonRecipeDTO(), common.getCommonRecipeDrugList());
        CommonRecipeDTO commonRecipe = common.getCommonRecipeDTO();
        validateAtop("常用方必填参数为空", commonRecipe.getDoctorId(), commonRecipe.getRecipeType(), commonRecipe.getCommonRecipeType(), commonRecipe.getCommonRecipeName());

        try {
            commonRecipeService.saveCommonRecipe(common);
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
        validateAtop(commonRecipeId);
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

    /**
     * 查询线下常用方
     *
     * @param organId  机构id
     * @param doctorId 医生id
     * @return 线下常用方数据集合
     */
    @RpcService
    public List<CommonDTO> offlineCommon(Integer organId, Integer doctorId) {
        logger.info("CommonRecipeAtop offlineCommon doctorId = {}", doctorId);
        validateAtop(doctorId, organId);
        try {
            List<CommonDTO> result = commonRecipeService.offlineCommon(organId, doctorId);
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

    /**
     * 添加线下常用方到线上
     *
     * @param commonList 线下常用方数据集合
     * @return boolean
     */
    @RpcService
    public List<String> batchAddOfflineCommon(Integer organId, List<CommonDTO> commonList) {
        logger.info("CommonRecipeAtop addOfflineCommon commonList = {}", JSON.toJSONString(commonList));
        validateAtop(organId, commonList);
        try {
            List<String> result = commonRecipeService.addOfflineCommon(organId, commonList);
            logger.info("CommonRecipeAtop addOfflineCommon result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.warn("CommonRecipeAtop addOfflineCommon error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("CommonRecipeAtop addOfflineCommon error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
