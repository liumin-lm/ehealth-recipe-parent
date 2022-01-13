package recipe.drugsenterprise;

import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.drugsenterprise.model.Position;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Pharmacy;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import ctd.persistence.DAOFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.PharmacyDAO;
import recipe.service.RecipeLogService;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author yinsheng
 * @date 2019\10\16 0016 10:10
 */
public class TestDrugStoreRemoteService extends AccessDrugEnterpriseService {
    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDrugStoreRemoteService.class);
    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("TestDrugStoreRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("TestDrugStoreRemoteService pushRecipeInfo not implement.");
        RecipeLogService.saveRecipeLog(recipeIds.get(0), 2, 2, "处方推送药企成功");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return "有库存";
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        return null;
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        PharmacyDAO pharmacyDAO = DAOFactory.getDAO(PharmacyDAO.class);
        String startStr = MapValueUtil.getString(ext, "start");
        String limitStr = MapValueUtil.getString(ext, "limit");
        Integer start = StringUtils.isNotEmpty(startStr)?Integer.parseInt(startStr):0;
        Integer limit = StringUtils.isNotEmpty(limitStr)?Integer.parseInt(limitStr):10;
        List<Pharmacy> pharmacies = pharmacyDAO.findEnterpriseByDepId(enterprise.getId() ,start, limit);
        List<DepDetailBean> depDetailBeans = new ArrayList<>();
        for (Pharmacy pharmacy : pharmacies) {
            DepDetailBean depDetailBean = new DepDetailBean();
            depDetailBean.setDepId(pharmacy.getDrugsenterpriseId());
            depDetailBean.setDepName(pharmacy.getPharmacyName());
            Position position = new Position();
            position.setLongitude(Double.parseDouble(pharmacy.getPharmacyLongitude()));
            position.setLatitude(Double.parseDouble(pharmacy.getPharmacyLatitude()));
            depDetailBean.setPosition(position);
            depDetailBean.setRecipeFee(new BigDecimal(20));
            depDetailBean.setPayMode(4);
            depDetailBean.setExpressFee(BigDecimal.ZERO);
            depDetailBean.setGysCode(pharmacy.getPharmacyCode());
            depDetailBean.setSendMethod("0");
            depDetailBean.setPayMode(20);
            depDetailBean.setAddress(pharmacy.getPharmacyAddress());
            depDetailBean.setDistance(2.0);
            depDetailBean.setPharmacyCode(pharmacy.getPharmacyCode());
            depDetailBeans.add(depDetailBean);
        }
        result.setObject(depDetailBeans);
        return result;
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_TEST_DRUGSTORE;
    }
}
