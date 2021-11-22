package recipe.business;

import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.dto.DrugStockAmountDTO;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.exception.DAOException;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.ErrorCode;
import recipe.core.api.patient.IDrugEnterpriseBusinessService;
import recipe.drugsenterprise.AccessDrugEnterpriseService;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.enumerate.type.AppointEnterpriseTypeEnum;
import recipe.manager.ButtonManager;
import recipe.manager.EnterpriseManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    public List<EnterpriseStock> enterpriseStockCheck(Recipe recipe, List<Recipedetail> recipeDetails) {
        Integer organId = recipe.getClinicOrgan();
        //获取机构配置按钮
        List<GiveModeButtonDTO> giveModeButtonBeans = buttonManager.getOrganGiveModeMap(organId);
        //获取需要查询库存的药企对象
        List<EnterpriseStock> enterpriseStockList = buttonManager.enterpriseStockList(organId, giveModeButtonBeans);
        if (CollectionUtils.isEmpty(enterpriseStockList)) {
            return enterpriseStockList;
        }
        //每个药企对应的 不满足的药品列表
        List<Integer> enterpriseIds = enterpriseStockList.stream().map(EnterpriseStock::getDrugsEnterpriseId).collect(Collectors.toList());
        Map<Integer, List<String>> enterpriseDrugNameGroup = enterpriseManager.checkEnterpriseDrugName(enterpriseIds, recipeDetails);
        //校验药企库存
        for (EnterpriseStock enterpriseStock : enterpriseStockList) {
            enterpriseStock.setStock(false);
            //药企无对应的购药按钮则 无需查询库存-返回无库存
            if (CollectionUtils.isEmpty(enterpriseStock.getGiveModeButton())) {
                continue;
            }
            //验证能否药品配送以及能否开具到一张处方单上
            List<String> drugNames = enterpriseDrugNameGroup.get(enterpriseStock.getDrugsEnterpriseId());
            if (CollectionUtils.isNotEmpty(drugNames)) {
                enterpriseStock.setDrugName(drugNames);
                continue;
            }
            //根据药企配置查询 库存
            enterpriseStock(enterpriseStock, recipe, recipeDetails);
        }
        return enterpriseStockList;
    }


    @Override
    public List<EnterpriseStock> enterpriseStockCheck(Integer organId, List<Recipedetail> recipeDetails) {
        //获取机构配置按钮
        List<GiveModeButtonDTO> giveModeButtonBeans = buttonManager.getOrganGiveModeMap(organId);
        //获取需要查询库存的药企对象
        List<EnterpriseStock> enterpriseStockList = buttonManager.enterpriseStockList(organId, giveModeButtonBeans);
        if (CollectionUtils.isEmpty(enterpriseStockList)) {
            return enterpriseStockList;
        }
        //校验药企库存
        for (EnterpriseStock enterpriseStock : enterpriseStockList) {
            enterpriseStock.setStock(false);
            enterpriseStock.setCheckDrugStock(true);
            //药企无对应的购药按钮则 无需查询库存-返回无库存
            if (CollectionUtils.isEmpty(enterpriseStock.getGiveModeButton())) {
                continue;
            }
            //根据药企配置查询 库存
            Recipe recipe = new Recipe();
            recipe.setClinicOrgan(organId);
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
        if (0 == drugsEnterprise.getCheckInventoryFlag()) {
            enterpriseStock.setStock(true);
            enterpriseStock.setCheckDrugStock(false);
            return;
        }
        //医院自建药企-查询医院库存
        if (3 == drugsEnterprise.getCheckInventoryFlag()) {
            enterpriseStock.setCheckStockFlag(AppointEnterpriseTypeEnum.ORGAN_APPOINT.getType());
            return;
        }
        //通过前置机调用
        if (1 == drugsEnterprise.getOperationType()) {
            DrugStockAmountDTO code = enterpriseManager.scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetails);
            enterpriseStock.setStock(code.isResult());
            enterpriseStock.setDrugInfoList(code.getDrugInfoList());
            return;
        }
        //通过平台调用药企
        AccessDrugEnterpriseService drugEnterpriseService = RemoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
        DrugEnterpriseResult result = drugEnterpriseService.enterpriseStock(recipe, drugsEnterprise, recipeDetails);
        enterpriseStock.setStock(RecipeResultBean.SUCCESS.equals(result.getCode()));
    }
}
