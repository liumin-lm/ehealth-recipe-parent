package recipe.service.recipecancel;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.service.IRecipeEnterpriseService;
import com.ngari.platform.recipe.mode.HospitalReqTo;
import com.ngari.recipe.entity.Recipe;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.service.RecipeServiceSub;

import java.util.Map;

/**
 * created by shiyuping on 2020/4/3
 * 处方撤销服务类
 */
@RpcBean("recipeCancelService")
public class RecipeCancelService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeCancelService.class);
    @Autowired
    private IRecipeEnterpriseService recipeEnterpriseService;

    /**
     * 处方撤销处方new----------(供医生端使用)
     *
     * @param recipeId 处方Id
     * @param message  处方撤销原因
     * @return Map<String                               ,                                                               Object>
     */
    @RpcService
    public Map<String, Object> cancelRecipe(Integer recipeId, String message) {
        return RecipeServiceSub.cancelRecipeImpl(recipeId, 0, "", message);
    }
//
//    /**
//     * 药师撤销处方  ----审核通过变为待审核
//     *
//     * @param recipeId 处方Id
//     * @param message  处方撤销原因
//     * @return Map<String,Object>
//     */
//    @RpcService
//    @Transactional(rollbackFor = Exception.class)
//    public Map<String, Object> cancelRecipeForChecker(Integer recipeId, String message) {
//        LOGGER.info("cancelRecipeForChecker recipeId={} cancelReason={}",recipeId,message);
//        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
//        //获取处方单
//        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
//        Map<String, Object> rMap = Maps.newHashMap();
//        Boolean result = false;
//        String msg = "";
//        if (null == recipe) {
//            rMap.put("result", result);
//            rMap.put("msg", "该处方单不存在");
//            return rMap;
//        }
//
//        //获取撤销前处方单状态
//        Integer beforeStatus = recipe.getStatus();
//        if (!(Integer.valueOf(RecipeStatusConstant.CHECK_PASS).equals(beforeStatus))) {
//            msg = "该处方单不是待处理的处方单,不能进行撤销操作";
//        }
//        if (Integer.valueOf(1).equals(recipe.getPayFlag())) {
//            msg = "该处方单用户已支付，不能进行撤销操作";
//        }
//        if (Integer.valueOf(1).equals(recipe.getChooseFlag())) {
//            msg = "患者已选择购药方式，不能进行撤销操作";
//        }
//
//        //判断第三方处方能否取消,若不能则获取不能取消的原因---只有推送成功的时候才判断第三方
//        if (new Integer(1).equals(recipe.getPushFlag())){
//            HisResponseTO res = canCancelRecipe(recipe);
//            if (!res.isSuccess()){
//                msg = res.getMsg();
//            }
//        }
//
//        if (StringUtils.isNotEmpty(msg)) {
//            rMap.put("result", result);
//            rMap.put("msg", msg);
//            return rMap;
//        }
//
//        //审核通过变为待审核
//        Map<String, Object> updateMap = Maps.newHashMap();
//        if (StringUtils.isNotEmpty(recipe.getChemistSignFile())){
//            LOGGER.info("cancelRecipeForChecker recipe.getChemistSignFile ="+recipe.getChemistSignFile());
//            updateMap.put("chemistSignFile",null);
//        }
//        result = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.READY_CHECK_YS, updateMap);
//        if (result){
//            //向医生端推送药师撤销处方系统消息-------药师撤销处方审核结果
//            RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_REVOKE_YS,recipe);
//            //如果该处方单是某药师抢单后撤销审核结果的处方单则要更新为该药师的抢单单子
//            updateCheckerGrabOrderStatus(recipeId);
//            //记录日志
//            if (StringUtils.isEmpty(message)){
//                message = "无";
//            }
//            RecipeLogService.saveRecipeLog(recipeId,beforeStatus,RecipeStatusConstant.READY_CHECK_YS,"撤销原因："+message);
//        }
//        rMap.put("result",result);
//        rMap.put("msg",msg);
//        LOGGER.info("cancelRecipeForChecker execute ok! rMap:"+JSONUtils.toString(rMap));
//        return rMap;
//    }

//    private void updateCheckerGrabOrderStatus(Integer recipeId) {
//        RecipeCheckDAO recipeCheckDAO = DAOFactory.getDAO(RecipeCheckDAO.class);
//        Map<String, Object> map = Maps.newHashMap();
//        map.put("Checker",null);
//        map.put("CheckerName",null);
//        map.put("CheckStatus",0);
//        map.put("grabOrderStatus",1);
//        map.put("localLimitDate", DateConversion.getDateAftMinute(new Date(), 10));
//        map.put("updateTime",new Date());
//        recipeCheckDAO.updateRecipeExInfoByRecipeId(recipeId,map);
//    }

    public HisResponseTO canCancelRecipe(Recipe recipe) {
        HisResponseTO res = doCancelRecipeForEnterprise(recipe);
        if (res == null) {
            res = new HisResponseTO();
            res.setSuccess();
        } else {
            if (StringUtils.isEmpty(res.getMsg())) {
                res.setMsg("抱歉，该处方单已被处理，无法撤销。");
            }
        }
        return res;
    }

    public HisResponseTO doCancelRecipeForEnterprise(Recipe recipe) {
        HisResponseTO res;
        try {
            HospitalReqTo req = new HospitalReqTo();
            if (recipe != null) {
                req.setOrganId(recipe.getClinicOrgan());
                req.setPrescriptionNo(String.valueOf(recipe.getRecipeId()));
                req.setOrgCode(RecipeServiceSub.getMinkeOrganCodeByOrganId(recipe.getClinicOrgan()));
            }
            LOGGER.info("doCancelRecipeForEnterprise recipeId={} req={}", recipe.getRecipeId(), JSONUtils.toString(req));
            res = recipeEnterpriseService.cancelRecipe(req);
            LOGGER.info("doCancelRecipeForEnterprise recipeId={} res={}", recipe.getRecipeId(), JSONUtils.toString(res));
        } catch (Exception e) {
            LOGGER.error("doCancelRecipeForEnterprise error recipeId={}", recipe.getRecipeId(), e);
            res = new HisResponseTO();
            res.setMsgCode("0");
            res.setMsg("调用撤销接口异常，无法撤销，请稍后重试");
        }
        return res;
    }
}
