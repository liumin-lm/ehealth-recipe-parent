package recipe.service;

import com.ngari.recipe.drug.model.DispensatoryDTO;
import com.ngari.recipe.entity.Dispensatory;
import com.ngari.recipe.entity.DrugList;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.dao.DispensatoryDAO;
import recipe.dao.DrugListDAO;
import recipe.serviceprovider.BaseService;

/**
 * Created by zhongzx on 2017/2/20 0020.
 *
 * @author zhongzx
 */
@RpcBean(value = "dispensatoryService", mvc_authentication = false)
public class DispensatoryService extends BaseService<DispensatoryDTO> {

    @RpcService
    public DispensatoryDTO getByDrugId(Integer drugId) {
        DispensatoryDAO dispensatoryDAO = DAOFactory.getDAO(DispensatoryDAO.class);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        Dispensatory dispensatory = dispensatoryDAO.getByDrugId(drugId);
        DispensatoryDTO dispensatoryDTO;
        if (null == dispensatory) {
            //若无药品说明书 则到drugList中取
            dispensatoryDTO = new DispensatoryDTO();
            DrugList drugList = drugListDAO.getById(drugId);
            if (null != drugList) {
                dispensatoryDTO.setName(drugList.getDrugName());
                dispensatoryDTO.setManufacturers(drugList.getProducer());
                dispensatoryDTO.setDrugName(drugList.getDrugName());
                dispensatoryDTO.setSaleName(drugList.getSaleName());
                dispensatoryDTO.setSpecs(drugList.getDrugSpec());
                dispensatoryDTO.setIndication(drugList.getIndications());
                dispensatoryDTO.setApprovalNumber(drugList.getApprovalNumber());
            }
        } else {
            dispensatoryDTO = getBean(dispensatory, DispensatoryDTO.class);
        }

        return dispensatoryDTO;
    }
}
