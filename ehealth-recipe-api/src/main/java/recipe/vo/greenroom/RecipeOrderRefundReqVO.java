package recipe.vo.greenroom;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 运营平台处方订单退费
 * @author yinsheng
 * @date 2022/04/07 15:41
 */
@Getter
@Setter
public class RecipeOrderRefundReqVO implements Serializable {
    private static final long serialVersionUID = -3009122469403595160L;

    private Integer busType;
    private Integer organId;
    private Integer orderStatus;
    private Integer payFlag;
    private Integer depId;
    private Integer refundStatus;
    private String giveModeKey;
    private String orderCode;
    private String patientName;
    private String beginTime;
    private String endTime;
    private Integer start;
    private Integer limit;
}
