package recipe.client;

import com.ngari.base.patient.model.HealthCardBean;
import com.ngari.his.recipe.mode.RecipeRefundReqTO;
import com.ngari.his.recipe.mode.RecipeRefundResTO;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.RefundResultDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.wxpay.service.INgariRefundService;
import ctd.util.JSONUtils;
import eh.utils.MapValueUtil;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 退费相关接口
 *
 * @author yinsheng
 */
@Service
public class RefundClient extends BaseClient {

    @Autowired
    private INgariRefundService refundService;


    /**
     * 退费
     *
     * @param orderId 订单ID
     * @param busType 业务类型
     * @return 退费返回
     */
    public RefundResultDTO refund(Integer orderId, String busType) {
        logger.info("RefundClient refund orderId:{},busType:{}.", orderId, busType);
        RefundResultDTO refundResultDTO = new RefundResultDTO();
        try {
            Map<String, Object> refundResult = refundService.refund(orderId, busType);
            if (null != refundResult) {
                refundResultDTO.setStatus(MapValueUtil.getInteger(refundResult, "status"));
                refundResultDTO.setRefundId(MapValueUtil.getString(refundResult, "refund_id"));
                refundResultDTO.setRefundAmount(MapValueUtil.getString(refundResult, "refund_amount"));
            }
        } catch (Exception e) {
            logger.error("RefundClient refund error orderId:{}", orderId, e);
        }
        logger.info("RefundClient refund refundResultDTO:{}.", JSONUtils.toString(refundResultDTO));
        return refundResultDTO;
    }


    /**
     * 处方退款推送his服务
     */
    public String recipeRefund(Recipe recipe, List<Recipedetail> details, PatientDTO patient, HealthCardBean card) {
        RecipeRefundReqTO requestTO = new RecipeRefundReqTO();
        if (null != recipe) {
            requestTO.setOrganID(String.valueOf(recipe.getClinicOrgan()));
            requestTO.setPatId(recipe.getPatientID());
        }

        if (CollectionUtils.isNotEmpty(details)) {
            requestTO.setInvoiceNo(details.get(0).getPatientInvoiceNo());
        }

        if (null != patient) {
            requestTO.setCertID(patient.getCertificate());
            requestTO.setCertificateType(patient.getCertificateType());
            requestTO.setPatientName(patient.getPatientName());
            requestTO.setPatientSex(patient.getPatientSex());
            requestTO.setMobile(patient.getMobile());
        }

        if (null != card) {
            requestTO.setCardType(card.getCardType());
            requestTO.setCardNo(card.getCardId());
        }
        requestTO.setHoscode("");
        requestTO.setEmpId("");
        logger.info("RefundClient recipeRefund recipeRefund request:{}.", JSONUtils.toString(requestTO));
        try {
            RecipeRefundResTO response = recipeHisService.recipeRefund(requestTO);
            logger.info("RefundClient recipeRefund response={}", JSONUtils.toString(response));
            if (null == response || null == response.getMsgCode()) {
                return "response is null";
            }
            if (0 != response.getMsgCode()) {
                return response.getMsg();
            }
            return "成功";
        } catch (Exception e) {
            logger.info("RefundClient recipeRefund error ", e);
            return e.getMessage();
        }
    }
}
