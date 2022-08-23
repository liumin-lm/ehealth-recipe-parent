package recipe.vo.greenroom;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class ImperfectInfoVO implements Serializable {
    private static final long serialVersionUID = 511061670752306612L;

    @ItemProperty(alias = "纳里平台机构Id号")
    private Integer organId;

    @ItemProperty(alias = "his处方号")
    private String recipeCode;

    @ItemProperty(alias = "完善标识 0 未完善 1完善")
    private Integer imperfectFlag;
}
