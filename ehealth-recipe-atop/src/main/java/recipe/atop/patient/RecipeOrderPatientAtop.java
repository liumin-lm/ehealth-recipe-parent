package recipe.atop.patient;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.RecipeFeeDTO;
import com.ngari.recipe.dto.ShoppingCartDetailDTO;
import com.ngari.recipe.dto.SkipThirdDTO;
import com.ngari.recipe.recipe.model.SkipThirdReqVO;
import com.ngari.recipe.vo.PreOrderInfoReqVO;
import com.ngari.recipe.vo.ShoppingCartReqVO;
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
import recipe.vo.patient.PatientSubmitRecipeVO;
import recipe.vo.second.CheckOrderAddressVo;

import java.util.Date;
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
     * @param recipeIds   处方id
     * @param organId     机构id
     * @param giveModeKey 购药方式key
     */
    @RpcService
    @Deprecated
    public void submitRecipeHis(List<Integer> recipeIds, Integer organId, String giveModeKey) {
        PatientSubmitRecipeVO patientSubmitRecipeVO = new PatientSubmitRecipeVO();
        patientSubmitRecipeVO.setRecipeIds(recipeIds);
        patientSubmitRecipeVO.setOrganId(organId);
        patientSubmitRecipeVO.setGiveModeKey(giveModeKey);
        this.submitRecipeHisNew(patientSubmitRecipeVO);
    }

    /**
     * 患者创建订单 根据配送方式上传处方给his
     *
     * @param patientSubmitRecipeVO   处方
     */
    @RpcService
    public void submitRecipeHisNew(PatientSubmitRecipeVO patientSubmitRecipeVO) {
        validateAtop(patientSubmitRecipeVO, patientSubmitRecipeVO.getRecipeIds(), patientSubmitRecipeVO.getGiveModeKey());
        //兼容老版本 因命名错误
        if (null != patientSubmitRecipeVO.getOrderId()) {
            patientSubmitRecipeVO.setOrganId(patientSubmitRecipeVO.getOrderId());
        }
        //过滤按钮
        boolean validate = iOrganBusinessService.giveModeValidate(patientSubmitRecipeVO.getOrganId(), patientSubmitRecipeVO.getGiveModeKey());
        if (!validate) {
            logger.info("RecipeOrderPatientAtop submitRecipeHis orderId = {} , giveModeKey = {}", patientSubmitRecipeVO.getOrganId(), patientSubmitRecipeVO.getGiveModeKey());
            return;
        }
        //推送his
        patientSubmitRecipeVO.getRecipeIds().forEach(recipeId -> {
            offlineToOnlineService.pushRecipe(recipeId, CommonConstant.RECIPE_PUSH_TYPE, CommonConstant.RECIPE_PATIENT_TYPE,
                    patientSubmitRecipeVO.getExpressFeePayType(), patientSubmitRecipeVO.getExpressFee(), patientSubmitRecipeVO.getGiveModeKey());
            recipeOrderService.updatePdfForSubmitOrderAfter(recipeId);
        });
    }

    /**
     * 门诊缴费 合并支付处方患者端上传his
     *
     * @param recipeIds   处方
     */
    @RpcService
    public void submitRecipeHisV1(List<Integer> recipeIds) {
        validateAtop(recipeIds);
        recipeOrderService.submitRecipeHisV1(recipeIds);
    }

    /**
     * 患者取消订单 根据配送方式上传处方给his 撤销处方
     *
     * @param recipeIds
     * @return
     */
    @RpcService
    public void cancelRecipeHis(List<Integer> recipeIds, Integer orderId) {
        validateAtop(recipeIds, orderId);
        //过滤按钮 拿订单的购药方式 过滤
        boolean validate = iOrganBusinessService.giveModeValidate(orderId);
        if (!validate) {
            logger.info("RecipeOrderPatientAtop cancelRecipeHis orderId = {}", orderId);
            return;
        }

        //推送his
        recipeIds.forEach(recipeId -> offlineToOnlineService.pushRecipe(recipeId, CommonConstant.RECIPE_CANCEL_TYPE,
                CommonConstant.RECIPE_PATIENT_TYPE, null, null, null));

    }

    @RpcService
    public Integer getOrderPayFlag(Integer orderId){
        return iOrganBusinessService.getOrderPayFlag(orderId);
    }

    /**
     * 获取购物车信息
     * @param mpiId
     * @return
     */
    @RpcService
    public List<ShoppingCartDetailDTO> getShoppingCartDetail(String mpiId){
        return recipeOrderService.getShoppingCartDetail(mpiId);
    }

    /**
     * 保存购物车信息
     * @param shoppingCartReqVO
     * @return
     */
    @RpcService
    public void saveRecipeBeforeOrderInfo(ShoppingCartReqVO shoppingCartReqVO){
        validateAtop(shoppingCartReqVO.getRecipeId());
        recipeOrderService.saveRecipeBeforeOrderInfo(shoppingCartReqVO);
    }

    /**
     * 完善购物车信息
     * @param preOrderInfoReqVO
     * @return
     */
    @RpcService
    public String improvePreOrderInfo(PreOrderInfoReqVO preOrderInfoReqVO){
        return recipeOrderService.improvePreOrderInfo(preOrderInfoReqVO);
    }

    /**
     * 获取加入购物车标识
     * @param recipeId
     * @return
     */
    @RpcService
    public Boolean getPreOrderFlag(Integer recipeId,String mpiId){
        return recipeOrderService.getPreOrderFlag(recipeId,mpiId);
    }
    /**
     * 批量校验订单配送地址
     * @param checkOrderAddressVoList
     * @return
     */
    @RpcService
    public String batchCheckSendAddressForOrder(List<CheckOrderAddressVo> checkOrderAddressVoList) {
        return recipeOrderService.batchCheckSendAddressForOrder(checkOrderAddressVoList);
    }

    /**
     * 患者完成订单
     * @param orderCode
     * @return
     */
    @RpcService
    public void patientFinishOrder(String orderCode) {
         recipeOrderService.patientFinishOrder(orderCode);
    }

    /**
     * 拦截患者退费申请
     * @param orderCode
     * @return
     */
    @RpcService
    public Boolean interceptPatientApplyRefund(String orderCode){
        validateAtop(orderCode);
        return recipeOrderService.interceptPatientApplyRefund(orderCode);
    }

    /**
     * 校验订单是否在支付中
     * @param orderId
     * @return
     */
    @RpcService
    public Integer checkOrderPayState(Integer orderId){
        validateAtop(orderId);
        return recipeOrderService.checkOrderPayState(orderId);
    }

    /**
     * 获取复诊提醒时间
     * @param orderCode
     * @return
     */
    @RpcService
    public Date getRevisitRemindTime(List<Integer> recipeIds){
        validateAtop(recipeIds);
        return recipeOrderService.getRevisitRemindTime(recipeIds);
    }

    /**
     * 是否可合并物流单
     * @param addressId 收货地址
     * @param enterpriseId 药企ID
     * @param recipeIdList 处方ID
     * @return 是否可以合并
     */
    @RpcService
    public Boolean mergeTrackingNumber(Integer addressId, Integer enterpriseId, List<Integer> recipeIdList) {
        validateAtop(enterpriseId, recipeIdList);
        return recipeOrderService.mergeTrackingNumber(addressId, enterpriseId, recipeIdList);
    }

}
