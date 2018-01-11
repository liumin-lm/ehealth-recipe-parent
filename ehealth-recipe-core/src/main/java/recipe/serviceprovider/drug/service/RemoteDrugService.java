package recipe.serviceprovider.drug.service;

import com.ngari.recipe.common.RecipeBussReqTO;
import com.ngari.recipe.common.RecipeListResTO;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drug.service.IDrugService;
import com.ngari.recipe.entity.DrugList;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.serviceprovider.BaseService;
import recipe.util.MapValueUtil;

import java.util.List;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/8/1.
 */
@RpcBean("remoteDrugService")
public class RemoteDrugService extends BaseService<DrugListBean> implements IDrugService {

    @RpcService
    @Override
    public DrugListBean get(Object id) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        DrugList drugList = drugListDAO.get(id);
        return getBean(drugList, DrugListBean.class);
    }

    @RpcService
    @Override
    public RecipeListResTO<DrugListBean> findDrugsByDepId(int depId) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<DrugList> drugLists = drugListDAO.findDrugsByDepId(depId);
        List<DrugListBean> backList = getList(drugLists, DrugListBean.class);
        return RecipeListResTO.getSuccessResponse(backList);
    }

    @RpcService
    @Override
    public long countDrugsNumByOrganId(RecipeBussReqTO request) {
        Integer organId = MapValueUtil.getInteger(request.getConditions(), "organId");
        if (null == organId) {
            return 0L;
        }

        Long num = 0L;
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        Integer drugType = MapValueUtil.getInteger(request.getConditions(), "drugType");
        if (null == drugType) {
            num = drugListDAO.getEffectiveDrugNum(organId);
        } else {
            num = drugListDAO.getSpecifyNum(organId, drugType);
        }

        return num.longValue();
    }

    @RpcService
    @Override
    public void changeDrugOrganId(int newOrganId, int oldOrganId) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        organDrugListDAO.updateOrganIdByOrganId(newOrganId, oldOrganId);
    }

    @RpcService
    @Override
    public long countAllDrugsNumByOrganId(int organId) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        return organDrugListDAO.getCountByOrganId(organId);
    }

    @RpcService
    @Override
    public List<Integer> queryOrganCanRecipe(List<Integer> organIds, Integer drugId) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        return organDrugListDAO.queryOrganCanRecipe(organIds, drugId);
    }


}
