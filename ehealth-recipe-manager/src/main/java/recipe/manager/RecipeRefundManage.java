package recipe.manager;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.RecipeRefund;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.IConfigurationClient;
import recipe.constant.RecipeRefundRoleConstant;
import recipe.dao.RecipeOrderDAO;
import recipe.dao.RecipeRefundDAO;

import java.util.Date;
import java.util.List;

@Service
public class RecipeRefundManage extends BaseManager{

    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private RecipeRefundDAO recipeRefundDAO;

    public void recipeReFundSave(String orderCode, RecipeRefund recipeRefund) {
        //处理合并支付问题
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(orderCode);
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIdList);
        recipes.forEach(recipe -> {
            recipeRefund.setBusId(recipe.getRecipeId());
            recipeRefund.setOrganId(recipe.getClinicOrgan());
            recipeRefund.setMpiid(recipe.getMpiid());
            recipeRefund.setPatientName(recipe.getPatientName());
            Boolean doctorReviewRefund = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "doctorReviewRefund", false);
            if (doctorReviewRefund) {
                recipeRefund.setDoctorId(recipe.getDoctor());
            }
            String memo = null;
            try {
                memo = DictionaryController.instance().get("eh.cdr.dictionary.RecipeRefundNode").getText(recipeRefund.getNode()) +
                        DictionaryController.instance().get("eh.cdr.dictionary.RecipeRefundCheckStatus").getText(recipeRefund.getStatus());
            } catch (ControllerException e) {
                logger.error("recipeReFundSave-未获取到处方单信息. recipeId={}, node={}, recipeRefund={}", recipe, JSONUtils.toString(recipeRefund));
                throw new DAOException("退费相关字典获取失败");
            }
            recipeRefund.setMemo(memo);
            if (recipeRefund.getNode() == RecipeRefundRoleConstant.RECIPE_REFUND_ROLE_PATIENT) {
                recipeRefund.setStatus(0);
                recipeRefund.setMemo("患者发起退费申请");
            }
            recipeRefund.setNode(recipeRefund.getNode());
            recipeRefund.setStatus(recipeRefund.getStatus());
            recipeRefund.setApplyTime(new Date());
            recipeRefund.setCheckTime(new Date());
            //保存记录
            recipeRefundDAO.saveRefund(recipeRefund);
        });

    }
}
