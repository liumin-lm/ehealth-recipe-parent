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
import recipe.client.PatientClient;
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
    @Autowired
    private PatientClient patientClient;

    /**
     * 处方撤销处方new----------(供医生端使用)
     *
     * @param recipeId 处方Id
     * @param message  处方撤销原因
     * @return Map<String, Object>
     */
    @RpcService
    public Map<String, Object> cancelRecipe(Integer recipeId, String message) {
        return RecipeServiceSub.cancelRecipeImpl(recipeId, 0, "", message);
    }

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
                req.setOrgCode(patientClient.getMinkeOrganCodeByOrganId(recipe.getClinicOrgan()));
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
