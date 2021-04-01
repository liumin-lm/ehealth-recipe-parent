package recipe.service.drugs.impl;

import com.ngari.recipe.drugsenterprise.model.DrugEnterpriseLogisticsBean;
import ctd.persistence.DAOFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import recipe.dao.DrugEnterpriseLogisticsDAO;
import recipe.service.drugs.IDrugEnterpriseLogisticsService;

import java.util.List;

/**
 * @description：药企对接物流信息service
 * @author： whf
 * @date： 2021-03-31 11:16
 */
@Service
public class DrugEnterpriseLogisticsServiceImpl implements IDrugEnterpriseLogisticsService {
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDrugEnterpriseLogistics(List<DrugEnterpriseLogisticsBean> drugEnterpriseLogistics) {
        DrugEnterpriseLogisticsDAO drugEnterpriseLogisticsDAO = DAOFactory.getDAO(DrugEnterpriseLogisticsDAO.class);
        Integer drugsEnterpriseId = drugEnterpriseLogistics.get(0).getDrugsEnterpriseId();
        drugEnterpriseLogisticsDAO.deleteByDrugsEnterpriseId(drugsEnterpriseId);
        drugEnterpriseLogisticsDAO.saveAll(drugEnterpriseLogistics);
    }
}
