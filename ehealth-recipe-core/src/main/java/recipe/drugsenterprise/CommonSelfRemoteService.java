package recipe.drugsenterprise;

import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.Position;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Pharmacy;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.SaleDrugList;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.PharmacyDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.SaleDrugListDAO;
import recipe.util.DistanceUtil;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 公共自建药企
 * @author yinsheng
 * @date 2019\11\7 0007 14:20
 */
public class CommonSelfRemoteService extends AccessDrugEnterpriseService{

    private static final String searchMapRANGE = "range";

    private static final String searchMapLatitude = "latitude";

    private static final String searchMapLongitude = "longitude";

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonSelfRemoteService.class);

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("PublicSelfRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("PublicSelfRemoteService pushRecipeInfo not implement.");
        //date 2019/12/4
        //添加自建药企推送处方时推送消息给药企
        pushMessageToEnterprise(recipeIds);
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        LOGGER.info("PublicSelfRemoteService scanStock not implement.");
        return DrugEnterpriseResult.getSuccess();
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
        String longitude = MapValueUtil.getString(ext, searchMapLongitude);
        String latitude = MapValueUtil.getString(ext, searchMapLatitude);
        if (recipeIds == null) {
            return DrugEnterpriseResult.getFail();
        }
        Integer recipeId = recipeIds.get(0);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (recipe == null) {
            return DrugEnterpriseResult.getFail();
        }
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Integer> drugs = recipeDetailDAO.findDrugIdByRecipeId(recipeId);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIds(enterprise.getId(), drugs);
        if (CollectionUtils.isEmpty(saleDrugLists) || saleDrugLists.size() < drugs.size()) {
            return DrugEnterpriseResult.getFail();
        }
        if (longitude != null && latitude != null) {
            //药店取药
            List<Pharmacy> pharmacyList = getPharmacies(recipeIds, ext, enterprise, result);
            List<DepDetailBean> detailList = new ArrayList<>();
            DepDetailBean detailBean;
            for (Pharmacy pharmacy : pharmacyList) {
                detailBean = new DepDetailBean();
                detailBean.setDepId(enterprise.getId());
                detailBean.setDepName(pharmacy.getPharmacyName());
                detailBean.setRecipeFee(BigDecimal.ZERO);
                detailBean.setExpressFee(BigDecimal.ZERO);
                detailBean.setPharmacyCode(pharmacy.getPharmacyCode());
                detailBean.setAddress(pharmacy.getPharmacyAddress());
                Position position = new Position();
                position.setLatitude(Double.parseDouble(pharmacy.getPharmacyLatitude()));
                position.setLongitude(Double.parseDouble(pharmacy.getPharmacyLongitude()));
                position.setRange(Integer.parseInt(ext.get(searchMapRANGE).toString()));
                detailBean.setPosition(position);
                detailBean.setBelongDepName(enterprise.getName());
                //记录药店和用户两个经纬度的距离
                detailBean.setDistance(DistanceUtil.getDistance(Double.parseDouble(ext.get(searchMapLatitude).toString()),
                        Double.parseDouble(ext.get(searchMapLongitude).toString()), Double.parseDouble(pharmacy.getPharmacyLatitude()), Double.parseDouble(pharmacy.getPharmacyLongitude())));
                detailList.add(detailBean);
            }
            result.setObject(detailList);
        }
        return result;
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_COMMON_SELF;
    }

    private List<Pharmacy> getPharmacies(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise, DrugEnterpriseResult result) {
        PharmacyDAO pharmacyDAO = DAOFactory.getDAO(PharmacyDAO.class);
        List<Pharmacy> pharmacyList = new ArrayList<Pharmacy>();
        if (ext != null && null != ext.get(searchMapRANGE) && null != ext.get(searchMapLongitude) && null != ext.get(searchMapLatitude)) {
            pharmacyList = pharmacyDAO.findByDrugsenterpriseIdAndStatusAndRangeAndLongitudeAndLatitude(enterprise.getId(), Double.parseDouble(ext.get(searchMapRANGE).toString()), Double.parseDouble(ext.get(searchMapLongitude).toString()), Double.parseDouble(ext.get(searchMapLatitude).toString()));
        }else{
            LOGGER.warn("CommonSelfRemoteService.findSupportDep:请求的搜索参数不健全" );
            getFailResult(result, "请求的搜索参数不健全");
        }
        if(CollectionUtils.isEmpty(recipeIds)){
            LOGGER.warn("CommonSelfRemoteService.findSupportDep:查询的处方单为空" );
            getFailResult(result, "查询的处方单为空");
        }
        return pharmacyList;
    }

    private void getFailResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return "有库存";
    }
}
