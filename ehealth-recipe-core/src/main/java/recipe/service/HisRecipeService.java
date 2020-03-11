package recipe.service;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.HisRecipe;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.HisRecipeVO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.*;

import java.util.*;

/**
 * @author yinsheng
 * @date 2020\3\10 0010 19:58
 */
@RpcBean("hisRecipeService")
public class HisRecipeService {
    private static final Log LOGGER = LogFactory.getLog(HisRecipeService.class);

    @Autowired
    private HisRecipeDAO hisRecipeDAO;
    @Autowired
    private HisRecipeExtDAO hisRecipeExtDAO;
    @Autowired
    private HisRecipeDetailDAO hisRecipeDetailDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    /**
     * organId 机构编码
     * mpiId 用户mpiId
     * timeQuantum 时间段  1 代表一个月  3 代表三个月 6 代表6个月
     * status 1 未处理 2 已处理
     * @param request
     * @return
     */
    @RpcService
    public Map<String, Object>  findHisRecipe(Map<String, Object> request){
       Integer organId = (Integer) request.get("organId");
       String mpiId = (String) request.get("mpiId");
       Integer timeQuantum = (Integer) request.get("timeQuantum");
       Integer status = (Integer) request.get("status");
       Integer start = (Integer) request.get("start");
       Integer limit = (Integer) request.get("limit");
       List<HisRecipe> hisRecipes = hisRecipeDAO.findHisRecipes(organId, mpiId, status);
       List<HisRecipe> result = new ArrayList<>();
       //根据status状态查询处方列表
        if (status == 1) {
            //表示想要查询未处理的处方
            // 1 该处方在平台上不存在,只存在HIS中
            for (HisRecipe hisRecipe : hisRecipes) {
                Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(hisRecipe.getRecipeCode(), organId);
                if (recipe == null) {
                    hisRecipe.setOrderStatusText("待支付");
                    hisRecipe.setFromFlag(0);
                    result.add(hisRecipe);
                } else {
                    //表示该处方已经在平台上存在了
                    if (StringUtils.isEmpty(recipe.getOrderCode())) {
                        hisRecipe.setOrderStatusText("待支付");
                        hisRecipe.setFromFlag(1);
                        result.add(hisRecipe);
                    }
                }
            }
        } else {
            //表示查询已处理的处方
            for (HisRecipe hisRecipe : hisRecipes) {
                Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(hisRecipe.getRecipeCode(), organId);
                if (recipe == null) {
                    //表示该处方单患者在his线下已完成
                    hisRecipe.setOrderStatusText("已完成");
                    hisRecipe.setFromFlag(0);
                    result.add(hisRecipe);
                } else {
                    if (StringUtils.isEmpty(recipe.getOrderCode())) {
                        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                        hisRecipe.setOrderStatusText(getTipsByStatusForPatient(recipe, recipeOrder));
                        result.add(hisRecipe);
                    }
                }
            }
        }

       Map<String, Object> map = new HashMap<>();
       map.put("hisRecipe", hisRecipes);
       return map;
    }


    /**
     * 状态文字提示（患者端）
     *
     * @param recipe
     * @return
     */
    public static String getTipsByStatusForPatient(Recipe recipe, RecipeOrder order) {
        Integer status = recipe.getStatus();
        Integer payMode = recipe.getPayMode();
        Integer payFlag = recipe.getPayFlag();
        Integer giveMode = recipe.getGiveMode();
        String orderCode = recipe.getOrderCode();
        String tips = "";
        switch (status) {
            case RecipeStatusConstant.FINISH:
                tips = "已完成.";
                break;
            case RecipeStatusConstant.HAVE_PAY:
                if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode)) {
                    //配送到家
                    tips = "待配送";
                } else if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(giveMode)) {
                    //医院取药
                    tips = "待取药";
                }
                break;
            case RecipeStatusConstant.NO_OPERATOR:
            case RecipeStatusConstant.NO_PAY:
                tips = "已失效";
                break;
            case RecipeStatusConstant.NO_DRUG:
                tips = "已失效.";
                break;
            case RecipeStatusConstant.CHECK_PASS:
                if (null == payMode || null == giveMode) {
                    tips = "";
                } else if (RecipeBussConstant.PAYMODE_TO_HOS.equals(payMode) && 0 == payFlag) {
                    tips = "待取药.";
                }

                if (StringUtils.isNotEmpty(orderCode) && null != order && 1 == order.getEffective()) {
                    tips = "待取药.";
                }

                break;
            case RecipeStatusConstant.READY_CHECK_YS:
                if (RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
                    //在线支付
                    tips = "待配送.";
                } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode) || RecipeBussConstant.PAYMODE_TFDS.equals(payMode)) {
                    tips = "待审核";
                }
                break;
            case RecipeStatusConstant.WAIT_SEND:
            case RecipeStatusConstant.CHECK_PASS_YS:
                if (RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
                    //在线支付
                    tips = "待配送.";
                } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode)) {
                    //货到付款
                    tips = "待配送.";
                } else if (RecipeBussConstant.PAYMODE_TFDS.equals(payMode)) {
                    tips = "待取药.";
                }
                break;
            case RecipeStatusConstant.IN_SEND:
                if (RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
                    //在线支付
                    tips = "待配送";
                } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode)) {
                    //货到付款
                    tips = "配送中";
                }
                break;
            case RecipeStatusConstant.CHECK_NOT_PASS_YS:
                tips = "已失效";
                if (StringUtils.isNotEmpty(orderCode) && null != order && 1 == order.getEffective()) {
                    if (RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
                        //在线支付
                        tips = "待配送";
                    } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode) || RecipeBussConstant.PAYMODE_TFDS.equals(payMode)) {
                        tips = "待审核";
                    }
                }

                break;
            case RecipeStatusConstant.REVOKE:
                tips = "已失效";
                break;
            //天猫特殊状态
            case RecipeStatusConstant.USING:
                tips = "处理中";
                break;
            default:
                tips = "未知状态" + status;

        }
        return tips;
    }
}
