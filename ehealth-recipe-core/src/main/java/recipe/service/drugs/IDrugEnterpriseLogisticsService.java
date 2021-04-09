package recipe.service.drugs;

import com.ngari.recipe.drugsenterprise.model.DrugEnterpriseLogisticsBean;

import java.util.List;

/**
 * @description：药企对接物流信息service
 * @author： whf
 * @date： 2021-03-31 11:16
 */
public interface IDrugEnterpriseLogisticsService {

    /**
     * 保存药企对接物流信息
     *
     * @param drugEnterpriseLogistics
     */
    void saveDrugEnterpriseLogistics(List<DrugEnterpriseLogisticsBean> drugEnterpriseLogistics,Integer drugsEnterpriseId);
}
