package recipe.business;

import com.ngari.recipe.dto.EnterpriseStock;
import org.springframework.stereotype.Service;
import recipe.core.api.patient.IDrugEnterpriseBusinessService;
import recipe.vo.doctor.ValidateDetailVO;

import java.util.List;

/**
 * 药企处理实现类
 * @author fuzi
 */
@Service
public class DrugEnterpriseBusinessService extends BaseService implements IDrugEnterpriseBusinessService {
    @Override
    public List<EnterpriseStock> enterpriseStockList(ValidateDetailVO validateDetailVO) {
        return null;
    }
}
