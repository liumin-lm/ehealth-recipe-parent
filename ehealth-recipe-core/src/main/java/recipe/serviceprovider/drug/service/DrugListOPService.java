package recipe.serviceprovider.drug.service;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drug.service.IDrugListService;
import com.ngari.recipe.entity.DrugList;
import ctd.dictionary.DictionaryItem;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.dao.DrugListDAO;

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
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        return drugListDAO.getDrugClass(parentKey, sliceType);
    }
}
