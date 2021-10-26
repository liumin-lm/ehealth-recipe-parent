package recipe.business;

import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.dto.GiveModeButtonDTO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.patient.IDrugEnterpriseBusinessService;
import recipe.manager.ButtonManager;
import recipe.manager.EnterpriseManager;
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
    @Autowired
    private EnterpriseManager enterpriseManager;

    @Override
    public List<EnterpriseStock> enterpriseStockList(ValidateDetailVO validateDetailVO) {
        Integer organId = validateDetailVO.getRecipeBean().getClinicOrgan();
        //获取机构配置按钮
        List<GiveModeButtonDTO> giveModeButtonBeans = buttonManager.getGiveModeMap(organId);
        //获取需要查询库存的药企对象
        List<EnterpriseStock> list = enterpriseManager.enterpriseStockList(organId, giveModeButtonBeans);
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }

        return list;
    }
}
