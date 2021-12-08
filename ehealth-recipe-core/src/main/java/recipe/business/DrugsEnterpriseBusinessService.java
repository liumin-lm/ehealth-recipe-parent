package recipe.business;

import com.ngari.recipe.entity.DrugsEnterprise;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.greenroom.IDrugsEnterpriseBusinessService;
import recipe.manager.EnterpriseManager;

import java.util.List;

/**
 * @description： 药企 业务类
 * @author： yinsheng
 * @date： 2021-12-08 18:58
 */
@Service
public class DrugsEnterpriseBusinessService extends BaseService implements IDrugsEnterpriseBusinessService {
    @Autowired
    private EnterpriseManager enterpriseManager;
    @Override
    public Boolean existEnterpriseByName(String name) {
        List<DrugsEnterprise> drugsEnterprises = enterpriseManager.findAllDrugsEnterpriseByName(name);
        if (CollectionUtils.isNotEmpty(drugsEnterprises)) {
            return true;
        }
        return false;
    }
}
