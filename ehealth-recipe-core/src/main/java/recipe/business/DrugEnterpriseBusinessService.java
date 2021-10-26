package recipe.business;

import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.exception.DAOException;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.ErrorCode;
import recipe.core.api.patient.IDrugEnterpriseBusinessService;
import recipe.drugsenterprise.AccessDrugEnterpriseService;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.manager.ButtonManager;
import recipe.manager.DrugStockManager;
import recipe.manager.EnterpriseManager;

import javax.annotation.Resource;
import java.util.List;

/**
 * 药企处理实现类
 *
 * @author fuzi
 */
@Service
public class DrugEnterpriseBusinessService extends BaseService implements IDrugEnterpriseBusinessService {
    @Autowired
    private ButtonManager buttonManager;
    @Autowired
    private EnterpriseManager enterpriseManager;
    @Resource
    private DrugStockManager drugStockManager;

    @Override
    public List<EnterpriseStock> enterpriseStockCheck(Recipe recipe, List<Recipedetail> recipeDetails) {
        Integer organId = recipe.getClinicOrgan();
        //获取机构配置按钮
        List<GiveModeButtonDTO> giveModeButtonBeans = buttonManager.getGiveModeMap(organId);
        //获取需要查询库存的药企对象
        List<EnterpriseStock> enterpriseStockList = enterpriseManager.enterpriseStockList(organId, giveModeButtonBeans);
        if (CollectionUtils.isEmpty(enterpriseStockList)) {
            return enterpriseStockList;
        }
        //校验药企库存
        for (EnterpriseStock enterpriseStock : enterpriseStockList) {
            //药企无对应的购药按钮则 无需查询库存-返回无库存
            if (CollectionUtils.isEmpty(enterpriseStock.getGiveModeButton())) {
                enterpriseStock.setStock(false);
                continue;
            }
            DrugsEnterprise drugsEnterprise = enterpriseStock.getDrugsEnterprise();
            //根据药企配置查询 库存
            enterpriseStock(enterpriseStock, recipe, recipeDetails);
        }
        return enterpriseStockList;
    }

    /**
     * 根据药企配置查询 库存
     *
     * @param enterpriseStock 药企购药配置-库存对象
     * @param recipe          处方信息
     * @param recipeDetails   处方明细
     */
    private void enterpriseStock(EnterpriseStock enterpriseStock, Recipe recipe, List<Recipedetail> recipeDetails) {
        DrugsEnterprise drugsEnterprise = enterpriseStock.getDrugsEnterprise();
        Integer checkInventoryFlag = drugsEnterprise.getCheckInventoryFlag();
        if (null == checkInventoryFlag) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, drugsEnterprise.getName() + "checkInventoryFlag is null");
        }
        enterpriseStock.setStock(true);
        if (0 == drugsEnterprise.getCheckInventoryFlag()) {
            return;
        }
        //医院自建药企-查询医院库存
        if (3 == drugsEnterprise.getCheckInventoryFlag()) {
            com.ngari.platform.recipe.mode.RecipeResultBean scanResult = drugStockManager.scanDrugStockByRecipeId(recipe, recipeDetails);
            //无库存
            if (RecipeResultBean.FAIL.equals(scanResult.getCode())) {
                List<String> drugName = ObjectUtils.isEmpty(scanResult.getObject()) ? null : (List<String>) scanResult.getObject();
                enterpriseStock.setDrugName(drugName);
                enterpriseStock.setStock(false);
            }
            return;
        }
        //通过前置机调用
        if (1 == drugsEnterprise.getOperationType()) {
            Integer code = drugStockManager.scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetails);
            enterpriseStock.setStock(RecipeResultBean.SUCCESS.equals(code));
            return;
        }
        //通过平台调用药企
        AccessDrugEnterpriseService drugEnterpriseService = RemoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
        DrugEnterpriseResult result = drugEnterpriseService.enterpriseStock(recipe, drugsEnterprise, recipeDetails);
        enterpriseStock.setStock(RecipeResultBean.SUCCESS.equals(result.getCode()));
    }
}
