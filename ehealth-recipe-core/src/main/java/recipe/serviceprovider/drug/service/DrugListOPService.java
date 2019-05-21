package recipe.serviceprovider.drug.service;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drug.model.DispensatoryDTO;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drug.service.IDrugListService;
import com.ngari.recipe.entity.Dispensatory;
import com.ngari.recipe.entity.DrugList;
import ctd.dictionary.DictionaryItem;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import recipe.ApplicationUtils;
import recipe.dao.DispensatoryDAO;
import recipe.dao.DrugListDAO;
import recipe.service.DrugListExtService;

import java.util.List;

@RpcBean("drugListOPService")
public class DrugListOPService implements IDrugListService {

    @Override
    public DrugListBean get(final int drugId){
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        DrugList res = drugListDAO.getById(drugId);
        DrugListBean drugListBean = ObjectCopyUtils.convert(res,DrugListBean.class);
        //获取扩展信息
        DispensatoryDAO dispensatoryDAO = DAOFactory.getDAO(DispensatoryDAO.class);
        Dispensatory dispensatory = dispensatoryDAO.getByDrugId(drugListBean.getDrugId());
        if(null != dispensatory) {
            DispensatoryDTO dispensatoryDTO = ObjectCopyUtils.convert(dispensatory, DispensatoryDTO.class);
            drugListBean.setDispensatory(dispensatoryDTO);
        }

        return drugListBean;
    }

    @Override
    public List<DictionaryItem> getDrugClass(String parentKey, int sliceType){
        DrugListExtService drugListExtService = ApplicationUtils.getRecipeService(DrugListExtService.class, "drugList");
        return drugListExtService.getDrugClass(parentKey, sliceType);
    }

    @Override
    public void saveDrugList(DrugListBean drugListBean) {
        DrugList drugList = ObjectCopyUtils.convert(drugListBean, DrugList.class);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        drugListDAO.save(drugList);
    }
}
