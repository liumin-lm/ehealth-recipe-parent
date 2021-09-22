package recipe.thread;

import com.ngari.recipe.entity.DrugsEnterprise;
import ctd.persistence.DAOFactory;
import recipe.ApplicationUtils;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;

import java.util.List;

/**
 * 更新药企token Runnable
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/15.
 */
public class UpdateDrugsEpCallable implements Runnable {

    private List<Integer> _drugsEnterpriseIds;

    public UpdateDrugsEpCallable(List<Integer> drugsEnterpriseIds) {
        this._drugsEnterpriseIds = drugsEnterpriseIds;
    }

    @Override
    public void run() {
        RemoteDrugEnterpriseService service = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> drugsEnterprises = drugsEnterpriseDAO.findByIdIn(this._drugsEnterpriseIds);
        for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
            service.updateAccessTokenByDep(drugsEnterprise);
        }
    }

}
