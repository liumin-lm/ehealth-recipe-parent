package recipe.service.recipecancel;

import com.alibaba.fastjson.JSON;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.service.IRecipeEnterpriseService;
import com.ngari.platform.recipe.mode.DrugsEnterpriseBean;
import com.ngari.platform.recipe.mode.HospitalReqTo;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.utils.ValidateUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.aop.LogRecord;
import recipe.client.PatientClient;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeParameterDao;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.drugsenterprise.bean.EsbWebService;
import recipe.service.RecipeServiceSub;
import recipe.util.DateConversion;
import recipe.util.JsonToXmlUtil;
import recipe.util.ObjectCopyUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;

    /**
     * 处方撤销处方new----------(供医生端使用)
     *
     * @param recipeId 处方Id
     * @param message  处方撤销原因
     * @return Map<String, Object>
     */
    @RpcService
    @LogRecord
    public Map<String, Object> cancelRecipe(Integer recipeId, String message) {
        LOGGER.info("recipeCancelService cancelRecipe recipeId:{}", recipeId);
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
        HisResponseTO res = new HisResponseTO();
        if (Objects.isNull(recipe) || Objects.isNull(recipe.getEnterpriseId())) {
            res.setMsgCode("0");
            res.setMsg("调用撤销接口异常，无法撤销，请稍后重试");
            return res;
        }
        RemoteDrugEnterpriseService remoteDrugEnterpriseService =
                ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        return remoteDrugEnterpriseService.doCancelRecipeForEnterprise(recipe);
    }
}
