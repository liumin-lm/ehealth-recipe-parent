package recipe.drugsenterprise;

import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.dto.DrugInfoDTO;
import com.ngari.recipe.dto.DrugStockAmountDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.SaleDrugListDAO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 上海益友药企
 * @author yinsheng
 * @date 2020\3\4 0004 11:24
 */
public class ShyyRemoteService  extends AccessDrugEnterpriseService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ShyyRemoteService.class);

    @Autowired
    private SaleDrugListDAO saleDrugListDAO;

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {

    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("ShyyRemoteService.pushRecipeInfo recipeIds:{}.", JSONUtils.toString(recipeIds));
        //上海益友此处进行减库存的操作
        updateEnterpriseInventory(recipeIds.get(0), enterprise);
        return DrugEnterpriseResult.getSuccess();
    }

    private void updateEnterpriseInventory(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        for (Recipedetail recipedetail : recipedetails) {
            Integer drugId = recipedetail.getDrugId();
            Double useTotalDose = recipedetail.getUseTotalDose();
            BigDecimal totalDose = new BigDecimal(useTotalDose);
            LOGGER.info("ShyyRemoteService.updateEnterpriseInventory 更新库存成功,更新药品:{},更新数量:{},处方单号：{}.", drugId, totalDose, recipeId);
            saleDrugListDAO.updateInventoryByOrganIdAndDrugId(drugsEnterprise.getId(), drugId, totalDose);
        }
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugStockAmountDTO scanEnterpriseDrugStock(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails) {
        DrugStockAmountDTO drugStockAmountDTO = new DrugStockAmountDTO();
        List<DrugInfoDTO> drugInfoList = new ArrayList<>();
        List<Integer> drugList = recipeDetails.stream().map(Recipedetail::getDrugId).distinct().collect(Collectors.toList());
        List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIds(drugsEnterprise.getId(), drugList);
        Map<Integer,SaleDrugList> saleDrugListMap = saleDrugLists.stream().collect(Collectors.toMap(SaleDrugList::getDrugId,a->a,(k1,k2)->k1));

        recipeDetails.forEach(recipeDetail -> {
            DrugInfoDTO drugInfoDTO = new DrugInfoDTO();
            BeanUtils.copyProperties(recipeDetail, drugInfoDTO);
            SaleDrugList saleDrugList = saleDrugListMap.get(recipeDetail.getDrugId());
            if (null != saleDrugList && saleDrugList.getStatus() == 1
                    && null != saleDrugList.getInventory()) {
                drugInfoDTO.setStock(saleDrugList.getInventory().doubleValue()>0d);
                drugInfoDTO.setStockAmountChin(drugInfoDTO.getStock()?saleDrugList.getInventory().toString():"0");
            } else {
                drugInfoDTO.setStock(false);
                drugInfoDTO.setStockAmountChin("0");
            }
            drugInfoList.add(drugInfoDTO);
        });
        LOGGER.info("scanEnterpriseDrugStock drugInfoList:{}.", JSONUtils.toString(drugInfoList));
        List<String> noDrugNames = drugInfoList.stream().filter(drugInfoDTO -> !drugInfoDTO.getStock()).map(DrugInfoDTO::getDrugName).collect(Collectors.toList());
        drugStockAmountDTO.setResult(true);
        if (CollectionUtils.isNotEmpty(noDrugNames)) {
            drugStockAmountDTO.setNotDrugNames(noDrugNames);
            drugStockAmountDTO.setResult(false);
        }
        drugStockAmountDTO.setDrugInfoList(drugInfoList);
        return drugStockAmountDTO;
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugId, drugsEnterprise.getId());
        if (saleDrugList.getInventory() == null){
            return "0";
        }
        return saleDrugList.getInventory()+"";
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        List<String> result = new ArrayList<>();
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        drugsDataBean.getRecipeDetailBeans().forEach(recipeDetailBean->{
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganIdAndStatus(recipeDetailBean.getDrugId(), drugsEnterprise.getId());
            List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(recipeDetailBean.getDrugId(), drugsDataBean.getOrganId());
            if (saleDrugList != null && saleDrugList.getInventory() != null && CollectionUtils.isNotEmpty(organDrugLists)) {
                result.add(organDrugLists.get(0).getDrugName());
            }
        });
        return result;
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
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_SHYY;
    }
}
