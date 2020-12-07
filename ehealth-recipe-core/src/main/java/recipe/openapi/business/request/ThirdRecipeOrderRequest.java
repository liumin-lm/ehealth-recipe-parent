package recipe.openapi.business.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2020\9\21 0021 16:49
 */
@Data
public class ThirdRecipeOrderRequest implements Serializable{
    private static final long serialVersionUID = -3279105316693696888L;

    private String addressId;

    private String payway;

    private String payMode;

    private String decoctionFlag;

    private String decoctionId;

    private String gfFeeFlag;

    private String depId;

    private Double expressFee;

    private String gysCode;

    private String sendMethod;

    private Double calculateFee;

    private String pharmacyCode;

    private Double registerFee;

    private Double decoctionFee;

    private Double auditFee;

    private Double recipeFee;

    private Double totalFee;

}
