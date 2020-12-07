package recipe.openapi.business.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author yinsheng
 * @date 2020\9\22 0022 10:01
 */
@Data
public class ThirdPayCallBackRequest implements Serializable{
    private static final long serialVersionUID = -6370059716143115899L;

    private String appkey;

    private String tid;

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
