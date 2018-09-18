package recipe.thread;

import com.ngari.recipe.entity.DrugsEnterprise;
import ctd.persistence.DAOFactory;
import org.apache.commons.lang3.StringUtils;
import recipe.ApplicationUtils;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;

import java.util.concurrent.Callable;

/**
 * 更新药企token Callable
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2016/6/15.
 */
public class UpdateDrugsEpCallable implements Callable<String> {

    private Integer _drugsEnterpriseId;

    public UpdateDrugsEpCallable(Integer drugsEnterpriseId) {
        this._drugsEnterpriseId = drugsEnterpriseId;
    }

    @Override
    public String call() throws Exception {

        if (null == this._drugsEnterpriseId) {
            return null;
        }

        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(this._drugsEnterpriseId);
        if (null != drugsEnterprise && StringUtils.isNotEmpty(drugsEnterprise.getAuthenUrl())) {
            RemoteDrugEnterpriseService service = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            service.updateAccessTokenByDep(drugsEnterprise);
        } else {
//            logger.warn("UpdateDrugsEpCallable 更新药企token功能，药企ID:" + this._drugsEnterpriseId + " 药企 AuthenUrl为空");
        }

        return null;
    }

}
