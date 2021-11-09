package recipe.vo.second;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @description： 端用订单信息
 * @author： whf
 * @date： 2021-11-08 15:50
 */
@Getter
@Setter
@ToString
public class RecipeOrderVO implements Serializable {
    @ItemProperty(alias = "订单ID")
    private Integer orderId;

    @ItemProperty(alias = "订单编号")
    private String orderCode;

    @ItemProperty(alias = "患者编号")
    private String mpiId;

    @ItemProperty(alias = "订单状态")
    private Integer status;

    @ItemProperty(alias = "订单状态")
    private String statusText;

    @ItemProperty(alias = "订单总费用")
    private BigDecimal totalFee;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "订单下处方信息")
    private List<RecipeVo> recipeVos;
}
