package recipe.atop.greenroom;

import com.ngari.common.dto.Buss2SessionMsg;
import com.ngari.recipe.entity.MedicationSyncConfig;
import com.ngari.recipe.recipe.model.PatientTabStatusMergeRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeOrderWaybillDTO;
import com.ngari.recipe.vo.CodeEnum;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IDrugBusinessService;
import recipe.core.api.IRecipeBusinessService;
import recipe.core.api.greenroom.IRecipeOrderRefundService;
import recipe.core.api.patient.IRecipeOrderBusinessService;
import recipe.util.RecipeBusiThreadPool;
import recipe.vo.ResultBean;
import recipe.vo.greenroom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @Description
 * @Author yzl
 * @Date 2022-06-02
 */
@RpcBean(value = "recipeGmAtop")
public class RecipeGmAtop extends BaseAtop {

    @Autowired
    IRecipeBusinessService recipeBusinessService;

    @Autowired
    private IRecipeOrderBusinessService recipeOrderService;

    @Autowired
    private IDrugBusinessService drugBusinessService;

    @Autowired
    private IRecipeOrderRefundService recipeOrderRefundService;


    /**
     * 运营平台查询处方单用法标签
     *
     * @param recipeId
     * @return
     */
    @RpcService
    public DrugUsageLabelResp queryRecipeDrugUsageLabel(Integer recipeId) {
        validateAtop(recipeId);
        return recipeBusinessService.queryRecipeDrugUsageLabel(recipeId);
    }

    /**
     * 运营平台查询处方单用法标签
     *
     * @param orderId
     * @return
     */
    @RpcService
    public List<DrugUsageLabelResp> queryRecipeDrugUsageLabelByOrder(Integer orderId) {
        validateAtop(orderId);
        return recipeBusinessService.queryRecipeDrugUsageLabelByOrder(orderId);
    }

    /**
     * 订单状态更新
     */
    @RpcService
    public ResultBean updateRecipeOrderStatusAndGiveUser(UpdateOrderStatusVO updateOrderStatusVO) {
        validateAtop(updateOrderStatusVO,updateOrderStatusVO.getRecipeId());
        ResultBean resultBean1 = new ResultBean();
        if (Objects.nonNull(updateOrderStatusVO.getTargetRecipeOrderStatus())) {
            // 更改状态
            ResultBean resultBean = recipeOrderService.updateRecipeOrderStatus(updateOrderStatusVO);
            if(CodeEnum.SERVICE_ERROR.getCode().equals(resultBean.getCode())){
                resultBean1.setCode(resultBean.getCode());
            }
        }
        if (Objects.nonNull(updateOrderStatusVO.getGiveUser())) {
            // 更改发药人
            ResultBean resultBean = recipeOrderService.updateOrderGiveUser(updateOrderStatusVO.getOrderId(), updateOrderStatusVO.getGiveUser());
            if(CodeEnum.SERVICE_ERROR.getCode().equals(resultBean.getCode())){
                resultBean1.setCode(resultBean.getCode());
            }
        }
        if(Objects.nonNull(updateOrderStatusVO.getLogisticsCompany()) && StringUtils.isNotEmpty(updateOrderStatusVO.getTrackingNumber())){
            // 更改物流信息
            recipeOrderService.updateTrackingNumberByOrderId(updateOrderStatusVO);
        }
        return resultBean1;

    }


    /**
     * 获取当前订单用户下历史订单的运单信息
     * @param mpiId
     * @return
     */
    @RpcService
    public List<RecipeOrderWaybillDTO> findOrderByMpiId(String mpiId) {
        validateAtop(mpiId);
        return recipeOrderService.findOrderByMpiId(mpiId);
    }

    @RpcService
    public List<Map<String, Object>> findRecipeListByOrderCode(String orderCode) {
        validateAtop(orderCode);
        return recipeBusinessService.findRecipeDetailsByOrderCode(orderCode);
    }

    @RpcService
    public Boolean makeUpInvoice(String orderCode) {
        validateAtop(orderCode);
        return recipeOrderService.makeUpInvoice(orderCode);
    }

    /**
     * 更新机构数据字典中用药频次、用药途径的同步配置
     * @param medicationSyncConfig
     * @return
     */
    @RpcService
    public Boolean updateMedicationSyncConfig(MedicationSyncConfig medicationSyncConfig){
        validateAtop(medicationSyncConfig);
        return drugBusinessService.updateMedicationSyncConfig(medicationSyncConfig);
    }

    /**
     * 查询机构数据字典中用药频次、用药途径的同步配置
     * @param organId,datatype
     * @return
     */
    @RpcService
    public MedicationSyncConfig getMedicationSyncConfig(Integer organId,Integer datatype){
        validateAtop(organId,datatype);
        return drugBusinessService.getMedicationSyncConfig(organId,datatype);
    }

    /**
     * 定时同步机构数据字典中用药频次、用药途径（每分钟调用一次）
     * 用药途径：以编码加类型作为唯一标识
     * 用药频次：以编码或名称作为唯一标识
     */
    @RpcService
    public List<String> medicationInfoSyncTask(){
        return drugBusinessService.medicationInfoSyncTask();
    }


