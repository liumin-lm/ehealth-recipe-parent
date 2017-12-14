package recipe.serviceprovider.drugsenterprise.service;

import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.drugsenterprise.service.IDrugsEnterpriseService;
import com.ngari.recipe.entity.DrugsEnterprise;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.serviceprovider.BaseService;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/11.
 */
@RpcBean("remoteDrugsEnterpriseService")
public class RemoteDrugsEnterpriseService extends BaseService<DrugsEnterpriseBean> implements IDrugsEnterpriseService {

    @Override
    public DrugsEnterpriseBean get(Object id) {
        DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise enterprise = enterpriseDAO.get(id);
        return getBean(enterprise, DrugsEnterpriseBean.class);
    }


}
