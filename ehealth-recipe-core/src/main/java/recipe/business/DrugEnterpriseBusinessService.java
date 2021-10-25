package recipe.business;

import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.dto.GiveModeButtonDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.patient.IDrugEnterpriseBusinessService;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;
import recipe.manager.ButtonManager;
import recipe.vo.doctor.ValidateDetailVO;

import java.util.List;

/**
 * 药企处理实现类
 *
 * @author fuzi
 */
@Service
public class DrugEnterpriseBusinessService extends BaseService implements IDrugEnterpriseBusinessService {
    @Autowired
    private ButtonManager buttonManager;

    @Override
    public List<EnterpriseStock> enterpriseStockList(ValidateDetailVO validateDetailVO) {
        List<GiveModeButtonDTO> giveModeButtonBeans = buttonManager.getGiveModeMap(validateDetailVO.getRecipeBean().getClinicOrgan());
        //无狗眼按钮
        if (!RecipeSupportGiveModeEnum.checkEnterprise(giveModeButtonBeans)) {
            return null;
        }
        return null;
    }
}
