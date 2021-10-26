package recipe.core.api.patient;

import com.ngari.recipe.dto.EnterpriseStock;
import recipe.vo.doctor.ValidateDetailVO;

import java.util.List;

/**
 * 药企处理实现类
 *
 * @author fuzi
 */
public interface IDrugEnterpriseBusinessService {
    /**
     * 获取药企库存列表
     *
     * @param validateDetailVO
     * @return
     */
    List<EnterpriseStock> enterpriseStockList(ValidateDetailVO validateDetailVO);
}
