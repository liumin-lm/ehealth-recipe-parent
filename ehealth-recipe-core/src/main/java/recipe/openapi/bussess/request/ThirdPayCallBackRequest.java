package recipe.openapi.bussess.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * @author yinsheng
 * @date 2020\9\22 0022 10:01
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class ThirdPayCallBackRequest extends ThirdBaseRequest implements Serializable{
    private static final long serialVersionUID = -6370059716143115899L;

    private Integer recipeId;

    private String recipeCode;

    private Integer orderId;

    private String payFlag;

    private String msg;

    private String outTradeNo;

    private String tradeNo;

    private Double totalAmount;

    private Double fundAmount;

    private Double cashAmount;

    private Date timestamp;

    private String payway;
}
