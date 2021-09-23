package recipe.client;

import com.ngari.recipe.dto.RefundResultDTO;
import com.ngari.wxpay.service.INgariRefundService;
import eh.utils.MapValueUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
     * @param orderId 订单ID
     * @param busType 业务类型
     * @return 退费返回
     */
    public RefundResultDTO refund(Integer orderId, String busType){
        logger.info("RefundClient refund orderId:{},busType:{}.", orderId, busType);
        Map<String, Object> refundResult = refundService.refund(orderId, busType);
        RefundResultDTO refundResultDTO = new RefundResultDTO();
        try {
            if (null != refundResult) {
                refundResultDTO.setStatus(MapValueUtil.getInteger(refundResult, "status"));
                refundResultDTO.setRefundId(MapValueUtil.getString(refundResult, "refund_id"));
                refundResultDTO.setRefundAmount(MapValueUtil.getString(refundResult, "refund_amount"));
            }
        } catch (Exception e) {
            logger.error("RefundClient refund error ", e);
        }
        return refundResultDTO;
    }

}
