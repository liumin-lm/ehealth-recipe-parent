package recipe.serviceprovider.drug.service;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drug.service.IDrugListService;
import com.ngari.recipe.entity.DrugList;
import ctd.dictionary.DictionaryItem;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import recipe.ApplicationUtils;
import recipe.dao.DrugListDAO;
import recipe.service.DrugListExtService;

import java.util.List;

@RpcBean("drugListOPService")
public class DrugListOPService implements IDrugListService {

    @Override
    public DrugListBean get(final int drugId){
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        DrugList res = drugListDAO.getById(drugId);
        return ObjectCopyUtils.convert(res,DrugListBean.class);
    }

    @Override
    public List<DictionaryItem> getDrugClass(String parentKey, int sliceType){
        DrugListExtService drugListExtService = ApplicationUtils.getRecipeService(DrugListExtService.class, "drugList");
        return drugListExtService.getDrugClass(parentKey, sliceType);
    }
}
