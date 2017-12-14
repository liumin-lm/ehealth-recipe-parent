package recipe.service;

import com.ngari.recipe.entity.Dispensatory;
import recipe.dao.DispensatoryDAO;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;

/**
 * Created by zhongzx on 2017/2/20 0020.
 */
@RpcBean(value = "dispensatoryService", mvc_authentication = false)
public class DispensatoryService {

    @RpcService
    public Dispensatory getByDrugId(Integer drugId) {
        DispensatoryDAO dispensatoryDAO = DAOFactory.getDAO(DispensatoryDAO.class);
        return dispensatoryDAO.getByDrugId(drugId);
    }
}
