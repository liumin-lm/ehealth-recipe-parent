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
    List<EnterpriseStock> enterpriseStockList(ValidateDetailVO validateDetailVO);
}
