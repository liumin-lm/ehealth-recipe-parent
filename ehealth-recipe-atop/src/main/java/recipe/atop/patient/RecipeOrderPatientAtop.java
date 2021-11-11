package recipe.atop.patient;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.RecipeFeeDTO;
import com.ngari.recipe.dto.SkipThirdDTO;
import com.ngari.recipe.recipe.model.SkipThirdReqVO;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.common.CommonConstant;
import recipe.constant.ErrorCode;
import recipe.core.api.IOrganBusinessService;
import recipe.core.api.patient.IOfflineRecipeBusinessService;
import recipe.core.api.patient.IRecipeOrderBusinessService;
import recipe.util.ValidateUtil;
import recipe.vo.ResultBean;

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
    @Autowired
    private IOfflineRecipeBusinessService offlineToOnlineService;
    @Autowired
    private IOrganBusinessService iOrganBusinessService;

    /**
     * 查询订单 详细费用 (邵逸夫模式专用)
     *
     * @param orderCode
     * @return
     */
    @RpcService

    public Map<String, List<RecipeFeeDTO>> findRecipeOrderDetailFee(String orderCode) {
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
            if (null != pushThirdUrl && new Integer(2) .equals(pushThirdUrl.getType()) ) {
                String msg = StringUtils.isEmpty(pushThirdUrl.getUrl()) ? "请在相关平台查看" : pushThirdUrl.getMsg();
                pushThirdUrl.setMsg(msg);
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


    /**
     * 患者创建订单 根据配送方式上传处方给his
     *
     * @param recipeIds
     * @return
     */
    @RpcService
    public boolean submitRecipeHis(List<Integer> recipeIds, Integer orderId) {
        validateAtop(recipeIds, orderId);
        //过滤按钮
        boolean validate = iOrganBusinessService.giveModeValidate(null, orderId);
        if (!validate) {
            return false;
        }
        //推送his
        recipeIds.forEach(a -> offlineToOnlineService.pushRecipe(a, CommonConstant.RECIPE_PUSH_TYPE, CommonConstant.RECIPE_PATIENT_TYPE));
        return true;
    }

    /**
     * 患者取消订单 根据配送方式上传处方给his 撤销处方
     *
     * @param recipeIds
     * @return
     */
    @RpcService
    public boolean cancelRecipeHis(List<Integer> recipeIds, Integer orderId) {
        validateAtop(recipeIds, orderId);
        //过滤按钮 拿订单的购药方式 过滤
        boolean validate = iOrganBusinessService.giveModeValidate(null, orderId);
        if (!validate) {
            return false;
        }
        //推送his
        recipeIds.forEach(a -> offlineToOnlineService.pushRecipe(a, CommonConstant.RECIPE_CANCEL_TYPE, CommonConstant.RECIPE_PATIENT_TYPE));
        return true;
    }
}
