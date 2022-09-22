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
import recipe.aop.LogRecord;
import recipe.client.PatientClient;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeParameterDao;
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
        if (Objects.isNull(recipe)) {
            res.setMsgCode("0");
            res.setMsg("调用撤销接口异常，无法撤销，请稍后重试");
            return res;
        }
        DrugsEnterprise drugsEnterprise = null;
        if (ValidateUtil.notNullAndZeroInteger(recipe.getEnterpriseId())) {
            drugsEnterprise = drugsEnterpriseDAO.getById(recipe.getEnterpriseId());
        }
        if (Objects.nonNull(drugsEnterprise) && new Integer(0).equals(drugsEnterprise.getOperationType())) {
            //平台流程，对接上药
            RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
            EsbWebService esbWebService = new EsbWebService();
            Map<String, Object> params = new HashMap<>();
            params.put("prescripNo", recipe.getRecipeCode());
            params.put("prescribeDate", DateConversion.formatDate(recipe.getSignDate()));
            String request = JsonToXmlUtil.jsonToXml(params);
            Map<String, String> param = new HashMap<>();
            String url = recipeParameterDao.getByName("logistics_shxk_url");
            param.put("url", url);
            esbWebService.initConfig(param);
            try {
                String webServiceResult = esbWebService.HXCFZT(request, "prsRefund");
                LOGGER.info("getDrugInventory webServiceResult:{}. ", webServiceResult);
                Map maps = (Map) JSON.parse(webServiceResult);

                if (Objects.nonNull(maps)) {
                    Boolean success = (Boolean) maps.get("success");
                    String code = (String) maps.get("code");
                    if (success && "0".equals(code)) {
                        res.setSuccess();
                    }
                } else {
                    res.setMsgCode("0");
                    res.setMsg("调用撤销接口异常，无法撤销，请稍后重试");
                }
            } catch (Exception e) {
                LOGGER.error("doCancelRecipeForEnterprise 平台流程 error", e);
                res.setMsgCode("0");
                res.setMsg("调用撤销接口异常，无法撤销，请稍后重试");
            }
        } else {
            //对接his流程
            try {
                HospitalReqTo req = new HospitalReqTo();
                req.setOrganId(recipe.getClinicOrgan());
                req.setPrescriptionNo(String.valueOf(recipe.getRecipeId()));
                req.setRecipeId(recipe.getRecipeId());
                req.setRecipeCode(recipe.getRecipeCode());
                req.setOrgCode(patientClient.getMinkeOrganCodeByOrganId(recipe.getClinicOrgan()));
                if (Objects.nonNull(drugsEnterprise)) {
                    req.setDrugsEnterpriseBean(ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterpriseBean.class));
                }

                LOGGER.info("doCancelRecipeForEnterprise recipeId={} req={}", recipe.getRecipeId(), JSONUtils.toString(req));
                res = recipeEnterpriseService.cancelRecipe(req);
                LOGGER.info("doCancelRecipeForEnterprise recipeId={} res={}", recipe.getRecipeId(), JSONUtils.toString(res));
            } catch (Exception e) {
                LOGGER.error("doCancelRecipeForEnterprise error recipeId={}", recipe.getRecipeId(), e);
                res.setMsgCode("0");
                res.setMsg("调用撤销接口异常，无法撤销，请稍后重试");
            }
        }
        return res;
    }
}
