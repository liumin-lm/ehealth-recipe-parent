package recipe.serviceprovider.drugsenterprise.service;

import com.google.common.collect.Lists;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.drugsenterprise.service.IDrugsEnterpriseService;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import org.omg.CORBA.INTERNAL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.manager.EnterpriseManager;
import recipe.serviceprovider.BaseService;

import java.util.ArrayList;
import java.util.List;

/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/9/11.
 */
@RpcBean("remoteDrugsEnterpriseService")
public class RemoteDrugsEnterpriseService extends BaseService<DrugsEnterpriseBean> implements IDrugsEnterpriseService {
    @Autowired
    private EnterpriseManager enterpriseManager;

    @Override
    public DrugsEnterpriseBean get(Object id) {
        DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise enterprise = enterpriseDAO.get(id);
        return getBean(enterprise, DrugsEnterpriseBean.class);
    }

    @Override
    public void pushRecipeInfoForThird(RecipeBean recipe, DrugsEnterpriseBean drugsEnterprise) {
        enterpriseManager.pushRecipeInfoForThird(ObjectCopyUtils.convert(recipe, Recipe.class), ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterprise.class), 0, "");
    }

    @Override
    public DrugsEnterpriseBean getByEnterpriseCode(Integer enterId) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise byEnterpriseCode = drugsEnterpriseDAO.getById(enterId);
        if (ObjectUtils.isEmpty(byEnterpriseCode)){
            return null;
        }
        return ObjectCopyUtils.convert(byEnterpriseCode, DrugsEnterpriseBean.class);
    }

    /**
     * 导出批量查询药企信息
     *
     * @param enterIds
     * @return
     */
    @Override
    public  List<DrugsEnterpriseBean> findByEnterpriseIdList(List<Integer> enterIds) {
        if (ObjectUtils.isEmpty(enterIds)){
            return new ArrayList<>();
        }
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise>  enterpriseList= drugsEnterpriseDAO.findByIdIn(enterIds);
        if (ObjectUtils.isEmpty(enterpriseList)){
            return new ArrayList<>();
        }
        return ObjectCopyUtils.convert(enterpriseList, DrugsEnterpriseBean.class);
    }
}
