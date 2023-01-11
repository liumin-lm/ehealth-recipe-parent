package recipe.vo.greenroom;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 退费结果通知
 */
@Getter
@Setter
public class RefundResultNotifyVO implements Serializable {
    private static final long serialVersionUID = -5374627498030684106L;

    @ItemProperty(alias = "处方id")
    private Integer recipeId;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "his处方号")
    private String recipeCode;

    @ItemProperty(alias = "退费状态 3 退费成功 4 退费失败")
    private Integer refundState;

    @ItemProperty(alias = "退费流水号")
    private String refundNo;

    @ItemProperty(alias = "退费金额")
    private String refundAmount;
}
