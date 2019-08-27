package recipe.purchase;

import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.persistence.DAOFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import recipe.ApplicationUtils;
import recipe.constant.OrderStatusConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;

import java.util.List;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/20
 * @description： 到院取药方式实现
 * @version： 1.0
 */
public class PayModeToHos implements IPurchaseService{

    @Override
    public RecipeResultBean findSupportDepList(Recipe dbRecipe, Map<String, String> extInfo) {
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        Integer recipeId = dbRecipe.getRecipeId();
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());
        StringBuilder sb = new StringBuilder();

        if(CollectionUtils.isNotEmpty(detailList)){
            String pharmNo = detailList.get(0).getPharmNo();
            if(StringUtils.isNotEmpty(pharmNo)){
                sb.append("到院自取需去医院取药窗口取药："+ organDTO.getName() + pharmNo + "取药窗口");
            }else {
                sb.append("选择到院自取后，需去医院取药窗口取药");
            }
        }

        resultBean.setMsg(sb.toString());
        return resultBean;
    }

    @Override
    public OrderCreateResult order(Recipe dbRecipe, Map<String, String> extInfo) {
       return null;
    }

    @Override
    public Integer getPayMode() {
        return RecipeBussConstant.PAYMODE_TO_HOS;
    }

    @Override
    public String getServiceName() {
        return "payModeToHosService";
    }

    @Override
    public String getTipsByStatusForPatient(Recipe recipe, RecipeOrder order) {
        Integer status = recipe.getStatus();
        Integer payFlag = recipe.getPayFlag();
        String orderCode = recipe.getOrderCode();
        String tips = "";
        switch (status) {
            case RecipeStatusConstant.CHECK_PASS:
                if (StringUtils.isEmpty(orderCode) && payFlag == 1) {
                    tips = "订单已处理，请到院取药";
                }
                break;
            case RecipeStatusConstant.CHECK_PASS_YS:
                tips = "处方已审核通过，请到院取药";
                break;
            case RecipeStatusConstant.RECIPE_FAIL:
                tips = "到院取药失败";
                break;
            case RecipeStatusConstant.FINISH:
                tips = "到院取药成功，订单完成";
                break;
        }
        return tips;
    }
}
