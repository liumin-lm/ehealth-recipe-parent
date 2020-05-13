package recipe.service.recipecancel;

import com.google.common.collect.Maps;
import com.ngari.home.asyn.model.BussCancelEvent;
import com.ngari.home.asyn.model.BussCreateEvent;
import com.ngari.home.asyn.service.IAsynDoBussService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.BussTypeConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeMsgEnum;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeCheckDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeServiceSub;
import recipe.util.DateConversion;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * created by shiyuping on 2020/4/3
 * 处方撤销服务类
 */
@RpcBean("recipeCancelService")
public class RecipeCancelService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeCancelService.class);

    /**
     * 处方撤销处方new----------(供医生端使用)
     *
     * @param recipeId 处方Id
     * @param message  处方撤销原因
     * @return Map<String       ,               Object>
     */
    @RpcService
    public Map<String, Object> cancelRecipe(Integer recipeId, String message) {
        return RecipeServiceSub.cancelRecipeImpl(recipeId, 0, "", message);
    }

    /**
     * 药师撤销处方  ----审核通过变为待审核
     *
     * @param recipeId 处方Id
     * @param message  处方撤销原因
     * @return Map<String,Object>
     */
    @RpcService
    public Map<String, Object> cancelRecipeForChecker(Integer recipeId, String message) {
        LOGGER.info("cancelRecipeForChecker recipeId={} cancelReason={}",recipeId,message);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        //获取处方单
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        Map<String, Object> rMap = Maps.newHashMap();
        Boolean result = false;
        String msg = "";
        if (null == recipe) {
            rMap.put("result", result);
            rMap.put("msg", "该处方单不存在");
            return rMap;
        }

        //获取撤销前处方单状态
        Integer beforeStatus = recipe.getStatus();
        if (!(Integer.valueOf(RecipeStatusConstant.CHECK_PASS).equals(beforeStatus))) {
            msg = "该处方单不是待处理的处方单,不能进行撤销操作";
        }
        if (Integer.valueOf(1).equals(recipe.getPayFlag())) {
            msg = "该处方单用户已支付，不能进行撤销操作";
        }
        if (Integer.valueOf(1).equals(recipe.getChooseFlag())) {
            msg = "患者已选择购药方式，不能进行撤销操作";
        }
        if (StringUtils.isNotEmpty(msg)) {
            rMap.put("result", result);
            rMap.put("msg", msg);
            return rMap;
        }
        //审核通过变为待审核
        result = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.READY_CHECK_YS, null);
        if (result){
            //向医生端推送药师撤销处方系统消息-------药师撤销处方审核结果
            RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_REVOKE_YS,recipe);
            //如果该处方单是某药师抢单后撤销审核结果的处方单则要更新为该药师的抢单单子
            updateCheckerGrabOrderStatus(recipeId);
            //记录日志
            if (StringUtils.isEmpty(message)){
                message = "无";
            }
            RecipeLogService.saveRecipeLog(recipeId,beforeStatus,RecipeStatusConstant.READY_CHECK_YS,"撤销原因："+message);
        }
        rMap.put("result",result);
        rMap.put("msg",msg);
        LOGGER.info("cancelRecipeForChecker execute ok! rMap:"+JSONUtils.toString(rMap));
        return rMap;
    }

    private void updateCheckerGrabOrderStatus(Integer recipeId) {
        RecipeCheckDAO recipeCheckDAO = DAOFactory.getDAO(RecipeCheckDAO.class);
        Map<String, Object> map = Maps.newHashMap();
        map.put("Checker",null);
        map.put("CheckerName",null);
        map.put("CheckStatus",0);
        map.put("grabOrderStatus",1);
        map.put("localLimitDate", DateConversion.getDateAftMinute(new Date(), 10));
        map.put("updateTime",new Date());
        recipeCheckDAO.updateRecipeExInfoByRecipeId(recipeId,map);
    }
}