    @RpcService
    public void testSendMsgToMq(String recipeId, String clinicId, String contentType, String sessionId, Integer doctorId, String mpiId) {
        Buss2SessionMsg msg = new Buss2SessionMsg();
        msg.setBusId(clinicId);
        msg.setContentId(recipeId);
        msg.setContentType(contentType);
        msg.setDoctorId(doctorId);
        msg.setStatus(0);
        msg.setMpiId(mpiId);
        msg.setSessionType(4);
        msg.setSessionId(sessionId);
        recipeBusinessService.sendMsgToMq(recipeId, clinicId, contentType, sessionId, doctorId, mpiId);
    }

    @RpcService
    public List<PatientTabStatusMergeRecipeDTO> findRecipeListForPatientByTabStatus(FindRecipeListForPatientVO param) {
        return recipeBusinessService.findRecipeListForPatientByTabStatus(param);
    }

    /**
     * 药品订单-打印快递面单获取物流订单详情
     * @param orderCode
     */
    @RpcService
    public logisticsOrderInfoVO getLogisticsOrderInfo(String orderCode){
        validateAtop(orderCode);
        return recipeBusinessService.getLogisticsOrderInfo(orderCode);
    }

    /**
     * 运营平台-药品订单列表批量打印发药清单
     *
     * @param orderCodes
     * @param busType
     */
    @RpcService
    public List<DrugDistributionListInfoVO> batchPrintDrugDistributionList(List<String> orderCodes, Integer busType) {
        validateAtop(orderCodes);
        List<DrugDistributionListInfoVO> drugDistributionListInfoVOList = new ArrayList<>();
        List<DrugDistributionCallable> callAbles = new ArrayList<>();

        for (String orderCode : orderCodes) {
            callAbles.add(new DrugDistributionCallable(orderCode, busType));
        }

        if (CollectionUtils.isNotEmpty(callAbles)) {
            try {
                List<Future<DrugDistributionListInfoVO>> list = RecipeBusiThreadPool.submitListReturn(callAbles, 15000);
                list.forEach(a -> {
                    try {
                        drugDistributionListInfoVOList.add(a.get());
                    } catch (Exception e) {
                        logger.error("Future<DrugDistributionListInfoVO>异常", e);
                    }
                });
            } catch (Exception e) {
                logger.error("submitListReturn 线程池异常", e);
            }
        }
//
//            DrugDistributionListInfoVO drugDistributionListInfoVO = new DrugDistributionListInfoVO();
//            //1、处方详情、审核信息
//            List<Map<String, Object>> recipeDetailsAndCheckInfo = recipeBusinessService.findRecipeDetailsByOrderCode(orderCode);
//            if (recipeDetailsAndCheckInfo.size() != 0) {
//                drugDistributionListInfoVO.setRecipeDetailsAndCheckInfo(recipeDetailsAndCheckInfo);
//            }
//            //2、药品订单详情
//            RecipeOrderRefundDetailVO refundOrderDetail = recipeOrderRefundService.getRefundOrderDetail(orderCode, busType);
//            if (Objects.nonNull(refundOrderDetail)) {
//                drugDistributionListInfoVO.setRecipeOrderRefundDetailVO(refundOrderDetail);
//            }
//            //3、物流编码文件流
//            String logisticsOrderNo = recipeOrderService.logisticsOrderNo(orderCode);
//            if(Objects.nonNull(logisticsOrderNo)){
//                drugDistributionListInfoVO.setLogisticsOrderNo(logisticsOrderNo);
//            }
//
//
//            drugDistributionListInfoVOList.add(drugDistributionListInfoVO);
        //  }
        return drugDistributionListInfoVOList;
    }

    /**
     * 运营平台-药品订单列表批量更新是否打印发药清单和快递面单
     *
     * @param orderCodes
     * @param invoiceType
     */
    @RpcService
    public Boolean batchUpdateInvoiceStatus(List<String> orderCodes, Integer invoiceType) {
        Boolean flag = null;
        for (String orderCode : orderCodes) {
            flag = recipeOrderService.updateInvoiceStatus(orderCode, invoiceType);
        }
        return flag;
    }

    class DrugDistributionCallable implements Callable<DrugDistributionListInfoVO> {
        private String orderCode;
        private Integer busType;

        public DrugDistributionCallable(String orderCode, Integer busType) {
            this.orderCode = orderCode;
            this.busType = busType;
        }

        @Override
        public DrugDistributionListInfoVO call() throws Exception {
            DrugDistributionListInfoVO drugDistributionListInfoVO = new DrugDistributionListInfoVO();
            //1、处方详情、审核信息
            List<Map<String, Object>> recipeDetailsAndCheckInfo = recipeBusinessService.findRecipeDetailsByOrderCode(orderCode);
            if (recipeDetailsAndCheckInfo.size() != 0) {
                drugDistributionListInfoVO.setRecipeDetailsAndCheckInfo(recipeDetailsAndCheckInfo);
            }
            //2、药品订单详情
            RecipeOrderRefundDetailVO refundOrderDetail = recipeOrderRefundService.getRefundOrderDetail(orderCode, busType);
            if (Objects.nonNull(refundOrderDetail)) {
                drugDistributionListInfoVO.setRecipeOrderRefundDetailVO(refundOrderDetail);
            }
            //3、物流编码文件流
            String logisticsOrderNo = recipeOrderService.logisticsOrderNo(orderCode);
            if (Objects.nonNull(logisticsOrderNo)) {
                drugDistributionListInfoVO.setLogisticsOrderNo(logisticsOrderNo);
            }
            return drugDistributionListInfoVO;
        }
    }
}
