package recipe.vo.doctor;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author fuzi
 */
@Data
public class DrugEnterpriseStockVO implements Serializable {
    private static final long serialVersionUID = 4653637093000228969L;
    private Integer drugId;
    private Boolean allStock;
    private List<EnterpriseStockVO> enterpriseStockList;
}
