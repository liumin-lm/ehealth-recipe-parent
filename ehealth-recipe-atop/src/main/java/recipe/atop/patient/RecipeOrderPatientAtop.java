package recipe.atop.patient;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.SkipThirdDTO;
import com.ngari.recipe.recipe.model.SkipThirdReqVO;
import com.ngari.recipe.vo.ResultBean;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.patient.IRecipeOrderBusinessService;
import recipe.util.ValidateUtil;
import com.ngari.recipe.dto.RecipeFeeDTO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 处方订单服务入口类
 *
 * @author fuzi
 */
@RpcBean("recipeOrderAtop")
public class RecipeOrderPatientAtop extends BaseAtop {

    @Autowired
    private IRecipeOrderBusinessService recipeOrderService;

    /**
     * 查询订单 详细费用 (邵逸夫模式专用)
     * @param orderCode
     * @return
     */
    @RpcService
    public Map<String, List<RecipeFeeDTO>> findRecipeOrderDetailFee(String orderCode){
        logger.info("RecipeOrderAtop findRecipeOrderDetailFee orderCode = {}", orderCode);
        if (StringUtils.isEmpty(orderCode)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参为空");
        }
        try {
            List<RecipeFeeDTO> recipeOrderDetailFee = recipeOrderService.findRecipeOrderDetailFee(orderCode);
            Map<String, List<RecipeFeeDTO>> stringListMap = recipeOrderDetailFee.stream().collect(Collectors.groupingBy(RecipeFeeDTO::getFeeType));
            return stringListMap;
        } catch (DAOException e1) {
            logger.error("RecipeOrderAtop updateRecipeOrderStatus error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeOrderAtop updateRecipeOrderStatus error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 订单状态更新
     */
    @RpcService
    public ResultBean updateRecipeOrderStatus(UpdateOrderStatusVO updateOrderStatusVO) {
        logger.info("RecipeOrderAtop updateRecipeOrderStatus updateOrderStatusVO = {}", JSON.toJSONString(updateOrderStatusVO));
        if (ValidateUtil.integerIsEmpty(updateOrderStatusVO.getRecipeId()) || null == updateOrderStatusVO.getTargetRecipeOrderStatus()) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参为空");
        }
        try {
            ResultBean result = recipeOrderService.updateRecipeOrderStatus(updateOrderStatusVO);
            logger.info("RecipeOrderAtop updateRecipeOrderStatus result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeOrderAtop updateRecipeOrderStatus error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeOrderAtop updateRecipeOrderStatus error", e);
            return ResultBean.serviceError(e.getMessage(), false);
        }
    }

    /**
     * 更新核发药师信息
     *
     * @param recipeId
     * @param giveUser
     * @return
     */
    @RpcService
    public ResultBean updateRecipeGiveUser(Integer recipeId, Integer giveUser) {
        logger.info("RecipeOrderAtop updateRecipeGiveUser recipeId = {} giveUser = {}", recipeId, giveUser);
        validateAtop(recipeId, giveUser);
        try {
            ResultBean result = recipeOrderService.updateRecipeGiveUser(recipeId, giveUser);
            logger.info("RecipeOrderAtop updateRecipeGiveUser result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeOrderAtop updateRecipeGiveUser error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeOrderAtop updateRecipeGiveUser error", e);
            return ResultBean.serviceError(e.getMessage(), false);
        }
    }


    /**
     * 跳转到第三方处方购药页面
     *
     * @param skipThirdReqVO 处方ID + 患者选择的购药方式
     * @return 跳转链接地址
     */
    @RpcService
    public SkipThirdDTO skipThirdPage(SkipThirdReqVO skipThirdReqVO) {
        logger.info("RecipeOrderPatientAtop skipThirdPage skipThirdReqVO:{}.", JSON.toJSONString(skipThirdReqVO));
        validateAtop(skipThirdReqVO.getRecipeIds());
        try {
            //上传处方到第三方,上传失败提示HIS返回的失败信息
            SkipThirdDTO pushThirdUrl = recipeOrderService.uploadRecipeInfoToThird(skipThirdReqVO);
            if (null != pushThirdUrl && StringUtils.isNotEmpty(pushThirdUrl.getUrl())) {
                pushThirdUrl.setType(2);
                return pushThirdUrl;
            }
            //获取跳转链接
            SkipThirdDTO skipThirdDTO = recipeOrderService.getSkipUrl(skipThirdReqVO);
            logger.info("RecipeOrderPatientAtop skipThirdPage skipThirdBean:{}.", JSON.toJSONString(skipThirdDTO));
            return skipThirdDTO;
        } catch (DAOException e1) {
            logger.error("RecipeOrderPatientAtop skipThirdPage error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeOrderPatientAtop skipThirdPage error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
