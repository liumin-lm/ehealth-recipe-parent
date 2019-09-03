package recipe.audit.auditmode;

import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import eh.cdr.constant.RecipeStatusConstant;
import eh.wxpay.constant.PayConstant;
import recipe.ApplicationUtils;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeSystemConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeServiceSub;
import recipe.util.MapValueUtil;

import java.util.Map;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * created by shiyuping on 2019/9/3
 */
public class AbstractAuidtMode implements IAuditMode{
    @Override
    public void afterHisCallBackChange(Integer status, Recipe recipe) {
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        //发送卡片
        RecipeServiceSub.sendRecipeTagToPatient(recipe, detailDAO.findByRecipeId(recipe.getRecipeId()), null, true);
    }

    @Override
    public void afterAuditRecipeChange(Integer status) {}

    @Override
    public void afterCheckPassYs(Recipe recipe) {}

    @Override
    public void afterCheckNotPassYs(Recipe recipe) {}

    @Override
    public void afterPayChange(Boolean saveFlag, Recipe recipe, RecipeResultBean result, Map<String, Object> attrMap) {
        //默认待处理
        Integer status = RecipeStatusConstant.CHECK_PASS;
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        String giveMode = MapValueUtil.getString(attrMap,"giveMode");
        Integer payMode = MapValueUtil.getInteger(attrMap, "payMode");
        Integer payFlag = MapValueUtil.getInteger(attrMap, "payFlag");
        //根据传入的方式来处理, 因为供应商列表，钥世圈提供的有可能是多种方式都支持，当时这2个值是保存为null的
        if (saveFlag) {
            attrMap.put("chooseFlag", 1);
            String memo = "";
            if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode)) {
                if (RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
                    //线上支付
                    if (PayConstant.PAY_FLAG_PAY_SUCCESS == payFlag) {
                        //配送到家-线上支付
                        memo = "配送到家-线上支付成功";
                    } else {
                        memo = "配送到家-线上支付失败";
                    }
                } else if (RecipeBussConstant.PAYMODE_MEDICAL_INSURANCE.equals(payMode)) {
                    if (recipe.canMedicalPay()) {
                        memo = "医保支付成功，发送药企处方";
                    }
                } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode)) {
                    //收到userConfirm通知
                    memo = "配送到家-货到付款成功";
                }
            } else if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode)) {
                //收到userConfirm通知
                memo = "药店取药-到店取药成功";
            }
            //记录日志
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), RecipeStatusConstant.CHECK_PASS, status, memo);
        } else {
            attrMap.put("chooseFlag", 0);
            if (RecipeBussConstant.FROMFLAG_HIS_USE.equals(recipe.getFromflag())) {
                status = recipe.getStatus();
            }
        }

        try {
            boolean flag = recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), status, attrMap);
            if (flag) {
                result.setMsg(RecipeSystemConstant.SUCCESS);
            } else {
                result.setCode(RecipeResultBean.FAIL);
                result.setError("更新处方失败");
            }
        } catch (Exception e) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("更新处方失败，" + e.getMessage());
        }
        //处方推送到药企
        RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        remoteDrugEnterpriseService.pushSingleRecipeInfo(recipe.getRecipeId());
    }
}
