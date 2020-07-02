package recipe.service.recipereportforms.bean;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class EnterpriseRecipeDetailResponse implements Serializable{
    private static final long serialVersionUID = -1050580939377832253L;

    private Integer organId; //机构id

    private String organName; //机构名称

    private String enterpriseName; //药企名称

    private Integer recipeId; //处方单号

    private String patientName; //患者姓名

    private Date payDate; //支付时间

    private BigDecimal totalFee; //总费用

    private BigDecimal drugFee; //药费

    private BigDecimal deliveryFee; //配送费

    private BigDecimal ngariRecivedFee; //平台分润

    private BigDecimal enterpriseReceivableFee; //药企应收

    private String tradeNo; //商户订单号
}
