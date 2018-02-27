package recipe.service;

import com.ngari.recipe.entity.Dispensatory;
import com.ngari.recipe.entity.DrugList;
import recipe.dao.DispensatoryDAO;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.dao.DrugListDAO;

/**
 * Created by zhongzx on 2017/2/20 0020.
 * @author zhongzx
 */
@RpcBean(value = "dispensatoryService", mvc_authentication = false)
public class DispensatoryService {

    @RpcService
    public Dispensatory getByDrugId(Integer drugId) {
        DispensatoryDAO dispensatoryDAO = DAOFactory.getDAO(DispensatoryDAO.class);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        Dispensatory dispensatory = dispensatoryDAO.getByDrugId(drugId);
        if(null == dispensatory){
            //若无药品说明书 则到drugList中取
            dispensatory = new Dispensatory();
            DrugList drugList = drugListDAO.getById(drugId);
            if(null != drugList){
                dispensatory.setName(drugList.getDrugName());
                dispensatory.setManufacturers(drugList.getProducer());
                dispensatory.setDrugName(drugList.getDrugName());
                dispensatory.setSaleName(drugList.getSaleName());
                dispensatory.setSpecs(drugList.getDrugSpec());
                dispensatory.setIndication(drugList.getIndications());
                dispensatory.setApprovalNumber(drugList.getApprovalNumber());
            }
        }
        return dispensatory;
    }
}
