package recipe.serviceprovider.drug.service;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drug.model.DispensatoryDTO;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drug.model.OrganDrugListBean;
import com.ngari.recipe.drug.service.IDrugListService;
import com.ngari.recipe.entity.Dispensatory;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.OrganDrugList;
import ctd.dictionary.DictionaryItem;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import recipe.ApplicationUtils;
import recipe.dao.DispensatoryDAO;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
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

    /**
     * 供运营平台使用,根据药品id查询药品
     * @param drugId 药品id
     * @return 平台药品
     */
    @Override
    public DrugListBean getDrugListBeanByDrugId(Integer drugId) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        DrugList drugList = drugListDAO.getById(drugId);
        DrugList result = new DrugList();
        result.setDrugId(drugList.getDrugId());
        result.setDrugName(drugList.getDrugName());
        result.setSaleName(drugList.getSaleName());
        result.setDrugSpec(drugList.getDrugSpec());
        result.setUnit(drugList.getUnit());
        result.setPack(drugList.getPack());
        result.setUsingRate(drugList.getUsingRate());
        result.setUsePathways(drugList.getUsePathways());
        result.setPrice1(drugList.getPrice1());
        return ObjectCopyUtils.convert(result, DrugListBean.class);
    }

    @Override
    public OrganDrugListBean getOrganDrugListByOrganDrugId(Integer organDrugId) {
        OrganDrugListDAO drugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        OrganDrugList organDrugList = drugListDAO.get(organDrugId);
        return ObjectCopyUtils.convert(organDrugList, OrganDrugListBean.class);
    }
}
