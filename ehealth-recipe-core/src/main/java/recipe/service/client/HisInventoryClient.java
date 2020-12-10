package recipe.service.client;

import com.alibaba.fastjson.JSON;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.platform.recipe.mode.RecipeDrugInventoryDTO;
import com.ngari.platform.recipe.mode.RecipeDrugInventoryInfoDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrderBill;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.exception.DAOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import recipe.constant.ErrorCode;
import recipe.dao.RecipeOrderBillDAO;

import java.util.LinkedList;
import java.util.List;

/**
 * his库存 交互处理类
 *
 * @author fuzi
 */
@Service
public class HisInventoryClient extends BaseClient {
    @Autowired
    private RecipeOrderBillDAO recipeOrderBillDAO;

    /**
     * 组织加减库存接口参数
     *
     * @param recipeId
     * @return
     */
    public RecipeDrugInventoryDTO recipeDrugInventory(Integer recipeId) {
        RecipeDrugInventoryDTO request = new RecipeDrugInventoryDTO();
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        request.setOrganId(recipe.getClinicOrgan());
        request.setRecipeId(recipe.getRecipeId());
        request.setRecipeType(recipe.getRecipeType());
        RecipeOrderBill recipeOrderBill = recipeOrderBillDAO.getRecipeOrderBillByOrderCode(recipe.getOrderCode());
        if (null != recipeOrderBill) {
            request.setInvoiceNumber(recipeOrderBill.getBillNumber());
        }
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isEmpty(recipeDetailList)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品列表为空");
        }
        List<RecipeDrugInventoryInfoDTO> infoList = new LinkedList<>();
        recipeDetailList.forEach(a -> {
            RecipeDrugInventoryInfoDTO info = new RecipeDrugInventoryInfoDTO();
            info.setCreateDt(a.getCreateDt());
            info.setDrugCost(a.getDrugCost());
            info.setDrugId(a.getDrugId());
            info.setOrganDrugCode(a.getOrganDrugCode());
            info.setUseTotalDose(a.getUseTotalDose());
            info.setPharmacyId(a.getPharmacyId());
            info.setProducerCode(a.getProducerCode());
            info.setDrugBatch(a.getDrugBatch());
            info.setSalePrice(a.getSalePrice());
            infoList.add(info);
        });
        request.setInfo(infoList);
        logger.info("HisInventoryClient RecipeDrugInventoryDTO request= {}", JSON.toJSONString(request));
        return request;
    }

    /**
     * 增减库存
     *
     * @param request
     */
    public void drugInventory(RecipeDrugInventoryDTO request) {
        logger.info("HisInventoryClient drugInventory request= {}", JSON.toJSONString(request));
        try {
            HisResponseTO<Boolean> hisResponse = recipeHisService.drugInventory(request);
            Boolean result = getResponse(hisResponse);
            if (!result) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "his库存操作失败");
            }
        } catch (Exception e) {
            logger.error("HisInventoryClient drugInventory hisResponse", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

}
