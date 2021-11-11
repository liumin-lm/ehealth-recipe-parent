package recipe.vo.open;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * @Author liuzj
 * @Date 2021/11/10 11:45
 * @Description
 */
@Getter
@Setter
public class ItemListBean {

    @ItemProperty(alias = "机构id")
    private Integer organID;

    @ItemProperty(alias = "项目名称")
    private String itemName;

    @ItemProperty(alias = "项目编码")
    private String itemCode;

    @ItemProperty(alias = "项目单位")
    private String itemUnit;

    @ItemProperty(alias = "项目费用")
    private BigDecimal itemPrice;

}
