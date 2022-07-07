package recipe.vo.second.enterpriseOrder;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
public class EnterpriseDrugVO implements Serializable {
    private static final long serialVersionUID = -666580525250437978L;

    private String appKey;
    private String drugCode;
    private String drugName;
    private String saleName;
    private String drugSpec;
    private BigDecimal price;
    private BigDecimal inventory;
}
