package recipe.serviceprovider.drug.service;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drug.model.DispensatoryDTO;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drug.model.OrganDrugListBean;
import com.ngari.recipe.drug.service.IDrugListService;
import com.ngari.recipe.entity.Dispensatory;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.DrugSources;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.dictionary.DictionaryItem;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import recipe.ApplicationUtils;
import recipe.dao.DispensatoryDAO;
import recipe.dao.DrugListDAO;
import recipe.dao.DrugSourcesDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.service.DrugListExtService;

import java.util.List;
import java.util.stream.Collectors;

@RpcBean("drugListOPService")
public class DrugListOPService implements IDrugListService {

    @Override
    public DrugListBean get(final int drugId) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        DrugList res = drugListDAO.getById(drugId);
        DrugListBean drugListBean = ObjectCopyUtils.convert(res, DrugListBean.class);
        //获取扩展信息
        DispensatoryDAO dispensatoryDAO = DAOFactory.getDAO(DispensatoryDAO.class);
        Dispensatory dispensatory = dispensatoryDAO.getByDrugId(drugListBean.getDrugId());
        if (null != dispensatory) {
            DispensatoryDTO dispensatoryDTO = ObjectCopyUtils.convert(dispensatory, DispensatoryDTO.class);
            drugListBean.setDispensatory(dispensatoryDTO);
        }
        DrugSourcesDAO dao = DAOFactory.getDAO(DrugSourcesDAO.class);
        if (drugListBean.getSourceOrgan() != null){
            List<DrugSources> byDrugSourcesId = dao.findByDrugSourcesId(drugListBean.getSourceOrgan());
            if (byDrugSourcesId != null && byDrugSourcesId.size() > 0 ){
                drugListBean.setSourceOrganText(byDrugSourcesId.get(0).getDrugSourcesName());
            }else {
                drugListBean.setSourceOrgan(0);
            }
        }else {
            drugListBean.setSourceOrgan(0);
        }

        return drugListBean;
    }

    @Override
    public List<DictionaryItem> getDrugClass(String parentKey, int sliceType) {
        DrugListExtService drugListExtService = ApplicationUtils.getRecipeService(DrugListExtService.class, "drugList");
        return drugListExtService.getDrugClass(parentKey, sliceType);
    }

    /**
     * 供运营平台使用,根据药品id查询药品
     *
     * @param drugId 药品id
     * @return 平台药品
     */
    @Override
    public DrugListBean getDrugListBeanByDrugId(Integer drugId) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        DrugList drugList = drugListDAO.getById(drugId);
        if(drugList==null){
            return null;
        }
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

    @Override
    public void saveDrugList(DrugListBean drugListBean) {
        DrugList drugList = ObjectCopyUtils.convert(drugListBean, DrugList.class);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        drugListDAO.save(drugList);
    }

    @Override
    public List<DrugListBean> findByDrugIds(List<Integer> drugIds) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<DrugList> drugLists = drugListDAO.findByDrugIds(drugIds);
        List<DrugListBean> drugListBeans = null;
        if (CollectionUtils.isNotEmpty(drugLists)) {
            drugListBeans = drugLists.stream().map(drugList -> {
                DrugListBean drugListBean = new DrugListBean();
                BeanUtils.copyProperties(drugList, drugListBean);
                return drugListBean;
            }).collect(Collectors.toList());
        }
        return drugListBeans;
    }
}
